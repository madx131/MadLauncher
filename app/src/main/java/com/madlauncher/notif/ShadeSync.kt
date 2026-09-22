package com.madlauncher.notif

/**
 * Cross-OEM rules for MAD window ↔ status-bar shade sync.
 *
 * Stock / Samsung / MAD OS honor [NotificationListenerService.cancelNotification].
 * Xiaomi / ColorOS / Vivo / some Huawei builds no-op cancel; those still need a
 * snooze fallback **only if the row is still active after cancel**.
 */
object ShadeSync {
    const val BOUNCE_MS = 1_200L
    const val CANCEL_RETRY_MS = 350L
    const val SNOOZE_MS = 12L * 60L * 60L * 1_000L

    fun isBounceRepost(pending: Boolean, cancelledPostTime: Long, newPostTime: Long): Boolean {
        if (!pending) return false
        return newPostTime <= cancelledPostTime
    }

    /** Shade swipe / clear-all / tap-to-open. Not snooze (user may unsnooze). */
    fun isUserShadeDismiss(reason: Int): Boolean {
        return reason == android.service.notification.NotificationListenerService.REASON_CANCEL ||
            reason == android.service.notification.NotificationListenerService.REASON_CANCEL_ALL ||
            reason == android.service.notification.NotificationListenerService.REASON_CLICK
    }

    /**
     * Expanding dismiss to "same conversation" is only safe when the id is a
     * real thread, not the package or app label (would wipe every alert from that app).
     */
    fun canExpandConversation(conversationId: String, packageName: String, appName: String): Boolean {
        val tail = conversationId.substringAfter('|', "").trim()
        if (tail.isBlank() || tail.equals(packageName, ignoreCase = true)) return false
        if (tail.equals(appName, ignoreCase = true)) return false
        return true
    }
}
