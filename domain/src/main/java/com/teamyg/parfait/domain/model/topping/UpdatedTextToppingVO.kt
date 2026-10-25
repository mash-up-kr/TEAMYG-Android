package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitTextId

/**
 * 텍스트 수정 결과. 배치 응답과 달리 placedBy 가 없다(`api/parfait-text.md`).
 */
data class UpdatedTextToppingVO(
    val parfaitTextId: ParfaitTextId,
    val text: String,
    val transform: ToppingTransform,
    val style: TextToppingStyle,
)
