package com.madlauncher.ui.theme

import com.madlauncher.data.IconShapeId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Silhouettes for the playful icon masks. Outlines are closed polylines in pixel
 * space so the fill clip and the tests share one shape. The app picture is not
 * scaled to these outlines.
 */
internal object IconShapeGeometry {
    private const val TAU = (PI * 2.0).toFloat()

    /** Logo anchor for each mask. Classic shapes keep the picture full-bleed and centered. */
    fun logoSlot(shape: IconShapeId): IconLogoSlot = when (shape) {
        IconShapeId.STAR -> IconLogoSlot(0.50f, 0.50f, 0.70f)
        IconShapeId.TRIANGLE -> IconLogoSlot(0.50f, 0.66f, 0.56f)
        IconShapeId.HEXAGON -> IconLogoSlot(0.50f, 0.50f, 0.82f)
        // Head of the comet, not the tail.
        IconShapeId.COMET -> IconLogoSlot(0.62f, 0.34f, 0.48f)
        // Thick limb on the left. The bite is to the right of this point.
        IconShapeId.CRESCENT -> IconLogoSlot(0.24f, 0.52f, 0.40f)
        // Face of the sun. The rays stay outside this disk.
        IconShapeId.SUN -> IconLogoSlot(0.50f, 0.50f, 0.72f)
        else -> IconLogoSlot()
    }

    fun outline(shape: IconShapeId, width: Float, height: Float): List<Pair<Float, Float>> =
        when (shape) {
            IconShapeId.STAR -> roundedPolyline(starPoints(width, height), min(width, height) * 0.035f)
            IconShapeId.TRIANGLE -> roundedPolyline(trianglePoints(width, height), min(width, height) * 0.07f)
            IconShapeId.HEXAGON -> roundedPolyline(hexPoints(width, height), min(width, height) * 0.07f)
            IconShapeId.COMET -> cometOutline(width, height)
            IconShapeId.CRESCENT -> crescentOutline(width, height)
            IconShapeId.SUN -> sunOutline(width, height)
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

    private fun starPoints(w: Float, h: Float): List<Pair<Float, Float>> {
        val cx = w / 2f
        val cy = h / 2f
        val outer = min(w, h) * 0.48f
        val inner = outer * 0.42f
        return List(10) { i ->
            val a = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
            val r = if (i % 2 == 0) outer else inner
            (cx + cos(a) * r) to (cy + sin(a) * r)
        }
    }

    private fun trianglePoints(w: Float, h: Float): List<Pair<Float, Float>> = listOf(
        w * 0.50f to h * 0.05f,
        w * 0.95f to h * 0.95f,
        w * 0.05f to h * 0.95f,
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

    /** Round head with a narrow tail. The mouth faces the tip so the head stays a disk. */
    private fun cometOutline(w: Float, h: Float): List<Pair<Float, Float>> {
        val m = min(w, h)
        val cx = w * 0.62f
        val cy = h * 0.34f
        val r = m * 0.32f
        val tipX = w * 0.06f
        val tipY = h * 0.92f
        val phi = atan2(tipY - cy, tipX - cx)
        val mouth = (70.0 * PI / 180.0).toFloat()
        val sweep = mouth - TAU
        return listOf(tipX to tipY) + sampleArc(cx, cy, r, phi - mouth / 2f, sweep, 40)
    }

    private fun crescentOutline(w: Float, h: Float): List<Pair<Float, Float>> {
        val m = min(w, h)
        val ox = w * 0.46f
        val oy = h * 0.50f
        val or = m * 0.45f
        val ix = w * 0.82f
        val iy = h * 0.48f
        val ir = m * 0.36f
        val hits = circleHits(ox, oy, or, ix, iy, ir) ?: return circleOutline(ox, oy, or, 40)
        val (p1, p2) = hits
        val a1 = atan2(p1.second - oy, p1.first - ox)
        val a2 = atan2(p2.second - oy, p2.first - ox)
        val outerSweep = sweepPreferring(ox, oy, or, a1, a2) { mx, my ->
            hypot(mx - ix, my - iy) >= ir
        }
        val b2 = atan2(p2.second - iy, p2.first - ix)
        val b1 = atan2(p1.second - iy, p1.first - ix)
        val innerSweep = sweepPreferring(ix, iy, ir, b2, b1) { mx, my ->
            hypot(mx - ox, my - oy) <= or
        }
        val outerPts = sampleArc(ox, oy, or, a1, outerSweep, 36)
        val innerPts = sampleArc(ix, iy, ir, b2, innerSweep, 28)
        return outerPts + innerPts.drop(1).dropLast(1)
    }

    /** Round face with eight triangular rays. One ray points straight up. */
    private fun sunOutline(w: Float, h: Float): List<Pair<Float, Float>> {
        val m = min(w, h)
        val cx = w * 0.50f
        val cy = h * 0.50f
        val disk = m * 0.30f
        val tipR = m * 0.48f
        val rays = 8
        val half = (12.0 * PI / 180.0).toFloat()
        val step = TAU / rays
        val pts = ArrayList<Pair<Float, Float>>(rays * 8)
        for (i in 0 until rays) {
            val mid = -PI.toFloat() / 2f + i * step
            val left = mid - half
            val prevRight = mid - step + half
            pts += sampleArc(cx, cy, disk, prevRight, left - prevRight, 5).dropLast(1)
            pts += pointOnCircle(cx, cy, disk, left)
            pts += pointOnCircle(cx, cy, tipR, mid)
            pts += pointOnCircle(cx, cy, disk, mid + half)
        }
        return pts
    }

    private fun circleOutline(cx: Float, cy: Float, r: Float, steps: Int): List<Pair<Float, Float>> =
        sampleArc(cx, cy, r, 0f, TAU, steps).dropLast(1)

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

    private fun sampleArc(
        cx: Float,
        cy: Float,
        r: Float,
        start: Float,
        sweep: Float,
        steps: Int,
    ): List<Pair<Float, Float>> = List(steps + 1) { i ->
        pointOnCircle(cx, cy, r, start + sweep * (i / steps.toFloat()))
    }

    private fun pointOnCircle(cx: Float, cy: Float, r: Float, angle: Float): Pair<Float, Float> =
        (cx + r * cos(angle)) to (cy + r * sin(angle))

    private fun circleHits(
        x1: Float, y1: Float, r1: Float,
        x2: Float, y2: Float, r2: Float,
    ): Pair<Pair<Float, Float>, Pair<Float, Float>>? {
        val dx = x2 - x1
        val dy = y2 - y1
        val d = hypot(dx, dy)
        if (d < 1e-3f || d > r1 + r2 || d < abs(r1 - r2)) return null
        val a = (r1 * r1 - r2 * r2 + d * d) / (2f * d)
        val h2 = r1 * r1 - a * a
        if (h2 < 0f) return null
        val h = sqrt(h2)
        val xm = x1 + a * dx / d
        val ym = y1 + a * dy / d
        val rx = -dy * (h / d)
        val ry = dx * (h / d)
        return (xm + rx to ym + ry) to (xm - rx to ym - ry)
    }

    /** Positive sweep is clockwise on screen (y grows downward). */
    private fun sweepPreferring(
        cx: Float,
        cy: Float,
        r: Float,
        start: Float,
        end: Float,
        prefer: (Float, Float) -> Boolean,
    ): Float {
        val cw = forwardSweep(start, end)
        val ccw = cw - TAU
        val midCw = pointOnCircle(cx, cy, r, start + cw / 2f)
        return if (prefer(midCw.first, midCw.second)) cw else ccw
    }

    private fun forwardSweep(start: Float, end: Float): Float {
        var d = end - start
        while (d <= 0.01f) d += TAU
        return d
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
