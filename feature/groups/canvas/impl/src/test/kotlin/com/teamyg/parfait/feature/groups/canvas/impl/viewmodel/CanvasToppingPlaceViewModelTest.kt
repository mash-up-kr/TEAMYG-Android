package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** ViewModel 안의 private 상수(TOPPING_DRAG_PX_PER_SCALE=300f, TOPPING_DRAG_DEGREES_PER_PX=0.5f)와 맞춘 값 */
private const val DRAG_PX_PER_SCALE = 300f
private const val DRAG_DEGREES_PER_PX = 0.5f
private const val SCALE_DELTA = 1e-4f

class CanvasToppingPlaceViewModelTest {
    private fun viewModel(imageUri: String = "uri") = CanvasToppingPlaceViewModel(imageUri = imageUri)

    /** 회전 0/90/180/270도에서 핸들이 가리키는 바깥쪽 방향. [resizeOutwardDirection]과 같은 값이다 */
    private fun outwardDirectionAt(rotationDegrees: Float): Offset {
        val base = 1f / sqrt(2f)
        return when (rotationDegrees) {
            0f -> Offset(base, -base)
            90f -> Offset(base, base)
            180f -> Offset(-base, base)
            270f -> Offset(-base, -base)
            else -> error("이 테스트는 0/90/180/270도만 다룬다: $rotationDegrees")
        }
    }

    private fun rotatedViewModel(rotationDegrees: Float): CanvasToppingPlaceViewModel {
        val viewModel = viewModel()
        if (rotationDegrees != 0f) {
            viewModel.processIntent(
                CanvasToppingPlaceIntent.OnToppingRotateDrag(Offset(x = rotationDegrees / DRAG_DEGREES_PER_PX, y = 0f)),
            )
        }
        return viewModel
    }

    @Test
    fun onToppingResizeDrag_atRotation0_matchesExpectedFormula() {
        // Given 회전하지 않은 토핑 (핸들은 우측 상단 모서리에 있다)
        val viewModel = rotatedViewModel(0f)
        val direction = outwardDirectionAt(0f)
        val dragLength = 150f

        // When 핸들을 바깥쪽(우측 상단 대각선)으로 끈다
        viewModel.processIntent(
            CanvasToppingPlaceIntent.OnToppingResizeDrag(
                Offset(
                    x = dragLength * direction.x,
                    y =
                    dragLength * direction.y,
                ),
            ),
        )

        // Then 드래그 거리를 바깥쪽 방향에 정사영한 만큼 커진다
        val expectedDeltaScale = dragLength / DRAG_PX_PER_SCALE
        assertEquals(1f + expectedDeltaScale, viewModel.state.value.scale, SCALE_DELTA)
    }

    @Test
    fun onToppingResizeDrag_atEachBoundaryRotation_draggingOutwardIncreasesScale() {
        // Given·When 회전 0/90/180/270도 각각에서, 그 회전에서의 바깥쪽 방향으로 핸들을 끈다
        listOf(0f, 90f, 180f, 270f).forEach { rotationDegrees ->
            val viewModel = rotatedViewModel(rotationDegrees)
            val direction = outwardDirectionAt(rotationDegrees)
            val dragLength = 150f

            viewModel.processIntent(
                CanvasToppingPlaceIntent.OnToppingResizeDrag(
                    Offset(x = dragLength * direction.x, y = dragLength * direction.y),
                ),
            )

            // Then 어느 회전에서든 바깥쪽으로 끌면 커진다
            assertTrue(
                viewModel.state.value.scale > 1f,
                "rotation=$rotationDegrees 에서 바깥쪽 드래그가 커지지 않았다: ${viewModel.state.value.scale}",
            )
        }
    }

    @Test
    fun onToppingResizeDrag_atEachBoundaryRotation_draggingInwardDecreasesScale() {
        // Given·When 회전 0/90/180/270도 각각에서, 그 회전에서의 안쪽(바깥쪽의 반대) 방향으로 핸들을 끈다
        listOf(0f, 90f, 180f, 270f).forEach { rotationDegrees ->
            val viewModel = rotatedViewModel(rotationDegrees)
            val direction = outwardDirectionAt(rotationDegrees)
            val dragLength = 150f

            viewModel.processIntent(
                CanvasToppingPlaceIntent.OnToppingResizeDrag(
                    Offset(x = -dragLength * direction.x, y = -dragLength * direction.y),
                ),
            )

            // Then 어느 회전에서든 안쪽으로 끌면 작아진다
            assertTrue(
                viewModel.state.value.scale < 1f,
                "rotation=$rotationDegrees 에서 안쪽 드래그가 작아지지 않았다: ${viewModel.state.value.scale}",
            )
        }
    }

    @Test
    fun onToppingResizeDrag_clampsAtMaxScale() {
        // Given 기본 배율
        val viewModel = viewModel()

        // When 최대 배율을 훌쩍 넘는 크기로 바깥쪽으로 끈다
        viewModel.processIntent(
            CanvasToppingPlaceIntent.OnToppingResizeDrag(Offset(x = 10_000f, y = -10_000f)),
        )

        // Then 상한(2.5)에서 멈춘다
        assertEquals(2.5f, viewModel.state.value.scale, SCALE_DELTA)
    }

    @Test
    fun onToppingResizeDrag_clampsAtMinScale() {
        // Given 기본 배율
        val viewModel = viewModel()

        // When 최소 배율보다 훨씬 작아지도록 안쪽으로 끈다
        viewModel.processIntent(
            CanvasToppingPlaceIntent.OnToppingResizeDrag(Offset(x = -10_000f, y = 10_000f)),
        )

        // Then 하한(0.5)에서 멈춘다
        assertEquals(0.5f, viewModel.state.value.scale, SCALE_DELTA)
    }

    @Test
    fun onToppingRotateDrag_accumulatesAcrossMultipleDrags() {
        // Given 기본 상태(회전 0도)
        val viewModel = viewModel()

        // When 여러 번에 걸쳐 회전 핸들을 끈다
        viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingRotateDrag(Offset(x = 40f, y = 0f)))
        viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingRotateDrag(Offset(x = 20f, y = 0f)))

        // Then 각 드래그의 각도 변화가 그대로 누적된다
        assertEquals((40f + 20f) * DRAG_DEGREES_PER_PX, viewModel.state.value.rotationDegrees, SCALE_DELTA)
    }
}
