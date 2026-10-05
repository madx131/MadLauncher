package com.madlauncher.action

import com.madlauncher.data.LaunchableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteBangTest {
    @Test
    fun colonOpensSocial() {
        val cmd = CommandDetector.detect(":face")
        assertTrue(cmd is ActionCommand.PostPick)
        assertEquals("face", (cmd as ActionCommand.PostPick).filter)
    }

    @Test
    fun bangIsNotSocial() {
        assertTrue(CommandDetector.detect("!yt cats") is ActionCommand.None)
        assertTrue(CommandDetector.detect("!w paris") is ActionCommand.None)
    }

    @Test
    fun webBangWhenAppMissing() {
        val wiki = SearchableApps.parseWebBang("!w paris", emptyList())
        assertEquals("Wikipedia", wiki?.first?.displayName)
        assertEquals("paris", wiki?.second)
        val urban = SearchableApps.parseWebBang("!ud yeet", emptyList())
        assertEquals("Urban Dictionary", urban?.first?.displayName)
        assertEquals("yeet", urban?.second)
        assertNull(SearchableApps.parseWebBang("!yt", emptyList()))
    }

    @Test
    fun bangOpensWebsiteEvenIfAppInstalled() {
        val youtube = SearchableApps.catalog.first { it.appName == "YouTube" }
        val resolved = ResolvedSearchApp(
            def = youtube,
            app = LaunchableApp(
                label = "YouTube",
                packageName = "com.google.android.youtube",
                activityName = "Main",
                icon = null,
            ),
        )
        val web = SearchableApps.parseWebBang("!yt cats", listOf(resolved))
        assertEquals("YouTube", web?.first?.displayName)
        assertEquals("cats", web?.second)
    }
}
