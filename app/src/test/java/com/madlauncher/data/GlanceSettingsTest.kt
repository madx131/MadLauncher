package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GlanceSettingsTest {
    @Test
    fun statStyleFollowsEachElement() {
        val settings = GlanceSettings(
            batteryStyle = GlanceStatStyle.RING,
            cpuStyle = GlanceStatStyle.BAR,
            ramStyle = GlanceStatStyle.VALUE,
        )
        assertEquals(GlanceStatStyle.RING, settings.statStyle(GlanceElements.BATTERY))
        assertEquals(GlanceStatStyle.BAR, settings.statStyle(GlanceElements.CPU))
        assertEquals(GlanceStatStyle.VALUE, settings.statStyle(GlanceElements.RAM))
        assertEquals(GlanceStatStyle.LABEL, GlanceSettings().statStyle(GlanceElements.BATTERY))
    }

    @Test
    fun newStylesDefaultWithoutBreakingClockAndDate() {
        val settings = GlanceSettings()
        assertEquals(GlanceClockStyle.CLASSIC, settings.clockStyle)
        assertEquals(GlanceDateStyle.LONG, settings.dateStyle)
        assertEquals(GlanceWeatherStyle.TEXT, settings.weatherStyle)
        assertEquals(GlanceUnreadStyle.ICONS, settings.unreadStyle)
        assertEquals(GlanceStatStyle.LABEL, settings.cpuStyle)
        assertEquals(GlanceStatStyle.LABEL, settings.ramStyle)
    }
}
