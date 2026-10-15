package com.madlauncher.ui

import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.data.DeviceLoad
import com.madlauncher.data.GlanceClockStyle
import com.madlauncher.data.GlanceDateStyle
import com.madlauncher.data.GlanceElements
import com.madlauncher.data.GlanceFont
import com.madlauncher.data.GlanceSettings
import com.madlauncher.data.GlanceStatStyle
import com.madlauncher.data.GlanceType
import com.madlauncher.data.GlanceUnreadStyle
import com.madlauncher.data.GlanceWeatherStyle
import com.madlauncher.data.GlanceWeight
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.MultiBoard
import com.madlauncher.data.MultiSlot
import com.madlauncher.ui.theme.LocalMadTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun Modifier.widgetLongPress(enabled: Boolean, onLongPress: () -> Unit): Modifier {
    if (!enabled) return this
    return pointerInput(onLongPress) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val slop = viewConfiguration.touchSlop
            val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                    if (!change.pressed) return@withTimeoutOrNull true
                    if ((change.position - down.position).getDistance() > slop) return@withTimeoutOrNull true
                }
                @Suppress("UNREACHABLE_CODE")
                true
            }
            if (released == null) onLongPress()
        }
    }
}

private fun glanceFontFamily(font: GlanceFont): FontFamily = when (font) {
    GlanceFont.SERIF -> FontFamily.Serif
    GlanceFont.MONO -> FontFamily.Monospace
    GlanceFont.DEFAULT -> FontFamily.Default
}

private fun glanceFontWeight(weight: GlanceWeight): FontWeight = when (weight) {
    GlanceWeight.LIGHT -> FontWeight.Light
    GlanceWeight.BOLD -> FontWeight.Bold
    GlanceWeight.REGULAR -> FontWeight.Normal
}

private fun glanceTypeface(font: GlanceFont): Typeface = when (font) {
    GlanceFont.SERIF -> Typeface.SERIF
    GlanceFont.MONO -> Typeface.MONOSPACE
    GlanceFont.DEFAULT -> Typeface.DEFAULT
}

fun multiPanelColor(board: MultiBoard, themeCard: Color): Color {
    val raw = if (board.background) Color(board.backgroundArgb.toInt()) else themeCard
    return raw.copy(alpha = board.alpha.coerceIn(0.05f, 1f))
}

fun glancePanelColor(settings: GlanceSettings, themeCard: Color): Color {
    val raw = if (settings.background) Color(settings.backgroundArgb.toInt()) else themeCard
    return raw.copy(alpha = settings.alpha.coerceIn(0.05f, 1f))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MadGlanceWidget(
    vm: LauncherViewModel,
    clicksEnabled: Boolean,
    onLongPress: () -> Unit = {},
    arrange: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val settings = vm.glanceSettings
    val now = Date()
    val theme = LocalMadTheme.current
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier.widgetLongPress(enabled = clicksEnabled && !arrange, onLongPress = onLongPress),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        settings.visible().forEach { id ->
            val box = settings.box(id)
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * box.x, y = maxHeight * box.y)
                    .size(width = maxWidth * box.w, height = maxHeight * box.h)
                    .then(
                        if (arrange) {
                            Modifier
                                .border(1.dp, theme.colors.primary.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                .pointerInput(id) {
                                    detectDragGestures(
                                        onDragEnd = { vm.placeGlance(id, vm.glanceSettings.box(id), persist = true) },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            val cur = vm.glanceSettings.box(id)
                                            vm.placeGlance(
                                                id,
                                                cur.copy(
                                                    x = cur.x + drag.x / widthPx,
                                                    y = cur.y + drag.y / heightPx,
                                                ),
                                                persist = false,
                                            )
                                        },
                                    )
                                }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(Modifier.fillMaxSize().padding(2.dp), contentAlignment = Alignment.Center) {
                    val type = settings.typeOf(id)
                    val scale = type.scale.coerceIn(0.6f, 2f)
                    val clockSp = ((maxHeight.value * 0.72f).coerceIn(12f, 48f) * scale).sp
                    val metaSp = ((maxHeight.value * 0.42f).coerceIn(9f, 16f) * scale).sp
                    val iconSize = (maxHeight * 0.7f * scale).coerceIn(16.dp, 36.dp)
                    val family = glanceFontFamily(type.font)
                    val weight = glanceFontWeight(type.weight)
                    when (id) {
                        GlanceElements.CLOCK -> GlanceTapZone(clicksEnabled && !arrange, { vm.openClockWidget() }, onLongPress, Modifier.fillMaxSize()) {
                            GlanceClock(settings.clockStyle, now, clockSp, type)
                        }
                        GlanceElements.DATE -> GlanceTapZone(clicksEnabled && !arrange, { vm.openCalendarWidget() }, onLongPress, Modifier.fillMaxSize()) {
                            Text(
                                glanceDate(settings.dateStyle, now),
                                color = theme.colors.primary,
                                fontSize = metaSp,
                                fontFamily = family,
                                fontWeight = weight,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                        GlanceElements.WEATHER -> GlanceTapZone(clicksEnabled && !arrange, { vm.openWeatherWidget() }, onLongPress, Modifier.fillMaxSize()) {
                            GlanceWeather(
                                style = settings.weatherStyle,
                                label = vm.weatherLabel,
                                color = theme.colors.muted,
                                fontSize = metaSp,
                                family = family,
                                weight = weight,
                            )
                        }
                        GlanceElements.UNREAD -> GlanceUnread(
                            style = settings.unreadStyle,
                            unread = vm.unreadApps,
                            clicksEnabled = clicksEnabled && !arrange,
                            arrange = arrange,
                            onClick = { vm.toggleNotifications() },
                            iconSize = iconSize,
                            metaSp = metaSp,
                            type = type,
                            family = family,
                            weight = weight,
                        )
                        GlanceElements.BATTERY, GlanceElements.CPU, GlanceElements.RAM -> {
                            GlanceStat(
                                style = settings.statStyle(id),
                                id = id,
                                load = vm.deviceLoad,
                                color = theme.colors.muted,
                                accent = theme.colors.primary,
                                fontSize = metaSp,
                                family = family,
                                weight = weight,
                            )
                        }
                    }
                }
                if (arrange) {
                    Text(
                        glanceElementShort(id),
                        color = theme.colors.primary,
                        fontSize = 8.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .background(theme.colors.primary, CircleShape)
                            .pointerInput(id) {
                                detectDragGestures(
                                    onDragEnd = { vm.placeGlance(id, vm.glanceSettings.box(id), persist = true) },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        val cur = vm.glanceSettings.box(id)
                                        vm.placeGlance(
                                            id,
                                            cur.copy(
                                                w = cur.w + drag.x / widthPx,
                                                h = cur.h + drag.y / heightPx,
                                            ),
                                            persist = false,
                                        )
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
fun GlancePreview(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val theme = LocalMadTheme.current
    val holdScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = available
        }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(glancePanelColor(vm.glanceSettings, theme.colors.card))
            .border(1.5.dp, theme.colors.primary, RoundedCornerShape(16.dp))
            .nestedScroll(holdScroll),
    ) {
        MadGlanceWidget(
            vm = vm,
            clicksEnabled = false,
            arrange = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun GlanceClock(style: GlanceClockStyle, now: Date, clockSp: TextUnit, type: GlanceType) {
    val context = LocalContext.current
    val c = LocalMadTheme.current.colors.fg
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val time = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
    val family = glanceFontFamily(type.font)
    val weight = glanceFontWeight(type.weight)
    when (style) {
        GlanceClockStyle.THIN -> Text(
            time,
            color = c,
            fontSize = clockSp,
            fontFamily = family,
            fontWeight = weight,
            lineHeight = clockSp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        GlanceClockStyle.STACKED -> {
            val hour = SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH" else "h", Locale.getDefault()).format(now)
            val minute = SimpleDateFormat("mm", Locale.getDefault()).format(now)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(hour, color = c, fontSize = clockSp, fontFamily = family, fontWeight = weight, lineHeight = clockSp, textAlign = TextAlign.Center)
                Text(minute, color = c, fontSize = clockSp, fontFamily = family, fontWeight = weight, lineHeight = clockSp, textAlign = TextAlign.Center)
            }
        }
        GlanceClockStyle.SECONDS -> {
            val seconds = SimpleDateFormat("ss", Locale.getDefault()).format(now)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                Text(time, color = c, fontSize = clockSp, fontFamily = family, fontWeight = weight, lineHeight = clockSp)
                Text(seconds, color = c.copy(alpha = 0.7f), fontSize = (clockSp.value * 0.45f).sp, fontFamily = family, fontWeight = weight, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
        }
        GlanceClockStyle.CLASSIC -> Text(
            time,
            color = c,
            fontSize = clockSp,
            fontFamily = family,
            fontWeight = weight,
            lineHeight = clockSp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        GlanceClockStyle.ANALOG, GlanceClockStyle.ANALOG_NUMBERS, GlanceClockStyle.ANALOG_MINIMAL ->
            AnalogClock(style, now, type.font, type.scale, Modifier.fillMaxSize())
    }
}

@Composable
private fun AnalogClock(
    style: GlanceClockStyle,
    now: Date,
    font: GlanceFont,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val cal = Calendar.getInstance().apply { time = now }
    val hour = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f
    val minute = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
    val second = cal.get(Calendar.SECOND).toFloat()
    val fg = LocalMadTheme.current.colors.fg
    val accent = LocalMadTheme.current.colors.primary
    Canvas(modifier) {
        val span = scale.coerceIn(0.6f, 2f)
        val radius = min(size.width, size.height) / 2f * (0.9f * span).coerceAtMost(0.96f)
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = 1.5.dp.toPx()
        drawCircle(color = fg.copy(alpha = 0.4f), radius = radius, center = center, style = Stroke(stroke))
        when (style) {
            GlanceClockStyle.ANALOG_MINIMAL -> {
                for (i in 0 until 4) {
                    val ang = Math.toRadians((i * 90 - 90).toDouble())
                    drawCircle(
                        fg,
                        radius * 0.04f,
                        Offset(
                            center.x + (cos(ang) * radius * 0.8).toFloat(),
                            center.y + (sin(ang) * radius * 0.8).toFloat(),
                        ),
                    )
                }
            }
            GlanceClockStyle.ANALOG_NUMBERS -> {
                val paint = android.graphics.Paint().apply {
                    color = fg.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = radius * 0.22f
                    typeface = glanceTypeface(font)
                    isAntiAlias = true
                }
                for (n in 1..12) {
                    val ang = Math.toRadians((n * 30 - 90).toDouble())
                    drawContext.canvas.nativeCanvas.drawText(
                        n.toString(),
                        center.x + (cos(ang) * radius * 0.7).toFloat(),
                        center.y + (sin(ang) * radius * 0.7).toFloat() + paint.textSize * 0.35f,
                        paint,
                    )
                }
            }
            else -> {
                for (i in 0 until 12) {
                    val ang = Math.toRadians((i * 30 - 90).toDouble())
                    val inner = if (i % 3 == 0) radius * 0.76f else radius * 0.86f
                    drawLine(
                        fg,
                        Offset(center.x + (cos(ang) * inner).toFloat(), center.y + (sin(ang) * inner).toFloat()),
                        Offset(center.x + (cos(ang) * radius * 0.94f).toFloat(), center.y + (sin(ang) * radius * 0.94f).toFloat()),
                        strokeWidth = if (i % 3 == 0) stroke * 1.5f else stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        fun hand(degrees: Float, length: Float, width: Float, color: Color) {
            val ang = Math.toRadians((degrees - 90).toDouble())
            drawLine(
                color,
                center,
                Offset(
                    center.x + (cos(ang) * radius * length).toFloat(),
                    center.y + (sin(ang) * radius * length).toFloat(),
                ),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }
        hand(hour * 30f, 0.48f, stroke * 2.1f, fg)
        hand(minute * 6f, 0.72f, stroke * 1.3f, fg)
        if (style != GlanceClockStyle.ANALOG_MINIMAL) hand(second * 6f, 0.8f, stroke * 0.65f, accent)
        drawCircle(accent, radius * 0.045f, center)
    }
}

private fun glanceDate(style: GlanceDateStyle, now: Date): String {
    val pattern = when (style) {
        GlanceDateStyle.WEEKDAY -> "EEEE"
        GlanceDateStyle.LONG -> "EEEE, MMMM d"
        GlanceDateStyle.SHORT -> "MMM d"
        GlanceDateStyle.NUMERIC -> "M/d/yyyy"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(now)
}

internal fun splitWeatherLabel(label: String): Pair<String, String?> {
    val parts = label.split(" · ", limit = 2)
    return if (parts.size == 2) parts[0] to parts[1] else label to null
}

internal fun glanceWeatherText(style: GlanceWeatherStyle, label: String): String {
    val (condition, temp) = splitWeatherLabel(label)
    return when (style) {
        GlanceWeatherStyle.TEXT -> label
        GlanceWeatherStyle.TEMP -> temp ?: label
        GlanceWeatherStyle.CONDITION -> condition
        GlanceWeatherStyle.STACKED -> label
    }
}

@Composable
private fun GlanceWeather(
    style: GlanceWeatherStyle,
    label: String,
    color: Color,
    fontSize: TextUnit,
    family: FontFamily,
    weight: FontWeight,
) {
    val (condition, temp) = splitWeatherLabel(label)
    if (style == GlanceWeatherStyle.STACKED && temp != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                temp,
                color = color,
                fontSize = fontSize,
                fontFamily = family,
                fontWeight = weight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                condition,
                color = color.copy(alpha = 0.75f),
                fontSize = (fontSize.value * 0.72f).sp,
                fontFamily = family,
                fontWeight = weight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    Text(
        glanceWeatherText(style, label),
        color = color,
        fontSize = fontSize,
        fontFamily = family,
        fontWeight = weight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun GlanceUnread(
    style: GlanceUnreadStyle,
    unread: List<com.madlauncher.data.UnreadApp>,
    clicksEnabled: Boolean,
    arrange: Boolean,
    onClick: () -> Unit,
    iconSize: Dp,
    metaSp: TextUnit,
    type: GlanceType,
    family: FontFamily,
    weight: FontWeight,
) {
    val muted = LocalMadTheme.current.colors.muted
    val primary = LocalMadTheme.current.colors.primary
    if (unread.isEmpty() && !arrange && style != GlanceUnreadStyle.COUNT) return
    val count = unread.sumOf { it.count.coerceAtLeast(1) }
    when (style) {
        GlanceUnreadStyle.ICONS -> {
            if (unread.isNotEmpty()) {
                UnreadIconRow(unread, clicksEnabled, onClick, iconSize, type)
            } else if (arrange) {
                Text("Apps", color = muted, fontSize = metaSp, fontFamily = family, fontWeight = weight)
            }
        }
        GlanceUnreadStyle.COUNT -> GlanceTapZone(clicksEnabled, onClick, {}, Modifier.fillMaxSize()) {
            Text(
                if (count <= 0) "0" else count.toString(),
                color = muted,
                fontSize = metaSp,
                fontFamily = family,
                fontWeight = weight,
                textAlign = TextAlign.Center,
            )
        }
        GlanceUnreadStyle.NAMES -> GlanceTapZone(clicksEnabled, onClick, {}, Modifier.fillMaxSize()) {
            val names = unread.take(3).joinToString { it.appName }.ifBlank { "Apps" }
            Text(
                names,
                color = muted,
                fontSize = metaSp,
                fontFamily = family,
                fontWeight = weight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        GlanceUnreadStyle.BADGE -> GlanceTapZone(clicksEnabled, onClick, {}, Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(iconSize.coerceAtLeast(22.dp))
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.18f))
                    .border(1.dp, primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (count <= 0) "0" else count.toString(),
                    color = primary,
                    fontSize = (iconSize.value * 0.38f).sp,
                    fontFamily = family,
                    fontWeight = weight,
                )
            }
        }
    }
}

internal fun glanceStatValue(id: String, load: DeviceLoad.Snapshot): Int = when (id) {
    GlanceElements.BATTERY -> load.battery
    GlanceElements.CPU -> load.cpu
    else -> load.ram
}

internal fun glanceStatLabel(id: String): String = when (id) {
    GlanceElements.BATTERY -> "Battery"
    GlanceElements.CPU -> "CPU"
    else -> "RAM"
}

internal fun glanceStatText(style: GlanceStatStyle, id: String, load: DeviceLoad.Snapshot): String {
    val value = glanceStatValue(id, load)
    val label = glanceStatLabel(id)
    return when (style) {
        GlanceStatStyle.VALUE -> if (value < 0) label else "$value%"
        GlanceStatStyle.LABEL, GlanceStatStyle.RING, GlanceStatStyle.BAR ->
            if (value < 0) label else "$label $value%"
    }
}

@Composable
private fun GlanceStat(
    style: GlanceStatStyle,
    id: String,
    load: DeviceLoad.Snapshot,
    color: Color,
    accent: Color,
    fontSize: TextUnit,
    family: FontFamily,
    weight: FontWeight,
) {
    val value = glanceStatValue(id, load)
    val text = glanceStatText(style, id, load)
    when (style) {
        GlanceStatStyle.RING -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize().padding(2.dp)) {
                    val radius = min(size.width, size.height) / 2f * 0.82f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val stroke = 3.dp.toPx()
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val oval = Size(radius * 2f, radius * 2f)
                    drawArc(color.copy(alpha = 0.25f), -90f, 360f, false, topLeft = topLeft, size = oval, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (value >= 0) {
                        drawArc(accent, -90f, 360f * (value / 100f), false, topLeft = topLeft, size = oval, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
                Text(if (value < 0) glanceStatLabel(id) else "$value%", color = color, fontSize = (fontSize.value * 0.72f).sp, fontFamily = family, fontWeight = weight)
            }
        }
        GlanceStatStyle.BAR -> {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text,
                    color = color,
                    fontSize = fontSize,
                    fontFamily = family,
                    fontWeight = weight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color.copy(alpha = 0.2f)),
                ) {
                    if (value >= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(value / 100f)
                                .fillMaxSize()
                                .background(accent, RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }
        }
        GlanceStatStyle.LABEL, GlanceStatStyle.VALUE -> Text(
            text,
            color = color,
            fontSize = fontSize,
            fontFamily = family,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

fun glanceElementShort(id: String): String = when (id) {
    GlanceElements.CLOCK -> "Clock"
    GlanceElements.DATE -> "Date"
    GlanceElements.WEATHER -> "Weather"
    GlanceElements.UNREAD -> "Apps"
    GlanceElements.BATTERY -> "Batt"
    GlanceElements.CPU -> "CPU"
    GlanceElements.RAM -> "RAM"
    else -> id
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlanceTapZone(
    enabled: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (enabled) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun UnreadIconRow(
    unread: List<com.madlauncher.data.UnreadApp>,
    clicksEnabled: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = 28.dp,
    type: GlanceType = GlanceType(),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.then(if (clicksEnabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        unread.take(6).forEach { u ->
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(LocalMadTheme.current.colors.primary.copy(alpha = 0.18f))
                    .border(1.dp, LocalMadTheme.current.colors.primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    u.appName.take(1),
                    color = LocalMadTheme.current.colors.primary,
                    fontSize = (iconSize.value * 0.42f).sp,
                    fontFamily = glanceFontFamily(type.font),
                    fontWeight = glanceFontWeight(type.weight),
                )
            }
        }
    }
}

@Composable
fun MultiPreview(vm: LauncherViewModel, itemId: String, modifier: Modifier = Modifier) {
    val theme = LocalMadTheme.current
    val board = vm.multiBoardFor(itemId)
    val holdScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = available
        }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(multiPanelColor(board, theme.colors.card))
            .border(1.5.dp, theme.colors.primary, RoundedCornerShape(16.dp))
            .nestedScroll(holdScroll),
    ) {
        MadMultiWidget(
            vm = vm,
            itemId = itemId,
            clicksEnabled = false,
            onLongPress = {},
            arrange = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun MadMultiWidget(
    vm: LauncherViewModel,
    itemId: String,
    clicksEnabled: Boolean,
    onLongPress: () -> Unit,
    arrange: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val board = vm.multiBoardFor(itemId)
    val theme = LocalMadTheme.current
    val density = LocalDensity.current
    if (board.slots.isEmpty()) {
        Text(
            "Mad Multi Widget",
            color = theme.colors.muted,
            modifier = modifier.widgetLongPress(clicksEnabled && !arrange, onLongPress),
        )
        return
    }
    BoxWithConstraints(
        modifier = modifier.widgetLongPress(enabled = clicksEnabled && !arrange, onLongPress = onLongPress),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        board.slots.forEachIndexed { index, slot ->
            val box = slot.box.clamped()
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * box.x, y = maxHeight * box.y)
                    .size(width = maxWidth * box.w, height = maxHeight * box.h)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (arrange) {
                            Modifier
                                .border(1.dp, theme.colors.primary.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                                .pointerInput(itemId, index) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            val current = vm.multiBoardFor(itemId).slots.getOrNull(index) ?: return@detectDragGestures
                                            vm.placeMultiSlot(itemId, index, current.box, persist = true)
                                        },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            val current = vm.multiBoardFor(itemId).slots.getOrNull(index) ?: return@detectDragGestures
                                            vm.placeMultiSlot(
                                                itemId,
                                                index,
                                                current.box.copy(
                                                    x = current.box.x + drag.x / widthPx,
                                                    y = current.box.y + drag.y / heightPx,
                                                ),
                                                persist = false,
                                            )
                                        },
                                    )
                                }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                MultiSlotContent(
                    vm = vm,
                    slot = slot,
                    clicksEnabled = clicksEnabled && !arrange,
                    onLongPress = onLongPress,
                    arrange = arrange,
                    modifier = Modifier.fillMaxSize(),
                )
                if (arrange) {
                    Text(
                        multiSlotShort(slot),
                        color = theme.colors.primary,
                        fontSize = 8.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .background(theme.colors.primary, CircleShape)
                            .pointerInput(itemId, index) {
                                detectDragGestures(
                                    onDragEnd = {
                                        val current = vm.multiBoardFor(itemId).slots.getOrNull(index) ?: return@detectDragGestures
                                        vm.placeMultiSlot(itemId, index, current.box, persist = true)
                                    },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        val current = vm.multiBoardFor(itemId).slots.getOrNull(index) ?: return@detectDragGestures
                                        vm.placeMultiSlot(
                                            itemId,
                                            index,
                                            current.box.copy(
                                                w = current.box.w + drag.x / widthPx,
                                                h = current.box.h + drag.y / heightPx,
                                            ),
                                            persist = false,
                                        )
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiSlotContent(
    vm: LauncherViewModel,
    slot: MultiSlot,
    clicksEnabled: Boolean,
    onLongPress: () -> Unit,
    arrange: Boolean,
    modifier: Modifier = Modifier,
) {
    when (slot.kind) {
        "notes" -> Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val note = vm.floatingNote
            Text(
                note?.title?.ifBlank { null } ?: "Notes",
                color = LocalMadTheme.current.colors.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                note?.text?.ifBlank { "No note yet" } ?: "No note yet",
                color = LocalMadTheme.current.colors.muted,
                fontSize = 12.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        "app" -> {
            if (arrange || slot.appWidgetId == 0) {
                Box(modifier, contentAlignment = Alignment.Center) {
                    Text(
                        slot.label.ifBlank { "Widget" },
                        color = LocalMadTheme.current.colors.fg,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                HostedAppWidget(
                    appWidgetId = slot.appWidgetId,
                    onLongPress = onLongPress,
                    interceptGestures = clicksEnabled,
                    blockTouches = !clicksEnabled,
                    modifier = modifier,
                )
            }
        }
        else -> MadGlanceWidget(
            vm = vm,
            clicksEnabled = clicksEnabled,
            onLongPress = onLongPress,
            modifier = modifier,
        )
    }
}

private fun multiSlotShort(slot: MultiSlot): String = when (slot.kind) {
    "notes" -> "Notes"
    "app" -> slot.label.ifBlank { "Widget" }
    else -> "Glance"
}
