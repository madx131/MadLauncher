package com.madlauncher.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.madlauncher.notif.MessagingAppRegistry

data class ActionCandidate(
    val name: String,
    val packages: List<String>,
    val color: Long,
)

data class InstalledActionApp(
    val name: String,
    val packageName: String,
    val color: Color,
    val icon: Drawable?,
)

enum class ActionAppKind { Messaging, Email, Social, Nav, Ai, Notes, Translator, VoiceCall, VideoCall, Contacts }

class ActionApps(private val context: Context) {
    private val pm: PackageManager get() = context.packageManager
    private val discoveredCache = HashMap<ActionAppKind, List<InstalledActionApp>>()

    fun invalidate() {
        synchronized(discoveredCache) { discoveredCache.clear() }
    }

    val messaging = listOf(
        ActionCandidate("WhatsApp", listOf("com.whatsapp", "com.whatsapp.w4b"), 0xFF25D366),
        ActionCandidate("Telegram", listOf("org.telegram.messenger", "org.telegram.messenger.web"), 0xFF29B6F6),
        ActionCandidate("Signal", listOf("org.thoughtcrime.securesms"), 0xFF3A76F0),
        ActionCandidate("Messages", listOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
        ), 0xFF3DDC84),
        ActionCandidate("Messenger", listOf("com.facebook.orca"), 0xFF0084FF),
        ActionCandidate("Viber", listOf("com.viber.voip"), 0xFF7360F2),
        ActionCandidate("Slack", listOf("com.Slack"), 0xFF4A154B),
        ActionCandidate("Skype", listOf("com.skype.raider"), 0xFF00AFF0),
        ActionCandidate("WeChat", listOf("com.tencent.mm"), 0xFF07C160),
        ActionCandidate("Line", listOf("jp.naver.line.android"), 0xFF06C755),
        ActionCandidate("KakaoTalk", listOf("com.kakao.talk"), 0xFFFFE812),
        ActionCandidate("Threema", listOf("ch.threema.app"), 0xFF333333),
        ActionCandidate("Wire", listOf("com.wire"), 0xFF369DFD),
        ActionCandidate("Element", listOf("im.vector.app"), 0xFF0DBD8B),
        ActionCandidate("Session", listOf("network.loki.messenger"), 0xFF00F782),
        ActionCandidate("TextNow", listOf("com.enflick.android.TextNow"), 0xFF652D90),
    )

    val email = listOf(
        ActionCandidate("Gmail", listOf("com.google.android.gm"), 0xFFEA4335),
        ActionCandidate("Outlook", listOf("com.microsoft.office.outlook"), 0xFF0A66C2),
        ActionCandidate("BlueMail", listOf("me.bluemail.mail"), 0xFF1E6BFF),
    )

    val social = socialCatalog

    val nav = listOf(
        ActionCandidate("Google Maps", listOf("com.google.android.apps.maps"), 0xFF4285F4),
        ActionCandidate("Waze", listOf("com.waze"), 0xFF33CCFF),
    )

    val ai = listOf(
        ActionCandidate("ChatGPT", listOf("com.openai.chatgpt"), 0xFF10A37F),
        ActionCandidate("Gemini", listOf("com.google.android.apps.bard"), 0xFF4285F4),
        ActionCandidate("Claude", listOf("com.anthropic.claude"), 0xFFD97706),
        ActionCandidate("Microsoft Copilot", listOf("com.microsoft.copilot", "com.microsoft.bing"), 0xFF0078D4),
        ActionCandidate("Perplexity", listOf("ai.perplexity.app.android"), 0xFF20808D),
        ActionCandidate("Poe", listOf("com.quora.poe", "com.poe.android"), 0xFF5B4BEE),
        ActionCandidate("Pi", listOf("ai.pi.app", "com.inflection.pi"), 0xFF7C3AED),
        ActionCandidate("Grok", listOf("ai.x.grok", "com.xai.grok"), 0xFF111111),
        ActionCandidate("Meta AI", listOf("com.facebook.stella", "com.meta.ai"), 0xFF0668E1),
        ActionCandidate("Character.AI", listOf("ai.character.app"), 0xFF7C3AED),
        ActionCandidate("DeepSeek", listOf("com.deepseek.chat"), 0xFF4D6BFE),
        ActionCandidate("Le Chat", listOf("ai.mistral.chat"), 0xFFFF7000),
        ActionCandidate("You.com", listOf("com.you.browser"), 0xFF6366F1),
        ActionCandidate("Phind", listOf("com.phind.app"), 0xFF0EA5E9),
    )

    val notes = listOf(
        ActionCandidate("Keep", listOf("com.google.android.keep"), 0xFFF4B400),
        ActionCandidate("Samsung Notes", listOf("com.samsung.android.app.notes", "com.samsung.android.app.notes.persistent"), 0xFF2088FF),
        ActionCandidate("OneNote", listOf("com.microsoft.office.onenote", "com.microsoft.office.onenoteim"), 0xFF7719AA),
        ActionCandidate("ColorNote", listOf("com.socialnmobile.dictapps.notepad.color.note"), 0xFFFFEB3B),
        ActionCandidate("Notion", listOf("notion.id", "com.notion.id"), 0xFF000000),
        ActionCandidate("Evernote", listOf("com.evernote"), 0xFF00A82D),
        ActionCandidate("Standard Notes", listOf("com.standardnotes"), 0xFF086DD6),
        ActionCandidate("Simplenote", listOf("com.automattic.simplenote"), 0xFF3361CC),
        ActionCandidate("Obsidian", listOf("md.obsidian"), 0xFF7C3AED),
        ActionCandidate("Notes", listOf("com.miui.notes", "com.samsung.android.app.notes"), 0xFFF4B400),
    )

    val translator = listOf(
        ActionCandidate("Google Translate", listOf("com.google.android.apps.translate"), 0xFF4285F4),
        ActionCandidate("Microsoft Translator", listOf("com.microsoft.translator"), 0xFF0078D4),
        ActionCandidate("DeepL", listOf("com.deepl.mobiletranslator"), 0xFF0F2B46),
        ActionCandidate("iTranslate", listOf("com.sonicomobile.itranslate"), 0xFF2196F3),
        ActionCandidate("Reverso", listOf("com.softissimo.reverso.context"), 0xFF5C6BC0),
    )

    val contacts = listOf(
        ActionCandidate("Contacts", listOf(
            "com.google.android.contacts",
            "com.android.contacts",
            "com.samsung.android.app.contacts",
            "com.samsung.android.contacts",
            "com.miui.contacts",
        ), 0xFF4285F4),
        ActionCandidate("Simple Contacts", listOf("com.simplemobiletools.contacts.pro", "com.simplemobiletools.contacts"), 0xFF4CAF50),
    )

    fun installed(kind: ActionAppKind, filter: String = ""): List<InstalledActionApp> {
        val f = filter.trim().lowercase()
        val unfiltered = synchronized(discoveredCache) { discoveredCache[kind] }
            ?: runCatching { computeInstalled(kind) }.getOrElse { emptyList() }.also { list ->
                synchronized(discoveredCache) { discoveredCache[kind] = list }
            }
        return if (f.isEmpty()) unfiltered
        else unfiltered.filter { it.name.lowercase().contains(f) || it.packageName.lowercase().contains(f) }
    }

    private fun computeInstalled(kind: ActionAppKind): List<InstalledActionApp> {
        val catalog = when (kind) {
            ActionAppKind.Messaging -> messaging
            ActionAppKind.Email -> email
            ActionAppKind.Social -> social
            ActionAppKind.Nav -> nav
            ActionAppKind.Ai -> ai
            ActionAppKind.Notes -> notes
            ActionAppKind.Translator -> translator
            ActionAppKind.Contacts -> contacts
            ActionAppKind.VoiceCall -> emptyList()
            ActionAppKind.VideoCall -> emptyList()
        }
        if (kind == ActionAppKind.VoiceCall) {
            return CallAppRegistry.discover(context, CallAppMode.Voice, "", installed(ActionAppKind.Messaging))
        }
        if (kind == ActionAppKind.VideoCall) {
            return CallAppRegistry.discover(context, CallAppMode.Video, "", installed(ActionAppKind.Messaging))
        }
        val fromCatalog = catalog.mapNotNull { resolve(it) }.toMutableList()
        when (kind) {
            ActionAppKind.Messaging -> {
                mergeHandlers(fromCatalog, smsIntent())
                fromCatalog.removeAll { isCallerIdPackage(it.packageName) }
                mergeHintDiscovery(fromCatalog, listOf(Intent(Intent.ACTION_SEND).setType("text/plain"))) { name, pkg ->
                    looksLikeMessaging(name, pkg)
                }
                mergeCustomBrowserApps(fromCatalog)
            }
            ActionAppKind.Email -> {
                mergeHandlers(fromCatalog, mailIntent())
                mergeHintDiscovery(fromCatalog, listOf(mailIntent())) { name, pkg ->
                    looksLikeEmail(name, pkg)
                }
            }
            ActionAppKind.Social -> {
                mergeHintDiscovery(
                    fromCatalog,
                    listOf(Intent(Intent.ACTION_SEND).setType("text/plain")),
                ) { name, pkg -> looksLikeSocial(name, pkg) }
            }
            ActionAppKind.Nav -> {
                mergeHintDiscovery(
                    fromCatalog,
                    listOf(
                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=test")),
                        Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=test")),
                    ),
                ) { name, pkg -> looksLikeNav(name, pkg) }
            }
            ActionAppKind.Ai -> {
                mergeHintDiscovery(
                    fromCatalog,
                    listOf(Intent(Intent.ACTION_SEND).setType("text/plain")),
                ) { name, pkg -> looksLikeAi(name, pkg) }
            }
            ActionAppKind.Notes -> {
                mergeHintDiscovery(
                    fromCatalog,
                    listOf(Intent(Intent.ACTION_SEND).setType("text/plain")),
                ) { name, pkg -> looksLikeNotes(name, pkg) }
            }
            ActionAppKind.Translator -> {
                mergeHintDiscovery(
                    fromCatalog,
                    listOf(Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain")),
                ) { name, pkg -> looksLikeTranslator(name, pkg) }
            }
            ActionAppKind.Contacts -> {
                mergeHintDiscovery(
                    fromCatalog,
                    listOf(
                        Intent(Intent.ACTION_INSERT).setType("vnd.android.cursor.dir/contact"),
                        Intent(Intent.ACTION_INSERT).setType("vnd.android.cursor.dir/raw_contact"),
                    ),
                ) { name, pkg -> looksLikeContacts(name, pkg) }
            }
            else -> Unit
        }
        return fromCatalog
    }

    fun find(kind: ActionAppKind, packageName: String): InstalledActionApp? {
        return installed(kind).firstOrNull { it.packageName == packageName }
    }

    fun resolvePackage(packageName: String): InstalledActionApp? {
        val pkg = packageName.trim()
        if (pkg.isBlank()) return null
        val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return null
        if (!info.enabled) return null
        pm.getLaunchIntentForPackage(pkg) ?: return null
        return InstalledActionApp(
            name = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(pkg),
            packageName = pkg,
            color = Color(0xFF81A1C1),
            icon = runCatching { pm.getApplicationIcon(info) }.getOrNull(),
        )
    }

    /** All SMS handler packages — smsto handlers, default SMS role, catalog, no launch intent required. */
    fun discoverSmsPackages(): Set<String> {
        val found = mutableSetOf<String>()
        found.addAll(MessagingAppRegistry.querySmsHandlerPackages(context))
        MessagingAppRegistry.defaultSmsPackage(context)?.let { found.add(it) }
        for (candidate in messaging) {
            for (pkg in candidate.packages) {
                val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: continue
                if (info.enabled) found.add(pkg)
            }
        }
        val discovered = mutableListOf<InstalledActionApp>()
        mergeHandlers(discovered, smsIntent())
        discovered.forEach { found.add(it.packageName) }
        return found.filter { pkg ->
            !ActionApps.isCallerIdPackage(pkg) &&
                runCatching { pm.getApplicationInfo(pkg, 0).enabled }.getOrDefault(false)
        }.toSet()
    }

    private fun mergeHintDiscovery(
        into: MutableList<InstalledActionApp>,
        extraIntents: List<Intent>,
        matches: (name: String, packageName: String) -> Boolean,
    ) {
        val seen = into.map { it.packageName }.toMutableSet()
        for (intent in extraIntents) {
            val resolves = runCatching {
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }.getOrDefault(emptyList())
            for (ri in resolves) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (pkg == context.packageName || pkg == "android") continue
                if (!seen.add(pkg)) continue
                val label = ri.loadLabel(pm)?.toString() ?: pkg
                if (!matches(label, pkg)) continue
                into.add(
                    InstalledActionApp(
                        name = label,
                        packageName = pkg,
                        color = Color(0xFF81A1C1),
                        icon = runCatching { ri.loadIcon(pm) }.getOrNull(),
                    ),
                )
            }
        }
    }

    private fun mergeCustomBrowserApps(into: MutableList<InstalledActionApp>) {
        val customs = runCatching {
            com.madlauncher.data.LocalStore(context).appearanceSettings.customBrowserApps
        }.getOrDefault(emptyList())
        for (custom in customs) {
            if (into.any { it.packageName.equals(custom.packageName, ignoreCase = true) }) continue
            val resolved = resolvePackage(custom.packageName) ?: continue
            val label = custom.label.ifBlank { resolved.name }
            into.add(resolved.copy(name = label))
        }
    }

    private fun resolve(candidate: ActionCandidate): InstalledActionApp? {
        for (pkg in candidate.packages) {
            val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: continue
            if (!info.enabled) continue
            pm.getLaunchIntentForPackage(pkg) ?: continue
            return InstalledActionApp(
                name = candidate.name,
                packageName = pkg,
                color = Color(candidate.color),
                icon = runCatching { pm.getApplicationIcon(info) }.getOrNull(),
            )
        }
        return null
    }

    private fun mergeHandlers(
        into: MutableList<InstalledActionApp>,
        intent: Intent,
    ) {
        val seen = into.map { it.packageName }.toMutableSet()
        val resolves = runCatching {
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrDefault(emptyList())
        for (ri in resolves) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || pkg == "android") continue
            if (!seen.add(pkg)) continue
            val label = ri.loadLabel(pm)?.toString() ?: pkg
            into.add(
                InstalledActionApp(
                    name = label,
                    packageName = pkg,
                    color = Color(0xFF81A1C1),
                    icon = runCatching { ri.loadIcon(pm) }.getOrNull(),
                ),
            )
        }
    }

    private fun smsIntent(): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))

    private fun mailIntent(): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))

    companion object {
        internal val socialCatalog = listOf(
            ActionCandidate("Facebook", listOf("com.facebook.katana", "com.facebook.lite"), 0xFF1877F2),
            ActionCandidate("Instagram", listOf("com.instagram.android"), 0xFFE1306C),
            ActionCandidate("Threads", listOf("com.instagram.barcelona"), 0xFF000000),
            ActionCandidate("X", listOf("com.twitter.android", "com.x.android"), 0xFF1DA1F2),
            ActionCandidate("Truth Social", listOf("com.truthsocial.android.app"), 0xFFE31C25),
            ActionCandidate("LinkedIn", listOf("com.linkedin.android"), 0xFF0A66C2),
            ActionCandidate("Reddit", listOf("com.reddit.frontpage"), 0xFFFF4500),
            ActionCandidate("Discord", listOf("com.discord"), 0xFF5865F2),
            ActionCandidate("TikTok", listOf("com.zhiliaoapp.musically"), 0xFF69C9D0),
        )

        private val socialHints = listOf(
            "facebook", "instagram", "threads", "barcelona",
            "twitter", "x.com", "linkedin", "reddit", "discord", "tiktok", "musically",
            "truth social", "truthsocial", "mastodon", "bluesky", "bsky",
            "tumblr", "pinterest", "snapchat", "weibo", "vkontakte",
        )
        private val messagingHints = listOf(
            "whatsapp", "telegram", "signal", "messenger", "viber", "wechat",
            "kakao", "threema", "slack", "skype", "wire", "element", "session",
            "textnow", "textra", "messages", "messaging", "sms",
        )
        private val emailHints = listOf(
            "gmail", "android.gm", "outlook", "proton", "thunderbird", "k-9", "k9mail",
            "yahoo mail", "spark", "edison", "fairmail", "samsung email",
        )
        private val navHints = listOf(
            "maps", "waze", "navigation", "citymapper", "moovit", "here wego",
            "osm", "organic maps", "mapy", "yandex maps", "petal maps",
        )
        private val aiHints = listOf(
            "chatgpt", "openai", "gemini", "bard", "claude", "anthropic",
            "copilot", "perplexity", "poe", "grok", "deepseek", "mistral",
            "character.ai", "character ai", "meta ai",
        )
        private val notesHints = listOf(
            "note", "notes", "keep", "onenote", "evernote", "notion",
            "simplenote", "obsidian", "memo", "notepad", "journal",
        )
        private val translatorHints = listOf(
            "translate", "translator", "deepl", "itranslate", "reverso", "dictionary",
        )
        private val contactsHints = listOf(
            "contact", "contacts", "people", "address book", "phonebook",
        )

        internal fun looksLikeSocial(name: String, packageName: String): Boolean {
            val label = name.lowercase()
            val pkg = packageName.lowercase()
            if (pkg.contains("twitter") || pkg.contains("truthsocial") || pkg == "com.x.android") return true
            return socialHints.any { label.contains(it) || pkg.contains(it) }
        }

        internal fun looksLikeMessaging(name: String, packageName: String): Boolean {
            val label = name.lowercase()
            val pkg = packageName.lowercase()
            if (isCallerIdPackage(pkg)) return false
            if (looksLikeEmail(name, packageName)) return false
            if (label == "line" || pkg.contains("naver.line")) return true
            return messagingHints.any { label.contains(it) || pkg.contains(it) }
        }

        internal fun looksLikeEmail(name: String, packageName: String): Boolean {
            val label = name.lowercase()
            val pkg = packageName.lowercase()
            if (label == "mail" || label.endsWith(" mail") || label.contains("email")) return true
            return emailHints.any { label.contains(it) || pkg.contains(it) }
        }

        internal fun looksLikeNav(name: String, packageName: String): Boolean {
            val label = name.lowercase()
            val pkg = packageName.lowercase()
            return navHints.any { label.contains(it) || pkg.contains(it) }
        }

        internal fun looksLikeAi(name: String, packageName: String): Boolean {
            val label = name.lowercase()
            val pkg = packageName.lowercase()
            if (pkg.contains(".ai.") || pkg.contains("chatgpt") || pkg.contains("anthropic")) return true
            return aiHints.any { label.contains(it) || pkg.contains(it) }
        }

        internal fun looksLikeNotes(name: String, packageName: String): Boolean {
            val label = name.lowercase()
            val pkg = packageName.lowercase()
            if (label.contains("notification")) return false
            return notesHints.any { label.contains(it) || pkg.contains(it) }
        }

        internal fun looksLikeTranslator(name: String, packageName: String): Boolean {
            val label = name.lowercase()
            val pkg = packageName.lowercase()
            return translatorHints.any { label.contains(it) || pkg.contains(it) }
        }

        internal fun looksLikeContacts(name: String, packageName: String): Boolean {
            val label = name.lowercase()
            val pkg = packageName.lowercase()
            if (!isReliableContactInsert(pkg)) return false
            if (label.contains("messenger") || pkg.contains("whatsapp")) return false
            return contactsHints.any { label.contains(it) || pkg.contains(it) }
        }

        /** Caller-ID shells register INSERT / smsto but are not a contacts editor or SMS app. */
        fun isCallerIdPackage(packageName: String): Boolean {
            val pkg = packageName.lowercase()
            if (pkg.isBlank()) return false
            return callerIdPackages.any { pkg.contains(it) }
        }

        fun isReliableContactInsert(packageName: String): Boolean {
            val pkg = packageName.lowercase()
            if (pkg.isBlank()) return false
            return !isCallerIdPackage(pkg)
        }

        private val callerIdPackages = listOf(
            "eyecon",
            "truecaller",
            "callapp",
            "callerid",
            "showcaller",
        )
    }
}
