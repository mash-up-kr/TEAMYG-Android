package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationCandidate

internal class HarvestedCandidate(
    val candidate: SegmentationCandidate,
    /** 후처리가 실패하거나 알파를 전멸시켜 후처리 이전 판으로 되돌렸다 */
    val reverted: Boolean,
)
