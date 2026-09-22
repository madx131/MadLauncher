package com.madlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_FULLY_REMOVED,
            -> AppsReloadBus.request()
        }
    }
}
