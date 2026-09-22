package com.madlauncher.action

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
    fun discordAndTikTok_useA11yCompose() {
        assertTrue(SocialPostBridge.usesA11yCompose("com.discord"))
        assertTrue(SocialPostBridge.usesA11yCompose("com.zhiliaoapp.musically"))
        assertFalse(SocialPostBridge.usesA11yCompose("com.twitter.android"))
    }
}
