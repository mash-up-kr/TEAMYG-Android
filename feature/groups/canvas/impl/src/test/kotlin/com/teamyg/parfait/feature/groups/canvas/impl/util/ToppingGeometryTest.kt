package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

/** 부동소수점 삼각함수 오차를 흡수하기 위한 허용 오차 */
private const val DELTA = 1e-4f

class ToppingGeometryTest {
    @Test
    fun resizeOutwardDirection_rotation0_pointsUpperRight() {
        // Given·When 회전하지 않은 토핑 (핸들은 우측 상단 모서리에 있다)
        val (x, y) = resizeOutwardDirection(0f)

        // Then 바깥쪽은 우측·위쪽이다 — 화면 좌표계에서 위쪽은 y 가 음수
        val base = 1f / sqrt(2f)
        assertEquals(base, x, DELTA)
        assertEquals(-base, y, DELTA)
    }

    @Test
    fun resizeOutwardDirection_rotation90_pointsLowerRight() {
        // Given·When 시계방향으로 90도 돌린 토핑 — 우측 상단 모서리가 우측 하단으로 온다
        val (x, y) = resizeOutwardDirection(90f)

        val base = 1f / sqrt(2f)
        assertEquals(base, x, DELTA)
        assertEquals(base, y, DELTA)
    }

    @Test
    fun resizeOutwardDirection_rotation180_pointsLowerLeft() {
        // Given·When 180도 돌린 토핑 — 우측 상단 모서리가 좌측 하단으로 온다
        val (x, y) = resizeOutwardDirection(180f)

        val base = 1f / sqrt(2f)
        assertEquals(-base, x, DELTA)
        assertEquals(base, y, DELTA)
    }

    @Test
    fun resizeOutwardDirection_rotation270_pointsUpperLeft() {
        // Given·When 270도 돌린 토핑 — 우측 상단 모서리가 좌측 상단으로 온다
        val (x, y) = resizeOutwardDirection(270f)

        val base = 1f / sqrt(2f)
        assertEquals(-base, x, DELTA)
        assertEquals(-base, y, DELTA)
    }

    @Test
    fun toppingImageSize_landscape_longSideIsWidth() {
        // Given 가로가 긴 원본 (2:1)
        val size = toppingImageSize(longSide = 100.dp, aspectRatio = 2f)

        // Then 긴 변이 가로에 붙고 세로만 비율대로 줄어든다
        assertEquals(100f, size.width.value, DELTA)
        assertEquals(50f, size.height.value, DELTA)
    }

    @Test
    fun toppingImageSize_portrait_longSideIsHeight() {
        // Given 세로가 긴 원본 (1:4)
        val size = toppingImageSize(longSide = 100.dp, aspectRatio = 0.25f)

        // Then
        assertEquals(25f, size.width.value, DELTA)
        assertEquals(100f, size.height.value, DELTA)
    }

    @Test
    fun toppingImageSize_square_bothSidesEqual() {
        val size = toppingImageSize(longSide = 60.dp, aspectRatio = 1f)

        assertEquals(60f, size.width.value, DELTA)
        assertEquals(60f, size.height.value, DELTA)
    }

    @Test
    fun toppingImageSize_nonPositiveRatio_fallsBackToSquare() {
        // Given 아직 원본 비율을 모르는 상태 — 정사각으로 두는 것이 현행 동작이다
        val size = toppingImageSize(longSide = 40.dp, aspectRatio = 0f)

        assertEquals(40f, size.width.value, DELTA)
        assertEquals(40f, size.height.value, DELTA)
    }

    @Test
    fun rotationDeltaDegrees_dragAlongClockwiseTangent_returnsPositive() {
        // Given 중심에서 우측 하단으로 떨어진 회전 핸들
        val handleVector = Offset(x = 100f, y = 100f)

        // When 시계방향 궤도를 따라 끈다 — 우측 하단에서 그 방향은 왼쪽이다
        val degrees = rotationDeltaDegrees(handleVector = handleVector, dragDelta = Offset(x = -10f, y = 0f))

        // Then 시계방향이므로 각도가 늘어난다
        assertEquals(Math.toDegrees(0.05).toFloat(), degrees, DELTA)
    }

    @Test
    fun rotationDeltaDegrees_dragAgainstClockwiseTangent_returnsNegative() {
        // Given·When 같은 핸들을 반시계 궤도(오른쪽)로 끈다
        val degrees = rotationDeltaDegrees(
            handleVector = Offset(x = 100f, y = 100f),
            dragDelta = Offset(x = 10f, y = 0f),
        )

        assertEquals(-Math.toDegrees(0.05).toFloat(), degrees, DELTA)
    }

    @Test
    fun rotationDeltaDegrees_dragDownwardAtLowerRightHandle_returnsPositive() {
        // Given·When 우측 하단 핸들을 아래로 끈다 — 이것도 시계방향 궤도의 성분이다
        val degrees = rotationDeltaDegrees(
            handleVector = Offset(x = 100f, y = 100f),
            dragDelta = Offset(x = 0f, y = 10f),
        )

        assertEquals(Math.toDegrees(0.05).toFloat(), degrees, DELTA)
    }

    @Test
    fun rotationDeltaDegrees_dragAwayFromCenter_returnsZero() {
        // Given·When 핸들을 중심 바깥 방향으로만 끈다 — 궤도를 도는 성분이 없다
        val degrees = rotationDeltaDegrees(
            handleVector = Offset(x = 100f, y = 100f),
            dragDelta = Offset(x = 5f, y = 5f),
        )

        assertEquals(0f, degrees, DELTA)
    }

    @Test
    fun rotationDeltaDegrees_fartherHandle_rotatesLess() {
        // Given 같은 방향에 있지만 중심에서 두 배 먼 핸들
        val drag = Offset(x = -10f, y = 0f)
        val near = rotationDeltaDegrees(Offset(x = 100f, y = 100f), drag)
        val far = rotationDeltaDegrees(Offset(x = 200f, y = 200f), drag)

        // Then 같은 거리를 끌어도 먼 핸들이 덜 돈다
        assertEquals(near / 2f, far, DELTA)
    }

    @Test
    fun rotationDeltaDegrees_handleAtCenter_returnsZero() {
        // Given·When 핸들이 중심과 겹쳐 궤도를 정의할 수 없는 경우
        val degrees = rotationDeltaDegrees(Offset.Zero, Offset(x = 10f, y = 10f))

        assertEquals(0f, degrees, DELTA)
    }
}
