package com.madlauncher.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.madlauncher.data.PermissionChecker

data class PlatformFixStep(
    val label: String,
    val intent: Intent?,
)

object PlatformHelp {
    fun featureTitle(feature: PlatformFeature): String = when (feature) {
        PlatformFeature.NOTIF_SHADE_MIRROR -> "Messages & System & apps mirror"
        PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC -> "Dismiss sync with status bar"
        PlatformFeature.NOTIF_BADGES -> "App icon badges"
        PlatformFeature.QS_MIRROR -> "Quick Settings mirror"
        PlatformFeature.QS_TOGGLE_SYNC -> "Quick Settings toggles"
        PlatformFeature.NAV_SHORTCUT_OVERLAY -> "Nav bar shortcuts"
        PlatformFeature.SYSTEM_SHADE_EXPAND -> "Open system notification shade"
        PlatformFeature.ACTIONBOX_RECIPIENT_HISTORY -> "Action Box recent recipients"
        PlatformFeature.ACTIONBOX_WHATSAPP_CALL -> "WhatsApp calls (# / *)"
        PlatformFeature.ACTIONBOX_PHONE_CALL -> "Phone calls (# / *)"
        PlatformFeature.ACTIONBOX_OTT_CALL -> "Other app calls (Messenger, Telegram…)"
        PlatformFeature.ACTIONBOX_SOCIAL_POST -> "Social posts (:)"
        PlatformFeature.ACTIONBOX_META_SOCIAL -> "Facebook / Instagram posts (:)"
        PlatformFeature.ACTIONBOX_MESSAGING -> "Messages (@)"
        PlatformFeature.ACTIONBOX_CONTACT_LOOKUP -> "Contact lookup (& / @)"
        PlatformFeature.ACTIONBOX_AI -> "Ask AI (?)"
        PlatformFeature.ACTIONBOX_TRANSLATE -> "Translate (/t)"
        PlatformFeature.ACTIONBOX_NOTES -> "Notes (/n)"
        PlatformFeature.ACTIONBOX_NAV -> "Navigation (nav)"
        PlatformFeature.ACTIONBOX_SOCIAL_DM -> "Social DMs in apps (@)"
        PlatformFeature.ACTIONBOX_TRUTH_POST -> "Social posts in apps (space)"
    }

    fun statusLabel(status: FeatureStatus): String = when (status) {
        FeatureStatus.WORKING -> "Working"
        FeatureStatus.DEGRADED -> "Limited"
        FeatureStatus.UNAVAILABLE -> "Not working"
        FeatureStatus.UNKNOWN -> "Needs setup"
        FeatureStatus.NOT_INSTALLED -> "Not installed"
    }

    fun explanation(feature: PlatformFeature, family: OemFamily, status: FeatureStatus): String {
        if (status == FeatureStatus.WORKING) {
            return "This feature is fully supported on your device."
        }
        if (status == FeatureStatus.NOT_INSTALLED) {
            return notInstalledExplanation(feature)
        }
        return when (feature) {
            PlatformFeature.NOTIF_SHADE_MIRROR -> when (family) {
                OemFamily.XIAOMI_HYPEROS, OemFamily.MIUI ->
                    "HyperOS may hide or re-post some alerts. MAD shows what the notification listener receives — it may not match the stock shade exactly."
                else ->
                    "Notification access must be enabled and the listener connected for a live mirror of the status bar."
            }
            PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC ->
                "Dismiss or mark-read in MAD cancels the same shade key; swipe/open in the shade drops it in MAD. If a ROM ignores cancel (HyperOS, ColorOS, some EMUI), MAD snoozes that row only after cancel fails. A newer message still shows. Notification listener access is required."
            PlatformFeature.NOTIF_BADGES -> "Icon badges use notification access (Permissions & setup). Once that is granted, unread dots and counts work on this phone."
            PlatformFeature.QS_MIRROR -> "Quick Settings mirror reads the system tile list. Some ROMs block the setting."
            PlatformFeature.QS_TOGGLE_SYNC -> when (family) {
                OemFamily.XIAOMI_HYPEROS, OemFamily.MIUI ->
                    "Stock tiles (Wi‑Fi, sound, mobile data) often ignore status-bar clicks on HyperOS. MAD uses local toggles when possible."
                else -> "Toggles need permission to change settings or a working status-bar click path."
            }
            PlatformFeature.NAV_SHORTCUT_OVERLAY -> when (family) {
                OemFamily.XIAOMI_HYPEROS, OemFamily.MIUI ->
                    "Recents Clear all on HyperOS can remove overlay buttons. Accessibility must stay on; pin MAD in recents if buttons vanish."
                OemFamily.STOCK ->
                    "On MAD OS, nav shortcuts are owned by SystemUI as a system default — Accessibility is not required."
                else -> "Nav shortcuts need accessibility enabled and battery exemption."
            }
            PlatformFeature.SYSTEM_SHADE_EXPAND -> "Opening the stock shade from MAD may be limited on some OEM skins."
            PlatformFeature.ACTIONBOX_RECIPIENT_HISTORY ->
                "Recent people in @ / # / * come from notification history and phone contacts — not full in-app chat lists. Notification access must be on."
            PlatformFeature.ACTIONBOX_WHATSAPP_CALL ->
                "WhatsApp calls use synced phone contacts (voip MIME). Allow Contacts in Permissions & setup. Accessibility is only a fallback when a number isn't on the contact card."
            PlatformFeature.ACTIONBOX_PHONE_CALL ->
                "Phone calls need a dialer app. Install or enable Phone / Contacts dialer to use # and * for cellular calls."
            PlatformFeature.ACTIONBOX_OTT_CALL ->
                "Messenger, Telegram, Signal, Instagram, Discord, and similar apps have no reliable public call API on Android. " +
                    "MAD only supports Phone and WhatsApp for # and *. Open those apps directly to call there."
            PlatformFeature.ACTIONBOX_SOCIAL_POST ->
                "Space posts into social apps do not open inside those apps. They are listed under Not working. Chats that already open in the app stay there until you turn that app on under Open in browser."
            PlatformFeature.ACTIONBOX_SOCIAL_DM -> when (status) {
                FeatureStatus.DEGRADED ->
                    "Opening messages inside those apps still does not work. Apps turned on under Experimental → Open in browser use the website, and they share one browser tab."
                else ->
                    "Opening messages inside X, Instagram, LinkedIn, TikTok, Reddit, Truth Social, Threads, Bluesky, Mastodon, Tumblr, Discord, Pinterest, Weibo, and VK does not work. Turn on each app under Settings → Experimental → Open in browser. Messenger searches inside the app."
            }
            PlatformFeature.ACTIONBOX_TRUTH_POST -> when (status) {
                FeatureStatus.DEGRADED ->
                    "Those apps still do not accept Space posts. Each app turned on under Experimental → Open in browser uses the website. They share one browser tab."
                else ->
                    "Space posts into X, LinkedIn, Reddit, Truth Social, Instagram, Threads, TikTok, Discord, Bluesky, Mastodon, Tumblr, Pinterest, Weibo, and VK do not work in the apps. Turn on each app under Settings → Experimental → Open in browser."
            }
            PlatformFeature.ACTIONBOX_META_SOCIAL -> when (status) {
                FeatureStatus.DEGRADED ->
                    "Instagram and Threads still block posts from other apps. Turn each one on under Experimental → Open in browser to use the website. They share one browser tab. Facebook's site only offers the app."
                else ->
                    "Instagram and Threads block posts from other apps. Turn each one on under Settings → Experimental → Open in browser. Facebook's site only offers the app."
            }
            PlatformFeature.ACTIONBOX_MESSAGING ->
                "@ picks an app and a contact, then opens that chat ready to type. Nothing is sent until you tap Send in the app. Recents need notification access."
            PlatformFeature.ACTIONBOX_CONTACT_LOOKUP ->
                "Contact pickers work best with Contacts permission. Without it, MAD falls back to recent notification people and typed numbers."
            PlatformFeature.ACTIONBOX_AI ->
                "AI apps rarely accept deep-link prompts. Type ? then your question — MAD shares or copies it into the app."
            PlatformFeature.ACTIONBOX_TRANSLATE ->
                "/t and !tr open the translate site with your text. A translator app is optional — pick one if you want the app instead."
            PlatformFeature.ACTIONBOX_NOTES ->
                "Install a notes app to use /n. MAD clears the Action Box then opens notes with your text when possible."
            PlatformFeature.ACTIONBOX_NAV ->
                "Install Maps, Waze, or another navigation app to use the nav command."
        }
    }

    private fun notInstalledExplanation(feature: PlatformFeature): String = when (feature) {
        PlatformFeature.ACTIONBOX_SOCIAL_POST ->
            "No social apps on this phone (X, Facebook, Instagram, LinkedIn, Reddit, and similar). ! shows them after you install one — MAD is fine."
        PlatformFeature.ACTIONBOX_META_SOCIAL ->
            "Facebook and Instagram aren't installed. ! for those apps appears after you install them."
        PlatformFeature.ACTIONBOX_WHATSAPP_CALL ->
            "WhatsApp isn't installed. # and * for WhatsApp appear after you install it."
        PlatformFeature.ACTIONBOX_PHONE_CALL ->
            "No Phone / dialer app on this device. # and * for cellular calls need one."
        PlatformFeature.ACTIONBOX_MESSAGING ->
            "No messaging or email apps installed. @ appears after you install one (Messages, WhatsApp, Gmail…)."
        PlatformFeature.ACTIONBOX_CONTACT_LOOKUP ->
            "No contacts, messaging, or phone apps to look people up from."
        PlatformFeature.ACTIONBOX_AI ->
            "No AI apps installed (ChatGPT, Gemini, Claude…). ? appears after you install one."
        PlatformFeature.ACTIONBOX_TRANSLATE ->
            "/t and !tr open the translate site. Install a translator app if you want that instead."
        PlatformFeature.ACTIONBOX_NOTES ->
            "No notes app installed. /n appears after you install one."
        PlatformFeature.ACTIONBOX_NAV ->
            "No maps app installed. nav appears after you install Maps or Waze."
        PlatformFeature.ACTIONBOX_SOCIAL_DM ->
            "Opening social DMs inside the apps does not work yet."
        PlatformFeature.ACTIONBOX_TRUTH_POST ->
            "Space posts into those social apps do not work yet."
        else ->
            "This isn't on the phone. MAD only lists it after the app is installed."
    }

    fun fixSteps(context: Context, feature: PlatformFeature, family: OemFamily): List<PlatformFixStep> {
        val pkg = context.packageName
        val pm = context.packageManager
        val steps = mutableListOf<PlatformFixStep>()

        when (feature) {
            PlatformFeature.NOTIF_SHADE_MIRROR,
            PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC,
            PlatformFeature.NOTIF_BADGES,
            -> {
                steps += PlatformFixStep(
                    "Notification access",
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                )
            }
            PlatformFeature.NAV_SHORTCUT_OVERLAY -> {
                if (!MadOsRuntime.isMadOs(context)) {
                    steps += PlatformFixStep(
                        "Accessibility",
                        PermissionChecker.accessibilitySettings(context),
                    )
                }
            }
            PlatformFeature.QS_MIRROR, PlatformFeature.QS_TOGGLE_SYNC -> Unit
            PlatformFeature.SYSTEM_SHADE_EXPAND -> Unit
            PlatformFeature.ACTIONBOX_RECIPIENT_HISTORY -> {
                steps += PlatformFixStep(
                    "Notification access",
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                )
            }
            PlatformFeature.ACTIONBOX_WHATSAPP_CALL -> {
                steps += PlatformFixStep(
                    "Contacts",
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                )
                steps += PlatformFixStep(
                    "Accessibility",
                    PermissionChecker.accessibilitySettings(context),
                )
            }
            PlatformFeature.ACTIONBOX_PHONE_CALL -> Unit
            PlatformFeature.ACTIONBOX_OTT_CALL -> Unit
            PlatformFeature.ACTIONBOX_META_SOCIAL -> Unit
            PlatformFeature.ACTIONBOX_SOCIAL_POST -> {
                steps += PlatformFixStep(
                    "Accessibility",
                    PermissionChecker.accessibilitySettings(context),
                )
            }
            PlatformFeature.ACTIONBOX_MESSAGING,
            PlatformFeature.ACTIONBOX_CONTACT_LOOKUP,
            -> {
                steps += PlatformFixStep(
                    "Notification access",
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                )
                steps += PlatformFixStep(
                    "Contacts",
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                )
            }
            PlatformFeature.ACTIONBOX_AI,
            PlatformFeature.ACTIONBOX_TRANSLATE,
            PlatformFeature.ACTIONBOX_NOTES,
            PlatformFeature.ACTIONBOX_NAV,
            PlatformFeature.ACTIONBOX_SOCIAL_DM,
            PlatformFeature.ACTIONBOX_TRUTH_POST,
            -> Unit
        }

        when (family) {
            OemFamily.XIAOMI_HYPEROS, OemFamily.MIUI -> {
                steps += PlatformFixStep("Autostart (Security app)", xiaomiAutostartIntent(pm, pkg))
                steps += PlatformFixStep("Battery: No restrictions", appDetailsIntent(pkg))
            }
            OemFamily.SAMSUNG -> {
                steps += PlatformFixStep("Sleeping apps exemption", samsungBatteryIntent(pm))
                steps += PlatformFixStep("App battery settings", appDetailsIntent(pkg))
            }
            else -> {
                steps += PlatformFixStep("Battery optimization", PermissionChecker.batteryRequest(context))
            }
        }

        if (feature == PlatformFeature.NAV_SHORTCUT_OVERLAY) {
            steps += PlatformFixStep("Battery optimization", PermissionChecker.batteryRequest(context))
        }

        return steps.distinctBy { it.label }
    }

    private fun appDetailsIntent(pkg: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))

    private fun xiaomiAutostartIntent(pm: android.content.pm.PackageManager, pkg: String): Intent {
        val intents = listOf(
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.MainAcitivty",
            ),
        )
        return intents.firstOrNull { it.resolveActivity(pm) != null }
            ?: appDetailsIntent(pkg)
    }

    private fun samsungBatteryIntent(pm: android.content.pm.PackageManager): Intent? {
        val intent = Intent().setClassName(
            "com.samsung.android.lool",
            "com.samsung.android.sm.battery.ui.BatteryActivity",
        )
        return if (intent.resolveActivity(pm) != null) intent else null
    }
}
