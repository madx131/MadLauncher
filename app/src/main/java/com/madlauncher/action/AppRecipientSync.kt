package com.madlauncher.action

import com.madlauncher.data.ContactEntry
import com.madlauncher.data.StoredRecipient
import com.madlauncher.notif.LiveNotif
import com.madlauncher.notif.NotifClassifier

/**
 * Builds Action Box recipient lists from MAD's live notification / conversation history —
 * not the device address book.
 */
object AppRecipientSync {
    fun sync(
        notifs: List<LiveNotif>,
        packageName: String?,
        query: String,
        emails: Boolean = false,
        requirePhone: Boolean = false,
        stored: List<StoredRecipient> = emptyList(),
        limit: Int = 40,
    ): List<ContactEntry> {
        val scoped = when {
            packageName != null -> notifs.filter { it.packageName == packageName }
            else -> notifs.filter { isRecipientCandidate(it) }
        }
        val q = query.trim().lowercase()
        val byConversation = linkedMapOf<String, Pair<Long, ContactEntry>>()

        for (s in stored) {
            val entry = s.toContactEntry()
            if (requirePhone && entry.phone.isNullOrBlank()) continue
            if (q.isNotBlank() && !matchesQuery(entry, q)) continue
            val prev = byConversation[entry.id]
            if (prev == null || s.lastActiveAt >= prev.first) {
                byConversation[entry.id] = s.lastActiveAt to entry
            }
        }

        for (notif in scoped.sortedByDescending { it.postedAt }) {
            val entry = contactFromNotif(notif, emails) ?: continue
            if (requirePhone && entry.phone.isNullOrBlank()) continue
            if (q.isNotBlank() && !matchesQuery(entry, q)) continue
            val prev = byConversation[entry.id]
            if (prev == null || notif.postedAt >= prev.first) {
                byConversation[entry.id] = notif.postedAt to entry
            }
        }
        return byConversation.values
            .sortedByDescending { it.first }
            .map { it.second }
            .take(limit)
    }

    fun contactFromNotif(notif: LiveNotif, emails: Boolean = false): ContactEntry? {
        if (NotifClassifier.isCallLiveNotif(notif)) return null
        return toContact(notif, emails)
    }

    private fun isRecipientCandidate(notif: LiveNotif): Boolean {
        if (NotifClassifier.isCallLiveNotif(notif)) return false
        if (CallAppRegistry.isHistoryPackage(notif.packageName)) return true
        if (NotifClassifier.isAtPackage(notif.packageName)) return true
        if (notif.category == android.app.Notification.CATEGORY_MESSAGE) return true
        if (notif.canInlineReply) return true
        if (notif.conversationId.startsWith("sms|")) return true
        return false
    }

    fun isHistoryCandidate(notif: LiveNotif): Boolean = isRecipientCandidate(notif)

    private fun toContact(notif: LiveNotif, emails: Boolean): ContactEntry? {
        val convName = notif.conversationId.substringAfter('|', "").trim()
        val name = notif.contactName.trim()
            .ifBlank { notif.title.trim() }
            .ifBlank { convName }
        if (name.isBlank()) return null
        val (phone, email) = reachFor(notif)
        if (name.equals(notif.appName, ignoreCase = true) && convName.isBlank()) return null
        if (convName.isNotBlank() && name.equals(notif.appName, ignoreCase = true)) {
            return ContactEntry(
                id = notif.conversationId,
                name = convName,
                phone = phone,
                email = email,
                fromAppHistory = true,
            )
        }
        if (emails && !email.isNullOrBlank()) {
            return ContactEntry(
                id = notif.conversationId,
                name = name,
                email = email,
                phone = phone,
                fromAppHistory = true,
            )
        }
        return ContactEntry(
            id = notif.conversationId,
            name = name,
            phone = phone,
            email = email,
            fromAppHistory = true,
        )
    }

    private fun reachFor(notif: LiveNotif): Pair<String?, String?> {
        if (notif.conversationId.startsWith("sms|")) {
            val addr = notif.conversationId.removePrefix("sms|").trim()
            return if (CommandDetector.isPhoneLike(addr)) normalizePhone(addr) to null else null to null
        }
        val conv = notif.conversationId.substringAfter('|', "").trim()
        if (conv.contains('@')) return null to conv
        if (CommandDetector.isPhoneLike(notif.title)) return normalizePhone(notif.title) to null
        if (CommandDetector.isPhoneLike(conv)) return normalizePhone(conv) to null
        if (CommandDetector.isPhoneLike(notif.contactName)) return normalizePhone(notif.contactName) to null
        return null to null
    }

    private fun normalizePhone(raw: String): String = raw.trim()

    private fun matchesQuery(entry: ContactEntry, q: String): Boolean {
        if (entry.name.lowercase().contains(q)) return true
        if (entry.phone?.filter { it.isDigit() }?.contains(q.filter { it.isDigit() }) == true) return true
        if (entry.email?.lowercase()?.contains(q) == true) return true
        return false
    }
}
