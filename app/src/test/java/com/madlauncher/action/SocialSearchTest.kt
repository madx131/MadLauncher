package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialSearchTest {
    @Test
    fun httpsSearchLeadsSoWebBangsStayOnTheSite() {
        val x = SocialSearch.urls("com.twitter.android", "cats")
        assertTrue(x.first().startsWith("https://x.com/search?q="))
        assertTrue(x.any { it.startsWith("twitter://search") })

        val ig = SocialSearch.urls("com.instagram.android", "cats")
        assertTrue(ig.first().startsWith("https://www.instagram.com/"))
        assertTrue(ig.any { it.startsWith("instagram://search") })

        val fb = SocialSearch.urls("com.facebook.katana", "cats")
        assertTrue(fb.first().startsWith("https://www.facebook.com/search/"))
        assertEquals("https://www.threads.net/search?q=cats", SocialSearch.urls("com.instagram.barcelona", "cats").first())
        assertTrue(SocialSearch.urls("com.zhiliaoapp.musically", "cats").first().startsWith("https://www.tiktok.com/search"))
        assertEquals("https://truthsocial.com/search?q=cats", SocialSearch.urls("com.truthsocial.android.app", "cats").first())
        assertEquals("https://bsky.app/search?q=cats", SocialSearch.urls("xyz.blueskyweb.app", "cats").first())
    }

    @Test
    fun catalogSearchUrlIsTheHttpsLookup() {
        val ig = SearchableApps.catalog.first { it.appName == "Instagram" }
        assertTrue(ig.searchUrl("cats").startsWith("https://"))
        val fb = SearchableApps.catalog.first { it.appName == "Facebook" }
        assertTrue(fb.searchUrl("cats").startsWith("https://"))
        val tiktok = SearchableApps.catalog.first { it.appName == "TikTok" }
        assertTrue(tiktok.searchUrl("cats").startsWith("https://"))
        val x = SearchableApps.catalog.first { it.appName == "Twitter" }
        assertTrue(x.urlsFor("cats").any { it.startsWith("twitter://") })
    }

    @Test
    fun unknownPackagesHaveNoSearch() {
        assertFalse(SocialSearch.hasSearch("com.whatsapp"))
        assertTrue(SocialSearch.hasSearch("com.x.android"))
    }
}
