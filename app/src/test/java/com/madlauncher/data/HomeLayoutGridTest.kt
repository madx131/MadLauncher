package com.madlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutGridTest {
    @Test
    fun unsetEmptyPageDefaultsToFive() {
        val page = HomePage(id = "p", items = emptyList())
        assertEquals(5, HomeLayout.gridSizeFor(page))
        assertEquals(5, HomeLayout.DEFAULT_GRID)
    }

    @Test
    fun reflowKeepsIconsWhenShrinkingGrid() {
        val items = listOf(
            HomeItem(id = "a", kind = "app", col = 0, row = 0),
            HomeItem(id = "b", kind = "shortcut", col = 4, row = 0, title = "Maps"),
            HomeItem(id = "c", kind = "app", col = 4, row = 4),
        )
        val packed = HomeLayout.reflowItemsToGrid(items, 4)
        assertEquals(setOf("a", "b", "c"), packed.map { it.id }.toSet())
        assertEquals(3, packed.size)
        packed.forEach { item ->
            assertEquals(true, item.col in 0..3 && item.row in 0..3)
        }
        val a = packed.first { it.id == "a" }
        assertEquals(0, a.col)
        assertEquals(0, a.row)
    }

    @Test
    fun reflowKeepsFitItemsWhenGrowingGrid() {
        val items = listOf(
            HomeItem(id = "a", kind = "app", col = 2, row = 1),
            HomeItem(id = "b", kind = "shortcut", col = 0, row = 0),
        )
        val packed = HomeLayout.reflowItemsToGrid(items, 5)
        assertEquals(2, packed.first { it.id == "a" }.col)
        assertEquals(1, packed.first { it.id == "a" }.row)
        assertEquals(0, packed.first { it.id == "b" }.col)
    }

    @Test
    fun reflowShrinksWideWidgetToFit() {
        val glance = HomeItem(id = "g", kind = "widget", widget = "glance", col = 0, row = 0, spanX = 4f, spanY = 2f)
        val packed = HomeLayout.reflowItemsToGrid(listOf(glance), 4)
        assertEquals(1, packed.size)
        assertEquals(4f, packed[0].spanX)
        assertEquals(2f, packed[0].spanY)
        assertEquals(0, packed[0].col)
        assertEquals(0, packed[0].row)
    }

    @Test
    fun legacyThreeBecomesSix() {
        val three = HomePage(id = "p3", items = emptyList(), gridSize = 3)
        assertEquals(6, HomeLayout.gridSizeFor(three))
        assertEquals(6, HomeLayout.normalizeGridSize(3))
    }

    @Test
    fun fourFiveAndSixAreKept() {
        val four = HomePage(id = "p4", items = emptyList(), gridSize = 4)
        val six = HomePage(id = "p6", items = emptyList(), gridSize = 6)
        assertEquals(4, HomeLayout.gridSizeFor(four))
        assertEquals(6, HomeLayout.gridSizeFor(six))
        assertEquals(listOf(4, 5, 6), HomeLayout.PAGE_GRID_SIZES)
    }

    @Test
    fun growingWidgetPushesIconsOutOfTheNewFootprint() {
        val glance = HomeItem(id = "g", kind = "widget", widget = "glance", col = 0, row = 0, spanX = 4f, spanY = 2f)
        val under = HomeItem(id = "a", kind = "app", col = 0, row = 2)
        val side = HomeItem(id = "b", kind = "app", col = 4, row = 0)
        val planned = HomeLayout.planWidgetResize(listOf(glance, under, side), "g", 4f, 3f, 5)
        requireNotNull(planned)
        val grown = planned.first { it.id == "g" }
        assertEquals(4f, grown.spanX)
        assertEquals(3f, grown.spanY)
        val moved = planned.first { it.id == "a" }
        assertEquals(false, moved.col < 4 && moved.row < 3)
        assertEquals(4, planned.first { it.id == "b" }.col)
        assertEquals(0, planned.first { it.id == "b" }.row)
    }

    @Test
    fun recoverPullsIconsBackFromPastLastRow() {
        val glance = HomeItem(id = "g", kind = "widget", widget = "glance", col = 0, row = 0, spanX = 5f, spanY = 1f)
        val visible = HomeItem(id = "keep", kind = "app", col = 0, row = 2)
        val lost = HomeItem(id = "lost", kind = "app", col = 1, row = 5)
        val page = HomePage(id = "p0", items = listOf(glance, visible, lost), gridSize = 5)
        val recovered = HomeLayout.recoverPages(listOf(page))
        val items = recovered.single().items
        assertEquals(setOf("g", "keep", "lost"), items.map { it.id }.toSet())
        val back = items.first { it.id == "lost" }
        assertEquals(true, HomeLayout.itemFitsGrid(back, 5))
        assertEquals(0, items.first { it.id == "keep" }.col)
        assertEquals(2, items.first { it.id == "keep" }.row)
    }

    @Test
    fun widgetResizeFailsWhenPageIsFull() {
        val glance = HomeItem(id = "g", kind = "widget", widget = "glance", col = 0, row = 0, spanX = 4f, spanY = 2f)
        val fillers = (0 until 16).map { i ->
            HomeItem(id = "i$i", kind = "app", col = i % 4, row = i / 4)
        }
        val planned = HomeLayout.planWidgetResize(listOf(glance) + fillers, "g", 4f, 3f, 4)
        assertEquals(null, planned)
    }
}
