package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds

internal data class AlphaPostProcessResult(
    val bounds: SegmentationBounds,
    val alphaSum: Long,
    val partialAlphaPixels: Int,
    /**
     * 거짓이면 알파가 하나도 안 바뀌었다는 뜻이다. 원본 판을 그대로 쓰려면 [bounds] 가 판 전체와
     * 같은지도 함께 봐야 한다 — 알파를 안 바꿔도 원판에 투명 여백이 있으면 판 치수와 [bounds]
     * 치수가 어긋나 `SegmentationCandidate` 의 계약이 깨진다.
     */
    val changed: Boolean,
    /** 정련에 든 시간. 안 돌았으면 0 이다. 원본 해상도 적용이 감당 가능한지 판정할 근거다 */
    val refineElapsedNanos: Long,
)
