package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MadShadeWindowsTest {
    @Test
    fun stockModeHidesSystemAppsActions() {
        assertFalse(MadShadeWindows.gestureActions(experimental = false).contains(GestureActionId.SYSTEM_APPS))
        assertTrue(MadShadeWindows.gestureActions(experimental = true).contains(GestureActionId.SYSTEM_APPS))
        assertFalse(MadShadeWindows.navKinds(experimental = false).contains(NavShortcutKind.SYSTEM_APPS))
        assertTrue(MadShadeWindows.navKinds(experimental = true).contains(NavShortcutKind.SYSTEM_APPS))
    }

    @Test
    fun sanitizeRemapsSystemAppsWhenExperimentalOff() {
        val settings = AppearanceSettings(
            experimentalMadWindows = false,
            gestures = mapOf(GestureId.SWIPE_DOWN to GestureActionId.SYSTEM_APPS),
            navShortcutSlots = listOf(
                NavShortcutSlot(NavShortcutKind.NOTIFICATIONS, NavShortcutPlacement.NAV_LEFT),
                NavShortcutSlot(NavShortcutKind.SYSTEM_APPS, NavShortcutPlacement.NAV_RIGHT),
            ),
        )
        val next = MadShadeWindows.sanitize(settings)
        assertEquals(GestureActionId.NOTIFICATIONS, next.gestures[GestureId.SWIPE_DOWN])
        assertFalse(next.navShortcutSlots.any { it.kind == NavShortcutKind.SYSTEM_APPS })
    }

    @Test
    fun sanitizeKeepsSystemAppsWhenExperimentalOn() {
        val settings = AppearanceSettings(
            experimentalMadWindows = true,
            gestures = mapOf(GestureId.SWIPE_DOWN to GestureActionId.SYSTEM_APPS),
        )
        assertEquals(GestureActionId.SYSTEM_APPS, MadShadeWindows.sanitize(settings).gestures[GestureId.SWIPE_DOWN])
    }
}
