package com.teamyg.parfait.domain.model

/**
 * 알맹이가 "올릴 만큼 남았는가"를 재는 하한.
 *
 * 자동 누끼 후보 필터와 수동 편집이 같은 값을 봐야 한다. 기준이 갈리면 자동으로는 버려질
 * 크기를 수동 편집으로 만들어 낼 수 있어, 하한이 있다는 사실 자체가 의미를 잃는다.
 *
 * 지표를 bounds 사각형이 아니라 알파 합으로 고른 이유와 값의 근거는
 * `parfait/specs/archive/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」에 있다.
 */
object SubjectCoverage {
    /** 캔버스 면적 대비 이 비율 **미만** 커버리지는 하한 미달이다 (만분율) */
    private const val MIN_COVERAGE_PERMYRIAD = 5L

    /** 작은 사진에서 비율만으로는 너무 헐거워지므로 두는 하한 (원본 픽셀) */
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

        // 나누지 않고 양변에 255를 곱해 부동소수를 거치지 않는다
        return alphaSum >= MAX_ALPHA * floorPixels(canvasArea)
    }
}
