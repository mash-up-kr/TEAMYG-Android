package com.teamyg.parfait.domain.model

data class SegmentationResult(
    val subjectImagePath: String,
    val subjectBounds: SegmentationBounds?,
)
