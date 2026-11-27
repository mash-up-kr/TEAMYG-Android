package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val SCALE_DELTA = 1e-4f

private val MEASURED = CanvasToppingPlaceUiState(
    canvasSize = DpSize(360.dp, 640.dp),
    toppingBaseSize = DpSize(100.dp, 50.dp),
)

class ToppingPlaceStateTest {
    @Test
    fun initialPlacement_centersAndScalesLongSideToCanvasRatio() {
        val placed = MEASURED.withInitialPlacementIfNeeded()

        // 360 × 0.4 ÷ 100 = 1.44, 짧은 변 하한(48 ÷ 50 = 0.96)보다 크다
        assertEquals(1.44f, placed.scale, SCALE_DELTA)
        assertEquals(130.dp, placed.offsetX)
        assertEquals(295.dp, placed.offsetY)
        assertEquals(130.dp, placed.rawOffsetX)
        assertEquals(295.dp, placed.rawOffsetY)
    }

    @Test
    fun initialPlacement_thinTopping_raisesShortSideToTouchMinimum() {
        val placed = MEASURED.copy(toppingBaseSize = DpSize(200.dp, 10.dp)).withInitialPlacementIfNeeded()

        // 긴 변 기준 0.72 로는 짧은 변이 7.2dp — 48dp 가 되도록 4.8 로 올린다
        assertEquals(4.8f, placed.scale, SCALE_DELTA)
    }

    @Test
    fun initialPlacement_afterUserAdjusted_keepsState() {
        val adjusted = MEASURED.copy(hasUserAdjustedPlacement = true, scale = 2f)

        assertSame(adjusted, adjusted.withInitialPlacementIfNeeded())
    }

    @Test
    fun initialPlacement_beforeBothMeasured_keepsState() {
        val onlyCanvas = MEASURED.copy(toppingBaseSize = null)

        assertSame(onlyCanvas, onlyCanvas.withInitialPlacementIfNeeded())
    }

    @Test
    fun transformStep_appliesPanZoomRotationAndMarksAdjusted() {
        val placed = MEASURED.withInitialPlacementIfNeeded()

        val step = placed.stepToppingTransform(pan = DpOffset(10.dp, 20.dp), zoom = 1.1f, rotationDelta = 15f)

        val state = step!!.state
        assertEquals(placed.offsetX + 10.dp, state.offsetX)
        assertEquals(placed.offsetY + 20.dp, state.offsetY)
        assertEquals(placed.scale * 1.1f, state.scale, SCALE_DELTA)
        assertEquals(15f, state.rotationDegrees, SCALE_DELTA)
        assertTrue(state.hasUserAdjustedPlacement)
        assertFalse(step.justSnapped)
    }

    @Test
    fun transformStep_clampsAtMinScale() {
        val placed = MEASURED.withInitialPlacementIfNeeded()

        val step = placed.stepToppingTransform(pan = DpOffset.Zero, zoom = 0f, rotationDelta = 0f)

        // 서버 scale 0.05 에 해당하는 배율(0.05 × 360 × 0.4 ÷ 100)
        assertEquals(0.072f, step!!.state.scale, SCALE_DELTA)
    }

    @Test
    fun transformStep_beforeMeasuredOrWhilePanelOpen_isIgnored() {
        assertNull(MEASURED.copy(canvasSize = null).stepToppingTransform(DpOffset(1.dp, 1.dp), 1f, 0f))
        assertNull(MEASURED.copy(isBorderPanelOpen = true).stepToppingTransform(DpOffset(1.dp, 1.dp), 1f, 0f))
    }

    @Test
    fun withCanvasContent_imageBackground_keepsFallbackColor() {
        val withImage = CanvasToppingPlaceUiState().withCanvasContent(
            background = CanvasBackground.Image("/files/bg.jpg"),
            toppings = emptyList(),
            texts = emptyList(),
        )

        assertEquals("/files/bg.jpg", withImage.backgroundImageUrl)
        assertEquals(CanvasToppingPlaceUiState().backgroundColor, withImage.backgroundColor)
    }
}
