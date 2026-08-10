package com.teamyg.parfait.domain.model.image

/**
 * 업로드할 이미지의 용도. 앱이 서버로 보내는 값이라 알 수 없는 값이 생길 수 없어
 * UNKNOWN 폴백을 두지 않는다(서버가 주는 [ImageStatus] 와 다른 점).
 *
 * 서버는 이 이름의 소문자를 S3 키 접두사로 쓴다.
 */
enum class ImageType {
    NUKKI,
    BACKGROUND,
}
