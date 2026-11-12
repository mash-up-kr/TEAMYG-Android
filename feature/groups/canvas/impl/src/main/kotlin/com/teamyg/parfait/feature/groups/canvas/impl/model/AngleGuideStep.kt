package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 각도 가이드 한 프레임 판정 결과. */
internal data class AngleGuideStep(
    val rawDegrees: Float,
    val hasRotated: Boolean,
    val effectiveDegrees: Float,
    val isVisible: Boolean,
    val isHorizontal: Boolean,
    val justSnapped: Boolean,
)
