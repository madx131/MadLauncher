package com.madlauncher.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.madlauncher.platform.MadOsRuntime

/** Missing system grants to prompt before turning an experimental flag on. */
object ExperimentalGrants {
    const val NLS = "nls"
    const val OVERLAY = "overlay"
    const val A11Y = "a11y"
    const val USAGE = "usage"

    enum class Feature { MAD_WINDOWS, NAV_SHORTCUTS, MAD_RECENTS }

    fun missing(context: Context, grants: GrantState, feature: Feature): List<String> {
        val out = mutableListOf<String>()
        when (feature) {
            Feature.MAD_WINDOWS -> {
                if (!grants.notificationListener) out += NLS
                if (!grants.overlay) out += OVERLAY
            }
            Feature.NAV_SHORTCUTS -> {
                if (!MadOsRuntime.isMadOs(context) && !grants.accessibility) out += A11Y
                if (!grants.overlay) out += OVERLAY
            }
            Feature.MAD_RECENTS -> {
                if (!grants.usageStats) out += USAGE
            }
        }
        return out
    }

    fun settingsIntent(context: Context, id: String): Intent = when (id) {
        NLS -> PermissionChecker.notificationListenerSettings()
        OVERLAY -> PermissionChecker.overlaySettings(context)
        A11Y -> PermissionChecker.accessibilitySettings(context)
        USAGE -> PermissionChecker.usageAccessSettings()
        else -> Intent(Settings.ACTION_SETTINGS)
    }
}
