package com.madlauncher.action

import org.junit.Assert.assertTrue
import org.junit.Test

class AbsShortcutSuggestTest {
    @Test
    fun spotifyGetsTwoAndThreeLetters() {
        val hits = AbsShortcutSuggest.phrases("Spotify", "com.spotify.music")
        assertTrue(hits.contains("sp"))
        assertTrue(hits.contains("spo"))
    }

    @Test
    fun multiWordUsesInitials() {
        val hits = AbsShortcutSuggest.phrases("YouTube Music", "com.google.android.apps.youtube.music")
        assertTrue(hits.contains("ym"))
        assertTrue(hits.contains("yt"))
        assertTrue(hits.contains("ytm") || hits.contains("you"))
    }

    @Test
    fun skipsOneLetterNamesWithoutPaddingFromPackage() {
        val hits = AbsShortcutSuggest.phrases("X", "com.twitter.android")
        assertTrue(hits.all { it.length in 2..3 })
        assertTrue(hits.isNotEmpty())
    }
}
