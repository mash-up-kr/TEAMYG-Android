package com.teamyg.parfait.domain.usecase.ui

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.ui.LoadingRepository
import javax.inject.Inject

class IsLoadingUseCase
@Inject
constructor(
    private val loadingRepository: LoadingRepository,
) {
    init {
        useCaseLogger.i { "ClearLoadingUseCase::init" }
    }

    operator fun invoke(tag: String): Boolean {
        return loadingRepository.containTag(tag)
    }
}
