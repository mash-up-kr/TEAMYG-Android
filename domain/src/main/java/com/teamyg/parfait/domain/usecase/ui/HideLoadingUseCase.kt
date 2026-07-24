package com.teamyg.parfait.domain.usecase.ui

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.ui.LoadingRepository
import javax.inject.Inject

class HideLoadingUseCase
@Inject
constructor(
    private val loadingRepository: LoadingRepository,
) {
    init {
        useCaseLogger.i { "HideLoadingUseCase::init" }
    }

    operator fun invoke(tag: String) {
        val currentMap = loadingRepository.getMutableLoadingMap()
        val currentCount = currentMap[tag] ?: 0

        when {
            currentCount > 1 -> currentMap[tag] = currentCount - 1
            else -> currentMap.remove(tag)
        }

        loadingRepository.setLoadingMap(currentMap.toMap())
        useCaseLogger.v { "[LOADING] hide - $tag" }
    }
}
