package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.platform.FeatureStatus
import com.madlauncher.platform.PlatformFeature
import com.madlauncher.platform.PlatformHelp
import com.madlauncher.ui.theme.LocalMadTheme

@Composable
fun SetupCompatibilitySection(
    vm: LauncherViewModel,
    requiredDone: Boolean,
) {
    if (!requiredDone) {
        Text(
            stringResource(R.string.compat_selftest_waiting),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        return
    }

    val profile = vm.platformProfile
    val device = vm.deviceOsInfo
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var submitting by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.compat_selftest_title),
            color = LocalMadTheme.current.colors.fg,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.compat_selftest_subtitle),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LocalMadTheme.current.colors.bg)
                .border(1.dp, LocalMadTheme.current.colors.border, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            InfoLine("Phone", "${device.manufacturer} ${device.model}")
            InfoLine("Android", "${device.androidRelease} (API ${device.androidSdk})")
            InfoLine("Native OS", "${device.nativeOsName} ${device.nativeOsVersion}")
            InfoLine("ROM", profile.oemFamily.displayName)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.compat_feature_results),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )
        PlatformFeature.entries.forEach { feature ->
            val status = vm.featureStatus(feature)
            val (fg, label) = when (status) {
                FeatureStatus.WORKING -> LocalMadTheme.current.colors.green to PlatformHelp.statusLabel(status)
                FeatureStatus.DEGRADED -> LocalMadTheme.current.colors.yellow to PlatformHelp.statusLabel(status)
                FeatureStatus.UNAVAILABLE -> LocalMadTheme.current.colors.red to PlatformHelp.statusLabel(status)
                FeatureStatus.UNKNOWN -> LocalMadTheme.current.colors.muted to PlatformHelp.statusLabel(status)
                FeatureStatus.NOT_INSTALLED -> LocalMadTheme.current.colors.muted to PlatformHelp.statusLabel(status)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    PlatformHelp.featureTitle(feature),
                    color = LocalMadTheme.current.colors.fg,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))
        CompatField(stringResource(R.string.compat_name_optional), name) { name = it }
        CompatField(stringResource(R.string.compat_email_optional), email) { email = it }
        CompatField(stringResource(R.string.compat_notes_hint), notes) { notes = it }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                if (vm.platformProbing) stringResource(R.string.platform_checking)
                else stringResource(R.string.platform_recheck),
                color = LocalMadTheme.current.colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !vm.platformProbing) {
                        vm.recheckCompatibility()
                    }
                    .padding(8.dp),
            )
        }

        val submitLabel = if (vm.compatibilityReportSubmitted) {
            stringResource(R.string.compat_submit_again)
        } else {
            stringResource(R.string.compat_submit_report)
        }
        Text(
            submitLabel,
            color = LocalMadTheme.current.colors.accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LocalMadTheme.current.colors.accent.copy(alpha = 0.14f))
                .clickable(enabled = !submitting) {
                    submitting = true
                    vm.submitCompatibilityReport(name, email, notes) { submitting = false }
                }
                .padding(12.dp),
        )
        if (!vm.compatibilityReportSubmitted) {
            Text(
                stringResource(R.string.compat_skip),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable { vm.skipCompatibilityReport() }
                    .padding(6.dp),
            )
        }
        Text(
            stringResource(R.string.compat_github_hint),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ", color = LocalMadTheme.current.colors.muted, fontSize = 11.sp)
        Text(value, color = LocalMadTheme.current.colors.fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CompatField(hint: String, value: String, onChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LocalMadTheme.current.colors.bg)
            .border(1.dp, LocalMadTheme.current.colors.border, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = androidx.compose.ui.text.TextStyle(color = LocalMadTheme.current.colors.fg, fontSize = 13.sp),
            cursorBrush = SolidColor(LocalMadTheme.current.colors.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = LocalMadTheme.current.colors.muted, fontSize = 13.sp)
                inner()
            },
        )
    }
}
