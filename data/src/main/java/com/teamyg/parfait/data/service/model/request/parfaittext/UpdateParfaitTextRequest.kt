package com.teamyg.parfait.data.service.model.request.parfaittext

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 배치 뒤 내용은 바꿀 수 없어 text 가 없다(`docs/api/parfait-text.md`). */
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
    @SerialName("textAlign")
    val textAlign: String? = null,
)
