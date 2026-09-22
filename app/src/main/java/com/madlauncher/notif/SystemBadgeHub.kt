package com.madlauncher.notif

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Per-package icon badge counts from the **active** status-bar shade.
 * SMS inbox unread is not merged in — that dump is not live shade unread.
 */
object SystemBadgeHub {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    private var nlsCounts: Map<String, Int> = emptyMap()

    fun publishNls(counts: Map<String, Int>) {
        nlsCounts = counts
        _counts.value = nlsCounts
    }

    fun publishInbox(@Suppress("UNUSED_PARAMETER") counts: Map<String, Int>) {
        // SMS-database unread is not a launcher badge. Shade only.
    }

    /** Legacy alias — NLS shade counts only. */
    fun publish(counts: Map<String, Int>) = publishNls(counts)

    fun clear() {
        nlsCounts = emptyMap()
        _counts.value = emptyMap()
    }

    fun countFor(packageName: String): Int = _counts.value[packageName] ?: 0
}
