package com.madlauncher.platform

import com.madlauncher.data.GrantState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureStatusTest {
    @Test
    fun missingAppsAreNotIssues() {
        assertFalse(FeatureStatus.NOT_INSTALLED.isIssue)
        assertFalse(FeatureStatus.WORKING.isIssue)
        assertTrue(FeatureStatus.UNAVAILABLE.isIssue)
        assertTrue(FeatureStatus.DEGRADED.isIssue)
        assertEquals("Not installed", PlatformHelp.statusLabel(FeatureStatus.NOT_INSTALLED))
        assertEquals("Not working", PlatformHelp.statusLabel(FeatureStatus.UNAVAILABLE))
    }

    @Test
    fun compatReportDoesNotTreatMissingSocialAppsAsBroken() {
        val report = CompatibilityReport(
            device = DeviceOsInfo.Snapshot(
                manufacturer = "Google",
                brand = "google",
                model = "sdk_gphone64_x86_64",
                device = "emu64xa",
                product = "sdk_gphone64_x86_64",
                androidRelease = "16",
                androidSdk = 36,
                nativeOsName = "MAD OS",
                nativeOsVersion = "16",
                buildDisplay = "test",
                buildFingerprint = "test",
                oemFamily = OemFamily.STOCK,
            ),
            madVersion = "0.9.101.4",
            probedAtMillis = 1L,
            features = mapOf(
                PlatformFeature.ACTIONBOX_SOCIAL_POST to FeatureStatus.NOT_INSTALLED,
                PlatformFeature.ACTIONBOX_META_SOCIAL to FeatureStatus.NOT_INSTALLED,
                PlatformFeature.NOTIF_BADGES to FeatureStatus.WORKING,
            ),
            grants = GrantState(),
        )
        assertTrue(report.limitedFeatures.isEmpty())
        assertEquals("All probed features working", report.limitedSummary)
    }
}
