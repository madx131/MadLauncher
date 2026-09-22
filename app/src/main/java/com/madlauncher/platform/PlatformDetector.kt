package com.madlauncher.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object PlatformDetector {
    fun detect(context: Context): OemFamily {
        if (MadOsRuntime.isMadOs(context)) return OemFamily.STOCK
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        val display = Build.DISPLAY.orEmpty().lowercase()
        val pm = context.packageManager

        if (manufacturer.contains("huawei") || brand.contains("huawei") || brand.contains("honor")) {
            return OemFamily.HUAWEI
        }
        if (manufacturer.contains("samsung") || brand.contains("samsung")) {
            return OemFamily.SAMSUNG
        }
        if (manufacturer.contains("oppo") || brand.contains("oppo") || brand.contains("realme")) {
            return OemFamily.OPPO_COLOROS
        }
        if (manufacturer.contains("vivo") || brand.contains("vivo") || brand.contains("iqoo")) {
            return OemFamily.VIVO
        }
        if (manufacturer.contains("oneplus") || brand.contains("oneplus")) {
            return OemFamily.ONEPLUS
        }
        if (isXiaomiFamily(manufacturer, brand)) {
            return if (isHyperOs(display, pm)) OemFamily.XIAOMI_HYPEROS else OemFamily.MIUI
        }
        if (manufacturer.contains("google") || brand.contains("google") || brand.contains("pixel")) {
            return OemFamily.STOCK
        }
        if (display.contains("hyperos") || propContains("ro.mi.os.version")) {
            return OemFamily.XIAOMI_HYPEROS
        }
        if (propContains("ro.miui.ui.version.name") || pmInstalled(pm, "com.miui.securitycenter")) {
            return OemFamily.MIUI
        }
        if (pmInstalled(pm, "com.samsung.android.lool")) return OemFamily.SAMSUNG
        if (pmInstalled(pm, "com.coloros.securepay")) return OemFamily.OPPO_COLOROS
        return OemFamily.UNKNOWN
    }

    private fun isXiaomiFamily(manufacturer: String, brand: String): Boolean {
        val keys = listOf("xiaomi", "redmi", "poco", "blackshark")
        return keys.any { manufacturer.contains(it) || brand.contains(it) }
    }

    private fun isHyperOs(display: String, pm: PackageManager): Boolean {
        if (display.contains("hyperos")) return true
        if (propContains("ro.mi.os.version")) return true
        return pmInstalled(pm, "com.miui.securitycenter") &&
            (Build.VERSION.SDK_INT >= 34 || display.contains("os2"))
    }

    private fun propContains(key: String): Boolean {
        return runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key)
                ?.toString()
                ?.isNotBlank() == true
        }.getOrDefault(false)
    }

    private fun pmInstalled(pm: PackageManager, pkg: String): Boolean {
        return runCatching {
            pm.getPackageInfo(pkg, 0)
            true
        }.getOrDefault(false)
    }
}
