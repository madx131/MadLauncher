package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcParserTest {
    @Test
    fun mathShowsAnswer() {
        val result = CalcParser.tryParse("2+2")!!
        assertEquals("4", result.answer)
        assertEquals(CalcKind.MATH, result.kind)
    }

    @Test
    fun equalsContinuesWithAnswer() {
        assertEquals("4", CalcParser.continueFromEquals("2+2="))
        assertEquals("4", CalcParser.continueFromEquals(" 2+2 = "))
        assertNull(CalcParser.continueFromEquals("2+2"))
        assertNull(CalcParser.continueFromEquals("="))
    }

    @Test
    fun conversionEqualsUsesNumericAnswer() {
        val next = CalcParser.continueFromEquals("32f=")
        assertEquals("0", next)
    }
}
