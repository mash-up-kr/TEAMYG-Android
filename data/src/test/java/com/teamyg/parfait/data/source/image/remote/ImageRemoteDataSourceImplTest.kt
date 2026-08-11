package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ImageService
import com.teamyg.parfait.data.service.model.request.image.IssueImageUploadUrlRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.image.ConfirmImageUploadResponse
import com.teamyg.parfait.data.service.model.response.image.IssueImageUploadUrlResponse
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageStatus
import com.teamyg.parfait.domain.model.image.ImageType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ImageRemoteDataSourceImplTest {
    private val imageService: ImageService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = ImageRemoteDataSourceImpl(
        imageService = imageService,
        apiCaller = apiCaller,
    )

    private fun issueSuccess(
        imageId: Long = 7L,
        expiresIn: Long = 900L,
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = IssueImageUploadUrlResponse(
            imageId = imageId,
            uploadUrl = "https://example.com/upload",
            imageUrl = "https://example.com/image",
            expiresIn = expiresIn,
        ),
    )

    private fun confirmSuccess(
        imageId: Long = 7L,
        status: String = "COMPLETED",
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = ConfirmImageUploadResponse(
            imageId = imageId,
            imageUrl = "https://example.com/image",
            status = status,
        ),
    )

    @Test
    fun issueUploadUrl_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서비스가 발급 성공 응답을 준다
        coEvery { imageService.postImages(any()) } returns issueSuccess(imageId = 7L, expiresIn = 900L)

        // When 업로드 URL 발급
        val result = dataSource.issueUploadUrl(
            fileName = "photo.png",
            contentType = "image/png",
            imageType = ImageType.NUKKI,
        )

        // Then VO 로 매핑된 성공 결과
        val vo = result.getOrThrow()
        assertEquals(ImageId(7L), vo.imageId)
        assertEquals("https://example.com/upload", vo.uploadUrl)
        assertEquals("https://example.com/image", vo.imageUrl)
        assertEquals(900.seconds, vo.expiresIn)
    }

    @Test
    fun issueUploadUrl_buildsRequestBodyFromArguments() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<IssueImageUploadUrlRequest>()
        coEvery { imageService.postImages(capture(request)) } returns issueSuccess()

        // When 파일명·MIME·용도를 넘겨 발급
        dataSource.issueUploadUrl(
            fileName = "photo.png",
            contentType = "image/png",
            imageType = ImageType.BACKGROUND,
        )

        // Then 세 인자가 그대로 실린다. imageType 은 enum 이름 문자열이다
        assertEquals("photo.png", request.captured.fileName)
        assertEquals("image/png", request.captured.contentType)
        assertEquals("BACKGROUND", request.captured.imageType)
    }

    @Test
    fun issueUploadUrl_businessFailure_returnsBusinessException() = runTest {
        // Given envelope 의 success=false 응답 (HTTP status 축은 여기서 잡지 않는다.
        // 실제 서버의 400 은 Retrofit 이 HttpException 을 던지는 별도 경로를 탄다)
        coEvery { imageService.postImages(any()) } returns ApiResponse(
            success = false,
            code = "INVALID_CONTENT_TYPE",
            message = "지원하지 않는 이미지 형식입니다",
            data = null,
        )

        // When 업로드 URL 발급
        val result = dataSource.issueUploadUrl(
            fileName = "photo.gif",
            contentType = "image/gif",
            imageType = ImageType.NUKKI,
        )

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_CONTENT_TYPE", error.code)
    }

    @Test
    fun issueUploadUrl_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery { imageService.postImages(any()) } throws IOException("connection reset")

        // When 업로드 URL 발급
        val result = dataSource.issueUploadUrl(
            fileName = "photo.png",
            contentType = "image/png",
            imageType = ImageType.NUKKI,
        )

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun confirmUpload_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서비스가 확인 성공 응답을 준다
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess(imageId = 7L)

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then VO 로 매핑된 성공 결과
        val vo = result.getOrThrow()
        assertEquals(ImageId(7L), vo.imageId)
        assertEquals(ImageStatus.COMPLETED, vo.status)
    }

    @Test
    fun confirmUpload_unwrapsImageIdForPathVariable() = runTest {
        // Given 성공 응답
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess()

        // When value class 로 감싼 id 로 확인 호출
        dataSource.confirmUpload(ImageId(42L))

        // Then 경로 변수에는 raw Long 이 들어간다 (Retrofit 경계에서 벗긴다)
        coVerify(exactly = 1) { imageService.postImagesByImageIdConfirm(42L) }
    }

    @Test
    fun confirmUpload_alreadyConfirmed_returnsBusinessException() = runTest {
        // Given envelope 의 success=false 응답 (HTTP status 축은 여기서 잡지 않는다.
        // 실제 서버의 409 는 Retrofit 이 HttpException 을 던지는 별도 경로를 탄다)
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns ApiResponse(
            success = false,
            code = "IMAGE_ALREADY_CONFIRMED",
            message = "이미 확인된 이미지입니다",
            data = null,
        )

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then 성공으로 번역하지 않고 Business 예외로 남긴다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("IMAGE_ALREADY_CONFIRMED", error.code)
    }

    @Test
    fun confirmUpload_successButNullData_returnsEmptyBodyException() = runTest {
        // Given success=true 인데 data 가 비었다
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = null,
        )

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.EmptyBody>(result.exceptionOrNull())
        assertEquals("SUCCESS", error.code)
    }

    @Test
    fun confirmUpload_pendingStatus_mapsToPending() = runTest {
        // Given 서버가 PENDING 상태를 준다
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess(status = "PENDING")

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then PENDING enum 으로 떨어진다
        assertEquals(ImageStatus.PENDING, result.getOrThrow().status)
    }

    @Test
    fun confirmUpload_unknownStatus_fallsBackToUnknown() = runTest {
        // Given 클라이언트가 모르는 상태 문자열
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess(status = "FAILED")

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then 예외를 던지지 않고 UNKNOWN 으로 떨어진다
        assertEquals(ImageStatus.UNKNOWN, result.getOrThrow().status)
    }

    @Test
    fun confirmUpload_statusMatchIsCaseSensitive() = runTest {
        // Given 값은 맞지만 대소문자가 다른 상태
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess(status = "completed")

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then enum 이름과 정확히 같아야 매칭되므로 UNKNOWN 이다
        assertEquals(ImageStatus.UNKNOWN, result.getOrThrow().status)
    }
}
