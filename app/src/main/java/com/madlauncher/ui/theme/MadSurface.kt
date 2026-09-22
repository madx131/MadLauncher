package com.madlauncher.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

enum class MadSurfaceKind { Window, Drawer, Widget }

@Composable
fun Modifier.madSurface(
    kind: MadSurfaceKind = MadSurfaceKind.Window,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
): Modifier {
    val theme = LocalMadTheme.current
    val alpha = when (kind) {
        MadSurfaceKind.Window -> theme.windowAlpha
        MadSurfaceKind.Drawer -> theme.drawerAlpha
        MadSurfaceKind.Widget -> theme.widgetAlpha
    }
    val blur = theme.blurEnabled && (kind != MadSurfaceKind.Widget || theme.blurAllScreens)
    return this
        .clip(shape)
        .then(
            if (blur && Build.VERSION.SDK_INT >= 31) {
                Modifier.graphicsLayer {
                    renderEffect = RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            } else Modifier,
        )
        .background(theme.colors.card.copy(alpha = alpha))
        .border(1.dp, theme.colors.border.copy(alpha = (alpha * 0.9f).coerceIn(0.1f, 1f)), shape)
}
