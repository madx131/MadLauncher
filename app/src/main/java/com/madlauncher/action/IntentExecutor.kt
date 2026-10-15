package com.madlauncher.action

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.app.WallpaperManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import com.madlauncher.LauncherCommands
import com.madlauncher.MadApp
import com.madlauncher.a11y.MessengerSearchBridge
import com.madlauncher.a11y.OttCallBridge
import com.madlauncher.a11y.OttChatSendBridge
import com.madlauncher.action.WhatsAppCallBridge
import com.madlauncher.data.ContactEntry
import com.madlauncher.data.LocalStore
import com.madlauncher.data.PermissionChecker
import com.madlauncher.data.WebSearchEngine

/** HyperOS / MIUI stock weather — main activity crashes on some builds; use browser forecast instead. */
object WeatherLaunchGuard {
    fun isBlocked(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg.contains("miui.weather") ||
            pkg.contains("xiaomi.weather") ||
            pkg.contains("hyperos.weather") ||
            pkg.contains(".mweather") ||
            pkg == "com.miui.weather2"
    }
}

/** How `@` / `&` opens a picked contact. Mail is only a real mail app. */
internal enum class DirectConversation {
    WhatsApp,
    Telegram,
    Signal,
    Mail,
    Sms,
    Search,
}

internal fun directConversation(packageName: String, hasPhone: Boolean, hasEmail: Boolean): DirectConversation {
    val pkg = packageName.lowercase()
    return when {
        pkg.contains("whatsapp") -> DirectConversation.WhatsApp
        pkg.contains("telegram") -> DirectConversation.Telegram
        pkg.contains("signal") -> DirectConversation.Signal
        hasEmail && isMailPackage(pkg) -> DirectConversation.Mail
        hasPhone && isSmsPackage(pkg) -> DirectConversation.Sms
        else -> DirectConversation.Search
    }
}

internal fun isMailPackage(packageName: String): Boolean {
    val pkg = packageName.lowercase()
    if (pkg.contains("messaging")) return false
    return pkg.contains(".gm") || pkg.contains("mail") || pkg.contains("outlook")
}

/** SMS / MMS handlers — not WhatsApp, Messenger, and the other chat apps. */
internal fun isSmsPackage(packageName: String): Boolean {
    val pkg = packageName.lowercase()
    val ott = listOf(
        "whatsapp", "telegram", "signal", "viber", "discord", "slack",
        "threema", "line.android", "kakao", "wechat", "skype",
        "facebook.orca", "messenger",
    )
    return ott.none { pkg.contains(it) }
}

sealed class SendMessageResult {
    /** Opened a chat/composer with text ready — user taps Send. */
    data object OpenedComposer : SendMessageResult()
    /** Accessibility is pasting into the composer (not sending). */
    data object Automating : SendMessageResult()
    data class Failed(val reason: String) : SendMessageResult()
}

/** Outcome of [IntentExecutor.searchInApp] so the UI can coach multi-step apps. */
sealed class SearchInAppResult {
    data object OpenedDeepLink : SearchInAppResult()
    data object OpenedSearchIntent : SearchInAppResult()
    /** Query copied; app opened — user must paste into in-app search (Instagram, etc.). */
    data object AssistedClipboard : SearchInAppResult()
    data object Failed : SearchInAppResult()
}

class IntentExecutor(
    private val context: Context,
) {
    fun dial(number: String) {
        start(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.trim()}")),
        )
    }

    fun videoDial(number: String) {
        val tel = number.trim()
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")).apply {
            putExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 3)
            putExtra("com.android.phone.videocall", true)
        }
        start(intent)
    }

    fun voiceCall(app: InstalledActionApp, contact: ContactEntry): CallResult {
        val result = CallAppHandlers.voiceCall(context, app, contact)
        if (result == CallResult.Failed) {
            val phone = contact.phone.orEmpty().trim()
            if (CallAppHandlers.isDialerPackage(app.packageName) && phone.isNotBlank()) {
                dial(phone)
                return CallResult.Started
            }
        }
        return maybeAutomateOttCall(app, video = false, result)
    }

    fun videoCall(app: InstalledActionApp, contact: ContactEntry): CallResult {
        val result = CallAppHandlers.videoCall(context, app, contact)
        if (result == CallResult.Failed) {
            val phone = contact.phone.orEmpty().trim()
            if (CallAppHandlers.isDialerPackage(app.packageName) && phone.isNotBlank()) {
                videoDial(phone)
                return CallResult.Started
            }
        }
        return maybeAutomateOttCall(app, video = true, result)
    }

    private fun maybeAutomateOttCall(
        app: InstalledActionApp,
        video: Boolean,
        result: CallResult,
    ): CallResult {
        if (result == CallResult.Automating) {
            LauncherCommands.suppressHomeBriefly()
            MadApp.instance.deferLauncherToBack()
            return result
        }
        if (result != CallResult.OpenedThread) return result
        if (CallAppHandlers.isDialerPackage(app.packageName)) return result
        if (!app.packageName.contains("whatsapp", ignoreCase = true)) return result
        if (!PermissionChecker.isAccessibilityEnabled(context)) return result
        OttCallBridge.request(context, app.packageName, video)
        LauncherCommands.suppressHomeBriefly()
        MadApp.instance.deferLauncherToBack()
        return CallResult.Automating
    }

    fun openWifi() {
        if (Build.VERSION.SDK_INT >= 29 && startIfResolvable(Intent(Settings.Panel.ACTION_WIFI))) return
        if (startIfResolvable(Intent(Settings.ACTION_WIFI_SETTINGS))) return
        openSystemSettings()
    }

    fun openBluetooth() {
        if (startIfResolvable(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))) return
        openSystemSettings()
    }

    fun openMobileData() {
        // Do NOT use Settings.Panel.ACTION_INTERNET_CONNECTIVITY — that opens Wi‑Fi / Internet.
        if (startIfResolvable(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))) return
        if (startIfResolvable(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))) return
        if (startIfResolvable(Intent("android.settings.MOBILE_NETWORK_SETTINGS"))) return
        openSystemSettings()
    }

    fun openQuickShare() {
        val nearby = Intent().setClassName(
            "com.google.android.gms",
            "com.google.android.gms.nearby.sharing.main.MainActivity",
        )
        if (startIfResolvable(nearby)) return
        val send = Intent("com.google.android.gms.nearby.sharing.ACTION_SEND")
        if (startIfResolvable(send)) return
        openBluetooth()
    }

    fun openQsTile(id: String) {
        when (id) {
            "wifi" -> openWifi()
            "bt" -> openBluetooth()
            "cell" -> openMobileData()
            "share" -> openQuickShare()
            "dnd" -> openSetting("android.settings.ZEN_MODE_SETTINGS", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            "air" -> openSetting(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            "torch" -> Unit
            "rotate", "display", "brightness", "dark" -> openSetting(Settings.ACTION_DISPLAY_SETTINGS)
            "hotspot" -> openSetting(Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_WIFI_SETTINGS)
            "location" -> openSetting(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            "battery" -> openSetting(Settings.ACTION_BATTERY_SAVER_SETTINGS, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            "night" -> openSetting("android.settings.NIGHT_DISPLAY_SETTINGS", Settings.ACTION_DISPLAY_SETTINGS)
            "volume" -> openSetting(Settings.ACTION_SOUND_SETTINGS)
            // "sound" / mute is cycled in-app via QsTileActions — never dump into Settings here.
            "sound", "mute" -> Unit
            "nfc" -> openSetting(Settings.ACTION_NFC_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS)
            "cast" -> openSetting("android.settings.CAST_SETTINGS", Settings.ACTION_WIFI_SETTINGS)
            "vpn" -> openSetting(Settings.ACTION_VPN_SETTINGS)
            "data_saver" -> openSetting("android.settings.DATA_SAVER_SETTINGS", Settings.ACTION_DATA_ROAMING_SETTINGS)
            "invert" -> openSetting(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            "bedtime" -> {
                val wellbeing = Intent().setClassName(
                    "com.google.android.apps.wellbeing",
                    "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity",
                )
                if (startIfResolvable(wellbeing)) return
                openSetting(Settings.ACTION_DISPLAY_SETTINGS)
            }
            "wallet" -> {
                val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.walletnfcrel")
                if (launch != null) {
                    start(launch)
                    return
                }
                openQuickShare()
            }
            "settings" -> openSystemSettings()
            else -> openSystemSettings()
        }
    }

    private fun openSetting(vararg actions: String) {
        for (action in actions) {
            if (action.isBlank()) continue
            if (startIfResolvable(Intent(action))) return
        }
        openSystemSettings()
    }

    fun openSystemSettings() {
        start(Intent(Settings.ACTION_SETTINGS))
    }

    fun openNotificationListenerSettings() {
        start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openSettingsSearch(query: String) {
        val search = Intent("com.android.settings.action.SETTINGS_SEARCH")
            .putExtra("query", query)
            .putExtra(":settings:query", query)
        if (startIfResolvable(search)) return
        openSystemSettings()
    }

    fun webSearch(query: String, engine: WebSearchEngine? = null) {
        val chosen = engine ?: LocalStore(context).appearanceSettings.webSearchEngine
        start(Intent(Intent.ACTION_VIEW, Uri.parse(chosen.searchUrl(query))))
    }

    /**
     * Opens the store's updates list (GetApps on Xiaomi, Play Store otherwise).
     * Cannot silent-install; user taps Update all in the store.
     */
    fun openAppUpdates(): Boolean {
        val getApps = "com.xiaomi.market"
        val play = "com.android.vending"
        val attempts = listOfNotNull(
            Intent(Intent.ACTION_VIEW, Uri.parse("mimarket://updates")).setPackage(getApps),
            Intent(Intent.ACTION_VIEW, Uri.parse("mimarket://update")).setPackage(getApps),
            Intent(Intent.ACTION_VIEW, Uri.parse("mimarket://updates")),
            Intent("com.google.android.finsky.VIEW_MY_DOWNLOADS").setPackage(play),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps")).setPackage(play),
            context.packageManager.getLaunchIntentForPackage(getApps),
            context.packageManager.getLaunchIntentForPackage(play),
        )
        return attempts.any { startIfResolvable(Intent(it)) }
    }

    fun navigate(app: InstalledActionApp, destination: String) {
        val dest = destination.trim()
        if (dest.isEmpty()) return
        val pkg = app.packageName
        for (url in NavIntents.navigateUris(pkg, dest)) {
            val uri = Uri.parse(url)
            if (startIfResolvableInPackage(Intent(Intent.ACTION_VIEW, uri).setPackage(pkg), pkg)) {
                return
            }
        }
        if (startPackageSearch(Intent.ACTION_SEARCH, pkg, dest)) return
        val fallback = NavIntents.navigateUris(pkg, dest).firstOrNull() ?: return
        start(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
    }

    fun sendMessage(app: InstalledActionApp, contact: ContactEntry, body: String): SendMessageResult {
        val text = body.trim()
        if (text.isEmpty()) return SendMessageResult.Failed("Type a message first")
        val phone = contact.phone.orEmpty().trim()
        if (phone.isBlank()) {
            if (contact.fromAppHistory) {
                openAppComposer(app, text)
                return SendMessageResult.OpenedComposer
            }
            return SendMessageResult.Failed("No phone number for ${contact.name}")
        }

        val pkg = app.packageName.lowercase()
        return when {
            pkg.contains("whatsapp") -> openWhatsApp(phone, text, app.packageName)
            pkg.contains("telegram") -> openTelegramChat(text, app.packageName, phone)
            pkg.contains("signal") -> openSignal(phone, text, app.packageName)
            isSmsHandler(pkg) -> openSmsComposer(phone, text, app.packageName)
            else -> {
                shareTo(app.packageName, text)
                SendMessageResult.OpenedComposer
            }
        }
    }

    /** `@` after picking a contact: open that chat/composer ready for typing. */
    fun openConversation(app: InstalledActionApp, contact: ContactEntry): Boolean {
        if (openFromLiveNotif(app.packageName, contact)) return true
        val pkg = app.packageName.lowercase()
        val phone = contact.phone.orEmpty().trim()
        val email = contact.email.orEmpty().trim()
        val opened = when (directConversation(pkg, phone.isNotBlank(), email.isNotBlank())) {
            DirectConversation.WhatsApp -> openWhatsAppThread(phone, app.packageName, contact)
            DirectConversation.Telegram -> openTelegramThread(app.packageName, phone)
            DirectConversation.Signal -> openSignalThread(phone, app.packageName)
            DirectConversation.Mail -> openMailto(email, app.packageName)
            DirectConversation.Sms -> openSmsThread(phone, app.packageName)
            DirectConversation.Search -> false
        }
        if (opened) return true
        val name = contact.name.trim()
        if (name.isNotBlank() && !isMailApp(pkg)) {
            MessengerSearchBridge.begin(context, app.packageName, name)
            return true
        }
        if (ReusedBrowser.openIfEnabled(context, app.packageName)) return true
        if (SocialInbox.hasInbox(app.packageName)) {
            return SocialInbox.open(context, app.packageName) is InboxOpenResult.Opened
        }
        return launchLauncherActivity(app.packageName)
    }

    fun openComposer(app: InstalledActionApp): PostResult =
        SocialPostBridge.openComposer(context, app.packageName)

    /** SMS / MMS handlers (Messages, Samsung Messaging, …) — not OTT apps. */
    private fun isSmsHandler(packageName: String): Boolean = isSmsPackage(packageName)

    private fun openSmsComposer(phone: String, body: String, packageName: String): SendMessageResult {
        val dest = normalizePhone(phone)
        if (dest.isBlank()) return SendMessageResult.Failed("Invalid phone number")
        val sendTo = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$dest"))
            .putExtra("sms_body", body)
            .putExtra(Intent.EXTRA_TEXT, body)
            .setPackage(packageName)
        if (startIfResolvable(sendTo)) return SendMessageResult.OpenedComposer
        val viewSms = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$dest"))
            .putExtra("sms_body", body)
            .putExtra(Intent.EXTRA_TEXT, body)
            .setPackage(packageName)
        if (startIfResolvable(viewSms)) return SendMessageResult.OpenedComposer
        copyPlainText(body)
        launchPackage(packageName)
        return SendMessageResult.OpenedComposer
    }

    private fun normalizePhone(raw: String): String {
        val trimmed = raw.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    private fun openWhatsApp(phone: String, body: String, packageName: String): SendMessageResult {
        val digits = phone.filter { it.isDigit() }
        if (digits.isBlank()) return SendMessageResult.Failed("No phone number")
        // Official send URL includes text in the composer (not a call link).
        val withText = Uri.parse("https://api.whatsapp.com/send?phone=$digits&text=${Uri.encode(body)}")
        if (startIfResolvable(Intent(Intent.ACTION_VIEW, withText).setPackage(packageName)) ||
            startIfResolvable(Intent(Intent.ACTION_VIEW, withText))
        ) {
            return SendMessageResult.OpenedComposer
        }
        val waMe = Uri.parse("https://wa.me/$digits?text=${Uri.encode(body)}")
        if (startIfResolvable(Intent(Intent.ACTION_VIEW, waMe).setPackage(packageName)) ||
            startIfResolvable(Intent(Intent.ACTION_VIEW, waMe))
        ) {
            return SendMessageResult.OpenedComposer
        }
        // JID open + clipboard so the user can paste if the app ignores extras.
        copyPlainText(body)
        WhatsAppCallBridge.buildChatIntent(context, packageName, digits)?.let { intent ->
            intent.putExtra(Intent.EXTRA_TEXT, body)
            if (startIfResolvable(intent)) return SendMessageResult.OpenedComposer
        }
        shareTo(packageName, body)
        return SendMessageResult.OpenedComposer
    }

    private fun openTelegramChat(body: String, packageName: String, phone: String? = null): SendMessageResult {
        val digits = phone?.filter { it.isDigit() }.orEmpty()
        copyPlainText(body)
        val chatIntent = if (digits.isNotBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=$digits")).setPackage(packageName)
        } else {
            null
        }
        val opened = chatIntent != null && startIfResolvable(Intent(chatIntent))
        if (opened && PermissionChecker.isAccessibilityEnabled(context)) {
            OttChatSendBridge.begin(
                context = context,
                packageName = packageName,
                text = body,
                alreadyFilled = false,
                chatIntent = chatIntent,
            )
            LauncherCommands.suppressHomeBriefly()
            MadApp.instance.deferLauncherToBack()
            return SendMessageResult.Automating
        }
        if (opened) return SendMessageResult.OpenedComposer
        val uri = Uri.parse("https://t.me/share/url?url=&text=${Uri.encode(body)}")
        return if (startIfResolvable(Intent(Intent.ACTION_VIEW, uri).setPackage(packageName)) ||
            startIfResolvable(Intent(Intent.ACTION_VIEW, uri))
        ) {
            SendMessageResult.OpenedComposer
        } else {
            shareTo(packageName, body)
            SendMessageResult.OpenedComposer
        }
    }

    private fun openSignal(phone: String, body: String, packageName: String): SendMessageResult {
        val digits = phone.filter { it.isDigit() }
        val uri = Uri.parse("sgnl://send?phone=$digits&text=${Uri.encode(body)}")
        return if (startIfResolvable(Intent(Intent.ACTION_VIEW, uri).setPackage(packageName))) {
            SendMessageResult.OpenedComposer
        } else {
            shareTo(packageName, body)
            SendMessageResult.OpenedComposer
        }
    }

    fun sendEmail(app: InstalledActionApp, contact: ContactEntry, body: String) {
        val email = contact.email.orEmpty().trim()
        if (email.isBlank()) {
            openAppComposer(app, body)
            return
        }
        val mailto = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
            .putExtra(Intent.EXTRA_TEXT, body)
            .setPackage(app.packageName)
        if (!startIfResolvable(mailto)) {
            openAppComposer(app, body)
        }
    }

    fun sharePost(app: InstalledActionApp, body: String): PostResult =
        SocialPostBridge.place(context, app, body)

    private fun openFromLiveNotif(packageName: String, contact: ContactEntry): Boolean {
        val open = com.madlauncher.notif.NotificationHub.open ?: return false
        val items = com.madlauncher.notif.NotificationHub.items.value.filter { it.packageName == packageName }
        if (items.isEmpty()) return false
        val name = contact.name.trim()
        val thread = ContactReach.threadKey(contact)
        val match = items.firstOrNull { it.conversationId == contact.id }
            ?: items.firstOrNull { thread != null && it.conversationId.endsWith("|$thread", ignoreCase = true) }
            ?: items.firstOrNull { it.title.equals(name, ignoreCase = true) }
            ?: items.firstOrNull { it.contactName.equals(name, ignoreCase = true) }
            ?: return false
        return runCatching { open(match.key); true }.getOrDefault(false)
    }

    private fun openWhatsAppThread(phone: String, packageName: String, contact: ContactEntry): Boolean {
        val digits = phone.filter { it.isDigit() }.ifBlank { ContactReach.phoneDigits(contact).orEmpty() }
        if (digits.isNotBlank()) {
            val send = Uri.parse("https://api.whatsapp.com/send?phone=$digits")
            if (startIfResolvable(Intent(Intent.ACTION_VIEW, send).setPackage(packageName)) ||
                startIfResolvable(Intent(Intent.ACTION_VIEW, send))
            ) {
                return true
            }
            val waMe = Uri.parse("https://wa.me/$digits")
            if (startIfResolvable(Intent(Intent.ACTION_VIEW, waMe).setPackage(packageName)) ||
                startIfResolvable(Intent(Intent.ACTION_VIEW, waMe))
            ) {
                return true
            }
            WhatsAppCallBridge.buildChatIntent(context, packageName, digits)?.let { intent ->
                if (startIfResolvable(intent)) return true
            }
        }
        return false
    }

    private fun openTelegramThread(packageName: String, phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        if (digits.isBlank()) return false
        val uri = Uri.parse("tg://resolve?phone=$digits")
        return startIfResolvable(Intent(Intent.ACTION_VIEW, uri).setPackage(packageName)) ||
            startIfResolvable(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun openSignalThread(phone: String, packageName: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        if (digits.isBlank()) return false
        val uri = Uri.parse("sgnl://send?phone=$digits")
        return startIfResolvable(Intent(Intent.ACTION_VIEW, uri).setPackage(packageName))
    }

    private fun isMailApp(packageName: String): Boolean = isMailPackage(packageName)

    private fun openMailto(email: String, packageName: String): Boolean {
        val mailto = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).setPackage(packageName)
        if (startIfResolvable(mailto)) return true
        return startIfResolvable(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
    }

    private fun openSmsThread(phone: String, packageName: String): Boolean {
        val dest = normalizePhone(phone)
        if (dest.isBlank()) return false
        val sendTo = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$dest")).setPackage(packageName)
        if (startIfResolvable(sendTo)) return true
        val viewSms = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$dest")).setPackage(packageName)
        return startIfResolvable(viewSms)
    }

    /** Open a translator app; omitted [fromCode]/[toCode] leave the app's last-used languages. */
    fun openTranslator(
        app: InstalledActionApp,
        text: String,
        fromCode: String?,
        toCode: String?,
    ) {
        val body = text.trim()
        val pkg = app.packageName.lowercase()
        when {
            pkg.contains("google") && pkg.contains("translate") ->
                openGoogleTranslate(app.packageName, body, fromCode, toCode)
            pkg.contains("microsoft.translator") || pkg.contains("bingtranslator") ->
                openBingTranslator(app.packageName, body, fromCode, toCode)
            pkg.contains("deepl") ->
                openDeepL(app.packageName, body, fromCode, toCode)
            else ->
                openGenericTranslator(app, body, fromCode, toCode)
        }
    }

    private fun openGoogleTranslate(
        packageName: String,
        text: String,
        fromCode: String?,
        toCode: String?,
    ) {
        // Prefer ACTION_SEND — VIEW URLs often open Translate without the query text.
        if (bodyNonEmpty(text) && shareTextToPackage(packageName, text, fromCode, toCode)) return

        val uriBuilder = Uri.parse("https://translate.google.com/").buildUpon()
        fromCode?.takeUnless { it.equals("auto", ignoreCase = true) }
            ?.let { uriBuilder.appendQueryParameter("sl", it) }
        toCode?.let { uriBuilder.appendQueryParameter("tl", it) }
        if (bodyNonEmpty(text)) {
            uriBuilder.appendQueryParameter("text", text)
            uriBuilder.appendQueryParameter("op", "translate")
        }
        val view = Intent(Intent.ACTION_VIEW, uriBuilder.build()).setPackage(packageName)
        if (startIfResolvable(view)) return

        if (bodyNonEmpty(text)) {
            copyPlainText(text)
            launchPackage(packageName)
        } else {
            launchPackage(packageName)
        }
    }

    private fun openBingTranslator(
        packageName: String,
        text: String,
        fromCode: String?,
        toCode: String?,
    ) {
        if (bodyNonEmpty(text) && shareTextToPackage(packageName, text, fromCode, toCode)) return

        val uriBuilder = Uri.Builder()
            .scheme("https")
            .authority("www.bing.com")
            .path("/translator")
        fromCode?.takeUnless { it.equals("auto", ignoreCase = true) }
            ?.let { uriBuilder.appendQueryParameter("from", it) }
        toCode?.let { uriBuilder.appendQueryParameter("to", it) }
        if (bodyNonEmpty(text)) uriBuilder.appendQueryParameter("text", text)
        val view = Intent(Intent.ACTION_VIEW, uriBuilder.build()).setPackage(packageName)
        if (startIfResolvable(view)) return
        if (bodyNonEmpty(text)) {
            copyPlainText(text)
            launchPackage(packageName)
        } else {
            launchPackage(packageName)
        }
    }

    private fun openDeepL(
        packageName: String,
        text: String,
        fromCode: String?,
        toCode: String?,
    ) {
        if (bodyNonEmpty(text) && shareTextToPackage(packageName, text, fromCode, toCode)) return

        val from = fromCode?.takeUnless { it.equals("auto", ignoreCase = true) } ?: "en"
        val to = toCode ?: "en"
        val fragment = buildString {
            append("#")
            append(Uri.encode(from))
            append("/")
            append(Uri.encode(to))
            if (bodyNonEmpty(text)) {
                append("/")
                append(Uri.encode(text))
            }
        }
        val view = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.deepl.com/translator$fragment"),
        ).setPackage(packageName)
        if (startIfResolvable(view)) return
        if (bodyNonEmpty(text)) {
            copyPlainText(text)
            launchPackage(packageName)
        } else {
            launchPackage(packageName)
        }
    }

    private fun openGenericTranslator(
        app: InstalledActionApp,
        text: String,
        fromCode: String?,
        toCode: String?,
    ) {
        if (bodyNonEmpty(text) && shareTextToPackage(app.packageName, text, fromCode, toCode)) return
        if (bodyNonEmpty(text)) openAppComposer(app, text) else launchPackage(app.packageName)
    }

    /** Share plain text into a translator (Google Translate honors this with the input filled). */
    private fun shareTextToPackage(
        packageName: String,
        text: String,
        fromCode: String?,
        toCode: String?,
    ): Boolean {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .setPackage(packageName)
            .putExtra(Intent.EXTRA_TEXT, text)
            .putExtra("key_text_input", text)
        fromCode?.takeUnless { it.equals("auto", ignoreCase = true) }?.let { code ->
            send.putExtra("key_language_from", code)
            send.putExtra("from", code)
        }
        toCode?.let { code ->
            send.putExtra("key_language_to", code)
            send.putExtra("to", code)
        }
        return startIfResolvable(send)
    }

    private fun bodyNonEmpty(text: String): Boolean = text.isNotBlank()

    /** Open an AI assistant and start a conversation with [prompt] when possible. */
    fun askAi(app: InstalledActionApp, prompt: String) {
        val text = prompt.trim()
        if (text.isEmpty()) {
            launchPackage(app.packageName)
            return
        }
        val pkg = app.packageName
        // Prefer SEND — deep-link VIEW often opens empty chats.
        if (shareTextToPackage(pkg, text, fromCode = null, toCode = null)) return

        val deepLinks = when {
            pkg.contains("openai") || pkg.contains("chatgpt") -> listOf(
                Uri.parse("https://chatgpt.com/?q=${Uri.encode(text)}"),
                Uri.parse("https://chat.openai.com/?q=${Uri.encode(text)}"),
            )
            pkg.contains("bard") || pkg.contains("gemini") -> listOf(
                Uri.parse("https://gemini.google.com/app?q=${Uri.encode(text)}"),
            )
            pkg.contains("perplexity") -> listOf(
                Uri.parse("https://www.perplexity.ai/search?q=${Uri.encode(text)}"),
            )
            pkg.contains("claude") || pkg.contains("anthropic") -> listOf(
                Uri.parse("https://claude.ai/new?q=${Uri.encode(text)}"),
            )
            pkg.contains("copilot") || pkg == "com.microsoft.bing" -> listOf(
                Uri.parse("https://copilot.microsoft.com/?q=${Uri.encode(text)}"),
            )
            else -> emptyList()
        }
        for (uri in deepLinks) {
            val view = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg)
            if (startIfResolvable(view)) return
            if (startIfResolvable(Intent(Intent.ACTION_VIEW, uri))) return
        }
        val search = Intent(Intent.ACTION_SEARCH)
            .setPackage(pkg)
            .putExtra(android.app.SearchManager.QUERY, text)
        if (startIfResolvable(search)) return
        copyPlainText(text)
        launchPackage(pkg)
    }

    fun insertCalendar(title: String, beginMillis: Long?, endMillis: Long?) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
        if (beginMillis != null) intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
        if (endMillis != null) intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        if (!startIfResolvable(intent)) {
            start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
        }
    }

    fun insertContact(app: InstalledActionApp, name: String, phone: String) {
        val pkg = app.packageName
        if (!ActionApps.isReliableContactInsert(pkg)) {
            error("${app.name} can't add contacts")
        }
        if (tryInsertInPackage(pkg, name, phone)) return
        val fallback = DefaultCommandApps.packageFor(context, ActionAppKind.Contacts)
        if (!fallback.isNullOrBlank() &&
            fallback != pkg &&
            ActionApps.isReliableContactInsert(fallback) &&
            tryInsertInPackage(fallback, name, phone)
        ) {
            return
        }
        error("No contact editor in $pkg")
    }

    private fun tryInsertInPackage(pkg: String, name: String, phone: String): Boolean {
        if (!ActionApps.isReliableContactInsert(pkg)) return false
        fun base(): Intent {
            val intent = Intent(Intent.ACTION_INSERT)
                .setType(ContactsContract.Contacts.CONTENT_TYPE)
                .setPackage(pkg)
            if (name.isNotBlank()) intent.putExtra(ContactsContract.Intents.Insert.NAME, name)
            if (phone.isNotBlank()) intent.putExtra(ContactsContract.Intents.Insert.PHONE, phone)
            return intent
        }
        for (cls in knownContactEditors(pkg)) {
            if (startFromUi(base().setClassName(pkg, cls))) return true
        }
        if (startExplicitInPackage(base(), pkg)) return true
        return startFromUi(base())
    }

    private fun knownContactEditors(packageName: String): List<String> {
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("google.android.contacts") -> listOf(
                "com.google.android.apps.contacts.editorlite.ContactsEditorlite",
                "com.android.contacts.activities.CompactContactEditorActivity",
                "com.android.contacts.activities.ContactEditorActivity",
            )
            pkg.contains("android.contacts") || pkg.contains("miui.contacts") ||
                pkg.contains("samsung") && pkg.contains("contact") -> listOf(
                "com.android.contacts.activities.ContactEditorActivity",
                "com.android.contacts.activities.CompactContactEditorActivity",
            )
            else -> emptyList()
        }
    }

    /**
     * HyperOS [resolveActivity] for contact INSERT is the system chooser (Eyecon also registers).
     * Pick an exported editor in [packageName] and start that component with extras.
     */
    private fun startExplicitInPackage(intent: Intent, packageName: String): Boolean {
        if (ActionApps.isCallerIdPackage(packageName)) return false
        val matches = runCatching {
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrDefault(emptyList()).mapNotNull { it.activityInfo }
            .filter { it.exported && it.packageName == packageName }
            .filter { !ActionApps.isCallerIdPackage(it.packageName) }
        val info = matches.firstOrNull { it.name.contains("Editor", ignoreCase = true) }
            ?: matches.firstOrNull()
            ?: return false
        intent.component = ComponentName(info.packageName, info.name)
        intent.setPackage(packageName)
        return startFromUi(intent)
    }

    private fun startFromUi(intent: Intent): Boolean {
        if (blocksCallerIdContactInsert(intent)) return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = runCatching { MadApp.instance.foregroundActivity }.getOrNull()
        return runCatching {
            if (activity != null && !activity.isFinishing) {
                activity.startActivity(intent)
            } else {
                context.startActivity(intent)
            }
            true
        }.getOrDefault(false)
    }

    fun shareNote(text: String) {
        start(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text)
                .putExtra(Intent.EXTRA_SUBJECT, "Note"),
        )
    }

    /** Open a notes app to create [text] (Keep create-note intent, else SEND). */
    fun createNoteInApp(app: InstalledActionApp, text: String) {
        val body = text.trim()
        val keep = Intent("com.google.android.keep.intent.action.CREATE_NOTE")
            .setPackage(app.packageName)
            .putExtra(Intent.EXTRA_TEXT, body)
        if (body.isNotEmpty() && startIfResolvable(keep)) return
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .setPackage(app.packageName)
            .putExtra(Intent.EXTRA_TEXT, body)
            .putExtra(Intent.EXTRA_SUBJECT, "Note")
        if (startIfResolvable(send)) return
        launchPackage(app.packageName)
    }

    fun openAppSettings(packageName: String) {
        val uri = Uri.parse("package:$packageName")
        if (startIfResolvable(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))) return
        openSystemSettings()
    }

    fun uninstallApp(packageName: String) {
        if (packageName.isBlank()) error("No package")
        val data = Uri.fromParts("package", packageName, null)
        // ACTION_UNINSTALL_PACKAGE shows the system confirmation; requires REQUEST_DELETE_PACKAGES.
        @Suppress("DEPRECATION")
        val uninstall = Intent(Intent.ACTION_UNINSTALL_PACKAGE).setData(data)
        if (startIfResolvable(uninstall)) return
        val delete = Intent(Intent.ACTION_DELETE).setData(data)
        if (startIfResolvable(delete)) return
        // Last resort: app details where the user can uninstall manually.
        if (startIfResolvable(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, data))) return
        error("No uninstaller available")
    }

    fun launchPackage(packageName: String) {
        if (!launchPackageSafe(packageName)) {
            error("Can't open $packageName")
        }
    }

    fun openClock() {
        if (launchCategory("android.intent.category.APP_CLOCK")) return
        // Discover packages that handle alarm intents, but open their MAIN activity
        // (SHOW_ALARMS trampolines crash on HyperOS/MIUI).
        val fromAlarms = packagesFor(Intent(AlarmClock.ACTION_SHOW_ALARMS)) +
            packagesFor(Intent(AlarmClock.ACTION_SET_ALARM))
        if (launchFirst(fromAlarms)) return
        if (launchFirst(
                listOf(
                    "com.android.deskclock",
                    "com.google.android.deskclock",
                    "com.sec.android.app.clockpackage",
                    "com.oneplus.deskclock",
                    "com.miui.clock",
                ),
            )
        ) return
        error("No clock app")
    }

    /**
     * Opens the stock Clock SET_TIMER / SET_ALARM UI (never SKIP_UI, never SHOW_ALARMS).
     * Delay phrases (`in 20 min`) become a timer; clock times (`at 8pm`) become an alarm.
     */
    fun setReminder(raw: String) {
        val parsed = ReminderParser.parse(raw)
        val intent = when {
            parsed.timerSeconds != null ->
                Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, parsed.timerSeconds.coerceIn(1, 24 * 60 * 60))
                    .also { if (parsed.message.isNotBlank()) it.putExtra(AlarmClock.EXTRA_MESSAGE, parsed.message.take(160)) }
            parsed.hour != null ->
                Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, parsed.hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, parsed.minute.coerceIn(0, 59))
                    .also { if (parsed.message.isNotBlank()) it.putExtra(AlarmClock.EXTRA_MESSAGE, parsed.message.take(160)) }
            else ->
                Intent(AlarmClock.ACTION_SET_ALARM)
                    .also { if (parsed.message.isNotBlank()) it.putExtra(AlarmClock.EXTRA_MESSAGE, parsed.message.take(160)) }
        }
        if (startClockAction(withClockSetupFlags(intent))) return
        openClock()
    }

    /**
     * SET_ALARM / SET_TIMER must not become Clock's launch intent. HyperOS/MIUI Clock
     * otherwise re-opens the same setup UI every time the Clock app is tapped, even
     * after the user deleted the alarm.
     */
    private fun withClockSetupFlags(intent: Intent): Intent {
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
        )
        return intent
    }

    private fun startClockAction(intent: Intent): Boolean {
        if (startIfResolvable(intent)) return true
        val pkgs = (
            packagesFor(Intent(AlarmClock.ACTION_SET_ALARM)) +
                packagesFor(Intent(AlarmClock.ACTION_SET_TIMER)) +
                listOf(
                    "com.android.deskclock",
                    "com.google.android.deskclock",
                    "com.sec.android.app.clockpackage",
                    "com.oneplus.deskclock",
                    "com.miui.clock",
                )
            ).distinct()
        for (pkg in pkgs) {
            if (startIfResolvableInPackage(withClockSetupFlags(Intent(intent).setPackage(pkg)), pkg)) return true
        }
        return false
    }

    fun openCalendar() {
        if (launchCategory(Intent.CATEGORY_APP_CALENDAR)) return
        val view = Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}"))
        if (launchFirst(packagesFor(view))) return
        if (launchFirst(
                listOf(
                    "com.google.android.calendar",
                    "com.xiaomi.calendar",
                    "com.android.calendar",
                    "com.samsung.android.calendar",
                ),
            )
        ) return
        error("No calendar app")
    }

    fun openWeather() {
        // MIUI Weather (com.miui.weather2) crashes on this HyperOS build for main + location
        // flows — skip it and prefer other weather apps, then a browser forecast.
        val preferred = listOf(
            "com.google.android.apps.weather",
            "com.samsung.android.weather",
            "com.huawei.android.totemweather",
            "com.sec.android.daemonapp",
            "com.accuweather.android",
            "com.weather.Weather",
        )
        if (launchLauncherPackages(preferred)) return
        val discovered = discoverWeatherPackages().filterNot { WeatherLaunchGuard.isBlocked(it) }
        if (launchLauncherPackages(discovered)) return
        openWeatherInBrowser()
    }

    private fun openWeatherInBrowser() {
        val coords = runCatching {
            com.madlauncher.data.DevicePlaceFinder.coordinates(context)
        }.getOrNull()
        val uri = if (coords != null) {
            val (lat, lon) = coords
            Uri.parse(
                "https://www.google.com/search?q=${Uri.encode("weather")}&near=${Uri.encode("$lat,$lon")}",
            )
        } else {
            Uri.parse("https://www.google.com/search?q=${Uri.encode("weather")}")
        }
        start(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun launchCategory(category: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return launchLauncherPackages(packagesFor(intent))
    }

    private fun launchFirst(packages: Iterable<String>): Boolean = launchLauncherPackages(packages)

    private fun launchLauncherPackages(packages: Iterable<String>): Boolean {
        val seen = LinkedHashSet<String>()
        for (pkg in packages) {
            if (pkg.isBlank() || !seen.add(pkg)) continue
            if (launchLauncherActivity(pkg)) return true
        }
        return false
    }

    private fun packagesFor(intent: Intent): List<String> {
        val pm = context.packageManager
        val flags = PackageManager.MATCH_DEFAULT_ONLY
        val resolved = runCatching { pm.resolveActivity(intent, flags)?.activityInfo?.packageName }.getOrNull()
        val queried = runCatching {
            pm.queryIntentActivities(intent, flags).mapNotNull { it.activityInfo?.packageName }
        }.getOrDefault(emptyList())
        return buildList {
            if (!resolved.isNullOrBlank()) add(resolved)
            queried.forEach { if (it !in this) add(it) }
        }
    }

    private fun discoverWeatherPackages(): List<String> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = runCatching { pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL) }
            .getOrDefault(emptyList())
        val tokens = listOf(
            "weather", "wetter", "meteo", "pogoda", "天気", "날씨", "погода", "hava", "clima",
        )
        val blocked = listOf(
            "googlequicksearchbox", "chrome", "browser", "firefox", "opera",
            "samsung.android.sbrowser", "miui.browser", "duckduckgo",
        )
        data class Hit(val pkg: String, val score: Int)
        val hits = mutableListOf<Hit>()
        for (ri in apps) {
            val pkg = ri.activityInfo?.packageName ?: continue
            val pkgLower = pkg.lowercase()
            if (blocked.any { pkgLower.contains(it) }) continue
            val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault("").lowercase()
            val pkgHit = tokens.any { pkgLower.contains(it) }
            val labelHit = tokens.any { label.contains(it) }
            if (!pkgHit && !labelHit) continue
            hits += Hit(pkg, if (pkgHit) 2 else 1)
        }
        return hits.sortedByDescending { it.score }.map { it.pkg }.distinct()
    }

    /**
     * Launch an app the same way a system launcher should: [LauncherApps.startMainActivity].
     * Falls back to an explicit MAIN/LAUNCHER intent with NEW_TASK only.
     */
    private fun launchLauncherActivity(packageName: String): Boolean {
        if (ActionApps.isCallerIdPackage(packageName)) return false
        val pm = context.packageManager
        val probe = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
        val info = runCatching {
            pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
                ?: pm.queryIntentActivities(probe, PackageManager.MATCH_ALL)
                    .firstOrNull()?.activityInfo
        }.getOrNull() ?: return false
        val component = ComponentName(info.packageName, info.name)
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        if (launcherApps != null) {
            val started = runCatching {
                launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
                true
            }.getOrDefault(false)
            if (started) return true
        }
        val launch = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launch)
            true
        }.getOrDefault(false)
    }

    /** Launch an app's MAIN/LAUNCHER activity; returns false if missing or start failed. */
    private fun launchPackageSafe(packageName: String): Boolean = launchLauncherActivity(packageName)

    /**
     * Search inside an app.
     *
     * Order matters: curated deeplinks first (Play/Maps/IG often break on ACTION_SEARCH),
     * then real package-scoped SEARCH intents (never the system ResolverActivity),
     * then clipboard + launch for apps that only expose multi-step in-app search UI.
     */
    fun searchInApp(
        packageName: String,
        query: String,
        urls: List<String>,
        assisted: Boolean = false,
        searchComponent: ComponentName? = null,
    ): SearchInAppResult {
        val q = query.trim()
        if (q.isEmpty()) return SearchInAppResult.Failed
        val linkList = urls.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        if (searchComponent != null && startComponentSearch(searchComponent, q)) {
            return SearchInAppResult.OpenedSearchIntent
        }
        // 1) Prefer curated / alternate deeplinks inside the target package.
        for (url in linkList) {
            if (startViewInPackage(packageName, url)) return SearchInAppResult.OpenedDeepLink
        }
        // 2) Package-scoped SEARCH / WEB_SEARCH only if the resolved activity is in that package.
        if (startPackageSearch(Intent.ACTION_SEARCH, packageName, q)) {
            return SearchInAppResult.OpenedSearchIntent
        }
        val discovered = SearchableApps.findSearchComponent(context, packageName)
        if (discovered != null && startComponentSearch(discovered, q)) {
            return SearchInAppResult.OpenedSearchIntent
        }
        if (startPackageSearch(Intent.ACTION_WEB_SEARCH, packageName, q)) {
            return SearchInAppResult.OpenedSearchIntent
        }
        // 3) Deeplink without package (last chance — may open browser).
        for (url in linkList) {
            if (startIfResolvable(Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
                return SearchInAppResult.OpenedDeepLink
            }
        }
        // 4) Multi-step apps (Instagram, Discord, …): copy query and open the app.
        if (assisted || linkList.isEmpty()) {
            copyQuery(q)
            if (launchPackageSafe(packageName)) return SearchInAppResult.AssistedClipboard
        } else {
            // Still assist when deeplink/"SEARCH" both failed to land a useful UI.
            copyQuery(q)
            if (launchPackageSafe(packageName)) return SearchInAppResult.AssistedClipboard
        }
        return SearchInAppResult.Failed
    }

    /** Backward-compatible overload used by older call sites. */
    fun searchInApp(packageName: String, query: String, url: String): SearchInAppResult {
        return searchInApp(packageName, query, listOfNotNull(url.takeIf { it.isNotBlank() }))
    }

    private fun copyQuery(query: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            ?: return
        cm.setPrimaryClip(android.content.ClipData.newPlainText("MAD search", query))
    }

    private fun startViewInPackage(packageName: String, url: String): Boolean {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(packageName)
        return startIfResolvableInPackage(view, packageName)
    }

    private fun startComponentSearch(component: ComponentName, query: String): Boolean {
        if (component.packageName == "android") return false
        if (component.className.contains("ResolverActivity", ignoreCase = true)) return false
        val intent = Intent(Intent.ACTION_SEARCH)
            .setComponent(component)
            .putExtra(android.app.SearchManager.QUERY, query)
            .putExtra("query", query)
        return startIfResolvableInPackage(intent, component.packageName)
    }

    private fun startPackageSearch(action: String, packageName: String, query: String): Boolean {
        val intent = Intent(action)
            .setPackage(packageName)
            .putExtra(android.app.SearchManager.QUERY, query)
        return startIfResolvableInPackage(intent, packageName)
    }

    /**
     * Like [startIfResolvable], but rejects system choosers / activities outside [packageName].
     * Play Store and others resolve ACTION_SEARCH to ResolverActivity — that must not "win".
     */
    private fun startIfResolvableInPackage(intent: Intent, packageName: String): Boolean {
        if (ActionApps.isCallerIdPackage(packageName)) return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolved = intent.resolveActivity(context.packageManager) ?: return false
        val pkg = resolved.packageName
        val cls = resolved.className
        if (pkg != packageName) return false
        if (pkg == "android" || cls.contains("ResolverActivity", ignoreCase = true)) return false
        return startFromUi(intent)
    }

    private fun openAppComposer(app: InstalledActionApp, body: String) {
        copyPlainText(body)
        launchPackage(app.packageName)
    }

    private fun copyPlainText(text: String) {
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clip?.setPrimaryClip(ClipData.newPlainText("MAD", text))
    }

    private fun shareTo(packageName: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
            .setPackage(packageName)
        if (!startIfResolvable(intent)) {
            start(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                    "Share",
                ),
            )
        }
    }

    fun openLiveWallpaperChooser() {
        if (startIfResolvable(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))) return
        start(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
    }

    private fun startIfResolvable(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (blocksCallerIdContactInsert(intent)) return false
        if (intent.resolveActivity(context.packageManager) == null) return false
        return startFromUi(intent)
    }

    private fun start(intent: Intent) {
        if (blocksCallerIdContactInsert(intent)) return
        startFromUi(intent)
    }

    /** INSERT extras crash Eyecon; DIAL / view-number to a preferred caller-ID dialer is OK. */
    private fun blocksCallerIdContactInsert(intent: Intent): Boolean {
        if (!isContactInsertIntent(intent)) return false
        intent.`package`?.let { if (ActionApps.isCallerIdPackage(it)) return true }
        intent.component?.packageName?.let { if (ActionApps.isCallerIdPackage(it)) return true }
        val resolved = runCatching { intent.resolveActivity(context.packageManager) }.getOrNull() ?: return false
        if (resolved.packageName == "android" ||
            resolved.className.contains("ResolverActivity", ignoreCase = true)
        ) {
            return false
        }
        return ActionApps.isCallerIdPackage(resolved.packageName)
    }

    private fun isContactInsertIntent(intent: Intent): Boolean {
        val action = intent.action ?: return false
        if (action != Intent.ACTION_INSERT && action != ContactsContract.Intents.Insert.ACTION) {
            return false
        }
        val type = intent.type.orEmpty()
        val data = intent.dataString.orEmpty()
        return type.contains("contact", ignoreCase = true) ||
            data.contains("contact", ignoreCase = true)
    }
}
