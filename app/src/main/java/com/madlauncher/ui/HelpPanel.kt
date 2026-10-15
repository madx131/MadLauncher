package com.madlauncher.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.theme.LocalMadTheme
import com.madlauncher.ui.theme.MadColors

private data class HelpRowSpec(
    val symbol: String,
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int,
    val tint: (MadColors) -> Color,
)

private val helpRowSpecs = listOf(
    HelpRowSpec("!", R.string.help_app_search_label, R.string.help_app_search_desc) { it.yellow },
    HelpRowSpec("@", R.string.help_filter_at_label, R.string.help_filter_at_desc) { it.primary },
    HelpRowSpec("#", R.string.help_filter_call_label, R.string.help_filter_call_desc) { it.green },
    HelpRowSpec(":", R.string.help_filter_social_label, R.string.help_filter_social_desc) { it.orange },
    HelpRowSpec("*", R.string.help_filter_video_label, R.string.help_filter_video_desc) { it.red },
    HelpRowSpec("?", R.string.help_filter_ai_label, R.string.help_filter_ai_desc) { it.primary },
    HelpRowSpec("msg", R.string.help_at_label, R.string.help_at_desc) { it.primary },
    HelpRowSpec("contact", R.string.help_contact_lookup_label, R.string.help_contact_lookup_desc) { it.primary },
    HelpRowSpec("post", R.string.help_social_label, R.string.help_social_desc) { it.orange },
    HelpRowSpec("video", R.string.help_video_call_label, R.string.help_video_call_desc) { it.red },
    HelpRowSpec("ai", R.string.help_group_label, R.string.help_group_desc) { it.primary },
    HelpRowSpec("call", R.string.help_call_label, R.string.help_call_desc) { it.green },
    HelpRowSpec("note", R.string.help_note_label, R.string.help_note_desc) { it.accent },
    HelpRowSpec("add contact", R.string.help_add_contact_label, R.string.help_add_contact_desc) { it.primary },
    HelpRowSpec("trans", R.string.help_translate_label, R.string.help_translate_desc) { it.blue },
    HelpRowSpec("go", R.string.help_nav_label, R.string.help_nav_desc) { it.accent },
    HelpRowSpec("sticky", R.string.help_sticky_label, R.string.help_sticky_desc) { it.yellow },
    HelpRowSpec("remind", R.string.help_reminder_label, R.string.help_reminder_desc) { it.accent },
    HelpRowSpec("schedule", R.string.help_schedule_label, R.string.help_schedule_desc) { it.purple },
    HelpRowSpec("calc", R.string.help_calc_label, R.string.help_calc_desc) { it.accent },
    HelpRowSpec("gm", R.string.help_common_apps_label, R.string.help_common_apps_desc) { it.primary },
    HelpRowSpec("ls", R.string.help_launcher_settings_label, R.string.help_launcher_settings_desc) { it.primary },
    HelpRowSpec("abs", R.string.help_action_box_settings_label, R.string.help_action_box_settings_desc) { it.primary },
    HelpRowSpec("ss", R.string.help_system_settings_label, R.string.help_system_settings_desc) { it.primary },
    HelpRowSpec("update", R.string.help_update_label, R.string.help_update_desc) { it.accent },
    HelpRowSpec("911", R.string.help_emergency_label, R.string.help_emergency_desc) { it.red },
)

/** Clickable list of Action Box commands for the experimental pill menu. */
@Composable
fun CommandHelpList(
    vm: LauncherViewModel,
    onPick: (String) -> Unit,
) {
    val c = LocalMadTheme.current.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.command_help_menu_hint),
            color = c.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        HelpPickRow(
            symbol = "?",
            color = c.primary,
            title = stringResource(R.string.action_box_howto_title),
            desc = stringResource(R.string.action_box_howto_row_sub),
            onPick = { onPick("howto") },
        )
        vm.userShortcuts.forEach { shortcut ->
            val appLabel = vm.apps.firstOrNull { it.packageName == shortcut.packageName }?.label
                ?: shortcut.packageName
            val symbol = shortcut.phrases.firstOrNull() ?: return@forEach
            HelpPickRow(
                symbol = symbol,
                color = LocalMadTheme.current.colors.yellow,
                title = shortcut.phrases.joinToString(" · "),
                desc = when {
                    shortcut.isCombo && shortcut.combo == com.madlauncher.action.UserShortcutCombo.SOCIAL ->
                        stringResource(R.string.action_box_combo_opens_social, appLabel)
                    shortcut.isCombo && shortcut.contact?.isPresent == true &&
                        shortcut.combo == com.madlauncher.action.UserShortcutCombo.CHAT ->
                        stringResource(
                            R.string.action_box_combo_opens_chat,
                            appLabel,
                            shortcut.contact?.name?.ifBlank { shortcut.contact?.phone }.orEmpty(),
                        )
                    shortcut.isCombo && shortcut.contact?.isPresent == true ->
                        stringResource(
                            R.string.action_box_combo_opens_call,
                            appLabel,
                            shortcut.contact?.name?.ifBlank { shortcut.contact?.phone }.orEmpty(),
                        )
                    shortcut.isCombo -> stringResource(R.string.action_box_combo_pick_contact, appLabel)
                    else -> stringResource(R.string.help_custom_opens, appLabel)
                },
                onPick = onPick,
            )
        }
        HelpPickRow(
            symbol = "+",
            color = LocalMadTheme.current.colors.yellow,
            title = stringResource(R.string.help_add_command_label),
            desc = stringResource(R.string.help_add_command_desc),
            onPick = onPick,
        )
        helpRowSpecs.filter { spec ->
            !(vm.independentActionBox && (spec.symbol == "sticky" || spec.symbol == "ls"))
        }.forEach { spec ->
            HelpPickRow(
                symbol = spec.symbol,
                color = spec.tint(c),
                title = stringResource(spec.labelRes),
                desc = stringResource(spec.descRes),
                onPick = onPick,
            )
        }
    }
}

@Composable
private fun HelpPickRow(
    symbol: String,
    color: Color,
    title: String,
    desc: String,
    onPick: (String) -> Unit,
) {
    val c = LocalMadTheme.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bg)
            .clickable { onPick(symbol) }
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            symbol,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(desc, color = c.muted, fontSize = 11.sp)
        }
    }
}
