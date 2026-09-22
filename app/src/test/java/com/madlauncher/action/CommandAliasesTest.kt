package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandAliasesTest {
    @Test
    fun chatAliases_mapToChatPick() {
        listOf("msg alice", "text bob", "email carol", "send hi").forEach { input ->
            val cmd = CommandDetector.detect(input)
            assertTrue("$input should be ChatPick", cmd is ActionCommand.ChatPick)
        }
        assertEquals("alice", (CommandDetector.detect("msg alice") as ActionCommand.ChatPick).filter)
    }

    @Test
    fun callAliases_mapToCall() {
        val cmd = CommandDetector.detect("call john")
        assertTrue(cmd is ActionCommand.Call)
        assertEquals("john", (cmd as ActionCommand.Call).query)
    }

    @Test
    fun videoAliases_mapToVideoCall() {
        val cmd = CommandDetector.detect("video mom")
        assertTrue(cmd is ActionCommand.VideoCall)
        assertEquals("mom", (cmd as ActionCommand.VideoCall).query)
    }

    @Test
    fun translateAliases_mapToTranslate() {
        val cmd = CommandDetector.detect("translate from english to spanish hello")
        assertTrue(cmd is ActionCommand.Translate)
        assertEquals("from english to spanish hello", (cmd as ActionCommand.Translate).tail)
    }

    @Test
    fun autocomplete_firstWordAlias() {
        assertEquals("message ", CommandAliases.autocomplete("mes"))
        assertEquals("translate ", CommandAliases.autocomplete("tra"))
    }

    @Test
    fun translateCommit_slashTTransTranslate() {
        assertTrue(CommandAliases.isTranslateCommit("/t "))
        assertTrue(CommandAliases.isTranslateCommit("trans "))
        assertTrue(CommandAliases.isTranslateCommit("translate "))
        assertTrue(CommandAliases.isTranslateCommit("/t  "))
    }

    @Test
    fun notesAndAiCommit() {
        assertTrue(CommandAliases.isNotesCommit("/n "))
        assertTrue(CommandAliases.isAiCommit("? "))
        assertTrue(CommandAliases.isAiCommit("ai "))
        assertTrue(CommandAliases.isContactLookupCommit("& "))
        org.junit.Assert.assertFalse(CommandAliases.isNotesCommit("/n hello"))
        org.junit.Assert.assertFalse(CommandAliases.isAiCommit("? hello"))
        org.junit.Assert.assertFalse(CommandAliases.isContactLookupCommit("& alice"))
    }

    @Test
    fun navCommit_slashGAndGo() {
        assertTrue(CommandAliases.isNavCommit("/g "))
        assertTrue(CommandAliases.isNavCommit("/G "))
        assertTrue(CommandAliases.isNavCommit("go "))
        org.junit.Assert.assertFalse(CommandAliases.isNavCommit("/g paris"))
        org.junit.Assert.assertFalse(CommandAliases.isNavCommit("go home"))
    }

    @Test
    fun slashSAndHelpAreLocalCommands() {
        val note = CommandDetector.detect("/s buy milk")
        assertTrue(note is ActionCommand.Note)
        assertEquals("buy milk", (note as ActionCommand.Note).text)
        assertTrue(note.floating)
        assertEquals(ActionCommand.Help, CommandDetector.detect("/h"))
        org.junit.Assert.assertFalse(CommandDetector.detect("+ grocery") is ActionCommand.Reminder)
        val slash = CommandDetector.detect("/r milk in 20 min")
        assertTrue(slash is ActionCommand.Reminder)
        assertEquals("milk in 20 min", (slash as ActionCommand.Reminder).text)
        val word = CommandDetector.detect("remind pickup at 8")
        assertTrue(word is ActionCommand.Reminder)
        assertEquals("pickup at 8", (word as ActionCommand.Reminder).text)
        val reminderWord = CommandDetector.detect("reminder take pills")
        assertTrue(reminderWord is ActionCommand.Reminder)
        assertEquals("take pills", (reminderWord as ActionCommand.Reminder).text)
        assertTrue(CommandDetector.detect("/c lunch") is ActionCommand.Schedule)
    }

    @Test
    fun disabledHelpAliasStopsSlashH() {
        val previous = CommandAliases.disabledBuiltIns
        try {
            CommandAliases.replaceDisabledBuiltIns(setOf("/h"))
            org.junit.Assert.assertFalse(CommandAliases.isActive(AliasTarget.Help, "/h"))
            org.junit.Assert.assertFalse(CommandDetector.detect("/h") is ActionCommand.Help)
        } finally {
            CommandAliases.replaceDisabledBuiltIns(previous)
        }
        assertEquals(ActionCommand.Help, CommandDetector.detect("/h"))
    }

    @Test
    fun stickyWordAliases() {
        listOf("stk buy milk", "stick buy milk", "sticky buy milk", "sitcky buy milk").forEach { input ->
            val cmd = CommandDetector.detect(input)
            assertTrue("$input should be a floating sticky", cmd is ActionCommand.Note)
            assertEquals("buy milk", (cmd as ActionCommand.Note).text)
            assertTrue(cmd.floating)
        }
    }
}
