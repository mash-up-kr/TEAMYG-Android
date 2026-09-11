package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.SegmentationBounds

internal data class ProjectedRegion(
    val mapped: SegmentationBounds,
    val clipped: SegmentationBounds,
)
