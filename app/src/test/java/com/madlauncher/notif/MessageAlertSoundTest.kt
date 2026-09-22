package com.madlauncher.notif

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageAlertSoundTest {
    @Test
    fun playbackWithProgressIsSkipped() {
        assertTrue(
            MessageAlertSound.looksLikePlayback(
                category = Notification.CATEGORY_MESSAGE,
                template = null,
                hasMediaSession = false,
                hasProgressKey = true,
                progressMax = 100,
                progressIndeterminate = false,
                ongoingWithProgress = true,
            ),
        )
    }

    @Test
    fun mediaSessionIsSkipped() {
        assertTrue(
            MessageAlertSound.looksLikePlayback(
                category = null,
                template = "android.app.Notification\$MediaStyle",
                hasMediaSession = true,
                hasProgressKey = false,
                progressMax = 0,
                progressIndeterminate = false,
                ongoingWithProgress = false,
            ),
        )
    }

    @Test
    fun transportCategoryIsSkipped() {
        assertTrue(
            MessageAlertSound.looksLikePlayback(
                category = Notification.CATEGORY_TRANSPORT,
                template = null,
                hasMediaSession = false,
                hasProgressKey = false,
                progressMax = 0,
                progressIndeterminate = false,
                ongoingWithProgress = false,
            ),
        )
    }

    @Test
    fun normalMessageIsNotPlayback() {
        assertFalse(
            MessageAlertSound.looksLikePlayback(
                category = Notification.CATEGORY_MESSAGE,
                template = "android.app.Notification\$MessagingStyle",
                hasMediaSession = false,
                hasProgressKey = false,
                progressMax = 0,
                progressIndeterminate = false,
                ongoingWithProgress = false,
            ),
        )
    }

    @Test
    fun unusedZeroProgressIsNotPlayback() {
        assertFalse(
            MessageAlertSound.looksLikePlayback(
                category = Notification.CATEGORY_MESSAGE,
                template = null,
                hasMediaSession = false,
                hasProgressKey = true,
                progressMax = 0,
                progressIndeterminate = false,
                ongoingWithProgress = false,
            ),
        )
    }

    @Test
    fun fingerprintIgnoresIdenticalReplay() {
        val a = MessageAlertSound.contentFingerprint(1L, "hello", "Alice", "hello")
        val b = MessageAlertSound.contentFingerprint(1L, "hello", "Alice", "hello")
        val c = MessageAlertSound.contentFingerprint(2L, "next", "Alice", "next")
        assertEquals(a, b)
        assertTrue(a != c)
    }
}
