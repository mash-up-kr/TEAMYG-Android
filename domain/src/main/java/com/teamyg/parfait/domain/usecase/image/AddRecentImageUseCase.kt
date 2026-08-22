package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.image.RecentImageKind
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

    suspend operator fun invoke(
        source: String,
        kind: RecentImageKind,
    ) {
        val stableUri: String? = runSuspendCatching {
            recentImageRepository.storeRecentImageInInternalStorage(
                source = source,
                kind = kind,
            )
        }.getOrNull()

        if (stableUri == null) {
            useCaseLogger.d { "AddRecentImageUseCase - stableUri is null" }
            return
        }

        val evicted: List<String> = recentImageRepository.addAndGetEvictedCacheFileName(
            uri = stableUri,
            kind = kind,
        )

        useCaseLogger.d { "AddRecentImageUseCase - evicted.size: ${evicted.size}" }

        evicted.forEach {
            recentImageRepository.deleteRecentImageInInternalStorage(it)
        }
    }
}
