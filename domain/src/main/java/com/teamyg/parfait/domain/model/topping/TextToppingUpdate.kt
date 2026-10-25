package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitTextId

/**
 * null 인 필드는 그대로 둔다. 서버가 내용 수정을 받지 않아 text 가 없다 — 고치려면 지우고 다시 배치한다.
 */
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
)
