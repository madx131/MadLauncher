package com.madlauncher.ui

import android.app.Activity
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madlauncher.LauncherCommands
import com.madlauncher.MadApp
import com.madlauncher.R
import com.madlauncher.data.GestureActionId
import com.madlauncher.data.GestureId
import com.madlauncher.data.HomePageKind
import com.madlauncher.data.NavShortcutKind
import com.madlauncher.data.NavShortcutMode
import com.madlauncher.NavShortcutBridge
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.settings.SettingsOverlay
import com.madlauncher.ui.theme.LocalMadTheme
import com.madlauncher.ui.theme.MadLauncherTheme
import java.io.File
import androidx.compose.ui.platform.LocalContext

@Composable
fun LauncherScreen(rootModifier: Modifier = Modifier) {
    val vm: LauncherViewModel = viewModel()
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val wallpaperCropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.onSystemWallpaperCropFinished(result.resultCode == Activity.RESULT_OK)
    }
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { picked ->
            val crop = vm.prepareWallpaperUri(picked)
            if (crop != null) wallpaperCropLauncher.launch(crop)
        }
    }
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.setCustomIcon(it) }
    }
    val stickyImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) vm.addStickyImages(uris)
    }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { picked ->
            runCatching {
                val name = "font_${System.currentTimeMillis()}.ttf"
                val out = File(vm.getApplication<android.app.Application>().filesDir, name)
                vm.getApplication<android.app.Application>().contentResolver.openInputStream(picked)?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                vm.setCustomFont(out.absolutePath)
            }
        }
    }
    MadLauncherTheme(
        appearance = vm.displayAppearance,
        refreshKey = vm.appearanceEpoch,
        wallpaperPalette = vm.wallpaperPalette,
    ) {
        LauncherScreenBody(
            vm = vm,
            rootModifier = rootModifier,
            onRootPositioned = { rootCoords = it },
            wallpaperPicker = { wallpaperPicker.launch("image/*") },
            iconPicker = { iconPicker.launch("image/*") },
            fontPicker = { fontPicker.launch(arrayOf("font/*", "application/octet-stream")) },
            stickyImagePicker = { stickyImagePicker.launch("image/*") },
        )
    }
}

@Composable
private fun LauncherScreenBody(
    vm: LauncherViewModel,
    rootModifier: Modifier,
    onRootPositioned: (LayoutCoordinates) -> Unit,
    wallpaperPicker: () -> Unit,
    iconPicker: () -> Unit,
    fontPicker: () -> Unit,
    stickyImagePicker: () -> Unit,
) {
    val theme = LocalMadTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as? Activity
    val appContext = LocalContext.current.applicationContext
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // ADJUST_RESIZE + IME padding double-lifts the pill; match ActionBoxActivity.
    SideEffect {
        activity?.window?.setSoftInputMode(
            if (vm.actionBoxOpen) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            },
        )
    }
    DisposableEffect(activity) {
        onDispose {
            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
    BackHandler { vm.handleBack() }
    val widgetBindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.onAppWidgetBindResult(result.resultCode == Activity.RESULT_OK)
    }
    val widgetConfigLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.onAppWidgetConfigResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(vm.widgetBindIntent) {
        vm.widgetBindIntent?.let { widgetBindLauncher.launch(it) }
    }
    LaunchedEffect(vm.widgetConfigIntent) {
        vm.widgetConfigIntent?.let { widgetConfigLauncher.launch(it) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshGrants()
                vm.refreshSystemRecents()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val app = appContext as? MadApp
        app?.panelOpener = { panel -> vm.openFromOverlay(panel) }
        app?.windowDismisser = { vm.dismissLauncherWindows() }
        app?.backHandler = { vm.handleBack() }
        app?.homeHandler = { vm.homePressed() }
        try {
            LauncherCommands.pendingOpen.collect { panel ->
                if (panel != null) {
                    vm.openFromOverlay(panel)
                    LauncherCommands.pendingOpen.value = null
                }
            }
        } finally {
            if (app?.panelOpener != null) {
                app.panelOpener = null
            }
            if (app?.windowDismisser != null) {
                app.windowDismisser = null
            }
            if (app?.backHandler != null) {
                app.backHandler = null
            }
            if (app?.homeHandler != null) {
                app.homeHandler = null
            }
        }
    }
    LaunchedEffect(Unit) {
        LauncherCommands.homePulse.collect { pulse ->
            if (LauncherCommands.shouldHandleHomePulse(pulse)) {
                vm.homePressed()
            }
        }
    }

    Box(
        modifier = rootModifier
            .fillMaxSize()
            .barrelRoll(vm.barrelRollGen)
            .onGloballyPositioned { coords ->
                rootCoords = coords
                onRootPositioned(coords)
            }
            .onPreviewKeyEvent { event ->
                val enter = event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter
                if (!enter) return@onPreviewKeyEvent false
                if (!(vm.actionBoxOpen || vm.eatActionEnter)) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyUp) {
                    if (vm.actionBoxOpen) vm.runPrimaryAction()
                    vm.eatActionEnter = false
                } else {
                    vm.eatActionEnter = true
                }
                true
            },
    ) {
        WallpaperLayer(vm.wallpaperPath)
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        var statusSwipeAcc by remember { mutableFloatStateOf(0f) }
        val landscape =
            LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val landscapeGestures = vm.displayAppearance.landscapeGesturesEnabled
        val homeGesturesEnabled = (!landscape || landscapeGestures) &&
            !vm.homeEditMode &&
            vm.editingWidgetId == null &&
            !vm.appDrawerOpen
        val currentHomeKind = vm.homePages.getOrNull(vm.homePageIndex)?.kind
        val allowVerticalHomeGestures = currentHomeKind != HomePageKind.NEWS
        val stealVerticalHomeGestures = currentHomeKind == HomePageKind.FREQUENT

        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                LandscapeKeepHost(
                    enabled = vm.displayAppearance.landscapeRotateContents,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HomeDesktop(
                            vm = vm,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .launcherHomeGestures(
                                    enabled = homeGesturesEnabled,
                                    allowVertical = allowVerticalHomeGestures,
                                    stealVertical = stealVerticalHomeGestures,
                                    onSwipeUp = { vm.runGestureAction(GestureId.SWIPE_UP) },
                                    onSwipeDown = { vm.runGestureAction(GestureId.SWIPE_DOWN) },
                                    onTwoFingerUp = { vm.runGestureAction(GestureId.TWO_FINGER_UP) },
                                    onTwoFingerDown = { vm.runGestureAction(GestureId.TWO_FINGER_DOWN) },
                                    onPinchIn = { vm.runGestureAction(GestureId.PINCH_IN) },
                                    onPinchOut = { vm.runGestureAction(GestureId.PINCH_OUT) },
                                ),
                        )
                        BottomDock(vm)
                    }

                if (vm.homeEditMode) {
                    HomeCustomizeSheet(
                        vm = vm,
                        onPickWallpaper = wallpaperPicker,
                        modifier = Modifier
                            .zIndex(12f)
                            .align(Alignment.BottomCenter)
                            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                    )
                }

                if (vm.hasOutsideTargets) {
                    Box(
                        modifier = Modifier
                            .zIndex(18f)
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            ) { vm.dismissOutsideFloats() },
                    )
                }

                vm.floatingNote?.let { note ->
                    FloatingNote(
                        note = note,
                        onDismiss = { vm.dismissFloatingNote() },
                        onTextChange = { vm.updateFloatingNoteText(it) },
                        onTitleChange = { vm.updateFloatingNoteTitle(it) },
                        onCollapsedChange = { vm.setFloatingNoteCollapsed(it) },
                        onLayoutChange = { x, y, edge -> vm.setFloatingNoteLayout(x, y, edge) },
                        onAttachImage = stickyImagePicker,
                        onRemoveImage = { vm.removeStickyImage(it) },
                        autoFocus = vm.editingStickyId == note.id && !note.collapsed,
                        onAutoFocusConsumed = { vm.clearEditingSticky() },
                    )
                }

                vm.floatingConvs.forEach { conv ->
                    FloatingNotifWidget(vm = vm, conv = conv)
                }

                if (vm.appDrawerOpen) {
                    AppDrawer(
                        vm = vm,
                        onDismiss = { vm.appDrawerOpen = false },
                        modifier = Modifier.zIndex(28f),
                    )
                }
                DragGhost(
                    vm = vm,
                    root = rootCoords,
                    modifier = Modifier.zIndex(90f),
                )
                EdgePanels(
                    vm = vm,
                    modifier = Modifier
                        .zIndex(if (vm.appHold != null || vm.expandedEdge != null) 40f else 8f)
                        .fillMaxSize(),
                )
                if (vm.useMadRecents && vm.recentsOpen) {
                    RecentsPanel(
                        apps = vm.recents.toList(),
                        onLaunch = { vm.launchApp(it) },
                        onClose = { vm.closeRecents() },
                        showUsageAccessHint = !vm.hasUsageAccess,
                        onUsageAccess = { vm.openUsageAccessSettings() },
                    )
                }
                if (vm.useMadShadeWindows && vm.notificationsOpen) {
                    NotificationsPanel(vm)
                }
                if (vm.useMadShadeWindows && vm.quickSettingsOpen) {
                    QuickSettingsPanel(vm)
                }
                if (vm.actionBoxOpen) {
                    val imeBottom = rememberImeBottomDp()
                    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val actionBottom = actionBoxLiftDp(imeBottom, navBottom, closedDock = 96.dp)
                    val screenH = LocalConfiguration.current.screenHeightDp.dp
                    val expandedMax = (screenH - actionBottom - statusBarHeight - 88.dp)
                        .coerceIn(120.dp, 520.dp)
                    ActionBox(
                        vm = vm,
                        expandedMaxHeight = expandedMax,
                        modifier = Modifier
                            .zIndex(30f)
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = actionBottom)
                            .fillMaxWidth(0.92f)
                            .widthIn(max = 360.dp),
                    )
                }
                if (vm.settingsOpen) {
                    SettingsOverlay(vm = vm, onPickFont = fontPicker, modifier = Modifier.align(Alignment.Center))
                }
                if (vm.openFolderId != null) {
                    OpenFolderOverlay(
                        vm = vm,
                        modifier = Modifier
                            .zIndex(36f)
                            .fillMaxSize(),
                    )
                }
                if (vm.setupOpen) {
                    SetupPanel(vm)
                }
                if (vm.widgetPickerOpen) {
                    HomeWidgetPicker(
                        vm = vm,
                        modifier = Modifier.zIndex(34f),
                    )
                }
                if (vm.newsPickerOpen) {
                    NewsSourcePicker(
                        vm = vm,
                        modifier = Modifier
                            .zIndex(34f)
                            .align(Alignment.BottomCenter)
                            .padding(start = 24.dp, end = 24.dp, bottom = 108.dp)
                            .fillMaxWidth()
                            .widthIn(max = 360.dp),
                    )
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
                                iconPicker()
                            },
                        )
                    }
                }
                if (vm.dockPickApp != null) {
                    Popup(
                        alignment = Alignment.Center,
                        onDismissRequest = { vm.dockPickApp = null },
                        properties = PopupProperties(
                            focusable = true,
                            dismissOnClickOutside = true,
                            dismissOnBackPress = true,
                        ),
                    ) {
                        val theme = LocalMadTheme.current
                        val app = vm.dockPickApp!!
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(theme.colors.card.copy(alpha = 0.97f))
                                .padding(12.dp)
                                .widthIn(max = 280.dp),
                        ) {
                            Text(stringResource(R.string.dock_add_to_which), color = theme.colors.fg, fontWeight = FontWeight.SemiBold)
                            vm.activeDockTargets().forEach { target ->
                                val label = when (target) {
                                    "left" -> stringResource(R.string.dock_left)
                                    "right" -> stringResource(R.string.dock_right)
                                    "top" -> stringResource(R.string.dock_top)
                                    else -> stringResource(R.string.dock_bottom)
                                }
                                Text(
                                    label,
                                    color = theme.colors.fg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.addAppToDock(app, target) }
                                        .padding(vertical = 12.dp),
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = vm.toast != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 88.dp),
                ) {
                    Text(
                        text = vm.toast.orEmpty(),
                        color = theme.colors.bg,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.colors.accent)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                }
            }

            Box(
                modifier = Modifier
                    .zIndex(4f)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(statusBarHeight.coerceAtLeast(24.dp))
                    .pointerInput(vm.displayAppearance.navShortcutMode, landscape, landscapeGestures, vm.useMadShadeWindows, vm.useNavShortcuts) {
                        if ((landscape && !landscapeGestures) || !vm.useNavShortcuts || NavShortcutBridge.useClassicNav) return@pointerInput
                        val onNav = vm.useMadShadeWindows && vm.displayAppearance.navShortcutSlots.any {
                            it.kind == NavShortcutKind.SYSTEM_APPS && it.placement.isNavBar
                        }
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (statusSwipeAcc > 70f && !onNav) {
                                    if (vm.useMadShadeWindows) vm.openSystemNotifShade()
                                    else vm.openSystemShade()
                                }
                                statusSwipeAcc = 0f
                            },
                            onDragCancel = { statusSwipeAcc = 0f },
                            onVerticalDrag = { change, dy ->
                                if (dy > 0f) {
                                    statusSwipeAcc += dy
                                    change.consume()
                                }
                            },
                        )
                    },
            )

        }
    }
}

@Composable
private fun WallpaperLayer(path: String?) {
    val file = path?.let { File(it) }
    if (file != null && file.exists()) {
        val bmp = remember(path) { BitmapFactory.decodeFile(path) }
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            return
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
}
