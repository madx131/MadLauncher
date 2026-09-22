package com.madlauncher.notif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemShadeControllerTest {
    @Test
    fun homeDoesNotMapToNotificationShade() {
        assertEquals(
            SystemShadeController.StockPanelKind.NONE,
            SystemShadeController.stockPanelKind("home"),
        )
    }

    @Test
    fun knownPanelsMap() {
        assertEquals(
            SystemShadeController.StockPanelKind.NOTIFICATIONS,
            SystemShadeController.stockPanelKind("notifications"),
        )
        assertEquals(
            SystemShadeController.StockPanelKind.QUICK_SETTINGS,
            SystemShadeController.stockPanelKind("quicksettings"),
        )
    }

    @Test
    fun recentsCandidatesPreferConfigThenHyperOsActivity() {
        val next = SystemShadeController.recentsActivityCandidates(
            configFlattened = "com.mi.android.globallauncher/com.miui.home.recents.RecentsActivity",
            quickstepPackages = listOf("com.mi.android.globallauncher"),
        )
        assertEquals(
            "com.mi.android.globallauncher/com.miui.home.recents.RecentsActivity",
            next.first(),
        )
        assertTrue(next.contains("com.mi.android.globallauncher/com.miui.home.recents.RecentsActivity"))
    }

    @Test
    fun recentsCandidatesIncludeMiuiHomeWithoutConfig() {
        val next = SystemShadeController.recentsActivityCandidates(
            configFlattened = null,
            quickstepPackages = listOf("com.mi.android.globallauncher"),
        )
        assertTrue(next.contains("com.mi.android.globallauncher/com.miui.home.recents.RecentsActivity"))
    }
}
