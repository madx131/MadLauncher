package com.madlauncher.action

enum class TranslateLangSlot { From, To }

data class ParsedTranslate(
    val fromCode: String?,
    val toCode: String?,
    val text: String,
    val activeSlot: TranslateLangSlot?,
    val partialLang: String,
)

object TranslateCommandParser {
    fun parse(tail: String): ParsedTranslate {
        var rest = tail.trim()
        var fromCode: String? = null
        var toCode: String? = null

        val fromRest = consumeKeyword(rest, "from")
        if (fromRest != null) {
            rest = fromRest
            val match = TranslatorLanguages.matchPrefix(rest)
            if (match != null) {
                fromCode = match.language.code
                rest = rest.substring(match.consumedChars).trimStart()
            }
        }

        val toRest = consumeKeyword(rest, "to")
        if (toRest != null) {
            rest = toRest
            val match = TranslatorLanguages.matchPrefix(rest)
            if (match != null) {
                toCode = match.language.code
                rest = rest.substring(match.consumedChars).trimStart()
            }
        }

        val text = rest.trim()
        val keywordSlot = activeSlot(tail)
        // Text without a target: prompt for "to" (do not guess Spanish / locale).
        val active = keywordSlot
            ?: if (toCode == null && text.isNotEmpty()) TranslateLangSlot.To to "" else null
        return ParsedTranslate(
            fromCode = fromCode,
            toCode = toCode,
            text = text,
            activeSlot = active?.first,
            partialLang = active?.second.orEmpty(),
        )
    }

    fun activeSlot(fullTail: String): Pair<TranslateLangSlot, String>? {
        val body = fullTail.trim()
        val lower = body.lowercase()
        if (lower == "from") return TranslateLangSlot.From to ""
        if (lower == "to") return TranslateLangSlot.To to ""
        val fromIdx = lower.indexOf("from ")
        val toIdx = keywordIndex(lower, "to")
        if (fromIdx >= 0 && (toIdx < 0 || fromIdx < toIdx)) {
            slotPartial(body.substring(fromIdx + 5), TranslateLangSlot.From)?.let { return it }
        }
        if (toIdx >= 0) {
            slotPartial(body.substring(toIdx + 2).trimStart(), TranslateLangSlot.To)?.let { return it }
        }
        return null
    }

    private fun slotPartial(afterKeyword: String, slot: TranslateLangSlot): Pair<TranslateLangSlot, String>? {
        val trimmed = afterKeyword.trimStart()
        if (isKeyword(trimmed, "to")) {
            return slotPartial(consumeKeyword(trimmed, "to").orEmpty(), TranslateLangSlot.To)
        }
        val match = TranslatorLanguages.matchPrefix(trimmed)
        if (match != null) {
            val rest = trimmed.substring(match.consumedChars).trimStart()
            if (rest.isEmpty()) return null
            if (isKeyword(rest, "to")) {
                return slotPartial(consumeKeyword(rest, "to").orEmpty(), TranslateLangSlot.To)
            }
            return null
        }
        if (trimmed.isEmpty()) return slot to ""
        return slot to trimmed
    }

    private fun tailLangPartial(tail: String): Pair<TranslateLangSlot, String>? {
        val from = Regex("from\\s+(\\S*)$", RegexOption.IGNORE_CASE).find(tail)
        if (from != null) return TranslateLangSlot.From to from.groupValues[1]
        val to = Regex("to\\s+(\\S*)$", RegexOption.IGNORE_CASE).find(tail)
        if (to != null) return TranslateLangSlot.To to to.groupValues[1]
        return null
    }

    fun autocomplete(fullQuery: String): String? {
        val parts = CommandAliases.translatePrefix(fullQuery)
        val commandPrefix = parts?.first ?: ""
        val tail = parts?.second ?: fullQuery
        val slot = activeSlot(tail) ?: tailLangPartial(tail) ?: return null
        val (kind, partial) = slot
        if (partial.isBlank()) return null
        val langWord = TranslatorLanguages.expandPartial(partial) ?: return null
        val keyword = if (kind == TranslateLangSlot.From) "from" else "to"
        val keyIdx = lastKeywordIndex(tail, keyword)
        if (keyIdx < 0) return null
        val prefix = commandPrefix + tail.substring(0, keyIdx + keyword.length).trimEnd() + " "
        return "$prefix$langWord "
    }

    fun applyLanguage(fullQuery: String, lang: TranslatorLanguage): String {
        val parts = CommandAliases.translatePrefix(fullQuery)
        val commandPrefix = parts?.first ?: ""
        val tail = parts?.second ?: fullQuery
        val slot = activeSlot(tail)
        if (slot != null) {
            val (kind, _) = slot
            val keyword = if (kind == TranslateLangSlot.From) "from" else "to"
            val keyIdx = lastKeywordIndex(tail, keyword)
            if (keyIdx >= 0) {
                val before = tail.substring(0, keyIdx + keyword.length).trimEnd() + " "
                return "$commandPrefix$before${lang.primaryAlias} "
            }
        }
        return insertToLanguage(commandPrefix, tail, lang)
    }

    /** Chip tap when there is no "to" keyword yet: keep from + text, insert target. */
    private fun insertToLanguage(commandPrefix: String, tail: String, lang: TranslatorLanguage): String {
        val parsed = parse(tail)
        val text = parsed.text
        val head = if (text.isEmpty()) {
            tail.trimEnd()
        } else {
            val idx = tail.lastIndexOf(text)
            if (idx >= 0) tail.substring(0, idx).trimEnd() else tail.trimEnd()
        }
        return buildString {
            append(commandPrefix)
            if (head.isNotEmpty()) {
                append(head)
                append(' ')
            }
            append("to ")
            append(lang.primaryAlias)
            if (text.isNotEmpty()) {
                append(' ')
                append(text)
            }
            append(' ')
        }
    }

    fun label(code: String?): String? =
        code?.let { TranslatorLanguages.byCode(it)?.displayName ?: it }

    /** Remaining text after a leading keyword, or null if [word] is not the next token. */
    private fun consumeKeyword(text: String, word: String): String? {
        val trimmed = text.trimStart()
        val lower = trimmed.lowercase()
        if (lower == word) return ""
        if (lower.startsWith("$word ")) return trimmed.substring(word.length).trimStart()
        return null
    }

    private fun isKeyword(text: String, word: String): Boolean =
        consumeKeyword(text, word) != null

    /** Index of [word] as its own token (`to` / `to `), or -1. */
    private fun keywordIndex(lower: String, word: String): Int {
        if (lower == word) return 0
        val spaced = "$word "
        val idx = lower.indexOf(spaced)
        if (idx >= 0) return idx
        val suffix = " $word"
        if (lower.endsWith(suffix)) return lower.length - word.length
        return -1
    }

    private fun lastKeywordIndex(text: String, word: String): Int {
        val lower = text.lowercase()
        if (lower.trim() == word) {
            val start = text.indexOfFirst { !it.isWhitespace() }
            return if (start >= 0) start else 0
        }
        val spaced = lower.lastIndexOf("$word ")
        if (spaced >= 0) return spaced
        val suffix = " $word"
        if (lower.trimEnd().endsWith(suffix)) {
            return lower.trimEnd().length - word.length
        }
        return -1
    }
}
