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
    fun addAndGetEvictedCacheFileName_atMaxSize_evictsOldestAndKeepsSizeAtNine() = runTest {
        // Given 이미 MAX_SIZE(9)개가 저장돼 있다
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
}
