package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationCandidate

/** 원본 면적 대비 이 비율 **미만** 인 후보는 버린다 */
internal const val MIN_SUBJECT_AREA_RATIO = 0.01f

internal const val MAX_SUBJECT_COUNT = 5

/**
 * ML Kit 가 돌려준 후보에서 화면에 올릴 것만 남긴다.
 *
 * 정렬이 결정적이어야 하는 이유가 둘이다 — 테스트가 ML Kit 반환 순서에 흔들리지 않아야 하고,
 * 탭 판정이 목록 순서를 근거로 삼지 않더라도 상한 절단 결과가 매번 같아야 한다.
 */
internal fun filterCandidates(candidates: List<SegmentationCandidate>): List<SegmentationCandidate> = candidates
    .distinctBy { it.bounds }
    .filter { it.isLargeEnough() }
    .sortedWith(
        compareByDescending<SegmentationCandidate> { it.area }
            .thenBy { it.bounds.top }
            .thenBy { it.bounds.left },
    ).take(MAX_SUBJECT_COUNT)

/**
 * 면적을 마스크의 실제 객체 픽셀이 아니라 bounds 로 재는 것은, 이 판정이 거르려는 것이 손톱만 한
 * 파편이라 사각형만으로 충분해서다.
 */
private val SegmentationCandidate.area: Int
    get() = bounds.width * bounds.height

private fun SegmentationCandidate.isLargeEnough(): Boolean {
    val canvasArea = canvasWidth.toLong() * canvasHeight
    if (canvasArea <= 0L) return false

    return area >= canvasArea * MIN_SUBJECT_AREA_RATIO
}
