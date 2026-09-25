package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.SegmentationCandidate

/**
 * @param reverted 후처리가 실패하거나 알파를 전멸시켜 후처리 이전 판으로 되돌렸다
 */
internal class HarvestedCandidate(
    val candidate: SegmentationCandidate,
    val reverted: Boolean,
)
