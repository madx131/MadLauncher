package com.madlauncher.action

/** Split `/a Alice 050123` into a name and optional phone. */
object ContactInsertParser {
    fun parse(raw: String): Pair<String, String> {
        val trimmed = stripCommandPrefix(raw)
        if (trimmed.isEmpty()) return "" to ""
        if (CommandDetector.isPhoneLike(trimmed)) {
            return "" to trimmed
        }
        val parts = trimmed.split(Regex("\\s+"))
        val last = parts.last()
        if (parts.size > 1 && CommandDetector.isPhoneLike(last)) {
            return parts.dropLast(1).joinToString(" ") to last
        }
        return trimmed to ""
    }

    /** Drop `/a`, `add contact`, and other aliases so they are not saved as the contact name. */
    fun stripCommandPrefix(raw: String): String {
        val trimmed = raw.trim().replace('／', '/')
        if (trimmed.isEmpty()) return ""
        val aliases = CommandAliases.allPhrases(AliasTarget.AddContact).sortedByDescending { it.length }
        for (alias in aliases) {
            if (trimmed.equals(alias, ignoreCase = true)) return ""
            if (trimmed.length > alias.length &&
                trimmed.regionMatches(0, alias, 0, alias.length, ignoreCase = true) &&
                trimmed[alias.length].isWhitespace()
            ) {
                return trimmed.substring(alias.length).trim()
            }
        }
        return trimmed
    }
}
