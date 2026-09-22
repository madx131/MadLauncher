package com.madlauncher.notif

import android.app.Notification
import android.app.RemoteInput
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * True 1:1 shade mirror for MAD panels.
 *
 * - Feed = [activeNotifications] only (no snooze / hidden ghosts).
 * - Messages = `@` apps filter in the UI (messaging + email); System & apps = everything else.
 * - MAD dismiss/mark-read → [cancelNotification] / mark-read on the same key.
 * - Shade remove → drop from MAD immediately.
 * - Badges = [SystemBadgeCounter] (system settings + universal OEM fallback).
 */
class MadNotificationListener : NotificationListenerService() {
    /** Keys MAD asked to hide; OEM bounce re-posts are cancelled until a newer postTime. */
    private val pendingCancel = ConcurrentHashMap.newKeySet<String>()
    private val cancelledPostTime = ConcurrentHashMap<String, Long>()
    private val mainHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    override fun onListenerConnected() {
        NotificationHub.setConnected(true)
        NotificationHub.cancel = { key -> hideKey(key) }
        NotificationHub.open = { key ->
            val sbn = findActive(key)
            rememberHide(key, sbn?.postTime ?: 0L)
            NotificationHub.notifyClearedBySystem(key, sbn?.let { conversationIdOf(it) })
            publish()
            publishSystemBadges(currentRanking)
            runCatching { sbn?.notification?.contentIntent?.send() }
            hideKey(key)
        }
        NotificationHub.markRead = markRead@{ key ->
            val sbn = findActive(key)
            val action = sbn?.notification?.let { findMarkReadAction(it) }
            if (action?.actionIntent != null) {
                val sent = runCatching {
                    action.actionIntent.send()
                    true
                }.getOrDefault(false)
                if (sent) {
                    rememberHide(key, sbn?.postTime ?: 0L)
                    NotificationHub.notifyClearedBySystem(key, sbn?.let { conversationIdOf(it) })
                    hideKey(key)
                    publish()
                    publishSystemBadges(currentRanking)
                    return@markRead true
                }
            }
            rememberHide(key, sbn?.postTime ?: 0L)
            NotificationHub.notifyClearedBySystem(key, sbn?.let { conversationIdOf(it) })
            hideKey(key)
            publish()
            publishSystemBadges(currentRanking)
            true
        }
        NotificationHub.clearPackage = { pkg ->
            clearPackageFromShadeAndMad(pkg)
        }
        SystemShadeController.syncWithAppearance(this, com.madlauncher.NavShortcutBridge.settings)
        publish()
        publishSystemBadges(currentRanking)
    }

    override fun onListenerDisconnected() {
        pendingCancel.clear()
        NotificationHub.setConnected(false)
        SystemBadgeHub.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        onNotificationPosted(sbn, currentRanking)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        if (sbn != null && shouldSuppressBounce(sbn)) {
            hideFromShade(sbn)
            scheduleSnoozeIfStillActive(sbn.key)
        } else if (sbn != null) {
            pendingCancel.remove(sbn.key)
            cancelledPostTime.remove(sbn.key)
            NotificationHub.clearDismissed(sbn.key)
            NotificationHub.clearDismissedConversation(conversationIdOf(sbn))
            MessageAlertSound.onNotificationPosted(sbn)
            MessageAlertSound.onMessagingPosted(this, sbn, rankingMap ?: currentRanking)
        }
        publish()
        publishSystemBadges(rankingMap ?: currentRanking)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        if (sbn != null) {
            NotificationHub.notifyClearedBySystem(sbn.key, conversationIdOf(sbn))
            MessageAlertSound.onNotificationRemoved(sbn)
            if (ShadeSync.isUserShadeDismiss(reason)) {
                rememberHide(sbn.key, sbn.postTime)
            } else if (reason == REASON_SNOOZED) {
                pendingCancel.remove(sbn.key)
                cancelledPostTime.remove(sbn.key)
            }
            mainHandler.postDelayed({
                if (findActive(sbn.key) == null) {
                    pendingCancel.remove(sbn.key)
                    cancelledPostTime.remove(sbn.key)
                }
            }, ShadeSync.BOUNCE_MS)
        }
        publish()
        publishSystemBadges(rankingMap ?: currentRanking)
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
        publishSystemBadges(rankingMap)
    }

    private fun clearPackageFromShadeAndMad(packageName: String) {
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        active.filter { it.packageName == packageName }.forEach { sbn ->
            rememberHide(sbn.key, sbn.postTime)
            NotificationHub.notifyClearedBySystem(sbn.key, conversationIdOf(sbn))
            hideFromShade(sbn)
            scheduleSnoozeIfStillActive(sbn.key)
        }
        publish()
        publishSystemBadges(currentRanking)
    }

    private fun hideKey(key: String) {
        val sbn = findActive(key)
        rememberHide(key, sbn?.postTime ?: cancelledPostTime[key] ?: 0L)
        if (sbn != null) {
            hideFromShade(sbn)
            hideSameConversation(sbn)
            hideOrphanSummary(sbn)
        } else {
            runCatching { cancelNotification(key) }
        }
        NotificationHub.notifyClearedBySystem(key, sbn?.let { conversationIdOf(it) })
        publish()
        publishSystemBadges(currentRanking)
        scheduleSnoozeIfStillActive(key)
    }

    /** Cancel first (AOSP / Samsung / MAD OS). Snooze only if the OEM left the row in the shade. */
    private fun hideFromShade(sbn: StatusBarNotification) {
        rememberHide(sbn.key, sbn.postTime)
        runCatching { cancelNotification(sbn.key) }
    }

    private fun scheduleSnoozeIfStillActive(key: String) {
        mainHandler.postDelayed({
            val still = findActive(key) ?: return@postDelayed
            if (keepInShade(still)) return@postDelayed
            if (!shouldSuppressBounce(still) && still.key !in pendingCancel) return@postDelayed
            snoozeKey(still.key)
            runCatching { cancelNotification(still.key) }
            publish()
            publishSystemBadges(currentRanking)
        }, ShadeSync.CANCEL_RETRY_MS)
    }

    private fun keepInShade(sbn: StatusBarNotification): Boolean {
        if (sbn.isOngoing) return true
        val flags = sbn.notification.flags
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return true
        val cat = sbn.notification.category
        if (cat == Notification.CATEGORY_CALL || cat == Notification.CATEGORY_TRANSPORT) return true
        if (sbn.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return true
        return false
    }

    private fun hideSameConversation(sbn: StatusBarNotification) {
        val conv = conversationIdOf(sbn)
        val appName = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        if (!ShadeSync.canExpandConversation(conv, sbn.packageName, appName)) return
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        active.filter { it.key != sbn.key && conversationIdOf(it) == conv }.forEach { peer ->
            hideFromShade(peer)
            NotificationHub.notifyClearedBySystem(peer.key, conv)
            scheduleSnoozeIfStillActive(peer.key)
        }
    }

    private fun hideOrphanSummary(sbn: StatusBarNotification) {
        val group = sbn.groupKey?.takeIf { it.isNotBlank() } ?: return
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        val children = active.filter {
            it.groupKey == group &&
                it.key != sbn.key &&
                (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 &&
                it.key !in pendingCancel
        }
        if (children.isNotEmpty()) return
        active.filter {
            it.groupKey == group && (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        }.forEach { summary ->
            hideFromShade(summary)
            NotificationHub.notifyClearedBySystem(summary.key, conversationIdOf(summary))
            scheduleSnoozeIfStillActive(summary.key)
        }
    }

    private fun rememberHide(key: String, postTime: Long) {
        pendingCancel.add(key)
        cancelledPostTime[key] = maxOf(cancelledPostTime[key] ?: 0L, postTime)
    }

    private fun shouldSuppressBounce(sbn: StatusBarNotification): Boolean =
        ShadeSync.isBounceRepost(
            pending = sbn.key in pendingCancel,
            cancelledPostTime = cancelledPostTime[sbn.key] ?: 0L,
            newPostTime = sbn.postTime,
        )

    private fun snoozeKey(key: String) {
        if (Build.VERSION.SDK_INT < 26) return
        runCatching { snoozeNotification(key, ShadeSync.SNOOZE_MS) }
    }

    private fun findActive(key: String): StatusBarNotification? {
        return runCatching { activeNotifications?.firstOrNull { it.key == key } }.getOrNull()
    }

    private fun publish() {
        val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
        val pm = packageManager
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        val keys = mutableSetOf<String>()
        val convIds = mutableSetOf<String>()
        val list = active
            .filter { sbn ->
                sbn.key !in pendingCancel &&
                    sbn.packageName != packageName &&
                    (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 &&
                    hasUsableContent(sbn.notification)
            }
            .map { sbn ->
                val extras = sbn.notification.extras
                val appName = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                }.getOrDefault(sbn.packageName)
                val messaging = messagingLines(sbn.notification)
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
                        ?.takeIf { it.isNotBlank() }
                    ?: messaging?.person
                    ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                        ?.takeIf { it.isNotBlank() }
                    ?: appName
                val body = messaging?.latest
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                        ?.takeIf { it.isNotBlank() }
                    ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                        ?.takeIf { it.isNotBlank() }
                    ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString().orEmpty()
                val conversationId = conversationIdOf(sbn)
                val lines = messaging?.lines
                    ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                        ?.map { it.toString() }
                        .orEmpty()
                        .ifEmpty { listOfNotNull(body.takeIf { it.isNotBlank() }) }
                val reply = findReply(sbn.notification)
                if (reply != null) {
                    NotificationHub.putReply(sbn.key, conversationId, reply)
                    keys.add(sbn.key)
                    convIds.add(conversationId)
                }
                LiveNotif(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appName = appName,
                    title = title,
                    body = body,
                    time = fmt.format(Date(sbn.postTime)),
                    postedAt = sbn.postTime,
                    conversationId = conversationId,
                    messages = lines,
                    canInlineReply = reply != null,
                    ongoing = sbn.isOngoing,
                    category = sbn.notification.category,
                    hasMediaSession = extras.containsKey(Notification.EXTRA_MEDIA_SESSION),
                )
            }
            .sortedByDescending { it.postedAt }
        NotificationHub.retainReplies(keys, convIds)
        NotificationHub.publish(list)
    }

    private fun publishSystemBadges(rankingMap: RankingMap?) {
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        val totals = SystemBadgeCounter.compute(
            active = active,
            ownPackage = packageName,
            pendingCancelKeys = pendingCancel,
        )
        SystemBadgeHub.publishNls(totals)
    }

    private fun conversationIdOf(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        val messaging = messagingLines(sbn.notification)
        val shortcut = if (Build.VERSION.SDK_INT >= 29) {
            sbn.notification.shortcutId?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        val conv = shortcut
            ?: extras.getString("android.shortcutId")?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
                ?.takeIf { it.isNotBlank() }
            ?: messaging?.person?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                ?.takeIf { it.isNotBlank() }
            ?: sbn.tag?.takeIf { it.isNotBlank() }
            ?: sbn.packageName
        return "${sbn.packageName}|${conv.trim().lowercase()}"
    }

    private data class MessagingContent(
        val person: String?,
        val latest: String?,
        val lines: List<String>,
    )

    private fun messagingLines(notification: Notification): MessagingContent? {
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
            ?: return null
        val lines = style.messages.mapNotNull { msg ->
            msg.text?.toString()?.takeIf { it.isNotBlank() }
        }
        if (lines.isEmpty()) return null
        val person = style.conversationTitle?.toString()
            ?: style.user?.name?.toString()
            ?: style.messages.lastOrNull()?.person?.name?.toString()
        return MessagingContent(person, lines.lastOrNull(), lines)
    }

    private fun hasUsableContent(notification: Notification): Boolean {
        if (NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification) != null) {
            return true
        }
        val extras = notification.extras
        if (extras.getCharSequence(Notification.EXTRA_TITLE) != null) return true
        if (extras.getCharSequence(Notification.EXTRA_TEXT) != null) return true
        if (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) != null) return true
        if (extras.getCharSequence(Notification.EXTRA_SUB_TEXT) != null) return true
        if (extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE) != null) return true
        if (extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) != null) return true
        if (Build.VERSION.SDK_INT >= 24 && extras.getParcelableArray(Notification.EXTRA_MESSAGES) != null) {
            return true
        }
        return notification.tickerText != null
    }

    private fun findReply(notification: Notification): StoredReply? {
        val actions = buildList {
            notification.actions?.let { addAll(it) }
            addAll(Notification.WearableExtender(notification).actions)
        }
        val preferred = actions.firstOrNull { action ->
            hasRemoteInput(action) && isSemanticReply(action)
        } ?: actions.firstOrNull { hasRemoteInput(it) }
        val remote = pickRemoteInput(preferred) ?: return null
        val intent = preferred?.actionIntent ?: return null
        return StoredReply(intent, remote)
    }

    private fun findMarkReadAction(notification: Notification): Notification.Action? {
        val actions = buildList {
            notification.actions?.let { addAll(it) }
            addAll(Notification.WearableExtender(notification).actions)
        }
        if (Build.VERSION.SDK_INT >= 28) {
            actions.firstOrNull {
                it.semanticAction == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ &&
                    it.actionIntent != null
            }?.let { return it }
        }
        return actions.firstOrNull { action ->
            if (action.actionIntent == null || hasRemoteInput(action)) return@firstOrNull false
            val title = action.title?.toString()?.lowercase().orEmpty()
            title.contains("mark as read") ||
                title.contains("mark read") ||
                title == "read" ||
                title.contains("als gelezen") ||
                title.contains("gelezen")
        }
    }

    private fun hasRemoteInput(action: Notification.Action): Boolean {
        return !action.remoteInputs.isNullOrEmpty()
    }

    private fun isSemanticReply(action: Notification.Action): Boolean {
        if (Build.VERSION.SDK_INT < 28) return false
        return action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY
    }

    private fun pickRemoteInput(action: Notification.Action?): RemoteInput? {
        return action?.remoteInputs?.firstOrNull { it.allowFreeFormInput }
            ?: action?.remoteInputs?.firstOrNull()
    }
}

