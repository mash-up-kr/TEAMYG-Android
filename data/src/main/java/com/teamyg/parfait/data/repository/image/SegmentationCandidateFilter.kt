package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate

/** 캔버스 면적 대비 이 비율 **미만** 커버리지의 후보는 버린다 (만분율) */
internal const val MIN_SUBJECT_COVERAGE_PERMYRIAD = 5L

/** 작은 사진에서 비율만으로는 너무 헐거워지므로 두는 하한 (원본 픽셀) */
internal const val MIN_SUBJECT_COVERAGE_PIXELS = 2_500L

internal const val MAX_SUBJECT_COUNT = 5

/** 이 값 **이상** 겹치는 후보 쌍은 같은 것으로 본다 (만분율) */
internal const val DUPLICATE_IOU_PERMYRIAD = 9_000L

/**
 * 후보가 넘어야 하는 "실제로 칠해진 픽셀 수".
 *
 * 값의 근거와 이 지표를 고른 이유는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」에 있다.
 */
internal fun coverageFloorPixels(canvasArea: Long): Long =
    maxOf(MIN_SUBJECT_COVERAGE_PIXELS, canvasArea * MIN_SUBJECT_COVERAGE_PERMYRIAD / 10_000L)

/**
 * 정렬이 결정적이어야 하는 이유가 둘이다 — 테스트가 ML Kit 반환 순서에 흔들리지 않아야 하고,
 * 탭 판정이 목록 순서를 근거로 삼지 않더라도 상한 절단 결과가 매번 같아야 한다.
 *
 * `top`·`left` 만으로는 전순서가 아니다(좌상단이 같고 크기가 다른 조합이 성립한다). 뒤 두 키가
 * 없으면 동률에서 순서가 ML Kit 반환 순서를 따라간다.
 */
private val candidateOrder = compareByDescending<SegmentationCandidate> { it.coverageAlphaSum }
    .thenBy { it.bounds.top }
    .thenBy { it.bounds.left }
    .thenBy { it.bounds.bottom }
    .thenBy { it.bounds.right }

/**
 * ML Kit 가 돌려준 후보에서 화면에 올릴 것만 남긴다.
 */
internal fun filterCandidates(candidates: List<SegmentationCandidate>): List<SegmentationCandidate> = candidates
    .filter { it.isLargeEnough() }
    .sortedWith(candidateOrder)
    .dropNearDuplicates()
    .take(MAX_SUBJECT_COUNT)

private fun SegmentationCandidate.isLargeEnough(): Boolean {
    val canvasArea = canvasWidth.toLong() * canvasHeight
    if (canvasArea <= 0L) return false

    // coverage = coverageAlphaSum / 255 이므로 양변에 255를 곱해 부동소수를 거치지 않는다
    return coverageAlphaSum >= 255L * coverageFloorPixels(canvasArea)
}

/**
 * 앞에서부터 훑으며 이미 채택한 것과 크게 겹치는 후보를 버린다. 정렬이 전순서라 결과가 매번 같다.
 *
 * ⚠️ **포함 관계는 병합하지 않는다.** 교집합을 작은 쪽 면적으로 나누는 지표로 바꾸면 사람이 든
 * 물건이 지워진다. 그 판단의 근거는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」에 있다.
 */
private fun List<SegmentationCandidate>.dropNearDuplicates(): List<SegmentationCandidate> {
    val kept = mutableListOf<SegmentationCandidate>()
    for (candidate in this) {
        if (kept.none { it.bounds.overlapsAsDuplicate(candidate.bounds) }) kept += candidate
    }
    return kept
}

private fun SegmentationBounds.overlapsAsDuplicate(other: SegmentationBounds): Boolean {
    val overlapWidth = minOf(right, other.right) - maxOf(left, other.left)
    val overlapHeight = minOf(bottom, other.bottom) - maxOf(top, other.top)
    if (overlapWidth <= 0 || overlapHeight <= 0) return false

    val intersection = overlapWidth.toLong() * overlapHeight
    val union = width.toLong() * height + other.width.toLong() * other.height - intersection
    if (union <= 0L) return false

    return intersection * 10_000L >= DUPLICATE_IOU_PERMYRIAD * union
}
