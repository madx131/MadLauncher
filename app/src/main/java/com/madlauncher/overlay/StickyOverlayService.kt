package com.madlauncher.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.madlauncher.ActionBoxActivity
import com.madlauncher.MadApp
import com.madlauncher.R
import com.madlauncher.data.SavedNote
import com.madlauncher.ui.FloatingNote
import com.madlauncher.ui.theme.MadLauncherTheme

/**
 * Holds [TYPE_APPLICATION_OVERLAY] sticky notes so they outlive Action Box.
 *
 * Windows stay [FLAG_NOT_FOCUSABLE] unless the user is editing a text field so
 * the overlay cannot steal IME from the app underneath.
 */
class StickyOverlayService : LifecycleService() {
    private val owner = OverlayCompositionOwner()
    private val windows = LinkedHashMap<String, OverlayWindow>()
    private val wm by lazy { getSystemService(WindowManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        owner.resume()
        startQuietForeground()
        rebuild()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        rebuild()
        val focusId = intent?.getStringExtra(StickyOverlay.EXTRA_FOCUS_ID)
        if (!focusId.isNullOrBlank()) {
            windows[focusId]?.let { window ->
                window.seedIme.value = true
                requestIme(window, true)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        windows.values.forEach { it.detach() }
        windows.clear()
        owner.destroy()
        super.onDestroy()
    }

    private fun rebuild() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        val notes = MadApp.instance.store.notes().filter { it.floating }
        if (notes.isEmpty()) {
            stopSelf()
            return
        }
        val keep = notes.map { it.id }.toSet()
        windows.keys.filter { it !in keep }.forEach { id ->
            windows.remove(id)?.detach()
        }
        notes.forEachIndexed { index, note ->
            val existing = windows[note.id]
            if (existing == null) {
                attach(note, index)
            } else {
                existing.noteState.value = note
                if (note.x != null && note.y != null && !existing.dragging) {
                    existing.params.x = note.x
                    existing.params.y = note.y
                    runCatching { wm?.updateViewLayout(existing.root, existing.params) }
                }
            }
        }
    }

    private fun attach(note: SavedNote, index: Int) {
        val wm = wm ?: return
        val density = resources.displayMetrics.density
        val view = ComposeView(this)
        val root = CappedOverlayHost(this).apply {
            isClickable = false
            isFocusable = false
        }
        val startX = note.x ?: (48 * density).toInt()
        val startY = note.y ?: ((180 + index * 36) * density).toInt()
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            unfocusedFlags(),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            softInputMode = LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }
        val window = OverlayWindow(note, root, view, params)
        applyWindowSize(window, updateLayout = false)
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeViewModelStoreOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                requestIme(window, false)
                true
            } else {
                false
            }
        }
        view.setContent {
            val current = window.noteState.value
            val imeAllowed = window.imeAllowed.value
            val seedIme = window.seedIme.value
            MadLauncherTheme {
                FloatingNote(
                    note = current,
                    onDismiss = { dismiss(current.id) },
                    onTextChange = { text -> updateText(current.id, text) },
                    onTitleChange = { title -> updateTitle(current.id, title) },
                    onCollapsedChange = { collapsed -> setCollapsed(window, collapsed) },
                    onLayoutChange = { _, _, _ -> },
                    onAttachImage = { pickImage(current.id) },
                    onRemoveImage = { path -> removeImage(current.id, path) },
                    autoFocus = seedIme,
                    onAutoFocusConsumed = { window.seedIme.value = false },
                    windowDrag = { dx, dy -> move(window, dx, dy) },
                    onDragEnd = { vx, vy -> settle(window, vx, vy) },
                    onImeRequested = { want -> requestIme(window, want) },
                    imeAllowed = imeAllowed,
                )
            }
        }
        root.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        val added = runCatching {
            wm.addView(root, params)
            true
        }.getOrDefault(false)
        if (!added) {
            view.disposeComposition()
            return
        }
        windows[note.id] = window
    }

    private fun move(window: OverlayWindow, dx: Float, dy: Float) {
        val wm = wm ?: return
        window.dragging = true
        window.params.x += dx.toInt()
        window.params.y += dy.toInt()
        runCatching { wm.updateViewLayout(window.root, window.params) }
    }

    private fun settle(window: OverlayWindow, velocityX: Float, velocityY: Float) {
        val wm = wm ?: return
        val metrics = resources.displayMetrics
        val density = metrics.density
        val snapPx = (StickyNoteLayout.SNAP_DP * density).toInt()
        val peekPx = (StickyNoteLayout.PEEK_DP * density).toInt()
        val viewW = window.root.width.takeIf { it > 0 }
            ?: (StickyNoteLayout.BUBBLE_DP * density).toInt()
        val viewH = window.root.height.takeIf { it > 0 }
            ?: (StickyNoteLayout.BUBBLE_DP * density).toInt()
        val note = window.noteState.value
        val placement = StickyNoteLayout.settle(
            window.params.x,
            window.params.y,
            viewW,
            viewH,
            metrics.widthPixels,
            metrics.heightPixels,
            snapPx,
            note.collapsed,
            peekPx,
            velocityX,
            velocityY,
        )
        window.params.x = placement.x
        window.params.y = placement.y
        runCatching { wm.updateViewLayout(window.root, window.params) }
        window.dragging = false
        persistLayout(window.id, placement.x, placement.y, StickyNoteLayout.edgeName(placement.edge))
    }

    private fun persistLayout(id: String, x: Int, y: Int, edge: String?) {
        MadApp.instance.store.updateNote(id) { it.copy(x = x, y = y, pinnedEdge = edge) }
        windows[id]?.let { win ->
            win.noteState.value = win.noteState.value.copy(x = x, y = y, pinnedEdge = edge)
        }
    }

    private fun setCollapsed(window: OverlayWindow, collapsed: Boolean) {
        if (collapsed) requestIme(window, false)
        MadApp.instance.store.updateNote(window.id) { it.copy(collapsed = collapsed) }
        window.noteState.value = window.noteState.value.copy(collapsed = collapsed)
        applyWindowSize(window)
        window.view.post { settle(window, 0f, 0f) }
    }

    /** ComposeView + WRAP_CONTENT otherwise measures to the full display and eats every tap. */
    private fun applyWindowSize(window: OverlayWindow, updateLayout: Boolean = true) {
        val density = resources.displayMetrics.density
        val collapsed = window.noteState.value.collapsed
        val w = ((if (collapsed) StickyNoteLayout.BUBBLE_DP else StickyNoteLayout.EXPANDED_WIDTH_DP) * density).toInt()
        val maxH = if (collapsed) {
            w
        } else {
            (resources.displayMetrics.heightPixels * 0.72f).toInt().coerceAtLeast(w)
        }
        window.params.width = w
        window.params.height = if (collapsed) w else LayoutParams.WRAP_CONTENT
        window.root.capWidthPx = w
        window.root.capHeightPx = maxH
        window.root.exactHeight = collapsed
        if (updateLayout) {
            runCatching { wm?.updateViewLayout(window.root, window.params) }
        }
    }

    private fun updateText(id: String, text: String) {
        MadApp.instance.store.updateNoteText(id, text)
        val window = windows[id] ?: return
        window.noteState.value = window.noteState.value.copy(text = text)
    }

    private fun updateTitle(id: String, title: String) {
        MadApp.instance.store.updateNote(id) { it.copy(title = title) }
        val window = windows[id] ?: return
        window.noteState.value = window.noteState.value.copy(title = title)
    }

    private fun pickImage(id: String) {
        val intent = StickyNotePickActivity.intent(this, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    private fun removeImage(id: String, path: String) {
        StickyNoteImages.delete(path)
        MadApp.instance.store.updateNote(id) { it.copy(imagePaths = it.imagePaths.filterNot { p -> p == path }) }
        val window = windows[id] ?: return
        window.noteState.value = window.noteState.value.copy(
            imagePaths = window.noteState.value.imagePaths.filterNot { it == path },
        )
    }

    private fun requestIme(window: OverlayWindow, want: Boolean) {
        val wm = wm ?: return
        val editing = want && !window.noteState.value.collapsed
        window.imeAllowed.value = editing
        window.params.flags = if (editing) focusedFlags() else unfocusedFlags()
        window.root.isFocusable = editing
        window.root.isFocusableInTouchMode = editing
        window.view.isFocusable = editing
        window.view.isFocusableInTouchMode = editing
        runCatching { wm.updateViewLayout(window.root, window.params) }
        if (!editing) {
            window.view.clearFocus()
        }
    }

    private fun dismiss(id: String) {
        MadApp.instance.store.dismissFloating(id)
        windows.remove(id)?.detach()
        if (windows.isEmpty()) stopSelf()
    }

    private fun OverlayWindow.detach() {
        runCatching { wm?.removeViewImmediate(root) }
            .recoverCatching { wm?.removeView(root) }
        view.disposeComposition()
    }

    private fun startQuietForeground() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Sticky notes", NotificationManager.IMPORTANCE_MIN),
            )
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            ActionBoxActivity.intent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_action_box)
            .setContentTitle(getString(R.string.floating_note_label))
            .setContentText(getString(R.string.sticky_overlay_running))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIF_ID, notif)
            }
        }
    }

    private class OverlayWindow(
        note: SavedNote,
        val root: CappedOverlayHost,
        val view: ComposeView,
        val params: LayoutParams,
    ) {
        val id: String get() = noteState.value.id
        val noteState = mutableStateOf(note)
        val imeAllowed = mutableStateOf(false)
        val seedIme = mutableStateOf(false)
        var dragging = false
    }

    companion object {
        private const val CHANNEL = "mad_sticky_overlay"
        private const val NOTIF_ID = 7101

        private fun unfocusedFlags(): Int =
            LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS

        private fun focusedFlags(): Int =
            LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }
}

/**
 * Caps overlay measure. ComposeView + WRAP_CONTENT otherwise fills the display
 * and intercepts every tap.
 */
private class CappedOverlayHost(context: android.content.Context) : FrameLayout(context) {
    var capWidthPx: Int = 0
    var capHeightPx: Int = 0
    var exactHeight: Boolean = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = if (capWidthPx > 0) {
            MeasureSpec.makeMeasureSpec(capWidthPx, MeasureSpec.EXACTLY)
        } else {
            widthMeasureSpec
        }
        val h = when {
            capHeightPx <= 0 -> heightMeasureSpec
            exactHeight -> MeasureSpec.makeMeasureSpec(capHeightPx, MeasureSpec.EXACTLY)
            else -> MeasureSpec.makeMeasureSpec(capHeightPx, MeasureSpec.AT_MOST)
        }
        super.onMeasure(w, h)
    }
}
