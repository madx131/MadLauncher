package com.madlauncher.platform

import android.content.Context
import com.madlauncher.action.CallAppRegistry
import com.madlauncher.data.PermissionChecker
import com.madlauncher.data.SystemQsMirror
import com.madlauncher.notif.NotificationHub
import kotlinx.coroutines.delay

object PlatformProber {
    suspend fun probe(context: Context, family: OemFamily, recheckDelayMs: Long = 0L): PlatformProfile {
        if (recheckDelayMs > 0L) delay(recheckDelayMs)

        val nlsGranted = PermissionChecker.isNotificationListener(context)
        val nlsConnected = NotificationHub.connected.value
        val a11yGranted = PermissionChecker.isAccessibilityEnabled(context)
        val qsReadable = SystemQsMirror.readRawSpecs(context).isNotEmpty()

        val caps = PlatformProfile.baseline(family).toMutableMap()

        if (!nlsGranted) {
            caps[PlatformFeature.NOTIF_SHADE_MIRROR] = FeatureStatus.UNKNOWN
            caps[PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC] = FeatureStatus.UNAVAILABLE
            caps[PlatformFeature.NOTIF_BADGES] = FeatureStatus.UNAVAILABLE
        } else {
            // Badges, recents-adjacent unread, and shade copy all work once NLS is granted.
            caps[PlatformFeature.NOTIF_BADGES] = FeatureStatus.WORKING
            if (!nlsConnected) {
                caps[PlatformFeature.NOTIF_SHADE_MIRROR] = FeatureStatus.DEGRADED
                caps[PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC] = FeatureStatus.DEGRADED
            } else {
                val stickyCancel = family == OemFamily.XIAOMI_HYPEROS ||
                    family == OemFamily.MIUI ||
                    family == OemFamily.OPPO_COLOROS ||
                    family == OemFamily.VIVO ||
                    family == OemFamily.HUAWEI
                caps[PlatformFeature.NOTIF_SHADE_MIRROR] =
                    if (stickyCancel) FeatureStatus.DEGRADED else FeatureStatus.WORKING
                caps[PlatformFeature.NOTIF_BIDIRECTIONAL_SYNC] =
                    if (stickyCancel) FeatureStatus.DEGRADED else FeatureStatus.WORKING
            }
        }

        if (!qsReadable) {
            val cur = caps[PlatformFeature.QS_MIRROR] ?: FeatureStatus.UNKNOWN
            if (cur == FeatureStatus.WORKING) {
                caps[PlatformFeature.QS_MIRROR] = FeatureStatus.UNAVAILABLE
            }
            caps[PlatformFeature.QS_TOGGLE_SYNC] = FeatureStatus.UNAVAILABLE
        }

        if (MadOsRuntime.useNativeSystemUi(context) || a11yGranted) {
            caps[PlatformFeature.NAV_SHORTCUT_OVERLAY] = FeatureStatus.WORKING
        } else if (MadOsRuntime.isMadOs(context)) {
            caps[PlatformFeature.NAV_SHORTCUT_OVERLAY] = FeatureStatus.DEGRADED
        } else if (!a11yGranted) {
            caps[PlatformFeature.NAV_SHORTCUT_OVERLAY] = FeatureStatus.UNAVAILABLE
        } else if (family == OemFamily.XIAOMI_HYPEROS || family == OemFamily.MIUI) {
            caps[PlatformFeature.NAV_SHORTCUT_OVERLAY] = FeatureStatus.DEGRADED
        }

        if (family == OemFamily.XIAOMI_HYPEROS || family == OemFamily.MIUI) {
            caps[PlatformFeature.QS_TOGGLE_SYNC] = FeatureStatus.DEGRADED
            caps[PlatformFeature.SYSTEM_SHADE_EXPAND] = FeatureStatus.DEGRADED
        }

        val actionBox = ActionBoxCapabilityProbe.probe(context)
        caps.putAll(actionBox.capabilities)

        return PlatformProfile.build(
            family = family,
            capabilities = caps,
            actionBoxCatalog = actionBox.catalogSummary,
        )
    }

    fun unsupportedOttCallNames(mode: com.madlauncher.action.CallAppMode = com.madlauncher.action.CallAppMode.Voice): String =
        CallAppRegistry.unsupportedCatalogNames(mode).joinToString(", ")
}
