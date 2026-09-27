package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param value type 이 COLOR 면 `#` + 6자리 HEX, IMAGE 면 imageId 가 아니라 저장된 이미지 URL 이다
 *   (`docs/api/parfait.md`).
 */
@Serializable
data class BackgroundResponse(
    @SerialName("type")
    val type: String,
    @SerialName("value")
    val value: String,
)
