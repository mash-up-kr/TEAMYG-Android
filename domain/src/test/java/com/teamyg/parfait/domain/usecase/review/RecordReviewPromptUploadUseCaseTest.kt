package com.teamyg.parfait.domain.usecase.review

import com.teamyg.parfait.domain.repository.review.ReviewPromptRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RecordReviewPromptUploadUseCaseTest {
    private val reviewPromptRepository: ReviewPromptRepository = mockk(relaxed = true)
    private val useCase = RecordReviewPromptUploadUseCase(reviewPromptRepository)

    @Test
    fun invoke_delegates() = runTest {
        useCase()

        coVerify(exactly = 1) { reviewPromptRepository.incrementUploadCount() }
    }

    @Test
    fun invoke_repositoryThrows_doesNotPropagate() = runTest {
        // 서버 저장은 이미 끝난 뒤라, 집계 실패가 업로드 자체를 에러로 만들면 안 된다
        coEvery { reviewPromptRepository.incrementUploadCount() } throws IllegalStateException("datastore")

        useCase()
    }
}
