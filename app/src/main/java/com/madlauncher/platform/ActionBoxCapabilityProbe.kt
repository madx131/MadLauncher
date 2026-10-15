package com.madlauncher.platform

import android.content.Context
import android.content.Intent
import com.madlauncher.action.ActionAppKind
import com.madlauncher.action.ActionApps
import com.madlauncher.action.CallAppMode
import com.madlauncher.action.CallAppRegistry
import com.madlauncher.action.ContactReach
import com.madlauncher.action.InstalledActionApp
import com.madlauncher.action.SocialBrowserApps
import com.madlauncher.action.SocialPostBridge
import com.madlauncher.data.PermissionChecker
import com.madlauncher.notif.NotificationHub

/**
 * Discovers which Action Box command paths are usable on this device
 * (installed apps + permissions). Re-run after package install/update/uninstall.
 */
object ActionBoxCapabilityProbe {
    data class Result(
        val capabilities: Map<PlatformFeature, FeatureStatus>,
        /** Human-readable catalog for compatibility reports. */
        val catalogSummary: String,
    )

    fun probe(context: Context): Result {
        val apps = (context.applicationContext as? com.madlauncher.MadApp)?.actionApps
            ?: ActionApps(context)
        val grants = PermissionChecker.snapshot(context)
        val nls = grants.notificationListener
        val nlsConnected = NotificationHub.connected.value
        val a11y = grants.accessibility
        val contacts = grants.contacts

        val messaging = apps.installed(ActionAppKind.Messaging)
        val email = apps.installed(ActionAppKind.Email)
        val social = apps.installed(ActionAppKind.Social)
        val ai = apps.installed(ActionAppKind.Ai)
        val notes = apps.installed(ActionAppKind.Notes)
        val translators = apps.installed(ActionAppKind.Translator)
        val nav = apps.installed(ActionAppKind.Nav)
        val voiceCall = CallAppRegistry.discover(context, CallAppMode.Voice, "", messaging)
        val waInstalled = voiceCall.any { it.packageName.contains("whatsapp", ignoreCase = true) }
        val dialerInstalled = voiceCall.any { ContactReach.isDialerPackage(it.packageName) }
        val socialNeedsA11y = social.any { pkgNeedsSocialA11y(it.packageName) }
        val sendableAi = ai.count { canReceiveSend(context, it.packageName) }

        val caps = mutableMapOf<PlatformFeature, FeatureStatus>()

        caps[PlatformFeature.ACTIONBOX_OTT_CALL] = FeatureStatus.UNAVAILABLE
        val metaInstalled = social.any { SocialPostBridge.metaBlocksOutsideInput(it.packageName) }

        caps[PlatformFeature.ACTIONBOX_RECIPIENT_HISTORY] = when {
            !nls -> FeatureStatus.UNAVAILABLE
            else -> FeatureStatus.WORKING
        }

        caps[PlatformFeature.ACTIONBOX_WHATSAPP_CALL] = when {
            !waInstalled -> FeatureStatus.NOT_INSTALLED
            !contacts -> FeatureStatus.DEGRADED
            else -> FeatureStatus.WORKING
        }

        caps[PlatformFeature.ACTIONBOX_PHONE_CALL] = when {
            !dialerInstalled -> FeatureStatus.NOT_INSTALLED
            else -> FeatureStatus.WORKING
        }

        caps[PlatformFeature.ACTIONBOX_SOCIAL_POST] = when {
            social.isEmpty() -> FeatureStatus.NOT_INSTALLED
            socialNeedsA11y && !a11y -> FeatureStatus.DEGRADED
            else -> FeatureStatus.WORKING
        }

        caps[PlatformFeature.ACTIONBOX_MESSAGING] = when {
            messaging.isEmpty() && email.isEmpty() -> FeatureStatus.NOT_INSTALLED
            !contacts && !nls -> FeatureStatus.DEGRADED
            else -> FeatureStatus.WORKING
        }

        caps[PlatformFeature.ACTIONBOX_CONTACT_LOOKUP] = when {
            !contacts && messaging.isEmpty() && email.isEmpty() && !dialerInstalled ->
                FeatureStatus.NOT_INSTALLED
            !contacts -> FeatureStatus.DEGRADED
            else -> FeatureStatus.WORKING
        }

        caps[PlatformFeature.ACTIONBOX_AI] = when {
            ai.isEmpty() -> FeatureStatus.NOT_INSTALLED
            sendableAi == 0 -> FeatureStatus.DEGRADED
            else -> FeatureStatus.DEGRADED // deep links remain unreliable
        }

        caps[PlatformFeature.ACTIONBOX_TRANSLATE] = FeatureStatus.WORKING

        caps[PlatformFeature.ACTIONBOX_NOTES] = when {
            notes.isEmpty() -> FeatureStatus.NOT_INSTALLED
            else -> FeatureStatus.WORKING
        }

        caps[PlatformFeature.ACTIONBOX_NAV] = when {
            nav.isEmpty() -> FeatureStatus.NOT_INSTALLED
            else -> FeatureStatus.WORKING
        }

        val socialCommand = { command: SocialBrowserApps.BrowserCommand ->
            runCatching {
                val settings = com.madlauncher.data.LocalStore(context).appearanceSettings
                SocialBrowserApps.socialApps.any { app ->
                    SocialBrowserApps.allowsApp(
                        app,
                        SocialBrowserApps.mode(
                            app.id,
                            settings.experimentalSocialBrowserMode,
                            settings.experimentalSocialBrowserApps,
                            settings.experimentalSocialBrowserOff,
                        ),
                        command,
                    )
                }
            }.getOrDefault(false)
        }
        fun webStatus(on: Boolean) = if (on) FeatureStatus.DEGRADED else FeatureStatus.UNAVAILABLE
        val dmWeb = webStatus(socialCommand(SocialBrowserApps.BrowserCommand.MSG))
        val postWeb = webStatus(socialCommand(SocialBrowserApps.BrowserCommand.POST))
        caps[PlatformFeature.ACTIONBOX_SOCIAL_DM] = dmWeb
        caps[PlatformFeature.ACTIONBOX_TRUTH_POST] = postWeb
        caps[PlatformFeature.ACTIONBOX_META_SOCIAL] = when {
            !metaInstalled -> FeatureStatus.NOT_INSTALLED
            else -> postWeb
        }

        val catalog = buildString {
            appendLine("Messaging: ${names(messaging)}")
            appendLine("Email: ${names(email)}")
            appendLine("Social: ${names(social)}")
            appendLine("Calls: ${names(voiceCall)}")
            appendLine("AI: ${names(ai)}")
            appendLine("Notes: ${names(notes)}")
            appendLine("Translate: ${names(translators)}")
            appendLine("Nav: ${names(nav)}")
            appendLine("Contacts permission: $contacts")
            appendLine("Notification access: $nls (connected=$nlsConnected)")
            appendLine("Accessibility: $a11y")
        }.trim()

        return Result(caps, catalog)
    }

    private fun names(list: List<InstalledActionApp>): String =
        if (list.isEmpty()) "(none)" else list.joinToString { it.name }

    private fun pkgNeedsSocialA11y(packageName: String): Boolean {
        if (SocialPostBridge.parkedInApp(packageName)) return false
        val p = packageName.lowercase()
        return p.contains("tiktok") || p.contains("zhiliaoapp") || p.contains("discord") ||
            p.contains("facebook") || p.contains("instagram")
    }

    private fun canReceiveSend(context: Context, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .setPackage(packageName)
        return intent.resolveActivity(context.packageManager) != null
    }
}
