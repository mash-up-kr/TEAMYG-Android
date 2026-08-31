package com.teamyg.parfait.data.service.model.response.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 원소는 단건 수정 응답과 같은 타입이다 — 서버가 그 DTO 를 그대로 재사용한다.
 *
 * 순서를 계약이 보장하지 않으므로 소비 측은 parfaitImageId 로 맞춘다(`api/parfait-image.md`).
 */
@Serializable
data class UpdateParfaitImagesResponse(
    @SerialName("images")
    val images: List<UpdateParfaitImageResponse>,
)
