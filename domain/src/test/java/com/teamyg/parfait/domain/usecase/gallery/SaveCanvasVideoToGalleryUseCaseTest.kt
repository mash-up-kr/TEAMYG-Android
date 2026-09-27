package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SaveCanvasVideoToGalleryUseCaseTest {
    private val galleryRepository: GalleryRepository = mockk()
    private val saveCanvasVideoToGallery = SaveCanvasVideoToGalleryUseCase(galleryRepository)
    private val videoFile = File("canvas_video.mp4")

    @Test
    fun invoke_delegatesToRepositoryWithSameFileAndDisplayName() = runTest {
        // Given 저장이 성공한다
        coEvery { galleryRepository.saveVideoToGallery(videoFile, "canvas_video.mp4") } returns Result.success(Unit)

        // When 유스케이스를 호출한다
        val result = saveCanvasVideoToGallery(videoFile, "canvas_video.mp4")

        // Then 리포지토리에 그대로 위임하고 성공을 돌려준다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { galleryRepository.saveVideoToGallery(videoFile, "canvas_video.mp4") }
    }

    @Test
    fun invoke_repositoryFails_returnsFailureWithoutThrowing() = runTest {
        // Given 저장이 실패한다
        val error = IllegalStateException("MediaStore insert 실패")
        coEvery { galleryRepository.saveVideoToGallery(any(), any()) } returns Result.failure(error)

        // When 유스케이스를 호출한다
        val result = saveCanvasVideoToGallery(videoFile, "canvas_video.mp4")

        // Then 예외를 던지지 않고 실패를 그대로 돌려준다
        assertTrue(result.isFailure)
    }
}
