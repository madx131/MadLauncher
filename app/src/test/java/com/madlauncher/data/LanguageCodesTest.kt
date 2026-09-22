package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageCodesTest {
    @Test
    fun mapsExamplesFromSpec() {
        assertEquals("eng", LanguageCodes.toIso6392t("en"))
        assertEquals("heb", LanguageCodes.toIso6392t("iw"))
        assertEquals("heb", LanguageCodes.toIso6392t("he"))
        assertEquals("rus", LanguageCodes.toIso6392t("ru"))
        assertEquals("ara", LanguageCodes.toIso6392t("ar"))
        assertEquals("deu", LanguageCodes.toIso6392t("de"))
        assertEquals("fra", LanguageCodes.toIso6392t("fr"))
        assertEquals("spa", LanguageCodes.toIso6392t("es"))
        assertEquals("zho", LanguageCodes.toIso6392t("zh"))
        assertEquals("jpn", LanguageCodes.toIso6392t("ja"))
        assertEquals("ind", LanguageCodes.toIso6392t("id"))
        assertEquals("fil", LanguageCodes.toIso6392t("fil"))
    }

    @Test
    fun migrateLegacyFilenames() {
        assertEquals("heb.json", LanguageCodes.migrateFileName("iw.json"))
        assertEquals("heb.json", LanguageCodes.migrateFileName("he.json"))
        assertEquals("rus.json", LanguageCodes.migrateFileName("ru.json"))
        assertEquals("heb.json", LanguageCodes.migrateFileName("heb.json"))
        assertEquals("", LanguageCodes.migrateFileName("en.json"))
        assertEquals("", LanguageCodes.migrateFileName("eng.json"))
        assertEquals("", LanguageCodes.migrateFileName(""))
    }

    @Test
    fun englishPacksAreSkipped() {
        assertTrue(LanguageCodes.isEnglishPack("en.json"))
        assertTrue(LanguageCodes.isEnglishPack("eng.json"))
        assertFalse(LanguageCodes.isEnglishPack("heb.json"))
        assertNull(LanguageCodes.overlayFileName("en"))
        assertEquals("heb.json", LanguageCodes.overlayFileName("iw"))
    }

    @Test
    fun everyLegacyTwoLetterCodeMapsToThreeLetters() {
        val codes = listOf(
            "af", "am", "ar", "bg", "bn", "ca", "cs", "da", "de", "el", "en", "es", "et", "eu",
            "fa", "fi", "fil", "fr", "gl", "gu", "ha", "hi", "hr", "hu", "id", "ig", "it", "iw",
            "ja", "km", "kn", "ko", "lo", "lt", "lv", "ml", "mr", "ms", "my", "ne", "nl", "no",
            "pa", "pl", "pt", "ro", "ru", "si", "sk", "sl", "sr", "sv", "sw", "ta", "te", "th",
            "tr", "uk", "ur", "vi", "yo", "zh", "zu",
        )
        codes.forEach { code ->
            val mapped = LanguageCodes.toIso6392t(code)
            assertTrue("$code -> $mapped", mapped.length == 3)
        }
    }
}
