package com.madlauncher.notif

/**
 * SMS-database unread is a fallback for the default SMS app only.
 * Stamping that count onto WhatsApp / Telegram / the whole @ catalog made every
 * messenger show 99+ when the inbox had many unread threads.
 */
object SmsInboxBadges {
    fun counts(
        unreadThreadCount: Int,
        defaultSmsPackage: String?,
        smsHandlerPackages: Set<String> = emptySet(),
    ): Map<String, Int> {
        if (unreadThreadCount <= 0) return emptyMap()
        val pkg = defaultSmsPackage?.trim()?.takeIf { it.isNotEmpty() }
            ?: smsHandlerPackages.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            ?: return emptyMap()
        return mapOf(pkg to unreadThreadCount)
    }
}
