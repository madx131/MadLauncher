package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialBrowserAppsTest {
    @Test
    fun packageMapsToOneApp() {
        assertEquals("x", SocialBrowserApps.idFor("com.x.android"))
        assertEquals("x", SocialBrowserApps.idFor("com.twitter.android"))
        assertEquals("threads", SocialBrowserApps.idFor("com.instagram.barcelona"))
        assertEquals("instagram", SocialBrowserApps.idFor("com.instagram.android"))
        assertEquals("facebook", SocialBrowserApps.idFor("com.facebook.lite"))
        assertEquals("vk", SocialBrowserApps.idFor("com.vkontakte.android"))
        assertEquals("whatsapp", SocialBrowserApps.idFor("com.whatsapp"))
        assertEquals("telegram", SocialBrowserApps.idFor("org.telegram.messenger"))
        assertEquals("messenger", SocialBrowserApps.idFor("com.facebook.orca"))
        assertNull(SocialBrowserApps.idFor("com.example.notes"))
    }

    @Test
    fun xMessagesOpenInBrowserByDefault() {
        val mode = SocialBrowserApps.mode("x", emptyMap(), emptySet(), emptySet())
        assertEquals(SocialBrowserApps.BrowserMode.MSG, mode)
        assertTrue(
            SocialBrowserApps.allowsCommand(
                "com.x.android",
                SocialBrowserApps.BrowserCommand.MSG,
                emptyMap(),
                emptySet(),
                emptySet(),
            ),
        )
        assertFalse(
            SocialBrowserApps.allowsCommand(
                "com.x.android",
                SocialBrowserApps.BrowserCommand.POST,
                emptyMap(),
                emptySet(),
                emptySet(),
            ),
        )
        assertEquals("https://x.com/messages", SocialBrowserApps.webUrl("com.x.android"))
        assertEquals("youtube", SocialBrowserApps.idFor("com.google.android.youtube"))
        assertEquals(
            SocialBrowserApps.BrowserMode.OFF,
            SocialBrowserApps.mode("netflix", emptyMap(), emptySet(), emptySet()),
        )
        assertEquals(
            listOf(SocialBrowserApps.BrowserMode.OFF, SocialBrowserApps.BrowserMode.ON),
            SocialBrowserApps.options(SocialBrowserApps.supportedApps.first { it.id == "netflix" }),
        )
    }

    @Test
    fun downloadWallsAreNotInTheBrowserList() {
        assertNull(SocialBrowserApps.all.firstOrNull { it.id == "facebook" })
        assertNull(SocialBrowserApps.all.firstOrNull { it.id == "messenger" })
        assertNull(SocialBrowserApps.all.firstOrNull { it.id == "snapchat" })
        assertNull(SocialBrowserApps.webUrl("com.facebook.katana"))
        assertNull(SocialBrowserApps.webUrl("com.facebook.orca"))
        assertFalse(
            SocialBrowserApps.allowsCommand(
                "com.facebook.katana",
                SocialBrowserApps.BrowserCommand.MSG,
                emptyMap(),
                setOf("facebook"),
                emptySet(),
            ),
        )
        val x = SocialBrowserApps.socialApps.first { it.id == "x" }
        assertTrue(SocialBrowserApps.options(x).contains(SocialBrowserApps.BrowserMode.MSG))
        assertTrue(SocialBrowserApps.options(x).contains(SocialBrowserApps.BrowserMode.POST))
    }

    @Test
    fun messagingThatFailedStartsOn() {
        assertEquals(
            SocialBrowserApps.BrowserMode.ON,
            SocialBrowserApps.mode("slack", emptyMap(), emptySet(), emptySet()),
        )
        assertEquals(
            SocialBrowserApps.BrowserMode.OFF,
            SocialBrowserApps.mode("whatsapp", emptyMap(), emptySet(), emptySet()),
        )
        assertEquals(
            SocialBrowserApps.BrowserMode.OFF,
            SocialBrowserApps.mode("messenger", emptyMap(), emptySet(), emptySet()),
        )
        assertFalse(
            SocialBrowserApps.allowsCommand(
                "com.facebook.orca",
                SocialBrowserApps.BrowserCommand.MSG,
                emptyMap(),
                setOf("messenger"),
                emptySet(),
            ),
        )
    }

    @Test
    fun addedAppUsesItsOwnSwitch() {
        val added = SocialBrowserApps.Custom("com.example.chat", "Example", "https://example.com/chat")
        assertTrue(
            SocialBrowserApps.allowsCommand(
                "com.example.chat",
                SocialBrowserApps.BrowserCommand.MSG,
                emptyMap(),
                emptySet(),
                emptySet(),
                listOf(added),
            ),
        )
        assertEquals("https://example.com/chat", SocialBrowserApps.webUrl("com.example.chat", listOf(added)))
        assertFalse(
            SocialBrowserApps.allowsCommand(
                "com.example.chat",
                SocialBrowserApps.BrowserCommand.MSG,
                emptyMap(),
                emptySet(),
                emptySet(),
                listOf(added.copy(mode = SocialBrowserApps.BrowserMode.OFF)),
            ),
        )
    }
}
