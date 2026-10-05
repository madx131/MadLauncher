package com.madlauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.madlauncher.R
import com.madlauncher.action.AppFuzzy
import com.madlauncher.action.ActionAppKind
import com.madlauncher.action.AliasTarget
import com.madlauncher.action.CommandAliases
import com.madlauncher.action.CommonAppShortcuts
import com.madlauncher.action.InstalledActionApp
import com.madlauncher.action.SpaceAddon
import com.madlauncher.action.UserShortcut
import com.madlauncher.action.UserShortcutCombo
import com.madlauncher.action.UserShortcutContact
import com.madlauncher.action.UserShortcutKind
import com.madlauncher.action.UserShortcuts
import com.madlauncher.data.CommandAppEditorRow
import com.madlauncher.data.LaunchableApp
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.action.OfflinePack
import com.madlauncher.data.WebSearchEngine
import com.madlauncher.ui.AppIcon
import com.madlauncher.ui.theme.LocalMadTheme
import kotlin.math.roundToInt

private data class CommandDrag(
    val from: ActionAppKind,
    val packageName: String,
    val name: String,
    val pos: Offset,
)

@Composable
fun ActionBoxCommandsPage(vm: LauncherViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val observed = vm.actionBoxCommandsRev to vm.apps
    val c = LocalMadTheme.current.colors
    LaunchedEffect(vm.apps, vm.loadingApps) {
        vm.noteAbsOpened()
    }
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ActionBoxSearchSettings(vm)
        Text(
            stringResource(R.string.action_box_commands_intro),
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        CommandIndexRow(stringResource(R.string.action_box_kind_message), stringResource(R.string.action_box_command_at_sub)) {
            vm.openCommandEdit(CommandEditId.AT)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_social), stringResource(R.string.action_box_command_apps_words_sub)) {
            vm.openCommandEdit(CommandEditId.SOCIAL)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_call), stringResource(R.string.action_box_command_apps_words_sub)) {
            vm.openCommandEdit(CommandEditId.CALL)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_video), stringResource(R.string.action_box_command_apps_words_sub)) {
            vm.openCommandEdit(CommandEditId.VIDEO)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_common_apps), stringResource(R.string.action_box_command_common_apps_sub)) {
            vm.openCommandEdit(CommandEditId.COMMON_APPS)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_search), stringResource(R.string.action_box_command_search_sub)) {
            vm.openCommandEdit(CommandEditId.SEARCH)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_notes), stringResource(R.string.action_box_command_apps_words_sub)) {
            vm.openCommandEdit(CommandEditId.NOTES)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_translate), stringResource(R.string.action_box_command_apps_words_sub)) {
            vm.openCommandEdit(CommandEditId.TRANSLATE)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_go), stringResource(R.string.action_box_command_apps_words_sub)) {
            vm.openCommandEdit(CommandEditId.NAV)
        }
        if (!vm.independentActionBox) {
            CommandIndexRow(stringResource(R.string.action_box_kind_sticky), stringResource(R.string.action_box_command_words_only_sub)) {
                vm.openCommandEdit(CommandEditId.STICKY)
            }
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_ai), stringResource(R.string.action_box_command_apps_words_sub)) {
            vm.openCommandEdit(CommandEditId.AI)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_contact), stringResource(R.string.action_box_command_contact_sub)) {
            vm.openCommandEdit(CommandEditId.CONTACT)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_add_contact), stringResource(R.string.action_box_command_apps_words_sub)) {
            vm.openCommandEdit(CommandEditId.ADD_CONTACT)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_help), stringResource(R.string.action_box_command_help_sub)) {
            vm.openCommandEdit(CommandEditId.HELP)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_update), stringResource(R.string.action_box_command_update_sub)) {
            vm.openCommandEdit(CommandEditId.UPDATE_APPS)
        }
        CommandIndexRow(stringResource(R.string.action_box_kind_reminder), stringResource(R.string.action_box_command_reminder_sub)) {
            vm.openCommandEdit(CommandEditId.REMINDER)
        }
        if (!vm.independentActionBox) {
            CommandIndexRow(
                stringResource(R.string.action_box_kind_launcher_settings),
                stringResource(R.string.action_box_command_launcher_settings_sub),
            ) {
                vm.openCommandEdit(CommandEditId.LAUNCHER_SETTINGS)
            }
        }
        CommandIndexRow(
            stringResource(R.string.action_box_kind_abs),
            stringResource(R.string.action_box_command_abs_sub),
        ) {
            vm.openCommandEdit(CommandEditId.ACTION_BOX_SETTINGS)
        }
        CommandIndexRow(
            stringResource(R.string.action_box_kind_system_settings),
            stringResource(R.string.action_box_command_system_settings_sub),
        ) {
            vm.openCommandEdit(CommandEditId.SYSTEM_SETTINGS)
        }
        CommandIndexRow(stringResource(R.string.action_box_your_commands), stringResource(R.string.action_box_your_commands_hint)) {
            vm.openCommandEdit(CommandEditId.CUSTOM)
        }
        if (vm.independentActionBox) {
            CommandIndexRow(
                stringResource(R.string.settings_route_about),
                stringResource(R.string.about_actionbox_sub),
            ) {
                vm.openAbout(SettingsRoute.ACTION_BOX_COMMANDS)
            }
        }
    }
        vm.pendingNewAppShortcut?.let { app ->
            NewAppShortcutDialog(vm = vm, app = app)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewAppShortcutDialog(vm: LauncherViewModel, app: LaunchableApp) {
    val c = LocalMadTheme.current.colors
    val suggestions = vm.absShortcutSuggestions(app)
    val remaining = vm.pendingNewAppRemaining
    Dialog(onDismissRequest = { vm.skipNewAppShortcut() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.action_box_new_app_title),
                color = c.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AppIcon(app.icon, contentDescription = app.label, size = 40.dp)
                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(app.label, color = c.fg, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    if (remaining > 1) {
                        Text(
                            stringResource(R.string.action_box_new_app_remaining, remaining),
                            color = c.muted,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.action_box_new_app_body, app.label),
                color = c.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            if (suggestions.isNotEmpty()) {
                Text(
                    stringResource(R.string.action_box_new_app_suggestions),
                    color = c.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    suggestions.forEach { phrase ->
                        val on = vm.newAppShortcutDraft.equals(phrase, ignoreCase = true)
                        Text(
                            phrase,
                            color = if (on) c.fg else c.muted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (on) c.primary.copy(alpha = 0.35f) else c.bg)
                                .clickable { vm.pickNewAppShortcutSuggestion(phrase) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            BasicTextField(
                value = vm.newAppShortcutDraft,
                onValueChange = { raw ->
                    vm.newAppShortcutDraft = raw.filter { it.isLetter() }.take(3).lowercase()
                    vm.clearNewAppShortcutError()
                },
                singleLine = true,
                textStyle = TextStyle(color = c.fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { vm.saveNewAppShortcut() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.bg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (vm.newAppShortcutDraft.isEmpty()) {
                        Text(
                            stringResource(R.string.action_box_new_app_hint),
                            color = c.muted,
                            fontSize = 14.sp,
                        )
                    }
                    inner()
                },
            )
            vm.newAppShortcutError?.let { err ->
                Text(err, color = c.red, fontSize = 12.sp)
            }
            Text(
                stringResource(R.string.action_box_save_command),
                color = c.bg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.primary)
                    .clickable { vm.saveNewAppShortcut() }
                    .padding(vertical = 12.dp),
            )
            Text(
                stringResource(R.string.action_box_new_app_skip),
                color = c.fg,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.secondary)
                    .clickable { vm.skipNewAppShortcut() }
                    .padding(vertical = 12.dp),
            )
            if (remaining > 1) {
                Text(
                    stringResource(R.string.action_box_new_app_skip_all),
                    color = c.muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.skipAllNewAppShortcuts() }
                        .padding(vertical = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionBoxSearchSettings(vm: LauncherViewModel) {
    val c = LocalMadTheme.current.colors
    val s = vm.appearanceDraft
    val engine = s.webSearchEngine
    val ddgEngine = engine == WebSearchEngine.DUCKDUCKGO
    val instantOn = s.instantAnswerCardsEnabled
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.secondary.copy(alpha = 0.45f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.action_box_pill_colors),
            color = c.fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        AccentColorPicker(
            selectedArgb = s.actionBoxPillArgb,
            onPick = { argb -> vm.patchAppearanceDraft { it.copy(actionBoxPillArgb = argb) } },
            title = stringResource(R.string.action_box_pill_color),
            subtitle = stringResource(R.string.action_box_pill_color_sub),
        )
        AccentColorPicker(
            selectedArgb = s.actionBoxTextArgb,
            onPick = { argb -> vm.patchAppearanceDraft { it.copy(actionBoxTextArgb = argb) } },
            title = stringResource(R.string.action_box_text_color),
            subtitle = stringResource(R.string.action_box_text_color_sub),
        )
        Text(
            stringResource(R.string.action_box_search_engine),
            color = c.fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        var optionalOpen by remember { mutableStateOf(false) }
        var pendingEngine by remember { mutableStateOf<WebSearchEngine?>(null) }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val ddgOn = engine == WebSearchEngine.DUCKDUCKGO
            Text(
                webSearchEngineLabel(WebSearchEngine.DUCKDUCKGO),
                color = if (ddgOn) c.fg else c.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (ddgOn) c.primary.copy(alpha = 0.35f) else c.bg)
                    .clickable { vm.setWebSearchEngine(WebSearchEngine.DUCKDUCKGO) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
            val currentEngine = webSearchEngineLabel(engine)
            Text(
                if (ddgEngine) {
                    stringResource(R.string.action_box_optional)
                } else {
                    stringResource(R.string.action_box_optional_on, currentEngine)
                },
                color = if (optionalOpen || !ddgEngine) c.fg else c.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (optionalOpen) c.primary.copy(alpha = 0.35f) else c.bg)
                    .clickable { optionalOpen = true }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        if (pendingEngine == null && optionalOpen) {
            OptionalModulesDialog(
                vm = vm,
                engine = engine,
                onPickEngine = { value ->
                    if (value != engine) pendingEngine = value
                },
                onDismiss = { optionalOpen = false },
            )
        }
        pendingEngine?.let { next ->
            SearchEngineWarningDialog(
                engineLabel = webSearchEngineLabel(next),
                onCancel = { pendingEngine = null },
                onSwitch = {
                    pendingEngine = null
                    vm.setWebSearchEngine(next)
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    stringResource(R.string.action_box_instant_answers),
                    color = c.fg,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    stringResource(
                        if (ddgEngine) R.string.action_box_instant_answers_sub
                        else R.string.action_box_instant_answers_ddg_only,
                    ),
                    color = if (ddgEngine) c.muted else c.primary,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = instantOn,
                enabled = ddgEngine,
                onCheckedChange = { vm.setInstantAnswersEnabled(it) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    stringResource(R.string.settings_experimental_help_window),
                    color = c.fg,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    if (vm.useHelpWindow) {
                        stringResource(R.string.settings_experimental_help_window_on)
                    } else {
                        stringResource(R.string.settings_experimental_help_window_off)
                    },
                    color = c.muted,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = vm.useHelpWindow,
                onCheckedChange = { vm.setExperimentalHelpWindow(it) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionalModulesDialog(
    vm: LauncherViewModel,
    engine: WebSearchEngine,
    onPickEngine: (WebSearchEngine) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalMadTheme.current.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.action_box_optional),
                color = c.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                stringResource(R.string.action_box_search_engine),
                color = c.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                WebSearchEngine.entries.filter { it != WebSearchEngine.DUCKDUCKGO }.forEach { value ->
                    val on = engine == value
                    Text(
                        webSearchEngineLabel(value),
                        color = if (on) c.fg else c.muted,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) c.primary.copy(alpha = 0.35f) else c.bg)
                            .clickable { onPickEngine(value) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                stringResource(R.string.action_box_offline_modules),
                color = c.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            vm.offlineCatalog.forEach { module ->
                val on = module.id in vm.offlineModules
                val busy = vm.offlineModuleBusy == module.id
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(module.name, color = c.fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            when {
                                busy -> stringResource(R.string.action_box_offline_module_busy)
                                else -> offlineModuleSubtitle(module.id)
                            },
                            color = c.muted,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = on || busy,
                        enabled = vm.offlineModuleBusy == null,
                        onCheckedChange = { vm.setOfflineModule(module.id, it) },
                    )
                }
            }
            Text(
                stringResource(R.string.action_close),
                color = c.fg,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.secondary)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun offlineModuleSubtitle(id: String): String = stringResource(
    when (id) {
        OfflinePack.CALC -> R.string.action_box_offline_calc_sub
        OfflinePack.CONVERSIONS -> R.string.action_box_offline_conversions_sub
        OfflinePack.CURRENCY -> R.string.action_box_offline_currency_sub
        OfflinePack.TRANSLATE -> R.string.action_box_offline_translate_sub
        else -> R.string.action_box_offline_modules
    },
)

@Composable
private fun SearchEngineWarningDialog(
    engineLabel: String,
    onCancel: () -> Unit,
    onSwitch: () -> Unit,
) {
    val c = LocalMadTheme.current.colors
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.action_box_engine_warn_title, engineLabel),
                color = c.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                stringResource(R.string.action_box_engine_warn_body),
                color = c.muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            listOf(
                R.string.action_box_engine_warn_bangs,
                R.string.action_box_engine_warn_instant,
                R.string.action_box_engine_warn_currency,
            ).forEach { id ->
                Text("•  " + stringResource(id), color = c.fg, fontSize = 14.sp)
            }
            Text(
                stringResource(R.string.action_box_engine_warn_switch),
                color = c.bg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.primary)
                    .clickable(onClick = onSwitch)
                    .padding(vertical = 12.dp),
            )
            Text(
                stringResource(R.string.cancel),
                color = c.fg,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.secondary)
                    .clickable(onClick = onCancel)
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun webSearchEngineLabel(engine: WebSearchEngine): String = stringResource(
    when (engine) {
        WebSearchEngine.DUCKDUCKGO -> R.string.action_box_search_engine_duckduckgo
        WebSearchEngine.GOOGLE -> R.string.action_box_search_engine_google
        WebSearchEngine.BING -> R.string.action_box_search_engine_bing
        WebSearchEngine.YAHOO -> R.string.action_box_search_engine_yahoo
        WebSearchEngine.STARTPAGE -> R.string.action_box_search_engine_startpage
        WebSearchEngine.ECOSIA -> R.string.action_box_search_engine_ecosia
        WebSearchEngine.BRAVE -> R.string.action_box_search_engine_brave
        WebSearchEngine.YANDEX -> R.string.action_box_search_engine_yandex
    },
)

@Composable
private fun CommandIndexRow(title: String, subtitle: String, onClick: () -> Unit) {
    val c = LocalMadTheme.current.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Text(title, color = c.fg, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text(subtitle, color = c.muted, fontSize = 12.sp)
    }
}

@Composable
fun ActionBoxCommandEditPage(vm: LauncherViewModel) {
    val id = vm.commandEditId ?: return
    @Suppress("UNUSED_VARIABLE")
    val observed = vm.actionBoxCommandsRev to vm.apps
    when (id) {
        CommandEditId.CUSTOM -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
            ) {
                YourCommandsSection(vm)
            }
        }
        CommandEditId.SEARCH -> SearchCommandEditPage(vm)
        CommandEditId.COMMON_APPS -> CommonAppsCommandEditPage(vm)
        else -> CommandEditBody(vm, id)
    }
}

@Composable
private fun SearchCommandEditPage(vm: LauncherViewModel) {
    val c = LocalMadTheme.current.colors
    var word by remember { mutableStateOf("") }
    var useLink by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var pickedPkg by remember { mutableStateOf("") }
    var appFilter by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val pending = vm.consumePendingBangPhrase()
        if (pending.isEmpty()) return@LaunchedEffect
        word = pending
        val hit = AppFuzzy.rank(pending, vm.apps, limit = 1, name = { it.label }).firstOrNull()
        if (hit != null) {
            pickedPkg = hit.packageName
            appFilter = hit.label
        }
    }
    val appHits = if (appFilter.isBlank()) {
        emptyList()
    } else {
        AppFuzzy.rank(appFilter, vm.apps, limit = 6, name = { it.label })
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.action_box_bang_add_title), color = c.fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.action_box_bang_add_hint), color = c.muted, fontSize = 12.sp)
        vm.customBangs.forEach { bang ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.secondary.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("!${bang.phrase}", color = c.fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (bang.isLink) bang.searchUrl else bang.packageName,
                    color = c.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_clear),
                    tint = c.muted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { vm.removeCustomBang(bang.id) },
                )
            }
        }
        Text(stringResource(R.string.action_box_bang_word), color = c.muted, fontSize = 11.sp)
        BangField(word, stringResource(R.string.action_box_bang_word_hint)) { word = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BangModeChip(stringResource(R.string.action_box_bang_app), selected = !useLink) { useLink = false }
            BangModeChip(stringResource(R.string.action_box_bang_link), selected = useLink) { useLink = true }
        }
        if (useLink) {
            BangField(link, stringResource(R.string.action_box_bang_link_hint)) { link = it }
        } else {
            BangField(appFilter, stringResource(R.string.action_box_bang_filter_app)) { appFilter = it }
            appHits.forEach { app ->
                val selected = app.packageName == pickedPkg
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) c.primary.copy(alpha = 0.18f) else c.secondary.copy(alpha = 0.45f))
                        .clickable {
                            pickedPkg = app.packageName
                            appFilter = app.label
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    AppIcon(app.icon, app.label, size = 28.dp)
                    Text(app.label, color = c.fg, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.primary)
                .clickable {
                    vm.saveCustomBang(
                        phraseRaw = word,
                        packageName = if (useLink) "" else pickedPkg,
                        searchUrl = if (useLink) link else "",
                    )
                }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.action_box_bang_save), color = c.bg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            stringResource(R.string.action_box_search_edit_hint),
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        CatalogBangSection(vm)
        Text(stringResource(R.string.action_box_bang_builtin), color = c.fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (vm.searchableApps.isEmpty()) {
            Text(stringResource(R.string.action_box_search_none), color = c.muted, fontSize = 13.sp)
        } else {
            vm.searchableApps.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.secondary.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    AppIcon(item.app.icon, item.app.label, size = 28.dp)
                    Text(
                        item.def.displayName,
                        color = c.fg,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommonAppsCommandEditPage(vm: LauncherViewModel) {
    val c = LocalMadTheme.current.colors
    @Suppress("UNUSED_VARIABLE")
    val observed = vm.apps
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.action_box_common_apps_edit_hint),
            color = c.muted,
            fontSize = 12.sp,
        )
        CommonAppShortcuts.catalog.forEach { shortcut ->
            val app = shortcut.packages.firstNotNullOfOrNull { pkg ->
                vm.apps.firstOrNull { it.packageName.equals(pkg, ignoreCase = true) }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.secondary.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                if (app != null) {
                    AppIcon(app.icon, app.label, size = 28.dp)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (app != null) 10.dp else 0.dp),
                ) {
                    Text(shortcut.label, color = c.fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        shortcut.phrases.joinToString(" · "),
                        color = c.muted,
                        fontSize = 12.sp,
                    )
                    Text(
                        stringResource(
                            R.string.action_box_common_apps_row_sub,
                            commonAddonLabel(shortcut.addon),
                            shortcut.phrases.first(),
                        ),
                        color = c.muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun commonAddonLabel(addon: SpaceAddon): String = stringResource(
    when (addon) {
        SpaceAddon.CALC -> R.string.action_box_common_addon_calc
        SpaceAddon.SEARCH -> R.string.action_box_common_addon_search
        SpaceAddon.NAV -> R.string.action_box_common_addon_nav
        SpaceAddon.NOTE -> R.string.action_box_common_addon_note
        SpaceAddon.CALL -> R.string.action_box_common_addon_call
        SpaceAddon.CHAT -> R.string.action_box_common_addon_chat
        SpaceAddon.EMAIL -> R.string.action_box_common_addon_email
        SpaceAddon.CONTACTS -> R.string.action_box_common_addon_contacts
        SpaceAddon.REMINDER -> R.string.action_box_common_addon_reminder
        SpaceAddon.SCHEDULE -> R.string.action_box_common_addon_schedule
        SpaceAddon.TRANSLATE -> R.string.action_box_common_addon_translate
        SpaceAddon.AI -> R.string.action_box_common_addon_ai
        SpaceAddon.OPEN -> R.string.action_box_common_addon_open
    },
)

@Composable
private fun CatalogBangSection(vm: LauncherViewModel) {
    val c = LocalMadTheme.current.colors
    var filter by remember { mutableStateOf("") }
    val rev = vm.bangCatalogRev
    val rows = remember(filter, rev) {
        vm.matchingBangCatalog(filter, if (filter.isBlank()) 30 else 60)
    }
    Text(
        stringResource(R.string.action_box_bang_catalog, vm.bangCatalogCount()),
        color = c.fg,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
    Text(stringResource(R.string.action_box_bang_catalog_hint), color = c.muted, fontSize = 12.sp)
    BangField(filter, stringResource(R.string.action_box_bang_catalog_filter)) { filter = it }
    rows.forEach { entry ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.secondary.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text("!${entry.trigger}", color = c.fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                entry.name,
                color = c.muted,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun BangModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalMadTheme.current.colors
    Text(
        label,
        color = if (selected) c.bg else c.fg,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) c.primary else c.secondary.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun BangField(value: String, hint: String, onChange: (String) -> Unit) {
    val c = LocalMadTheme.current.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = c.fg, fontSize = 14.sp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.secondary.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(hint, color = c.muted, fontSize = 13.sp)
                inner()
            }
        },
    )
}

@Composable
private fun CommandEditBody(vm: LauncherViewModel, id: CommandEditId) {
    val kinds = commandKindsFor(id)
    val targets = aliasTargetsFor(id)
    var drag by remember { mutableStateOf<CommandDrag?>(null) }
    var parentOrigin by remember { mutableStateOf(Offset.Zero) }
    val sectionTops = remember { mutableStateMapOf<ActionAppKind, Pair<Float, Float>>() }
    val rowMids = remember { mutableStateMapOf<String, Float>() }
    val c = LocalMadTheme.current.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { parentOrigin = it.positionInWindow() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(), enabled = drag == null)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (id == CommandEditId.HELP) {
                Text(
                    stringResource(R.string.action_box_help_edit_hint),
                    color = c.muted,
                    fontSize = 12.sp,
                )
            }
            if (id == CommandEditId.UPDATE_APPS) {
                Text(
                    stringResource(R.string.action_box_update_edit_hint),
                    color = c.muted,
                    fontSize = 12.sp,
                )
            }
            targets.forEach { target ->
                SingleAliasTargetSection(vm, target)
            }
            kinds.forEach { kind ->
                CommandKindSection(
                    vm = vm,
                    kind = kind,
                    draggingPkg = drag?.packageName,
                    onSectionBounds = { top, bottom -> sectionTops[kind] = top to bottom },
                    onRowMid = { pkg, mid -> rowMids["${kind.name}|$pkg"] = mid },
                    onDragStart = { pkg, name, pos ->
                        drag = CommandDrag(kind, pkg, name, pos)
                    },
                    onDrag = { pos -> drag = drag?.copy(pos = pos) },
                    onDragEnd = {
                        val current = drag ?: return@CommandKindSection
                        val target = hitKind(sectionTops, current.pos.y) ?: current.from
                        val index = insertIndex(vm, rowMids, target, current.packageName, current.pos.y)
                        vm.dropCommandApp(current.from, current.packageName, target, index)
                        drag = null
                    },
                    onDragCancel = { drag = null },
                )
            }
        }
        val floating = drag
        if (floating != null) {
            Row(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (floating.pos.x - parentOrigin.x).roundToInt(),
                            (floating.pos.y - parentOrigin.y).roundToInt(),
                        )
                    }
                    .zIndex(20f)
                    .graphicsLayer { alpha = 0.92f }
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.secondary)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(floating.name, color = c.fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun commandKindsFor(id: CommandEditId): List<ActionAppKind> = when (id) {
    CommandEditId.AT -> listOf(ActionAppKind.Messaging, ActionAppKind.Email)
    CommandEditId.SOCIAL -> listOf(ActionAppKind.Social)
    CommandEditId.CALL -> listOf(ActionAppKind.VoiceCall)
    CommandEditId.VIDEO -> listOf(ActionAppKind.VideoCall)
    CommandEditId.NOTES -> listOf(ActionAppKind.Notes)
    CommandEditId.TRANSLATE -> listOf(ActionAppKind.Translator)
    CommandEditId.NAV -> listOf(ActionAppKind.Nav)
    CommandEditId.AI -> listOf(ActionAppKind.Ai)
    CommandEditId.ADD_CONTACT -> listOf(ActionAppKind.Contacts)
    else -> emptyList()
}

private fun aliasTargetsFor(id: CommandEditId): List<AliasTarget> = when (id) {
    CommandEditId.AT -> listOf(AliasTarget.Chat)
    CommandEditId.SOCIAL -> listOf(AliasTarget.Social)
    CommandEditId.CALL -> listOf(AliasTarget.Call)
    CommandEditId.VIDEO -> listOf(AliasTarget.Video)
    CommandEditId.NOTES -> listOf(AliasTarget.Notes)
    CommandEditId.TRANSLATE -> listOf(AliasTarget.Translate)
    CommandEditId.NAV -> listOf(AliasTarget.Nav)
    CommandEditId.STICKY -> listOf(AliasTarget.Sticky)
    CommandEditId.AI -> listOf(AliasTarget.Ai)
    CommandEditId.CONTACT -> listOf(AliasTarget.Contact)
    CommandEditId.ADD_CONTACT -> listOf(AliasTarget.AddContact)
    CommandEditId.HELP -> listOf(AliasTarget.Help)
    CommandEditId.UPDATE_APPS -> listOf(AliasTarget.UpdateApps)
    CommandEditId.REMINDER -> listOf(AliasTarget.Reminder)
    CommandEditId.SYSTEM_SETTINGS -> listOf(AliasTarget.SystemSettings)
    CommandEditId.LAUNCHER_SETTINGS -> listOf(AliasTarget.LauncherSettings)
    CommandEditId.ACTION_BOX_SETTINGS -> listOf(AliasTarget.ActionBoxSettings)
    else -> emptyList()
}

@Composable
private fun SingleAliasTargetSection(vm: LauncherViewModel, target: AliasTarget) {
    var editing by remember { mutableStateOf<Pair<AliasTarget, String>?>(null) }
    var editDraft by remember { mutableStateOf("") }
    val c = LocalMadTheme.current.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.secondary.copy(alpha = 0.45f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.action_box_command_words),
            color = c.fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Text(
            stringResource(R.string.action_box_remove_builtin_hint),
            color = c.muted,
            fontSize = 12.sp,
        )
        AliasTargetWordGroup(
            vm = vm,
            target = target,
            forceShow = true,
            editing = editing,
            editDraft = editDraft,
            onStartEdit = { t, word ->
                editing = t to word
                editDraft = word
            },
            onEditDraft = { editDraft = it },
            onDoneEdit = { t, word ->
                vm.renameCommandAlias(t, word, editDraft)
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YourCommandsSection(vm: LauncherViewModel) {
    @Suppress("UNUSED_VARIABLE")
    val observed = vm.actionBoxCommandsRev to vm.apps
    val c = LocalMadTheme.current.colors
    var addOpen by remember { mutableStateOf(false) }
    var wizardGen by remember { mutableIntStateOf(0) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    val pending = remember { mutableStateOf(listOf<String>()) }
    var appQuery by remember { mutableStateOf("") }
    var pickedPkg by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf(UserShortcutKind.OPEN_APP) }
    var combo by remember { mutableStateOf(UserShortcutCombo.CHAT) }
    var contact by remember { mutableStateOf<UserShortcutContact?>(null) }
    var contactQuery by remember { mutableStateOf("") }
    fun resetAddForm() {
        pending.value = emptyList()
        draft = ""
        appQuery = ""
        pickedPkg = null
        contact = null
        contactQuery = ""
        kind = UserShortcutKind.OPEN_APP
        combo = UserShortcutCombo.CHAT
        wizardGen++
        addOpen = true
        editingId = null
    }
    LaunchedEffect(vm.pendingYourCommandKind, vm.pendingYourCommandPhrase) {
        val pendingKind = vm.pendingYourCommandKind ?: return@LaunchedEffect
        resetAddForm()
        kind = pendingKind
        if (vm.pendingYourCommandPhrase.isNotBlank()) {
            draft = vm.pendingYourCommandPhrase
        }
        vm.consumePendingYourCommand()
    }
    val editing = editingId?.let { id -> vm.userShortcuts.firstOrNull { it.id == id } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.secondary.copy(alpha = 0.45f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (editing != null) {
            BackHandler { editingId = null }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = c.fg,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { editingId = null }
                        .padding(end = 6.dp),
                )
                Text(
                    stringResource(R.string.action_box_edit_command),
                    color = c.fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
            }
            SavedShortcutEditor(vm, editing)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.action_box_your_commands),
                color = c.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (addOpen) stringResource(R.string.action_box_close) else stringResource(R.string.action_box_add_command),
                color = c.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { addOpen = !addOpen }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Text(
            stringResource(R.string.action_box_your_commands_hint),
            color = c.muted,
            fontSize = 12.sp,
        )
        if (vm.userShortcuts.isEmpty() && !addOpen) {
            Text(stringResource(R.string.action_box_no_custom_commands), color = c.muted, fontSize = 12.sp)
        }
        vm.userShortcuts.forEach { shortcut ->
            val appLabel = vm.apps.firstOrNull { it.packageName == shortcut.packageName }?.label
                ?: shortcut.packageName
            CommandIndexRow(
                title = shortcut.phrases.joinToString(" · ").ifBlank { appLabel },
                subtitle = shortcutSummary(shortcut, appLabel),
            ) { editingId = shortcut.id }
        }
        if (addOpen) {
            key(wizardGen) {
                AddCommandWizard(
                    vm = vm,
                    draft = draft,
                    onDraft = { draft = it },
                    pending = pending.value,
                    onPending = { pending.value = it },
                    kind = kind,
                    onKind = { kind = it },
                    combo = combo,
                    onCombo = { combo = it },
                    pickedPkg = pickedPkg,
                    onPickedPkg = { pickedPkg = it },
                    appQuery = appQuery,
                    onAppQuery = { appQuery = it },
                    contact = contact,
                    onContact = { contact = it },
                    contactQuery = contactQuery,
                    onContactQuery = { contactQuery = it },
                    onSaved = { resetAddForm() },
                    onClose = { addOpen = false },
                )
            }
        }
    }
}

private enum class AddCommandStep { ACTION, APP, CONTACT }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCommandWizard(
    vm: LauncherViewModel,
    draft: String,
    onDraft: (String) -> Unit,
    pending: List<String>,
    onPending: (List<String>) -> Unit,
    kind: UserShortcutKind,
    onKind: (UserShortcutKind) -> Unit,
    combo: UserShortcutCombo,
    onCombo: (UserShortcutCombo) -> Unit,
    pickedPkg: String?,
    onPickedPkg: (String?) -> Unit,
    appQuery: String,
    onAppQuery: (String) -> Unit,
    contact: UserShortcutContact?,
    onContact: (UserShortcutContact?) -> Unit,
    contactQuery: String,
    onContactQuery: (String) -> Unit,
    onSaved: () -> Unit,
    onClose: () -> Unit,
) {
    val c = LocalMadTheme.current.colors
    var step by remember { mutableStateOf(AddCommandStep.ACTION) }
    val needsContact = kind == UserShortcutKind.COMBO && combo.usesContact
    val hasWords = pending.isNotEmpty() || draft.isNotBlank()
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val listMax = if (imeOpen) 140.dp else 240.dp
    fun goBack() {
        step = when (step) {
            AddCommandStep.CONTACT -> AddCommandStep.APP
            AddCommandStep.APP -> AddCommandStep.ACTION
            AddCommandStep.ACTION -> {
                onClose()
                AddCommandStep.ACTION
            }
        }
    }
    BackHandler { goBack() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (step != AddCommandStep.ACTION) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = c.fg,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { goBack() }
                        .padding(end = 6.dp),
                )
            }
            Text(
                stringResource(
                    when (step) {
                        AddCommandStep.ACTION -> R.string.action_box_combo_step_action
                        AddCommandStep.APP -> R.string.action_box_combo_step_app
                        AddCommandStep.CONTACT -> R.string.action_box_combo_step_contact
                    },
                ),
                color = c.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        when (step) {
            AddCommandStep.ACTION -> {
                Text(stringResource(R.string.action_box_words), color = c.muted, fontSize = 11.sp)
                AliasDraftRow(
                    value = draft,
                    onValue = onDraft,
                    hint = stringResource(R.string.action_box_words_example),
                    onAdd = {
                        val extra = UserShortcuts.splitDraft(draft)
                        val next = pending.toMutableList()
                        extra.forEach { w ->
                            if (next.none { it.equals(w, true) }) next += w
                        }
                        onPending(next)
                        onDraft("")
                    },
                )
                if (pending.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        pending.forEach { word ->
                            AliasChip(
                                word = word,
                                onEdit = {},
                                onRemove = { onPending(pending.filterNot { it == word }) },
                            )
                        }
                    }
                }
                Text(stringResource(R.string.action_box_shortcut_kind), color = c.muted, fontSize = 11.sp)
                ShortcutKindRow(kind) { next ->
                    onKind(next)
                    onPickedPkg(null)
                    onContact(null)
                    onAppQuery("")
                }
                if (kind == UserShortcutKind.COMBO) {
                    Text(stringResource(R.string.action_box_combo_command), color = c.muted, fontSize = 11.sp)
                    ComboTypeRow(combo) { next ->
                        onCombo(next)
                        onPickedPkg(null)
                        onContact(null)
                        onAppQuery("")
                    }
                }
                WizardCta(
                    label = stringResource(R.string.action_box_combo_next),
                    enabled = hasWords,
                    onClick = { step = AddCommandStep.APP },
                )
            }
            AddCommandStep.APP -> {
                if (kind == UserShortcutKind.OPEN_APP) {
                    Text(stringResource(R.string.action_box_opens_this_app), color = c.muted, fontSize = 11.sp)
                    ShortcutAppPicker(
                        vm = vm,
                        query = appQuery,
                        onQuery = onAppQuery,
                        selectedPkg = pickedPkg,
                        onPick = { onPickedPkg(it) },
                        listMax = listMax,
                    )
                } else {
                    Text(stringResource(R.string.action_box_combo_pick_app), color = c.muted, fontSize = 11.sp)
                    ComboAppPicker(
                        apps = vm.comboAppsFor(combo),
                        query = appQuery,
                        onQuery = onAppQuery,
                        selectedPkg = pickedPkg,
                        onPick = {
                            onPickedPkg(it)
                            onContact(null)
                        },
                        listMax = listMax,
                    )
                }
                val nextLabel = if (needsContact) {
                    stringResource(R.string.action_box_combo_next)
                } else {
                    stringResource(R.string.action_box_save_command)
                }
                WizardCta(
                    label = nextLabel,
                    enabled = pickedPkg != null,
                    onClick = {
                        if (needsContact) {
                            step = AddCommandStep.CONTACT
                        } else {
                            saveNewCommand(
                                vm, pending, draft, pickedPkg, kind, combo, contact, onSaved,
                            )
                        }
                    },
                )
            }
            AddCommandStep.CONTACT -> {
                ComboContactBlock(
                    vm = vm,
                    packageName = pickedPkg.orEmpty(),
                    emails = combo == UserShortcutCombo.CHAT &&
                        vm.installedFor(ActionAppKind.Email, "").any { it.packageName == pickedPkg },
                    selected = contact,
                    query = contactQuery,
                    onQuery = onContactQuery,
                    onPick = onContact,
                    listMax = listMax,
                )
                WizardCta(
                    label = stringResource(R.string.action_box_save_command),
                    enabled = pickedPkg != null && hasWords,
                    onClick = {
                        saveNewCommand(
                            vm, pending, draft, pickedPkg, kind, combo, contact, onSaved,
                        )
                    },
                )
            }
        }
    }
}

private fun saveNewCommand(
    vm: LauncherViewModel,
    pending: List<String>,
    draft: String,
    pickedPkg: String?,
    kind: UserShortcutKind,
    combo: UserShortcutCombo,
    contact: UserShortcutContact?,
    onSaved: () -> Unit,
) {
    val words = pending + UserShortcuts.splitDraft(draft)
    val err = vm.addUserShortcut(
        phrases = words,
        packageName = pickedPkg.orEmpty(),
        kind = kind,
        combo = if (kind == UserShortcutKind.COMBO) combo else null,
        contact = if (kind == UserShortcutKind.COMBO && combo.usesContact) contact else null,
    )
    if (err != null) vm.showToast(err) else onSaved()
}

@Composable
private fun WizardCta(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalMadTheme.current.colors
    Text(
        label,
        color = if (enabled) c.bg else c.muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) c.primary else c.bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedShortcutEditor(vm: LauncherViewModel, shortcut: UserShortcut) {
    val c = LocalMadTheme.current.colors
    val app = vm.apps.firstOrNull { it.packageName == shortcut.packageName }
    var editingWord by remember(shortcut.id) { mutableStateOf<String?>(null) }
    var editDraft by remember(shortcut.id) { mutableStateOf("") }
    var localDraft by remember(shortcut.id) { mutableStateOf("") }
    var picking by remember(shortcut.id) { mutableStateOf(false) }
    var changeQuery by remember(shortcut.id) { mutableStateOf("") }
    var contactQuery by remember(shortcut.id) { mutableStateOf("") }
    val combo = shortcut.combo
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f).clickable { picking = !picking }) {
                Text(
                    app?.label ?: shortcut.packageName,
                    color = c.fg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    shortcutSummary(shortcut, app?.label ?: shortcut.packageName),
                    color = c.muted,
                    fontSize = 11.sp,
                )
            }
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.action_box_remove_command),
                tint = c.muted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { vm.removeUserShortcut(shortcut.id) },
            )
        }
        ShortcutKindRow(shortcut.kind) { next ->
            vm.setUserShortcutKind(shortcut.id, next, shortcut.combo ?: UserShortcutCombo.CHAT)
        }
        if (shortcut.isCombo) {
            ComboTypeRow(combo ?: UserShortcutCombo.CHAT) { next ->
                vm.setUserShortcutKind(shortcut.id, UserShortcutKind.COMBO, next)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            shortcut.phrases.forEach { word ->
                if (editingWord?.equals(word, ignoreCase = true) == true) {
                    AliasEditChip(
                        value = editDraft,
                        onValue = { editDraft = it },
                        onDone = {
                            vm.renameUserShortcutPhrase(shortcut.id, word, editDraft)
                            editingWord = null
                        },
                    )
                } else {
                    AliasChip(
                        word = word,
                        onEdit = {
                            editingWord = word
                            editDraft = word
                        },
                        onRemove = { vm.removeUserShortcutPhrase(shortcut.id, word) },
                    )
                }
            }
        }
        AliasDraftRow(
            value = localDraft,
            onValue = { localDraft = it },
            hint = stringResource(R.string.action_box_add_phrase_hint),
            onAdd = {
                UserShortcuts.splitDraft(localDraft).forEach { w ->
                    vm.addUserShortcutPhrase(shortcut.id, w)
                }
                localDraft = ""
            },
        )
        if (picking) {
            if (shortcut.isCombo && combo != null) {
                ComboAppPicker(
                    apps = vm.comboAppsFor(combo),
                    query = changeQuery,
                    onQuery = { changeQuery = it },
                    selectedPkg = shortcut.packageName,
                    onPick = {
                        vm.setUserShortcutApp(shortcut.id, it)
                        picking = false
                    },
                )
            } else {
                ShortcutAppPicker(
                    vm = vm,
                    query = changeQuery,
                    onQuery = { changeQuery = it },
                    selectedPkg = shortcut.packageName,
                    onPick = {
                        vm.setUserShortcutApp(shortcut.id, it)
                        picking = false
                    },
                )
            }
        }
        if (shortcut.isCombo && combo?.usesContact == true) {
            ComboContactBlock(
                vm = vm,
                packageName = shortcut.packageName,
                emails = vm.installedFor(ActionAppKind.Email, "").any { it.packageName == shortcut.packageName },
                selected = shortcut.contact,
                query = contactQuery,
                onQuery = { contactQuery = it },
                onPick = { vm.setUserShortcutContact(shortcut.id, it) },
            )
        }
    }
}

@Composable
private fun shortcutSummary(shortcut: UserShortcut, appLabel: String): String {
    val combo = shortcut.combo
    val contact = shortcut.contact?.takeIf { it.isPresent }
    return when {
        !shortcut.isCombo -> stringResource(R.string.action_box_opens_this_app)
        combo == UserShortcutCombo.SOCIAL -> stringResource(R.string.action_box_combo_opens_social, appLabel)
        contact != null && combo == UserShortcutCombo.CHAT ->
            stringResource(R.string.action_box_combo_opens_chat, appLabel, contact.name.ifBlank { contact.phone })
        contact != null ->
            stringResource(R.string.action_box_combo_opens_call, appLabel, contact.name.ifBlank { contact.phone })
        else -> stringResource(R.string.action_box_combo_pick_contact, appLabel)
    }
}

@Composable
private fun ShortcutKindRow(kind: UserShortcutKind, onPick: (UserShortcutKind) -> Unit) {
    val c = LocalMadTheme.current.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            UserShortcutKind.OPEN_APP to stringResource(R.string.action_box_shortcut_kind_app),
            UserShortcutKind.COMBO to stringResource(R.string.action_box_shortcut_kind_combo),
        ).forEach { (value, label) ->
            val on = kind == value
            Text(
                label,
                color = if (on) c.fg else c.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.primary.copy(alpha = 0.35f) else c.bg)
                    .clickable { onPick(value) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ComboTypeRow(combo: UserShortcutCombo, onPick: (UserShortcutCombo) -> Unit) {
    val c = LocalMadTheme.current.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        UserShortcutCombo.entries.forEach { value ->
            val on = combo == value
            Text(
                comboLabel(value),
                color = if (on) c.fg else c.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.primary.copy(alpha = 0.35f) else c.bg)
                    .clickable { onPick(value) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun comboLabel(combo: UserShortcutCombo): String = stringResource(
    when (combo) {
        UserShortcutCombo.CHAT -> R.string.action_box_combo_at
        UserShortcutCombo.SOCIAL -> R.string.action_box_combo_social
        UserShortcutCombo.CALL -> R.string.action_box_combo_call
        UserShortcutCombo.VIDEO -> R.string.action_box_combo_video
    },
)

@Composable
private fun ComboAppPicker(
    apps: List<InstalledActionApp>,
    query: String,
    onQuery: (String) -> Unit,
    selectedPkg: String?,
    onPick: (String) -> Unit,
    listMax: androidx.compose.ui.unit.Dp = 220.dp,
) {
    val c = LocalMadTheme.current.colors
    val ranked = if (query.isBlank()) {
        apps.take(16)
    } else {
        AppFuzzy.rank(query, apps, limit = 16, name = { it.name })
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AliasDraftRow(
            value = query,
            onValue = onQuery,
            hint = stringResource(R.string.action_box_search_apps),
            onAdd = {},
            showAdd = false,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = listMax)
                .verticalScroll(rememberScrollState()),
        ) {
            ranked.forEach { app ->
                val on = app.packageName == selectedPkg
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) c.primary.copy(alpha = 0.2f) else c.bg)
                        .clickable { onPick(app.packageName) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    AppIcon(app.icon, contentDescription = app.name, size = 28.dp)
                    Text(
                        app.name,
                        color = c.fg,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComboContactBlock(
    vm: LauncherViewModel,
    packageName: String,
    emails: Boolean,
    selected: UserShortcutContact?,
    query: String,
    onQuery: (String) -> Unit,
    onPick: (UserShortcutContact?) -> Unit,
    listMax: androidx.compose.ui.unit.Dp = 180.dp,
) {
    val c = LocalMadTheme.current.colors
    LaunchedEffect(packageName, query, emails) {
        vm.searchShortcutContacts(packageName, query, emails)
    }
    Text(stringResource(R.string.action_box_combo_contact), color = c.muted, fontSize = 11.sp)
    Text(
        selected?.name?.ifBlank { selected.phone }?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.action_box_combo_contact_none),
        color = c.fg,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onPick(null) }
            .padding(vertical = 4.dp),
    )
    AliasDraftRow(
        value = query,
        onValue = onQuery,
        hint = stringResource(R.string.action_box_combo_contact_hint),
        onAdd = {},
        showAdd = false,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
                .heightIn(max = listMax)
            .verticalScroll(rememberScrollState()),
    ) {
        vm.shortcutContactHits.take(12).forEach { entry ->
            val on = selected?.let { hit ->
                (hit.id.isNotBlank() && hit.id == entry.id) ||
                    hit.name.equals(entry.name, ignoreCase = true)
            } == true
            Text(
                listOfNotNull(entry.name, entry.phone ?: entry.email).joinToString(" · "),
                color = if (on) c.primary else c.fg,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) c.primary.copy(alpha = 0.16f) else c.bg)
                    .clickable { onPick(UserShortcutContact.from(entry)) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ShortcutAppPicker(
    vm: LauncherViewModel,
    query: String,
    onQuery: (String) -> Unit,
    selectedPkg: String?,
    onPick: (String) -> Unit,
    listMax: androidx.compose.ui.unit.Dp = 220.dp,
) {
    val c = LocalMadTheme.current.colors
    val ranked = if (query.isBlank()) {
        vm.apps.sortedBy { it.label.lowercase() }.take(16)
    } else {
        AppFuzzy.rank(query, vm.apps, limit = 16, name = { it.label })
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AliasDraftRow(
            value = query,
            onValue = onQuery,
            hint = stringResource(R.string.action_box_search_apps),
            onAdd = {},
            showAdd = false,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = listMax)
                .verticalScroll(rememberScrollState()),
        ) {
            ranked.forEach { app ->
                val on = app.packageName == selectedPkg
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) c.primary.copy(alpha = 0.2f) else c.bg)
                        .clickable { onPick(app.packageName) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    AppIcon(app.icon, contentDescription = app.label, size = 28.dp)
                    Text(
                        app.label,
                        color = c.fg,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AliasTargetWordGroup(
    vm: LauncherViewModel,
    target: AliasTarget,
    forceShow: Boolean,
    editing: Pair<AliasTarget, String>?,
    editDraft: String,
    onStartEdit: (AliasTarget, String) -> Unit,
    onEditDraft: (String) -> Unit,
    onDoneEdit: (AliasTarget, String) -> Unit,
) {
    val c = LocalMadTheme.current.colors
    val builtIn = CommandAliases.builtIn(target)
    val extra = vm.userAliases.phrases(target)
    val alwaysShow = CommandAliases.stockPhrases(target).isNotEmpty()
    if (!alwaysShow && extra.isEmpty() && !forceShow) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(aliasTargetLabelRes(target)), color = c.fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (builtIn + extra).distinctBy { it.lowercase() }.forEach { word ->
                val isEditing = editing?.first == target && editing?.second.equals(word, ignoreCase = true) == true
                if (isEditing) {
                    AliasEditChip(
                        value = editDraft,
                        onValue = onEditDraft,
                        onDone = { onDoneEdit(target, word) },
                    )
                } else {
                    AliasChip(
                        word = word,
                        onEdit = { onStartEdit(target, word) },
                        onRemove = { vm.removeCommandAlias(target, word) },
                    )
                }
            }
        }
        var localDraft by remember(target) { mutableStateOf("") }
        AliasDraftRow(
            value = localDraft,
            onValue = { localDraft = it },
            hint = stringResource(R.string.action_box_add_word_any),
            onAdd = {
                vm.addCommandAlias(target, localDraft)
                localDraft = ""
            },
        )
    }
}

@Composable
private fun AliasChip(word: String, onEdit: () -> Unit, onRemove: () -> Unit) {
    val c = LocalMadTheme.current.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.primary.copy(alpha = 0.14f))
            .border(1.dp, c.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            word,
            color = c.fg,
            fontSize = 12.sp,
            modifier = Modifier.clickable(onClick = onEdit),
        )
        Icon(
            Icons.Outlined.Close,
            contentDescription = stringResource(R.string.action_box_remove_word),
            tint = c.muted,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(14.dp)
                .clickable(onClick = onRemove),
        )
    }
}

@Composable
private fun AliasEditChip(
    value: String,
    onValue: (String) -> Unit,
    onDone: () -> Unit,
) {
    val c = LocalMadTheme.current.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.bg)
            .border(1.dp, c.primary, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = TextStyle(color = c.fg, fontSize = 12.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth(0.45f),
        )
        Text(
            stringResource(R.string.action_box_save),
            color = c.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(start = 6.dp)
                .clickable(onClick = onDone),
        )
    }
}

@Composable
private fun AliasDraftRow(
    value: String,
    onValue: (String) -> Unit,
    onAdd: () -> Unit,
    hint: String,
    showAdd: Boolean = true,
) {
    val c = LocalMadTheme.current.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(c.bg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = TextStyle(color = c.fg, fontSize = 13.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, color = c.muted, fontSize = 12.sp)
                }
                inner()
            },
        )
        if (showAdd) {
            Text(
                stringResource(R.string.action_box_add),
                color = c.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onAdd)
                    .padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CommandKindSection(
    vm: LauncherViewModel,
    kind: ActionAppKind,
    draggingPkg: String?,
    onSectionBounds: (Float, Float) -> Unit,
    onRowMid: (String, Float) -> Unit,
    onDragStart: (String, String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val c = LocalMadTheme.current.colors
    var addOpen by remember(kind) { mutableStateOf(false) }
    val rows = vm.commandAppEditorRows(kind)
    val known = rows.map { it.app.packageName }.toSet()
    val addable = vm.apps.filter { it.packageName !in known }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val origin = coords.positionInWindow()
                onSectionBounds(origin.y, origin.y + coords.size.height)
            }
            .clip(RoundedCornerShape(12.dp))
            .background(c.secondary.copy(alpha = 0.45f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                commandKindLabel(kind),
                color = c.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (addOpen) Icons.Outlined.Close else Icons.Outlined.Add,
                contentDescription = if (addOpen) {
                    stringResource(R.string.action_box_close_list)
                } else {
                    stringResource(R.string.action_box_add_app)
                },
                tint = c.primary,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { addOpen = !addOpen }
                    .padding(4.dp),
            )
        }
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.action_box_no_apps_drag),
                color = c.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .padding(vertical = 8.dp),
            )
        }
        rows.forEach { row ->
            CommandAppRow(
                row = row,
                dimmed = draggingPkg == row.app.packageName,
                onBounds = { onRowMid(row.app.packageName, it) },
                onRemove = { vm.removeCommandApp(kind, row.app.packageName) },
                onDragStart = { pos -> onDragStart(row.app.packageName, row.app.name, pos) },
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
            )
        }
        if (addOpen) {
            CloseAddListRow { addOpen = false }
            if (addable.isEmpty()) {
                Text(stringResource(R.string.action_box_all_apps_in_command), color = c.muted, fontSize = 12.sp)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    addable.forEach { app ->
                        Text(
                            app.label,
                            color = c.fg,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    vm.addCommandApp(kind, app.packageName)
                                    addOpen = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandAppRow(
    row: CommandAppEditorRow,
    dimmed: Boolean,
    onBounds: (Float) -> Unit,
    onRemove: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val c = LocalMadTheme.current.colors
    var origin by remember { mutableStateOf(Offset.Zero) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (dimmed) 0.35f else 1f }
            .onGloballyPositioned { coords ->
                origin = coords.positionInWindow()
                onBounds(origin.y + coords.size.height / 2f)
            }
            .clip(RoundedCornerShape(10.dp))
            .background(row.app.color.copy(alpha = 0.12f))
            .border(1.dp, row.app.color.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Menu,
            contentDescription = stringResource(R.string.action_box_drag_to_move),
            tint = c.muted,
            modifier = Modifier
                .size(22.dp)
                .pointerInput(row.app.packageName) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { start -> onDragStart(origin + start) },
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(origin + change.position)
                            origin += amount
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                    )
                },
        )
        AppIcon(
            drawable = row.app.icon,
            contentDescription = row.app.name,
            size = 28.dp,
            corner = 8.dp,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(row.app.name, color = c.fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (row.systemDefault) {
                Text(stringResource(R.string.action_box_default_app), color = c.muted, fontSize = 11.sp)
            }
        }
        Icon(
            Icons.Outlined.Close,
            contentDescription = stringResource(R.string.remove),
            tint = c.muted,
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRemove),
        )
    }
}

private fun hitKind(
    sections: Map<ActionAppKind, Pair<Float, Float>>,
    y: Float,
): ActionAppKind? {
    sections.forEach { (kind, range) ->
        if (y >= range.first && y <= range.second) return kind
    }
    return sections.minByOrNull { (_, range) ->
        val mid = (range.first + range.second) / 2f
        kotlin.math.abs(y - mid)
    }?.key
}

private fun insertIndex(
    vm: LauncherViewModel,
    rowMids: Map<String, Float>,
    kind: ActionAppKind,
    excludePkg: String,
    dropY: Float,
): Int {
    val rows = vm.commandAppEditorRows(kind).filter { it.app.packageName != excludePkg }
    rows.forEachIndexed { i, row ->
        val mid = rowMids["${kind.name}|${row.app.packageName}"] ?: return@forEachIndexed
        if (dropY < mid) return i
    }
    return rows.size
}

@Composable
private fun CloseAddListRow(onClose: () -> Unit) {
    val c = LocalMadTheme.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(c.bg.copy(alpha = 0.55f))
            .clickable(onClick = onClose)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.action_box_close_list), color = c.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Icon(
            Icons.Outlined.Close,
            contentDescription = stringResource(R.string.action_box_close_list),
            tint = c.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun commandKindLabel(kind: ActionAppKind): String = stringResource(
    when (kind) {
        ActionAppKind.Messaging -> R.string.action_box_kind_message
        ActionAppKind.Email -> R.string.action_box_kind_email
        ActionAppKind.Social -> R.string.action_box_kind_social
        ActionAppKind.VoiceCall -> R.string.action_box_kind_call
        ActionAppKind.VideoCall -> R.string.action_box_kind_video
        ActionAppKind.Nav -> R.string.action_box_kind_go
        ActionAppKind.Notes -> R.string.action_box_kind_notes
        ActionAppKind.Translator -> R.string.action_box_kind_translate
        ActionAppKind.Ai -> R.string.action_box_kind_ai
        ActionAppKind.Contacts -> R.string.action_box_kind_contacts_apps
    },
)

@Composable
private fun aliasTargetLabelRes(target: AliasTarget): Int = when (target) {
    AliasTarget.Chat -> R.string.action_box_kind_message
    AliasTarget.Email -> R.string.action_box_kind_email
    AliasTarget.Social -> R.string.action_box_kind_social
    AliasTarget.Call -> R.string.action_box_kind_call
    AliasTarget.Video -> R.string.action_box_kind_video
    AliasTarget.Translate -> R.string.action_box_kind_translate
    AliasTarget.Notes -> R.string.action_box_kind_notes
    AliasTarget.Sticky -> R.string.action_box_kind_sticky
    AliasTarget.Ai -> R.string.action_box_kind_ai
    AliasTarget.Contact -> R.string.action_box_kind_contact
    AliasTarget.Nav -> R.string.action_box_kind_go
    AliasTarget.Reminder -> R.string.action_box_kind_reminder
    AliasTarget.Schedule -> R.string.action_box_kind_schedule
    AliasTarget.Help -> R.string.action_box_kind_help
    AliasTarget.AddContact -> R.string.action_box_kind_add_contact
    AliasTarget.UpdateApps -> R.string.action_box_kind_update
    AliasTarget.SystemSettings -> R.string.action_box_kind_system_settings
    AliasTarget.LauncherSettings -> R.string.action_box_kind_launcher_settings
    AliasTarget.ActionBoxSettings -> R.string.action_box_kind_abs
}
