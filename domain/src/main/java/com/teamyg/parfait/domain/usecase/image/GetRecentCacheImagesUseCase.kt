package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentCacheImagesUseCase
@Inject
constructor(
    private val recentImageRepository: RecentImageRepository,
) {
    operator fun invoke(): Flow<List<String>> = recentImageRepository.recentCacheImages
}
