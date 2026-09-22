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
        if (text.isBlank()) return openComposer(context, packageName)
        val pkg = packageName.lowercase()
        when {
            pkg.contains("twitter") || pkg == "com.x.android" || pkg.startsWith("com.x.android.") -> if (twitterCompose(context, packageName, text)) return PostResult.Started
            pkg.contains("linkedin") -> if (linkedInCompose(context, packageName, text)) return PostResult.Started
            pkg.contains("reddit") -> if (redditCompose(context, packageName, text)) return PostResult.Started
        }
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
        val pkg = packageName.lowercase()
        if (isFacebook(pkg) || isInstagram(pkg)) {
            return openMetaComposer(context, packageName)
        }
        when {
            pkg.contains("twitter") || pkg == "com.x.android" || pkg.startsWith("com.x.android.") -> if (twitterCompose(context, packageName, "")) return PostResult.Started
            pkg.contains("linkedin") -> if (linkedInCompose(context, packageName, "")) return PostResult.Started
            pkg.contains("reddit") -> if (redditCompose(context, packageName, "")) return PostResult.Started
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

    private fun twitterCompose(context: Context, packageName: String, text: String): Boolean {
        val uri = Uri.parse("https://twitter.com/intent/tweet").buildUpon()
        if (text.isNotBlank()) uri.appendQueryParameter("text", text)
        return start(context, Intent(Intent.ACTION_VIEW, uri.build()).setPackage(packageName))
    }

    private fun linkedInCompose(context: Context, packageName: String, text: String): Boolean {
        val uri = Uri.parse("https://www.linkedin.com/shareArticle").buildUpon()
            .appendQueryParameter("mini", "true")
        if (text.isNotBlank()) uri.appendQueryParameter("summary", text)
        return start(context, Intent(Intent.ACTION_VIEW, uri.build()).setPackage(packageName))
    }

    private fun redditCompose(context: Context, packageName: String, text: String): Boolean {
        val uri = Uri.parse("https://www.reddit.com/submit").buildUpon()
        if (text.isNotBlank()) uri.appendQueryParameter("title", text)
        return start(context, Intent(Intent.ACTION_VIEW, uri.build()).setPackage(packageName))
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
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        clip?.setPrimaryClip(android.content.ClipData.newPlainText("Post", text))
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
