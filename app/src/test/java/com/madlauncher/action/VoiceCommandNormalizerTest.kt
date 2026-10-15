package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCommandNormalizerTest {
    @Test
    fun numberWordsBecomeDigits() {
        assertEquals("1", VoiceCommandNormalizer.normalize("one"))
        assertEquals("1", VoiceCommandNormalizer.normalize("One!"))
        assertEquals("12", VoiceCommandNormalizer.normalize("one two"))
        assertEquals("10", VoiceCommandNormalizer.normalize("ten"))
    }

    @Test
    fun spokenMessageBecomesMsgPlusContact() {
        assertEquals("msg+mom", VoiceCommandNormalizer.normalize("message mom"))
        assertEquals("msg+mom", VoiceCommandNormalizer.normalize("msg mom"))
        assertEquals("msg+alice", VoiceCommandNormalizer.normalize("text alice"))
    }

    @Test
    fun spokenCallAndPost() {
        assertEquals("call+dad", VoiceCommandNormalizer.normalize("call dad"))
        assertEquals("post+cats", VoiceCommandNormalizer.normalize("post cats"))
        assertEquals("help", VoiceCommandNormalizer.normalize("help"))
    }

    @Test
    fun plusFormStillDetectsAsChat() {
        val cmd = CommandDetector.detect("msg+mom")
        assertEquals("mom", (cmd as ActionCommand.ChatPick).filter)
    }
}
