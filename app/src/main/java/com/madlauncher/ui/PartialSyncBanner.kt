package com.madlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.settings.PlatformPartialSyncBanner

@Composable
fun PartialSyncBanner(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    PlatformPartialSyncBanner(vm, modifier)
}
