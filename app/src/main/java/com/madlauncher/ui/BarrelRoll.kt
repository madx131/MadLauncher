package com.madlauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

const val BARREL_ROLL_MS = 1600

/** Google-style 360° spin when [generation] increments. */
fun Modifier.barrelRoll(generation: Int): Modifier = composed {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(generation) {
        if (generation <= 0) return@LaunchedEffect
        anim.snapTo(0f)
        anim.animateTo(
            360f,
            animationSpec = tween(BARREL_ROLL_MS, easing = FastOutSlowInEasing),
        )
        anim.snapTo(0f)
    }
    graphicsLayer { rotationZ = anim.value }
}
