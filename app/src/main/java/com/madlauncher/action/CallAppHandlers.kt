package com.madlauncher.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.madlauncher.data.ContactEntry

/** Result of attempting a voice / video call through a specific app. */
enum class CallResult {
    /** App accepted a call deep link or telecom intent. */
    Started,
    /** Accessibility is tapping the in-app call button after opening the thread. */
    Automating,
    /** Opened the app's conversation (no public call API — user taps call there). */
    OpenedThread,
    Failed,
}

/**
 * Per-app voice / video call routing for Action Box `#` and `*`.
 * Only Phone (tel:) and WhatsApp (Contacts voip MIME / JID chat) are supported.
 */
object CallAppHandlers {
    fun voiceCall(context: Context, app: InstalledActionApp, contact: ContactEntry): CallResult =
        place(context, app.packageName, contact, video = false)

    fun videoCall(context: Context, app: InstalledActionApp, contact: ContactEntry): CallResult =
        place(context, app.packageName, contact, video = true)

    internal fun place(
        context: Context,
        packageName: String,
        contact: ContactEntry,
        video: Boolean,
    ): CallResult {
        if (!CallAppRegistry.isSupportedCallPackage(packageName)) {
            return CallResult.Failed
        }
        return placeDefault(context, packageName, contact, video)
    }

    internal fun placeDefault(
        context: Context,
        packageName: String,
        contact: ContactEntry,
        video: Boolean,
    ): CallResult {
        val pkg = packageName.lowercase()
        if (pkg.contains("whatsapp")) {
            return WhatsAppCallBridge.place(context, packageName, contact, video)
        }
        if (isDialerPackage(pkg)) {
            for (plan in dialerPlans(contact, video)) {
                if (tryPlan(context, packageName, plan)) {
                    return CallResult.Started
                }
            }
        }
        return CallResult.Failed
    }

    internal data class CallPlan(
        val uri: String? = null,
        val action: String? = null,
        val extras: Map<String, String> = emptyMap(),
        val isCallIntent: Boolean = true,
    )

    internal fun phoneDigits(contact: ContactEntry): String? = ContactReach.phoneDigits(contact)

    internal fun threadKey(contact: ContactEntry): String? = ContactReach.threadKey(contact)

    internal fun isDialerPackage(packageName: String): Boolean =
        ContactReach.isDialerPackage(packageName)

    private fun dialerPlans(contact: ContactEntry, video: Boolean): List<CallPlan> {
        val digits = phoneDigits(contact) ?: contact.phone?.filter { it.isDigit() }
        if (digits.isNullOrBlank()) return emptyList()
        val tel = contact.phone?.trim()?.takeIf { it.startsWith("+") } ?: "+$digits"
        val base = CallPlan(uri = "tel:$tel", isCallIntent = true)
        if (!video) return listOf(base)
        return listOf(
            base.copy(
                extras = mapOf(
                    "android.telecom.extra.START_CALL_WITH_VIDEO_STATE" to "3",
                    "com.android.phone.videocall" to "true",
                ),
            ),
        )
    }

    private fun tryPlan(context: Context, packageName: String, plan: CallPlan): Boolean {
        if (plan.uri?.startsWith("tel:", ignoreCase = true) == true &&
            !isDialerPackage(packageName)
        ) {
            return false
        }
        if (plan.uri?.startsWith("http", ignoreCase = true) == true) {
            return false
        }
        val pm = context.packageManager
        val intent = when {
            plan.action != null -> Intent(plan.action).setPackage(packageName)
            plan.uri != null -> Intent(Intent.ACTION_VIEW, Uri.parse(plan.uri)).setPackage(packageName)
            else -> return false
        }
        plan.extras.forEach { (k, v) ->
            when (v) {
                "true" -> intent.putExtra(k, true)
                "false" -> intent.putExtra(k, false)
                else -> {
                    if (k.contains("VIDEO_STATE") || k.contains("state", ignoreCase = true)) {
                        intent.putExtra(k, v.toIntOrNull() ?: v)
                    } else {
                        intent.putExtra(k, v)
                    }
                }
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolved = intent.resolveActivity(pm) ?: return false
        if (resolved.packageName != packageName) return false
        if (resolved.className.contains("ResolverActivity", ignoreCase = true)) return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
