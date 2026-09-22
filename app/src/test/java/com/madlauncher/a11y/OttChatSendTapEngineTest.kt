package com.madlauncher.a11y

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OttChatSendTapEngineTest {
    @Test
    fun pasteOnly_neverStartsAtSend() {
        val wa = OttChatSendBridge.Request(
            packageName = "com.whatsapp",
            text = "hello",
            alreadyFilled = true,
        )
        val tg = OttChatSendBridge.Request(
            packageName = "org.telegram.messenger",
            text = "hello",
            alreadyFilled = false,
        )
        assertEquals(OttChatSendBridge.Phase.WaitForChat, wa.phase)
        assertEquals(OttChatSendBridge.Phase.WaitForChat, tg.phase)
        assertFalse(OttChatSendBridge.Phase.entries.any { it.name == "TapSend" })
    }
}
