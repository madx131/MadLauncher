package com.madlauncher.data

import android.graphics.drawable.Drawable

data class PersistentApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val subtitle: String,
    val isMedia: Boolean = false,
    val isPlaying: Boolean = false,
    val canSkip: Boolean = false,
)
