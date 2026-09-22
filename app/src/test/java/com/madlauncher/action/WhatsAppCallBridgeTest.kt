package com.madlauncher.action

import com.madlauncher.data.ContactEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppCallBridgeTest {
    @Test
    fun placePath_usesPhoneFromContact_notTelUri() {
        val contact = ContactEntry(id = "42", name = "Bob", phone = "+1 555-0100")
        assertEquals("15550100", ContactReach.phoneDigits(contact))
    }
}
