package com.teamyg.parfait.data.model.exception

/**
 * 고른 이미지를 업로드에 쓸 수 없다 — 형식이 서버 계약 밖이거나 파일을 열지 못했다.
 *
 * `IllegalArgumentException` 을 그대로 올리지 않는 이유: `require` 는 업로드 경로 어디서든
 * 쓰이므로, 받는 쪽이 타입만 보고 "이 사진이 문제"라고 판정하면 무관한 실패까지 그렇게
 * 읽힌다. 이 갈래인지 아는 것은 던지는 자리뿐이라 여기서 이름을 붙인다.
 */
class UnsupportedImageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
