package com.madlauncher.action

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSpaceCapabilityTest {
    @Test
    fun geoOrMapsCategoryCountsAsNav() {
        assertTrue(AppSpaceCapability.isReliableNav("com.citymapper.app", handlesGeo = true, handlesGoogleNav = false))
        assertTrue(
            AppSpaceCapability.isReliableNav(
                "com.here.app.maps",
                handlesGeo = false,
                handlesGoogleNav = false,
                appCategory = ApplicationInfo.CATEGORY_MAPS,
            ),
        )
        assertTrue(AppSpaceCapability.isReliableNav("com.waze", handlesGeo = false, handlesGoogleNav = false))
        assertFalse(
            AppSpaceCapability.isReliableNav(
                "com.android.vending",
                handlesGeo = false,
                handlesGoogleNav = false,
            ),
        )
    }

    @Test
    fun curatedUrlCountsAsSearchButSearchableInfoAloneDoesNot() {
        assertTrue(
            AppSpaceCapability.isReliableSearch(
                curatedUrl = "market://search?q=q",
                assisted = false,
                hasSearchableInfo = false,
                commonAddon = SpaceAddon.SEARCH,
            ),
        )
        assertFalse(
            AppSpaceCapability.isReliableSearch(
                curatedUrl = "",
                assisted = false,
                hasSearchableInfo = true,
                commonAddon = null,
            ),
        )
        assertTrue(
            AppSpaceCapability.isReliableSearch(
                curatedUrl = "https://instagram.com/explore",
                assisted = true,
                hasSearchableInfo = false,
                commonAddon = SpaceAddon.POST,
            ),
        )
        assertFalse(
            AppSpaceCapability.isReliableSearch(
                curatedUrl = "",
                assisted = false,
                hasSearchableInfo = true,
                commonAddon = SpaceAddon.NAV,
            ),
        )
    }

    @Test
    fun requestFileBaseIsPackageSafe() {
        val device = com.madlauncher.platform.DeviceOsInfo.Snapshot(
            manufacturer = "Xiaomi",
            brand = "POCO",
            model = "F5 Pro",
            device = "mondrian",
            product = "mondrian",
            androidRelease = "15",
            androidSdk = 35,
            nativeOsName = "HyperOS",
            nativeOsVersion = "2",
            buildDisplay = "test",
            buildFingerprint = "test",
            oemFamily = com.madlauncher.platform.OemFamily.XIAOMI_HYPEROS,
        )
        val req = SearchableAppRequest(
            appLabel = "Waze",
            packageName = "com.waze",
            appVersion = "5.0",
            appCategory = "MAPS",
            handlesGeo = true,
            handlesGoogleNav = false,
            hasSearchableInfo = false,
            curatedSearchUrl = false,
            assistedSearch = false,
            commonShortcutId = "waze",
            guessedAddon = "NAV",
            madVersion = "0.9.116.77",
            device = device,
            probedAtMillis = 1_700_000_000_000L,
        )
        assertEquals("searchable-com.waze-1700000000000", req.fileBase())
        assertTrue(req.toPlainText().contains("Package: com.waze"))
        assertTrue(req.toJson().contains("\"packageName\": \"com.waze\""))
    }
}
