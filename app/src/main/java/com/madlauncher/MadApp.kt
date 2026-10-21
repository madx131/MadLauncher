package com.madlauncher

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import com.madlauncher.action.ActionApps
import com.madlauncher.action.CommandAliases
import com.madlauncher.action.UserShortcuts
import com.madlauncher.action.IntentExecutor
import com.madlauncher.data.ContactsRepository
import com.madlauncher.data.InstalledAppsRepository
import com.madlauncher.data.LanguageOverlay
import com.madlauncher.data.IconStyleStore
import com.madlauncher.data.LocalStore
import com.madlauncher.data.MadShadeWindows
import com.madlauncher.data.SettingsExternalBackup
import com.madlauncher.notif.MessagingAppRegistry
import com.madlauncher.notif.SmsInboxMonitor
import com.madlauncher.notif.SystemShadeController
import com.madlauncher.widget.MadAppWidgetHost
import kotlinx.coroutines.launch
import java.util.Locale

class MadApp : Application() {
    lateinit var appsRepo: InstalledAppsRepository
        private set
    lateinit var contacts: ContactsRepository
        private set
    lateinit var store: LocalStore
        private set
    lateinit var actionApps: ActionApps
        private set
    lateinit var intents: IntentExecutor
        private set
    lateinit var widgetHost: MadAppWidgetHost
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Set by [com.madlauncher.ui.LauncherScreen] while home UI is alive. */
    @Volatile
    var panelOpener: ((String) -> Unit)? = null

    /** Close floating launcher windows (Recents / Home leave). */
    @Volatile
    var windowDismisser: (() -> Unit)? = null

    /** Back from the system nav button when Compose does not receive it. */
    @Volatile
    var backHandler: (() -> Boolean)? = null

    /** Home from the system nav button / HOME intent when the launcher is already in front. */
    @Volatile
    var homeHandler: (() -> Unit)? = null

    /** Visible activity — Action Box must start other apps from here or Android 15 drops them. */
    @Volatile
    var foregroundActivity: Activity? = null
        private set

    /** Set by [MainActivity] while the home activity is resumed. */
    @Volatile
    var launcherActivity: MainActivity? = null

    fun deferLauncherToBack() {
        launcherActivity?.moveTaskToBack(true)
    }

    fun notifyHomePressed() {
        if (LauncherCommands.isHomeSuppressed()) return
        if (LauncherWindowDismiss.shouldPreserve()) return
        mainHandler.post {
            homeHandler?.invoke() ?: LauncherCommands.goHome()
        }
    }

    /**
     * Open a nav-shortcut panel. Prefers a direct UI callback so we don't depend on
     * Activity start / StateFlow collector races with Home.
     */
    fun openLauncherPanel(panel: String) {
        if (panel == "home") {
            notifyHomePressed()
            return
        }
        if (!NavShortcutBridge.enabled) return
        if (!NavShortcutBridge.settings.experimentalMadWindows) {
            com.madlauncher.notif.SystemShadeController.openStockPanel(this, panel)
            return
        }
        // Suppress Home first so any REORDER_TO_FRONT home intent cannot wipe the panel.
        LauncherCommands.request(panel)
        mainHandler.post {
            panelOpener?.invoke(panel)
        }
    }

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = AppsReloadBus.request()
        override fun onPackageRemoved(packageName: String, user: UserHandle) = AppsReloadBus.request()
        override fun onPackageChanged(packageName: String, user: UserHandle) = AppsReloadBus.request()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = AppsReloadBus.request()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = AppsReloadBus.request()
        override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) = AppsReloadBus.request()
        override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) = AppsReloadBus.request()
    }

    override fun startActivity(intent: Intent) {
        LauncherWindowDismiss.noteIfKeepWindows(intent)
        super.startActivity(intent)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        LauncherWindowDismiss.noteIfKeepWindows(intent)
        super.startActivity(intent, options)
    }

    override fun attachBaseContext(base: Context) {
        if (BuildConfig.ACTION_BOX_APP) {
            super.attachBaseContext(base)
        } else {
            super.attachBaseContext(launcherContext(base))
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (!BuildConfig.ACTION_BOX_APP) {
            // Before LocalStore loads — recover prefs wiped by uninstall when external backup exists.
            runCatching { SettingsExternalBackup.restoreIfNeeded(this) }
        }
        appsRepo = InstalledAppsRepository(this)
        contacts = ContactsRepository(this)
        store = LocalStore(this)
        actionApps = ActionApps(this)
        intents = IntentExecutor(this)
        widgetHost = MadAppWidgetHost(this)
        registerActivityLifecycleCallbacks(foregroundTracker)
        getSystemService(LauncherApps::class.java)
            ?.registerCallback(launcherAppsCallback, Handler(Looper.getMainLooper()))
        runCatching { com.madlauncher.action.InstantModules.loadFromAssets(assets) }
        CommandAliases.replaceExtras(store.userCommandAliases)
        CommandAliases.replaceDisabledBuiltIns(store.disabledBuiltInCommandAliases)
        UserShortcuts.replace(store.userShortcuts)
        if (BuildConfig.ACTION_BOX_APP) {
            LanguageOverlay.applySystemLanguage(this)
            IconStyleStore.activate(this, store.appearanceSettings.iconStyleId)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val fileName = com.madlauncher.data.SystemLanguagePack.fileNameFor(
                    LanguageOverlay.systemLocale(),
                ) ?: return@launch
                val dest = java.io.File(LanguageOverlay.deviceDir(this@MadApp), fileName)
                if (dest.exists()) return@launch
                com.madlauncher.data.LanguageCatalog.downloadPack(
                    this@MadApp,
                    com.madlauncher.data.LanguagePackEntry(fileName, fileName),
                ).onSuccess { LanguageOverlay.applySystemLanguage(this@MadApp) }
            }
            return
        }
        widgetHost.ensureListening()
        val appearance = MadShadeWindows.sanitize(store.appearanceSettings)
        if (appearance != store.appearanceSettings) {
            store.appearanceSettings = appearance
        }
        LanguageOverlay.migrateInstalledPacks(this)
        val overlay = com.madlauncher.data.LanguageCodes.migrateFileName(
            store.appearanceSettings.languageOverlay,
        )
        val systemPack = if (!store.languageUserPicked && overlay.isBlank()) {
            com.madlauncher.data.SystemLanguagePack.fileNameFor(LanguageOverlay.systemLocale())
        } else {
            null
        }
        val appliedOverlay = systemPack ?: overlay
        if (appliedOverlay != store.appearanceSettings.languageOverlay) {
            store.appearanceSettings = store.appearanceSettings.copy(languageOverlay = appliedOverlay)
        }
        if (appliedOverlay == "heb.json" || appliedOverlay == "rus.json") {
            LanguageOverlay.installBundledPack(this, appliedOverlay)
        }
        LanguageOverlay.load(this, appliedOverlay)
        IconStyleStore.activate(this, store.appearanceSettings.iconStyleId)
        val live = store.appearanceSettings
        NavShortcutBridge.update(live)
        AppearanceBridge.update(live)
        SystemShadeController.syncWithAppearance(this, live)
        seedPlatformBridge()
        runCatching { MessagingAppRegistry.refresh(this, actionApps) }
        SmsInboxMonitor.start(this)
        com.madlauncher.platform.MadOsSetup.onAppStart(this)
        runCatching { com.madlauncher.systemui.MadNavBarOverlay.sync(this) }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (BuildConfig.ACTION_BOX_APP) {
            LanguageOverlay.applySystemLanguage(this)
        }
    }

    private fun seedPlatformBridge() {
        val familyName = store.platformOemFamily
        val family = runCatching {
            com.madlauncher.platform.OemFamily.valueOf(familyName.orEmpty())
        }.getOrDefault(com.madlauncher.platform.PlatformDetector.detect(this))
        val cached = store.loadPlatformCapabilities()
        val caps = if (cached.isEmpty()) {
            com.madlauncher.platform.PlatformProfile.baseline(family)
        } else {
            cached.mapNotNull { (k, v) ->
                val feature = runCatching { com.madlauncher.platform.PlatformFeature.valueOf(k) }.getOrNull()
                    ?: return@mapNotNull null
                val status = runCatching { com.madlauncher.platform.FeatureStatus.valueOf(v) }.getOrNull()
                    ?: return@mapNotNull null
                feature to status
            }.toMap()
        }
        com.madlauncher.platform.PlatformBridge.update(
            com.madlauncher.platform.PlatformProfile.build(
                family,
                caps,
                store.platformProbeAt,
                actionBoxCatalog = store.actionBoxCatalog,
            ),
        )
    }

    private val foregroundTracker = object : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) {
            foregroundActivity = activity
            com.madlauncher.overlay.EdgeDockOverlay.sync(activity)
        }
        override fun onActivityPaused(activity: Activity) {
            if (foregroundActivity === activity) foregroundActivity = null
            com.madlauncher.overlay.EdgeDockOverlay.sync(activity)
            if (!BuildConfig.ACTION_BOX_APP) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    runCatching { SettingsExternalBackup.exportNow(this@MadApp) }
                }
            }
        }
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            if (foregroundActivity === activity) foregroundActivity = null
        }
    }

    companion object {
        lateinit var instance: MadApp
            private set

        fun englishContext(base: Context): Context = configurationContext(base, Locale.US)

        /** Hebrew ships in the APK. A Hebrew phone uses it until Settings picks another language. */
        fun launcherContext(base: Context): Context {
            val prefs = base.getSharedPreferences("mad_store", Context.MODE_PRIVATE)
            val saved = runCatching {
                val raw = prefs.getString("appearance", null) ?: return@runCatching ""
                org.json.JSONObject(raw).optString("languageOverlay", "")
            }.getOrDefault("")
            val overlay = com.madlauncher.data.LanguageCodes.migrateFileName(saved)
            val picked = prefs.getBoolean("language_user_picked", false)
            val systemHebrew = runCatching {
                val lang = base.resources.configuration.locales[0].language
                lang == "he" || lang == "iw"
            }.getOrDefault(false)
            val hebrew = overlay == "heb.json" || (!picked && overlay.isBlank() && systemHebrew)
            return configurationContext(base, if (hebrew) Locale.forLanguageTag("he") else Locale.US)
        }

        private fun configurationContext(base: Context, locale: Locale): Context {
            Locale.setDefault(locale)
            val baseConfig = base.resources.configuration
            val config = Configuration(baseConfig)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            // Keep system font size / bold-text slider (setLocale can drop accessibility fields on OEMs).
            config.fontScale = baseConfig.fontScale
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                config.fontWeightAdjustment = baseConfig.fontWeightAdjustment
            }
            return base.createConfigurationContext(config)
        }
    }
}
