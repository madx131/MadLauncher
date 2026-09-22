package com.madlauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import com.madlauncher.data.PageEffectId
import kotlin.math.abs

enum class OverlapEffect { FADE, DEPTH, CUBE }

/**
 * Fade / Depth / Cube — pages overlap in one viewport (pager slide cancelled via [overlapTranslationX]).
 */
@Composable
fun OverlapPagerPage(
    effect: OverlapEffect,
    pageIndex: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pageOffset = pagerState.pageOffsetFor(pageIndex)
    val density = LocalDensity.current.density
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .zIndex(1f - abs(pageOffset).coerceIn(0f, 1f))
            .overlapPageLayer(effect, pageOffset, density),
    ) {
        content()
    }
}

/**
 * Card deck — neighbor pages peek scaled behind at rest; active page peels on swipe.
 */
@Composable
fun StackPagerPage(
    pageIndex: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pageOffset = pagerState.pageOffsetFor(pageIndex)
    val settled = !pagerState.isScrollInProgress
    val density = LocalDensity.current.density
    val abs = abs(pageOffset)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .zIndex(
                when {
                    abs < 0.02f -> 3f
                    settled && abs >= 0.98f -> 1f
                    else -> 2f
                },
            )
            .stackPageLayer(pageOffset, settled, density),
    ) {
        content()
    }
}

private fun PagerState.pageOffsetFor(pageIndex: Int): Float {
    return (currentPage - pageIndex) + currentPageOffsetFraction
}

/** Align page content to the viewport center (cancel HorizontalPager strip scroll). */
private fun overlapTranslationX(pageOffset: Float, width: Float): Float {
    return pageOffset * width
}

private fun Modifier.overlapPageLayer(
    effect: OverlapEffect,
    pageOffset: Float,
    density: Float,
): Modifier = graphicsLayer {
    val abs = abs(pageOffset).coerceIn(0f, 1f)
    translationX = overlapTranslationX(pageOffset, size.width)
    clip = true
    cameraDistance = 14f * density

    if (abs(pageOffset) > 1.05f) {
        alpha = 0f
        return@graphicsLayer
    }

    when (effect) {
        OverlapEffect.FADE -> {
            alpha = 1f - abs * 0.98f
            val scale = 0.93f + (1f - abs) * 0.07f
            scaleX = scale
            scaleY = scale
        }
        OverlapEffect.DEPTH -> {
            val scale = 0.68f + (1f - abs) * 0.32f
            scaleX = scale
            scaleY = scale
            alpha = 0.22f + (1f - abs) * 0.78f
        }
        OverlapEffect.CUBE -> {
            transformOrigin = TransformOrigin(
                pivotFractionX = if (pageOffset > 0f) 0f else 1f,
                pivotFractionY = 0.5f,
            )
            rotationY = pageOffset * -90f
            alpha = 1f - abs * 0.12f
        }
    }
}

private fun Modifier.stackPageLayer(
    pageOffset: Float,
    settled: Boolean,
    density: Float,
): Modifier = graphicsLayer {
    clip = true
    cameraDistance = 12f * density
    val abs = abs(pageOffset)

    if (abs > 1.15f) {
        alpha = 0f
        return@graphicsLayer
    }

    when {
        // Top card (current page).
        abs < 0.02f -> {
            translationX = overlapTranslationX(pageOffset, size.width)
            scaleX = 1f
            scaleY = 1f
            alpha = 1f
            shadowElevation = 10f
        }
        // Neighbor cards peeking behind when idle.
        settled && abs >= 0.98f -> {
            val peek = 0.14f
            translationX = overlapTranslationX(pageOffset, size.width) * (1f - peek)
            scaleX = 0.86f
            scaleY = 0.86f
            alpha = 0.78f
            shadowElevation = 4f
            transformOrigin = TransformOrigin(
                pivotFractionX = if (pageOffset > 0f) 1f else 0f,
                pivotFractionY = 0.5f,
            )
        }
        // Peel while swiping.
        else -> {
            val progress = abs.coerceIn(0f, 1f)
            translationX = overlapTranslationX(pageOffset, size.width)
            scaleX = 0.86f + (1f - progress) * 0.14f
            scaleY = scaleX
            alpha = 0.6f + (1f - progress) * 0.4f
            shadowElevation = (1f - progress) * 18f
            transformOrigin = TransformOrigin(
                pivotFractionX = if (pageOffset <= 0f) 1f else 0f,
                pivotFractionY = 0.5f,
            )
        }
    }
}

/** Maps [PageEffectId] to overlap variant when applicable. */
internal fun PageEffectId.toOverlapEffect(): OverlapEffect? = when (this) {
    PageEffectId.FADE -> OverlapEffect.FADE
    PageEffectId.DEPTH -> OverlapEffect.DEPTH
    PageEffectId.CUBE -> OverlapEffect.CUBE
    else -> null
}
