package com.madlauncher.action

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.madlauncher.data.LaunchableApp
import com.madlauncher.platform.DeviceOsInfo

enum class SpaceQueryKind { SEARCH, NAV }

data class SpaceCapableApp(
    val app: LaunchableApp,
    val kind: SpaceQueryKind,
    val phrases: List<String>,
)

data class SearchableAppRequest(
    val appLabel: String,
    val packageName: String,
    val appVersion: String,
    val appCategory: String,
    val handlesGeo: Boolean,
    val handlesGoogleNav: Boolean,
    val hasSearchableInfo: Boolean,
    val curatedSearchUrl: Boolean,
    val assistedSearch: Boolean,
    val commonShortcutId: String?,
    val guessedAddon: String,
    val madVersion: String,
    val device: DeviceOsInfo.Snapshot,
    val probedAtMillis: Long,
) {
    fun fileBase(): String {
        val safePkg = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(48)
        return "searchable-$safePkg-$probedAtMillis"
    }

    fun toPlainText(): String = buildString {
        appendLine("MAD Launcher — Searchable app request")
        appendLine("MAD version: $madVersion")
        appendLine("App: $appLabel")
        appendLine("Package: $packageName")
        appendLine("App version: $appVersion")
        appendLine("Play/appCategory: $appCategory")
        appendLine("Handles geo: $handlesGeo")
        appendLine("Handles google.navigation: $handlesGoogleNav")
        appendLine("SearchableInfo: $hasSearchableInfo")
        appendLine("Curated search URL: $curatedSearchUrl")
        appendLine("Assisted (clipboard) search: $assistedSearch")
        appendLine("Common shortcut: ${commonShortcutId ?: "none"}")
        appendLine("Guessed Space addon: $guessedAddon")
        appendLine()
        appendLine("=== Device ===")
        appendLine("${device.manufacturer} ${device.model}")
        appendLine("Android ${device.androidRelease} (API ${device.androidSdk})")
        appendLine("ROM: ${device.oemFamily.displayName} / ${device.nativeOsName} ${device.nativeOsVersion}")
    }

    fun toJson(): String {
        return """
            {
              "madVersion": ${jsonStr(madVersion)},
              "appLabel": ${jsonStr(appLabel)},
              "packageName": ${jsonStr(packageName)},
              "appVersion": ${jsonStr(appVersion)},
              "appCategory": ${jsonStr(appCategory)},
              "handlesGeo": $handlesGeo,
              "handlesGoogleNav": $handlesGoogleNav,
              "hasSearchableInfo": $hasSearchableInfo,
              "curatedSearchUrl": $curatedSearchUrl,
              "assistedSearch": $assistedSearch,
              "commonShortcutId": ${commonShortcutId?.let { jsonStr(it) } ?: "null"},
              "guessedAddon": ${jsonStr(guessedAddon)},
              "probedAtMillis": $probedAtMillis,
              "device": {
                "manufacturer": ${jsonStr(device.manufacturer)},
                "model": ${jsonStr(device.model)},
                "androidRelease": ${jsonStr(device.androidRelease)},
                "androidSdk": ${device.androidSdk},
                "oemFamily": ${jsonStr(device.oemFamily.name)}
              }
            }
        """.trimIndent()
    }

    private fun jsonStr(value: String): String =
        "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

/**
 * Apps MAD can search or navigate without a per-title recipe:
 * curated search URLs (including social post lookup, even when Space is POST /
 * clipboard-assisted), common SEARCH shortcuts, or geo/nav intent filters.
 * Android SearchableInfo alone is not enough (AdAway and similar false positives).
 */
object AppSpaceCapability {
    fun partition(
        context: Context,
        apps: List<LaunchableApp>,
        searchable: List<ResolvedSearchApp>,
        selfPackage: String,
    ): Pair<List<SpaceCapableApp>, List<LaunchableApp>> {
        val byPkg = searchable.associateBy { it.app.packageName }
        val reliable = ArrayList<SpaceCapableApp>()
        val rest = ArrayList<LaunchableApp>()
        val seen = HashSet<String>()
        for (app in apps) {
            val pkg = app.packageName
            if (pkg.equals(selfPackage, ignoreCase = true)) continue
            if (!seen.add(pkg)) continue
            val kind = classify(context, app, byPkg[pkg])
            if (kind != null) {
                reliable += SpaceCapableApp(app, kind, phrasesFor(pkg, byPkg[pkg]))
            } else {
                rest += app
            }
        }
        val sortedReliable = reliable.sortedWith(
            compareBy<SpaceCapableApp> { it.kind != SpaceQueryKind.SEARCH }
                .thenBy { it.app.label.lowercase() },
        )
        val sortedRest = rest.sortedBy { it.label.lowercase() }
        return sortedReliable to sortedRest
    }

    fun classify(
        context: Context,
        app: LaunchableApp,
        resolved: ResolvedSearchApp?,
    ): SpaceQueryKind? {
        val pkg = app.packageName
        val handlesGeo = handlesView(context, pkg, "geo:0,0?q=test")
        val handlesGoogleNav = handlesView(context, pkg, "google.navigation:q=test")
        val category = appCategory(context, pkg)
        if (isReliableNav(pkg, handlesGeo, handlesGoogleNav, category)) {
            return SpaceQueryKind.NAV
        }
        val def = resolved?.def ?: SearchableApps.catalog.firstOrNull { pkg in it.packages }
        val common = CommonAppShortcuts.byPackage(pkg)
        val curated = def?.let { runCatching { it.searchUrl("q") }.getOrDefault("") }.orEmpty()
        val assisted = def?.assistedSearch == true
        val hasInfo = resolved?.searchComponent != null ||
            SearchableApps.findSearchComponent(context, pkg) != null
        if (isReliableSearch(curated, assisted, hasInfo, common?.addon)) {
            return SpaceQueryKind.SEARCH
        }
        return null
    }

    fun isReliableNav(
        packageName: String,
        handlesGeo: Boolean,
        handlesGoogleNav: Boolean,
        appCategory: Int = ApplicationInfo.CATEGORY_UNDEFINED,
    ): Boolean {
        if (handlesGeo || handlesGoogleNav) return true
        if (packageName.contains("waze", ignoreCase = true)) return true
        if (appCategory == ApplicationInfo.CATEGORY_MAPS) return true
        return false
    }

    fun isReliableSearch(
        curatedUrl: String,
        assisted: Boolean,
        @Suppress("UNUSED_PARAMETER") hasSearchableInfo: Boolean,
        commonAddon: SpaceAddon?,
    ): Boolean {
        if (commonAddon == SpaceAddon.NAV) return false
        if (curatedUrl.isNotBlank()) return true
        if (assisted) return false
        if (commonAddon == SpaceAddon.SEARCH) return true
        return false
    }

    fun buildRequest(context: Context, app: LaunchableApp, madVersion: String): SearchableAppRequest {
        val pkg = app.packageName
        val resolved = SearchableApps.catalog.firstOrNull { pkg in it.packages }
        val common = CommonAppShortcuts.byPackage(pkg)
        val curated = resolved?.let { runCatching { it.searchUrl("q") }.getOrDefault("") }.orEmpty()
        return SearchableAppRequest(
            appLabel = app.label,
            packageName = pkg,
            appVersion = packageVersion(context, pkg),
            appCategory = categoryLabel(appCategory(context, pkg)),
            handlesGeo = handlesView(context, pkg, "geo:0,0?q=test"),
            handlesGoogleNav = handlesView(context, pkg, "google.navigation:q=test"),
            hasSearchableInfo = SearchableApps.findSearchComponent(context, pkg) != null,
            curatedSearchUrl = curated.isNotBlank(),
            assistedSearch = resolved?.assistedSearch == true,
            commonShortcutId = common?.id,
            guessedAddon = AppAddonGuess.forPackage(pkg, app.label).name,
            madVersion = madVersion,
            device = DeviceOsInfo.capture(context),
            probedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun phrasesFor(packageName: String, resolved: ResolvedSearchApp?): List<String> {
        CommonAppShortcuts.byPackage(packageName)?.phrases?.take(3)?.let { if (it.isNotEmpty()) return it }
        resolved?.def?.keywords?.take(3)?.let { if (it.isNotEmpty()) return it }
        return emptyList()
    }

    fun handlesView(context: Context, packageName: String, uri: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(packageName)
        return queryInPackage(context, intent, packageName)
    }

    private fun queryInPackage(context: Context, intent: Intent, packageName: String): Boolean {
        val pm = context.packageManager
        val hits = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        }.getOrDefault(emptyList())
        return hits.any { info ->
            val pkg = info.activityInfo?.packageName ?: return@any false
            pkg.equals(packageName, ignoreCase = true) &&
                !info.activityInfo.name.contains("ResolverActivity", ignoreCase = true)
        }
    }

    private fun appCategory(context: Context, packageName: String): Int {
        val info = runCatching {
            context.packageManager.getApplicationInfo(packageName, 0)
        }.getOrNull() ?: return ApplicationInfo.CATEGORY_UNDEFINED
        return info.category
    }

    private fun packageVersion(context: Context, packageName: String): String {
        return runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0),
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0).versionName
            }
        }.getOrNull().orEmpty()
    }

    internal fun categoryLabel(category: Int): String = when (category) {
        ApplicationInfo.CATEGORY_GAME -> "GAME"
        ApplicationInfo.CATEGORY_AUDIO -> "AUDIO"
        ApplicationInfo.CATEGORY_VIDEO -> "VIDEO"
        ApplicationInfo.CATEGORY_IMAGE -> "IMAGE"
        ApplicationInfo.CATEGORY_SOCIAL -> "SOCIAL"
        ApplicationInfo.CATEGORY_NEWS -> "NEWS"
        ApplicationInfo.CATEGORY_MAPS -> "MAPS"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "PRODUCTIVITY"
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> "ACCESSIBILITY"
        else -> "UNDEFINED"
    }
}
