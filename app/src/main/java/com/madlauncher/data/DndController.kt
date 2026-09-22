package com.madlauncher.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService

object DndController {
    const val ACTION_DND_OFF = "com.madlauncher.action.DND_OFF"
    private const val PREFS = "mad_dnd"
    private const val KEY_UNTIL = "until_ms"
    private const val REQ = 4401

    data class DurationOption(val label: String, val minutes: Int)

    /** Presets from 30 minutes up to 12 hours. */
    val durationOptions: List<DurationOption> = listOf(
        DurationOption("30 minutes", 30),
        DurationOption("1 hour", 60),
        DurationOption("2 hours", 120),
        DurationOption("3 hours", 180),
        DurationOption("4 hours", 240),
        DurationOption("6 hours", 360),
        DurationOption("8 hours", 480),
        DurationOption("12 hours", 720),
    )

    fun hasPolicyAccess(context: Context): Boolean {
        val nm = context.getSystemService<NotificationManager>() ?: return false
        return nm.isNotificationPolicyAccessGranted
    }

    fun policyAccessIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun isEnabled(context: Context): Boolean {
        val nm = context.getSystemService<NotificationManager>() ?: return false
        return when (nm.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
            -> false
            else -> true
        }
    }

    /** Remaining timed session, or null if indefinite / off. */
    fun untilMillis(context: Context): Long? {
        val until = prefs(context).getLong(KEY_UNTIL, 0L)
        if (until <= 0L) return null
        if (until <= System.currentTimeMillis()) {
            clearUntil(context)
            return null
        }
        return until
    }

    fun toggle(context: Context): Boolean {
        return if (isEnabled(context)) {
            turnOff(context)
            false
        } else {
            turnOn(context, durationMinutes = null)
            true
        }
    }

    /**
     * @param durationMinutes null = until manually turned off
     * @return true if DND is now on
     */
    fun turnOn(context: Context, durationMinutes: Int?): Boolean {
        if (!hasPolicyAccess(context)) return false
        val nm = context.getSystemService<NotificationManager>() ?: return false
        runCatching {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }.onFailure { return false }
        cancelAlarm(context)
        if (durationMinutes != null && durationMinutes > 0) {
            val until = System.currentTimeMillis() + durationMinutes * 60_000L
            prefs(context).edit().putLong(KEY_UNTIL, until).apply()
            scheduleOff(context, until)
        } else {
            clearUntil(context)
        }
        return true
    }

    fun turnOff(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        runCatching { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL) }
        cancelAlarm(context)
        clearUntil(context)
    }

    private fun scheduleOff(context: Context, atMillis: Long) {
        val am = context.getSystemService<AlarmManager>() ?: return
        val pi = pendingOff(context)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }.onFailure {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        }
    }

    private fun cancelAlarm(context: Context) {
        val am = context.getSystemService<AlarmManager>() ?: return
        am.cancel(pendingOff(context))
    }

    private fun pendingOff(context: Context): PendingIntent {
        val intent = Intent(context, DndAlarmReceiver::class.java).setAction(ACTION_DND_OFF)
        return PendingIntent.getBroadcast(
            context,
            REQ,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun clearUntil(context: Context) {
        prefs(context).edit().remove(KEY_UNTIL).apply()
    }
}

class DndAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DndController.ACTION_DND_OFF) return
        DndController.turnOff(context)
    }
}
