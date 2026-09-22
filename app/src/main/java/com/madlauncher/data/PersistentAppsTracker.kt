package com.madlauncher.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import com.madlauncher.notif.LiveNotif
import com.madlauncher.notif.MadNotificationListener

class PersistentAppsTracker(private val context: Context) {
    fun snapshot(live: List<LiveNotif>): List<PersistentApp> {
        if (!PermissionChecker.isNotificationListener(context)) return emptyList()
        val byPkg = linkedMapOf<String, PersistentApp>()
        collectSessions().forEach { byPkg[it.packageName] = it }
        collectOngoing(live).forEach { app ->
            val existing = byPkg[app.packageName]
            if (existing == null) {
                byPkg[app.packageName] = app
            } else if (!existing.isMedia && app.subtitle.isNotBlank()) {
                byPkg[app.packageName] = existing.copy(subtitle = app.subtitle)
            } else if (existing.isMedia && existing.subtitle in listOf("Playing", "Paused") && app.subtitle.isNotBlank()) {
                byPkg[app.packageName] = existing.copy(subtitle = app.subtitle)
            }
        }
        return byPkg.values.take(12)
    }

    fun playPause(packageName: String): Boolean {
        val controller = controllerFor(packageName) ?: return false
        val state = controller.playbackState?.state ?: return false
        return runCatching {
            if (state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING ||
                state == PlaybackState.STATE_FAST_FORWARDING ||
                state == PlaybackState.STATE_REWINDING
            ) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            true
        }.getOrDefault(false)
    }

    fun skipNext(packageName: String): Boolean {
        val controller = controllerFor(packageName) ?: return false
        return runCatching {
            controller.transportControls.skipToNext()
            true
        }.getOrDefault(false)
    }

    fun skipPrevious(packageName: String): Boolean {
        val controller = controllerFor(packageName) ?: return false
        return runCatching {
            controller.transportControls.skipToPrevious()
            true
        }.getOrDefault(false)
    }

    private fun controllerFor(packageName: String): MediaController? {
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return null
        val listener = ComponentName(context, MadNotificationListener::class.java)
        return runCatching { msm.getActiveSessions(listener) }
            .getOrDefault(emptyList())
            .firstOrNull { it.packageName == packageName }
    }

    private fun collectSessions(): List<PersistentApp> {
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return emptyList()
        val listener = ComponentName(context, MadNotificationListener::class.java)
        val controllers = runCatching { msm.getActiveSessions(listener) }.getOrDefault(emptyList())
        val pm = context.packageManager
        return controllers.mapNotNull { controller ->
            val pkg = controller.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            val playback = controller.playbackState
            val state = playback?.state ?: PlaybackState.STATE_NONE
            if (state == PlaybackState.STATE_STOPPED ||
                state == PlaybackState.STATE_NONE ||
                state == PlaybackState.STATE_ERROR
            ) return@mapNotNull null
            val md = controller.metadata
            val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val playing = state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING ||
                state == PlaybackState.STATE_FAST_FORWARDING
            val subtitle = listOfNotNull(title, artist).filter { it.isNotBlank() }.joinToString(" · ")
                .ifBlank { if (playing) "Playing" else "Paused" }
            val actions = playback?.actions ?: 0L
            val canSkip = actions == 0L ||
                (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L ||
                (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L
            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
            val icon = runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
            PersistentApp(
                packageName = pkg,
                appName = label,
                icon = icon,
                subtitle = subtitle,
                isMedia = true,
                isPlaying = playing,
                canSkip = canSkip,
            )
        }
    }

    private fun collectOngoing(live: List<LiveNotif>): List<PersistentApp> {
        val pm = context.packageManager
        return live.filter { it.ongoing && isPersistentCategory(it) }.map { item ->
            val icon = runCatching { pm.getApplicationIcon(item.packageName) }.getOrNull()
            val subtitle = item.body.ifBlank { item.title }.ifBlank { persistentLabel(item.category) }
            val isMedia = item.hasMediaSession || item.category == Notification.CATEGORY_TRANSPORT
            PersistentApp(
                packageName = item.packageName,
                appName = item.appName,
                icon = icon,
                subtitle = subtitle,
                isMedia = isMedia,
                isPlaying = isMedia,
                canSkip = isMedia,
            )
        }.distinctBy { it.packageName }
    }

    private fun isPersistentCategory(item: LiveNotif): Boolean {
        if (item.hasMediaSession) return true
        return when (item.category) {
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_NAVIGATION,
            -> true
            else -> Build.VERSION.SDK_INT >= 31 && item.category == "navigation"
        }
    }

    private fun persistentLabel(category: String?): String {
        return when (category) {
            Notification.CATEGORY_CALL -> "On a call"
            Notification.CATEGORY_NAVIGATION, "navigation" -> "Navigation"
            Notification.CATEGORY_TRANSPORT -> "Playing"
            else -> "Running"
        }
    }
}
