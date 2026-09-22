package com.madlauncher.data

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.madlauncher.a11y.MadAccessibilityService
import com.madlauncher.notif.MadNotificationListener
import com.madlauncher.platform.MadOsRuntime
import com.madlauncher.platform.MadOsSetup

data class GrantState(
    val defaultHome: Boolean = false,
    val notificationListener: Boolean = false,
    val overlay: Boolean = false,
    val accessibility: Boolean = false,
    val contacts: Boolean = false,
    val postNotifications: Boolean = true,
    val battery: Boolean = false,
    val location: Boolean = false,
    val readSms: Boolean = false,
    val usageStats: Boolean = false,
    /** MAD OS / system default — core features must not require a user Accessibility toggle. */
    val systemDefault: Boolean = false,
) {
    val requiredDone: Boolean
        get() {
            // Overlay / Accessibility are experimental (nav shortcuts, MAD windows)
            // and are requested only when those features are turned on — not on first setup.
            return defaultHome && notificationListener && contacts && postNotifications && battery
        }
}

object PermissionChecker {
    fun snapshot(context: Context): GrantState {
        val madOs = MadOsRuntime.isMadOs(context)
        if (madOs) {
            MadOsSetup.applyPrivAppGrants(context)
        }
        return GrantState(
            defaultHome = isDefaultHome(context),
            notificationListener = isNotificationListener(context),
            overlay = Settings.canDrawOverlays(context),
            accessibility = isAccessibilityEnabled(context),
            contacts = granted(context, Manifest.permission.READ_CONTACTS),
            postNotifications = if (Build.VERSION.SDK_INT >= 33) {
                granted(context, Manifest.permission.POST_NOTIFICATIONS)
            } else true,
            battery = isIgnoringBattery(context),
            location = granted(context, Manifest.permission.ACCESS_COARSE_LOCATION),
            readSms = granted(context, Manifest.permission.READ_SMS),
            usageStats = isUsageAccess(context),
            systemDefault = madOs,
        )
    }

    fun isDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val home = intent.resolveActivity(context.packageManager)?.packageName
        return home == context.packageName
    }

    fun isNotificationListener(context: Context): Boolean {
        val component = android.content.ComponentName(context, MadNotificationListener::class.java)
        if (Build.VERSION.SDK_INT >= 27) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationListenerAccessGranted(component)) return true
        }
        val cn = component.flattenToString()
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        return flat.contains(cn)
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val want = MadAccessibilityService::class.java.name
        val lists = listOf(
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC),
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK),
        )
        return lists.any { list -> list.any { it.resolveInfo.serviceInfo.name == want } }
    }

    fun accessibilitySettings(context: Context): Intent {
        // ACCESSIBILITY_DETAILS_SETTINGS needs OPEN_ACCESSIBILITY_DETAILS_SETTINGS
        // (privileged). resolveActivity is true, startActivity still throws.
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(
                Intent.EXTRA_COMPONENT_NAME,
                ComponentName(context, MadAccessibilityService::class.java),
            )
        }
    }

    fun isUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessSettings(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun isIgnoringBattery(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun granted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun notificationListenerSettings(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun overlaySettings(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
    }

    fun batteryRequest(context: Context): Intent {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        return if (direct.resolveActivity(context.packageManager) != null) direct
        else Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
}
