package com.madlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LandscapeKeepTest {
    @Test
    fun snapUprightBuckets() {
        assertEquals(0f, snapUprightDegrees(0), 0.01f)
        assertEquals(0f, snapUprightDegrees(10), 0.01f)
        assertEquals(-90f, snapUprightDegrees(90), 0.01f)
        assertEquals(180f, snapUprightDegrees(180), 0.01f)
        assertEquals(90f, snapUprightDegrees(270), 0.01f)
        assertEquals(0f, snapUprightDegrees(350), 0.01f)
    }

    @Test
    fun stripAppWidgetsDoNotRotate() {
        assertEquals(false, appWidgetRotatesInPlace(4f, 1f))
        assertEquals(false, appWidgetRotatesInPlace(5f, 1f))
        assertEquals(false, appWidgetRotatesInPlace(1f, 4f))
        assertEquals(false, appWidgetRotatesInPlace(4f, 2f))
    }

    @Test
    fun squareAppWidgetsRotate() {
        assertEquals(true, appWidgetRotatesInPlace(1f, 1f))
        assertEquals(true, appWidgetRotatesInPlace(2f, 2f))
        assertEquals(true, appWidgetRotatesInPlace(4f, 4f))
        assertEquals(true, appWidgetRotatesInPlace(3f, 3f))
    }

    @Test
    fun glanceFillSwapsOnSidewaysHold() {
        assertEquals(false, uprightFillSwapsAxes(0f))
        assertEquals(true, uprightFillSwapsAxes(90f))
        assertEquals(true, uprightFillSwapsAxes(-90f))
        assertEquals(false, uprightFillSwapsAxes(180f))
    }
}
