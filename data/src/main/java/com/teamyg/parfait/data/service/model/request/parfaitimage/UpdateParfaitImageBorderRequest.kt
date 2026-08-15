package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 테두리 수정 요청.
 *
 * 위치 수정(UpdateParfaitImageRequest)과 달리 부분 병합이 아니다 — 서버가 세 필드를
 * 통째로 덮는다. borderType 이 SOLID 인데 색·두께가 없으면 400 INVALID_BORDER 다.
 */
@Serializable
data class UpdateParfaitImageBorderRequest(
    @SerialName("borderType")
    val borderType: String,
    @SerialName("borderColor")
    val borderColor: String? = null,
    @SerialName("borderWidth")
    val borderWidth: Double? = null,
)
