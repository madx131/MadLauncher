package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialPostBridgeTest {
    @Test
    fun twitterComposeUri_includesText() {
        val uri = "https://twitter.com/intent/tweet?text=Hello+MAD"
        assertTrue(uri.contains("text=Hello+MAD"))
        assertTrue(uri.contains("twitter.com/intent/tweet"))
    }

    @Test
    fun facebookAndInstagram_blockOutsideInput_butUseA11yComposer() {
        assertTrue(SocialPostBridge.metaBlocksOutsideInput("com.facebook.katana"))
        assertTrue(SocialPostBridge.metaBlocksOutsideInput("com.facebook.lite"))
        assertTrue(SocialPostBridge.metaBlocksOutsideInput("com.instagram.barcelona"))
        assertFalse(SocialPostBridge.metaBlocksOutsideInput("com.twitter.android"))
        assertTrue(SocialPostBridge.usesA11yCompose("com.facebook.katana"))
        assertTrue(SocialPostBridge.usesA11yCompose("com.instagram.android"))
        assertTrue(SocialPostBridge.facebookComposerUris().any { it.startsWith("fb://") })
        assertTrue(SocialPostBridge.instagramCreateUris().none { it.contains("camera") })
    }

    @Test
    fun parkedSocialPostsUseTheWebsiteAndWorkingAppsDoNot() {
        assertEquals(
            "https://m.facebook.com/composer?text=hello%20there",
            SocialPostBridge.webComposeUrl("com.facebook.katana", "hello there"),
        )
        assertEquals(
            "https://www.threads.net/intent/post?text=hello",
            SocialPostBridge.webComposeUrl("com.instagram.barcelona", "hello"),
        )
        assertEquals(
            "https://www.instagram.com/",
            SocialPostBridge.webComposeUrl("com.instagram.android", "caption"),
        )
        assertEquals(
            "https://www.tiktok.com/tiktokstudio/upload",
            SocialPostBridge.webComposeUrl("com.zhiliaoapp.musically", ""),
        )
        assertEquals(
            "https://bsky.app/intent/compose?text=hello",
            SocialPostBridge.webComposeUrl("xyz.blueskyweb.app", "hello"),
        )
        assertTrue(SocialPostBridge.parkedInApp("com.snapchat.android"))
        assertTrue(SocialPostBridge.parkedInApp("com.discord"))
        assertFalse(SocialPostBridge.parkedInApp("com.x.android"))
        assertTrue(SocialPostBridge.isX("com.x.android"))
        assertEquals(
            "https://x.com/intent/post?text=hello",
            SocialPostBridge.webComposeUrl("com.x.android", "hello"),
        )
        assertEquals(
            "https://www.linkedin.com/feed/?shareActive=true&text=hello",
            SocialPostBridge.webComposeUrl("com.linkedin.android", "hello"),
        )
        assertEquals(
            "https://www.reddit.com/submit?title=hello",
            SocialPostBridge.webComposeUrl("com.reddit.frontpage", "hello"),
        )
        assertTrue(SocialPostBridge.parkedInApp("com.sina.weibo"))
        assertTrue(SocialPostBridge.parkedInApp("com.vkontakte.android"))
        assertFalse(SocialPostBridge.parkedInApp("com.whatsapp"))
        assertTrue(SocialPostBridge.webUrlCarriesText("https://www.threads.net/intent/post?text=hi"))
        assertTrue(SocialPostBridge.webUrlCarriesText("https://www.reddit.com/submit?title=hi"))
        assertFalse(SocialPostBridge.webUrlCarriesText("https://www.instagram.com/"))
    }

    @Test
    fun truthSocialSpacePostUsesTheWebsite() {
        assertTrue(SocialPostBridge.isTruthSocial("com.truthsocial.android.app"))
        assertFalse(SocialPostBridge.isTruthSocial("com.twitter.android"))
        assertEquals("https://truthsocial.com/compose", SocialPostBridge.truthWebUrl(""))
        assertEquals(
            "https://truthsocial.com/share?text=hello%20there",
            SocialPostBridge.truthWebUrl("hello there"),
        )
    }

    @Test
    fun discordAndTikTok_useA11yCompose() {
        assertTrue(SocialPostBridge.usesA11yCompose("com.discord"))
        assertTrue(SocialPostBridge.usesA11yCompose("com.zhiliaoapp.musically"))
        assertFalse(SocialPostBridge.usesA11yCompose("com.twitter.android"))
    }
}
