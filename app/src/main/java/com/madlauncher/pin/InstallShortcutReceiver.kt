package com.madlauncher.pin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Parcelable
import android.util.Log
import android.widget.Toast

/**
 * Legacy [com.android.launcher.action.INSTALL_SHORTCUT] used by Shortcut Maker
 * and older apps when the launcher is not using requestPinShortcut.
 */
class InstallShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(
            TAG,
            "onReceive action=${intent?.action} extras=${intent?.extras?.keySet()} " +
                "component=${intent?.component}",
        )
        if (intent == null) return
        val action = intent.action
        if (action != ACTION_INSTALL && action != ACTION_INSTALL_ALT) {
            Log.w(TAG, "ignored unexpected action=$action")
            return
        }
        val name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)?.trim().orEmpty()
            .ifBlank { intent.getStringExtra("android.intent.extra.shortcut.NAME")?.trim().orEmpty() }
            .ifBlank { "Shortcut" }
        @Suppress("DEPRECATION")
        val launch = intent.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
            ?: intent.getParcelableExtra("android.intent.extra.shortcut.INTENT")
        if (launch == null) {
            Log.e(TAG, "missing EXTRA_SHORTCUT_INTENT")
            Toast.makeText(context, "Invalid shortcut", Toast.LENGTH_SHORT).show()
            return
        }
        @Suppress("DEPRECATION")
        val icon = (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_SHORTCUT_ICON)
            ?: intent.getParcelableExtra("android.intent.extra.shortcut.ICON")) as? Bitmap
        Log.i(TAG, "placing legacy shortcut name=$name launch=$launch")
        val ok = PinShortcutHelper.placeLegacyShortcut(context, name, launch, icon)
        Toast.makeText(
            context,
            if (ok) "Shortcut added" else "No space for shortcut",
            Toast.LENGTH_SHORT,
        ).show()
    }

    companion object {
        private const val TAG = "MadPin"
        const val ACTION_INSTALL = "com.android.launcher.action.INSTALL_SHORTCUT"
        /** Some older apps use the android.intent form. */
        const val ACTION_INSTALL_ALT = "android.intent.action.INSTALL_SHORTCUT"
    }
}
