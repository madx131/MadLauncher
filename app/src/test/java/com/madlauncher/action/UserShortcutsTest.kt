package com.madlauncher.action

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserShortcutsTest {
    @After
    fun reset() {
        UserShortcuts.replace(emptyList())
        CommandAliases.replaceExtras(UserAliasCatalog.Empty)
        CommandAliases.replaceDisabledBuiltIns(emptySet())
    }

    @Test
    fun slashDAndDoOpenApp() {
        UserShortcuts.replace(
            listOf(UserShortcut("1", listOf("/d", "do"), "com.todoist")),
        )
        val slash = CommandDetector.detect("/d buy milk")
        assertTrue(slash is ActionCommand.OpenApp)
        assertEquals("com.todoist", (slash as ActionCommand.OpenApp).packageName)
        assertEquals("buy milk", slash.payload)
        val word = CommandDetector.detect("do")
        assertEquals("com.todoist", (word as ActionCommand.OpenApp).packageName)
    }

    @Test
    fun multiWordStickPicture() {
        UserShortcuts.replace(
            listOf(UserShortcut("2", listOf("/sp", "stick picture"), "com.google.android.apps.photos")),
        )
        val cmd = CommandDetector.detect("stick picture vacation")
        assertTrue(cmd is ActionCommand.OpenApp)
        assertEquals("com.google.android.apps.photos", (cmd as ActionCommand.OpenApp).packageName)
        assertEquals("vacation", cmd.payload)
        val slash = CommandDetector.detect("/sp")
        assertEquals("com.google.android.apps.photos", (slash as ActionCommand.OpenApp).packageName)
    }

    @Test
    fun longerCustomBeatsShorterBuiltin() {
        UserShortcuts.replace(
            listOf(UserShortcut("3", listOf("/sp"), "com.pics")),
        )
        val custom = CommandDetector.detect("/sp shot")
        assertTrue(custom is ActionCommand.OpenApp)
        val sticky = CommandDetector.detect("/s hello")
        assertTrue(sticky is ActionCommand.Note)
    }

    @Test
    fun splitDraftCommaAndSlash() {
        assertEquals(listOf("/d", "do", "stick picture"), UserShortcuts.splitDraft("/d, do, stick picture"))
    }

    @Test
    fun sameLengthPrefersBuiltin() {
        UserShortcuts.replace(listOf(UserShortcut("x", listOf("/s"), "com.hijack")))
        val cmd = CommandDetector.detect("/s hello")
        assertTrue(cmd is ActionCommand.Note)
    }

    @Test
    fun comboAtExpandsWithPackageAndContact() {
        UserShortcuts.replace(
            listOf(
                UserShortcut(
                    id = "wa",
                    phrases = listOf("wa", "@whatsapp"),
                    packageName = "com.whatsapp",
                    kind = UserShortcutKind.COMBO,
                    combo = UserShortcutCombo.CHAT,
                    contact = UserShortcutContact(id = "12", name = "Alice", phone = "+15551212"),
                ),
            ),
        )
        val cmd = CommandDetector.detect("wa")
        assertTrue(cmd is ActionCommand.QuickCombo)
        val combo = cmd as ActionCommand.QuickCombo
        assertEquals(UserShortcutCombo.CHAT, combo.combo)
        assertEquals("com.whatsapp", combo.packageName)
        assertEquals("Alice", combo.contactName)
        assertEquals("+15551212", combo.contactPhone)
        val alias = CommandDetector.detect("@whatsapp") as ActionCommand.QuickCombo
        assertEquals("com.whatsapp", alias.packageName)
        assertEquals("Alice", alias.contactName)
    }

    @Test
    fun comboSocialExpandsWithPackage() {
        UserShortcuts.replace(
            listOf(
                UserShortcut(
                    id = "x",
                    phrases = listOf("xtwitter", "!x"),
                    packageName = "com.twitter.android",
                    kind = UserShortcutKind.COMBO,
                    combo = UserShortcutCombo.SOCIAL,
                ),
            ),
        )
        val bang = CommandDetector.detect("!x") as ActionCommand.QuickCombo
        assertEquals(UserShortcutCombo.SOCIAL, bang.combo)
        assertEquals("com.twitter.android", bang.packageName)
        assertEquals(false, bang.hasContact)
        val word = CommandDetector.detect("xtwitter") as ActionCommand.QuickCombo
        assertEquals("com.twitter.android", word.packageName)
    }

    @Test
    fun oldShortcutsDefaultToOpenApp() {
        val item = UserShortcut("1", listOf("/d", "do"), "com.todoist")
        assertEquals(UserShortcutKind.OPEN_APP, item.kind)
        UserShortcuts.replace(listOf(item))
        val cmd = CommandDetector.detect("/d")
        assertTrue(cmd is ActionCommand.OpenApp)
        assertEquals("com.todoist", (cmd as ActionCommand.OpenApp).packageName)
    }
}
