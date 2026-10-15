package com.madlauncher.action

import android.content.Context

/**
 * Social DM inboxes that are not phone-number messengers.
 * `@` / `msg` jump here. In-app deep links are not working; the experimental
 * browser option opens the website instead. Space on those apps is still **post**.
 */
sealed class InboxOpenResult {
    data object Opened : InboxOpenResult()
    /** In-app DM links are parked under Not working. */
    data object NotWorking : InboxOpenResult()
    data object Failed : InboxOpenResult()
}

object SocialInbox {
    fun hasInbox(packageName: String): Boolean = urisFor(packageName).isNotEmpty()

    /** Website inbox. App schemes stay unused while in-app DMs are not working. */
    internal fun webUrl(packageName: String): String? =
        urisFor(packageName).firstOrNull { it.startsWith("https://") }

    /**
     * In-app DM deep links do not work. With the experimental browser option on,
     * the https inbox opens in one reused browser tab.
     */
    fun open(context: Context, packageName: String): InboxOpenResult {
        val pkg = packageName.trim()
        if (pkg.isBlank() || !hasInbox(pkg)) return InboxOpenResult.Failed
        if (!ReusedBrowser.enabled(context, pkg, SocialBrowserApps.BrowserCommand.MSG)) {
            return InboxOpenResult.NotWorking
        }
        val url = webUrl(pkg) ?: return InboxOpenResult.Failed
        return if (ReusedBrowser.open(context, url)) InboxOpenResult.Opened else InboxOpenResult.Failed
    }

    internal fun urisFor(packageName: String): List<String> {
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("twitter") || pkg == "com.x.android" || pkg.startsWith("com.x.android.") ->
                listOf(
                    "twitter://messages",
                    "twitter://direct_messages",
                    "https://x.com/messages",
                    "https://twitter.com/messages",
                )
            pkg.contains("instagram.barcelona") ->
                listOf(
                    "barcelona://direct_inbox",
                    "https://www.threads.net/direct/inbox",
                )
            pkg.contains("instagram") ->
                listOf(
                    "instagram://direct-inbox",
                    "instagram://direct",
                    "https://www.instagram.com/direct/inbox/",
                )
            pkg.contains("facebook.katana") || pkg.contains("facebook.lite") ->
                listOf(
                    "fb://messaging",
                    "fb://page_inbox",
                    "fb://messages",
                    "https://www.messenger.com/",
                )
            pkg.contains("linkedin") ->
                listOf(
                    "linkedin://messaging",
                    "https://www.linkedin.com/messaging/",
                )
            pkg.contains("zhiliaoapp") || pkg.contains("musically") || pkg.contains("ugc.trill") ->
                listOf(
                    "snssdk1233://message",
                    "tiktok://messages",
                    "https://www.tiktok.com/messages",
                )
            pkg.contains("snapchat") ->
                listOf(
                    "snapchat://chat",
                    "https://www.snapchat.com/add",
                )
            pkg.contains("reddit") ->
                listOf(
                    "reddit://chat",
                    "https://www.reddit.com/chat",
                )
            pkg.contains("truthsocial") ->
                listOf("https://truthsocial.com/conversations")
            pkg.contains("bluesky") || pkg.contains("bsky") ->
                listOf("https://bsky.app/messages")
            pkg.contains("mastodon") ->
                listOf("https://mastodon.social/conversations")
            pkg.contains("tumblr") ->
                listOf("https://www.tumblr.com/messaging")
            pkg.contains("discord") ->
                listOf("https://discord.com/channels/@me")
            pkg.contains("pinterest") ->
                listOf("https://www.pinterest.com/messages/")
            pkg.contains("weibo") ->
                listOf("https://weibo.com/message")
            pkg.contains("vkontakte") || pkg.contains(".vk.") ->
                listOf("https://vk.com/im")
            else -> emptyList()
        }
    }
}

/** Space addon for a Your-command / newly installed app when it is not in the common catalog. */
object AppAddonGuess {
    fun forPackage(packageName: String, label: String = ""): SpaceAddon {
        CommonAppShortcuts.byPackage(packageName)?.addon?.let { return it }
        return when {
            ActionApps.looksLikeSocial(label, packageName) -> SpaceAddon.POST
            ActionApps.looksLikeMessaging(label, packageName) -> SpaceAddon.CHAT
            ActionApps.looksLikeEmail(label, packageName) -> SpaceAddon.EMAIL
            ActionApps.looksLikeNav(label, packageName) -> SpaceAddon.NAV
            ActionApps.looksLikeAi(label, packageName) -> SpaceAddon.AI
            ActionApps.looksLikeNotes(label, packageName) -> SpaceAddon.NOTE
            ActionApps.looksLikeTranslator(label, packageName) -> SpaceAddon.TRANSLATE
            ActionApps.looksLikeContacts(label, packageName) -> SpaceAddon.CONTACTS
            else -> SpaceAddon.SEARCH
        }
    }
}
