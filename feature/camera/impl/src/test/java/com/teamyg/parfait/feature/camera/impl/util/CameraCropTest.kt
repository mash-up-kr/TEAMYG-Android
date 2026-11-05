package com.teamyg.parfait.feature.camera.impl.util

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CameraCropTest {
    private val feedRect = Rect(0f, 0f, 1000f, 2000f)
    private val viewfinder = Rect(100f, 400f, 900f, 1200f)

    private fun crop(
        visible: IntRect,
        rotation: Int = 0,
        viewfinderRect: Rect = viewfinder,
        feed: Rect = feedRect,
        front: Boolean = false,
    ) = computeCaptureCropRect(viewfinderRect, feed, visible, rotation, front)

    @Test
    fun crop_rotation0_mapsFractionIntoVisibleRect() {
        assertEquals(IntRect(100, 400, 900, 1200), crop(IntRect(0, 0, 1000, 2000)))
    }

    @Test
    fun crop_rotation90_mapsToBuffer() {
        assertEquals(IntRect(400, 100, 1200, 900), crop(IntRect(0, 0, 2000, 1000), rotation = 90))
    }

    @Test
    fun crop_rotation180() {
        assertEquals(IntRect(100, 800, 900, 1600), crop(IntRect(0, 0, 1000, 2000), rotation = 180))
    }

    @Test
    fun crop_rotation270() {
        assertEquals(IntRect(800, 100, 1600, 900), crop(IntRect(0, 0, 2000, 1000), rotation = 270))
    }

    @Test
    fun crop_visibleRectOffset_addsOrigin() {
        assertEquals(IntRect(150, 500, 950, 1300), crop(IntRect(50, 100, 1050, 2100)))
    }

    @Test
    fun crop_frontFacing_mirrorsHorizontally() {
        assertEquals(
            IntRect(500, 400, 1000, 1200),
            crop(IntRect(0, 0, 1000, 2000), viewfinderRect = Rect(0f, 400f, 500f, 1200f), front = true),
        )
    }

    @Test
    fun crop_frontFacing_rotation270_mirrorsBeforeRotation() {
        assertEquals(
            IntRect(800, 500, 1600, 1000),
            crop(
                IntRect(0, 0, 2000, 1000),
                rotation = 270,
                viewfinderRect = Rect(0f, 400f, 500f, 1200f),
                front = true,
            ),
        )
    }

    @Test
    fun crop_viewfinderOutsideFeed_clamps() {
        val visible = IntRect(0, 0, 1000, 2000)

        assertEquals(visible, crop(visible, viewfinderRect = Rect(-100f, -100f, 1100f, 2100f)))
    }

    @Test
    fun crop_emptyFeed_returnsNull() {
        assertNull(crop(IntRect(0, 0, 1000, 2000), feed = Rect(0f, 0f, 0f, 0f)))
    }

    @Test
    fun crop_emptyIntersection_returnsNull() {
        assertNull(crop(IntRect(0, 0, 1000, 2000), viewfinderRect = Rect(1100f, 0f, 1200f, 100f)))
    }

    @Test
    fun fallback_rotation90_centersFeedAspect() {
        assertEquals(
            IntRect(0, 500, 4000, 2500),
            fallbackCropRect(IntSize(4000, 3000), 90, Size(1000f, 2000f)),
        )
    }

    @Test
    fun fallback_rotation0_sameAspect_isFull() {
        assertEquals(
            IntRect(0, 0, 1000, 2000),
            fallbackCropRect(IntSize(1000, 2000), 0, Size(500f, 1000f)),
        )
    }

    @Test
    fun fallback_emptyFeed_returnsNull() {
        assertNull(fallbackCropRect(IntSize(4000, 3000), 90, Size(0f, 0f)))
    }
}
