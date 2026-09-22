package com.madlauncher.notif

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import com.madlauncher.action.ActionAppKind
import com.madlauncher.action.ActionApps

/**
 * All installed messaging / SMS handler packages — used for `@`, badges, sound, and inbox fallback.
 */
object MessagingAppRegistry {

    @Volatile
    var smsPackages: Set<String> = emptySet()
        private set

    @Volatile
    var atPackages: Set<String> = emptySet()
        private set

    fun refresh(context: Context, actionApps: ActionApps) {
        smsPackages = actionApps.discoverSmsPackages()
        atPackages = buildSet {
            addAll(actionApps.installed(ActionAppKind.Messaging).map { it.packageName })
            addAll(actionApps.installed(ActionAppKind.Email).map { it.packageName })
            addAll(smsPackages)
        }
        NotificationHub.setAtPackages(atPackages)
    }

    fun isSmsPackage(packageName: String): Boolean = packageName in smsPackages

    fun defaultSmsPackage(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleSmsHolder(rm)?.let { return it }
            }
        }
        @Suppress("DEPRECATION")
        return Telephony.Sms.getDefaultSmsPackage(context)?.takeIf { it.isNotBlank() }
    }

    /** RoleManager.getRoleHolders is @SystemApi — reflect when Telephony fallback is empty. */
    private fun roleSmsHolder(rm: RoleManager): String? {
        return runCatching {
            @Suppress("DiscouragedPrivateApi")
            val method = RoleManager::class.java.getMethod("getRoleHolders", String::class.java)
            @Suppress("UNCHECKED_CAST")
            val holders = method.invoke(rm, RoleManager.ROLE_SMS) as? List<String>
            holders?.firstOrNull()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun querySmsHandlerPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("sms:")),
            Intent("android.intent.action.RESPOND_VIA_MESSAGE", Uri.parse("smsto:")),
        )
        val found = mutableSetOf<String>()
        for (intent in intents) {
            val resolves = runCatching {
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }.getOrDefault(emptyList())
            for (ri in resolves) {
                ri.activityInfo?.packageName?.takeIf { it.isNotBlank() }?.let { found.add(it) }
            }
        }
        return found.filter { pkg ->
            !ActionApps.isCallerIdPackage(pkg) &&
                runCatching { pm.getApplicationInfo(pkg, 0).enabled }.getOrDefault(false)
        }.toSet()
    }
}
