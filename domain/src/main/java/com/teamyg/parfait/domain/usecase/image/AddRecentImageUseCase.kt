package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import javax.inject.Inject

class AddRecentImageUseCase
@Inject
constructor(
    private val recentImageRepository: RecentImageRepository,
) {
    suspend operator fun invoke(uri: String) {
        val stableUri: String = runCatching { recentImageRepository.storeRecentImageInInternalStorage(uri) }
            .getOrNull()
            ?: return

        val evicted: List<String> = recentImageRepository.addAndGetEvictedCacheFileName(stableUri)

        evicted.forEach {
            recentImageRepository.deleteRecentImageInInternalStorage(it)
        }
    }
}
