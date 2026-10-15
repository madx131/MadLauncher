package com.madlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.ui.theme.LocalMadTheme

/** Glance-style side-by-side option chips in pairs. */
@Composable
fun <T> SettingsStyleRow(options: List<Pair<T, Int>>, selected: T, onPick: (T) -> Unit) {
    val c = LocalMadTheme.current.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        options.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { (value, label) ->
                    val on = value == selected
                    Text(
                        stringResource(label),
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
                if (row.size == 1) {
                    // Keep single trailing chip from stretching full width awkwardly.
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
