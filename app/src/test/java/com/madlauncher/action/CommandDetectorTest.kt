package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandDetectorTest {
    @Test
    fun chatAtPrefix() {
        val cmd = CommandDetector.detect("@alice hi")
        assertTrue(cmd is ActionCommand.ChatPick)
        assertEquals("alice hi", (cmd as ActionCommand.ChatPick).filter)
    }

    @Test
    fun leadingAtSignsStripToChatPick() {
        val cmd = CommandDetector.detect("@@bob")
        assertTrue(cmd is ActionCommand.ChatPick)
        assertEquals("bob", (cmd as ActionCommand.ChatPick).filter)
        val triple = CommandDetector.detect("@@@carol")
        assertEquals("carol", (triple as ActionCommand.ChatPick).filter)
    }

    @Test
    fun videoCallStarPrefix() {
        val cmd = CommandDetector.detect("*5551234")
        assertTrue(cmd is ActionCommand.VideoCall)
        assertEquals("5551234", (cmd as ActionCommand.VideoCall).query)
    }

    @Test
    fun noteSlashS() {
        val cmd = CommandDetector.detect("/s sticky")
        assertTrue(cmd is ActionCommand.Note)
        assertEquals("sticky", (cmd as ActionCommand.Note).text)
        assertTrue((cmd as ActionCommand.Note).floating)
    }

    @Test
    fun questionMarkIsAiAsk() {
        val cmd = CommandDetector.detect("? what is MAD")
        assertTrue(cmd is ActionCommand.AiAsk)
        assertEquals("what is MAD", (cmd as ActionCommand.AiAsk).prompt)
    }

    @Test
    fun symbolsAreFiltersNotSpaceCommits() {
        assertTrue(CommandDetector.isSymbolFilter("@wa"))
        assertTrue(CommandDetector.isSymbolFilter("#"))
        assertTrue(CommandDetector.isSymbolFilter(":ig"))
        assertTrue(CommandDetector.isSymbolFilter("*"))
        assertTrue(CommandDetector.isSymbolFilter("?gpt"))
        org.junit.Assert.assertFalse(CommandDetector.isSymbolFilter("msg alice"))
        org.junit.Assert.assertFalse(CommandDetector.isSymbolFilter("call john"))
        org.junit.Assert.assertFalse(CommandDetector.isSymbolFilter("&alice"))
        assertTrue(CommandDetector.detect("@wa") is ActionCommand.ChatPick)
        assertTrue(CommandDetector.detect("#wa") is ActionCommand.Call)
    }

    @Test
    fun ampersandIsContactLookup() {
        val cmd = CommandDetector.detect("&alice")
        assertTrue(cmd is ActionCommand.ContactLookup)
        assertEquals("alice", (cmd as ActionCommand.ContactLookup).filter)
    }

    @Test
    fun slashGIsNavigate() {
        val empty = CommandDetector.detect("/g")
        assertTrue(empty is ActionCommand.Navigate)
        assertEquals("", (empty as ActionCommand.Navigate).destination)

        val dest = CommandDetector.detect("/G Tel Aviv")
        assertTrue(dest is ActionCommand.Navigate)
        assertEquals("Tel Aviv", (dest as ActionCommand.Navigate).destination)
    }

    @Test
    fun slashGStripsLeadingTo() {
        val cmd = CommandDetector.detect("/g to paris")
        assertTrue(cmd is ActionCommand.Navigate)
        assertEquals("paris", (cmd as ActionCommand.Navigate).destination)
    }

    @Test
    fun mapsBangIsWebsiteNotGoCommand() {
        assertTrue(CommandDetector.detect("!maps Tel Aviv") is ActionCommand.None)
        assertTrue(CommandDetector.detect("!map home") is ActionCommand.None)
        assertTrue(CommandDetector.detect("!gmaps airport") is ActionCommand.None)
        assertTrue(CommandDetector.detect("!waze work") is ActionCommand.None)
    }

    @Test
    fun translateBangIsWebsiteNotTranslateCommand() {
        assertTrue(CommandDetector.detect("!tr hello") is ActionCommand.None)
        assertTrue(CommandDetector.detect("!translate good morning") is ActionCommand.None)
    }

    @Test
    fun playBangStaysASearchNotACommand() {
        assertTrue(CommandDetector.detect("!p widgets") is ActionCommand.None)
        assertTrue(CommandDetector.detect("!play widgets") is ActionCommand.None)
    }

    @Test
    fun goWordIsNavigate() {
        val cmd = CommandDetector.detect("go home")
        assertTrue(cmd is ActionCommand.Navigate)
        assertEquals("home", (cmd as ActionCommand.Navigate).destination)
        val viaTo = CommandDetector.detect("go to the airport")
        assertEquals("the airport", (viaTo as ActionCommand.Navigate).destination)
    }

    @Test
    fun updateOpensStoreCommand() {
        assertTrue(CommandDetector.detect("update") is ActionCommand.UpdateApps)
        assertTrue(CommandDetector.detect("updates") is ActionCommand.UpdateApps)
        assertTrue(CommandDetector.detect("update apps") is ActionCommand.UpdateApps)
        assertTrue(CommandDetector.detect("/u") is ActionCommand.UpdateApps)
    }

    @Test
    fun slashRIsReminder() {
        val empty = CommandDetector.detect("/r")
        assertTrue(empty is ActionCommand.Reminder)
        assertEquals("", (empty as ActionCommand.Reminder).text)
        val body = CommandDetector.detect("/R milk in 20 min")
        assertTrue(body is ActionCommand.Reminder)
        assertEquals("milk in 20 min", (body as ActionCommand.Reminder).text)
    }

    @Test
    fun slashSettingsCommands() {
        assertEquals(ActionCommand.SystemSettings, CommandDetector.detect("/ss"))
        assertEquals(ActionCommand.LauncherSettings, CommandDetector.detect("/ls"))
        assertEquals(ActionCommand.ActionBoxSettings, CommandDetector.detect("/abs"))
        assertEquals(ActionCommand.SystemSettings, CommandDetector.detect("/SS"))
        assertTrue(CommandDetector.detect("/a Alice") is ActionCommand.AddContact)
    }

    @Test
    fun plusStartsAddUserCommand() {
        val empty = CommandDetector.detect("+")
        assertTrue(empty is ActionCommand.AddUserCommand)
        assertEquals("", (empty as ActionCommand.AddUserCommand).phrase)
        val body = CommandDetector.detect("+ shop")
        assertEquals("shop", (body as ActionCommand.AddUserCommand).phrase)
        assertTrue(CommandDetector.detect("+15551212") !is ActionCommand.AddUserCommand)
    }

    @Test
    fun barrelRollIsHiddenExactPhrase() {
        assertTrue(CommandDetector.detect("do a barrel roll") is ActionCommand.BarrelRoll)
        assertTrue(CommandDetector.detect("Do a barrel roll!") is ActionCommand.BarrelRoll)
        assertTrue(CommandDetector.detect("barrel roll") is ActionCommand.BarrelRoll)
        assertTrue(CommandDetector.detect("update chrome") is ActionCommand.UpdateApps)
    }
}
