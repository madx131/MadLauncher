package com.madlauncher.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.madlauncher.data.LanguageOverlay

@Composable
fun madString(@StringRes id: Int): String {
    val context = LocalContext.current
    val rev = LanguageOverlay.revision.intValue
    return remember(rev, id) {
        context.getString(id)
    }
}
