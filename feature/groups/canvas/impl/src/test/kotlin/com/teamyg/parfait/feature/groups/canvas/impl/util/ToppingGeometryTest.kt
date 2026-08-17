package com.teamyg.parfait.feature.groups.canvas.impl.util

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

}
