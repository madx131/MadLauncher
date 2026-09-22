package com.madlauncher.data

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

object WallpaperApplier {
    fun uriFrom(intent: Intent?): Uri? {
        if (intent == null) return null
        intent.data?.let { return it }
        if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { return it }
        } else {
            @Suppress("DEPRECATION")
            (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let { return it }
        }
        return intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    fun screenSize(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        return if (Build.VERSION.SDK_INT >= 30) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width().coerceAtLeast(1) to bounds.height().coerceAtLeast(1)
        } else {
            val metrics = context.resources.displayMetrics
            metrics.widthPixels.coerceAtLeast(1) to metrics.heightPixels.coerceAtLeast(1)
        }
    }

    /** Fill the screen with the current wallpaper and disable parallax scrolling. */
    fun bindToScreen(activity: Activity) {
        val wallpaper = WallpaperManager.getInstance(activity)
        val (w, h) = screenSize(activity)
        runCatching { wallpaper.suggestDesiredDimensions(w, h) }
        runCatching { wallpaper.setWallpaperOffsetSteps(1f, 1f) }
        activity.window.decorView.post {
            val token = activity.window.decorView.windowToken ?: return@post
            runCatching { wallpaper.setWallpaperOffsets(token, 0.5f, 0.5f) }
        }
    }

    /**
     * Prefer the system crop-and-set UI when available; returns null when the OEM
     * does not support it (caller should [applyImage]).
     */
    fun cropAndSetIntent(context: Context, uri: Uri): Intent? {
        return runCatching {
            WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(uri)
                ?.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.getOrNull()
    }

    fun applyImage(context: Context, uri: Uri): Boolean {
        val resolver = context.contentResolver
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val decoded = decodeForScreen(context) { resolver.openInputStream(uri) } ?: return false
        return setFitted(context, decoded)
    }

    fun applyFile(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        val decoded = decodeForScreen(context) { file.inputStream() } ?: return false
        return setFitted(context, decoded)
    }

    private fun setFitted(context: Context, source: Bitmap): Boolean {
        val wallpaper = WallpaperManager.getInstance(context)
        val (w, h) = screenSize(context)
        runCatching { wallpaper.suggestDesiredDimensions(w, h) }
        val fitted = fitToScreen(source, w, h)
        val ok = runCatching {
            if (Build.VERSION.SDK_INT >= 24) {
                wallpaper.setBitmap(
                    fitted,
                    Rect(0, 0, fitted.width, fitted.height),
                    true,
                    WallpaperManager.FLAG_SYSTEM,
                )
            } else {
                wallpaper.setBitmap(fitted)
            }
            true
        }.getOrDefault(false)
        if (fitted !== source && !fitted.isRecycled) fitted.recycle()
        if (!source.isRecycled && source !== fitted) source.recycle()
        return ok
    }

    private fun decodeForScreen(context: Context, open: () -> InputStream?): Bitmap? {
        val (targetW, targetH) = screenSize(context)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetW && bounds.outHeight / (sample * 2) >= targetH) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return open()?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun fitToScreen(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        if (src.width == targetW && src.height == targetH) return src
        val scale = max(targetW / src.width.toFloat(), targetH / src.height.toFloat())
        val scaledW = max(1, (src.width * scale).roundToInt())
        val scaledH = max(1, (src.height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
        val x = ((scaled.width - targetW) / 2).coerceAtLeast(0)
        val y = ((scaled.height - targetH) / 2).coerceAtLeast(0)
        val w = targetW.coerceAtMost(scaled.width)
        val h = targetH.coerceAtMost(scaled.height)
        if (x == 0 && y == 0 && scaled.width == targetW && scaled.height == targetH) {
            return scaled
        }
        val cropped = Bitmap.createBitmap(scaled, x, y, w, h)
        if (scaled !== src && scaled !== cropped && !scaled.isRecycled) scaled.recycle()
        return cropped
    }
}
