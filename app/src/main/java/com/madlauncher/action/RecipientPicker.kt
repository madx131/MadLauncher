package com.madlauncher.action

import com.madlauncher.data.ContactEntry
import com.madlauncher.data.ContactsRepository
import com.madlauncher.data.LocalStore
import com.madlauncher.data.RecipientHistory
import com.madlauncher.notif.LiveNotif

/** App conversation history first, then device address book (deduped). */
object RecipientPicker {
    suspend fun load(
        contactsRepo: ContactsRepository,
        store: LocalStore,
        notifs: List<LiveNotif>,
        packageName: String?,
        query: String,
        emails: Boolean,
    ): List<ContactEntry> {
        RecipientHistory.mergeFromNotifs(store, notifs)
        val recent = AppRecipientSync.sync(
            notifs = notifs,
            packageName = packageName,
            query = query,
            emails = emails,
            stored = RecipientHistory.storedFor(store, packageName),
        )
        val mergedRecent = recent
        val phone = if (contactsRepo.hasPermission()) {
            contactsRepo.search(query, emails)
        } else {
            emptyList()
        }
        val merged = mergeRecentAndPhone(mergedRecent, phone)
        return if (query.isBlank()) merged else ContactMatch.order(query, merged).take(40)
    }

    fun mergeRecentAndPhone(recent: List<ContactEntry>, phone: List<ContactEntry>): List<ContactEntry> {
        if (phone.isEmpty()) return recent
        val out = recent.toMutableList()
        val phoneKeys = recent.mapNotNull { phoneDedupeKey(it) }.toMutableSet()
        for (entry in phone) {
            val key = phoneDedupeKey(entry)
            if (key != null && key in phoneKeys) continue
            if (recent.any { samePerson(it, entry) }) continue
            out.add(entry)
            if (key != null) phoneKeys.add(key)
        }
        return out
    }

    fun partition(merged: List<ContactEntry>): Pair<List<ContactEntry>, List<ContactEntry>> =
        merged.partition { it.fromAppHistory }

    private fun phoneDedupeKey(entry: ContactEntry): String? {
        val digits = entry.phone?.filter { it.isDigit() }?.takeIf { it.length >= 7 } ?: return null
        return digits
    }

    private fun samePerson(a: ContactEntry, b: ContactEntry): Boolean {
        val ap = a.phone?.filter { it.isDigit() }?.takeIf { it.length >= 7 }
        val bp = b.phone?.filter { it.isDigit() }?.takeIf { it.length >= 7 }
        if (ap != null && bp != null && ap == bp) return true
        if (ap == null && bp == null && a.name.equals(b.name, ignoreCase = true)) return true
        return false
    }
}
