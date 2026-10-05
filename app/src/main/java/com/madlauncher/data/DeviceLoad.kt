package com.madlauncher.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File

object DeviceLoad {
    data class Snapshot(
        val battery: Int = -1,
        val cpu: Int = -1,
        val ram: Int = -1,
    )

    private var previousCpu: LongArray? = null

    fun sample(context: Context): Snapshot {
        return Snapshot(
            battery = batteryPercent(context),
            cpu = cpuPercent(),
            ram = ramPercent(context),
        )
    }

    private fun batteryPercent(context: Context): Int {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
        val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return -1
        return (level * 100f / scale).toInt().coerceIn(0, 100)
    }

    private fun ramPercent(context: Context): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return -1
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        if (info.totalMem <= 0L) return -1
        val used = info.totalMem - info.availMem
        return ((used * 100f) / info.totalMem).toInt().coerceIn(0, 100)
    }

    private fun cpuPercent(): Int {
        val now = readCpu() ?: return -1
        val prev = previousCpu
        previousCpu = now
        if (prev == null || prev.size < 2 || now.size < 2) return -1
        val total = now[0] - prev[0]
        val idle = now[1] - prev[1]
        if (total <= 0L) return -1
        return (((total - idle) * 100f) / total).toInt().coerceIn(0, 100)
    }

    /** First value is total jiffies, second is idle. */
    private fun readCpu(): LongArray? {
        val line = runCatching { File("/proc/stat").bufferedReader().use { it.readLine() } }.getOrNull()
            ?: return null
        val parts = line.split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (parts.size < 4) return null
        val idle = parts[3] + parts.getOrElse(4) { 0L }
        return longArrayOf(parts.sum(), idle)
    }
}
