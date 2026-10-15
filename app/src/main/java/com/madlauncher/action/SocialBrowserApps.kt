package com.madlauncher.action

/**
 * Apps whose @ chat or : post can open in one reused browser tab.
 * Social apps default off. Messaging apps that already open a chat default off.
 * Messaging apps that do not open a chat default on.
 */
object SocialBrowserApps {
    enum class Kind { Social, Messaging, Supported }

    /** Off, every command, posts only, or DMs only. */
    enum class BrowserMode { OFF, ON, POST, MSG }

    enum class BrowserCommand { POST, MSG }

    data class App(
        val id: String,
        val label: String,
        val kind: Kind,
        /** Messaging apps whose in-app chat failed start fully on. */
        val defaultOn: Boolean,
        val webUrl: String?,
        /** Facebook hands @ to Messenger, so X-style DM choice is hidden. */
        val separateMessenger: Boolean = false,
        /** `:` already opens a composer in the app, so it is not a browser choice. */
        val postsInApp: Boolean = false,
    ) {
        val post: Boolean get() = kind == Kind.Social
        val msg: Boolean get() = kind == Kind.Social || kind == Kind.Messaging
    }

    data class Custom(
        val packageName: String,
        val label: String,
        val url: String,
        val mode: BrowserMode = BrowserMode.ON,
    ) {
        val enabled: Boolean get() = mode != BrowserMode.OFF
    }

    private val social = listOf(
        App("x", "X", Kind.Social, false, "https://x.com/messages", postsInApp = true),
        App("instagram", "Instagram", Kind.Social, false, "https://www.instagram.com/direct/inbox/"),
        App("threads", "Threads", Kind.Social, false, "https://www.threads.net/"),
        App("linkedin", "LinkedIn", Kind.Social, false, "https://www.linkedin.com/messaging/"),
        App("reddit", "Reddit", Kind.Social, false, "https://www.reddit.com/chat"),
        App("tiktok", "TikTok", Kind.Social, false, "https://www.tiktok.com/messages"),
        App("truth", "Truth Social", Kind.Social, false, "https://truthsocial.com/conversations"),
        App("discord", "Discord", Kind.Social, false, "https://discord.com/channels/@me"),
        App("bluesky", "Bluesky", Kind.Social, false, "https://bsky.app/messages"),
        App("mastodon", "Mastodon", Kind.Social, false, "https://mastodon.social/"),
        App("tumblr", "Tumblr", Kind.Social, false, "https://www.tumblr.com/messaging"),
        App("pinterest", "Pinterest", Kind.Social, false, "https://www.pinterest.com/"),
        App("weibo", "Weibo", Kind.Social, false, "https://weibo.com/"),
        App("vk", "VK", Kind.Social, false, "https://vk.com/im"),
    )

    /** Chats that already open on a contact stay off. The rest start on. */
    private val messaging = listOf(
        App("whatsapp", "WhatsApp", Kind.Messaging, false, "https://web.whatsapp.com/"),
        App("telegram", "Telegram", Kind.Messaging, false, "https://web.telegram.org/a/"),
        App("signal", "Signal", Kind.Messaging, false, null),
        App("messages", "Messages", Kind.Messaging, false, "https://messages.google.com/web/conversations"),
        App("gmail", "Gmail", Kind.Messaging, false, "https://mail.google.com/"),
        App("outlook", "Outlook", Kind.Messaging, false, "https://outlook.live.com/mail/"),
        App("slack", "Slack", Kind.Messaging, true, "https://app.slack.com/"),
        App("textnow", "TextNow", Kind.Messaging, true, "https://www.textnow.com/messaging"),
    )

    /** Launcher shortcuts that may never have been installed. Off until the user turns one on. */
    private val supported = listOf(
        App("youtube", "YouTube", Kind.Supported, false, "https://www.youtube.com/"),
        App("ytmusic", "YouTube Music", Kind.Supported, false, "https://music.youtube.com/"),
        App("maps", "Maps", Kind.Supported, false, "https://www.google.com/maps"),
        App("photos", "Photos", Kind.Supported, false, "https://photos.google.com/"),
        App("drive", "Drive", Kind.Supported, false, "https://drive.google.com/"),
        App("calendar", "Calendar", Kind.Supported, false, "https://calendar.google.com/"),
        App("play", "Play Store", Kind.Supported, false, "https://play.google.com/store"),
        App("keep", "Keep", Kind.Supported, false, "https://keep.google.com/"),
        App("gemini", "Gemini", Kind.Supported, false, "https://gemini.google.com/"),
        App("translate", "Translate", Kind.Supported, false, "https://translate.google.com/"),
        App("spotify", "Spotify", Kind.Supported, false, "https://open.spotify.com/"),
        App("netflix", "Netflix", Kind.Supported, false, "https://www.netflix.com/"),
        App("chatgpt", "ChatGPT", Kind.Supported, false, "https://chatgpt.com/"),
        App("amazon", "Amazon", Kind.Supported, false, "https://www.amazon.com/"),
        App("twitch", "Twitch", Kind.Supported, false, "https://www.twitch.tv/"),
    )

    val all: List<App> = social + messaging + supported
    val socialApps: List<App> = social
    val messagingApps: List<App> = messaging
    val supportedApps: List<App> = supported
    val ids: Set<String> = all.map { it.id }.toSet()
    val socialIds: Set<String> = social.map { it.id }.toSet()

    fun idFor(packageName: String): String? {
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("youtube.music") || pkg.contains("apps.youtube.music") -> "ytmusic"
            pkg == "com.google.android.youtube" || pkg.contains(".youtube") -> "youtube"
            pkg.contains("apps.maps") -> "maps"
            pkg.contains("apps.photos") -> "photos"
            pkg.contains("apps.docs") -> "drive"
            pkg.contains("android.calendar") || pkg.contains(".calendar") -> "calendar"
            pkg == "com.android.vending" -> "play"
            pkg.contains("android.keep") -> "keep"
            pkg.contains("apps.bard") -> "gemini"
            pkg.contains("apps.translate") -> "translate"
            pkg.contains("spotify") -> "spotify"
            pkg.contains("netflix") -> "netflix"
            pkg.contains("openai.chatgpt") -> "chatgpt"
            pkg.contains("mshop.android") -> "amazon"
            pkg.contains("twitch") -> "twitch"
            pkg.contains("telegram") -> "telegram"
            pkg.contains("whatsapp") -> "whatsapp"
            pkg.contains("thoughtcrime") || pkg.contains("securesms") -> "signal"
            pkg.contains("facebook.orca") || pkg.contains("facebook.mlite") -> "messenger"
            pkg.contains("apps.messaging") || pkg == "com.android.mms" ||
                pkg.contains("samsung.android.messaging") -> "messages"
            pkg == "com.google.android.gm" || pkg.startsWith("com.google.android.gm.") -> "gmail"
            pkg.contains("office.outlook") -> "outlook"
            pkg.contains("viber") -> "viber"
            pkg.contains("slack") -> "slack"
            pkg.contains("skype") -> "skype"
            pkg.contains("tencent.mm") || pkg.contains("wechat") -> "wechat"
            pkg.contains("naver.line") -> "line"
            pkg.contains("kakao.talk") -> "kakao"
            pkg.contains("threema") -> "threema"
            pkg == "com.wire" || pkg.startsWith("com.wire.") -> "wire"
            pkg.contains("vector.app") || pkg.contains("element.android") -> "element"
            pkg.contains("loki.messenger") -> "session"
            pkg.contains("textnow") -> "textnow"
            pkg.contains("twitter") || pkg == "com.x.android" || pkg.startsWith("com.x.android.") -> "x"
            pkg.contains("facebook.katana") || pkg.contains("facebook.lite") -> "facebook"
            pkg.contains("barcelona") -> "threads"
            pkg.contains("instagram") -> "instagram"
            pkg.contains("linkedin") -> "linkedin"
            pkg.contains("reddit") -> "reddit"
            pkg.contains("tiktok") || pkg.contains("zhiliaoapp") || pkg.contains("ugc.trill") -> "tiktok"
            pkg.contains("truthsocial") -> "truth"
            pkg.contains("discord") -> "discord"
            pkg.contains("snapchat") -> "snapchat"
            pkg.contains("bluesky") || pkg.contains("bsky") -> "bluesky"
            pkg.contains("mastodon") -> "mastodon"
            pkg.contains("tumblr") -> "tumblr"
            pkg.contains("pinterest") -> "pinterest"
            pkg.contains("weibo") -> "weibo"
            pkg.contains("vkontakte") || pkg.contains(".vk.") -> "vk"
            else -> null
        }
    }

    fun options(app: App): List<BrowserMode> {
        if (app.kind == Kind.Supported) return listOf(BrowserMode.OFF, BrowserMode.ON)
        val modes = mutableListOf(BrowserMode.OFF, BrowserMode.ON)
        if (app.post && (!app.postsInApp || app.id == "x")) modes += BrowserMode.POST
        if (app.msg) modes += BrowserMode.MSG
        return modes
    }

    fun customOptions(): List<BrowserMode> =
        listOf(BrowserMode.OFF, BrowserMode.ON, BrowserMode.POST, BrowserMode.MSG)

    fun defaultMode(app: App): BrowserMode = when {
        app.kind == Kind.Messaging && app.defaultOn -> BrowserMode.ON
        app.kind == Kind.Messaging -> BrowserMode.OFF
        app.separateMessenger -> BrowserMode.OFF
        app.msg -> BrowserMode.MSG
        else -> BrowserMode.OFF
    }

    fun parseMode(raw: String): BrowserMode? = when (raw.lowercase()) {
        "off" -> BrowserMode.OFF
        "on" -> BrowserMode.ON
        "post" -> BrowserMode.POST
        "msg" -> BrowserMode.MSG
        else -> null
    }

    fun mode(
        id: String,
        explicit: Map<String, BrowserMode>,
        enabledIds: Set<String>,
        disabledIds: Set<String>,
    ): BrowserMode {
        explicit[id]?.let { return it }
        val app = all.firstOrNull { it.id == id } ?: return BrowserMode.OFF
        if (id in disabledIds) return BrowserMode.OFF
        if (id in enabledIds) return BrowserMode.ON
        return defaultMode(app)
    }

    fun allows(mode: BrowserMode, command: BrowserCommand): Boolean = when (mode) {
        BrowserMode.OFF -> false
        BrowserMode.ON -> true
        BrowserMode.POST -> command == BrowserCommand.POST
        BrowserMode.MSG -> command == BrowserCommand.MSG
    }

    fun allowsApp(app: App, mode: BrowserMode, command: BrowserCommand): Boolean {
        if (command == BrowserCommand.POST && app.postsInApp && mode != BrowserMode.POST && mode != BrowserMode.ON) {
            return false
        }
        return allows(mode, command)
    }

    fun allowsCommand(
        packageName: String,
        command: BrowserCommand,
        explicit: Map<String, BrowserMode>,
        enabledIds: Set<String>,
        disabledIds: Set<String>,
        custom: List<Custom> = emptyList(),
    ): Boolean {
        if (idFor(packageName) == null) {
            val hit = custom.firstOrNull { it.packageName.equals(packageName, ignoreCase = true) } ?: return false
            return allows(hit.mode, command)
        }
        val id = idFor(packageName) ?: return false
        val app = all.firstOrNull { it.id == id } ?: return false
        return allowsApp(app, mode(id, explicit, enabledIds, disabledIds), command)
    }

    fun webUrl(packageName: String, custom: List<Custom> = emptyList()): String? {
        if (idFor(packageName) == null) {
            return custom.firstOrNull { it.packageName.equals(packageName, ignoreCase = true) }
                ?.url
                ?.takeIf { it.isNotBlank() }
        }
        val id = idFor(packageName) ?: return null
        return all.firstOrNull { it.id == id }?.webUrl?.takeIf { it.isNotBlank() }
    }
}
