package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SegmentationResult
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

class PersistSubjectUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "PersistSubjectUseCase::init" }
    }

    suspend operator fun invoke(candidate: SegmentationCandidate): Result<SegmentationResult> =
        repository.persistSubject(candidate)
}
