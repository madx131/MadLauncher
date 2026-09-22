package com.madlauncher.notif

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.graphics.Color
import com.madlauncher.ui.theme.NordBlue
import com.madlauncher.ui.theme.NordGreen
import com.madlauncher.ui.theme.NordOrange
import com.madlauncher.ui.theme.NordPrimary
import com.madlauncher.ui.theme.NordPurple
import com.madlauncher.ui.theme.NordRed
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveNotif(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val time: String,
    val postedAt: Long,
    val conversationId: String,
    val messages: List<String> = emptyList(),
    val canInlineReply: Boolean = false,
    val ongoing: Boolean = false,
    val category: String? = null,
    val hasMediaSession: Boolean = false,
) {
    val accent: Color
        get() {
            val palette = listOf(NordOrange, NordRed, NordBlue, NordPrimary, NordPurple, NordGreen)
            return palette[packageName.hashCode().absoluteValue % palette.size]
        }

    /** Contact / conversation name for floating chips (not the app name). */
    val contactName: String
        get() {
            val fromTitle = title.trim()
            if (fromTitle.isNotBlank() && !fromTitle.equals(appName, ignoreCase = true)) return fromTitle
            val fromConv = conversationId.substringAfter('|', "").trim()
            return fromConv.ifBlank { appName }
        }

    val contactInitial: String
        get() {
            val ch = contactName.firstOrNull { it.isLetterOrDigit() }
            return ch?.uppercaseChar()?.toString() ?: "?"
        }
}

data class StoredReply(
    val pendingIntent: PendingIntent,
    val remoteInput: RemoteInput,
)

object NotificationHub {
    private val _items = MutableStateFlow<List<LiveNotif>>(emptyList())
    val items: StateFlow<List<LiveNotif>> = _items.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /** Keys cleared by the posting app / system (opened or marked read outside MAD). */
    private val _clearedKeys = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val clearedKeys: SharedFlow<String> = _clearedKeys.asSharedFlow()

    private val _clearedConversations = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val clearedConversations: SharedFlow<String> = _clearedConversations.asSharedFlow()

    private val repliesByKey = ConcurrentHashMap<String, StoredReply>()
    private val repliesByConversation = ConcurrentHashMap<String, StoredReply>()
    /** Keys the user dismissed in MAD — kept out of the feed until a fresh post/update. */
    private val dismissedKeys = ConcurrentHashMap.newKeySet<String>()
    private val dismissedConversations = ConcurrentHashMap.newKeySet<String>()

    /** Live ActionBox `@` package names (messaging + email). */
    @Volatile
    var atPackageNames: Set<String> = emptySet()
        private set

    fun setAtPackages(packages: Set<String>) {
        atPackageNames = packages.filter { it.isNotBlank() }.toSet()
    }

    @Volatile
    var cancel: ((String) -> Unit)? = null

    @Volatile
    var open: ((String) -> Unit)? = null

    /** Prefer semantic Mark as Read; otherwise cancel so the posting app clears unread. */
    @Volatile
    var markRead: ((String) -> Boolean)? = null

    /** Clear all shade + MAD state for a package (e.g. user opened WhatsApp). */
    @Volatile
    var clearPackage: ((String) -> Unit)? = null

    fun requestClearPackage(packageName: String) {
        if (packageName.isBlank()) return
        clearPackage?.invoke(packageName)
    }

    fun setConnected(value: Boolean) {
        _connected.value = value
        if (!value) {
            _items.value = emptyList()
            lastPublishedLive = emptyList()
            inboxOverlays.clear()
            repliesByKey.clear()
            repliesByConversation.clear()
            cancel = null
            open = null
            markRead = null
            clearPackage = null
        } else {
            // Active shade is the source of truth — drop persisted dismiss ghosts.
            dismissedKeys.clear()
            dismissedConversations.clear()
        }
    }

    fun seedDismissed(keys: Collection<String>) {
        dismissedKeys.addAll(keys)
    }

    fun seedClearedConversations(ids: Collection<String>) {
        dismissedConversations.addAll(ids.filter { it.isNotBlank() })
    }

    fun snapshotDismissed(): Set<String> = dismissedKeys.toSet()

    fun snapshotClearedConversations(): Set<String> = dismissedConversations.toSet()

    fun markDismissed(key: String) {
        dismissedKeys.add(key)
        mockOverlays.remove(key)
        inboxOverlays.remove(key)
        rebuildItems()
        cancel?.invoke(key)
    }

    /** Hide in MAD only — no status-bar cancel (OEM sync unavailable). */
    fun markDismissedLocal(key: String) {
        dismissedKeys.add(key)
        mockOverlays.remove(key)
        inboxOverlays.remove(key)
        rebuildItems()
    }

    fun markConversationDismissed(conversationId: String) {
        if (conversationId.isBlank()) return
        dismissedConversations.add(conversationId)
        mockOverlays.entries.removeAll { it.value.conversationId == conversationId }
        inboxOverlays.entries.removeAll { it.value.conversationId == conversationId }
        val keys = _items.value.filter { it.conversationId == conversationId }.map { it.key }
        keys.forEach { dismissedKeys.add(it) }
        rebuildItems()
        keys.forEach { key -> cancel?.invoke(key) }
    }

    fun clearDismissed(key: String) {
        dismissedKeys.remove(key)
    }

    fun clearDismissedConversation(conversationId: String) {
        dismissedConversations.remove(conversationId)
    }

    fun isDismissed(key: String): Boolean = key in dismissedKeys

    fun isConversationDismissed(conversationId: String): Boolean = conversationId in dismissedConversations

    /** Tell the posting app this notification was read (action or cancel). */
    fun requestMarkRead(key: String): Boolean {
        return markRead?.invoke(key) == true
    }

    /** Listener saw the status-bar entry go away for a non-snooze reason. */
    fun notifyClearedBySystem(key: String, conversationId: String? = null) {
        dismissedKeys.add(key)
        mockOverlays.remove(key)
        inboxOverlays.remove(key)
        if (!conversationId.isNullOrBlank()) {
            dismissedConversations.add(conversationId)
        }
        rebuildItems()
        _clearedKeys.tryEmit(key)
        if (!conversationId.isNullOrBlank()) {
            _clearedConversations.tryEmit(conversationId)
        }
    }

    private var lastPublishedLive: List<LiveNotif> = emptyList()

    fun publish(list: List<LiveNotif>) {
        lastPublishedLive = list.filterNot { it.key in dismissedKeys }
        rebuildItems()
    }

    private fun rebuildItems() {
        val mocks = mockOverlays.values.filterNot { it.key in dismissedKeys }
        val inbox = inboxOverlays.values.filterNot { it.key in dismissedKeys }
        val live = lastPublishedLive.filterNot { it.key in dismissedKeys }
        _items.value = (mocks + inbox + live)
            .distinctBy { it.key }
            .sortedByDescending { it.postedAt }
    }

    private val mockOverlays = ConcurrentHashMap<String, LiveNotif>()
    private val inboxOverlays = ConcurrentHashMap<String, LiveNotif>()

    /** SMS inbox fallback rows when NLS does not mirror SMS alerts. */
    fun setInboxOverlays(list: List<LiveNotif>) {
        inboxOverlays.clear()
        list.forEach { inboxOverlays[it.key] = it }
        rebuildItems()
    }

    /** Debug/test: inject a fake messaging notif that survives shade republish until dismissed. */
    fun injectMock(item: LiveNotif) {
        dismissedKeys.remove(item.key)
        dismissedConversations.remove(item.conversationId)
        mockOverlays[item.key] = item
        rebuildItems()
    }

    fun clearMock(key: String) {
        mockOverlays.remove(key)
        rebuildItems()
    }

    fun clearAllMocks() {
        mockOverlays.clear()
        rebuildItems()
    }

    fun putReply(key: String, conversationId: String, reply: StoredReply) {
        repliesByKey[key] = reply
        repliesByConversation[conversationId] = reply
    }

    fun retainReplies(keys: Set<String>, conversationIds: Set<String>) {
        repliesByKey.keys.retainAll(keys)
        repliesByConversation.keys.retainAll(conversationIds)
    }

    fun canReply(key: String, conversationId: String): Boolean {
        return repliesByKey.containsKey(key) || repliesByConversation.containsKey(conversationId)
    }

    fun reply(context: Context, key: String, conversationId: String, text: String): Boolean {
        val stored = repliesByKey[key] ?: repliesByConversation[conversationId] ?: return false
        val fill = Intent()
        val results = Bundle().apply {
            putCharSequence(stored.remoteInput.resultKey, text)
        }
        RemoteInput.addResultsToIntent(arrayOf(stored.remoteInput), fill, results)
        return runCatching {
            stored.pendingIntent.send(context, 0, fill)
            true
        }.getOrDefault(false)
    }
}
