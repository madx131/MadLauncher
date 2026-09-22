package com.madlauncher.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import java.text.DateFormat
import java.util.Date

/**
 * adb:
 *   am broadcast -a com.madlauncher.MOCK_MESSAGE -n com.madlauncher/.notif.MockMessageReceiver
 *
 * Injects a Messages-window row + icon badge for an installed messaging package
 * (Samsung Messages / Messenger / Google Messages), and posts a visible shade notif from MAD.
 */
class MockMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION) return
        val pkg = resolveMessagingPackage(context)
        val appName = when (pkg) {
            "com.samsung.android.messaging" -> "Messages"
            "com.facebook.orca" -> "Messenger"
            "com.google.android.apps.messaging" -> "Messages"
            else -> "Messages"
        }
        val who = intent.getStringExtra(EXTRA_WHO) ?: "Alice"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Hey — testing MAD Messages sync"
        val now = System.currentTimeMillis()
        val key = "mock|$pkg|$who|$now"
        val conv = "$pkg|$who"
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now))

        NotificationHub.injectMock(
            LiveNotif(
                key = key,
                packageName = pkg,
                appName = appName,
                title = who,
                body = body,
                time = time,
                postedAt = now,
                conversationId = conv,
                messages = listOf(body),
                canInlineReply = false,
                category = "msg",
            ),
        )
        val badge = SystemBadgeHub.countFor(pkg) + 1
        SystemBadgeHub.publish(SystemBadgeHub.counts.value.toMutableMap().apply { put(pkg, badge) })

        postShadeHint(context, who, body)
    }

    private fun resolveMessagingPackage(context: Context): String {
        val candidates = listOf(
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging",
            "com.facebook.orca",
            "com.android.mms",
        )
        val pm = context.packageManager
        return candidates.firstOrNull { pkg ->
            runCatching { pm.getApplicationInfo(pkg, 0).enabled }.getOrDefault(false)
        } ?: "com.samsung.android.messaging"
    }

    private fun postShadeHint(context: Context, who: String, body: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "MAD test", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val person = Person.Builder().setName(who).build()
        val style = NotificationCompat.MessagingStyle(person)
            .addMessage(body, System.currentTimeMillis(), person)
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(who)
            .setContentText(body)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    companion object {
        const val ACTION = "com.madlauncher.MOCK_MESSAGE"
        const val EXTRA_WHO = "who"
        const val EXTRA_BODY = "body"
        private const val CHANNEL = "mad_mock_message"
        private const val NOTIF_ID = 71001
    }
}
