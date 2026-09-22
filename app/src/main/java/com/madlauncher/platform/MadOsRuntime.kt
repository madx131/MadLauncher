package com.madlauncher.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.madlauncher.data.AppearanceSettings
import com.madlauncher.data.NavShortcutKind
import com.madlauncher.data.NavShortcutPlacements
import org.json.JSONObject
import java.io.File

/**
 * MAD OS integration: system-default detection, Settings.Secure sync for patched
 * SystemUI, and native nav mode (skip accessibility overlays when SystemUI owns
 * the buttons).
 */
object MadOsRuntime {
    const val ACTION_NAV_SHORTCUT = "com.madlauncher.action.NAV_SHORTCUT"
    const val EXTRA_NAV_KIND = "kind"
    const val SECURE_NAV_SLOTS = "mad_nav_slots"
    const val SECURE_NATIVE_SYSTEMUI = "mad_native_systemui"
    private const val STATUS_BAR_SERVICE = "android.permission.STATUS_BAR_SERVICE"

    fun isPrivApp(context: Context): Boolean {
        val ai = context.applicationContext.applicationInfo
        val src = ai.sourceDir.orEmpty()
        return src.contains("/priv-app/") ||
            (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    /**
     * True when MAD is the OS / default system launcher, not a sideloaded app
     * that must ask the user for Accessibility.
     */
    fun isMadOs(context: Context): Boolean {
        if (isPrivApp(context)) return true
        if (hasMadOsRomMarker()) return true
        return isKnownMadOsBuild(Build.PRODUCT.orEmpty(), Build.DEVICE.orEmpty())
    }

    fun hasMadOsRomMarker(): Boolean {
        if (File("/system/etc/mad/systemui_native").exists()) return true
        if (File("/system/etc/mad").isDirectory) return true
        return listOf("ro.mad.os", "ro.mad.launcher", "ro.mad.systemui")
            .any { readProp(it).isNotBlank() }
    }

    /** Redmi Note 7 AOSP (`aosp_lavender`) and similar MAD OS builds. */
    fun isKnownMadOsBuild(product: String, device: String): Boolean {
        val p = product.lowercase()
        val d = device.lowercase()
        if (p == "aosp_lavender") return true
        if (d == "lavender" && p.contains("aosp")) return true
        return false
    }

    fun hasStatusBarService(context: Context): Boolean =
        context.checkCallingOrSelfPermission(STATUS_BAR_SERVICE) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * True only when the ROM itself draws nav shortcuts (marker / prop).
     * Do not treat [SECURE_NATIVE_SYSTEMUI] as proof — this app writes that flag.
     */
    fun romSystemUiOwnsNav(): Boolean {
        if (File("/system/etc/mad/systemui_native").exists()) return true
        return readProp("ro.mad.systemui").isNotBlank()
    }

    /**
     * Skip a11y nav overlays only when something else can actually draw them:
     * patched SystemUI, or TYPE_NAVIGATION_BAR_PANEL ([STATUS_BAR_SERVICE]).
     * MAD OS without those still uses overlays; they are enabled in the
     * background so the user is not sent to Accessibility settings.
     */
    fun useNativeSystemUi(context: Context): Boolean {
        val romOwns = romSystemUiOwnsNav()
        if (com.madlauncher.systemui.MadNavBarOverlay.isDenied()) return romOwns
        return shouldSkipA11yNav(
            hasStatusBarService = hasStatusBarService(context),
            romOwnsNav = romOwns,
            overlayDenied = false,
        )
    }

    fun shouldSkipA11yNav(
        hasStatusBarService: Boolean,
        romOwnsNav: Boolean,
        overlayDenied: Boolean,
    ): Boolean {
        if (overlayDenied) return romOwnsNav
        return romOwnsNav || hasStatusBarService
    }

    fun publishNavSlots(context: Context, appearance: AppearanceSettings) {
        if (!isMadOs(context)) return
        val obj = JSONObject()
        val slots = if (appearance.experimentalNavShortcuts) {
            appearance.navShortcutSlots
        } else {
            emptyList()
        }
        NavShortcutKind.entries.forEach { kind ->
            obj.put(kind.name, NavShortcutPlacements.placementOf(slots, kind).name)
        }
        obj.put(
            "mode",
            if (appearance.experimentalNavShortcuts) appearance.navShortcutMode.name else "CLASSIC",
        )
        runCatching {
            Settings.Secure.putString(context.contentResolver, SECURE_NAV_SLOTS, obj.toString())
        }
        runCatching {
            context.contentResolver.notifyChange(
                Settings.Secure.getUriFor(SECURE_NAV_SLOTS),
                null,
            )
        }
    }

    fun enableNativeSystemUiFlag(context: Context, enabled: Boolean) {
        if (!isMadOs(context)) return
        runCatching {
            Settings.Secure.putInt(
                context.contentResolver,
                SECURE_NATIVE_SYSTEMUI,
                if (enabled) 1 else 0,
            )
        }
    }

    fun navKindFromExtra(extra: String?): NavShortcutKind? = when (extra?.uppercase()) {
        NavShortcutKind.NOTIFICATIONS.name, "NOTIFICATIONS" -> NavShortcutKind.NOTIFICATIONS
        NavShortcutKind.SYSTEM_APPS.name, "SYSTEM_APPS", "SYSTEM" -> NavShortcutKind.SYSTEM_APPS
        NavShortcutKind.QUICK_SETTINGS.name, "QUICK_SETTINGS", "QS" -> NavShortcutKind.QUICK_SETTINGS
        else -> null
    }

    fun panelForKind(kind: NavShortcutKind): String = when (kind) {
        NavShortcutKind.NOTIFICATIONS -> "notifications"
        NavShortcutKind.SYSTEM_APPS -> "systemnotifs"
        NavShortcutKind.QUICK_SETTINGS -> "quicksettings"
    }

    private fun readProp(key: String): String {
        return runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key)
                ?.toString()
                ?.trim()
                .orEmpty()
        }.getOrDefault("")
    }
}
