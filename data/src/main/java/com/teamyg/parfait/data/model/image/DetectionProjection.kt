package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.SegmentationBounds

/** 검출 공간이 원본에 어떻게 놓이는가. 1차 경로에는 없다 */
internal data class DetectionProjection(
    val transform: RecoveryTransform,
    val clip: SegmentationBounds,
)
