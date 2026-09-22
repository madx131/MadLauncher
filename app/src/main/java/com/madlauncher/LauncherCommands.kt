package com.madlauncher

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow

object LauncherCommands {
    val pendingOpen = MutableStateFlow<String?>(null)
    /** Monotonic pulse so repeated Home presses always reach the UI. */
    val homePulse = MutableStateFlow(0L)

    @Volatile
    private var suppressHomeUntilMs = 0L

    fun request(panel: String) {
        // Opening a nav-shortcut panel: ignore a brief HOME race from REORDER_TO_FRONT /
        // onNewIntent so homePressed does not immediately close the window we just opened.
        if (panel != "home") {
            suppressHomeUntilMs = SystemClock.uptimeMillis() + 1_500L
        } else if (isHomeSuppressed()) {
            return
        }
        // StateFlow skips equal values — bump through null so repeated bell/gear taps re-emit.
        if (pendingOpen.value == panel) {
            pendingOpen.value = null
        }
        pendingOpen.value = panel
        if (panel == "home") {
            goHome()
        }
    }

    /** Brief HOME ignore after launching an OTT call flow (keeps target app on screen). */
    fun suppressHomeBriefly(durationMs: Long = 3_500L) {
        suppressHomeUntilMs = SystemClock.uptimeMillis() + durationMs
    }

    fun isHomeSuppressed(): Boolean = SystemClock.uptimeMillis() < suppressHomeUntilMs

    @Volatile
    private var lastHandledHomePulse = 0L

    fun shouldHandleHomePulse(pulse: Long): Boolean {
        if (pulse == 0L || pulse == lastHandledHomePulse) return false
        if (isHomeSuppressed()) return false
        lastHandledHomePulse = pulse
        return true
    }

    fun goHome() {
        if (isHomeSuppressed()) return
        homePulse.value = System.nanoTime()
    }
}
