package com.madlauncher

import com.madlauncher.data.AppearanceSettings

import com.madlauncher.platform.MadOsRuntime

object AppearanceBridge {
    @Volatile
    var settings: AppearanceSettings = AppearanceSettings()
        private set

    fun update(settings: AppearanceSettings) {
        this.settings = settings
        runCatching {
            MadOsRuntime.publishNavSlots(MadApp.instance, settings)
            com.madlauncher.systemui.MadNavBarOverlay.sync(MadApp.instance)
        }
        listeners.forEach { it(settings) }
    }

    private val listeners = mutableListOf<(AppearanceSettings) -> Unit>()

    fun addListener(listener: (AppearanceSettings) -> Unit) {
        listeners += listener
        listener(settings)
    }

    fun removeListener(listener: (AppearanceSettings) -> Unit) {
        listeners -= listener
    }
}
