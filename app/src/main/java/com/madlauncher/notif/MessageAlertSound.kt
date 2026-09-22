package com.madlauncher.notif

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.util.concurrent.ConcurrentHashMap

/**
 * Plays a short alert when a messaging notification arrives but the system/app
 * would not already alert (HyperOS often silences shade sounds).
 *
 * Voice / audio playback updates the same shade row many times a second.
 * Those must not re-trigger this ding.
 */
object MessageAlertSound {
    private val recentAlerts = ConcurrentHashMap<String, Long>()
    private val recentPackageAlerts = ConcurrentHashMap<String, Long>()
    private val lastFingerprint = ConcurrentHashMap<String, String>()
    private val activeCallCounts = ConcurrentHashMap<String, Int>()
    private const val DEBOUNCE_MS = 1_200L
    private const val SAME_KEY_MS = 8_000L

    @Volatile
    private var playing: Ringtone? = null

    /** Track call notifications so messaging alerts are muted for that app while ringing. */
    fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!NotifClassifier.isCallSbn(sbn)) return
        activeCallCounts.merge(sbn.packageName, 1, Int::plus)
    }

    fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (!NotifClassifier.isCallSbn(sbn)) return
        activeCallCounts.compute(sbn.packageName) { _, count ->
            when {
                count == null || count <= 1 -> null
                else -> count - 1
            }
        }
        lastFingerprint.remove(sbn.key)
    }

    fun onMessagingPosted(
        context: Context,
        sbn: StatusBarNotification,
        rankingMap: NotificationListenerService.RankingMap? = null,
    ) {
        if (sbn.packageName == context.packageName) return
        if (com.madlauncher.BuildConfig.ACTION_BOX_APP) return
        if (NotifClassifier.isCallSbn(sbn)) return
        if (packageInActiveCall(sbn.packageName)) return
        if (!NotifClassifier.isMessagingSbn(sbn)) return
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if (looksLikePlayback(sbn.notification)) {
            stopRingtone()
            lastFingerprint[sbn.key] = contentFingerprint(sbn)
            return
        }
        if (notificationAlreadyAlerts(sbn, rankingMap)) return

        val fingerprint = contentFingerprint(sbn)
        val prev = lastFingerprint.put(sbn.key, fingerprint)
        if (prev == fingerprint) return

        val convKey = conversationKey(sbn) ?: return
        if (!claimAlert(convKey, sbn.key, sbn.packageName)) return
        play(context)
    }

    /**
     * Inbox fallback when NLS misses SMS. Skipped if that SMS package already
     * alerted recently (NLS path won the race).
     */
    fun onSmsInboxAlert(context: Context, threadId: Long, smsPackage: String?) {
        if (smsPackage != null && (recentlyAlertedPackage(smsPackage) || packageInActiveCall(smsPackage))) return
        val convKey = "sms|inbox|$threadId"
        if (!claimAlert(convKey, convKey, smsPackage)) return
        play(context)
    }

    fun recentlyAlertedPackage(packageName: String, withinMs: Long = DEBOUNCE_MS): Boolean {
        val last = recentPackageAlerts[packageName] ?: return false
        return System.currentTimeMillis() - last < withinMs
    }

    fun packageInActiveCall(packageName: String): Boolean = (activeCallCounts[packageName] ?: 0) > 0

    internal fun looksLikePlayback(
        category: String?,
        template: String?,
        hasMediaSession: Boolean,
        hasProgressKey: Boolean,
        progressMax: Int,
        progressIndeterminate: Boolean,
        ongoingWithProgress: Boolean,
    ): Boolean {
        if (hasMediaSession) return true
        if (category == Notification.CATEGORY_TRANSPORT) return true
        if (template?.contains("MediaStyle", ignoreCase = true) == true) return true
        if (progressIndeterminate) return true
        if (progressMax > 0) return true
        if (ongoingWithProgress) return true
        return false
    }

    private fun looksLikePlayback(n: Notification): Boolean {
        val extras = n.extras
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val hasProgress = extras.containsKey(Notification.EXTRA_PROGRESS) ||
            extras.containsKey(Notification.EXTRA_PROGRESS_MAX)
        return looksLikePlayback(
            category = n.category,
            template = extras.getString(Notification.EXTRA_TEMPLATE),
            hasMediaSession = extras.containsKey(Notification.EXTRA_MEDIA_SESSION),
            hasProgressKey = hasProgress,
            progressMax = max,
            progressIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false),
            ongoingWithProgress = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0 && hasProgress,
        )
    }

    internal fun contentFingerprint(
        lastMessageStamp: Long,
        lastMessageText: String?,
        title: String?,
        text: String?,
    ): String = listOf(
        lastMessageStamp.toString(),
        lastMessageText.orEmpty(),
        title.orEmpty(),
        text.orEmpty(),
    ).joinToString("\u0001")

    private fun contentFingerprint(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.notification)
        val last = style?.messages?.lastOrNull()
        return contentFingerprint(
            lastMessageStamp = last?.timestamp ?: 0L,
            lastMessageText = last?.text?.toString(),
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        )
    }

    private fun claimAlert(convKey: String, notifKey: String, packageName: String?): Boolean {
        val now = System.currentTimeMillis()
        val lastKey = recentAlerts[notifKey] ?: 0L
        if (now - lastKey < SAME_KEY_MS) return false
        val lastConv = recentAlerts[convKey] ?: 0L
        if (now - lastConv < DEBOUNCE_MS) return false
        recentAlerts[notifKey] = now
        recentAlerts[convKey] = now
        if (packageName != null) {
            recentPackageAlerts[packageName] = now
        }
        return true
    }

    private fun notificationAlreadyAlerts(
        sbn: StatusBarNotification,
        rankingMap: NotificationListenerService.RankingMap?,
    ): Boolean {
        val n = sbn.notification
        if (n.sound != null) return true
        @Suppress("DEPRECATION")
        if ((n.defaults and Notification.DEFAULT_SOUND) != 0) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || rankingMap == null) return false
        val ranking = NotificationListenerService.Ranking()
        if (!rankingMap.getRanking(sbn.key, ranking)) return false
        val channel = ranking.channel ?: return false
        return channel.importance >= NotificationManager.IMPORTANCE_DEFAULT
    }

    private fun conversationKey(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        val convId = extras.getString("android.conversationId")
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        if (!convId.isNullOrBlank()) {
            return "${sbn.packageName}|conv|${convId.trim()}"
        }
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.notification)
        val label = style?.conversationTitle?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        return label?.takeIf { it.isNotBlank() }?.let { "${sbn.packageName}|${it.trim()}" }
            ?: sbn.key.takeIf { it.isNotBlank() }
    }

    private fun play(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        when (am.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> return
            AudioManager.RINGER_MODE_VIBRATE -> {
                vibrate(context)
                return
            }
        }
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        stopRingtone()
        runCatching {
            RingtoneManager.getRingtone(context, uri)?.also {
                playing = it
                it.play()
            }
        }
        vibrate(context, short = true)
    }

    private fun stopRingtone() {
        runCatching { playing?.stop() }
        playing = null
    }

    private fun vibrate(context: Context, short: Boolean = false) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        val ms = if (short) 35L else 100L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }
}
