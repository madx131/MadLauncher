package com.madlauncher.action

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandAliasesBuiltInPolicyTest {

    @After
    fun reset() {
        CommandAliases.replaceExtras(UserAliasCatalog.Empty)
        CommandAliases.replaceDisabledBuiltIns(emptySet())
    }

    @Test
    fun removingAStockWordStopsMatchingIt() {
        CommandAliases.replaceDisabledBuiltIns(setOf("msg"))
        assertNull(CommandAliases.match("msg alice"))
        val call = CommandAliases.match("call bob")
        assertTrue(call is ActionCommand.Call)
        assertEquals("bob", (call as ActionCommand.Call).query)
    }

    @Test
    fun extraWordsWorkAfterStockWordRemoved() {
        CommandAliases.replaceDisabledBuiltIns(setOf("msg"))
        CommandAliases.replaceExtras(
            UserAliasCatalog.Empty.withAdded(AliasTarget.Chat, "שלח"),
        )
        val cmd = CommandAliases.match("שלח alice")
        assertTrue(cmd is ActionCommand.ChatPick)
        assertEquals("alice", (cmd as ActionCommand.ChatPick).filter)
    }

    @Test
    fun extraStickyWordMapsToFloatingNote() {
        CommandAliases.replaceExtras(
            UserAliasCatalog.Empty.withAdded(AliasTarget.Sticky, "sticky"),
        )
        val cmd = CommandAliases.match("sticky buy milk")
        assertTrue(cmd is ActionCommand.Note)
        assertEquals("buy milk", (cmd as ActionCommand.Note).text)
        assertTrue(cmd.floating)
    }

    @Test
    fun removingSlashSStopsMatchingIt() {
        CommandAliases.replaceDisabledBuiltIns(setOf("/s"))
        assertNull(CommandAliases.match("/s hello"))
        assertEquals(ActionCommand.None, CommandDetector.detect("/s hello"))
    }

    @Test
    fun restoringAStockWordMatchesAgain() {
        CommandAliases.replaceDisabledBuiltIns(setOf("msg"))
        assertNull(CommandAliases.match("msg alice"))
        CommandAliases.replaceDisabledBuiltIns(emptySet())
        val cmd = CommandAliases.match("msg alice")
        assertTrue(cmd is ActionCommand.ChatPick)
    }
}
