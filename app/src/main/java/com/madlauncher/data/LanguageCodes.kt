package com.madlauncher.data

import java.util.Locale

/**
 * ISO 639-2/T (terminological) codes for MAD language overlay filenames.
 * English is bundled in strings.xml — [isEnglishPack] / blank overlay, not a download.
 */
object LanguageCodes {
    /** ISO 639-1 (and legacy Android aliases) → ISO 639-2/T. `fil` stays `fil`. */
    private val iso1ToT: Map<String, String> = mapOf(
        "af" to "afr",
        "am" to "amh",
        "ar" to "ara",
        "bg" to "bul",
        "bn" to "ben",
        "ca" to "cat",
        "cs" to "ces",
        "da" to "dan",
        "de" to "deu",
        "el" to "ell",
        "en" to "eng",
        "es" to "spa",
        "et" to "est",
        "eu" to "eus",
        "fa" to "fas",
        "fi" to "fin",
        "fil" to "fil",
        "fr" to "fra",
        "gl" to "glg",
        "gu" to "guj",
        "ha" to "hau",
        "he" to "heb",
        "hi" to "hin",
        "hr" to "hrv",
        "hu" to "hun",
        "id" to "ind",
        "ig" to "ibo",
        "in" to "ind",
        "it" to "ita",
        "iw" to "heb",
        "ja" to "jpn",
        "km" to "khm",
        "kn" to "kan",
        "ko" to "kor",
        "lo" to "lao",
        "lt" to "lit",
        "lv" to "lav",
        "ml" to "mal",
        "mr" to "mar",
        "ms" to "msa",
        "my" to "mya",
        "nb" to "nor",
        "ne" to "nep",
        "nl" to "nld",
        "nn" to "nor",
        "no" to "nor",
        "pa" to "pan",
        "pl" to "pol",
        "pt" to "por",
        "ro" to "ron",
        "ru" to "rus",
        "si" to "sin",
        "sk" to "slk",
        "sl" to "slv",
        "sr" to "srp",
        "sv" to "swe",
        "sw" to "swa",
        "ta" to "tam",
        "te" to "tel",
        "th" to "tha",
        "tl" to "fil",
        "tr" to "tur",
        "uk" to "ukr",
        "ur" to "urd",
        "vi" to "vie",
        "yo" to "yor",
        "zh" to "zho",
        "zu" to "zul",
    )

    private val knownT: Set<String> = iso1ToT.values.toSet() + setOf("eng", "heb", "ind", "fil")

    fun toIso6392t(code: String): String {
        val raw = code.trim().lowercase(Locale.ROOT).removeSuffix(".json")
        if (raw.isEmpty()) return raw
        iso1ToT[raw]?.let { return it }
        if (raw in knownT || raw.length == 3) return raw
        return raw
    }

    fun isEnglishCode(code: String): Boolean {
        val t = toIso6392t(code)
        return t == "eng" || t == "en"
    }

    fun isEnglishPack(fileName: String): Boolean {
        val name = fileName.trim().lowercase(Locale.ROOT)
        return name == "en.json" || name == "eng.json"
    }

    /** Overlay filename for a locale language tag, or null for built-in English. */
    fun overlayFileName(language: String): String? {
        val lang = language.trim().lowercase(Locale.ROOT)
        if (lang.isEmpty() || isEnglishCode(lang)) return null
        return "${toIso6392t(lang)}.json"
    }

    /**
     * Map a stored overlay filename to the current 3-letter name.
     * English packs become blank (built-in strings.xml).
     */
    fun migrateFileName(fileName: String): String {
        val trimmed = fileName.trim()
        if (trimmed.isEmpty()) return ""
        if (isEnglishPack(trimmed)) return ""
        val mapped = overlayFileName(trimmed.removeSuffix(".json").removeSuffix(".JSON"))
        return mapped ?: ""
    }
}
