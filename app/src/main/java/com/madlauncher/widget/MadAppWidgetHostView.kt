package com.madlauncher.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration

/**
 * Host view that can long-press into launcher edit even when the widget's
 * RemoteViews children consume the touch stream. After touch slop, the pager
 * may steal the swipe and children are cancelled so they cannot click.
 *
 * When [blockTouches] is true (widget edit mode), touches are consumed so they
 * cannot fall through to the home wallpaper long-press, but long-press itself
 * is suppressed — Compose overlays own move/resize.
 *
 * Layout stays stock AppWidgetHostView (MATCH_PARENT) so portrait scaling is unchanged.
 */
class MadAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    var onWidgetLongPress: (() -> Unit)? = null
    var onPressChanged: ((Boolean) -> Unit)? = null
    var interceptGestures = true
    var blockTouches = false
    /** Last size pushed via updateAppWidgetSize — skip redundant notifies that stall media widgets. */
    var lastSizeWdp: Int = 0
    var lastSizeHdp: Int = 0
    /** Bumped on each Compose attach so a stale onRelease cannot detach the new parent. */
    var attachEpoch: Int = 0

    init {
        forceZeroPadding()
    }

    override fun setAppWidget(appWidgetId: Int, info: android.appwidget.AppWidgetProviderInfo?) {
        super.setAppWidget(appWidgetId, info)
        forceZeroPadding()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        // System AppWidgetHostView re-applies default padding after setAppWidget — keep flush.
        super.setPadding(0, 0, 0, 0)
    }

    override fun updateAppWidget(remoteViews: android.widget.RemoteViews?) {
        super.updateAppWidget(remoteViews)
        // Detached pooled views still receive updates; ensure a redraw when reattached / on-screen.
        invalidate()
        requestLayout()
    }

    private fun forceZeroPadding() {
        super.setPadding(0, 0, 0, 0)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var longTriggered = false
    private var passedSlop = false
    private var downX = 0f
    private var downY = 0f
    private var tracking = false

    private val longPressRunnable = Runnable {
        if (!tracking || passedSlop || blockTouches) return@Runnable
        val edge = minOf(width, height) * 0.14f
        if (edge > 0f &&
            (downX < edge || downX > width - edge || downY < edge || downY > height - edge)
        ) {
            // Near the visual border — let the launcher treat this as free-space home edit.
            return@Runnable
        }
        longTriggered = true
        parent?.requestDisallowInterceptTouchEvent(true)
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        cancelChildPress()
        onWidgetLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        handleTouch(ev)
        return longTriggered || blockTouches
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        handleTouch(ev)
        return true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!interceptGestures && !blockTouches) return false
        handleTouch(ev)
        if (blockTouches) {
            if (ev.actionMasked == MotionEvent.ACTION_UP ||
                ev.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                finishGesture()
            }
            return true
        }
        if (longTriggered) {
            if (ev.actionMasked == MotionEvent.ACTION_UP ||
                ev.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                finishGesture()
            }
            return true
        }
        if (passedSlop) {
            if (ev.actionMasked == MotionEvent.ACTION_UP ||
                ev.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                finishGesture()
            }
            return false
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun handleTouch(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tracking = true
                longTriggered = false
                passedSlop = false
                downX = ev.x
                downY = ev.y
                handler.removeCallbacks(longPressRunnable)
                if (!blockTouches) {
                    handler.postDelayed(longPressRunnable, longPressTimeout)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (tracking && !longTriggered && !passedSlop && !blockTouches) {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    if (dx * dx + dy * dy > slop * slop) {
                        passedSlop = true
                        handler.removeCallbacks(longPressRunnable)
                        cancelChildPress()
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                handler.removeCallbacks(longPressRunnable)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!longTriggered) {
                    handler.removeCallbacks(longPressRunnable)
                    finishGesture()
                }
            }
        }
    }

    private fun finishGesture() {
        tracking = false
        longTriggered = false
        passedSlop = false
        handler.removeCallbacks(longPressRunnable)
        onPressChanged?.invoke(false)
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    private fun cancelChildPress() {
        val now = System.currentTimeMillis()
        val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        try {
            super.dispatchTouchEvent(cancel)
        } finally {
            cancel.recycle()
        }
    }

    override fun onDetachedFromWindow() {
        finishGesture()
        super.onDetachedFromWindow()
    }
}
