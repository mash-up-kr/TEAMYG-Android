package com.teamyg.parfait.data.service.model.response.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param status ImageStatus enum 이름 문자열. 성공 응답이면 항상 COMPLETED 다 —
 *   서버는 PENDING 인 것만 통과시켜 COMPLETED 로 전이시키고, 이미 COMPLETED 인 것은
 *   409 IMAGE_ALREADY_CONFIRMED 로 거른다.
 */
@Serializable
data class ConfirmImageUploadResponse(
    @SerialName("imageId")
    val imageId: Long,
    @SerialName("imageUrl")
    val imageUrl: String,
    @SerialName("status")
    val status: String,
)
