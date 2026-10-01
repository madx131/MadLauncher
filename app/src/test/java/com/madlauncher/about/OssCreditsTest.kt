package com.madlauncher.about

import org.junit.Assert.assertTrue
import org.junit.Test

class OssCreditsTest {
    @Test
    fun everyModuleLinksToAGitHubPage() {
        assertTrue(OssCredits.all.isNotEmpty())
        OssCredits.all.forEach { credit ->
            assertTrue(credit.name, credit.authorUrl.startsWith("https://github.com/"))
            assertTrue(credit.name, credit.projectUrl.startsWith("https://github.com/"))
            assertTrue(credit.author.isNotBlank())
        }
    }
}
