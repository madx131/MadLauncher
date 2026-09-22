package com.madlauncher.action

import com.madlauncher.data.ContactEntry

/** Shared contact / thread identity for @ # * — history sync and call routing must match. */
object ContactReach {
    fun threadKey(contact: ContactEntry): String? {
        val key = contact.id.substringAfter('|', "").trim()
        return key.takeIf { it.isNotBlank() && key != contact.id }
    }

    fun phoneDigits(contact: ContactEntry): String? {
        val fromPhone = contact.phone?.filter { it.isDigit() }?.takeIf { it.length >= 7 }
        if (fromPhone != null) return fromPhone
        val thread = threadKey(contact) ?: return null
        return thread.filter { it.isDigit() }.takeIf { it.length >= 7 }
    }

    fun conversationId(packageName: String, thread: String): String =
        "$packageName|${thread.trim()}"

    fun isDialerPackage(packageName: String): Boolean {
        val p = packageName.lowercase()
        return p.contains("dialer") || p.contains("phone") || p.contains("caller") ||
            p.contains("smart.caller") || p.contains("eyecon")
    }
}
