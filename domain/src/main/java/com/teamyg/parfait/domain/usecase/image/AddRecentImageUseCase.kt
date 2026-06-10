package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import javax.inject.Inject

class AddRecentImageUseCase
@Inject
constructor(
    private val recentImageRepository: RecentImageRepository,
) {
    suspend operator fun invoke(uri: String) {
        recentImageRepository.addRecentImage(uri)
    }
}
