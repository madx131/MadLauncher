package com.madlauncher.platform

enum class OemFamily {
    STOCK,
    SAMSUNG,
    XIAOMI_HYPEROS,
    MIUI,
    OPPO_COLOROS,
    VIVO,
    ONEPLUS,
    HUAWEI,
    UNKNOWN,
    ;

    val displayName: String
        get() = when (this) {
            STOCK -> "Stock Android"
            SAMSUNG -> "Samsung One UI"
            XIAOMI_HYPEROS -> "Xiaomi HyperOS"
            MIUI -> "Xiaomi MIUI"
            OPPO_COLOROS -> "Oppo ColorOS"
            VIVO -> "Vivo OriginOS"
            ONEPLUS -> "OnePlus OxygenOS"
            HUAWEI -> "Huawei EMUI"
            UNKNOWN -> "Unknown ROM"
        }
}

enum class PlatformFeature {
    NOTIF_SHADE_MIRROR,
    NOTIF_BIDIRECTIONAL_SYNC,
    NOTIF_BADGES,
    QS_MIRROR,
    QS_TOGGLE_SYNC,
    NAV_SHORTCUT_OVERLAY,
    SYSTEM_SHADE_EXPAND,
    ACTIONBOX_RECIPIENT_HISTORY,
    ACTIONBOX_WHATSAPP_CALL,
    ACTIONBOX_PHONE_CALL,
    ACTIONBOX_OTT_CALL,
    ACTIONBOX_SOCIAL_POST,
    ACTIONBOX_META_SOCIAL,
    ACTIONBOX_MESSAGING,
    ACTIONBOX_CONTACT_LOOKUP,
    ACTIONBOX_AI,
    ACTIONBOX_TRANSLATE,
    ACTIONBOX_NOTES,
    ACTIONBOX_NAV,
}

enum class FeatureStatus {
    WORKING,
    DEGRADED,
    UNAVAILABLE,
    UNKNOWN,
    /** Catalog app / hardware is not on this device — not a MAD failure. */
    NOT_INSTALLED,
    ;

    val isUsable: Boolean
        get() = this == WORKING || this == DEGRADED || this == UNKNOWN

    /** Counts toward "limited / not working". Missing apps do not. */
    val isIssue: Boolean
        get() = this == DEGRADED || this == UNAVAILABLE || this == UNKNOWN
}
