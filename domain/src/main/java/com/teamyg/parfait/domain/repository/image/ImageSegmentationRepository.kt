package com.teamyg.parfait.domain.repository.image

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationResult

interface ImageSegmentationRepository {
    /**
     * [uri] 가 가리키는 이미지를 비트맵으로 읽는다.
     *
     * **실패하면 던진다.** URI 가 만료됐거나 파일이 깨졌으면 디코더의 예외가 그대로 올라오므로
     * 호출부가 감싸야 한다.
     */
    suspend fun decodeImage(uri: String): BitmapWrapper

    suspend fun segmentImage(bitmapWrapper: BitmapWrapper): Result<SegmentationResult>

    /**
     * 손으로 다듬은 결과 이미지를 캐시에 저장한다.
     *
     * 화면 사이에서는 비트맵 대신 경로를 주고받아야 해서, 편집을 마칠 때 한 번 파일로 떨군다.
     *
     * @return 저장된 파일의 절대 경로
     */
    suspend fun saveEditedImage(bitmapWrapper: BitmapWrapper): Result<String>

    /**
     * 세그멘테이션이 만든 캐시 파일을 전부 지운다.
     *
     * **새 흐름이 시작될 때 부른다.** 이전 흐름이 남긴 파일은 그 시점에 아무도 보지 않는다 —
     * 캔버스로 돌아와야만 새 흐름을 시작할 수 있고, 돌아오는 길에 그 화면들이 이미 걷힌다.
     */
    suspend fun clearSegmentationCache()
}
