package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.SegmentationBounds

/** 재표본은 [mapped] 크기로 하고, 그다음 [clipped] 로 자른다. 순서를 뒤집으면 알파가 어긋난다 */
internal data class ProjectedRegion(
    val mapped: SegmentationBounds,
    val clipped: SegmentationBounds,
)
