package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InstantAnswerClientTest {
    @Before
    fun defaults() {
        InstantModules.resetToDefaults()
    }

    @Test
    fun skipsCommandsAndShortQueries() {
        assertFalse(InstantAnswerClient.shouldLookup("ab"))
        assertFalse(InstantAnswerClient.shouldLookup("@alice"))
        assertFalse(InstantAnswerClient.shouldLookup("msg bob"))
        assertFalse(InstantAnswerClient.shouldLookup("/t hello"))
        assertTrue(InstantAnswerClient.shouldLookup("2+2"))
        assertTrue(InstantAnswerClient.shouldLookup("capital of france"))
    }

    @Test
    fun parsePrefersAnswerThenAbstract() {
        val json = """{"Answer":"Paris","Heading":"France","AbstractText":"A country","AbstractSource":"Wikipedia","AbstractURL":"https://en.wikipedia.org/wiki/France"}"""
        val hit = InstantAnswerClient.parse(json)!!
        assertEquals("Paris", hit.text)
        assertEquals("France", hit.heading)
        assertEquals("Wikipedia", hit.source)
        assertEquals("https://en.wikipedia.org/wiki/France", hit.url)
        assertTrue(hit.fromDuckDuckGo)
    }

    @Test
    fun parseEmptyReturnsNull() {
        assertNull(InstantAnswerClient.parse("""{"Answer":"","AbstractText":"","Heading":""}"""))
    }

    @Test
    fun emptyCalcAnswerTypeIsNotAFact() {
        val json = """{"Answer":"","AnswerType":"calc","AbstractText":"A car-body style","Heading":"2+2","AbstractSource":"Wikipedia","AbstractURL":"https://en.wikipedia.org/wiki/2%2B2"}"""
        assertNull(InstantAnswerClient.parse(json))
    }

    @Test
    fun emptyConversionsAnswerTypeIsNotAFact() {
        val json = """{"Answer":"","AnswerType":"conversions","AbstractText":"Temperature","Heading":"72 F"}"""
        assertNull(InstantAnswerClient.parse(json))
    }

    @Test
    fun populatedCalcAnswerIsDuckDuckGo() {
        val json = """{"Answer":"4","AnswerType":"calc","Heading":"2+2","AbstractText":""}"""
        val hit = InstantAnswerClient.parse(json)!!
        assertEquals("4", hit.text)
        assertEquals(InstantAnswerClient.DDG_ATTRIBUTION, hit.source)
        assertTrue(hit.fromDuckDuckGo)
    }

    @Test
    fun resolveFallsBackToLocalCalcWhenDdgEmpty() {
        val json = """{"Answer":"","AnswerType":"calc","AbstractText":"A car-body style","Heading":"2+2"}"""
        val hit = InstantAnswerClient.resolve("2+2", json)!!
        assertEquals("4", hit.text)
        assertEquals(InstantModules.LOCAL_SOURCE, hit.source)
        assertFalse(hit.fromDuckDuckGo)
    }

    @Test
    fun resolveFallsBackToLocalConversion() {
        val json = """{"Answer":"","AnswerType":"conversions","AbstractText":"","Heading":""}"""
        val hit = InstantAnswerClient.resolve("72 f to c", json)!!
        assertTrue(hit.text.contains("°C"))
        assertEquals(CalcKind.CONVERSION, CalcParser.tryParse("72 f to c")!!.kind)
        assertFalse(hit.fromDuckDuckGo)
    }
}

class InstantModulesTest {
    @Before
    fun defaults() {
        InstantModules.resetToDefaults()
    }

    @Test
    fun localTwoPlusTwo() {
        val hit = InstantModules.localAnswer("2+2")!!
        assertEquals("4", hit.text)
        assertEquals(InstantModules.LOCAL_SOURCE, hit.source)
    }

    @Test
    fun localFahrenheitConversion() {
        val parsed = CalcParser.tryParse("72 f to c")!!
        assertEquals(CalcKind.CONVERSION, parsed.kind)
        assertTrue(parsed.answer.contains("°C"))
        val hit = InstantModules.localAnswer("72 f to c")!!
        assertTrue(hit.text.contains("°C"))
    }

    @Test
    fun packCanDisableCalc() {
        InstantModules.loadFromManifest(
            """{"modules":[{"id":"calc","file":"calc.json"}]}""",
        ) {
            """{"id":"calc","name":"Calculator","enabled":false,"offline":true,"answerType":"calc"}"""
        }
        assertFalse(InstantModules.isEnabled(InstantModules.CALC))
        assertNull(InstantModules.localAnswer("2+2"))
        assertTrue(CommandDetector.detect("2+2") is ActionCommand.None)
        InstantModules.resetToDefaults()
    }

    @Test
    fun translateStaysSlashCommand() {
        val cmd = CommandDetector.detect("/t hello")
        assertTrue(cmd is ActionCommand.Translate)
        assertFalse(InstantAnswerClient.shouldLookup("/t hello"))
        assertTrue(InstantModules.isEnabled(InstantModules.TRANSLATE))
    }

    @Test
    fun skipsWebInstantAnswersWhenDisabled() {
        assertFalse(InstantAnswerClient.shouldFetchWeb("capital of france", enabled = false))
        assertTrue(InstantAnswerClient.shouldFetchWeb("capital of france", enabled = true))
        assertFalse(InstantAnswerClient.shouldFetchWeb("ab", enabled = true))
        assertFalse(InstantAnswerClient.shouldFetchWeb("capital of france", enabled = true, duckDuckGo = false))
        assertNull(InstantModules.localAnswer("capital of france"))
        val local = InstantModules.localAnswer("2+2")
        assertEquals("4", local!!.text)
        assertFalse(local.fromDuckDuckGo)
    }
}

class AppFuzzyTest {
    @Test
    fun ranksTypoNearContainsMatch() {
        val apps = listOf("WhatsApp", "Settings", "Chrome", "Camera")
        val ranked = AppFuzzy.rank("whtsapp", apps, limit = 4, cutoff = 50) { it }
        assertTrue(ranked.contains("WhatsApp"))
        assertEquals("WhatsApp", ranked.first())
    }
}
