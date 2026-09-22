package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDropHitTest {
    @Test
    fun prefersCurrentPageWhenRectsOverlap() {
        val hits = listOf("0:0,0" to 100f, "1:0,0" to 100f)
        assertEquals("1:0,0", HomeDropHit.choose(hits, preferredPage = 1))
        assertEquals("0:0,0", HomeDropHit.choose(hits, preferredPage = 0))
    }

    @Test
    fun canHitNeighborPageWhenCurrentMisses() {
        val hits = listOf("1:0,0" to 6400f)
        assertEquals("1:0,0", HomeDropHit.choose(hits, preferredPage = 0))
    }

    @Test
    fun emptyHitsYieldNull() {
        assertNull(HomeDropHit.choose(emptyList(), preferredPage = 0))
    }

    @Test
    fun containsMatchesAndroidRectF() {
        assertTrue(HomeDropHit.contains(0f, 0f, 100f, 100f, 50f, 50f))
        assertTrue(HomeDropHit.contains(0f, 0f, 100f, 100f, 0f, 0f))
        assertTrue(!HomeDropHit.contains(0f, 0f, 100f, 100f, 100f, 50f))
        assertTrue(!HomeDropHit.contains(0f, 0f, 100f, 100f, 50f, 100f))
        assertTrue(!HomeDropHit.contains(0f, 0f, 100f, 100f, -1f, 50f))
    }
}
