package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 토핑 일괄 수정의 텍스트 항목. 단건 텍스트 PATCH 와 달리 위치·크기·각도만 바꾼다.
 */
@Serializable
data class UpdateParfaitTextItemRequest(
    @SerialName("parfaitTextId")
    val parfaitTextId: Long,
    @SerialName("positionX")
    val positionX: Double? = null,
    @SerialName("positionY")
    val positionY: Double? = null,
    @SerialName("positionZ")
    val positionZ: Int? = null,
    @SerialName("scale")
    val scale: Double? = null,
    @SerialName("rotation")
    val rotation: Double? = null,
)
