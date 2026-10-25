package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 캔버스 조회의 texts 원소이자 텍스트 배치 POST 의 응답이다 — 서버가 같은 DTO 를 쓴다.
 *
 * @param positionZ 이미지 토핑과 한 축이다. 겹침 순서는 images 와 합쳐서 정해야 한다.
 */
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
    @SerialName("placedBy")
    val placedBy: PlacedByResponse,
    @SerialName("createdAt")
    val createdAt: String,
)
