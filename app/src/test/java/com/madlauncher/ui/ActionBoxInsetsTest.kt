package com.madlauncher.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionBoxInsetsTest {
    @Test
    fun keyboardOpen_sitsAboveIme_notDock() {
        val lift = actionBoxLiftDp(ime = 280.dp, nav = 48.dp, closedDock = 96.dp)
        assertEquals(288.dp, lift)
    }

    @Test
    fun keyboardClosed_usesDockGap() {
        val lift = actionBoxLiftDp(ime = 0.dp, nav = 24.dp, closedDock = 96.dp)
        assertEquals(96.dp, lift)
    }

    @Test
    fun keyboardStarting_doesNotDropBelowDock() {
        val lift = actionBoxLiftDp(ime = 40.dp, nav = 48.dp, closedDock = 96.dp)
        assertEquals(96.dp, lift)
    }

    @Test
    fun keyboardTallerThanDock_followsIme() {
        val lift = actionBoxLiftDp(ime = 120.dp, nav = 48.dp, closedDock = 96.dp)
        assertEquals(128.dp, lift)
    }

    @Test
    fun animationIgnoresASnappedFinalInset() {
        assertEquals(40, resolveImeBottomPx(previous = 40, reported = 900, accept = false))
        assertEquals(120, resolveImeBottomPx(previous = 40, reported = 120, accept = true))
    }
}
