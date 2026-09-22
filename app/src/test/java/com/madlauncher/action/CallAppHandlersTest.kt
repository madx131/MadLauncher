package com.madlauncher.action

import com.madlauncher.data.ContactEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallAppHandlersTest {
    @Test
    fun isDialerPackage_recognizesStockDialers() {
        assertTrue(CallAppHandlers.isDialerPackage("com.google.android.dialer"))
        assertTrue(CallAppHandlers.isDialerPackage("com.sh.smart.caller"))
        assertTrue(CallAppHandlers.isDialerPackage("com.eyecon.global"))
        assertFalse(CallAppHandlers.isDialerPackage("com.whatsapp"))
        assertFalse(CallAppHandlers.isDialerPackage("com.facebook.orca"))
    }

    @Test
    fun phoneDigits_fromContactPhone() {
        val c = ContactEntry(id = "x", name = "Bob", phone = "+1 555-0100")
        assertEquals("15550100", CallAppHandlers.phoneDigits(c))
    }

    @Test
    fun phoneDigits_fromConversationId() {
        val c = ContactEntry(
            id = "com.whatsapp|15550199",
            name = "Alice",
            fromAppHistory = true,
        )
        assertEquals("15550199", CallAppHandlers.phoneDigits(c))
    }

    @Test
    fun threadKey_fromConversationId() {
        val c = ContactEntry(id = "com.facebook.orca|jane.doe", name = "Jane", fromAppHistory = true)
        assertEquals("jane.doe", CallAppHandlers.threadKey(c))
    }
}
