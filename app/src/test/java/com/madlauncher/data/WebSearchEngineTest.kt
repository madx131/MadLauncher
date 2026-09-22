package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WebSearchEngineTest {
    @Test
    fun defaultIsDuckDuckGo() {
        assertEquals(WebSearchEngine.DUCKDUCKGO, AppearanceSettings().webSearchEngine)
    }

    @Test
    fun buildsEngineUrls() {
        assertEquals("https://duckduckgo.com/?q=hello", WebSearchEngine.DUCKDUCKGO.searchUrl("hello"))
        assertEquals("https://www.google.com/search?q=hello", WebSearchEngine.GOOGLE.searchUrl("hello"))
        assertEquals("https://www.bing.com/search?q=hello", WebSearchEngine.BING.searchUrl("hello"))
        assertEquals("https://search.yahoo.com/search?p=hello", WebSearchEngine.YAHOO.searchUrl("hello"))
        assertEquals("https://www.startpage.com/sp/search?query=hello", WebSearchEngine.STARTPAGE.searchUrl("hello"))
        assertEquals("https://www.ecosia.org/search?q=hello", WebSearchEngine.ECOSIA.searchUrl("hello"))
        assertEquals("https://search.brave.com/search?q=hello", WebSearchEngine.BRAVE.searchUrl("hello"))
        assertEquals("https://yandex.com/search/?text=hello", WebSearchEngine.YANDEX.searchUrl("hello"))
    }

    @Test
    fun encodesQuery() {
        assertEquals("https://duckduckgo.com/?q=hello+world", WebSearchEngine.DUCKDUCKGO.searchUrl("hello world"))
        assertEquals(
            "https://www.google.com/search?q=C%2B%2B",
            WebSearchEngine.GOOGLE.searchUrl("C++"),
        )
    }
}
