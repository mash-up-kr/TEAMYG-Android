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
    val placedBy: PlacedByResponse,
)

@Serializable
data class PlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
)
