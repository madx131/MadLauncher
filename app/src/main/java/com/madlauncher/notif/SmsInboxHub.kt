package com.madlauncher.notif

/**
 * SMS inbox fallback when HyperOS does not mirror SMS alerts to the notification listener.
 */
object SmsInboxHub {
    private var inboxCounts: Map<String, Int> = emptyMap()

    fun publishInbox(counts: Map<String, Int>) {
        inboxCounts = counts
        SystemBadgeHub.publishInbox(counts)
    }

    fun clear() {
        inboxCounts = emptyMap()
        SystemBadgeHub.publishInbox(emptyMap())
        NotificationHub.setInboxOverlays(emptyList())
    }

    fun inboxCountFor(packageName: String): Int = inboxCounts[packageName] ?: 0
}
