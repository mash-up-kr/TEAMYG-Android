package com.teamyg.parfait.domain.model

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper

data class SegmentationResult(
    val bitmap: BitmapWrapper,
    val subjectImagePath: String,
)
