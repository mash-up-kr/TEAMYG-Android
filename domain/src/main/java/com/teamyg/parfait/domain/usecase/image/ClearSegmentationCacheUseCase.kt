package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

class ClearSegmentationCacheUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "ClearSegmentationCacheUseCase::init" }
    }

    suspend operator fun invoke() = repository.clearSegmentationCache()
}
