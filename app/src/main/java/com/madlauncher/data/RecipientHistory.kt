package com.madlauncher.data

import com.madlauncher.action.AppRecipientSync
import com.madlauncher.action.CallAppRegistry
import com.madlauncher.data.ContactEntry
import com.madlauncher.notif.LiveNotif
import com.madlauncher.notif.NotifClassifier

data class StoredRecipient(
    val packageName: String,
    val conversationId: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val lastActiveAt: Long,
) {
    fun toContactEntry(): ContactEntry = ContactEntry(
        id = conversationId,
        name = name,
        phone = phone,
        email = email,
        fromAppHistory = true,
    )
}

/** Remembers recent messaging recipients so @ / # / * work after shade clears. */
object RecipientHistory {
    private const val MAX = 400

    fun mergeFromNotifs(store: LocalStore, notifs: List<LiveNotif>) {
        if (notifs.isEmpty()) return
        val byId = store.recipientHistory
            .filter { CallAppRegistry.isHistoryPackage(it.packageName) }
            .associateBy { it.conversationId }
            .toMutableMap()
        for (notif in notifs) {
            if (!AppRecipientSync.isHistoryCandidate(notif)) continue
            if (NotifClassifier.isCallLiveNotif(notif)) continue
            val contact = AppRecipientSync.contactFromNotif(notif) ?: continue
            byId[contact.id] = StoredRecipient(
                packageName = notif.packageName,
                conversationId = contact.id,
                name = contact.name,
                phone = contact.phone,
                email = contact.email,
                lastActiveAt = notif.postedAt,
            )
        }
        store.recipientHistory = byId.values
            .sortedByDescending { it.lastActiveAt }
            .take(MAX)
    }

    fun storedFor(store: LocalStore, packageName: String?): List<StoredRecipient> {
        val list = store.recipientHistory
        val scoped = if (packageName != null) list.filter { it.packageName == packageName } else list
        return scoped.sortedByDescending { it.lastActiveAt }
    }

    fun contactsFor(store: LocalStore, packageName: String?): List<ContactEntry> =
        storedFor(store, packageName).map { it.toContactEntry() }
}
