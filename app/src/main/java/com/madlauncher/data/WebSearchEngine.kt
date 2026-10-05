package com.madlauncher.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** App id DuckDuckGo expects on search and instant-answer requests (`t=`). */
object DuckDuckGoApp {
    const val ID = "madactionbox"

    fun tag(url: String): String {
        val host = url.substringAfter("://", "")
            .substringBefore("/")
            .substringBefore("?")
            .lowercase()
        val duck = host == "duckduckgo.com" || host.endsWith(".duckduckgo.com")
        if (!duck || Regex("""[?&]t=""").containsMatchIn(url)) return url
        val sep = if ('?' in url) "&" else "?"
        return "$url${sep}t=$ID"
    }
}

enum class WebSearchEngine {
    DUCKDUCKGO,
    GOOGLE,
    BING,
    YAHOO,
    STARTPAGE,
    ECOSIA,
    BRAVE,
    YANDEX,
    ;

    fun searchUrl(query: String): String {
        val q = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        return when (this) {
            DUCKDUCKGO -> DuckDuckGoApp.tag("https://duckduckgo.com/?q=$q")
            GOOGLE -> "https://www.google.com/search?q=$q"
            BING -> "https://www.bing.com/search?q=$q"
            YAHOO -> "https://search.yahoo.com/search?p=$q"
            STARTPAGE -> "https://www.startpage.com/sp/search?query=$q"
            ECOSIA -> "https://www.ecosia.org/search?q=$q"
            BRAVE -> "https://search.brave.com/search?q=$q"
            YANDEX -> "https://yandex.com/search/?text=$q"
        }
    }
}
