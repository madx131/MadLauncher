package com.madlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madlauncher.data.AppearanceSettings
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.PaletteId
import com.madlauncher.data.ThemeMode
import com.madlauncher.ui.settings.SettingsOverlay
import com.madlauncher.ui.theme.MadLauncherTheme
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun ActionBoxOverlay(
    onFinished: () -> Unit,
    vm: LauncherViewModel = viewModel(),
) {
    SideEffect { vm.hostIndependentActionBox() }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { picked ->
            runCatching {
                val app = vm.getApplication<android.app.Application>()
                val name = "font_${System.currentTimeMillis()}.ttf"
                val out = File(app.filesDir, name)
                app.contentResolver.openInputStream(picked)?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                vm.setCustomFont(out.absolutePath)
            }
        }
    }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.setCustomIcon(it) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshGrants()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        vm.openActionBox()
        snapshotFlow { vm.actionBoxOpen }.first { it }
        snapshotFlow { vm.actionBoxOpen || vm.settingsOpen || vm.setupOpen }.first { !it }
        onFinished()
    }

    var scrimArmed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(450)
        scrimArmed = true
    }

    BackHandler {
        if (!vm.handleBack()) onFinished()
    }

    MadLauncherTheme(
        appearance = hostLauncherAppearance(vm),
        refreshKey = vm.appearanceEpoch,
        wallpaperPalette = null,
    ) {
        val imeBottom = rememberImeBottomDp()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val actionBottom = actionBoxLiftDp(imeBottom, navBottom, closedDock = 72.dp)
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val screenH = LocalConfiguration.current.screenHeightDp.dp
        val expandedMax = (screenH - actionBottom - statusBarHeight - 88.dp)
            .coerceIn(80.dp, 420.dp)
        val dismiss = remember { MutableInteractionSource() }
        val blocking = vm.settingsOpen || vm.helpOpen || vm.setupOpen || vm.appMenu != null

        Box(modifier = Modifier.fillMaxSize().barrelRoll(vm.barrelRollGen)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = dismiss,
                        indication = null,
                    ) {
                        when {
                            vm.appMenu != null -> vm.dismissAppMenu()
                            !blocking && scrimArmed -> vm.closeActionBox()
                        }
                    },
            )
            Column(
                modifier = Modifier
                    .zIndex(30f)
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = actionBottom)
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionBoxIndependentGrants(vm)
                ActionBox(
                    vm = vm,
                    expandedMaxHeight = expandedMax,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (vm.settingsOpen) {
                SettingsOverlay(
                    vm = vm,
                    onPickFont = { fontPicker.launch(arrayOf("font/*", "application/octet-stream")) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            if (vm.setupOpen) {
                SetupPanel(vm)
            }
            if (vm.appMenu != null && vm.appHold == null) {
                Popup(
                    alignment = Alignment.Center,
                    onDismissRequest = { vm.dismissAppMenu() },
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnClickOutside = true,
                        dismissOnBackPress = true,
                    ),
                ) {
                    AppContextMenu(
                        vm = vm,
                        onEditIcon = {
                            vm.requestIconEdit()
                            iconPicker.launch("image/*")
                        },
                    )
                }
            }
        }
    }
}

/** Overlay: system light/dark Material — not Nord, not wallpaper. */
private fun hostLauncherAppearance(vm: LauncherViewModel): AppearanceSettings {
    val base = vm.displayAppearance
    return base.copy(
        themeMode = ThemeMode.SYSTEM,
        palette = PaletteId.MATERIAL,
        accentArgb = 0xFF6750A4,
        windowAlpha = 1f,
        drawerAlpha = 1f,
        dockAlpha = 1f,
        fontPath = "",
    )
}
