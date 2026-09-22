package com.madlauncher.action

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactInsertParserTest {
    @Test
    fun nameAndPhone() {
        assertEquals("Alice" to "0501234567", ContactInsertParser.parse("Alice 0501234567"))
    }

    @Test
    fun phoneOnly() {
        val (name, phone) = ContactInsertParser.parse("050-123-4567")
        assertEquals("", name)
        assertEquals("050-123-4567", phone)
    }

    @Test
    fun nameOnly() {
        assertEquals("Alice Cohen" to "", ContactInsertParser.parse("Alice Cohen"))
    }

    @Test
    fun stripsSlashAFromName() {
        assertEquals("Alice" to "0501234567", ContactInsertParser.parse("/a Alice 0501234567"))
        assertEquals("Alice" to "", ContactInsertParser.parse("/a Alice"))
        assertEquals("" to "", ContactInsertParser.parse("/a"))
    }

    @Test
    fun stripsAddContactPhrase() {
        assertEquals("Bob" to "", ContactInsertParser.parse("add contact Bob"))
    }
}

class AddContactCommandTest {
    @After
    fun reset() {
        UserShortcuts.replace(emptyList())
        CommandAliases.replaceExtras(UserAliasCatalog.Empty)
        CommandAliases.replaceDisabledBuiltIns(emptySet())
    }

    @Test
    fun slashAIsAddContact() {
        val cmd = CommandDetector.detect("/a Alice 050123")
        assertTrue(cmd is ActionCommand.AddContact)
        assertEquals("Alice 050123", (cmd as ActionCommand.AddContact).details)
    }

    @Test
    fun addContactPhrase() {
        val cmd = CommandDetector.detect("add contact Bob")
        assertTrue(cmd is ActionCommand.AddContact)
        assertEquals("Bob", (cmd as ActionCommand.AddContact).details)
    }

    @Test
    fun contactWordIsLookupNotInsert() {
        val cmd = CommandDetector.detect("contact Alice")
        assertTrue(cmd is ActionCommand.ContactLookup)
        assertEquals("Alice", (cmd as ActionCommand.ContactLookup).filter)
    }

    @Test
    fun addContactSpaceCommit() {
        assertTrue(CommandAliases.isAddContactCommit("/a "))
        assertTrue(CommandAliases.isAddContactCommit("add contact "))
    }
}
