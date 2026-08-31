package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 배치된 토핑 하나의 부분 수정. null 인 축은 그대로 두라는 뜻이다.
 *
 * [ToppingTransform] 과 달리 축이 전부 널 허용이라 "안 바꾼다"를 표현할 수 있다.
 */
data class ToppingTransformUpdate(
    val parfaitImageId: ParfaitImageId,
    val positionX: Double? = null,
    val positionY: Double? = null,
    val positionZ: Int? = null,
    val scale: Double? = null,
    val rotation: Double? = null,
)
