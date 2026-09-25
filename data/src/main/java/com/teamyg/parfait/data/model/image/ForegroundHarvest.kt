package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.SegmentationCandidate

/**
 * @param hint 다음 단계 크롭 위치. 검출 공간 좌표
 */
internal class ForegroundHarvest(
    val candidates: List<SegmentationCandidate>,
    val hint: DetectionBounds?,
)
