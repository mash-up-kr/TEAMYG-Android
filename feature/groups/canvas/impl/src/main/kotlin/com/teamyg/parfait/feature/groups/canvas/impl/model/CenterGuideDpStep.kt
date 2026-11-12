package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.ui.unit.Dp

/** Dp 오프셋 기반 중앙 가이드 한 프레임 판정 결과(Place·TextInput 공유). */
internal data class CenterGuideDpStep(
    val rawOffsetX: Dp,
    val rawOffsetY: Dp,
    val guideX: CenterGuideAxisResult,
    val guideY: CenterGuideAxisResult,
    val justSnapped: Boolean,
)
