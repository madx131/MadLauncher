package com.madlauncher.ui

import android.graphics.RectF
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.HomeLayout
import com.madlauncher.data.LaunchableApp
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.theme.LocalMadTheme
import com.madlauncher.ui.theme.MadColors

@Composable
fun BottomDock(vm: LauncherViewModel) {
    val theme = LocalMadTheme.current
    val c = theme.colors
    val accent = vm.displayAppearance.actionBoxTextColor
    val pill = vm.displayAppearance.actionBoxPillColor
    val s = vm.displayAppearance
    val collapsed = s.dockAutoHide.isAutoHide && !vm.dockRevealed
    val rowHeight = s.dockHeightDp.dp.coerceIn(48.dp, 120.dp)
    val secondRow = s.dockSecondRow
    val expanded = secondRow && vm.dockSecondRowExpanded
    val dockPad = 14.dp
    val contentHeight = if (expanded) rowHeight * 2 else rowHeight
    val height by animateDpAsState(
        targetValue = if (collapsed) 10.dp else contentHeight + dockPad,
        animationSpec = tween(durationMillis = 180),
        label = "dockH",
    )
    val iconSize = s.dockIconSizeDp.dp.coerceIn(32.dp, 72.dp)

    LaunchedEffect(expanded) {
        if (!expanded) {
            val row = HomeLayout.DOCK_ROW_SLOTS
            for (i in row until row * 2) vm.dockDropRects[i].setEmpty()
        }
    }

    if (collapsed) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(c.card.copy(alpha = theme.dockAlpha))
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dy ->
                        if (dy < -8f) vm.dockRevealed = true
                    }
                }
                .clickable { vm.dockRevealed = true },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.28f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.primary.copy(alpha = 0.7f)),
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(c.card.copy(alpha = theme.dockAlpha))
            .dockSwipeGestures(
                enabled = vm.appHold == null && vm.widgetMovePreview == null,
                onPullUp = { vm.onDockPullUp() },
                onSwipeUp = { vm.onDockSwipeUp() },
                onSwipeDown = { vm.onDockSwipeDown() },
            )
            .padding(top = 6.dp, bottom = 8.dp, start = 6.dp, end = 6.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        DockIconRow(vm, vm.dockSlots, 0, c, accent, pill, iconSize, rowHeight)
        if (expanded) {
            DockIconRow(vm, vm.dockRow2Slots, HomeLayout.DOCK_ROW_SLOTS, c, accent, pill, iconSize, rowHeight)
        }
    }
}

@Composable
private fun DockIconRow(
    vm: LauncherViewModel,
    slots: List<String>,
    indexOffset: Int,
    c: MadColors,
    accent: Color,
    pill: Color,
    iconSize: Dp,
    rowHeight: Dp,
) {
    val padded = List(HomeLayout.DOCK_ROW_SLOTS) { slots.getOrNull(it).orEmpty() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        padded.forEachIndexed { index, slot ->
            val slotMod = if (slot == "actions") {
                Modifier.fillMaxHeight()
            } else {
                Modifier.weight(1f).fillMaxHeight()
            }
            DockSlotView(
                vm, indexOffset + index, slot, c, accent, pill, iconSize,
                modifier = slotMod,
            )
        }
    }
}

@Composable
private fun DockSlotView(
    vm: LauncherViewModel,
    index: Int,
    slot: String,
    c: MadColors,
    accent: Color,
    pill: Color,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val aiming = vm.appHold != null && vm.dragMoved
    val hovered = vm.dragDropKey == "dock:$index"
    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                vm.dockDropRects[index] = RectF(b.left, b.top, b.right, b.bottom)
            }
            .then(if (aiming) Modifier.padding(horizontal = 3.dp, vertical = 2.dp) else Modifier)
            .dropAimTile(aiming, hovered, c.primary)
            .then(if (aiming) Modifier.padding(3.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when (slot) {
            "actions" -> ActionsPill(vm, index, c, accent, pill, iconSize)
            "apps" -> SpecialDockIcon(
                vm, index, Icons.Outlined.Apps, stringResource(R.string.drawer_title),
                accent, slot, c.muted, iconSize, editDocksOnLongPress = true,
            )
            "launcher_settings", "settings" -> SpecialDockIcon(
                vm, index, Icons.Outlined.Settings,
                stringResource(R.string.launcher_settings),
                accent, "launcher_settings", c.muted, iconSize,
            )
            "phone", "browser" -> {
                val app = vm.resolveDockApp(index)
                if (app != null) AppDockIcon(vm, index, app, c, iconSize)
                else EmptyDockSlot(c, iconSize)
            }
            else -> {
                val app = vm.resolveDockApp(index)
                if (app != null) AppDockIcon(vm, index, app, c, iconSize)
                else EmptyDockSlot(c, iconSize)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpecialDockIcon(
    vm: LauncherViewModel,
    index: Int,
    icon: ImageVector,
    label: String,
    color: Color,
    slot: String,
    muted: Color,
    iconSize: Dp,
    editDocksOnLongPress: Boolean = false,
) {
    val dummy = LaunchableApp(label, slot, slot, null)
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val holdModifier = if (editDocksOnLongPress) {
        Modifier.combinedClickable(
            onClick = { vm.activateDockSlot(index) },
            onLongClick = { vm.openEditDocksFromControl() },
        )
    } else {
        Modifier.appLongPress(
            key = slot + index,
            coordinates = coords,
            onPositioned = { coords = it },
            onTap = { vm.activateDockSlot(index) },
            onHoldStart = { vm.beginAppHold(dummy, "dock", dockIndex = index) },
            onMove = { x, y, moved -> vm.updateHoldPointer(x, y, moved) },
            onHoldEnd = { vm.endAppHold() },
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = holdModifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.keepUpright(),
        ) {
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(iconSize * 0.72f))
            }
            Spacer(Modifier.height(4.dp))
            Text(label, color = muted, fontSize = 10.sp, letterSpacing = 0.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AppDockIcon(vm: LauncherViewModel, index: Int, app: LaunchableApp, c: MadColors, iconSize: Dp) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.appLongPress(
            key = app.id + index,
            coordinates = coords,
            onPositioned = { coords = it },
            onTap = { vm.activateDockSlot(index) },
            onHoldStart = { vm.beginAppHold(app, "dock", dockIndex = index) },
            onMove = { x, y, moved -> vm.updateHoldPointer(x, y, moved) },
            onHoldEnd = { vm.endAppHold() },
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.keepUpright(),
        ) {
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center,
            ) {
                DesktopIcon(vm, app, iconSize, rotateUpright = false)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                app.label,
                color = c.muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyDockSlot(c: MadColors, iconSize: Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(iconSize))
        Spacer(Modifier.height(4.dp))
        Text(" ", color = c.muted, fontSize = 10.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionsPill(vm: LauncherViewModel, index: Int, c: MadColors, text: Color, pill: Color, iconSize: Dp) {
    val actionsLabel = stringResource(R.string.dock_actions)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(
            onClick = { if (!vm.homeEditMode) vm.openActionBox(showCommandMenu = true) },
            onLongClick = { vm.openEditDocksFromControl() },
        ),
    ) {
        Row(
            modifier = Modifier
                .keepUpright()
                .height(iconSize)
                .clip(RoundedCornerShape(iconSize / 2))
                .background(pill.copy(alpha = 0.22f))
                .border(1.dp, pill.copy(alpha = 0.55f), RoundedCornerShape(iconSize / 2))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.Search, contentDescription = actionsLabel, tint = text, modifier = Modifier.size(16.dp))
            Text(
                actionsLabel,
                color = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(" ", color = c.muted, fontSize = 10.sp)
    }
}
