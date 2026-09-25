package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 서버 계약을 그대로 미러링한 평면 DTO. sealed 는 domain 쪽에만 산다.
 *
 * @param imageId COMPLETED 상태여야 한다. PENDING 이면 409 IMAGE_NOT_CONFIRMED.
 *   같은 parfait 에 같은 imageId 로 다시 보내면 새 행 없이 기존 배치가 이동하고,
 *   배치자도 호출자로 바뀐다(`docs/api/parfait-image.md`).
 * @param borderType NONE 또는 SOLID. enum 밖 값은 Jackson 역직렬화가 먼저 깨져
 *   400 INVALID_REQUEST 다(도메인 코드가 아니라 공통 코드).
 * @param borderColor borderType=SOLID 면 필수, 없으면 400 INVALID_BORDER. NONE 이면 검증 없이 보낸 값이
 *   그대로 저장된다.
 * @param borderWidth borderType=SOLID 면 필수, 없으면 400 INVALID_BORDER. NONE 이면 검증 없이 보낸 값이
 *   그대로 저장된다. 범위 검증은 서버에 없다.
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
