package com.madlauncher.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Browser
import com.madlauncher.data.LocalStore

/**
 * Opens an https page in a real browser and asks that browser to reuse one tab.
 *
 * [Browser.EXTRA_APPLICATION_ID] is the contract Chrome, Samsung Internet, and Firefox
 * use to find the tab this app already opened and navigate it, instead of stacking
 * a new tab on every launch. The target package is a browser so social apps cannot
 * claim the link and bounce it into the app.
 */
object ReusedBrowser {
    /** Same id for every social browser open, so they all share one tab. */
    internal const val TAB_ID_SUFFIX = ".mad.social.tab"

    /** Chrome reads this extra; false keeps the app-id tab instead of adding another. */
    private const val EXTRA_CREATE_NEW_TAB = "create_new_tab"

    fun enabled(
        context: Context,
        packageName: String,
        command: SocialBrowserApps.BrowserCommand,
    ): Boolean =
        runCatching {
            val settings = LocalStore(context).appearanceSettings
            SocialBrowserApps.allowsCommand(
                packageName,
                command,
                settings.experimentalSocialBrowserMode,
                settings.experimentalSocialBrowserApps,
                settings.experimentalSocialBrowserOff,
                settings.customBrowserApps,
            )
        }.getOrDefault(false)

    /** Opens the website when this app's @ command is set to the browser. */
    fun openIfEnabled(context: Context, packageName: String): Boolean {
        val settings = runCatching { LocalStore(context).appearanceSettings }.getOrNull() ?: return false
        if (!SocialBrowserApps.allowsCommand(
                packageName,
                SocialBrowserApps.BrowserCommand.MSG,
                settings.experimentalSocialBrowserMode,
                settings.experimentalSocialBrowserApps,
                settings.experimentalSocialBrowserOff,
                settings.customBrowserApps,
            )
        ) {
            return false
        }
        val url = SocialBrowserApps.webUrl(packageName, settings.customBrowserApps) ?: return false
        return open(context, url)
    }

    fun open(context: Context, url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        if (uri.scheme != "https" && uri.scheme != "http") return false
        val browser = browserPackage(context) ?: return false
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(browser)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
            )
            val tabId = context.packageName + TAB_ID_SUFFIX
            putExtra(Browser.EXTRA_APPLICATION_ID, tabId)
            putExtra(EXTRA_CREATE_NEW_TAB, false)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    internal fun browserPackage(context: Context): String? {
        val pm = context.packageManager
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val resolved = runCatching {
            pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        }.getOrNull()
        if (isBrowserPackage(context, resolved)) return resolved
        val installed = runCatching {
            pm.queryIntentActivities(probe, PackageManager.MATCH_ALL)
                .map { it.activityInfo.packageName }
                .distinct()
        }.getOrDefault(emptyList())
        val known = listOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.brave.browser",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.duckduckgo.mobile.android",
            "com.android.browser",
        )
        return known.firstOrNull { it in installed }
            ?: installed.firstOrNull { isBrowserPackage(context, it) }
    }

    private fun isBrowserPackage(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName == "android" || packageName == context.packageName) return false
        return true
    }
}
