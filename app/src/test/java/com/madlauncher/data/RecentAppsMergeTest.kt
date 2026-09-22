package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentAppsMergeTest {
    private val self = "com.madlauncher"

    @Test
    fun skipsLauncherAndSystemUi() {
        assertFalse(RecentAppsMerge.isCandidate("com.madlauncher", self))
        assertFalse(RecentAppsMerge.isCandidate("com.android.systemui", self))
        assertFalse(RecentAppsMerge.isCandidate("com.miui.home", self))
        assertFalse(RecentAppsMerge.isCandidate("com.google.android.inputmethod.latin", self))
        assertTrue(RecentAppsMerge.isCandidate("com.android.chrome", self))
        assertTrue(RecentAppsMerge.isCandidate("com.miui.gallery", self))
        assertTrue(RecentAppsMerge.isCandidate("com.whatsapp", self))
        assertFalse(RecentAppsMerge.isCandidate("com.sika524.android.quickshortcut", self))
        assertFalse(RecentAppsMerge.isTrampolinePackage("com.tiny.browser"))
        assertTrue(RecentAppsMerge.isTrampolinePackage("com.sika524.android.quickshortcut"))
        assertTrue(RecentAppsMerge.isTrampolinePackage("rk.android.app.shortcutmaker"))
    }

    @Test
    fun resumeEventsCaptureAppsLaunchedByOthers() {
        val into = mutableMapOf<String, Long>()
        RecentAppsMerge.foldResumeEvent(into, "com.whatsapp", eventType = 1, timestamp = 10L)
        RecentAppsMerge.foldResumeEvent(into, "com.android.chrome", eventType = 1, timestamp = 50L)
        RecentAppsMerge.foldResumeEvent(into, "com.android.chrome", eventType = 2, timestamp = 80L)
        assertEquals(80L, into["com.android.chrome"])
        assertEquals(
            listOf("com.android.chrome", "com.whatsapp"),
            RecentAppsMerge.orderByLastUsed(into),
        )
    }

    @Test
    fun pauseEventCountsShortcutOpenedBrowser() {
        val into = mutableMapOf<String, Long>()
        RecentAppsMerge.foldResumeEvent(into, "com.tiny.browser", eventType = 2, timestamp = 90L)
        assertEquals(listOf("com.tiny.browser"), RecentAppsMerge.orderByLastUsed(into))
    }

    @Test
    fun shortcutIgnoresMakerAndKeepsLaunchedApp() {
        val pkg = RecentAppsMerge.packageFromShortcut(
            itemPackage = "com.sika524.android.quickshortcut",
            intentPackage = "com.sika524.android.quickshortcut",
            componentPackage = "com.sika524.android.quickshortcut",
            resolvedPackage = "com.tiny.browser",
        )
        assertEquals("com.tiny.browser", pkg)
        assertEquals(
            null,
            RecentAppsMerge.packageFromShortcut(
                itemPackage = "com.sika524.android.quickshortcut",
                intentPackage = null,
                componentPackage = null,
                resolvedPackage = null,
            ),
        )
    }

    @Test
    fun systemTaskOrderWinsWhenPhoneRecentsAreReadable() {
        val ordered = RecentAppsMerge.preferSystemTaskOrder(
            taskNewestFirst = listOf("com.android.chrome", "com.whatsapp"),
            usageNewestFirst = listOf("com.spotify.music", "com.whatsapp"),
            limit = 12,
        )
        assertEquals(listOf("com.android.chrome", "com.whatsapp", "com.spotify.music"), ordered)
    }

    @Test
    fun usageOrderUsedWhenSystemTasksAreOnlySelf() {
        val ordered = RecentAppsMerge.preferSystemTaskOrder(
            taskNewestFirst = listOf("com.madlauncher"),
            usageNewestFirst = listOf("com.whatsapp", "com.android.chrome"),
        )
        assertEquals(listOf("com.whatsapp", "com.android.chrome"), ordered)
    }

    @Test
    fun usageStatsWinWhenNewerThanLocalLaunch() {
        val merged = RecentAppsMerge.mergeLastUsed(
            local = mapOf("com.whatsapp" to 10L),
            usage = mapOf("com.android.chrome" to 50L, "com.whatsapp" to 20L),
        )
        assertEquals(listOf("com.android.chrome", "com.whatsapp"), RecentAppsMerge.orderByLastUsed(merged))
    }

    @Test
    fun drawerRowFollowsLauncherRecentsAfterNewInstalls() {
        val row = RecentAppsMerge.drawerRow(
            recentsNewestFirst = listOf("com.whatsapp", "com.android.chrome"),
            neverOpenedInstallsNewestFirst = listOf("com.spotify.music"),
            limit = 8,
        )
        assertEquals("com.spotify.music", row[0].packageName)
        assertTrue(row[0].newlyInstalled)
        assertEquals("com.whatsapp", row[1].packageName)
        assertFalse(row[1].newlyInstalled)
        assertEquals("com.android.chrome", row[2].packageName)
        assertEquals(3, row.size)
    }

    @Test
    fun drawerRowDoesNotKeepOpenedInstallAsNew() {
        val row = RecentAppsMerge.drawerRow(
            recentsNewestFirst = listOf("com.spotify.music", "com.whatsapp"),
            neverOpenedInstallsNewestFirst = emptyList(),
        )
        assertEquals(2, row.size)
        assertEquals("com.spotify.music", row[0].packageName)
        assertFalse(row[0].newlyInstalled)
    }

    @Test
    fun newInstallOnlyWithinHourAndNeverOpened() {
        val now = 10_000_000L
        val hour = RecentAppsMerge.NEW_INSTALL_MS
        assertTrue(RecentAppsMerge.isNeverOpenedNewInstall(now - 5 * 60 * 1000, lastUsed = 0L, now = now))
        assertFalse(RecentAppsMerge.isNeverOpenedNewInstall(now - hour - 1, lastUsed = 0L, now = now))
        assertFalse(RecentAppsMerge.isNeverOpenedNewInstall(now - 60_000L, lastUsed = now - 1, now = now))
        assertEquals(hour, 60L * 60 * 1000)
    }
}
