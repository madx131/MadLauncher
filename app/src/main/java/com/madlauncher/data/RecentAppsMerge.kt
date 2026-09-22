package com.madlauncher.data

/** Pure merge rules for Recents + the app-drawer recent row. */
object RecentAppsMerge {
    const val RECENTS_LIMIT = 12
    const val DRAWER_ROW_LIMIT = 8
    /** "New" in the drawer row: installed within this window and never opened. */
    const val NEW_INSTALL_MS = 60L * 60 * 1000

    data class DrawerSlot(val packageName: String, val newlyInstalled: Boolean)

    fun isCandidate(packageName: String, selfPackage: String): Boolean {
        val pkg = packageName.trim()
        if (pkg.isEmpty()) return false
        if (pkg.equals(selfPackage, ignoreCase = true)) return false
        if (isTrampolinePackage(pkg)) return false
        val p = pkg.lowercase()
        if (p.contains("systemui")) return false
        if (p.contains("inputmethod") || p.contains(".latinime")) return false
        if (p.contains("packageinstaller")) return false
        if (p.contains("permissioncontroller")) return false
        if (p == "com.miui.home" || p == "com.google.android.apps.nexuslauncher") return false
        if (p.contains("android.launcher") || p.endsWith(".launcher")) return false
        return true
    }

    /** Shortcut Maker and similar hosts launch the real app through their own activity. */
    fun isTrampolinePackage(packageName: String): Boolean {
        val p = packageName.trim().lowercase()
        if (p.isEmpty()) return false
        if (p.contains("shortcutmaker") || p.contains("quickshortcut")) return true
        if (p.contains("shortcut.maker") || p.contains("shortcut_maker")) return true
        if (p.contains("intentintercept") || p.contains("activitylauncher")) return true
        return p in TRAMPOLINE_PACKAGES
    }

    private val TRAMPOLINE_PACKAGES = setOf(
        "com.sika524.android.quickshortcut",
        "rk.android.app.shortcutmaker",
        "com.alensw.nodelay",
    )

    fun mergeLastUsed(local: Map<String, Long>, usage: Map<String, Long>): Map<String, Long> {
        val out = local.toMutableMap()
        usage.forEach { (pkg, time) ->
            if (time <= 0L) return@forEach
            out[pkg] = maxOf(out[pkg] ?: 0L, time)
        }
        return out
    }

    /**
     * Usage events that mean an app was on screen (including opens from other apps).
     * 1 = ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
     * 2 = ACTIVITY_PAUSED / MOVE_TO_BACKGROUND (HyperOS often logs this when leaving a VIEW/shortcut task)
     * 8 = SHORTCUT_INVOCATION
     * 23 = ACTIVITY_STOPPED
     */
    fun isRecentUsageEvent(eventType: Int): Boolean = when (eventType) {
        1, 2, 8, 23 -> true
        else -> false
    }

    fun foldResumeEvent(
        into: MutableMap<String, Long>,
        packageName: String?,
        eventType: Int,
        timestamp: Long,
    ) {
        if (!isRecentUsageEvent(eventType) || timestamp <= 0L) return
        val pkg = packageName?.trim().orEmpty()
        if (pkg.isEmpty()) return
        into[pkg] = maxOf(into[pkg] ?: 0L, timestamp)
    }

    fun packageFromShortcut(
        itemPackage: String?,
        intentPackage: String?,
        componentPackage: String?,
        resolvedPackage: String?,
    ): String? {
        return listOf(intentPackage, componentPackage, resolvedPackage, itemPackage)
            .map { it?.trim().orEmpty() }
            .firstOrNull { it.isNotBlank() && !isTrampolinePackage(it) }
    }

    fun preferSystemTaskOrder(
        taskNewestFirst: List<String>,
        usageNewestFirst: List<String>,
        limit: Int = RECENTS_LIMIT,
    ): List<String> {
        if (taskNewestFirst.size < 2) return usageNewestFirst.take(limit)
        val out = ArrayList<String>(limit)
        val seen = HashSet<String>()
        for (pkg in taskNewestFirst) {
            if (!seen.add(pkg)) continue
            out += pkg
            if (out.size >= limit) return out
        }
        for (pkg in usageNewestFirst) {
            if (!seen.add(pkg)) continue
            out += pkg
            if (out.size >= limit) return out
        }
        return out
    }

    fun orderByLastUsed(lastUsed: Map<String, Long>): List<String> {
        return lastUsed.entries
            .filter { it.value > 0L }
            .sortedByDescending { it.value }
            .map { it.key }
    }

    fun isNeverOpenedNewInstall(
        firstInstallTime: Long,
        lastUsed: Long,
        now: Long = System.currentTimeMillis(),
        windowMs: Long = NEW_INSTALL_MS,
    ): Boolean {
        if (firstInstallTime <= 0L) return false
        if (lastUsed > 0L) return false
        return firstInstallTime >= now - windowMs
    }

    fun drawerRow(
        recentsNewestFirst: List<String>,
        neverOpenedInstallsNewestFirst: List<String>,
        limit: Int = DRAWER_ROW_LIMIT,
    ): List<DrawerSlot> {
        val out = ArrayList<DrawerSlot>(limit)
        val seen = HashSet<String>()
        for (pkg in neverOpenedInstallsNewestFirst) {
            if (!seen.add(pkg)) continue
            out += DrawerSlot(pkg, newlyInstalled = true)
            if (out.size >= limit) return out
        }
        for (pkg in recentsNewestFirst) {
            if (!seen.add(pkg)) continue
            out += DrawerSlot(pkg, newlyInstalled = false)
            if (out.size >= limit) return out
        }
        return out
    }
}
