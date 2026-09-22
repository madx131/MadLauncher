package com.madlauncher.platform

import com.madlauncher.data.GrantState
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.Locale

data class CompatibilityReport(
    val device: DeviceOsInfo.Snapshot,
    val madVersion: String,
    val probedAtMillis: Long,
    val features: Map<PlatformFeature, FeatureStatus>,
    val grants: GrantState,
    val userNotes: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val actionBoxCatalog: String = "",
) {
    val limitedFeatures: List<PlatformFeature>
        get() = features.filter { (_, s) -> s.isIssue }.keys.toList()

    val limitedSummary: String
        get() = if (limitedFeatures.isEmpty()) {
            "All probed features working"
        } else {
            limitedFeatures.joinToString("\n") { f ->
                "- ${PlatformHelp.featureTitle(f)}: ${PlatformHelp.statusLabel(features[f] ?: FeatureStatus.UNKNOWN)}"
            }
        }

    fun toPlainText(): String = buildString {
        appendLine("MAD Launcher — Compatibility self-test")
        appendLine("Generated: ${DateFormat.getDateTimeInstance().format(Date(probedAtMillis))}")
        appendLine("MAD version: $madVersion")
        appendLine()
        appendLine("=== Device ===")
        appendLine("Phone model: ${device.manufacturer} ${device.model}")
        appendLine("Device codename: ${device.device}")
        appendLine("Android: ${device.androidRelease} (API ${device.androidSdk})")
        appendLine("Native OS: ${device.nativeOsName} ${device.nativeOsVersion}")
        appendLine("ROM family: ${device.oemFamily.displayName}")
        appendLine("Build display: ${device.buildDisplay}")
        appendLine()
        appendLine("=== Permissions granted ===")
        appendLine("Default home: ${grants.defaultHome}")
        appendLine("Notification listener: ${grants.notificationListener}")
        appendLine("Overlay: ${grants.overlay}")
        appendLine("Accessibility: ${grants.accessibility}")
        appendLine("Contacts: ${grants.contacts}")
        appendLine("Post notifications: ${grants.postNotifications}")
        appendLine("Battery exempt: ${grants.battery}")
        appendLine()
        appendLine("=== Feature probe ===")
        PlatformFeature.entries.forEach { f ->
            val s = features[f] ?: FeatureStatus.UNKNOWN
            appendLine("${PlatformHelp.featureTitle(f)}: ${PlatformHelp.statusLabel(s)}")
        }
        if (actionBoxCatalog.isNotBlank()) {
            appendLine()
            appendLine("=== Action Box apps discovered ===")
            appendLine(actionBoxCatalog.trim())
        }
        appendLine()
        appendLine("=== Not working / limited on this device ===")
        appendLine(limitedSummary)
        if (userNotes.isNotBlank()) {
            appendLine()
            appendLine("=== User notes ===")
            appendLine(userNotes.trim())
        }
        if (userName.isNotBlank() || userEmail.isNotBlank()) {
            appendLine()
            appendLine("Contact: ${userName.trim()} ${userEmail.trim()}".trim())
        }
    }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("madVersion", madVersion)
        obj.put("probedAtMillis", probedAtMillis)
        obj.put("phoneModel", "${device.manufacturer} ${device.model}")
        obj.put("androidVersion", "${device.androidRelease} (API ${device.androidSdk})")
        obj.put("nativeOs", device.nativeOsName)
        obj.put("nativeOsVersion", device.nativeOsVersion)
        obj.put("oemFamily", device.oemFamily.name)
        obj.put("limitedFeatures", limitedSummary)
        obj.put("userNotes", userNotes)
        obj.put("userName", userName)
        obj.put("userEmail", userEmail)
        val feat = JSONObject()
        features.forEach { (k, v) -> feat.put(k.name, v.name) }
        obj.put("features", feat)
        obj.put("actionBoxCatalog", actionBoxCatalog)
        return obj
    }

    fun formSubject(): String =
        "MAD compat: ${device.manufacturer} ${device.model} / ${device.nativeOsName} ${device.nativeOsVersion}"
            .take(120)
}
