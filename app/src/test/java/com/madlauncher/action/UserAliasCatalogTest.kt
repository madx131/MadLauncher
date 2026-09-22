package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAliasCatalogTest {
    @Test
    fun extraPhrase_anyLanguage_mapsToCommand() {
        CommandAliases.replaceExtras(UserAliasCatalog.Empty)
        val catalog = UserAliasCatalog.Empty.withAdded(AliasTarget.Chat, "שלח")
        CommandAliases.replaceExtras(catalog)
        try {
            val cmd = CommandDetector.detect("שלח alice")
            assertTrue(cmd is ActionCommand.ChatPick)
            assertEquals("alice", (cmd as ActionCommand.ChatPick).filter)
        } finally {
            CommandAliases.replaceExtras(UserAliasCatalog.Empty)
        }
    }

    @Test
    fun addAndRemovePhrases() {
        val catalog = UserAliasCatalog.Empty
            .withAdded(AliasTarget.Call, "llamar")
            .withAdded(AliasTarget.Call, "התקשר")
        assertEquals(listOf("llamar", "התקשר"), catalog.phrases(AliasTarget.Call))
        val removed = catalog.withRemoved(AliasTarget.Call, "llamar")
        assertEquals(listOf("התקשר"), removed.phrases(AliasTarget.Call))
    }
}
