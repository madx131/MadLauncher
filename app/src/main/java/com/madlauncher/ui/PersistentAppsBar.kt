package com.madlauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.data.PersistentApp
import com.madlauncher.ui.theme.LocalMadTheme

private const val MAX_VISIBLE = 6

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersistentAppsBar(
    apps: List<PersistentApp>,
    onLaunch: (PersistentApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (apps.isEmpty()) return
    var selected by remember { mutableStateOf<PersistentApp?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }
    val visible = apps.take(MAX_VISIBLE)
    val hidden = apps.drop(MAX_VISIBLE)

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visible.forEach { app ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LocalMadTheme.current.colors.card.copy(alpha = 0.92f))
                        .border(
                            1.dp,
                            if (selected?.packageName == app.packageName) LocalMadTheme.current.colors.primary else LocalMadTheme.current.colors.border,
                            CircleShape,
                        )
                        .combinedClickable(
                            onClick = { onLaunch(app) },
                            onLongClick = {
                                overflowOpen = false
                                selected = if (selected?.packageName == app.packageName) null else app
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(app.icon, app.appName, 28.dp, 14.dp)
                }
            }
            if (hidden.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LocalMadTheme.current.colors.card)
                        .border(1.dp, LocalMadTheme.current.colors.border, CircleShape)
                        .combinedClickable(
                            onClick = {
                                selected = null
                                overflowOpen = !overflowOpen
                            },
                            onLongClick = { overflowOpen = !overflowOpen },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+${hidden.size}",
                        color = LocalMadTheme.current.colors.fg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        val callout = selected
        if (callout != null) {
            Spacer(Modifier.height(8.dp))
            CalloutCard(callout, onLaunch)
        }
        if (overflowOpen) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalMadTheme.current.colors.card.copy(alpha = 0.97f))
                    .border(1.dp, LocalMadTheme.current.colors.border, RoundedCornerShape(12.dp))
                    .padding(8.dp)
                    .widthIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("More running apps", color = LocalMadTheme.current.colors.muted, fontSize = 11.sp)
                hidden.forEach { app ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = {
                                    overflowOpen = false
                                    onLaunch(app)
                                },
                                onLongClick = {
                                    overflowOpen = false
                                    selected = app
                                },
                            )
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.icon, app.appName, 22.dp, 8.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(app.appName, color = LocalMadTheme.current.colors.fg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalloutCard(app: PersistentApp, onLaunch: (PersistentApp) -> Unit) {
    Row(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(LocalMadTheme.current.colors.card.copy(alpha = 0.97f))
            .border(1.dp, LocalMadTheme.current.colors.border, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = { onLaunch(app) }, onLongClick = {})
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.icon, app.appName, 28.dp, 8.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(app.appName, color = LocalMadTheme.current.colors.fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                app.subtitle.ifBlank { "Running" },
                color = LocalMadTheme.current.colors.muted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
