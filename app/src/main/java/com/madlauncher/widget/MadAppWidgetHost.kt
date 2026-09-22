package com.madlauncher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.SparseArray
import android.view.ViewGroup

/**
 * Stock [AppWidgetHost] with a one-view-per-id pool.
 *
 * Compose HorizontalPager (especially infinite home) remounts pages and would otherwise
 * call [createView] multiple times for the same id — updates then hit a disposed/off-screen
 * view (classic YouTube Music “stuck on old song” bug).
 */
class MadAppWidgetHost(
    context: Context,
    hostId: Int = HOST_ID,
) : AppWidgetHost(context.applicationContext, hostId) {
    @Volatile
    private var listening = false

    private val pooled = SparseArray<MadAppWidgetHostView>()

    /** Register for live widget updates (media widgets, clocks, etc.). Safe to call repeatedly. */
    @Synchronized
    fun ensureListening() {
        if (listening) return
        runCatching {
            startListening()
            listening = true
        }
    }

    @Synchronized
    fun stopListeningIfNeeded() {
        if (!listening) return
        runCatching {
            stopListening()
            listening = false
        }
    }

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?,
    ): AppWidgetHostView {
        return MadAppWidgetHostView(context)
    }

    /**
     * Returns the single host view for [appWidgetId], reparenting it into the caller’s
     * container. Safe across Compose dispose/recreate.
     */
    @Synchronized
    fun obtainHostView(
        context: Context,
        appWidgetId: Int,
        info: AppWidgetProviderInfo?,
    ): AppWidgetHostView {
        ensureListening()
        val existing = pooled.get(appWidgetId)
        if (existing != null) {
            detachFromParent(existing)
            bindIntoHostMap(appWidgetId, existing)
            return existing
        }
        val created = createView(context, appWidgetId, info)
        val mad = created as? MadAppWidgetHostView
        if (mad != null) {
            pooled.put(appWidgetId, mad)
            bindIntoHostMap(appWidgetId, mad)
        }
        return created
    }

    /** Detach for Compose [onRelease] without dropping the host registration. */
    @Synchronized
    fun detachHostView(view: AppWidgetHostView) {
        detachFromParent(view)
    }

    /** Drop pool entry when the widget is removed from the home grid. */
    @Synchronized
    fun discardHostView(appWidgetId: Int) {
        val view = pooled.get(appWidgetId) ?: return
        detachFromParent(view)
        pooled.remove(appWidgetId)
    }

    fun ensureViewRegistered(appWidgetId: Int, view: AppWidgetHostView?) {
        if (view == null || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        bindIntoHostMap(appWidgetId, view)
    }

    private fun detachFromParent(view: AppWidgetHostView) {
        val parent = view.parent as? ViewGroup ?: return
        runCatching { parent.removeView(view) }
    }

    /**
     * AppWidgetHost delivers RemoteViews only to the view in its internal map.
     * Keep our pooled instance there even across Compose remounts.
     */
    private fun bindIntoHostMap(appWidgetId: Int, view: AppWidgetHostView) {
        runCatching {
            val field = AppWidgetHost::class.java.getDeclaredField("mViews")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val views = field.get(this) as SparseArray<AppWidgetHostView>
            if (views.get(appWidgetId) !== view) {
                views.put(appWidgetId, view)
            }
        }
    }

    companion object {
        const val HOST_ID = 0x4D4144
    }
}
