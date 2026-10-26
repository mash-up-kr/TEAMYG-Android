package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitTextId

/** null 인 축은 그대로 두라는 뜻이다 */
data class TextToppingTransformUpdate(
    val parfaitTextId: ParfaitTextId,
    val positionX: Double? = null,
    val positionY: Double? = null,
    val positionZ: Int? = null,
    val scale: Double? = null,
    val rotation: Double? = null,
)
