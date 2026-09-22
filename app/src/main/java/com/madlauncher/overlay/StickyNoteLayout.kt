package com.madlauncher.overlay

/**
 * Snap / pin math for sticky note overlay windows. Pure so it can be unit-tested.
 */
object StickyNoteLayout {
    const val SNAP_DP = 48
    const val PEEK_DP = 12
    const val FLING_PX_S = 1400f
    const val BUBBLE_DP = 56
    const val EXPANDED_WIDTH_DP = 260

    enum class Edge { LEFT, RIGHT, TOP, BOTTOM }

    data class Placement(
        val x: Int,
        val y: Int,
        val edge: Edge?,
    )

    fun edgeName(edge: Edge?): String? = edge?.name?.lowercase()

    fun parseEdge(raw: String?): Edge? =
        Edge.entries.find { it.name.equals(raw, ignoreCase = true) }

    fun settle(
        x: Int,
        y: Int,
        viewW: Int,
        viewH: Int,
        screenW: Int,
        screenH: Int,
        snapPx: Int,
        collapsed: Boolean,
        peekPx: Int,
        velocityX: Float = 0f,
        velocityY: Float = 0f,
        flingPxS: Float = FLING_PX_S,
    ): Placement {
        val flung = flingEdge(velocityX, velocityY, flingPxS)
        if (flung != null) {
            return pinTo(flung, x, y, viewW, viewH, screenW, screenH, collapsed, peekPx)
        }
        val left = x
        val right = screenW - (x + viewW)
        val top = y
        val bottom = screenH - (y + viewH)
        val nearest = listOf(
            Edge.LEFT to left,
            Edge.RIGHT to right,
            Edge.TOP to top,
            Edge.BOTTOM to bottom,
        ).minBy { it.second }
        return if (nearest.second <= snapPx) {
            pinTo(nearest.first, x, y, viewW, viewH, screenW, screenH, collapsed, peekPx)
        } else {
            val c = clamp(x, y, viewW, viewH, screenW, screenH, collapsed, peekPx)
            Placement(c.first, c.second, null)
        }
    }

    fun pinTo(
        edge: Edge,
        x: Int,
        y: Int,
        viewW: Int,
        viewH: Int,
        screenW: Int,
        screenH: Int,
        collapsed: Boolean,
        peekPx: Int,
    ): Placement {
        val c = clamp(x, y, viewW, viewH, screenW, screenH, collapsed, peekPx)
        val pinned = when (edge) {
            Edge.LEFT -> 0 to c.second
            Edge.RIGHT -> (screenW - viewW).coerceAtLeast(0) to c.second
            Edge.TOP -> c.first to 0
            Edge.BOTTOM -> c.first to (screenH - viewH).coerceAtLeast(0)
        }
        return Placement(pinned.first, pinned.second, edge)
    }

    fun clamp(
        x: Int,
        y: Int,
        viewW: Int,
        viewH: Int,
        screenW: Int,
        screenH: Int,
        collapsed: Boolean,
        peekPx: Int,
    ): Pair<Int, Int> {
        val minX = if (collapsed) -(viewW - peekPx).coerceAtLeast(0) else 0
        val maxX = if (collapsed) screenW - peekPx else (screenW - viewW).coerceAtLeast(0)
        val minY = 0
        val maxY = (screenH - viewH).coerceAtLeast(0)
        return x.coerceIn(minX, maxX.coerceAtLeast(minX)) to y.coerceIn(minY, maxY.coerceAtLeast(minY))
    }

    private fun flingEdge(velocityX: Float, velocityY: Float, flingPxS: Float): Edge? {
        val absX = kotlin.math.abs(velocityX)
        val absY = kotlin.math.abs(velocityY)
        return when {
            absX >= flingPxS && absX >= absY -> if (velocityX < 0f) Edge.LEFT else Edge.RIGHT
            absY >= flingPxS -> if (velocityY < 0f) Edge.TOP else Edge.BOTTOM
            else -> null
        }
    }
}
