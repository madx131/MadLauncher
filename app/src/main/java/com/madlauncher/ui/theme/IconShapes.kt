package com.madlauncher.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.madlauncher.data.IconShapeId
import kotlin.math.min

/**
 * Where the app logo sits inside a mask.
 * [centerX] and [centerY] are fractions of the icon slot (0–1, y grows downward).
 * [scale] is the fraction of the slot used by the foreground layer. Adaptive glyphs
 * occupy about the center 66% of that layer.
 */
data class IconLogoSlot(
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val scale: Float = 1f,
) {
    val placesLogo: Boolean
        get() = scale < 0.999f || kotlin.math.abs(centerX - 0.5f) > 0.001f || kotlin.math.abs(centerY - 0.5f) > 0.001f
}

/** MAD icon masks — keep radii conservative so paths never collapse to empty (blank icons). */
object IconShapes {
    fun logoSlot(shape: IconShapeId): IconLogoSlot = IconShapeGeometry.logoSlot(shape)

    /** Star, moon, and the other playful masks reshape the fill and park the logo on [logoSlot]. */
    fun shapesFillOnly(shape: IconShapeId): Boolean = when (shape) {
        IconShapeId.HEXAGON,
        IconShapeId.DIAMOND,
        IconShapeId.STAR,
        IconShapeId.TRIANGLE,
        IconShapeId.COMET,
        IconShapeId.CRESCENT,
        IconShapeId.SUN,
        IconShapeId.MOUNTAIN,
        IconShapeId.PINE,
        IconShapeId.DOG,
        IconShapeId.CAT,
        IconShapeId.TIE,
        IconShapeId.BOW_TIE -> true
        else -> false
    }

    /**
     * Keep the icon the package manager already drew (Edge / BlueStacks silhouette).
     * Do not force a rounded tile or a fill plate.
     */
    fun usesAdaptiveInset(shape: IconShapeId): Boolean = shape == IconShapeId.ADAPTIVE

    fun of(shape: IconShapeId, cornerDp: Float): Shape = when (shape) {
        // Adaptive keeps each glyph’s own alpha; clip is a no-op square.
        IconShapeId.ADAPTIVE -> RoundedCornerShape(0.dp)
        IconShapeId.CIRCLE -> RoundedCornerShape(percent = 50)
        IconShapeId.SQUARE -> RoundedCornerShape(0.dp)
        IconShapeId.ROUNDED_SQUARE -> RoundedCornerShape(cornerDp.dp.coerceIn(4.dp, 18.dp))
        // Squircle-ish: round but not a full circle.
        IconShapeId.SQUIRCLE -> RoundedCornerShape(percent = 30)
        // Teardrop: three round corners + one sharp. Never use 50% on three corners —
        // that self-intersects and clips the icon to nothing.
        IconShapeId.TEARDROP -> TeardropShape
        IconShapeId.HEXAGON -> HexagonShape
        IconShapeId.DIAMOND -> DiamondShape
        IconShapeId.STAR -> StarShape
        IconShapeId.TRIANGLE -> TriangleShape
        IconShapeId.COMET -> CometShape
        IconShapeId.CRESCENT -> CrescentShape
        IconShapeId.SUN -> SunShape
        IconShapeId.MOUNTAIN -> MountainShape
        IconShapeId.PINE -> PineShape
        IconShapeId.DOG -> DogShape
        IconShapeId.CAT -> CatShape
        IconShapeId.TIE -> TieShape
        IconShapeId.BOW_TIE -> BowTieShape
        // No MAD mask. Transparent pixels around the glyph stay transparent.
        IconShapeId.ADAPTIVE -> RoundedCornerShape(0.dp)
    }
}

/** Closed polyline mask. Same instance per shape so clip equality stays stable. */
private class OutlineShape(private val id: IconShapeId) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val pts = IconShapeGeometry.outline(id, size.width, size.height)
        val path = Path()
        if (pts.size < 3) {
            path.moveTo(0f, 0f)
            path.lineTo(size.width, 0f)
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.close()
        } else {
            path.moveTo(pts[0].first, pts[0].second)
            for (i in 1 until pts.size) path.lineTo(pts[i].first, pts[i].second)
            path.close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean = other is OutlineShape && other.id == id

    override fun hashCode(): Int = id.hashCode()
}

private val HexagonShape = OutlineShape(IconShapeId.HEXAGON)
private val DiamondShape = OutlineShape(IconShapeId.DIAMOND)
private val StarShape = OutlineShape(IconShapeId.STAR)
private val TriangleShape = OutlineShape(IconShapeId.TRIANGLE)
private val CometShape = OutlineShape(IconShapeId.COMET)
private val CrescentShape = OutlineShape(IconShapeId.CRESCENT)
private val SunShape = OutlineShape(IconShapeId.SUN)
private val MountainShape = OutlineShape(IconShapeId.MOUNTAIN)
private val PineShape = OutlineShape(IconShapeId.PINE)
private val DogShape = OutlineShape(IconShapeId.DOG)
private val CatShape = OutlineShape(IconShapeId.CAT)
private val TieShape = OutlineShape(IconShapeId.TIE)
private val BowTieShape = OutlineShape(IconShapeId.BOW_TIE)

/** Bottom-start is the pointed/soft corner; other three are round (~38%). */
private val TeardropShape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = min(size.width, size.height) * 0.38f
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    topLeftCornerRadius = CornerRadius(r, r),
                    topRightCornerRadius = CornerRadius(r, r),
                    bottomRightCornerRadius = CornerRadius(r, r),
                    bottomLeftCornerRadius = CornerRadius(0f, 0f),
                ),
            )
        }
        return Outline.Generic(path)
    }
}
