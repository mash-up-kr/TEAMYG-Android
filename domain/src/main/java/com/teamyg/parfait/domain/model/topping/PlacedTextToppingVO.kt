package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitTextId

/**
 * 텍스트 배치 확정 결과.
 *
 * [transform] 의 positionZ 는 이미지 토핑과 한 축을 공유한다.
 */
data class PlacedTextToppingVO(
    val parfaitTextId: ParfaitTextId,
    val text: String,
    val transform: ToppingTransform,
    val style: TextToppingStyle,
    val placedBy: ToppingPlacerVO,
)
