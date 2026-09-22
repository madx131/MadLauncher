package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockAutoHideTest {
    @Test
    fun intelligentMapsToAlwaysShow() {
        assertEquals(DockAutoHide.REGULAR, DockAutoHide.INTELLIGENT.forUi())
        assertFalse(DockAutoHide.INTELLIGENT.isAutoHide)
        assertFalse(DockAutoHide.REGULAR.isAutoHide)
        assertTrue(DockAutoHide.AUTOMATIC.isAutoHide)
        assertEquals(DockAutoHide.AUTOMATIC, DockAutoHide.AUTOMATIC.forUi())
    }

    @Test
    fun showOverAppsOnlyForSideDocks() {
        val left = EdgePanel(EdgeSide.LEFT, showOverApps = true)
        val top = EdgePanel(EdgeSide.TOP, showOverApps = true)
        assertTrue(left.showOverApps)
        assertTrue(top.side == EdgeSide.TOP)
        assertEquals(EdgeSide.LEFT, left.side)
    }
}
