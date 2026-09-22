package com.madlauncher.platform

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import com.madlauncher.a11y.MadAccessibilityService
import com.madlauncher.data.LocalStore
import com.madlauncher.data.PermissionChecker
import com.madlauncher.notif.MadNotificationListener

/** One-time and ongoing setup when MAD runs as a system priv-app (MAD OS). */
object MadOsSetup {
    private const val PREFS = "mad_os_setup"
    private const val KEY_DONE = "priv_setup_done"
    private const val OP_SYSTEM_ALERT_WINDOW = 24
    private const val OP_GET_USAGE_STATS = 43
    private const val APP_OP_MODE_ALLOWED = 0

    private const val NAVIGATION_MODE = "navigation_mode"
    private const val NAVIGATION_MODE_3BUTTON = 0
    private const val OVERLAY_THREE_BUTTON = "com.android.internal.systemui.navbar.threebutton"

    /** Pixel/Quickstep packages that own the on-screen nav bar. Never disable the app. */
    internal val quickstepPackages = listOf(
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3",
    )

    private val RUNTIME_PERMISSIONS = listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_MEDIA_IMAGES,
    )

    fun onAppStart(context: Context) {
        if (!MadOsRuntime.isMadOs(context)) return
        MadOsRuntime.publishNavSlots(context, LocalStore(context).appearanceSettings)
        applyPrivAppGrants(context)
        val native = MadOsRuntime.hasStatusBarService(context) || MadOsRuntime.romSystemUiOwnsNav()
        MadOsRuntime.enableNativeSystemUiFlag(context, native)
        runCatching { restoreQuickstepForNavBar(context) }
        runCatching { forceThreeButtonNav(context) }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return
        runCatching { setDefaultHome(context) }
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    /** Pre-grant everything MAD OS needs — no setup prompts. */
    fun applyPrivAppGrants(context: Context) {
        if (!MadOsRuntime.isMadOs(context)) return
        val pkg = context.packageName
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                pm.setApplicationEnabledSetting(pkg, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0)
            }
        }
        RUNTIME_PERMISSIONS.forEach { grantRuntime(context, it) }
        allowAppOp(context, OP_SYSTEM_ALERT_WINDOW)
        allowAppOp(context, OP_GET_USAGE_STATS)
        enableNotificationListener(context)
        enableAccessibilityService(context)
        whitelistBattery(context)
        LocalStore(context).setupDismissed = true
    }

    private fun grantRuntime(context: Context, permission: String) {
        if (PermissionChecker.granted(context, permission)) return
        runCatching {
            val pm = context.packageManager
            val user = Process.myUserHandle()
            val method = PackageManager::class.java.getMethod(
                "grantRuntimePermission",
                String::class.java,
                String::class.java,
                user.javaClass,
            )
            method.invoke(pm, context.packageName, permission, user)
        }.onFailure {
            runCatching {
                Runtime.getRuntime().exec(arrayOf("pm", "grant", context.packageName, permission)).waitFor()
            }
        }
    }

    private fun allowAppOp(context: Context, op: Int) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return
        runCatching {
            val method = AppOpsManager::class.java.getMethod(
                "setMode",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java,
                Int::class.javaPrimitiveType,
            )
            method.invoke(appOps, op, Process.myUid(), context.packageName, APP_OP_MODE_ALLOWED)
        }
    }

    private fun enableNotificationListener(context: Context) {
        val cn = ComponentName(context, MadNotificationListener::class.java).flattenToString()
        val key = "enabled_notification_listeners"
        val cur = Settings.Secure.getString(context.contentResolver, key).orEmpty()
        if (cur.split(':').any { it.equals(cn, ignoreCase = true) }) return
        val merged = if (cur.isBlank()) cn else "$cur:$cn"
        Settings.Secure.putString(context.contentResolver, key, merged)
    }

    private fun enableAccessibilityService(context: Context) {
        val cn = ComponentName(context, MadAccessibilityService::class.java).flattenToString()
        val cr = context.contentResolver
        val cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        if (!cur.split(':').any { it.equals(cn, ignoreCase = true) }) {
            val merged = if (cur.isBlank()) cn else "$cur:$cn"
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, merged)
        }
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
    }

    private fun whitelistBattery(context: Context) {
        runCatching {
            Runtime.getRuntime().exec(
                arrayOf("cmd", "deviceidle", "whitelist", "+${context.packageName}"),
            ).waitFor()
        }
    }

    /**
     * Pixel SystemUI draws Back/Home/Recents through Quickstep (Nexus Launcher package).
     * Disabling that package removes the on-screen nav bar. Keep the package enabled and
     * leave MAD as the HOME role holder.
     */
    private fun restoreQuickstepForNavBar(context: Context) {
        val pm = context.packageManager
        quickstepPackages.forEach { target ->
            runCatching {
                pm.setApplicationEnabledSetting(
                    target,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    0,
                )
            }
        }
    }

    /** MAD uses 3-button navigation; gesture nav is not supported. */
    private fun forceThreeButtonNav(context: Context) {
        runCatching {
            Settings.Secure.putInt(context.contentResolver, NAVIGATION_MODE, NAVIGATION_MODE_3BUTTON)
        }
        runCatching { enableNavOverlay(context, OVERLAY_THREE_BUTTON) }
        runCatching {
            Runtime.getRuntime().exec(
                arrayOf("cmd", "overlay", "enable", "--user", "0", OVERLAY_THREE_BUTTON),
            ).waitFor()
        }
    }

    private fun enableNavOverlay(context: Context, packageName: String) {
        val omClass = Class.forName("android.content.om.OverlayManager")
        val om = context.getSystemService(omClass) ?: return
        val user = Process.myUserHandle()
        val ok = runCatching {
            val idClass = Class.forName("android.content.om.OverlayIdentifier")
            val id = idClass.getConstructor(String::class.java).newInstance(packageName)
            omClass.getMethod("setEnabled", idClass, Boolean::class.javaPrimitiveType, UserHandle::class.java)
                .invoke(om, id, java.lang.Boolean.TRUE, user)
            true
        }.getOrDefault(false)
        if (ok) return
        val userId = runCatching {
            UserHandle::class.java.getMethod("getIdentifier").invoke(user) as Int
        }.getOrDefault(0)
        omClass.getMethod(
            "setEnabled",
            String::class.java,
            Boolean::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        ).invoke(om, packageName, java.lang.Boolean.TRUE, userId)
    }

    private fun setDefaultHome(context: Context) {
        if (PermissionChecker.isDefaultHome(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                Runtime.getRuntime().exec(
                    arrayOf("cmd", "role", "add-role-holder", "android.app.role.HOME", context.packageName),
                ).waitFor()
            }
        }
    }
}
