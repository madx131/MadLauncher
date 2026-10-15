package com.madlauncher.action

/** In-app / https search for looking up posts (Space on these apps is still post). */
object SocialSearch {
    fun urls(packageName: String, query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val q = java.net.URLEncoder.encode(trimmed, Charsets.UTF_8.name()).replace("+", "%20")
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("twitter") || pkg == "com.x.android" || pkg.startsWith("com.x.android.") ->
                listOf(
                    "https://x.com/search?q=$q",
                    "https://twitter.com/search?q=$q",
                    "twitter://search?query=$q",
                )
            pkg.contains("instagram.barcelona") ->
                listOf("https://www.threads.net/search?q=$q")
            pkg.contains("instagram") ->
                listOf(
                    "https://www.instagram.com/explore/search/keyword/?q=$q",
                    "instagram://search?query=$q",
                )
            pkg.contains("facebook.katana") || pkg.contains("facebook.lite") ->
                listOf(
                    "https://www.facebook.com/search/top/?q=$q",
                    "fb://search",
                )
            pkg.contains("linkedin") ->
                listOf("https://www.linkedin.com/search/results/all/?keywords=$q")
            pkg.contains("reddit") ->
                listOf("https://www.reddit.com/search/?q=$q")
            pkg.contains("zhiliaoapp") || pkg.contains("musically") || pkg.contains("ugc.trill") ->
                listOf(
                    "https://www.tiktok.com/search?q=$q",
                    "snssdk1233://search?keyword=$q",
                )
            pkg.contains("snapchat") ->
                listOf(
                    "snapchat://search?query=$q",
                    "https://www.snapchat.com/explore",
                )
            pkg.contains("truthsocial") ->
                listOf("https://truthsocial.com/search?q=$q")
            pkg.contains("bsky") || pkg.contains("bluesky") ->
                listOf("https://bsky.app/search?q=$q")
            else -> emptyList()
        }
    }

    fun hasSearch(packageName: String): Boolean = urls(packageName, "q").isNotEmpty()
}
