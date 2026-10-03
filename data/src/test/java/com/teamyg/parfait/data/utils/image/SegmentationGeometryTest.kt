package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SegmentationGeometryTest {
    @Test
    fun offsetBy_movesEveryEdge() {
        assertEquals(SegmentationBounds(11, 22, 13, 24), SegmentationBounds(1, 2, 3, 4).offsetBy(dx = 10, dy = 20))
    }

    @Test
    fun isInsideCanvas_exactFit_isTrue() {
        assertTrue(isInsideCanvas(SegmentationBounds(0, 0, 100, 50), canvasWidth = 100, canvasHeight = 50))
    }

    @Test
    fun isInsideCanvas_anyEdgeOutside_isFalse() {
        assertFalse(isInsideCanvas(SegmentationBounds(-1, 0, 100, 50), canvasWidth = 100, canvasHeight = 50))
        assertFalse(isInsideCanvas(SegmentationBounds(0, -1, 100, 50), canvasWidth = 100, canvasHeight = 50))
        assertFalse(isInsideCanvas(SegmentationBounds(0, 0, 101, 50), canvasWidth = 100, canvasHeight = 50))
        assertFalse(isInsideCanvas(SegmentationBounds(0, 0, 100, 51), canvasWidth = 100, canvasHeight = 50))
    }
}
