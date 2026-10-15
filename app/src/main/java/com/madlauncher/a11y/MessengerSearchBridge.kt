package com.madlauncher.a11y

import android.content.Context
import android.content.Intent
import com.madlauncher.data.NameKey

/** Open a chat by typing the contact name into Search and tapping that row. */
object MessengerSearchBridge {
    enum class Phase {
        OpenSearch,
        TypeQuery,
        PickResult,
        Done,
    }

    class Request(
        val packageName: String,
        val contactName: String,
        val queries: List<String> = searchQueries(contactName),
        var queryIndex: Int = 0,
        val deadlineMs: Long = System.currentTimeMillis() + 18_000L,
        var phase: Phase = Phase.OpenSearch,
        var picked: Boolean = false,
    )

    @Volatile
    var pending: Request? = null
        private set

    fun begin(context: Context, packageName: String, contactName: String) {
        val name = contactName.trim()
        if (name.isBlank()) return
        pending = Request(packageName = packageName, contactName = name)
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        runCatching { context.startActivity(launch) }
        MadAccessibilityService.instance?.scheduleMessengerSearch(700L)
        MadAccessibilityService.instance?.scheduleMessengerSearch(1_400L)
    }

    fun clear() {
        pending = null
    }

    fun markDone() {
        pending = null
    }

    fun searchQueries(name: String): List<String> {
        val full = name.trim()
        if (full.isEmpty()) return emptyList()
        val joined = NameKey.joinMarks(full)
        val words = NameKey.fold(full).split(' ').filter { it.length >= 3 }
        return (listOf(full, joined) + words).distinct()
    }
}
