package com.teamyg.parfait.data.source.image.mapper

import com.teamyg.parfait.data.service.model.response.image.ConfirmImageUploadResponse
import com.teamyg.parfait.data.service.model.response.image.IssueImageUploadUrlResponse
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ConfirmedImageVO
import com.teamyg.parfait.domain.model.image.ImageStatus
import com.teamyg.parfait.domain.model.image.ImageUploadUrlVO
import kotlin.time.Duration.Companion.seconds

internal fun IssueImageUploadUrlResponse.toImageUploadUrlVO(): ImageUploadUrlVO = ImageUploadUrlVO(
    imageId = ImageId(imageId),
    uploadUrl = uploadUrl,
    imageUrl = imageUrl,
    expiresIn = expiresIn.seconds,
)

internal fun ConfirmImageUploadResponse.toConfirmedImageVO(): ConfirmedImageVO = ConfirmedImageVO(
    imageId = ImageId(imageId),
    imageUrl = imageUrl,
    status = status.toImageStatus(),
)

private fun String.toImageStatus(): ImageStatus = when (this) {
    ImageStatus.PENDING.name -> ImageStatus.PENDING
    ImageStatus.COMPLETED.name -> ImageStatus.COMPLETED
    else -> ImageStatus.UNKNOWN
}
