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
    fun indonesianAndNorwegian() {
        assertEquals("ind.json", SystemLanguagePack.fileNameForLanguage("in"))
        assertEquals("nor.json", SystemLanguagePack.fileNameForLanguage("nb"))
    }
}
