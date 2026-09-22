package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.DrawerSortId
import com.madlauncher.data.LaunchableApp
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.theme.LocalMadTheme
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps = vm.drawerApps
    val loading = vm.loadingApps
    val query = remember { mutableStateOf("") }
    val sort = vm.displayAppearance.drawerSort
    val sorted = remember(apps, query.value, sort) {
        val q = query.value.trim()
        val source = if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
        vm.sortDrawerApps(source)
    }
    val grouped = remember(sorted, sort) {
        if (sort == DrawerSortId.MOST_USED) {
            linkedMapOf(' ' to sorted)
        } else {
            groupAppsByLetter(sorted)
        }
    }
    val showRecentRow = query.value.isEmpty() && vm.drawerRecent.isNotEmpty()
    val recentHeaderCount = if (showRecentRow) 1 else 0
    val letters = remember(grouped, sort) {
        if (sort == DrawerSortId.MOST_USED) emptyList()
        else grouped.keys.toList()
    }
    val gridState = rememberLazyGridState()
    val iconSize = homeIconSize(vm.displayAppearance.homeIconSizeDp)
    val gridLetterIndex = remember(sorted, sort, recentHeaderCount) {
        if (sort == DrawerSortId.MOST_USED) emptyMap()
        else {
            var index = recentHeaderCount
            buildMap {
                groupAppsByLetter(sorted).forEach { (letter, section) ->
                    put(letter, index)
                    index += 1 + section.size
                }
            }
        }
    }
    val scope = rememberCoroutineScope()
    fun jumpTo(letter: Char) {
        val target = gridLetterIndex[letter] ?: return
        scope.launch { gridState.scrollToItem(target) }
    }

    val theme = LocalMadTheme.current
    val c = theme.colors
    val holding = vm.appHold?.from == "drawer"
    val pinning = holding && vm.dragMoved
    var sortMenuOpen by remember { mutableStateOf(false) }
    val dismissDrawer = rememberUpdatedState(onDismiss)
    val pullDownClose = remember {
        object : NestedScrollConnection {
            private var pulled = 0f
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > 72f) {
                        pulled = 0f
                        dismissDrawer.value()
                    }
                } else if (available.y <= 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullDownClose)
            .background(c.bg.copy(alpha = if (pinning) 0f else theme.drawerAlpha))
            .clickable(enabled = !holding, onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(top = 48.dp)
                .graphicsLayer {
                    // Fade only — translating this sheet breaks localToWindow and the drag ghost.
                    alpha = if (pinning) 0f else 1f
                }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(c.card.copy(alpha = if (pinning) 0f else theme.drawerAlpha))
                .clickable(enabled = false) {}
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.drawer_title),
                    color = c.fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { sortMenuOpen = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(
                            drawerSortLabel(sort),
                            color = c.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Icon(
                            Icons.Outlined.ArrowDropDown,
                            contentDescription = "Sort",
                            tint = c.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false },
                    ) {
                        DrawerSortId.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        drawerSortLabel(option),
                                        color = if (option == sort) c.primary else c.fg,
                                        fontWeight = if (option == sort) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    vm.setDrawerSort(option)
                                    sortMenuOpen = false
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.drawer_close), tint = c.fg)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.bg.copy(alpha = theme.drawerAlpha))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                BasicTextField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.fg, fontSize = 15.sp),
                    cursorBrush = SolidColor(c.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.value.isEmpty()) {
                            Text(stringResource(R.string.search_apps), color = c.muted, fontSize = 15.sp)
                        }
                        inner()
                    },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.primary)
                    }
                }
                sorted.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_apps_found), color = c.muted)
                    }
                }
                else -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = iconGridMinCell(iconSize)),
                            state = gridState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 24.dp, end = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (showRecentRow) {
                                item(
                                    key = "drawer-recent",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    DrawerRecentRow(vm = vm, iconSize = iconSize, fg = c.fg, accent = c.primary)
                                }
                            }
                            if (sort == DrawerSortId.MOST_USED) {
                                items(sorted, key = { it.id }) { app ->
                                    AppGridCell(vm = vm, app = app, iconSize = iconSize, fg = c.fg)
                                }
                            } else {
                                grouped.forEach { (letter, section) ->
                                    item(
                                        key = "letter-$letter",
                                        span = { GridItemSpan(maxLineSpan) },
                                    ) {
                                        Text(
                                            letter.toString(),
                                            color = c.accent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                        )
                                    }
                                    items(section, key = { it.id }) { app ->
                                        AppGridCell(vm = vm, app = app, iconSize = iconSize, fg = c.fg)
                                    }
                                }
                            }
                        }
                        LetterBar(
                            letters = letters,
                            onLetter = ::jumpTo,
                            primary = c.primary,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(22.dp)
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LetterBar(
    letters: List<Char>,
    onLetter: (Char) -> Unit,
    primary: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) return
    Box(
        modifier = modifier.pointerInput(letters) {
            fun letterAt(y: Float): Char {
                val h = size.height.coerceAtLeast(1)
                val i = (y / h * letters.size).toInt().coerceIn(0, letters.lastIndex)
                return letters[i]
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                onLetter(letterAt(down.position.y))
                drag(down.id) { change ->
                    onLetter(letterAt(change.position.y))
                    change.consume()
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { letter ->
                Text(
                    letter.toString(),
                    color = primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onLetter(letter) },
                )
            }
        }
    }
}

internal fun appLetter(label: String): Char {
    val ch = label.firstOrNull { it.isLetterOrDigit() } ?: return '#'
    return if (ch.isLetter()) ch.uppercaseChar() else '#'
}

/** LinkedHashMap grouped A–Z — avoids [SortedMap]/[java.util.SequencedMap] (API 35+) which crashes on older devices. */
private fun groupAppsByLetter(apps: List<LaunchableApp>): LinkedHashMap<Char, List<LaunchableApp>> {
    val grouped = apps.groupBy { appLetter(it.label) }
    return LinkedHashMap<Char, List<LaunchableApp>>(grouped.size).apply {
        grouped.keys.sorted().forEach { key ->
            put(key, grouped.getValue(key))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DrawerRecentRow(
    vm: LauncherViewModel,
    iconSize: androidx.compose.ui.unit.Dp,
    fg: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
) {
    val cellW = iconGridMinCell(iconSize)
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            stringResource(R.string.drawer_recent_row),
            color = fg.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            vm.drawerRecent.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(cellW)
                        .keepUpright()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { vm.launchApp(item.app) }
                        .padding(vertical = 4.dp),
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        DesktopIcon(vm, item.app, iconSize, rotateUpright = false)
                        if (item.newlyInstalled) {
                            Text(
                                stringResource(R.string.drawer_recent_new),
                                color = accent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LocalMadTheme.current.colors.card)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        item.app.label,
                        color = fg,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun drawerSortLabel(sort: DrawerSortId): String = when (sort) {
    DrawerSortId.NAME_AZ -> "A–Z"
    DrawerSortId.NAME_ZA -> "Z–A"
    DrawerSortId.MOST_USED -> "Most used"
}

@Composable
private fun AppGridCell(
    vm: LauncherViewModel,
    app: LaunchableApp,
    iconSize: androidx.compose.ui.unit.Dp,
    fg: androidx.compose.ui.graphics.Color,
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .appLongPress(
                key = "grid/${app.id}",
                coordinates = coords,
                onPositioned = { coords = it },
                onTap = { vm.launchApp(app) },
                onHoldStart = { vm.beginAppHold(app, "drawer") },
                onMove = { x, y, moved -> vm.updateHoldPointer(x, y, moved) },
                onHoldEnd = { vm.endAppHold() },
            )
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.keepUpright(),
        ) {
            DesktopIcon(vm, app, iconSize, rotateUpright = false)
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                app.label,
                color = fg,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
