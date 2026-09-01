package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateParfaitImagesRequest(
    @SerialName("items")
    val items: List<UpdateParfaitImageItemRequest>,
)

/**
 * null 인 축은 서버가 기존 값을 유지한다.
 *
 * 안 바꾸는 축도 `"positionX": null` 로 실려 나간다 — 기본값을 생략하지 않는 직렬화 설정 탓이고,
 * 서버에게 키 부재와 명시적 null 이 같은 뜻이라 동작은 정확하다. 이 API 하나 때문에 전역 Json
 * 설정을 바꾸지 않는다.
 */
@Serializable
data class UpdateParfaitImageItemRequest(
    @SerialName("parfaitImageId")
    val parfaitImageId: Long,
    @SerialName("positionX")
    val positionX: Double? = null,
    @SerialName("positionY")
    val positionY: Double? = null,
    @SerialName("positionZ")
    val positionZ: Int? = null,
    @SerialName("scale")
    val scale: Double? = null,
    @SerialName("rotation")
    val rotation: Double? = null,
)
