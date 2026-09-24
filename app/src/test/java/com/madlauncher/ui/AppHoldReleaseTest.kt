package com.madlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppHoldReleaseTest {
    @Test
    fun menuNeedsStillHoldAndDelay() {
        assertFalse(openAppMenuOnHoldRelease(dragMoved = true, heldStillMs = 1_000L))
        assertFalse(openAppMenuOnHoldRelease(dragMoved = false, heldStillMs = 200L))
        assertTrue(openAppMenuOnHoldRelease(dragMoved = false, heldStillMs = IconMenuExtraHoldMs))
        assertTrue(openAppMenuOnHoldRelease(dragMoved = false, heldStillMs = 800L))
    }

    @Test
    fun iconDragSlopIsTighterThanStockTouchSlop() {
        assertEquals(4f, iconDragDetectSlopPx(8f), 0.01f)
        assertEquals(9f, iconDragDetectSlopPx(20f), 0.01f)
    }

    @Test
    fun wallpaperMenuWaitsLongerThanIconPickup() {
        assertEquals(550L, pageContextMenuTimeoutMs(400L))
        assertEquals(600L, pageContextMenuTimeoutMs(600L))
    }

    @Test
    fun wallpaperMenuSlopIsLooserThanStockTouchSlop() {
        assertEquals(20f, pageContextMenuSlopPx(8f), 0.01f)
    }
}
