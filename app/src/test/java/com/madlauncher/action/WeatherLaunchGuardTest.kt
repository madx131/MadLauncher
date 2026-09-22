package com.madlauncher.action

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherLaunchGuardTest {
    @Test
    fun blocksMiuiWeatherPackages() {
        assertTrue(WeatherLaunchGuard.isBlocked("com.miui.weather2"))
        assertTrue(WeatherLaunchGuard.isBlocked("com.xiaomi.mweather"))
    }

    @Test
    fun allowsOtherWeatherApps() {
        assertFalse(WeatherLaunchGuard.isBlocked("com.google.android.apps.weather"))
        assertFalse(WeatherLaunchGuard.isBlocked("com.accuweather.android"))
    }
}
