package com.madlauncher.data

import androidx.compose.ui.graphics.Color
import com.madlauncher.action.SocialBrowserApps
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class PaletteId { NORD, NORD_LIGHT, WALLPAPER, MATERIAL, STEAMPUNK }

enum class IconShapeId {
    /** Keep each app’s own silhouette (Edge / BlueStacks style) — no MAD mask or fill plate. */
    ADAPTIVE,
    ROUNDED_SQUARE,
    SQUARE,
    SQUIRCLE,
    CIRCLE,
    TEARDROP,
    STAR,
    TRIANGLE,
    HEXAGON,
    COMET,
    CRESCENT,
    SUN,
    /** System adaptive icon: background fill, glyph in the safe zone, clipped once. */
    ADAPTIVE,
}

/** Action Box input-pill typeface (ABS editor). */
enum class ActionBoxFontId {
    DEFAULT,
    SERIF,
    MONO,
}

/** Where Settings / Mic / Close sit on the Action Box input pill. */
enum class ActionBoxButtonsId {
    /** Leading icon · field · mic · settings · close */
    TRAILING,
    /** Settings · close · mic · field · leading icon flipped to end */
    LEADING,
    /** Mic left of field; settings + close on the right */
    SPLIT,
}

enum class PageEffectId { SLIDE, FADE, STACK, CUBE, DEPTH, BOOK }

enum class DockAutoHide {
    REGULAR,
    INTELLIGENT,
    AUTOMATIC,
    ;

    /** Legacy Intelligent is treated as always-show. */
    fun forUi(): DockAutoHide = if (this == AUTOMATIC) AUTOMATIC else REGULAR

    val isAutoHide: Boolean get() = this == AUTOMATIC
}

enum class DrawerLayoutId {
    LIST,
    GRID_4,
    GRID_5,
    GRID_6,
    ;

    val columns: Int
        get() = when (this) {
            LIST -> 0
            GRID_4 -> 4
            GRID_5 -> 5
            GRID_6 -> 6
        }
}

enum class DrawerSortId {
    NAME_AZ,
    NAME_ZA,
    MOST_USED,
}

enum class FolderViewId {
    GRID,
    STACK,
    BADGE,
}

/** Notification badge drawn on app icons. */
enum class IconBadgeStyle {
    /** Small unread indicator with no count. */
    DOT,
    /** Dot-style pill that includes the unread count. */
    NUMBER,
}

enum class NavShortcutMode { CLASSIC, NAV_BAR }

enum class NavShortcutKind { NOTIFICATIONS, SYSTEM_APPS, QUICK_SETTINGS }

enum class GestureId {
    SWIPE_UP,
    SWIPE_DOWN,
    DOUBLE_TAP,
    TWO_FINGER_UP,
    TWO_FINGER_DOWN,
    PINCH_IN,
    PINCH_OUT,
    TAP_HOME,
}

enum class GestureActionId {
    NONE,
    APP_DRAWER,
    SEARCH,
    NOTIFICATIONS,
    SYSTEM_APPS,
    QUICK_SETTINGS,
    RECENTS,
    CUSTOMIZE,
    LOCK_SCREEN,
    HIDDEN_APPS,
}

data class NavShortcutSlot(
    val kind: NavShortcutKind,
    val placement: NavShortcutPlacement,
)

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: PaletteId = PaletteId.NORD,
    val accentArgb: Long = 0xFF81A1C1,
    /** ActionBox dock pill + field background. */
    val actionBoxPillArgb: Long = 0xFF81A1C1,
    /** Text / icons inside the ActionBox pill. */
    val actionBoxTextArgb: Long = 0xFF81A1C1,
    /** Input-pill text scale (0.75–1.5). */
    val actionBoxFontScale: Float = 1f,
    /** Input-pill overall size (padding / min height). */
    val actionBoxPillScale: Float = 1f,
    val actionBoxFont: ActionBoxFontId = ActionBoxFontId.DEFAULT,
    val actionBoxButtons: ActionBoxButtonsId = ActionBoxButtonsId.TRAILING,
    val windowAlpha: Float = 0.97f,
    val drawerAlpha: Float = 0.96f,
    val dockAlpha: Float = 0.88f,
    val widgetAlpha: Float = 0.88f,
    val newsAlpha: Float = 0.92f,
    /** Opacity of each app's adaptive-icon background layer. Glyph stays solid. */
    val iconBgAlpha: Float = 1f,
    val blurEnabled: Boolean = false,
    val blurAllScreens: Boolean = false,
    val iconShape: IconShapeId = IconShapeId.ROUNDED_SQUARE,
    val iconPackPackage: String = "",
    val fontPath: String = "",
    val pageEffect: PageEffectId = PageEffectId.SLIDE,
    /** When true, swiping past the last home page wraps to the first (and vice versa). */
    val infiniteHomePages: Boolean = true,
    val navShortcutMode: NavShortcutMode = NavShortcutMode.NAV_BAR,
    val navShortcutSlots: List<NavShortcutSlot> = defaultNavSlots(),
    val gestures: Map<GestureId, GestureActionId> = defaultGestures(),
    /** When false (default), home-screen gestures are disabled in landscape so the grid can scroll freely. */
    val landscapeGesturesEnabled: Boolean = false,
    /**
     * Phone landscape: keep the portrait home/dock layout and turn icons, widgets, and dock
     * contents upright. Off by default (tablets reflow normally).
     */
    val landscapeRotateContents: Boolean = false,
    /** Default square grid size (3–5) for empty home pages. */
    val homeGridSize: Int = HomeLayout.DEFAULT_GRID,
    val showDockApps: Boolean = true,
    val showDockActionBox: Boolean = true,
    val dockHeightDp: Float = 68f,
    val dockIconSizeDp: Float = 50f,
    val homeIconSizeDp: Float = 56f,
    /** Caption under home, dock, drawer, and folder icons. */
    val iconLabelSizeSp: Float = 11f,
    val dockAutoHide: DockAutoHide = DockAutoHide.REGULAR,
    /** Swipe up on the dock (not home) slides the dock up and shows an extra app row underneath. */
    val dockSecondRow: Boolean = false,
    val drawerLayout: DrawerLayoutId = DrawerLayoutId.LIST,
    val drawerSort: DrawerSortId = DrawerSortId.NAME_AZ,
    val folderView: FolderViewId = FolderViewId.GRID,
    val iconBadgeStyle: IconBadgeStyle = IconBadgeStyle.NUMBER,
    val languageOverlay: String = "",
    /** MAD Messages / System & apps / Quick Settings windows. Off = system shade. */
    val experimentalMadWindows: Boolean = false,
    /** ActionBox pill opens a command picker. /h is always edited under ActionBox Settings. */
    val experimentalHelpWindow: Boolean = false,
    /** Nav-bar overlay shortcuts. Off = no overlays and no MAD intercept of nav taps. */
    val experimentalNavShortcuts: Boolean = false,
    /** MAD Recents window. Off = system Overview / Recents. */
    val experimentalMadRecents: Boolean = false,
    /** Unread app icons inside the MAD Glance (clock) widget. Off = clock/date/weather only. */
    val experimentalWidgetBadges: Boolean = false,
    /**
     * Catalog ids explicitly turned on. Apps whose default is already on are not listed here.
     * Apps that are on share one browser tab.
     */
    val experimentalSocialBrowserApps: Set<String> = emptySet(),
    /** Catalog ids explicitly turned off, including messaging apps that default on. */
    val experimentalSocialBrowserOff: Set<String> = emptySet(),
    /** Per-app Off / On / : post / @ msg. Missing ids use [SocialBrowserApps.defaultMode]. */
    val experimentalSocialBrowserMode: Map<String, SocialBrowserApps.BrowserMode> = emptyMap(),
    /** Apps added from Experimental → Open in browser. Default on. */
    val customBrowserApps: List<SocialBrowserApps.Custom> = emptyList(),
    /** Action Box "Search web" engine. Missing JSON → DuckDuckGo. */
    val webSearchEngine: WebSearchEngine = WebSearchEngine.DUCKDUCKGO,
    /** DuckDuckGo Instant Answer cards. Off = on-device calc/conversions only. */
    val instantAnswersEnabled: Boolean = true,
    /** First-run Action Box How it works cards. */
    val actionBoxHowToSeen: Boolean = false,
) {
    val accentColor: Color get() = Color(accentArgb)
    val actionBoxPillColor: Color get() = Color(actionBoxPillArgb)
    val actionBoxTextColor: Color get() = Color(actionBoxTextArgb)
    /** Web Instant Answer cards only when DuckDuckGo is the search engine. */
    val instantAnswerCardsEnabled: Boolean
        get() = instantAnswersEnabled && webSearchEngine == WebSearchEngine.DUCKDUCKGO

    companion object {
        fun defaultNavSlots(): List<NavShortcutSlot> = listOf(
            NavShortcutSlot(NavShortcutKind.NOTIFICATIONS, NavShortcutPlacement.NAV_LEFT),
            NavShortcutSlot(NavShortcutKind.QUICK_SETTINGS, NavShortcutPlacement.NAV_RIGHT),
            NavShortcutSlot(NavShortcutKind.SYSTEM_APPS, NavShortcutPlacement.SHADE),
        )

        fun defaultGestures(): Map<GestureId, GestureActionId> = mapOf(
            GestureId.SWIPE_UP to GestureActionId.APP_DRAWER,
            GestureId.SWIPE_DOWN to GestureActionId.SEARCH,
            GestureId.DOUBLE_TAP to GestureActionId.NONE,
            GestureId.TWO_FINGER_UP to GestureActionId.NONE,
            GestureId.TWO_FINGER_DOWN to GestureActionId.NONE,
            GestureId.PINCH_IN to GestureActionId.CUSTOMIZE,
            GestureId.PINCH_OUT to GestureActionId.RECENTS,
            GestureId.TAP_HOME to GestureActionId.NONE,
        )

        fun normalizePalette(id: PaletteId): PaletteId =
            if (id == PaletteId.NORD_LIGHT) PaletteId.NORD else id

        fun fromJson(raw: String?): AppearanceSettings {
            if (raw.isNullOrBlank()) return AppearanceSettings()
            val o = runCatching { JSONObject(raw) }.getOrNull() ?: return AppearanceSettings()
            val slots = buildList {
                val arr = o.optJSONArray("navSlots") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val s = arr.optJSONObject(i) ?: continue
                    val kind = runCatching { NavShortcutKind.valueOf(s.optString("kind")) }.getOrNull() ?: continue
                    val placement = when {
                        s.has("placement") ->
                            runCatching { NavShortcutPlacement.valueOf(s.optString("placement")) }.getOrNull()
                        s.has("side") -> when (s.optString("side")) {
                            "LEFT" -> NavShortcutPlacement.NAV_LEFT
                            "RIGHT" -> NavShortcutPlacement.NAV_RIGHT
                            else -> null
                        }
                        else -> null
                    } ?: continue
                    if (placement.isActive) add(NavShortcutSlot(kind, placement))
                }
            }
            val gestures = defaultGestures().toMutableMap()
            o.optJSONObject("gestures")?.let { g ->
                GestureId.entries.forEach { id ->
                    val action = g.optString(id.name, "")
                    if (action.isNotBlank()) {
                        runCatching { GestureActionId.valueOf(action) }.getOrNull()?.let { gestures[id] = it }
                    }
                }
            }
            val engine = enumOr(o.optString("webSearchEngine"), WebSearchEngine.DUCKDUCKGO)
            return AppearanceSettings(
                themeMode = ThemeMode.SYSTEM,
                palette = normalizePalette(enumOr(o.optString("palette"), PaletteId.NORD)),
                accentArgb = o.optLong("accentArgb", 0xFF81A1C1),
                actionBoxPillArgb = when {
                    o.has("actionBoxPillArgb") -> o.optLong("actionBoxPillArgb")
                    else -> o.optLong("accentArgb", 0xFF81A1C1)
                },
                actionBoxTextArgb = when {
                    o.has("actionBoxTextArgb") -> o.optLong("actionBoxTextArgb")
                    else -> o.optLong("accentArgb", 0xFF81A1C1)
                },
                actionBoxFontScale = o.optDouble("actionBoxFontScale", 1.0).toFloat().coerceIn(0.75f, 1.5f),
                actionBoxPillScale = o.optDouble("actionBoxPillScale", 1.0).toFloat().coerceIn(0.75f, 1.5f),
                actionBoxFont = enumOr(o.optString("actionBoxFont"), ActionBoxFontId.DEFAULT),
                actionBoxButtons = enumOr(o.optString("actionBoxButtons"), ActionBoxButtonsId.TRAILING),
                windowAlpha = o.optDouble("windowAlpha", 0.97).toFloat(),
                drawerAlpha = o.optDouble("drawerAlpha", 0.96).toFloat(),
                dockAlpha = o.optDouble("dockAlpha", 0.88).toFloat(),
                widgetAlpha = o.optDouble("widgetAlpha", 0.88).toFloat(),
                newsAlpha = o.optDouble("newsAlpha", 0.92).toFloat(),
                iconBgAlpha = run {
                    // iconBgArgb meant dock-plate color; that era's alpha was not the icon fill.
                    if (o.has("iconBgArgb")) 1f
                    else o.optDouble("iconBgAlpha", 1.0).toFloat().coerceIn(0f, 1f)
                },
                blurEnabled = o.optBoolean("blurEnabled", false),
                blurAllScreens = o.optBoolean("blurAllScreens", false),
                iconShape = enumOr(o.optString("iconShape"), IconShapeId.ROUNDED_SQUARE),
                iconPackPackage = o.optString("iconPackPackage", ""),
                fontPath = o.optString("fontPath", ""),
                pageEffect = enumOr(o.optString("pageEffect"), PageEffectId.SLIDE),
                infiniteHomePages = o.optBoolean("infiniteHomePages", true),
                navShortcutMode = enumOr(o.optString("navShortcutMode"), NavShortcutMode.NAV_BAR),
                navShortcutSlots = NavShortcutPlacements.normalize(slots.ifEmpty { defaultNavSlots() }),
                gestures = gestures,
                landscapeGesturesEnabled = o.optBoolean("landscapeGesturesEnabled", false),
                landscapeRotateContents = o.optBoolean("landscapeRotateContents", false),
                homeGridSize = HomeLayout.normalizeGridSize(
                    o.optInt("homeGridSize", HomeLayout.DEFAULT_GRID),
                ),
                showDockApps = o.optBoolean("showDockApps", true),
                showDockActionBox = o.optBoolean("showDockActionBox", true),
                dockHeightDp = o.optDouble("dockHeightDp", 68.0).toFloat(),
                dockIconSizeDp = o.optDouble("dockIconSizeDp", 50.0).toFloat(),
                homeIconSizeDp = o.optDouble("homeIconSizeDp", 56.0).toFloat(),
                iconLabelSizeSp = o.optDouble("iconLabelSizeSp", 11.0).toFloat(),
                dockAutoHide = enumOr(o.optString("dockAutoHide"), DockAutoHide.REGULAR).forUi(),
                dockSecondRow = o.optBoolean("dockSecondRow", false),
                drawerLayout = enumOr(o.optString("drawerLayout"), DrawerLayoutId.LIST),
                drawerSort = enumOr(o.optString("drawerSort"), DrawerSortId.NAME_AZ),
                folderView = enumOr(o.optString("folderView"), FolderViewId.GRID),
                iconBadgeStyle = enumOr(o.optString("iconBadgeStyle"), IconBadgeStyle.NUMBER),
                languageOverlay = LanguageCodes.migrateFileName(o.optString("languageOverlay", "")),
                experimentalMadWindows = o.optBoolean("experimentalMadWindows", false),
                experimentalHelpWindow = o.optBoolean("experimentalHelpWindow", false),
                experimentalNavShortcuts = o.optBoolean("experimentalNavShortcuts", false),
                experimentalMadRecents = o.optBoolean("experimentalMadRecents", false),
                experimentalWidgetBadges = o.optBoolean("experimentalWidgetBadges", false),
                experimentalSocialBrowserApps = socialBrowserApps(o),
                experimentalSocialBrowserOff = socialBrowserOff(o),
                experimentalSocialBrowserMode = socialBrowserMode(o),
                customBrowserApps = customBrowserApps(o),
                webSearchEngine = engine,
                instantAnswersEnabled = o.optBoolean("instantAnswersEnabled", true) &&
                    engine == WebSearchEngine.DUCKDUCKGO,
                actionBoxHowToSeen = o.optBoolean("actionBoxHowToSeen", false),
            )
        }

        private fun socialBrowserApps(o: JSONObject): Set<String> {
            if (o.has("experimentalSocialBrowserApps")) {
                val arr = o.optJSONArray("experimentalSocialBrowserApps") ?: return emptySet()
                return buildSet {
                    for (i in 0 until arr.length()) {
                        val id = arr.optString(i)
                        if (id in SocialBrowserApps.ids) add(id)
                    }
                }
            }
            // Older builds stored one switch for every broken social app.
            return if (o.optBoolean("experimentalSocialBrowser", false)) {
                SocialBrowserApps.socialIds
            } else {
                emptySet()
            }
        }

        private fun socialBrowserOff(o: JSONObject): Set<String> {
            if (!o.has("experimentalSocialBrowserOff")) return emptySet()
            val arr = o.optJSONArray("experimentalSocialBrowserOff") ?: return emptySet()
            return buildSet {
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (id in SocialBrowserApps.ids) add(id)
                }
            }
        }

        private fun socialBrowserMode(o: JSONObject): Map<String, SocialBrowserApps.BrowserMode> {
            val obj = o.optJSONObject("experimentalSocialBrowserMode") ?: return emptyMap()
            return buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    if (id !in SocialBrowserApps.ids) continue
                    val mode = SocialBrowserApps.parseMode(obj.optString(id)) ?: continue
                    put(id, mode)
                }
            }
        }

        private fun customBrowserApps(o: JSONObject): List<SocialBrowserApps.Custom> {
            val arr = o.optJSONArray("customBrowserApps") ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val pkg = item.optString("packageName").trim()
                    val url = item.optString("url").trim()
                    if (pkg.isBlank() || !(url.startsWith("https://") || url.startsWith("http://"))) continue
                    if (SocialBrowserApps.idFor(pkg) != null) continue
                    val label = item.optString("label").ifBlank { pkg }
                    val mode = SocialBrowserApps.parseMode(item.optString("mode"))
                        ?: if (item.optBoolean("enabled", true)) {
                            SocialBrowserApps.BrowserMode.ON
                        } else {
                            SocialBrowserApps.BrowserMode.OFF
                        }
                    add(SocialBrowserApps.Custom(pkg, label, url, mode))
                }
            }
        }

        private inline fun <reified T : Enum<T>> enumOr(raw: String, fallback: T): T {
            if (raw.isBlank()) return fallback
            return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
        }
    }

    fun toJson(): String {
        val slots = JSONArray()
        navShortcutSlots.forEach { slot ->
            slots.put(
                JSONObject()
                    .put("kind", slot.kind.name)
                    .put("placement", slot.placement.name),
            )
        }
        val gesturesJson = JSONObject()
        gestures.forEach { (id, action) -> gesturesJson.put(id.name, action.name) }
        return JSONObject()
            .put("themeMode", themeMode.name)
            .put("palette", palette.name)
            .put("accentArgb", accentArgb)
            .put("actionBoxPillArgb", actionBoxPillArgb)
            .put("actionBoxTextArgb", actionBoxTextArgb)
            .put("actionBoxFontScale", actionBoxFontScale.toDouble())
            .put("actionBoxPillScale", actionBoxPillScale.toDouble())
            .put("actionBoxFont", actionBoxFont.name)
            .put("actionBoxButtons", actionBoxButtons.name)
            .put("windowAlpha", windowAlpha.toDouble())
            .put("drawerAlpha", drawerAlpha.toDouble())
            .put("dockAlpha", dockAlpha.toDouble())
            .put("widgetAlpha", widgetAlpha.toDouble())
            .put("newsAlpha", newsAlpha.toDouble())
            .put("iconBgAlpha", iconBgAlpha.toDouble())
            .put("blurEnabled", blurEnabled)
            .put("blurAllScreens", blurAllScreens)
            .put("iconShape", iconShape.name)
            .put("iconPackPackage", iconPackPackage)
            .put("fontPath", fontPath)
            .put("pageEffect", pageEffect.name)
            .put("infiniteHomePages", infiniteHomePages)
            .put("navShortcutMode", navShortcutMode.name)
            .put("navSlots", slots)
            .put("gestures", gesturesJson)
            .put("landscapeGesturesEnabled", landscapeGesturesEnabled)
            .put("landscapeRotateContents", landscapeRotateContents)
            .put("homeGridSize", homeGridSize)
            .put("showDockApps", showDockApps)
            .put("showDockActionBox", showDockActionBox)
            .put("dockHeightDp", dockHeightDp.toDouble())
            .put("dockIconSizeDp", dockIconSizeDp.toDouble())
            .put("homeIconSizeDp", homeIconSizeDp.toDouble())
            .put("iconLabelSizeSp", iconLabelSizeSp.toDouble())
            .put("dockAutoHide", dockAutoHide.forUi().name)
            .put("dockSecondRow", dockSecondRow)
            .put("drawerLayout", drawerLayout.name)
            .put("drawerSort", drawerSort.name)
            .put("folderView", folderView.name)
            .put("iconBadgeStyle", iconBadgeStyle.name)
            .put("languageOverlay", languageOverlay)
            .put("experimentalMadWindows", experimentalMadWindows)
            .put("experimentalHelpWindow", experimentalHelpWindow)
            .put("experimentalNavShortcuts", experimentalNavShortcuts)
            .put("experimentalMadRecents", experimentalMadRecents)
            .put("experimentalWidgetBadges", experimentalWidgetBadges)
            .put("experimentalSocialBrowserApps", JSONArray().apply {
                experimentalSocialBrowserApps.forEach { put(it) }
            })
            .put("experimentalSocialBrowserOff", JSONArray().apply {
                experimentalSocialBrowserOff.forEach { put(it) }
            })
            .put("experimentalSocialBrowserMode", JSONObject().apply {
                experimentalSocialBrowserMode.forEach { (id, mode) -> put(id, mode.name.lowercase()) }
            })
            .put("customBrowserApps", JSONArray().apply {
                customBrowserApps.forEach { app ->
                    put(JSONObject().apply {
                        put("packageName", app.packageName)
                        put("label", app.label)
                        put("url", app.url)
                        put("mode", app.mode.name.lowercase())
                        put("enabled", app.enabled)
                    })
                }
            })
            .put("webSearchEngine", webSearchEngine.name)
            .put("instantAnswersEnabled", instantAnswersEnabled)
            .put("actionBoxHowToSeen", actionBoxHowToSeen)
            .toString()
    }
}
