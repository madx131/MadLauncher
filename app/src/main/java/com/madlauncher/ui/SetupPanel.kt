package com.madlauncher.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madlauncher.LauncherCommands
import com.madlauncher.LauncherWindowDismiss
import com.madlauncher.R
import com.madlauncher.data.GrantState
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.PermissionChecker
import com.madlauncher.platform.PlatformHelp
import com.madlauncher.platform.PlatformFeature
import com.madlauncher.ui.theme.LocalMadTheme

private data class SetupRow(
    val id: String,
    val title: String,
    val why: String,
    val button: String,
    val icon: ImageVector,
    val color: Color,
    val required: Boolean,
    val granted: (GrantState) -> Boolean,
)

@Composable
fun SetupPanel(vm: LauncherViewModel) {
    val context = LocalContext.current
    val grants = vm.grants
    val contactsLaunch = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.refreshGrants() }
    val notifLaunch = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.refreshGrants() }
    val locationLaunch = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.refreshGrants() }
    val smsLaunch = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.refreshGrants() }

    val rows = buildList {
        addAll(listOf(
        SetupRow(
            "home",
            stringResource(R.string.setup_row_home_title),
            stringResource(R.string.setup_row_home_why),
            stringResource(R.string.setup_row_home_button),
            Icons.Outlined.Home,
            LocalMadTheme.current.colors.green,
            true,
            { it.defaultHome },
        ),
        SetupRow(
            "listener",
            stringResource(R.string.setup_row_listener_title),
            stringResource(R.string.setup_row_listener_why),
            stringResource(R.string.setup_row_listener_button),
            Icons.Outlined.Notifications,
            LocalMadTheme.current.colors.yellow,
            true,
            { it.notificationListener },
        ),
        SetupRow(
            "contacts",
            stringResource(R.string.setup_row_contacts_title),
            stringResource(R.string.setup_row_contacts_why),
            stringResource(R.string.setup_row_contacts_button),
            Icons.Outlined.Contacts,
            LocalMadTheme.current.colors.accent,
            true,
            { it.contacts },
        ),
        SetupRow(
            "post",
            stringResource(R.string.setup_row_post_title),
            stringResource(R.string.setup_row_post_why),
            stringResource(R.string.setup_row_post_button),
            Icons.Outlined.NotificationsActive,
            LocalMadTheme.current.colors.yellow,
            true,
            { it.postNotifications },
        ),
        SetupRow(
            "battery",
            stringResource(R.string.setup_row_battery_title),
            stringResource(R.string.setup_row_battery_why),
            stringResource(R.string.setup_row_battery_button),
            Icons.Outlined.BatteryAlert,
            LocalMadTheme.current.colors.red,
            true,
            { it.battery },
        ),
        SetupRow(
            "usage",
            stringResource(R.string.setup_row_usage_title),
            stringResource(R.string.setup_row_usage_why),
            stringResource(R.string.setup_row_usage_button),
            Icons.Outlined.History,
            LocalMadTheme.current.colors.primary,
            false,
            { it.usageStats },
        ),
        SetupRow(
            "location",
            stringResource(R.string.setup_row_location_title),
            stringResource(R.string.setup_row_location_why),
            stringResource(R.string.setup_row_location_button),
            Icons.Outlined.LocationOn,
            LocalMadTheme.current.colors.primary,
            false,
            { it.location },
        ),
        SetupRow(
            "read_sms",
            stringResource(R.string.setup_row_read_sms_title),
            stringResource(R.string.setup_row_read_sms_why),
            stringResource(R.string.setup_row_read_sms_button),
            Icons.Outlined.Sms,
            LocalMadTheme.current.colors.accent,
            false,
            { it.readSms },
        ),
        ))
        // Overlay / Accessibility only after experimental features that need them are on.
        val needOverlay = vm.useNavShortcuts || vm.useMadShadeWindows
        val needA11y = vm.useNavShortcuts && !com.madlauncher.platform.MadOsRuntime.isMadOs(context)
        if (needOverlay) {
            add(
                SetupRow(
                    "overlay",
                    stringResource(R.string.setup_row_overlay_title),
                    stringResource(R.string.setup_row_overlay_why),
                    stringResource(R.string.setup_row_overlay_button),
                    Icons.Outlined.Layers,
                    LocalMadTheme.current.colors.primary,
                    false,
                    { it.overlay },
                ),
            )
        }
        if (needA11y) {
            add(
                SetupRow(
                    "a11y",
                    stringResource(R.string.setup_row_a11y_title),
                    stringResource(R.string.setup_row_a11y_why),
                    stringResource(R.string.setup_row_a11y_button),
                    Icons.Outlined.AccessibilityNew,
                    LocalMadTheme.current.colors.purple,
                    false,
                    { it.accessibility },
                ),
            )
        }
        if (vm.isXiaomiPlatform() && !com.madlauncher.platform.MadOsRuntime.isMadOs(context)) {
            add(
                SetupRow(
                    "xiaomi_autostart",
                    stringResource(R.string.setup_row_xiaomi_autostart_title),
                    stringResource(R.string.setup_row_xiaomi_autostart_why),
                    stringResource(R.string.setup_row_xiaomi_autostart_button),
                    Icons.Outlined.NotificationsActive,
                    LocalMadTheme.current.colors.yellow,
                    false,
                    { false },
                ),
            )
            add(
                SetupRow(
                    "xiaomi_battery",
                    stringResource(R.string.setup_row_xiaomi_battery_title),
                    stringResource(R.string.setup_row_xiaomi_battery_why),
                    stringResource(R.string.setup_row_xiaomi_battery_button),
                    Icons.Outlined.BatteryAlert,
                    LocalMadTheme.current.colors.red,
                    false,
                    { grants.battery },
                ),
            )
        }
        if (vm.independentActionBox) {
            removeAll {
                it.id in setOf(
                    "home",
                    "listener",
                    "post",
                    "battery",
                    "usage",
                    "location",
                    "read_sms",
                    "overlay",
                    "a11y",
                    "xiaomi_autostart",
                    "xiaomi_battery",
                )
            }
        }
    }

    val required = rows.filter { it.required }
    val doneCount = required.count { it.granted(grants) }

    LaunchedEffect(grants.defaultHome, grants.notificationListener, grants.overlay, grants.accessibility, grants.battery) {
        vm.refreshPlatformProfile()
    }

    FloatingWindow(
        title = stringResource(R.string.setup_title),
        geom = vm.setupGeom,
        onGeomChange = { vm.saveWindow("setup", it) },
        onClose = { vm.closeSetup() },
        zIndex = 40f,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.setup_description),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LocalMadTheme.current.colors.border),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (required.isEmpty()) 0f else doneCount / required.size.toFloat())
                            .height(6.dp)
                            .background(LocalMadTheme.current.colors.primary),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("$doneCount/${required.size}", color = LocalMadTheme.current.colors.muted, fontSize = 11.sp)
            }
            rows.forEach { row ->
                val done = row.granted(grants)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalMadTheme.current.colors.bg)
                        .border(
                            1.dp,
                            if (done) row.color.copy(alpha = 0.45f) else LocalMadTheme.current.colors.border,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            LauncherWindowDismiss.preserve(30_000L)
                            LauncherCommands.suppressHomeBriefly(12_000L)
                            val launch = stepIntent(row.id)
                            when (row.id) {
                                "contacts" -> contactsLaunch.launch(Manifest.permission.READ_CONTACTS)
                                "post" -> if (Build.VERSION.SDK_INT >= 33) {
                                    notifLaunch.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                "location" -> locationLaunch.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                "read_sms" -> smsLaunch.launch(Manifest.permission.READ_SMS)
                                "xiaomi_autostart" -> runCatching {
                                    PlatformHelp.fixSteps(context, PlatformFeature.NOTIF_SHADE_MIRROR, vm.platformProfile.oemFamily)
                                        .firstOrNull { it.label.contains("Autostart", ignoreCase = true) }
                                        ?.intent
                                        ?.let { startSetupIntent(context, it) }
                                }
                                else -> runCatching {
                                    startSetupIntent(context, launch)
                                }
                            }
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(row.color.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(row.icon, contentDescription = null, tint = row.color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.title, color = LocalMadTheme.current.colors.fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(row.why, color = LocalMadTheme.current.colors.muted, fontSize = 11.sp, lineHeight = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (done) stringResource(R.string.granted_tap_to_open) else row.button,
                            color = if (done) LocalMadTheme.current.colors.green else row.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (!grants.requiredDone) {
                Text(
                    stringResource(R.string.setup_skip),
                    color = LocalMadTheme.current.colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { vm.skipSetup() }
                        .padding(8.dp),
                )
            }
            SetupCompatibilitySection(vm, grants.requiredDone)
            Text(
                stringResource(R.string.setup_reopen_hint),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private fun startSetupIntent(context: android.content.Context, intent: Intent) {
    var ctx: android.content.Context? = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) {
            ctx.startActivity(intent)
            return
        }
        ctx = ctx.baseContext
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun stepIntent(id: String): Intent {
    val ctx = com.madlauncher.MadApp.instance
    return when (id) {
        "home" -> Intent(Settings.ACTION_HOME_SETTINGS)
        "listener" -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        "overlay" -> PermissionChecker.overlaySettings(ctx)
        "a11y" -> PermissionChecker.accessibilitySettings(ctx)
        "battery" -> PermissionChecker.batteryRequest(ctx)
        "usage" -> PermissionChecker.usageAccessSettings()
        "xiaomi_battery" -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${ctx.packageName}"),
        )
        else -> Intent(Settings.ACTION_SETTINGS)
    }
}
