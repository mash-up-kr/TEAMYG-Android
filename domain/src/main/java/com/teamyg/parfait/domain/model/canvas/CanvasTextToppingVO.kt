package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import kotlinx.datetime.LocalDateTime

/**
 * @param isMine 서버가 판정해 준 값. 판정 축이 계정 id 라 [placedBy] 의 groupMemberId 로는
 *   재현할 수 없다.
 */
data class CanvasTextToppingVO(
    val parfaitTextId: ParfaitTextId,
    val text: String,
    val transform: ToppingTransform,
    val style: TextToppingStyle,
    val placedBy: ToppingPlacerVO,
    val isMine: Boolean,
    val createdAt: LocalDateTime,
)
