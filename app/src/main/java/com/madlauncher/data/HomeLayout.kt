package com.madlauncher.data

data class FolderApp(
    val packageName: String,
    val activityName: String,
) {
    val id: String get() = "$packageName/$activityName"

    companion object {
        fun from(app: LaunchableApp) = FolderApp(app.packageName, app.activityName)
    }
}

data class HomeItem(
    val id: String,
    val kind: String,
    val widget: String = "",
    val packageName: String = "",
    val activityName: String = "",
    val appWidgetId: Int = 0,
    val col: Int,
    val row: Int,
    val spanX: Float = 1f,
    val spanY: Float = 1f,
    val title: String = "",
    val folderApps: List<FolderApp> = emptyList(),
    /** Intent URI for legacy launcher shortcuts (Shortcut Maker, etc.). */
    val intentUri: String = "",
    /** Optional custom icon file path for shortcuts. */
    val iconPath: String = "",
) {
    val isApp: Boolean get() = kind == "app"
    val isFolder: Boolean get() = kind == "folder"
    val isAppWidget: Boolean get() = kind == "appwidget" && appWidgetId != 0
    /** Pinned / installed home shortcut (not a full app activity). */
    val isShortcut: Boolean get() = kind == "shortcut"
    fun occupies(c: Int, r: Int): Boolean {
        return c + 1 > col && c < col + spanX && r + 1 > row && r < row + spanY
    }
}

data class HomePage(
    val id: String,
    val items: List<HomeItem>,
    val kind: HomePageKind = HomePageKind.EMPTY,
    val frequentSortTop: Boolean = true,
    val newsProviders: List<String> = emptyList(),
    /** 0 = use appearance default; otherwise square grid size (3–5) for empty pages. */
    val gridSize: Int = 0,
)

enum class HomePageKind {
    EMPTY,
    FREQUENT,
    NEWS;

    val key: String get() = name.lowercase()

    val label: String
        get() = when (this) {
            EMPTY -> "Empty page"
            FREQUENT -> "Most-used apps"
            NEWS -> "News"
        }

    companion object {
        fun fromKey(raw: String): HomePageKind =
            entries.find { it.key == raw.lowercase() } ?: EMPTY
    }
}

data class AppHold(
    val app: LaunchableApp,
    val from: String,
    val itemId: String? = null,
    val dockIndex: Int? = null,
    val edgeSide: EdgeSide? = null,
    /** Home page the icon was picked up from — not [LauncherViewModel.homePageIndex] after a page flip. */
    val fromPageIndex: Int? = null,
)

enum class EdgeSide {
    LEFT, RIGHT, TOP;
    val key: String get() = name.lowercase()
    companion object {
        fun fromKey(raw: String): EdgeSide? = entries.find { it.key == raw.lowercase() }
    }
}

data class EdgePanel(
    val side: EdgeSide,
    val slots: List<String> = List(HomeLayout.EDGE_SLOTS) { "" },
    val thicknessDp: Float = 72f,
    val iconSizeDp: Float = 40f,
    val autoHide: DockAutoHide = DockAutoHide.REGULAR,
    /** Left/right only: keep this dock (or its pull bar) over other apps. */
    val showOverApps: Boolean = false,
)

object HomeLayout {
    const val MIN_GRID = 3
    const val MAX_GRID = 5
    const val DEFAULT_GRID = 4
    /** Upper bound for placement scans; per-page grid may be smaller. */
    const val COLS = MAX_GRID
    const val ROWS = MAX_GRID
    const val MIN_SPAN = 0.5f
    const val EDGE_SLOTS = 6
    const val FOLDER_MAX = 12
    const val DOCK_ROW_SLOTS = 5
    val defaultDock = listOf("phone", "browser", "actions", "apps", "launcher_settings")
    val protectedDock = setOf("actions", "apps")
    val specialDock = setOf("actions", "apps", "launcher_settings", "phone", "browser", "settings")

    fun gridSizeFor(page: HomePage, defaultGridSize: Int = DEFAULT_GRID): Int {
        return if (page.gridSize in MIN_GRID..MAX_GRID) {
            page.gridSize
        } else {
            defaultGridSize.coerceIn(MIN_GRID, MAX_GRID)
        }
    }

    fun appSlot(app: LaunchableApp): String {
        if (app.packageName in specialDock) return app.packageName
        return "app:${app.packageName}/${app.activityName}"
    }

    fun parseAppSlot(slot: String): Pair<String, String>? {
        if (!slot.startsWith("app:")) return null
        val body = slot.removePrefix("app:")
        val slash = body.indexOf('/')
        if (slash <= 0) return null
        return body.substring(0, slash) to body.substring(slash + 1)
    }
}
