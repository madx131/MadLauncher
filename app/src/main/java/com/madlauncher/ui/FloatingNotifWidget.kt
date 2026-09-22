package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.madlauncher.data.FloatingConv
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.theme.LocalMadTheme
import kotlin.math.roundToInt

@Composable
fun FloatingNotifWidget(vm: LauncherViewModel, conv: FloatingConv) {
    val latest = vm.latestInConversation(conv.conversationId) ?: return
    val lines = vm.conversationLines(conv.conversationId)
    var x by remember(conv.conversationId) { mutableFloatStateOf(conv.x) }
    var y by remember(conv.conversationId) { mutableFloatStateOf(conv.y) }
    var w by remember(conv.conversationId) { mutableFloatStateOf(conv.w) }
    var h by remember(conv.conversationId) { mutableFloatStateOf(conv.h) }
    var draft by remember(conv.conversationId) { mutableStateOf("") }
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenW = with(density) { config.screenWidthDp.dp.toPx() }
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }
    val minW = with(density) { 240.dp.toPx() }
    val minH = with(density) { 180.dp.toPx() }

    fun persist() = vm.moveFloating(conv.conversationId, x, y)
    fun persistSize() = vm.resizeFloating(conv.conversationId, w, h)
    fun send() {
        vm.sendReply(latest, draft)
        draft = ""
    }

    if (conv.minimized) {
        Box(
            modifier = Modifier
                .zIndex(40f)
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(52.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(latest.accent)
                .border(2.dp, latest.accent, CircleShape)
                .pointerInput(conv.conversationId) {
                    detectDragGestures(
                        onDragEnd = { persist() },
                        onDrag = { change, drag ->
                            change.consume()
                            x += drag.x
                            y += drag.y
                        },
                    )
                }
                .clickable { vm.minimizeFloating(conv.conversationId, false) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                latest.contactInitial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .zIndex(40f)
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(with(density) { w.toDp() })
            .height(with(density) { h.toDp() })
            .shadow(18.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(LocalMadTheme.current.colors.card.copy(alpha = 0.97f))
            .border(1.dp, latest.accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(latest.accent))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(conv.conversationId) {
                        detectDragGestures(
                            onDragEnd = { persist() },
                            onDrag = { change, drag ->
                                change.consume()
                                x += drag.x
                                y += drag.y
                            },
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(latest.accent.copy(alpha = 0.18f))
                        .border(1.dp, latest.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(latest.appName.take(1), color = latest.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        latest.appName.uppercase(),
                        color = latest.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                    )
                    Text(latest.title, color = LocalMadTheme.current.colors.fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(latest.time, color = LocalMadTheme.current.colors.muted, fontSize = 10.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "_",
                    color = LocalMadTheme.current.colors.muted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { vm.minimizeFloating(conv.conversationId, true) }
                        .padding(horizontal = 4.dp),
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = LocalMadTheme.current.colors.muted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { vm.closeFloating(conv.conversationId) },
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                lines.forEach { line ->
                    Text(
                        line.text,
                        color = if (line.outgoing) latest.accent else LocalMadTheme.current.colors.fg,
                        fontSize = 13.sp,
                        fontWeight = if (line.outgoing) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, latest.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .background(LocalMadTheme.current.colors.bg.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = TextStyle(color = LocalMadTheme.current.colors.fg, fontSize = 13.sp),
                    cursorBrush = SolidColor(latest.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            Text(
                                if (latest.canInlineReply) "Reply…" else "Reply in ${latest.appName}…",
                                color = LocalMadTheme.current.colors.muted,
                                fontSize = 13.sp,
                            )
                        }
                        inner()
                    },
                )
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    tint = latest.accent,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = { send() }),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(10.dp)
                .padding(top = 40.dp, bottom = 18.dp)
                .background(LocalMadTheme.current.colors.border.copy(alpha = 0.22f))
                .pointerInput(conv.conversationId) {
                    detectDragGestures(
                        onDragEnd = { persistSize() },
                        onDrag = { change, drag ->
                            change.consume()
                            w = (w + drag.x).coerceIn(minW, (screenW - 16f).coerceAtLeast(minW))
                        },
                    )
                },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(10.dp)
                .padding(start = 18.dp, end = 18.dp)
                .background(LocalMadTheme.current.colors.border.copy(alpha = 0.22f))
                .pointerInput(conv.conversationId) {
                    detectDragGestures(
                        onDragEnd = { persistSize() },
                        onDrag = { change, drag ->
                            change.consume()
                            h = (h + drag.y).coerceIn(minH, (screenH * 0.8f).coerceAtLeast(minH))
                        },
                    )
                },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(22.dp)
                .background(LocalMadTheme.current.colors.primary.copy(alpha = 0.35f), RoundedCornerShape(topStart = 8.dp))
                .pointerInput(conv.conversationId) {
                    detectDragGestures(
                        onDragEnd = { persistSize() },
                        onDrag = { change, drag ->
                            change.consume()
                            w = (w + drag.x).coerceIn(minW, (screenW - 16f).coerceAtLeast(minW))
                            h = (h + drag.y).coerceIn(minH, (screenH * 0.8f).coerceAtLeast(minH))
                        },
                    )
                },
        )
    }
}
