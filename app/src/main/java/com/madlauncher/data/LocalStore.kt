package com.madlauncher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedNote(
    val id: String,
    val text: String,
    val floating: Boolean,
    val createdAt: Long,
    val title: String = "",
    val collapsed: Boolean = true,
    val x: Int? = null,
    val y: Int? = null,
    val pinnedEdge: String? = null,
    val imagePaths: List<String> = emptyList(),
)

data class SavedReminder(
    val id: String,
    val text: String,
    val createdAt: Long,
)

data class SavedEvent(
    val id: String,
    val title: String,
    val whenLabel: String,
    val createdAt: Long,
)

data class AppLaunchStats(
    val count: Int,
    val lastUsed: Long,
)

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("mad_store", Context.MODE_PRIVATE)

    var preferredMessaging: String?
        get() = prefs.getString("pref_msg", null)
        set(value) { prefs.edit().putString("pref_msg", value).apply() }

    var preferredEmail: String?
        get() = prefs.getString("pref_email", null)
        set(value) { prefs.edit().putString("pref_email", value).apply() }

    var preferredSocial: String?
        get() = prefs.getString("pref_social", null)
        set(value) { prefs.edit().putString("pref_social", value).apply() }

    var preferredNav: String?
        get() = prefs.getString("pref_nav", null)
        set(value) { prefs.edit().putString("pref_nav", value).apply() }

    fun commandPreferred(kind: String): String? {
        commandJson(kind)?.optString("preferred")?.takeIf { it.isNotBlank() }?.let { return it }
        return when (kind) {
            "Messaging" -> preferredMessaging
            "Email" -> preferredEmail
            "Social" -> preferredSocial
            "Nav" -> preferredNav
            else -> null
        }
    }

    fun setCommandPreferred(kind: String, packageName: String?) {
        patchCommand(kind) { o ->
            if (packageName.isNullOrBlank()) o.remove("preferred") else o.put("preferred", packageName)
        }
        when (kind) {
            "Messaging" -> preferredMessaging = packageName
            "Email" -> preferredEmail = packageName
            "Social" -> preferredSocial = packageName
            "Nav" -> preferredNav = packageName
        }
    }

    fun commandHidden(kind: String): Set<String> {
        val arr = commandJson(kind)?.optJSONArray("hidden") ?: return emptySet()
        return buildSet {
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }

    fun setCommandHidden(kind: String, hidden: Set<String>) {
        patchCommand(kind) { o ->
            val arr = JSONArray()
            hidden.filter { it.isNotBlank() }.forEach { arr.put(it) }
            o.put("hidden", arr)
        }
    }

    fun commandExtra(kind: String): List<String> {
        val arr = commandJson(kind)?.optJSONArray("extra") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }

    fun setCommandExtra(kind: String, extra: List<String>) {
        patchCommand(kind) { o ->
            val arr = JSONArray()
            extra.filter { it.isNotBlank() }.distinct().forEach { arr.put(it) }
            o.put("extra", arr)
        }
    }

    fun commandOrder(kind: String): List<String> {
        val arr = commandJson(kind)?.optJSONArray("order") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }

    fun setCommandOrder(kind: String, order: List<String>) {
        patchCommand(kind) { o ->
            val arr = JSONArray()
            order.filter { it.isNotBlank() }.distinct().forEach { arr.put(it) }
            o.put("order", arr)
        }
    }

    private fun commandRoot(): JSONObject {
        val raw = prefs.getString("action_box_commands", null) ?: return JSONObject()
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun commandJson(kind: String): JSONObject? =
        commandRoot().optJSONObject(kind)

    private fun patchCommand(kind: String, block: (JSONObject) -> Unit) {
        val root = commandRoot()
        val o = root.optJSONObject(kind) ?: JSONObject()
        block(o)
        root.put(kind, o)
        prefs.edit().putString("action_box_commands", root.toString()).apply()
    }

    var setupDismissed: Boolean
        get() = prefs.getBoolean("setup_dismissed", false)
        set(value) { prefs.edit().putBoolean("setup_dismissed", value).apply() }

    fun loadGeom(key: String, x: Float, y: Float, w: Float, h: Float): FloatArray {
        val raw = prefs.getString("geom_$key", null) ?: return floatArrayOf(x, y, w, h)
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return floatArrayOf(x, y, w, h)
        return floatArrayOf(
            obj.optDouble("x", x.toDouble()).toFloat(),
            obj.optDouble("y", y.toDouble()).toFloat(),
            obj.optDouble("w", w.toDouble()).toFloat(),
            obj.optDouble("h", h.toDouble()).toFloat(),
        )
    }

    fun saveGeom(key: String, x: Float, y: Float, w: Float, h: Float) {
        prefs.edit().putString(
            "geom_$key",
            JSONObject().put("x", x.toDouble()).put("y", y.toDouble()).put("w", w.toDouble()).put("h", h.toDouble()).toString(),
        ).apply()
    }

    var floatNotifW: Float
        get() = prefs.getFloat("float_notif_w", 300f)
        set(value) { prefs.edit().putFloat("float_notif_w", value).apply() }

    var floatNotifH: Float
        get() = prefs.getFloat("float_notif_h", 280f)
        set(value) { prefs.edit().putFloat("float_notif_h", value).apply() }

    var qsTileIds: List<String>
        get() {
            val raw = prefs.getString("qs_tiles", null)
            if (raw.isNullOrBlank()) return emptyList()
            // Keep stock catalog ids and mirrored custom/system tokens (custom:…).
            return raw.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
        set(value) {
            prefs.edit().putString("qs_tiles", value.joinToString(",")).apply()
        }

    fun qsToggleStrategy(spec: String): String? =
        prefs.getString("qs_toggle_$spec", null)

    fun qsToggleStrategy(spec: String, strategy: String) {
        prefs.edit().putString("qs_toggle_$spec", strategy).apply()
    }

    var dismissedNotifKeys: Set<String>
        get() {
            val raw = prefs.getString("dismissed_notif_keys", null) ?: return emptySet()
            return raw.split("\u0001").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
        set(value) {
            prefs.edit().putString("dismissed_notif_keys", value.take(200).joinToString("\u0001")).apply()
        }

    var dismissedConversations: Set<String>
        get() {
            val raw = prefs.getString("dismissed_conversations", null) ?: return emptySet()
            return raw.split("\u0001").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
        set(value) {
            prefs.edit().putString("dismissed_conversations", value.take(200).joinToString("\u0001")).apply()
        }

    var recentAppIds: List<String>
        get() {
            val raw = prefs.getString("recent_apps", null) ?: return emptyList()
            return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(12)
        }
        set(value) {
            prefs.edit().putString("recent_apps", value.take(12).joinToString(",")).apply()
        }

    var wallpaperPath: String?
        get() = prefs.getString("wallpaper_path", null)
        set(value) { prefs.edit().putString("wallpaper_path", value).apply() }

    var dockSlots: List<String>
        get() {
            val raw = prefs.getString("dock_slots", null)
            if (raw.isNullOrBlank()) return HomeLayout.defaultDock
            val list = raw.split(",").map { it.trim() }
            return if (list.size == 5) list else HomeLayout.defaultDock
        }
        set(value) { prefs.edit().putString("dock_slots", value.joinToString(",")).apply() }

    var dockRow2Slots: List<String>
        get() {
            val raw = prefs.getString("dock_row2_slots", null)
            if (raw.isNullOrBlank()) return List(HomeLayout.DOCK_ROW_SLOTS) { "" }
            val list = raw.split(",").map { it.trim() }
            return if (list.size == HomeLayout.DOCK_ROW_SLOTS) list else List(HomeLayout.DOCK_ROW_SLOTS) { "" }
        }
        set(value) { prefs.edit().putString("dock_row2_slots", value.joinToString(",")).apply() }

    var edgePanels: List<EdgePanel>
        get() {
            val raw = prefs.getString("edge_panels", null) ?: return emptyList()
            val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val side = EdgeSide.fromKey(obj.optString("side")) ?: continue
                    val slotsArr = obj.optJSONArray("slots") ?: JSONArray()
                    val slots = buildList {
                        for (s in 0 until HomeLayout.EDGE_SLOTS) {
                            add(slotsArr.optString(s, ""))
                        }
                    }
                    add(
                        EdgePanel(
                            side = side,
                            slots = slots,
                            thicknessDp = obj.optDouble("thicknessDp", 72.0).toFloat(),
                            iconSizeDp = obj.optDouble("iconSizeDp", 40.0).toFloat(),
                            autoHide = runCatching {
                                DockAutoHide.valueOf(obj.optString("autoHide", DockAutoHide.REGULAR.name))
                            }.getOrDefault(DockAutoHide.REGULAR).forUi(),
                            showOverApps = obj.optBoolean("showOverApps", false) &&
                                (side == EdgeSide.LEFT || side == EdgeSide.RIGHT),
                        ),
                    )
                }
            }.distinctBy { it.side }
        }
        set(value) {
            val arr = JSONArray()
            value.forEach { panel ->
                arr.put(
                    JSONObject()
                        .put("side", panel.side.key)
                        .put("slots", JSONArray(panel.slots))
                        .put("thicknessDp", panel.thicknessDp.toDouble())
                        .put("iconSizeDp", panel.iconSizeDp.toDouble())
                        .put("autoHide", panel.autoHide.forUi().name)
                        .put("showOverApps", panel.showOverApps && panel.side != EdgeSide.TOP),
                )
            }
            prefs.edit().putString("edge_panels", arr.toString()).apply()
        }

    var homePages: List<HomePage>
        get() {
            val raw = prefs.getString("home_pages", null) ?: return listOf(HomePage("p0", emptyList()))
            val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return listOf(HomePage("p0", emptyList()))
            if (arr.length() == 0) return listOf(HomePage("p0", emptyList()))
            return buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(readPage(obj))
                }
            }.ifEmpty { listOf(HomePage("p0", emptyList())) }
        }
        set(value) {
            val arr = JSONArray()
            value.forEach { arr.put(writePage(it)) }
            prefs.edit().putString("home_pages", arr.toString()).apply()
        }

    var homePageId: String
        get() = prefs.getString("home_page_id", "") ?: ""
        set(value) { prefs.edit().putString("home_page_id", value).apply() }

    /** Last home-screen page the user was viewing (survives rotation / process death). */
    var lastHomePageId: String
        get() = prefs.getString("last_home_page_id", "") ?: ""
        set(value) { prefs.edit().putString("last_home_page_id", value).apply() }

    var hiddenApps: Set<String>
        get() {
            val raw = prefs.getString("hidden_apps", "") ?: ""
            return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
        set(value) { prefs.edit().putString("hidden_apps", value.joinToString(",")).apply() }

    fun customIconPath(packageName: String): String? = prefs.getString("icon_$packageName", null)

    fun setCustomIconPath(packageName: String, path: String?) {
        val ed = prefs.edit()
        if (path == null) ed.remove("icon_$packageName") else ed.putString("icon_$packageName", path)
        ed.apply()
    }

    private fun readPage(obj: JSONObject): HomePage {
        val itemsArr = obj.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (i in 0 until itemsArr.length()) {
                val it = itemsArr.optJSONObject(i) ?: continue
                add(
                    HomeItem(
                        id = it.optString("id"),
                        kind = it.optString("kind", "app"),
                        widget = it.optString("widget"),
                        packageName = it.optString("packageName"),
                        activityName = it.optString("activityName"),
                        appWidgetId = it.optInt("appWidgetId"),
                        col = it.optInt("col"),
                        row = it.optInt("row"),
                        spanX = it.optDouble("spanX", 1.0).toFloat().coerceAtLeast(0.5f),
                        spanY = it.optDouble("spanY", 1.0).toFloat().coerceAtLeast(0.5f),
                        title = it.optString("title", ""),
                        folderApps = buildList {
                            val apps = it.optJSONArray("folderApps") ?: JSONArray()
                            for (j in 0 until apps.length()) {
                                val a = apps.optJSONObject(j) ?: continue
                                val pkg = a.optString("packageName")
                                val act = a.optString("activityName")
                                if (pkg.isNotBlank() && act.isNotBlank()) add(FolderApp(pkg, act))
                            }
                        },
                        intentUri = it.optString("intentUri", ""),
                        iconPath = it.optString("iconPath", ""),
                    ),
                )
            }
        }
        return HomePage(
            id = obj.optString("id", "p0"),
            items = items,
            kind = HomePageKind.fromKey(obj.optString("kind", "empty")),
            frequentSortTop = obj.optBoolean("frequentSortTop", true),
            newsProviders = buildList {
                val arr = obj.optJSONArray("newsProviders") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (id.isNotBlank()) add(id)
                }
            },
            gridSize = obj.optInt("gridSize", 0),
        )
    }

    private fun writePage(page: HomePage): JSONObject {
        val items = JSONArray()
        page.items.forEach { it ->
            val folderApps = JSONArray()
            it.folderApps.forEach { app ->
                folderApps.put(
                    JSONObject()
                        .put("packageName", app.packageName)
                        .put("activityName", app.activityName),
                )
            }
            items.put(
                JSONObject()
                    .put("id", it.id)
                    .put("kind", it.kind)
                    .put("widget", it.widget)
                    .put("packageName", it.packageName)
                    .put("activityName", it.activityName)
                    .put("appWidgetId", it.appWidgetId)
                    .put("col", it.col)
                    .put("row", it.row)
                    .put("spanX", it.spanX)
                    .put("spanY", it.spanY)
                    .put("title", it.title)
                    .put("folderApps", folderApps)
                    .put("intentUri", it.intentUri)
                    .put("iconPath", it.iconPath),
            )
        }
        val providers = JSONArray()
        page.newsProviders.forEach { providers.put(it) }
        return JSONObject()
            .put("id", page.id)
            .put("kind", page.kind.key)
            .put("frequentSortTop", page.frequentSortTop)
            .put("newsProviders", providers)
            .put("gridSize", page.gridSize)
            .put("items", items)
    }

    var appLaunchStats: Map<String, AppLaunchStats>
        get() {
            val raw = prefs.getString("app_launches", null) ?: return emptyMap()
            val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
            val out = mutableMapOf<String, AppLaunchStats>()
            obj.keys().forEach { pkg ->
                val value = obj.opt(pkg)
                when (value) {
                    is JSONObject -> out[pkg] = AppLaunchStats(
                        count = value.optInt("count", 0),
                        lastUsed = value.optLong("last", 0L),
                    )
                    is Number -> out[pkg] = AppLaunchStats(value.toInt(), 0L)
                }
            }
            return out
        }
        set(value) {
            val obj = JSONObject()
            value.forEach { (pkg, stats) ->
                obj.put(
                    pkg,
                    JSONObject().put("count", stats.count).put("last", stats.lastUsed),
                )
            }
            prefs.edit().putString("app_launches", obj.toString()).apply()
        }

    fun recordAppLaunch(packageName: String) {
        val now = System.currentTimeMillis()
        val current = appLaunchStats.toMutableMap()
        val prev = current[packageName]
        current[packageName] = AppLaunchStats((prev?.count ?: 0) + 1, now)
        appLaunchStats = current
    }

    fun touchAppUsed(packageName: String) {
        if (packageName.isBlank()) return
        val now = System.currentTimeMillis()
        val current = appLaunchStats.toMutableMap()
        val prev = current[packageName]
        current[packageName] = AppLaunchStats(prev?.count ?: 0, now)
        appLaunchStats = current
    }

    fun notes(): List<SavedNote> = readArray("notes") { noteFromJson(it) }

    fun reminders(): List<SavedReminder> = readArray("reminders") { obj ->
        SavedReminder(
            id = obj.getString("id"),
            text = obj.getString("text"),
            createdAt = obj.optLong("createdAt"),
        )
    }

    fun events(): List<SavedEvent> = readArray("events") { obj ->
        SavedEvent(
            id = obj.getString("id"),
            title = obj.getString("title"),
            whenLabel = obj.optString("whenLabel"),
            createdAt = obj.optLong("createdAt"),
        )
    }

    fun addNote(text: String, floating: Boolean, collapsed: Boolean = floating): SavedNote {
        val note = SavedNote(
            id = System.currentTimeMillis().toString(),
            text = text,
            floating = floating,
            createdAt = System.currentTimeMillis(),
            collapsed = collapsed,
        )
        persistNotes(notes() + note)
        return note
    }

    fun addReminder(text: String): SavedReminder {
        val item = SavedReminder(
            id = System.currentTimeMillis().toString(),
            text = text,
            createdAt = System.currentTimeMillis(),
        )
        writeArray("reminders", reminders() + item) { r ->
            JSONObject().put("id", r.id).put("text", r.text).put("createdAt", r.createdAt)
        }
        return item
    }

    fun addEvent(title: String, whenLabel: String): SavedEvent {
        val item = SavedEvent(
            id = System.currentTimeMillis().toString(),
            title = title,
            whenLabel = whenLabel,
            createdAt = System.currentTimeMillis(),
        )
        writeArray("events", events() + item) { e ->
            JSONObject()
                .put("id", e.id)
                .put("title", e.title)
                .put("whenLabel", e.whenLabel)
                .put("createdAt", e.createdAt)
        }
        return item
    }

    fun updateNote(id: String, transform: (SavedNote) -> SavedNote) {
        persistNotes(notes().map { if (it.id == id) transform(it) else it })
    }

    fun updateNoteText(id: String, text: String) {
        updateNote(id) { it.copy(text = text) }
    }

    fun dismissFloating(id: String) {
        updateNote(id) { it.copy(floating = false) }
    }

    private fun persistNotes(items: List<SavedNote>) {
        writeArray("notes", items) { noteToJson(it) }
    }

    private fun noteFromJson(obj: JSONObject): SavedNote {
        val images = obj.optJSONArray("images")
        val paths = buildList {
            if (images != null) {
                for (i in 0 until images.length()) {
                    val path = images.optString(i)
                    if (path.isNotBlank()) add(path)
                }
            }
        }
        val edge = obj.optString("edge").ifBlank { null }
        return SavedNote(
            id = obj.getString("id"),
            text = obj.getString("text"),
            floating = obj.optBoolean("floating"),
            createdAt = obj.optLong("createdAt"),
            title = obj.optString("title"),
            collapsed = if (obj.has("collapsed")) obj.optBoolean("collapsed") else true,
            x = if (obj.has("x") && !obj.isNull("x")) obj.optInt("x") else null,
            y = if (obj.has("y") && !obj.isNull("y")) obj.optInt("y") else null,
            pinnedEdge = edge,
            imagePaths = paths,
        )
    }

    private fun noteToJson(n: SavedNote): JSONObject {
        val images = JSONArray()
        n.imagePaths.forEach { images.put(it) }
        return JSONObject()
            .put("id", n.id)
            .put("text", n.text)
            .put("floating", n.floating)
            .put("createdAt", n.createdAt)
            .put("title", n.title)
            .put("collapsed", n.collapsed)
            .put("x", n.x ?: JSONObject.NULL)
            .put("y", n.y ?: JSONObject.NULL)
            .put("edge", n.pinnedEdge.orEmpty())
            .put("images", images)
    }

    private fun <T> readArray(key: String, map: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (i in 0 until arr.length()) {
                runCatching { add(map(arr.getJSONObject(i))) }
            }
        }
    }

    private fun <T> writeArray(key: String, items: List<T>, map: (T) -> JSONObject) {
        val arr = JSONArray()
        items.takeLast(80).forEach { arr.put(map(it)) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    var appearanceSettings: AppearanceSettings
        get() = AppearanceSettings.fromJson(prefs.getString("appearance", null))
        set(value) { prefs.edit().putString("appearance", value.toJson()).apply() }

    var platformProbeAt: Long
        get() = prefs.getLong("platform_probe_at", 0L)
        set(value) { prefs.edit().putLong("platform_probe_at", value).apply() }

    var platformOemFamily: String?
        get() = prefs.getString("platform_oem_family", null)
        set(value) { prefs.edit().putString("platform_oem_family", value).apply() }

    fun loadPlatformCapabilities(): Map<String, String> {
        val raw = prefs.getString("platform_caps", null) ?: return emptyMap()
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
        return buildMap {
            obj.keys().forEach { key ->
                put(key, obj.optString(key, "UNKNOWN"))
            }
        }
    }

    fun savePlatformCapabilities(family: String, caps: Map<String, String>, probedAt: Long) {
        platformOemFamily = family
        platformProbeAt = probedAt
        val obj = JSONObject()
        caps.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("platform_caps", obj.toString()).apply()
    }

    var actionBoxCatalog: String
        get() = prefs.getString("action_box_catalog", "") ?: ""
        set(value) { prefs.edit().putString("action_box_catalog", value).apply() }

    var userCommandAliases: com.madlauncher.action.UserAliasCatalog
        get() = com.madlauncher.action.UserAliasCatalog.fromJson(prefs.getString("user_command_aliases", null))
        set(value) { prefs.edit().putString("user_command_aliases", value.toJson()).apply() }

    var userShortcuts: List<com.madlauncher.action.UserShortcut>
        get() = com.madlauncher.action.UserShortcuts.fromJson(prefs.getString("user_shortcuts", null))
        set(value) { prefs.edit().putString("user_shortcuts", com.madlauncher.action.UserShortcuts.toJson(value)).apply() }

    var disabledBuiltInCommandAliases: Set<String>
        get() {
            val raw = prefs.getString("disabled_built_in_aliases", null) ?: return emptySet()
            val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptySet()
            return buildSet {
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i).trim().lowercase()
                    if (s.isNotEmpty()) add(s)
                }
            }
        }
        set(value) {
            val arr = JSONArray()
            value.forEach { arr.put(it) }
            prefs.edit().putString("disabled_built_in_aliases", arr.toString()).apply()
        }

    var compatibilityReportSubmitted: Boolean
        get() = prefs.getBoolean("compat_report_submitted", false)
        set(value) { prefs.edit().putBoolean("compat_report_submitted", value).apply() }

    var languageEnglishOnlyMigrated: Boolean
        get() = prefs.getBoolean("language_english_only_migrated", false)
        set(value) { prefs.edit().putBoolean("language_english_only_migrated", value).apply() }

    var recipientHistory: List<StoredRecipient>
        get() {
            val raw = prefs.getString("recipient_history", null) ?: return emptyList()
            val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val pkg = o.optString("packageName", "")
                    val conv = o.optString("conversationId", "")
                    val name = o.optString("name", "")
                    if (pkg.isBlank() || conv.isBlank() || name.isBlank()) continue
                    add(
                        StoredRecipient(
                            packageName = pkg,
                            conversationId = conv,
                            name = name,
                            phone = o.optString("phone").takeIf { it.isNotBlank() },
                            email = o.optString("email").takeIf { it.isNotBlank() },
                            lastActiveAt = o.optLong("lastActiveAt", 0L),
                        ),
                    )
                }
            }
        }
        set(value) {
            val arr = JSONArray()
            value.forEach { r ->
                arr.put(
                    JSONObject()
                        .put("packageName", r.packageName)
                        .put("conversationId", r.conversationId)
                        .put("name", r.name)
                        .put("phone", r.phone.orEmpty())
                        .put("email", r.email.orEmpty())
                        .put("lastActiveAt", r.lastActiveAt),
                )
            }
            prefs.edit().putString("recipient_history", arr.toString()).apply()
        }
}
