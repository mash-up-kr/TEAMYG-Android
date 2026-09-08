package com.teamyg.parfait.data.source.image.local

import com.teamyg.parfait.data.model.image.UploadImageFormat
import com.teamyg.parfait.domain.model.image.ImageType
import java.io.File

/**
 * @param isTemporary 전처리가 새로 만든 파일이라 부른 쪽이 지워야 한다. 거짓이면 넘겨받은
 *   파일 그대로라 수명은 부른 쪽 밖에 있다.
 */
data class PreparedUploadImage(
    val file: File,
    val format: UploadImageFormat,
    val isTemporary: Boolean,
)

/** 업로드 직전에 이미지를 서버로 보낼 형태로 맞춘다 */
interface UploadImagePreprocessor {
    suspend fun prepare(
        file: File,
        imageType: ImageType,
    ): Result<PreparedUploadImage>
}
