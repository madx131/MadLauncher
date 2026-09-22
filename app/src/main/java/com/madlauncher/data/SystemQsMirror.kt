package com.madlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.provider.Settings

/**
 * Mirrors the system Quick Settings row ([Settings.Secure] `sysui_qs_tiles`) into MAD
 * and clicks tiles via `cmd statusbar click-tile` so the status bar stays in sync.
 */
object SystemQsMirror {
    private val skipSpecs = setOf("edit", "tiles", "")

    /** One-shot tiles: fire status-bar click only (no on/off state). */
    private val actionSpecs = setOf(
        "scanner", "screenshot", "screenrecord", "taskmanager", "freeformhang",
        "controls", "settings", "wallet",
    )

    /** Built-in SystemUI tile tokens → MAD catalog id (when one exists). */
    private val stockToMad = mapOf(
        "wifi" to "wifi",
        "bt" to "bt",
        "bluetooth" to "bt",
        "cell" to "cell",
        "airplane" to "air",
        "flashlight" to "torch",
        "batterysaver" to "battery",
        "battery" to "battery",
        "rotation" to "rotate",
        "hotspot" to "hotspot",
        "location" to "location",
        "night" to "night",
        "dnd" to "dnd",
        "quietmode" to "dnd",
        "mute" to "sound",
        "volume" to "volume",
        "wallet" to "wallet",
        "nfc" to "nfc",
        "cast" to "cast",
        "vpn" to "vpn",
        "dark" to "dark",
        "nightmode" to "dark",
        "datasaver" to "data_saver",
        "data_saver" to "data_saver",
        "inversion" to "invert",
        "invert" to "invert",
        "screenrecord" to "screenrecord",
        "screenshot" to "screenshot",
        "settings" to "settings",
        "internet" to "internet",
        "autobrightness" to "autobrightness",
        "scanner" to "scanner",
    )

    private val stockLabels = mapOf(
        "wifi" to "Wi-Fi",
        "bt" to "Bluetooth",
        "bluetooth" to "Bluetooth",
        "cell" to "Mobile data",
        "airplane" to "Airplane",
        "flashlight" to "Flashlight",
        "batterysaver" to "Battery Saver",
        "rotation" to "Auto-rotate",
        "hotspot" to "Hotspot",
        "location" to "Location",
        "night" to "Night Light",
        "dnd" to "Do Not Disturb",
        "quietmode" to "Do Not Disturb",
        "mute" to "Mute",
        "volume" to "Volume",
        "wallet" to "Wallet",
        "nfc" to "NFC",
        "cast" to "Cast",
        "vpn" to "VPN",
        "dark" to "Dark theme",
        "nightmode" to "Dark theme",
        "screenshot" to "Screenshot",
        "screenrecord" to "Screen record",
        "scanner" to "Scanner",
        "autobrightness" to "Auto brightness",
        "taskmanager" to "Recents",
        "freeformhang" to "Freeform",
        "controls" to "Device controls",
        "internet" to "Internet",
    )

    /** Friendly names when TileService metadata / labels are useless. */
    private val customLabels = mapOf(
        "com.miui.securitycenter/com.miui.powercenter.powersaver.PerformanceModeTileService" to "Performance mode",
        "com.miui.screenrecorder/.service.QuickService" to "Screen recorder",
        "com.miui.screenrecorder/com.miui.screenrecorder.service.QuickService" to "Screen recorder",
        "com.milink.service/com.milink.ui.service.MiLinkTileService" to "Mi Link",
        "com.google.android.gms/com.google.android.gms.nearby.sharing.SharingTileService" to "Quick Share",
        "com.google.android.gms/.nearby.sharing.SharingTileService" to "Quick Share",
        "com.google.android.googlequicksearchbox/com.google.android.apps.search.soundsearch.quicksettings.SoundSearchTileService" to "Sound Search",
        "com.alibaba.aliexpresshd/com.aliexpress.module.imagesearch.quicksetting.ScreenShotService" to "AliExpress scan",
        "in.snapcore.screen_alive/.view.FreeQuickSetting" to "Screen Alive",
        "in.snapcore.screen_alive/in.snapcore.screen_alive.view.FreeQuickSetting" to "Screen Alive",
        "org.adaway/.tile.AdBlockingTileService" to "AdAway",
        "org.adaway/org.adaway.tile.AdBlockingTileService" to "AdAway",
        "com.microsoft.office.officehubrow/com.microsoft.office.officemobile.tiles.CopilotVoiceTileService" to "Copilot",
    )

    private val customLabelsByPackage = mapOf(
        "com.miui.screenrecorder" to "Screen recorder",
        "in.snapcore.screen_alive" to "Screen Alive",
        "org.adaway" to "AdAway",
        "com.google.android.gms" to "Quick Share",
        "com.miui.securitycenter" to "Performance mode",
        "com.milink.service" to "Mi Link",
        "com.microsoft.office.officehubrow" to "Copilot",
        "com.alibaba.aliexpresshd" to "AliExpress scan",
        "com.google.android.googlequicksearchbox" to "Sound Search",
    )

    data class SystemTile(
        val spec: String,
        val id: String,
        val label: String,
        val customComponent: ComponentName? = null,
    )

    fun isActionTile(spec: String): Boolean {
        val token = spec.trim().lowercase()
        if (token.startsWith("custom(")) return false
        return token in actionSpecs
    }

    fun isCustomSpec(spec: String): Boolean = spec.trim().startsWith("custom(", ignoreCase = true)

    fun readRawSpecs(context: Context): List<String> {
        val raw = runCatching {
            Settings.Secure.getString(context.contentResolver, "sysui_qs_tiles")
        }.getOrNull().orEmpty()
        if (raw.isBlank()) return emptyList()
        return parseSpecs(raw)
    }

    fun readTiles(context: Context): List<SystemTile> {
        val specs = readRawSpecs(context)
        if (specs.isEmpty()) return emptyList()
        return specs.mapNotNull { resolveTile(context, it) }
    }

    fun discoverExtraTiles(context: Context, already: Set<String>): List<SystemTile> {
        val pm = context.packageManager
        val intent = Intent("android.service.quicksettings.action.QS_TILE")
        val services = runCatching {
            pm.queryIntentServices(intent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())
        return services.mapNotNull { ri ->
            val info = ri.serviceInfo ?: return@mapNotNull null
            val cn = ComponentName(info.packageName, info.name)
            val spec = "custom(${cn.flattenToShortString()})"
            val id = idForSpec(spec)
            if (id in already || spec in already) return@mapNotNull null
            val label = customTileLabel(context, cn, info)
            SystemTile(spec, id, label, cn)
        }.distinctBy { it.id }
            .sortedBy { it.label.lowercase() }
    }

    fun clickTile(spec: String): Boolean {
        for (arg in clickArgVariants(spec)) {
            if (runClick(null, arg)) return true
        }
        return false
    }

    fun clickTile(context: Context, spec: String): Boolean {
        for (arg in clickArgVariants(spec)) {
            if (runClick(context, arg)) return true
            if (clickViaStatusBarService(arg)) return true
        }
        return false
    }

    fun writeSpecs(context: Context, specs: List<String>): Boolean {
        val cleaned = specs.map { it.trim() }.filter { it.isNotEmpty() && it !in skipSpecs }
        if (cleaned.isEmpty()) return false
        val joined = cleaned.joinToString(",")
        val secureOk = runCatching {
            Settings.Secure.putString(context.contentResolver, "sysui_qs_tiles", joined)
            true
        }.getOrDefault(false)
        val cmdOk = runCatching {
            val proc = Runtime.getRuntime().exec(arrayOf("cmd", "statusbar", "set-tiles", joined))
            proc.waitFor()
            proc.exitValue() == 0
        }.getOrDefault(false)
        return secureOk || cmdOk
    }

    fun idForSpec(spec: String): String {
        val s = spec.trim()
        if (s.startsWith("custom(", ignoreCase = true) && s.endsWith(")")) {
            val inner = s.removePrefix("custom(").removePrefix("Custom(").removeSuffix(")")
            return "custom:$inner"
        }
        return stockToMad[s.lowercase()] ?: s.lowercase()
    }

    fun specForId(id: String, known: List<SystemTile> = emptyList()): String? {
        known.firstOrNull { it.id == id }?.let { return it.spec }
        if (id.startsWith("custom:")) {
            return "custom(${id.removePrefix("custom:")})"
        }
        stockToMad.entries.firstOrNull { it.value == id }?.let { return it.key }
        return id.takeIf { it.isNotBlank() }
    }

    fun customTileLabel(context: Context, cn: ComponentName, serviceInfo: ServiceInfo? = null): String {
        val flat = cn.flattenToShortString()
        customLabels[flat]?.let { return it }
        customLabels["${cn.packageName}/${cn.className.substringAfterLast('.')}"]?.let { return it }
        customLabelsByPackage[cn.packageName]?.let { return it }
        val si = serviceInfo ?: runCatching {
            context.packageManager.getServiceInfo(cn, PackageManager.GET_META_DATA)
        }.getOrNull()
        if (si != null) {
            si.metaData?.let { meta ->
                meta.getString("android.service.quicksettings.TILE_LABEL")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
                val resId = meta.getInt("android.service.quicksettings.TILE_LABEL", 0)
                if (resId != 0) {
                    runCatching { context.getString(resId) }.getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { return it }
                }
            }
            val svcLabel = si.loadLabel(context.packageManager)?.toString().orEmpty().trim()
            val shortClass = cn.className.substringAfterLast('.')
            if (svcLabel.isNotBlank() && svcLabel != shortClass && !looksLikeClassName(svcLabel)) {
                return svcLabel
            }
        }
        runCatching {
            val appInfo = context.packageManager.getApplicationInfo(cn.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo)?.toString()?.trim()
        }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        return friendlyClassName(cn.className.substringAfterLast('.'))
    }

    private fun looksLikeClassName(label: String): Boolean {
        return label.endsWith("TileService") ||
            label.endsWith("Service") ||
            label.endsWith("QuickSetting") ||
            label.contains("TileService")
    }

    private fun resolveTile(context: Context, spec: String): SystemTile? {
        val s = spec.trim()
        if (s.isEmpty() || s.lowercase() in skipSpecs) return null
        if (s.startsWith("custom(", ignoreCase = true) && s.endsWith(")")) {
            val inner = s.substringAfter('(').substringBeforeLast(')')
            val cn = parseComponent(inner) ?: return null
            val si = runCatching {
                context.packageManager.getServiceInfo(cn, PackageManager.GET_META_DATA)
            }.getOrNull()
            val label = customTileLabel(context, cn, si)
            return SystemTile(s, idForSpec(s), label, cn)
        }
        val key = s.lowercase()
        val id = stockToMad[key] ?: key
        val label = stockLabels[key]
            ?: QsCatalog.spec(id)?.label
            ?: s.replaceFirstChar { it.uppercase() }
        return SystemTile(s, id, label)
    }

    private fun parseComponent(inner: String): ComponentName? {
        ComponentName.unflattenFromString(inner)?.let { return it }
        val pkg = inner.substringBefore('/')
        val cls = inner.substringAfter('/')
        if (pkg.isBlank() || cls.isBlank()) return null
        return ComponentName(pkg, if (cls.startsWith('.')) pkg + cls else cls)
    }

    private fun friendlyClassName(raw: String): String {
        return raw
            .removeSuffix("TileService")
            .removeSuffix("Service")
            .removeSuffix("QuickSetting")
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace('_', ' ')
            .trim()
            .ifBlank { raw }
            .replaceFirstChar { it.uppercase() }
    }

    private fun clickArgVariants(spec: String): List<String> {
        val primary = clickArg(spec) ?: return emptyList()
        val out = linkedSetOf(primary)
        if (spec.startsWith("custom(", ignoreCase = true)) {
            val inner = spec.substringAfter('(').substringBeforeLast(')')
            parseComponent(inner)?.let { cn ->
                out += cn.flattenToShortString()
                out += "${cn.packageName}/${cn.className}"
                out += "${cn.packageName}/.${cn.className.substringAfterLast('.')}"
            }
        }
        return out.toList()
    }

    private fun clickArg(spec: String): String? {
        val s = spec.trim()
        if (s.isEmpty()) return null
        if (s.startsWith("custom(", ignoreCase = true)) {
            return s.substringAfter('(').substringBeforeLast(')')
        }
        if (s.startsWith("custom:")) return s.removePrefix("custom:")
        return s
    }

    private fun runClick(context: Context?, arg: String): Boolean {
        if (execClick(arrayOf("cmd", "statusbar", "click-tile", arg))) return true
        if (execClick(arrayOf("sh", "-c", "cmd statusbar click-tile ${shellQuote(arg)}"))) return true
        if (context != null && clickViaStatusBarManager(context, arg)) return true
        return false
    }

    private fun clickViaStatusBarManager(context: Context, component: String): Boolean {
        val cn = ComponentName.unflattenFromString(component)
        return runCatching {
            @Suppress("WrongConstant")
            val sbm = context.getSystemService("statusbar") ?: return false
            val mService = sbm.javaClass.getDeclaredField("mService").apply { isAccessible = true }.get(sbm)
                ?: return false
            val svc = mService.javaClass
            if (cn != null) {
                runCatching {
                    svc.getMethod("clickTile", ComponentName::class.java).invoke(mService, cn)
                    return true
                }
            }
            svc.getMethod("clickTile", String::class.java).invoke(mService, component)
            true
        }.getOrDefault(false)
    }

    private fun clickViaStatusBarService(component: String): Boolean {
        return runCatching {
            val svc = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "statusbar") ?: return false
            val stub = Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
                .getMethod("asInterface", android.os.IBinder::class.java)
                .invoke(null, svc)
            stub?.javaClass?.getMethod("clickTile", String::class.java)?.invoke(stub, component)
            true
        }.getOrDefault(false)
    }

    private fun execClick(cmd: Array<String>): Boolean {
        return runCatching {
            val proc = Runtime.getRuntime().exec(cmd)
            proc.waitFor()
            proc.exitValue() == 0
        }.getOrDefault(false)
    }

    private fun shellQuote(arg: String): String {
        return "'" + arg.replace("'", "'\\''") + "'"
    }

    private fun parseSpecs(raw: String): List<String> {
        val out = ArrayList<String>()
        var i = 0
        while (i < raw.length) {
            while (i < raw.length && (raw[i] == ',' || raw[i].isWhitespace())) i++
            if (i >= raw.length) break
            if (raw.regionMatches(i, "custom(", 0, 7, ignoreCase = true)) {
                val start = i
                val close = raw.indexOf(')', i)
                if (close < 0) {
                    out += raw.substring(i).trim()
                    break
                }
                out += raw.substring(start, close + 1).trim()
                i = close + 1
            } else {
                val comma = raw.indexOf(',', i)
                if (comma < 0) {
                    out += raw.substring(i).trim()
                    break
                }
                out += raw.substring(i, comma).trim()
                i = comma + 1
            }
        }
        return out.filter { it.isNotEmpty() }
    }
}
