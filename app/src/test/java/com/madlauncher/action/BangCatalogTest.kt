package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BangCatalogTest {
    private val sample = """
        g	100	Google	https://www.google.com/search?q={{{s}}}
        yt	90	YouTube	https://www.youtube.com/results?search_query={{{s}}}
        w	80	Wikipedia	https://en.wikipedia.org/wiki/Special:Search?search={{{s}}}
    """.trimIndent()

    @Test
    fun parsesTriggerNameAndUrl() {
        BangCatalog.load(sample)
        assertEquals(3, BangCatalog.count)
        assertEquals("YouTube", BangCatalog.exact("yt")?.name)
    }

    @Test
    fun matchNeedsAnExactTriggerAndAQuery() {
        BangCatalog.load(sample)
        assertNull(BangCatalog.match("!yt"))
        assertNull(BangCatalog.match("!you cats"))
        val hit = BangCatalog.match("!yt cats")
        assertEquals("yt", hit?.first?.trigger)
        assertEquals("cats", hit?.second)
        assertEquals(
            "https://www.youtube.com/results?search_query=cats",
            BangCatalog.apply(hit!!.first, hit.second),
        )
    }

    @Test
    fun bangsAreWebsiteSearches() {
        BangCatalog.load(
            """
            maps	10	Google Maps	https://google.com/maps?q={{{s}}}
            p	9	Pinterest	https://www.pinterest.com/search/pins/?q={{{s}}}
            play	8	Google Play	https://play.google.com/store/search?q={{{s}}}
            tr	7	Google Translate	https://translate.google.com/#auto/en/{{{s}}}
            pin	6	Pinterest	https://pinterest.com/search/pins/?q={{{s}}}
            """.trimIndent(),
        )
        assertEquals("maps", BangCatalog.match("!maps paris")?.first?.trigger)
        assertEquals("p", BangCatalog.match("!p widgets")?.first?.trigger)
        assertEquals("play", BangCatalog.match("!play widgets")?.first?.trigger)
        assertEquals("tr", BangCatalog.match("!tr hello")?.first?.trigger)
        assertEquals("pin", BangCatalog.match("!pin cats")?.first?.trigger)
        val play = SearchableApps.parseWebBang("!p widgets", emptyList())
        assertEquals("Play Store", play?.first?.displayName)
        assertTrue(play!!.first.keywords.containsAll(listOf("p", "play")))
        assertEquals("Play Store", SearchableApps.parseWebBang("!play widgets", emptyList())?.first?.displayName)
    }

    @Test
    fun translateSiteUsesLanguages() {
        assertEquals(
            "https://translate.google.com/?sl=auto&tl=en&text=hello&op=translate",
            BangCatalog.translateSiteUrl("hello", null, null),
        )
        assertEquals(
            "https://translate.google.com/?sl=en&tl=es&text=hello&op=translate",
            BangCatalog.translateSiteUrl("hello", "en", "es"),
        )
    }
}
