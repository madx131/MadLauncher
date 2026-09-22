package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherEntryDeduperTest {
    @Test
    fun copilotIconAliasIsCollapsedToDefault() {
        val entries = listOf(
            LauncherEntryDeduper.Entry("Copilot", "com.microsoft.copilot", "com.microsoft.copilotn.MainActivityAlt"),
            LauncherEntryDeduper.Entry("Copilot", "com.microsoft.copilot", "com.microsoft.copilotn.MainActivityDefault"),
            LauncherEntryDeduper.Entry("Maps", "com.google.android.apps.maps", "com.google.android.maps.MapsActivity"),
        )
        val out = LauncherEntryDeduper.collapse(entries) { pkg ->
            if (pkg == "com.microsoft.copilot") "com.microsoft.copilotn.MainActivityDefault" else null
        }
        assertEquals(
            listOf("com.microsoft.copilotn.MainActivityDefault", "com.google.android.maps.MapsActivity"),
            out.map { it.activityName },
        )
    }

    @Test
    fun chromeAndIncognitoStaySeparate() {
        val entries = listOf(
            LauncherEntryDeduper.Entry("Chrome", "com.android.chrome", "com.google.android.apps.chrome.Main"),
            LauncherEntryDeduper.Entry("Chrome Incognito", "com.android.chrome", "com.google.android.apps.chrome.Incognito"),
        )
        val out = LauncherEntryDeduper.collapse(entries)
        assertEquals(2, out.size)
    }

    @Test
    fun bingRelabeledCopilotIsHiddenWhenRealCopilotExists() {
        val entries = listOf(
            LauncherEntryDeduper.Entry("Copilot", "com.microsoft.copilot", "com.microsoft.copilotn.MainActivityDefault"),
            LauncherEntryDeduper.Entry("Copilot", "com.microsoft.bing", "com.microsoft.sapphire.app.main.SapphireMainActivity"),
        )
        val out = LauncherEntryDeduper.collapse(entries)
        assertEquals(listOf("com.microsoft.copilot"), out.map { it.packageName })
    }

    @Test
    fun bingKeepsItsOwnIconWhenLabeledBing() {
        val entries = listOf(
            LauncherEntryDeduper.Entry("Copilot", "com.microsoft.copilot", "com.microsoft.copilotn.MainActivityDefault"),
            LauncherEntryDeduper.Entry("Bing", "com.microsoft.bing", "com.microsoft.sapphire.app.main.SapphireMainActivity"),
        )
        val out = LauncherEntryDeduper.collapse(entries)
        assertEquals(2, out.size)
    }

    @Test
    fun officeHubLabeledCopilotIsHiddenWhenRealCopilotExists() {
        val entries = listOf(
            LauncherEntryDeduper.Entry("Copilot", "com.microsoft.copilot", "com.microsoft.copilotn.MainActivityDefault"),
            LauncherEntryDeduper.Entry("Copilot", "com.microsoft.office.officehubrow", "com.microsoft.office.officesuite.OfficeSuiteActivity"),
        )
        val out = LauncherEntryDeduper.collapse(entries)
        assertEquals(listOf("com.microsoft.copilot"), out.map { it.packageName })
    }

    @Test
    fun microsoftCopilotLabelStillCollapsesAgainstCopilot() {
        val entries = listOf(
            LauncherEntryDeduper.Entry("Microsoft Copilot", "com.microsoft.copilot", "com.microsoft.copilotn.MainActivityDefault"),
            LauncherEntryDeduper.Entry("Copilot", "com.microsoft.office.officehubrow", "com.microsoft.office.officesuite.OfficeSuiteActivity"),
        )
        val out = LauncherEntryDeduper.collapse(entries)
        assertEquals(listOf("com.microsoft.copilot"), out.map { it.packageName })
    }
}
