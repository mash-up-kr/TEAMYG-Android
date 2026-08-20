package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.model.error.toAppError
import com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSource
import com.teamyg.parfait.data.source.image.remote.PresignedUploadDataSource
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import java.io.File
import javax.inject.Inject

class ImageUploadRepositoryImpl @Inject constructor(
    private val imageRemoteDataSource: ImageRemoteDataSource,
    private val presignedUploadDataSource: PresignedUploadDataSource,
) : ImageUploadRepository {
    override suspend fun upload(
        filePath: String,
        imageType: ImageType,
    ): Result<ImageId> {
        val file = File(filePath)
        // 발급을 먼저 부르면 올릴 것도 없는데 PENDING 행과 S3 키만 남고, 재시도해도 영원히
        // 같은 자리에서 실패한다. 캐시 파일은 다음 흐름이 시작될 때 지워진다
        if (file.isFile.not()) {
            return Result.failure(IllegalStateException("업로드할 파일이 없다 - $filePath").toAppError())
        }
        // 발급 요청과 PUT 헤더가 같은 값을 써야 한다 — 둘 다 S3 서명 대상이고 어긋난 실패는
        // 서버 로그에 남지 않는다. 그래서 여기서 한 번만 정해 양쪽에 넘긴다
        val contentType = contentTypeOf(file) ?: return Result.failure(
            IllegalArgumentException("서버가 받지 않는 확장자다 - ${file.extension}").toAppError(),
        )

        val issued = imageRemoteDataSource
            .issueUploadUrl(fileName = file.name, contentType = contentType, imageType = imageType)
            .getOrElse { return Result.failure(it.toAppError()) }

        presignedUploadDataSource
            .put(uploadUrl = issued.uploadUrl, contentType = contentType, file = file)
            .getOrElse { return Result.failure(it.toAppError()) }

        return imageRemoteDataSource
            .confirmUpload(issued.imageId)
            .map { confirmed -> confirmed.imageId }
            .mapErrorToAppError()
    }

    private fun contentTypeOf(file: File): String? = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> null
    }
}
