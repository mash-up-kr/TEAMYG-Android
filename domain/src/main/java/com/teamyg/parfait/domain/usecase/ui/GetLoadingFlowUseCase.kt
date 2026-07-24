package com.teamyg.parfait.domain.usecase.ui

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.ui.LoadingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLoadingFlowUseCase
@Inject
constructor(
    private val loadingRepository: LoadingRepository,
) {
    init {
        useCaseLogger.i { "GetLoadingFlowUseCase::init" }
    }

    operator fun invoke(): Flow<Boolean> {
        return loadingRepository.loadingFlow
    }
}
