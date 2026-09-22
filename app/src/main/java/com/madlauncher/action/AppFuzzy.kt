package com.madlauncher.action

/**
 * Typo-tolerant ranking for Action Box app / alias search.
 * FuzzyKot 1.0.0 needs Kotlin 2.3; MAD is on 2.0 so this is the same extractTop idea in-repo.
 */
object AppFuzzy {
    fun <T> rank(
        query: String,
        items: List<T>,
        limit: Int = 8,
        cutoff: Int = 56,
        name: (T) -> String,
    ): List<T> {
        val q = query.trim()
        if (q.isEmpty() || items.isEmpty()) return emptyList()
        val contains = items.filter { name(it).contains(q, ignoreCase = true) }
        if (contains.size >= limit) return contains.take(limit)
        val fuzzy = items.map { it to score(q, name(it)) }
            .filter { it.second >= cutoff }
            .sortedByDescending { it.second }
            .map { it.first }
        return (contains + fuzzy).distinct().take(limit)
    }

    internal fun score(query: String, target: String): Int {
        val q = query.lowercase()
        val t = target.lowercase()
        if (q.isEmpty() || t.isEmpty()) return 0
        if (t.contains(q)) return 100
        return maxOf(ratio(q, t), partialRatio(q, t))
    }

    private fun ratio(a: String, b: String): Int {
        val dist = levenshtein(a, b)
        val max = maxOf(a.length, b.length)
        if (max == 0) return 100
        return ((1.0 - dist.toDouble() / max) * 100).toInt()
    }

    private fun partialRatio(query: String, target: String): Int {
        if (query.length >= target.length) return ratio(query, target)
        var best = 0
        val window = query.length
        for (i in 0..target.length - window) {
            best = maxOf(best, ratio(query, target.substring(i, i + window)))
            if (best == 100) return 100
        }
        return best
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val prev = IntArray(n + 1) { it }
        val cur = IntArray(n + 1)
        for (i in 1..m) {
            cur[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            for (j in 0..n) prev[j] = cur[j]
        }
        return prev[n]
    }
}
