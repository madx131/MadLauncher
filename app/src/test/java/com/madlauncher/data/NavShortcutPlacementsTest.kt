package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavShortcutPlacementsTest {
    private val messages = NavShortcutSlot(NavShortcutKind.NOTIFICATIONS, NavShortcutPlacement.NAV_LEFT)
    private val qs = NavShortcutSlot(NavShortcutKind.QUICK_SETTINGS, NavShortcutPlacement.NAV_RIGHT)
    private val system = NavShortcutSlot(NavShortcutKind.SYSTEM_APPS, NavShortcutPlacement.SHADE)

    @Test
    fun enableThirdShortcutTakesShadeSeat() {
        val base = listOf(messages, qs)
        val next = NavShortcutPlacements.setEnabled(base, NavShortcutKind.SYSTEM_APPS, true)
        assertEquals(NavShortcutPlacement.SHADE, NavShortcutPlacements.placementOf(next, NavShortcutKind.SYSTEM_APPS))
    }

    @Test
    fun cycleChairSwapsNavOccupants() {
        val base = listOf(messages, qs)
        val next = NavShortcutPlacements.cycleChair(base, NavShortcutKind.NOTIFICATIONS)
        assertEquals(NavShortcutPlacement.NAV_RIGHT, NavShortcutPlacements.placementOf(next, NavShortcutKind.NOTIFICATIONS))
        assertEquals(NavShortcutPlacement.NAV_LEFT, NavShortcutPlacements.placementOf(next, NavShortcutKind.QUICK_SETTINGS))
    }

    @Test
    fun cycleFromShadeTurnsOff() {
        val base = listOf(messages, qs, system)
        val off = NavShortcutPlacements.cycleChair(base, NavShortcutKind.SYSTEM_APPS)
        assertFalse(NavShortcutPlacements.placementOf(off, NavShortcutKind.SYSTEM_APPS).isActive)
    }

    @Test
    fun normalizeKeepsAtMostTwoNavSeats() {
        val messy = listOf(
            NavShortcutSlot(NavShortcutKind.NOTIFICATIONS, NavShortcutPlacement.NAV_LEFT),
            NavShortcutSlot(NavShortcutKind.QUICK_SETTINGS, NavShortcutPlacement.NAV_LEFT),
            NavShortcutSlot(NavShortcutKind.SYSTEM_APPS, NavShortcutPlacement.NAV_RIGHT),
        )
        val fixed = NavShortcutPlacements.normalize(messy)
        assertTrue(NavShortcutPlacements.navSlots(fixed).size <= 2)
    }
}
