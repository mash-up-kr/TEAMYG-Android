package com.teamyg.parfait.data.repository.review

import com.teamyg.parfait.data.source.review.local.ReviewPromptLocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ReviewPromptRepositoryImplTest {
    private val local: ReviewPromptLocalDataSource = mockk(relaxed = true)
    private val repository = ReviewPromptRepositoryImpl(local)

    @Test
    fun markFirstSeenIfAbsent_delegatesToLocal() = runTest {
        repository.markFirstSeenIfAbsent()

        coVerify(exactly = 1) { local.markFirstSeenIfAbsent() }
    }

    @Test
    fun incrementUploadCount_delegatesToLocal() = runTest {
        repository.incrementUploadCount()

        coVerify(exactly = 1) { local.incrementUploadCount() }
    }

    @Test
    fun checkAndMarkTriggered_readsFromLocal() = runTest {
        coEvery { local.checkAndMarkTriggered() } returns true

        assertTrue(repository.checkAndMarkTriggered())
    }
}
