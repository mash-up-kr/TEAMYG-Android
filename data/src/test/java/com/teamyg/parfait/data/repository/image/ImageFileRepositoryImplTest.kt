package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.source.image.local.ImageFileLocalDataSource
import com.teamyg.parfait.domain.model.error.AppError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val URI = "content://media/external/images/media/1"

class ImageFileRepositoryImplTest {
    private val imageFileLocalDataSource: ImageFileLocalDataSource = mockk()
    private val repository = ImageFileRepositoryImpl(
        imageFileLocalDataSource = imageFileLocalDataSource,
    )

    private lateinit var file: File

    @BeforeTest
    fun setUp() {
        file = File.createTempFile("background", ".png")
    }

    @AfterTest
    fun tearDown() {
        file.delete()
    }

    @Test
    fun copyToCache_success_returnsTheAbsolutePath() = runTest {
        // Given 캐시에 떨궈진 파일
        every { imageFileLocalDataSource.copyToCache(URI) } returns file

        val result = repository.copyToCache(URI)

        // Then 업로드가 받는 것은 File 이 아니라 절대경로다
        assertEquals(file.absolutePath, result.getOrNull())
    }

    @Test
    fun copyToCache_cannotOpenTheUri_failsAsAppError() = runTest {
        // Given 열 수 없는 uri — 사용자가 사이에 사진을 지웠거나 권한이 빠진 경우다
        every { imageFileLocalDataSource.copyToCache(URI) } throws IOException("stream")

        val result = repository.copyToCache(URI)

        // Then 던지지 않고, :data 예외를 그대로 올려보내지도 않는다 — 그래야 feature 가
        // `:data` 를 보지 않는다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
    }

    @Test
    fun copyToCache_unsupportedFormat_failsAsAppError() = runTest {
        // Given 서버가 받지 않는 형식이라 떨구는 쪽에서 끊은 경우
        every { imageFileLocalDataSource.copyToCache(URI) } throws IllegalArgumentException("형식")

        val result = repository.copyToCache(URI)

        // Then 이것도 같은 경계에서 AppError 로 바뀐다 — 화면이 예외 타입을 구분하지 않는다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
    }
}
