package com.madlauncher.about

/** A library compiled into this build, with the project's page and the developers' GitHub page. */
data class OssCredit(
    val name: String,
    val role: String,
    val author: String,
    val projectUrl: String,
    val authorUrl: String,
    val license: String,
)

object OssCredits {
    const val DUCKDUCKGO_PLAY =
        "https://play.google.com/store/apps/details?id=com.duckduckgo.mobile.android"
    const val DUCKDUCKGO_MARKET =
        "market://details?id=com.duckduckgo.mobile.android"

    val all: List<OssCredit> = listOf(
        OssCredit(
            name = "Coil",
            role = "Loads app icons and pictures",
            author = "Colin White",
            projectUrl = "https://github.com/coil-kt/coil",
            authorUrl = "https://github.com/colinrtwhite",
            license = "Apache 2.0",
        ),
        OssCredit(
            name = "ZXing",
            role = "Draws the share QR code",
            author = "ZXing team",
            projectUrl = "https://github.com/zxing/zxing",
            authorUrl = "https://github.com/zxing",
            license = "Apache 2.0",
        ),
        OssCredit(
            name = "kotlinx.coroutines",
            role = "Background work in the launcher",
            author = "Kotlin team",
            projectUrl = "https://github.com/Kotlin/kotlinx.coroutines",
            authorUrl = "https://github.com/Kotlin",
            license = "Apache 2.0",
        ),
        OssCredit(
            name = "AndroidX Jetpack",
            role = "Compose UI, lifecycle, and app core",
            author = "Android team",
            projectUrl = "https://github.com/androidx/androidx",
            authorUrl = "https://github.com/androidx",
            license = "Apache 2.0",
        ),
        OssCredit(
            name = "ML Kit",
            role = "On-device translation and language detection",
            author = "Google",
            projectUrl = "https://github.com/googlesamples/mlkit",
            authorUrl = "https://github.com/google",
            license = "ML Kit terms",
        ),
    )
}
