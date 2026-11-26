package com.teamyg.parfait.feature.groups.canvas.impl.model

import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceUiState

/**
 * 배치 화면 제스처 한 프레임의 결과.
 *
 * @param justSnapped 이 프레임에 중앙·각도 가이드에 막 붙었다. 햅틱을 한 번 울릴 근거다
 */
data class ToppingPlaceTransformStep(
    val state: CanvasToppingPlaceUiState,
    val justSnapped: Boolean,
)
