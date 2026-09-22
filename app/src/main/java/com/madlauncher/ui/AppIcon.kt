package com.madlauncher.ui

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ScaleDrawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.madlauncher.data.IconBadgeStyle
import com.madlauncher.ui.theme.LocalMadTheme
import com.madlauncher.ui.theme.MadTheme
import kotlin.math.max
import kotlin.math.min
import android.graphics.Color as AndroidColor

@Composable
fun AppIcon(
    drawable: Drawable?,
    contentDescription: String,
    size: Dp = 36.dp,
    corner: Dp = 10.dp,
    packageName: String? = null,
    activityName: String? = null,
) {
    val theme = LocalMadTheme.current
    val shapeId = theme.iconShape
    val shape = MadTheme.iconClip(shapeId, theme.iconCornerDp)
    val density = LocalDensity.current
    val context = LocalContext.current
    val px = remember(size, density) {
        with(density) { size.roundToPx() }.coerceIn(48, 256)
    }
    val bgAlpha = theme.iconBgAlpha.coerceIn(0f, 1f)
    // Rasterize without the OEM adaptive mask so MAD shapes are actually visible.
    val bmp = remember(drawable, px, shapeId, bgAlpha, packageName, activityName) {
        rasterizeForMadShape(context, drawable, px, bgAlpha, packageName, activityName)
    }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(shape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(theme.colors.primary),
        )
    }
}

/**
 * Draw adaptive foreground+background as a full square (no system squircle mask).
 * MAD then clips with [MadTheme.iconClip] so circle/square/teardrop differences show.
 * [bgAlpha] fades only the app's own background layer; the glyph stays solid.
 * Non-adaptive / OEM-pre-masked icons get a fill plate so they occupy the same shape.
 */
private fun rasterizeForMadShape(
    context: Context,
    drawable: Drawable?,
    px: Int,
    bgAlpha: Float,
    packageName: String?,
    activityName: String?,
): Bitmap? {
    val adaptive = unwrapAdaptive(drawable)
        ?: packageName?.let { loadAdaptiveFromResources(context, it, activityName) }
    val raw = when {
        adaptive != null -> rasterizeAdaptive(adaptive, px, bgAlpha)
        drawable != null -> drawable.toBitmap(px, px, Bitmap.Config.ARGB_8888)
        else -> return null
    }
    return applyPlateIfNeeded(raw, bgAlpha)
}

private fun rasterizeAdaptive(drawable: AdaptiveIconDrawable, px: Int, bgAlpha: Float): Bitmap {
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fill = bgAlpha.coerceIn(0f, 1f)
    if (fill > 0.001f) {
        drawable.background?.mutate()?.let { layer ->
            val old = layer.alpha
            layer.alpha = (fill * 255f).toInt().coerceIn(0, 255)
            layer.setBounds(0, 0, px, px)
            layer.draw(canvas)
            layer.alpha = old
        }
    }
    drawable.foreground?.mutate()?.let { layer ->
        layer.setBounds(0, 0, px, px)
        layer.draw(canvas)
    }
    return bmp
}

private fun applyPlateIfNeeded(src: Bitmap, bgAlpha: Float): Bitmap {
    val plate = samplePlateColor(src)
    if (plate == AndroidColor.TRANSPARENT || bgAlpha <= 0.001f) return src
    val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = plate
        alpha = (bgAlpha * 255f).toInt().coerceIn(0, 255)
    }
    canvas.drawRect(0f, 0f, src.width.toFloat(), src.height.toFloat(), paint)
    canvas.drawBitmap(src, 0f, 0f, null)
    return bmp
}

private fun unwrapAdaptive(drawable: Drawable?): AdaptiveIconDrawable? {
    if (drawable == null || Build.VERSION.SDK_INT < 26) return null
    val queue = ArrayDeque<Drawable>()
    val seen = HashSet<Int>()
    queue.add(drawable)
    while (queue.isNotEmpty()) {
        val d = queue.removeFirst()
        if (!seen.add(System.identityHashCode(d))) continue
        if (d is AdaptiveIconDrawable) return d
        when (d) {
            is DrawableWrapper -> d.drawable?.let(queue::add)
            is InsetDrawable -> d.drawable?.let(queue::add)
            is ScaleDrawable -> d.drawable?.let(queue::add)
            is LayerDrawable -> {
                for (i in 0 until d.numberOfLayers) d.getDrawable(i)?.let(queue::add)
            }
        }
        runCatching {
            val m = d.javaClass.methods.firstOrNull {
                it.name == "getDrawable" && it.parameterCount == 0 &&
                    Drawable::class.java.isAssignableFrom(it.returnType)
            }
            val inner = m?.invoke(d) as? Drawable
            if (inner != null && inner !== d) queue.add(inner)
        }
    }
    return null
}

private fun loadAdaptiveFromResources(
    context: Context,
    packageName: String,
    activityName: String?,
): AdaptiveIconDrawable? {
    val pm = context.packageManager
    val fromActivity = if (!activityName.isNullOrBlank()) {
        runCatching {
            val ai = pm.getActivityInfo(ComponentName(packageName, activityName), 0)
            val res = pm.getResourcesForApplication(ai.applicationInfo)
            val id = when {
                ai.iconResource != 0 -> ai.iconResource
                ai.icon != 0 -> ai.icon
                else -> ai.applicationInfo.icon
            }
            if (id != 0) res.getDrawable(id, null) else null
        }.getOrNull()
    } else {
        null
    }
    unwrapAdaptive(fromActivity)?.let { return it }
    val fromApp = runCatching {
        val info = pm.getApplicationInfo(packageName, 0)
        val res = pm.getResourcesForApplication(info)
        if (info.icon != 0) res.getDrawable(info.icon, null) else null
    }.getOrNull()
    return unwrapAdaptive(fromApp)
}

/** Transparent-corner (already circular / OEM-masked) icons need a fill so MAD's clip is visible. */
private fun samplePlateColor(bmp: Bitmap): Int {
    val w = bmp.width
    val h = bmp.height
    if (w < 4 || h < 4) return AndroidColor.TRANSPARENT
    fun alphaAt(x: Int, y: Int): Int =
        AndroidColor.alpha(bmp.getPixel(x.coerceIn(0, w - 1), y.coerceIn(0, h - 1)))
    val inset = max(1, min(w, h) / 10)
    val cornersTransparent = listOf(
        0 to 0, w - 1 to 0, 0 to h - 1, w - 1 to h - 1,
        inset to inset, w - 1 - inset to inset,
        inset to h - 1 - inset, w - 1 - inset to h - 1 - inset,
    ).all { (x, y) -> alphaAt(x, y) < 48 }
    if (!cornersTransparent) return AndroidColor.TRANSPARENT
    val counts = HashMap<Int, Int>()
    val step = max(1, min(w, h) / 36)
    for (y in 0 until h step step) {
        for (x in 0 until w step step) {
            val p = bmp.getPixel(x, y)
            if (AndroidColor.alpha(p) < 200) continue
            val key = (AndroidColor.red(p) shr 4 shl 8) or
                (AndroidColor.green(p) shr 4 shl 4) or
                (AndroidColor.blue(p) shr 4)
            counts[key] = (counts[key] ?: 0) + 1
        }
    }
    val best = counts.maxByOrNull { it.value }?.key ?: return AndroidColor.TRANSPARENT
    val r = (best shr 8 and 0xF) * 17
    val g = (best shr 4 and 0xF) * 17
    val b = (best and 0xF) * 17
    return AndroidColor.argb(255, r, g, b)
}

@Composable
fun PackageAppIcon(
    packageName: String,
    contentDescription: String,
    size: Dp = 32.dp,
) {
    val context = LocalContext.current
    val drawable = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }
    AppIcon(
        drawable = drawable,
        contentDescription = contentDescription,
        size = size,
        packageName = packageName,
    )
}

@Composable
fun NotificationIconBadge(
    count: Int,
    style: IconBadgeStyle,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    when (style) {
        IconBadgeStyle.DOT -> {
            Box(
                modifier = modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(LocalMadTheme.current.colors.red),
            )
        }
        IconBadgeStyle.NUMBER -> {
            val label = if (count > 99) "99+" else count.toString()
            Text(
                label,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LocalMadTheme.current.colors.red)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}
