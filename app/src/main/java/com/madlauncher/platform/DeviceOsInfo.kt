package com.madlauncher.platform

import android.content.Context
import android.os.Build

/** Device + OEM ROM metadata for compatibility reports. */
object DeviceOsInfo {
    data class Snapshot(
        val manufacturer: String,
        val brand: String,
        val model: String,
        val device: String,
        val product: String,
        val androidRelease: String,
        val androidSdk: Int,
        val nativeOsName: String,
        val nativeOsVersion: String,
        val buildDisplay: String,
        val buildFingerprint: String,
        val oemFamily: OemFamily,
    )

    fun capture(context: Context): Snapshot {
        val family = PlatformDetector.detect(context)
        val nativeName = if (MadOsRuntime.isMadOs(context)) "MAD OS" else nativeOsName(family)
        val nativeVersion = nativeOsVersion(family)
        return Snapshot(
            manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "Unknown" },
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty().ifBlank { "Unknown" },
            device = Build.DEVICE.orEmpty(),
            product = Build.PRODUCT.orEmpty(),
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            androidSdk = Build.VERSION.SDK_INT,
            nativeOsName = nativeName,
            nativeOsVersion = nativeVersion,
            buildDisplay = Build.DISPLAY.orEmpty(),
            buildFingerprint = Build.FINGERPRINT.orEmpty(),
            oemFamily = family,
        )
    }

    private fun nativeOsName(family: OemFamily): String = when (family) {
        OemFamily.XIAOMI_HYPEROS -> "HyperOS"
        OemFamily.MIUI -> "MIUI"
        OemFamily.SAMSUNG -> "One UI"
        OemFamily.OPPO_COLOROS -> "ColorOS"
        OemFamily.VIVO -> "OriginOS"
        OemFamily.ONEPLUS -> "OxygenOS"
        OemFamily.HUAWEI -> "EMUI / HarmonyOS"
        OemFamily.STOCK -> "Stock Android"
        OemFamily.UNKNOWN -> "Unknown"
    }

    private fun nativeOsVersion(family: OemFamily): String {
        return when (family) {
            OemFamily.XIAOMI_HYPEROS, OemFamily.MIUI ->
                readProp("ro.mi.os.version.name")
                    ?: readProp("ro.miui.ui.version.name")
                    ?: readProp("ro.build.version.incremental")
            OemFamily.SAMSUNG ->
                readProp("ro.build.version.oneui")
                    ?: readProp("ro.build.version.sep")
            OemFamily.OPPO_COLOROS ->
                readProp("ro.build.version.opporom")
            OemFamily.VIVO ->
                readProp("ro.vivo.os.version")
            OemFamily.HUAWEI ->
                readProp("ro.build.version.emui")
            else -> Build.DISPLAY.orEmpty()
        }.orEmpty().ifBlank { Build.DISPLAY.orEmpty() }
    }

    private fun readProp(key: String): String? {
        return runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key)
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
