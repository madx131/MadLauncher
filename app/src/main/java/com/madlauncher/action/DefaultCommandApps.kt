package com.madlauncher.action

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager

/** System default handler for each Action Box command, plus picker ranking. */
object DefaultCommandApps {
    fun packageFor(context: Context, kind: ActionAppKind): String? {
        return when (kind) {
            ActionAppKind.Messaging -> defaultSms(context)
            ActionAppKind.Email -> resolveDefault(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")))
            ActionAppKind.VoiceCall, ActionAppKind.VideoCall -> defaultDialer(context)
            ActionAppKind.Social -> defaultSocial(context)
            ActionAppKind.Nav -> resolveDefault(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=test")),
            )
            ActionAppKind.Notes, ActionAppKind.Translator, ActionAppKind.Ai -> null
            ActionAppKind.Contacts -> firstReliableHandler(
                context,
                Intent(Intent.ACTION_INSERT).setType("vnd.android.cursor.dir/contact"),
            ) { ActionApps.isReliableContactInsert(it) }
        }
    }

    /** Auto-commit `/a` — never Eyecon / caller-ID, even if they are the INSERT default. */
    fun pickAddContactTarget(
        apps: List<InstalledActionApp>,
        systemDefault: String?,
    ): InstalledActionApp? {
        val allowed = apps.filter { ActionApps.isReliableContactInsert(it.packageName) }
        val preferred = systemDefault?.takeIf { ActionApps.isReliableContactInsert(it) }
        return allowed.firstOrNull { it.packageName == preferred } ?: allowed.singleOrNull()
    }

    /**
     * Hidden packages are omitted (including a hidden system default).
     * [order] is the user list from ActionBox Settings; [pinDefault] keeps the
     * system default first in pickers even if it was dragged elsewhere in the editor.
     */
    fun arrange(
        apps: List<InstalledActionApp>,
        defaultPackage: String?,
        preferredPackage: String?,
        hidden: Set<String>,
        order: List<String> = emptyList(),
        pinDefault: Boolean = true,
    ): List<InstalledActionApp> {
        val byPkg = LinkedHashMap<String, InstalledActionApp>()
        apps.forEach { byPkg.putIfAbsent(it.packageName, it) }
        val out = ArrayList<InstalledActionApp>(byPkg.size)
        val seen = linkedSetOf<String>()
        fun take(pkg: String?) {
            if (pkg.isNullOrBlank() || pkg in hidden || !seen.add(pkg)) return
            byPkg[pkg]?.let { out.add(it) }
        }
        if (pinDefault) take(defaultPackage)
        order.forEach { take(it) }
        take(preferredPackage)
        byPkg.values.forEach { take(it.packageName) }
        return out
    }

    private fun defaultSms(context: Context): String? {
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        if (Build.VERSION.SDK_INT >= 29) {
            roleHolder(context, RoleManager.ROLE_SMS)?.let { return it }
        }
        return null
    }

    private fun defaultDialer(context: Context): String? {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        telecom?.defaultDialerPackage?.takeIf { it.isNotBlank() }?.let { return it }
        if (Build.VERSION.SDK_INT >= 29) {
            roleHolder(context, RoleManager.ROLE_DIALER)?.let { return it }
        }
        return null
    }

    private fun defaultSocial(context: Context): String? {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
        val pkg = resolveDefault(context, send) ?: return null
        val label = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault("")
        return pkg.takeIf { ActionApps.looksLikeSocial(label, it) }
    }

    private fun roleHolder(context: Context, role: String): String? {
        if (Build.VERSION.SDK_INT < 29) return null
        val rm = context.getSystemService(RoleManager::class.java) ?: return null
        // getRoleHolders is @SystemApi — reflect like MessagingAppRegistry.
        return runCatching {
            val method = RoleManager::class.java.getMethod("getRoleHolders", String::class.java)
            @Suppress("UNCHECKED_CAST")
            val holders = method.invoke(rm, role) as? List<String>
            holders?.firstOrNull()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun resolveDefault(context: Context, intent: Intent): String? {
        val pm = context.packageManager
        val ri = runCatching {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull() ?: return null
        val pkg = ri.activityInfo?.packageName ?: return null
        if (pkg == context.packageName || pkg == "android") return null
        val cls = ri.activityInfo?.name.orEmpty()
        if (cls.contains("ResolverActivity", ignoreCase = true) ||
            cls.contains("ChooserActivity", ignoreCase = true)
        ) {
            return null
        }
        return pkg
    }

    /** [resolveActivity] returns the system chooser when Eyecon also handles INSERT. */
    private fun firstReliableHandler(
        context: Context,
        intent: Intent,
        ok: (String) -> Boolean,
    ): String? {
        val pm = context.packageManager
        val resolves = runCatching {
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrDefault(emptyList())
        for (ri in resolves) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || pkg == "android") continue
            val cls = ri.activityInfo?.name.orEmpty()
            if (cls.contains("ResolverActivity", ignoreCase = true) ||
                cls.contains("ChooserActivity", ignoreCase = true)
            ) {
                continue
            }
            if (ok(pkg)) return pkg
        }
        return null
    }
}
