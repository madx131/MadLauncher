package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
