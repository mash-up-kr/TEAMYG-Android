package com.teamyg.parfait.data.model.entity

import kotlinx.serialization.Serializable

/**
 * 혼자 공간 `canvas.json` 의 저장 형태.
 *
 * @param nextId 토핑·텍스트가 함께 쓰는 다음 id. 비운 뒤에도 이어 써야 화면이 들고 있던 옛 id 와 겹치지 않는다
 */
@Serializable
data class SoloSpaceRecordEntity(
    val nextId: Long = 1,
    val background: SoloBackgroundEntity? = null,
    val toppings: List<SoloToppingEntity> = emptyList(),
    val texts: List<SoloTextEntity> = emptyList(),
)
