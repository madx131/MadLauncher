package com.madlauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.DndController
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.QsTileSpec
import com.madlauncher.data.ToggleItem
import com.madlauncher.ui.theme.LocalMadTheme

private val QsActiveGreen = Color(0xFF2E7D32)
private val QsInactiveBlack = Color(0xFF000000)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSettingsPanel(vm: LauncherViewModel) {
    FloatingWindow(
        title = if (vm.qsEditing) stringResource(R.string.qs_edit_title) else stringResource(R.string.qs_title),
        geom = vm.qsGeom,
        onGeomChange = { vm.saveWindow("qs", it) },
        onClose = { vm.toggleQuickSettings() },
        zIndex = 21f,
        titleActions = {
            IconButton(onClick = { vm.toggleQsEditing() }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (vm.qsEditing) Icons.Outlined.Check else Icons.Outlined.Edit,
                    contentDescription = if (vm.qsEditing) stringResource(R.string.qs_done_desc) else stringResource(R.string.qs_edit_desc),
                    tint = if (vm.qsEditing) LocalMadTheme.current.colors.accent else LocalMadTheme.current.colors.muted,
                )
            }
        },
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (vm.qsEditing) {
                Text(
                    if (vm.qsReplaceId == null) {
                        stringResource(R.string.qs_hint_tap_replace)
                    } else {
                        stringResource(R.string.qs_hint_tap_available)
                    },
                    color = LocalMadTheme.current.colors.muted,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(10.dp))
            }
            val rows = vm.toggles.chunked(3)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { toggle ->
                        Box(modifier = Modifier.weight(1f)) {
                            ToggleTile(
                                toggle = toggle,
                                editing = vm.qsEditing,
                                selected = vm.qsReplaceId == toggle.id,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { vm.toggle(toggle.id) },
                                onLongClick = if (toggle.id == "dnd" && !vm.qsEditing) {
                                    { vm.openDndDurationMenu() }
                                } else {
                                    null
                                },
                                onMoveLeft = { vm.moveQsTile(toggle.id, -1) },
                                onMoveRight = { vm.moveQsTile(toggle.id, 1) },
                                onRemove = { vm.removeQsTile(toggle.id) },
                            )
                            if (toggle.id == "dnd") {
                                DropdownMenu(
                                    expanded = vm.dndDurationMenuOpen,
                                    onDismissRequest = { vm.closeDndDurationMenu() },
                                ) {
                                    Text(
                                        "Do Not Disturb for…",
                                        color = LocalMadTheme.current.colors.muted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                    DndController.durationOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(option.label, color = LocalMadTheme.current.colors.fg, fontSize = 14.sp)
                                            },
                                            onClick = { vm.setDndDuration(option.minutes) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (vm.qsEditing) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.qs_available),
                    color = LocalMadTheme.current.colors.fg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                val available = vm.availableQsTiles
                if (available.isEmpty()) {
                    Text(stringResource(R.string.qs_all_showing), color = LocalMadTheme.current.colors.muted, fontSize = 11.sp)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        available.forEach { spec ->
                            AvailableChip(spec) { vm.addQsTile(spec.id) }
                        }
                    }
                }
            } else if (vm.toggles.any { it.id == "dnd" }) {
                Text(
                    "Long-press Do Not Disturb to set a timer (up to 12 hours)",
                    color = LocalMadTheme.current.colors.muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToggleTile(
    toggle: ToggleItem,
    editing: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit,
) {
    val on = toggle.enabled && !editing
    val bg = if (on) QsActiveGreen else QsInactiveBlack
    val fg = Color.White
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(
                if (selected) Modifier.border(2.dp, LocalMadTheme.current.colors.yellow, RoundedCornerShape(16.dp))
                else Modifier,
            )
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                },
            )
            .padding(top = 12.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                qsIcon(toggle.id),
                contentDescription = toggle.label,
                tint = fg,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.Center),
            )
            if (editing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(LocalMadTheme.current.colors.bg.copy(alpha = 0.85f))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.qs_remove_desc), tint = LocalMadTheme.current.colors.muted, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            toggle.label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (editing) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.qs_move_left),
                    tint = fg.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onMoveLeft),
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.qs_move_right),
                    tint = fg.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onMoveRight),
                )
            }
        }
    }
}

@Composable
private fun AvailableChip(spec: QsTileSpec, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LocalMadTheme.current.colors.bg)
            .border(1.dp, LocalMadTheme.current.colors.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onAdd)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = LocalMadTheme.current.colors.accent, modifier = Modifier.size(14.dp))
        Icon(qsIcon(spec.id), contentDescription = null, tint = LocalMadTheme.current.colors.fg, modifier = Modifier.size(16.dp))
        Text(spec.label, color = LocalMadTheme.current.colors.fg, fontSize = 12.sp)
    }
}
