package com.madlauncher.notif

import android.service.notification.NotificationListenerService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShadeSyncTest {
    @Test
    fun bounce_samePostTime_isSuppressed() {
        assertTrue(ShadeSync.isBounceRepost(true, 1000L, 1000L))
        assertTrue(ShadeSync.isBounceRepost(true, 1000L, 900L))
        assertFalse(ShadeSync.isBounceRepost(true, 1000L, 1001L))
        assertFalse(ShadeSync.isBounceRepost(false, 1000L, 1000L))
    }

    @Test
    fun userShadeDismiss_notSnooze() {
        assertTrue(ShadeSync.isUserShadeDismiss(NotificationListenerService.REASON_CANCEL))
        assertTrue(ShadeSync.isUserShadeDismiss(NotificationListenerService.REASON_CANCEL_ALL))
        assertTrue(ShadeSync.isUserShadeDismiss(NotificationListenerService.REASON_CLICK))
        assertFalse(ShadeSync.isUserShadeDismiss(NotificationListenerService.REASON_SNOOZED))
        assertFalse(ShadeSync.isUserShadeDismiss(NotificationListenerService.REASON_APP_CANCEL))
        assertFalse(ShadeSync.isUserShadeDismiss(NotificationListenerService.REASON_LISTENER_CANCEL))
    }

    @Test
    fun conversationExpand_requiresRealThread() {
        assertTrue(ShadeSync.canExpandConversation("com.whatsapp|mike", "com.whatsapp", "WhatsApp"))
        assertFalse(ShadeSync.canExpandConversation("com.whatsapp|com.whatsapp", "com.whatsapp", "WhatsApp"))
        assertFalse(ShadeSync.canExpandConversation("com.android.systemui|system ui", "com.android.systemui", "System UI"))
        assertFalse(ShadeSync.canExpandConversation("pkg|", "pkg", "App"))
    }
}
