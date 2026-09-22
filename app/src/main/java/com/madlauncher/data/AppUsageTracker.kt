package com.madlauncher.data

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build

class AppUsageTracker(
    private val context: Context,
    private val store: LocalStore,
) {
    /** Session overlay so Recents does not wait on async prefs or truncated usage logs. */
    private val liveLastUsed = LinkedHashMap<String, Long>()

    fun recordLaunch(packageName: String) {
        if (!RecentAppsMerge.isCandidate(packageName, context.packageName)) return
        noteLive(packageName)
        store.recordAppLaunch(packageName)
    }

    fun touchUsed(packageName: String) {
        if (!RecentAppsMerge.isCandidate(packageName, context.packageName)) return
        noteLive(packageName)
        store.touchAppUsed(packageName)
    }

    fun hasUsageAccess(): Boolean = PermissionChecker.isUsageAccess(context)

    fun usageAccessIntent(): Intent = PermissionChecker.usageAccessSettings()
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun ranked(apps: List<LaunchableApp>, mostUsedOnTop: Boolean): List<LaunchableApp> {
        val local = store.appLaunchStats
        val usageMs = if (hasUsageAccess()) queryUsageMs() else emptyMap()
        val scored = apps
            .filter { it.packageName != context.packageName }
            .map { app ->
                val stats = local[app.packageName]
                val launches = stats?.count ?: 0
                val last = stats?.lastUsed ?: 0L
                val fg = usageMs[app.packageName] ?: 0L
                val score = fg + launches * 45_000L
                Triple(app, score, last)
            }
            .filter { it.second > 0 || it.third > 0 }
            .sortedWith(
                compareByDescending<Triple<LaunchableApp, Long, Long>> { it.second }
                    .thenByDescending { it.third },
            )
            .map { it.first }
            .distinctBy { it.packageName }
        val ordered = if (mostUsedOnTop) scored else scored.asReversed()
        return ordered.take(24)
    }

    data class DrawerRecentApp(
        val app: LaunchableApp,
        val newlyInstalled: Boolean,
    )

    fun lastUsedByPackage(apps: List<LaunchableApp>): Map<String, Long> {
        val self = context.packageName
        val local = store.appLaunchStats
            .filterKeys { RecentAppsMerge.isCandidate(it, self) }
            .mapValues { it.value.lastUsed }
        val live = liveSnapshot().filterKeys { RecentAppsMerge.isCandidate(it, self) }
        val device = queryDeviceRecentMs().filterKeys { RecentAppsMerge.isCandidate(it, self) }
        noteLiveAll(device)
        return RecentAppsMerge.mergeLastUsed(RecentAppsMerge.mergeLastUsed(local, live), device)
    }

    fun recents(apps: List<LaunchableApp>): List<LaunchableApp> {
        val self = context.packageName
        val byPkg = apps.filter { RecentAppsMerge.isCandidate(it.packageName, self) }
            .distinctBy { it.packageName }
            .associateBy { it.packageName }
            .toMutableMap()
        val lastUsed = lastUsedByPackage(apps)
        val usageOrder = RecentAppsMerge.orderByLastUsed(lastUsed)
        val taskOrder = querySystemTaskRecents()
            .filterKeys { RecentAppsMerge.isCandidate(it, self) }
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
        val ordered = RecentAppsMerge.preferSystemTaskOrder(taskOrder, usageOrder)
        return ordered.mapNotNull { pkg ->
            byPkg[pkg] ?: resolveLaunchable(pkg)?.also { byPkg[pkg] = it }
        }.take(RecentAppsMerge.RECENTS_LIMIT)
    }

    fun resolveLaunchable(packageName: String): LaunchableApp? {
        if (!RecentAppsMerge.isCandidate(packageName, context.packageName)) return null
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return null
        val cn = intent.component ?: return null
        val label = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
        val icon = runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
        return LaunchableApp(label, cn.packageName, cn.className, icon)
    }

    fun drawerRecentRow(
        recentsNewestFirst: List<LaunchableApp>,
        allApps: List<LaunchableApp>,
    ): List<DrawerRecentApp> {
        val self = context.packageName
        val pool = (recentsNewestFirst + allApps)
            .filter { RecentAppsMerge.isCandidate(it.packageName, self) }
            .distinctBy { it.packageName }
        val lastUsed = lastUsedByPackage(pool)
        val recentPkgs = recentsNewestFirst.map { it.packageName }.toSet()
        val newInstalls = neverOpenedNewInstalls(pool, lastUsed)
            .filter { it.packageName !in recentPkgs }
        val byPkg = pool.associateBy { it.packageName }
        return RecentAppsMerge.drawerRow(
            recentsNewestFirst = recentsNewestFirst.map { it.packageName },
            neverOpenedInstallsNewestFirst = newInstalls.map { it.packageName },
        ).mapNotNull { slot ->
            val app = byPkg[slot.packageName] ?: return@mapNotNull null
            DrawerRecentApp(app, slot.newlyInstalled)
        }
    }

    private fun neverOpenedNewInstalls(
        apps: List<LaunchableApp>,
        lastUsed: Map<String, Long>,
    ): List<LaunchableApp> {
        val pm = context.packageManager
        val now = System.currentTimeMillis()
        return apps.mapNotNull { app ->
            val installed = runCatching {
                pm.getPackageInfo(app.packageName, 0).firstInstallTime
            }.getOrDefault(0L)
            if (RecentAppsMerge.isNeverOpenedNewInstall(installed, lastUsed[app.packageName] ?: 0L, now)) {
                app to installed
            } else {
                null
            }
        }.sortedByDescending { it.second }.map { it.first }.distinctBy { it.packageName }
    }

    private fun noteLive(packageName: String, time: Long = System.currentTimeMillis()) {
        if (!RecentAppsMerge.isCandidate(packageName, context.packageName) || time <= 0L) return
        synchronized(liveLastUsed) {
            val prev = liveLastUsed[packageName] ?: 0L
            if (time > prev) liveLastUsed[packageName] = time
            while (liveLastUsed.size > 48) {
                val oldest = liveLastUsed.minByOrNull { it.value }?.key ?: break
                liveLastUsed.remove(oldest)
            }
        }
    }

    private fun noteLiveAll(times: Map<String, Long>) {
        times.forEach { (pkg, time) -> noteLive(pkg, time) }
    }

    private fun liveSnapshot(): Map<String, Long> = synchronized(liveLastUsed) { liveLastUsed.toMap() }

    /** Last-used timestamps from the phone: system recents tasks, usage events, and usage stats. */
    private fun queryDeviceRecentMs(): Map<String, Long> {
        val out = LinkedHashMap<String, Long>()
        fun putAll(src: Map<String, Long>) {
            src.forEach { (pkg, time) ->
                if (time <= 0L) return@forEach
                out[pkg] = maxOf(out[pkg] ?: 0L, time)
            }
        }
        putAll(querySystemTaskRecents())
        if (hasUsageAccess()) {
            putAll(queryForegroundEvents())
            putAll(queryLastUsedMs())
        }
        return out
    }

    /**
     * System Overview/Recents task list. Third-party apps usually only see their own task;
     * MAD OS / privileged installs can return the full phone list.
     */
    @Suppress("DEPRECATION")
    private fun querySystemTaskRecents(): Map<String, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return emptyMap()
        val tasks = runCatching {
            am.getRecentTasks(25, ActivityManager.RECENT_IGNORE_UNAVAILABLE)
        }.getOrNull().orEmpty()
        if (tasks.isEmpty()) return emptyMap()
        val now = System.currentTimeMillis()
        val out = LinkedHashMap<String, Long>()
        tasks.forEachIndexed { index, info ->
            val pkg = info.baseIntent?.component?.packageName
                ?: info.origActivity?.packageName
                ?: info.baseActivity?.packageName
                ?: info.topActivity?.packageName
            if (pkg.isNullOrBlank()) return@forEachIndexed
            out[pkg] = maxOf(out[pkg] ?: 0L, now - index)
        }
        return out
    }

    private fun queryForegroundEvents(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - 3L * 24 * 60 * 60 * 1000
        val events = runCatching { usm.queryEvents(start, end) }.getOrNull() ?: return emptyMap()
        val out = LinkedHashMap<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            RecentAppsMerge.foldResumeEvent(
                into = out,
                packageName = event.packageName,
                eventType = event.eventType,
                timestamp = event.timeStamp,
            )
        }
        return out
    }

    private fun queryLastUsedMs(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - 7L * 24 * 60 * 60 * 1000
        val out = LinkedHashMap<String, Long>()
        fun put(pkg: String, last: Long) {
            if (last <= 0L) return
            out[pkg] = maxOf(out[pkg] ?: 0L, last)
        }
        runCatching { usm.queryAndAggregateUsageStats(start, end) }.getOrNull()?.forEach { (pkg, stat) ->
            put(pkg, lastUsedOf(stat))
        }
        runCatching {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
        }.getOrNull().orEmpty().forEach { stat ->
            put(stat.packageName, lastUsedOf(stat))
        }
        return out
    }

    private fun lastUsedOf(stat: UsageStats): Long {
        var last = stat.lastTimeUsed
        if (Build.VERSION.SDK_INT >= 29) {
            last = maxOf(last, stat.lastTimeVisible)
        }
        return last
    }

    private fun queryUsageMs(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - 14L * 24 * 60 * 60 * 1000
        val stats = runCatching { usm.queryAndAggregateUsageStats(start, end) }.getOrNull()
            ?: return emptyMap()
        return stats.mapValues { it.value.totalTimeInForeground }
    }
}
