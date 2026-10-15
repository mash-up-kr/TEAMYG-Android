package com.teamyg.parfait.data.service.model.response.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 원소는 단건 수정 응답과 같은 타입이다 — 서버가 그 DTO 를 그대로 재사용한다.
 *
 * 순서를 계약이 보장하지 않으므로 소비 측은 parfaitImageId 로 맞춘다(`docs/api/parfait-image.md`).
 * 요청에 같은 parfaitImageId 가 중복되면 서버가 막지 않아 응답에도 두 번 나온다.
 */
@Serializable
data class UpdateParfaitImagesResponse(
    @SerialName("images")
    val images: List<UpdateParfaitImageResponse>,
)
