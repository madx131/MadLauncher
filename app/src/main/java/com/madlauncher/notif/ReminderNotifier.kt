package com.madlauncher.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.madlauncher.ActionBoxActivity
import com.madlauncher.R

/** Surfaces `+` reminders when there is no MAD home list to show them. */
object ReminderNotifier {
    private const val CHANNEL = "mad_reminders"
    private const val NOTIF_BASE = 7200

    fun notify(context: Context, text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val open = PendingIntent.getActivity(
            context,
            body.hashCode(),
            ActionBoxActivity.intent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_action_box)
            .setContentTitle(context.getString(R.string.add_reminder))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(NOTIF_BASE + (body.hashCode() and 0x0fff), notif) }
    }
}
