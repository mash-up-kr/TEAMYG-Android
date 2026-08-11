package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 부분 수정. null 인 필드는 서버가 기존 값을 유지한다(ParfaitImage.update 의 ?: 병합).
 *
 * @RemoteJson Json 이 explicitNulls 기본값을 쓰므로 안 바꾸는 필드도 "positionX": null 로
 * 실려 나간다. 서버에게 키 부재와 명시적 null 이 같은 뜻이라 동작은 정확하다.
 * 이 API 하나 때문에 전역 Json 설정을 바꾸지 않는다.
 *
 * 전 필드가 null 인 빈 패치도 서버가 받아들이며 updatedAt 만 올라간다(에러가 아니다).
 */
@Serializable
data class UpdateParfaitImageRequest(
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
