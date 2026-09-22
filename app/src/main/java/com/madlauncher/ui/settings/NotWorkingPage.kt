package com.madlauncher.ui.settings

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.madlauncher.R
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.MadShadeWindows
import com.madlauncher.platform.FeatureStatus
import com.madlauncher.platform.PlatformFeature
import com.madlauncher.platform.PlatformHelp
import com.madlauncher.ui.theme.LocalMadTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun NotWorkingPage(vm: LauncherViewModel) {
    val context = LocalContext.current
    val theme = LocalMadTheme.current
    val profile = vm.platformProfile
    var showUploadDialog by rememberSaveable { mutableStateOf(false) }
    var uploadName by rememberSaveable { mutableStateOf("") }
    var uploadEmail by rememberSaveable { mutableStateOf("") }
    var uploadNotes by rememberSaveable { mutableStateOf("") }

    Text(
        stringResource(R.string.platform_device_line, profile.deviceLabel, profile.oemFamily.displayName),
        color = theme.colors.fg,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
    val checked = if (profile.probedAtMillis > 0L) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(profile.probedAtMillis))
    } else {
        stringResource(R.string.platform_never_checked)
    }
    Text(
        stringResource(R.string.platform_last_checked, checked),
        color = theme.colors.muted,
        fontSize = 11.sp,
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !vm.platformProbing) {
                vm.recheckCompatibility { showUploadDialog = true }
            }
            .background(theme.colors.primary.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Refresh, contentDescription = null, tint = theme.colors.primary)
        Text(
            if (vm.platformProbing) stringResource(R.string.platform_checking) else stringResource(R.string.platform_recheck),
            color = theme.colors.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.platform_intro),
        color = theme.colors.muted,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
    Spacer(Modifier.height(6.dp))

    PlatformFeature.entries.filter { feature ->
        (vm.useMadShadeWindows || feature !in MadShadeWindows.experimentalFeatures) &&
            (vm.useNavShortcuts || feature != PlatformFeature.NAV_SHORTCUT_OVERLAY)
    }.forEach { feature ->
        val status = vm.featureStatus(feature)
        FeatureStatusCard(
            title = PlatformHelp.featureTitle(feature),
            status = status,
            explanation = PlatformHelp.explanation(feature, profile.oemFamily, status),
            fixSteps = if (status.isIssue) {
                PlatformHelp.fixSteps(context, feature, profile.oemFamily)
            } else {
                emptyList()
            },
            onFixClick = { intent ->
                runCatching {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            },
            onOpenSetup = { vm.openSetup(); vm.closeSettings() },
        )
    }
    if (showUploadDialog) {
        CompatUploadDialog(
            preview = vm.buildCompatibilityReport(uploadName, uploadEmail, uploadNotes).toPlainText(),
            name = uploadName,
            email = uploadEmail,
            notes = uploadNotes,
            onName = { uploadName = it },
            onEmail = { uploadEmail = it },
            onNotes = { uploadNotes = it },
            onConfirm = {
                showUploadDialog = false
                vm.uploadCompatibilityReport(uploadName, uploadEmail, uploadNotes)
            },
            onCancel = { showUploadDialog = false },
        )
    }
}

@Composable
private fun CompatUploadDialog(
    preview: String,
    name: String,
    email: String,
    notes: String,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onNotes: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val theme = LocalMadTheme.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.colors.card)
                .padding(16.dp)
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.compat_upload_title),
                color = theme.colors.fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                stringResource(R.string.compat_upload_body),
                color = theme.colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            CompatDialogField(stringResource(R.string.compat_name_optional), name, onName)
            CompatDialogField(stringResource(R.string.compat_email_optional), email, onEmail)
            CompatDialogField(stringResource(R.string.compat_notes_hint), notes, onNotes)
            Text(
                stringResource(R.string.compat_upload_preview),
                color = theme.colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                preview.take(1200),
                color = theme.colors.fg,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.colors.bg)
                    .padding(8.dp),
            )
            Text(
                stringResource(R.string.compat_upload_confirm),
                color = theme.colors.bg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.colors.primary)
                    .clickable(onClick = onConfirm)
                    .padding(vertical = 12.dp),
            )
            Text(
                stringResource(R.string.compat_upload_cancel),
                color = theme.colors.fg,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.colors.secondary)
                    .clickable(onClick = onCancel)
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun CompatDialogField(hint: String, value: String, onChange: (String) -> Unit) {
    val theme = LocalMadTheme.current
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(theme.colors.bg)
            .border(1.dp, theme.colors.border, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = androidx.compose.ui.text.TextStyle(color = theme.colors.fg, fontSize = 13.sp),
            cursorBrush = SolidColor(theme.colors.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = theme.colors.muted, fontSize = 13.sp)
                inner()
            },
        )
    }
}

@Composable
private fun FeatureStatusCard(
    title: String,
    status: FeatureStatus,
    explanation: String,
    fixSteps: List<com.madlauncher.platform.PlatformFixStep>,
    onFixClick: (Intent) -> Unit,
    onOpenSetup: () -> Unit,
) {
    val theme = LocalMadTheme.current
    val (chipBg, chipFg) = when (status) {
        FeatureStatus.WORKING -> theme.colors.green.copy(alpha = 0.18f) to theme.colors.green
        FeatureStatus.DEGRADED -> theme.colors.yellow.copy(alpha = 0.18f) to theme.colors.yellow
        FeatureStatus.UNAVAILABLE -> theme.colors.red.copy(alpha = 0.16f) to theme.colors.red
        FeatureStatus.UNKNOWN -> theme.colors.muted.copy(alpha = 0.2f) to theme.colors.muted
        FeatureStatus.NOT_INSTALLED -> theme.colors.muted.copy(alpha = 0.2f) to theme.colors.muted
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.colors.card)
            .border(1.dp, theme.colors.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = theme.colors.fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                PlatformHelp.statusLabel(status),
                color = chipFg,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(chipBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Text(explanation, color = theme.colors.muted, fontSize = 11.sp, lineHeight = 15.sp)
        if (status == FeatureStatus.UNKNOWN) {
            Text(
                stringResource(R.string.platform_open_setup),
                color = theme.colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onOpenSetup),
            )
        }
        fixSteps.forEach { step ->
            if (step.intent != null) {
                Text(
                    step.label,
                    color = theme.colors.primary,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { onFixClick(step.intent) },
                )
            }
        }
    }
}

@Composable
fun PlatformPartialSyncBanner(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    if (!vm.useMadShadeWindows) return
    val status = vm.featureStatus(PlatformFeature.NOTIF_SHADE_MIRROR)
    if (status == FeatureStatus.WORKING) return
    val theme = LocalMadTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(theme.colors.yellow.copy(alpha = 0.12f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stringResource(R.string.platform_partial_sync_title),
            color = theme.colors.yellow,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.platform_partial_sync_body),
            color = theme.colors.muted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
        if (vm.isFeatureEnabled(PlatformFeature.SYSTEM_SHADE_EXPAND)) {
            Text(
                stringResource(R.string.platform_open_system_shade),
                color = theme.colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { vm.openSystemShade() },
            )
        }
        Text(
            stringResource(R.string.platform_view_not_working),
            color = theme.colors.muted,
            fontSize = 10.sp,
            modifier = Modifier.clickable { vm.openSettingsRoute(SettingsRoute.NOT_WORKING) },
        )
    }
}
