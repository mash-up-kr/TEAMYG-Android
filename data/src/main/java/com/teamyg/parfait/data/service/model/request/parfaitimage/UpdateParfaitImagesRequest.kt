package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 여러 토핑의 위치·크기·각도를 한 요청으로 수정한다.
 *
 * 부분 성공이 없다 — 서버가 트랜잭션 하나로 묶어 항목 하나가 걸리면 전부 롤백하고,
 * 어느 항목이 걸렸는지는 응답에 없다(`api/parfait-image.md`).
 */
@Serializable
data class UpdateParfaitImagesRequest(
    @SerialName("items")
    val items: List<UpdateParfaitImageItemRequest>,
)

/**
 * null 인 필드는 서버가 기존 값을 유지한다(ParfaitImage.update 의 ?: 병합).
 *
 * @RemoteJson Json 이 explicitNulls 기본값(true)을 쓰므로 안 바꾸는 필드도 "positionX": null 로
 * 실려 나간다. 실제 결정 인자는 encodeDefaults 다 — @RemoteJson 은 encodeDefaults = true 라서,
 * 다섯 축이 전부 `= null` 기본값이어도 프로퍼티가 생략되지 않고 그대로 실린다
 * (`JsonModule.provideRemoteJson`). encodeDefaults = false 였다면 explicitNulls 와 무관하게
 * 기본값과 같은 필드는 통째로 생략됐을 것이다. 서버에게 키 부재와 명시적 null 이 같은 뜻이라
 * 동작은 정확하다. 이 API 하나 때문에 전역 Json 설정을 바꾸지 않는다.
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
