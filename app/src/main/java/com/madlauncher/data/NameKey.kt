package com.madlauncher.data

/** Contact names with apostrophes and punctuation folded so Pe'er matches Peer. */
object NameKey {
    private val apostrophes = Regex("['’ʼ`׳´]")

    fun fold(raw: String): String =
        raw.lowercase()
            .replace(apostrophes, "")
            .replace('/', ' ')
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Words to look up, longest-useful first, plus the full folded phrase. */
    fun needles(query: String): List<String> {
        val folded = fold(query)
        if (folded.isEmpty()) return emptyList()
        val words = folded.split(' ').filter { it.length >= 3 }
        return (words + folded).distinct()
    }

    fun joinMarks(raw: String): String = raw.replace(apostrophes, "")
}
