package com.madlauncher.action

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import com.madlauncher.data.LaunchableApp

data class SearchableAppDef(
    val appName: String,
    val displayName: String,
    val keywords: List<String>,
    val color: Long,
    val packages: List<String>,
    /** True when discovered via ACTION_SEARCH / searchable metadata rather than the curated catalog. */
    val discovered: Boolean = false,
    /**
     * Apps whose public search URLs rarely land on a filled search UI (Instagram, Discord, …).
     * MAD copies the query and opens the app so the user can paste into in-app search.
     */
    val assistedSearch: Boolean = false,
    /** When the app is not installed, `!alias query` can still open [searchUrl]. */
    val siteSearch: Boolean = true,
    val altSearchUrls: (String) -> List<String> = { emptyList() },
    /** Last param so trailing-lambda catalog entries bind correctly. */
    val searchUrl: (String) -> String,
) {
    fun urlsFor(query: String): List<String> {
        return (listOf(searchUrl(query)) + altSearchUrls(query))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}

data class ResolvedSearchApp(
    val def: SearchableAppDef,
    val app: LaunchableApp,
    /** Activity that actually hosts in-app search, when known. */
    val searchComponent: ComponentName? = null,
)

object SearchableApps {
    val catalog: List<SearchableAppDef> = listOf(
        SearchableAppDef(
            "YouTube", "YouTube",
            listOf("yt", "yout", "youtu", "youtub", "youtube", "tube"),
            0xFFFF0000,
            listOf(
                "com.google.android.youtube",
                "app.morphe.android.youtube",
                "app.revanced.android.youtube",
            ),
        ) { q -> "https://www.youtube.com/results?search_query=${enc(q)}" },
        SearchableAppDef(
            "YouTube Music", "YT Music",
            listOf("ytm", "ytmu", "ytmus", "ytmusic", "yt music", "youtubemusic"),
            0xFFFF0000,
            listOf(
                "com.google.android.apps.youtube.music",
                "app.morphe.android.apps.youtube.music",
                "app.revanced.android.apps.youtube.music",
            ),
        ) { q -> "https://music.youtube.com/search?q=${enc(q)}" },
        SearchableAppDef(
            appName = "NewPipe",
            displayName = "NewPipe",
            keywords = listOf("np", "newpipe", "pipe"),
            color = 0xFFCD201F,
            packages = listOf("org.schabi.newpipe"),
            assistedSearch = true,
            searchUrl = { q -> "https://www.youtube.com/results?search_query=${enc(q)}" },
        ),
        SearchableAppDef(
            "Netflix", "Netflix",
            listOf("netfl", "netflix", "nflx"),
            0xFFE50914,
            listOf("com.netflix.mediaclient"),
        ) { q -> "https://www.netflix.com/search?q=${enc(q)}" },
        SearchableAppDef(
            "Twitch", "Twitch",
            listOf("twitch", "twtch", "live"),
            0xFF9146FF,
            listOf("tv.twitch.android.app"),
        ) { q -> "https://www.twitch.tv/search?term=${enc(q)}" },
        SearchableAppDef(
            "Spotify", "Spotify",
            listOf("sp", "spot", "spoti", "spotify"),
            0xFF1DB954,
            listOf("com.spotify.music"),
        ) { q -> "spotify:search:${enc(q)}" },
        SearchableAppDef(
            "SoundCloud", "SoundCloud",
            listOf("sc", "soundc", "soundcloud"),
            0xFFFF5500,
            listOf("com.soundcloud.android"),
        ) { q -> "https://soundcloud.com/search?q=${enc(q)}" },
        SearchableAppDef(
            appName = "Reddit",
            displayName = "Reddit",
            keywords = listOf("rd", "redd", "reddit"),
            color = 0xFFFF4500,
            packages = listOf("com.reddit.frontpage"),
            altSearchUrls = { q -> SocialSearch.urls("com.reddit.frontpage", q).drop(1) },
            searchUrl = { q -> SocialSearch.urls("com.reddit.frontpage", q).first() },
        ),
        SearchableAppDef(
            appName = "Twitter",
            displayName = "X / Twitter",
            keywords = listOf("x", "tw", "twit", "tweet", "twitter", "x.com"),
            color = 0xFF1DA1F2,
            packages = listOf("com.twitter.android", "com.x.android"),
            altSearchUrls = { q -> SocialSearch.urls("com.twitter.android", q).drop(1) },
            searchUrl = { q -> SocialSearch.urls("com.twitter.android", q).first() },
        ),
        SearchableAppDef(
            appName = "Instagram",
            displayName = "Instagram",
            keywords = listOf("ig", "inst", "insta", "instagram"),
            color = 0xFFE1306C,
            packages = listOf("com.instagram.android"),
            assistedSearch = true,
            altSearchUrls = { q -> SocialSearch.urls("com.instagram.android", q).drop(1) },
            searchUrl = { q -> SocialSearch.urls("com.instagram.android", q).first() },
        ),
        SearchableAppDef(
            appName = "Facebook",
            displayName = "Facebook",
            keywords = listOf("fb", "face", "facebook"),
            color = 0xFF1877F2,
            packages = listOf("com.facebook.katana", "com.facebook.lite"),
            assistedSearch = true,
            altSearchUrls = { q -> SocialSearch.urls("com.facebook.katana", q).drop(1) },
            searchUrl = { q -> SocialSearch.urls("com.facebook.katana", q).first() },
        ),
        SearchableAppDef(
            appName = "Messenger",
            displayName = "Messenger",
            keywords = listOf("msg", "messenger", "fbm"),
            color = 0xFF0084FF,
            packages = listOf("com.facebook.orca"),
            assistedSearch = true,
            siteSearch = false,
            searchUrl = { _ -> "" },
        ),
        SearchableAppDef(
            "Gmail", "Gmail",
            listOf("gm", "gmail"),
            0xFFEA4335,
            listOf("com.google.android.gm"),
        ) { q -> "https://mail.google.com/mail/u/0/#search/${enc(q)}" },
        SearchableAppDef(
            appName = "Google Drive",
            displayName = "Drive",
            keywords = listOf("drive", "gdrive", "docs"),
            color = 0xFF4285F4,
            packages = listOf("com.google.android.apps.docs"),
            assistedSearch = true,
            searchUrl = { q -> "https://drive.google.com/drive/search?q=${enc(q)}" },
        ),
        SearchableAppDef(
            "GitHub", "GitHub",
            listOf("gh", "git", "githu", "github"),
            0xFFC9D1D9,
            listOf("com.github.android"),
        ) { q -> "https://github.com/search?q=${enc(q)}" },
        SearchableAppDef(
            appName = "Calendar",
            displayName = "Calendar",
            keywords = listOf("cal", "calendar", "agenda"),
            color = 0xFF1A73E8,
            packages = listOf(
                "com.google.android.calendar",
                "com.android.calendar",
                "com.samsung.android.calendar",
            ),
            siteSearch = false,
            searchUrl = { _ -> "" },
        ),
        SearchableAppDef(
            appName = "Play Store",
            displayName = "Play Store",
            keywords = listOf("p", "play", "playstore"),
            color = 0xFF34A853,
            packages = listOf("com.android.vending"),
            altSearchUrls = { q ->
                listOf("https://play.google.com/store/search?q=${enc(q)}&c=apps")
            },
            searchUrl = { q -> "market://search?q=${enc(q)}" },
        ),
        SearchableAppDef(
            appName = "Aurora Store",
            displayName = "Aurora",
            keywords = listOf("aurora", "aurorastore"),
            color = 0xFF6B5CE7,
            packages = listOf("com.aurora.store"),
            assistedSearch = true,
            searchUrl = { q -> "https://play.google.com/store/search?q=${enc(q)}" },
        ),
        SearchableAppDef(
            "F-Droid", "F-Droid",
            listOf("fdroid", "f-droid", "f droid"),
            0xFF1976D2,
            listOf("org.fdroid.fdroid"),
        ) { q -> "https://search.f-droid.org/?q=${enc(q)}" },
        SearchableAppDef(
            "Chrome", "Chrome",
            listOf("chr", "chrom", "chrome", "google"),
            0xFF4285F4,
            listOf("com.android.chrome", "com.chrome.android"),
        ) { q -> "https://www.google.com/search?q=${enc(q)}" },
        SearchableAppDef(
            "Vivaldi", "Vivaldi",
            listOf("viv", "vivaldi"),
            0xFFEF3939,
            listOf("com.vivaldi.browser"),
        ) { q -> "https://www.google.com/search?q=${enc(q)}" },
        SearchableAppDef(
            "Amazon", "Amazon",
            listOf("az", "amaz", "amazon"),
            0xFFFF9900,
            listOf("com.amazon.mShop.android.shopping"),
        ) { q -> "https://www.amazon.com/s?k=${enc(q)}" },
        SearchableAppDef(
            "Wikipedia", "Wikipedia",
            listOf("w", "wiki", "wikip", "wikipedia"),
            0xFFA8A8A8,
            listOf("org.wikipedia"),
        ) { q -> "https://en.wikipedia.org/w/index.php?search=${enc(q)}" },
        SearchableAppDef(
            appName = "Discord",
            displayName = "Discord",
            keywords = listOf("dc", "disc", "discord"),
            color = 0xFF5865F2,
            packages = listOf("com.discord"),
            assistedSearch = true,
            siteSearch = false,
            searchUrl = { _ -> "https://discord.com/channels/@me" },
        ),
        SearchableAppDef(
            "LinkedIn", "LinkedIn",
            listOf("li", "linkedin", "linked"),
            0xFF0A66C2,
            listOf("com.linkedin.android"),
        ) { q -> SocialSearch.urls("com.linkedin.android", q).first() },
        SearchableAppDef(
            appName = "TikTok",
            displayName = "TikTok",
            keywords = listOf("tiktok", "tt"),
            color = 0xFF010101,
            packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
            altSearchUrls = { q -> SocialSearch.urls("com.zhiliaoapp.musically", q).drop(1) },
            searchUrl = { q -> SocialSearch.urls("com.zhiliaoapp.musically", q).first() },
        ),
        SearchableAppDef(
            "Threads", "Threads",
            listOf("threads"),
            0xFF000000,
            listOf("com.instagram.barcelona"),
        ) { q -> SocialSearch.urls("com.instagram.barcelona", q).first() },
        SearchableAppDef(
            appName = "Snapchat",
            displayName = "Snapchat",
            keywords = listOf("snap", "snapchat"),
            color = 0xFFFFFC00,
            packages = listOf("com.snapchat.android"),
            assistedSearch = true,
            altSearchUrls = { q -> SocialSearch.urls("com.snapchat.android", q).drop(1) },
            searchUrl = { q -> SocialSearch.urls("com.snapchat.android", q).first() },
        ),
        SearchableAppDef(
            "Truth Social", "Truth Social",
            listOf("truth", "truthsocial"),
            0xFFE31C25,
            listOf("com.truthsocial.android.app"),
        ) { q -> SocialSearch.urls("com.truthsocial.android.app", q).first() },
        SearchableAppDef(
            "Bluesky", "Bluesky",
            listOf("bsky", "bluesky"),
            0xFF1185FE,
            listOf("xyz.blueskyweb.app"),
        ) { q -> SocialSearch.urls("xyz.blueskyweb.app", q).first() },
        SearchableAppDef(
            "Pinterest", "Pinterest",
            listOf("pin", "pinterest"),
            0xFFE60023,
            listOf("com.pinterest"),
        ) { q -> "https://www.pinterest.com/search/pins/?q=${enc(q)}" },
        SearchableAppDef(
            "Bing", "Bing",
            listOf("bing"),
            0xFF00809D,
            listOf("com.microsoft.bing"),
        ) { q -> "https://www.bing.com/search?q=${enc(q)}" },
        SearchableAppDef(
            "Temu", "Temu",
            listOf("temu"),
            0xFFFF6A00,
            listOf("com.einnovation.temu"),
        ) { q -> "https://www.temu.com/search_result.html?search_key=${enc(q)}" },
        SearchableAppDef(
            "iHerb", "iHerb",
            listOf("iherb"),
            0xFF458500,
            listOf("com.iherb"),
        ) { q -> "https://www.iherb.com/search?kw=${enc(q)}" },
        SearchableAppDef(
            "Steam", "Steam",
            listOf("steam"),
            0xFF171A21,
            listOf("com.valvesoftware.android.steam.community"),
        ) { q -> "https://store.steampowered.com/search/?term=${enc(q)}" },
    )

    /** Sites that open in the browser when no app is installed. */
    val webBangs: List<SearchableAppDef> = listOf(
        SearchableAppDef(
            appName = "Urban Dictionary",
            displayName = "Urban Dictionary",
            keywords = listOf("ud", "urban"),
            color = 0xFFE85D04,
            packages = emptyList(),
            searchUrl = { q -> "https://www.urbandictionary.com/define.php?term=${enc(q)}" },
        ),
        SearchableAppDef(
            appName = "IMDb",
            displayName = "IMDb",
            keywords = listOf("imdb"),
            color = 0xFFF5C518,
            packages = emptyList(),
            searchUrl = { q -> "https://www.imdb.com/find/?q=${enc(q)}" },
        ),
        SearchableAppDef(
            appName = "Stack Overflow",
            displayName = "Stack Overflow",
            keywords = listOf("so", "stackoverflow"),
            color = 0xFFF48024,
            packages = emptyList(),
            searchUrl = { q -> "https://stackoverflow.com/search?q=${enc(q)}" },
        ),
    )

    /** Curated + auto-detected searchable apps that are currently installed. */
    fun resolveAll(context: Context, apps: List<LaunchableApp>): List<ResolvedSearchApp> {
        val byPkg = LinkedHashMap<String, ResolvedSearchApp>()
        val launchByPkg = apps.groupBy { it.packageName }.mapValues { (_, list) -> list.first() }

        catalog.forEach { def ->
            val installed = apps.firstOrNull { app ->
                app.packageName in def.packages ||
                    app.label.equals(def.appName, ignoreCase = true) ||
                    app.label.equals(def.displayName, ignoreCase = true)
            } ?: return@forEach
            byPkg[installed.packageName] = ResolvedSearchApp(
                def,
                installed,
                searchComponent = findSearchComponent(context, installed.packageName),
            )
        }

        // Discover SearchableInfo / ACTION_SEARCH apps not already in the curated catalog.
        discoverSearchCapable(context, launchByPkg).forEach { resolved ->
            byPkg.putIfAbsent(resolved.app.packageName, resolved)
        }

        return byPkg.values.sortedWith(
            compareBy<ResolvedSearchApp> { it.def.discovered }
                .thenBy { it.def.displayName.lowercase() },
        )
    }

    @Deprecated("Use resolveAll", ReplaceWith("resolveAll(context, apps)"))
    fun resolveInstalled(apps: List<LaunchableApp>): List<ResolvedSearchApp> {
        return catalog.mapNotNull { def ->
            val installed = apps.firstOrNull { app ->
                app.packageName in def.packages ||
                    app.label.equals(def.appName, ignoreCase = true) ||
                    app.label.equals(def.displayName, ignoreCase = true)
            } ?: return@mapNotNull null
            ResolvedSearchApp(def, installed)
        }
    }

    /**
     * Site search is `!` + alias, e.g. `!yt`, `!yt cats`.
     * Installed apps open in the app. A lone `!` lists those apps.
     */
    fun matching(query: String, installed: List<ResolvedSearchApp>): List<ResolvedSearchApp> {
        val trimmed = query.trim()
        if (!trimmed.startsWith("!")) return emptyList()
        val rest = trimmed.removePrefix("!").trim()
        if (rest.isEmpty()) return installed.take(24)
        val alias = rest.substringBefore(' ').lowercase()
        val exact = installed.filter { item ->
            item.def.keywords.any { kw ->
                kw.equals(alias, ignoreCase = true) ||
                    (alias.length >= 2 && kw.startsWith(alias, ignoreCase = true))
            } ||
                item.def.displayName.startsWith(alias, ignoreCase = true) ||
                item.app.label.startsWith(alias, ignoreCase = true)
        }
        if (exact.isNotEmpty()) return exact
        return AppFuzzy.rank(alias, installed, limit = 8, cutoff = 52) { item ->
            (item.def.keywords + item.def.displayName + item.app.label).joinToString(" ")
        }
    }

    /**
     * Parse one-shot `!alias query` (e.g. `!ig cats`, `!yt music jazz`) for an installed app.
     * Returns null when there is no trailing search text yet.
     */
    fun parseInlineSearch(
        query: String,
        installed: List<ResolvedSearchApp>,
    ): Pair<ResolvedSearchApp, String>? {
        val trimmed = query.trim()
        if (!trimmed.startsWith("!")) return null
        val rest = trimmed.removePrefix("!").trim()
        if (rest.isEmpty() || ' ' !in rest && '\t' !in rest) return null
        val lower = rest.lowercase()
        var best: Pair<ResolvedSearchApp, Int>? = null
        for (item in installed) {
            val candidates = item.def.keywords +
                listOf(item.def.displayName, item.app.label).map { it.lowercase() }
            for (kw in candidates) {
                val k = kw.trim().lowercase()
                if (k.isEmpty() || lower == k) continue
                if (lower.startsWith("$k ")) {
                    val score = k.length
                    if (best == null || score > best.second) {
                        best = item to score
                    }
                }
            }
        }
        val matched = best ?: return null
        val q = rest.substring(matched.second).trim()
        if (q.isEmpty()) return null
        return matched.first to q
    }

    /**
     * `!ud yeet` / `!yt cats` — always the website, even if the app is installed.
     * In-app search is Space on a common app, not `!`.
     */
    fun parseWebBang(
        query: String,
        installed: List<ResolvedSearchApp>,
    ): Pair<SearchableAppDef, String>? {
        val trimmed = query.trim()
        if (!trimmed.startsWith("!")) return null
        val rest = trimmed.removePrefix("!").trim()
        if (rest.isEmpty() || ' ' !in rest && '\t' !in rest) return null
        val lower = rest.lowercase()
        var best: Pair<SearchableAppDef, Int>? = null
        for (def in matchingWeb(trimmed, installed)) {
            val candidates = def.keywords + listOf(def.displayName, def.appName)
            for (kw in candidates) {
                val k = kw.trim().lowercase()
                if (k.isEmpty() || lower == k) continue
                if (lower.startsWith("$k ")) {
                    if (best == null || k.length > best.second) best = def to k.length
                }
            }
        }
        val matched = best ?: return null
        val q = rest.substring(matched.second).trim()
        if (q.isEmpty()) return null
        return matched.first to q
    }

    /** Catalog and web-only sites for `!` — always the website, including when the app is installed. */
    fun matchingWeb(query: String, @Suppress("UNUSED_PARAMETER") installed: List<ResolvedSearchApp>): List<SearchableAppDef> {
        val trimmed = query.trim()
        if (!trimmed.startsWith("!")) return emptyList()
        val alias = trimmed.removePrefix("!").trim().substringBefore(' ').lowercase()
        val pool = (catalog + webBangs).filter { def -> def.siteSearch }
        if (alias.isEmpty()) return pool.take(16)
        return pool.filter { def ->
            def.keywords.any { kw ->
                kw.equals(alias, ignoreCase = true) ||
                    (alias.length >= 2 && kw.startsWith(alias, ignoreCase = true))
            } || def.displayName.startsWith(alias, ignoreCase = true)
        }
    }

    fun autocomplete(query: String, installed: List<ResolvedSearchApp>, apps: List<LaunchableApp>): String? {
        val trimmed = query.trim()
        if (trimmed.startsWith("!")) {
            val q = trimmed.removePrefix("!").trim().lowercase()
            if (q.isEmpty() || ' ' in q) return null
            val kw = (installed.map { it.def } + matchingWeb(trimmed, installed)).asSequence()
                .flatMap { def ->
                    def.keywords.asSequence() + sequenceOf(def.displayName.lowercase(), def.appName.lowercase())
                }
                .firstOrNull { it.startsWith(q) && it.length > q.length }
            return kw?.let { "!$it" }
        }
        val q = trimmed.lowercase()
        if (q.length < 2) return null
        return apps.firstOrNull { it.label.lowercase().startsWith(q) && it.label.length > q.length }?.label
    }

    private fun discoverSearchCapable(
        context: Context,
        launchByPkg: Map<String, LaunchableApp>,
    ): List<ResolvedSearchApp> {
        val pm = context.packageManager
        val self = context.packageName
        val sm = context.getSystemService(SearchManager::class.java) ?: return emptyList()
        val found = LinkedHashMap<String, ComponentName>()

        runCatching {
            sm.getSearchablesInGlobalSearch().orEmpty().forEach { info ->
                val cn = info.searchActivity ?: return@forEach
                val pkg = cn.packageName
                if (pkg == self || pkg == "android") return@forEach
                found.putIfAbsent(pkg, cn)
            }
        }

        launchByPkg.keys.forEach { pkg ->
            if (pkg == self || found.containsKey(pkg)) return@forEach
            findSearchComponent(context, pkg)?.let { found[pkg] = it }
        }

        return found.mapNotNull { (pkg, cn) ->
            val app = launchByPkg[pkg] ?: return@mapNotNull null
            if (looksLikeMediaOnly(pkg, app.label)) return@mapNotNull null
            val searchable = runCatching { sm.getSearchableInfo(cn) }.getOrNull()
            if (searchable == null) return@mapNotNull null
            ResolvedSearchApp(
                SearchableAppDef(
                    appName = app.label,
                    displayName = app.label,
                    keywords = deriveKeywords(app.label, pkg),
                    color = 0xFF81A1C1,
                    packages = listOf(pkg),
                    discovered = true,
                    assistedSearch = false,
                    searchUrl = { "" },
                ),
                app,
                searchComponent = searchable.searchActivity ?: cn,
            )
        }
    }

    /** Component that actually receives [Intent.ACTION_SEARCH] for [packageName], if any. */
    fun findSearchComponent(context: Context, packageName: String): ComponentName? {
        val pm = context.packageManager
        val sm = context.getSystemService(SearchManager::class.java) ?: return null
        for (ri in querySearchActivities(pm, packageName)) {
            val info = ri.activityInfo ?: continue
            if (info.name.contains("ResolverActivity", ignoreCase = true)) continue
            val cn = ComponentName(info.packageName, info.name)
            val searchable = runCatching { sm.getSearchableInfo(cn) }.getOrNull()
            if (searchable != null) return searchable.searchActivity ?: cn
        }
        packageInfo(pm, packageName)?.activities.orEmpty().forEach { activity ->
            val cn = ComponentName(packageName, activity.name)
            val searchable = runCatching { sm.getSearchableInfo(cn) }.getOrNull()
            if (searchable != null) return searchable.searchActivity ?: cn
            if (activity.metaData?.containsKey("android.app.searchable") == true) {
                return cn
            }
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun querySearchActivities(pm: PackageManager, packageName: String): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_SEARCH).setPackage(packageName)
        return runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )
            } else {
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
        }.getOrDefault(emptyList())
    }

    @Suppress("DEPRECATION")
    private fun packageInfo(pm: PackageManager, packageName: String): PackageInfo? {
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
        return runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(packageName, flags)
            }
        }.getOrNull()
    }

    private fun looksLikeMediaOnly(packageName: String, label: String): Boolean {
        val blob = "$packageName $label".lowercase()
        return listOf(
            "camera", "gallery", "photos", "photo", "camcorder", "recorder",
            "scan", "scanner", "documentui", "files", "download",
        ).any { blob.contains(it) }
    }

    fun deriveKeywords(label: String, packageName: String): List<String> {
        val clean = label.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()
        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        val joined = words.joinToString("")
        val out = linkedSetOf<String>()
        fun addPrefixes(s: String, max: Int = 12) {
            for (i in 2..s.length.coerceAtMost(max)) out += s.take(i)
        }
        addPrefixes(joined)
        words.forEach { addPrefixes(it, 10) }
        if (words.size >= 2) {
            val initials = words.mapNotNull { it.firstOrNull()?.toString() }.joinToString("")
            if (initials.length >= 2) out += initials
        }
        packageName.substringAfterLast('.').takeIf { it.length >= 2 }?.let { addPrefixes(it, 10) }
        return out.toList()
    }

    private fun enc(q: String) = Uri.encode(q)
}
