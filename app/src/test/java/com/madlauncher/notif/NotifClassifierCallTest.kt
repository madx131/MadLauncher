package com.madlauncher.notif

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifClassifierCallTest {
    @Test
    fun callLikeText_detectsIncomingCall() {
        assertTrue(NotifClassifier.callLikeText("WhatsApp", "Incoming voice call"))
        assertTrue(NotifClassifier.callLikeText("John", "Video call"))
        assertTrue(NotifClassifier.callLikeText("Jane", "is calling you"))
    }

    @Test
    fun callLikeText_rejectsNormalMessage() {
        assertFalse(NotifClassifier.callLikeText("Alice", "Hey are you free?"))
        assertFalse(NotifClassifier.callLikeText("Bob", "Missed call from Carol"))
    }
}
