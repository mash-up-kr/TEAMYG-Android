package com.teamyg.parfait.data.service.model.request.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 캔버스 배경 변경 요청.
 *
 * type 이 COLOR 면 value(HEX)가, IMAGE 면 imageId 가 필수다 — 서버에 Bean Validation 이 없어
 * 스키마에는 안 드러나고 판정은 서비스·도메인이 한다(`api/conventions.md`). 둘 다 채워 보내도
 * 오류가 아니라 type 에 해당하는 쪽만 쓰이고 나머지는 버려진다.
 *
 * 이 평면 형태는 서버의 거울이다 — 잘못된 조합을 막는 것은 도메인
 * `CanvasBackgroundEdit` 이고 여기서 다시 세우지 않는다.
 */
@Serializable
data class ChangeParfaitBackgroundRequest(
    @SerialName("type")
    val type: String,
    @SerialName("value")
    val value: String? = null,
    @SerialName("imageId")
    val imageId: Long? = null,
)
