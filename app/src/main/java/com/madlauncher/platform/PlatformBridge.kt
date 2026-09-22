package com.madlauncher.platform

/** Process-wide capability snapshot for services that cannot reach [LauncherViewModel]. */
object PlatformBridge {
    @Volatile
    var profile: PlatformProfile = PlatformProfile.build(
        OemFamily.UNKNOWN,
        PlatformProfile.baseline(OemFamily.UNKNOWN),
        probedAtMillis = 0L,
    )

    fun status(feature: PlatformFeature): FeatureStatus = profile.status(feature)

    fun isEnabled(feature: PlatformFeature): Boolean = profile.isEnabled(feature)

    fun update(newProfile: PlatformProfile) {
        profile = newProfile
    }
}
