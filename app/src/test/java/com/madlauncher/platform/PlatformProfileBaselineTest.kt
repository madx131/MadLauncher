package com.madlauncher.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class PlatformProfileBaselineTest {
    @Test
    fun badgesAreWorkingOnOemSkinsOncePermissionIsTheOnlyGate() {
        listOf(
            OemFamily.XIAOMI_HYPEROS,
            OemFamily.MIUI,
            OemFamily.OPPO_COLOROS,
            OemFamily.VIVO,
            OemFamily.HUAWEI,
            OemFamily.STOCK,
            OemFamily.SAMSUNG,
        ).forEach { family ->
            val caps = PlatformProfile.baseline(family)
            assertEquals(
                "Badges should not stay Limited just because of OEM family $family",
                FeatureStatus.WORKING,
                caps[PlatformFeature.NOTIF_BADGES],
            )
            assertEquals(FeatureStatus.UNAVAILABLE, caps[PlatformFeature.ACTIONBOX_SOCIAL_DM])
            assertEquals(FeatureStatus.UNAVAILABLE, caps[PlatformFeature.ACTIONBOX_TRUTH_POST])
        }
    }
}
