package com.teamyg.parfait.data.repository.solo

import com.teamyg.parfait.data.model.entity.SoloSpaceRecordEntity
import com.teamyg.parfait.data.source.solo.local.SoloSpaceLocalDataSource
import com.teamyg.parfait.data.source.solo.local.SoloSpaceLocalDataSourceImpl
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.solo.SoloBackgroundEdit
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.model.topping.TextToppingAlign
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.URI
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SoloSpaceRepositoryImplTest {
    private val tempDir: File = createTempDirectory("solo-space-test").toFile()
    private val root = File(tempDir, "solo_space")
    private val sourceDir = File(tempDir, "source").apply { mkdirs() }
    private val recordFile = File(root, "canvas.json")
    private val imagesDir = File(root, "images")

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val clock: Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-26T03:00:00Z")
    }

    private val localDataSource = SpyLocalDataSource(SoloSpaceLocalDataSourceImpl(root, json))

    private fun repository(
        dataSource: SoloSpaceLocalDataSource = localDataSource,
        dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    ) = SoloSpaceRepositoryImpl(
        localDataSource = dataSource,
        clock = clock,
        ioDispatcher = dispatcher,
    )

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun noFile_emitsEmptyCanvas() = runTest {
        // Given 기록 파일이 아직 없다
        val repository = repository()

        // When 처음 구독한다
        val canvas = repository.canvas.first().getOrThrow()

        // Then 빈 캔버스이고, 읽기만으로는 파일을 만들지 않는다
        assertEquals(SoloCanvasVO(background = null, toppings = emptyList(), texts = emptyList()), canvas)
        assertFalse(recordFile.exists())

        // When 쓰기를 한 번 한다
        repository.placeText("안녕", TRANSFORM, TEXT_STYLE).getOrThrow()

        // Then 그때 파일이 생긴다
        assertTrue(recordFile.isFile)
    }

    @Test
    fun placeTopping_copiesImageAndEmitsTopping() = runTest {
        // Given 캐시에 떨군 사진 한 장
        val repository = repository()
        val source = sourceImage("cutout.png", byteArrayOf(1, 2, 3))

        // When 배치한다
        val result = repository.placeTopping(source.absolutePath, TRANSFORM, BORDER)

        // Then 혼자 공간으로 복사된 사본을 가리키는 내 토핑이 생긴다
        assertTrue(result.isSuccess)
        val topping = repository.canvas
            .first()
            .getOrThrow()
            .toppings
            .single()
        assertTrue(topping.isMine)
        assertEquals(TRANSFORM, topping.transform)
        assertEquals(BORDER, topping.border)
        assertEquals("나", topping.placedBy.nickname.value)
        assertTrue(topping.imageUrl.startsWith("file://"))
        val copied = topping.imageUrl.toFile()
        assertTrue(copied.isFile)
        assertTrue(copied.name.endsWith(".png"))
        assertEquals(imagesDir.canonicalFile, copied.parentFile?.canonicalFile)
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(copied.readBytes()))
    }

    @Test
    fun changeBackgroundImage_deletesOldImageAfterSave() = runTest {
        // Given 사진 배경이 이미 있다
        val repository = repository()
        repository.changeBackground(SoloBackgroundEdit.Image(sourceImage("bg1.jpg").absolutePath)).getOrThrow()
        val oldFile = repository.backgroundImageFile()
        assertTrue(oldFile.isFile)
        var oldFileExistedWhenSaving = false
        localDataSource.beforeWrite = { oldFileExistedWhenSaving = oldFile.exists() }

        // When 다른 사진으로 바꾼다
        repository.changeBackground(SoloBackgroundEdit.Image(sourceImage("bg2.jpg").absolutePath)).getOrThrow()

        // Then 옛 사본은 기록을 저장한 뒤에 지워지고 새 사본이 남는다
        assertTrue(oldFileExistedWhenSaving)
        assertFalse(oldFile.exists())
        val newFile = repository.backgroundImageFile()
        assertTrue(newFile.isFile)
        assertTrue(newFile.name.endsWith(".jpg"))

        // When 색 배경으로 바꾼다
        repository.changeBackground(SoloBackgroundEdit.Color("#FFAA00")).getOrThrow()

        // Then 사진 사본도 지워진다
        assertEquals(
            CanvasBackground.Color("#FFAA00"),
            repository.canvas
                .first()
                .getOrThrow()
                .background,
        )
        assertFalse(newFile.exists())
    }

    @Test
    fun deleteTopping_deletesImageFile() = runTest {
        // Given 토핑 둘
        val repository = repository()
        repository.placeTopping(sourceImage("a.png").absolutePath, TRANSFORM, BORDER).getOrThrow()
        repository.placeTopping(sourceImage("b.png").absolutePath, TRANSFORM, BORDER).getOrThrow()
        val (target, other) = repository.canvas
            .first()
            .getOrThrow()
            .toppings
        val targetFile = target.imageUrl.toFile()
        var fileExistedWhenSaving = false
        localDataSource.beforeWrite = { fileExistedWhenSaving = targetFile.exists() }

        // When 하나를 지운다
        repository.deleteTopping(target.parfaitImageId).getOrThrow()

        // Then 그 토핑의 사본만 저장 뒤에 지워진다
        assertTrue(fileExistedWhenSaving)
        assertFalse(targetFile.exists())
        assertTrue(other.imageUrl.toFile().isFile)
        assertEquals(
            listOf(other.parfaitImageId),
            repository.canvas
                .first()
                .getOrThrow()
                .toppings
                .map { it.parfaitImageId },
        )
    }

    @Test
    fun saveFails_rollsBackMemoryAndDeletesNewCopy() = runTest {
        // Given 토핑 하나가 있는 캔버스
        val repository = repository()
        repository.placeTopping(sourceImage("a.png").absolutePath, TRANSFORM, BORDER).getOrThrow()
        val before = repository.canvas.first().getOrThrow()
        val recordBefore = recordFile.readText()
        val imagesBefore = imagesDir
            .listFiles()
            .orEmpty()
            .map { it.name }
            .toSet()
        localDataSource.failWrite = true

        // When 저장이 실패하는 배치를 한다
        val result = repository.placeTopping(sourceImage("b.png").absolutePath, TRANSFORM, BORDER)

        // Then 실패이고, 메모리·파일·이미지 모두 이전 그대로다
        assertTrue(result.isFailure)
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
        assertEquals(before, repository.canvas.first().getOrThrow())
        assertEquals(recordBefore, recordFile.readText())
        assertEquals(
            imagesBefore,
            imagesDir
                .listFiles()
                .orEmpty()
                .map { it.name }
                .toSet(),
        )

        // When 저장이 실패하는 삭제를 한다
        val deleteResult = repository.deleteTopping(before.toppings.single().parfaitImageId)

        // Then 지우려던 사본도 그대로 남는다
        assertTrue(deleteResult.isFailure)
        assertEquals(before, repository.canvas.first().getOrThrow())
        assertTrue(
            before.toppings
                .single()
                .imageUrl
                .toFile()
                .isFile,
        )
    }

    @Test
    fun reset_emptiesCanvasAndImagesKeepsNextId() = runTest {
        // Given 사진 배경, 토핑 둘, 텍스트 하나
        val repository = repository()
        repository.changeBackground(SoloBackgroundEdit.Image(sourceImage("bg.jpg").absolutePath)).getOrThrow()
        repository.placeTopping(sourceImage("a.png").absolutePath, TRANSFORM, BORDER).getOrThrow()
        repository.placeTopping(sourceImage("b.png").absolutePath, TRANSFORM, BORDER).getOrThrow()
        repository.placeText("글", TRANSFORM, TEXT_STYLE).getOrThrow()
        val before = repository.canvas.first().getOrThrow()
        val maxId = (before.toppings.map { it.parfaitImageId.value } + before.texts.map { it.parfaitTextId.value })
            .max()
        var imagesExistedWhenSaving = false
        localDataSource.beforeWrite = { imagesExistedWhenSaving = imagesDir.listFiles().orEmpty().size == 3 }

        // When 비운다
        repository.reset().getOrThrow()

        // Then 빈 기록을 먼저 저장하고 그다음 images 를 비운다
        assertTrue(imagesExistedWhenSaving)
        assertEquals(
            SoloCanvasVO(background = null, toppings = emptyList(), texts = emptyList()),
            repository.canvas.first().getOrThrow(),
        )
        assertTrue(imagesDir.listFiles().orEmpty().isEmpty())

        // When 비운 뒤 새 토핑을 놓는다
        localDataSource.beforeWrite = {}
        repository.placeTopping(sourceImage("c.png").absolutePath, TRANSFORM, BORDER).getOrThrow()

        // Then id 는 이어서 발급된다
        val newId = repository.canvas
            .first()
            .getOrThrow()
            .toppings
            .single()
            .parfaitImageId.value
        assertTrue(newId > maxId, "newId=$newId maxId=$maxId")
    }

    @Test
    fun delete_thenResubscribe_emitsEmptyCanvas() = runTest {
        // Given 토핑과 텍스트가 있는 캔버스
        val repository = repository()
        repository.placeTopping(sourceImage("a.png").absolutePath, TRANSFORM, BORDER).getOrThrow()
        repository.placeText("글", TRANSFORM, TEXT_STYLE).getOrThrow()

        // When 삭제한다
        repository.delete().getOrThrow()

        // Then 기록과 이미지가 사라지고, 다시 구독하면 빈 캔버스다
        val empty = SoloCanvasVO(background = null, toppings = emptyList(), texts = emptyList())
        assertFalse(recordFile.exists())
        assertTrue(imagesDir.listFiles().orEmpty().isEmpty())
        assertEquals(empty, repository.canvas.first().getOrThrow())

        // Then 앱을 다시 켠 것처럼 새로 읽어도 빈 캔버스다
        assertEquals(empty, repository().canvas.first().getOrThrow())
    }

    @Test
    fun corruptedFile_emitsFailureAndKeepsFile() = runTest {
        // Given 손상된 기록 파일
        root.mkdirs()
        recordFile.writeText("{broken")
        val repository = repository()

        // When 구독한다
        val result = repository.canvas.first()

        // Then 실패를 흘리고 파일은 그대로다
        assertTrue(result.isFailure)
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
        assertEquals("{broken", recordFile.readText())

        // When 비우기가 아닌 쓰기를 시도한다
        val writeResult = repository.placeText("글", TRANSFORM, TEXT_STYLE)

        // Then 쓰기도 실패하고 파일을 덮지 않는다
        assertTrue(writeResult.isFailure)
        assertEquals("{broken", recordFile.readText())
    }

    @Test
    fun corruptedFile_resetRecovers() = runTest {
        // Given 손상된 기록 파일
        root.mkdirs()
        recordFile.writeText("{broken")
        val repository = repository()
        assertTrue(repository.canvas.first().isFailure)

        // When 비운다
        val result = repository.reset()

        // Then 빈 캔버스로 돌아오고 이후 쓰기가 된다
        assertTrue(result.isSuccess)
        assertEquals(
            SoloCanvasVO(background = null, toppings = emptyList(), texts = emptyList()),
            repository.canvas.first().getOrThrow(),
        )
        repository.placeText("글", TRANSFORM, TEXT_STYLE).getOrThrow()
        assertEquals(
            1,
            repository()
                .canvas
                .first()
                .getOrThrow()
                .texts.size,
        )
    }

    @Test
    fun concurrentWrites_allPersisted() = runTest {
        // Given 실제 스레드 위에서 도는 저장소
        val repository = repository(dispatcher = Dispatchers.IO)
        repository.canvas.first().getOrThrow()

        // When 텍스트 20개를 동시에 놓는다
        (1..20)
            .map { index -> async(Dispatchers.Default) { repository.placeText("글$index", TRANSFORM, TEXT_STYLE) } }
            .awaitAll()
            .forEach { result -> result.getOrThrow() }

        // Then 다시 읽어도 20개가 모두 있고 id 가 겹치지 않는다
        val texts = repository(dispatcher = Dispatchers.IO)
            .canvas
            .first()
            .getOrThrow()
            .texts
        assertEquals(20, texts.size)
        assertEquals(20, texts.map { it.parfaitTextId }.toSet().size)
        assertEquals((1..20).map { "글$it" }.toSet(), texts.map { it.text }.toSet())
    }

    private fun sourceImage(
        name: String,
        bytes: ByteArray = name.toByteArray(),
    ): File = File(sourceDir, name).apply { writeBytes(bytes) }

    private suspend fun SoloSpaceRepositoryImpl.backgroundImageFile(): File {
        val background = assertIs<CanvasBackground.Image>(canvas.first().getOrThrow().background)
        return background.url.toFile()
    }

    private fun String.toFile(): File = File(URI(this))

    /** 저장 시점의 파일 상태를 엿보거나 저장을 실패시킨다 */
    private class SpyLocalDataSource(
        private val delegate: SoloSpaceLocalDataSource,
    ) : SoloSpaceLocalDataSource by delegate {
        var failWrite = false
        var beforeWrite: (SoloSpaceRecordEntity) -> Unit = {}

        override fun writeRecord(record: SoloSpaceRecordEntity) {
            beforeWrite(record)
            if (failWrite) {
                throw IOException("disk full")
            }
            delegate.writeRecord(record)
        }
    }

    private companion object {
        val TRANSFORM = ToppingTransform(
            positionX = 0.5,
            positionY = 0.25,
            positionZ = 3,
            scale = 1.2,
            rotation = 15.0,
        )
        val BORDER = ToppingBorder.Solid(color = "#FFFFFF", width = 4.0)
        val TEXT_STYLE = TextToppingStyle(
            color = "#000000",
            fontSize = 24.0,
            isFill = true,
            isStroke = false,
            textAlign = TextToppingAlign.CENTER,
        )
    }
}
