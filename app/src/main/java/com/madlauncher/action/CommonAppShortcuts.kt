package com.madlauncher.action

/**
 * Bare-word app openers. Enter launches the app. Space opens an in-box addon
 * (calc keypad, in-app search, compose). `!gm cats` always searches the website
 * in the browser.
 */
enum class SpaceAddon {
    CALC,
    SEARCH,
    NAV,
    NOTE,
    CALL,
    CHAT,
    EMAIL,
    CONTACTS,
    REMINDER,
    SCHEDULE,
    TRANSLATE,
    AI,
    OPEN,
}

data class CommonAppShortcut(
    val id: String,
    val label: String,
    val phrases: List<String>,
    val packages: List<String>,
    val labels: List<String> = listOf(label),
    /** [android.content.Intent] CATEGORY_APP_* when set. */
    val category: String? = null,
    val addon: SpaceAddon,
)

object CommonAppShortcuts {
    const val CAT_BROWSER = "android.intent.category.APP_BROWSER"
    const val CAT_CALCULATOR = "android.intent.category.APP_CALCULATOR"
    const val CAT_CALENDAR = "android.intent.category.APP_CALENDAR"
    const val CAT_CONTACTS = "android.intent.category.APP_CONTACTS"
    const val CAT_EMAIL = "android.intent.category.APP_EMAIL"
    const val CAT_FILES = "android.intent.category.APP_FILES"
    const val CAT_GALLERY = "android.intent.category.APP_GALLERY"
    const val CAT_MAPS = "android.intent.category.APP_MAPS"
    const val CAT_MARKET = "android.intent.category.APP_MARKET"
    const val CAT_MESSAGING = "android.intent.category.APP_MESSAGING"

    val catalog: List<CommonAppShortcut> = listOf(
        CommonAppShortcut(
            id = "gmail",
            label = "Gmail",
            phrases = listOf("gm", "gmail"),
            packages = listOf("com.google.android.gm"),
            category = CAT_EMAIL,
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "maps",
            label = "Maps",
            phrases = listOf("maps", "gmaps"),
            packages = listOf("com.google.android.apps.maps"),
            labels = listOf("Maps", "Google Maps"),
            category = CAT_MAPS,
            addon = SpaceAddon.NAV,
        ),
        CommonAppShortcut(
            id = "youtube",
            label = "YouTube",
            phrases = listOf("yt", "youtube"),
            packages = listOf(
                "com.google.android.youtube",
                "app.morphe.android.youtube",
                "app.revanced.android.youtube",
            ),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "ytmusic",
            label = "YouTube Music",
            phrases = listOf("ytm", "ytmusic"),
            packages = listOf(
                "com.google.android.apps.youtube.music",
                "app.morphe.android.apps.youtube.music",
                "app.revanced.android.apps.youtube.music",
            ),
            labels = listOf("YouTube Music", "YT Music"),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "photos",
            label = "Photos",
            phrases = listOf("photos", "gp"),
            packages = listOf("com.google.android.apps.photos"),
            labels = listOf("Photos", "Gallery"),
            category = CAT_GALLERY,
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "drive",
            label = "Drive",
            phrases = listOf("drive", "gdrive"),
            packages = listOf("com.google.android.apps.docs"),
            labels = listOf("Drive", "Google Drive"),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "calendar",
            label = "Calendar",
            phrases = listOf("cal", "calendar"),
            packages = listOf(
                "com.google.android.calendar",
                "com.android.calendar",
                "com.samsung.android.calendar",
            ),
            category = CAT_CALENDAR,
            addon = SpaceAddon.SCHEDULE,
        ),
        CommonAppShortcut(
            id = "chrome",
            label = "Chrome",
            phrases = listOf("chrome", "chr"),
            packages = listOf("com.android.chrome", "com.chrome.android"),
            category = CAT_BROWSER,
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "play",
            label = "Play Store",
            phrases = listOf("play", "playstore"),
            packages = listOf("com.android.vending"),
            labels = listOf("Play Store", "Google Play"),
            category = CAT_MARKET,
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "contacts",
            label = "Contacts",
            phrases = listOf("contacts"),
            packages = listOf(
                "com.google.android.contacts",
                "com.android.contacts",
                "com.samsung.android.app.contacts",
                "com.samsung.android.contacts",
                "com.miui.contacts",
            ),
            category = CAT_CONTACTS,
            addon = SpaceAddon.CONTACTS,
        ),
        CommonAppShortcut(
            id = "messages",
            label = "Messages",
            phrases = listOf("sms", "messages"),
            packages = listOf(
                "com.google.android.apps.messaging",
                "com.android.mms",
                "com.samsung.android.messaging",
            ),
            category = CAT_MESSAGING,
            addon = SpaceAddon.CHAT,
        ),
        CommonAppShortcut(
            id = "phone",
            label = "Phone",
            phrases = listOf("phone", "dialer"),
            packages = listOf(
                "com.google.android.dialer",
                "com.android.dialer",
                "com.samsung.android.dialer",
            ),
            labels = listOf("Phone", "Dialer"),
            addon = SpaceAddon.CALL,
        ),
        CommonAppShortcut(
            id = "clock",
            label = "Clock",
            phrases = listOf("clock", "alarm"),
            packages = listOf(
                "com.google.android.deskclock",
                "com.android.deskclock",
                "com.sec.android.app.clockpackage",
            ),
            labels = listOf("Clock", "Alarm"),
            addon = SpaceAddon.REMINDER,
        ),
        CommonAppShortcut(
            id = "files",
            label = "Files",
            phrases = listOf("files", "explorer"),
            packages = listOf(
                "com.google.android.apps.nbu.files",
                "com.google.android.documentsui",
                "com.android.documentsui",
                "com.mi.android.globalFileexplorer",
                "com.samsung.android.app.myfiles",
            ),
            labels = listOf("Files", "My Files", "File Manager"),
            category = CAT_FILES,
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "keep",
            label = "Keep",
            phrases = listOf("keep", "notepad"),
            packages = listOf(
                "com.google.android.keep",
                "com.miui.notes",
                "com.samsung.android.app.notes",
            ),
            labels = listOf("Keep", "Notes", "Notepad"),
            addon = SpaceAddon.NOTE,
        ),
        CommonAppShortcut(
            id = "camera",
            label = "Camera",
            phrases = listOf("cam", "camera"),
            packages = listOf(
                "com.google.android.GoogleCamera",
                "com.android.camera2",
                "com.android.camera",
                "com.sec.android.app.camera",
                "org.codeaurora.snapcam",
            ),
            labels = listOf("Camera"),
            addon = SpaceAddon.OPEN,
        ),
        CommonAppShortcut(
            id = "gemini",
            label = "Gemini",
            phrases = listOf("gemini", "bard"),
            packages = listOf("com.google.android.apps.bard"),
            labels = listOf("Gemini", "Bard"),
            addon = SpaceAddon.AI,
        ),
        CommonAppShortcut(
            id = "weather",
            label = "Weather",
            phrases = listOf("weather"),
            packages = listOf("com.google.android.apps.weather"),
            labels = listOf("Weather"),
            addon = SpaceAddon.OPEN,
        ),
        CommonAppShortcut(
            id = "calculator",
            label = "Calculator",
            phrases = listOf("calc", "calculate", "calculator"),
            packages = listOf(
                "com.google.android.calculator",
                "com.android.calculator2",
                "com.miui.calculator",
                "com.sec.android.app.popupcalculator",
            ),
            category = CAT_CALCULATOR,
            addon = SpaceAddon.CALC,
        ),
        CommonAppShortcut(
            id = "translate",
            label = "Translate",
            phrases = listOf("translate"),
            packages = listOf("com.google.android.apps.translate"),
            labels = listOf("Translate", "Google Translate"),
            addon = SpaceAddon.TRANSLATE,
        ),
        CommonAppShortcut(
            id = "spotify",
            label = "Spotify",
            phrases = listOf("sp", "spotify"),
            packages = listOf("com.spotify.music"),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "whatsapp",
            label = "WhatsApp",
            phrases = listOf("wa", "whatsapp"),
            packages = listOf("com.whatsapp", "com.whatsapp.w4b"),
            addon = SpaceAddon.CHAT,
        ),
        CommonAppShortcut(
            id = "instagram",
            label = "Instagram",
            phrases = listOf("ig", "insta", "instagram"),
            packages = listOf("com.instagram.android"),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "netflix",
            label = "Netflix",
            phrases = listOf("netflix", "nflx"),
            packages = listOf("com.netflix.mediaclient"),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "discord",
            label = "Discord",
            phrases = listOf("discord", "dc"),
            packages = listOf("com.discord"),
            addon = SpaceAddon.CHAT,
        ),
        CommonAppShortcut(
            id = "reddit",
            label = "Reddit",
            phrases = listOf("reddit", "rd"),
            packages = listOf("com.reddit.frontpage"),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "facebook",
            label = "Facebook",
            phrases = listOf("fb", "facebook"),
            packages = listOf("com.facebook.katana", "com.facebook.lite"),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "telegram",
            label = "Telegram",
            phrases = listOf("tg", "telegram"),
            packages = listOf(
                "org.telegram.messenger",
                "org.telegram.messenger.web",
                "org.thunderdog.challegram",
            ),
            labels = listOf("Telegram", "Telegram X"),
            addon = SpaceAddon.CHAT,
        ),
        CommonAppShortcut(
            id = "tiktok",
            label = "TikTok",
            phrases = listOf("tiktok"),
            packages = listOf(
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
            ),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "chatgpt",
            label = "ChatGPT",
            phrases = listOf("gpt", "chatgpt"),
            packages = listOf("com.openai.chatgpt"),
            labels = listOf("ChatGPT", "Chat GPT"),
            addon = SpaceAddon.AI,
        ),
        CommonAppShortcut(
            id = "amazon",
            label = "Amazon",
            phrases = listOf("amazon", "amzn"),
            packages = listOf("com.amazon.mShop.android.shopping"),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "snapchat",
            label = "Snapchat",
            phrases = listOf("snap", "snapchat"),
            packages = listOf("com.snapchat.android"),
            addon = SpaceAddon.SEARCH,
        ),
        CommonAppShortcut(
            id = "twitch",
            label = "Twitch",
            phrases = listOf("twitch"),
            packages = listOf("tv.twitch.android.app"),
            addon = SpaceAddon.SEARCH,
        ),
    )

    private val byPhrase: List<Pair<String, CommonAppShortcut>> =
        catalog.flatMap { item -> item.phrases.map { it.lowercase() to item } }
            .sortedByDescending { it.first.length }

    fun allPhrases(): List<String> = byPhrase.map { it.first }

    fun byId(id: String): CommonAppShortcut? = catalog.firstOrNull { it.id == id }

    fun byPackage(packageName: String): CommonAppShortcut? =
        catalog.firstOrNull { packageName in it.packages }

    /** Exact word, no extra text — Enter launches. */
    fun matchExact(raw: String): CommonAppShortcut? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("!")) return null
        return byPhrase.firstOrNull { (phrase, _) ->
            trimmed.equals(phrase, ignoreCase = true)
        }?.second
    }

    /** `gm ` / `calc ` — Space commits the in-box addon. */
    fun matchSpaceCommit(raw: String): CommonAppShortcut? {
        if (raw.isEmpty() || !raw.last().isWhitespace()) return null
        return matchExact(raw.trimEnd())
    }

    /** `gm invoice` / `maps paris` — addon payload without `!`. */
    fun matchPayload(raw: String): Pair<CommonAppShortcut, String>? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("!")) return null
        for ((phrase, item) in byPhrase) {
            if (trimmed.length <= phrase.length) continue
            if (!trimmed.regionMatches(0, phrase, 0, phrase.length, ignoreCase = true)) continue
            if (!trimmed[phrase.length].isWhitespace()) continue
            val payload = trimmed.substring(phrase.length).trim()
            if (payload.isEmpty()) continue
            return item to payload
        }
        return null
    }
}
