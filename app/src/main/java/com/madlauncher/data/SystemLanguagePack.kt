package com.madlauncher.data

import java.util.Locale

/** Maps a device locale to a Lang pack overlay filename, or null for built-in English. */
object SystemLanguagePack {
    fun fileNameFor(locale: Locale): String? = fileNameForLanguage(locale.language)

    fun fileNameForLanguage(language: String): String? = LanguageCodes.overlayFileName(language)
}
