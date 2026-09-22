package com.madlauncher.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.madlauncher.LauncherWindowDismiss
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.PermissionChecker

/**
 * Action Box only needs contacts + phone for @ / # / * commands.
 * Shade, SMS, overlay, and accessibility belong to the launcher.
 */
@Composable
fun ActionBoxIndependentGrants(vm: LauncherViewModel) {
    val context = LocalContext.current
    val grants = vm.grants
    val runtimeLaunch = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        vm.overlayGrantInFlight = false
        vm.refreshGrants()
    }

    LaunchedEffect(Unit) {
        val missing = buildList {
            if (!grants.contacts) add(Manifest.permission.READ_CONTACTS)
            if (!PermissionChecker.granted(context, Manifest.permission.CALL_PHONE)) {
                add(Manifest.permission.CALL_PHONE)
            }
        }
        if (missing.isNotEmpty()) {
            vm.overlayGrantInFlight = true
            LauncherWindowDismiss.preserve(30_000)
            runtimeLaunch.launch(missing.toTypedArray())
        }
    }
}
