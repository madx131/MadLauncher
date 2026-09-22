package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageCatalogReadmeTest {
    @Test
    fun parseReadmeMigratesTwoLetterCodesAndSkipsEnglish() {
        val text = """
            Language codes (filename.json):
              eng English (master)
              heb Hebrew
              rus Russian
              iw Hebrew legacy
        """.trimIndent()
        val parsed = LanguageCatalog.parseReadme(text)
        assertTrue(parsed.none { LanguageCodes.isEnglishPack(it.fileName) })
        assertEquals(setOf("heb.json", "rus.json"), parsed.map { it.fileName }.toSet())
        assertEquals("Hebrew", parsed.first { it.fileName == "heb.json" }.label)
    }
}
