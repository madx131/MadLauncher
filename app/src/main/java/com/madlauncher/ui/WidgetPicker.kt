package com.madlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.WidgetCatalogEntry
import com.madlauncher.ui.theme.LocalMadTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeWidgetPicker(
    vm: LauncherViewModel,
    onDismiss: () -> Unit = { vm.closeWidgetPicker() },
    modifier: Modifier = Modifier,
) {
    val theme = LocalMadTheme.current
    val c = theme.colors
    var query by remember { mutableStateOf("") }
    val catalog = vm.widgetCatalog
    val filtered by remember(catalog, query) {
        derivedStateOf {
            val q = query.trim().lowercase()
            if (q.isEmpty()) catalog
            else catalog.filter {
                it.appLabel.lowercase().contains(q) ||
                    it.widgetLabel.lowercase().contains(q) ||
                    it.packageName.lowercase().contains(q)
            }
        }
    }
    val appsOnly = vm.multiWidgetPickHost != null
    BackHandler(onBack = onDismiss)
    val builtIn = remember(filtered, appsOnly) { if (appsOnly) emptyList() else filtered.filter { it.isBuiltIn } }
    val thirdParty = remember(filtered) { filtered.filter { !it.isBuiltIn } }
    val grouped = remember(thirdParty) {
        thirdParty.groupBy { appLetter(it.appLabel) }.toSortedMap()
    }
    val letters = remember(grouped) { grouped.keys.toList() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val letterIndex = remember(grouped, builtIn) {
        buildMap {
            var index = 0
            if (builtIn.isNotEmpty()) {
                // sticky MAD header + rows — letter jump skips built-ins
                index += 1 + builtIn.size
            }
            grouped.forEach { (letter, section) ->
                put(letter, index)
                index += 1 + section.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg.copy(alpha = theme.drawerAlpha))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(c.card)
                .clickable(enabled = false) {}
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.widgets),
                    color = c.fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cancel), tint = c.fg)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.bg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.fg, fontSize = 15.sp),
                    cursorBrush = SolidColor(c.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(stringResource(R.string.search_widgets), color = c.muted, fontSize = 15.sp)
                        }
                        inner()
                    },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            when {
                vm.widgetCatalogLoading && catalog.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.primary)
                    }
                }
                filtered.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_third_party_widgets), color = c.muted)
                    }
                }
                else -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 24.dp, end = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (builtIn.isNotEmpty()) {
                                stickyHeader(key = "mad-header") {
                                    Text(
                                        stringResource(R.string.mad_widgets),
                                        color = c.accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(c.card)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                    )
                                }
                                items(builtIn, key = { it.providerKey }) { entry ->
                                    WidgetOfferRow(entry = entry, vm = vm) { vm.pickWidget(entry) }
                                }
                            }
                            grouped.forEach { (letter, section) ->
                                stickyHeader(key = "letter-$letter") {
                                    Text(
                                        letter.toString(),
                                        color = c.accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(c.card)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                    )
                                }
                                items(section, key = { it.providerKey }) { entry ->
                                    WidgetOfferRow(entry = entry, vm = vm) { vm.pickWidget(entry) }
                                }
                            }
                        }
                        LetterBar(
                            letters = letters,
                            onLetter = { ch ->
                                val target = letterIndex[ch] ?: return@LetterBar
                                scope.launch { listState.scrollToItem(target) }
                            },
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
private fun WidgetOfferRow(
    entry: WidgetCatalogEntry,
    vm: LauncherViewModel,
    onClick: () -> Unit,
) {
    val theme = LocalMadTheme.current
    val c = theme.colors
    var preview by remember(entry.providerKey) { mutableStateOf(entry.preview) }
    LaunchedEffect(entry.providerKey) {
        if (preview != null || entry.info == null) return@LaunchedEffect
        preview = withContext(Dispatchers.IO) {
            vm.loadWidgetPreviewBitmap(entry.info)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.bg)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.card)
                .border(1.dp, c.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = preview
            if (bmp != null && !bmp.isRecycled) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = entry.widgetLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                )
            } else {
                Icon(
                    Icons.Outlined.Widgets,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.widgetLabel,
                color = c.fg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.appLabel,
                color = c.muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${entry.spanX}×${entry.spanY}",
                color = c.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
