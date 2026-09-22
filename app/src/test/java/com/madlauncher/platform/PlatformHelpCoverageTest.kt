package com.madlauncher.platform

import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformHelpCoverageTest {
    @Test
    fun featureTitlesAndExplanationsCoverAllEntries() {
        PlatformFeature.entries.forEach { feature ->
            assertTrue(PlatformHelp.featureTitle(feature).isNotBlank())
            assertTrue(
                PlatformHelp.explanation(feature, OemFamily.STOCK, FeatureStatus.DEGRADED).isNotBlank(),
            )
            assertTrue(
                PlatformHelp.explanation(feature, OemFamily.STOCK, FeatureStatus.NOT_INSTALLED).isNotBlank(),
            )
        }
    }
}
