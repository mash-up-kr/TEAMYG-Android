package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

class DecodeImageUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "DecodeImageUseCase::init" }
    }

    suspend operator fun invoke(uri: String): BitmapWrapper = repository.decodeImage(uri)
}
