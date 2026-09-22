package com.madlauncher.notif

import android.app.Notification
import android.service.notification.StatusBarNotification

/**
 * Computes per-app icon badge counts for any OEM.
 *
 * Active notification shade is the source of truth: if MAD's listener sees it, it counts.
 * HyperOS often reports canShowBadge=false or IMPORTANCE_NONE while still showing alerts
 * (WhatsApp worked via group-summary number; SMS/Messenger failed on channel/importance filters).
 */
object SystemBadgeCounter {

    /** Packages that should never get launcher badges from shade noise. */
    private val NO_BADGE_PACKAGES = setOf(
        "android",
        "com.android.systemui",
    )

    fun compute(
        active: List<StatusBarNotification>,
        ownPackage: String,
        pendingCancelKeys: Set<String>,
    ): Map<String, Int> {
        val totals = LinkedHashMap<String, Int>()

        active
            .filter { isEligible(it, ownPackage, pendingCancelKeys) }
            .filter { !isNoisePackage(it.packageName) }
            .filter { !isSilentNoise(it) }
            .groupBy { it.packageName }
            .forEach { (_, notifications) ->
                val count = countForPackage(notifications)
                if (count > 0) totals[notifications.first().packageName] = count
            }

        return totals
    }

    private fun countForPackage(notifications: List<StatusBarNotification>): Int {
        val summaries = notifications.filter { isGroupSummary(it) }
        val details = notifications.filter { !isGroupSummary(it) }

        // Aggregate number on group summary (WhatsApp, some messengers).
        val maxSummaryNumber = summaries.maxOfOrNull { it.notification.number } ?: 0
        if (maxSummaryNumber > 0) return maxSummaryNumber

        // Individual / per-conversation notifications (Google Messages, Messenger, etc.).
        val detailCount = details.sumOf { badgeIncrement(it) }
        if (detailCount > 0) return detailCount

        // Collapsed group: only summary row visible, no number field.
        if (summaries.isNotEmpty()) return summaries.size.coerceAtLeast(1)

        return 0
    }

    private fun isEligible(
        sbn: StatusBarNotification,
        ownPackage: String,
        pendingCancelKeys: Set<String>,
    ): Boolean {
        if (sbn.packageName == ownPackage) return false
        if (sbn.key in pendingCancelKeys) return false
        return true
    }

    private fun isGroupSummary(sbn: StatusBarNotification): Boolean {
        return sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
    }

    private fun isNoisePackage(packageName: String): Boolean {
        return packageName in NO_BADGE_PACKAGES
    }

    /** Skip ongoing / FGS stubs, not user-facing unread. */
    private fun isSilentNoise(sbn: StatusBarNotification): Boolean {
        val flags = sbn.notification.flags
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return true
        if (flags and Notification.FLAG_ONGOING_EVENT == 0) return false
        if (flags and Notification.FLAG_NO_CLEAR != 0) return true
        return false
    }

    private fun badgeIncrement(sbn: StatusBarNotification): Int {
        val number = sbn.notification.number
        return if (number > 0) number else 1
    }
}
