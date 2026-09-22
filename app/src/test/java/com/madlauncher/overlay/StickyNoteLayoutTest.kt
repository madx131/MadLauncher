package com.madlauncher.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StickyNoteLayoutTest {
    @Test
    fun nearLeftEdgeSnapsAndPins() {
        val p = StickyNoteLayout.settle(
            x = 20,
            y = 200,
            viewW = 56,
            viewH = 56,
            screenW = 1080,
            screenH = 2400,
            snapPx = 48,
            collapsed = true,
            peekPx = 12,
        )
        assertEquals(0, p.x)
        assertEquals(200, p.y)
        assertEquals(StickyNoteLayout.Edge.LEFT, p.edge)
    }

    @Test
    fun flingRightPinsToRight() {
        val p = StickyNoteLayout.settle(
            x = 400,
            y = 300,
            viewW = 56,
            viewH = 56,
            screenW = 1080,
            screenH = 2400,
            snapPx = 48,
            collapsed = true,
            peekPx = 12,
            velocityX = 2000f,
            velocityY = 10f,
        )
        assertEquals(1080 - 56, p.x)
        assertEquals(StickyNoteLayout.Edge.RIGHT, p.edge)
    }

    @Test
    fun centerStaysUnpinned() {
        val p = StickyNoteLayout.settle(
            x = 400,
            y = 800,
            viewW = 260,
            viewH = 180,
            screenW = 1080,
            screenH = 2400,
            snapPx = 48,
            collapsed = false,
            peekPx = 12,
        )
        assertEquals(400, p.x)
        assertEquals(800, p.y)
        assertNull(p.edge)
    }

    @Test
    fun offscreenRightPins() {
        val p = StickyNoteLayout.settle(
            x = 2000,
            y = -40,
            viewW = 260,
            viewH = 180,
            screenW = 1080,
            screenH = 2400,
            snapPx = 48,
            collapsed = false,
            peekPx = 12,
        )
        assertEquals(1080 - 260, p.x)
        assertEquals(0, p.y)
        assertEquals(StickyNoteLayout.Edge.RIGHT, p.edge)
    }
}
