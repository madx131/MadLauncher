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

/** MAD icon masks — keep radii conservative so paths never collapse to empty (blank icons). */
object IconShapes {
    fun of(shape: IconShapeId, cornerDp: Float): Shape = when (shape) {
        IconShapeId.CIRCLE -> RoundedCornerShape(percent = 50)
        IconShapeId.SQUARE -> RoundedCornerShape(0.dp)
        IconShapeId.ROUNDED_SQUARE -> RoundedCornerShape(cornerDp.dp.coerceIn(4.dp, 18.dp))
        // Squircle-ish: round but not a full circle.
        IconShapeId.SQUIRCLE -> RoundedCornerShape(percent = 30)
        // Teardrop: three round corners + one sharp. Never use 50% on three corners —
        // that self-intersects and clips the icon to nothing.
        IconShapeId.TEARDROP -> TeardropShape
    }
}

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
