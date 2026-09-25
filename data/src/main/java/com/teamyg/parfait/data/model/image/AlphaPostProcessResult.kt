package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.SegmentationBounds

/**
 * @param changed 거짓이면 알파가 안 바뀌었다. 원본 판을 그대로 쓰려면 [bounds] 가 판 전체와
 *   같은지도 봐야 한다. 투명 여백이 있으면 판 치수와 [bounds] 가 어긋난다.
 * @param refineElapsedNanos 정련에 든 시간. 안 돌았으면 0
 */
internal data class AlphaPostProcessResult(
    val bounds: SegmentationBounds,
    val alphaSum: Long,
    val partialAlphaPixels: Int,
    val changed: Boolean,
    val refineElapsedNanos: Long,
)
