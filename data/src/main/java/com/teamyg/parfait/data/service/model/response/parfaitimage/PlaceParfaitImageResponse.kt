package com.teamyg.parfait.data.service.model.response.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 요청에 보낸 borderType·borderColor·borderWidth 가 응답에 없다. 서버가 저장만 하고
 * 돌려주지 않는다(`api/parfait-image.md`).
 *
 * @param imageId 요청에 넣은 image_meta id 그대로.
 * @param parfaitImageId 배치 행의 id. 이후 PATCH 가 쓰는 키다.
 */
@Serializable
data class PlaceParfaitImageResponse(
    @SerialName("parfaitImageId")
    val parfaitImageId: Long,
    @SerialName("imageId")
    val imageId: Long,
    @SerialName("imageUrl")
    val imageUrl: String,
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
    @SerialName("placedBy")
    val placedBy: PlaceParfaitImagePlacedByResponse,
)

/**
 * 배치자. 이름이 긴 것은 캔버스 응답의 `PlacedByResponse` 와 구분하려는 서버 이름을 따랐기
 * 때문이다(사유는 `api/parfait-image.md`) — 짧게 고치면 거울이 깨진다.
 *
 * @param nameTagChip 읽는 화면이 생길 때 도메인으로 올린다.
 */
@Serializable
data class PlaceParfaitImagePlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
)
