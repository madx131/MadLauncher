package com.madlauncher.a11y

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessengerSearchTapEngineTest {
    @Test
    fun exactNameDoesNotMatchAGroupThatContainsIt() {
        assertTrue(MessengerSearchTapEngine.isExactName("Tomer Ben David", "Tomer Ben David"))
        assertFalse(MessengerSearchTapEngine.isExactName("Tomer and Elena", "Tomer Ben David"))
        assertFalse(MessengerSearchTapEngine.isExactName("Barak Ben David and Tomer Ben David", "Tomer Ben David"))
    }

    @Test
    fun openChatIsTheThreadScreen() {
        assertTrue(MessengerSearchTapEngine.isOpenChat("Tomer Ben David, Thread details"))
        assertTrue(MessengerSearchTapEngine.isOpenChat("Type a message"))
        assertFalse(MessengerSearchTapEngine.isOpenChat("Search"))
        assertTrue(MessengerSearchTapEngine.isSearchLabel("Search"))
        assertTrue(MessengerSearchTapEngine.isSearchLabel("Search chats"))
        assertFalse(MessengerSearchTapEngine.isSearchLabel("Search settings"))
        assertTrue(MessengerSearchTapEngine.isPersonRow("Barak Ben-David", "Barak Ben David"))
        assertTrue(MessengerSearchTapEngine.isPersonRow("Barak Ben David.", "Barak Ben David"))
        assertTrue(MessengerSearchTapEngine.tokensMatch("Svety Brener", "Orit Sveta Brener"))
        assertFalse(MessengerSearchTapEngine.tokensMatch("Svety Brener", "Orit Brener"))
        assertTrue(MessengerSearchTapEngine.isPersonRow("Barak Ben David, active now", "Barak Ben David"))
        assertFalse(
            MessengerSearchTapEngine.isPersonRow(
                "Barak Ben David and Tomer Ben David",
                "Barak Ben David",
            ),
        )
        assertTrue(MessengerSearchTapEngine.isPersonRow("Peer Ben David", "Pe'er Ben David"))
        assertTrue(MessengerSearchTapEngine.isPersonRow("Pe'er Ben-David", "Pe'er Ben David"))
        assertTrue(MessengerSearchTapEngine.tokensMatch("Pe'er Ben David", "Peer Ben David"))
        assertTrue(MessengerSearchTapEngine.tokensMatch("Pe'er Ban David", "Peer Ben David"))
        assertFalse(MessengerSearchTapEngine.tokensMatch("Pe'er Ben David", "Barak Ben David"))
    }

    @Test
    fun searchTriesTheNameWithoutTheApostrophe() {
        val queries = com.madlauncher.a11y.MessengerSearchBridge.searchQueries("Pe'er Ben David")
        assertTrue(queries.contains("Pe'er Ben David"))
        assertTrue(queries.contains("Peer Ben David"))
        assertTrue(queries.contains("peer"))
    }
}
