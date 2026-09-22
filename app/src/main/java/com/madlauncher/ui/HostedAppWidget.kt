package com.madlauncher.ui

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.os.Build
import android.util.SizeF
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.madlauncher.MadApp
import com.madlauncher.widget.MadAppWidgetHostView

/** False for duplicate infinite-pager copies of a home page — see HomeDesktop. */
val LocalHostLiveAppWidgets = compositionLocalOf { true }

@Composable
fun HostedAppWidget(
    appWidgetId: Int,
    onLongPress: () -> Unit,
    onPressChanged: (Boolean) -> Unit = {},
    interceptGestures: Boolean = true,
    blockTouches: Boolean = false,
    /** Allocated cell size in dp — pushed to the provider so RemoteViews can reflow. */
    widthDp: Dp = 0.dp,
    heightDp: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val hostLive = LocalHostLiveAppWidgets.current
    if (!hostLive) {
        // Keep layout space; do not obtainHostView — one live attachment per id.
        Box(modifier)
        return
    }

    val context = LocalContext.current
    val app = context.applicationContext as MadApp
    val manager = remember { AppWidgetManager.getInstance(context) }
    val density = LocalDensity.current.density
    var laidOut by remember(appWidgetId) { mutableStateOf(IntSize.Zero) }
    val epochAtAttach = remember(appWidgetId) { intArrayOf(0) }
    key(appWidgetId) {
        AndroidView(
            modifier = modifier.onSizeChanged { size ->
                laidOut = size
            },
            factory = { ctx ->
                val info = manager.getAppWidgetInfo(appWidgetId)
                val view = app.widgetHost.obtainHostView(ctx, appWidgetId, info)
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                view.setPadding(0, 0, 0, 0)
                if (view is MadAppWidgetHostView) {
                    view.attachEpoch += 1
                    epochAtAttach[0] = view.attachEpoch
                    view.onWidgetLongPress = onLongPress
                    view.onPressChanged = onPressChanged
                    view.interceptGestures = interceptGestures
                    view.blockTouches = blockTouches
                }
                view
            },
            update = { view ->
                if (view is MadAppWidgetHostView) {
                    view.onWidgetLongPress = onLongPress
                    view.onPressChanged = onPressChanged
                    view.interceptGestures = interceptGestures
                    view.blockTouches = blockTouches
                }
                app.widgetHost.ensureListening()
                app.widgetHost.ensureViewRegistered(appWidgetId, view)
                view.post {
                    notifyWidgetSize(view, widthDp, heightDp, laidOut, density)
                }
            },
            onRelease = { view ->
                // Do not detach here. Pager remounts steal the pooled HostView via
                // obtainHostView (which reparents). Detaching on dispose blanks
                // third-party widgets while the destination page is still attaching.
                val mad = view as? MadAppWidgetHostView
                if (mad != null && mad.attachEpoch != epochAtAttach[0]) return@AndroidView
            },
        )
    }
}

private fun notifyWidgetSize(
    view: AppWidgetHostView,
    widthDp: Dp,
    heightDp: Dp,
    laidOut: IntSize,
    density: Float,
) {
    val fromLayoutW = if (laidOut.width > 0) laidOut.width / density else 0f
    val fromLayoutH = if (laidOut.height > 0) laidOut.height / density else 0f
    val wDp = when {
        widthDp.value > 0f -> widthDp.value
        fromLayoutW > 0f -> fromLayoutW
        view.width > 0 -> view.width / density
        else -> return
    }.toInt().coerceAtLeast(1)
    val hDp = when {
        heightDp.value > 0f -> heightDp.value
        fromLayoutH > 0f -> fromLayoutH
        view.height > 0 -> view.height / density
        else -> return
    }.toInt().coerceAtLeast(1)
    if (view is MadAppWidgetHostView) {
        if (view.lastSizeWdp == wDp && view.lastSizeHdp == hDp) return
        view.lastSizeWdp = wDp
        view.lastSizeHdp = hDp
    }
    if (Build.VERSION.SDK_INT >= 31) {
        view.updateAppWidgetSize(
            android.os.Bundle(),
            listOf(SizeF(wDp.toFloat(), hDp.toFloat())),
        )
    } else {
        @Suppress("DEPRECATION")
        view.updateAppWidgetSize(android.os.Bundle(), wDp, hDp, wDp, hDp)
    }
}
