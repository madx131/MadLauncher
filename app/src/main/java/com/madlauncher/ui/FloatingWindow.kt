package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.madlauncher.R
import com.madlauncher.ui.theme.LocalMadTheme
import com.madlauncher.ui.theme.MadSurfaceKind
import com.madlauncher.ui.theme.madSurface
import kotlin.math.roundToInt

data class WindowGeom(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
)

@Composable
fun FloatingWindow(
    title: String,
    geom: WindowGeom,
    onGeomChange: (WindowGeom) -> Unit,
    onClose: () -> Unit,
    zIndex: Float = 10f,
    titleActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val theme = LocalMadTheme.current
    val config = LocalConfiguration.current
    val screenW = with(density) { config.screenWidthDp.dp.toPx() }
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }
    val minW = with(density) { 240.dp.toPx() }
    val minH = with(density) { 140.dp.toPx() }
    val maxW = (screenW - with(density) { 16.dp.toPx() }).coerceAtLeast(minW)
    val maxH = (screenH * 0.82f).coerceAtLeast(minH)

    var latest by remember { mutableStateOf(geom) }
    latest = geom

    // Portrait → landscape: clamp stored geom so windows fit and coerceIn never inverts.
    LaunchedEffect(screenW, screenH, maxW, maxH) {
        val clamped = latest.copy(
            w = latest.w.coerceIn(minW, maxW),
            h = latest.h.coerceIn(minH, maxH),
        ).let { g ->
            g.copy(
                x = g.x.coerceIn(0f, (screenW - g.w).coerceAtLeast(0f)),
                y = g.y.coerceIn(0f, (screenH - 80f - g.h).coerceAtLeast(0f)),
            )
        }
        if (clamped != latest) {
            latest = clamped
            onGeomChange(clamped)
        }
    }

    fun update(transform: (WindowGeom) -> WindowGeom) {
        val next = transform(latest)
        latest = next
        onGeomChange(next)
    }

    Box(
        modifier = Modifier
            .zIndex(zIndex)
            .offset { IntOffset(latest.x.roundToInt(), latest.y.roundToInt()) }
            .width(with(density) { latest.w.toDp() })
            .height(with(density) { latest.h.toDp() })
            .shadow(20.dp, RoundedCornerShape(16.dp))
            .madSurface(MadSurfaceKind.Window, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            update { g ->
                                val nx = (g.x + drag.x).coerceIn(0f, (screenW - g.w).coerceAtLeast(0f))
                                val ny = (g.y + drag.y).coerceIn(0f, (screenH - 80f - g.h).coerceAtLeast(0f))
                                g.copy(x = nx, y = ny)
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(theme.colors.border.copy(alpha = 0.7f)),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    color = theme.colors.fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                titleActions()
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.window_close), tint = theme.colors.muted)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 14.dp),
                content = content,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(10.dp)
                .padding(top = 40.dp, bottom = 18.dp)
                .background(theme.colors.border.copy(alpha = 0.22f))
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        update { g -> g.copy(w = (g.w + drag.x).coerceIn(minW, maxW)) }
                    }
                },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(10.dp)
                .padding(start = 18.dp, end = 18.dp)
                .background(theme.colors.border.copy(alpha = 0.22f))
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        update { g -> g.copy(h = (g.h + drag.y).coerceIn(minH, maxH)) }
                    }
                },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(22.dp)
                .background(theme.colors.primary.copy(alpha = 0.35f), RoundedCornerShape(topStart = 8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        update { g ->
                            g.copy(
                                w = (g.w + drag.x).coerceIn(minW, maxW),
                                h = (g.h + drag.y).coerceIn(minH, maxH),
                            )
                        }
                    }
                },
        )
    }
}
