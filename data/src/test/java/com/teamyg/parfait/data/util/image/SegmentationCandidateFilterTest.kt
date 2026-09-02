package com.teamyg.parfait.data.util.image

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
    private fun candidate(
        left: Int = 0,
        top: Int = 0,
        width: Int = 50,
        height: Int = 50,
        canvasWidth: Int = CANVAS_SIDE,
        canvasHeight: Int = CANVAS_SIDE,
        coverageAlphaSum: Long = 255L * width * height,
    ) = SegmentationCandidate(
        bounds = SegmentationBounds(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        ),
        bitmap = FakeBitmap,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        coverageAlphaSum = coverageAlphaSum,
    )

    @Test
    fun filterCandidates_duplicateBounds_keepsOnlyOne() {
        // Given — 기본 후보는 50×50 이라 커버리지가 정확히 하한(2,500px)이다
        val first = candidate(left = 10, top = 10)
        val second = candidate(left = 10, top = 10)

        // When
        val filtered = filterCandidates(listOf(first, second))

        // Then
        assertEquals(1, filtered.size)
    }

    @Test
    fun filterCandidates_moreThanTheLimit_keepsTheBiggestOnes() {
        // Given — 겹치지 않게 떼어 놓은 여섯 후보. 커버리지 내림차순으로 다섯만 남아야 한다
        val sides = listOf(50, 60, 70, 80, 90, 100)
        val candidates = sides.mapIndexed { index, side ->
            candidate(
                left = index * 200,
                top = 0,
                width = side,
                height = side,
                canvasWidth = 2_000,
                canvasHeight = 1_000,
            )
        }

        // When
        val filtered = filterCandidates(candidates)

        // Then
        assertEquals(listOf(100, 90, 80, 70, 60), filtered.map { it.bounds.width })
    }

    @Test
    fun filterCandidates_coverageIsExactlyTheFloor_keepsIt() {
        // Given — 캔버스 1,000,000px 이면 하한은 max(2500, 500) = 2500px
        val exactly = candidate(
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 2_500,
        )

        // When
        val filtered = filterCandidates(listOf(exactly))

        // Then
        assertEquals(listOf(exactly), filtered)
    }

    @Test
    fun filterCandidates_coverageIsBelowTheFloor_dropsIt() {
        // Given
        val below = candidate(
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 2_499,
        )

        // When
        val filtered = filterCandidates(listOf(below))

        // Then
        assertEquals(emptyList(), filtered)
    }

    @Test
    fun filterCandidates_bigCanvas_ratioFloorTakesOverTheAbsoluteFloor() {
        // Given — 12,000,000px 캔버스에서 비율 하한은 6,000px 이라 절대 하한 2,500 을 넘는다.
        // 둘의 bounds 를 떼어 놓아야 중복 판정에 걸리지 않는다
        val below = candidate(
            left = 0,
            width = 100,
            height = 100,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 5_999,
        )
        val above = candidate(
            left = 500,
            width = 100,
            height = 100,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 6_000,
        )

        // When
        val filtered = filterCandidates(listOf(below, above))

        // Then
        assertEquals(listOf(above), filtered)
    }

    @Test
    fun filterCandidates_thinButLargeSubject_survivesWhileTinyFragmentDoesNot() {
        // Given — 같은 큰 사각형을 차지하지만 하나는 알맹이가 있고 하나는 파편이다
        val pen = candidate(
            width = 1_000,
            height = 1_000,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 10_000,
        )
        val fragment = candidate(
            width = 900,
            height = 900,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 300,
        )

        // When
        val filtered = filterCandidates(listOf(fragment, pen))

        // Then
        assertEquals(listOf(pen), filtered)
    }

    @Test
    fun filterCandidates_twelveMegapixelOpaqueSubject_survivesTheIntBoundary() {
        // Given — 12MP 전면 불투명 후보의 알파 총합은 30억이라 Int 를 넘는다.
        // Int 로 누적하면 음수로 래핑되어 가장 큰 피사체가 조용히 삭제된다
        val fullFrame = candidate(
            width = 4_000,
            height = 3_000,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 12_000_000,
        )

        // When
        val filtered = filterCandidates(listOf(fullFrame))

        // Then
        assertEquals(listOf(fullFrame), filtered)
    }

    @Test
    fun filterCandidates_sameCoverage_ordersByTopThenLeftThenBottomThenRight() {
        // Given — 커버리지가 같고 좌상단도 같은데 크기가 다른 쌍을 섞는다
        val shorter = candidate(
            left = 1,
            top = 1,
            width = 60,
            height = 60,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 3_000,
        )
        val taller = candidate(
            left = 1,
            top = 1,
            width = 60,
            height = 70,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 3_000,
        )
        val lower = candidate(
            left = 1,
            top = 9,
            width = 60,
            height = 70,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 3_000,
        )

        // When
        val filtered = filterCandidates(listOf(lower, taller, shorter))

        // Then
        assertEquals(listOf(shorter, taller, lower), filtered)
    }

    @Test
    fun filterCandidates_emptyInput_returnsEmpty() {
        // Given 후보가 없는 입력
        // When 거른다
        val filtered = filterCandidates(emptyList())

        // Then 빈 목록이다
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun filterCandidates_iouAboveTheThreshold_keepsTheOneWithMoreCoverage() {
        // Given — 100×100 과 96×96(안쪽으로 2px). IoU = 9216 / 10000 = 0.9216
        val bigger = candidate(
            width = 100,
            height = 100,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 9_000,
        )
        val nearlySame = candidate(
            left = 2,
            top = 2,
            width = 96,
            height = 96,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 8_000,
        )

        // When
        val filtered = filterCandidates(listOf(nearlySame, bigger))

        // Then
        assertEquals(listOf(bigger), filtered)
    }

    @Test
    fun filterCandidates_iouBelowTheThreshold_keepsBoth() {
        // Given — 100×100 과 94×94(안쪽으로 3px). IoU = 8836 / 10000 = 0.8836
        val bigger = candidate(
            width = 100,
            height = 100,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 9_000,
        )
        val overlapping = candidate(
            left = 3,
            top = 3,
            width = 94,
            height = 94,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 8_000,
        )

        // When
        val filtered = filterCandidates(listOf(overlapping, bigger))

        // Then
        assertEquals(listOf(bigger, overlapping), filtered)
    }

    @Test
    fun filterCandidates_smallSubjectInsideABigOne_keepsBoth() {
        // Given — 사람 안의 든 물건. 포함 관계는 IoU 가 낮아 병합되지 않고, 그게 의도다
        val person = candidate(
            width = 300,
            height = 900,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 100_000,
        )
        val heldItem = candidate(
            left = 100,
            top = 400,
            width = 80,
            height = 80,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 5_000,
        )

        // When
        val filtered = filterCandidates(listOf(heldItem, person))

        // Then
        assertEquals(listOf(person, heldItem), filtered)
    }

    @Test
    fun filterCandidates_duplicateClusterExceedsTheLimit_keepsTheDistinctCandidate() {
        // Given — 100×100 을 left=0..5 로 1px 씩 민 여섯 후보. 가장 먼 쌍(left=0 vs left=5)도
        // overlapWidth=95, intersection=9,500, union=10,500 이라 IoU=9,500/10,500≈0.9048 ≥ 0.9 라
        // 여섯 개가 서로 전부 중복 판정이다. 커버리지는 255×10,000(만점)으로 하한(255×2,500)을 훌쩍 넘는다.
        // 정상 후보는 뭉치와 겹치지 않는 자리(left=500)에 두고, 커버리지는 255×3,000 으로 하한은
        // 넘되 뭉치보다는 낮게 잡아 정렬에서 뭉치 뒤로 밀리게 한다.
        // dropNearDuplicates 가 take 보다 앞이면 뭉치가 하나로 뭉쳐 정상 후보와 합쳐 2개가 남지만,
        // take 가 앞이면 정렬 상위 5개(뭉치 중 5개)만 남아 dedup 후에도 정상 후보는 이미 잘려 나가 있다.
        val cluster = (0..5).map { offset ->
            candidate(
                left = offset,
                top = 0,
                width = 100,
                height = 100,
                canvasWidth = 1_000,
                canvasHeight = 1_000,
                coverageAlphaSum = 255L * 10_000,
            )
        }
        val distinct = candidate(
            left = 500,
            top = 500,
            width = 100,
            height = 100,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 3_000,
        )

        // When
        val filtered = filterCandidates(cluster + distinct)

        // Then
        assertTrue(distinct in filtered)
    }
}
