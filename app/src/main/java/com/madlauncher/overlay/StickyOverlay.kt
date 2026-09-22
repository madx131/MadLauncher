package com.madlauncher.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.madlauncher.MadApp

/** Sticky notes that draw over other apps (Action Box, not MAD home). */
object StickyOverlay {
    const val EXTRA_FOCUS_ID = "focus_note_id"

    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun sync(context: Context, focusNoteId: String? = null) {
        val app = context.applicationContext
        val hasFloating = runCatching {
            MadApp.instance.store.notes().any { it.floating }
        }.getOrDefault(false)
        val intent = Intent(app, StickyOverlayService::class.java)
        if (!hasFloating) {
            app.stopService(intent)
            return
        }
        if (!canShow(app)) return
        if (!focusNoteId.isNullOrBlank()) {
            intent.putExtra(EXTRA_FOCUS_ID, focusNoteId)
        }
        runCatching { app.startService(intent) }
    }
}
