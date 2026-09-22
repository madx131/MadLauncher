package com.madlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.madlauncher.data.LocalStore
import com.madlauncher.notif.SystemShadeController

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        SystemShadeController.syncWithAppearance(context, LocalStore(context).appearanceSettings)
        com.madlauncher.overlay.EdgeDockOverlay.sync(context)
    }
}
