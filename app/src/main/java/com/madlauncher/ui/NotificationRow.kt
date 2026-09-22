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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.notif.LiveNotif
import com.madlauncher.ui.theme.LocalMadTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NotificationRow(
    item: LiveNotif,
    isRead: Boolean,
    onMarkRead: () -> Unit,
    onDismiss: () -> Unit,
    onFloat: () -> Unit,
    onReply: (String) -> Unit,
) {
    val density = LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }
    var rawX by remember(item.key) { mutableFloatStateOf(0f) }
    val offsetX by animateFloatAsState(rawX, label = "swipe")
    var expanded by remember(item.key) { mutableStateOf(false) }
    var replying by remember(item.key) { mutableStateOf(false) }
    var replyText by remember(item.key) { mutableStateOf("") }
    var dragging by remember { mutableStateOf(false) }

    val reveal = when {
        rawX > 8f -> LocalMadTheme.current.colors.green
        rawX < -8f -> LocalMadTheme.current.colors.red
        else -> LocalMadTheme.current.colors.secondary.copy(alpha = 0f)
    }
    val revealColor by animateColorAsState(reveal, label = "reveal")
    val alpha = if (isRead) 0.45f else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalMadTheme.current.colors.secondary.copy(alpha = if (isRead) 0.35f else 0.72f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
                        Text(stringResource(R.string.notif_row_read), color = LocalMadTheme.current.colors.fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                if (rawX < -8f) {
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(Icons.Outlined.Close, null, tint = LocalMadTheme.current.colors.fg, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.notif_row_dismiss), color = LocalMadTheme.current.colors.fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Swipe only on the header — action chips live outside this detector
            Row(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .fillMaxWidth()
                    .background(LocalMadTheme.current.colors.secondary.copy(alpha = if (isRead) 0.35f else 0.72f))
                    .then(
                        if (expanded) {
                            Modifier
                        } else {
                            Modifier.pointerInput(item.key) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragging = true },
                                    onDragEnd = {
                                        dragging = false
                                        when {
                                            rawX > threshold -> onMarkRead()
                                            rawX < -threshold -> onDismiss()
                                        }
                                        rawX = 0f
                                    },
                                    onDragCancel = {
                                        dragging = false
                                        rawX = 0f
                                    },
                                    onHorizontalDrag = { change, dx ->
                                        change.consume()
                                        rawX = (rawX + dx).coerceIn(-threshold * 1.6f, threshold * 1.6f)
                                    },
                                )
                            }
                        },
                    )
                    .clickable {
                        if (!dragging && abs(rawX) < 8f) {
                            expanded = !expanded
                            if (!expanded) {
                                replying = false
                                rawX = 0f
                            }
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PackageAppIcon(
                    packageName = item.packageName,
                    contentDescription = item.appName,
                    size = 32.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.title,
                            color = LocalMadTheme.current.colors.fg.copy(alpha = alpha),
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            item.time,
                            color = LocalMadTheme.current.colors.muted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        item.body,
                        color = LocalMadTheme.current.colors.muted.copy(alpha = alpha),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (expanded) 4 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = LocalMadTheme.current.colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (expanded) {
            if (replying) {
                Row(
                    modifier = Modifier
                        .padding(start = 14.dp, end = 10.dp, bottom = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, LocalMadTheme.current.colors.border, RoundedCornerShape(10.dp))
                        .background(LocalMadTheme.current.colors.bg.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = LocalMadTheme.current.colors.fg,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        ),
                        cursorBrush = SolidColor(LocalMadTheme.current.colors.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                onReply(replyText)
                                replyText = ""
                                replying = false
                            },
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (replyText.isEmpty()) {
                                Text(
                                    if (item.canInlineReply) {
                                        stringResource(R.string.notif_reply_placeholder)
                                    } else {
                                        stringResource(R.string.notif_reply_in_app, item.appName)
                                    },
                                    color = LocalMadTheme.current.colors.muted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            inner()
                        },
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = stringResource(R.string.send_content_desc),
                        tint = LocalMadTheme.current.colors.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                onReply(replyText)
                                replyText = ""
                                replying = false
                            },
                    )
                }
            } else {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ActionChip(stringResource(R.string.notif_action_reply), LocalMadTheme.current.colors.primary, Icons.AutoMirrored.Outlined.Reply) {
                        replying = true
                    }
                    ActionChip(stringResource(R.string.notif_action_mark_read), LocalMadTheme.current.colors.green, Icons.Outlined.CheckCircle) {
                        onMarkRead()
                    }
                    ActionChip(stringResource(R.string.notif_action_float), LocalMadTheme.current.colors.accent, Icons.Outlined.PictureInPictureAlt, onFloat)
                    ActionChip(stringResource(R.string.notif_action_dismiss), LocalMadTheme.current.colors.red, Icons.Outlined.Close, onDismiss)
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, LocalMadTheme.current.colors.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
