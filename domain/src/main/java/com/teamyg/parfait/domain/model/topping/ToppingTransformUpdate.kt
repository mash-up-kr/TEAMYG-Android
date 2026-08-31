package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 배치된 토핑 하나의 부분 수정. null 인 축은 그대로 두라는 뜻이라 [ToppingTransform] 으로는
 * 대신할 수 없다.
 */
data class ToppingTransformUpdate(
    val parfaitImageId: ParfaitImageId,
    val positionX: Double? = null,
    val positionY: Double? = null,
    val positionZ: Int? = null,
    val scale: Double? = null,
    val rotation: Double? = null,
)
