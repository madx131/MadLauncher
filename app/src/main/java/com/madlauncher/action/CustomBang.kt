package com.madlauncher.action

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** `!+youtube cats` — [appWord] is `youtube`, [query] is `cats`. */
data class PlusQuery(val appWord: String, val query: String)

/** A user-made `!word` search: an installed app, or a pasted site search link. */
data class CustomBang(
    val id: String,
    val phrase: String,
    val packageName: String = "",
    val searchUrl: String = "",
) {
    val isLink: Boolean get() = searchUrl.isNotBlank()

    companion object {
        fun normalizePhrase(raw: String): String? {
            val word = raw.trim().removePrefix("!").removePrefix("+").trim().lowercase()
            if (word.isEmpty() || word.any { it.isWhitespace() }) return null
            if (word.any { it == '!' || it == '+' || it == ':' }) return null
            return word
        }

        /** Turn a pasted `search=` / `q=` / `{q}` link into a URL for [query]. */
        fun applyUrl(template: String, query: String): String {
            val q = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
            val t = template.trim()
            if (t.contains("{{{s}}}")) return t.replace("{{{s}}}", q)
            if (t.contains("{q}")) return t.replace("{q}", q)
            if (t.contains("%s")) return t.replace("%s", q)
            if (t.endsWith("=")) return t + q
            val join = if (t.contains("?")) "&" else "?"
            return "$t${join}q=$q"
        }

        /**
         * `!word rest` against [bangs]. Longest phrase wins.
         * Returns null when there is no query after the word yet.
         */
        fun match(raw: String, bangs: List<CustomBang>): Pair<CustomBang, String>? {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("!")) return null
            val rest = trimmed.removePrefix("!").trim()
            if (rest.isEmpty() || ' ' !in rest && '\t' !in rest) return null
            val lower = rest.lowercase()
            var best: Pair<CustomBang, Int>? = null
            for (bang in bangs) {
                val phrase = bang.phrase.lowercase()
                if (phrase.isEmpty()) continue
                if (lower.startsWith("$phrase ")) {
                    if (best == null || phrase.length > best.second) best = bang to phrase.length
                }
            }
            val hit = best ?: return null
            val query = rest.substring(hit.second).trim()
            if (query.isEmpty()) return null
            return hit.first to query
        }

        /**
         * `!+app` or `!+app query`.
         * [appWord] is the app (or the word you want to save). [query] is empty until a space.
         */
        fun parsePlus(raw: String): PlusQuery? {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("!+")) return null
            val rest = trimmed.removePrefix("!+").trim()
            if (rest.isEmpty()) return PlusQuery("", "")
            val space = rest.indexOfFirst { it.isWhitespace() }
            if (space < 0) return PlusQuery(rest, "")
            return PlusQuery(rest.substring(0, space), rest.substring(space + 1).trim())
        }

        fun fromJson(raw: String?): List<CustomBang> {
            if (raw.isNullOrBlank()) return emptyList()
            val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val phrase = normalizePhrase(o.optString("phrase")) ?: continue
                    add(
                        CustomBang(
                            id = o.optString("id").ifBlank { phrase },
                            phrase = phrase,
                            packageName = o.optString("packageName"),
                            searchUrl = o.optString("searchUrl"),
                        ),
                    )
                }
            }
        }

        fun toJson(items: List<CustomBang>): String {
            val arr = JSONArray()
            items.forEach { bang ->
                arr.put(
                    JSONObject()
                        .put("id", bang.id)
                        .put("phrase", bang.phrase)
                        .put("packageName", bang.packageName)
                        .put("searchUrl", bang.searchUrl),
                )
            }
            return arr.toString()
        }
    }
}
