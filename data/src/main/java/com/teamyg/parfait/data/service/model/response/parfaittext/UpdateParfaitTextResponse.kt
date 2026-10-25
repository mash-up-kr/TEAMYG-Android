package com.teamyg.parfait.data.service.model.response.parfaittext

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 병합 후 전체 값이다. 토핑 일괄 수정 응답의 texts 원소도 이 타입이다.
 */
@Serializable
data class UpdateParfaitTextResponse(
    @SerialName("parfaitTextId")
    val parfaitTextId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("positionX")
    val positionX: Double,
    @SerialName("positionY")
    val positionY: Double,
    @SerialName("positionZ")
    val positionZ: Int,
    @SerialName("scale")
    val scale: Double,
    @SerialName("rotation")
    val rotation: Double,
    @SerialName("color")
    val color: String,
    @SerialName("fontSize")
    val fontSize: Double,
    @SerialName("isFill")
    val isFill: Boolean,
    @SerialName("isStroke")
    val isStroke: Boolean,
)
