package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.NotifPane
import com.madlauncher.ui.theme.LocalMadTheme

@Composable
fun NotificationsPanel(vm: LauncherViewModel) {
    val messages = vm.displayedNotifs
    val showSystem = vm.binderFilter == null && vm.notifPane == NotifPane.System
    val canDismiss = if (showSystem) {
        vm.systemNotifs.isNotEmpty()
    } else {
        messages.isNotEmpty()
    }
    val title = vm.binderFilterAppName?.let { "$it" }
        ?: stringResource(R.string.notifications_title)
    FloatingWindow(
        title = title,
        geom = vm.notifGeom,
        onGeomChange = { vm.saveWindow("notif", it) },
        onClose = { vm.closeNotifications() },
        zIndex = 20f,
        titleActions = {
            if (canDismiss) {
                Text(
                    stringResource(R.string.notif_dismiss_all),
                    color = LocalMadTheme.current.colors.red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { vm.dismissAllMessagingNotifs() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        },
    ) {
        if (!vm.listenerConnected && !vm.listenerEnabled()) {
            Text(
                stringResource(R.string.notif_enable_access),
                color = LocalMadTheme.current.colors.yellow,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(LocalMadTheme.current.colors.yellow.copy(alpha = 0.12f))
                    .clickable { vm.openListenerSettings() }
                    .padding(10.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        PartialSyncBanner(vm)
        if (vm.binderFilter == null) {
            Spacer(Modifier.height(8.dp))
            NotifPaneTabs(vm)
        }
        Spacer(Modifier.height(8.dp))
        if (showSystem) {
            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                SystemNotifBody(vm)
            }
            return@FloatingWindow
        }
        val items = messages
        val apps = items.groupBy { it.packageName }.map { it.value.first() }
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalMadTheme.current.colors.bg)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BinderDot(
                    selected = vm.binderFilter == null,
                    label = stringResource(R.string.notif_all_label),
                    onClick = { vm.setBinder(null) },
                )
                apps.forEach { n ->
                    BinderDot(
                        selected = vm.binderFilter == n.packageName,
                        label = n.appName.take(1),
                        onClick = { vm.setBinder(n.packageName) },
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.NotificationsOff, null, tint = LocalMadTheme.current.colors.muted)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.notif_all_clear), color = LocalMadTheme.current.colors.muted)
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 2.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = LocalMadTheme.current.colors.green, modifier = Modifier.size(14.dp))
                            Text(stringResource(R.string.notif_mark_read), color = LocalMadTheme.current.colors.muted, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.notif_dismiss), color = LocalMadTheme.current.colors.muted, fontSize = 10.sp)
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = LocalMadTheme.current.colors.red, modifier = Modifier.size(14.dp))
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items, key = { it.key }) { item ->
                            NotificationRow(
                                item = item,
                                isRead = item.key in vm.readIds,
                                onMarkRead = { vm.markAsRead(item.key) },
                                onDismiss = { vm.dismissNotification(item.key) },
                                onFloat = { vm.floatNotification(item) },
                                onReply = { vm.sendReply(item, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BinderDot(selected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (selected) LocalMadTheme.current.colors.primary else LocalMadTheme.current.colors.secondary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.take(1), color = LocalMadTheme.current.colors.fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NotifPaneTabs(vm: LauncherViewModel) {
    val messageCount = vm.messagingNotifs.size
    val systemCount = vm.systemNotifs.size + vm.persistentApps.size
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NotifPaneChip(
            label = stringResource(R.string.notif_tab_messages),
            count = messageCount,
            selected = vm.notifPane == NotifPane.Messages,
            onClick = { vm.selectNotifPane(NotifPane.Messages) },
            modifier = Modifier.weight(1f),
        )
        NotifPaneChip(
            label = stringResource(R.string.notif_tab_system),
            count = systemCount,
            selected = vm.notifPane == NotifPane.System,
            onClick = { vm.selectNotifPane(NotifPane.System) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NotifPaneChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) LocalMadTheme.current.colors.primary.copy(alpha = 0.28f) else LocalMadTheme.current.colors.bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            color = if (selected) LocalMadTheme.current.colors.fg else LocalMadTheme.current.colors.muted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                color = if (selected) LocalMadTheme.current.colors.fg else LocalMadTheme.current.colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
