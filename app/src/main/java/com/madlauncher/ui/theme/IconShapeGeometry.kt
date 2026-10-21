package com.madlauncher.ui.theme

import com.madlauncher.data.IconShapeId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Outlines for hexagon and diamond. Polylines are in pixel space so the fill clip
 * and the tests share one shape. An installed icon style brings its own points.
 */
internal object IconShapeGeometry {
    /** Logo anchor for each mask. Classic shapes keep the picture full-bleed and centered. */
    fun logoSlot(shape: IconShapeId): IconLogoSlot = when (shape) {
        IconShapeId.HEXAGON -> IconLogoSlot(0.50f, 0.50f, 0.82f)
        IconShapeId.DIAMOND -> IconLogoSlot(0.50f, 0.50f, 0.58f)
        else -> IconLogoSlot()
    }

    fun outline(shape: IconShapeId, width: Float, height: Float): List<Pair<Float, Float>> =
        when (shape) {
            IconShapeId.HEXAGON -> roundedPolyline(hexPoints(width, height), min(width, height) * 0.07f)
            IconShapeId.DIAMOND -> roundedPolyline(diamondPoints(width, height), min(width, height) * 0.06f)
            else -> emptyList()
        }

    fun contains(shape: IconShapeId, width: Float, height: Float, x: Float, y: Float): Boolean {
        val poly = outline(shape, width, height)
        if (poly.size < 3) return false
        return pointInPolygon(poly, x, y)
    }

    /** Area of the outline as a fraction of width*height. 0 when the shape has no polyline. */
    fun areaFraction(shape: IconShapeId, width: Float, height: Float): Float {
        val poly = outline(shape, width, height)
        if (poly.size < 3) return 0f
        val box = width * height
        if (box <= 0f) return 0f
        return abs(signedArea(poly)).toFloat() / box
    }

    private fun diamondPoints(w: Float, h: Float): List<Pair<Float, Float>> = listOf(
        w * 0.50f to h * 0.04f,
        w * 0.96f to h * 0.50f,
        w * 0.50f to h * 0.96f,
        w * 0.04f to h * 0.50f,
    )

    /** Flat-top hex, sized to the width of the slot. */
    private fun hexPoints(w: Float, h: Float): List<Pair<Float, Float>> {
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) * 0.48f
        return List(6) { i ->
            val a = i * PI.toFloat() / 3f
            (cx + cos(a) * r) to (cy + sin(a) * r)
        }
    }

    private fun roundedPolyline(
        points: List<Pair<Float, Float>>,
        radius: Float,
        cornerSamples: Int = 5,
    ): List<Pair<Float, Float>> {
        val n = points.size
        val out = ArrayList<Pair<Float, Float>>(n * (cornerSamples + 1))
        for (i in 0 until n) {
            val prev = points[(i + n - 1) % n]
            val curr = points[i]
            val next = points[(i + 1) % n]
            val v1x = prev.first - curr.first
            val v1y = prev.second - curr.second
            val v2x = next.first - curr.first
            val v2y = next.second - curr.second
            val len1 = hypot(v1x, v1y).coerceAtLeast(0.001f)
            val len2 = hypot(v2x, v2y).coerceAtLeast(0.001f)
            val cut = min(radius, min(len1, len2) * 0.45f)
            val ax = curr.first + v1x / len1 * cut
            val ay = curr.second + v1y / len1 * cut
            val bx = curr.first + v2x / len2 * cut
            val by = curr.second + v2y / len2 * cut
            for (s in 0..cornerSamples) {
                val t = s / cornerSamples.toFloat()
                val u = 1f - t
                out += (u * u * ax + 2f * u * t * curr.first + t * t * bx) to
                    (u * u * ay + 2f * u * t * curr.second + t * t * by)
            }
        }
        return out
    }

    private fun pointInPolygon(poly: List<Pair<Float, Float>>, x: Float, y: Float): Boolean {
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val yi = poly[i].second
            val yj = poly[j].second
            if ((yi > y) != (yj > y)) {
                val xi = poly[i].first
                val xj = poly[j].first
                val denom = (yj - yi)
                if (denom != 0f && x < (xj - xi) * (y - yi) / denom + xi) inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun signedArea(poly: List<Pair<Float, Float>>): Double {
        var a = 0.0
        for (i in poly.indices) {
            val j = (i + 1) % poly.size
            a += poly[i].first.toDouble() * poly[j].second - poly[j].first.toDouble() * poly[i].second
        }
        return a * 0.5
    }
}
