package com.teamyg.parfait.data.service.model.response.parfaittext

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("textAlign")
    val textAlign: String = DEFAULT_TEXT_ALIGN,
)
