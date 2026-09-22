package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madlauncher.R
import com.madlauncher.data.SavedNote
import com.madlauncher.overlay.StickyNoteLayout
import com.madlauncher.ui.theme.NordBg
import com.madlauncher.ui.theme.NordYellow
import java.io.File
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun FloatingNote(
    note: SavedNote,
    onDismiss: () -> Unit,
    onTextChange: (String) -> Unit,
    onTitleChange: (String) -> Unit = {},
    onCollapsedChange: (Boolean) -> Unit = {},
    onLayoutChange: (x: Int, y: Int, edge: String?) -> Unit = { _, _, _ -> },
    onAttachImage: () -> Unit = {},
    onRemoveImage: (String) -> Unit = {},
    autoFocus: Boolean = false,
    onAutoFocusConsumed: () -> Unit = {},
    windowDrag: ((Float, Float) -> Unit)? = null,
    onDragEnd: ((Float, Float) -> Unit)? = null,
    onImeRequested: (Boolean) -> Unit = {},
    imeAllowed: Boolean = true,
) {
    var x by remember(note.id) { mutableFloatStateOf(note.x?.toFloat() ?: 48f) }
    var y by remember(note.id) { mutableFloatStateOf(note.y?.toFloat() ?: 220f) }
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    var pendingIme by remember { mutableStateOf(autoFocus && !note.collapsed) }
    var focusedField by remember { mutableStateOf<NoteField?>(null) }
    var fullScreenPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(autoFocus, note.id, note.collapsed) {
        if (autoFocus && !note.collapsed) {
            pendingIme = true
            onImeRequested(true)
            onAutoFocusConsumed()
        }
    }

    LaunchedEffect(imeAllowed, pendingIme, note.collapsed) {
        if (note.collapsed) {
            pendingIme = false
            focusedField = null
            keyboard?.hide()
            focusManager.clearFocus(force = true)
            return@LaunchedEffect
        }
        if (imeAllowed && pendingIme) {
            val target = when (focusedField) {
                NoteField.Title -> titleFocus
                NoteField.Body -> bodyFocus
                null -> if (note.title.isEmpty() && note.text.isNotEmpty()) bodyFocus else titleFocus
            }
            runCatching { target.requestFocus() }
            keyboard?.show()
        }
        if (!imeAllowed) {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    fun settleLocal(vx: Float, vy: Float) {
        val snapPx = with(density) { StickyNoteLayout.SNAP_DP.dp.roundToPx() }
        val peekPx = with(density) { StickyNoteLayout.PEEK_DP.dp.roundToPx() }
        val screenW = with(density) { config.screenWidthDp.dp.roundToPx() }
        val screenH = with(density) { config.screenHeightDp.dp.roundToPx() }
        val viewW = with(density) {
            (if (note.collapsed) StickyNoteLayout.BUBBLE_DP else StickyNoteLayout.EXPANDED_WIDTH_DP).dp.roundToPx()
        }
        val viewH = with(density) { (if (note.collapsed) StickyNoteLayout.BUBBLE_DP else 180).dp.roundToPx() }
        val p = StickyNoteLayout.settle(
            x.roundToInt(),
            y.roundToInt(),
            viewW,
            viewH,
            screenW,
            screenH,
            snapPx,
            note.collapsed,
            peekPx,
            vx,
            vy,
        )
        x = p.x.toFloat()
        y = p.y.toFloat()
        onLayoutChange(p.x, p.y, StickyNoteLayout.edgeName(p.edge))
    }

    val velocityTracker = remember { VelocityTracker() }
    val dragMod = Modifier.pointerInput(windowDrag, note.collapsed) {
        detectDragGestures(
            onDragStart = { velocityTracker.resetTracking() },
            onDrag = { change, drag ->
                change.consume()
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                if (windowDrag != null) {
                    windowDrag(drag.x, drag.y)
                } else {
                    x += drag.x
                    y += drag.y
                }
            },
            onDragEnd = {
                val v = velocityTracker.calculateVelocity()
                if (windowDrag == null) settleLocal(v.x, v.y)
                onDragEnd?.invoke(v.x, v.y)
            },
        )
    }

    val offsetMod = if (windowDrag == null) {
        Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }
    } else {
        Modifier
    }

    if (note.collapsed) {
        StickyBubble(
            note = note,
            modifier = offsetMod.then(dragMod),
            tapSlopPx = with(density) { 18.dp.toPx() },
            onExpand = { onCollapsedChange(false) },
            onDrag = { dx, dy ->
                if (windowDrag != null) windowDrag(dx, dy) else {
                    x += dx
                    y += dy
                }
            },
            onDragEnd = { vx, vy ->
                if (windowDrag == null) settleLocal(vx, vy)
                onDragEnd?.invoke(vx, vy)
            },
        )
        return
    }

    Box(
        modifier = offsetMod
            .width(StickyExpandedWidth)
            .shadow(12.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(NordYellow.copy(alpha = 0.92f))
            .padding(12.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(dragMod),
            ) {
                Text(
                    stringResource(R.string.floating_note_label),
                    color = NordBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.floating_note_collapse),
                    tint = NordBg,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            onImeRequested(false)
                            onCollapsedChange(true)
                        },
                )
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.floating_note_dismiss),
                    tint = NordBg,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp)
                        .clickable(onClick = onDismiss),
                )
            }
            NoteField(
                value = note.title,
                onValueChange = onTitleChange,
                hint = stringResource(R.string.sticky_note_title_hint),
                style = TextStyle(color = NordBg, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                focusRequester = titleFocus,
                imeAllowed = imeAllowed,
                minHeight = 28,
                onBeginEdit = {
                    pendingIme = true
                    focusedField = NoteField.Title
                    onImeRequested(true)
                },
                onFocus = { focused -> if (focused) focusedField = NoteField.Title },
            )
            NoteField(
                value = note.text,
                onValueChange = onTextChange,
                hint = stringResource(R.string.sticky_note_hint),
                style = TextStyle(color = NordBg, fontSize = 14.sp),
                focusRequester = bodyFocus,
                imeAllowed = imeAllowed,
                minHeight = 48,
                onBeginEdit = {
                    pendingIme = true
                    focusedField = NoteField.Body
                    onImeRequested(true)
                },
                onFocus = { focused -> if (focused) focusedField = NoteField.Body },
            )
            if (note.imagePaths.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    note.imagePaths.take(6).forEach { path ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        ) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = stringResource(R.string.floating_note_image),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { fullScreenPath = path },
                            )
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.floating_note_remove_image),
                                tint = NordYellow,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .background(NordBg.copy(alpha = 0.7f), CircleShape)
                                    .clickable { onRemoveImage(path) },
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable(onClick = onAttachImage),
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = stringResource(R.string.floating_note_add_image),
                    tint = NordBg,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    stringResource(R.string.floating_note_add_image),
                    color = NordBg,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
    fullScreenPath?.let { path ->
        Dialog(
            onDismissRequest = { fullScreenPath = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { fullScreenPath = null },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = File(path),
                    contentDescription = stringResource(R.string.floating_note_image),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            }
        }
    }
}

private enum class NoteField { Title, Body }

private val StickyExpandedWidth = 260.dp

@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    style: TextStyle,
    focusRequester: FocusRequester,
    imeAllowed: Boolean,
    minHeight: Int,
    onBeginEdit: () -> Unit,
    onFocus: (Boolean) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        cursorBrush = SolidColor(NordBg),
        enabled = imeAllowed,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .padding(top = 6.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocus(it.isFocused) },
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(hint, color = NordBg.copy(alpha = 0.55f), fontSize = style.fontSize)
                }
                inner()
                if (!imeAllowed) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(onClick = onBeginEdit),
                    )
                }
            }
        },
    )
}

@Composable
private fun StickyBubble(
    note: SavedNote,
    modifier: Modifier,
    tapSlopPx: Float,
    onExpand: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: (Float, Float) -> Unit,
) {
    val preview = note.title.ifBlank { note.text }.trim()
    val thumb = note.imagePaths.firstOrNull()
    val velocityTracker = remember { VelocityTracker() }
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(NordYellow)
            .pointerInput(note.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    velocityTracker.resetTracking()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    var traveled = 0f
                    var moved = false
                    drag(down.id) { change ->
                        val dx = change.positionChange().x
                        val dy = change.positionChange().y
                        change.consume()
                        traveled += hypot(dx, dy)
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        if (traveled >= tapSlopPx) {
                            moved = true
                            onDrag(dx, dy)
                        }
                    }
                    val v = velocityTracker.calculateVelocity()
                    if (!moved) onExpand() else onDragEnd(v.x, v.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (thumb != null) {
            AsyncImage(
                model = File(thumb),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NordBg.copy(alpha = 0.28f)),
            )
        }
        if (preview.isNotEmpty()) {
            Text(
                preview.take(2).uppercase(),
                color = NordBg,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        } else {
            Icon(Icons.Outlined.Note, contentDescription = null, tint = NordBg, modifier = Modifier.size(26.dp))
        }
    }
}
