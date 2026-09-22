package com.madlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

/**
 * ADW/Nova/GO-style icon pack loader with an in-memory appfilter cache.
 * MAD still applies its own icon masks/shapes in Compose.
 */
object IconPackResolver {
    data class PackInfo(val packageName: String, val label: String)

    /** packPackage → (componentKey → drawable name) */
    private val appFilterCache = ConcurrentHashMap<String, Map<String, String>>()

    fun listInstalled(context: Context): List<PackInfo> {
        val pm = context.packageManager
        val intents = listOf(
            Intent("org.adw.launcher.THEMES"),
            Intent("com.gau.go.launcherex.theme"),
            Intent("com.novalauncher.THEME"),
            Intent("com.teslacoilsw.launcher.THEME"),
        )
        val found = linkedMapOf<String, PackInfo>()
        intents.forEach { base ->
            pm.queryIntentActivities(base, PackageManager.MATCH_ALL).forEach { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg !in found) {
                    val label = ri.loadLabel(pm).toString()
                    found[pkg] = PackInfo(pkg, label)
                }
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }

    fun clearCache(packPackage: String? = null) {
        if (packPackage.isNullOrBlank()) appFilterCache.clear()
        else appFilterCache.remove(packPackage)
    }

    fun loadIcon(context: Context, packPackage: String, component: ComponentName): Drawable? {
        if (packPackage.isBlank()) return null
        val pm = context.packageManager
        val resources = runCatching { pm.getResourcesForApplication(packPackage) }.getOrNull() ?: return null
        val drawableName = resolveDrawableName(context, packPackage, component) ?: return null
        val id = resources.getIdentifier(drawableName, "drawable", packPackage)
        if (id == 0) return null
        return runCatching { resources.getDrawable(id, null) }.getOrNull()
    }

    private fun resolveDrawableName(context: Context, packPackage: String, component: ComponentName): String? {
        val map = appFilterCache[packPackage] ?: loadAppFilter(context, packPackage).also {
            if (it.isNotEmpty()) appFilterCache[packPackage] = it
        }
        if (map.isEmpty()) return null
        val keys = listOf(
            component.flattenToString(),
            component.flattenToShortString(),
            "ComponentInfo{${component.flattenToString()}}",
            "ComponentInfo{${component.flattenToShortString()}}",
        )
        keys.forEach { key ->
            map[key]?.let { return it }
            map[key.lowercase()]?.let { return it }
        }
        // Loose package fallback (last resort — first matching item for that package).
        val pkgPrefix = component.packageName
        return map.entries.firstOrNull { (k, _) ->
            k.contains(pkgPrefix, ignoreCase = true)
        }?.value
    }

    private fun loadAppFilter(context: Context, packPackage: String): Map<String, String> {
        val pm = context.packageManager
        val resources = runCatching { pm.getResourcesForApplication(packPackage) }.getOrNull() ?: return emptyMap()
        val xmlId = resources.getIdentifier("appfilter", "xml", packPackage)
        if (xmlId == 0) return emptyMap()
        val parser = runCatching { resources.getXml(xmlId) }.getOrNull() ?: return emptyMap()
        val out = linkedMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component").orEmpty()
                val drawable = parser.getAttributeValue(null, "drawable").orEmpty()
                if (component.isNotBlank() && drawable.isNotBlank()) {
                    out[component] = drawable
                    out[component.lowercase()] = drawable
                }
            }
            event = parser.next()
        }
        return out
    }
}
