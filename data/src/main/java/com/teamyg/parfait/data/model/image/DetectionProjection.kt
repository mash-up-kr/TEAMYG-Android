package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.SegmentationBounds

internal data class DetectionProjection(
    val transform: RecoveryTransform,
    val clip: SegmentationBounds,
)
