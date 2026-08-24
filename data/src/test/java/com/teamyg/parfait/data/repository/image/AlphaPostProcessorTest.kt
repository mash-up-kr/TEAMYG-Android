package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 각 값이 알파 하나. 행 구분은 호출부가 width 로 준다 */
private fun alphaBytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

private fun ByteArray.asInts() = IntArray(size) { this[it].toInt() and 0xFF }

class AlphaPostProcessorTest {
    @Test
    fun applyKeepMask_maskIsFalse_clearsThatBlock() {
        // Given — 2×2 원본, 배율 2 라 축소판은 1픽셀이다
        val alpha = alphaBytes(255, 255, 255, 255)
        val keep = booleanArrayOf(false)

        // When
        val changed = applyKeepMask(alpha, width = 2, height = 2, keep = keep, maskWidth = 1, factor = 2)

        // Then
        assertContentEquals(intArrayOf(0, 0, 0, 0), alpha.asInts())
        assertEquals(true, changed)
    }

    @Test
    fun applyKeepMask_maskIsTrue_leavesTheOriginalAlphaUntouched() {
        // Given — 이진화 결과로 알파를 덮어쓰지 않는 것이 이 설계의 요점이다
        val alpha = alphaBytes(10, 200, 255, 0)
        val keep = booleanArrayOf(true)

        // When
        val changed = applyKeepMask(alpha, width = 2, height = 2, keep = keep, maskWidth = 1, factor = 2)

        // Then
        assertContentEquals(intArrayOf(10, 200, 255, 0), alpha.asInts())
        assertEquals(false, changed)
    }

    @Test
    fun measureAlpha_someOpaquePixels_returnsTightBoundsAndCoverage() {
        // Given — 4×3 에서 가운데 두 픽셀만 남았다
        val alpha = alphaBytes(
            0, 0, 0, 0,
            0, 100, 255, 0,
            0, 0, 0, 0,
        )

        // When
        val measured = measureAlpha(alpha, width = 4, height = 3)

        // Then — right·bottom 은 마지막 픽셀을 포함하도록 exclusive 다
        assertEquals(SegmentationBounds(left = 1, top = 1, right = 3, bottom = 2), measured?.bounds)
        assertEquals(355L, measured?.alphaSum)
        assertEquals(1, measured?.partialAlphaPixels)
    }

    @Test
    fun measureAlpha_everythingIsTransparent_returnsNull() {
        // Given
        val alpha = alphaBytes(0, 0, 0, 0)

        // When
        val measured = measureAlpha(alpha, width = 2, height = 2)

        // Then
        assertNull(measured)
    }

    @Test
    fun measureAlpha_alphaAbove127_isNotMisreadAsNegative() {
        // Given — 부호 처리를 빠뜨리면 합이 음수가 되고 bounds 도 안 잡힌다
        val alpha = alphaBytes(200, 255)

        // When
        val measured = measureAlpha(alpha, width = 2, height = 1)

        // Then
        assertEquals(455L, measured?.alphaSum)
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 2, bottom = 1), measured?.bounds)
    }

    @Test
    fun erodeEdge_ramp_shiftsItInwardByOnePixelWithoutMakingAStep() {
        // Given — 한 행짜리 램프. 제자리에서 돌리면 왼쪽부터 연쇄로 전멸한다
        val alpha = alphaBytes(0, 64, 128, 191, 255, 255)

        // When
        val changed = erodeEdge(alpha, width = 6, height = 1)

        // Then
        assertEquals(true, changed)
        assertContentEquals(intArrayOf(0, 0, 64, 128, 191, 255), alpha.asInts())
    }

    @Test
    fun erodeEdge_verticalRamp_shiftsItInwardByOnePixelWithoutMakingAStep() {
        // Given — 테스트 1의 전치판. previousRow 스냅샷이 깨지면 위에서부터 연쇄로 전멸해
        // [0, 0, 0, 0, 0, 255] 가 나온다. up/down 이 최솟값을 결정하는 경로를 실행하는 유일한 테스트다
        val alpha = alphaBytes(0, 64, 128, 191, 255, 255)

        // When
        val changed = erodeEdge(alpha, width = 1, height = 6)

        // Then
        assertEquals(true, changed)
        assertContentEquals(intArrayOf(0, 0, 64, 128, 191, 255), alpha.asInts())
    }

    @Test
    fun erodeEdge_hardMatte_stillLosesOneLayer() {
        // Given — 알파가 0 아니면 255 뿐이다. "1~254 만 대상"이면 아무 일도 안 일어난다
        val alpha = alphaBytes(0, 255, 255, 255, 255, 0)

        // When
        erodeEdge(alpha, width = 6, height = 1)

        // Then
        assertContentEquals(intArrayOf(0, 0, 255, 255, 0, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_oneVerticalOpaqueLine_isProtectedByTheRidgeRule() {
        // Given — 폭 1 불투명 선. 좌우가 둘 다 0 이라 능선으로 보호한다
        val alpha = alphaBytes(
            0, 255, 0,
            0, 255, 0,
            0, 255, 0,
        )

        // When
        val changed = erodeEdge(alpha, width = 3, height = 3)

        // Then
        assertEquals(false, changed)
        assertContentEquals(intArrayOf(0, 255, 0, 0, 255, 0, 0, 255, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_oneVerticalPartialAlphaLine_isProtectedToo() {
        // Given — 값이 낮아도 능선이면 안 깎는다
        val alpha = alphaBytes(
            0, 100, 0,
            0, 100, 0,
            0, 100, 0,
        )

        // When
        erodeEdge(alpha, width = 3, height = 3)

        // Then
        assertContentEquals(intArrayOf(0, 100, 0, 0, 100, 0, 0, 100, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_twoPixelWideBar_disappears() {
        // Given — 양쪽에서 한 겹씩 깎이므로 사라진다. 1픽셀 침식에 내재한 한계다
        val alpha = alphaBytes(
            0, 255, 255, 0,
            0, 255, 255, 0,
            0, 255, 255, 0,
        )

        // When
        erodeEdge(alpha, width = 4, height = 3)

        // Then
        assertEquals(0, alpha.asInts().sum())
    }

    @Test
    fun erodeEdge_subjectTouchingTheFrame_keepsTheEdgeRow() {
        // Given — 판 전체가 불투명하다. 이미지 밖을 투명으로 치면 테두리가 깎인다
        // 2x2
        val alpha = alphaBytes(
            255,
            255,
            255,
            255,
        )

        // When
        val changed = erodeEdge(alpha, width = 2, height = 2)

        // Then
        assertEquals(false, changed)
        assertContentEquals(intArrayOf(255, 255, 255, 255), alpha.asInts())
    }
}
