package com.madlauncher

import android.content.Intent
import android.os.SystemClock

/**
 * Recents / Home leave should close MAD panels. Starting a grant / picker / OEM
 * settings screen must not — Setup and Settings stay until the user comes back.
 */
object LauncherWindowDismiss {
    private const val PRESERVE_MS = 20_000L

    @Volatile
    private var preserveUntilMs = 0L

    fun preserve(ms: Long) {
        preserveUntilMs = SystemClock.uptimeMillis() + ms
    }

    fun noteIfKeepWindows(intent: Intent?) {
        if (intent != null && keepsLauncherWindows(intent)) {
            preserveUntilMs = SystemClock.uptimeMillis() + PRESERVE_MS
        }
    }

    fun shouldPreserve(): Boolean = SystemClock.uptimeMillis() < preserveUntilMs

    fun keepsLauncherWindows(intent: Intent): Boolean = keepsLauncherWindows(
        action = intent.action.orEmpty(),
        pkg = intent.component?.packageName ?: intent.`package`.orEmpty(),
        cls = intent.component?.className.orEmpty(),
    )

    fun keepsLauncherWindows(action: String, pkg: String = "", cls: String = ""): Boolean {
        if (action.startsWith("android.settings.")) return true
        if (action == "android.speech.action.RECOGNIZE_SPEECH") return true
        if (action == "android.content.pm.action.REQUEST_PERMISSIONS") return true
        if (action == "android.app.role.action.REQUEST_ROLE") return true
        if (action.contains("REQUEST_ROLE", ignoreCase = true)) return true
        if (action.contains("APP_PERM", ignoreCase = true)) return true
        when (action) {
            Intent.ACTION_OPEN_DOCUMENT,
            Intent.ACTION_GET_CONTENT,
            Intent.ACTION_CREATE_DOCUMENT,
            Intent.ACTION_OPEN_DOCUMENT_TREE,
            Intent.ACTION_PICK,
            "android.service.wallpaper.CROP_AND_SET_WALLPAPER",
            -> return true
        }
        if (action.contains("APPWIDGET", ignoreCase = true)) return true
        if (action.contains("CONFIRM_PIN", ignoreCase = true)) return true

        if (pkg == "com.android.settings" ||
            pkg == "com.android.systemui" ||
            pkg.contains("permissioncontroller") ||
            pkg.contains("packageinstaller") ||
            pkg.contains("securitycenter") ||
            pkg.contains("permcenter") ||
            pkg.contains("lbe.security") ||
            pkg == "com.miui.securitycenter" ||
            pkg == "com.miui.powerkeeper" ||
            pkg == "com.samsung.android.lool"
        ) {
            return true
        }
        return cls.contains("ConfirmPin") ||
            cls.contains("AutoStart") ||
            cls.contains("permcenter")
    }
}
