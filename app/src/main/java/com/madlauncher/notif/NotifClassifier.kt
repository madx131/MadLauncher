package com.madlauncher.notif

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

object NotifClassifier {
    fun isMessagingNotification(notification: Notification): Boolean {
        if (NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification) != null) {
            return true
        }
        if (notification.category == Notification.CATEGORY_MESSAGE) return true
        val extras = notification.extras
        if (extras.containsKey(Notification.EXTRA_MESSAGES)) return true
        if (extras.getString("android.conversationId")?.isNotBlank() == true) return true
        return false
    }

    fun isMessagingSbn(sbn: StatusBarNotification): Boolean {
        if (isCallSbn(sbn)) return false
        if (isAtPackage(sbn.packageName)) return true
        if (MessagingAppRegistry.isSmsPackage(sbn.packageName)) return true
        return isMessagingNotification(sbn.notification)
    }

    /** Incoming / ongoing voice or video call — not missed-call summaries. */
    fun isCallSbn(sbn: StatusBarNotification): Boolean = isCallLiveNotif(
        category = sbn.notification.category,
        ongoing = sbn.isOngoing,
        title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE),
        body = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT),
        bigText = sbn.notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
    )

    fun isCallLiveNotif(notif: LiveNotif): Boolean = isCallLiveNotif(
        category = notif.category,
        ongoing = notif.ongoing,
        title = notif.title,
        body = notif.body,
    )

    private fun isCallLiveNotif(
        category: String?,
        ongoing: Boolean,
        title: CharSequence?,
        body: CharSequence?,
        bigText: CharSequence? = null,
    ): Boolean {
        if (category == Notification.CATEGORY_CALL) return true
        if (ongoing && callLikeText(title, body, bigText)) return true
        return false
    }

    internal fun callLikeText(vararg parts: CharSequence?): Boolean {
        val hay = parts.mapNotNull { it?.toString()?.trim()?.lowercase() }.joinToString(" ")
        if (hay.isBlank()) return false
        return hay.contains("incoming call") ||
            hay.contains("incoming video") ||
            hay.contains("video call") ||
            hay.contains("voice call") ||
            hay.contains("ongoing call") ||
            hay.contains("on a call") ||
            hay.contains("ringing") ||
            (hay.contains("calling") && !hay.contains("missed call")) ||
            hay.contains("is calling")
    }

    /**
     * Notifications window membership = ActionBox `@` apps (messaging + email) plus universal SMS/messaging signals.
     */
    fun isMessagingWindow(item: LiveNotif, installedMessaging: Set<String>): Boolean {
        if (item.packageName in installedMessaging) return true
        if (MessagingAppRegistry.isSmsPackage(item.packageName)) return true
        if (item.category == Notification.CATEGORY_MESSAGE) return true
        if (item.canInlineReply) return true
        if (item.key.startsWith("sms|inbox|")) return true
        return false
    }

    /** Listener helper: package is in the live `@` apps set (messaging + email). */
    fun isAtPackage(packageName: String): Boolean {
        return packageName in NotificationHub.atPackageNames
    }

    fun isAtPackage(sbn: StatusBarNotification): Boolean = isAtPackage(sbn.packageName)
}
