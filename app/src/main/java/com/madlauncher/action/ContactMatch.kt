package com.madlauncher.action

import com.madlauncher.data.ContactEntry
import com.madlauncher.data.NameKey

/** Puts the closest name first. Used by `&` contact search. */
object ContactMatch {
    fun order(query: String, contacts: List<ContactEntry>): List<ContactEntry> {
        val q = normalize(query)
        if (q.isEmpty() || contacts.size < 2) return contacts
        return contacts.sortedByDescending { score(q, it.name) }
    }

    internal fun score(query: String, name: String): Int {
        val q = normalize(query)
        val n = normalize(name)
        if (q.isEmpty() || n.isEmpty()) return 0
        val words = n.split(' ')
        val queryWords = q.split(' ')
        val tier = when {
            n == q -> 4
            n.startsWith(q) -> 3
            words.any { it.startsWith(q) } -> 2
            queryWords.all { word -> words.any { it.startsWith(word) } } -> 2
            queryWords.all { word -> closeWord(word, words) } -> 2
            n.contains(q) -> 1
            else -> 0
        }
        val coverage = q.length * 100 / n.length.coerceAtLeast(1)
        val fuzzy = if (tier == 0) AppFuzzy.score(q, n) else 0
        return tier * 1000 + coverage + fuzzy
    }

    private fun closeWord(word: String, words: List<String>): Boolean {
        if (word.length < 3) return false
        return words.any { it.length >= 3 && editDistance(word, it) <= 1 }
    }

    private fun editDistance(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            for (j in 0..b.length) prev[j] = cur[j]
        }
        return prev[b.length]
    }

    private fun normalize(raw: String): String = NameKey.fold(raw)
}
