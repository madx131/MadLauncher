package com.madlauncher.data

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.net.URLEncoder
import java.util.Locale

data class NewsProvider(
    val id: String,
    val name: String,
    val packageName: String = "",
    val rssUrl: String = "",
    val countries: List<String> = emptyList(),
)

data class NewsStory(
    val id: String,
    val title: String,
    val url: String,
    val sourceId: String,
    val sourceName: String,
    val publishedAt: Long,
    val snippet: String = "",
    val packageName: String = "",
    val imageUrl: String = "",
)

object NewsCatalog {
    fun googleRss(place: DevicePlace): String {
        val lang = place.language.ifBlank { "en" }
        val country = place.country.ifBlank { "US" }
        return "https://news.google.com/rss?hl=$lang-$country&gl=$country&ceid=$country:$lang"
    }

    fun googleLocalRss(place: DevicePlace): String? {
        val geo = listOf(place.city, place.countryName.ifBlank { place.country })
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")
        if (geo.isBlank()) return null
        val query = URLEncoder.encode(geo, "UTF-8")
        val lang = place.language.ifBlank { "en" }
        val country = place.country.ifBlank { "US" }
        return "https://news.google.com/rss/headlines/section/geo/$query?hl=$lang-$country&gl=$country&ceid=$country:$lang"
    }

    val catalog: List<NewsProvider> = listOf(
        NewsProvider("google", "Google News", "com.google.android.apps.magazines"),
        NewsProvider("bbc", "BBC News", "bbc.mobile.news.ww", "https://feeds.bbci.co.uk/news/rss.xml", listOf("GB")),
        NewsProvider("cnn", "CNN", "com.cnn.mobile.android.phone", "https://rss.cnn.com/rss/edition.rss", listOf("US")),
        NewsProvider("reuters", "Reuters", "com.thomsonreuters.reuters", "https://www.reutersagency.com/feed/?best-topics=world&post_type=best"),
        NewsProvider("ap", "AP News", "com.apnews", "https://news.google.com/rss/search?q=site:apnews.com&hl=en-US&gl=US&ceid=US:en", listOf("US")),
        NewsProvider("guardian", "The Guardian", "com.guardian", "https://www.theguardian.com/world/rss", listOf("GB")),
        NewsProvider("nyt", "New York Times", "com.nytimes.android", "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml", listOf("US")),
        NewsProvider("wapo", "Washington Post", "com.washingtonpost.android", "https://feeds.washingtonpost.com/rss/world", listOf("US")),
        NewsProvider("npr", "NPR", "org.npr.nprnews", "https://feeds.npr.org/1001/rss.xml", listOf("US")),
        NewsProvider("aljazeera", "Al Jazeera", "com.aljazeera.english", "https://www.aljazeera.com/xml/rss/all.xml"),
        NewsProvider("fox", "Fox News", "com.foxnews.android", "https://moxie.foxnews.com/google-publisher/latest.xml", listOf("US")),
        NewsProvider("abc", "ABC News", "com.abc.abcnews", "https://abcnews.go.com/abcnews/topstories", listOf("US")),
        NewsProvider("nbc", "NBC News", "com.zumobi.msnbc", "https://feeds.nbcnews.com/nbcnews/public/news", listOf("US")),
        NewsProvider("wsj", "Wall Street Journal", "com.wsj.android", "https://feeds.a.dj.com/rss/RSSWorldNews.xml", listOf("US")),
        NewsProvider("economist", "The Economist", "com.economist.lamarr", "https://www.economist.com/the-world-this-week/rss.xml"),
        NewsProvider("verge", "The Verge", "com.theverge.android", "https://www.theverge.com/rss/index.xml", listOf("US")),
        NewsProvider("techcrunch", "TechCrunch", "com.aol.mobile.techcrunch", "https://techcrunch.com/feed/"),
        NewsProvider("wired", "WIRED", "com.wired.app", "https://www.wired.com/feed/rss", listOf("US")),
        NewsProvider("ars", "Ars Technica", "com.tinyspeck.android.ars", "https://feeds.arstechnica.com/arstechnica/index", listOf("US")),
        NewsProvider("cbc", "CBC News", "cbc.mobile.news", "https://www.cbc.ca/webfeed/rss/rss-topstories", listOf("CA")),
        NewsProvider("globe", "The Globe and Mail", "com.theglobeandmail.android", "https://www.theglobeandmail.com/arc/outboundfeeds/rss/category/canada/", listOf("CA")),
        NewsProvider("abcau", "ABC News Australia", "au.net.abc.news", "https://www.abc.net.au/news/feed/51120/rss.xml", listOf("AU")),
        NewsProvider("smh", "Sydney Morning Herald", "au.com.fairfax.smh", "https://www.smh.com.au/rss/feed.xml", listOf("AU")),
        NewsProvider("toi", "Times of India", "com.toi.reader.activities", "https://timesofindia.indiatimes.com/rssfeedstopstories.cms", listOf("IN")),
        NewsProvider("ndtv", "NDTV", "com.ndtv.android", "https://feeds.feedburner.com/ndtvnews-top-stories", listOf("IN")),
        NewsProvider("toi_il", "Times of Israel", "com.timesofisrael.android", "https://www.timesofisrael.com/feed/", listOf("IL")),
        NewsProvider("jpost", "Jerusalem Post", "com.jpost.app", "https://www.jpost.com/rss/rssfeedsfrontpage.aspx", listOf("IL")),
        NewsProvider("haaretz", "Haaretz", "com.haaretz", "https://www.haaretz.com/srv/rss--www", listOf("IL")),
        NewsProvider("ynet", "Ynet", "com.ynet.android", "https://www.ynet.co.il/Integration/StoryRss2.xml", listOf("IL")),
        NewsProvider("spiegel", "Der Spiegel", "de.spiegel.android.app.spon", "https://www.spiegel.de/international/index.rss", listOf("DE")),
        NewsProvider("dw", "Deutsche Welle", "com.dw.android", "https://rss.dw.com/rdf/rss-en-all", listOf("DE")),
        NewsProvider("lemonde", "Le Monde", "com.lemonde.android", "https://www.lemonde.fr/rss/une.xml", listOf("FR")),
        NewsProvider("france24", "France 24", "com.france24.androidapp", "https://www.france24.com/en/rss", listOf("FR")),
        NewsProvider("elpais", "El País", "com.elpais.elpais", "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/portada", listOf("ES")),
        NewsProvider("ansa", "ANSA", "it.ansa.android", "https://www.ansa.it/sito/ansait_rss.xml", listOf("IT")),
        NewsProvider("nos", "NOS", "nl.nos.app", "https://feeds.nos.nl/nosnieuwsalgemeen", listOf("NL")),
        NewsProvider("svt", "SVT Nyheter", "se.svt.android.svtnyheter", "https://www.svt.se/rss.xml", listOf("SE")),
        NewsProvider("nrk", "NRK", "no.nrk.yr", "https://www.nrk.no/toppsaker.rss", listOf("NO")),
        NewsProvider("yle", "Yle", "fi.yle.weatherservice", "https://feeds.yle.fi/uutiset/v1/majorHeadlines/YLE_NEWS.rss", listOf("FI")),
        NewsProvider("globo", "G1", "com.globo.globonews", "https://g1.globo.com/rss/g1/", listOf("BR")),
        NewsProvider("nhk", "NHK World", "jp.or.nhk.nhkworld", "https://www3.nhk.or.jp/rss/news/cat0.xml", listOf("JP")),
        NewsProvider("koreaherald", "Korea Herald", "com.heraldcorp.android", "https://www.koreaherald.com/rss", listOf("KR")),
        NewsProvider("scmp", "South China Morning Post", "com.scmp.mobile", "https://www.scmp.com/rss/91/feed", listOf("HK")),
        NewsProvider("straitstimes", "The Straits Times", "com.buuuk.st", "https://www.straitstimes.com/news/world/rss.xml", listOf("SG")),
        NewsProvider("news24", "News24", "com.news24.android", "https://feeds.news24.com/articles/news24/TopStories/rss", listOf("ZA")),
        NewsProvider("thenational", "The National", "ae.thenational.android", "https://www.thenationalnews.com/arc/outboundfeeds/rss/", listOf("AE")),
        NewsProvider("arabnews", "Arab News", "com.arabnews.app", "https://www.arabnews.com/rss.xml", listOf("SA")),
        NewsProvider("rte", "RTÉ News", "ie.rte.news", "https://www.rte.ie/news/rss/news-headlines.xml", listOf("IE")),
        NewsProvider("kyiv", "Kyiv Independent", "com.kyivindependent.app", "https://kyivindependent.com/feed/", listOf("UA")),
        NewsProvider("flipboard", "Flipboard", "flipboard.app"),
        NewsProvider("smartnews", "SmartNews", "jp.gocro.smartnews.android", countries = listOf("JP")),
        NewsProvider("newsbreak", "NewsBreak", "com.particlenews.newsbreak", countries = listOf("US")),
        NewsProvider("feedly", "Feedly", "com.devhd.feedly"),
        NewsProvider("ground", "Ground News", "com.groundnews.app", countries = listOf("CA")),
        NewsProvider("apple", "News", "com.apple.news"),
    )

    fun discover(
        pm: PackageManager,
        installed: List<LaunchableApp>,
        place: DevicePlace,
        locationGranted: Boolean,
    ): List<NewsProvider> {
        val known = withFeeds(place, locationGranted)
        val byPkg = known.associateBy { it.packageName }.filterKeys { it.isNotBlank() }
        val found = linkedMapOf<String, NewsProvider>()
        known.forEach { found[it.id] = it }
        installed.forEach { app ->
            val match = byPkg[app.packageName]
            if (match != null) {
                found[match.id] = match.copy(name = app.label.ifBlank { match.name })
            } else if (looksLikeNews(app)) {
                val id = "app:${app.packageName}"
                found[id] = NewsProvider(id, app.label, app.packageName)
            }
        }
        newsCategoryPackage(pm)?.let { pkg ->
            val app = installed.firstOrNull { it.packageName == pkg }
            if (app != null && found.values.none { it.packageName == pkg }) {
                found["app:$pkg"] = NewsProvider("app:$pkg", app.label, pkg)
            }
        }
        val installedPkgs = installed.map { it.packageName }.toSet()
        return found.values.sortedWith(compareBy({ rank(it, place, locationGranted, installedPkgs) }, { it.name.lowercase(Locale.US) }))
    }

    fun defaultSelection(
        providers: List<NewsProvider>,
        installedPkgs: Set<String>,
        place: DevicePlace,
        locationGranted: Boolean,
    ): List<String> {
        val google = providers.filter { it.id == "google" || it.id == "google-local" }.map { it.id }
        if (locationGranted) {
            val local = providers.filter { isLocal(it, place) }.map { it.id }
            val picked = (google + local).distinct()
            if (picked.isNotEmpty()) return picked
        }
        val installedKnown = providers.filter { it.packageName.isNotBlank() && it.packageName in installedPkgs }
        if (installedKnown.isNotEmpty()) return (google + installedKnown.map { it.id }).distinct()
        return google.ifEmpty { listOf("google") }
    }

    fun isLocal(provider: NewsProvider, place: DevicePlace): Boolean {
        if (provider.id == "google" || provider.id == "google-local") return true
        return place.country.uppercase(Locale.US) in provider.countries.map { it.uppercase(Locale.US) }
    }

    private fun withFeeds(place: DevicePlace, locationGranted: Boolean): List<NewsProvider> {
        val google = catalog.first { it.id == "google" }.copy(rssUrl = googleRss(place))
        val rest = catalog.filterNot { it.id == "google" }
        val local = if (locationGranted) {
            googleLocalRss(place)?.let { url ->
                val title = if (place.city.isNotBlank()) "Google News · ${place.city}" else "Google News · Local"
                NewsProvider("google-local", title, rssUrl = url, countries = listOf(place.country))
            }
        } else null
        return listOfNotNull(google, local) + rest
    }

    private fun rank(
        provider: NewsProvider,
        place: DevicePlace,
        locationGranted: Boolean,
        installedPkgs: Set<String>,
    ): Int {
        if (provider.id == "google") return 0
        if (provider.id == "google-local") return 1
        val local = locationGranted && isLocal(provider, place)
        val installed = provider.packageName.isNotBlank() && provider.packageName in installedPkgs
        return when {
            local && installed -> 2
            local -> 3
            installed -> 4
            else -> 5
        }
    }

    private fun looksLikeNews(app: LaunchableApp): Boolean {
        val pkg = app.packageName.lowercase(Locale.US)
        val label = app.label.lowercase(Locale.US)
        if (pkg == "com.android.vending" || pkg.contains("chrome") || pkg.contains("gmail")) return false
        return pkg.contains("news") || label.contains("news") || pkg.contains("newspaper")
    }

    private fun newsCategoryPackage(pm: PackageManager): String? {
        if (Build.VERSION.SDK_INT < 33) return null
        val intent = Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_NEWS")
        return runCatching { intent.resolveActivity(pm)?.packageName }.getOrNull()
    }
}
