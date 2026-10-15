package com.madlauncher.a11y



import android.accessibilityservice.AccessibilityService

import android.content.BroadcastReceiver

import android.content.Context

import android.content.Intent

import android.content.IntentFilter

import android.content.res.Configuration

import android.graphics.PixelFormat

import android.graphics.Rect

import android.os.Build

import android.os.SystemClock

import android.view.Gravity

import android.view.View

import android.view.WindowInsets

import android.view.WindowManager

import android.view.accessibility.AccessibilityEvent

import android.view.accessibility.AccessibilityNodeInfo

import android.widget.FrameLayout

import android.widget.ImageView

import android.widget.LinearLayout

import com.madlauncher.LauncherCommands

import com.madlauncher.MainActivity

import com.madlauncher.MadApp

import com.madlauncher.NavShortcutBridge

import com.madlauncher.R

import com.madlauncher.data.NavShortcutKind

import com.madlauncher.data.NavShortcutMode

import com.madlauncher.data.NavShortcutPlacement

import com.madlauncher.data.NavShortcutPlacements

import com.madlauncher.data.RecentsIngest

import com.madlauncher.notif.SystemShadeController



/**

 * Nav-bar shortcut overlays. HyperOS Recents "Clear all" often tears down

 * TYPE_ACCESSIBILITY_OVERLAY windows without destroying this service — so we

 * health-check and rebuild only when overlays are actually missing.

 */

class MadAccessibilityService : AccessibilityService() {

    private val overlayButtons = linkedMapOf<NavShortcutKind, View>()

    private val shadeButtons = linkedMapOf<NavShortcutKind, View>()

    private var shadeButtonRow: LinearLayout? = null

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private var lastRebuildAt = 0L

    private var receiverRegistered = false



    private val overlayWm: WindowManager

        get() = getSystemService(WINDOW_SERVICE) as WindowManager



    private val configListener: (com.madlauncher.data.AppearanceSettings) -> Unit = {

        scheduleSync(0L)

    }



    private val rebuildListener: () -> Unit = {

        scheduleSync(0L)

    }



    private val layoutListener: () -> Unit = {

        scheduleLayout(0L)

    }



    private val pendingSync = Runnable { syncOverlays() }

    private val pendingLayout = Runnable { layoutButtonsSafe() }

    private val ottCallRunnable = Runnable { tryOttCallTap() }

    private val ottSocialPostRunnable = Runnable { trySocialPostTap() }

    private val ottSocialSendLearnRunnable = Runnable { trySocialSendLearn() }

    private val ottChatSendRunnable = Runnable { tryChatSendTap() }

    private val messengerSearchRunnable = Runnable { tryMessengerSearch() }



    private val watchdog = object : Runnable {

        override fun run() {

            syncOverlays()

            mainHandler.postDelayed(this, WATCHDOG_MS)

        }

    }



    private val recoveryReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            scheduleSync(400L)

        }

    }



    override fun onServiceConnected() {

        instance = this

        NavShortcutBridge.addListener(configListener)

        NavShortcutBridge.addRebuildListener(rebuildListener)

        NavShortcutBridge.addLayoutListener(layoutListener)

        registerRecoveryReceiver()

        scheduleSync(0L)

        mainHandler.removeCallbacks(watchdog)

        mainHandler.postDelayed(watchdog, WATCHDOG_MS)

    }



    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return



        val pkg = event.packageName?.toString().orEmpty()

        val className = event.className?.toString().orEmpty()

        val fromSystemUi = pkg.contains("systemui") || pkg.contains("miui") || pkg.contains("hyperos")

        val recentsLike = className.contains("Recents", ignoreCase = true) ||

            className.contains("Home", ignoreCase = true) ||

            className.contains("Launcher", ignoreCase = true)



        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||

            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||

            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

        ) {

            if (fromSystemUi || recentsLike) {

                if (!com.madlauncher.BuildConfig.ACTION_BOX_APP) scheduleSync(350L)

            }

            if (!com.madlauncher.BuildConfig.ACTION_BOX_APP &&
                NavShortcutBridge.enabled &&
                !NavShortcutBridge.useClassicNav
            ) {

                syncShadeShortcuts()

            }

            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
            ) {
                RecentsIngest.noteForeground(pkg, packageName)
            }

            maybeScheduleOttCallTap(pkg)

            maybeScheduleSocialPostTap(pkg)

            maybeScheduleSocialSendLearn(pkg)

            maybeScheduleChatSendTap(pkg)

            maybeScheduleMessengerSearch(pkg)

        } else if (OttCallBridge.pending != null) {

            maybeScheduleOttCallTap(pkg)

        } else if (OttSocialPostBridge.pending != null) {

            maybeScheduleSocialPostTap(pkg)

        } else if (OttSocialSendLearn.pending != null) {

            maybeScheduleSocialSendLearn(pkg)

        } else if (OttChatSendBridge.pending != null) {

            maybeScheduleChatSendTap(pkg)

        } else if (MessengerSearchBridge.pending != null) {

            maybeScheduleMessengerSearch(pkg)

        }

    }


    fun scheduleOttCallAttempt(delayMs: Long) {

        mainHandler.removeCallbacks(ottCallRunnable)

        mainHandler.postDelayed(ottCallRunnable, delayMs)

    }



    fun scheduleSocialPostAttempt(delayMs: Long) {

        mainHandler.removeCallbacks(ottSocialPostRunnable)

        mainHandler.postDelayed(ottSocialPostRunnable, delayMs)

    }



    fun scheduleSocialSendLearn(delayMs: Long) {

        mainHandler.removeCallbacks(ottSocialSendLearnRunnable)

        mainHandler.postDelayed(ottSocialSendLearnRunnable, delayMs)

    }



    fun scheduleChatSendAttempt(delayMs: Long) {

        mainHandler.removeCallbacks(ottChatSendRunnable)

        mainHandler.postDelayed(ottChatSendRunnable, delayMs)

    }

    fun scheduleMessengerSearch(delayMs: Long) {

        mainHandler.removeCallbacks(messengerSearchRunnable)

        mainHandler.postDelayed(messengerSearchRunnable, delayMs)

    }



    private fun maybeScheduleOttCallTap(activePackage: String) {

        val req = OttCallBridge.pending ?: return

        if (matchesOttPackage(activePackage, req.packageName)) {

            scheduleOttCallAttempt(250L)

        }

    }



    private fun maybeScheduleSocialPostTap(activePackage: String) {

        val req = OttSocialPostBridge.pending ?: return

        if (matchesOttPackage(activePackage, req.packageName)) {

            scheduleSocialPostAttempt(250L)

        }

    }



    private fun maybeScheduleSocialSendLearn(activePackage: String) {

        val watch = OttSocialSendLearn.pending ?: return

        if (matchesOttPackage(activePackage, watch.packageName)) {

            scheduleSocialSendLearn(250L)

        }

    }



    private fun maybeScheduleChatSendTap(activePackage: String) {

        val req = OttChatSendBridge.pending ?: return

        if (matchesOttPackage(activePackage, req.packageName)) {

            scheduleChatSendAttempt(250L)

        }

    }

    private fun maybeScheduleMessengerSearch(activePackage: String) {

        val req = MessengerSearchBridge.pending ?: return

        if (matchesOttPackage(activePackage, req.packageName)) {

            scheduleMessengerSearch(250L)

        }

    }



    private fun tryOttCallTap() {

        val req = OttCallBridge.pending ?: return

        if (System.currentTimeMillis() > req.deadlineMs) {

            OttCallBridge.clear()

            return

        }

        val root = findTargetRoot(req)

        if (root == null) {

            OttCallBridge.maybeRelaunch(this, req)

            scheduleOttCallAttempt(450L)

            return

        }

        when (OttCallTapEngine.attempt(this, root, req)) {

            OttCallTapEngine.Step.Completed -> OttCallBridge.markDone()

            OttCallTapEngine.Step.Progress -> scheduleOttCallAttempt(450L)

            OttCallTapEngine.Step.NotFound -> scheduleOttCallAttempt(400L)

        }

    }



    private fun trySocialPostTap() {

        val req = OttSocialPostBridge.pending ?: return

        if (System.currentTimeMillis() > req.deadlineMs) {

            OttSocialPostBridge.clear()

            return

        }

        val root = findTargetRoot(req.packageName)

        if (root == null) {

            OttSocialPostBridge.maybeRelaunch(this, req)

            scheduleSocialPostAttempt(450L)

            return

        }

        when (OttSocialPostTapEngine.attempt(this, root, req)) {

            OttSocialPostTapEngine.Step.Completed -> OttSocialPostBridge.markDone()

            OttSocialPostTapEngine.Step.Progress -> scheduleSocialPostAttempt(450L)

            OttSocialPostTapEngine.Step.NotFound -> scheduleSocialPostAttempt(400L)

        }

    }



    private fun trySocialSendLearn() {

        val watch = OttSocialSendLearn.pending ?: return

        if (System.currentTimeMillis() > watch.deadlineMs) return

        val root = findTargetRoot(watch.packageName) ?: return

        OttSocialSendLearn.onTree(root)

    }



    private fun tryChatSendTap() {

        val req = OttChatSendBridge.pending ?: return

        if (System.currentTimeMillis() > req.deadlineMs) {

            OttChatSendBridge.clear()

            return

        }

        val root = findTargetRoot(req.packageName)

        if (root == null) {

            OttChatSendBridge.maybeRelaunch(this, req)

            scheduleChatSendAttempt(450L)

            return

        }

        when (OttChatSendTapEngine.attempt(this, root, req)) {

            OttChatSendTapEngine.Step.Completed -> OttChatSendBridge.markDone()

            OttChatSendTapEngine.Step.Progress -> scheduleChatSendAttempt(450L)

            OttChatSendTapEngine.Step.NotFound -> scheduleChatSendAttempt(400L)

        }

    }

    private fun tryMessengerSearch() {

        val req = MessengerSearchBridge.pending ?: return

        if (System.currentTimeMillis() > req.deadlineMs) {

            MessengerSearchBridge.clear()

            return

        }

        val root = findTargetRoot(req.packageName)

        if (root == null) {

            scheduleMessengerSearch(450L)

            return

        }

        when (MessengerSearchTapEngine.attempt(this, root, req)) {

            MessengerSearchTapEngine.Step.Completed -> MessengerSearchBridge.markDone()

            MessengerSearchTapEngine.Step.Progress -> scheduleMessengerSearch(500L)

            MessengerSearchTapEngine.Step.NotFound -> scheduleMessengerSearch(400L)

        }

    }



    private fun findTargetRoot(req: OttCallBridge.Request): AccessibilityNodeInfo? =
        findTargetRoot(req.packageName)



    private fun findTargetRoot(packageName: String): AccessibilityNodeInfo? {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            windows?.forEach { window ->

                val root = window.root ?: return@forEach

                val pkg = root.packageName?.toString().orEmpty()

                if (matchesOttPackage(pkg, packageName)) return root

            }

        }

        val active = rootInActiveWindow ?: return null

        val pkg = active.packageName?.toString().orEmpty()

        return if (matchesOttPackage(pkg, packageName)) active else null

    }



    private fun matchesOttPackage(active: String, want: String): Boolean {

        if (active.equals(want, ignoreCase = true)) return true

        val short = want.substringAfterLast('.')

        return active.contains(short, ignoreCase = true)

    }



    override fun onConfigurationChanged(newConfig: Configuration) {

        super.onConfigurationChanged(newConfig)

        scheduleLayout(0L)

    }



    override fun onInterrupt() {

        scheduleSync(500L)

    }



    override fun onDestroy() {

        mainHandler.removeCallbacksAndMessages(null)

        NavShortcutBridge.removeListener(configListener)

        NavShortcutBridge.removeRebuildListener(rebuildListener)

        NavShortcutBridge.removeLayoutListener(layoutListener)

        unregisterRecoveryReceiver()

        removeButtons()

        removeShadeShortcuts(fully = true)

        if (instance == this) instance = null

        super.onDestroy()

    }



    private fun registerRecoveryReceiver() {

        if (receiverRegistered) return

        val filter = IntentFilter().apply {

            addAction(Intent.ACTION_USER_PRESENT)

            addAction(Intent.ACTION_SCREEN_ON)

            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)

            addAction(Intent.ACTION_USER_UNLOCKED)

        }

        runCatching {

            if (Build.VERSION.SDK_INT >= 33) {

                registerReceiver(recoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

            } else {

                @Suppress("DEPRECATION")

                registerReceiver(recoveryReceiver, filter)

            }

            receiverRegistered = true

        }

    }



    private fun unregisterRecoveryReceiver() {

        if (!receiverRegistered) return

        runCatching { unregisterReceiver(recoveryReceiver) }

        receiverRegistered = false

    }



    private fun scheduleSync(delayMs: Long) {

        mainHandler.removeCallbacks(pendingSync)

        mainHandler.postDelayed(pendingSync, delayMs.coerceAtLeast(50L))

    }



    private fun scheduleLayout(delayMs: Long) {

        mainHandler.removeCallbacks(pendingLayout)

        mainHandler.postDelayed(pendingLayout, delayMs.coerceAtLeast(0L))

    }



    private fun isHealthy(): Boolean {

        if (com.madlauncher.BuildConfig.ACTION_BOX_APP) return true

        if (!NavShortcutBridge.enabled) return overlayButtons.isEmpty()

        if (NavShortcutBridge.settings.navShortcutMode != NavShortcutMode.NAV_BAR) return true

        val navSlots = NavShortcutPlacements.navSlots(NavShortcutBridge.settings.navShortcutSlots)

        if (overlayButtons.isEmpty() && navSlots.isNotEmpty()) return false

        if (overlayButtons.size != navSlots.size) return false

        if (navSlots.any { !overlayButtons.containsKey(it.kind) }) return false

        return overlayButtons.values.all { it.isAttachedToWindow && it.parent != null }

    }



    private fun syncOverlays() {

        if (com.madlauncher.BuildConfig.ACTION_BOX_APP) {
            removeButtons()
            removeShadeShortcuts(fully = true)
            return
        }

        if (!NavShortcutBridge.enabled) {
            removeButtons()
            removeShadeShortcuts(fully = true)
            return
        }

        if (!com.madlauncher.platform.PlatformBridge.isEnabled(
                com.madlauncher.platform.PlatformFeature.NAV_SHORTCUT_OVERLAY,
            )
        ) {
            removeButtons()
            removeShadeShortcuts(fully = true)
            return
        }

        if (NavShortcutBridge.useNativeSystemUi) {
            removeButtons()
            removeShadeShortcuts(fully = true)
            return
        }

        if (NavShortcutBridge.settings.navShortcutMode != NavShortcutMode.NAV_BAR) {

            removeButtons()

            removeShadeShortcuts(fully = true)

            return

        }

        if (isHealthy()) {

            layoutButtonsSafe()

            return

        }

        rebuildButtons(force = true)

    }



    private fun layoutButtonsSafe() {

        if (!NavShortcutBridge.enabled || NavShortcutBridge.settings.navShortcutMode != NavShortcutMode.NAV_BAR) return

        if (!isHealthy()) {

            scheduleSync(150L)

            return

        }

        layoutButtons()

    }



    private fun rebuildButtons(force: Boolean = false) {

        val now = SystemClock.uptimeMillis()

        if (!force && now - lastRebuildAt < 200L) {

            scheduleSync(200L - (now - lastRebuildAt))

            return

        }

        if (!force && isHealthy()) {

            layoutButtonsSafe()

            return

        }

        lastRebuildAt = now

        removeButtons()

        if (NavShortcutBridge.enabled && NavShortcutBridge.settings.navShortcutMode == NavShortcutMode.NAV_BAR) {

            showButtons()

            if (!NavShortcutBridge.useClassicNav) {

                SystemShadeController.syncWithAppearance(this, NavShortcutBridge.settings)

            }

        } else {

            removeShadeShortcuts(fully = true)

        }

        layoutButtonsSafe()

    }



    private fun showButtons() {

        NavShortcutPlacements.navSlots(NavShortcutBridge.settings.navShortcutSlots).filter { slot ->

            NavShortcutBridge.settings.experimentalMadWindows || slot.kind != NavShortcutKind.SYSTEM_APPS

        }.forEach { slot ->

            if (overlayButtons.containsKey(slot.kind)) return@forEach

            val (icon, bg, label, panel) = shortcutVisuals(slot.kind)

            val view = addCornerButton(

                if (slot.placement == NavShortcutPlacement.NAV_LEFT) Gravity.BOTTOM or Gravity.START else Gravity.BOTTOM or Gravity.END,

                icon,

                bg,

                label,

            ) { openLauncher(panel) }

            if (view != null) {

                overlayButtons[slot.kind] = view

            } else {

                scheduleSync(250L)

            }

        }

    }



    private data class Quad(val icon: Int, val bg: Int, val label: String, val panel: String)



    private fun shortcutVisuals(kind: NavShortcutKind): Quad {

        val experimental = NavShortcutBridge.settings.experimentalMadWindows

        return when (kind) {

            NavShortcutKind.NOTIFICATIONS -> Quad(

                R.drawable.ic_bell,

                R.drawable.bg_overlay_bell,

                if (experimental) "Messages" else "Notifications",

                "notifications",

            )

            NavShortcutKind.QUICK_SETTINGS -> Quad(

                R.drawable.ic_gear,

                R.drawable.bg_overlay_gear,

                "Quick settings",

                "quicksettings",

            )

            NavShortcutKind.SYSTEM_APPS -> Quad(

                R.drawable.ic_monitor,

                R.drawable.bg_overlay_monitor,

                "System and apps",

                "systemnotifs",

            )

        }

    }



    private fun addCornerButton(

        gravity: Int,

        icon: Int,

        background: Int,

        label: String,

        onClick: () -> Unit,

    ): View? {

        val density = resources.displayMetrics.density

        val size = (40 * density).toInt()

        val inset = (10 * density).toInt()

        val host = FrameLayout(this).apply {

            contentDescription = label

            isClickable = true

            isFocusable = true

            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

            setOnClickListener { onClick() }

        }

        val image = ImageView(this).apply {

            setImageResource(icon)

            setBackgroundResource(background)

            setPadding(inset, inset, inset, inset)

            scaleType = ImageView.ScaleType.CENTER_INSIDE

            isClickable = false

            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        }

        host.addView(

            image,

            FrameLayout.LayoutParams(size, size, Gravity.CENTER),

        )

        val added = runCatching {

            overlayWm.addView(host, navButtonParams(gravity))

            true

        }.getOrDefault(false)

        return if (added) host else null

    }



    private fun layoutButtons() {

        NavShortcutPlacements.navSlots(NavShortcutBridge.settings.navShortcutSlots).forEach { slot ->

            val view = overlayButtons[slot.kind] ?: return@forEach

            if (!view.isAttachedToWindow) {

                scheduleSync(150L)

                return

            }

            val gravity = if (slot.placement == NavShortcutPlacement.NAV_LEFT) {

                Gravity.BOTTOM or Gravity.START

            } else {

                Gravity.BOTTOM or Gravity.END

            }

            val ok = runCatching {

                overlayWm.updateViewLayout(view, navButtonParams(gravity))

                true

            }.getOrDefault(false)

            if (!ok) scheduleSync(150L)

        }

    }



    private fun navButtonParams(gravity: Int): WindowManager.LayoutParams {

        val density = resources.displayMetrics.density

        val hit = (52 * density).toInt()

        val pad = (10 * density).toInt()

        val navH = navigationBarHeight()

        val y = ((navH - hit) / 2).coerceAtLeast(0)

        return WindowManager.LayoutParams().apply {

            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

            format = PixelFormat.TRANSLUCENT

            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or

                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or

                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            width = hit

            height = hit

            this.gravity = gravity

            x = pad

            this.y = y

            if (Build.VERSION.SDK_INT >= 28) {

                layoutInDisplayCutoutMode =

                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

            }

            if (Build.VERSION.SDK_INT >= 30) {

                setFitInsetsTypes(0)

                setFitInsetsSides(0)

            }

        }

    }



    private fun navigationBarHeight(): Int {

        if (Build.VERSION.SDK_INT >= 30) {

            val nav = overlayWm.currentWindowMetrics.windowInsets

                .getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())

                .bottom

            if (nav > 0) return nav

        }

        val resId = resources.getIdentifier("navigation_bar_height", "dimen", "android")

        if (resId > 0) {

            val fromRes = resources.getDimensionPixelSize(resId)

            if (fromRes > 0) return fromRes

        }

        return (48 * resources.displayMetrics.density).toInt()

    }



    private fun syncShadeShortcuts() {

        if (com.madlauncher.BuildConfig.ACTION_BOX_APP) {

            removeShadeShortcuts(fully = true)

            return

        }

        if (!NavShortcutBridge.enabled || NavShortcutBridge.useClassicNav) {

            removeShadeShortcuts(fully = true)

            return

        }

        val shadeSlots = NavShortcutPlacements.shadeSlots(NavShortcutBridge.settings.navShortcutSlots).filter { slot ->

            NavShortcutBridge.settings.experimentalMadWindows || slot.kind != NavShortcutKind.SYSTEM_APPS

        }

        if (shadeSlots.isEmpty()) {

            removeShadeShortcuts(fully = false)

            return

        }

        val shadeRoot = windows

            ?.firstOrNull { it.root?.packageName == "com.android.systemui" }

            ?.root

        if (shadeRoot == null) {

            removeShadeShortcuts(fully = false)

            return

        }

        val header = findByViewId(shadeRoot, "com.android.systemui:id/shade_header_root")

        val notifs = findByViewId(shadeRoot, "com.android.systemui:id/notifications_container")

            ?: findByViewId(shadeRoot, "com.android.systemui:id/notification_stack_scroller")

        if (header == null || notifs == null) {

            removeShadeShortcuts(fully = false)

            return

        }

        val headerBounds = Rect()

        val notifBounds = Rect()

        header.getBoundsInScreen(headerBounds)

        notifs.getBoundsInScreen(notifBounds)

        val gap = notifBounds.top - headerBounds.bottom

        val minGap = (56 * resources.displayMetrics.density).toInt()

        if (gap < minGap) {

            removeShadeShortcuts(fully = false)

            return

        }

        showShadeShortcutRow(headerBounds.bottom, gap, shadeSlots)

    }



    private fun findByViewId(node: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {

        val found = node.findAccessibilityNodeInfosByViewId(id)

        return found.firstOrNull()

    }



    private fun showShadeShortcutRow(top: Int, height: Int, slots: List<com.madlauncher.data.NavShortcutSlot>) {

        val density = resources.displayMetrics.density

        val pad = (8 * density).toInt()

        val row = shadeButtonRow ?: LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER

            val params = WindowManager.LayoutParams().apply {

                type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

                format = PixelFormat.TRANSLUCENT

                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or

                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or

                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or

                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                width = WindowManager.LayoutParams.MATCH_PARENT

                gravity = Gravity.TOP

            }

            val added = runCatching {

                overlayWm.addView(this, params)

                true

            }.getOrDefault(false)

            if (!added) return

            shadeButtonRow = this

        }

        val needed = slots.map { it.kind }.toSet()

        shadeButtons.keys.filter { it !in needed }.forEach { kind ->

            shadeButtons.remove(kind)?.let { view ->

                runCatching { row.removeView(view) }

            }

        }

        slots.forEach { slot ->

            if (shadeButtons.containsKey(slot.kind)) return@forEach

            val (icon, bg, label, panel) = shortcutVisuals(slot.kind)

            val size = (40 * density).toInt()

            val inset = (8 * density).toInt()

            val button = FrameLayout(this).apply {

                contentDescription = label

                isClickable = true

                isFocusable = true

                setOnClickListener { openLauncher(panel) }

            }

            button.addView(

                ImageView(this).apply {

                    setImageResource(icon)

                    setBackgroundResource(bg)

                    setPadding(inset, inset, inset, inset)

                    scaleType = ImageView.ScaleType.CENTER_INSIDE

                    isClickable = false

                },

                FrameLayout.LayoutParams(size, size, Gravity.CENTER),

            )

            val margin = (pad / 2).coerceAtLeast(4)

            row.addView(

                button,

                LinearLayout.LayoutParams(size, size).apply {

                    marginStart = margin

                    marginEnd = margin

                },

            )

            shadeButtons[slot.kind] = button

        }

        row.visibility = View.VISIBLE

        val params = row.layoutParams as? WindowManager.LayoutParams ?: return

        params.y = top

        params.height = height.coerceAtLeast((48 * density).toInt())

        runCatching { overlayWm.updateViewLayout(row, params) }

    }



    private fun removeButtons() {

        overlayButtons.values.forEach { view ->

            runCatching { overlayWm.removeViewImmediate(view) }

                .recoverCatching { overlayWm.removeView(view) }

        }

        overlayButtons.clear()

    }



    private fun removeShadeShortcuts(fully: Boolean) {

        val row = shadeButtonRow ?: return

        if (fully) {

            runCatching { overlayWm.removeViewImmediate(row) }

                .recoverCatching { overlayWm.removeView(row) }

            shadeButtonRow = null

            shadeButtons.clear()

        } else {

            row.visibility = View.GONE

        }

    }



    private fun openLauncher(panel: String) {

        if (com.madlauncher.BuildConfig.ACTION_BOX_APP) return

        val app = applicationContext as? MadApp

        if (!NavShortcutBridge.settings.experimentalMadWindows) {

            SystemShadeController.openStockPanel(this, panel)

            return

        }

        app?.openLauncherPanel(panel)

        val intent = Intent(this, MainActivity::class.java)

            .addFlags(

                Intent.FLAG_ACTIVITY_NEW_TASK or

                    Intent.FLAG_ACTIVITY_SINGLE_TOP or

                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,

            )

            .putExtra("open", panel)

        runCatching { startActivity(intent) }

            .onFailure {

                app?.openLauncherPanel(panel)

                LauncherCommands.request(panel)

            }

    }



    companion object {

        private const val WATCHDOG_MS = 15_000L



        @Volatile

        var instance: MadAccessibilityService? = null

            private set

    }

}


