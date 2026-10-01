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
    /** 0 = use appearance default; otherwise square grid size (4–6) for empty pages. */
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
    const val MIN_GRID = 4
    const val MAX_GRID = 6
    const val DEFAULT_GRID = 5
    val PAGE_GRID_SIZES = listOf(4, 5, 6)
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
        return normalizeGridSize(page.gridSize, defaultGridSize)
    }

    fun frequentCapacity(gridSize: Int, defaultGridSize: Int = DEFAULT_GRID): Int {
        val grid = normalizeGridSize(gridSize, defaultGridSize)
        return grid * grid
    }

    fun normalizeGridSize(size: Int, defaultGridSize: Int = DEFAULT_GRID): Int {
        val mapped = if (size == 3) MAX_GRID else size
        return if (mapped in MIN_GRID..MAX_GRID) {
            mapped
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

    /**
     * Fit every item into [gridSize]. Icons that still fit stay put; the rest
     * pack into the first free cells (widgets shrink if they must).
     */
    /** Closest free 1×1 cell to [nearCol],[nearRow], or null when the page is full. */
    fun firstEmptyCell(
        items: List<HomeItem>,
        gridSize: Int,
        nearCol: Int = 0,
        nearRow: Int = 0,
    ): Pair<Int, Int>? {
        val grid = gridSize.coerceIn(MIN_GRID, MAX_GRID)
        var best: Pair<Int, Int>? = null
        var bestDist = Int.MAX_VALUE
        for (row in 0 until grid) {
            for (col in 0 until grid) {
                if (items.any { it.occupies(col, row) }) continue
                val dx = col - nearCol
                val dy = row - nearRow
                val dist = dx * dx + dy * dy
                if (dist < bestDist) {
                    bestDist = dist
                    best = col to row
                }
            }
        }
        return best
    }

    fun itemFitsGrid(item: HomeItem, gridSize: Int): Boolean {
        val grid = gridSize.coerceIn(MIN_GRID, MAX_GRID)
        val spanX = item.spanX.toInt().coerceAtLeast(1)
        val spanY = item.spanY.toInt().coerceAtLeast(1)
        return item.col >= 0 && item.row >= 0 &&
            item.col + spanX <= grid &&
            item.row + spanY <= grid
    }

    fun reflowItemsToGrid(items: List<HomeItem>, gridSize: Int): List<HomeItem> {
        return packItems(items, gridSize.coerceIn(MIN_GRID, MAX_GRID))
    }

    /** Pull icons that landed past the last row/column back onto the page. */
    fun recoverPages(pages: List<HomePage>, defaultGridSize: Int = DEFAULT_GRID): List<HomePage> {
        return pages.map { page ->
            if (page.kind != HomePageKind.EMPTY) page
            else {
                val grid = gridSizeFor(page, defaultGridSize)
                if (page.items.all { itemFitsGrid(it, grid) }) page
                else page.copy(items = reflowItemsToGrid(page.items, grid))
            }
        }
    }

    /**
     * Re-place [items] around a reserved rectangle (the growing widget).
     * Returns null if any item cannot be kept on the page.
     */
    fun reflowAroundRect(
        items: List<HomeItem>,
        gridSize: Int,
        blockCol: Int,
        blockRow: Int,
        blockSpanX: Int,
        blockSpanY: Int,
    ): List<HomeItem>? {
        val grid = gridSize.coerceIn(MIN_GRID, MAX_GRID)
        if (blockCol < 0 || blockRow < 0 || blockSpanX < 1 || blockSpanY < 1) return null
        if (blockCol + blockSpanX > grid || blockRow + blockSpanY > grid) return null
        val packed = packItems(items, grid, blockCol, blockRow, blockSpanX, blockSpanY)
        return if (packed.size == items.size) packed else null
    }

    /**
     * Grow or shrink [widgetId] and move anything that would sit under the new
     * footprint. Null when the size is off-grid or leftover items cannot be packed.
     */
    fun planWidgetResize(
        items: List<HomeItem>,
        widgetId: String,
        spanX: Float,
        spanY: Float,
        gridSize: Int,
    ): List<HomeItem>? {
        val grid = gridSize.coerceIn(MIN_GRID, MAX_GRID)
        val item = items.firstOrNull { it.id == widgetId } ?: return null
        val maxSpanX = (grid - item.col).coerceAtLeast(1)
        val maxSpanY = (grid - item.row).coerceAtLeast(1)
        val sx = kotlin.math.round(spanX).toInt().coerceIn(1, maxSpanX)
        val sy = kotlin.math.round(spanY).toInt().coerceIn(1, maxSpanY)
        val others = items.filter { it.id != widgetId }
        val relocated = reflowAroundRect(others, grid, item.col, item.row, sx, sy) ?: return null
        return (relocated + item.copy(spanX = sx.toFloat(), spanY = sy.toFloat()))
            .sortedWith(compareBy({ it.row }, { it.col }, { it.id }))
    }

    private fun packItems(
        items: List<HomeItem>,
        grid: Int,
        blockCol: Int = -1,
        blockRow: Int = -1,
        blockSpanX: Int = 0,
        blockSpanY: Int = 0,
    ): List<HomeItem> {
        val occ = Array(grid) { BooleanArray(grid) }
        if (blockSpanX > 0 && blockSpanY > 0) {
            for (y in blockRow until (blockRow + blockSpanY).coerceAtMost(grid)) {
                for (x in blockCol until (blockCol + blockSpanX).coerceAtMost(grid)) {
                    if (y >= 0 && x >= 0) occ[y][x] = true
                }
            }
        }
        fun canPlace(col: Int, row: Int, spanX: Int, spanY: Int): Boolean {
            if (col < 0 || row < 0 || col + spanX > grid || row + spanY > grid) return false
            for (y in row until row + spanY) {
                for (x in col until col + spanX) {
                    if (occ[y][x]) return false
                }
            }
            return true
        }
        fun mark(col: Int, row: Int, spanX: Int, spanY: Int) {
            for (y in row until row + spanY) {
                for (x in col until col + spanX) occ[y][x] = true
            }
        }
        fun firstHole(spanX: Int, spanY: Int): Pair<Int, Int>? {
            for (row in 0..grid - spanY) {
                for (col in 0..grid - spanX) {
                    if (canPlace(col, row, spanX, spanY)) return col to row
                }
            }
            return null
        }
        fun placeSized(item: HomeItem, spanX: Int, spanY: Int): HomeItem? {
            val hole = firstHole(spanX, spanY) ?: return null
            mark(hole.first, hole.second, spanX, spanY)
            return item.copy(
                col = hole.first,
                row = hole.second,
                spanX = spanX.toFloat(),
                spanY = spanY.toFloat(),
            )
        }

        val sorted = items.sortedWith(compareBy({ it.row }, { it.col }, { it.id }))
        val placed = mutableListOf<HomeItem>()
        val leftover = mutableListOf<HomeItem>()
        for (item in sorted) {
            val spanX = item.spanX.toInt().coerceAtLeast(1).coerceAtMost(grid)
            val spanY = item.spanY.toInt().coerceAtLeast(1).coerceAtMost(grid)
            if (canPlace(item.col, item.row, spanX, spanY)) {
                mark(item.col, item.row, spanX, spanY)
                placed.add(item.copy(spanX = spanX.toFloat(), spanY = spanY.toFloat()))
            } else {
                leftover.add(item)
            }
        }
        for (item in leftover) {
            val maxX = item.spanX.toInt().coerceAtLeast(1).coerceAtMost(grid)
            val maxY = item.spanY.toInt().coerceAtLeast(1).coerceAtMost(grid)
            var moved: HomeItem? = null
            sy@ for (spanY in maxY downTo 1) {
                for (spanX in maxX downTo 1) {
                    moved = placeSized(item, spanX, spanY)
                    if (moved != null) break@sy
                }
            }
            if (moved != null) placed.add(moved)
        }
        return placed.sortedWith(compareBy({ it.row }, { it.col }, { it.id }))
    }
}
