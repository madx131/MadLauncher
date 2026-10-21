package com.madlauncher.ui

import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.madlauncher.data.EdgePanel
import com.madlauncher.data.EdgeSide
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.theme.LocalMadTheme

@Composable
fun EdgePanels(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    // Outer box must not steal home gestures: children are edge-sized only (no fillMaxSize wrappers).
    Box(modifier = modifier.fillMaxSize()) {
        vm.edgePanels.forEach { panel ->
            when (panel.side) {
                EdgeSide.LEFT -> SideEdge(vm, panel, Alignment.CenterStart, left = true)
                EdgeSide.RIGHT -> SideEdge(vm, panel, Alignment.CenterEnd, left = false)
                EdgeSide.TOP -> TopEdge(vm, panel)
            }
        }
    }
}

@Composable
private fun BoxScope.SideEdge(
    vm: LauncherViewModel,
    panel: EdgePanel,
    alignment: Alignment,
    left: Boolean,
) {
    val theme = LocalMadTheme.current
    val showPanel = vm.edgeShouldShowExpanded(panel)
    // Width follows the size slider; icons shrink to fit inside the strip.
    val thickness = panel.thicknessDp.dp.coerceIn(48.dp, 160.dp)
    val iconSize = edgeIconDp(panel.iconSizeDp, panel.thicknessDp).dp
    val shape = if (left) {
        RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
    }
    if (!showPanel) {
        Box(
            modifier = Modifier
                .align(alignment)
                .width(14.dp)
                .height(96.dp)
                .clip(shape)
                .background(theme.colors.primary.copy(alpha = 0.88f))
                .pointerInput(panel.side, left) {
                    detectHorizontalDragGestures { _, dx ->
                        if ((left && dx > 8f) || (!left && dx < -8f)) {
                            vm.toggleEdgeExpanded(panel.side)
                        }
                    }
                }
                .clickable { vm.toggleEdgeExpanded(panel.side) }
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    vm.edgeDropRects["${panel.side.key}:handle"] =
                        RectF(b.left, b.top, b.right, b.bottom)
                },
        )
    } else {
        Column(
            modifier = Modifier
                .align(alignment)
                .width(thickness)
                .shadow(12.dp, shape)
                .clip(shape)
                .background(theme.colors.card.copy(alpha = 0.96f))
                .border(1.dp, theme.colors.border, shape)
                .padding(vertical = 10.dp, horizontal = 8.dp)
                .then(
                    if (panel.autoHide.isAutoHide) {
                        Modifier.clickable { /* keep open */ }
                    } else Modifier,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            panel.slots.forEachIndexed { index, _ ->
                EdgeSlot(vm, panel.side, index, iconSize)
            }
            if (panel.autoHide.isAutoHide) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(theme.colors.primary.copy(alpha = 0.5f))
                        .clickable { vm.toggleEdgeExpanded(panel.side) },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.TopEdge(vm: LauncherViewModel, panel: EdgePanel) {
    val theme = LocalMadTheme.current
    val showPanel = vm.edgeShouldShowExpanded(panel)
    val thickness = panel.thicknessDp.dp.coerceIn(48.dp, 160.dp)
    val iconSize = edgeIconDp(panel.iconSizeDp, panel.thicknessDp).dp
    if (!showPanel) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .width(96.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(theme.colors.primary.copy(alpha = 0.88f))
                .pointerInput(panel.side) {
                    detectVerticalDragGestures { _, dy ->
                        if (dy > 8f) vm.toggleEdgeExpanded(panel.side)
                    }
                }
                .clickable { vm.toggleEdgeExpanded(panel.side) }
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    vm.edgeDropRects["${panel.side.key}:handle"] =
                        RectF(b.left, b.top, b.right, b.bottom)
                },
        )
    } else {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .height(thickness)
                .shadow(12.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(theme.colors.card.copy(alpha = 0.96f))
                .border(1.dp, theme.colors.border, RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            panel.slots.forEachIndexed { index, _ ->
                EdgeSlot(vm, panel.side, index, iconSize)
            }
            if (panel.autoHide.isAutoHide) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(theme.colors.primary.copy(alpha = 0.5f))
                        .clickable { vm.toggleEdgeExpanded(panel.side) },
                )
            }
        }
    }
}

@Composable
private fun EdgeSlot(vm: LauncherViewModel, side: EdgeSide, index: Int, iconSize: Dp) {
    val key = "${side.key}:$index"
    val app = vm.resolveEdgeApp(side, index)
    val highlight = vm.dragDropKey == key
    val theme = LocalMadTheme.current
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val slotSize = (iconSize + 12.dp).coerceAtLeast(40.dp)
    Box(
        modifier = Modifier
            .size(slotSize)
            .onGloballyPositioned {
                coords = it
                val b = it.boundsInWindow()
                vm.edgeDropRects[key] = RectF(b.left, b.top, b.right, b.bottom)
            }
            .clip(CircleShape)
            .background(
                if (highlight) theme.colors.primary.copy(alpha = 0.28f)
                else theme.colors.primary.copy(alpha = 0.10f),
            )
            .then(
                if (highlight) Modifier.border(2.dp, theme.colors.primary, CircleShape)
                else Modifier.border(1.dp, theme.colors.border, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (app != null) {
            Box(
                modifier = Modifier.appLongPress(
                    key = "$key/${app.id}",
                    coordinates = coords,
                    onPositioned = { coords = it },
                    onTap = { vm.launchApp(app) },
                    onHoldStart = {
                        vm.beginAppHold(app, "edge", dockIndex = index, edgeSide = side)
                    },
                    onMove = { x, y, moved -> vm.updateHoldPointer(x, y, moved) },
                    onHoldEnd = { vm.endAppHold() },
                ),
            ) {
                DesktopIcon(vm, app, iconSize, showBadge = true)
            }
        }
    }
}
