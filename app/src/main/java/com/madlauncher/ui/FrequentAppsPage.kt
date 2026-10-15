package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.HomeLayout
import com.madlauncher.data.HomePage
import com.madlauncher.data.LaunchableApp
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.theme.LocalMadTheme

@Composable
fun FrequentAppsPage(
    vm: LauncherViewModel,
    page: HomePage,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(page.id, page.frequentSortTop, vm.apps, vm.displayAppearance.homeGridSize) {
        vm.refreshFrequentApps()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .homePageContextMenu(
                enabled = !vm.homeEditMode && vm.editingWidgetId == null && vm.appHold == null,
                onLongPress = { vm.enterHomeEdit() },
            ),
    ) {
        Text(stringResource(R.string.frequent_title), color = LocalMadTheme.current.colors.fg, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip(
                label = stringResource(R.string.frequent_sort_top),
                selected = page.frequentSortTop,
                onClick = { if (!page.frequentSortTop) vm.toggleFrequentSort() },
            )
            SortChip(
                label = stringResource(R.string.frequent_sort_bottom),
                selected = !page.frequentSortTop,
                onClick = { if (page.frequentSortTop) vm.toggleFrequentSort() },
            )
        }
        if (!vm.hasUsageAccess) {
            Text(
                stringResource(R.string.frequent_usage_access_hint),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { vm.openUsageAccessSettings() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        if (vm.frequentApps.isEmpty()) {
            Text(
                stringResource(R.string.frequent_empty),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 28.dp),
            )
        } else {
            val fromBottom = !page.frequentSortTop
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                val grid = HomeLayout.normalizeGridSize(vm.displayAppearance.homeGridSize)
                val shown = remember(vm.frequentApps, grid) {
                    vm.frequentApps.take(HomeLayout.frequentCapacity(grid))
                }
                val cellW = maxWidth / grid
                val cellH = maxHeight / grid
                val iconSize = emptyPageIconSize(vm.displayAppearance.homeIconSizeDp, cellW, cellH)
                Box(Modifier.fillMaxSize()) {
                    for (row in 0 until grid) {
                        for (col in 0 until grid) {
                            val idx = if (fromBottom) {
                                (grid - 1 - row) * grid + col
                            } else {
                                row * grid + col
                            }
                            val app = shown.getOrNull(idx) ?: continue
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = cellW * col, top = cellH * row)
                                    .size(cellW, cellH),
                                contentAlignment = Alignment.Center,
                            ) {
                                FrequentAppCell(vm, app, iconSize)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) LocalMadTheme.current.colors.fg else LocalMadTheme.current.colors.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) LocalMadTheme.current.colors.primary.copy(alpha = 0.35f) else LocalMadTheme.current.colors.card)
            .border(1.dp, if (selected) LocalMadTheme.current.colors.accent else LocalMadTheme.current.colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun FrequentAppCell(vm: LauncherViewModel, app: LaunchableApp, iconSize: androidx.compose.ui.unit.Dp) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appLongPress(
                key = "freq/${app.id}",
                coordinates = coords,
                onPositioned = { coords = it },
                onTap = { vm.launchApp(app) },
                onHoldStart = { vm.beginAppHold(app, "frequent") },
                onMove = { x, y, moved -> vm.updateHoldPointer(x, y, moved) },
                onHoldEnd = { vm.endAppHold() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.keepUpright(),
        ) {
            DesktopIcon(vm, app, iconSize, rotateUpright = false)
            Spacer(Modifier.height(4.dp))
            Text(
                app.label,
                color = LocalMadTheme.current.colors.fg,
                fontSize = iconLabelSize(vm.displayAppearance.iconLabelSizeSp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
