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
 * 배치자. 서버가 캔버스 응답의 동명 클래스와 스키마 충돌을 없애려고 이쪽만 개명했다.
 *
 * @param nameTagChip **아직 도메인으로 올리지 않는다** — 이 값을 읽는 화면이 0건이다.
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
