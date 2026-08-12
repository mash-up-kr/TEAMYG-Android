package com.teamyg.parfait.data.service.model.response.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param uploadUrl S3 presigned PUT URL. 이 주소로 앱이 직접 PUT 한다(서버를 지나지 않는다).
 * @param imageUrl 업로드 후 접근할 공개 주소.
 * @param expiresIn uploadUrl 유효 시간, 초 단위. 매퍼가 Duration 으로 바꾼다.
 */
@Serializable
data class IssueImageUploadUrlResponse(
    @SerialName("imageId")
    val imageId: Long,
    @SerialName("uploadUrl")
    val uploadUrl: String,
    @SerialName("imageUrl")
    val imageUrl: String,
    @SerialName("expiresIn")
    val expiresIn: Long,
)
