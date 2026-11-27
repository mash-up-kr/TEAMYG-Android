package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSoloCanvasFlowUseCase @Inject constructor(
    private val soloSpaceRepository: SoloSpaceRepository,
) {
    operator fun invoke(): Flow<Result<SoloCanvasVO>> = soloSpaceRepository.canvas
}
