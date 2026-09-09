package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.data.model.image.PreparedUploadImage
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.SourceLongSide
import java.io.File

/** 업로드 직전에 이미지를 서버로 보낼 형태로 맞춘다 */
interface UploadImagePreprocessor {
    /**
     * @param sourceLongSide 모르면 null 이다 — 그때는 잘린 판 상한만 걸린다
     */
    suspend fun prepare(
        file: File,
        imageType: ImageType,
        sourceLongSide: SourceLongSide?,
    ): Result<PreparedUploadImage>
}
