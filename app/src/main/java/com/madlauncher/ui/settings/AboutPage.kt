package com.madlauncher.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.BuildConfig
import com.madlauncher.R
import com.madlauncher.about.OssCredit
import com.madlauncher.about.OssCredits
import com.madlauncher.ui.theme.LocalMadTheme

@Composable
fun AboutPage() {
    val context = LocalContext.current
    val kind = if (BuildConfig.ACTION_BOX_APP) {
        stringResource(R.string.about_build_kind_actionbox)
    } else {
        stringResource(R.string.about_build_kind_launcher)
    }
    val channel = if (BuildConfig.DEBUG) {
        stringResource(R.string.about_build_debug)
    } else {
        stringResource(R.string.about_build_release)
    }
    SectionLabel(stringResource(R.string.about_build_heading))
    Text(
        stringResource(
            R.string.about_build_body,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildConfig.APPLICATION_ID,
            kind,
            channel,
        ),
        color = LocalMadTheme.current.colors.fg,
        fontSize = 14.sp,
    )
    Text(
        stringResource(R.string.about_build_explain),
        color = LocalMadTheme.current.colors.muted,
        fontSize = 13.sp,
    )
    SectionLabel(stringResource(R.string.about_ddg_heading))
    Text(
        stringResource(R.string.about_ddg_body),
        color = LocalMadTheme.current.colors.muted,
        fontSize = 13.sp,
    )
    AboutLink(stringResource(R.string.about_ddg_play)) {
        openAboutLink(context, OssCredits.DUCKDUCKGO_PLAY)
    }
    AboutLink(stringResource(R.string.about_ddg_store)) {
        openAboutLink(context, OssCredits.DUCKDUCKGO_MARKET)
    }
    SectionLabel(stringResource(R.string.about_credits_heading))
    Text(
        stringResource(R.string.about_credits_body),
        color = LocalMadTheme.current.colors.muted,
        fontSize = 13.sp,
    )
    OssCredits.all.forEach { credit ->
        CreditBlock(credit) { openAboutLink(context, it) }
    }
}

@Composable
private fun CreditBlock(credit: OssCredit, onLink: (String) -> Unit) {
    val c = LocalMadTheme.current.colors
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(credit.name, color = c.fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(credit.role, color = c.muted, fontSize = 12.sp)
        Text(
            stringResource(R.string.about_credit_license, credit.license),
            color = c.muted,
            fontSize = 11.sp,
        )
        Text(
            credit.author,
            color = c.primary,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onLink(credit.authorUrl) }
                .padding(vertical = 2.dp),
        )
        Text(
            credit.projectUrl.removePrefix("https://"),
            color = c.primary,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onLink(credit.projectUrl) }
                .padding(vertical = 2.dp),
        )
    }
}

@Composable
private fun AboutLink(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = LocalMadTheme.current.colors.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

private fun openAboutLink(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
