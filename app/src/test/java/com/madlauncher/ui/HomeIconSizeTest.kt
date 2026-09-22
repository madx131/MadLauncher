package com.madlauncher.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeIconSizeTest {
    @Test
    fun sliderIsTheDrawnSize() {
        assertEquals(56.dp, homeIconSize(56f))
        assertEquals(88.dp, homeIconSize(88f))
        assertEquals(40.dp, homeIconSize(40f))
    }

    @Test
    fun wideLandscapeCellDoesNotInflateIcon() {
        assertEquals(56.dp, iconSizeForCell(56f, cellW = 200.dp, cellH = 72.dp))
    }

    @Test
    fun shortLandscapeCellDoesNotShrinkIcon() {
        assertEquals(56.dp, iconSizeForCell(56f, cellW = 90.dp, cellH = 48.dp))
    }

    @Test
    fun narrowCellShrinksOnlyToFitWidth() {
        assertEquals(60.dp, iconSizeForCell(88f, cellW = 64.dp, cellH = 120.dp))
    }

    @Test
    fun largeIconsWrapToFewerColumns() {
        assertEquals(3, columnsForIconSize(availableWidth = 328.dp, iconSize = 88.dp))
        assertEquals(5, columnsForIconSize(availableWidth = 328.dp, iconSize = 56.dp))
    }

    @Test
    fun landscapeFitsMoreSameSizeIcons() {
        assertEquals(9, columnsForIconSize(availableWidth = 608.dp, iconSize = 56.dp))
    }
}
