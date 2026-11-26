package com.teamyg.parfait.data.model.entity

import kotlinx.serialization.Serializable

/** @param textAlign `TextToppingAlign` 상수 이름. 모르는 값은 가운데 정렬로 읽는다 */
@Serializable
data class SoloTextEntity(
    val id: Long,
    val text: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val color: String,
    val fontSize: Double,
    val isFill: Boolean,
    val isStroke: Boolean,
    val textAlign: String,
    val createdAtEpochMillis: Long,
)
