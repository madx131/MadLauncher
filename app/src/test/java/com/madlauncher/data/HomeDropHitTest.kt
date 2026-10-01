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
    fun ignoresNeighborPageWhenCurrentMisses() {
        val hits = listOf("1:0,0" to 6400f)
        assertNull(HomeDropHit.choose(hits, preferredPage = 0))
    }

    @Test
    fun emptyHitsYieldNull() {
        assertNull(HomeDropHit.choose(emptyList(), preferredPage = 0))
    }

    @Test
    fun parseKeyReadsPageColRow() {
        assertEquals(Triple(0, 1, 5), HomeDropHit.parseKey("0:1,5"))
        assertEquals(Triple(2, 4, 0), HomeDropHit.parseKey("2:4,0"))
        assertNull(HomeDropHit.parseKey("dock:0"))
        assertNull(HomeDropHit.parseKey("0:1"))
    }

    @Test
    fun choosePrefersSmallestLiveCell() {
        val hits = listOf("0:1,5" to 1600f, "0:1,4" to 6400f)
        val live = hits.filter { (key, _) ->
            val parsed = HomeDropHit.parseKey(key)
            parsed != null && parsed.second in 0 until 5 && parsed.third in 0 until 5
        }
        assertEquals("0:1,4", HomeDropHit.choose(live, preferredPage = 0))
    }

    @Test
    fun centeredInUsesInnerHalf() {
        assertTrue(HomeDropHit.centeredIn(0f, 0f, 100f, 100f, 50f, 50f))
        assertTrue(!HomeDropHit.centeredIn(0f, 0f, 100f, 100f, 5f, 50f))
        assertTrue(HomeDropHit.contains(0f, 0f, 100f, 100f, 5f, 50f))
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
