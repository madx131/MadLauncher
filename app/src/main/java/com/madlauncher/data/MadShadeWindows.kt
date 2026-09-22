package com.madlauncher.data

import com.madlauncher.platform.PlatformFeature

/**
 * MAD Messages / System & apps / Quick Settings windows. Off by default;
 * stock shade is used until the user turns Experimental on.
 */
object MadShadeWindows {
    val experimentalFeatures: Set<PlatformFeature> = setOf(
        PlatformFeature.NOTIF_SHADE_MIRROR,
        PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC,
        PlatformFeature.QS_MIRROR,
        PlatformFeature.QS_TOGGLE_SYNC,
    )

    fun gestureActions(experimental: Boolean): List<GestureActionId> =
        if (experimental) {
            GestureActionId.entries
        } else {
            GestureActionId.entries.filter { it != GestureActionId.SYSTEM_APPS }
        }

    fun navKinds(experimental: Boolean): List<NavShortcutKind> =
        if (experimental) {
            NavShortcutKind.entries
        } else {
            NavShortcutKind.entries.filter { it != NavShortcutKind.SYSTEM_APPS }
        }

    fun sanitize(settings: AppearanceSettings): AppearanceSettings {
        if (settings.experimentalMadWindows) return settings
        val gestures = settings.gestures.mapValues { (_, action) ->
            if (action == GestureActionId.SYSTEM_APPS) GestureActionId.NOTIFICATIONS else action
        }
        val slots = settings.navShortcutSlots.filter { it.kind != NavShortcutKind.SYSTEM_APPS }
        return settings.copy(
            gestures = gestures,
            navShortcutSlots = NavShortcutPlacements.normalize(slots),
        )
    }
}
