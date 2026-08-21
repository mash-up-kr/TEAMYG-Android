package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.feature.groups.canvas.impl.component.TOPPING_BASE_LONG_SIDE_RATIO
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DELTA = 1e-4

private val CANVAS = DpSize(width = 360.dp, height = 640.dp)

class ToppingPlacementTest {
    /**
     * 읽기 쪽 `CanvasToppingLayer#CanvasTopping` 의 식을 그대로 옮긴 역함수.
     * 정사각 박스의 한 변을 내고, 그 안에서 원본 비율을 유지한 긴 변이 곧 화면 긴 변이다.
     */
    private fun readBackLongSideDp(
        scale: Double,
        canvasWidth: Float,
    ): Double = canvasWidth * TOPPING_BASE_LONG_SIDE_RATIO * scale

    @Test
    fun toToppingTransform_centered_mapsToHalfHalf() {
        // Given 캔버스 정중앙에 놓인 토핑
        val baseSize = DpSize(width = 100.dp, height = 50.dp)
        val transform = toToppingTransform(
            offsetX = (CANVAS.width - baseSize.width) / 2,
            offsetY = (CANVAS.height - baseSize.height) / 2,
            scale = 1f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 7,
        )

        // Then 정규화 좌표는 중심 기준 0.5·0.5 다
        assertEquals(0.5, transform.positionX, DELTA)
        assertEquals(0.5, transform.positionY, DELTA)
        assertEquals(7, transform.positionZ)
        assertEquals(0.0, transform.rotation, DELTA)
    }

    @Test
    fun toToppingTransform_topLeftCorner_mapsToHalfSizeFractions() {
        // Given 좌상단에 딱 붙인 토핑을 2배로 키웠다.
        // 중심은 **배율 적용 전** 크기의 절반만큼 안쪽이다 — 화면이 그렇게 계산한다
        // (CanvasToppingPlaceScreen 의 center = offsetX + baseSize.width / 2).
        // scale 을 곱해 중심을 구하는 구현은 여기서 갈린다
        val baseSize = DpSize(width = 100.dp, height = 50.dp)
        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 2f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 1,
        )

        // 캔버스가 가로/세로로 달라 x·y 를 뒤바꾼 구현도 여기서 걸린다
        assertEquals(50.0 / 360.0, transform.positionX, DELTA)
        assertEquals(25.0 / 640.0, transform.positionY, DELTA)
    }

    @Test
    fun toToppingTransform_overflowingTopping_roundTripsToo() {
        // Given 캔버스 폭을 넘도록 키운 토핑 — maxScaleToOverflowCanvas 가 허용하는 구간이다
        val baseSize = DpSize(width = 200.dp, height = 80.dp)
        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 4f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 1,
        )

        // 화면 긴 변 800dp 는 캔버스 폭 360dp 를 넘는다. 읽기 쪽 박스가 clamp 되면
        // 여기가 아니라 실제 화면에서만 갈리므로, 이 단언과 Step 3 의 수정이 한 쌍이다
        assertEquals(
            200.0 * 4,
            readBackLongSideDp(scale = transform.scale, canvasWidth = CANVAS.width.value),
            DELTA,
        )
    }

    @Test
    fun toToppingTransform_scale_roundTripsThroughReadSideFormula() {
        // Given 배율 1.75 로 키운 가로로 긴 토핑
        val baseSize = DpSize(width = 200.dp, height = 80.dp)
        val screenLongSideDp = 200.0 * 1.75

        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 1.75f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 1,
        )

        // Then 읽기 쪽 식으로 되돌리면 화면에서 보던 긴 변이 그대로 나온다
        assertEquals(
            screenLongSideDp,
            readBackLongSideDp(scale = transform.scale, canvasWidth = CANVAS.width.value),
            DELTA,
        )
    }

    @Test
    fun toToppingTransform_portraitTopping_roundTripsOnTheLongSideToo() {
        // 세로로 긴 토핑에서도 기준은 긴 변이다 — 짧은 변으로 나누면 여기서 갈린다
        val baseSize = DpSize(width = 60.dp, height = 240.dp)
        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 0.5f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 1,
        )

        assertEquals(
            240.0 * 0.5,
            readBackLongSideDp(scale = transform.scale, canvasWidth = CANVAS.width.value),
            DELTA,
        )
    }

    @Test
    fun toToppingTransform_rotation_passesThroughUnchanged() {
        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 1f,
            rotationDegrees = -37.5f,
            canvasSize = CANVAS,
            toppingBaseSize = DpSize(width = 100.dp, height = 100.dp),
            positionZ = 1,
        )

        assertEquals(-37.5, transform.rotation, DELTA)
    }
}
