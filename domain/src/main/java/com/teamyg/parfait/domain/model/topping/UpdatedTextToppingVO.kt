package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitTextId

data class UpdatedTextToppingVO(
    val parfaitTextId: ParfaitTextId,
    val text: String,
    val transform: ToppingTransform,
    val style: TextToppingStyle,
)
