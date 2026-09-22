package com.madlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import android.provider.Settings
import com.madlauncher.action.IntentExecutor
import com.madlauncher.action.WeatherLaunchGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable?,
) {
    val id: String get() = "$packageName/$activityName"

    companion object {
        const val SETTINGS_ACTIVITY = "com.madlauncher.internal.LauncherSettings"

        fun isMadSettings(packageName: String, activityName: String, selfPackage: String): Boolean {
            if (!packageName.equals(selfPackage, ignoreCase = true)) return false
            return activityName == SETTINGS_ACTIVITY ||
                activityName.endsWith(".MainActivity") ||
                activityName == "com.madlauncher.MainActivity"
        }
    }
}

class InstalledAppsRepository(private val appContext: Context) {
    val context: Context get() = appContext

    suspend fun loadLaunchableApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val fromLauncher = runCatching {
            val launcherApps = appContext.getSystemService(LauncherApps::class.java) ?: return@runCatching null
            launcherApps.getActivityList(null, Process.myUserHandle()).mapNotNull { info ->
                val cn = info.componentName
                if (!isLauncherEnabled(cn.packageName, cn.className)) return@mapNotNull null
                LaunchableApp(
                    label = info.label?.toString() ?: cn.packageName,
                    packageName = cn.packageName,
                    activityName = cn.className,
                    icon = info.getBadgedIcon(0),
                )
            }
        }.getOrNull()
        val list = fromLauncher ?: queryLegacy()
        collapseDuplicateLaunchers(list)
            .let { injectMadSettings(it) }
            .sortedBy { it.label.lowercase() }
    }

    private fun queryLegacy(): List<LaunchableApp> {
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolves.mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            if (!isLauncherEnabled(activity.packageName, activity.name)) return@mapNotNull null
            LaunchableApp(
                label = info.loadLabel(pm)?.toString() ?: activity.packageName,
                packageName = activity.packageName,
                activityName = activity.name,
                icon = info.loadIcon(pm),
            )
        }
    }

    private fun isLauncherEnabled(packageName: String, activityName: String): Boolean {
        val pm = appContext.packageManager
        val cn = ComponentName(packageName, activityName)
        when (pm.getComponentEnabledSetting(cn)) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
            -> return false
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> return true
        }
        val info = runCatching {
            pm.getActivityInfo(cn, PackageManager.MATCH_DISABLED_COMPONENTS)
        }.getOrNull() ?: return false
        return info.enabled && info.applicationInfo.enabled
    }

    private fun collapseDuplicateLaunchers(apps: List<LaunchableApp>): List<LaunchableApp> {
        val byId = apps.associateBy { it.id }
        val preferred = { pkg: String ->
            appContext.packageManager.getLaunchIntentForPackage(pkg)?.component?.className
        }
        val kept = LauncherEntryDeduper.collapse(
            apps.map { LauncherEntryDeduper.Entry(it.label, it.packageName, it.activityName) },
            preferred,
        )
        return kept.mapNotNull { byId["${it.packageName}/${it.activityName}"] }
    }

    private fun injectMadSettings(apps: List<LaunchableApp>): List<LaunchableApp> {
        if (com.madlauncher.BuildConfig.ACTION_BOX_APP) return apps
        val self = appContext.packageName
        val withoutHome = apps.filterNot {
            it.packageName.equals(self, ignoreCase = true) &&
                (it.activityName.endsWith(".MainActivity") || it.activityName == "com.madlauncher.MainActivity")
        }
        if (withoutHome.any { it.packageName.equals(self, ignoreCase = true) && it.activityName == LaunchableApp.SETTINGS_ACTIVITY }) {
            return withoutHome
        }
        val icon = runCatching { appContext.packageManager.getApplicationIcon(self) }.getOrNull()
        return withoutHome + LaunchableApp(
            label = appContext.getString(com.madlauncher.R.string.mad_settings_label),
            packageName = self,
            activityName = LaunchableApp.SETTINGS_ACTIVITY,
            icon = icon,
        )
    }

    fun launch(app: LaunchableApp) {
        if (LaunchableApp.isMadSettings(app.packageName, app.activityName, appContext.packageName)) {
            return
        }
        if (WeatherLaunchGuard.isBlocked(app.packageName)) {
            IntentExecutor(appContext).openWeather()
            return
        }
        val launcherApps = appContext.getSystemService(LauncherApps::class.java)
        val component = ComponentName(app.packageName, app.activityName)
        if (launcherApps != null) {
            val started = runCatching {
                launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
                true
            }.getOrDefault(false)
            if (started) return
        }
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun openDialer(number: String? = null) {
        val intent = if (number.isNullOrBlank()) {
            Intent(Intent.ACTION_DIAL)
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun openBrowser(url: String = "https://www.google.com") {
        appContext.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openSettings() {
        appContext.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun defaultDialer(): LaunchableApp? {
        val dial = Intent(Intent.ACTION_DIAL).addCategory(Intent.CATEGORY_DEFAULT)
        val info = appContext.packageManager.resolveActivity(dial, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            ?: return null
        return launchableFrom(info.packageName, info.name)
    }

    fun defaultBrowser(): LaunchableApp? {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        val info = appContext.packageManager.resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            ?: return null
        if (info.packageName == appContext.packageName) return null
        return launchableFrom(info.packageName, info.name)
    }

    private fun launchableFrom(packageName: String, activityName: String): LaunchableApp? {
        val pm = appContext.packageManager
        val cn = ComponentName(packageName, activityName)
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(cn)
        val info = pm.resolveActivity(intent, 0) ?: pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName),
            0,
        ).firstOrNull()
        val activity = info?.activityInfo ?: return LaunchableApp(packageName, packageName, activityName, null)
        return LaunchableApp(
            label = info.loadLabel(pm)?.toString() ?: packageName,
            packageName = activity.packageName,
            activityName = activity.name,
            icon = info.loadIcon(pm),
        )
    }

    fun webSearch(query: String, engine: WebSearchEngine? = null) {
        val chosen = engine ?: LocalStore(appContext).appearanceSettings.webSearchEngine
        openBrowser(chosen.searchUrl(query))
    }
}
