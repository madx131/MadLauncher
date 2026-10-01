package com.madlauncher.action

import com.madlauncher.data.LaunchableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchableAppsKeywordsTest {
    @Test
    fun deriveKeywordsIncludesPrefixes() {
        val kws = SearchableApps.deriveKeywords("YouTube Music", "com.google.android.apps.youtube.music")
        assertTrue(kws.any { it.startsWith("you") })
        assertTrue(kws.contains("ym") || kws.any { it == "youtubemusic" || it.startsWith("yt") })
    }

    @Test
    fun catalogIncludesCalendarPackages() {
        val cal = SearchableApps.catalog.first { it.appName == "Calendar" }
        assertTrue(cal.packages.contains("com.google.android.calendar"))
        assertTrue(cal.packages.contains("com.android.calendar"))
        assertTrue(cal.packages.contains("com.samsung.android.calendar"))
    }

    @Test
    fun matchingUsesPrefixNotContains() {
        val yt = resolved(SearchableApps.catalog.first { it.appName == "YouTube" })
        val cal = resolved(SearchableApps.catalog.first { it.appName == "Calendar" })
        val installed = listOf(yt, cal)

        val ytHits = SearchableApps.matching("!yt", installed)
        assertEquals(1, ytHits.size)
        assertEquals("YouTube", ytHits.first().def.appName)

        val calHits = SearchableApps.matching("!cal", installed)
        assertEquals(1, calHits.size)
        assertEquals("Calendar", calHits.first().def.appName)

        val prefixHits = SearchableApps.matching("!calend", installed)
        assertEquals(1, prefixHits.size)
        assertEquals("Calendar", prefixHits.first().def.appName)
    }

    private fun resolved(def: SearchableAppDef): ResolvedSearchApp {
        val pkg = def.packages.first()
        return ResolvedSearchApp(
            def,
            LaunchableApp(def.displayName, pkg, "$pkg.Main", icon = null),
        )
    }
}
