package com.teamyg.parfait.domain.exception

sealed class
SegmentationException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class ImageNotFound(cause: Throwable?) : SegmentationException("bitmap 초기화 실패", cause)
    class ClientInit(cause: Throwable?) : SegmentationException("client 초기화 실패", cause)
}
