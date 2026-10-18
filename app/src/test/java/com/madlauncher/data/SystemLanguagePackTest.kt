package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemLanguagePackTest {
    @Test
    fun englishIsBuiltIn() {
        assertNull(SystemLanguagePack.fileNameForLanguage("en"))
        assertNull(SystemLanguagePack.fileNameForLanguage("EN"))
    }

    @Test
    fun hebrewMapsToHeb() {
        assertEquals("heb.json", SystemLanguagePack.fileNameForLanguage("he"))
        assertEquals("heb.json", SystemLanguagePack.fileNameForLanguage("iw"))
    }

    @Test
    fun onlyHebrewAndRussianShip() {
        assertEquals("rus.json", SystemLanguagePack.fileNameForLanguage("ru"))
        assertNull(SystemLanguagePack.fileNameForLanguage("in"))
        assertNull(SystemLanguagePack.fileNameForLanguage("nb"))
        assertNull(SystemLanguagePack.fileNameForLanguage("fr"))
    }
}
