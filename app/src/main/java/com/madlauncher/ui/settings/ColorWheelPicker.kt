package com.madlauncher.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.madlauncher.R
import com.madlauncher.ui.theme.LocalMadTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AccentColorPicker(
    selectedArgb: Long,
    onPick: (Long) -> Unit,
    title: String = stringResource(R.string.settings_section_accent),
    subtitle: String = stringResource(R.string.settings_accent_tap_wheel),
) {
    val theme = LocalMadTheme.current
    var open by remember { mutableStateOf(false) }
    val color = Color(selectedArgb.toInt())
    val hex = remember(selectedArgb) { "#%06X".format(selectedArgb.toInt() and 0xFFFFFF) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.colors.secondary)
            .clickable { open = true }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, theme.colors.border, CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                subtitle,
                color = theme.colors.fg,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(hex, color = theme.colors.muted, fontSize = 12.sp)
        }
    }

    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            val c = LocalMadTheme.current.colors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.card)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    color = c.fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
                ColorWheelPanel(
                    color = color,
                    onColorChange = { onPick((it.toArgb().toLong()) and 0xFFFFFFFFL) },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.settings_accent_done),
                    color = c.bg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.primary)
                        .clickable { open = false }
                        .padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ColorWheelPanel(
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    val hsv = remember(color) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), arr)
        arr
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    fun emit() {
        onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))))
    }

    // Keep local HSV in sync only when the incoming color differs from our HSV output.
    androidx.compose.runtime.LaunchedEffect(color) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), arr)
        val current = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        if (current != color.toArgb()) {
            hue = arr[0]
            sat = arr[1]
            value = arr[2]
        }
    }

    val wheelSize = 220.dp
    val density = LocalDensity.current
    val sizePx = with(density) { wheelSize.roundToPx() }
    val wheelBmp = remember(sizePx, value) { buildHsvWheelBitmap(sizePx, value) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(wheelSize)
            .pointerInput(Unit) {
                fun pick(pos: Offset) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = pos.x - cx
                    val dy = pos.y - cy
                    val radius = min(cx, cy)
                    val dist = hypot(dx, dy)
                    if (dist > radius * 1.05f) return
                    sat = (dist / radius).coerceIn(0f, 1f)
                    var h = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (h < 0f) h += 360f
                    hue = h
                    emit()
                }
                detectTapGestures { pick(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val pos = change.position
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = pos.x - cx
                    val dy = pos.y - cy
                    val radius = min(cx, cy)
                    val dist = hypot(dx, dy).coerceAtMost(radius)
                    sat = (dist / radius).coerceIn(0f, 1f)
                    var h = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (h < 0f) h += 360f
                    hue = h
                    emit()
                }
            },
    ) {
        Image(
            bitmap = wheelBmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(wheelSize),
        )
        Canvas(modifier = Modifier.size(wheelSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f
            val angleRad = Math.toRadians(hue.toDouble())
            val thumb = center + Offset(
                (sat * radius * cos(angleRad)).toFloat(),
                (sat * radius * sin(angleRad)).toFloat(),
            )
            drawCircle(color = Color.White, radius = 12f, center = thumb)
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = 12f,
                center = thumb,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
            )
            drawCircle(
                color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))),
                radius = 8f,
                center = thumb,
            )
        }
    }

    val theme = LocalMadTheme.current
    Text(
        stringResource(R.string.settings_accent_brightness),
        color = theme.colors.muted,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
    Slider(
        value = value,
        onValueChange = {
            value = it
            emit()
        },
        valueRange = 0.05f..1f,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))))
                .border(1.dp, theme.colors.border, CircleShape),
        )
        Text(
            "#%06X".format(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)) and 0xFFFFFF),
            color = theme.colors.fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun buildHsvWheelBitmap(size: Int, value: Float): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val cx = (size - 1) / 2f
    val cy = (size - 1) / 2f
    val radius = min(cx, cy)
    val pixels = IntArray(size * size)
    val hsv = floatArrayOf(0f, 0f, value.coerceIn(0.05f, 1f))
    for (y in 0 until size) {
        for (x in 0 until size) {
            val dx = x - cx
            val dy = y - cy
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (dist > radius) {
                pixels[y * size + x] = 0
                continue
            }
            var hue = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            if (hue < 0f) hue += 360f
            hsv[0] = hue
            hsv[1] = (dist / radius).coerceIn(0f, 1f)
            // Soft edge anti-alias
            val alpha = when {
                dist > radius - 1.2f -> ((radius - dist) / 1.2f).coerceIn(0f, 1f)
                else -> 1f
            }
            val rgb = android.graphics.Color.HSVToColor(hsv)
            pixels[y * size + x] = if (alpha >= 0.999f) {
                rgb
            } else {
                (rgb and 0x00FFFFFF) or (((alpha * 255).toInt() and 0xFF) shl 24)
            }
        }
    }
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    return bmp
}
