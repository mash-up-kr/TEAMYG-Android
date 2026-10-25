package com.teamyg.parfait.data.service.model.request.parfaittext

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param text 2000자 상한을 UTF-16 코드 유닛으로 센다 — 이모지 하나가 2다(`docs/api/parfait-text.md`).
 */
@Serializable
data class PlaceParfaitTextRequest(
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
