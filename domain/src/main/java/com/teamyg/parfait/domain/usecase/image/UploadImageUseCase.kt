package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.repository.image.ImageFileRepository
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import javax.inject.Inject

/**
 * 화면은 uri 를 쥐고 있고 업로드는 파일 절대경로를 받으므로, 그 사이를 여기서 잇는다 —
 * 두 단계 순서를 화면마다 다시 세우지 않기 위해서다.
 */
class UploadImageUseCase
@Inject
constructor(
    private val imageFileRepository: ImageFileRepository,
    private val imageUploadRepository: ImageUploadRepository,
) {
    suspend operator fun invoke(
        uri: String,
        imageType: ImageType,
    ): Result<ImageId> {
        val filePath = imageFileRepository
            .copyToCache(uri)
            .getOrElse { throwable -> return Result.failure(throwable) }

        return imageUploadRepository.upload(filePath = filePath, imageType = imageType)
    }
}
