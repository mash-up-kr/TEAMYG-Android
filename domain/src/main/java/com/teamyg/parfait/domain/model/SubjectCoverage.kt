package com.teamyg.parfait.domain.model

/**
 * 알맹이가 올릴 만큼 남았는지 재는 하한. 자동 누끼 후보 필터와 수동 편집이 같은 값을 봐야
 * 한다 — 기준이 갈리면 자동으로는 버려질 크기를 수동 편집으로 만들어 낼 수 있다.
 *
 * 지표와 값의 근거는 `parfait/specs/archive/2026-08-24-segmentation-mask-postprocessing.md`
 * 「필터 판정」에 있다.
 */
object SubjectCoverage {
    private const val MIN_COVERAGE_PERMYRIAD = 5L

    /** 작은 사진에서는 비율만으로 너무 헐거워져 함께 두는 하한 */
    private const val MIN_COVERAGE_PIXELS = 2_500L

    private const val MAX_ALPHA = 255L

    private const val PERMYRIAD_BASE = 10_000L

    fun floorPixels(canvasArea: Long): Long =
        maxOf(MIN_COVERAGE_PIXELS, canvasArea * MIN_COVERAGE_PERMYRIAD / PERMYRIAD_BASE)

    /**
     * @param alphaSum 알파의 총합. [MAX_ALPHA] 로 나누면 실제로 칠해진 픽셀 수가 된다
     */
    fun isLargeEnough(
        alphaSum: Long,
        canvasArea: Long,
    ): Boolean {
        if (canvasArea <= 0L) return false

        // 양변에 255를 곱해 부동소수를 거치지 않는다
        return alphaSum >= MAX_ALPHA * floorPixels(canvasArea)
    }
}
