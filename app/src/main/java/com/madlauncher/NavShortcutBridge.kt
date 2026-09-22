package com.madlauncher

import com.madlauncher.data.AppearanceSettings
import com.madlauncher.data.NavShortcutMode
import com.madlauncher.platform.MadOsRuntime

object NavShortcutBridge {
    @Volatile
    var settings: AppearanceSettings = AppearanceSettings()
        private set

    fun update(settings: AppearanceSettings) {
        this.settings = settings
        listeners.forEach { it(settings) }
    }

    private val listeners = mutableListOf<(AppearanceSettings) -> Unit>()
    private val rebuildListeners = mutableListOf<() -> Unit>()
    private val layoutListeners = mutableListOf<() -> Unit>()

    fun addListener(listener: (AppearanceSettings) -> Unit) {
        listeners += listener
        listener(settings)
    }

    fun removeListener(listener: (AppearanceSettings) -> Unit) {
        listeners -= listener
    }

    fun addRebuildListener(listener: () -> Unit) {
        rebuildListeners += listener
    }

    fun removeRebuildListener(listener: () -> Unit) {
        rebuildListeners -= listener
    }

    fun addLayoutListener(listener: () -> Unit) {
        layoutListeners += listener
    }

    fun removeLayoutListener(listener: () -> Unit) {
        layoutListeners -= listener
    }

    /** Re-attach nav overlays when they may have been torn down (e.g. Recents Clear all). */
    fun requestRebuild() {
        rebuildListeners.toList().forEach { it() }
    }

    /** Reposition existing nav overlays without remove/re-add (avoids flicker on resume/focus). */
    fun requestLayout() {
        layoutListeners.toList().forEach { it() }
    }

    /** Patched SystemUI renders nav/shade shortcuts — skip a11y overlays. */
    val useNativeSystemUi: Boolean
        get() = runCatching {
            MadOsRuntime.useNativeSystemUi(MadApp.instance)
        }.getOrDefault(false)

    val useClassicNav: Boolean get() = settings.navShortcutMode == NavShortcutMode.CLASSIC

    /** Overlays and MAD intercept of nav taps. Off until Experimental is turned on. */
    val enabled: Boolean get() = settings.experimentalNavShortcuts
}
