package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitTextId

/** null 인 필드는 그대로 둔다. text 가 없는 이유는 `docs/api/parfait-text.md` PATCH 절. */
data class TextToppingUpdate(
    val parfaitTextId: ParfaitTextId,
    val positionX: Double? = null,
    val positionY: Double? = null,
    val positionZ: Int? = null,
    val scale: Double? = null,
    val rotation: Double? = null,
    val color: String? = null,
    val fontSize: Double? = null,
    val isFill: Boolean? = null,
    val isStroke: Boolean? = null,
    val textAlign: TextToppingAlign? = null,
)
