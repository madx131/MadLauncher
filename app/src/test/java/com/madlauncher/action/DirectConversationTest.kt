package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Test

class DirectConversationTest {
    @Test
    fun messengerWithEmailSearchesByName() {
        assertEquals(
            DirectConversation.Search,
            directConversation("com.facebook.orca", hasPhone = true, hasEmail = true),
        )
    }

    @Test
    fun gmailWithEmailOpensMail() {
        assertEquals(
            DirectConversation.Mail,
            directConversation("com.google.android.gm", hasPhone = true, hasEmail = true),
        )
    }

    @Test
    fun messagesWithPhoneOpensSms() {
        assertEquals(
            DirectConversation.Sms,
            directConversation("com.google.android.apps.messaging", hasPhone = true, hasEmail = true),
        )
    }
}
