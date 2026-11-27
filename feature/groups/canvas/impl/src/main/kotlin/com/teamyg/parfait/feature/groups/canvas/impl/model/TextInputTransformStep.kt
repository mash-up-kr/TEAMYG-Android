package com.teamyg.parfait.feature.groups.canvas.impl.model

import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputUiState

/**
 * 텍스트 입력 화면 제스처 한 프레임의 결과.
 *
 * @param justSnapped 이 프레임에 중앙·각도 가이드에 막 붙었다. 햅틱을 한 번 울릴 근거다
 */
data class TextInputTransformStep(
    val state: CanvasTextInputUiState,
    val justSnapped: Boolean,
)
