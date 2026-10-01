package com.madlauncher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

/** Bottom gap so the input row sits above the keyboard, never under it.
 *  Never drop below the dock gap while the keyboard is still shorter than the dock —
 *  that dip is the jumpy pill on open.
 */
fun actionBoxLiftDp(ime: Dp, nav: Dp, closedDock: Dp): Dp {
    return maxOf(closedDock, nav + 8.dp, ime + 8.dp)
}

/**
 * While the keyboard animation is running, ignore other inset reports.
 * Those reports jump to the final height (or a visible-frame guess) and fight the animation.
 */
internal fun resolveImeBottomPx(previous: Int, reported: Int, accept: Boolean): Int {
    if (!accept) return previous
    return reported.coerceAtLeast(0)
}

/** One keyboard height, then a short glide. No layout guesses, no second inset source. */
@Composable
internal fun rememberImeBottomDp(): Dp {
    val density = LocalDensity.current
    val view = LocalView.current
    var imePx by remember { mutableIntStateOf(0) }
    DisposableEffect(view) {
        var animating = false
        val imeType = WindowInsetsCompat.Type.ime()
        fun push(reported: Int, accept: Boolean) {
            imePx = resolveImeBottomPx(imePx, reported, accept)
        }
        val insetListener = androidx.core.view.OnApplyWindowInsetsListener { _, insets ->
            push(insets.getInsets(imeType).bottom, accept = !animating)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(view, insetListener)
        val animCallback = object : WindowInsetsAnimationCompat.Callback(
            WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE,
        ) {
            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                if (animation.typeMask and imeType != 0) animating = true
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>,
            ): WindowInsetsCompat {
                val imeFrame = runningAnimations.any { it.typeMask and imeType != 0 }
                if (imeFrame) {
                    animating = true
                    push(insets.getInsets(imeType).bottom, accept = true)
                }
                return insets
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                if (animation.typeMask and imeType == 0) return
                animating = false
                val settled = ViewCompat.getRootWindowInsets(view)?.getInsets(imeType)?.bottom ?: imePx
                push(settled, accept = true)
            }
        }
        ViewCompat.setWindowInsetsAnimationCallback(view, animCallback)
        push(ViewCompat.getRootWindowInsets(view)?.getInsets(imeType)?.bottom ?: 0, accept = true)
        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
            ViewCompat.setWindowInsetsAnimationCallback(view, null)
        }
    }
    val target = with(density) { imePx.toDp() }
    val glide by animateDpAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "actionBoxIme",
    )
    return glide
}
