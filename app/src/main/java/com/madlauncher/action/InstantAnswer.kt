package com.madlauncher.action

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class InstantAnswer(
    val heading: String,
    val text: String,
    val source: String,
    val url: String,
    val fromDuckDuckGo: Boolean = true,
)

object InstantAnswerClient {
    const val DDG_ATTRIBUTION = "Results from DuckDuckGo"

    fun shouldLookup(query: String): Boolean {
        val q = query.trim()
        if (q.length < 3 || q.length > 80) return false
        if (q.startsWith("@") || q.startsWith("!") || q.startsWith("#") ||
            q.startsWith("*") || q.startsWith("/") || q.startsWith(":") ||
            q.startsWith("&") || q.startsWith("?") ||
            q.startsWith("=")
        ) {
            return false
        }
        if (CommandDetector.isPhoneLike(q)) return false
        return when (val cmd = CommandDetector.detect(q)) {
            is ActionCommand.Calc -> InstantModules.isEnabled(InstantModules.moduleIdFor(cmd.result))
            is ActionCommand.None -> true
            else -> false
        }
    }

    fun shouldFetchWeb(query: String, enabled: Boolean, duckDuckGo: Boolean = true): Boolean =
        enabled && duckDuckGo && shouldLookup(query)

    fun parse(json: String): InstantAnswer? {
        val answerType = jsonField(json, "AnswerType").trim().lowercase()
        val answer = jsonField(json, "Answer").let(::stripHtml)
        if (answerType == "calc" || answerType == "conversions") {
            if (answer.isBlank()) return null
            val heading = jsonField(json, "Heading").trim()
            val url = jsonField(json, "AbstractURL").ifBlank { jsonField(json, "AnswerURL") }.trim()
            return InstantAnswer(
                heading = heading.ifBlank { answerType },
                text = answer.take(420),
                source = DDG_ATTRIBUTION,
                url = url,
                fromDuckDuckGo = true,
            )
        }
        val abstract = jsonField(json, "AbstractText").ifBlank { jsonField(json, "Abstract") }.let(::stripHtml)
        val text = answer.ifBlank { abstract }.trim()
        if (text.isBlank()) return null
        val heading = jsonField(json, "Heading").trim()
        val url = jsonField(json, "AbstractURL").ifBlank { jsonField(json, "AnswerURL") }.trim()
        val source = jsonField(json, "AbstractSource")
            .ifBlank { jsonField(json, "AnswerType") }
            .ifBlank { "DuckDuckGo" }
            .trim()
        return InstantAnswer(
            heading = heading.ifBlank { source },
            text = text.take(420),
            source = source,
            url = url,
            fromDuckDuckGo = true,
        )
    }

    /** Prefer a real DDG [parse] hit; otherwise the local calc/conversions module. */
    fun resolve(query: String, ddgJson: String?): InstantAnswer? {
        ddgJson?.let { parse(it) }?.let { return it }
        return InstantModules.localAnswer(query)
    }

    fun fetch(query: String): InstantAnswer? {
        if (!shouldLookup(query)) return null
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        val url =
            "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1&t=madactionbox"
        val body = runCatching { httpGet(url) }.getOrNull()
        return resolve(query, body)
    }

    internal fun jsonField(raw: String, key: String): String {
        val pattern = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val encoded = pattern.find(raw)?.groupValues?.get(1) ?: return ""
        return encoded
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\n", "\n")
            .replace("\\\\", "\\")
    }

    private fun stripHtml(raw: String): String =
        raw.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "MADActionBox/1.0")
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
