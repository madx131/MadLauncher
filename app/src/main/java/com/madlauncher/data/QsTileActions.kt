package com.madlauncher.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.location.LocationManagerCompat

/**
 * Local actions for stock QS tiles when [SystemQsMirror.clickTile] is a no-op (common on HyperOS).
 */
object QsTileActions {
    fun hasLocalToggle(id: String, spec: String): Boolean {
        val token = specToken(spec)
        if (token.startsWith("custom(")) return false
        return when (token) {
            "mute", "volume", "cell", "wifi", "bt", "bluetooth", "airplane", "flashlight",
            "rotation", "batterysaver", "battery", "autobrightness", "night", "hotspot",
            "location", "nfc", "dark", "nightmode", "datasaver", "data_saver" -> true
            else -> when (id.lowercase()) {
                "sound", "mute", "volume", "cell", "wifi", "bt", "air", "torch", "rotate",
                "battery", "autobrightness", "night", "hotspot", "location", "nfc", "dark",
                "data_saver" -> true
                else -> false
            }
        }
    }

    fun toggle(context: Context, id: String, spec: String): Boolean {
        val key = id.lowercase()
        val token = specToken(spec)
        return when {
            token == "mute" || token == "volume" || key == "sound" || key == "mute" || key == "volume" ->
                cycleRinger(context)
            token == "cell" || key == "cell" -> toggleMobileData(context)
            token == "wifi" || key == "wifi" -> toggleWifi(context)
            token == "bt" || token == "bluetooth" || key == "bt" -> toggleBluetooth(context)
            token == "airplane" || key == "air" -> toggleAirplane(context)
            token == "flashlight" || key == "torch" -> toggleFlashlight(context)
            token == "rotation" || key == "rotate" -> toggleAutoRotate(context)
            token == "batterysaver" || token == "battery" || key == "battery" -> toggleBatterySaver(context)
            token == "autobrightness" || key == "autobrightness" -> toggleAutoBrightness(context)
            token == "night" || key == "night" -> toggleNightLight(context)
            token == "hotspot" || key == "hotspot" -> toggleHotspot(context)
            token == "location" || key == "location" -> toggleLocation(context)
            token == "nfc" || key == "nfc" -> toggleNfc(context)
            token == "dark" || token == "nightmode" || key == "dark" -> toggleDarkTheme(context)
            token == "datasaver" || token == "data_saver" || key == "data_saver" -> toggleDataSaver(context)
            else -> false
        }
    }

    fun cycleRinger(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return false
        return runCatching {
            am.ringerMode = when (am.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            true
        }.getOrDefault(false)
    }

    fun toggleMobileData(context: Context): Boolean {
        val tm = context.getSystemService(TelephonyManager::class.java) ?: return false
        return runCatching {
            val enabled = if (Build.VERSION.SDK_INT >= 26) tm.isDataEnabled
            else Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 1
            val method = tm.javaClass.getDeclaredMethod("setDataEnabled", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(tm, !enabled)
            true
        }.recoverCatching {
            val cur = Settings.Global.getInt(context.contentResolver, "mobile_data", 1)
            Settings.Global.putInt(context.contentResolver, "mobile_data", if (cur == 1) 0 else 1)
            true
        }.getOrDefault(false)
    }

    fun toggleWifi(context: Context): Boolean {
        @Suppress("DEPRECATION")
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return false
        return runCatching {
            @Suppress("DEPRECATION")
            wifi.isWifiEnabled = !wifi.isWifiEnabled
            true
        }.getOrDefault(false)
    }

    fun toggleBluetooth(context: Context): Boolean {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter()
            ?: return false
        return runCatching {
            @Suppress("DEPRECATION")
            if (adapter.isEnabled) adapter.disable() else adapter.enable()
            true
        }.getOrDefault(false)
    }

    private fun toggleAirplane(context: Context): Boolean {
        return runCatching {
            val cr = context.contentResolver
            val on = Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
            Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, if (on) 0 else 1)
            context.sendBroadcast(
                Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", !on),
            )
            true
        }.getOrDefault(false)
    }

    private fun toggleFlashlight(context: Context): Boolean {
        val cm = context.getSystemService(CameraManager::class.java) ?: return false
        val cameraId = cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        return runCatching {
            val on = Settings.Global.getInt(context.contentResolver, "flashlight_enabled", 0) == 1
            cm.setTorchMode(cameraId, !on)
            true
        }.getOrDefault(false)
    }

    private fun toggleAutoRotate(context: Context): Boolean {
        return runCatching {
            val cr = context.contentResolver
            val on = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
            Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, if (on) 0 else 1)
            true
        }.getOrDefault(false)
    }

    private fun toggleBatterySaver(context: Context): Boolean {
        val pm = context.getSystemService(android.os.PowerManager::class.java) ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val method = pm.javaClass.getMethod("setPowerSaveModeEnabled", Boolean::class.javaPrimitiveType)
                method.invoke(pm, !pm.isPowerSaveMode)
                true
            } else false
        }.getOrDefault(false)
    }

    private fun toggleAutoBrightness(context: Context): Boolean {
        return runCatching {
            val cr = context.contentResolver
            val auto = Settings.System.getInt(
                cr,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            Settings.System.putInt(
                cr,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                else Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
            )
            true
        }.getOrDefault(false)
    }

    private fun toggleNightLight(context: Context): Boolean {
        return runCatching {
            val cr = context.contentResolver
            val on = Settings.Secure.getInt(cr, "night_display_activated", 0) == 1
            Settings.Secure.putInt(cr, "night_display_activated", if (on) 0 else 1)
            true
        }.getOrDefault(false)
    }

    private fun toggleHotspot(context: Context): Boolean {
        @Suppress("DEPRECATION")
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return false
        return runCatching {
            val method = wifi.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            val on = method.invoke(wifi) as? Boolean == true
            val setMethod = wifi.javaClass.getDeclaredMethod("setWifiApEnabled", android.net.wifi.WifiConfiguration::class.java, Boolean::class.javaPrimitiveType)
            setMethod.isAccessible = true
            setMethod.invoke(wifi, null, !on)
            true
        }.getOrDefault(false)
    }

    private fun toggleLocation(context: Context): Boolean {
        return runCatching {
            val lm = context.getSystemService(LocationManager::class.java) ?: return false
            val enabled = LocationManagerCompat.isLocationEnabled(lm)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val method = LocationManager::class.java.getMethod(
                    "setLocationEnabledForUser",
                    Boolean::class.javaPrimitiveType,
                    android.os.UserHandle::class.java,
                )
                method.invoke(lm, !enabled, android.os.Process.myUserHandle())
                true
            } else false
        }.getOrDefault(false)
    }

    private fun toggleNfc(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
        return runCatching {
            val method = if (adapter.isEnabled) "disable" else "enable"
            NfcAdapter::class.java.getMethod(method).invoke(adapter)
            true
        }.getOrDefault(false)
    }

    private fun toggleDarkTheme(context: Context): Boolean {
        return runCatching {
            val uiMode = context.resources.configuration.uiMode
            val night = uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mgr = context.getSystemService(android.app.UiModeManager::class.java) ?: return false
                mgr.nightMode = if (night) android.app.UiModeManager.MODE_NIGHT_NO
                else android.app.UiModeManager.MODE_NIGHT_YES
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    private fun toggleDataSaver(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return false
        return runCatching {
            val on = cm.restrictBackgroundStatus ==
                android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
            val method = ConnectivityManager::class.java.getMethod(
                "setRestrictBackgroundStatus",
                Boolean::class.javaPrimitiveType,
            )
            method.invoke(cm, !on)
            true
        }.getOrDefault(false)
    }

    private fun specToken(spec: String): String = spec.trim().lowercase()
}
