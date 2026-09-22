package com.madlauncher.notif

import android.Manifest
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.madlauncher.action.ActionApps
import java.text.DateFormat
import java.util.Date

/**
 * Observes the SMS content provider when [Manifest.permission.READ_SMS] is granted.
 * Supplies badges, MAD Messages rows, and alert sound when NLS does not receive SMS.
 */
object SmsInboxMonitor {
    private val inboxUri: Uri = Telephony.Sms.CONTENT_URI
    private var observer: ContentObserver? = null
    private var lastThreadAlertAt = mutableMapOf<Long, Long>()
    private var started = false

    fun start(context: Context) {
        if (com.madlauncher.BuildConfig.ACTION_BOX_APP) {
            stop(context)
            return
        }
        val app = context.applicationContext
        if (!hasReadSms(app)) {
            stop(app)
            return
        }
        MessagingAppRegistry.refresh(app, ActionApps(app))
        if (observer == null) {
            observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    refresh(app)
                }
            }
            app.contentResolver.registerContentObserver(inboxUri, true, observer!!)
        }
        if (!started) {
            started = true
            refresh(app)
        }
    }

    fun stop(context: Context) {
        observer?.let { context.applicationContext.contentResolver.unregisterContentObserver(it) }
        observer = null
        started = false
        SmsInboxHub.clear()
    }

    fun refresh(context: Context) {
        val app = context.applicationContext
        if (!hasReadSms(app)) return
        MessagingAppRegistry.refresh(app, ActionApps(app))

        val threads = queryRecentThreads(app)
        val unreadThreads = queryUnreadThreads(app)
        val smsPkgs = MessagingAppRegistry.smsPackages
        if (smsPkgs.isEmpty()) return

        val defaultPkg = MessagingAppRegistry.defaultSmsPackage(app)
            ?: smsPkgs.firstOrNull()
            ?: return
        // Icon badges follow the shade only — do not copy SMS-database unread
        // (hundreds of old threads) onto Messages as 99+.
        SmsInboxHub.publishInbox(emptyMap())
        val appName = runCatching {
            app.packageManager.getApplicationLabel(
                app.packageManager.getApplicationInfo(defaultPkg, 0),
            ).toString()
        }.getOrDefault("Messages")

        val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
        val overlays = threads.map { thread ->
            LiveNotif(
                key = "sms|inbox|${thread.threadId}",
                packageName = defaultPkg,
                appName = appName,
                title = thread.address,
                body = thread.body,
                time = fmt.format(Date(thread.date)),
                postedAt = thread.date,
                conversationId = "sms|${thread.address.trim().lowercase()}",
                messages = listOf(thread.body),
                canInlineReply = false,
                category = android.app.Notification.CATEGORY_MESSAGE,
            )
        }
        NotificationHub.setInboxOverlays(overlays)

        val now = System.currentTimeMillis()
        for (thread in unreadThreads) {
            val last = lastThreadAlertAt[thread.threadId] ?: 0L
            if (thread.date > last) {
                lastThreadAlertAt[thread.threadId] = thread.date
                if (last > 0L || unreadThreads.size == 1) {
                    if (!MessageAlertSound.recentlyAlertedPackage(defaultPkg)) {
                        MessageAlertSound.onSmsInboxAlert(app, thread.threadId, defaultPkg)
                    }
                }
            }
        }
        // Drop stale thread keys
        val liveIds = unreadThreads.map { it.threadId }.toSet()
        lastThreadAlertAt.keys.retainAll(liveIds)
    }

    private data class SmsThread(
        val threadId: Long,
        val address: String,
        val body: String,
        val date: Long,
    )

    /** Latest message per thread — read and unread — for Action Box history. */
    private fun queryRecentThreads(context: Context, limit: Int = 60): List<SmsThread> {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC",
            )
        }.getOrNull() ?: return emptyList()

        val byThread = LinkedHashMap<Long, SmsThread>()
        cursor.use {
            val threadIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
            val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            while (it.moveToNext() && byThread.size < limit) {
                val threadId = it.getLong(threadIdx)
                if (threadId in byThread) continue
                val address = it.getString(addrIdx)?.takeIf { a -> a.isNotBlank() } ?: "Unknown"
                val body = it.getString(bodyIdx)?.takeIf { b -> b.isNotBlank() } ?: "New message"
                val date = it.getLong(dateIdx)
                byThread[threadId] = SmsThread(threadId, address, body, date)
            }
        }
        return byThread.values.toList()
    }

    private fun queryUnreadThreads(context: Context): List<SmsThread> {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                "${Telephony.Sms.READ}=0",
                null,
                "${Telephony.Sms.DATE} DESC",
            )
        }.getOrNull() ?: return emptyList()

        val byThread = LinkedHashMap<Long, SmsThread>()
        cursor.use {
            val idIdx = it.getColumnIndex(Telephony.Sms._ID)
            val threadIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
            val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            while (it.moveToNext()) {
                val threadId = it.getLong(threadIdx)
                if (threadId in byThread) continue
                val address = it.getString(addrIdx)?.takeIf { a -> a.isNotBlank() } ?: "Unknown"
                val body = it.getString(bodyIdx)?.takeIf { b -> b.isNotBlank() } ?: "New message"
                val date = it.getLong(dateIdx)
                byThread[threadId] = SmsThread(threadId, address, body, date)
            }
        }
        return byThread.values.toList()
    }

    fun hasReadSms(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
