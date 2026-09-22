package com.madlauncher.action

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import com.madlauncher.a11y.OttCallBridge
import com.madlauncher.data.ContactEntry
import com.madlauncher.data.PermissionChecker

/**
 * WhatsApp voice/video — never [tel:], whatsapp://call, or https send links (invalid call link).
 * Uses ContactsProvider voip rows, component chat open + a11y call tap.
 */
object WhatsAppCallBridge {
    private const val MIME_VOICE = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
    private const val MIME_VIDEO = "vnd.android.cursor.item/vnd.com.whatsapp.video.call"

    fun place(
        context: Context,
        packageName: String,
        contact: ContactEntry,
        video: Boolean,
    ): CallResult {
        val digits = ContactReach.phoneDigits(contact)
        if (digits != null) {
            findVoipDataId(context, digits, video)?.let { dataId ->
                if (launchVoip(context, packageName, dataId, video)) {
                    return CallResult.Started
                }
            }
            val canAutomate = PermissionChecker.isAccessibilityEnabled(context)
            if (canAutomate) {
                buildChatIntent(context, packageName, digits)?.let { intent ->
                    OttCallBridge.begin(context, packageName, video, intent)
                    return CallResult.Automating
                }
            }
            if (openChatByJid(context, packageName, digits)) {
                return CallResult.OpenedThread
            }
        }
        val thread = ContactReach.threadKey(contact)
        if (thread != null && thread.all { it.isDigit() }) {
            return place(context, packageName, contact.copy(phone = thread), video)
        }
        if (thread != null && !thread.all { it.isDigit() }) {
            return openNamedChat(context, packageName, thread, video)
        }
        return CallResult.Failed
    }

    private fun openNamedChat(
        context: Context,
        packageName: String,
        name: String,
        @Suppress("UNUSED_PARAMETER") video: Boolean,
    ): CallResult {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return CallResult.Failed
        runCatching {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return CallResult.OpenedThread
    }

    internal fun buildChatIntent(context: Context, packageName: String, digits: String): Intent? {
        val clean = digits.filter { it.isDigit() }
        if (clean.length < 7) return null
        val jid = PhoneNumberUtils.stripSeparators(clean) + "@s.whatsapp.net"
        val convo = conversationComponent(packageName)
        val byJid = Intent(Intent.ACTION_MAIN).apply {
            setComponent(convo)
            putExtra("jid", jid)
        }
        if (byJid.resolveActivity(context.packageManager) != null) return byJid
        return null
    }

    internal fun findVoipDataId(context: Context, phoneDigits: String, video: Boolean): Long? {
        val mime = if (video) MIME_VIDEO else MIME_VOICE
        val want = phoneDigits.filter { it.isDigit() }.takeLast(10)
        if (want.length < 7) return null
        val cursor = runCatching {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data._ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(mime),
                null,
            )
        }.getOrNull() ?: return null
        cursor.use {
            val idIdx = it.getColumnIndex(ContactsContract.Data._ID)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (idIdx < 0) return null
            while (it.moveToNext()) {
                val rowDigits = it.getString(numIdx)?.filter { c -> c.isDigit() }.orEmpty()
                if (rowDigits.isBlank()) continue
                if (rowDigits.takeLast(10) == want || want.takeLast(10) == rowDigits.takeLast(10)) {
                    return it.getLong(idIdx)
                }
            }
        }
        return null
    }

    private fun launchVoip(
        context: Context,
        packageName: String,
        dataId: Long,
        video: Boolean,
    ): Boolean {
        val mime = if (video) MIME_VIDEO else MIME_VOICE
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                Uri.parse("${ContactsContract.Data.CONTENT_URI}/$dataId"),
                mime,
            )
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun openChatByJid(context: Context, packageName: String, digits: String): Boolean {
        val intent = buildChatIntent(context, packageName, digits) ?: return false
        return startIfResolvable(context, intent)
    }

    private fun conversationComponent(packageName: String): ComponentName {
        val pkg = packageName.lowercase()
        val cls = when {
            pkg.contains("w4b") -> "$packageName.Conversation"
            else -> "com.whatsapp.Conversation"
        }
        return ComponentName(packageName, cls)
    }

    private fun startIfResolvable(context: Context, intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
