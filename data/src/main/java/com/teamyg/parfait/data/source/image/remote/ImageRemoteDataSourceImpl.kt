package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ImageService
import com.teamyg.parfait.data.service.model.request.image.IssueImageUploadUrlRequest
import com.teamyg.parfait.data.source.image.mapper.toConfirmedImageVO
import com.teamyg.parfait.data.source.image.mapper.toImageUploadUrlVO
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ConfirmedImageVO
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.ImageUploadUrlVO
import javax.inject.Inject

class ImageRemoteDataSourceImpl @Inject constructor(
    private val imageService: ImageService,
    private val apiCaller: ApiCaller,
) : ImageRemoteDataSource {
    override suspend fun issueUploadUrl(
        fileName: String,
        contentType: String,
        imageType: ImageType,
    ): Result<ImageUploadUrlVO> = apiCaller.safeApiCall(
        block = {
            imageService.postImages(
                IssueImageUploadUrlRequest(
                    fileName = fileName,
                    contentType = contentType,
                    imageType = imageType.name,
                ),
            )
        },
        transform = { it.toImageUploadUrlVO() },
    )

    override suspend fun confirmUpload(imageId: ImageId): Result<ConfirmedImageVO> = apiCaller
        .safeApiCall(
            block = { imageService.postImagesByImageIdConfirm(imageId.value) },
            transform = { it.toConfirmedImageVO() },
        )
}
