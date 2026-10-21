package com.madlauncher.ui.theme

import com.madlauncher.data.IconShapeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class IconShapeGeometryTest {
    private val fillOnly = listOf(
        IconShapeId.HEXAGON,
        IconShapeId.DIAMOND,
    )

    @Test
    fun fillOnlyMasksStayInsideTheSlot() {
        val size = 200f
        for (shape in fillOnly) {
            val poly = IconShapeGeometry.outline(shape, size, size)
            assertTrue("${shape.name} outline", poly.size >= 8)
            for ((x, y) in poly) {
                assertTrue("${shape.name} x=$x", x >= -1f && x <= size + 1f)
                assertTrue("${shape.name} y=$y", y >= -1f && y <= size + 1f)
            }
            val area = IconShapeGeometry.areaFraction(shape, size, size)
            assertTrue("${shape.name} area=$area", area in 0.20f..0.85f)
            assertFalse(
                "${shape.name} corner",
                IconShapeGeometry.contains(shape, size, size, 1f, 1f),
            )
            assertTrue(IconShapes.shapesFillOnly(shape))
        }
    }

    @Test
    fun hexagonIsWideAndDiamondContainsItsCenter() {
        val size = 100f
        val hex = IconShapeGeometry.outline(IconShapeId.HEXAGON, size, size)
        val minX = hex.minOf { it.first }
        val maxX = hex.maxOf { it.first }
        assertTrue(maxX - minX > 80f)
        assertTrue(IconShapeGeometry.contains(IconShapeId.DIAMOND, size, size, 50f, 50f))
        assertFalse(IconShapeGeometry.contains(IconShapeId.DIAMOND, size, size, 1f, 1f))
    }

    @Test
    fun classicShapesDoNotMoveThePicture() {
        listOf(
            IconShapeId.ROUNDED_SQUARE,
            IconShapeId.SQUARE,
            IconShapeId.SQUIRCLE,
            IconShapeId.CIRCLE,
            IconShapeId.TEARDROP,
            IconShapeId.ADAPTIVE,
        ).forEach { shape ->
            assertEquals(0f, IconShapeGeometry.areaFraction(shape, 100f, 100f), 0.01f)
            assertFalse(IconShapes.shapesFillOnly(shape))
        }
        assertEquals(12f, MadTheme.cornerDpFor(IconShapeId.ROUNDED_SQUARE), 0.01f)
        assertEquals(999f, MadTheme.cornerDpFor(IconShapeId.CIRCLE), 0.01f)
        assertEquals(0f, MadTheme.cornerDpFor(IconShapeId.ADAPTIVE), 0.01f)
        assertTrue(IconShapes.usesAdaptiveInset(IconShapeId.ADAPTIVE))
        assertFalse(IconShapes.usesAdaptiveInset(IconShapeId.SQUIRCLE))
    }

    @Test
    fun logoCenterSitsInTheBody() {
        val size = 200f
        for (shape in fillOnly) {
            val slot = IconShapeGeometry.logoSlot(shape)
            val cx = slot.centerX * size
            val cy = slot.centerY * size
            assertTrue(
                "${shape.name} center",
                IconShapeGeometry.contains(shape, size, size, cx, cy),
            )
            val radius = slot.scale * 0.66f * 0.5f * size * 0.72f
            for (k in 0 until 8) {
                val a = k * Math.PI / 4.0
                val x = cx + radius * cos(a).toFloat()
                val y = cy + radius * sin(a).toFloat()
                assertTrue(
                    "${shape.name} k=$k x=$x y=$y",
                    IconShapeGeometry.contains(shape, size, size, x, y),
                )
            }
        }
    }

    @Test
    fun classicShapesKeepTheLogoCentered() {
        listOf(
            IconShapeId.ROUNDED_SQUARE,
            IconShapeId.SQUARE,
            IconShapeId.SQUIRCLE,
            IconShapeId.CIRCLE,
            IconShapeId.TEARDROP,
            IconShapeId.ADAPTIVE,
        ).forEach { shape ->
            val slot = IconShapeGeometry.logoSlot(shape)
            assertEquals(0.5f, slot.centerX, 0.01f)
            assertEquals(0.5f, slot.centerY, 0.01f)
            assertEquals(1f, slot.scale, 0.01f)
            assertFalse(slot.placesLogo)
        }
    }

    @Test
    fun fillOnlyShapeNamesRoundTrip() {
        for (shape in fillOnly) {
            assertEquals(shape, enumValueOf<IconShapeId>(shape.name))
            assertEquals(0f, MadTheme.cornerDpFor(shape), 0.01f)
        }
    }
}
