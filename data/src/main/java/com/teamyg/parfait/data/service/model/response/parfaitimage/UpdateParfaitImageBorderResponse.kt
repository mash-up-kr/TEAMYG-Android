package com.teamyg.parfait.data.service.model.response.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 테두리 수정 응답. 이 도메인에서 테두리를 되돌려주는 유일한 응답이다 —
 * 배치 확정·위치 수정 둘 다 테두리 필드가 없다(`api/parfait-image.md`).
 */
@Serializable
data class UpdateParfaitImageBorderResponse(
    @SerialName("parfaitImageId")
    val parfaitImageId: Long,
    @SerialName("borderType")
    val borderType: String,
    @SerialName("borderColor")
    val borderColor: String? = null,
    @SerialName("borderWidth")
    val borderWidth: Double? = null,
)
