package com.teamyg.parfait.feature.segmentation.impl.component

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 원본 100×100 이미지를 200×400 캔버스에 Fit 으로 그리면 배율 2, 위아래 여백이 각 100 이다 */
private const val IMAGE_SIDE = 100
private const val CANVAS_WIDTH = 200f
private const val CANVAS_HEIGHT = 400f

class SegmentationHighlightGeometryTest {
    private fun bounds(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) = SegmentationBounds(left = left, top = top, right = right, bottom = bottom)

    private fun rectOf(bounds: SegmentationBounds) = scaledRectOrNull(
        bounds = bounds,
        imageWidth = IMAGE_SIDE,
        imageHeight = IMAGE_SIDE,
        canvasWidth = CANVAS_WIDTH,
        canvasHeight = CANVAS_HEIGHT,
    )

    private fun pick(
        boundsList: List<SegmentationBounds>,
        tapX: Float,
        tapY: Float,
    ) = pickCandidateIndex(
        boundsList = boundsList,
        imageWidth = IMAGE_SIDE,
        imageHeight = IMAGE_SIDE,
        canvasWidth = CANVAS_WIDTH,
        canvasHeight = CANVAS_HEIGHT,
        tapX = tapX,
        tapY = tapY,
    )

    @Test
    fun scaledRectOrNull_imageIsLetterboxed_offsetsByTheEmptyMargin() {
        // Given 원본 왼쪽 위 모서리에 붙은 10×10 영역
        val corner = bounds(left = 0, top = 0, right = 10, bottom = 10)

        // When 화면 좌표로 옮긴다
        val rect = rectOf(corner)

        // Then 배율 2 가 곱해지고, 세로로 남는 여백(100) 만큼 아래로 밀린다
        assertEquals(0f, rect?.left)
        assertEquals(100f, rect?.top)
        assertEquals(20f, rect?.right)
        assertEquals(120f, rect?.bottom)
    }

    @Test
    fun scaledRectOrNull_imageSizeIsNotUsable_returnsNull() {
        // Given 아직 이미지 치수를 모르는 상태
        // When 화면 좌표로 옮긴다
        val rect = scaledRectOrNull(
            bounds = bounds(left = 0, top = 0, right = 10, bottom = 10),
            imageWidth = 0,
            imageHeight = IMAGE_SIDE,
            canvasWidth = CANVAS_WIDTH,
            canvasHeight = CANVAS_HEIGHT,
        )

        // Then null 이다 — 0 으로 나눈 좌표를 그리면 Path 가 예외를 던진다
        assertNull(rect)
    }

    @Test
    fun scaledRect_contains_edgeIsInsideAndBeyondIsOutside() {
        // Given 화면 좌표로 (0,100)~(20,120) 인 사각형
        val rect = rectOf(bounds(left = 0, top = 0, right = 10, bottom = 10))!!

        // When·Then 경계는 안이고 그 바깥은 밖이다
        assertTrue(rect.contains(20f, 120f))
        assertFalse(rect.contains(20.1f, 120f))
    }

    @Test
    fun pickCandidateIndex_tapIsOutsideEveryCandidate_returnsNull() {
        // Given 왼쪽 위에 후보 하나
        val list = listOf(bounds(left = 0, top = 0, right = 10, bottom = 10))

        // When 아무 후보에도 안 걸리는 자리를 탭한다
        val picked = pick(list, tapX = 150f, tapY = 300f)

        // Then 고르지 않는다
        assertNull(picked)
    }

    @Test
    fun pickCandidateIndex_smallCandidateSitsInsideABigOne_picksTheSmallOne() {
        // Given 큰 후보 안에 작은 후보가 들어 있다(목록은 면적 내림차순)
        val big = bounds(left = 0, top = 0, right = 80, bottom = 80)
        val small = bounds(left = 20, top = 20, right = 40, bottom = 40)

        // When 둘 다 걸리는 자리를 탭한다(원본 좌표 30,30 → 화면 60,160)
        val picked = pick(listOf(big, small), tapX = 60f, tapY = 160f)

        // Then 안쪽 것을 고른다 — 바깥을 고르면 안쪽 대상을 영영 못 고른다
        assertEquals(1, picked)
    }

    @Test
    fun pickCandidateIndex_orderIsNotByArea_stillPicksTheSmallOne() {
        // Given 작은 것이 앞에 오도록 뒤집어 넘긴다
        val small = bounds(left = 20, top = 20, right = 40, bottom = 40)
        val big = bounds(left = 0, top = 0, right = 80, bottom = 80)

        // When 둘 다 걸리는 자리를 탭한다
        val picked = pick(listOf(small, big), tapX = 60f, tapY = 160f)

        // Then 목록 순서와 무관하게 면적이 작은 쪽이다 — 정렬 기준이 바뀌어도 안 깨져야 한다
        assertEquals(0, picked)
    }

    @Test
    fun pickCandidateIndex_tapHitsOnlyTheBigCandidate_picksIt() {
        // Given 큰 후보 안에 작은 후보가 들어 있다
        val big = bounds(left = 0, top = 0, right = 80, bottom = 80)
        val small = bounds(left = 20, top = 20, right = 40, bottom = 40)

        // When 작은 후보 바깥이면서 큰 후보 안인 자리를 탭한다(원본 60,60 → 화면 120,220)
        val picked = pick(listOf(big, small), tapX = 120f, tapY = 220f)

        // Then 큰 쪽도 고를 수 있다
        assertEquals(0, picked)
    }

    @Test
    fun pickCandidateIndex_noCandidates_returnsNull() {
        // Given 후보가 없다
        // When 아무 데나 탭한다
        val picked = pick(emptyList(), tapX = 100f, tapY = 200f)

        // Then 고르지 않는다
        assertNull(picked)
    }
}
