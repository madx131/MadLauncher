package com.madlauncher.notif

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.IBinder
import android.provider.Settings
import com.madlauncher.MadApp
import com.madlauncher.a11y.MadAccessibilityService
import com.madlauncher.data.AppearanceSettings
import com.madlauncher.data.NavShortcutKind
import com.madlauncher.data.NavShortcutMode

object SystemShadeController {
    fun expandNotifications(context: Context): Boolean {
        @Suppress("WrongConstant")
        val statusBar = context.getSystemService("statusbar")
        val reflected = runCatching {
            statusBar?.javaClass?.getMethod("expandNotificationsPanel")?.invoke(statusBar)
            true
        }.getOrDefault(false)
        if (reflected) return true
        return runCatching {
            Runtime.getRuntime().exec(arrayOf("cmd", "statusbar", "expand-notifications")).waitFor() == 0
        }.getOrDefault(false)
    }

    /** System Overview / Recents. Does not open MAD's RecentsPanel. */
    fun expandRecents(context: Context): Boolean {
        if (a11yRecents()) return true
        // Same path as the recents button: SystemUI → OverviewProxy.onOverviewToggle().
        // Starting RecentsActivity as a normal task shows tiny cards and a blank top inset.
        if (statusBarRecents(context)) {
            scheduleRecentsFallback(context)
            return true
        }
        if (startRecentsActivity(context)) return true
        return intentRecents(context)
    }

    private fun a11yRecents(): Boolean {
        val a11y = MadAccessibilityService.instance ?: return false
        return runCatching {
            a11y.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }.getOrDefault(false)
    }

    private fun startRecentsActivity(context: Context): Boolean {
        val pm = context.packageManager
        val starter = recentsStartContext(context)
        for (flat in recentsActivityCandidates(systemRecentsComponentName(), quickstepPackages(pm))) {
            val cn = ComponentName.unflattenFromString(flat) ?: continue
            if (runCatching { pm.getActivityInfo(cn, 0) }.isFailure) continue
            val launched = runCatching {
                val intent = Intent().setComponent(cn).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                )
                val opts = recentsLaunchOptions()
                if (opts != null) starter.startActivity(intent, opts) else starter.startActivity(intent)
                true
            }.getOrDefault(false)
            if (launched) return true
        }
        return false
    }

    private fun recentsStartContext(context: Context): Context {
        val app = context.applicationContext as? MadApp
        return app?.foregroundActivity ?: app?.launcherActivity ?: context
    }

    /**
     * toggleRecentApps can return without error and still no-op. If home still has
     * focus shortly after, open Recents as ACTIVITY_TYPE_RECENTS instead.
     */
    private fun scheduleRecentsFallback(context: Context) {
        val home = recentsStartContext(context) as? android.app.Activity ?: return
        home.window.decorView.postDelayed({
            if (home.hasWindowFocus()) startRecentsActivity(context)
        }, 280)
    }

    private fun systemRecentsComponentName(): String? {
        val id = Resources.getSystem().getIdentifier("config_recentsComponentName", "string", "android")
        if (id == 0) return null
        return runCatching { Resources.getSystem().getString(id) }.getOrNull()
            ?.trim()
            ?.takeIf { it.contains('/') }
    }

    private fun quickstepPackages(pm: PackageManager): List<String> {
        val found = runCatching {
            pm.queryIntentServices(Intent(QUICKSTEP_SERVICE), PackageManager.MATCH_ALL)
                .map { it.serviceInfo.packageName }
        }.getOrDefault(emptyList())
        return (found + FALLBACK_RECENTS_PACKAGES).distinct()
    }

    private fun recentsLaunchOptions(): android.os.Bundle? = runCatching {
        val opts = ActivityOptions.makeBasic()
        opts.javaClass.getMethod("setLaunchActivityType", Int::class.javaPrimitiveType)
            .invoke(opts, ACTIVITY_TYPE_RECENTS)
        runCatching { opts.javaClass.getMethod("setTransientLaunch").invoke(opts) }
        opts.toBundle()
    }.getOrNull()

    private fun statusBarRecents(context: Context): Boolean {
        @Suppress("WrongConstant")
        val statusBar = context.getSystemService("statusbar") ?: return false
        if (invokeRecentsApi(statusBar)) return true
        val svc = statusBarBinder(statusBar) ?: return false
        return invokeRecentsApi(svc)
    }

    private fun invokeRecentsApi(target: Any): Boolean {
        for (name in listOf("toggleRecentApps", "showRecentApps", "expandRecentApps")) {
            val methods = target.javaClass.methods.filter { it.name == name }
            for (m in methods) {
                val ok = runCatching {
                    when {
                        m.parameterCount == 0 -> {
                            m.invoke(target)
                            true
                        }
                        m.parameterCount == 1 &&
                            m.parameterTypes[0] == Boolean::class.javaPrimitiveType -> {
                            m.invoke(target, false)
                            true
                        }
                        else -> false
                    }
                }.getOrDefault(false)
                if (ok) return true
            }
        }
        return false
    }

    private fun statusBarBinder(statusBar: Any): Any? {
        runCatching {
            val get = statusBar.javaClass.getDeclaredMethod("getService")
            get.isAccessible = true
            val svc = get.invoke(statusBar)
            if (svc != null) return svc
        }
        return runCatching {
            val sm = Class.forName("android.os.ServiceManager")
            val binder = sm.getMethod("getService", String::class.java)
                .invoke(null, "statusbar") as IBinder
            val stub = Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
            stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        }.getOrNull()
    }

    private fun intentRecents(context: Context): Boolean {
        val actions = listOf(
            "com.android.systemui.recents.TOGGLE_RECENTS",
            "android.intent.action.TOGGLE_RECENTS",
        )
        for (action in actions) {
            val launched = runCatching {
                context.startActivity(
                    Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            }.getOrDefault(false)
            if (launched) return true
            val broadcast = runCatching {
                context.sendBroadcast(Intent(action))
                true
            }.getOrDefault(false)
            if (broadcast) return true
        }
        return false
    }

    /** Extended QS shade (same as a second status-bar swipe). */
    fun expandQuickSettings(context: Context): Boolean {
        @Suppress("WrongConstant")
        val statusBar = context.getSystemService("statusbar")
        val reflected = runCatching {
            statusBar?.javaClass?.getMethod("expandSettingsPanel")?.invoke(statusBar)
            true
        }.getOrDefault(false)
        if (reflected) return true
        return runCatching {
            Runtime.getRuntime().exec(arrayOf("cmd", "statusbar", "expand-settings")).waitFor() == 0
        }.getOrDefault(false)
    }

    fun openStockPanel(context: Context, panel: String): Boolean = when (stockPanelKind(panel)) {
        StockPanelKind.QUICK_SETTINGS -> expandQuickSettings(context)
        StockPanelKind.NOTIFICATIONS -> expandNotifications(context)
        StockPanelKind.NONE -> false
    }

    enum class StockPanelKind { NOTIFICATIONS, QUICK_SETTINGS, NONE }

    /** Home is not a shade panel — unknown ids must not expand notifications. */
    fun stockPanelKind(panel: String): StockPanelKind = when (panel) {
        "quicksettings" -> StockPanelKind.QUICK_SETTINGS
        "notifications", "systemnotifs", "systemapps" -> StockPanelKind.NOTIFICATIONS
        else -> StockPanelKind.NONE
    }

    fun openStockForKind(context: Context, kind: NavShortcutKind): Boolean = when (kind) {
        NavShortcutKind.QUICK_SETTINGS -> expandQuickSettings(context)
        NavShortcutKind.NOTIFICATIONS, NavShortcutKind.SYSTEM_APPS -> expandNotifications(context)
    }

    fun syncWithAppearance(context: Context, appearance: AppearanceSettings) {
        if (appearance.experimentalMadWindows && appearance.navShortcutMode != NavShortcutMode.CLASSIC) {
            hideSystemQuickSettings(context)
        } else {
            restoreSystemQuickSettings(context)
        }
    }

    fun hideSystemQuickSettings(context: Context) {
        val cr = context.contentResolver
        runCatching { Settings.Secure.putInt(cr, "sysui_qqs_count", 0) }
        runCatching { Settings.Secure.putInt(cr, "qs_show_brightness", 0) }
        runCatching {
            Runtime.getRuntime().exec(arrayOf("cmd", "statusbar", "set-large-tiles"))
        }
        setQuickSettingsDisabled(context, disabled = true)
    }

    fun restoreSystemQuickSettings(context: Context) {
        val cr = context.contentResolver
        runCatching { Settings.Secure.putInt(cr, "sysui_qqs_count", 5) }
        runCatching { Settings.Secure.putInt(cr, "qs_show_brightness", 1) }
        setQuickSettingsDisabled(context, disabled = false)
    }

    private fun setQuickSettingsDisabled(context: Context, disabled: Boolean) {
        val statusBar = context.getSystemService("statusbar") ?: return
        runCatching {
            val method = statusBar.javaClass.getMethod("disable2", Int::class.javaPrimitiveType)
            method.invoke(statusBar, if (disabled) 0x00000001 else 0)
        }
    }

    internal fun recentsActivityCandidates(
        configFlattened: String?,
        quickstepPackages: List<String>,
    ): List<String> {
        val out = LinkedHashSet<String>()
        val cfg = configFlattened?.trim().orEmpty()
        if (cfg.contains('/')) out += cfg
        for (pkg in quickstepPackages) {
            for (cls in RECENTS_ACTIVITY_CLASSES) {
                out += "$pkg/$cls"
            }
        }
        return out.toList()
    }

    private const val QUICKSTEP_SERVICE = "android.intent.action.QUICKSTEP_SERVICE"
    private const val ACTIVITY_TYPE_RECENTS = 3

    private val RECENTS_ACTIVITY_CLASSES = listOf(
        "com.miui.home.recents.RecentsActivity",
        "com.android.quickstep.RecentsActivity",
        "com.android.systemui.recents.RecentsActivity",
    )

    private val FALLBACK_RECENTS_PACKAGES = listOf(
        "com.mi.android.globallauncher",
        "com.miui.home",
        "com.google.android.apps.nexuslauncher",
        "com.android.systemui",
    )
}
