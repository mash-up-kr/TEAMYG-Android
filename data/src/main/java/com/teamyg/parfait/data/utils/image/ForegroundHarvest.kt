package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.data.model.image.DetectionBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate

internal class ForegroundHarvest(
    val candidates: List<SegmentationCandidate>,
    /** 다음 단계가 어디를 크롭할지 정하는 데만 쓴다. 검출 공간 좌표다 */
    val hint: DetectionBounds?,
)
