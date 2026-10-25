package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitTextId

/**
 * 배치된 텍스트 하나의 부분 수정. null 인 필드는 그대로 두라는 뜻이다.
 *
 * 내용(text)은 배치 뒤 바꿀 수 없어 필드가 없다 — 고치려면 지우고 다시 배치한다.
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
