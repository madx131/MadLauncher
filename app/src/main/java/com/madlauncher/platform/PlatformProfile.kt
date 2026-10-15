package com.madlauncher.platform

import android.os.Build

data class PlatformProfile(
    val oemFamily: OemFamily,
    val manufacturer: String,
    val model: String,
    val deviceLabel: String,
    val capabilities: Map<PlatformFeature, FeatureStatus>,
    val probedAtMillis: Long,
    /** Installed Action Box apps + permission snapshot from last probe. */
    val actionBoxCatalog: String = "",
) {
    fun status(feature: PlatformFeature): FeatureStatus =
        capabilities[feature] ?: FeatureStatus.UNKNOWN

    fun isEnabled(feature: PlatformFeature): Boolean =
        status(feature).isUsable

    fun isWorking(feature: PlatformFeature): Boolean =
        status(feature) == FeatureStatus.WORKING

    companion object {
        fun baseline(family: OemFamily): Map<PlatformFeature, FeatureStatus> {
            val actionBox = mapOf(
                PlatformFeature.ACTIONBOX_RECIPIENT_HISTORY to FeatureStatus.UNKNOWN,
                PlatformFeature.ACTIONBOX_WHATSAPP_CALL to FeatureStatus.UNKNOWN,
                PlatformFeature.ACTIONBOX_PHONE_CALL to FeatureStatus.UNKNOWN,
                PlatformFeature.ACTIONBOX_OTT_CALL to FeatureStatus.UNAVAILABLE,
                PlatformFeature.ACTIONBOX_SOCIAL_POST to FeatureStatus.UNKNOWN,
                PlatformFeature.ACTIONBOX_META_SOCIAL to FeatureStatus.UNKNOWN,
                PlatformFeature.ACTIONBOX_MESSAGING to FeatureStatus.WORKING,
                PlatformFeature.ACTIONBOX_CONTACT_LOOKUP to FeatureStatus.UNKNOWN,
                PlatformFeature.ACTIONBOX_AI to FeatureStatus.DEGRADED,
                PlatformFeature.ACTIONBOX_TRANSLATE to FeatureStatus.UNKNOWN,
                PlatformFeature.ACTIONBOX_NOTES to FeatureStatus.UNKNOWN,
                PlatformFeature.ACTIONBOX_NAV to FeatureStatus.UNKNOWN,
                PlatformFeature.ACTIONBOX_SOCIAL_DM to FeatureStatus.UNAVAILABLE,
                PlatformFeature.ACTIONBOX_TRUTH_POST to FeatureStatus.UNAVAILABLE,
            )
            return when (family) {
                OemFamily.STOCK, OemFamily.SAMSUNG, OemFamily.ONEPLUS -> mapOf(
                    PlatformFeature.NOTIF_SHADE_MIRROR to FeatureStatus.WORKING,
                    PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC to FeatureStatus.WORKING,
                    PlatformFeature.NOTIF_BADGES to FeatureStatus.WORKING,
                    PlatformFeature.QS_MIRROR to FeatureStatus.WORKING,
                    PlatformFeature.QS_TOGGLE_SYNC to FeatureStatus.WORKING,
                    PlatformFeature.NAV_SHORTCUT_OVERLAY to FeatureStatus.WORKING,
                    PlatformFeature.SYSTEM_SHADE_EXPAND to FeatureStatus.WORKING,
                ) + actionBox
                OemFamily.XIAOMI_HYPEROS, OemFamily.MIUI -> mapOf(
                    PlatformFeature.NOTIF_SHADE_MIRROR to FeatureStatus.DEGRADED,
                    PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC to FeatureStatus.DEGRADED,
                    PlatformFeature.NOTIF_BADGES to FeatureStatus.WORKING,
                    PlatformFeature.QS_MIRROR to FeatureStatus.WORKING,
                    PlatformFeature.QS_TOGGLE_SYNC to FeatureStatus.DEGRADED,
                    PlatformFeature.NAV_SHORTCUT_OVERLAY to FeatureStatus.DEGRADED,
                    PlatformFeature.SYSTEM_SHADE_EXPAND to FeatureStatus.DEGRADED,
                ) + actionBox
                OemFamily.OPPO_COLOROS, OemFamily.VIVO -> mapOf(
                    PlatformFeature.NOTIF_SHADE_MIRROR to FeatureStatus.DEGRADED,
                    PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC to FeatureStatus.DEGRADED,
                    PlatformFeature.NOTIF_BADGES to FeatureStatus.WORKING,
                    PlatformFeature.QS_MIRROR to FeatureStatus.WORKING,
                    PlatformFeature.QS_TOGGLE_SYNC to FeatureStatus.DEGRADED,
                    PlatformFeature.NAV_SHORTCUT_OVERLAY to FeatureStatus.DEGRADED,
                    PlatformFeature.SYSTEM_SHADE_EXPAND to FeatureStatus.DEGRADED,
                ) + actionBox
                OemFamily.HUAWEI -> mapOf(
                    PlatformFeature.NOTIF_SHADE_MIRROR to FeatureStatus.DEGRADED,
                    PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC to FeatureStatus.DEGRADED,
                    PlatformFeature.NOTIF_BADGES to FeatureStatus.WORKING,
                    PlatformFeature.QS_MIRROR to FeatureStatus.DEGRADED,
                    PlatformFeature.QS_TOGGLE_SYNC to FeatureStatus.UNAVAILABLE,
                    PlatformFeature.NAV_SHORTCUT_OVERLAY to FeatureStatus.DEGRADED,
                    PlatformFeature.SYSTEM_SHADE_EXPAND to FeatureStatus.DEGRADED,
                ) + actionBox
                OemFamily.UNKNOWN -> mapOf(
                    PlatformFeature.NOTIF_SHADE_MIRROR to FeatureStatus.UNKNOWN,
                    PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC to FeatureStatus.UNKNOWN,
                    PlatformFeature.NOTIF_BADGES to FeatureStatus.UNKNOWN,
                    PlatformFeature.QS_MIRROR to FeatureStatus.UNKNOWN,
                    PlatformFeature.QS_TOGGLE_SYNC to FeatureStatus.UNKNOWN,
                    PlatformFeature.NAV_SHORTCUT_OVERLAY to FeatureStatus.UNKNOWN,
                    PlatformFeature.SYSTEM_SHADE_EXPAND to FeatureStatus.UNKNOWN,
                ) + actionBox
            }
        }

        fun build(
            family: OemFamily,
            capabilities: Map<PlatformFeature, FeatureStatus>,
            probedAtMillis: Long = System.currentTimeMillis(),
            actionBoxCatalog: String = "",
        ): PlatformProfile {
            val manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "Unknown" }
            val model = Build.MODEL.orEmpty().ifBlank { "Device" }
            return PlatformProfile(
                oemFamily = family,
                manufacturer = manufacturer,
                model = model,
                deviceLabel = "$manufacturer $model",
                capabilities = capabilities,
                probedAtMillis = probedAtMillis,
                actionBoxCatalog = actionBoxCatalog,
            )
        }
    }
}
