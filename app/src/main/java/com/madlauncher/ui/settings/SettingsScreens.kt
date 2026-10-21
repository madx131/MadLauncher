package com.madlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import com.madlauncher.data.IconStyleStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.madlauncher.BuildConfig
import com.madlauncher.data.MadApkShare
import androidx.compose.ui.res.stringResource
import com.madlauncher.data.DockAutoHide
import com.madlauncher.data.EdgeSide
import com.madlauncher.data.ExperimentalGrants
import com.madlauncher.data.LanguageCatalog
import com.madlauncher.data.LanguagePackEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.madlauncher.R
import kotlinx.coroutines.launch
import com.madlauncher.data.HomeLayout
import com.madlauncher.data.AppearanceSettings
import com.madlauncher.data.GestureActionId
import com.madlauncher.data.GestureId
import com.madlauncher.data.FolderViewId
import com.madlauncher.data.IconBadgeStyle
import com.madlauncher.data.IconShapeId
import com.madlauncher.data.MadShadeWindows
import com.madlauncher.data.NavShortcutKind
import com.madlauncher.data.NavShortcutMode
import com.madlauncher.data.NavShortcutPlacement
import com.madlauncher.data.NavShortcutPlacements
import com.madlauncher.data.NavShortcutSlot
import com.madlauncher.data.PageEffectId
import com.madlauncher.data.PaletteId
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.platform.FeatureStatus
import com.madlauncher.ui.AppIcon
import com.madlauncher.ui.BottomDock
import com.madlauncher.ui.edgeIconDp
import com.madlauncher.ui.maxDockIconDp
import com.madlauncher.ui.MadShapePreview
import com.madlauncher.ui.madString
import com.madlauncher.ui.theme.LocalMadTheme
import com.madlauncher.ui.theme.MadSurfaceKind
import com.madlauncher.ui.theme.MadTheme
import com.madlauncher.ui.theme.madSurface

@Composable
private fun commandEditTitle(id: CommandEditId?): String = when (id) {
    CommandEditId.HOW_TO -> stringResource(R.string.action_box_howto_title)
    CommandEditId.AT -> stringResource(R.string.action_box_kind_message)
    CommandEditId.SOCIAL -> stringResource(R.string.action_box_kind_social)
    CommandEditId.CALL -> stringResource(R.string.action_box_kind_call)
    CommandEditId.VIDEO -> stringResource(R.string.action_box_kind_video)
    CommandEditId.SEARCH -> stringResource(R.string.action_box_kind_search)
    CommandEditId.COMMON_APPS -> stringResource(R.string.action_box_kind_common_apps)
    CommandEditId.SEARCHABLE_APPS -> stringResource(R.string.action_box_kind_searchable_apps)
    CommandEditId.NOTES -> stringResource(R.string.action_box_kind_notes)
    CommandEditId.TRANSLATE -> stringResource(R.string.action_box_kind_translate)
    CommandEditId.NAV -> stringResource(R.string.action_box_kind_go)
    CommandEditId.STICKY -> stringResource(R.string.action_box_kind_sticky)
    CommandEditId.AI -> stringResource(R.string.action_box_kind_ai)
    CommandEditId.CONTACT -> stringResource(R.string.action_box_kind_contact)
    CommandEditId.ADD_CONTACT -> stringResource(R.string.action_box_kind_add_contact)
    CommandEditId.HELP -> stringResource(R.string.action_box_kind_help)
    CommandEditId.UPDATE_APPS -> stringResource(R.string.action_box_kind_update)
    CommandEditId.REMINDER -> stringResource(R.string.action_box_kind_reminder)
    CommandEditId.SYSTEM_SETTINGS -> stringResource(R.string.action_box_kind_system_settings)
    CommandEditId.LAUNCHER_SETTINGS -> stringResource(R.string.action_box_kind_launcher_settings)
    CommandEditId.ACTION_BOX_SETTINGS -> stringResource(R.string.action_box_kind_abs)
    CommandEditId.CUSTOM -> stringResource(R.string.action_box_your_commands)
    null -> stringResource(R.string.action_box_commands_title)
}

@Composable
fun SettingsOverlay(
    vm: LauncherViewModel,
    onPickFont: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val route = vm.settingsRoute ?: return
    val theme = LocalMadTheme.current

    BackHandler { vm.settingsBack() }

    val title = when (route) {
        SettingsRoute.HUB -> stringResource(R.string.settings_route_hub)
        SettingsRoute.THEMES -> stringResource(R.string.settings_route_themes)
        SettingsRoute.ICONS -> stringResource(R.string.settings_route_icons)
        SettingsRoute.GESTURES -> stringResource(R.string.settings_route_gestures)
        SettingsRoute.FONTS -> stringResource(R.string.settings_route_fonts)
        SettingsRoute.NAV_SHORTCUTS -> stringResource(R.string.settings_route_nav_shortcuts)
        SettingsRoute.PERMISSIONS -> stringResource(R.string.settings_route_permissions)
        SettingsRoute.EDIT_DOCKS -> stringResource(R.string.settings_route_edit_docks)
        SettingsRoute.ACTION_BOX_COMMANDS -> stringResource(R.string.action_box_commands_title)
        SettingsRoute.ACTION_BOX_COMMAND_EDIT -> commandEditTitle(vm.commandEditId)
        SettingsRoute.EXTRAS -> stringResource(R.string.settings_route_extras)
        SettingsRoute.EXPERIMENTAL -> stringResource(R.string.settings_route_experimental)
        SettingsRoute.SOCIAL_BROWSER -> stringResource(R.string.settings_route_social_browser)
        SettingsRoute.SOCIAL_BROWSER_ABSENT -> stringResource(R.string.settings_route_social_browser_absent)
        SettingsRoute.NOT_WORKING -> stringResource(R.string.settings_route_not_working)
        SettingsRoute.SHARE_APK -> stringResource(R.string.settings_route_share_apk)
        SettingsRoute.ABOUT -> stringResource(R.string.settings_route_about)
        SettingsRoute.GLANCE -> stringResource(R.string.settings_route_glance)
        SettingsRoute.MULTI -> stringResource(R.string.settings_route_multi)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(50f)
            .background(theme.colors.bg.copy(alpha = 0.35f))
            .clickable(enabled = false) {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { vm.settingsBack() }
                        .padding(8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back), tint = theme.colors.fg)
                }
                Text(title, color = theme.colors.fg, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, modifier = Modifier.padding(start = 4.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .madSurface(),
            ) {
                when (route) {
                    SettingsRoute.ACTION_BOX_COMMANDS -> ActionBoxCommandsPage(vm)
                    SettingsRoute.ACTION_BOX_COMMAND_EDIT -> ActionBoxCommandEditPage(vm)
                    SettingsRoute.GLANCE -> GlanceSettingsPage(vm)
                    SettingsRoute.ICONS -> IconsPage(vm)
                    SettingsRoute.EDIT_DOCKS -> EditDocksPage(vm)
                    else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (route) {
                            SettingsRoute.HUB -> SettingsHub(vm)
                            SettingsRoute.THEMES -> ThemesPage(vm)
                            SettingsRoute.ICONS -> Unit
                            SettingsRoute.GESTURES -> GesturesPage(vm)
                            SettingsRoute.FONTS -> FontsPage(vm, onPickFont)
                            SettingsRoute.NAV_SHORTCUTS -> NavShortcutsPage(vm)
                            SettingsRoute.PERMISSIONS -> PermissionsPage(vm)
                            SettingsRoute.EDIT_DOCKS -> Unit
                            SettingsRoute.EXTRAS -> ExtrasPage(vm)
                            SettingsRoute.EXPERIMENTAL -> ExperimentalPage(vm)
                            SettingsRoute.SOCIAL_BROWSER -> SocialBrowserPage(vm)
                            SettingsRoute.SOCIAL_BROWSER_ABSENT -> SocialBrowserAbsentPage(vm)
                            SettingsRoute.NOT_WORKING -> NotWorkingPage(vm)
                            SettingsRoute.SHARE_APK -> ShareApkPage(vm)
                            SettingsRoute.ABOUT -> AboutPage()
                            SettingsRoute.MULTI -> MultiWidgetSettingsPage(vm)
                            SettingsRoute.ACTION_BOX_COMMANDS,
                            SettingsRoute.ACTION_BOX_COMMAND_EDIT,
                            SettingsRoute.GLANCE -> Unit
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    theme: com.madlauncher.ui.theme.MadThemeSpec,
) {
    Text(
        label,
        color = if (primary) theme.colors.bg else theme.colors.fg,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) theme.colors.primary else theme.colors.secondary)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun SettingsHub(vm: LauncherViewModel) {
    if (vm.independentActionBox) {
        HubRow(
            stringResource(R.string.action_box_commands_title),
            stringResource(R.string.action_box_commands_hub_sub),
        ) { vm.openSettingsRoute(SettingsRoute.ACTION_BOX_COMMANDS) }
        HubRow(stringResource(R.string.settings_route_permissions), stringResource(R.string.settings_hub_permissions_subtitle)) {
            vm.openSettingsRoute(SettingsRoute.PERMISSIONS)
        }
        HubRow(stringResource(R.string.settings_route_extras), stringResource(R.string.extras_hub_subtitle)) {
            vm.openSettingsRoute(SettingsRoute.EXTRAS)
        }
        HubRow(stringResource(R.string.settings_route_about), stringResource(R.string.about_actionbox_sub)) {
            vm.openAbout(SettingsRoute.HUB)
        }
        return
    }
    HubRow(stringResource(R.string.settings_route_themes), stringResource(R.string.settings_hub_themes_subtitle)) { vm.openSettingsRoute(SettingsRoute.THEMES) }
    HubRow(stringResource(R.string.settings_route_icons), stringResource(R.string.settings_hub_icons_subtitle)) { vm.openSettingsRoute(SettingsRoute.ICONS) }
    HubRow(stringResource(R.string.settings_route_gestures), stringResource(R.string.settings_hub_gestures_subtitle)) { vm.openSettingsRoute(SettingsRoute.GESTURES) }
    HubRow(stringResource(R.string.settings_route_glance), stringResource(R.string.settings_hub_glance_subtitle)) { vm.openSettingsRoute(SettingsRoute.GLANCE) }
    if (vm.multiOnHome()) {
        HubRow(stringResource(R.string.settings_route_multi), stringResource(R.string.settings_hub_multi_subtitle)) {
            vm.openSettingsRoute(SettingsRoute.MULTI)
        }
    }
    HubRow(stringResource(R.string.settings_route_edit_docks), stringResource(R.string.settings_hub_docks_subtitle)) { vm.openSettingsRoute(SettingsRoute.EDIT_DOCKS) }
    HubRow(
        stringResource(R.string.action_box_commands_title),
        stringResource(R.string.action_box_commands_hub_sub),
    ) { vm.openSettingsRoute(SettingsRoute.ACTION_BOX_COMMANDS) }
    HubRow(stringResource(R.string.settings_route_permissions), stringResource(R.string.settings_hub_permissions_subtitle)) { vm.openSettingsRoute(SettingsRoute.PERMISSIONS) }
    HubRow(stringResource(R.string.settings_route_extras), stringResource(R.string.extras_hub_subtitle)) { vm.openSettingsRoute(SettingsRoute.EXTRAS) }
    HubRow(stringResource(R.string.settings_route_about), stringResource(R.string.about_hub_sub)) {
        vm.openAbout(SettingsRoute.HUB)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemesPage(vm: LauncherViewModel) {
    val s = vm.appearanceDraft
    val theme = LocalMadTheme.current
    SectionLabel(stringResource(R.string.settings_section_palette))
    val palettes = listOf(
        PaletteId.MATERIAL to stringResource(R.string.settings_palette_system),
        PaletteId.NORD to stringResource(R.string.settings_palette_nord),
        PaletteId.WALLPAPER to stringResource(R.string.settings_palette_wallpaper),
        PaletteId.STEAMPUNK to stringResource(R.string.settings_palette_steampunk),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        palettes.forEach { (id, label) ->
            val selected = s.palette == id
            Text(
                label,
                color = if (selected) theme.colors.fg else theme.colors.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) theme.colors.primary.copy(alpha = 0.35f) else theme.colors.secondary)
                    .clickable {
                        vm.patchAppearanceDraft {
                            it.copy(
                                palette = id,
                                accentArgb = when (id) {
                                    PaletteId.STEAMPUNK -> 0xFFC4A35A
                                    PaletteId.NORD -> 0xFF81A1C1
                                    else -> it.accentArgb
                                },
                            )
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
    Text(
        stringResource(R.string.settings_palette_hint),
        color = LocalMadTheme.current.colors.muted,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
    if (s.palette == PaletteId.WALLPAPER && vm.wallpaperPalette == null) {
        Text(stringResource(R.string.settings_no_wallpaper_colors), color = LocalMadTheme.current.colors.muted, fontSize = 11.sp)
    }
    ToggleRow(
        stringResource(R.string.settings_landscape_rotate),
        s.landscapeRotateContents,
    ) {
        vm.patchAppearanceDraft { it.copy(landscapeRotateContents = !it.landscapeRotateContents) }
    }
    Text(
        stringResource(R.string.settings_landscape_rotate_hint),
        color = LocalMadTheme.current.colors.muted,
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    SectionLabel(stringResource(R.string.settings_section_transparency))
    AlphaSlider(stringResource(R.string.settings_alpha_windows), s.windowAlpha) { v -> vm.patchAppearanceDraft { it.copy(windowAlpha = v) } }
    AlphaSlider(stringResource(R.string.settings_alpha_app_drawer), s.drawerAlpha) { v -> vm.patchAppearanceDraft { it.copy(drawerAlpha = v) } }
    AlphaSlider(stringResource(R.string.settings_alpha_dock), s.dockAlpha) { v -> vm.patchAppearanceDraft { it.copy(dockAlpha = v) } }
    AlphaSlider(stringResource(R.string.settings_alpha_widgets), s.widgetAlpha) { v -> vm.patchAppearanceDraft { it.copy(widgetAlpha = v) } }
    AlphaSlider(stringResource(R.string.settings_alpha_news), s.newsAlpha) { v -> vm.patchAppearanceDraft { it.copy(newsAlpha = v) } }
    SectionLabel(stringResource(R.string.settings_section_page_switching))
    val effects = PageEffectId.entries
    SegmentedRow(effects.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, effects.indexOf(s.pageEffect).coerceAtLeast(0)) { idx ->
        vm.setPageEffect(effects[idx])
    }
    Text(
        "Swipe home pages to preview. Slide = stock swipe; Fade = crossfade; Stack = card deck; Cube = 3D rotate; Depth = zoom back; Book = page flip.",
        color = LocalMadTheme.current.colors.muted,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
    ToggleRow(
        stringResource(R.string.settings_infinite_home_pages),
        s.infiniteHomePages,
    ) { vm.setInfiniteHomePages(!s.infiniteHomePages) }
    Text(
        stringResource(R.string.settings_infinite_home_pages_hint),
        color = LocalMadTheme.current.colors.muted,
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    SectionLabel(stringResource(R.string.settings_section_page_grid))
    val gridSizes = HomeLayout.PAGE_GRID_SIZES
    SegmentedRow(gridSizes.map { "${it}×$it" }, gridSizes.indexOf(s.homeGridSize.let { HomeLayout.normalizeGridSize(it) })) { idx ->
        vm.setDefaultHomeGridSize(gridSizes[idx])
    }
    Text(
        stringResource(R.string.settings_page_grid_hint),
        color = LocalMadTheme.current.colors.muted,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(theme.colors.secondary.copy(alpha = s.widgetAlpha))
            .padding(12.dp),
    ) {
        Text(vm.clock.ifBlank { "12:00" }, color = theme.colors.fg, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(vm.date, color = theme.colors.primary, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomStart))
    }
}

private fun IconShapeId.pickerLabel(): String = name.lowercase().replace('_', ' ')

@Composable
private fun IconsPage(vm: LauncherViewModel) {
    val s = vm.appearanceDraft
    val c = LocalMadTheme.current.colors
    LaunchedEffect(Unit) {
        vm.refreshIconPacks()
        vm.refreshIconStyles()
    }
    val previewDp = s.homeIconSizeDp.coerceIn(40f, 72f)
    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.settings_icons_preview), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        IconsLivePreview(vm, previewDp.dp)
        Text(stringResource(R.string.settings_icons_preview_hint), color = c.muted, fontSize = 12.sp)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.settings_section_shape), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            ShapeOptionGrid(
                shapes = listOf(
                    IconShapeId.ADAPTIVE,
                    IconShapeId.ROUNDED_SQUARE,
                    IconShapeId.SQUARE,
                    IconShapeId.SQUIRCLE,
                    IconShapeId.CIRCLE,
                    IconShapeId.TEARDROP,
                    IconShapeId.HEXAGON,
                    IconShapeId.DIAMOND,
                ),
                selected = s.iconShape,
                onPick = vm::setIconShape,
            )
            Text(stringResource(R.string.settings_section_icon_style), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(stringResource(R.string.settings_icon_style_hint), color = c.muted, fontSize = 12.sp)
            IconStyleSection(vm)
            Text(stringResource(R.string.settings_icon_size), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Slider(
                value = s.homeIconSizeDp,
                onValueChange = { v -> vm.applyHomeIconSize(v) },
                valueRange = 40f..88f,
            )
            Text(stringResource(R.string.settings_icon_size_hint), color = c.muted, fontSize = 11.sp)
            ToggleRow(stringResource(R.string.settings_sync_dock_icons), s.syncDockIcons) {
                vm.setSyncDockIcons(!s.syncDockIcons)
            }
            Text(stringResource(R.string.settings_sync_dock_icons_hint), color = c.muted, fontSize = 11.sp)
            Text(stringResource(R.string.settings_icon_label_size), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Slider(
                value = s.iconLabelSizeSp.coerceIn(9f, 16f),
                onValueChange = { v -> vm.applyIconLabelSize(v) },
                valueRange = 9f..16f,
            )
            Text(stringResource(R.string.settings_icon_label_hint), color = c.muted, fontSize = 11.sp)
            Text(stringResource(R.string.settings_section_icon_bg), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            AlphaSlider(stringResource(R.string.settings_alpha_icon_bg), s.iconBgAlpha) { v ->
                vm.patchAppearanceDraft { it.copy(iconBgAlpha = v) }
            }
            Text(stringResource(R.string.settings_icon_bg_hint), color = c.muted, fontSize = 11.sp)
            Text(stringResource(R.string.settings_folders), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            LabeledOptionGrid(
                options = listOf(
                    FolderViewId.GRID to stringResource(R.string.settings_folder_grid),
                    FolderViewId.STACK to stringResource(R.string.settings_folder_stack),
                    FolderViewId.BADGE to stringResource(R.string.settings_folder_badge),
                ),
                selected = s.folderView,
                onPick = { view -> vm.patchAppearanceDraft { it.copy(folderView = view) } },
            )
            Text(stringResource(R.string.settings_section_badges), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            LabeledOptionGrid(
                options = listOf(
                    IconBadgeStyle.DOT to stringResource(R.string.settings_badge_dot),
                    IconBadgeStyle.NUMBER to stringResource(R.string.settings_badge_number),
                ),
                selected = s.iconBadgeStyle,
                onPick = { style -> vm.patchAppearanceDraft { it.copy(iconBadgeStyle = style) } },
            )
            Text(stringResource(R.string.settings_section_icon_pack), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            HubRow(
                stringResource(R.string.settings_icon_default),
                if (s.iconPackPackage.isBlank()) {
                    stringResource(R.string.settings_icon_default_on)
                } else {
                    stringResource(R.string.settings_icon_default_reset)
                },
            ) { vm.setIconPack("") }
            vm.iconPacks.forEach { pack ->
                HubRow(pack.label, pack.packageName) { vm.setIconPack(pack.packageName) }
            }
        }
    }
}

@Composable
private fun IconStyleSection(vm: LauncherViewModel) {
    val revision = IconStyleStore.revision.intValue
    val selected = vm.appearanceDraft.iconStyleId
    val styles = remember(revision, vm.iconStyles) { vm.iconStyles }
    val designerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.importIconStyle(result.data)
    }
    HubRow(
        stringResource(R.string.settings_icon_style_none),
        if (selected.isBlank()) {
            stringResource(R.string.settings_icon_style_on)
        } else {
            stringResource(R.string.settings_icon_style_none_on)
        },
    ) { vm.setIconStyle("") }
    styles.forEach { style ->
        val on = style.id == selected
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { vm.setIconStyle(style.id) }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                Text(style.name, color = LocalMadTheme.current.colors.fg, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(
                    if (on) stringResource(R.string.settings_icon_style_on) else style.id,
                    color = LocalMadTheme.current.colors.muted,
                    fontSize = 12.sp,
                )
            }
            Text(
                stringResource(R.string.settings_icon_style_remove),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { vm.removeIconStyle(style.id) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
    vm.iconStyleDesigners.forEach { designer ->
        HubRow(stringResource(R.string.settings_icon_style_design), designer.label) {
            val intent = Intent(IconStyleStore.ACTION_INSTALL).apply {
                setClassName(designer.packageName, designer.className)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { designerLauncher.launch(intent) }
        }
    }
}

@Composable
private fun IconsLivePreview(vm: LauncherViewModel, size: androidx.compose.ui.unit.Dp) {
    val c = LocalMadTheme.current.colors
    val samples = remember(vm.apps) { vm.apps.filter { it.icon != null }.take(4) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.secondary.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp),
    ) {
        if (samples.isEmpty()) {
            listOf(IconShapeId.ADAPTIVE, IconShapeId.ROUNDED_SQUARE, IconShapeId.CIRCLE, IconShapeId.HEXAGON).forEach { shape ->
                MadShapePreview(shape, size, vm.appearanceDraft.iconBgAlpha)
            }
        } else {
            samples.forEach { app ->
                AppIcon(app.icon, app.label, size = size, packageName = app.packageName, activityName = app.activityName)
            }
        }
    }
}

@Composable
private fun ShapeOptionGrid(
    shapes: List<IconShapeId>,
    selected: IconShapeId,
    onPick: (IconShapeId) -> Unit,
) {
    LabeledOptionGrid(
        options = shapes.map { it to it.pickerLabel() },
        selected = selected,
        onPick = onPick,
    )
}

@Composable
private fun <T> LabeledOptionGrid(
    options: List<Pair<T, String>>,
    selected: T,
    onPick: (T) -> Unit,
) {
    val c = LocalMadTheme.current.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (value, label) ->
                    val on = value == selected
                    Text(
                        label,
                        color = if (on) c.fg else c.muted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) c.primary.copy(alpha = 0.35f) else c.secondary)
                            .clickable { onPick(value) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GesturesPage(vm: LauncherViewModel) {
    val s = vm.appearanceDraft
    ToggleRow(
        stringResource(R.string.gesture_landscape_enabled),
        s.landscapeGesturesEnabled,
    ) {
        vm.patchAppearanceDraft { it.copy(landscapeGesturesEnabled = !it.landscapeGesturesEnabled) }
    }
    Text(
        stringResource(R.string.gesture_landscape_enabled_hint),
        color = LocalMadTheme.current.colors.muted,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    val experimental = vm.useMadShadeWindows
    GestureId.entries.forEach { gesture ->
        val current = s.gestures[gesture] ?: GestureActionId.NONE
        val shown = if (!experimental && current == GestureActionId.SYSTEM_APPS) {
            GestureActionId.NOTIFICATIONS
        } else {
            current
        }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(gestureLabel(gesture), color = LocalMadTheme.current.colors.fg, fontWeight = FontWeight.Medium)
            Text(gestureActionLabel(shown, experimental), color = LocalMadTheme.current.colors.muted, fontSize = 12.sp)
            FlowRowPicker(MadShadeWindows.gestureActions(experimental), shown, experimental) { picked ->
                vm.patchAppearanceDraft { it.copy(gestures = it.gestures + (gesture to picked)) }
            }
        }
    }
}

@Composable
private fun FontsPage(vm: LauncherViewModel, onPickFont: () -> Unit) {
    val path = vm.appearanceDraft.fontPath
    HubRow(stringResource(R.string.settings_font_system), if (path.isBlank()) stringResource(R.string.settings_font_system_on) else stringResource(R.string.settings_font_system_tap)) { vm.setCustomFont("") }
    HubRow(stringResource(R.string.settings_font_choose), if (path.isBlank()) stringResource(R.string.settings_font_none) else path.substringAfterLast('/')) { onPickFont() }
}

@Composable
private fun NavShortcutsPage(vm: LauncherViewModel) {
    val s = vm.appearanceDraft
    val c = LocalMadTheme.current.colors
    SectionLabel(stringResource(R.string.settings_section_mode))
    SegmentedRow(listOf("Classic", "Nav bar"), if (s.navShortcutMode == NavShortcutMode.CLASSIC) 0 else 1) { idx ->
        vm.patchAppearanceDraft {
            it.copy(navShortcutMode = if (idx == 0) NavShortcutMode.CLASSIC else NavShortcutMode.NAV_BAR)
        }
    }
    Text(
        if (s.navShortcutMode == NavShortcutMode.CLASSIC)
            stringResource(R.string.settings_nav_classic_hint)
        else
            stringResource(R.string.settings_nav_bar_hint),
        color = c.muted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
    )
    if (s.navShortcutMode == NavShortcutMode.NAV_BAR) {
        MadShadeWindows.navKinds(vm.useMadShadeWindows).forEach { kind ->
            val placement = NavShortcutPlacements.placementOf(s.navShortcutSlots, kind)
            val enabled = placement.isActive
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    navKindLabel(kind, vm.useMadShadeWindows),
                    color = c.fg,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                if (enabled) {
                    Text(
                        navPlacementLabel(placement),
                        color = c.muted,
                        fontSize = 12.sp,
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c.secondary)
                            .clickable {
                                vm.patchAppearanceDraft { settings ->
                                    settings.copy(
                                        navShortcutSlots = NavShortcutPlacements.cycleChair(
                                            settings.navShortcutSlots,
                                            kind,
                                        ),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.CompareArrows,
                            contentDescription = stringResource(R.string.settings_nav_swap_side),
                            tint = c.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { on ->
                        vm.patchAppearanceDraft { settings ->
                            settings.copy(
                                navShortcutSlots = NavShortcutPlacements.setEnabled(
                                    settings.navShortcutSlots,
                                    kind,
                                    on,
                                ),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ExperimentalPage(vm: LauncherViewModel) {
    val context = LocalContext.current
    val theme = LocalMadTheme.current
    var pending by remember { mutableStateOf<ExperimentalGrants.Feature?>(null) }
    val missing = pending?.let { ExperimentalGrants.missing(context, vm.grants, it) }.orEmpty()

    LaunchedEffect(pending, vm.grants.notificationListener, vm.grants.overlay, vm.grants.accessibility, vm.grants.usageStats) {
        val feature = pending ?: return@LaunchedEffect
        if (ExperimentalGrants.missing(context, vm.grants, feature).isEmpty()) {
            when (feature) {
                ExperimentalGrants.Feature.MAD_WINDOWS -> vm.setExperimentalMadWindows(true)
                ExperimentalGrants.Feature.NAV_SHORTCUTS -> vm.setExperimentalNavShortcuts(true)
                ExperimentalGrants.Feature.MAD_RECENTS -> vm.setExperimentalMadRecents(true)
            }
            pending = null
        }
    }

    Text(
        stringResource(R.string.settings_experimental_intro),
        color = theme.colors.muted,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    ToggleRow(
        stringResource(R.string.settings_experimental_mad_windows),
        vm.useMadShadeWindows,
    ) {
        if (vm.useMadShadeWindows) {
            vm.setExperimentalMadWindows(false)
        } else {
            val need = ExperimentalGrants.missing(context, vm.grants, ExperimentalGrants.Feature.MAD_WINDOWS)
            if (need.isEmpty()) vm.setExperimentalMadWindows(true) else pending = ExperimentalGrants.Feature.MAD_WINDOWS
        }
    }
    Text(
        if (vm.useMadShadeWindows) {
            stringResource(R.string.settings_experimental_mad_windows_on)
        } else {
            stringResource(R.string.settings_experimental_mad_windows_off)
        },
        color = theme.colors.muted,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
    ToggleRow(
        stringResource(R.string.settings_experimental_widget_badges),
        vm.useWidgetBadges,
    ) {
        vm.setExperimentalWidgetBadges(!vm.useWidgetBadges)
    }
    Text(
        if (vm.useWidgetBadges) {
            stringResource(R.string.settings_experimental_widget_badges_on)
        } else {
            stringResource(R.string.settings_experimental_widget_badges_off)
        },
        color = theme.colors.muted,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
    ToggleRow(
        stringResource(R.string.settings_experimental_mad_recents),
        vm.useMadRecents,
    ) {
        if (vm.useMadRecents) {
            vm.setExperimentalMadRecents(false)
        } else {
            val need = ExperimentalGrants.missing(context, vm.grants, ExperimentalGrants.Feature.MAD_RECENTS)
            if (need.isEmpty()) vm.setExperimentalMadRecents(true) else pending = ExperimentalGrants.Feature.MAD_RECENTS
        }
    }
    Text(
        if (vm.useMadRecents) {
            stringResource(R.string.settings_experimental_mad_recents_on)
        } else {
            stringResource(R.string.settings_experimental_mad_recents_off)
        },
        color = theme.colors.muted,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
    ToggleRow(
        stringResource(R.string.settings_experimental_nav_shortcuts),
        vm.useNavShortcuts,
    ) {
        if (vm.useNavShortcuts) {
            vm.setExperimentalNavShortcuts(false)
        } else {
            val need = ExperimentalGrants.missing(context, vm.grants, ExperimentalGrants.Feature.NAV_SHORTCUTS)
            if (need.isEmpty()) vm.setExperimentalNavShortcuts(true) else pending = ExperimentalGrants.Feature.NAV_SHORTCUTS
        }
    }
    Text(
        if (vm.useNavShortcuts) {
            stringResource(R.string.settings_experimental_nav_shortcuts_on)
        } else {
            stringResource(R.string.settings_experimental_nav_shortcuts_off)
        },
        color = theme.colors.muted,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
    if (vm.useNavShortcuts) {
        HubRow(
            stringResource(R.string.settings_route_nav_shortcuts),
            stringResource(R.string.settings_hub_nav_subtitle),
        ) {
            vm.openSettingsRoute(SettingsRoute.NAV_SHORTCUTS)
        }
    }
    HubRow(
        stringResource(R.string.settings_experimental_social_browser),
        stringResource(
            R.string.settings_experimental_social_browser_sub,
            vm.socialBrowserEnabledCount(),
            vm.socialBrowserTotalCount(),
        ),
    ) {
        vm.openSettingsRoute(SettingsRoute.SOCIAL_BROWSER)
    }
    if (pending != null) {
        ExperimentalGrantDialog(
            missing = missing,
            onOpenGrant = { id ->
                runCatching {
                    context.startActivity(
                        ExperimentalGrants.settingsIntent(context, id)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
            onTurnOn = {
                val feature = pending ?: return@ExperimentalGrantDialog
                if (ExperimentalGrants.missing(context, vm.grants, feature).isNotEmpty()) return@ExperimentalGrantDialog
                when (feature) {
                    ExperimentalGrants.Feature.MAD_WINDOWS -> vm.setExperimentalMadWindows(true)
                    ExperimentalGrants.Feature.NAV_SHORTCUTS -> vm.setExperimentalNavShortcuts(true)
                    ExperimentalGrants.Feature.MAD_RECENTS -> vm.setExperimentalMadRecents(true)
                }
                pending = null
            },
            onCancel = { pending = null },
        )
    }
}

@Composable
private fun SocialBrowserPage(vm: LauncherViewModel) {
    val theme = LocalMadTheme.current
    var adding by remember { mutableStateOf(false) }
    var absentWindow by remember { mutableStateOf(false) }
    var urlFor by remember { mutableStateOf<com.madlauncher.data.LaunchableApp?>(null) }
    Text(
        stringResource(R.string.settings_social_browser_intro),
        color = theme.colors.muted,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val installedSocial = com.madlauncher.action.SocialBrowserApps.socialApps.filter { vm.browserAppInstalled(it.id) }
    val installedMessaging = com.madlauncher.action.SocialBrowserApps.messagingApps.filter { vm.browserAppInstalled(it.id) }
    if (installedSocial.isNotEmpty()) {
        BrowserSection(stringResource(R.string.settings_social_browser_social))
        installedSocial.forEach { app ->
            BrowserModeRow(app.label, com.madlauncher.action.SocialBrowserApps.options(app), vm.socialBrowserMode(app.id)) {
                vm.setSocialBrowserMode(app.id, it)
            }
        }
    }
    val installedSupported = com.madlauncher.action.SocialBrowserApps.supportedApps.filter { vm.browserAppInstalled(it.id) }
    if (installedSupported.isNotEmpty()) {
        BrowserSection(stringResource(R.string.settings_social_browser_supported))
        installedSupported.forEach { app ->
            BrowserModeRow(app.label, com.madlauncher.action.SocialBrowserApps.options(app), vm.socialBrowserMode(app.id)) {
                vm.setSocialBrowserMode(app.id, it)
            }
        }
    }
    if (installedMessaging.isNotEmpty()) {
        BrowserSection(stringResource(R.string.settings_social_browser_messaging))
        installedMessaging.forEach { app ->
            BrowserModeRow(app.label, com.madlauncher.action.SocialBrowserApps.options(app), vm.socialBrowserMode(app.id)) {
                vm.setSocialBrowserMode(app.id, it)
            }
        }
    }
    val added = vm.customBrowserApps().filter { vm.customBrowserInstalled(it.packageName) }
    if (added.isNotEmpty()) {
        BrowserSection(stringResource(R.string.settings_social_browser_added))
        added.forEach { app ->
            BrowserModeRow(app.label, com.madlauncher.action.SocialBrowserApps.customOptions(), app.mode) {
                vm.setCustomBrowserMode(app.packageName, it)
            }
            Text(
                stringResource(R.string.settings_social_browser_remove),
                color = theme.colors.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { vm.removeCustomBrowserApp(app.packageName) }
                    .padding(bottom = 8.dp),
            )
        }
    }
    if (vm.absentBrowserCount() > 0) {
        HubRow(
            stringResource(R.string.settings_social_browser_absent),
            stringResource(R.string.settings_social_browser_absent_sub, vm.absentBrowserCount()),
        ) {
            absentWindow = true
        }
    }
    if (absentWindow) {
        BrowserAbsentWindow(onClose = { absentWindow = false }) {
            SocialBrowserAbsentPage(vm)
        }
    }
    HubRow(
        stringResource(R.string.settings_social_browser_add),
        stringResource(R.string.settings_social_browser_add_sub),
    ) {
        adding = true
    }
    if (adding) {
        AddBrowserAppDialog(
            apps = vm.apps,
            taken = vm.customBrowserApps().map { it.packageName }.toSet(),
            onPick = { app ->
                adding = false
                urlFor = app
            },
            onCancel = { adding = false },
        )
    }
    val pendingUrl = urlFor
    if (pendingUrl != null) {
        BrowserUrlDialog(
            appName = pendingUrl.label,
            onSave = { url ->
                vm.addCustomBrowserApp(pendingUrl.packageName, pendingUrl.label, url)
                urlFor = null
            },
            onCancel = { urlFor = null },
        )
    }
}

@Composable
private fun BrowserAbsentWindow(onClose: () -> Unit, content: @Composable () -> Unit) {
    val theme = LocalMadTheme.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.colors.card),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                        .padding(8.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        tint = theme.colors.fg,
                    )
                }
                Text(
                    stringResource(R.string.settings_route_social_browser_absent),
                    color = theme.colors.fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SocialBrowserAbsentPage(vm: LauncherViewModel) {
    val theme = LocalMadTheme.current
    Text(
        stringResource(R.string.settings_social_browser_absent_intro),
        color = theme.colors.muted,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val social = com.madlauncher.action.SocialBrowserApps.socialApps.filter { !vm.browserAppInstalled(it.id) }
    val messaging = com.madlauncher.action.SocialBrowserApps.messagingApps.filter { !vm.browserAppInstalled(it.id) }
    if (social.isNotEmpty()) {
        BrowserSection(stringResource(R.string.settings_social_browser_social))
        social.forEach { app ->
            BrowserModeRow(app.label, com.madlauncher.action.SocialBrowserApps.options(app), vm.absentBrowserMode(app.id)) {
                vm.setSocialBrowserMode(app.id, it)
            }
        }
    }
    val supported = com.madlauncher.action.SocialBrowserApps.supportedApps.filter { !vm.browserAppInstalled(it.id) }
    if (supported.isNotEmpty()) {
        BrowserSection(stringResource(R.string.settings_social_browser_supported))
        supported.forEach { app ->
            BrowserModeRow(app.label, com.madlauncher.action.SocialBrowserApps.options(app), vm.absentBrowserMode(app.id)) {
                vm.setSocialBrowserMode(app.id, it)
            }
        }
    }
    if (messaging.isNotEmpty()) {
        BrowserSection(stringResource(R.string.settings_social_browser_messaging))
        messaging.forEach { app ->
            BrowserModeRow(app.label, com.madlauncher.action.SocialBrowserApps.options(app), vm.absentBrowserMode(app.id)) {
                vm.setSocialBrowserMode(app.id, it)
            }
        }
    }
    val added = vm.customBrowserApps().filter { !vm.customBrowserInstalled(it.packageName) }
    if (added.isNotEmpty()) {
        BrowserSection(stringResource(R.string.settings_social_browser_added))
        added.forEach { app ->
            BrowserModeRow(app.label, com.madlauncher.action.SocialBrowserApps.customOptions(), app.mode) {
                vm.setCustomBrowserMode(app.packageName, it)
            }
            Text(
                stringResource(R.string.settings_social_browser_remove),
                color = theme.colors.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { vm.removeCustomBrowserApp(app.packageName) }
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowserModeRow(
    label: String,
    modes: List<com.madlauncher.action.SocialBrowserApps.BrowserMode>,
    selected: com.madlauncher.action.SocialBrowserApps.BrowserMode,
    onPick: (com.madlauncher.action.SocialBrowserApps.BrowserMode) -> Unit,
) {
    val theme = LocalMadTheme.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = theme.colors.fg, fontSize = 15.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            modes.forEach { mode ->
                val on = mode == selected
                Text(
                    browserModeLabel(mode),
                    color = if (on) theme.colors.bg else theme.colors.fg,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) theme.colors.primary else theme.colors.bg)
                        .clickable { onPick(mode) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun browserModeLabel(mode: com.madlauncher.action.SocialBrowserApps.BrowserMode): String =
    when (mode) {
        com.madlauncher.action.SocialBrowserApps.BrowserMode.OFF ->
            stringResource(R.string.settings_social_browser_mode_off)
        com.madlauncher.action.SocialBrowserApps.BrowserMode.ON ->
            stringResource(R.string.settings_social_browser_mode_on)
        com.madlauncher.action.SocialBrowserApps.BrowserMode.POST ->
            stringResource(R.string.settings_social_browser_mode_post)
        com.madlauncher.action.SocialBrowserApps.BrowserMode.MSG ->
            stringResource(R.string.settings_social_browser_mode_msg)
    }

@Composable
private fun BrowserSection(title: String) {
    Text(
        title,
        color = LocalMadTheme.current.colors.fg,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun AddBrowserAppDialog(
    apps: List<com.madlauncher.data.LaunchableApp>,
    taken: Set<String>,
    onPick: (com.madlauncher.data.LaunchableApp) -> Unit,
    onCancel: () -> Unit,
) {
    val theme = LocalMadTheme.current
    var query by remember { mutableStateOf("") }
    val choices = apps
        .filter { app ->
            com.madlauncher.action.SocialBrowserApps.idFor(app.packageName) == null &&
                taken.none { it.equals(app.packageName, ignoreCase = true) }
        }
        .distinctBy { it.packageName }
        .filter { app ->
            query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
        }
        .take(40)
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.colors.card)
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.settings_social_browser_add),
                color = theme.colors.fg,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = theme.colors.fg, fontSize = 14.sp),
                cursorBrush = SolidColor(theme.colors.fg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.colors.bg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.settings_social_browser_search),
                            color = theme.colors.muted,
                            fontSize = 14.sp,
                        )
                    }
                    inner()
                },
            )
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                choices.forEach { app ->
                    Text(
                        app.label,
                        color = theme.colors.fg,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(app) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserUrlDialog(
    appName: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val theme = LocalMadTheme.current
    var url by remember { mutableStateOf("https://") }
    val ok = url.startsWith("https://") || url.startsWith("http://")
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.colors.card)
                .padding(16.dp),
        ) {
            Text(
                appName,
                color = theme.colors.fg,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            Text(
                stringResource(R.string.settings_social_browser_add_url),
                color = theme.colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            )
            BasicTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = theme.colors.fg, fontSize = 14.sp),
                cursorBrush = SolidColor(theme.colors.fg),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.colors.bg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    stringResource(android.R.string.cancel),
                    color = theme.colors.muted,
                    modifier = Modifier
                        .clickable(onClick = onCancel)
                        .padding(12.dp),
                )
                Text(
                    stringResource(R.string.settings_social_browser_add_save),
                    color = if (ok && url.length > 8) theme.colors.fg else theme.colors.muted,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable(enabled = ok && url.length > 8) { onSave(url.trim()) }
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ExperimentalGrantDialog(
    missing: List<String>,
    onOpenGrant: (String) -> Unit,
    onTurnOn: () -> Unit,
    onCancel: () -> Unit,
) {
    val theme = LocalMadTheme.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.colors.card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.settings_experimental_need_grants_title),
                color = theme.colors.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                stringResource(R.string.settings_experimental_need_grants_body),
                color = theme.colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            missing.forEach { id ->
                val label = when (id) {
                    ExperimentalGrants.NLS -> stringResource(R.string.settings_experimental_grant_nls)
                    ExperimentalGrants.OVERLAY -> stringResource(R.string.settings_experimental_grant_overlay)
                    ExperimentalGrants.A11Y -> stringResource(R.string.settings_experimental_grant_a11y)
                    ExperimentalGrants.USAGE -> stringResource(R.string.settings_experimental_grant_usage)
                    else -> id
                }
                Text(
                    label,
                    color = theme.colors.bg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.colors.primary)
                        .clickable { onOpenGrant(id) }
                        .padding(vertical = 12.dp),
                )
            }
            DialogButton(
                stringResource(R.string.settings_experimental_turn_on),
                primary = true,
                onClick = onTurnOn,
                theme = theme,
            )
            DialogButton(
                stringResource(R.string.settings_discard),
                primary = false,
                onClick = onCancel,
                theme = theme,
            )
        }
    }
}

@Composable
private fun PermissionsPage(vm: LauncherViewModel) {
    val theme = LocalMadTheme.current
    if (!vm.independentActionBox) {
        val limited = vm.platformProfile.capabilities
            .filterKeys {
                (vm.useMadShadeWindows || it !in MadShadeWindows.experimentalFeatures) &&
                    (vm.useNavShortcuts || it != com.madlauncher.platform.PlatformFeature.NAV_SHORTCUT_OVERLAY)
            }
            .values
            .count { it.isIssue }
        if (limited > 0) {
            Text(
                stringResource(R.string.platform_permissions_warning, limited),
                color = theme.colors.yellow,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.colors.yellow.copy(alpha = 0.1f))
                    .clickable { vm.openSettingsRoute(SettingsRoute.NOT_WORKING) }
                    .padding(10.dp),
            )
        }
    }
    HubRow(stringResource(R.string.settings_permissions_row_title), stringResource(R.string.settings_permissions_row_subtitle)) { vm.openSetup(); vm.closeSettings() }
}

@Composable
private fun EditDocksPage(vm: LauncherViewModel) {
    val s = vm.appearanceDraft
    val c = LocalMadTheme.current.colors
    val context = LocalContext.current
    val iconCap = maxDockIconDp(s.dockHeightDp, s.iconLabelSizeSp)
    val iconValue = s.dockIconSizeDp.coerceIn(32f, iconCap)
    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.settings_dock_preview), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        LaunchedEffect(Unit) { vm.dockRevealed = true }
        Box(Modifier.fillMaxWidth()) {
            BottomDock(vm)
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
            )
        }
        Text(stringResource(R.string.settings_dock_preview_hint), color = c.muted, fontSize = 12.sp)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
    Text(stringResource(R.string.dock_bottom), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            ToggleRow(stringResource(R.string.dock_apps_button), s.showDockApps) {
                vm.patchAppearanceDraft { it.copy(showDockApps = !it.showDockApps) }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            ToggleRow(stringResource(R.string.dock_action_box), s.showDockActionBox) {
                vm.patchAppearanceDraft { it.copy(showDockActionBox = !it.showDockActionBox) }
            }
        }
    }
    val bottomFollows = s.syncDockIcons && s.dockFollowsIconSize
    ToggleRow(stringResource(R.string.dock_match_icons), s.dockFollowsIconSize) {
        vm.setDockFollowsIconSize(!s.dockFollowsIconSize)
    }
    Text(
        if (s.syncDockIcons) stringResource(R.string.dock_match_icons_hint) else stringResource(R.string.dock_match_icons_needs_sync),
        color = c.muted,
        fontSize = 11.sp,
    )
    Text(stringResource(R.string.dock_height), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Slider(
        value = s.dockHeightDp.coerceIn(56f, 148f),
        onValueChange = { v ->
            vm.patchAppearanceDraft {
                val cap = maxDockIconDp(v, it.iconLabelSizeSp)
                it.copy(dockHeightDp = v, dockIconSizeDp = it.dockIconSizeDp.coerceIn(32f, cap))
            }
        },
        enabled = !bottomFollows,
        valueRange = 56f..148f,
    )
    Text(stringResource(R.string.dock_icon_size), color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Slider(
        value = iconValue.coerceIn(32f, iconCap.coerceAtLeast(32f)),
        onValueChange = { v -> vm.patchAppearanceDraft { it.copy(dockIconSizeDp = v) } },
        enabled = !bottomFollows,
        valueRange = 32f..iconCap.coerceAtLeast(32.5f),
    )
    ToggleRow(stringResource(R.string.dock_second_row), s.dockSecondRow) {
        vm.patchAppearanceDraft { it.copy(dockSecondRow = !it.dockSecondRow) }
    }
    Text(
        stringResource(R.string.dock_second_row_hint),
        color = c.muted,
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Text(stringResource(R.string.dock_visibility), color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    SegmentedRow(
        listOf(stringResource(R.string.dock_always_show), stringResource(R.string.dock_auto_hide)),
        if (s.dockAutoHide.isAutoHide) 1 else 0,
    ) { idx ->
        vm.patchAppearanceDraft {
            it.copy(dockAutoHide = if (idx == 1) DockAutoHide.AUTOMATIC else DockAutoHide.REGULAR)
        }
    }
    Text(
        if (s.dockAutoHide.isAutoHide) {
            stringResource(R.string.dock_auto_hide_hint)
        } else {
            stringResource(R.string.dock_always_show_hint)
        },
        color = c.muted,
        fontSize = 11.sp,
    )
    if (s.dockAutoHide.isAutoHide) {
        Text(
            stringResource(R.string.dock_auto_hide_bottom_warning),
            color = c.yellow,
            fontSize = 12.sp,
        )
    }

    EdgeSide.entries.forEach { side ->
        val label = when (side) {
            EdgeSide.LEFT -> stringResource(R.string.dock_left)
            EdgeSide.RIGHT -> stringResource(R.string.dock_right)
            EdgeSide.TOP -> stringResource(R.string.dock_top)
        }
        SectionLabel(label)
        val enabled = vm.hasEdgePanel(side)
        ToggleRow(stringResource(R.string.dock_enabled), enabled) { vm.toggleEdgePanel(side) }
        if (enabled) {
            val panel = vm.edgePanel(side) ?: return@forEach
            val edgeFollows = s.syncDockIcons && panel.followIconSize
            val thickness = panel.thicknessDp.coerceIn(48f, 160f)
            val edgeCap = edgeIconDp(88f, thickness).coerceAtLeast(24f)
            ToggleRow(stringResource(R.string.dock_match_icons), panel.followIconSize) {
                vm.setEdgeFollowsIconSize(side, !panel.followIconSize)
            }
            Text(
                if (s.syncDockIcons) stringResource(R.string.dock_match_icons_hint) else stringResource(R.string.dock_match_icons_needs_sync),
                color = c.muted,
                fontSize = 11.sp,
            )
            Text(
                if (side == EdgeSide.TOP) stringResource(R.string.dock_height) else stringResource(R.string.dock_width),
                color = c.fg,
                fontSize = 13.sp,
            )
            Slider(
                value = thickness,
                onValueChange = { v -> vm.patchEdgePanel(side) { it.copy(thicknessDp = v) } },
                enabled = !edgeFollows,
                valueRange = 48f..160f,
            )
            Text(stringResource(R.string.dock_icon_size), color = c.fg, fontSize = 13.sp)
            Slider(
                value = panel.iconSizeDp.coerceIn(24f, edgeCap),
                onValueChange = { v -> vm.patchEdgePanel(side) { it.copy(iconSizeDp = v) } },
                enabled = !edgeFollows,
                valueRange = 24f..edgeCap.coerceAtLeast(24.5f),
            )
            Text(stringResource(R.string.dock_visibility), color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            SegmentedRow(
                listOf(stringResource(R.string.dock_always_show), stringResource(R.string.dock_auto_hide)),
                if (panel.autoHide.isAutoHide) 1 else 0,
            ) { idx ->
                vm.patchEdgePanel(side) {
                    it.copy(autoHide = if (idx == 1) DockAutoHide.AUTOMATIC else DockAutoHide.REGULAR)
                }
            }
            Text(
                if (panel.autoHide.isAutoHide) {
                    stringResource(R.string.dock_auto_hide_hint)
                } else {
                    stringResource(R.string.dock_always_show_hint)
                },
                color = c.muted,
                fontSize = 11.sp,
            )
            if (side == EdgeSide.LEFT || side == EdgeSide.RIGHT) {
                ToggleRow(stringResource(R.string.dock_show_over_apps), panel.showOverApps) {
                    val enabling = !panel.showOverApps
                    if (enabling && !android.provider.Settings.canDrawOverlays(context)) {
                        runCatching {
                            context.startActivity(
                                com.madlauncher.data.PermissionChecker.overlaySettings(context)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                    vm.patchEdgePanel(side) { it.copy(showOverApps = enabling) }
                }
                Text(
                    stringResource(R.string.dock_show_over_apps_hint),
                    color = c.muted,
                    fontSize = 11.sp,
                )
            }
        }
    }
        }
    }
}

@Composable
private fun ShareApkPage(vm: LauncherViewModel) {
    val theme = LocalMadTheme.current
    val c = theme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val url = remember { MadApkShare.downloadUrl() }
    val qr = remember(url) { MadApkShare.qrBitmap(640, url).asImageBitmap() }
    var sending by remember { mutableStateOf(false) }
    Text(
        stringResource(R.string.share_apk_version, BuildConfig.VERSION_NAME),
        color = c.fg,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    )
    Text(
        stringResource(R.string.share_apk_hint),
        color = c.muted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = qr,
            contentDescription = stringResource(R.string.share_apk_title),
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(10.dp),
        )
    }
    Text(
        url,
        color = c.primary,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
    DialogButton(
        if (sending) stringResource(R.string.share_apk_sending) else stringResource(R.string.share_apk_send),
        primary = true,
        onClick = {
            if (sending) return@DialogButton
            sending = true
            scope.launch {
                MadApkShare.preparePublicApk(context)
                    .onSuccess { file ->
                        runCatching {
                            context.startActivity(
                                android.content.Intent.createChooser(
                                    MadApkShare.apkFileIntent(context, file),
                                    context.getString(R.string.share_apk_send),
                                ),
                            )
                        }
                    }
                    .onFailure { err ->
                        vm.showToast(err.message ?: context.getString(R.string.share_apk_send_failed))
                    }
                sending = false
            }
        },
        theme = theme,
    )
    Spacer(Modifier.height(8.dp))
    DialogButton(stringResource(R.string.share_apk_copy_link), primary = false, onClick = {
        clipboard.setText(AnnotatedString(url))
        vm.showToast(context.getString(R.string.share_apk_copied))
    }, theme = theme)
    Spacer(Modifier.height(8.dp))
    DialogButton(stringResource(R.string.share_apk_share), primary = false, onClick = {
        runCatching {
            context.startActivity(
                android.content.Intent.createChooser(
                    MadApkShare.shareIntent(),
                    context.getString(R.string.share_apk_share),
                ),
            )
        }
    }, theme = theme)
}

@Composable
private fun ExtrasPage(vm: LauncherViewModel) {
    val c = LocalMadTheme.current.colors
    if (!vm.independentActionBox) {
        HubRow(stringResource(R.string.settings_route_fonts), stringResource(R.string.settings_hub_fonts_subtitle)) {
            vm.openSettingsRoute(SettingsRoute.FONTS)
        }
    }
    if (!vm.independentActionBox) {
        HubRow(
            stringResource(R.string.settings_route_experimental),
            stringResource(R.string.settings_hub_experimental_subtitle),
        ) { vm.openSettingsRoute(SettingsRoute.EXPERIMENTAL) }
    }
    HubRow(
        stringResource(R.string.settings_route_not_working),
        stringResource(R.string.settings_hub_not_working_subtitle),
    ) { vm.openSettingsRoute(SettingsRoute.NOT_WORKING) }
    if (!vm.independentActionBox && com.madlauncher.data.MadApkShare.isDistributorBuild()) {
        HubRow(
            stringResource(R.string.settings_route_share_apk),
            stringResource(R.string.settings_hub_share_apk_subtitle),
        ) { vm.openSettingsRoute(SettingsRoute.SHARE_APK) }
    }
    if (vm.independentActionBox) {
        SectionLabel(stringResource(R.string.extras_language_section))
        Text(
            stringResource(R.string.extras_language_follows_system),
            color = c.muted,
            fontSize = 12.sp,
        )
        ExtrasMessageForm(vm)
        return
    }
    val revision = vm.installedLanguageRevision
    val installed = vm.installedLanguagePacks
    val selectedFile = vm.appearanceSettings.languageOverlay
    val context = LocalContext.current
    var dropdownOpen by remember { mutableStateOf(false) }
    var getMoreOpen by remember { mutableStateOf(false) }

    LaunchedEffect(revision) {
        vm.refreshInstalledLanguagePacks()
    }

    LaunchedEffect(Unit) {
        vm.refreshInstalledLanguagePacks()
        if (vm.remoteLanguageCatalog.isEmpty()) vm.refreshRemoteLanguageCatalog()
    }

    val labelMap = vm.remoteLanguageCatalog.associate { it.fileName to it.label } + vm.installedLanguageLabels
    val bundled = LanguageCatalog.bundledEntries(context, labelMap)
    val bundledFiles = bundled.map { it.fileName }.toSet()
    val dropdownPacks = (installed + bundled)
        .filter { com.madlauncher.data.LanguageCodes.isShippedPack(it.fileName) }
        .distinctBy { it.fileName }
        .sortedBy { it.label.lowercase() }

    fun packLabel(fileName: String, label: String): String =
        if (fileName in bundledFiles) context.getString(R.string.extras_language_bundled, label) else label

    val selectedLabel = when {
        selectedFile.isBlank() -> stringResource(R.string.extras_language_english)
        else -> dropdownPacks.find { it.fileName == selectedFile }?.let { packLabel(it.fileName, it.label) }
            ?: packLabel(selectedFile, LanguageCatalog.labelFromFileName(selectedFile))
    }

    SectionLabel(stringResource(R.string.extras_language_section))
    Text(
        stringResource(R.string.extras_language_hint),
        color = c.muted,
        fontSize = 12.sp,
    )
    Spacer(Modifier.height(8.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.bg)
                .clickable { dropdownOpen = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(selectedLabel, color = c.fg, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Text("▾", color = c.muted, fontSize = 16.sp)
        }
        DropdownMenu(
            expanded = dropdownOpen,
            onDismissRequest = { dropdownOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.extras_language_english), color = c.fg) },
                onClick = {
                    vm.setLanguageOverlay("")
                    dropdownOpen = false
                },
            )
            dropdownPacks.forEach { entry ->
                androidx.compose.runtime.key(entry.fileName) {
                    DropdownMenuItem(
                        text = { Text(packLabel(entry.fileName, entry.label), color = c.fg) },
                        onClick = {
                            vm.setLanguageOverlay(entry.fileName)
                            dropdownOpen = false
                        },
                    )
                }
            }
        }
    }
    if (getMoreOpen) {
        GetMoreLanguagesDialog(
            vm = vm,
            labelMap = labelMap,
            onDismiss = { getMoreOpen = false },
        )
    }
    ExtrasMessageForm(vm)
}

@Composable
private fun ExtrasMessageForm(vm: LauncherViewModel) {
    val c = LocalMadTheme.current.colors
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    SectionLabel(stringResource(R.string.extras_message_section))
    Text(
        stringResource(R.string.extras_message_hint),
        color = c.muted,
        fontSize = 12.sp,
    )
    Spacer(Modifier.height(8.dp))
    ExtrasField(stringResource(R.string.extras_message_name), name) { name = it }
    Spacer(Modifier.height(8.dp))
    ExtrasField(stringResource(R.string.extras_message_email), email) { email = it }
    Spacer(Modifier.height(8.dp))
    ExtrasField(stringResource(R.string.extras_message_body), body, minLines = 4) { body = it }
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.extras_message_submit),
        color = c.bg,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.primary)
            .clickable {
                vm.submitHelpImprove(name, email, body)
            }
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun ExtrasField(
    hint: String,
    value: String,
    minLines: Int = 1,
    onChange: (String) -> Unit,
) {
    val c = LocalMadTheme.current.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.bg)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = androidx.compose.ui.text.TextStyle(color = c.fg, fontSize = 13.sp),
            cursorBrush = SolidColor(c.primary),
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = c.muted, fontSize = 13.sp)
                inner()
            },
        )
    }
}

@Composable
private fun GetMoreLanguagesDialog(
    vm: LauncherViewModel,
    labelMap: Map<String, String>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val c = LocalMadTheme.current.colors
    val revision = vm.installedLanguageRevision
    val installedFiles = vm.installedLanguagePacks.map { it.fileName }.toSet()
    val available = vm.remoteLanguageCatalog.filter { it.fileName !in installedFiles }
    val downloading = vm.languageDownloadingFile
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .padding(16.dp)
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(R.string.extras_get_more_title),
                color = c.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(12.dp))
            vm.languageDownloadError?.let { err ->
                Text(err, color = c.red, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            when {
                vm.remoteLanguageLoading -> Text(stringResource(R.string.extras_loading), color = c.muted, fontSize = 14.sp)
                vm.languageCatalogError != null -> Text(
                    vm.languageCatalogError ?: stringResource(R.string.extras_download_failed),
                    color = c.muted,
                    fontSize = 14.sp,
                )
                available.isEmpty() -> Text(stringResource(R.string.extras_no_more_languages), color = c.muted, fontSize = 14.sp)
                else -> available.forEach { entry ->
                    val isDownloading = downloading == entry.fileName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = downloading == null && !isDownloading) {
                                vm.downloadAndApplyLanguage(entry)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(entry.label, color = c.fg, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(
                            when {
                                isDownloading -> stringResource(R.string.extras_downloading)
                                else -> entry.fileName
                            },
                            color = if (isDownloading) c.primary else c.muted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.action_close),
                color = c.primary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onDismiss)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun HubRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(title, color = LocalMadTheme.current.colors.fg, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text(subtitle, color = LocalMadTheme.current.colors.muted, fontSize = 12.sp)
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text.uppercase(), color = LocalMadTheme.current.colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
private fun SegmentedRow(labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { idx, label ->
            Text(
                label,
                color = if (idx == selected) LocalMadTheme.current.colors.fg else LocalMadTheme.current.colors.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (idx == selected) LocalMadTheme.current.colors.primary.copy(alpha = 0.35f) else LocalMadTheme.current.colors.secondary)
                    .clickable { onPick(idx) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AlphaSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Text(label, color = LocalMadTheme.current.colors.fg, fontSize = 13.sp)
    Slider(value = value.coerceIn(0f, 1f), onValueChange = onChange, valueRange = 0f..1f)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LocalMadTheme.current.colors.fg, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowPicker(
    items: List<GestureActionId>,
    selected: GestureActionId,
    experimental: Boolean = false,
    onPick: (GestureActionId) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Text(
                gestureActionLabel(item, experimental),
                fontSize = 11.sp,
                color = if (item == selected) LocalMadTheme.current.colors.fg else LocalMadTheme.current.colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item == selected) LocalMadTheme.current.colors.primary.copy(alpha = 0.3f) else Color.Transparent)
                    .clickable { onPick(item) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun gestureLabel(gesture: GestureId): String = when (gesture) {
    GestureId.SWIPE_UP -> stringResource(R.string.gesture_swipe_up)
    GestureId.SWIPE_DOWN -> stringResource(R.string.gesture_swipe_down)
    GestureId.DOUBLE_TAP -> stringResource(R.string.gesture_double_tap)
    GestureId.TWO_FINGER_UP -> stringResource(R.string.gesture_two_finger_up)
    GestureId.TWO_FINGER_DOWN -> stringResource(R.string.gesture_two_finger_down)
    GestureId.PINCH_IN -> stringResource(R.string.gesture_pinch_in)
    GestureId.PINCH_OUT -> stringResource(R.string.gesture_pinch_out)
    GestureId.TAP_HOME -> stringResource(R.string.gesture_tap_home)
}

@Composable
private fun gestureActionLabel(action: GestureActionId, experimental: Boolean): String = when (action) {
    GestureActionId.NONE -> stringResource(R.string.gesture_action_none)
    GestureActionId.APP_DRAWER -> stringResource(R.string.gesture_action_app_drawer)
    GestureActionId.SEARCH -> stringResource(R.string.gesture_action_search)
    GestureActionId.NOTIFICATIONS -> if (experimental) {
        stringResource(R.string.gesture_action_messages)
    } else {
        stringResource(R.string.gesture_action_notifications)
    }
    GestureActionId.SYSTEM_APPS -> stringResource(R.string.gesture_action_system_apps)
    GestureActionId.QUICK_SETTINGS -> stringResource(R.string.gesture_action_quick_settings)
    GestureActionId.RECENTS -> stringResource(R.string.gesture_action_recents)
    GestureActionId.CUSTOMIZE -> stringResource(R.string.gesture_action_customize)
    GestureActionId.LOCK_SCREEN -> stringResource(R.string.gesture_action_lock_screen)
    GestureActionId.HIDDEN_APPS -> stringResource(R.string.gesture_action_hidden_apps)
}

@Composable
private fun navPlacementLabel(placement: NavShortcutPlacement): String = when (placement) {
    NavShortcutPlacement.NAV_LEFT -> stringResource(R.string.settings_nav_left)
    NavShortcutPlacement.NAV_RIGHT -> stringResource(R.string.settings_nav_right)
    NavShortcutPlacement.SHADE -> stringResource(R.string.settings_nav_shade)
    NavShortcutPlacement.OFF -> stringResource(R.string.settings_nav_off)
}

@Composable
private fun navKindLabel(kind: NavShortcutKind, experimental: Boolean): String = when (kind) {
    NavShortcutKind.NOTIFICATIONS -> if (experimental) {
        stringResource(R.string.nav_shortcut_messages)
    } else {
        stringResource(R.string.nav_shortcut_notifications)
    }
    NavShortcutKind.SYSTEM_APPS -> stringResource(R.string.nav_shortcut_system_apps)
    NavShortcutKind.QUICK_SETTINGS -> stringResource(R.string.nav_shortcut_quick_settings)
}
