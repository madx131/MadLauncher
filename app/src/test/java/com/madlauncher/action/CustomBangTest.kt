package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomBangTest {
    @Test
    fun plusSplitsAppAndQuery() {
        assertNull(CustomBang.parsePlus("!yt cats"))
        assertEquals(PlusQuery("YouTube", "cats"), CustomBang.parsePlus("!+YouTube cats"))
        assertEquals(PlusQuery("urban", ""), CustomBang.parsePlus("!+urban"))
        assertEquals(PlusQuery("", ""), CustomBang.parsePlus("!+"))
    }

    @Test
    fun savedWordNeedsAQuery() {
        val bangs = listOf(CustomBang(id = "urban", phrase = "urban", searchUrl = "https://www.urbandictionary.com/define.php?term="))
        assertNull(CustomBang.match("!urban", bangs))
        val hit = CustomBang.match("!urban yeet", bangs)
        assertEquals("urban", hit?.first?.phrase)
        assertEquals("yeet", hit?.second)
    }

    @Test
    fun pastedEqualsLinkKeepsTheQuery() {
        val url = CustomBang.applyUrl("https://example.com/search?search=", "new york")
        assertEquals("https://example.com/search?search=new+york", url)
    }

    @Test
    fun braceTemplateReplacesQuery() {
        assertEquals(
            "https://example.com/find/paris",
            CustomBang.applyUrl("https://example.com/find/{q}", "paris"),
        )
    }

    @Test
    fun wordIsOneToken() {
        assertEquals("urban", CustomBang.normalizePhrase("!urban"))
        assertNull(CustomBang.normalizePhrase("two words"))
    }
}
