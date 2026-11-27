package com.teamyg.parfait.feature.groups.canvas.impl.model

import com.teamyg.parfait.domain.model.topping.TextToppingTransformUpdate

/**
 * 재편집 확인 때 저장할 것. 변형과 테두리를 따로 담는 것은 저장 요청이 두 갈래라서다 —
 * 테두리만 바꾼 토핑은 변형 요청에 실리지 않는다.
 */
internal data class ToppingArrangeChanges(
    val transformChanged: List<EditableTopping>,
    val textUpdates: List<TextToppingTransformUpdate>,
    val borderChanged: List<EditableTopping>,
)
