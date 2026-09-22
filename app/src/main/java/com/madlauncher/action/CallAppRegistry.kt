package com.madlauncher.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.graphics.Color

enum class CallAppMode { Voice, Video }

/**
 * Call-capable apps for `#` / `*`.
 * Uses the same messaging catalog as `@` (+ Phone) — not generic ACTION_DIAL discovery.
 */
object CallAppRegistry {
    data class CallCandidate(
        val name: String,
        val packages: List<String>,
        val color: Long,
        val voice: Boolean = true,
        val video: Boolean = false,
        /** Shares conversation history with `@` recipient sync. */
        val sharesMessageHistory: Boolean = false,
        /** `#` / `*` picker — only Phone and WhatsApp have reliable call APIs today. */
        val supportedForCall: Boolean = false,
    )

    fun catalogNames(mode: CallAppMode): List<String> =
        supportedCatalog(mode).map { it.name }

    fun unsupportedCatalogNames(mode: CallAppMode): List<String> =
        catalog.filter { c ->
            !c.supportedForCall && (if (mode == CallAppMode.Video) c.video else c.voice)
        }.map { it.name }

    fun isSupportedCallPackage(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        if (ContactReach.isDialerPackage(pkg)) return true
        return catalog.any { c ->
            c.supportedForCall && c.packages.any { it.equals(packageName, ignoreCase = true) }
        }
    }

    private fun supportedCatalog(mode: CallAppMode): List<CallCandidate> =
        catalog.filter { c ->
            c.supportedForCall && (if (mode == CallAppMode.Video) c.video else c.voice)
        }

    /** Packages whose notification threads appear in @ / # / * recipient lists. */
    fun historyPackages(): Set<String> = buildSet {
        catalog.filter { it.sharesMessageHistory || it.voice }.forEach { c ->
            c.packages.forEach { add(it) }
        }
    }

    fun isHistoryPackage(packageName: String): Boolean =
        packageName in historyPackages() || ContactReach.isDialerPackage(packageName)

    private val catalog = listOf(
        CallCandidate(
            "Phone",
            listOf(
                "com.google.android.dialer",
                "com.android.dialer",
                "com.samsung.android.dialer",
                "com.sh.smart.caller",
            ),
            0xFF81A1C1,
            voice = true,
            video = true,
            sharesMessageHistory = false,
            supportedForCall = true,
        ),
        CallCandidate(
            "WhatsApp",
            listOf("com.whatsapp", "com.whatsapp.w4b"),
            0xFF25D366,
            voice = true,
            video = true,
            sharesMessageHistory = true,
            supportedForCall = true,
        ),
        CallCandidate(
            "Messenger",
            listOf("com.facebook.orca"),
            0xFF0084FF,
            voice = true,
            video = true,
            sharesMessageHistory = true,
        ),
        CallCandidate(
            "Telegram",
            listOf("org.telegram.messenger", "org.telegram.messenger.web"),
            0xFF29B6F6,
            voice = true,
            video = true,
            sharesMessageHistory = true,
        ),
        CallCandidate(
            "Signal",
            listOf("org.thoughtcrime.securesms"),
            0xFF3A76F0,
            voice = true,
            video = true,
            sharesMessageHistory = true,
        ),
        CallCandidate(
            "Messages",
            listOf(
                "com.google.android.apps.messaging",
                "com.android.mms",
                "com.samsung.android.messaging",
            ),
            0xFF3DDC84,
            voice = true,
            video = false,
            sharesMessageHistory = true,
        ),
        CallCandidate("Google Meet", listOf("com.google.android.apps.meetings", "com.google.android.apps.tachyon"), 0xFF00897B, voice = true, video = true),
        CallCandidate("Skype", listOf("com.skype.raider"), 0xFF00AFF0, voice = true, video = true),
        CallCandidate("Viber", listOf("com.viber.voip"), 0xFF7360F2, voice = true, video = true),
        CallCandidate("Discord", listOf("com.discord"), 0xFF5865F2, voice = true, video = true, sharesMessageHistory = true),
        CallCandidate("LINE", listOf("jp.naver.line.android"), 0xFF00C300, voice = true, video = true),
        CallCandidate("Snapchat", listOf("com.snapchat.android"), 0xFFFFFC00, voice = true, video = true),
        CallCandidate(
            "Instagram",
            listOf("com.instagram.android"),
            0xFFE1306C,
            voice = true,
            video = true,
            sharesMessageHistory = true,
        ),
        CallCandidate("Zoom", listOf("us.zoom.videomeetings"), 0xFF2D8CFF, voice = true, video = true),
        CallCandidate("Teams", listOf("com.microsoft.teams"), 0xFF6264A7, voice = true, video = true),
    )

    fun discover(
        context: Context,
        mode: CallAppMode,
        filter: String = "",
        @Suppress("UNUSED_PARAMETER") messagingApps: List<InstalledActionApp> = emptyList(),
    ): List<InstalledActionApp> {
        val fromCatalog = supportedCatalog(mode)
            .mapNotNull { resolve(context.packageManager, it) }
            .toMutableList()
        val seen = fromCatalog.map { it.packageName }.toMutableSet()

        // Stock dialer only — never merge every ACTION_DIAL handler (browser, etc.).
        discoverDialer(context.packageManager, context.packageName, seen, fromCatalog)

        val f = filter.trim().lowercase()
        val filtered = if (f.isEmpty()) fromCatalog
        else fromCatalog.filter {
            it.name.lowercase().contains(f) || it.packageName.lowercase().contains(f)
        }
        return filtered.distinctBy { it.packageName }
    }

    private fun discoverDialer(
        pm: PackageManager,
        self: String,
        seen: MutableSet<String>,
        into: MutableList<InstalledActionApp>,
    ) {
        if (into.any { ContactReach.isDialerPackage(it.packageName) }) return
        val resolves = runCatching {
            pm.queryIntentActivities(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:1234567890")),
                PackageManager.MATCH_DEFAULT_ONLY,
            )
        }.getOrDefault(emptyList())
        for (ri in resolves) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == self || pkg == "android") continue
            if (!ContactReach.isDialerPackage(pkg)) continue
            if (!seen.add(pkg)) continue
            into.add(0,
                InstalledActionApp(
                    name = ri.loadLabel(pm)?.toString() ?: "Phone",
                    packageName = pkg,
                    color = Color(0xFF81A1C1),
                    icon = runCatching { ri.loadIcon(pm) }.getOrNull(),
                ),
            )
            break
        }
    }

    private fun resolve(pm: PackageManager, candidate: CallCandidate): InstalledActionApp? {
        for (pkg in candidate.packages) {
            val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: continue
            if (!info.enabled) continue
            pm.getLaunchIntentForPackage(pkg) ?: continue
            return InstalledActionApp(
                name = candidate.name,
                packageName = pkg,
                color = Color(candidate.color),
                icon = runCatching { pm.getApplicationIcon(info) }.getOrNull(),
            )
        }
        return null
    }
}
