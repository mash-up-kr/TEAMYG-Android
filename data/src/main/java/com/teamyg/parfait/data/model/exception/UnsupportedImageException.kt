package com.teamyg.parfait.data.model.exception

/**
 * 고른 이미지를 업로드에 쓸 수 없다 — 형식이 서버 계약 밖이거나 파일을 열지 못했다.
 *
 * `IllegalArgumentException` 은 업로드 경로 어디서든 나올 수 있어, 그 타입으로 "이 사진이
 * 문제"라고 판정하면 무관한 실패까지 섞인다.
 */
class UnsupportedImageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
