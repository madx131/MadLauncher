package com.madlauncher.pin

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.madlauncher.MadApp
import com.madlauncher.data.HomeItem
import com.madlauncher.data.HomeLayout
import com.madlauncher.data.HomePage
import com.madlauncher.data.HomePageKind
import com.madlauncher.data.LocalStore
import java.io.File
import java.io.FileOutputStream

/**
 * Handles [LauncherApps] pin requests so apps like Shortcut Maker can add
 * shortcuts (and widgets) to MAD home when MAD is the default launcher.
 */
class ConfirmPinActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate action=${intent?.action} extras=${intent?.extras?.keySet()}")
        if (Build.VERSION.SDK_INT < 26) {
            finish()
            return
        }
        val request = resolvePinRequest(intent)
        if (request == null || !request.isValid) {
            Log.w(TAG, "pin request missing/invalid action=${intent?.action} extras=${intent?.extras?.keySet()}")
            Toast.makeText(this, "Couldn't add that shortcut", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Log.i(TAG, "pin request type=${request.requestType} valid=${request.isValid}")
        val label = when (request.requestType) {
            LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT -> {
                val info = request.shortcutInfo
                info?.shortLabel?.toString()?.ifBlank { info.longLabel?.toString() }
                    ?: "Shortcut"
            }
            LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET -> {
                request.getAppWidgetProviderInfo(this)?.loadLabel(packageManager)?.toString()
                    ?: "Widget"
            }
            else -> "Item"
        }
        // System AlertDialog — avoids Compose/MaterialTheme crashes in this trampoline activity.
        AlertDialog.Builder(this)
            .setTitle("Add to home screen?")
            .setMessage(label)
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setPositiveButton("Add") { _, _ ->
                val ok = runCatching {
                    when (request.requestType) {
                        LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT ->
                            acceptShortcut(request)
                        LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET ->
                            acceptWidget(request)
                        else -> false
                    }
                }.onFailure { Log.e(TAG, "accept failed", it) }.getOrDefault(false)
                Toast.makeText(
                    this,
                    if (ok) "Added to home" else "Couldn't place on home",
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun resolvePinRequest(intent: Intent?): LauncherApps.PinItemRequest? {
        if (intent == null) return null
        val launcherApps = getSystemService(LauncherApps::class.java) ?: return null
        launcherApps.getPinItemRequest(intent)?.let { return it }
        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST, LauncherApps.PinItemRequest::class.java)
        } else {
            intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST)
        }
    }

    private fun acceptShortcut(request: LauncherApps.PinItemRequest): Boolean {
        val info = request.shortcutInfo ?: return false
        if (!request.accept()) {
            Log.w(TAG, "request.accept() returned false")
            return false
        }
        return placeShortcutInfo(info)
    }

    private fun acceptWidget(request: LauncherApps.PinItemRequest): Boolean {
        val info = request.getAppWidgetProviderInfo(this) ?: return false
        val host = (application as MadApp).widgetHost
        val id = host.allocateAppWidgetId()
        val extras = Bundle().apply {
            putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        val bound = AppWidgetManager.getInstance(this).bindAppWidgetIdIfAllowed(id, info.provider)
        if (!bound) {
            host.deleteAppWidgetId(id)
            return false
        }
        if (!request.accept(extras)) {
            host.deleteAppWidgetId(id)
            return false
        }
        return PinShortcutHelper.placeAppWidget(this, info, id)
    }

    private fun placeShortcutInfo(info: ShortcutInfo): Boolean {
        val launcherApps = getSystemService(LauncherApps::class.java) ?: return false
        val density = resources.displayMetrics.densityDpi
        val drawable = runCatching { launcherApps.getShortcutIconDrawable(info, density) }.getOrNull()
        val bitmap = drawable?.let { drawableToBitmap(it) }
        val id = "sc${System.currentTimeMillis()}"
        val iconPath = bitmap?.let { PinShortcutHelper.saveIcon(this, id, it) }.orEmpty()
        return PinShortcutHelper.placeShortcut(
            context = this,
            id = id,
            title = info.shortLabel?.toString()?.ifBlank { info.longLabel?.toString() } ?: "Shortcut",
            packageName = info.`package`,
            shortcutId = info.id,
            intentUri = "",
            iconPath = iconPath,
        )
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val w = (drawable.intrinsicWidth.takeIf { it > 0 } ?: 192)
        val h = (drawable.intrinsicHeight.takeIf { it > 0 } ?: 192)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bmp
    }

    companion object {
        private const val TAG = "MadPin"
    }
}

object PinShortcutHelper {
    fun saveIcon(context: android.content.Context, id: String, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "shortcuts").apply { mkdirs() }
        val file = File(dir, "$id.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file.absolutePath
    }

    fun placeShortcut(
        context: android.content.Context,
        id: String,
        title: String,
        packageName: String,
        shortcutId: String,
        intentUri: String,
        iconPath: String,
    ): Boolean {
        val item = HomeItem(
            id = id,
            kind = "shortcut",
            packageName = packageName,
            activityName = shortcutId,
            title = title,
            col = 0,
            row = 0,
            spanX = 1f,
            spanY = 1f,
            intentUri = intentUri,
            iconPath = iconPath,
        )
        return placeItem(context, item, spanX = 1, spanY = 1)
    }

    fun placeAppWidget(
        context: android.content.Context,
        info: android.appwidget.AppWidgetProviderInfo,
        appWidgetId: Int,
    ): Boolean {
        val spanX = ((info.minWidth + 30) / 70).coerceIn(1, HomeLayout.COLS)
        val spanY = ((info.minHeight + 30) / 70).coerceIn(1, HomeLayout.ROWS)
        val item = HomeItem(
            id = "aw$appWidgetId",
            kind = "appwidget",
            widget = info.loadLabel(context.packageManager)?.toString().orEmpty(),
            packageName = info.provider.packageName,
            activityName = info.provider.className,
            appWidgetId = appWidgetId,
            col = 0,
            row = 0,
            spanX = spanX.toFloat(),
            spanY = spanY.toFloat(),
        )
        return placeItem(context, item, spanX, spanY)
    }

    fun placeLegacyShortcut(
        context: android.content.Context,
        title: String,
        launchIntent: Intent,
        icon: Bitmap?,
    ): Boolean {
        val id = "sc${System.currentTimeMillis()}"
        val iconPath = icon?.let { saveIcon(context, id, it) }.orEmpty()
        val uri = runCatching { launchIntent.toUri(Intent.URI_INTENT_SCHEME) }.getOrDefault("")
        return placeShortcut(
            context = context,
            id = id,
            title = title,
            packageName = launchIntent.component?.packageName.orEmpty(),
            shortcutId = "",
            intentUri = uri,
            iconPath = iconPath,
        )
    }

    private fun placeItem(
        context: android.content.Context,
        item: HomeItem,
        spanX: Int,
        spanY: Int,
    ): Boolean {
        val store = LocalStore(context)
        val pages = store.homePages.toMutableList()
        val order = buildList {
            addAll(pages.indices.filter { pages[it].kind == HomePageKind.EMPTY })
            addAll(pages.indices.filter { pages[it].kind != HomePageKind.EMPTY })
        }.distinct()
        for (pageIndex in order) {
            val page = pages[pageIndex]
            val full = firstEmpty(context, page, spanX, spanY)
            val cell = full ?: if (spanX != 1 || spanY != 1) firstEmpty(context, page, 1, 1) else null
            if (cell == null) continue
            val placed = if (full != null) {
                item.copy(col = cell.first, row = cell.second, spanX = spanX.toFloat(), spanY = spanY.toFloat())
            } else {
                item.copy(col = cell.first, row = cell.second, spanX = 1f, spanY = 1f)
            }
            pages[pageIndex] = page.copy(items = page.items + placed)
            store.homePages = pages
            ShortcutBus.notifyChanged()
            Log.i("MadPin", "placed '${item.title}' page=$pageIndex cell=$cell")
            return true
        }
        pages.add(
            HomePage(
                id = "p${System.currentTimeMillis()}",
                kind = HomePageKind.EMPTY,
                items = listOf(item.copy(col = 0, row = 0)),
            ),
        )
        store.homePages = pages
        ShortcutBus.notifyChanged()
        Log.i("MadPin", "placed '${item.title}' on new page")
        return true
    }

    private fun firstEmpty(
        context: android.content.Context,
        page: HomePage,
        spanX: Int = 1,
        spanY: Int = 1,
    ): Pair<Int, Int>? {
        val grid = HomeLayout.gridSizeFor(page, LocalStore(context).appearanceSettings.homeGridSize)
        for (row in 0 until grid) {
            for (col in 0 until grid) {
                if (col + spanX > grid || row + spanY > grid) continue
                val free = (0 until spanX).all { dx ->
                    (0 until spanY).all { dy ->
                        page.items.none { it.occupies(col + dx, row + dy) }
                    }
                }
                if (free) return col to row
            }
        }
        return null
    }
}

/** Notifies the running launcher UI to reload home pages after an external pin. */
object ShortcutBus {
    @Volatile
    var epoch: Long = 0L
        private set
    private val listeners = mutableListOf<() -> Unit>()

    @Synchronized
    fun notifyChanged() {
        epoch = System.currentTimeMillis()
        listeners.toList().forEach { runCatching { it.invoke() } }
    }

    @Synchronized
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
}
