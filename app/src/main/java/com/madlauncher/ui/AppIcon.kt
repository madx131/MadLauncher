package com.madlauncher.ui

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.madlauncher.data.IconBadgeStyle
import com.madlauncher.data.IconStyleStore
import com.madlauncher.R
import com.madlauncher.data.IconShapeId
import com.madlauncher.ui.theme.IconLogoSlot
import com.madlauncher.ui.theme.IconShapes
import com.madlauncher.ui.theme.LocalMadTheme
import com.madlauncher.ui.theme.MadTheme
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.Path as AndroidPath
import kotlin.math.max
import kotlin.math.min
import android.graphics.Color as AndroidColor

internal data class IconMaskMode(
    val shape: Shape,
    val fillOnly: Boolean,
    val logoSlot: IconLogoSlot,
    val adaptiveInset: Boolean,
    val key: String,
)

/**
 * Built-in shape, or the installed style. A style mask replaces the shape.
 * Style art with no mask is drawn adaptive so MAD does not tile it.
 */
@Composable
internal fun rememberIconMask(styleArt: Boolean): IconMaskMode {
    val theme = LocalMadTheme.current
    val revision = IconStyleStore.revision.intValue
    val installed = IconStyleStore.active
    val shapeId = theme.iconShape
    val corner = theme.iconCornerDp
    return remember(shapeId, corner, revision, styleArt, installed?.id, installed?.mask) {
        val mask = installed?.mask
        val art = styleArt && mask == null
        val shape = when {
            mask != null -> IconShapes.normalized(mask.points)
            art || IconShapes.usesAdaptiveInset(shapeId) -> RoundedCornerShape(0.dp)
            else -> MadTheme.iconClip(shapeId, corner)
        }
        val fillOnly = mask != null || (!art && IconShapes.shapesFillOnly(shapeId))
        val logo = when {
            mask != null -> IconLogoSlot(mask.logo.cx, mask.logo.cy, mask.logo.scale)
            art -> IconLogoSlot()
            else -> IconShapes.logoSlot(shapeId)
        }
        val adaptive = mask == null && (art || IconShapes.usesAdaptiveInset(shapeId))
        IconMaskMode(
            shape = shape,
            fillOnly = fillOnly,
            logoSlot = logo,
            adaptiveInset = adaptive,
            key = "$shapeId|$corner|${installed?.id.orEmpty()}|$styleArt|$revision",
        )
    }
}

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
    val density = LocalDensity.current
    val context = LocalContext.current
    val px = remember(size, density) {
        with(density) { size.roundToPx() }.coerceIn(48, 256)
    }
    val bgAlpha = theme.iconBgAlpha.coerceIn(0f, 1f)
    val stylePath = packageName?.let { IconStyleStore.iconPath(it, activityName) }
    val mask = rememberIconMask(styleArt = stylePath != null)
    val bmp = remember(drawable, px, mask.key, bgAlpha, packageName, activityName, stylePath) {
        val decoded = stylePath?.let { BitmapFactory.decodeFile(it) }
        val source = if (decoded != null) BitmapDrawable(context.resources, decoded) else drawable
        rasterizeForMadShape(
            context,
            source,
            px,
            bgAlpha,
            mask.shape,
            density,
            mask.fillOnly,
            mask.logoSlot,
            mask.adaptiveInset,
            packageName,
            activityName,
        )
    }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = if (mask.adaptiveInset) ContentScale.Fit else ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .then(if (mask.adaptiveInset) Modifier else Modifier.clip(mask.shape)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .then(if (mask.adaptiveInset) Modifier else Modifier.clip(mask.shape))
                .background(theme.colors.primary),
        )
    }
}

/**
 * Draw adaptive foreground+background as a full square (no system squircle mask).
 * Fill-only masks clip the fill and place the logo on [IconLogoSlot].
 * Classic shapes stay full-bleed and are clipped in Compose.
 * [bgAlpha] fades only the background layer.
 */
private fun rasterizeForMadShape(
    context: Context,
    drawable: Drawable?,
    px: Int,
    bgAlpha: Float,
    shape: Shape,
    density: Density,
    fillOnly: Boolean,
    logoSlot: IconLogoSlot,
    adaptiveInset: Boolean,
    packageName: String?,
    activityName: String?,
): Bitmap? {
    val adaptive = unwrapAdaptive(drawable)
        ?: packageName?.let { loadAdaptiveFromResources(context, it, activityName) }
    if (adaptiveInset) {
        val source = drawable ?: return null
        return source.toBitmap(px, px, Bitmap.Config.ARGB_8888)
    }
    if (adaptive != null) {
        val raw = renderAdaptiveSquare(adaptive, px, bgAlpha)
        // Some apps (AliExpress) ship a rounded mark with empty corners. Every mask,
        // including ones added later, plates that gap so the silhouette is the MAD shape.
        if (samplePlateColor(raw) != AndroidColor.TRANSPARENT) {
            return if (fillOnly) {
                maskIconFill(raw, shape, density, bgAlpha, logoSlot)
            } else {
                applyPlateIfNeeded(raw, bgAlpha)
            }
        }
        return rasterizeAdaptive(adaptive, px, bgAlpha, shape, density, clipAll = fillOnly, logoSlot)
    }
    if (drawable == null) return null
    val raw = drawable.toBitmap(px, px, Bitmap.Config.ARGB_8888)
    return if (fillOnly) maskIconFill(raw, shape, density, bgAlpha, logoSlot) else applyPlateIfNeeded(raw, bgAlpha)
}

/** Custom home icons. The logo is parked on the shape's center when the mask has one. */
internal fun maskIconFill(
    src: Bitmap,
    shape: Shape,
    density: Density,
    bgAlpha: Float,
    logoSlot: IconLogoSlot,
): Bitmap {
    val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val plate = samplePlateColor(src)
    val fill = bgAlpha.coerceIn(0f, 1f)
    canvas.withClip(shape, density) {
        if (plate != AndroidColor.TRANSPARENT && fill > 0.001f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = plate
                alpha = (fill * 255f).toInt().coerceIn(0, 255)
            }
            drawRect(0f, 0f, src.width.toFloat(), src.height.toFloat(), paint)
        }
        drawLogoBitmap(src, logoSlot)
    }
    return bmp
}

private fun Canvas.drawLogoBitmap(src: Bitmap, slot: IconLogoSlot) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    if (!slot.placesLogo) {
        drawBitmap(src, 0f, 0f, paint)
        return
    }
    val side = width * slot.scale
    val left = slot.centerX * width - side / 2f
    val top = slot.centerY * height - side / 2f
    drawBitmap(src, null, RectF(left, top, left + side, top + side), paint)
}

/**
 * One clip. Adaptive layers use the system 1.5× canvas so the glyph stays in the
 * safe zone. A legacy bitmap with transparent corners is scaled to that same zone;
 * a solid bitmap is clipped as a whole.
 */
private fun rasterizeSystemAdaptive(
    drawable: Drawable?,
    adaptive: AdaptiveIconDrawable?,
    px: Int,
    bgAlpha: Float,
    shape: Shape,
    density: Density,
): Bitmap? {
    if (adaptive == null && drawable == null) return null
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fill = bgAlpha.coerceIn(0f, 1f)
    val layer = (px * (1f + 2f * AdaptiveIconDrawable.getExtraInsetFraction())).toInt()
    val offset = (px - layer) / 2
    val bounds = Rect(offset, offset, offset + layer, offset + layer)
    canvas.withClip(shape, density) {
        if (adaptive != null) {
            drawAdaptiveExpanded(adaptive, bounds, fill)
        } else {
            val raw = drawable!!.toBitmap(px, px, Bitmap.Config.ARGB_8888)
            val plate = samplePlateColor(raw)
            if (plate != AndroidColor.TRANSPARENT) {
                if (fill > 0.001f) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = plate
                        alpha = (fill * 255f).toInt().coerceIn(0, 255)
                    }
                    drawRect(0f, 0f, px.toFloat(), px.toFloat(), paint)
                }
                val side = px * 0.70f
                val left = (px - side) / 2f
                drawBitmap(raw, null, RectF(left, left, left + side, left + side), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } else {
                drawBitmap(raw, 0f, 0f, null)
            }
        }
    }
    return bmp
}

private fun Canvas.drawAdaptiveExpanded(drawable: AdaptiveIconDrawable, bounds: Rect, fill: Float) {
    if (fill > 0.001f) {
        drawable.background?.mutate()?.let { layer ->
            val old = layer.alpha
            layer.alpha = (fill * 255f).toInt().coerceIn(0, 255)
            layer.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
            layer.draw(this)
            layer.alpha = old
        }
    }
    drawable.foreground?.mutate()?.let { layer ->
        if (layer is android.graphics.drawable.BitmapDrawable) layer.setFilterBitmap(true)
        layer.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
        layer.draw(this)
    }
}

private fun renderAdaptiveSquare(drawable: AdaptiveIconDrawable, px: Int, bgAlpha: Float): Bitmap {
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    Canvas(bmp).drawAdaptiveLayers(drawable, px, bgAlpha.coerceIn(0f, 1f), IconLogoSlot())
    return bmp
}

private fun rasterizeAdaptive(
    drawable: AdaptiveIconDrawable,
    px: Int,
    bgAlpha: Float,
    shape: Shape,
    density: Density,
    clipAll: Boolean,
    logoSlot: IconLogoSlot,
): Bitmap {
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fill = bgAlpha.coerceIn(0f, 1f)
    if (clipAll) {
        canvas.withClip(shape, density) {
            drawAdaptiveLayers(drawable, px, fill, logoSlot)
        }
    } else {
        canvas.drawAdaptiveLayers(drawable, px, fill, IconLogoSlot())
    }
    return bmp
}

private fun Canvas.drawAdaptiveLayers(
    drawable: AdaptiveIconDrawable,
    px: Int,
    fill: Float,
    logoSlot: IconLogoSlot,
) {
    if (fill > 0.001f) {
        drawable.background?.mutate()?.let { layer ->
            val old = layer.alpha
            layer.alpha = (fill * 255f).toInt().coerceIn(0, 255)
            layer.setBounds(0, 0, px, px)
            layer.draw(this)
            layer.alpha = old
        }
    }
    drawForeground(drawable, px, logoSlot)
}

private fun Canvas.drawForeground(drawable: AdaptiveIconDrawable, px: Int, slot: IconLogoSlot) {
    drawable.foreground?.mutate()?.let { layer ->
        if (layer is android.graphics.drawable.BitmapDrawable) layer.setFilterBitmap(true)
        if (!slot.placesLogo) {
            layer.setBounds(0, 0, px, px)
        } else {
            val side = px * slot.scale
            val left = (slot.centerX * px - side / 2f).toInt()
            val top = (slot.centerY * px - side / 2f).toInt()
            layer.setBounds(
                left,
                top,
                (left + side).toInt().coerceAtLeast(left + 1),
                (top + side).toInt().coerceAtLeast(top + 1),
            )
        }
        layer.draw(this)
    }
}

/**
 * Shape-picker chip. Draws this launcher's own icon in [shapeId], using the same
 * mask and logo center as the home screen, so the menu matches a tap immediately.
 */
@Composable
fun MadShapePreview(shapeId: IconShapeId, size: Dp, iconBgAlpha: Float) {
    val theme = LocalMadTheme.current
    val shape = MadTheme.iconClip(shapeId, MadTheme.cornerDpFor(shapeId))
    val density = LocalDensity.current
    val context = LocalContext.current
    val px = remember(size, density) {
        with(density) { size.roundToPx() }.coerceIn(48, 256)
    }
    val bgAlpha = iconBgAlpha.coerceIn(0f, 1f)
    val drawable = remember(context.packageName) {
        runCatching { context.packageManager.getApplicationIcon(context.packageName) }.getOrNull()
            ?: context.getDrawable(R.mipmap.ic_launcher)
    }
    val bmp = remember(drawable, px, shapeId, bgAlpha) {
        rasterizeForMadShape(
            context,
            drawable,
            px,
            bgAlpha,
            shape,
            density,
            IconShapes.shapesFillOnly(shapeId),
            IconShapes.logoSlot(shapeId),
            IconShapes.usesAdaptiveInset(shapeId),
            context.packageName,
            null,
        )
    }
    val silhouette = IconShapes.usesAdaptiveInset(shapeId)
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = if (silhouette) ContentScale.Fit else ContentScale.Crop,
            modifier = Modifier.size(size).then(if (silhouette) Modifier else Modifier.clip(shape)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .then(if (silhouette) Modifier else Modifier.clip(shape))
                .background(theme.colors.primary),
        )
    }
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

private inline fun Canvas.withClip(shape: Shape, density: Density, draw: Canvas.() -> Unit) {
    val path = androidClip(shape, width.toFloat(), height.toFloat(), density)
    save()
    clipPath(path)
    try {
        draw()
    } finally {
        restore()
    }
}

private fun androidClip(shape: Shape, width: Float, height: Float, density: Density): AndroidPath {
    val outline = shape.createOutline(Size(width, height), LayoutDirection.Ltr, density)
    return when (outline) {
        is Outline.Generic -> AndroidPath(outline.path.asAndroidPath())
        is Outline.Rectangle -> AndroidPath().apply {
            addRect(outline.rect.left, outline.rect.top, outline.rect.right, outline.rect.bottom, AndroidPath.Direction.CW)
        }
        is Outline.Rounded -> {
            val r = outline.roundRect
            val radii = floatArrayOf(
                r.topLeftCornerRadius.x, r.topLeftCornerRadius.y,
                r.topRightCornerRadius.x, r.topRightCornerRadius.y,
                r.bottomRightCornerRadius.x, r.bottomRightCornerRadius.y,
                r.bottomLeftCornerRadius.x, r.bottomLeftCornerRadius.y,
            )
            AndroidPath().apply {
                addRoundRect(RectF(r.left, r.top, r.right, r.bottom), radii, AndroidPath.Direction.CW)
            }
        }
    }
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
    return dominantOpaqueColor(bmp)
}

private fun dominantOpaqueColor(bmp: Bitmap): Int {
    val w = bmp.width
    val h = bmp.height
    if (w < 2 || h < 2) return AndroidColor.TRANSPARENT
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
