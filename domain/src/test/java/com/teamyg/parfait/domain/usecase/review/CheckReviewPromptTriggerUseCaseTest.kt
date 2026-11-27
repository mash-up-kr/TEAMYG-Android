package com.teamyg.parfait.domain.usecase.review

import com.teamyg.parfait.domain.repository.review.ReviewPromptRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class CheckReviewPromptTriggerUseCaseTest {
    private val reviewPromptRepository: ReviewPromptRepository = mockk(relaxed = true)
    private val useCase = CheckReviewPromptTriggerUseCase(reviewPromptRepository)

    @Test
    fun invoke_delegates() = runTest {
        coEvery { reviewPromptRepository.checkTrigger() } returns true

        assertTrue(useCase())
    }
}
