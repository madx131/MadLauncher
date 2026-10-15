package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialInboxTest {
    @Test
    fun webInboxesAreHttps() {
        assertEquals("https://x.com/messages", SocialInbox.webUrl("com.x.android"))
        assertEquals("https://x.com/messages", SocialInbox.webUrl("com.twitter.android"))
        assertEquals(
            "https://www.instagram.com/direct/inbox/",
            SocialInbox.webUrl("com.instagram.android"),
        )
        assertEquals(
            "https://truthsocial.com/conversations",
            SocialInbox.webUrl("com.truthsocial.android.app"),
        )
        assertTrue(SocialInbox.webUrl("com.facebook.katana")!!.startsWith("https://"))
        assertEquals("https://bsky.app/messages", SocialInbox.webUrl("xyz.blueskyweb.app"))
        assertEquals("https://discord.com/channels/@me", SocialInbox.webUrl("com.discord"))
        assertEquals("https://vk.com/im", SocialInbox.webUrl("com.vkontakte.android"))
        assertEquals("https://weibo.com/message", SocialInbox.webUrl("com.sina.weibo"))
    }

    @Test
    fun xAndInstagramHaveInbox() {
        assertTrue(SocialInbox.hasInbox("com.twitter.android"))
        assertTrue(SocialInbox.hasInbox("com.x.android"))
        assertTrue(SocialInbox.hasInbox("com.instagram.android"))
        assertTrue(SocialInbox.hasInbox("com.facebook.katana"))
        assertTrue(SocialInbox.hasInbox("com.linkedin.android"))
        assertTrue(SocialInbox.hasInbox("com.zhiliaoapp.musically"))
        assertTrue(SocialInbox.hasInbox("com.snapchat.android"))
    }

    @Test
    fun phoneMessengersAreNotHandleInbox() {
        assertFalse(SocialInbox.hasInbox("com.whatsapp"))
        assertFalse(SocialInbox.hasInbox("org.telegram.messenger"))
        assertFalse(SocialInbox.hasInbox("org.thoughtcrime.securesms"))
        assertFalse(SocialInbox.hasInbox("com.facebook.orca"))
        assertFalse(SocialInbox.hasInbox("com.google.android.apps.messaging"))
        assertTrue(SocialInbox.hasInbox("com.discord"))
    }

    @Test
    fun guessSocialIsPostAndChatIsChat() {
        assertEquals(SpaceAddon.POST, AppAddonGuess.forPackage("com.twitter.android", "X"))
        assertEquals(SpaceAddon.POST, AppAddonGuess.forPackage("com.instagram.android", "Instagram"))
        assertEquals(SpaceAddon.CHAT, AppAddonGuess.forPackage("com.whatsapp", "WhatsApp"))
        assertEquals(SpaceAddon.SEARCH, AppAddonGuess.forPackage("com.spotify.music", "Spotify"))
    }
}
