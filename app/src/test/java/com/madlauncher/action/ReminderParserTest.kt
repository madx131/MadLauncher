package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderParserTest {
    @Test
    fun inMinutesIsTimer() {
        val parsed = ReminderParser.parse("milk in 20 min")
        assertEquals(20 * 60, parsed.timerSeconds)
        assertEquals("milk", parsed.message)
        assertNull(parsed.hour)
    }

    @Test
    fun inHoursWithLeadingMeTo() {
        val parsed = ReminderParser.parse("me to take pills in 1 hour")
        assertEquals(3600, parsed.timerSeconds)
        assertEquals("take pills", parsed.message)
    }

    @Test
    fun trailingMinutesIsTimer() {
        val parsed = ReminderParser.parse("pills 10 minutes")
        assertEquals(10 * 60, parsed.timerSeconds)
        assertEquals("pills", parsed.message)
    }

    @Test
    fun atEightPmIsAlarm() {
        val parsed = ReminderParser.parse("pickup at 8pm")
        assertEquals(20, parsed.hour)
        assertEquals(0, parsed.minute)
        assertEquals("pickup", parsed.message)
        assertNull(parsed.timerSeconds)
    }

    @Test
    fun atSevenThirtyAm() {
        val parsed = ReminderParser.parse("meeting at 7:30 am")
        assertEquals(7, parsed.hour)
        assertEquals(30, parsed.minute)
        assertEquals("meeting", parsed.message)
    }

    @Test
    fun colonClockWithoutAt() {
        val parsed = ReminderParser.parse("pickup 8:00")
        assertEquals(8, parsed.hour)
        assertEquals(0, parsed.minute)
        assertEquals("pickup", parsed.message)
    }

    @Test
    fun messageOnly() {
        val parsed = ReminderParser.parse("me buy milk")
        assertEquals("buy milk", parsed.message)
        assertNull(parsed.timerSeconds)
        assertNull(parsed.hour)
    }
}
