package com.madlauncher.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class DockFitTest {
    @Test
    fun syncedDockKeepsTheLabelInside() {
        val label = 16f
        val icon = 88f
        val height = dockHeightForSyncedIcon(icon, label)
        assertTrue(icon + dockLabelBandDp(label) <= height + 0.01f)
        assertTrue(maxDockIconDp(height, label) + 0.01f >= icon)
    }

    @Test
    fun syncedEdgeStripKeepsTheIconInside() {
        val icon = 64f
        val thickness = edgeThicknessForSyncedIcon(icon)
        assertTrue(edgeIconDp(icon, thickness) + 0.01f >= icon)
    }
}
