package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param value type 이 COLOR 면 색 문자열, IMAGE 면 URL 이다.
 */
@Serializable
data class BackgroundResponse(
    @SerialName("type")
    val type: String,
    @SerialName("value")
    val value: String,
)
