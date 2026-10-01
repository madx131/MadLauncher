package com.madlauncher.data

import android.graphics.RectF

/** Home-grid drop targeting. Infinite pager duplicates must not steal the visible page's cells. */
object HomeDropHit {
    /** `"page:col,row"` → page, col, row. */
    fun parseKey(key: String): Triple<Int, Int, Int>? {
        val page = key.substringBefore(":").toIntOrNull() ?: return null
        val parts = key.substringAfter(":").split(",")
        if (parts.size != 2) return null
        val col = parts[0].toIntOrNull() ?: return null
        val row = parts[1].toIntOrNull() ?: return null
        return Triple(page, col, row)
    }

    fun pick(
        rects: Map<String, RectF>,
        x: Float,
        y: Float,
        preferredPage: Int,
        validCell: (page: Int, col: Int, row: Int) -> Boolean = { _, _, _ -> true },
    ): String? {
        val hits = rects.mapNotNull { (key, rect) ->
            val parsed = parseKey(key) ?: return@mapNotNull null
            if (parsed.first != preferredPage) return@mapNotNull null
            if (!validCell(parsed.first, parsed.second, parsed.third)) return@mapNotNull null
            if (!contains(rect.left, rect.top, rect.right, rect.bottom, x, y)) return@mapNotNull null
            val cx = (rect.left + rect.right) / 2f
            val cy = (rect.top + rect.bottom) / 2f
            val dx = cx - x
            val dy = cy - y
            key to (dx * dx + dy * dy)
        }
        return hits.minByOrNull { it.second }?.first
    }

    fun pagePrefix(pageIndex: Int): String = "$pageIndex:"

    internal fun choose(hits: List<Pair<String, Float>>, preferredPage: Int): String? {
        if (hits.isEmpty()) return null
        val prefix = pagePrefix(preferredPage)
        val pool = hits.filter { it.first.startsWith(prefix) }
        return pool.minByOrNull { it.second }?.first
    }

    internal fun contains(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        x: Float,
        y: Float,
    ): Boolean {
        return left < right &&
            top < bottom &&
            x >= left &&
            x < right &&
            y >= top &&
            y < bottom
    }

    /** True when the point sits in the inner [inner] fraction of the cell (folder-merge). */
    fun centeredIn(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        x: Float,
        y: Float,
        inner: Float = 0.5f,
    ): Boolean {
        val w = right - left
        val h = bottom - top
        if (w <= 0f || h <= 0f) return false
        val insetX = w * (1f - inner.coerceIn(0.15f, 1f)) / 2f
        val insetY = h * (1f - inner.coerceIn(0.15f, 1f)) / 2f
        return contains(left + insetX, top + insetY, right - insetX, bottom - insetY, x, y)
    }
}
