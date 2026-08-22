package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.exception.UnsupportedImageException
import com.teamyg.parfait.data.source.image.local.ImageFileLocalDataSource
import com.teamyg.parfait.domain.model.error.AppError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val URI = "content://media/external/images/media/1"

class ImageFileRepositoryImplTest {
    private val imageFileLocalDataSource: ImageFileLocalDataSource = mockk()
    private val repository = ImageFileRepositoryImpl(
        imageFileLocalDataSource = imageFileLocalDataSource,
    )

    // absolutePath 는 문자열 연산이라 이 파일은 실제로 있을 필요가 없다. 진짜 파일을 두면
    // 구현이 언젠가 파일을 열기 시작해도 이 테스트가 모른 채 계속 통과한다
    private val file = File("/tmp/background.png")

    @Test
    fun copyToCache_success_returnsTheAbsolutePath() = runTest {
        // Given 캐시에 떨궈진 파일
        every { imageFileLocalDataSource.copyToCache(URI) } returns file

        val result = repository.copyToCache(URI)

        // Then 업로드가 받는 것은 File 이 아니라 절대경로다
        assertEquals(file.absolutePath, result.getOrNull())
    }

    @Test
    fun copyToCache_unusableImage_failsAsUnsupportedImage() = runTest {
        // Given 열 수 없는 uri 든 모르는 형식이든, 떨구는 쪽은 한 갈래로 끊는다
        every { imageFileLocalDataSource.copyToCache(URI) } throws
            UnsupportedImageException("이미지를 열 수 없다")

        val result = repository.copyToCache(URI)

        // Then 화면이 cause 를 뒤지지 않고도 "이 사진이 문제"라고 말할 수 있어야 한다
        assertIs<AppError.UnsupportedImage>(result.exceptionOrNull())
    }

    @Test
    fun copyToCache_otherFailure_staysUnexpected() = runTest {
        // Given 사진 탓이라고 단정할 수 없는 실패
        every { imageFileLocalDataSource.copyToCache(URI) } throws IOException("stream")

        val result = repository.copyToCache(URI)

        // Then 사진을 바꾸라고 말하지 않는다 — 그래도 :data 예외를 그대로 올리지는 않는다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
    }
}
