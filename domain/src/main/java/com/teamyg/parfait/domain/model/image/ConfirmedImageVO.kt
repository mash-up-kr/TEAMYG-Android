package com.teamyg.parfait.domain.model.image

import com.teamyg.parfait.domain.model.id.ImageId

data class ConfirmedImageVO(
    val imageId: ImageId,
    val imageUrl: String,
    val status: ImageStatus,
)
