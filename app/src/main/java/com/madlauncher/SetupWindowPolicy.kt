package com.madlauncher

/**
 * First-run Permissions stays open while the user is bounced through Settings
 * / permission dialogs. Home and Recents would otherwise close it after 1–2 taps.
 */
object SetupWindowPolicy {
    fun keepOpen(
        setupOpen: Boolean,
        setupDismissed: Boolean,
        requiredDone: Boolean,
        independentActionBox: Boolean,
    ): Boolean = setupOpen && !setupDismissed && !requiredDone && !independentActionBox
}
