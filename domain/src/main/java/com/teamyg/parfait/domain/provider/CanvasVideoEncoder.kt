package com.teamyg.parfait.domain.provider

import java.io.File

/**
 * 등장 순서대로 캡처된 프레임 시퀀스를 동영상 파일로 인코딩한다. 구현은 플랫폼 인코더(media3
 * Transformer)를 쓰므로 `data` 모듈에 있다 — `domain`은 계약만 안다.
 */
interface CanvasVideoEncoder {
    /**
     * [frames] 를 순서대로 이어 붙여 [outputFile] 에 mp4로 쓴다. [frameDurationMs] 는 프레임
     * 하나가 화면에 머무는 시간이다(전체 재생 시간이 아니다).
     */
    suspend fun encode(
        frames: List<File>,
        frameDurationMs: Long,
        outputFile: File,
    ): Result<Unit>
}
