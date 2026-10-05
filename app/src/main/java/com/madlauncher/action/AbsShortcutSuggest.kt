package com.madlauncher.action

/**
 * 2–3 letter Action Box shortcuts in the same style as `gm`, `yt`, `wa`, `ig`.
 */
object AbsShortcutSuggest {
    fun phrases(label: String, packageName: String = ""): List<String> {
        val words = label.split(Regex("[^A-Za-z0-9]+"))
            .map { it.filter { ch -> ch.isLetter() }.lowercase() }
            .filter { it.isNotEmpty() }
        val pkgParts = packageName.split('.')
            .map { it.filter { ch -> ch.isLetter() }.lowercase() }
            .filter { it.length >= 2 }
        val out = LinkedHashSet<String>()
        fun add(raw: String) {
            val word = raw.lowercase().filter { it.isLetter() }
            if (word.length in 2..3) out.add(word)
        }
        val first = words.firstOrNull()
        if (first != null) {
            if (first.length >= 2) add(first.take(2))
            if (first.length >= 3) add(first.take(3))
            val consonants = first.filter { it !in "aeiou" }
            if (consonants.length >= 2) add(consonants.take(2))
            if (consonants.length >= 3) add(consonants.take(3))
        }
        if (words.size >= 2) {
            add(words.take(2).joinToString("") { it.take(1) })
            add(words.first().take(1) + words.last().take(1))
        }
        if (words.size >= 3) {
            add(words.take(3).joinToString("") { it.take(1) })
        }
        pkgParts.takeLast(2).forEach { part ->
            add(part.take(2))
            add(part.take(3))
        }
        return out.toList()
    }
}
