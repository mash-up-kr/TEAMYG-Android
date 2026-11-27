package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.model.solo.SoloBackgroundEdit
import com.teamyg.parfait.domain.repository.image.ImageFileRepository
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChangeSoloBackgroundUseCaseTest {
    private val imageFileRepository: ImageFileRepository = mockk()
    private val soloSpaceRepository: SoloSpaceRepository = mockk()
    private val changeBackground = ChangeSoloBackgroundUseCase(
        imageFileRepository = imageFileRepository,
        soloSpaceRepository = soloSpaceRepository,
    )

    @Test
    fun image_copiesToCacheThenChangesWithCachedPath() = runTest {
        // Given 캐시 복사가 성공한다
        coEvery { imageFileRepository.copyToCache("content://x") } returns Result.success("/cache/a.jpg")
        coEvery { soloSpaceRepository.changeBackground(any()) } returns Result.success(Unit)

        val result = changeBackground.image("content://x")

        // Then 캐시 사본 경로로 저장소를 한 번 부른다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            soloSpaceRepository.changeBackground(SoloBackgroundEdit.Image("/cache/a.jpg"))
        }
    }

    @Test
    fun image_copyFails_doesNotTouchRepository() = runTest {
        // Given 캐시 복사가 실패한다
        val failure = IllegalStateException("copy failed")
        coEvery { imageFileRepository.copyToCache(any()) } returns Result.failure(failure)

        val result = changeBackground.image("content://x")

        // Then 그 실패를 돌려주고 저장소는 건드리지 않는다
        assertEquals(failure, result.exceptionOrNull())
        coVerify(exactly = 0) { soloSpaceRepository.changeBackground(any()) }
    }

    @Test
    fun color_changesWithColorEdit() = runTest {
        coEvery { soloSpaceRepository.changeBackground(any()) } returns Result.success(Unit)

        changeBackground.color("#FFFFFF")

        coVerify(exactly = 1) { soloSpaceRepository.changeBackground(SoloBackgroundEdit.Color("#FFFFFF")) }
    }
}
