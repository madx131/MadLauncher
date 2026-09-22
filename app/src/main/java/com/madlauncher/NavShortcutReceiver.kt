package com.madlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.madlauncher.platform.MadOsRuntime

/** Patched SystemUI sends nav/shade shortcut taps here. */
class NavShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != MadOsRuntime.ACTION_NAV_SHORTCUT) return
        if (!NavShortcutBridge.enabled) return
        val kind = MadOsRuntime.navKindFromExtra(
            intent.getStringExtra(MadOsRuntime.EXTRA_NAV_KIND),
        ) ?: return
        MadApp.instance.openLauncherPanel(MadOsRuntime.panelForKind(kind))
    }
}
