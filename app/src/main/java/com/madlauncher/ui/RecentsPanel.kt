package com.madlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.data.LaunchableApp
import com.madlauncher.ui.theme.LocalMadTheme

@Composable
fun RecentsPanel(
    apps: List<LaunchableApp>,
    onLaunch: (LaunchableApp) -> Unit,
    onClose: () -> Unit,
    showUsageAccessHint: Boolean = false,
    onUsageAccess: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalMadTheme.current.colors.bg.copy(alpha = 0.55f))
            .clickable(onClick = onClose),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LocalMadTheme.current.colors.card)
                .clickable(enabled = false) {}
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recents_title), color = LocalMadTheme.current.colors.fg, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.recents_close), tint = LocalMadTheme.current.colors.fg)
                }
            }
            if (showUsageAccessHint) {
                Text(
                    stringResource(R.string.recents_usage_access_hint),
                    color = LocalMadTheme.current.colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onUsageAccess)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            if (apps.isEmpty()) {
                Text(stringResource(R.string.recents_empty), color = LocalMadTheme.current.colors.muted, modifier = Modifier.padding(vertical = 20.dp))
            } else {
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(apps, key = { it.id }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onLaunch(app) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(app.icon, app.label, 36.dp, 10.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(app.label, color = LocalMadTheme.current.colors.fg, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
