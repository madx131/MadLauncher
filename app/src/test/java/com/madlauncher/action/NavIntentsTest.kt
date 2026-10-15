package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavIntentsTest {
    @Test
    fun searchDoesNotStartATrip() {
        val waze = NavIntents.searchUris("com.waze", "Tel Aviv")
        assertEquals("geo:0,0?q=Tel%20Aviv", waze.first())
        assertTrue(waze.any { it.contains("maps/search") })
        assertTrue(waze.contains("https://waze.com/ul?q=Tel%20Aviv"))
        assertFalse(waze.any { it.contains("navigate=yes") })
        assertFalse(waze.any { it.startsWith("google.navigation") })
    }

    @Test
    fun navigateStartsTurnByTurn() {
        val waze = NavIntents.navigateUris("com.waze", "Tel Aviv")
        assertEquals("google.navigation:q=Tel%20Aviv", waze.first())
        assertTrue(waze.contains("https://waze.com/ul?q=Tel%20Aviv&navigate=yes"))
        assertFalse(waze.any { it.contains("google.com/maps") })

        val maps = NavIntents.navigateUris("com.google.android.apps.maps", "Paris")
        assertTrue(maps.any { it.contains("google.com/maps/dir") })
        assertTrue(maps.contains("geo:0,0?q=Paris"))
    }

    @Test
    fun blankDestinationYieldsNothing() {
        assertTrue(NavIntents.searchUris("com.waze", "   ").isEmpty())
        assertTrue(NavIntents.navigateUris("com.waze", "   ").isEmpty())
    }
}
