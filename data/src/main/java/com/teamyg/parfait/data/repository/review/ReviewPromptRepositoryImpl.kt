package com.teamyg.parfait.data.repository.review

import com.teamyg.parfait.data.source.review.local.ReviewPromptLocalDataSource
import com.teamyg.parfait.domain.repository.review.ReviewPromptRepository
import javax.inject.Inject

class ReviewPromptRepositoryImpl
@Inject
constructor(
    private val reviewPromptLocalDataSource: ReviewPromptLocalDataSource,
) : ReviewPromptRepository {
    override suspend fun incrementUploadCount() = reviewPromptLocalDataSource.incrementUploadCount()

    override suspend fun checkTrigger(): Boolean = reviewPromptLocalDataSource.checkTrigger()
}
