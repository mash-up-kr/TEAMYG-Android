package com.teamyg.parfait.data.repository.gallery

import android.net.Uri
import com.teamyg.parfait.data.utils.GalleryMediaProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class GalleryRepositoryImplTest {
    private val galleryMediaProvider: GalleryMediaProvider = mockk(relaxed = true)
    private val uri: Uri = mockk(relaxed = true)

    private val repository = GalleryRepositoryImpl(galleryMediaProvider = galleryMediaProvider)

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun givenVideoFile(bytes: ByteArray = byteArrayOf(1, 2, 3)): File =
        File.createTempFile("canvas_video", ".mp4").apply {
            writeBytes(bytes)
            deleteOnExit()
        }

    @Test
    fun saveVideoToGallery_streamOpens_copiesBytesAndFinalizes() = runTest {
        // Given 등록이 되고 출력 스트림이 열린다
        val file = givenVideoFile(byteArrayOf(7, 8, 9))
        val output = ByteArrayOutputStream()
        every { galleryMediaProvider.insertPendingVideo(any()) } returns uri
        every { galleryMediaProvider.openOutputStream(uri) } returns output

        // When 저장한다
        val result = repository.saveVideoToGallery(
            videoFilePath = file.absolutePath,
            displayName = "parfait_1.mp4",
        )

        // Then 파일 내용이 그대로 들어가고 대기 표시가 내려간다
        assertTrue(result.isSuccess)
        assertTrue(output.toByteArray().contentEquals(byteArrayOf(7, 8, 9)))
        verify(exactly = 1) { galleryMediaProvider.finalizePendingVideo(uri) }
    }

    @Test
    fun saveVideoToGallery_insertFails_returnsFailureWithoutDeleting() = runTest {
        // Given 등록 자체가 실패한다
        val file = givenVideoFile()
        every { galleryMediaProvider.insertPendingVideo(any()) } returns null

        // When 저장한다
        val result = repository.saveVideoToGallery(
            videoFilePath = file.absolutePath,
            displayName = "parfait_1.mp4",
        )

        // Then 지울 대상이 없으므로 삭제를 부르지 않는다
        assertTrue(result.isFailure)
        verify(exactly = 0) { galleryMediaProvider.deleteImage(any()) }
    }

    @Test
    fun saveVideoToGallery_outputStreamNull_deletesRegistration() = runTest {
        // Given 등록은 됐지만 스트림이 안 열린다
        val file = givenVideoFile()
        every { galleryMediaProvider.insertPendingVideo(any()) } returns uri
        every { galleryMediaProvider.openOutputStream(uri) } returns null

        // When 저장한다
        val result = repository.saveVideoToGallery(
            videoFilePath = file.absolutePath,
            displayName = "parfait_1.mp4",
        )

        // Then 빈 항목이 갤러리에 남지 않게 등록을 되돌린다
        assertTrue(result.isFailure)
        verify(exactly = 1) { galleryMediaProvider.deleteImage(uri) }
        verify(exactly = 0) { galleryMediaProvider.finalizePendingVideo(uri) }
    }

    @Test
    fun saveVideoToGallery_missingFile_returnsFailureAndDeletesRegistration() = runTest {
        // Given 등록은 됐는데 원본 파일이 없다
        every { galleryMediaProvider.insertPendingVideo(any()) } returns uri
        every { galleryMediaProvider.openOutputStream(uri) } returns ByteArrayOutputStream()

        // When 없는 경로로 저장한다
        val result = repository.saveVideoToGallery(
            videoFilePath = "/does/not/exist.mp4",
            displayName = "parfait_1.mp4",
        )

        // Then 실패로 접고 등록을 되돌린다
        assertTrue(result.isFailure)
        verify(exactly = 1) { galleryMediaProvider.deleteImage(uri) }
    }
}
