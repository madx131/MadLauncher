package com.madlauncher.ui

import com.madlauncher.data.DeviceLoad
import com.madlauncher.data.GlanceElements
import com.madlauncher.data.GlanceStatStyle
import com.madlauncher.data.GlanceWeatherStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class GlanceStyleTextTest {
    @Test
    fun weatherSplitsConditionAndTemp() {
        assertEquals("Clear" to "22°C", splitWeatherLabel("Clear · 22°C"))
        assertEquals("22°C", glanceWeatherText(GlanceWeatherStyle.TEMP, "Clear · 22°C"))
        assertEquals("Clear", glanceWeatherText(GlanceWeatherStyle.CONDITION, "Clear · 22°C"))
        assertEquals("Clear · 22°C", glanceWeatherText(GlanceWeatherStyle.TEXT, "Clear · 22°C"))
    }

    @Test
    fun statsFormatLabelAndPercent() {
        val load = DeviceLoad.Snapshot(battery = 81, cpu = 12, ram = 44)
        assertEquals("Battery 81%", glanceStatText(GlanceStatStyle.LABEL, GlanceElements.BATTERY, load))
        assertEquals("81%", glanceStatText(GlanceStatStyle.VALUE, GlanceElements.BATTERY, load))
        assertEquals("CPU 12%", glanceStatText(GlanceStatStyle.BAR, GlanceElements.CPU, load))
        assertEquals("RAM", glanceStatText(GlanceStatStyle.VALUE, GlanceElements.RAM, DeviceLoad.Snapshot()))
    }
}
