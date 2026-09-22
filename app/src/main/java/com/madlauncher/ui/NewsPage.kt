package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.madlauncher.R
import com.madlauncher.data.HomePage
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.NewsCatalog
import com.madlauncher.data.NewsProvider
import com.madlauncher.data.NewsStory
import com.madlauncher.ui.theme.LocalMadTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun NewsPage(
    vm: LauncherViewModel,
    page: HomePage,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMadTheme.current
    val c = theme.colors
    LaunchedEffect(page.id, page.newsProviders) {
        vm.refreshNewsProviders()
        vm.refreshNews()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.news_title), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.news_sources_button),
                color = c.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { vm.openNewsPicker() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        val selected = vm.newsProviders.filter { it.id in page.newsProviders }
        Text(
            if (selected.isEmpty()) stringResource(R.string.news_pick_sources)
            else selected.joinToString(" · ") { it.name },
            color = c.muted,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        when {
            vm.newsLoading && vm.newsStories.isEmpty() -> {
                Text(stringResource(R.string.news_loading), color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 24.dp))
            }
            vm.newsStories.isEmpty() -> {
                Text(
                    stringResource(R.string.news_no_stories),
                    color = c.muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(vm.newsStories, key = { it.id }) { story ->
                        NewsCard(story, theme) { vm.openNewsStory(story) }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(story: NewsStory, theme: com.madlauncher.ui.theme.MadThemeSpec, onClick: () -> Unit) {
    val whenLabel = rememberTime(story.publishedAt)
    val context = LocalContext.current
    val c = theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card.copy(alpha = theme.newsAlpha))
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(story.sourceName.uppercase(), color = c.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(story.title, color = c.fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
            if (story.snippet.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(story.snippet, color = c.muted, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (whenLabel.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(whenLabel, color = c.muted, fontSize = 11.sp)
            }
        }
        if (story.imageUrl.isNotBlank()) {
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(story.imageUrl)
                    .crossfade(true)
                    .addHeader("User-Agent", "MADLauncher/1.0")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.bg),
            )
        }
    }
}

@Composable
private fun rememberTime(publishedAt: Long): String {
    if (publishedAt <= 0L) return ""
    val dateFmt = DateFormat.getDateInstance(DateFormat.MEDIUM)
    val timeFmt = DateFormat.getTimeInstance(DateFormat.SHORT)
    val d = Date(publishedAt)
    return "${dateFmt.format(d)} · ${timeFmt.format(d)}"
}

@Composable
fun NewsSourcePicker(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val theme = LocalMadTheme.current
    val c = theme.colors
    val page = vm.homePages.getOrNull(vm.homePageIndex)
    val selected = page?.newsProviders.orEmpty().toSet()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.card.copy(alpha = theme.newsAlpha))
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .padding(14.dp)
            .heightIn(max = 460.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.news_sources_panel_title), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(
            if (vm.grants.location) {
                val where = vm.newsPlace?.label.orEmpty().ifBlank { "you" }
                "Google first, then publishers near $where."
            } else {
                "Installed Android news apps and publishers. MAD reads their feeds and notifications."
            },
            color = c.muted,
            fontSize = 12.sp,
        )
        val place = vm.newsPlace
        val localFirst = vm.grants.location && place != null
        val local = if (localFirst) vm.newsProviders.filter { NewsCatalog.isLocal(it, place!!) } else vm.newsProviders.take(0)
        val more = if (localFirst) vm.newsProviders.filterNot { NewsCatalog.isLocal(it, place!!) } else vm.newsProviders
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f, fill = false)) {
            if (local.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.news_local_label),
                        color = c.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
                items(local, key = { it.id }) { provider ->
                    NewsSourceRow(
                        provider = provider,
                        selected = provider.id in selected,
                        installed = vm.apps.any { it.packageName == provider.packageName },
                        local = true,
                        onToggle = { vm.toggleNewsProvider(provider.id) },
                        theme = theme,
                    )
                }
            }
            if (more.isNotEmpty()) {
                if (local.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.news_more_sources),
                            color = c.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                        )
                    }
                }
                items(more, key = { it.id }) { provider ->
                    NewsSourceRow(
                        provider = provider,
                        selected = provider.id in selected,
                        installed = vm.apps.any { it.packageName == provider.packageName },
                        local = false,
                        onToggle = { vm.toggleNewsProvider(provider.id) },
                        theme = theme,
                    )
                }
            }
        }
        Text(
            stringResource(R.string.news_done),
            color = c.muted,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { vm.newsPickerOpen = false }
                .padding(12.dp),
        )
    }
}

@Composable
private fun NewsSourceRow(
    provider: NewsProvider,
    selected: Boolean,
    installed: Boolean,
    local: Boolean,
    onToggle: () -> Unit,
    theme: com.madlauncher.ui.theme.MadThemeSpec,
) {
    val c = theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.primary.copy(alpha = 0.22f) else c.bg)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (selected) c.primary else c.border),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(provider.name, color = c.fg, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    if (local) append("Local")
                    else if (installed) append("Installed")
                    else if (provider.rssUrl.isNotBlank()) append("Feed")
                    else append("App")
                    if (installed && local) append(" · installed")
                    if (selected) append(" · on")
                },
                color = c.muted,
                fontSize = 11.sp,
            )
        }
    }
}
