package com.madlauncher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.PersistentApp
import com.madlauncher.notif.LiveNotif
import com.madlauncher.notif.NotificationHub
import com.madlauncher.ui.theme.LocalMadTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** System & apps list — used inside the unified Notifications window. */
@Composable
fun SystemNotifBody(vm: LauncherViewModel) {
    val items = vm.systemNotifs
    val running = vm.persistentApps
    Text(
        "Media, calendar, downloads, and other non-chat alerts",
        color = LocalMadTheme.current.colors.muted,
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    if (running.isEmpty() && items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing else right now", color = LocalMadTheme.current.colors.muted, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
                if (running.isNotEmpty()) {
                    item(key = "running-header") {
                        Text("Now playing / running", color = LocalMadTheme.current.colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    items(running, key = { "run:${it.packageName}" }) { app ->
                        PersistentAppRow(
                            app = app,
                            onOpen = {
                                vm.closeNotifications()
                                vm.launchPersistent(app)
                            },
                            onPlayPause = { vm.mediaPlayPause(app) },
                            onSkipNext = { vm.mediaSkipNext(app) },
                            onSkipPrevious = { vm.mediaSkipPrevious(app) },
                        )
                    }
                }
                if (items.isNotEmpty()) {
                    if (running.isNotEmpty()) {
                        item(key = "alerts-header") {
                            Text(
                                "Alerts",
                                color = LocalMadTheme.current.colors.muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    items(items, key = { it.key }) { item ->
                        SystemAlertRow(
                            item = item,
                            isRead = item.key in vm.readIds,
                            onOpen = {
                                vm.markAsRead(item.key)
                                runCatching { NotificationHub.open?.invoke(item.key) }
                            },
                            onMarkRead = { vm.markAsRead(item.key) },
                            onDismiss = { vm.dismissNotification(item.key) },
                        )
                    }
                }
        }
    }
}

@Composable
private fun SystemAlertRow(
    item: LiveNotif,
    isRead: Boolean,
    onOpen: () -> Unit,
    onMarkRead: () -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }
    var rawX by remember(item.key) { mutableFloatStateOf(0f) }
    val offsetX by animateFloatAsState(rawX, label = "system-swipe")
    var dragging by remember { mutableStateOf(false) }
    val reveal = when {
        rawX > 8f -> LocalMadTheme.current.colors.green
        rawX < -8f -> LocalMadTheme.current.colors.red
        else -> LocalMadTheme.current.colors.secondary.copy(alpha = 0f)
    }
    val revealColor by animateColorAsState(reveal, label = "system-reveal")
    val alpha = if (isRead) 0.45f else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(revealColor)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (rawX > 8f) {
                Column(horizontalAlignment = Alignment.Start) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = LocalMadTheme.current.colors.fg, modifier = Modifier.size(18.dp))
                    Text("Read", color = LocalMadTheme.current.colors.fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            if (rawX < -8f) {
                Column(horizontalAlignment = Alignment.End) {
                    Icon(Icons.Outlined.Close, null, tint = LocalMadTheme.current.colors.fg, modifier = Modifier.size(18.dp))
                    Text("Dismiss", color = LocalMadTheme.current.colors.fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .clip(RoundedCornerShape(12.dp))
                .background(LocalMadTheme.current.colors.bg.copy(alpha = alpha))
                .pointerInput(item.key) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            rawX = (rawX + dx).coerceIn(-threshold * 1.4f, threshold * 1.4f)
                        },
                        onDragEnd = {
                            dragging = false
                            when {
                                rawX <= -threshold -> {
                                    rawX = 0f
                                    onDismiss()
                                }
                                rawX >= threshold -> {
                                    rawX = 0f
                                    onMarkRead()
                                }
                                else -> rawX = 0f
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            rawX = 0f
                        },
                    )
                }
                .clickable(enabled = !dragging && abs(rawX) < 4f, onClick = onOpen)
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PackageAppIcon(
                packageName = item.packageName,
                contentDescription = item.appName,
                size = 32.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.appName,
                    color = LocalMadTheme.current.colors.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    item.title.ifBlank { item.body },
                    color = LocalMadTheme.current.colors.fg,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.body.isNotBlank() && item.title.isNotBlank()) {
                    Text(
                        item.body,
                        color = LocalMadTheme.current.colors.muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                item.time,
                color = LocalMadTheme.current.colors.muted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun PersistentAppRow(
    app: PersistentApp,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalMadTheme.current.colors.bg)
            .border(1.dp, LocalMadTheme.current.colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app.icon, app.appName, 36.dp, 8.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, color = LocalMadTheme.current.colors.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    app.subtitle.ifBlank { "Running" },
                    color = LocalMadTheme.current.colors.muted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (app.isMedia) {
            Spacer(Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaControlButton(
                    enabled = app.canSkip,
                    onClick = onSkipPrevious,
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = LocalMadTheme.current.colors.fg, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LocalMadTheme.current.colors.accent.copy(alpha = 0.25f))
                        .border(1.dp, LocalMadTheme.current.colors.accent.copy(alpha = 0.55f), CircleShape)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (app.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (app.isPlaying) "Pause" else "Play",
                        tint = LocalMadTheme.current.colors.fg,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                MediaControlButton(
                    enabled = app.canSkip,
                    onClick = onSkipNext,
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = LocalMadTheme.current.colors.fg, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
private fun MediaControlButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(LocalMadTheme.current.colors.card.copy(alpha = if (enabled) 0.9f else 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
