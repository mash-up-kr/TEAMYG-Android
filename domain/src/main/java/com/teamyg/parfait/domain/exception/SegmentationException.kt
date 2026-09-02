package com.teamyg.parfait.domain.exception

sealed class
SegmentationException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class ImageNotFound(cause: Throwable?) : SegmentationException("bitmap 초기화 실패", cause)

    class ClientInit(cause: Throwable?) : SegmentationException("client 초기화 실패", cause)

    /**
     * 세그멘테이션 모델이 Play 서비스에서 아직 다운로드되지 않은 상태.
     * 일시적인 상황이므로 잠시 후 재시도하면 해결된다.
     */
    class ModuleNotReady(cause: Throwable?) : SegmentationException("세그멘테이션 모듈 다운로드 미완료", cause)

    class Process(cause: Throwable?) : SegmentationException("세그멘테이션 처리 실패", cause)
}
