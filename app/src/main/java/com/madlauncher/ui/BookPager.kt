package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlin.math.abs

/**
 * Book-style page turn: static page underneath, top leaf rotates 0..180 around the left spine.
 * Requires [translationX] cancel on pager drift (see [bookPageLayer]).
 */
@Composable
fun BookPagerPage(
    pageIndex: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
    val current = pagerState.currentPage
    val density = LocalDensity.current

    val forwardTurn = pageIndex == current && pageOffset < -0.001f
    val backwardTurn = pageIndex == current && pageOffset > 0.001f
    val forwardUnder = pageIndex == current + 1 && pageOffset < -0.001f
    val backwardUnder = pageIndex == current - 1 && pageOffset > 0.001f
    val isTurning = forwardTurn || backwardTurn

    val turnProgress = when {
        forwardTurn -> (-pageOffset).coerceIn(0f, 1f)
        backwardTurn -> pageOffset.coerceIn(0f, 1f)
        else -> 0f
    }
    val pivotLeft = forwardTurn || forwardUnder

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .zIndex(
                when {
                    isTurning -> 3f
                    forwardUnder || backwardUnder -> 1f
                    pageIndex == current -> 2f
                    else -> 0f
                },
            )
            .bookPageLayer(
                pageIndex = pageIndex,
                currentPage = current,
                pageOffset = pageOffset,
                density = density.density,
            ),
    ) {
        content()
        if (isTurning && turnProgress > 0.5f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2C2824).copy(alpha = ((turnProgress - 0.5f) * 2f).coerceIn(0f, 1f))),
            )
        }
        if (isTurning) {
            BookFoldShadow(
                progress = turnProgress,
                spineOnLeft = pivotLeft,
            )
        }
    }
}

@Composable
private fun BookFoldShadow(
    progress: Float,
    spineOnLeft: Boolean,
) {
    if (progress <= 0.01f || progress >= 0.99f) return
    val foldStrength = (1f - abs(progress - 0.5f) * 2f).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .align(if (spineOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .graphicsLayer { alpha = 0.65f * foldStrength }
                .background(
                    if (spineOnLeft) {
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        )
                    },
                ),
        )
    }
}

private fun Modifier.bookPageLayer(
    pageIndex: Int,
    currentPage: Int,
    pageOffset: Float,
    density: Float,
): Modifier = graphicsLayer {
    // Cancel HorizontalPager sideways drift so pages overlap in one viewport.
    translationX = pageOffset * size.width
    clip = true
    cameraDistance = 11f * density

    when {
        // Swipe to next page: current leaf turns away from left spine.
        pageIndex == currentPage && pageOffset < 0f -> {
            val progress = (-pageOffset).coerceIn(0f, 1f)
            transformOrigin = TransformOrigin(0f, 0.5f)
            rotationY = -180f * progress
            alpha = if (progress > 0.5f) 0f else 1f
        }
        // Next page stays flat underneath while turning.
        pageIndex == currentPage + 1 && pageOffset < 0f -> {
            rotationY = 0f
            alpha = 1f
        }
        // Swipe to previous page: current leaf turns from right spine.
        pageIndex == currentPage && pageOffset > 0f -> {
            val progress = pageOffset.coerceIn(0f, 1f)
            transformOrigin = TransformOrigin(1f, 0.5f)
            rotationY = 180f * progress
            alpha = if (progress > 0.5f) 0f else 1f
        }
        pageIndex == currentPage - 1 && pageOffset > 0f -> {
            rotationY = 0f
            alpha = 1f
        }
        pageIndex == currentPage && abs(pageOffset) < 0.001f -> {
            rotationY = 0f
            alpha = 1f
        }
        abs(pageOffset) >= 1f -> alpha = 0f
        else -> alpha = 0f
    }
}
