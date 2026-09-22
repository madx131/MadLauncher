package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.ui.theme.LocalMadTheme

/** Legacy settings shell — permissions entry only (preferred apps removed). */
@Composable
fun LauncherSettingsPanel(vm: LauncherViewModel) {
    FloatingWindow(
        title = stringResource(R.string.launcher_settings_title),
        geom = vm.settingsGeom,
        onGeomChange = { vm.saveWindow("settings", it) },
        onClose = { vm.closeSettings() },
        zIndex = 31f,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(R.string.settings_permissions_setup_button),
                color = LocalMadTheme.current.colors.fg,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalMadTheme.current.colors.accent.copy(alpha = 0.16f))
                    .clickable { vm.openSetup() }
                    .padding(12.dp),
            )
            Text(
                stringResource(R.string.settings_home_notif_description),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
