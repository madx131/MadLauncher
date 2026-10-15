package com.madlauncher.action

/**
 * Maps intents: [searchUris] looks up a place (Space), [navigateUris] starts a trip (Enter).
 */
object NavIntents {
    /** @deprecated Use [navigateUris] — kept so older call sites keep compiling. */
    fun uris(packageName: String, destination: String): List<String> = navigateUris(packageName, destination)

    fun searchUris(packageName: String, query: String): List<String> {
        val dest = encodeQuery(query)
        if (dest.isEmpty()) return emptyList()
        val pkg = packageName.lowercase()
        return buildList {
            add("geo:0,0?q=$dest")
            add("https://www.google.com/maps/search/?api=1&query=$dest")
            if (pkg.contains("waze")) {
                add("https://waze.com/ul?q=$dest")
            }
        }.distinct()
    }

    fun navigateUris(packageName: String, destination: String): List<String> {
        val dest = encodeQuery(destination)
        if (dest.isEmpty()) return emptyList()
        val pkg = packageName.lowercase()
        return buildList {
            add("google.navigation:q=$dest")
            if (pkg.contains("waze")) {
                add("https://waze.com/ul?q=$dest&navigate=yes")
            }
            if (pkg.contains("maps") || pkg.contains("navigation")) {
                add("https://www.google.com/maps/dir/?api=1&destination=$dest")
            }
            add("geo:0,0?q=$dest")
        }.distinct()
    }

    internal fun encodeQuery(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        return java.net.URLEncoder.encode(trimmed, Charsets.UTF_8.name()).replace("+", "%20")
    }
}
