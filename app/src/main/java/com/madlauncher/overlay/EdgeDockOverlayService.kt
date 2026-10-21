package com.madlauncher.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.madlauncher.MadApp
import com.madlauncher.R
import com.madlauncher.data.EdgePanel
import com.madlauncher.data.EdgeSide
import com.madlauncher.data.HomeLayout
import com.madlauncher.data.LaunchableApp
import com.madlauncher.ui.AppIcon
import com.madlauncher.ui.theme.MadLauncherTheme

/**
 * [TYPE_APPLICATION_OVERLAY] left/right docks so they stay up in other apps.
 */
class EdgeDockOverlayService : LifecycleService() {
    private val owner = OverlayCompositionOwner()
    private val windows = LinkedHashMap<EdgeSide, OverlayWindow>()
    private val wm by lazy { getSystemService(WindowManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        owner.resume()
        startQuietForeground()
        rebuild()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        rebuild()
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
        val panels = MadApp.instance.store.edgePanels.filter {
            it.showOverApps && (it.side == EdgeSide.LEFT || it.side == EdgeSide.RIGHT)
        }
        if (panels.isEmpty()) {
            stopSelf()
            return
        }
        val keep = panels.map { it.side }.toSet()
        windows.keys.filter { it !in keep }.forEach { side ->
            windows.remove(side)?.detach()
        }
        panels.forEach { panel ->
            val existing = windows[panel.side]
            if (existing == null) {
                attach(panel)
            } else {
                existing.panel = panel
                if (!panel.autoHide.isAutoHide) existing.expanded = true
                applyWindowSize(existing)
                existing.revision++
            }
        }
    }

    private fun attach(panel: EdgePanel) {
        val wm = wm ?: return
        val view = ComposeView(this)
        val root = FrameLayout(this).apply {
            isClickable = false
            isFocusable = false
        }
        val left = panel.side == EdgeSide.LEFT
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or if (left) Gravity.START else Gravity.END
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        val window = OverlayWindow(panel, root, view, params)
        window.expanded = !panel.autoHide.isAutoHide
        applyWindowSize(window, updateLayout = false)
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeViewModelStoreOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        view.setContent {
            val tick = window.revision
            val current = window.panel
            val expanded = window.expanded
            MadLauncherTheme(appearance = MadApp.instance.store.appearanceSettings) {
                OverlaySideDock(
                    panel = current,
                    expanded = expanded,
                    left = current.side == EdgeSide.LEFT,
                    apps = current.slots.map { resolveSlot(it) },
                    onToggle = { toggle(window) },
                    onLaunch = { app -> launch(window, app) },
                    revision = tick,
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
        windows[panel.side] = window
    }

    private fun toggle(window: OverlayWindow) {
        if (!window.panel.autoHide.isAutoHide) return
        window.expanded = !window.expanded
        window.revision++
        applyWindowSize(window)
    }

    private fun launch(window: OverlayWindow, app: LaunchableApp) {
        runCatching { MadApp.instance.appsRepo.launch(app) }
        if (window.panel.autoHide.isAutoHide) {
            window.expanded = false
            window.revision++
            applyWindowSize(window)
        }
    }

    private fun applyWindowSize(window: OverlayWindow, updateLayout: Boolean = true) {
        val density = resources.displayMetrics.density
        val expanded = window.expanded
        val thickness = (window.panel.thicknessDp.coerceIn(48f, 160f) * density).toInt()
        window.params.width = if (expanded) thickness else (14 * density).toInt()
        window.params.height = if (expanded) LayoutParams.WRAP_CONTENT else (96 * density).toInt()
        if (updateLayout) {
            runCatching { wm?.updateViewLayout(window.root, window.params) }
        }
    }

    private fun OverlayWindow.detach() {
        runCatching { wm?.removeViewImmediate(root) }
            .recoverCatching { wm?.removeView(root) }
        view.disposeComposition()
    }

    private fun resolveSlot(slot: String): LaunchableApp? {
        val parsed = HomeLayout.parseAppSlot(slot) ?: return null
        val pm = packageManager
        val cn = ComponentName(parsed.first, parsed.second)
        val info = runCatching { pm.getActivityInfo(cn, 0) }.getOrNull() ?: return null
        return LaunchableApp(
            label = info.loadLabel(pm).toString(),
            packageName = parsed.first,
            activityName = parsed.second,
            icon = info.loadIcon(pm),
        )
    }

    private fun startQuietForeground() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Side docks", NotificationManager.IMPORTANCE_MIN),
            )
        }
        val notif = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_action_box)
            .setContentTitle(getString(R.string.settings_route_edit_docks))
            .setContentText(getString(R.string.dock_overlay_running))
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
        panel: EdgePanel,
        val root: FrameLayout,
        val view: ComposeView,
        val params: LayoutParams,
    ) {
        var panel: EdgePanel = panel
        var expanded: Boolean = true
        var revision by mutableStateOf(0)
    }

    companion object {
        private const val CHANNEL = "mad_edge_dock_overlay"
        private const val NOTIF_ID = 7102
    }
}

@Composable
private fun OverlaySideDock(
    panel: EdgePanel,
    expanded: Boolean,
    left: Boolean,
    apps: List<LaunchableApp?>,
    onToggle: () -> Unit,
    onLaunch: (LaunchableApp) -> Unit,
    revision: Int,
) {
    val theme = com.madlauncher.ui.theme.LocalMadTheme.current
    val thickness = panel.thicknessDp.dp.coerceIn(48.dp, 160.dp)
    val iconSize = com.madlauncher.ui.edgeIconDp(panel.iconSizeDp, panel.thicknessDp).dp
    val shape = if (left) {
        RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
    }
    if (!expanded) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(96.dp)
                .clip(shape)
                .background(theme.colors.primary.copy(alpha = 0.88f))
                .pointerInput(left, revision) {
                    detectHorizontalDragGestures { _, dx ->
                        if ((left && dx > 8f) || (!left && dx < -8f)) onToggle()
                    }
                }
                .clickable(onClick = onToggle),
        )
        return
    }
    Column(
        modifier = Modifier
            .width(thickness)
            .shadow(12.dp, shape)
            .clip(shape)
            .background(theme.colors.card.copy(alpha = 0.96f))
            .border(1.dp, theme.colors.border, shape)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        apps.forEach { app ->
            Box(
                modifier = Modifier
                    .size((iconSize + 12.dp).coerceAtLeast(40.dp))
                    .clip(CircleShape)
                    .background(theme.colors.primary.copy(alpha = 0.10f))
                    .border(1.dp, theme.colors.border, CircleShape)
                    .then(
                        if (app != null) Modifier.clickable { onLaunch(app) } else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (app != null) {
                    AppIcon(app.icon, app.label, iconSize)
                }
            }
        }
        if (panel.autoHide.isAutoHide) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.colors.primary.copy(alpha = 0.5f))
                    .clickable(onClick = onToggle),
            )
        }
    }
}
