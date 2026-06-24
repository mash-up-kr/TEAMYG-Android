package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import javax.inject.Inject

class AddRecentImageUseCase
@Inject
constructor(
    private val recentImageRepository: RecentImageRepository,
) {
    init {
        useCaseLogger.i { "AddRecentImageUseCase::init" }
    }

    suspend operator fun invoke(uri: String) {
        val stableUri: String? = runCatching {
            recentImageRepository.storeRecentImageInInternalStorage(uri)
        }.getOrNull()

        if (stableUri == null) {
            useCaseLogger.d { "AddRecentImageUseCase - stableUri is null" }
            return
        }

        val evicted: List<String> = recentImageRepository.addAndGetEvictedCacheFileName(stableUri)

        useCaseLogger.d { "AddRecentImageUseCase - evicted.size: ${evicted.size}" }

        evicted.forEach {
            recentImageRepository.deleteRecentImageInInternalStorage(it)
        }
    }
}
