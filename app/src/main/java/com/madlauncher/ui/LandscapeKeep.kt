package com.madlauncher.ui

import android.view.OrientationEventListener
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/** Degrees to rotate icons / widget innards in place so they stay upright. */
val LocalUprightRotation = compositionLocalOf { 0f }

/**
 * Does not move or scale the home layout. When keep-portrait is on, the activity stays
 * portrait and only this rotation angle is applied to icons, labels, and widget contents.
 */
@Composable
fun LandscapeKeepHost(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val upright = rememberUprightRotation(enabled)
    CompositionLocalProvider(LocalUprightRotation provides upright) {
        Box(modifier, content = content)
    }
}

@Composable
private fun rememberUprightRotation(enabled: Boolean): Float {
    var deg by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    DisposableEffect(enabled) {
        if (!enabled) {
            deg = 0f
            return@DisposableEffect onDispose { }
        }
        val listener = object : OrientationEventListener(context.applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val snapped = snapUprightDegrees(orientation)
                if (snapped != deg) deg = snapped
            }
        }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
    return if (enabled) deg else 0f
}

/**
 * Snap [OrientationEventListener] degrees to the Compose rotationZ that keeps content upright.
 * Sensor 90 = device CCW (left side up) → rotate drawing CCW (−90).
 */
fun snapUprightDegrees(orientation: Int): Float = when {
    orientation in 45 until 135 -> -90f
    orientation in 135 until 225 -> 180f
    orientation in 225 until 315 -> 90f
    else -> 0f
}

/** Rotate in place. Does not change layout size or position. */
fun Modifier.keepUpright(): Modifier = composed {
    val deg = LocalUprightRotation.current
    if (deg == 0f) this else graphicsLayer { rotationZ = deg }
}

fun uprightFillSwapsAxes(deg: Float): Boolean = deg == 90f || deg == -90f

/**
 * Relayout [this] in the rotated box so a portrait stack (clock on top, date, weather)
 * still fills the original cell. Does not clip-and-spin.
 */
fun Modifier.keepUprightFill(): Modifier = composed {
    val deg = LocalUprightRotation.current
    if (deg == 0f) this
    else layout { measurable, constraints ->
        val swap = uprightFillSwapsAxes(deg)
        val childConstraints = if (swap) {
            Constraints(
                minWidth = constraints.minHeight,
                maxWidth = if (constraints.hasBoundedHeight) constraints.maxHeight else Constraints.Infinity,
                minHeight = constraints.minWidth,
                maxHeight = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity,
            )
        } else {
            constraints
        }
        val placeable = measurable.measure(childConstraints)
        val w = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
        val h = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
        layout(w, h) {
            val x = ((w - placeable.width) / 2f).roundToInt()
            val y = ((h - placeable.height) / 2f).roundToInt()
            placeable.placeRelativeWithLayer(x, y) {
                rotationZ = deg
                transformOrigin = TransformOrigin.Center
            }
        }
    }
}

/**
 * Third-party RemoteViews only look OK spun 90° when the cell is close to square.
 * One-row / one-column strips (YouTube Music now-playing, etc.) stay as drawn.
 */
fun appWidgetRotatesInPlace(spanX: Float, spanY: Float): Boolean {
    val w = kotlin.math.abs(spanX)
    val h = kotlin.math.abs(spanY)
    val short = minOf(w, h)
    val long = maxOf(w, h).coerceAtLeast(0.01f)
    if (short <= 1.01f && long >= 1.5f) return false
    return short / long >= 0.7f
}

/**
 * Rotate [content] in place and clip it to this box's original bounds so nothing
 * draws outside the widget cell. When [enabled] is false, content is left as laid out.
 */
@Composable
fun KeepUprightInBounds(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier.clipToBounds()) {
        Box(
            modifier = Modifier.fillMaxSize().then(if (enabled) Modifier.keepUpright() else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
