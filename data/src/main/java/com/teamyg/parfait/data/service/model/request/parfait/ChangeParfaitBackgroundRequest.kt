package com.teamyg.parfait.data.service.model.request.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * type 이 COLOR 면 value(HEX)가, IMAGE 면 imageId 가 필수다 — 서버에 Bean Validation 이 없어
 * 스키마에는 안 드러나고 판정은 서비스·도메인이 한다(`docs/api/conventions.md`). 둘 다 채워 보내도
 * 오류가 아니라 type 에 해당하는 쪽만 쓰이고 나머지는 버려진다.
 *
 * 잘못된 조합을 막는 것은 도메인 `CanvasBackgroundEdit` 이고, 이 DTO 는 서버 형태를 그대로 따른다.
 *
 * @param type COLOR 또는 IMAGE. 누락·모르는 값은 검증 에러가 아니라 역직렬화 실패로 400 INVALID_REQUEST 다.
 * @param value `#` + 6자리 HEX 만 통과한다(대소문자 무관). 3자리 축약·8자리 알파·`#` 없는 형태는
 *   400 INVALID_BACKGROUND 다(`docs/api/parfait.md`).
 * @param imageId 업로드 확인까지 마친 이미지여야 한다. 확인 전이면 409 BACKGROUND_IMAGE_NOT_CONFIRMED,
 *   없는 id 면 404 IMAGE_NOT_FOUND 다(`docs/api/parfait.md`).
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
