package com.madlauncher.action

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** One public `!trigger` site search. Queries open [url] directly. */
data class BangEntry(
    val trigger: String,
    val name: String,
    val url: String,
    val rank: Int = 0,
)

/**
 * The public `!` shortcut list (trigger, site name, search URL).
 * Loaded from `assets/bangs.tsv`. Lookups stay on device.
 */
object BangCatalog {
    @Volatile
    private var byTrigger: Map<String, BangEntry> = emptyMap()

    @Volatile
    private var all: List<BangEntry> = emptyList()

    val count: Int get() = all.size

    fun load(text: String) {
        val parsed = parse(text)
        all = parsed
        byTrigger = parsed.associateBy { it.trigger }
    }

    fun parse(text: String): List<BangEntry> {
        if (text.isBlank()) return emptyList()
        return text.lineSequence().mapNotNull { line ->
            val raw = line.trim()
            if (raw.isEmpty() || raw.startsWith("#")) return@mapNotNull null
            val parts = raw.split('\t', limit = 4)
            if (parts.size < 4) return@mapNotNull null
            val trigger = parts[0].trim().lowercase()
            if (trigger.isEmpty() || trigger.any { it.isWhitespace() }) return@mapNotNull null
            BangEntry(
                trigger = trigger,
                name = parts[2].trim().ifBlank { trigger },
                url = parts[3].trim(),
                rank = parts[1].toIntOrNull() ?: 0,
            )
        }.toList()
    }

    fun exact(trigger: String): BangEntry? = byTrigger[trigger.trim().lowercase()]

    /** Blank [filter] returns the full list. Otherwise trigger prefix, then site name. */
    fun search(filter: String, limit: Int = 0): List<BangEntry> {
        val q = filter.trim().removePrefix("!").trim().lowercase()
        val cap = if (limit <= 0) Int.MAX_VALUE else limit
        if (q.isEmpty()) return if (all.size <= cap) all else all.take(cap)
        val prefix = ArrayList<BangEntry>()
        val names = ArrayList<BangEntry>()
        for (entry in all) {
            when {
                entry.trigger.startsWith(q) -> if (prefix.size < cap) prefix.add(entry)
                q.length >= 2 && entry.name.contains(q, ignoreCase = true) ->
                    if (names.size < cap) names.add(entry)
            }
            if (prefix.size >= cap) break
        }
        if (prefix.size >= cap) return prefix
        return (prefix + names).take(cap)
    }

    /** `!g cats` when [g] is in the list. Exact trigger only, and the query must be present. */
    fun match(raw: String): Pair<BangEntry, String>? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("!") || trimmed.startsWith("!+")) return null
        val rest = trimmed.removePrefix("!").trim()
        val space = rest.indexOfFirst { it.isWhitespace() }
        if (space <= 0) return null
        val trigger = rest.substring(0, space)
        val query = rest.substring(space).trim()
        if (query.isEmpty()) return null
        val entry = exact(trigger) ?: return null
        return entry to query
    }

    fun apply(entry: BangEntry, query: String): String {
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        var url = entry.url
        url = if (url.contains("{{{s}}}")) url.replace("{{{s}}}", encoded) else CustomBang.applyUrl(url, query)
        if (url.startsWith("http://")) url = "https://" + url.removePrefix("http://")
        return url
    }
}
