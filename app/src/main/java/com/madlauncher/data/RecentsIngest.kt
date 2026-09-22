package com.madlauncher.data

import android.os.SystemClock
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Foreground-app ingest for Recents. Accessibility (and MAD launches) push packages here
 * so apps opened by other apps or shortcuts still appear, matching the phone Overview list.
 */
object RecentsIngest {
    private const val DEBOUNCE_MS = 1_200L
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    @Volatile
    private var lastPkg = ""

    @Volatile
    private var lastAt = 0L

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    fun noteForeground(packageName: String, selfPackage: String) {
        if (!RecentAppsMerge.isCandidate(packageName, selfPackage)) return
        val now = SystemClock.elapsedRealtime()
        if (packageName == lastPkg && now - lastAt < DEBOUNCE_MS) return
        lastPkg = packageName
        lastAt = now
        listeners.forEach { it(packageName) }
    }
}
