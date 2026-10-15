package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonAppShortcutsTest {
    @Test
    fun exactWordIsOpenCommonApp() {
        val gm = CommandDetector.detect("gm")
        assertTrue(gm is ActionCommand.OpenCommonApp)
        assertEquals("gmail", (gm as ActionCommand.OpenCommonApp).shortcutId)
        assertEquals("", gm.payload)

        val calc = CommandDetector.detect("calc")
        assertTrue(calc is ActionCommand.OpenCommonApp)
        assertEquals("calculator", (calc as ActionCommand.OpenCommonApp).shortcutId)

        val ytm = CommandDetector.detect("ytm")
        assertTrue(ytm is ActionCommand.OpenCommonApp)
        assertEquals("ytmusic", (ytm as ActionCommand.OpenCommonApp).shortcutId)

        val sp = CommandDetector.detect("sp")
        assertTrue(sp is ActionCommand.OpenCommonApp)
        assertEquals("spotify", (sp as ActionCommand.OpenCommonApp).shortcutId)

        val x = CommandDetector.detect("x")
        assertTrue(x is ActionCommand.OpenCommonApp)
        assertEquals("x", (x as ActionCommand.OpenCommonApp).shortcutId)
        assertEquals(SpaceAddon.POST, CommonAppShortcuts.byId("instagram")?.addon)
        assertEquals(SpaceAddon.POST, CommonAppShortcuts.byId("facebook")?.addon)
        assertEquals(SpaceAddon.POST, CommonAppShortcuts.byId("x")?.addon)
        assertEquals(SpaceAddon.POST, CommonAppShortcuts.byId("truth")?.addon)
        assertEquals(SpaceAddon.POST, CommonAppShortcuts.byId("bluesky")?.addon)

        val wa = CommandDetector.detect("wa")
        assertTrue(wa is ActionCommand.OpenCommonApp)
        assertEquals("whatsapp", (wa as ActionCommand.OpenCommonApp).shortcutId)
    }

    @Test
    fun bangStillSearchesNotOpens() {
        assertTrue(CommandDetector.detect("!gm invoice") is ActionCommand.None)
        assertTrue(CommandDetector.detect("!yt cats") is ActionCommand.None)
        assertTrue(CommandDetector.detect("!ytm jazz") is ActionCommand.None)
        assertTrue(CommandDetector.detect("!sp lo-fi") is ActionCommand.None)
    }

    @Test
    fun phoneExactOpensAppCallWithNameStaysCall() {
        val open = CommandDetector.detect("phone")
        assertTrue(open is ActionCommand.OpenCommonApp)
        assertEquals("phone", (open as ActionCommand.OpenCommonApp).shortcutId)

        val call = CommandDetector.detect("phone alice")
        assertTrue(call is ActionCommand.Call)
        assertEquals("alice", (call as ActionCommand.Call).query)
    }

    @Test
    fun payloadWithoutBangIsAddonBody() {
        val gm = CommandDetector.detect("gm invoice")
        assertTrue(gm is ActionCommand.OpenCommonApp)
        assertEquals("invoice", (gm as ActionCommand.OpenCommonApp).payload)

        val maps = CommandDetector.detect("maps paris")
        assertTrue(maps is ActionCommand.OpenCommonApp)
        assertEquals("paris", (maps as ActionCommand.OpenCommonApp).payload)

        val waze = CommandDetector.detect("waze")
        assertTrue(waze is ActionCommand.OpenCommonApp)
        assertEquals("waze", (waze as ActionCommand.OpenCommonApp).shortcutId)
        assertEquals(SpaceAddon.NAV, CommonAppShortcuts.byId("waze")?.addon)
        assertEquals("waze", CommonAppShortcuts.matchSpaceCommit("waze ")?.id)
        assertEquals("bing", CommonAppShortcuts.matchSpaceCommit("bing ")?.id)
        assertEquals(SpaceAddon.SEARCH, CommonAppShortcuts.byId("temu")?.addon)
        assertEquals(SpaceAddon.AI, CommonAppShortcuts.byId("grok")?.addon)

        val wa = CommandDetector.detect("wa alice")
        assertTrue(wa is ActionCommand.OpenCommonApp)
        assertEquals("whatsapp", (wa as ActionCommand.OpenCommonApp).shortcutId)
        assertEquals("alice", (wa as ActionCommand.OpenCommonApp).payload)

        val ig = CommandDetector.detect("ig cats")
        assertTrue(ig is ActionCommand.OpenCommonApp)
        assertEquals("instagram", (ig as ActionCommand.OpenCommonApp).shortcutId)
        assertEquals("cats", ig.payload)
        val truth = CommandDetector.detect("truth cats")
        assertEquals("truth", (truth as ActionCommand.OpenCommonApp).shortcutId)
        assertEquals("cats", truth.payload)
    }

    @Test
    fun translateWithTextStaysTranslateCommand() {
        val cmd = CommandDetector.detect("translate hello")
        assertTrue(cmd is ActionCommand.Translate)
        assertEquals("hello", (cmd as ActionCommand.Translate).tail)
    }

    @Test
    fun longestPhraseWinsCalcOverCal() {
        assertEquals("calculator", CommonAppShortcuts.matchExact("calc")?.id)
        assertEquals("calendar", CommonAppShortcuts.matchExact("cal")?.id)
        assertEquals("youtube", CommonAppShortcuts.matchExact("yt")?.id)
        assertEquals("ytmusic", CommonAppShortcuts.matchExact("ytm")?.id)
        assertEquals("ytmusic", CommonAppShortcuts.matchExact("ytmusic")?.id)
        assertEquals("calculator", CommonAppShortcuts.matchSpaceCommit("calc ")?.id)
        assertEquals("calendar", CommonAppShortcuts.matchSpaceCommit("cal ")?.id)
    }

    @Test
    fun spaceCommitNeedsTrailingSpace() {
        assertNull(CommonAppShortcuts.matchSpaceCommit("gm"))
        assertEquals("gmail", CommonAppShortcuts.matchSpaceCommit("gm ")?.id)
        assertNull(CommonAppShortcuts.matchPayload("gm"))
        assertNull(CommonAppShortcuts.matchPayload("gm "))
    }
}
