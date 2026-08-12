package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 서버 계약을 그대로 미러링한 평면 DTO. sealed 는 domain 쪽에만 산다.
 *
 * @param imageId COMPLETED 상태여야 한다. PENDING 이면 409 IMAGE_NOT_CONFIRMED.
 * @param borderType NONE 또는 SOLID. enum 밖 값은 Jackson 역직렬화가 먼저 깨져
 *   400 INVALID_REQUEST 다(도메인 코드가 아니라 공통 코드).
 * @param borderColor borderType=SOLID 면 필수. NONE 이면 서버가 무시한다.
 * @param borderWidth borderType=SOLID 면 필수. NONE 이면 서버가 무시한다.
 */
@Serializable
data class PlaceParfaitImageRequest(
    @SerialName("imageId")
    val imageId: Long,
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
    @SerialName("borderType")
    val borderType: String,
    @SerialName("borderColor")
    val borderColor: String? = null,
    @SerialName("borderWidth")
    val borderWidth: Double? = null,
)
