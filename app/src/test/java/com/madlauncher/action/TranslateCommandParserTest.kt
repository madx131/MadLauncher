package com.madlauncher.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateCommandParserTest {
    @Test
    fun parse_fromAndToWithText() {
        val parsed = TranslateCommandParser.parse("from english to spanish hello world")
        assertEquals("en", parsed.fromCode)
        assertEquals("es", parsed.toCode)
        assertEquals("hello world", parsed.text)
        assertNull(parsed.activeSlot)
    }

    @Test
    fun parse_textOnly() {
        val parsed = TranslateCommandParser.parse("bonjour")
        assertNull(parsed.fromCode)
        assertNull(parsed.toCode)
        assertEquals("bonjour", parsed.text)
        assertEquals(TranslateLangSlot.To, parsed.activeSlot)
        assertEquals("", parsed.partialLang)
    }

    @Test
    fun parse_fromAndText_promptsForTo() {
        val parsed = TranslateCommandParser.parse("from english hello")
        assertEquals("en", parsed.fromCode)
        assertNull(parsed.toCode)
        assertEquals("hello", parsed.text)
        assertEquals(TranslateLangSlot.To, parsed.activeSlot)
    }

    @Test
    fun applyLanguage_insertsToBeforeText() {
        val spanish = TranslatorLanguages.byCode("es")!!
        assertEquals(
            "/t to spanish hello ",
            TranslateCommandParser.applyLanguage("/t hello", spanish),
        )
    }

    @Test
    fun applyLanguage_keepsFromAndText() {
        val french = TranslatorLanguages.byCode("fr")!!
        assertEquals(
            "/t from english to french hello ",
            TranslateCommandParser.applyLanguage("/t from english hello", french),
        )
    }

    @Test
    fun parse_bareTo_isPromptNotText() {
        val parsed = TranslateCommandParser.parse("to")
        assertNull(parsed.toCode)
        assertEquals("", parsed.text)
        assertEquals(TranslateLangSlot.To, parsed.activeSlot)
    }

    @Test
    fun applyLanguage_existingToSlot() {
        val spanish = TranslatorLanguages.byCode("es")!!
        assertEquals(
            "/t to spanish ",
            TranslateCommandParser.applyLanguage("/t to", spanish),
        )
        assertEquals(
            "/t to spanish ",
            TranslateCommandParser.applyLanguage("/t to ", spanish),
        )
        assertEquals(
            "/t to spanish ",
            TranslateCommandParser.applyLanguage("/t to sp", spanish),
        )
    }

    @Test
    fun parse_toOnly() {
        val parsed = TranslateCommandParser.parse("to french merci")
        assertNull(parsed.fromCode)
        assertEquals("fr", parsed.toCode)
        assertEquals("merci", parsed.text)
        assertNull(parsed.activeSlot)
    }

    @Test
    fun autocomplete_completesPartialLanguage() {
        val complete = TranslateCommandParser.autocomplete("translate from eng")
        assertTrue(complete!!.lowercase().contains("english"))
    }

    @Test
    fun detectSlashT() {
        val cmd = CommandDetector.detect("/t from en to he shalom")
        assertTrue(cmd is ActionCommand.Translate)
        val parsed = TranslateCommandParser.parse((cmd as ActionCommand.Translate).tail)
        assertEquals("en", parsed.fromCode)
        assertEquals("he", parsed.toCode)
        assertEquals("shalom", parsed.text)
    }
}
