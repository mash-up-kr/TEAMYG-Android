package com.teamyg.parfait.domain.usecase.ui

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.ui.LoadingRepository
import javax.inject.Inject

class ShowLoadingUseCase
@Inject
constructor(
    private val loadingRepository: LoadingRepository,
) {
    init {
        useCaseLogger.i { "ShowLoadingUseCase::init" }
    }

    operator fun invoke(tag: String) {
        val currentMap = loadingRepository.getMutableLoadingMap()
        val currentCount = currentMap[tag] ?: 0

        currentMap[tag] = currentCount + 1

        loadingRepository.setLoadingMap(currentMap.toMap())
        useCaseLogger.v { "[LOADING] show - $tag" }
    }
}
