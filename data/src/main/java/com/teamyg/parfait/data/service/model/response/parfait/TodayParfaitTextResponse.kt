package com.teamyg.parfait.data.service.model.response.parfait

import com.teamyg.parfait.data.service.model.response.parfaittext.DEFAULT_TEXT_ALIGN
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 캔버스 texts 원소와 텍스트 배치 POST 응답이 공유하는 타입이다(`docs/api/parfait-text.md`). */
@Serializable
data class TodayParfaitTextResponse(
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
    @SerialName("placedBy")
    val placedBy: PlacedByResponse,
    @SerialName("createdAt")
    val createdAt: String,
)
