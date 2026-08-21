package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentImageRepositoryImplTest {
    private val dir: File = File(System.getProperty("java.io.tmpdir"), "recent-image-test").apply {
        deleteRecursively()
        mkdirs()
    }

    private val localDataSource: RecentImageLocalDataSource = mockk(relaxed = true)
    private val fileDataSource: FileRecentImageLocalDataSource = mockk(relaxed = true)

    /**
     * 프로퍼티가 아니라 함수다. 구현의 `recentCacheImages` 가 **생성자 초기화식**에서
     * `localDataSource.values` 를 곧바로 읽으므로, 저장소를 먼저 만들어 두면 relaxed mock 이 준
     * 빈 흐름을 붙들어 뒤늦은 `every` 가 닿지 않는다(`ToppingDraftRepositoryImplTest` 가 같은
     * 함정을 문서로 박아 두었다).
     */
    private fun repository() = RecentImageRepositoryImpl(
        recentImageLocalDataSource = localDataSource,
        fileRecentImageLocalDataSource = fileDataSource,
    )

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun store_withContentUri_readsThroughContentResolverPath() = runTest {
        // Given 갤러리가 준 content uri
        val bytes = byteArrayOf(1, 2, 3)
        val target = File(dir, "abc.jpg")
        every { fileDataSource.readBytes("content://media/1") } returns bytes
        every { fileDataSource.getTargetFile(bytes, "jpg") } returns target
        every { fileDataSource.getUriStringForFile(target) } returns "content://recent/abc.jpg"

        // When 원본 사진으로 저장한다
        val stored = repository().storeRecentImageInInternalStorage(
            source = "content://media/1",
            kind = com.teamyg.parfait.domain.model.image.RecentImageKind.SOURCE,
        )

        // Then uri 읽기 경로를 탄다
        verify { fileDataSource.readBytes("content://media/1") }
        assertEquals("content://recent/abc.jpg", stored)
        assertTrue(target.exists())
    }

    @Test
    fun store_withAbsolutePath_readsThroughFilePathAndKeepsPngExtension() = runTest {
        // Given 초안이 들고 있는 절대경로. content resolver 로는 열리지 않는다
        val bytes = byteArrayOf(9, 9)
        val target = File(dir, "def.png")
        every { fileDataSource.readFileBytes("/data/cache/segmentation/subject.png") } returns bytes
        every { fileDataSource.getTargetFile(bytes, "png") } returns target
        every { fileDataSource.getUriStringForFile(target) } returns "content://recent/def.png"

        // When 알맹이로 저장한다
        val stored = repository().storeRecentImageInInternalStorage(
            source = "/data/cache/segmentation/subject.png",
            kind = com.teamyg.parfait.domain.model.image.RecentImageKind.CUTOUT,
        )

        // Then 파일 읽기 경로를 타고 확장자가 png 다 — jpg 로 굳으면 투명 PNG 가
        // image/jpeg 로 올라가 알파가 사라진다
        verify { fileDataSource.readFileBytes("/data/cache/segmentation/subject.png") }
        verify { fileDataSource.getTargetFile(bytes, "png") }
        assertEquals("content://recent/def.png", stored)
    }
}
