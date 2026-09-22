package com.madlauncher.action

/**
 * Common translate targets with ISO-style codes used by Google Translate / Bing.
 * [aliases] are matched longest-first for parsing and autocomplete.
 */
data class TranslatorLanguage(
    val code: String,
    val displayName: String,
    val aliases: List<String>,
) {
    val primaryAlias: String get() = aliases.first()
}

object TranslatorLanguages {
    val all: List<TranslatorLanguage> = listOf(
        lang("auto", "Auto-detect", "auto", "detect", "automatic"),
        lang("en", "English", "english", "en", "eng"),
        lang("es", "Spanish", "spanish", "es", "esp", "spa"),
        lang("fr", "French", "french", "fr", "fra", "fre"),
        lang("de", "German", "german", "de", "deu", "ger"),
        lang("it", "Italian", "italian", "it", "ita"),
        lang("pt", "Portuguese", "portuguese", "pt", "por"),
        lang("pt-BR", "Portuguese (Brazil)", "portuguese brazil", "brazilian portuguese", "pt-br", "ptbr"),
        lang("ru", "Russian", "russian", "ru", "rus"),
        lang("zh-CN", "Chinese (Simplified)", "chinese simplified", "chinese", "mandarin", "zh", "zh-cn", "zhcn"),
        lang("zh-TW", "Chinese (Traditional)", "chinese traditional", "zh-tw", "zhtw"),
        lang("ja", "Japanese", "japanese", "ja", "jpn", "jp"),
        lang("ko", "Korean", "korean", "ko", "kor", "kr"),
        lang("ar", "Arabic", "arabic", "ar", "ara"),
        lang("he", "Hebrew", "hebrew", "he", "heb", "iw"),
        lang("hi", "Hindi", "hindi", "hi", "hin"),
        lang("bn", "Bengali", "bengali", "bn", "ben"),
        lang("tr", "Turkish", "turkish", "tr", "tur"),
        lang("pl", "Polish", "polish", "pl", "pol"),
        lang("uk", "Ukrainian", "ukrainian", "uk", "ukr"),
        lang("nl", "Dutch", "dutch", "nl", "nld", "dut"),
        lang("sv", "Swedish", "swedish", "sv", "swe"),
        lang("no", "Norwegian", "norwegian", "no", "nor"),
        lang("da", "Danish", "danish", "da", "dan"),
        lang("fi", "Finnish", "finnish", "fi", "fin"),
        lang("cs", "Czech", "czech", "cs", "cze", "ces"),
        lang("el", "Greek", "greek", "el", "gre", "ell"),
        lang("ro", "Romanian", "romanian", "ro", "ron", "rum"),
        lang("hu", "Hungarian", "hungarian", "hu", "hun"),
        lang("th", "Thai", "thai", "th", "tha"),
        lang("vi", "Vietnamese", "vietnamese", "vi", "vie"),
        lang("id", "Indonesian", "indonesian", "id", "ind"),
        lang("ms", "Malay", "malay", "ms", "may", "msa"),
        lang("fil", "Filipino", "filipino", "tagalog", "fil"),
        lang("sw", "Swahili", "swahili", "sw", "swa"),
        lang("fa", "Persian", "persian", "farsi", "fa", "fas"),
        lang("ur", "Urdu", "urdu", "ur", "urd"),
        lang("ta", "Tamil", "tamil", "ta", "tam"),
        lang("te", "Telugu", "telugu", "te", "tel"),
        lang("mr", "Marathi", "marathi", "mr", "mar"),
        lang("gu", "Gujarati", "gujarati", "gu", "guj"),
        lang("kn", "Kannada", "kannada", "kn", "kan"),
        lang("ml", "Malayalam", "malayalam", "ml", "mal"),
        lang("pa", "Punjabi", "punjabi", "pa", "pan"),
        lang("am", "Amharic", "amharic", "am", "amh"),
        lang("af", "Afrikaans", "afrikaans", "af", "afr"),
        lang("bg", "Bulgarian", "bulgarian", "bg", "bul"),
        lang("hr", "Croatian", "croatian", "hr", "hrv"),
        lang("sr", "Serbian", "serbian", "sr", "srp"),
        lang("sk", "Slovak", "slovak", "sk", "slk", "slo"),
        lang("sl", "Slovenian", "slovenian", "sl", "slv"),
        lang("lt", "Lithuanian", "lithuanian", "lt", "lit"),
        lang("lv", "Latvian", "latvian", "lv", "lav"),
        lang("et", "Estonian", "estonian", "et", "est"),
        lang("ca", "Catalan", "catalan", "ca", "cat"),
        lang("eu", "Basque", "basque", "eu", "baq", "eus"),
        lang("gl", "Galician", "galician", "gl", "glg"),
        lang("is", "Icelandic", "icelandic", "is", "ice", "isl"),
    )

    val popular: List<TranslatorLanguage> = listOf(
        byCode("en"), byCode("es"), byCode("fr"), byCode("de"), byCode("he"),
        byCode("ar"), byCode("ru"), byCode("zh-CN"), byCode("ja"), byCode("pt"),
    ).filterNotNull()

    private val byAliasLength: List<Pair<String, TranslatorLanguage>> =
        all.flatMap { lang -> lang.aliases.map { alias -> alias to lang } }
            .sortedByDescending { it.first.length }

    fun byCode(code: String): TranslatorLanguage? =
        all.firstOrNull { it.code.equals(code, ignoreCase = true) }

    fun matchPrefix(text: String): LanguagePrefixMatch? {
        val trimmed = text.trimStart()
        val lower = trimmed.lowercase()
        for ((alias, lang) in byAliasLength) {
            if (!lower.startsWith(alias)) continue
            val next = trimmed.getOrNull(alias.length)
            if (next != null && next !in " \t") continue
            return LanguagePrefixMatch(lang, alias.length)
        }
        return null
    }

    fun suggest(partial: String, limit: Int = 8): List<TranslatorLanguage> {
        val q = partial.trim().lowercase()
        if (q.isEmpty()) return popular.take(limit)
        return all.filter { lang ->
            lang.aliases.any { it.startsWith(q) } ||
                lang.displayName.lowercase().contains(q)
        }.sortedWith(
            compareBy<TranslatorLanguage> { lang ->
                when {
                    lang.aliases.any { it == q } -> 0
                    lang.aliases.any { it.startsWith(q) } -> 1
                    else -> 2
                }
            }.thenBy { it.displayName },
        ).take(limit)
    }

    fun completePartial(partial: String): TranslatorLanguage? {
        val q = partial.trim().lowercase()
        if (q.isEmpty()) return null
        val exact = all.firstOrNull { lang -> lang.aliases.any { it == q } }
        if (exact != null) return exact
        val starts = all.filter { lang -> lang.aliases.any { it.startsWith(q) && it.length > q.length } }
        return starts.minByOrNull { it.displayName.length }
    }

    /** Longer alias to finish typing, e.g. eng → english. */
    fun expandPartial(partial: String): String? {
        val q = partial.trim().lowercase()
        if (q.isEmpty()) return null
        val lang = all.firstOrNull { l -> l.aliases.any { it.startsWith(q) } } ?: return null
        val best = lang.aliases.filter { it.startsWith(q) }.maxByOrNull { it.length } ?: return null
        return best.takeIf { it.length > q.length }
    }

    private fun lang(code: String, displayName: String, vararg aliases: String): TranslatorLanguage {
        val normalized = aliases.map { it.lowercase() }.distinct()
        return TranslatorLanguage(code, displayName, normalized)
    }
}

data class LanguagePrefixMatch(
    val language: TranslatorLanguage,
    val consumedChars: Int,
)
