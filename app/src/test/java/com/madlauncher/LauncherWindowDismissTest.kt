package com.madlauncher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherWindowDismissTest {
    @Test
    fun usageAccessAndOverlayKeepWindows() {
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows("android.settings.USAGE_ACCESS_SETTINGS"),
        )
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows("android.settings.action.MANAGE_OVERLAY_PERMISSION"),
        )
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows("android.content.pm.action.REQUEST_PERMISSIONS"),
        )
    }

    @Test
    fun documentAndWidgetPickersKeepWindows() {
        assertTrue(LauncherWindowDismiss.keepsLauncherWindows("android.intent.action.GET_CONTENT"))
        assertTrue(LauncherWindowDismiss.keepsLauncherWindows("android.intent.action.OPEN_DOCUMENT"))
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows("android.appwidget.action.APPWIDGET_BIND"),
        )
    }

    @Test
    fun xiaomiAutostartKeepsWindows() {
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows(
                action = "",
                pkg = "com.miui.securitycenter",
                cls = "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        )
    }

    @Test
    fun roleAndXiaomiPermissionDialogsKeepWindows() {
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows("android.app.role.action.REQUEST_ROLE"),
        )
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows(
                action = "",
                pkg = "com.lbe.security.miui",
            ),
        )
    }

    @Test
    fun batteryOptimizationKeepsWindows() {
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows(
                "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            ),
        )
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows(
                "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS",
            ),
        )
        assertTrue(
            LauncherWindowDismiss.keepsLauncherWindows(
                action = "",
                pkg = "com.miui.powerkeeper",
            ),
        )
    }

    @Test
    fun launchingAnAppDoesNotKeepWindows() {
        assertFalse(
            LauncherWindowDismiss.keepsLauncherWindows(
                action = "android.intent.action.MAIN",
                pkg = "com.whatsapp",
            ),
        )
    }
}
