package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableText
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditUiState

/**
 * 저장된 배경을 편집 화면의 선택 상태로 옮긴다. 그룹·혼자 공간 배경 편집이 함께 쓴다.
 *
 * 색을 못 읽으면 지금 선택된 색을 그대로 둔다. 이미지는 [CanvasBGEditUiState.selectedImageSource]
 * 를 null 로 두어 "이미 저장돼 있던 배경"으로 표시한다 — 확인을 눌러도 다시 저장하지 않는다.
 */
internal fun CanvasBGEditUiState.seededFrom(background: CanvasBackground?): CanvasBGEditUiState = copy(
    selectedColor = (background as? CanvasBackground.Color)?.value?.toColorOrNull() ?: selectedColor,
    selectedImageUri = (background as? CanvasBackground.Image)?.url,
    selectedImageSource = null,
)

/** 배경 위에 보이기만 할 토핑을 그리는 순서(positionZ 오름차순)로 세운다 */
internal fun List<CanvasToppingVO>.toEditableToppingsInDrawOrder(): List<EditableTopping> = this
    .sortedBy { topping -> topping.transform.positionZ }
    .map { topping -> topping.toEditableTopping() }

internal fun List<CanvasTextToppingVO>.toEditableTexts(): List<EditableText> = map { text -> text.toEditableText() }
