package com.madlauncher.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardTab
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.action.ActionCommand
import com.madlauncher.action.ActionAppKind
import com.madlauncher.action.AppFuzzy
import com.madlauncher.action.UserShortcutCombo
import com.madlauncher.action.UserShortcutKind
import com.madlauncher.action.OnDeviceTranslateUi
import com.madlauncher.action.TranslateCommandParser
import com.madlauncher.action.TranslateLangSlot
import com.madlauncher.action.CalcResult
import com.madlauncher.action.CommandDetector
import com.madlauncher.action.InstantAnswer
import com.madlauncher.action.InstantAnswerClient
import com.madlauncher.action.InstantModules
import com.madlauncher.action.ResolvedSearchApp
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Translate
import com.madlauncher.data.ContactEntry
import com.madlauncher.data.LaunchableApp
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.action.ActionCta
import com.madlauncher.ui.action.AppViaList
import com.madlauncher.ui.action.AppViaTile
import com.madlauncher.ui.theme.LocalMadTheme

@Composable
fun ActionBox(
    vm: LauncherViewModel,
    modifier: Modifier = Modifier,
    expandedMaxHeight: Dp = 380.dp,
) {
    val theme = LocalMadTheme.current
    val c = theme.colors
    val accent = vm.displayAppearance.actionBoxTextColor
    val pill = vm.displayAppearance.actionBoxPillColor
    val focus = remember { FocusRequester() }
    val context = LocalContext.current
    val madOs = com.madlauncher.platform.MadOsRuntime.isMadOs(context)
    val contactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.onContactsPermission(it) }
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        focus.requestFocus()
        // Standalone Action Box does not declare POST_NOTIFICATIONS — requesting it
        // opens a permission sheet that finishes the translucent activity over home.
        if (!com.madlauncher.BuildConfig.ACTION_BOX_APP && !madOs && Build.VERSION.SDK_INT >= 33 &&
            !com.madlauncher.data.PermissionChecker.granted(context, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val requestContacts: () -> Unit = {
        if (!madOs && !com.madlauncher.data.PermissionChecker.granted(context, Manifest.permission.READ_CONTACTS)) {
            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val cmd = vm.command
    val leading = when {
        vm.searchScope != null -> Icons.Outlined.Search
        vm.composeKind == ActionAppKind.Translator -> Icons.Outlined.Translate
        vm.composeKind == ActionAppKind.Notes -> Icons.Outlined.EditNote
        vm.composeKind == ActionAppKind.Contacts -> Icons.Outlined.PersonAdd
        vm.composeKind == ActionAppKind.Ai -> Icons.Outlined.AutoAwesome
        vm.composeKind == ActionAppKind.Nav -> Icons.Outlined.Map
        vm.composeContact != null && vm.composeApp == null -> Icons.Outlined.Person
        vm.contactLookupActive || cmd is ActionCommand.ContactLookup -> Icons.Outlined.Person
        vm.composeApp != null -> Icons.Outlined.Person
        cmd is ActionCommand.ChatPick -> Icons.Outlined.Person
        cmd is ActionCommand.AiAsk -> Icons.Outlined.AutoAwesome
        cmd is ActionCommand.PostPick -> Icons.Outlined.Campaign
        cmd is ActionCommand.Call || cmd is ActionCommand.Emergency -> Icons.Outlined.Phone
        cmd is ActionCommand.VideoCall -> Icons.Outlined.Videocam
        cmd is ActionCommand.NotePick -> Icons.Outlined.EditNote
        cmd is ActionCommand.AddContact -> Icons.Outlined.PersonAdd
        cmd is ActionCommand.Translate -> Icons.Outlined.Translate
        cmd is ActionCommand.Navigate -> Icons.Outlined.Map
        cmd is ActionCommand.OpenApp -> Icons.Outlined.Apps
        cmd is ActionCommand.QuickCombo -> when (cmd.combo) {
            UserShortcutCombo.SOCIAL -> Icons.Outlined.Campaign
            UserShortcutCombo.CALL -> Icons.Outlined.Phone
            UserShortcutCombo.VIDEO -> Icons.Outlined.Videocam
            UserShortcutCombo.CHAT -> Icons.Outlined.Person
        }
        cmd is ActionCommand.AddUserCommand -> Icons.Outlined.Add
        cmd is ActionCommand.Calc -> Icons.Outlined.Calculate
        cmd is ActionCommand.UpdateApps -> Icons.Outlined.Refresh
        cmd is ActionCommand.BarrelRoll -> Icons.Outlined.Search
        else -> Icons.Outlined.Search
    }
    val expanded = vm.actionQuery.isNotBlank() || vm.composeApp != null ||
        vm.searchScope != null ||
        vm.composeContact != null ||
        vm.contactLookupActive ||
        vm.commandPickerOpen ||
        vm.composeKind == ActionAppKind.Translator ||
        vm.composeKind == ActionAppKind.Notes ||
        vm.composeKind == ActionAppKind.Contacts ||
        vm.composeKind == ActionAppKind.Ai ||
        vm.composeKind == ActionAppKind.Nav ||
        cmd !is ActionCommand.None
    val plainSearch = cmd is ActionCommand.None && vm.composeApp == null &&
        vm.composeContact == null &&
        !vm.contactLookupActive &&
        vm.composeKind != ActionAppKind.Translator &&
        vm.composeKind != ActionAppKind.Notes &&
        vm.composeKind != ActionAppKind.Ai &&
        vm.composeKind != ActionAppKind.Nav

    val fieldStyle = MaterialTheme.typography.bodyLarge.copy(color = accent)
    val placeholderStyle = MaterialTheme.typography.bodyLarge.copy(color = accent.copy(alpha = 0.55f))
    val fieldMinHeight = with(LocalDensity.current) {
        val line = fieldStyle.lineHeight.takeIf { it.isSp } ?: fieldStyle.fontSize * 1.25f
        line.toDp() + 20.dp
    }

    Column(
        modifier = modifier
            .shadow(20.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(c.card.copy(alpha = theme.windowAlpha))
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .onPreviewKeyEvent { event ->
                val enter = event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter
                if (!enter) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyUp) vm.runPrimaryAction()
                true
            }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .heightIn(max = expandedMaxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                    vm.searchScope?.let { scope ->
                        if (vm.actionQuery.isNotBlank()) {
                            ActionCta(
                                label = stringResource(R.string.search_in_app, vm.actionQuery, scope.def.displayName),
                                sublabel = scope.app.label,
                                color = Color(scope.def.color),
                                onClick = { vm.searchInScope() },
                            )
                        } else {
                            Text(
                                stringResource(R.string.placeholder_search_in, scope.def.displayName),
                                color = LocalMadTheme.current.colors.muted,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    ComposeBanner(vm)

                    when {
                        cmd is ActionCommand.Help ||
                            (vm.commandPickerOpen && cmd is ActionCommand.None) -> {
                            CommandHelpList(vm, onPick = { vm.applyHelpCommand(it) })
                        }
                        cmd is ActionCommand.AddUserCommand -> {
                            AddCommandFromPill(vm, cmd.phrase)
                        }
                        vm.composeApp != null && vm.composeKind == ActionAppKind.Social -> {
                            ActionCta(
                                label = if (vm.actionQuery.isBlank()) stringResource(R.string.type_your_post) else stringResource(R.string.post_to_app, vm.composeApp!!.name),
                                sublabel = vm.composeApp!!.name,
                                color = vm.composeApp!!.color,
                                enabled = vm.actionQuery.isNotBlank(),
                                onClick = { vm.runPrimaryAction() },
                            )
                        }
                        vm.composeApp != null && vm.composeContact != null -> {
                            ActionCta(
                                label = if (vm.actionQuery.isBlank()) {
                                    stringResource(R.string.type_your_message)
                                } else {
                                    stringResource(R.string.send_to_contact, vm.composeContact!!.name)
                                },
                                sublabel = stringResource(R.string.via_app_opens, vm.composeApp!!.name),
                                color = vm.composeApp!!.color,
                                enabled = vm.actionQuery.isNotBlank(),
                                onClick = { vm.runPrimaryAction() },
                            )
                        }
                        vm.composeContact != null && vm.composeApp == null &&
                            (vm.contactLookupActive || cmd is ActionCommand.ContactLookup) -> {
                            ContactActionAppsStep(vm)
                        }
                        vm.composeApp != null && (
                            vm.composeKind == ActionAppKind.Messaging ||
                                vm.composeKind == ActionAppKind.Email ||
                                vm.composeKind == ActionAppKind.VoiceCall ||
                                vm.composeKind == ActionAppKind.VideoCall
                            ) -> {
                            if (vm.composeKind == ActionAppKind.VoiceCall || vm.composeKind == ActionAppKind.VideoCall) {
                                val video = vm.composeKind == ActionAppKind.VideoCall
                                CallOrVideoStep(
                                    vm = vm,
                                    query = vm.callContactQuery.ifBlank {
                                        when (val c = cmd) {
                                            is ActionCommand.Call -> c.query
                                            is ActionCommand.VideoCall -> c.query
                                            else -> ""
                                        }
                                    },
                                    video = video,
                                    requestContacts = requestContacts,
                                )
                            } else {
                                ContactStep(vm = vm, requestContacts = requestContacts)
                            }
                        }
                        cmd is ActionCommand.ContactLookup -> {
                            ContactLookupStep(vm, requestContacts)
                        }
                        cmd is ActionCommand.ChatPick -> {
                            AppViaList(
                                stringResource(R.string.at_via),
                                vm.installedForAt(cmd.filter),
                                vm.systemDefaultPackage(ActionAppKind.Messaging)
                                    ?: vm.systemDefaultPackage(ActionAppKind.Email),
                            ) {
                                vm.pickAtApp(it)
                            }
                        }
                        cmd is ActionCommand.AiAsk -> {
                            val apps = vm.installedFor(ActionAppKind.Ai, "")
                            when {
                                apps.isEmpty() -> {
                                    Text(
                                        stringResource(R.string.no_ai_assistants),
                                        color = LocalMadTheme.current.colors.muted,
                                        fontSize = 13.sp,
                                    )
                                }
                                apps.size == 1 -> {
                                    val app = apps.first()
                                    ActionCta(
                                        label = if (cmd.prompt.isBlank() && vm.actionQuery.isBlank()) {
                                            stringResource(R.string.open_ai_app, app.name)
                                        } else {
                                            stringResource(R.string.ask_ai_app, app.name)
                                        },
                                        sublabel = vm.actionQuery.ifBlank { cmd.prompt }
                                            .ifBlank { stringResource(R.string.type_ai_prompt) },
                                        color = app.color,
                                        enabled = true,
                                        onClick = { vm.askAi(app, vm.actionQuery.ifBlank { cmd.prompt }) },
                                    )
                                }
                                else -> {
                                    if (cmd.prompt.isNotBlank()) {
                                        Text(
                                            stringResource(R.string.ai_prompt_preview, cmd.prompt),
                                            color = LocalMadTheme.current.colors.muted,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    AppViaList(
                                        stringResource(R.string.ask_ai_via),
                                        apps,
                                        vm.systemDefaultPackage(ActionAppKind.Ai),
                                    ) {
                                        vm.askAi(it, vm.actionQuery.ifBlank { cmd.prompt })
                                    }
                                }
                            }
                        }
                        cmd is ActionCommand.PostPick -> {
                            AppViaList(
                                stringResource(R.string.post_via),
                                vm.installedFor(ActionAppKind.Social, cmd.filter),
                                vm.systemDefaultPackage(ActionAppKind.Social),
                            ) {
                                vm.pickApp(ActionAppKind.Social, it)
                            }
                        }
                        cmd is ActionCommand.Navigate -> {
                            AppViaList(
                                title = if (cmd.destination.isBlank()) stringResource(R.string.navigate_section_title) else stringResource(R.string.navigate_to_destination, cmd.destination.uppercase()),
                                apps = vm.installedFor(ActionAppKind.Nav, ""),
                                defaultPackage = vm.systemDefaultPackage(ActionAppKind.Nav),
                            ) { vm.navigateWith(it, cmd.destination) }
                        }
                        cmd is ActionCommand.Call -> CallStep(vm, cmd, requestContacts)
                        cmd is ActionCommand.VideoCall -> VideoCallStep(vm, cmd, requestContacts)
                        cmd is ActionCommand.Reminder -> {
                            ActionCta(
                                label = stringResource(R.string.add_reminder),
                                sublabel = cmd.text.trim().ifBlank { stringResource(R.string.placeholder_reminder) },
                                color = LocalMadTheme.current.colors.accent,
                                enabled = true,
                                onClick = { vm.saveReminder(cmd.text) },
                            )
                        }
                        cmd is ActionCommand.Note -> {
                            ActionCta(
                                label = if (cmd.floating) stringResource(R.string.create_floating_note) else stringResource(R.string.create_note),
                                sublabel = cmd.text.ifBlank { stringResource(R.string.note_press_enter) },
                                color = LocalMadTheme.current.colors.yellow,
                                enabled = true,
                                onClick = { vm.saveNote(cmd.text, cmd.floating) },
                            )
                        }
                        cmd is ActionCommand.NotePick -> {
                            AppViaList(
                                stringResource(R.string.note_via),
                                vm.installedFor(ActionAppKind.Notes, ""),
                                vm.systemDefaultPackage(ActionAppKind.Notes),
                            ) {
                                vm.pickNotesApp(it, vm.actionQuery)
                            }
                        }
                        cmd is ActionCommand.AddContact -> {
                            AppViaList(
                                stringResource(R.string.action_box_add_contact_via),
                                vm.installedFor(ActionAppKind.Contacts, ""),
                                vm.systemDefaultPackage(ActionAppKind.Contacts),
                            ) {
                                vm.pickAddContactApp(it, cmd.details.ifBlank { vm.actionQuery })
                            }
                        }
                        cmd is ActionCommand.Translate -> TranslateStep(vm, cmd)
                        cmd is ActionCommand.Schedule -> ScheduleStep(vm, cmd.activity)
                        cmd is ActionCommand.Calc -> CalcResultCard(
                            result = cmd.result,
                            ddg = vm.instantAnswer?.takeIf {
                                it.fromDuckDuckGo && vm.appearanceSettings.instantAnswersEnabled
                            },
                        )
                        cmd is ActionCommand.OpenApp -> {
                            val app = vm.apps.firstOrNull { it.packageName == cmd.packageName }
                            ActionCta(
                                label = stringResource(R.string.action_box_open_app, app?.label ?: cmd.packageName),
                                sublabel = cmd.payload.ifBlank { app?.label ?: cmd.packageName },
                                color = LocalMadTheme.current.colors.primary,
                                enabled = true,
                                onClick = { vm.runUserShortcut(cmd.packageName, cmd.payload) },
                            )
                        }
                        cmd is ActionCommand.QuickCombo -> {
                            val app = vm.apps.firstOrNull { it.packageName == cmd.packageName }
                            val appLabel = app?.label ?: cmd.packageName
                            val (label, sub) = when {
                                cmd.combo == UserShortcutCombo.SOCIAL ->
                                    stringResource(R.string.action_box_combo_opens_social, appLabel) to appLabel
                                cmd.hasContact && cmd.combo == UserShortcutCombo.CHAT ->
                                    stringResource(R.string.action_box_combo_opens_chat, appLabel, cmd.contactName.ifBlank { cmd.contactPhone }) to
                                        cmd.contactName.ifBlank { cmd.contactPhone }
                                cmd.hasContact ->
                                    stringResource(R.string.action_box_combo_opens_call, appLabel, cmd.contactName.ifBlank { cmd.contactPhone }) to
                                        cmd.contactName.ifBlank { cmd.contactPhone }
                                else ->
                                    stringResource(R.string.action_box_combo_pick_contact, appLabel) to appLabel
                            }
                            ActionCta(
                                label = label,
                                sublabel = sub,
                                color = LocalMadTheme.current.colors.primary,
                                enabled = true,
                                onClick = { vm.runQuickCombo(cmd) },
                            )
                        }
                        cmd is ActionCommand.Emergency -> {
                            ActionCta(
                                label = stringResource(R.string.call_emergency),
                                sublabel = stringResource(R.string.emergency_confirm),
                                color = LocalMadTheme.current.colors.red,
                                onClick = { vm.confirmEmergency() },
                            )
                        }
                        cmd is ActionCommand.SystemSettings -> {
                            ActionCta(stringResource(R.string.open_system_settings), stringResource(R.string.android_settings), LocalMadTheme.current.colors.primary, onClick = { vm.runPrimaryAction() })
                        }
                        cmd is ActionCommand.LauncherSettings -> {
                            if (vm.independentActionBox) {
                                ActionCta(
                                    stringResource(R.string.action_box_settings),
                                    stringResource(R.string.action_box_commands_hub_sub),
                                    LocalMadTheme.current.colors.primary,
                                    onClick = { vm.runPrimaryAction() },
                                )
                                ActionCta(
                                    stringResource(R.string.permissions_setup),
                                    stringResource(R.string.settings_hub_permissions_subtitle),
                                    LocalMadTheme.current.colors.accent,
                                    onClick = { vm.openSetup() },
                                )
                            } else {
                                ActionCta(stringResource(R.string.open_launcher_settings), stringResource(R.string.themes_icons_gestures), LocalMadTheme.current.colors.primary, onClick = { vm.runPrimaryAction() })
                                ActionCta(stringResource(R.string.permissions_setup), stringResource(R.string.home_overlays_notif_access), LocalMadTheme.current.colors.accent, onClick = { vm.openSetup() })
                            }
                        }
                        cmd is ActionCommand.ActionBoxSettings -> {
                            ActionCta(
                                stringResource(R.string.action_box_settings),
                                stringResource(R.string.help_action_box_settings_desc),
                                LocalMadTheme.current.colors.primary,
                                onClick = { vm.runPrimaryAction() },
                            )
                        }
                        cmd is ActionCommand.UpdateApps -> {
                            ActionCta(
                                stringResource(R.string.action_box_open_updates),
                                stringResource(R.string.action_box_open_updates_sub),
                                LocalMadTheme.current.colors.primary,
                                onClick = { vm.runPrimaryAction() },
                            )
                        }
                        plainSearch && vm.searchScope == null && vm.actionQuery.isNotBlank() -> {
                            QuickSearchBody(vm)
                        }
                    }
                }
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .defaultMinSize(minHeight = fieldMinHeight)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, pill.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .background(pill.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(leading, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = vm.actionQuery,
                onValueChange = vm::onQueryChange,
                singleLine = true,
                maxLines = 1,
                textStyle = fieldStyle,
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onGo = { vm.runPrimaryAction() },
                    onDone = { vm.runPrimaryAction() },
                    onSend = { vm.runPrimaryAction() },
                    onSearch = { vm.runPrimaryAction() },
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        val enter = event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter
                        if (!enter) return@onPreviewKeyEvent false
                        if (event.type == KeyEventType.KeyUp) vm.runPrimaryAction()
                        true
                    },
                decorationBox = { inner ->
                    Box {
                        if (vm.actionQuery.isEmpty()) {
                            Text(
                                placeholder(vm, cmd),
                                style = placeholderStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
            if (vm.actionQuery.isNotEmpty()) {
                IconButton(onClick = { vm.onQueryChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_clear), tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
            IconButton(
                onClick = { vm.openActionBoxSettings() },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.action_box_settings),
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = { vm.closeActionBox() }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickSearchBody(vm: LauncherViewModel) {
    val q = vm.actionQuery
    val complete = vm.autocompleteFor(q)
    val inside = vm.matchingSearchable(q)
    val launch = vm.matchingLaunchApps(q)

    val answer = vm.instantAnswer?.takeIf {
        !it.fromDuckDuckGo || vm.appearanceSettings.instantAnswersEnabled
    }
    if (answer != null) {
        val c = LocalMadTheme.current.colors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.primary.copy(alpha = 0.12f))
                .border(1.dp, c.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .clickable { vm.openInstantAnswer() }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                if (answer.fromDuckDuckGo) InstantAnswerClient.DDG_ATTRIBUTION
                else stringResource(R.string.instant_answer),
                color = c.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            if (answer.heading.isNotBlank() && !answer.heading.equals(answer.text, ignoreCase = true)) {
                Text(answer.heading, color = c.fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(answer.text, color = c.fg, fontSize = 13.sp)
            Text(answer.source, color = c.muted, fontSize = 11.sp)
        }
    }

    if (complete != null && !complete.equals(q, ignoreCase = true)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(LocalMadTheme.current.colors.primary.copy(alpha = 0.12f))
                .border(1.dp, LocalMadTheme.current.colors.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .clickable { vm.onQueryChange(complete) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.KeyboardTab, contentDescription = null, tint = LocalMadTheme.current.colors.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(q, color = LocalMadTheme.current.colors.muted, fontSize = 13.sp)
            Text(
                if (complete.startsWith(q, ignoreCase = true)) complete.substring(q.length) else complete,
                color = LocalMadTheme.current.colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    if (inside.isNotEmpty()) {
        Text(stringResource(R.string.search_inside_app), color = LocalMadTheme.current.colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
        inside.forEach { item ->
            SearchInsideTile(item) { vm.pickSearchScope(item) }
        }
    }

    if (launch.isNotEmpty()) {
        Text(stringResource(R.string.apps_section), color = LocalMadTheme.current.colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            launch.forEach { app ->
                SearchLaunchAppChip(vm, app)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalMadTheme.current.colors.bg)
            .border(1.dp, LocalMadTheme.current.colors.border, RoundedCornerShape(12.dp))
            .clickable { vm.runPrimaryAction() }
            .focusProperties { canFocus = false }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Language, contentDescription = null, tint = LocalMadTheme.current.colors.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.search_web), color = LocalMadTheme.current.colors.fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SearchLaunchAppChip(vm: LauncherViewModel, app: LaunchableApp) {
    val c = LocalMadTheme.current.colors
    var coords by remember(app.id) { mutableStateOf<LayoutCoordinates?>(null) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.bg)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .appLongPress(
                key = "actionbox/${app.id}",
                coordinates = coords,
                onPositioned = { coords = it },
                onTap = {
                    vm.launchApp(app)
                    vm.closeActionBox()
                },
                onHoldStart = { vm.beginAppHold(app, "actionbox") },
                onMove = { x, y, moved -> vm.updateHoldPointer(x, y, moved) },
                onHoldEnd = { vm.endAppHold() },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.icon, app.label, 22.dp, 6.dp)
        Spacer(Modifier.width(8.dp))
        Text(app.label, color = c.fg, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CalcResultCard(result: CalcResult, ddg: InstantAnswer? = null) {
    val c = LocalMadTheme.current.colors
    val answer = ddg?.text?.ifBlank { null } ?: result.answer
    val label = if (ddg != null) InstantAnswerClient.DDG_ATTRIBUTION
    else stringResource(R.string.calc_result_label)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.primary.copy(alpha = 0.14f))
            .border(1.dp, c.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            color = c.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        Text(
            answer,
            color = c.fg,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            result.expression,
            color = c.muted,
            fontSize = 13.sp,
        )
        if (ddg == null) {
            result.extras.forEach { line ->
                Text(line, color = c.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        Text(
            stringResource(R.string.calc_enter_copy),
            color = c.muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SearchInsideTile(item: ResolvedSearchApp, onSelect: () -> Unit) {
    val fake = com.madlauncher.action.InstalledActionApp(
        name = item.def.displayName,
        packageName = item.app.packageName,
        color = Color(item.def.color),
        icon = item.app.icon,
    )
    AppViaTile(app = fake, hint = stringResource(R.string.action_tap_or_drag), onSelect = onSelect)
}

@Composable
private fun ComposeBanner(vm: LauncherViewModel) {
    if (vm.composeApp == null) {
        val banner = when {
            vm.composeKind == ActionAppKind.Translator -> stringResource(R.string.translate_compose_banner)
            vm.composeKind == ActionAppKind.Notes -> stringResource(R.string.notes_compose_banner)
            vm.composeKind == ActionAppKind.Contacts -> stringResource(R.string.help_add_contact_label)
            vm.composeKind == ActionAppKind.Ai -> stringResource(R.string.ai_compose_banner)
            vm.composeKind == ActionAppKind.Nav -> stringResource(R.string.nav_compose_banner)
            vm.composeContact != null && vm.contactLookupActive ->
                stringResource(R.string.contact_lookup_pick_app, vm.composeContact!!.name)
            vm.contactLookupActive -> stringResource(R.string.contact_lookup_banner)
            else -> null
        }
        if (banner != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LocalMadTheme.current.colors.primary.copy(alpha = 0.14f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    banner,
                    color = LocalMadTheme.current.colors.fg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.compose_clear),
                    color = LocalMadTheme.current.colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { vm.clearCompose() },
                )
            }
            return
        }
    }
    val app = vm.composeApp ?: return
    val label = when {
        vm.composeKind == ActionAppKind.Social -> stringResource(R.string.post_to_social_banner, app.name)
        vm.composeKind == ActionAppKind.VoiceCall -> stringResource(R.string.call_via_app, app.name)
        vm.composeKind == ActionAppKind.VideoCall -> stringResource(R.string.video_call_via_app, app.name)
        vm.composeContact != null -> "${vm.composeContact!!.name} · ${app.name}"
        else -> stringResource(R.string.app_recent_conversations, app.name)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(app.color.copy(alpha = 0.16f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.icon, app.name, 22.dp, 6.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = LocalMadTheme.current.colors.fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.compose_clear), color = LocalMadTheme.current.colors.muted, fontSize = 12.sp, modifier = Modifier.clickable { vm.clearCompose() })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslateStep(vm: LauncherViewModel, @Suppress("UNUSED_PARAMETER") cmd: ActionCommand.Translate) {
    val parsed = vm.parsedTranslate(vm.actionQuery)
    val fromLabel = TranslateCommandParser.label(parsed.fromCode)
    val toLabel = TranslateCommandParser.label(parsed.toCode)
    val complete = vm.autocompleteFor(vm.actionQuery)
    val q = vm.actionQuery

    LaunchedEffect(parsed.text, parsed.fromCode, parsed.toCode) {
        if (InstantModules.isEnabled(InstantModules.TRANSLATE) &&
            parsed.toCode != null &&
            parsed.text.isNotBlank()
        ) {
            vm.requestOnDeviceTranslate(parsed.text, parsed.fromCode, parsed.toCode)
        } else {
            vm.requestOnDeviceTranslate("", null, null)
        }
    }

    if (fromLabel != null || toLabel != null) {
        val route = when {
            fromLabel != null && toLabel != null -> stringResource(R.string.translate_route, fromLabel, toLabel)
            fromLabel != null -> stringResource(R.string.translate_from_only, fromLabel)
            toLabel != null -> stringResource(R.string.translate_to_only, toLabel)
            else -> null
        }
        if (route != null) {
            Text(route, color = LocalMadTheme.current.colors.muted, fontSize = 12.sp)
        }
    } else if (parsed.text.isBlank()) {
        Text(
            stringResource(R.string.translate_hint_word),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 12.sp,
        )
    }

    val composeMode = vm.composeKind == ActionAppKind.Translator
    if (!composeMode && complete != null && !complete.equals(q, ignoreCase = true)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(LocalMadTheme.current.colors.primary.copy(alpha = 0.12f))
                .border(1.dp, LocalMadTheme.current.colors.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .clickable { vm.onQueryChange(complete) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.KeyboardTab, contentDescription = null, tint = LocalMadTheme.current.colors.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(q, color = LocalMadTheme.current.colors.muted, fontSize = 13.sp)
            Text(
                if (complete.startsWith(q, ignoreCase = true)) complete.substring(q.length) else complete,
                color = LocalMadTheme.current.colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    when (val tr = vm.onDeviceTranslate) {
        is OnDeviceTranslateUi.Working -> {
            Text(tr.message, color = LocalMadTheme.current.colors.muted, fontSize = 12.sp)
        }
        is OnDeviceTranslateUi.Done -> {
            val c = LocalMadTheme.current.colors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.primary.copy(alpha = 0.12f))
                    .border(1.dp, c.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.translate_on_device),
                    color = c.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
                Text(tr.text, color = c.fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
        is OnDeviceTranslateUi.Error -> {
            Text(tr.message, color = LocalMadTheme.current.colors.muted, fontSize = 12.sp)
        }
        OnDeviceTranslateUi.Idle -> Unit
    }

    if (parsed.activeSlot != null) {
        val slotLabel = when (parsed.activeSlot) {
            TranslateLangSlot.From -> stringResource(R.string.translate_pick_from)
            TranslateLangSlot.To -> stringResource(R.string.translate_pick_to)
        }
        Text(
            slotLabel,
            color = LocalMadTheme.current.colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
    }
    val langs = vm.translateLanguageSuggestions(vm.actionQuery)
    if (langs.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            langs.forEach { lang ->
                val selected = lang.code == parsed.toCode || lang.code == parsed.fromCode
                val chip = if (parsed.activeSlot == TranslateLangSlot.From) {
                    stringResource(R.string.translate_chip_from, lang.displayName)
                } else {
                    stringResource(R.string.translate_chip_to, lang.displayName)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) LocalMadTheme.current.colors.primary.copy(alpha = 0.18f)
                            else LocalMadTheme.current.colors.bg,
                        )
                        .border(
                            1.dp,
                            if (selected) LocalMadTheme.current.colors.primary.copy(alpha = 0.5f)
                            else LocalMadTheme.current.colors.border,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { vm.applyTranslateLanguage(lang) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(chip, color = LocalMadTheme.current.colors.fg, fontSize = 13.sp)
                }
            }
        }
    }

    AppViaList(
        stringResource(R.string.translate_via),
        vm.installedFor(ActionAppKind.Translator, ""),
        vm.systemDefaultPackage(ActionAppKind.Translator),
    ) {
        vm.pickTranslatorApp(it, vm.actionQuery)
    }
}

@Composable
private fun ContactStep(
    vm: LauncherViewModel,
    requestContacts: () -> Unit,
) {
    val app = vm.composeApp ?: return
    LaunchedEffect(app.packageName, vm.recipientQuery, vm.actionQuery, vm.contactsAllowed) {
        vm.refreshContacts()
    }
    RecipientPickList(
        vm = vm,
        appColor = app.color,
        appName = app.name,
        onSelect = { vm.pickContact(it) },
        requestContacts = requestContacts,
    )
}

@Composable
private fun ContactLookupStep(
    vm: LauncherViewModel,
    requestContacts: () -> Unit,
) {
    LaunchedEffect(vm.recipientQuery, vm.actionQuery, vm.contactsAllowed, vm.contactLookupActive) {
        vm.refreshContacts()
    }
    RecipientPickList(
        vm = vm,
        appColor = LocalMadTheme.current.colors.primary,
        appName = stringResource(R.string.contact_lookup_recents),
        onSelect = { vm.pickLookupContact(it) },
        requestContacts = requestContacts,
    )
}

@Composable
private fun ContactActionAppsStep(vm: LauncherViewModel) {
    val messageApps = vm.installedForAt("")
    val callApps = vm.installedFor(ActionAppKind.VoiceCall, "")
    if (messageApps.isNotEmpty()) {
        AppViaList(
            stringResource(R.string.contact_lookup_message_via),
            messageApps,
            vm.systemDefaultPackage(ActionAppKind.Messaging),
        ) {
            vm.pickLookupActionApp(it, call = false)
        }
    }
    if (callApps.isNotEmpty()) {
        AppViaList(
            stringResource(R.string.contact_lookup_call_via),
            callApps,
            vm.systemDefaultPackage(ActionAppKind.VoiceCall),
        ) {
            vm.pickLookupActionApp(it, call = true)
        }
    }
    if (messageApps.isEmpty() && callApps.isEmpty()) {
        Text(
            stringResource(R.string.no_call_apps),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun VideoCallStep(
    vm: LauncherViewModel,
    cmd: ActionCommand.VideoCall,
    requestContacts: () -> Unit,
) {
    CallOrVideoStep(vm, cmd.query, video = true, requestContacts = requestContacts)
}

@Composable
private fun CallStep(
    vm: LauncherViewModel,
    cmd: ActionCommand.Call,
    requestContacts: () -> Unit,
) {
    CallOrVideoStep(vm, cmd.query, video = false, requestContacts = requestContacts)
}

@Composable
private fun CallOrVideoStep(
    vm: LauncherViewModel,
    query: String,
    video: Boolean,
    requestContacts: () -> Unit,
) {
    val kind = if (video) ActionAppKind.VideoCall else ActionAppKind.VoiceCall
    val app = vm.composeApp
    val isPhone = CommandDetector.isPhoneLike(query)

    if (app == null || vm.composeKind != kind) {
        if (isPhone) {
            ActionCta(
                label = if (video) {
                    stringResource(R.string.dial_video_number, query.trim())
                } else {
                    stringResource(R.string.dial_number, query.trim())
                },
                sublabel = if (video) {
                    stringResource(R.string.open_video_dialer)
                } else {
                    stringResource(R.string.open_dialer)
                },
                color = LocalMadTheme.current.colors.accent,
                onClick = { vm.runPrimaryAction() },
            )
        }
        val title = if (video) stringResource(R.string.video_call_via) else stringResource(R.string.call_via)
        val apps = vm.installedFor(kind, if (isPhone) "" else query)
        if (apps.isEmpty()) {
            Text(
                stringResource(R.string.no_call_apps),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 13.sp,
            )
        } else {
            AppViaList(title, apps, vm.systemDefaultPackage(kind)) { vm.pickCallApp(kind, it, if (isPhone) "" else query) }
            Text(
                stringResource(R.string.actionbox_ott_calls_disabled),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { vm.openSettingsRoute(com.madlauncher.ui.settings.SettingsRoute.NOT_WORKING) },
            )
        }
        return
    }

    LaunchedEffect(app.packageName, vm.callContactQuery, vm.actionQuery, vm.contactsAllowed) {
        vm.refreshContacts()
    }
    RecipientPickList(
        vm = vm,
        appColor = app.color,
        appName = app.name,
        onSelect = { contact -> vm.callContact(contact, video) },
        requestContacts = requestContacts,
        showDialSubtitle = true,
    )
}

@Composable
private fun RecipientPickList(
    vm: LauncherViewModel,
    appColor: androidx.compose.ui.graphics.Color,
    appName: String,
    onSelect: (ContactEntry) -> Unit,
    requestContacts: () -> Unit,
    showDialSubtitle: Boolean = false,
) {
    val (recent, phone) = com.madlauncher.action.RecipientPicker.partition(vm.contacts)
    if (vm.contactsSyncing) {
        Text(stringResource(R.string.app_recipients_syncing), color = LocalMadTheme.current.colors.muted, fontSize = 13.sp)
    }
    if (!vm.contactsSyncing && recent.isEmpty() && phone.isEmpty() && !vm.contactsAllowed) {
        ActionCta(
            label = stringResource(R.string.allow_contacts),
            sublabel = stringResource(R.string.allow_contacts_sublabel),
            color = LocalMadTheme.current.colors.primary,
            onClick = requestContacts,
        )
    }
    if (recent.isNotEmpty()) {
        Text(
            stringResource(R.string.app_recent_conversations, appName),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        recent.forEach { c ->
            val sub = if (showDialSubtitle) c.phone ?: c.name else null
            ContactRow(c, appColor, sub, onSelect)
        }
    } else if (!vm.contactsSyncing && vm.contactsAllowed) {
        Text(
            stringResource(R.string.no_recent_conversations, appName),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 13.sp,
        )
    }
    if (phone.isNotEmpty()) {
        Text(
            stringResource(R.string.phone_contacts),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(top = if (recent.isNotEmpty()) 6.dp else 0.dp),
        )
        phone.forEach { c ->
            val sub = if (showDialSubtitle) c.phone ?: c.email else null
            ContactRow(c, appColor, sub, onSelect)
        }
    } else if (!vm.contactsAllowed && recent.isNotEmpty()) {
        ActionCta(
            label = stringResource(R.string.allow_contacts),
            sublabel = stringResource(R.string.allow_contacts_sublabel),
            color = LocalMadTheme.current.colors.primary,
            onClick = requestContacts,
        )
    } else if (!vm.contactsSyncing && vm.contactsAllowed && recent.isEmpty() && phone.isEmpty()) {
        Text(stringResource(R.string.no_contacts_found), color = LocalMadTheme.current.colors.muted, fontSize = 13.sp)
    }
}

@Composable
private fun ContactRow(contact: ContactEntry, color: androidx.compose.ui.graphics.Color, subtitle: String? = null, onSelect: (ContactEntry) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LocalMadTheme.current.colors.bg)
            .clickable { onSelect(contact) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(contact.name.take(1).uppercase(), color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(contact.name, color = LocalMadTheme.current.colors.fg, fontWeight = FontWeight.Medium)
            val sub = subtitle ?: contact.phone ?: contact.email
            if (!sub.isNullOrBlank()) Text(sub, color = LocalMadTheme.current.colors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScheduleStep(vm: LauncherViewModel, activity: String) {
    Text(stringResource(R.string.schedule_section), color = LocalMadTheme.current.colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
    BasicTextField(
        value = vm.scheduleDate,
        onValueChange = { vm.scheduleDate = it },
        singleLine = true,
        textStyle = TextStyle(color = LocalMadTheme.current.colors.fg, fontSize = 14.sp),
        cursorBrush = SolidColor(LocalMadTheme.current.colors.accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LocalMadTheme.current.colors.bg)
            .padding(12.dp),
        decorationBox = { inner ->
            if (vm.scheduleDate.isEmpty()) Text(stringResource(R.string.schedule_date_hint), color = LocalMadTheme.current.colors.muted, fontSize = 14.sp)
            inner()
        },
    )
    BasicTextField(
        value = vm.scheduleTime,
        onValueChange = { vm.scheduleTime = it },
        singleLine = true,
        textStyle = TextStyle(color = LocalMadTheme.current.colors.fg, fontSize = 14.sp),
        cursorBrush = SolidColor(LocalMadTheme.current.colors.accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LocalMadTheme.current.colors.bg)
            .padding(12.dp),
        decorationBox = { inner ->
            if (vm.scheduleTime.isEmpty()) Text(stringResource(R.string.schedule_time_hint), color = LocalMadTheme.current.colors.muted, fontSize = 14.sp)
            inner()
        },
    )
    val title = activity.ifBlank { vm.actionQuery.removePrefix("/c").trim() }
    ActionCta(
        label = stringResource(R.string.add_to_calendar),
        sublabel = title.ifBlank { stringResource(R.string.type_activity_after_c) },
        color = LocalMadTheme.current.colors.primary,
        enabled = title.isNotBlank(),
        onClick = { vm.saveEvent(title, vm.scheduleDate, vm.scheduleTime) },
    )
}

@Composable
private fun placeholder(vm: LauncherViewModel, cmd: ActionCommand): String {
    if (vm.composeKind == ActionAppKind.Translator) return stringResource(R.string.placeholder_translate_text)
    if (vm.composeKind == ActionAppKind.Notes) return stringResource(R.string.placeholder_note_body)
    if (vm.composeKind == ActionAppKind.Ai) return stringResource(R.string.placeholder_ask_ai)
    if (vm.composeKind == ActionAppKind.Nav) return stringResource(R.string.placeholder_navigate_place)
    if (vm.composeKind == ActionAppKind.Social) return stringResource(R.string.placeholder_whats_on_mind)
    if (vm.composeContact != null && vm.composeApp == null) {
        return stringResource(R.string.placeholder_pick_action_app)
    }
    if (vm.composeContact != null) return stringResource(R.string.placeholder_message_contact, vm.composeContact!!.name)
    if (vm.composeApp != null) return stringResource(R.string.placeholder_search_contacts)
    if (vm.contactLookupActive || cmd is ActionCommand.ContactLookup) {
        return stringResource(R.string.placeholder_search_contacts)
    }
    if (vm.searchScope != null) return stringResource(R.string.placeholder_search_in, vm.searchScope!!.def.displayName)
    return when (cmd) {
        is ActionCommand.ChatPick -> stringResource(R.string.placeholder_pick_at)
        is ActionCommand.ContactLookup -> stringResource(R.string.placeholder_search_contacts)
        is ActionCommand.AiAsk -> stringResource(R.string.placeholder_ask_ai)
        is ActionCommand.PostPick -> stringResource(R.string.placeholder_pick_social)
        is ActionCommand.Call -> stringResource(R.string.placeholder_search_contacts_or_number)
        is ActionCommand.VideoCall -> stringResource(R.string.placeholder_video_call)
        is ActionCommand.Reminder -> stringResource(R.string.placeholder_reminder)
        is ActionCommand.Note -> stringResource(R.string.placeholder_note)
        is ActionCommand.NotePick -> stringResource(R.string.placeholder_note)
        is ActionCommand.AddContact -> stringResource(R.string.action_box_placeholder_add_contact)
        is ActionCommand.Translate -> stringResource(R.string.placeholder_translate)
        is ActionCommand.Schedule -> stringResource(R.string.placeholder_activity)
        is ActionCommand.Navigate -> stringResource(R.string.placeholder_navigate)
        is ActionCommand.OpenApp -> stringResource(R.string.action_box_placeholder_open_app)
        is ActionCommand.QuickCombo -> when {
            cmd.combo == UserShortcutCombo.SOCIAL -> stringResource(R.string.placeholder_pick_social)
            cmd.hasContact && cmd.combo == UserShortcutCombo.CHAT ->
                stringResource(R.string.placeholder_message_contact, cmd.contactName.ifBlank { cmd.contactPhone })
            else -> stringResource(R.string.placeholder_search_contacts)
        }
        is ActionCommand.AddUserCommand -> stringResource(R.string.placeholder_add_command)
        is ActionCommand.Emergency -> stringResource(R.string.placeholder_emergency)
        is ActionCommand.UpdateApps -> stringResource(R.string.placeholder_update_apps)
        is ActionCommand.BarrelRoll -> stringResource(R.string.placeholder_search_command)
        is ActionCommand.Calc -> stringResource(R.string.placeholder_calc)
        else -> stringResource(R.string.placeholder_search_command)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCommandFromPill(vm: LauncherViewModel, phrase: String) {
    val c = LocalMadTheme.current.colors
    var kind by remember { mutableStateOf(UserShortcutKind.OPEN_APP) }
    var combo by remember { mutableStateOf(UserShortcutCombo.CHAT) }
    var pickedPkg by remember { mutableStateOf<String?>(null) }
    var appQuery by remember { mutableStateOf("") }
    val wordsReady = phrase.isNotBlank()
    if (!wordsReady) {
        Text(
            stringResource(R.string.placeholder_add_command),
            color = c.muted,
            fontSize = 13.sp,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.action_box_add_command),
            color = c.fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
        Text(phrase, color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val openOn = kind == UserShortcutKind.OPEN_APP
            Text(
                stringResource(R.string.action_box_shortcut_kind_app),
                color = if (openOn) c.fg else c.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (openOn) c.primary.copy(alpha = 0.35f) else c.bg)
                    .clickable {
                        kind = UserShortcutKind.OPEN_APP
                        pickedPkg = null
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
            UserShortcutCombo.entries.forEach { value ->
                val on = kind == UserShortcutKind.COMBO && combo == value
                val label = when (value) {
                    UserShortcutCombo.CHAT -> stringResource(R.string.action_box_combo_at)
                    UserShortcutCombo.SOCIAL -> stringResource(R.string.action_box_combo_social)
                    UserShortcutCombo.CALL -> stringResource(R.string.action_box_combo_call)
                    UserShortcutCombo.VIDEO -> stringResource(R.string.action_box_combo_video)
                }
                Text(
                    label,
                    color = if (on) c.fg else c.muted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) c.primary.copy(alpha = 0.35f) else c.bg)
                        .clickable {
                            kind = UserShortcutKind.COMBO
                            combo = value
                            pickedPkg = null
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        val comboApps = if (kind == UserShortcutKind.COMBO) vm.comboAppsFor(combo) else emptyList()
        val launchable = if (kind == UserShortcutKind.OPEN_APP) {
            if (appQuery.isBlank()) vm.apps.sortedBy { it.label.lowercase() }.take(16)
            else AppFuzzy.rank(appQuery, vm.apps, limit = 16, name = { it.label })
        } else emptyList()
        val comboRanked = if (kind == UserShortcutKind.COMBO) {
            if (appQuery.isBlank()) comboApps.take(16)
            else AppFuzzy.rank(appQuery, comboApps, limit = 16, name = { it.name })
        } else emptyList()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (kind == UserShortcutKind.OPEN_APP) {
                launchable.forEach { app ->
                    val on = app.packageName == pickedPkg
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (on) c.primary.copy(alpha = 0.2f) else c.bg)
                            .clickable { pickedPkg = app.packageName }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        AppIcon(app.icon, app.label, 22.dp, 6.dp)
                        Text(app.label, color = c.fg, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            } else {
                comboRanked.forEach { app ->
                    val on = app.packageName == pickedPkg
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (on) c.primary.copy(alpha = 0.2f) else c.bg)
                            .clickable { pickedPkg = app.packageName }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        AppIcon(app.icon, app.name, 22.dp, 6.dp)
                        Text(app.name, color = c.fg, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        val savedMsg = stringResource(R.string.action_box_command_added, phrase)
        ActionCta(
            label = stringResource(R.string.action_box_save_command),
            sublabel = phrase,
            color = c.primary,
            enabled = pickedPkg != null,
            onClick = {
                val err = vm.addUserShortcut(
                    phrases = listOf(phrase),
                    packageName = pickedPkg.orEmpty(),
                    kind = kind,
                    combo = if (kind == UserShortcutKind.COMBO) combo else null,
                    contact = null,
                )
                if (err != null) {
                    vm.showToast(err)
                } else {
                    vm.showToast(savedMsg)
                    vm.onQueryChange("")
                }
            },
        )
    }
}