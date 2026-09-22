package com.madlauncher.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataSaverOff
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.ui.graphics.vector.ImageVector

fun qsIcon(id: String): ImageVector {
    return when (id) {
        "wifi" -> Icons.Outlined.Wifi
        "bt" -> Icons.Outlined.Bluetooth
        "cell" -> Icons.Outlined.SignalCellularAlt
        "share" -> Icons.Outlined.Share
        "dnd" -> Icons.Outlined.DoNotDisturbOn
        "air" -> Icons.Outlined.AirplanemodeActive
        "torch" -> Icons.Outlined.FlashlightOn
        "rotate" -> Icons.Outlined.ScreenRotation
        "hotspot" -> Icons.Outlined.WifiTethering
        "location" -> Icons.Outlined.LocationOn
        "battery" -> Icons.Outlined.BatterySaver
        "dark" -> Icons.Outlined.DarkMode
        "night" -> Icons.Outlined.Nightlight
        "volume", "sound" -> Icons.AutoMirrored.Outlined.VolumeUp
        "brightness" -> Icons.Outlined.Brightness6
        "display" -> Icons.Outlined.DisplaySettings
        "nfc" -> Icons.Outlined.Nfc
        "cast" -> Icons.Outlined.Cast
        "vpn" -> Icons.Outlined.VpnKey
        "data_saver" -> Icons.Outlined.DataSaverOff
        "invert" -> Icons.Outlined.InvertColors
        "bedtime" -> Icons.Outlined.Bedtime
        "wallet" -> Icons.Outlined.AccountBalanceWallet
        else -> Icons.Outlined.Settings
    }
}
