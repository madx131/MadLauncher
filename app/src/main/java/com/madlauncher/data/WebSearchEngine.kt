package com.madlauncher.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
            DUCKDUCKGO -> "https://duckduckgo.com/?q=$q"
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
