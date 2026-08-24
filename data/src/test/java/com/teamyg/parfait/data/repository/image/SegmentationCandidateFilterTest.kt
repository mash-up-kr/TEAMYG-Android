package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CANVAS_SIDE = 100

/** 필터는 비트맵을 보지 않는다 — 자리만 채운다 */
private object FakeBitmap : BitmapWrapper

class SegmentationCandidateFilterTest {
    /** 원본은 100×100(면적 10000)이라, 한 변이 10이면 면적 100 = 정확히 1% 다 */
    private fun candidate(
        left: Int = 0,
        top: Int = 0,
        width: Int = 50,
        height: Int = 50,
    ) = SegmentationCandidate(
        bounds = SegmentationBounds(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        ),
        bitmap = FakeBitmap,
        canvasWidth = CANVAS_SIDE,
        canvasHeight = CANVAS_SIDE,
    )

    @Test
    fun filterCandidates_areaIsExactlyTheThreshold_keepsIt() {
        // Given 면적이 원본의 정확히 1% 인 후보
        val onePercent = candidate(width = 10, height = 10)

        // When 거른다
        val filtered = filterCandidates(listOf(onePercent))

        // Then 임계 "미만" 만 버리므로 남는다
        assertEquals(listOf(onePercent), filtered)
    }

    @Test
    fun filterCandidates_areaIsBelowTheThreshold_dropsIt() {
        // Given 면적이 원본의 0.99% 인 후보(99 픽셀)
        val tooSmall = candidate(width = 9, height = 11)

        // When 거른다
        val filtered = filterCandidates(listOf(tooSmall))

        // Then 손톱만 한 파편은 화면에 올리지 않는다
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun filterCandidates_everyCandidateIsBelowTheThreshold_returnsEmpty() {
        // Given 전부 임계 미만인 후보 셋
        val all = listOf(
            candidate(left = 0, width = 5, height = 5),
            candidate(left = 20, width = 6, height = 6),
            candidate(left = 40, width = 7, height = 7),
        )

        // When 거른다
        val filtered = filterCandidates(all)

        // Then 빈 목록이다 — 호출부가 이걸 보고 폴백을 태운다
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun filterCandidates_moreThanTheLimit_keepsTheBiggestOnes() {
        // Given 상한(5)보다 하나 많은 후보 여섯. 면적이 제각각이다
        val sizes = listOf(20, 60, 30, 50, 40, 70)
        val candidates = sizes.mapIndexed { index, side ->
            candidate(left = index, top = index, width = side, height = side)
        }

        // When 거른다
        val filtered = filterCandidates(candidates)

        // Then 면적 큰 것부터 다섯만 남는다
        assertEquals(5, filtered.size)
        assertEquals(listOf(70, 60, 50, 40, 30), filtered.map { it.bounds.width })
    }

    @Test
    fun filterCandidates_sameArea_ordersByTopThenLeft() {
        // Given 면적이 모두 같고 위치만 다른 후보 셋(입력 순서는 뒤섞여 있다)
        val bottomLeft = candidate(left = 0, top = 40, width = 20, height = 20)
        val topRight = candidate(left = 40, top = 0, width = 20, height = 20)
        val topLeft = candidate(left = 0, top = 0, width = 20, height = 20)

        // When 거른다
        val filtered = filterCandidates(listOf(bottomLeft, topRight, topLeft))

        // Then top → left 오름차순으로 갈린다. ML Kit 반환 순서에 기대면 테스트가 흔들린다
        assertEquals(listOf(topLeft, topRight, bottomLeft), filtered)
    }

    @Test
    fun filterCandidates_duplicateBounds_keepsOnlyOne() {
        // Given 좌표가 완전히 같은 후보 둘
        val first = candidate(left = 10, top = 10, width = 30, height = 30)
        val second = candidate(left = 10, top = 10, width = 30, height = 30)

        // When 거른다
        val filtered = filterCandidates(listOf(first, second))

        // Then 하나만 남는다 — 탭 판정이 겹친 둘 중 하나를 영영 못 고른다
        assertEquals(1, filtered.size)
    }

    @Test
    fun filterCandidates_emptyInput_returnsEmpty() {
        // Given 후보가 없는 입력
        // When 거른다
        val filtered = filterCandidates(emptyList())

        // Then 빈 목록이다
        assertTrue(filtered.isEmpty())
    }
}
