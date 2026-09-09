package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.data.model.image.PreparedUploadImage
import com.teamyg.parfait.domain.model.image.ImageType
import java.io.File

/** 업로드 직전에 이미지를 서버로 보낼 형태로 맞춘다 */
interface UploadImagePreprocessor {
    suspend fun prepare(
        file: File,
        imageType: ImageType,
    ): Result<PreparedUploadImage>
}
