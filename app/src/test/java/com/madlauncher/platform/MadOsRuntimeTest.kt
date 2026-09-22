package com.madlauncher.platform

import com.madlauncher.data.GrantState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MadOsRuntimeTest {
    @Test
    fun note7AospIsMadOs() {
        assertTrue(MadOsRuntime.isKnownMadOsBuild("aosp_lavender", "lavender"))
    }

    @Test
    fun stockMiuiNote7IsNotMadOs() {
        assertFalse(MadOsRuntime.isKnownMadOsBuild("lavender", "lavender"))
    }

    @Test
    fun systemDefaultCoreGrantsDoNotNeedOverlayOrA11y() {
        val grants = GrantState(
            defaultHome = true,
            notificationListener = true,
            overlay = false,
            accessibility = false,
            contacts = true,
            postNotifications = true,
            battery = true,
            systemDefault = true,
        )
        assertTrue(grants.requiredDone)
    }

    @Test
    fun skipA11yNavOnlyWhenSomethingElseCanDraw() {
        assertFalse(
            MadOsRuntime.shouldSkipA11yNav(
                hasStatusBarService = false,
                romOwnsNav = false,
                overlayDenied = false,
            ),
        )
        assertTrue(
            MadOsRuntime.shouldSkipA11yNav(
                hasStatusBarService = true,
                romOwnsNav = false,
                overlayDenied = false,
            ),
        )
        assertTrue(
            MadOsRuntime.shouldSkipA11yNav(
                hasStatusBarService = false,
                romOwnsNav = true,
                overlayDenied = false,
            ),
        )
        assertFalse(
            MadOsRuntime.shouldSkipA11yNav(
                hasStatusBarService = true,
                romOwnsNav = false,
                overlayDenied = true,
            ),
        )
    }

    @Test
    fun quickstepPackagesStayEnabledForNavBar() {
        assertTrue(MadOsSetup.quickstepPackages.contains("com.google.android.apps.nexuslauncher"))
        assertTrue(MadOsSetup.quickstepPackages.contains("com.android.launcher3"))
    }

    @Test
    fun sideloadCoreGrantsDoNotNeedOverlayOrA11y() {
        val grants = GrantState(
            defaultHome = true,
            notificationListener = true,
            overlay = false,
            accessibility = false,
            contacts = true,
            postNotifications = true,
            battery = true,
            systemDefault = false,
        )
        assertTrue(grants.requiredDone)
    }
}
