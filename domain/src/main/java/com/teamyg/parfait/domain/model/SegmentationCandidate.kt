package com.teamyg.parfait.domain.model

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper

/**
 * 사용자가 고를 수 있는 피사체 후보 하나.
 *
 * [canvasWidth] 와 [canvasHeight] 는 [bounds] 가 어느 좌표계의 값인지를 말한다. 한 번의
 * 세그멘테이션에서 나온 후보끼리 같은 값이 복제되지만, 그 대가로 후보 하나만 넘겨도 좌표계가
 * 온전히 따라간다 — 저장할 때 다른 크기를 실어 보내 그림이 어긋나는 조합이 성립하지 않는다.
 */
data class SegmentationCandidate(
    val bounds: SegmentationBounds,
    /**
     * **반드시 [bounds] 크기로 잘린 판이어야 한다.** 저장이 이 판을 원본 크기 캔버스의
     * `(bounds.left, bounds.top)` 에 그대로 얹으므로, 원본 크기 판을 실으면 오른쪽과 아래가
     * 잘린 채 저장된다 — 예외가 아니라 조용한 파손이라 늦게 드러난다.
     */
    val bitmap: BitmapWrapper,
    val canvasWidth: Int,
    val canvasHeight: Int,
    /**
     * [bitmap] 알파의 총합. 255로 나누면 "실제로 칠해진 픽셀 수"가 된다.
     *
     * 후보 판정이 [bounds] 사각형이 아니라 이 값을 보는 이유는
     * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」에 있다.
     */
    val coverageAlphaSum: Long,
)
