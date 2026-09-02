package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.repository.image.ImageFileRepository
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val URI = "content://media/external/images/media/1"
private const val FILE_PATH = "/data/user/0/com.teamyg.parfait/cache/upload/abc.png"

class UploadImageUseCaseTest {
    private val imageFileRepository: ImageFileRepository = mockk()
    private val imageUploadRepository: ImageUploadRepository = mockk()
    private val uploadImage = UploadImageUseCase(
        imageFileRepository = imageFileRepository,
        imageUploadRepository = imageUploadRepository,
    )

    private fun givenBothStepsSucceed() {
        coEvery { imageFileRepository.copyToCache(any()) } returns Result.success(FILE_PATH)
        coEvery { imageUploadRepository.upload(any(), any()) } returns Result.success(CONFIRMED_IMAGE_ID)
    }

    @Test
    fun invoke_bothSteps_returnsTheConfirmedImageId() = runTest {
        // Given 떨구기와 업로드가 모두 성공한다
        givenBothStepsSucceed()

        val result = uploadImage(uri = URI, imageType = ImageType.BACKGROUND)

        assertEquals(CONFIRMED_IMAGE_ID, result.getOrNull())
    }

    @Test
    fun invoke_uploadsTheCachedPathNotTheUri() = runTest {
        // Given 화면이 쥔 것은 경로가 아닌 uri 다
        givenBothStepsSucceed()
        val sentFilePath = slot<String>()

        uploadImage(uri = URI, imageType = ImageType.BACKGROUND)

        // Then 업로드는 uri 를 그대로 받으면 파일을 찾지 못한다 — 떨군 경로가 가야 한다
        coVerify { imageUploadRepository.upload(capture(sentFilePath), any()) }
        assertEquals(FILE_PATH, sentFilePath.captured)
    }

    @Test
    fun invoke_carriesTheImageType() = runTest {
        // 용도가 서버의 S3 키 접두사를 정하는데 배치는 그것을 검사하지 않아,
        // 잘못 실려도 아무 실패가 드러나지 않는다
        givenBothStepsSucceed()
        val uploadedType = slot<ImageType>()

        uploadImage(uri = URI, imageType = ImageType.BACKGROUND)

        coVerify { imageUploadRepository.upload(any(), capture(uploadedType)) }
        assertEquals(ImageType.BACKGROUND, uploadedType.captured)
    }

    @Test
    fun invoke_copyFails_doesNotUpload() = runTest {
        // Given 사진을 캐시로 떨구지 못한다
        coEvery { imageFileRepository.copyToCache(any()) } returns Result.failure(AppError.Network(null))

        val result = uploadImage(uri = URI, imageType = ImageType.BACKGROUND)

        // Then 발급을 부르지 않는다 — 올릴 것도 없는데 서버에 PENDING 행만 남는다
        coVerify(exactly = 0) { imageUploadRepository.upload(any(), any()) }
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun invoke_uploadFails_liftsTheFailure() = runTest {
        // Given 떨구기는 됐지만 업로드가 실패한다
        coEvery { imageFileRepository.copyToCache(any()) } returns Result.success(FILE_PATH)
        coEvery { imageUploadRepository.upload(any(), any()) } returns Result.failure(AppError.Network(null))

        val result = uploadImage(uri = URI, imageType = ImageType.BACKGROUND)

        // Then 삼키지 않는다 — 화면이 저장 실패를 알려야 한다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    private companion object {
        val CONFIRMED_IMAGE_ID = ImageId(7L)
    }
}
