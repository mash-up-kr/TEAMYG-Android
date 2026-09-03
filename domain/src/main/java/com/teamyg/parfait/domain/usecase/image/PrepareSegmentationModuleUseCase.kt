package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

class PrepareSegmentationModuleUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "PrepareSegmentationModuleUseCase::init" }
    }

    suspend operator fun invoke() = repository.prepareSegmentationModule()
}
