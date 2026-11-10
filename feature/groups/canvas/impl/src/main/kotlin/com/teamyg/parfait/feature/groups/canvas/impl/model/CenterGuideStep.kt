package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 비율 기반 중앙·각도 가이드 한 프레임 판정 결과(`CanvasToppingArrangeViewModel` 전용). */
internal data class CenterGuideStep(
    val rawX: Float,
    val rawY: Float,
    val guideX: CenterGuideAxisResult,
    val guideY: CenterGuideAxisResult,
    val angle: AngleGuideStep,
    val justSnapped: Boolean,
)
