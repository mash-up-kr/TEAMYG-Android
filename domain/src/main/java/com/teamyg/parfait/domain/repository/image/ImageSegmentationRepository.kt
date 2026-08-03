package com.teamyg.parfait.domain.repository.image

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationResult

interface ImageSegmentationRepository {
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
}
