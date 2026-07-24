package com.teamyg.parfait.domain.usecase.ui

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.ui.LoadingRepository
import javax.inject.Inject

class ClearLoadingUseCase
@Inject
constructor(
    private val loadingRepository: LoadingRepository,
) {
    init {
        useCaseLogger.i { "ClearLoadingUseCase::init" }
    }

    operator fun invoke(tag: String) {
        val currentMap = loadingRepository.getMutableLoadingMap()

        currentMap -= tag

        loadingRepository.setLoadingMap(currentMap.toMap())
        useCaseLogger.v { "[LOADING] clear - $tag" }
    }
}
