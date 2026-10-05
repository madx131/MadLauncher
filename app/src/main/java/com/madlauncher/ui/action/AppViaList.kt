package com.madlauncher.ui.action

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.action.InstalledActionApp
import com.madlauncher.ui.AppIcon
import com.madlauncher.ui.theme.LocalMadTheme

@Composable
fun AppViaList(
    title: String,
    apps: List<InstalledActionApp>,
    defaultPackage: String? = null,
    onLongPress: ((InstalledActionApp) -> Unit)? = null,
    onSelect: (InstalledActionApp) -> Unit,
) {
    val c = LocalMadTheme.current.colors
    val filterHint = if (onLongPress != null) stringResource(R.string.action_box_filter_hint) else null
    val defaultHint = stringResource(R.string.action_default_app)
    fun tileHint(app: InstalledActionApp): String? = filterHint
        ?: if (app.packageName == defaultPackage) defaultHint else null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            color = c.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        if (apps.isEmpty()) {
            Text(stringResource(R.string.no_matching_apps), color = c.muted, fontSize = 13.sp)
        } else if (apps.size <= 4) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                apps.forEach { app ->
                    AppViaTile(
                        app = app,
                        hint = tileHint(app),
                        onSelect = { onSelect(app) },
                        onLongPress = onLongPress?.let { handler -> { handler(app) } },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppViaTile(
                        app = app,
                        hint = tileHint(app),
                        onSelect = { onSelect(app) },
                        onLongPress = onLongPress?.let { handler -> { handler(app) } },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppViaTile(
    app: InstalledActionApp,
    hint: String? = null,
    onSelect: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val resolvedHint = hint ?: stringResource(R.string.action_tap_or_drag_select)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(app.color.copy(alpha = 0.12f))
            .border(1.dp, app.color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .then(
                if (onLongPress != null) {
                    Modifier.combinedClickable(onClick = onSelect, onLongClick = onLongPress)
                } else {
                    Modifier.clickable(onClick = onSelect)
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(drawable = app.icon, contentDescription = app.name, size = 28.dp, corner = 8.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(app.name, color = LocalMadTheme.current.colors.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(resolvedHint, color = LocalMadTheme.current.colors.muted, fontSize = 11.sp)
        }
    }
}

@Composable
fun ActionCta(
    label: String,
    sublabel: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) color.copy(alpha = 0.18f) else LocalMadTheme.current.colors.secondary)
            .border(1.dp, color.copy(alpha = if (enabled) 0.45f else 0.15f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .focusProperties { canFocus = false }
            .padding(14.dp),
    ) {
        Text(label, color = if (enabled) color else LocalMadTheme.current.colors.muted, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(2.dp))
        Text(sublabel, color = LocalMadTheme.current.colors.muted, fontSize = 12.sp)
    }
}
