package com.teamyg.parfait.domain.usecase.review

import com.teamyg.parfait.domain.repository.review.ReviewPromptRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ReviewPromptUseCaseTest {
    private val reviewPromptRepository: ReviewPromptRepository = mockk(relaxed = true)
    private val useCase = ReviewPromptUseCase(reviewPromptRepository)

    @Test
    fun markFirstSeen_delegates() = runTest {
        useCase.markFirstSeen()

        coVerify(exactly = 1) { reviewPromptRepository.markFirstSeenIfAbsent() }
    }

    @Test
    fun recordUpload_delegates() = runTest {
        useCase.recordUpload()

        coVerify(exactly = 1) { reviewPromptRepository.incrementUploadCount() }
    }

    @Test
    fun checkAndMarkTriggered_delegates() = runTest {
        coEvery { reviewPromptRepository.checkAndMarkTriggered() } returns true

        assertTrue(useCase.checkAndMarkTriggered())
    }
}
