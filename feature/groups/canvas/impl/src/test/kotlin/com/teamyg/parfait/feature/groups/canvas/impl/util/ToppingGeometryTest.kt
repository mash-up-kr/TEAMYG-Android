package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/** 부동소수점 삼각함수 오차를 흡수하기 위한 허용 오차 */
private const val DELTA = 1e-4f

class ToppingGeometryTest {
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
}
