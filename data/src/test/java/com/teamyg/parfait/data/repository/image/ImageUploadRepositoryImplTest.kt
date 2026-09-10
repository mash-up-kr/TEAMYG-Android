package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.exception.UnsupportedImageException
import com.teamyg.parfait.data.model.image.PreparedUploadImage
import com.teamyg.parfait.data.model.image.UploadImageFormat
import com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSource
import com.teamyg.parfait.data.source.image.remote.PresignedUploadDataSource
import com.teamyg.parfait.data.utils.image.UploadImagePreprocessor
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ConfirmedImageVO
import com.teamyg.parfait.domain.model.image.ImageStatus
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.ImageUploadUrlVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class ImageUploadRepositoryImplTest {
    private val imageRemoteDataSource: ImageRemoteDataSource = mockk()
    private val presignedUploadDataSource: PresignedUploadDataSource = mockk()
    private val uploadImagePreprocessor: UploadImagePreprocessor = mockk()
    private val repository = ImageUploadRepositoryImpl(
        imageRemoteDataSource = imageRemoteDataSource,
        presignedUploadDataSource = presignedUploadDataSource,
        uploadImagePreprocessor = uploadImagePreprocessor,
    )

    private lateinit var file: File

    private val issued = ImageUploadUrlVO(
        imageId = ISSUED_IMAGE_ID,
        uploadUrl = "https://s3.example.com/upload",
        imageUrl = "https://cdn.example.com/image.png",
        expiresIn = 900.seconds,
    )

    @BeforeTest
    fun setUp() {
        file = File.createTempFile("topping", ".png")
        file.writeBytes(ByteArray(FILE_SIZE))
    }

    @AfterTest
    fun tearDown() {
        file.delete()
    }

    private fun givenAllStepsSucceed() {
        coEvery { uploadImagePreprocessor.prepare(any(), any(), any()) } answers {
            val source = firstArg<File>()
            Result.success(
                PreparedUploadImage(
                    file = source,
                    format = UploadImageFormat.ofExtension(source.extension) ?: UploadImageFormat.PNG,
                    isTemporary = false,
                ),
            )
        }
        coEvery { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) } returns Result.success(issued)
        coEvery { presignedUploadDataSource.put(any(), any(), any()) } returns Result.success(Unit)
        // 확인 응답의 id 를 발급 id 와 다르게 둔다 — 같은 값이면 확인을 건너뛴 구현도 통과한다
        coEvery { imageRemoteDataSource.confirmUpload(any()) } returns Result.success(
            ConfirmedImageVO(
                imageId = CONFIRMED_IMAGE_ID,
                imageUrl = "https://cdn.example.com/image.png",
                status = ImageStatus.COMPLETED,
            ),
        )
    }

    @Test
    fun upload_allStepsSucceed_returnsConfirmedImageId() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()

        // When 업로드한다
        val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then 발급 id 가 아니라 확인까지 마친 id 가 나온다
        assertEquals(CONFIRMED_IMAGE_ID, result.getOrNull())
        coVerify(exactly = 1) { imageRemoteDataSource.confirmUpload(ISSUED_IMAGE_ID) }
    }

    @Test
    fun upload_allStepsSucceed_callsIssueThenPutThenConfirm() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then 서버 계약이 정한 순서 그대로다 — 발급 전 PUT 은 서명이 없고, 전송 전 확인은 빈 객체를 굳힌다
        coVerifyOrder {
            imageRemoteDataSource.issueUploadUrl(any(), any(), any())
            presignedUploadDataSource.put(any(), any(), any())
            imageRemoteDataSource.confirmUpload(any())
        }
    }

    @Test
    fun upload_allStepsSucceed_putsToIssuedUploadUrl() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()
        val putUrl = slot<String>()
        val putFile = slot<File>()
        coEvery {
            presignedUploadDataSource.put(capture(putUrl), any(), capture(putFile))
        } returns Result.success(Unit)

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then 표시용 imageUrl 이 아니라 서명된 uploadUrl 로, 그리고 넘겨받은 그 파일로 나간다
        assertEquals(issued.uploadUrl, putUrl.captured)
        assertEquals(file.absolutePath, putFile.captured.absolutePath)
    }

    @Test
    fun upload_allStepsSucceed_usesSameContentTypeForIssueAndPut() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()
        val issuedContentType = slot<String>()
        val putContentType = slot<String>()
        coEvery {
            imageRemoteDataSource.issueUploadUrl(any(), capture(issuedContentType), any())
        } returns Result.success(issued)
        coEvery {
            presignedUploadDataSource.put(any(), capture(putContentType), any())
        } returns Result.success(Unit)

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then 두 값이 같다. 어긋나면 S3 가 서명 불일치로 거절하고 서버 로그에 안 남는다
        assertEquals("image/png", issuedContentType.captured)
        assertEquals(issuedContentType.captured, putContentType.captured)
    }

    @Test
    fun upload_allStepsSucceed_sendsRealFileName() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()
        val fileName = slot<String>()
        coEvery {
            imageRemoteDataSource.issueUploadUrl(capture(fileName), any(), any())
        } returns Result.success(issued)

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then 더미가 아니라 실제 파일명을 보낸다
        assertEquals(file.name, fileName.captured)
    }

    @Test
    fun upload_allStepsSucceed_sendsGivenImageType() = runTest {
        // Given 발급·전송·확인이 모두 성공한다
        givenAllStepsSucceed()
        val imageType = slot<ImageType>()
        coEvery {
            imageRemoteDataSource.issueUploadUrl(any(), any(), capture(imageType))
        } returns Result.success(issued)

        // When NUKKI 가 아닌 imageType 으로 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.BACKGROUND, sourceLongSide = null)

        // Then 넘긴 값이 그대로 발급 요청까지 간다 — 하드코딩되면 서버가 엉뚱한 S3 키 접두사로 저장한다
        assertEquals(ImageType.BACKGROUND, imageType.captured)
    }

    @Test
    fun upload_allStepsSucceed_usesJpegContentTypeForJpgFile() = runTest {
        // Given .jpg 파일로 발급·전송·확인이 모두 성공한다
        val jpgFile = File.createTempFile("topping", ".jpg")
        jpgFile.deleteOnExit()
        jpgFile.writeBytes(ByteArray(FILE_SIZE))
        givenAllStepsSucceed()
        val issuedContentType = slot<String>()
        coEvery {
            imageRemoteDataSource.issueUploadUrl(any(), capture(issuedContentType), any())
        } returns Result.success(issued)

        // When 업로드한다
        repository.upload(filePath = jpgFile.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then image/jpg 가 아니라 image/jpeg 다 — image/jpg 는 서버가 INVALID_CONTENT_TYPE 으로 거절한다
        assertEquals("image/jpeg", issuedContentType.captured)
    }

    @Test
    fun upload_issueFails_doesNotPutOrConfirm() = runTest {
        // Given 발급이 실패한다
        givenAllStepsSucceed()
        coEvery { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) } returns Result.failure(
            ApiException.Network(IOException("connection reset")),
        )

        // When 업로드한다
        val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then 다음 단계로 넘어가지 않고 도메인 에러로 바뀌어 나온다
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { presignedUploadDataSource.put(any(), any(), any()) }
        coVerify(exactly = 0) { imageRemoteDataSource.confirmUpload(any()) }
    }

    @Test
    fun upload_putFails_doesNotConfirm() = runTest {
        // Given 발급은 되고 전송이 실패한다
        givenAllStepsSucceed()
        coEvery { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) } returns Result.success(issued)
        coEvery { presignedUploadDataSource.put(any(), any(), any()) } returns Result.failure(
            ApiException.Network(IOException("broken pipe")),
        )

        // When 업로드한다
        val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then 확인을 부르지 않는다. 부르면 S3 에 없는 객체가 COMPLETED 로 굳는다
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { imageRemoteDataSource.confirmUpload(any()) }
    }

    @Test
    fun upload_confirmFails_mapsToDomainError() = runTest {
        // Given 발급·전송은 되고 확인이 업무 에러로 실패한다
        givenAllStepsSucceed()
        coEvery { imageRemoteDataSource.confirmUpload(any()) } returns Result.failure(
            ApiException.Business(
                code = "IMAGE_ALREADY_CONFIRMED",
                serverMessage = "이미 확정된 이미지입니다",
                statusCode = 409,
                errorDetail = null,
            ),
        )

        // When 업로드한다
        val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then ApiException 이 도메인까지 새지 않는다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("IMAGE_ALREADY_CONFIRMED", error.code)
    }

    @Test
    fun upload_fileMissing_failsWithoutCallingServer() = runTest {
        // Given 초안이 가리키는 캐시 파일이 이미 지워졌다
        val missing = File(file.parentFile, "gone.png")

        // When 업로드한다
        val result = repository.upload(
            filePath = missing.absolutePath,
            imageType = ImageType.NUKKI,
            sourceLongSide = null,
        )

        // Then 발급을 부르지 않는다 — 부르면 올릴 것도 없는데 PENDING 행과 S3 키만 남는다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
        coVerify(exactly = 0) { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) }
    }

    @Test
    fun upload_preprocessorFails_failsWithoutCallingServer() = runTest {
        // Given 전처리가 실패한다
        coEvery { uploadImagePreprocessor.prepare(any(), any(), any()) } returns Result.failure(
            UnsupportedImageException("서버가 받지 않는 확장자다 - heic"),
        )

        // When 업로드한다
        val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then 원본으로 폴백하지 않고, 화면이 사진을 바꾸라고 말할 수 있는 갈래로 올린다
        assertIs<AppError.UnsupportedImage>(result.exceptionOrNull())
        coVerify(exactly = 0) { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) }
    }

    @Test
    fun upload_preprocessorReencoded_usesPreparedFileAndFormat() = runTest {
        // Given 전처리가 JPEG 축소본을 새로 만들었다
        givenAllStepsSucceed()
        val prepared = File.createTempFile("prepared", ".jpg").also { it.writeBytes(ByteArray(FILE_SIZE)) }
        coEvery { uploadImagePreprocessor.prepare(any(), any(), any()) } returns Result.success(
            PreparedUploadImage(file = prepared, format = UploadImageFormat.JPEG, isTemporary = true),
        )
        val issuedContentType = slot<String>()
        val putContentType = slot<String>()
        val putFile = slot<File>()
        coEvery {
            imageRemoteDataSource.issueUploadUrl(any(), capture(issuedContentType), any())
        } returns Result.success(issued)
        coEvery {
            presignedUploadDataSource.put(any(), capture(putContentType), capture(putFile))
        } returns Result.success(Unit)

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.BACKGROUND, sourceLongSide = null)

        // Then 원본이 아니라 축소본이, 그리고 발급과 PUT 이 같은 contentType 으로 나간다
        assertEquals(prepared.absolutePath, putFile.captured.absolutePath)
        assertEquals("image/jpeg", issuedContentType.captured)
        assertEquals(issuedContentType.captured, putContentType.captured)
    }

    @Test
    fun upload_temporaryPreparedFile_isDeletedAfterUpload() = runTest {
        // Given 전처리가 임시 파일을 만들었고 업로드가 성공한다
        givenAllStepsSucceed()
        val prepared = File.createTempFile("prepared", ".jpg").also { it.writeBytes(ByteArray(FILE_SIZE)) }
        coEvery { uploadImagePreprocessor.prepare(any(), any(), any()) } returns Result.success(
            PreparedUploadImage(file = prepared, format = UploadImageFormat.JPEG, isTemporary = true),
        )

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.BACKGROUND, sourceLongSide = null)

        // Then 축소본은 남지 않는다 - 캐시가 쌓이기만 하는 자리를 늘리지 않는다
        assertEquals(false, prepared.exists())
    }

    @Test
    fun upload_temporaryPreparedFile_isDeletedEvenWhenPutFails() = runTest {
        // Given 전처리는 임시 파일을 만들었으나 전송이 실패한다
        givenAllStepsSucceed()
        val prepared = File.createTempFile("prepared", ".jpg").also { it.writeBytes(ByteArray(FILE_SIZE)) }
        coEvery { uploadImagePreprocessor.prepare(any(), any(), any()) } returns Result.success(
            PreparedUploadImage(file = prepared, format = UploadImageFormat.JPEG, isTemporary = true),
        )
        coEvery { presignedUploadDataSource.put(any(), any(), any()) } returns Result.failure(IOException("boom"))

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.BACKGROUND, sourceLongSide = null)

        // Then 실패해도 지운다
        assertEquals(false, prepared.exists())
    }

    @Test
    fun upload_passthroughFile_isNotDeleted() = runTest {
        // Given 전처리가 원본을 그대로 통과시켰다
        givenAllStepsSucceed()

        // When 업로드한다
        repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI, sourceLongSide = null)

        // Then 남의 파일을 지우지 않는다 - 그 수명은 부른 쪽이 쥐고 있다
        assertEquals(true, file.exists())
    }

    private companion object {
        const val FILE_SIZE = 16
        val ISSUED_IMAGE_ID = ImageId(7L)
        val CONFIRMED_IMAGE_ID = ImageId(99L)
    }
}
