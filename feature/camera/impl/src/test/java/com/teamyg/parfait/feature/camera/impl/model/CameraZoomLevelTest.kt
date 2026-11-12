package com.teamyg.parfait.feature.camera.impl.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CameraZoomLevelTest {
    @Test
    fun labelOf_showsOneDecimalAndDropsTrailingZero() {
        assertEquals("1x", CameraZoomLevel.labelOf(1f))
        assertEquals("1.4x", CameraZoomLevel.labelOf(1.4f))
        assertEquals("1.4x", CameraZoomLevel.labelOf(1.44f))
        assertEquals("1.5x", CameraZoomLevel.labelOf(1.46f))
        assertEquals("0.5x", CameraZoomLevel.labelOf(0.5f))
        assertEquals("2x", CameraZoomLevel.labelOf(1.97f))
        assertEquals("10x", CameraZoomLevel.labelOf(10f))
    }

    @Test
    fun matching_betweenLevels_isNull() {
        assertNull(CameraZoomLevel.matching(1.4f, CameraZoomLevel.entries))
    }

    @Test
    fun matching_roundsToTenths() {
        assertEquals(CameraZoomLevel.ONE, CameraZoomLevel.matching(1.04f, CameraZoomLevel.entries))
    }

    @Test
    fun matching_unavailableLevel_isNull() {
        assertNull(CameraZoomLevel.matching(2f, listOf(CameraZoomLevel.ONE)))
    }
}
