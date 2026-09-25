package com.teamyg.parfait.data.service.model.request.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param fileName 서버가 현재 이 값을 쓰지 않는다. S3 키는 UUID 로 만들고 확장자는
 *   contentType 에서 유도한다(계약 문서 `docs/api/image.md` 미결). 서버가 쓰기 시작할 때
 *   값이 맞도록 실제 파일명을 보낸다. 빈 문자열은 서버 @NotBlank 에 걸려 400 이다.
 * @param contentType image/png 또는 image/jpeg 만 서버가 받는다. 그 외는 400 INVALID_CONTENT_TYPE.
 *   presign 서명에 들어가므로 S3 PUT 의 Content-Type 헤더도 이 값과 같아야 한다. 다르면 S3 가 거절하고
 *   서버 로그에는 남지 않는다(`docs/api/image.md`).
 * @param imageType NUKKI(토핑 누끼) 또는 BACKGROUND(배경). enum 밖 값은 400 INVALID_REQUEST 다.
 *   서버 OpenAPI 의 required 목록에는 없지만 Kotlin 비널 타입이라 누락하면 400 이다.
 *   springdoc 이 required 를 Bean Validation 애노테이션에서만 유도하기 때문이다.
 *   nullable 로 만들지 않는다(`docs/api/conventions.md`).
 */
@Serializable
data class IssueImageUploadUrlRequest(
    @SerialName("fileName")
    val fileName: String,
    @SerialName("contentType")
    val contentType: String,
    @SerialName("imageType")
    val imageType: String,
)
