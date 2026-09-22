package com.madlauncher.ui.settings

/** Nested settings routes: NAV_SHORTCUTS → EXPERIMENTAL → EXTRAS → HUB. */
object SettingsBackStack {
    fun parent(
        route: SettingsRoute?,
        fromActionBox: Boolean,
        independentActionBox: Boolean,
    ): SettingsRoute? = when (route) {
        SettingsRoute.ACTION_BOX_COMMAND_EDIT -> SettingsRoute.ACTION_BOX_COMMANDS
        SettingsRoute.NAV_SHORTCUTS -> SettingsRoute.EXPERIMENTAL
        SettingsRoute.EXPERIMENTAL, SettingsRoute.NOT_WORKING, SettingsRoute.SHARE_APK -> SettingsRoute.EXTRAS
        SettingsRoute.ACTION_BOX_COMMANDS -> when {
            independentActionBox -> SettingsRoute.HUB
            fromActionBox -> null
            else -> SettingsRoute.EDIT_DOCKS
        }
        SettingsRoute.HUB, null -> null
        else -> SettingsRoute.HUB
    }
}
