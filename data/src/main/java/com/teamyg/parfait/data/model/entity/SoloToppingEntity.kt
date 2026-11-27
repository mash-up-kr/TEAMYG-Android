package com.teamyg.parfait.data.model.entity

import kotlinx.serialization.Serializable

/**
 * @param fileName `images/` 안의 파일명. 절대경로를 두지 않는 이유는 [SoloBackgroundEntity.Image] 와 같다
 * @param borderColor [borderWidth] 와 함께 있을 때만 테두리가 있다
 */
@Serializable
data class SoloToppingEntity(
    val id: Long,
    val fileName: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val borderColor: String? = null,
    val borderWidth: Double? = null,
    val createdAtEpochMillis: Long,
)
