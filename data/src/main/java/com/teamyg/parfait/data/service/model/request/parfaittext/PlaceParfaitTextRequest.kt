package com.teamyg.parfait.data.service.model.request.parfaittext

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param text 공백만이거나 2000자를 넘으면 400 INVALID_TEXT. 서버는 UTF-16 코드 유닛으로 센다.
 * @param scale fontSize 에 곱하는 배율이다. scale·fontSize 가 0 이하면 400 INVALID_TEXT_STYLE.
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
