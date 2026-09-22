package com.madlauncher.systemui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import com.madlauncher.MadApp
import com.madlauncher.data.NavShortcutKind
import com.madlauncher.data.NavShortcutMode
import com.madlauncher.data.NavShortcutPlacements
import com.madlauncher.platform.MadOsRuntime

/**
 * Priv-app nav shortcuts using [WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL].
 * Needs [android.permission.STATUS_BAR_SERVICE] (platform signature) — without it,
 * attach fails safely and a11y nav overlays remain the path.
 */
object MadNavBarOverlay {
    private const val TAG = "MadNavBarOverlay"
    private const val STATUS_BAR_SERVICE = "android.permission.STATUS_BAR_SERVICE"

    @Volatile
    private var denied = false
    private var attached = false
    private var root: FrameLayout? = null
    private var leftBtn: ImageButton? = null
    private var rightBtn: ImageButton? = null

    fun sync(context: Context) {
        if (!shouldShow(context)) {
            detach(context)
            return
        }
        val wm = context.getSystemService(WindowManager::class.java) ?: return
        if (!attached) {
            if (!attach(context, wm)) return
        }
        layoutForSlots(context)
    }

    /** True when panel attach was rejected (caller should keep a11y overlays). */
    fun isDenied(): Boolean = denied

    fun canAttach(context: Context): Boolean {
        if (denied) return false
        return context.checkCallingOrSelfPermission(STATUS_BAR_SERVICE) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun detach(context: Context) {
        if (!attached) return
        val wm = context.getSystemService(WindowManager::class.java) ?: return
        root?.let { runCatching { wm.removeView(it) } }
        root = null
        leftBtn = null
        rightBtn = null
        attached = false
    }

    private fun shouldShow(context: Context): Boolean {
        if (denied) return false
        if (!MadOsRuntime.isMadOs(context)) return false
        if (!canAttach(context)) return false
        if (!MadOsRuntime.useNativeSystemUi(context)) return false
        val settings = com.madlauncher.NavShortcutBridge.settings
        if (!settings.experimentalNavShortcuts) return false
        if (settings.navShortcutMode != NavShortcutMode.NAV_BAR) return false
        return true
    }

    private fun attach(context: Context, wm: WindowManager): Boolean {
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL
            title = "MAD nav shortcuts"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        val frame = FrameLayout(context)
        val left = ImageButton(context).apply {
            contentDescription = "MAD nav left"
            setBackgroundColor(0x00000000)
        }
        val right = ImageButton(context).apply {
            contentDescription = "MAD nav right"
            setBackgroundColor(0x00000000)
        }
        frame.addView(left, FrameLayout.LayoutParams(dp(context, 56), dp(context, 48)).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        })
        frame.addView(right, FrameLayout.LayoutParams(dp(context, 56), dp(context, 48)).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        })
        left.setOnClickListener { open(NavShortcutKind.NOTIFICATIONS) }
        right.setOnClickListener { open(NavShortcutKind.QUICK_SETTINGS) }
        return try {
            wm.addView(frame, lp)
            root = frame
            leftBtn = left
            rightBtn = right
            attached = true
            true
        } catch (t: Throwable) {
            denied = true
            Log.w(TAG, "nav panel attach failed; falling back to a11y overlays", t)
            runCatching { wm.removeView(frame) }
            false
        }
    }

    private fun layoutForSlots(context: Context) {
        val experimental = com.madlauncher.NavShortcutBridge.settings.experimentalMadWindows
        val slots = NavShortcutPlacements.navSlots(com.madlauncher.NavShortcutBridge.settings.navShortcutSlots)
            .filter { experimental || it.kind != NavShortcutKind.SYSTEM_APPS }
        val leftKind = slots.firstOrNull { it.placement == com.madlauncher.data.NavShortcutPlacement.NAV_LEFT }?.kind
        val rightKind = slots.firstOrNull { it.placement == com.madlauncher.data.NavShortcutPlacement.NAV_RIGHT }?.kind
        leftBtn?.visibility = if (leftKind != null) View.VISIBLE else View.GONE
        rightBtn?.visibility = if (rightKind != null) View.VISIBLE else View.GONE
        leftBtn?.setOnClickListener { leftKind?.let { open(it) } }
        rightBtn?.setOnClickListener { rightKind?.let { open(it) } }
    }

    private fun open(kind: NavShortcutKind) {
        val ctx = root?.context ?: return
        if (!com.madlauncher.NavShortcutBridge.settings.experimentalMadWindows) {
            com.madlauncher.notif.SystemShadeController.openStockForKind(ctx, kind)
            return
        }
        MadApp.instance.openLauncherPanel(MadOsRuntime.panelForKind(kind))
    }

    @Suppress("DEPRECATION")
    private fun windowType(): Int {
        // WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL (hidden @SystemApi)
        return 2024
    }

    private fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}
