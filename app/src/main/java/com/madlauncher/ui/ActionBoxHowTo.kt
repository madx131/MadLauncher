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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.R
import com.madlauncher.ui.theme.LocalMadTheme

private data class HowToPage(
    val title: Int,
    val body: Int,
    val sample: String,
    val spaceOn: Boolean,
    val enterOn: Boolean,
    val bang: Boolean,
)

@Composable
fun ActionBoxHowToPager(
    onSkip: () -> Unit,
    onDone: () -> Unit,
    compact: Boolean = true,
) {
    val c = LocalMadTheme.current.colors
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        HowToPage(
            R.string.action_box_howto_1_title,
            R.string.action_box_howto_1_body,
            sample = "ig",
            spaceOn = false,
            enterOn = false,
            bang = false,
        ),
        HowToPage(
            R.string.action_box_howto_2_title,
            R.string.action_box_howto_2_body,
            sample = "ig cats",
            spaceOn = true,
            enterOn = false,
            bang = false,
        ),
        HowToPage(
            R.string.action_box_howto_3_title,
            R.string.action_box_howto_3_body,
            sample = "ig hello",
            spaceOn = false,
            enterOn = true,
            bang = false,
        ),
        HowToPage(
            R.string.action_box_howto_4_title,
            R.string.action_box_howto_4_body,
            sample = "!ig cats",
            spaceOn = false,
            enterOn = true,
            bang = true,
        ),
    )
    val item = pages[page]
    val last = page == pages.lastIndex
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.primary.copy(alpha = 0.12f))
            .padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.action_box_howto_title),
            color = c.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        Text(
            stringResource(item.title),
            color = c.fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 15.sp else 17.sp,
        )
        HowToPill(sample = item.sample, bang = item.bang)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HowToKey(
                label = stringResource(R.string.action_box_howto_key_space),
                on = item.spaceOn,
            )
            HowToKey(
                label = stringResource(R.string.action_box_howto_key_enter),
                on = item.enterOn,
            )
        }
        Text(
            stringResource(item.body),
            color = c.fg,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                pages.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (i == page) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == page) c.primary else c.muted.copy(alpha = 0.4f)),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (!last) {
                Text(
                    stringResource(R.string.action_box_howto_skip),
                    color = c.muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(onClick = onSkip)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            Text(
                stringResource(if (last) R.string.action_box_howto_done else R.string.action_box_howto_next),
                color = c.bg,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.primary)
                    .clickable {
                        if (last) onDone() else page++
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun HowToPill(sample: String, bang: Boolean) {
    val c = LocalMadTheme.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, c.primary.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .background(c.primary.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            sample,
            color = if (bang) c.yellow else c.fg,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HowToKey(label: String, on: Boolean) {
    val c = LocalMadTheme.current.colors
    Text(
        label,
        color = if (on) c.bg else c.fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) c.primary else c.secondary)
            .border(1.dp, if (on) c.primary else c.border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
fun ActionBoxHowToPage(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ActionBoxHowToPager(onSkip = onClose, onDone = onClose, compact = false)
        Spacer(Modifier.height(4.dp))
    }
}
