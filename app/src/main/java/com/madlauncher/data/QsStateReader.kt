package com.madlauncher.data



import android.bluetooth.BluetoothManager

import android.content.Context

import android.content.pm.PackageManager

import android.location.LocationManager

import android.media.AudioManager

import android.net.ConnectivityManager

import android.net.wifi.WifiManager

import android.nfc.NfcAdapter

import android.os.Build

import android.os.PowerManager

import android.provider.Settings

import android.telephony.TelephonyManager

import androidx.core.location.LocationManagerCompat



/** Reads live on/off state for launcher quick-settings tiles. */

object QsStateReader {

    /** Stock / HyperOS tiles we can read without a TileService binding. */

    fun canReadState(id: String, spec: String = id): Boolean {

        val token = specToken(spec)

        if (token.startsWith("custom(")) return false

        val key = when {

            token.isNotBlank() -> token

            else -> id.lowercase()

        }

        return key in readableTokens || id.lowercase() in readableIds

    }



    fun isEnabled(context: Context, id: String, spec: String = id): Boolean {

        val token = specToken(spec)

        return runCatching {

            when {

                token == "mute" || id == "sound" || id == "mute" || id == "volume" ->

                    ringerMode(context) != AudioManager.RINGER_MODE_NORMAL

                token == "wifi" || id == "wifi" -> wifiEnabled(context)

                token == "bt" || token == "bluetooth" || id == "bt" -> btEnabled(context)

                token == "cell" || id == "cell" -> readMobileData(context)

                token == "airplane" || id == "air" ->

                    Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

                token == "quietmode" || token == "dnd" || id == "dnd" -> DndController.isEnabled(context)

                token == "flashlight" || id == "torch" -> readFlashlight(context)

                token == "rotation" || id == "rotate" ->

                    Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

                token == "batterysaver" || token == "battery" || id == "battery" ->

                    context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

                token == "location" || id == "location" -> {

                    val lm = context.getSystemService(LocationManager::class.java) ?: return false

                    LocationManagerCompat.isLocationEnabled(lm)

                }

                token == "night" || id == "night" ->

                    Settings.Secure.getInt(context.contentResolver, "night_display_activated", 0) == 1

                token == "nightmode" || token == "dark" || id == "dark" -> {

                    val night = context.resources.configuration.uiMode and

                        android.content.res.Configuration.UI_MODE_NIGHT_MASK

                    night == android.content.res.Configuration.UI_MODE_NIGHT_YES

                }

                token == "nfc" || id == "nfc" -> NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

                token == "datasaver" || token == "data_saver" || id == "data_saver" -> readDataSaver(context)

                token == "hotspot" || id == "hotspot" -> readHotspot(context)

                token == "autobrightness" -> readAutoBrightness(context)

                token == "internet" -> wifiEnabled(context) || readMobileData(context)
                else -> false
            }
        }.getOrDefault(false)
    }



    /** Live label for profile-style tiles (mirrors status-bar mute/sound/vibrate). */

    fun liveLabel(context: Context, id: String, fallback: String, spec: String = id): String {

        val token = specToken(spec)

        return when {

            token == "mute" || id == "sound" || id == "mute" -> when (ringerMode(context)) {

                AudioManager.RINGER_MODE_SILENT -> "Mute"

                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"

                else -> "Sound"

            }

            token == "internet" -> "Internet"

            token == "quietmode" -> "Do Not Disturb"

            token == "autobrightness" -> "Auto brightness"

            else -> fallback

        }

    }



    fun ringerMode(context: Context): Int {

        return runCatching {

            context.getSystemService(AudioManager::class.java)?.ringerMode

                ?: AudioManager.RINGER_MODE_NORMAL

        }.getOrDefault(AudioManager.RINGER_MODE_NORMAL)

    }



    fun readMobileData(context: Context): Boolean {

        return runCatching {

            val tm = context.getSystemService(TelephonyManager::class.java) ?: return false

            if (Build.VERSION.SDK_INT >= 26) tm.isDataEnabled

            else Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 1

        }.getOrDefault(false)

    }



    fun availableOnDevice(context: Context): List<QsTileSpec> {

        val pm = context.packageManager

        return QsCatalog.all.filter { spec ->

            when (spec.id) {

                "nfc" -> pm.hasSystemFeature(PackageManager.FEATURE_NFC)

                "torch" -> pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

                "cell" -> pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

                "wallet" ->

                    pm.getLaunchIntentForPackage("com.google.android.apps.walletnfcrel") != null ||

                        pm.getLaunchIntentForPackage("com.google.android.gms") != null

                else -> true

            }

        }

    }



    private fun specToken(spec: String): String {

        val s = spec.trim()

        if (s.startsWith("custom(", ignoreCase = true)) return s.lowercase()

        return s.lowercase()

    }



    private fun wifiEnabled(context: Context): Boolean {

        @Suppress("DEPRECATION")

        return context.applicationContext.getSystemService(WifiManager::class.java)?.isWifiEnabled == true

    }



    private fun btEnabled(context: Context): Boolean {

        val mgr = context.getSystemService(BluetoothManager::class.java)

        return mgr?.adapter?.isEnabled == true

    }



    private fun readDataSaver(context: Context): Boolean {

        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false

        if (Build.VERSION.SDK_INT < 24) return false

        return cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED

    }



    private fun readHotspot(context: Context): Boolean {

        return runCatching {

            @Suppress("DEPRECATION")

            val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return false

            val method = wifi.javaClass.getDeclaredMethod("isWifiApEnabled")

            method.isAccessible = true

            method.invoke(wifi) as? Boolean == true

        }.getOrDefault(false)

    }



    private fun readAutoBrightness(context: Context): Boolean {

        return Settings.System.getInt(

            context.contentResolver,

            Settings.System.SCREEN_BRIGHTNESS_MODE,

            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,

        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    }



    private fun readFlashlight(context: Context): Boolean {
        return Settings.Global.getInt(context.contentResolver, "flashlight_enabled", 0) == 1
    }

    private val readableIds = setOf(

        "wifi", "bt", "air", "dnd", "location", "rotate", "battery", "dark", "night",

        "nfc", "data_saver", "hotspot", "cell", "sound", "mute", "volume", "torch",

    )



    private val readableTokens = setOf(

        "wifi", "bt", "bluetooth", "cell", "airplane", "quietmode", "dnd", "mute",

        "flashlight", "rotation", "batterysaver", "battery", "location", "night",

        "nightmode", "dark", "nfc", "datasaver", "data_saver", "hotspot", "internet",

        "autobrightness",

    )

}


