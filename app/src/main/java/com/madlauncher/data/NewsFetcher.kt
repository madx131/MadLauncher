package com.madlauncher.data

import android.text.Html
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object NewsFetcher {
    private val imgSrc = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    fun fetch(providers: List<NewsProvider>): List<NewsStory> {
        val stories = providers.flatMap { provider ->
            if (provider.rssUrl.isBlank()) return@flatMap emptyList()
            runCatching { parseFeed(httpGet(provider.rssUrl), provider) }.getOrDefault(emptyList())
        }
        return stories
            .distinctBy { it.url.ifBlank { it.title } }
            .sortedByDescending { it.publishedAt }
            .take(80)
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "MADLauncher/1.0")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }

    private fun parseFeed(xml: String, provider: NewsProvider): List<NewsStory> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.reader())
        val out = mutableListOf<NewsStory>()
        var event = parser.eventType
        var inItem = false
        var title = ""
        var link = ""
        var date = ""
        var snippet = ""
        var image = ""
        while (event != XmlPullParser.END_DOCUMENT && out.size < 20) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.substringAfter(':').lowercase()
                    when (tag) {
                        "item", "entry" -> {
                            inItem = true
                            title = ""
                            link = ""
                            date = ""
                            snippet = ""
                            image = ""
                        }
                        "title" -> if (inItem) title = parser.nextTextSafe()
                        "link" -> if (inItem) {
                            val href = parser.getAttributeValue(null, "href").orEmpty()
                            link = href.ifBlank { parser.nextTextSafe() }
                        }
                        "pubdate", "published", "updated", "date" -> if (inItem) date = parser.nextTextSafe()
                        "enclosure", "thumbnail", "image" -> if (inItem && image.isBlank()) {
                            image = imageAttr(parser, tag)
                        }
                        "content" -> if (inItem) {
                            val media = parser.getAttributeValue(null, "url").orEmpty()
                            if (media.isNotBlank()) {
                                if (image.isBlank()) image = media
                            } else {
                                val raw = parser.nextTextSafe()
                                if (image.isBlank()) image = extractImg(raw)
                                if (snippet.isBlank()) snippet = stripHtml(raw)
                            }
                        }
                        "description", "summary" -> if (inItem) {
                            val raw = parser.nextTextSafe()
                            if (image.isBlank()) image = extractImg(raw)
                            if (snippet.isBlank()) snippet = stripHtml(raw)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.substringAfter(':').lowercase()
                    if (inItem && (tag == "item" || tag == "entry")) {
                        inItem = false
                        val cleanTitle = stripHtml(title).trim()
                        if (cleanTitle.isNotBlank()) {
                            out += NewsStory(
                                id = "${provider.id}:${link.ifBlank { cleanTitle }}",
                                title = cleanTitle,
                                url = link.trim(),
                                sourceId = provider.id,
                                sourceName = provider.name,
                                publishedAt = parseDate(date),
                                snippet = snippet.take(180),
                                packageName = provider.packageName,
                                imageUrl = image.trim(),
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun imageAttr(parser: XmlPullParser, tag: String): String {
        val url = parser.getAttributeValue(null, "url")
            ?: parser.getAttributeValue(null, "href")
            ?: return ""
        val type = parser.getAttributeValue(null, "type").orEmpty()
        val medium = parser.getAttributeValue(null, "medium").orEmpty()
        return if (
            medium == "image" ||
            type.startsWith("image") ||
            tag == "thumbnail" ||
            tag == "image" ||
            looksLikeImage(url)
        ) url else ""
    }

    private fun extractImg(html: String): String {
        return imgSrc.find(html)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun looksLikeImage(url: String): Boolean {
        val lower = url.lowercase()
        return listOf(".jpg", ".jpeg", ".png", ".webp", ".gif").any { lower.contains(it) }
    }

    private fun XmlPullParser.nextTextSafe(): String {
        return runCatching { nextText() }.getOrDefault("").trim()
    }

    private fun stripHtml(raw: String): String {
        val decoded = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
        return decoded.replace(Regex("\\s+"), " ").trim()
    }

    private fun parseDate(raw: String): Long {
        if (raw.isBlank()) return 0L
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
        )
        formats.forEach { pattern ->
            val fmt = SimpleDateFormat(pattern, Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            runCatching { fmt.parse(raw)?.time }.getOrNull()?.let { return it }
        }
        return 0L
    }
}
