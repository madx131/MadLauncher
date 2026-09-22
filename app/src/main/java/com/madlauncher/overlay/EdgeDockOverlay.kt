package com.madlauncher.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.madlauncher.BuildConfig
import com.madlauncher.MadApp
import com.madlauncher.data.EdgeSide
import com.madlauncher.data.LocalStore

/** Left/right docks that stay on screen while other apps are open. */
object EdgeDockOverlay {
    fun sync(context: Context) {
        val app = context.applicationContext
        val intent = Intent(app, EdgeDockOverlayService::class.java)
        if (BuildConfig.ACTION_BOX_APP) {
            app.stopService(intent)
            return
        }
        val wanted = runCatching {
            LocalStore(app).edgePanels.any {
                it.showOverApps && (it.side == EdgeSide.LEFT || it.side == EdgeSide.RIGHT)
            }
        }.getOrDefault(false)
        val launcherUi = runCatching { MadApp.instance.foregroundActivity != null }.getOrDefault(false)
        if (!wanted || launcherUi || !Settings.canDrawOverlays(app)) {
            app.stopService(intent)
            return
        }
        runCatching { app.startService(intent) }
    }
}
