package com.teamyg.parfait.data.service.model.response.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param imageId image_meta 행 id. confirm 과 토핑 배치 요청의 imageId 로 쓴다. 배치 행 id(parfaitImageId)와
 *   다른 키다.
 * @param uploadUrl S3 presigned PUT URL. 이 주소로 앱이 직접 PUT 한다(서버를 지나지 않는다).
 *   서명이 쿼리 스트링에 실려 URL 자체가 자격증명이므로 로그에 남기지 않는다(`docs/api/image.md`).
 * @param expiresIn uploadUrl 유효 시간, 초 단위.
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
