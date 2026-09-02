package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.local.RecentImageEntity
import com.teamyg.parfait.data.model.local.RecentImageKindEntity
import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import com.teamyg.parfait.data.datastore.RecentImageEditor
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentImageRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dir: File by lazy { temporaryFolder.newFolder("recent-image-test") }

    private val localDataSource: RecentImageLocalDataSource = mockk(relaxed = true)
    private val fileDataSource: FileRecentImageLocalDataSource = mockk(relaxed = true)
    private val editor: RecentImageEditor = mockk(relaxed = true)

    /** 생성자 초기화식이 `localDataSource.values`를 곧바로 읽으므로 stub을 먼저 둔다 */
    private fun repository() = RecentImageRepositoryImpl(
        recentImageLocalDataSource = localDataSource,
        fileRecentImageLocalDataSource = fileDataSource,
    )

    /** `edit`이 실제 DataStore 트랜잭션을 열지 않으므로, 전달된 transform 을 그대로 실행해 준다 */
    private fun stubEdit() {
        coEvery { localDataSource.edit(any()) } coAnswers {
            firstArg<suspend (RecentImageEditor) -> Unit>().invoke(editor)
        }
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
            kind = RecentImageKind.SOURCE,
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
            kind = RecentImageKind.CUTOUT,
        )

        // Then 파일 읽기 경로를 타고 확장자가 png 다 — jpg 로 굳으면 투명 PNG 가
        // image/jpeg 로 올라가 알파가 사라진다
        verify { fileDataSource.readFileBytes("/data/cache/segmentation/subject.png") }
        verify { fileDataSource.getTargetFile(bytes, "png") }
        assertEquals("content://recent/def.png", stored)
    }

    @Test
    fun store_sourceIsActuallyPng_namesItPngNotJpg() = runTest {
        // Given 사용자가 갤러리에서 고른 PNG. SOURCE 경로로 들어온다
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val target = File(dir, "abc.png")
        every { fileDataSource.readBytes("content://media/2") } returns bytes
        every { fileDataSource.getTargetFile(bytes, "png") } returns target
        every { fileDataSource.getUriStringForFile(target) } returns "content://recent/abc.png"

        // When 최근 이미지로 저장한다
        val stored = repository().storeRecentImageInInternalStorage(
            source = "content://media/2",
            kind = RecentImageKind.SOURCE,
        )

        // Then 내용대로 png 다. jpg 로 굳으면 배경으로 다시 골랐을 때
        // 확장자에서 유도된 image/jpeg 로 올라간다
        verify { fileDataSource.getTargetFile(bytes, "png") }
        assertEquals("content://recent/abc.png", stored)
    }

    @Test
    fun store_sourceFormatIsUnknown_fallsBackToJpg() = runTest {
        // Given 앞머리가 PNG 도 JPEG 도 아닌 바이트
        val bytes = byteArrayOf(0x47, 0x49, 0x46, 0x38)
        val target = File(dir, "def.jpg")
        every { fileDataSource.readBytes("content://media/3") } returns bytes
        every { fileDataSource.getTargetFile(bytes, "jpg") } returns target
        every { fileDataSource.getUriStringForFile(target) } returns "content://recent/def.jpg"

        // When 최근 이미지로 저장한다
        repository().storeRecentImageInInternalStorage(
            source = "content://media/3",
            kind = RecentImageKind.SOURCE,
        )

        // Then 판정 실패가 저장 실패가 되지는 않는다. 종전 동작을 폴백으로 둔다
        verify { fileDataSource.getTargetFile(bytes, "jpg") }
    }

    @Test
    fun recentCacheImages_attachesAbsolutePathToEachEntry() = runTest {
        // Given 종류가 섞인 저장 목록
        every { localDataSource.values } returns flowOf(
            listOf(
                RecentImageEntity(uri = "content://recent/a.jpg", kind = RecentImageKindEntity.SOURCE),
                RecentImageEntity(uri = "content://recent/b.png", kind = RecentImageKindEntity.CUTOUT),
            ),
        )
        every { fileDataSource.getTargetFileFromUri("content://recent/a.jpg") } returns File(dir, "a.jpg")
        every { fileDataSource.getTargetFileFromUri("content://recent/b.png") } returns File(dir, "b.png")

        // When 목록을 읽는다
        val images: List<RecentImage> = repository().recentCacheImages.first()

        // Then 절대경로가 함께 온다 — 확인 화면과 초안이 요구하는 형태가 uri 가 아니라 경로다
        assertEquals(
            listOf(
                RecentImage(
                    uri = "content://recent/a.jpg",
                    filePath = File(dir, "a.jpg").absolutePath,
                    kind = RecentImageKind.SOURCE,
                ),
                RecentImage(
                    uri = "content://recent/b.png",
                    filePath = File(dir, "b.png").absolutePath,
                    kind = RecentImageKind.CUTOUT,
                ),
            ),
            images,
        )
    }

    @Test
    fun recentCacheImages_dropsEntryWhoseFileNameIsUnreadable() = runTest {
        // Given 파일 이름을 못 읽는 값이 섞여 있다
        every { localDataSource.values } returns flowOf(
            listOf(RecentImageEntity(uri = "broken", kind = RecentImageKindEntity.SOURCE)),
        )
        every { fileDataSource.getTargetFileFromUri("broken") } returns null

        // When 목록을 읽는다
        val images: List<RecentImage> = repository().recentCacheImages.first()

        // Then 경로 없는 항목을 지어내지 않고 뺀다
        assertEquals(emptyList(), images)
    }

    @Test
    fun addAndGetEvictedCacheFileName_withCutout_keepsCutoutKindInStoredEntity() = runTest {
        // Given 빈 저장소이고, encodeValue 로 넘어가는 리스트를 슬롯으로 잡는다
        val encoded = slot<List<RecentImageEntity>>()
        every { localDataSource.decodeValue(any()) } returns emptyList()
        every { localDataSource.encodeValue(capture(encoded)) } returns "encoded"
        stubEdit()

        // When 배치까지 마친 알맹이를 추가한다
        repository().addAndGetEvictedCacheFileName(uri = "content://recent/new.png", kind = RecentImageKind.CUTOUT)

        // Then 저장되는 엔티티가 CUTOUT 을 유지한다 — SOURCE 로 강등되면 갤러리가 이 항목을
        // 원본 사진으로 오인해 엉뚱한 화면으로 보낸다
        assertEquals(RecentImageKindEntity.CUTOUT, encoded.captured.single().kind)
    }

    @Test
    fun addAndGetEvictedCacheFileName_atMaxSizeOfSameKind_evictsOldestOfThatKind() = runTest {
        // Given 원본이 정원(9)을 다 채우고 있다
        val existing = (1..9).map { index ->
            RecentImageEntity(uri = "content://recent/$index.jpg", kind = RecentImageKindEntity.SOURCE)
        }
        val encoded = slot<List<RecentImageEntity>>()
        every { localDataSource.decodeValue(any()) } returns existing
        every { localDataSource.encodeValue(capture(encoded)) } returns "encoded"
        stubEdit()

        // When 10 번째 항목을 추가한다
        val evicted = repository().addAndGetEvictedCacheFileName(
            uri = "content://recent/10.jpg",
            kind = RecentImageKind.SOURCE,
        )

        // Then 가장 오래된 하나가 evicted 로 돌아오고 목록은 9개로 유지된다
        assertEquals(listOf("content://recent/1.jpg"), evicted)
        assertEquals(9, encoded.captured.size)
        assertTrue(encoded.captured.none { it.uri == "content://recent/1.jpg" })
        assertTrue(encoded.captured.any { it.uri == "content://recent/10.jpg" })
    }

    @Test
    fun addAndGetEvictedCacheFileName_atMaxSizeOfOtherKind_doesNotEvictAcrossKinds() = runTest {
        // Given 원본이 정원(9)을 다 채우고 있다
        val existing = (1..9).map { index ->
            RecentImageEntity(uri = "content://recent/$index.jpg", kind = RecentImageKindEntity.SOURCE)
        }
        val encoded = slot<List<RecentImageEntity>>()
        every { localDataSource.decodeValue(any()) } returns existing
        every { localDataSource.encodeValue(capture(encoded)) } returns "encoded"
        stubEdit()

        // When 알맹이 한 개를 추가한다
        val evicted = repository().addAndGetEvictedCacheFileName(
            uri = "content://recent/cutout.png",
            kind = RecentImageKind.CUTOUT,
        )

        // Then 정원은 종류마다 따로라 원본은 하나도 밀려나지 않는다
        assertEquals(emptyList(), evicted)
        assertEquals(10, encoded.captured.size)
        assertEquals(9, encoded.captured.count { it.kind == RecentImageKindEntity.SOURCE })
        assertTrue(encoded.captured.any { it.uri == "content://recent/cutout.png" })
    }

    @Test
    fun addAndGetEvictedCacheFileName_whenOneKindOverflows_keepsInsertionOrderAcrossKinds() = runTest {
        // Given 원본이 정원을 채운 목록 사이에 알맹이 하나가 끼어 있다
        val existing = listOf(
            RecentImageEntity(uri = "content://recent/s1.jpg", kind = RecentImageKindEntity.SOURCE),
            RecentImageEntity(uri = "content://recent/c1.png", kind = RecentImageKindEntity.CUTOUT),
        ) + (2..9).map { index ->
            RecentImageEntity(uri = "content://recent/s$index.jpg", kind = RecentImageKindEntity.SOURCE)
        }
        val encoded = slot<List<RecentImageEntity>>()
        every { localDataSource.decodeValue(any()) } returns existing
        every { localDataSource.encodeValue(capture(encoded)) } returns "encoded"
        stubEdit()

        // When 원본을 하나 더 넣어 원본 쪽만 넘치게 한다
        repository().addAndGetEvictedCacheFileName(
            uri = "content://recent/s10.jpg",
            kind = RecentImageKind.SOURCE,
        )

        // Then 시간순은 그대로다 — 종류끼리 뭉치면 최근 목록의 정렬이 깨진다
        assertEquals(
            listOf(
                "content://recent/c1.png",
                "content://recent/s2.jpg",
                "content://recent/s3.jpg",
                "content://recent/s4.jpg",
                "content://recent/s5.jpg",
                "content://recent/s6.jpg",
                "content://recent/s7.jpg",
                "content://recent/s8.jpg",
                "content://recent/s9.jpg",
                "content://recent/s10.jpg",
            ),
            encoded.captured.map(RecentImageEntity::uri),
        )
    }

    @Test
    fun addAndGetEvictedCacheFileName_atMaxSizeOfCutout_evictsOldestCutoutOnly() = runTest {
        // Given 알맹이가 정원(9)을 채우고 원본도 둘 있다
        val existing = (1..9).map { index ->
            RecentImageEntity(uri = "content://recent/c$index.png", kind = RecentImageKindEntity.CUTOUT)
        } + listOf(
            RecentImageEntity(uri = "content://recent/s1.jpg", kind = RecentImageKindEntity.SOURCE),
            RecentImageEntity(uri = "content://recent/s2.jpg", kind = RecentImageKindEntity.SOURCE),
        )
        val encoded = slot<List<RecentImageEntity>>()
        every { localDataSource.decodeValue(any()) } returns existing
        every { localDataSource.encodeValue(capture(encoded)) } returns "encoded"
        stubEdit()

        // When 알맹이를 하나 더 넣는다
        val evicted = repository().addAndGetEvictedCacheFileName(
            uri = "content://recent/c10.png",
            kind = RecentImageKind.CUTOUT,
        )

        // Then 밀려나는 것은 가장 오래된 알맹이 하나뿐이고 원본은 그대로다
        assertEquals(listOf("content://recent/c1.png"), evicted)
        assertEquals(9, encoded.captured.count { it.kind == RecentImageKindEntity.CUTOUT })
        assertEquals(2, encoded.captured.count { it.kind == RecentImageKindEntity.SOURCE })
    }
}
