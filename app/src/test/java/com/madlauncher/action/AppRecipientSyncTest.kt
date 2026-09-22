package com.madlauncher.action

import com.madlauncher.notif.LiveNotif
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRecipientSyncTest {
    @Test
    fun sync_prefersAppHistory_overDuplicates() {
        val notifs = listOf(
            live(
                pkg = "com.facebook.orca",
                title = "Alice",
                conversationId = "com.facebook.orca|thread-1",
                postedAt = 100L,
            ),
            live(
                pkg = "com.facebook.orca",
                title = "Alice",
                conversationId = "com.facebook.orca|thread-1",
                postedAt = 200L,
            ),
            live(
                pkg = "com.facebook.orca",
                title = "Bob",
                conversationId = "com.facebook.orca|thread-2",
                postedAt = 150L,
            ),
        )
        val out = AppRecipientSync.sync(
            notifs = notifs,
            packageName = "com.facebook.orca",
            query = "",
        )
        assertEquals(2, out.size)
        assertEquals("Alice", out[0].name)
        assertEquals("Bob", out[1].name)
        assertTrue(out.all { it.fromAppHistory })
    }

    @Test
    fun sync_smsThread_extractsPhone() {
        val notifs = listOf(
            live(
                pkg = "com.google.android.apps.messaging",
                title = "+1 555 0100",
                conversationId = "sms|+15550100",
                postedAt = 50L,
                category = android.app.Notification.CATEGORY_MESSAGE,
            ),
        )
        val out = AppRecipientSync.sync(
            notifs = notifs,
            packageName = null,
            query = "",
            requirePhone = true,
        )
        assertEquals(1, out.size)
        assertEquals("+15550100", out[0].phone)
    }

    @Test
    fun sync_filtersByQuery() {
        val notifs = listOf(
            live(
                pkg = "com.whatsapp",
                title = "Charlie",
                conversationId = "com.whatsapp|1",
                postedAt = 10L,
            ),
            live(
                pkg = "com.whatsapp",
                title = "Dana",
                conversationId = "com.whatsapp|2",
                postedAt = 20L,
            ),
        )
        val out = AppRecipientSync.sync(
            notifs = notifs,
            packageName = "com.whatsapp",
            query = "dan",
        )
        assertEquals(1, out.size)
        assertEquals("Dana", out[0].name)
    }

    @Test
    fun sync_mergesStoredHistory_whenShadeEmpty() {
        val stored = listOf(
            com.madlauncher.data.StoredRecipient(
                packageName = "com.whatsapp",
                conversationId = "com.whatsapp|alice",
                name = "Alice",
                lastActiveAt = 500L,
            ),
        )
        val out = AppRecipientSync.sync(
            notifs = emptyList(),
            packageName = "com.whatsapp",
            query = "",
            stored = stored,
        )
        assertEquals(1, out.size)
        assertEquals("Alice", out[0].name)
    }

    @Test
    fun sync_skipsCallNotifications() {
        val notifs = listOf(
            live(
                pkg = "com.whatsapp",
                title = "Bob",
                conversationId = "com.whatsapp|bob",
                postedAt = 100L,
            ),
            live(
                pkg = "com.whatsapp",
                title = "Bob",
                conversationId = "com.whatsapp|bob-call",
                postedAt = 200L,
                category = android.app.Notification.CATEGORY_CALL,
                ongoing = true,
            ),
        )
        val out = AppRecipientSync.sync(
            notifs = notifs,
            packageName = "com.whatsapp",
            query = "",
        )
        assertEquals(1, out.size)
        assertEquals("Bob", out[0].name)
    }

    private fun live(
        pkg: String,
        title: String,
        conversationId: String,
        postedAt: Long,
        category: String? = android.app.Notification.CATEGORY_MESSAGE,
        ongoing: Boolean = false,
    ): LiveNotif = LiveNotif(
        key = "$conversationId|$postedAt",
        packageName = pkg,
        appName = "App",
        title = title,
        body = "hi",
        time = "",
        postedAt = postedAt,
        conversationId = conversationId,
        category = category,
        canInlineReply = true,
        ongoing = ongoing,
    )
}
