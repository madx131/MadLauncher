package com.madlauncher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupWindowPolicyTest {
    @Test
    fun firstRunSetupSurvivesHomeWhileIncomplete() {
        assertTrue(
            SetupWindowPolicy.keepOpen(
                setupOpen = true,
                setupDismissed = false,
                requiredDone = false,
                independentActionBox = false,
            ),
        )
    }

    @Test
    fun skipOrFinishedAllowsClose() {
        assertFalse(
            SetupWindowPolicy.keepOpen(
                setupOpen = true,
                setupDismissed = true,
                requiredDone = false,
                independentActionBox = false,
            ),
        )
        assertFalse(
            SetupWindowPolicy.keepOpen(
                setupOpen = true,
                setupDismissed = false,
                requiredDone = true,
                independentActionBox = false,
            ),
        )
    }

    @Test
    fun alreadyClosedOrActionBoxDoesNotStick() {
        assertFalse(
            SetupWindowPolicy.keepOpen(
                setupOpen = false,
                setupDismissed = false,
                requiredDone = false,
                independentActionBox = false,
            ),
        )
        assertFalse(
            SetupWindowPolicy.keepOpen(
                setupOpen = true,
                setupDismissed = false,
                requiredDone = false,
                independentActionBox = true,
            ),
        )
    }
}
