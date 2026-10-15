package com.madlauncher.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.madlauncher.a11y.OttSocialPostBridge
import com.madlauncher.a11y.OttSocialSendLearn
import com.madlauncher.data.PermissionChecker

/** Routes Action Box `!` — open the app composer (Facebook/Instagram: tap the field, no fill). */
object SocialPostBridge {
    fun place(context: Context, app: InstalledActionApp, body: String): PostResult =
        place(context, app.packageName, body)

    fun place(context: Context, packageName: String, body: String): PostResult {
        val text = body.trim()
        if (isX(packageName)) return openXPost(context, packageName, text)
        if (parkedInApp(packageName)) return parkedPost(context, packageName, text)
        if (text.isBlank()) return openComposer(context, packageName)
        if (needsA11yCompose(context, packageName) && PermissionChecker.isAccessibilityEnabled(context)) {
            OttSocialPostBridge.begin(context, packageName, text)
            return PostResult.Automating
        }
        if (genericShare(context, packageName, text)) {
            OttSocialSendLearn.watch(context, packageName, text)
            return PostResult.Started
        }
        copyAndLaunch(context, packageName, text)
        return PostResult.Started
    }

    /** `!` after picking an app: land on the compose field. Does not insert or send. */
    fun openComposer(context: Context, packageName: String): PostResult {
        if (isX(packageName)) return openXPost(context, packageName, "")
        if (parkedInApp(packageName)) return parkedPost(context, packageName, "")
        val pkg = packageName.lowercase()
        if (isFacebook(pkg) || isInstagram(pkg)) {
            return openMetaComposer(context, packageName)
        }
        if (PermissionChecker.isAccessibilityEnabled(context) && usesA11yCompose(packageName)) {
            OttSocialPostBridge.begin(context, packageName, "")
            if (OttSocialPostBridge.pending != null) return PostResult.Automating
        }
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return PostResult.Failed
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launch)
            PostResult.Started
        }.getOrDefault(PostResult.Failed)
    }

    /**
     * Facebook MAIN lands on Reels; Instagram camera URIs open Stories.
     * Prefer a dedicated composer when one exists, then always drive a11y to the field.
     */
    private fun openMetaComposer(context: Context, packageName: String): PostResult {
        val pkg = packageName.lowercase()
        val dedicated = when {
            isInstagram(pkg) -> false
            else -> facebookComposer(context, packageName)
        }
        if (PermissionChecker.isAccessibilityEnabled(context)) {
            OttSocialPostBridge.begin(context, packageName, "", launchApp = !dedicated)
            if (OttSocialPostBridge.pending != null) return PostResult.Automating
        }
        if (dedicated) return PostResult.Started
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return PostResult.Failed
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launch)
            PostResult.Started
        }.getOrDefault(PostResult.Failed)
    }

    internal fun isTruthSocial(packageName: String): Boolean =
        packageName.lowercase().contains("truthsocial")

    /**
     * In-app Space posts for these networks do not work.
     * With the experimental browser option on, [webComposeUrl] opens instead.
     */
    internal fun parkedInApp(packageName: String): Boolean =
        !isX(packageName) && webComposeUrl(packageName, "") != null

    /** Website composer. Null means the in-app composer is still used (X, LinkedIn, Reddit). */
    internal fun webComposeUrl(packageName: String, text: String): String? {
        val pkg = packageName.lowercase()
        val body = text.trim()
        val encoded = if (body.isEmpty()) null else encodePost(body)
        return when {
            pkg.contains("truthsocial") ->
                if (encoded == null) "https://truthsocial.com/compose" else "https://truthsocial.com/share?text=$encoded"
            isFacebook(pkg) ->
                if (encoded == null) "https://m.facebook.com/composer" else "https://m.facebook.com/composer?text=$encoded"
            pkg.contains("barcelona") || pkg.contains("threads") ->
                if (encoded == null) "https://www.threads.net/" else "https://www.threads.net/intent/post?text=$encoded"
            isInstagram(pkg) -> "https://www.instagram.com/"
            pkg.contains("tiktok") || pkg.contains("zhiliaoapp") || pkg.contains("ugc.trill") ->
                "https://www.tiktok.com/tiktokstudio/upload"
            pkg.contains("discord") -> "https://discord.com/channels/@me"
            pkg.contains("snapchat") -> "https://web.snapchat.com/"
            pkg.contains("bluesky") || pkg == "xyz.blueskyweb.app" || pkg.contains("bsky") ->
                if (encoded == null) "https://bsky.app/intent/compose" else "https://bsky.app/intent/compose?text=$encoded"
            pkg.contains("mastodon") ->
                if (encoded == null) "https://mastodon.social/share" else "https://mastodon.social/share?text=$encoded"
            pkg.contains("tumblr") ->
                if (encoded == null) "https://www.tumblr.com/new/text" else "https://www.tumblr.com/new/text?body=$encoded"
            pkg.contains("pinterest") ->
                if (encoded == null) "https://www.pinterest.com/pin-creation-tool/" else "https://www.pinterest.com/pin/create/button/?description=$encoded"
            isX(pkg) ->
                if (encoded == null) "https://x.com/intent/post" else "https://x.com/intent/post?text=$encoded"
            pkg.contains("linkedin") ->
                if (encoded == null) "https://www.linkedin.com/feed/?shareActive=true" else "https://www.linkedin.com/feed/?shareActive=true&text=$encoded"
            pkg.contains("reddit") ->
                if (encoded == null) "https://www.reddit.com/submit" else "https://www.reddit.com/submit?title=$encoded"
            pkg.contains("weibo") ->
                if (encoded == null) "https://weibo.com/" else "https://service.weibo.com/share/share.php?title=$encoded"
            pkg.contains("vkontakte") || pkg.contains(".vk.") ->
                if (encoded == null) "https://vk.com/" else "https://vk.com/share.php?comment=$encoded"
            else -> null
        }
    }

    internal fun truthWebUrl(text: String): String =
        webComposeUrl("com.truthsocial.android.app", text)!!

    internal fun webUrlCarriesText(url: String): Boolean =
        url.contains("text=") || url.contains("body=") || url.contains("description=") ||
            url.contains("title=") || url.contains("comment=")

    private fun encodePost(body: String): String =
        java.net.URLEncoder.encode(body, Charsets.UTF_8.name()).replace("+", "%20")

    private fun parkedPost(context: Context, packageName: String, text: String): PostResult {
        if (!ReusedBrowser.enabled(context, packageName, SocialBrowserApps.BrowserCommand.POST)) {
            return PostResult.NotWorking
        }
        val url = webComposeUrl(packageName, text) ?: return PostResult.Failed
        val copied = text.isNotBlank() && !webUrlCarriesText(url)
        if (copied) copyPost(context, text)
        if (!ReusedBrowser.open(context, url)) return PostResult.Failed
        return if (copied) PostResult.PasteManual else PostResult.Started
    }

    private fun copyPost(context: Context, text: String) {
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        clip?.setPrimaryClip(android.content.ClipData.newPlainText("Post", text))
    }

    internal fun isX(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg.contains("twitter") || pkg == "com.x.android" || pkg.startsWith("com.x.android.")
    }

    /** In the app unless Experimental → Open in browser is On or : post. */
    private fun openXPost(context: Context, packageName: String, text: String): PostResult {
        if (ReusedBrowser.enabled(context, packageName, SocialBrowserApps.BrowserCommand.POST)) {
            return parkedPost(context, packageName, text)
        }
        return openXComposer(context, packageName, text)
    }

    /** X's composer opens in the app. `@` DMs stay on the browser path. */
    private fun openXComposer(context: Context, packageName: String, text: String): PostResult {
        val body = text.trim()
        if (body.isNotEmpty()) {
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, body)
                .setPackage(packageName)
            if (start(context, send)) return PostResult.Started
        }
        val post = Intent(Intent.ACTION_VIEW, Uri.parse("twitter://post")).setPackage(packageName)
        if (start(context, post)) return PostResult.Started
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return PostResult.Failed
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launch)
            PostResult.Started
        }.getOrDefault(PostResult.Failed)
    }

    private fun isFacebook(pkg: String): Boolean =
        pkg.contains("facebook.katana") || pkg.contains("facebook.lite")

    private fun isInstagram(pkg: String): Boolean =
        pkg.contains("instagram") && !pkg.contains("barcelona")

    /** Facebook / Instagram reject SET_TEXT, paste, and share-fill from other apps. */
    internal fun metaBlocksOutsideInput(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg.contains("instagram") ||
            pkg.contains("facebook.katana") ||
            pkg.contains("facebook.lite") ||
            pkg.contains("instagram.barcelona")
    }

    internal fun usesA11yCompose(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg.contains("discord") ||
            pkg.contains("tiktok") ||
            pkg.contains("zhiliaoapp") ||
            pkg.contains("facebook") ||
            pkg.contains("instagram")
    }

    internal fun facebookComposerUris(): List<String> = listOf(
        "fb://composer",
        "fb://publishing/composer",
        "fb://facewebmodal/f?href=https://m.facebook.com/composer",
    )

    internal fun instagramCreateUris(): List<String> = listOf(
        "instagram://share",
        "instagram://library",
    )

    private fun facebookComposer(context: Context, packageName: String): Boolean {
        for (uri in facebookComposerUris()) {
            if (start(context, Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(packageName))) {
                return true
            }
        }
        return false
    }

    private fun needsA11yCompose(context: Context, packageName: String): Boolean {
        if (usesA11yCompose(packageName)) return true
        when (OttSocialSendLearn.socialSendOutcome(packageName)) {
            true -> return false
            false -> return true
            null -> Unit
        }
        return shareHandlerLooksWrong(context.packageManager, packageName)
    }

    private fun genericShare(context: Context, packageName: String, text: String): Boolean {
        if (shareHandlerLooksWrong(context.packageManager, packageName)) return false
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
            .setPackage(packageName)
        return start(context, send)
    }

    private fun shareHandlerLooksWrong(pm: PackageManager, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(packageName)
        val resolve = runCatching {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull() ?: return true
        val cls = resolve.activityInfo?.name?.lowercase().orEmpty()
        return cls.contains("directshare") ||
            cls.contains("resolver") ||
            cls.contains("chooser")
    }

    private fun copyAndLaunch(context: Context, packageName: String, text: String) {
        copyPost(context, text)
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(launch) }
    }

    private fun start(context: Context, intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
