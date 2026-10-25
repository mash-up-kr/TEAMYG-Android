package com.teamyg.parfait.data.service.model.request.parfaittext

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * null 인 필드는 서버가 기존 값을 유지한다. text 키는 서버가 받지 않는다(넣어도 무시된다).
 */
@Serializable
data class UpdateParfaitTextRequest(
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
    @SerialName("color")
    val color: String? = null,
    @SerialName("fontSize")
    val fontSize: Double? = null,
    @SerialName("isFill")
    val isFill: Boolean? = null,
    @SerialName("isStroke")
    val isStroke: Boolean? = null,
)
