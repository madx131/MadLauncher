package com.madlauncher.ui

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
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

/** Keyboard height from IME insets; ignore tiny visible-frame guesses that yank the field down. */
@Composable
internal fun rememberImeBottomDp(): Dp {
    val density = LocalDensity.current
    val composeIme = WindowInsets.ime.getBottom(density)
    val view = LocalView.current
    var viewIme by remember { mutableIntStateOf(0) }
    DisposableEffect(view) {
        fun readIme(): Int {
            val inset = ViewCompat.getRootWindowInsets(view)
                ?.getInsets(WindowInsetsCompat.Type.ime())
                ?.bottom
                ?: 0
            if (inset > 0) return inset
            val visible = Rect()
            view.getWindowVisibleDisplayFrame(visible)
            val guess = (view.rootView.height - visible.bottom).coerceAtLeast(0)
            return if (guess > 80) guess else 0
        }
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { viewIme = readIme() }
        view.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        val insetListener = androidx.core.view.OnApplyWindowInsetsListener { _, insets ->
            viewIme = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom.let { ime ->
                if (ime > 0) ime else readIme()
            }
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(view, insetListener)
        val animCallback = object : WindowInsetsAnimationCompat.Callback(
            WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE,
        ) {
            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>,
            ): WindowInsetsCompat {
                viewIme = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                return insets
            }
        }
        ViewCompat.setWindowInsetsAnimationCallback(view, animCallback)
        viewIme = readIme()
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
            ViewCompat.setWindowInsetsAnimationCallback(view, null)
        }
    }
    return with(density) { maxOf(composeIme, viewIme).toDp() }
}
