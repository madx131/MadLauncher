package com.madlauncher.action

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionAppsSocialCatalogTest {
    @Test
    fun catalog_includesMetaAndTruthSocial() {
        val names = ActionApps.socialCatalog.map { it.name }
        val pkgs = ActionApps.socialCatalog.flatMap { it.packages }
        assertTrue(names.contains("Truth Social"))
        assertTrue(pkgs.contains("com.truthsocial.android.app"))
        assertTrue(names.contains("Facebook"))
        assertTrue(names.contains("X"))
        assertTrue(pkgs.contains("com.twitter.android"))
        assertTrue(pkgs.contains("com.x.android"))
    }

    @Test
    fun looksLikeSocial_truthSocialAndMeta() {
        assertTrue(ActionApps.looksLikeSocial("Truth Social", "com.truthsocial.android.app"))
        assertTrue(ActionApps.looksLikeSocial("truth", "com.truthsocial.android.app"))
        assertTrue(ActionApps.looksLikeSocial("X", "com.twitter.android"))
        assertTrue(ActionApps.looksLikeSocial("X", "com.x.android"))
        assertTrue(ActionApps.looksLikeSocial("Instagram", "com.instagram.android"))
        assertFalse(ActionApps.looksLikeSocial("ColorNote", "com.socialnmobile.dictapps.notepad.color.note"))
    }

    @Test
    fun looksLikeMessaging_chatAppsNotEmail() {
        assertTrue(ActionApps.looksLikeMessaging("Telegram", "org.telegram.messenger"))
        assertTrue(ActionApps.looksLikeMessaging("Viber", "com.viber.voip"))
        assertTrue(ActionApps.looksLikeMessaging("LINE", "jp.naver.line.android"))
        assertFalse(ActionApps.looksLikeMessaging("Gmail", "com.google.android.gm"))
    }

    @Test
    fun looksLikeEmail_gmailAndOutlook() {
        assertTrue(ActionApps.looksLikeEmail("Gmail", "com.google.android.gm"))
        assertTrue(ActionApps.looksLikeEmail("Outlook", "com.microsoft.office.outlook"))
        assertTrue(ActionApps.looksLikeEmail("Proton Mail", "ch.protonmail.android"))
        assertFalse(ActionApps.looksLikeEmail("WhatsApp", "com.whatsapp"))
    }

    @Test
    fun looksLikeNav_mapsAndWaze() {
        assertTrue(ActionApps.looksLikeNav("Google Maps", "com.google.android.apps.maps"))
        assertTrue(ActionApps.looksLikeNav("Waze", "com.waze"))
        assertFalse(ActionApps.looksLikeNav("Gmail", "com.google.android.gm"))
    }

    @Test
    fun looksLikeAi_andNotes_andTranslator() {
        assertTrue(ActionApps.looksLikeAi("ChatGPT", "com.openai.chatgpt"))
        assertTrue(ActionApps.looksLikeAi("Claude", "com.anthropic.claude"))
        assertTrue(ActionApps.looksLikeNotes("Keep", "com.google.android.keep"))
        assertFalse(ActionApps.looksLikeNotes("Notifications", "com.android.systemui"))
        assertTrue(ActionApps.looksLikeTranslator("Google Translate", "com.google.android.apps.translate"))
        assertTrue(ActionApps.looksLikeTranslator("DeepL", "com.deepl.mobiletranslator"))
    }

    @Test
    fun looksLikeContacts_skipsEyecon() {
        assertTrue(ActionApps.looksLikeContacts("Contacts", "com.google.android.contacts"))
        assertTrue(ActionApps.looksLikeContacts("Contacts", "com.android.contacts"))
        assertTrue(ActionApps.looksLikeContacts("Contacts", "com.miui.contacts"))
        assertTrue(ActionApps.isReliableContactInsert("com.google.android.contacts"))
        assertFalse(ActionApps.looksLikeContacts("New contact", "com.eyecon.global"))
        assertFalse(ActionApps.isReliableContactInsert("com.eyecon.global"))
        assertFalse(ActionApps.isReliableContactInsert("com.truecaller"))
        assertTrue(ActionApps.isCallerIdPackage("com.eyecon.global"))
        assertTrue(ActionApps.isCallerIdPackage("com.truecaller"))
    }

    @Test
    fun looksLikeMessaging_skipsEyeconSms() {
        assertTrue(ActionApps.looksLikeMessaging("Messages", "com.google.android.apps.messaging"))
        assertFalse(ActionApps.looksLikeMessaging("SMS", "com.eyecon.global"))
        assertFalse(ActionApps.looksLikeMessaging("WhatsApp", "com.eyecon.global"))
    }
}
