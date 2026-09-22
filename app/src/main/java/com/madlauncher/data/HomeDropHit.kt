package com.madlauncher.data

import android.graphics.RectF

/** Home-grid drop targeting. Infinite pager duplicates must not steal the visible page's cells. */
object HomeDropHit {
    fun pick(
        rects: Map<String, RectF>,
        x: Float,
        y: Float,
        preferredPage: Int,
    ): String? {
        val hits = rects.mapNotNull { (key, rect) ->
            if (!contains(rect.left, rect.top, rect.right, rect.bottom, x, y)) return@mapNotNull null
            key to area(rect.left, rect.top, rect.right, rect.bottom)
        }
        return choose(hits, preferredPage)
    }

    fun pagePrefix(pageIndex: Int): String = "$pageIndex:"

    internal fun choose(hits: List<Pair<String, Float>>, preferredPage: Int): String? {
        if (hits.isEmpty()) return null
        val prefix = pagePrefix(preferredPage)
        val pool = hits.filter { it.first.startsWith(prefix) }.ifEmpty { hits }
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

    private fun area(left: Float, top: Float, right: Float, bottom: Float): Float {
        val a = (right - left) * (bottom - top)
        return if (a <= 0f) Float.MAX_VALUE else a
    }
}
