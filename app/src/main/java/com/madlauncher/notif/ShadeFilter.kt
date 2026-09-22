package com.madlauncher.notif

import android.app.Notification
import android.service.notification.StatusBarNotification

object ShadeFilter {
    fun keepInSystemShade(sbn: StatusBarNotification): Boolean {
        if (isCoreSystemPackage(sbn.packageName)) return true
        if (sbn.isOngoing || !sbn.isClearable) return true
        val flags = sbn.notification.flags
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return true
        if (flags and Notification.FLAG_NO_CLEAR != 0) return true
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return true
        return when (sbn.notification.category) {
            Notification.CATEGORY_SYSTEM,
            Notification.CATEGORY_STATUS,
            Notification.CATEGORY_ERROR,
            -> true
            else -> false
        }
    }

    private fun isCoreSystemPackage(pkg: String): Boolean {
        if (pkg == "android" || pkg == "com.android.systemui") return true
        if (pkg == "com.android.settings" || pkg == "com.android.phone") return true
        if (pkg == "com.android.server.telecom" || pkg == "com.android.documentsui") return true
        if (pkg == "com.android.permissioncontroller") return true
        if (pkg == "com.google.android.permissioncontroller") return true
        if (pkg == "com.android.packageinstaller") return true
        if (pkg == "com.google.android.packageinstaller") return true
        if (pkg.startsWith("com.android.providers.")) return true
        if (pkg.contains("setupwizard")) return true
        return false
    }
}
