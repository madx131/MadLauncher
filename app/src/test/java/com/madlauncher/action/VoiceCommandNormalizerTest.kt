package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandNormalizerTest {
    @Test
    fun numberWordsBecomeDigits() {
        assertEquals("1", VoiceCommandNormalizer.normalize("one"))
        assertEquals("1", VoiceCommandNormalizer.normalize("One!"))
    }

    @Test
    fun spokenMessageBecomesMsgPlusContact() {
        assertEquals("msg+mom", VoiceCommandNormalizer.normalize("message mom"))
        assertEquals("mom", (CommandDetector.detect("msg+mom") as ActionCommand.ChatPick).filter)
    }
}
