package com.madlauncher.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchableAppTest {
    @Test
    fun madSettingsMatchesInternalAndHomeActivity() {
        val self = "com.madlauncher"
        assertTrue(LaunchableApp.isMadSettings(self, LaunchableApp.SETTINGS_ACTIVITY, self))
        assertTrue(LaunchableApp.isMadSettings(self, "com.madlauncher.MainActivity", self))
        assertFalse(LaunchableApp.isMadSettings(self, "com.madlauncher.ActionBoxActivity", self))
        assertFalse(LaunchableApp.isMadSettings("com.android.settings", LaunchableApp.SETTINGS_ACTIVITY, self))
    }
}
