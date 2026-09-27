# 캔버스 갤러리 동영상 저장 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** C-001 캔버스 저장 미리보기(`CanvasImageSaveScreen`)에 "동영상으로 저장" 버튼을 추가해, 토핑이 서버 겹침 순서(`positionZ`)대로 하나씩 페이드인+슬라이드인하며 쌓이는 짧은 mp4를 만들어 기기 갤러리에 저장한다.

**Architecture:** 프레임은 raw Canvas 재구현이 아니라 기존 `CanvasToppingLayer`(내부 컴포저블)를 화면 밖(오프스크린)에 재생하며 `GraphicsLayer.toImageBitmap()`으로 캡처한다 — 정지 이미지 캡처가 이미 쓰는 것과 같은 메커니즘이다. 캡처(Compose 전용 자원)는 `CanvasMainRoute`가, 인코딩(media3 Transformer)·갤러리 저장은 `CanvasMainViewModel`이 맡는다 — 기존 "캡처는 화면만 할 수 있어서 왕복한다" 이펙트↔인텐트 패턴을 그대로 한 겹 더 쓴다. 미리보기 화면(`CanvasImageSaveScreen`/`CanvasImageSaveRoute`)은 ViewModel 없이 결과 버스로 "동영상 요청됨"만 전달하고 즉시 `navigator.onBack()` 한다(이미지 확정과 동일 패턴) — 처리는 전부 캔버스 메인으로 돌아온 뒤 화면 밖에서 조용히 진행된다.

**Tech Stack:** Kotlin, Jetpack Compose(Compose GraphicsLayer 캡처), Coil3(`ImageLoader.execute` 선행 디코딩), Hilt, `androidx.media3:media3-transformer` 1.5.1(신규 의존성), MediaStore(Video).

**Spec:** [docs/superpowers/specs/2026-09-27-canvas-gallery-save-video-design.md](../specs/2026-09-27-canvas-gallery-save-video-design.md)

## Global Constraints

- Kotlin 코드 주석·KDoc은 `docs/code-conventions.md` 규약을 따른다 — 코드가 이미 말하는 것은 쓰지 않는다, 고정 KDoc 틀(`@return`/`@param`)은 타입·이름이 오해 소지 있을 때만 쓴다, 다른 컴포넌트의 "현재 상태"를 코드 주석에 단정하지 않는다(잘 낡는다), 주석 분량은 코드의 어려움에 비례하지 코드의 중요함에 비례하지 않는다.
- `.claude/rules/one-type-per-file.md`: 파일 하나에 non-private top-level `class`·`interface`·`object` 하나만. 파일명은 그 선언 이름과 맞춘다. 매핑 확장 함수는 별도 Mapper 파일로 분리한다(이 작업엔 매핑 확장 함수 신규 추가 없음).
- `.claude/rules/repository-package.md`: `domain`의 `repository` 하위엔 `Repository`로 끝나는 인터페이스만, `data`의 `repository` 하위엔 `RepositoryImpl`로 끝나는 구현체만 둔다. 플랫폼 SDK가 값을 대 주는 인터페이스는 `domain/provider`에 둔다(`CanvasVideoEncoder`가 이 경우).
- 순서 기준은 `positionZ` 단일 기준이다. `createdAt`을 쓰지 않는다(스펙 "순서 기준" 절).
- 토핑 개수(0개 포함)와 무관하게 "동영상으로 저장" 버튼은 항상 노출한다.
- media3 버전은 `1.5.1`로 고정한다(`gradle/libs.versions.toml`).
- 새 파일의 커밋 메시지는 Conventional Commits 스타일(`feat:`/`test:`/`refactor:` 등)을 따른다. 커밋 메시지 끝에는 대화의 system-reminder가 지정한 attribution 줄을 붙인다.

## Review Focus

- 토핑이 0개인 캔버스에서 "동영상으로 저장"을 눌러도 배경 한 프레임짜리 mp4가 크래시 없이 만들어져 저장돼야 한다(Task 8, `canvasVideoRevealProgressSteps`/프레임 리스트가 빈 토핑 목록에서도 최소 1프레임을 만드는지).
- 토핑·배경 이미지 선행 디코딩(preload)이 실패하면(네트워크 오류 등) 캡처를 시작하지 않고 실패로 끝나며, 부분 생성된 임시 프레임 파일이 남지 않아야 한다(Task 8).
- 인코딩(media3 Transformer) 또는 갤러리 저장이 실패하면 임시 프레임 PNG와 mp4 파일이 모두 정리되고, 갤러리에 반쯤 쓰인 파일이 남지 않아야 한다(Task 3, Task 7).
- API 28 이하 기기에서 `WRITE_EXTERNAL_STORAGE` 권한이 없을 때 "동영상으로 저장"을 누르면, 이미지 저장과 동일하게 권한을 요청하고 거부 시 실패 토스트로 끝나야 한다(크래시나 무응답이 아니라) — Task 8에서 처리하며, Route 계층은 이 저장소에 테스트가 없는 기존 관행대로 수동/계측 확인 대상이다.
- 인코딩·저장이 오래 걸려도(사용자가 캔버스 메인에서 다른 조작을 하거나 화면이 재구성돼도) `SAVE_CANVAS_VIDEO_TO_GALLERY_KEY`로 묶인 작업이 끝까지 완료되고 완료 시점의 `selectedDate`로 결과를 알려야 한다(Task 7, 기존 `handleSaveCapturedCanvas`와 동일한 `launch(key=)` 패턴 재사용으로 보장).

---

### Task 1: `GalleryRepository`에 동영상 저장 계약 추가 + `SaveCanvasVideoToGalleryUseCase`

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/gallery/GalleryRepository.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/gallery/SaveCanvasVideoToGalleryUseCase.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/gallery/SaveCanvasVideoToGalleryUseCaseTest.kt`

**Interfaces:**
- Consumes: 없음(순수 domain 계약 추가).
- Produces: `GalleryRepository.saveVideoToGallery(videoFile: File, displayName: String): Result<Unit>`, `SaveCanvasVideoToGalleryUseCase.invoke(videoFile: File, displayName: String): Result<Unit>` — Task 7에서 `CanvasMainViewModel`이 이 UseCase를 주입받아 쓴다.

- [ ] **Step 1: `GalleryRepository`에 메서드 추가**

`domain/src/main/java/com/teamyg/parfait/domain/repository/gallery/GalleryRepository.kt`을 다음과 같이 고친다(기존 `loadFilterYGGalleryImages`/`saveImageToGallery`는 그대로 두고 아래를 추가):

```kotlin
package com.teamyg.parfait.domain.repository.gallery

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import kotlinx.datetime.LocalDate
import java.io.File

interface GalleryRepository {
    suspend fun loadFilterYGGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>>

    suspend fun saveImageToGallery(
        bitmap: BitmapWrapper,
        displayName: String,
    ): Result<Unit>

    /** 이미 인코딩된 [videoFile]을 기기 갤러리에 새 동영상으로 저장한다. */
    suspend fun saveVideoToGallery(
        videoFile: File,
        displayName: String,
    ): Result<Unit>
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`domain/src/test/java/com/teamyg/parfait/domain/usecase/gallery/SaveCanvasVideoToGalleryUseCaseTest.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SaveCanvasVideoToGalleryUseCaseTest {
    private val galleryRepository: GalleryRepository = mockk()
    private val saveCanvasVideoToGallery = SaveCanvasVideoToGalleryUseCase(galleryRepository)
    private val videoFile = File("canvas_video.mp4")

    @Test
    fun invoke_delegatesToRepositoryWithSameFileAndDisplayName() = runTest {
        // Given 저장이 성공한다
        coEvery { galleryRepository.saveVideoToGallery(videoFile, "canvas_video.mp4") } returns Result.success(Unit)

        // When 유스케이스를 호출한다
        val result = saveCanvasVideoToGallery(videoFile, "canvas_video.mp4")

        // Then 리포지토리에 그대로 위임하고 성공을 돌려준다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { galleryRepository.saveVideoToGallery(videoFile, "canvas_video.mp4") }
    }

    @Test
    fun invoke_repositoryFails_returnsFailureWithoutThrowing() = runTest {
        // Given 저장이 실패한다
        val error = IllegalStateException("MediaStore insert 실패")
        coEvery { galleryRepository.saveVideoToGallery(any(), any()) } returns Result.failure(error)

        // When 유스케이스를 호출한다
        val result = saveCanvasVideoToGallery(videoFile, "canvas_video.mp4")

        // Then 예외를 던지지 않고 실패를 그대로 돌려준다
        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 3: 테스트가 실패하는 것을 확인**

Run: `./gradlew :domain:testDebugUnitTest --tests "com.teamyg.parfait.domain.usecase.gallery.SaveCanvasVideoToGalleryUseCaseTest"`
Expected: FAIL — `SaveCanvasVideoToGalleryUseCase` unresolved reference(아직 없음).

- [ ] **Step 4: `SaveCanvasVideoToGalleryUseCase` 작성**

`domain/src/main/java/com/teamyg/parfait/domain/usecase/gallery/SaveCanvasVideoToGalleryUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import java.io.File
import javax.inject.Inject

/**
 * 이미 인코딩된 캔버스 동영상 파일을 기기 갤러리에 저장한다.
 *
 * 인코딩 자체(media3 Transformer)는 이 유스케이스의 책임이 아니다 — 호출부가 이미 완성된
 * mp4 파일을 들고 있어야 한다. [SaveCanvasToGalleryUseCase]가 이미 만들어진 비트맵만
 * 받는 것과 같은 경계다.
 */
class SaveCanvasVideoToGalleryUseCase
@Inject
constructor(
    private val galleryRepository: GalleryRepository,
) {
    init {
        useCaseLogger.i { "SaveCanvasVideoToGalleryUseCase::init" }
    }

    suspend operator fun invoke(
        videoFile: File,
        displayName: String,
    ): Result<Unit> = galleryRepository
        .saveVideoToGallery(videoFile = videoFile, displayName = displayName)
        .onFailure { throwable ->
            useCaseLogger.e(throwable) { "SaveCanvasVideoToGalleryUseCase - 갤러리 동영상 저장 실패" }
        }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :domain:testDebugUnitTest --tests "com.teamyg.parfait.domain.usecase.gallery.SaveCanvasVideoToGalleryUseCaseTest"`
Expected: PASS (2 tests)

- [ ] **Step 6: `GalleryRepository`의 다른 구현체가 있는지 확인**(있으면 컴파일이 깨진다 — 이 저장소엔 `GalleryRepositoryImpl` 하나뿐이라 이 단계에서는 아직 컴파일 에러가 나는 것이 정상이다. Task 2에서 고친다)

Run: `./gradlew :domain:compileDebugKotlin`
Expected: PASS (domain 모듈은 인터페이스만 바꿨으므로 여기선 성공. `data` 모듈은 Task 2 전까지 컴파일 실패 상태로 둔다)

- [ ] **Step 7: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/repository/gallery/GalleryRepository.kt \
        domain/src/main/java/com/teamyg/parfait/domain/usecase/gallery/SaveCanvasVideoToGalleryUseCase.kt \
        domain/src/test/java/com/teamyg/parfait/domain/usecase/gallery/SaveCanvasVideoToGalleryUseCaseTest.kt
git commit -m "feat: add SaveCanvasVideoToGalleryUseCase and GalleryRepository.saveVideoToGallery contract"
```

---

### Task 2: `GalleryMediaProvider` 동영상 MediaStore 지원 + `GalleryRepositoryImpl.saveVideoToGallery`

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/utils/GalleryMediaProvider.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/gallery/GalleryRepositoryImpl.kt`

**Interfaces:**
- Consumes: Task 1의 `GalleryRepository.saveVideoToGallery` 시그니처.
- Produces: `GalleryMediaProvider.insertPendingVideo(displayName: String): Uri?`, `GalleryMediaProvider.finalizePendingMedia(uri: Uri)`, `GalleryMediaProvider.deleteMedia(uri: Uri)`(기존 `finalizePendingImage`/`deleteImage`를 이미지·동영상 공용으로 이름을 바꾼다).

이 클래스는 `MediaStore` 직접 호출이라 유닛 테스트가 없다(기존 `insertPendingImage`도 테스트가 없다 — `docs/superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md` "드리프트 4"가 이미 인정한 기존 관행). 이 Task는 컴파일 확인으로 검증한다.

- [ ] **Step 1: `GalleryMediaProvider`에 동영상 등록 + 공용 확정/삭제 추가**

`data/src/main/java/com/teamyg/parfait/data/utils/GalleryMediaProvider.kt`을 다음으로 바꾼다(기존 `insertPendingImage`는 그대로 두고, `finalizePendingImage`→`finalizePendingMedia`, `deleteImage`→`deleteMedia`로 이름을 바꿔 이미지·동영상 공용으로 만든다 — 두 함수는 이미 `Uri`만 받아 컬럼 이름도 `MediaStore.MediaColumns.IS_PENDING`으로 공용이라 로직 변경은 없다):

```kotlin
package com.teamyg.parfait.data.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

private const val IMAGE_MIME_TYPE = "image/png"
private const val VIDEO_MIME_TYPE = "video/mp4"
private const val SAVE_SUBDIRECTORY = "Parfait"

class GalleryMediaProvider(
    private val context: Context,
) {
    val collectionUri: Uri? = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val projection: Array<String> = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
    )

    val sortOrder: String =
        "COALESCE(${MediaStore.Images.Media.DATE_TAKEN}, ${MediaStore.Images.Media.DATE_ADDED} * 1000) DESC"

    val selection: String =
        "(${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?) " +
            "OR ${MediaStore.Images.Media.DATE_TAKEN} IS NULL"

    fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        val resolver: ContentResolver = context.contentResolver ?: return null

        return resolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )
    }

    fun resolveTimestampMs(
        cursor: Cursor,
        takenColumn: Int,
        addedColumn: Int,
    ): Long {
        val takenMs = when (!cursor.isNull(takenColumn)) {
            true -> cursor.getLong(takenColumn)
            false -> 0L
        }

        if (takenMs > 0L) {
            return takenMs
        }

        val addedSeconds = cursor.getLong(addedColumn)
        return addedSeconds * 1000L
    }

    /**
     * 새 이미지를 [MediaStore.MediaColumns.IS_PENDING] 상태로 갤러리에 등록한다. 바이트는 돌려받은
     * [Uri] 로 [openOutputStream] 을 열어 쓴다. API 29 미만은 IS_PENDING 을 쓰지 않는다.
     */
    fun insertPendingImage(displayName: String): Uri? {
        val collection = collectionUri ?: return null
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, IMAGE_MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$SAVE_SUBDIRECTORY")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        return context.contentResolver?.insert(collection, values)
    }

    /**
     * 새 동영상을 [MediaStore.MediaColumns.IS_PENDING] 상태로 갤러리에 등록한다. 바이트는
     * [openOutputStream] 으로 이미 인코딩된 mp4 파일을 그대로 복사해 쓴다.
     */
    fun insertPendingVideo(displayName: String): Uri? {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, VIDEO_MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$SAVE_SUBDIRECTORY")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        return context.contentResolver?.insert(collection, values)
    }

    fun openOutputStream(uri: Uri): OutputStream? = context.contentResolver?.openOutputStream(uri)

    /** [insertPendingImage]·[insertPendingVideo] 가 건 IS_PENDING 을 내려 갤러리에 보이게 한다 */
    fun finalizePendingMedia(uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver?.update(uri, values, null, null)
    }

    /** 바이트를 다 못 썼을 때 갤러리에 빈 파일이 남지 않게 등록을 되돌린다 */
    fun deleteMedia(uri: Uri) {
        context.contentResolver?.delete(uri, null, null)
    }
}
```

- [ ] **Step 2: `GalleryRepositoryImpl` 호출부 갱신 + `saveVideoToGallery` 추가**

`data/src/main/java/com/teamyg/parfait/data/repository/gallery/GalleryRepositoryImpl.kt`의 `saveImageToGallery`에서 `finalizePendingImage`/`deleteImage` 호출을 `finalizePendingMedia`/`deleteMedia`로 바꾸고, 아래 `saveVideoToGallery`를 추가한다:

```kotlin
package com.teamyg.parfait.data.repository.gallery

import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.DayWindow
import com.teamyg.parfait.data.utils.GalleryMediaProvider
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class GalleryRepositoryImpl
@Inject
constructor(
    private val galleryMediaProvider: GalleryMediaProvider,
) : GalleryRepository {
    init {
        repositoryLogger.i { "GalleryRepositoryImpl::init" }
    }

    override suspend fun loadFilterYGGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>> =
        withContext(Dispatchers.IO) {
            val uri: Uri = galleryMediaProvider
                .collectionUri
                ?: return@withContext LinkedHashMap<LocalDate, MutableList<String>>()
            val timeZone: TimeZone = TimeZone.currentSystemDefault()
            val window: DayWindow = DayWindow.current(timeZone)
            val selectionArgs: Array<String> = arrayOf(
                window.startMs.toString(),
                window.endMs.toString(),
            )

            val grouped = linkedMapOf<LocalDate, MutableList<String>>()

            galleryMediaProvider
                .query(
                    uri = uri,
                    projection = galleryMediaProvider.projection,
                    selection = galleryMediaProvider.selection,
                    selectionArgs = selectionArgs,
                    sortOrder = galleryMediaProvider.sortOrder,
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id: Long = cursor.getLong(idColumn)

                        val timestampMs: Long = galleryMediaProvider.resolveTimestampMs(
                            cursor = cursor,
                            takenColumn = takenColumn,
                            addedColumn = addedColumn,
                        )

                        if (timestampMs !in window) {
                            continue
                        }

                        val dateKey: LocalDate = Instant
                            .fromEpochMilliseconds(timestampMs)
                            .minus(DayWindow.DAY_BOUNDARY_HOUR.hours)
                            .toLocalDateTime(timeZone)
                            .date

                        val imageUri: String = ContentUris
                            .withAppendedId(uri, id)
                            .toString()

                        grouped
                            .getOrPut(dateKey) { mutableListOf() }
                            .add(imageUri)
                    }
                }

            return@withContext grouped
        }

    /**
     * IS_PENDING 으로 등록해 두고 바이트를 다 쓴 뒤에야 내린다 — 쓰다 만 파일이 갤러리에
     * 잠깐이라도 온전한 것처럼 보이지 않게 하려는 것이다(API 29+). 어느 단계에서든 실패하면
     * 등록 자체를 지워 빈 항목이 남지 않게 한다.
     */
    override suspend fun saveImageToGallery(
        bitmap: BitmapWrapper,
        displayName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runSuspendCatching {
            val rawBitmap: Bitmap = (bitmap as? AndroidBitmap)?.getRawData()
                ?: error("갤러리에 저장할 비트맵을 읽지 못했다")
            val uri = galleryMediaProvider.insertPendingImage(displayName)
                ?: error("갤러리에 이미지를 등록하지 못했다")

            try {
                galleryMediaProvider.openOutputStream(uri)?.use { output ->
                    rawBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                } ?: error("갤러리 이미지의 출력 스트림을 열지 못했다")

                galleryMediaProvider.finalizePendingMedia(uri)
            } catch (throwable: Throwable) {
                galleryMediaProvider.deleteMedia(uri)
                throw throwable
            }
        }
    }

    /** [saveImageToGallery] 와 같은 IS_PENDING 왕복이나, 인코딩된 바이트를 그대로 복사만 한다. */
    override suspend fun saveVideoToGallery(
        videoFile: File,
        displayName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runSuspendCatching {
            val uri = galleryMediaProvider.insertPendingVideo(displayName)
                ?: error("갤러리에 동영상을 등록하지 못했다")

            try {
                galleryMediaProvider.openOutputStream(uri)?.use { output ->
                    videoFile.inputStream().use { input -> input.copyTo(output) }
                } ?: error("갤러리 동영상의 출력 스트림을 열지 못했다")

                galleryMediaProvider.finalizePendingMedia(uri)
            } catch (throwable: Throwable) {
                galleryMediaProvider.deleteMedia(uri)
                throw throwable
            }
        }
    }
}
```

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew :data:compileDebugKotlin :domain:compileDebugKotlin`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/utils/GalleryMediaProvider.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/gallery/GalleryRepositoryImpl.kt
git commit -m "feat: add MediaStore Video support to GalleryMediaProvider and GalleryRepositoryImpl"
```

---

### Task 3: media3 의존성 추가 + `CanvasVideoEncoder` provider

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build-logic/convention/src/main/kotlin/ModuleDataConventionPlugin.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/provider/CanvasVideoEncoder.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/provider/CanvasVideoEncoderImpl.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/di/VideoModule.kt`

**Interfaces:**
- Consumes: 없음.
- Produces: `CanvasVideoEncoder.encode(frames: List<File>, frameDurationMs: Long, outputFile: File): Result<Unit>` — Task 7에서 `CanvasMainViewModel`이 주입받아 쓴다.

media3 Transformer는 실제 인코더/디코더를 구동해 계측 테스트 없이는 검증할 수 없다(기존 `GalleryMediaProvider`와 같은 사정). 이 Task도 컴파일 확인으로 검증한다.

- [ ] **Step 1: 버전 카탈로그에 media3 추가**

`gradle/libs.versions.toml`의 `[versions]` 섹션(Coil 항목 근처, 28번째 줄 부근)에 추가:

```toml
media3 = "1.5.1"
```

`[libraries]` 섹션(Coil 항목 근처, 135번째 줄 부근)에 추가:

```toml
media3-transformer = { group = "androidx.media3", name = "media3-transformer", version.ref = "media3" }
media3-effect = { group = "androidx.media3", name = "media3-effect", version.ref = "media3" }
media3-common = { group = "androidx.media3", name = "media3-common", version.ref = "media3" }
```

- [ ] **Step 2: `data` 모듈 컨벤션 플러그인에 의존성 추가**

`build-logic/convention/src/main/kotlin/ModuleDataConventionPlugin.kt`:

```kotlin
import com.teamyg.parfait.buildlogic.utils.extensions.implementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.dependencies

class ModuleDataConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.parfait.android.library.get().pluginId)
        apply(libs.plugins.parfait.dagger.hilt.core.get().pluginId)
        apply(libs.plugins.parfait.android.network.get().pluginId)
    }

    dependencies {
        implementation(project(":core:util:android"))
        implementation(project(":core:util:jvm"))

        implementation(project(":domain"))

        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.datastore.preferences)

        implementation(libs.kakao.sdk.user)

        implementation(libs.google.mlkit.subject.segmentation)

        implementation(libs.media3.transformer)
        implementation(libs.media3.effect)
        implementation(libs.media3.common)
    }
})
```

- [ ] **Step 3: `domain/provider/CanvasVideoEncoder.kt` 작성**

```kotlin
package com.teamyg.parfait.domain.provider

import java.io.File

/**
 * 등장 순서대로 캡처된 프레임 시퀀스를 동영상 파일로 인코딩한다. 구현은 플랫폼 인코더(media3
 * Transformer)를 쓰므로 `data` 모듈에 있다 — `domain`은 계약만 안다.
 */
interface CanvasVideoEncoder {
    /**
     * [frames] 를 순서대로 이어 붙여 [outputFile] 에 mp4로 쓴다. [frameDurationMs] 는 프레임
     * 하나가 화면에 머무는 시간이다(전체 재생 시간이 아니다).
     */
    suspend fun encode(
        frames: List<File>,
        frameDurationMs: Long,
        outputFile: File,
    ): Result<Unit>
}
```

- [ ] **Step 4: `data/provider/CanvasVideoEncoderImpl.kt` 작성**

```kotlin
package com.teamyg.parfait.data.provider

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.provider.CanvasVideoEncoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val CANVAS_VIDEO_FRAME_RATE = 30

/**
 * [Transformer] 는 호출 스레드에 Looper 가 있어야 해서 [Dispatchers.Main] 에서 돌린다.
 * 이미지 시퀀스는 [EditedMediaItem.durationUs]·[EditedMediaItem.frameRate] 로 노출 시간을
 * 정하고, [Transformer] 가 그 시간만큼의 프레임을 만들어 이어 붙인다.
 */
@UnstableApi
class CanvasVideoEncoderImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : CanvasVideoEncoder {
    override suspend fun encode(
        frames: List<File>,
        frameDurationMs: Long,
        outputFile: File,
    ): Result<Unit> = runSuspendCatching {
        val editedItems = frames.map { frame ->
            EditedMediaItem.Builder(MediaItem.fromUri(frame.toUri()))
                .setDurationUs(frameDurationMs.milliseconds.inWholeMicroseconds)
                .setFrameRate(CANVAS_VIDEO_FRAME_RATE)
                .build()
        }
        val composition = Composition
            .Builder(EditedMediaItemSequence.Builder(editedItems).build())
            .build()

        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val transformer = Transformer.Builder(context)
                    .addListener(
                        object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                if (continuation.isActive) continuation.resumeWith(Result.failure(exportException))
                            }
                        },
                    )
                    .build()

                transformer.start(composition, outputFile.absolutePath)
                continuation.invokeOnCancellation { transformer.cancel() }
            }
        }
    }
}
```

- [ ] **Step 5: DI 바인딩 추가**

`data/src/main/java/com/teamyg/parfait/data/di/VideoModule.kt`:

```kotlin
package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.provider.CanvasVideoEncoderImpl
import com.teamyg.parfait.domain.provider.CanvasVideoEncoder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface VideoModule {
    @Binds
    @Singleton
    fun bindCanvasVideoEncoder(canvasVideoEncoderImpl: CanvasVideoEncoderImpl): CanvasVideoEncoder
}
```

- [ ] **Step 6: 빌드 확인**

Run: `./gradlew :data:compileDebugKotlin :domain:compileDebugKotlin`
Expected: PASS. (Gradle sync가 버전 카탈로그 변경을 반영하도록 먼저 `./gradlew :data:compileDebugKotlin` 이전에 `./gradlew help`로 카탈로그를 재생성해도 된다 — 실패하면 그렇게 한다.)

- [ ] **Step 7: 커밋**

```bash
git add gradle/libs.versions.toml build-logic/convention/src/main/kotlin/ModuleDataConventionPlugin.kt \
        domain/src/main/java/com/teamyg/parfait/domain/provider/CanvasVideoEncoder.kt \
        data/src/main/java/com/teamyg/parfait/data/provider/CanvasVideoEncoderImpl.kt \
        data/src/main/java/com/teamyg/parfait/data/di/VideoModule.kt
git commit -m "feat: add media3 Transformer dependency and CanvasVideoEncoder provider"
```

---

### Task 4: `CanvasToppingLayer`에 `revealProgress`(페이드인+슬라이드인) 추가

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt`

**Interfaces:**
- Consumes: 없음(기존 시그니처에 기본값 있는 파라미터 추가라 기존 호출부는 변경 불필요).
- Produces: `CanvasToppingLayer(..., revealProgress: (ParfaitImageId) -> Float = { 1f })` — Task 6의 `CanvasVideoCaptureHost`가 이 파라미터로 애니메이션을 건다.

이 변경은 순수 Compose 렌더링이라 스크린샷/계측 테스트 없이는 시각적으로 검증할 수 없다(기존 `CanvasToppingLayer`도 테스트가 없다). 컴파일 확인 + 기존 화면(캔버스 메인·미리보기)이 기본값(`{ 1f }`)으로 동작이 그대로인지 수동 확인으로 검증한다.

- [ ] **Step 1: `CanvasToppingLayer`/`CanvasTopping`에 `revealProgress` 추가**

`feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt`에서 시그니처와 두 곳을 바꾼다:

`CanvasToppingLayer` 시그니처에 파라미터 추가(기존 파라미터는 그대로):

```kotlin
@Composable
internal fun CanvasToppingLayer(
    toppings: List<CanvasToppingVO>,
    spotlightedToppingId: ParfaitImageId?,
    onClickTopping: (CanvasToppingVO) -> Unit,
    onClickSpotlightDim: () -> Unit,
    modifier: Modifier = Modifier,
    hitTestEnabled: Boolean = true,
    revealTogether: Boolean = true,
    revealResetKey: Any? = Unit,
    retryKey: Int = 0,
    onLoadStateChange: (CanvasLoadState) -> Unit = {},
    /** 동영상 저장이 토핑을 순서대로 드러낼 때만 쓴다. 기본값은 항상 다 보이는 것이다. */
    revealProgress: (ParfaitImageId) -> Float = { 1f },
) {
```

`entries.forEach` 블록과 `spotlighted` 블록의 `CanvasTopping(...)` 호출 두 곳에 `revealProgress = revealProgress(entry.topping.parfaitImageId)` / `revealProgress = revealProgress(spotlighted.topping.parfaitImageId)`를 추가한다:

```kotlin
            entries.forEach { entry ->
                if (entry.topping.parfaitImageId != spotlightedToppingId) {
                    CanvasTopping(
                        entry = entry,
                        canvasWidth = areaWidth,
                        canvasHeight = areaHeight,
                        onClick = { onClickTopping(entry.topping) },
                        clickable = hitTestEnabled,
                        revealProgress = revealProgress(entry.topping.parfaitImageId),
                    )
                }
            }

            if (spotlighted != null) {
                // ...(딤 Box는 그대로)

                CanvasTopping(
                    entry = spotlighted,
                    canvasWidth = areaWidth,
                    canvasHeight = areaHeight,
                    onClick = { onClickTopping(spotlighted.topping) },
                    clickable = hitTestEnabled,
                    revealProgress = revealProgress(spotlighted.topping.parfaitImageId),
                )
            }
```

`CanvasTopping` 자체에 파라미터와 알파·슬라이드 적용을 추가한다:

```kotlin
/** 토핑 하나가 최종 위치까지 슬라이드하며 나타나는 거리. 동영상 등장 애니메이션에만 쓰인다. */
private val TOPPING_REVEAL_SLIDE_DISTANCE = 24.dp

@Composable
private fun CanvasTopping(
    entry: ToppingHitEntry,
    canvasWidth: Dp,
    canvasHeight: Dp,
    onClick: () -> Unit,
    clickable: Boolean,
    revealProgress: Float = 1f,
) {
    val transform = entry.topping.transform
    val side = toppingLongSide(canvasWidth = canvasWidth, scale = transform.scale.toFloat())
    val description = stringResource(R.string.canvas_topping_content_description)
    val density = LocalDensity.current
    val revealSlideOffsetPx = with(density) {
        (TOPPING_REVEAL_SLIDE_DISTANCE * (1f - revealProgress)).toPx()
    }

    Box(
        modifier = Modifier
            .centeredAt(
                toppingCenter(
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    positionX = transform.positionX.toFloat(),
                    positionY = transform.positionY.toFloat(),
                ),
            )
            .requiredSize(side)
            .graphicsLayer {
                rotationZ = transform.rotation.toFloat()
                alpha = revealProgress
                translationY = revealSlideOffsetPx
            }
            .then(
                if (clickable) {
                    Modifier.semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = description
                        onClick {
                            onClick()
                            true
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        ToppingImage(
            painter = entry.painter,
            outline = entry.outline,
            border = entry.topping.border,
        )
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin`
Expected: PASS

- [ ] **Step 3: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt
git commit -m "feat: add revealProgress fade+slide animation to CanvasToppingLayer"
```

---

### Task 5: `CanvasVideoSourceHolder` + `CanvasMainRoute`가 캡처 시점에 채우기

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoSourceSnapshot.kt`
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoSourceHolder.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasMainScreen.kt`(가시성만 변경)
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`

**Interfaces:**
- Consumes: `CanvasMainViewModel.state.value.canvasBackground`(domain `CanvasBackground?`), `.toppings`(`List<CanvasToppingVO>`, 이미 `positionZ` 오름차순), `CanvasBackground?.toYGCanvasBackground()`(Task에서 `internal`로 승격).
- Produces: `CanvasVideoSourceHolder.put(CanvasVideoSourceSnapshot)`/`peek(): CanvasVideoSourceSnapshot?` — Task 8이 `RequestCanvasVideoCapture` 처리에서 `peek()`으로 읽는다.

- [ ] **Step 1: `CanvasVideoSourceSnapshot.kt` 작성**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO

/**
 * 동영상 프레임 캡처에 필요한 배경·토핑 스냅샷. [toppings] 는 `positionZ` 오름차순이어야 한다 —
 * [CanvasVideoSourceHolder] 를 채우는 쪽이 그 순서를 보장한다.
 */
internal data class CanvasVideoSourceSnapshot(
    val background: YGCanvasBackground?,
    val toppings: List<CanvasToppingVO>,
)
```

- [ ] **Step 2: `CanvasVideoSourceHolder.kt` 작성**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

/**
 * 캡처한 배경·토핑 스냅샷을 동영상 저장 요청 시점까지 건네는 자리. [CanvasCaptureHolder] 와
 * 같은 이유로 읽으면서 비우지 않는다 — 다음 [put] 이 덮을 때만 갱신된다.
 */
internal object CanvasVideoSourceHolder {
    @Volatile
    private var snapshot: CanvasVideoSourceSnapshot? = null

    fun put(value: CanvasVideoSourceSnapshot) {
        snapshot = value
    }

    fun peek(): CanvasVideoSourceSnapshot? = snapshot
}
```

- [ ] **Step 3: `toYGCanvasBackground()`를 `internal`로 승격**

`feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasMainScreen.kt`에서:

```kotlin
private fun CanvasBackground?.toYGCanvasBackground(): YGCanvasBackground? = when (this) {
```

를

```kotlin
internal fun CanvasBackground?.toYGCanvasBackground(): YGCanvasBackground? = when (this) {
```

로 바꾼다(같은 모듈이라 `route` 패키지에서도 호출 가능해진다. 함수 앞 KDoc은 그대로 둔다).

- [ ] **Step 4: `CanvasMainRoute`가 캡처 시점에 홀더를 채운다**

`feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`의 `RequestCanvasCaptureForPreview` 처리 블록(85-201줄 부근)에 임포트 두 개(`CanvasVideoSourceHolder`, `CanvasVideoSourceSnapshot`, `toYGCanvasBackground`)를 추가하고, `CanvasCaptureHolder.put(bitmap)` 바로 다음 줄에 추가한다:

```kotlin
                is CanvasMainEffect.RequestCanvasCaptureForPreview -> {
                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    val selectedDate = viewModel.state.value.selectedDate

                    withContext(Dispatchers.IO) { bitmap.writeToCanvasCaptureCache(context) }
                        .onSuccess { file ->
                            // ⚠️ 홀더에 건넸다고 파일 쓰기를 지우면 안 된다 — 위 ResultEffect 가
                            // 저장할 때 그 파일을 읽는다
                            CanvasCaptureHolder.put(bitmap)
                            CanvasVideoSourceHolder.put(
                                CanvasVideoSourceSnapshot(
                                    background = viewModel.state.value.canvasBackground.toYGCanvasBackground(),
                                    toppings = viewModel.state.value.toppings,
                                ),
                            )
                            navigator.goTo(
                                destination = NavKeyCanvasImageSave(
                                    imagePath = file.absolutePath,
                                    date = selectedDate.toString(),
                                ),
                            )
                        }.onFailure { toastPolicy.showError(captureFailureMessage) }
                }
```

필요한 임포트를 파일 상단에 추가한다:

```kotlin
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoSourceHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoSourceSnapshot
import com.teamyg.parfait.feature.groups.canvas.impl.screen.toYGCanvasBackground
```

- [ ] **Step 5: 컴파일 확인**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoSourceSnapshot.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoSourceHolder.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasMainScreen.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt
git commit -m "feat: capture background+topping snapshot for video into CanvasVideoSourceHolder"
```

---

### Task 6: 프레임 순서 계획(순수 함수, TDD) + `CanvasVideoCaptureHost` 오프스크린 컴포저블

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoFramePlan.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoFramePlanTest.kt`
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasVideoCaptureHost.kt`

**Interfaces:**
- Consumes: Task 5의 `CanvasVideoSourceSnapshot`, Task 4의 `CanvasToppingLayer(revealProgress = ...)`.
- Produces: `canvasVideoRevealSteps(stepsPerTopping: Int): List<Float>`, `CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING`, `CANVAS_VIDEO_FRAME_DURATION_MS`, `CANVAS_VIDEO_FINAL_HOLD_FRAMES` — Task 8의 캡처 루프가 이 상수·함수로 몇 프레임을 몇 단계로 찍을지 정한다. `CanvasVideoCaptureHost(background, toppings, revealProgress, graphicsLayer, modifier)` — Task 8이 오프스크린으로 마운트한다.

- [ ] **Step 1: 실패하는 테스트 작성**

`feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoFramePlanTest.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasVideoFramePlanTest {
    @Test
    fun canvasVideoRevealSteps_returnsProgressFromFirstStepToOne() {
        // Given 토핑 하나가 4단계로 나타난다
        val steps = canvasVideoRevealSteps(stepsPerTopping = 4)

        // Then 0은 포함하지 않고(이미 이전 프레임에서 봤다) 1.0으로 끝난다
        assertEquals(listOf(0.25f, 0.5f, 0.75f, 1.0f), steps)
    }

    @Test
    fun canvasVideoRevealSteps_singleStep_returnsOnlyFullyRevealed() {
        val steps = canvasVideoRevealSteps(stepsPerTopping = 1)

        assertEquals(listOf(1.0f), steps)
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoFramePlanTest"`
Expected: FAIL — `canvasVideoRevealSteps` unresolved reference.

- [ ] **Step 3: `CanvasVideoFramePlan.kt` 작성**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

/** 토핑 하나가 나타나는 데 쓰는 단계 수. 값이 클수록 더 부드럽지만 프레임(=인코딩 비용)이 늘어난다. */
internal const val CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING = 6

/** 캡처한 프레임 하나가 완성된 동영상에서 머무는 시간. */
internal const val CANVAS_VIDEO_FRAME_DURATION_MS = 60L

/** 마지막 토핑까지 다 나타난 뒤, 화면이 바로 끝나지 않도록 더 유지하는 정지 프레임 수. */
internal const val CANVAS_VIDEO_FINAL_HOLD_FRAMES = 10

/**
 * 토핑 하나가 나타나는 동안 캡처할 진행도 목록. 0은 포함하지 않는다 — 그 상태는 이전 토핑의
 * 마지막 프레임(또는 배경만 있는 시작 프레임)이 이미 담고 있다.
 */
internal fun canvasVideoRevealSteps(stepsPerTopping: Int): List<Float> =
    (1..stepsPerTopping).map { step -> step / stepsPerTopping.toFloat() }
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoFramePlanTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: `CanvasVideoCaptureHost.kt` 작성**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 동영상 프레임을 캡처하기 위한 오프스크린 재생 자리. [YGCanvas] 의 캡처(배경+토핑만, 테두리·
 * 메뉴 같은 화면 크롬 제외)와 같은 대상을 그린다 — 호출부가 화면 밖으로 배치해 사용자에게는
 * 보이지 않는다.
 */
@Composable
internal fun CanvasVideoCaptureHost(
    background: YGCanvasBackground?,
    toppings: List<CanvasToppingVO>,
    revealProgress: Map<ParfaitImageId, Float>,
    graphicsLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(LocalConfiguration.current.screenWidthDp.dp)
            .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
            .drawWithContent {
                graphicsLayer.record { this@drawWithContent.drawContent() }
                drawLayer(graphicsLayer)
            },
    ) {
        when (background) {
            null -> Box(modifier = Modifier.matchParentSize().background(YGAtomicColors.Gray.White))
            is YGCanvasBackground.Solid -> Box(modifier = Modifier.matchParentSize().background(background.color))
            is YGCanvasBackground.Image -> AsyncImage(
                model = background.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        CanvasToppingLayer(
            toppings = toppings,
            spotlightedToppingId = null,
            onClickTopping = {},
            onClickSpotlightDim = {},
            hitTestEnabled = false,
            revealProgress = { id -> revealProgress[id] ?: 0f },
            modifier = Modifier.matchParentSize(),
        )
    }
}
```

- [ ] **Step 6: 컴파일 확인**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoFramePlan.kt \
        feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoFramePlanTest.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasVideoCaptureHost.kt
git commit -m "feat: add canvas video frame plan and offscreen capture host"
```

---

### Task 7: `CanvasMainViewModel`에 동영상 저장 인텐트/이펙트 (TDD)

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt`
- Modify: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `SaveCanvasVideoToGalleryUseCase`, Task 3의 `CanvasVideoEncoder`.
- Produces: `CanvasMainIntent.SaveCapturedCanvasVideo`, `CanvasMainIntent.EncodeAndSaveCanvasVideo(frames: List<File>, outputFile: File)`, `CanvasMainEffect.RequestCanvasVideoCapture`, `CanvasMainEffect.ShowGalleryVideoSaveResult(isSuccess: Boolean, date: LocalDate)` — Task 8의 `CanvasMainRoute`가 이 이펙트를 받아 캡처를 실행하고, 캡처가 끝나면 `EncodeAndSaveCanvasVideo` 인텐트를 보낸다.

- [ ] **Step 1: 실패하는 테스트 작성**

`feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`에 `mockk()` 필드 2개, `viewModel()` 팩토리 인자 2개, 테스트 2개를 추가한다.

기존 필드 선언부(83번째 줄 `saveCanvasToGallery` 바로 아래)에 추가:

```kotlin
    private val saveCanvasVideoToGallery: SaveCanvasVideoToGalleryUseCase = mockk()
    private val canvasVideoEncoder: CanvasVideoEncoder = mockk()
```

`viewModel()` 팩토리(142번째 줄 부근, `saveCanvasToGalleryUseCase = saveCanvasToGallery,` 바로 아래)에 추가:

```kotlin
        saveCanvasVideoToGalleryUseCase = saveCanvasVideoToGallery,
        canvasVideoEncoder = canvasVideoEncoder,
```

파일 상단 import에 추가:

```kotlin
import com.teamyg.parfait.domain.provider.CanvasVideoEncoder
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasVideoToGalleryUseCase
import java.io.File
```

`saveCapturedCanvas_useCaseSucceeds_showsSuccessWithTheViewedDate` 테스트(746번째 줄 부근) 바로 아래에 두 테스트를 추가:

```kotlin
    @Test
    fun onClickSaveVideo_requestsCaptureWithoutEncodingYet() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열린 상태
        val viewModel = enteredViewModel()

        // When "동영상으로 저장"을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvasVideo)

            // Then 바로 인코딩하지 않는다 — 화면 밖 캡처만 요청한다
            assertEquals(CanvasMainEffect.RequestCanvasVideoCapture, awaitItem())
        }
    }

    @Test
    fun encodeAndSaveCanvasVideo_useCaseSucceeds_showsSuccessWithTheViewedDate() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열린 상태이고 인코딩·저장이 모두 성공한다
        val viewModel = enteredViewModel()
        val frames = listOf(mockk<File>(relaxed = true))
        val outputFile = mockk<File>(relaxed = true)
        coEvery { canvasVideoEncoder.encode(frames, any(), outputFile) } returns Result.success(Unit)
        coEvery { saveCanvasVideoToGallery(outputFile, any()) } returns Result.success(Unit)

        // When 화면이 캡처한 프레임을 돌려준다
        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.EncodeAndSaveCanvasVideo(frames, outputFile))

            // Then 지금 보고 있는 날짜와 함께 성공을 알린다
            assertEquals(
                CanvasMainEffect.ShowGalleryVideoSaveResult(isSuccess = true, date = today),
                awaitItem(),
            )
        }
    }

    @Test
    fun encodeAndSaveCanvasVideo_encodingFails_showsFailureAndNeverCallsSave() = runTest(mainDispatcherRule.dispatcher) {
        // Given 인코딩이 실패한다
        val viewModel = enteredViewModel()
        val frames = listOf(mockk<File>(relaxed = true))
        val outputFile = mockk<File>(relaxed = true)
        coEvery { canvasVideoEncoder.encode(frames, any(), outputFile) } returns
            Result.failure(IllegalStateException("인코딩 실패"))

        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.EncodeAndSaveCanvasVideo(frames, outputFile))

            // Then 실패를 알리고
            assertEquals(
                CanvasMainEffect.ShowGalleryVideoSaveResult(isSuccess = false, date = today),
                awaitItem(),
            )
        }

        // 갤러리 저장까지 가지 않는다 — 인코딩 결과가 없으니 저장할 파일도 없다
        coVerify(exactly = 0) { saveCanvasVideoToGallery(any(), any()) }
    }
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainViewModelTest"`
Expected: FAIL — `CanvasMainIntent.SaveCapturedCanvasVideo`/`EncodeAndSaveCanvasVideo`, `CanvasMainEffect.RequestCanvasVideoCapture`/`ShowGalleryVideoSaveResult`, 생성자 파라미터 unresolved.

- [ ] **Step 3: `CanvasMainViewModel`에 인텐트·이펙트·핸들러 추가**

`sealed interface CanvasMainEffect`(186번째 줄)의 `ShowGallerySaveResult` 선언 바로 아래에 추가:

```kotlin
    /**
     * 저장 미리보기에서 "동영상으로 저장"을 눌렀다. 프레임 캡처(Compose GraphicsLayer)도
     * 화면만 할 수 있어 ViewModel 은 요청만 보낸다. 인코딩·갤러리 저장은 화면이 프레임을 다
     * 모아 돌려준 뒤 [CanvasMainIntent.EncodeAndSaveCanvasVideo] 로 이어진다.
     */
    data object RequestCanvasVideoCapture : CanvasMainEffect

    data class ShowGalleryVideoSaveResult(
        val isSuccess: Boolean,
        val date: LocalDate,
    ) : CanvasMainEffect
```

`sealed interface CanvasMainIntent`(239번째 줄)의 `SaveCapturedCanvas` 선언 바로 아래에 추가:

```kotlin
    /** 미리보기에서 "동영상으로 저장"을 눌렀다. */
    data object SaveCapturedCanvasVideo : CanvasMainIntent

    /** 화면이 오프스크린으로 캡처한 프레임을 모아 돌려줬다. 인코딩·저장은 여기서부터다. */
    data class EncodeAndSaveCanvasVideo(
        val frames: List<File>,
        val outputFile: File,
    ) : CanvasMainIntent
```

생성자(297-315번째 줄)에 두 의존성을 `saveCanvasToGalleryUseCase` 바로 아래에 추가:

```kotlin
    private val saveCanvasToGalleryUseCase: SaveCanvasToGalleryUseCase,
    private val saveCanvasVideoToGalleryUseCase: SaveCanvasVideoToGalleryUseCase,
    private val canvasVideoEncoder: CanvasVideoEncoder,
```

`when (intent)` 분기(611번째 줄 `is CanvasMainIntent.SaveCapturedCanvas -> ...` 바로 아래)에 추가:

```kotlin
            is CanvasMainIntent.SaveCapturedCanvasVideo -> handleSaveCapturedCanvasVideo()
            is CanvasMainIntent.EncodeAndSaveCanvasVideo ->
                handleEncodeAndSaveCanvasVideo(intent.frames, intent.outputFile)
```

`handleSaveCapturedCanvas`(831번째 줄) 바로 아래에 핸들러 두 개를 추가:

```kotlin
    private fun handleSaveCapturedCanvasVideo() {
        postSideEffect(effect = CanvasMainEffect.RequestCanvasVideoCapture)
    }

    private fun handleEncodeAndSaveCanvasVideo(
        frames: List<File>,
        outputFile: File,
    ) {
        val date = state.value.selectedDate

        launch(key = SAVE_CANVAS_VIDEO_TO_GALLERY_KEY) {
            val encodeResult = canvasVideoEncoder.encode(
                frames = frames,
                frameDurationMs = CANVAS_VIDEO_FRAME_DURATION_MS,
                outputFile = outputFile,
            )
            val saveResult = encodeResult.fold(
                onSuccess = { saveCanvasVideoToGalleryUseCase(outputFile, outputFile.name) },
                onFailure = { Result.failure(it) },
            )

            frames.forEach { it.delete() }
            outputFile.delete()

            postSideEffect(
                effect = CanvasMainEffect.ShowGalleryVideoSaveResult(
                    isSuccess = saveResult.isSuccess,
                    date = date,
                ),
            )
        }
    }
```

파일 상단에 다음 import를 추가한다:

```kotlin
import com.teamyg.parfait.domain.provider.CanvasVideoEncoder
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasVideoToGalleryUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.CANVAS_VIDEO_FRAME_DURATION_MS
import java.io.File
```

(`CANVAS_VIDEO_FRAME_DURATION_MS`는 Task 6에서 `CanvasVideoFramePlan.kt`에 이미 선언했다 — 프레임 하나의 노출 시간이라는 같은 값을 캡처 쪽과 인코딩 쪽이 상수 하나로 공유한다.)

`companion object`(955번째 줄 `SAVE_CANVAS_TO_GALLERY_KEY` 바로 아래)에 추가:

```kotlin
        const val SAVE_CANVAS_VIDEO_TO_GALLERY_KEY = "saveCanvasVideoToGallery"
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainViewModelTest"`
Expected: PASS (전체 스위트 — 새 테스트 3개 포함 기존 테스트도 모두 통과)

- [ ] **Step 5: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt \
        feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt
git commit -m "feat: add video capture/encode intents and effects to CanvasMainViewModel"
```

---

### Task 8: `CanvasMainRoute`에서 캡처 루프 실행(권한·선행 디코딩·프레임 스텝) + 결과 토스트

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: Task 5 `CanvasVideoSourceHolder`, Task 6 `CanvasVideoCaptureHost`/`canvasVideoRevealSteps`/`CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING`/`CANVAS_VIDEO_FINAL_HOLD_FRAMES`, Task 7 `CanvasMainEffect.RequestCanvasVideoCapture`/`ShowGalleryVideoSaveResult`, `CanvasMainIntent.EncodeAndSaveCanvasVideo`.
- Produces: 없음(이 저장소는 Route 계층 테스트가 없다 — 기존 관행. Review Focus 항목은 수동/계측 확인 대상).

- [ ] **Step 1: 문자열 추가**

`feature/groups/canvas/impl/src/main/res/values/strings.xml`의 `canvas_main_capture_failure` 바로 아래에 추가:

```xml
    <string name="canvas_main_video_capture_failure">동영상을 만들지 못했어요. 잠시 후 다시 시도해 주세요</string>
    <string name="canvas_main_video_save_success">%1$d월 %2$d일의 캔버스가 동영상으로 갤러리에 저장됐어요</string>
    <string name="canvas_main_video_save_failure">동영상 저장에 실패했어요. 나중에 다시 시도해 주세요</string>
```

- [ ] **Step 2: `CanvasMainRoute`에 상태·권한 분기·캡처 루프 추가**

`CanvasMainRoute.kt`에서 바꿀 부분을 순서대로 적용한다.

(a) 상단 import 블록에 추가:

```kotlin
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.layer.GraphicsLayer
import android.content.Context
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasVideoCaptureHost
import com.teamyg.parfait.feature.groups.canvas.impl.util.CANVAS_VIDEO_FINAL_HOLD_FRAMES
import com.teamyg.parfait.feature.groups.canvas.impl.util.CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoSourceHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoSourceSnapshot
import com.teamyg.parfait.feature.groups.canvas.impl.util.canvasVideoRevealSteps
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import java.io.File
import java.io.FileOutputStream
```

(참고: `androidx.compose.ui.graphics.rememberGraphicsLayer`·`asAndroidBitmap`은 이미 다른 목적으로 import 돼 있을 수 있다 — 중복 import는 컴파일 에러이므로, 이미 있는 줄은 다시 추가하지 않는다.)

(b) `val graphicsLayer = rememberGraphicsLayer()`(92번째 줄) 바로 아래에 동영상 캡처 전용 상태를 추가:

```kotlin
    val videoGraphicsLayer = rememberGraphicsLayer()
    var activeVideoSnapshot by remember { mutableStateOf<CanvasVideoSourceSnapshot?>(null) }
    val videoRevealProgress = remember { mutableStateMapOf<ParfaitImageId, Float>() }
    var pendingVideoCapture by remember { mutableStateOf(false) }
```

(c) 권한 요청 결과 처리 람다(121-131번째 줄)를 이미지·동영상 공용으로 바꾼다:

```kotlin
    var pendingGalleryBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val galleryWritePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val bitmap = pendingGalleryBitmap
        pendingGalleryBitmap = null
        val videoRequested = pendingVideoCapture
        pendingVideoCapture = false

        when {
            granted && bitmap != null -> viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(bitmap))
            granted && videoRequested -> scope.launch {
                runCanvasVideoCapture(
                    context = context,
                    videoGraphicsLayer = videoGraphicsLayer,
                    onSnapshotActive = { activeVideoSnapshot = it },
                    onRevealProgressChange = { id, progress -> videoRevealProgress[id] = progress },
                    onFailure = { toastPolicy.showError(videoCaptureFailureMessage) },
                    onFramesReady = { frames, outputFile ->
                        viewModel.processIntent(
                            CanvasMainIntent.EncodeAndSaveCanvasVideo(frames = frames, outputFile = outputFile),
                        )
                    },
                )
            }
            bitmap != null || videoRequested -> toastPolicy.showError(gallerySaveFailureMessage)
        }
    }
```

(d) 문자열 리소스 선언부(102-104번째 줄 `captureFailureMessage` 근처)에 추가:

```kotlin
    val videoCaptureFailureMessage = stringResource(R.string.canvas_main_video_capture_failure)
    val gallerySaveVideoSuccessFormat = stringResource(R.string.canvas_main_video_save_success)
    val gallerySaveVideoFailureMessage = stringResource(R.string.canvas_main_video_save_failure)
```

(e) 이펙트 `when` 블록(161번째 줄부터)에 두 분기를 추가한다 — `RequestCanvasCaptureForPreview` 분기와 `ShowGallerySaveResult` 분기 사이 아무 곳에나:

```kotlin
                is CanvasMainEffect.RequestCanvasVideoCapture -> {
                    if (GalleryWritePermissionManager.hasPermission(context)) {
                        scope.launch {
                            runCanvasVideoCapture(
                                context = context,
                                videoGraphicsLayer = videoGraphicsLayer,
                                onSnapshotActive = { activeVideoSnapshot = it },
                                onRevealProgressChange = { id, progress -> videoRevealProgress[id] = progress },
                                onFailure = { toastPolicy.showError(videoCaptureFailureMessage) },
                                onFramesReady = { frames, outputFile ->
                                    viewModel.processIntent(
                                        CanvasMainIntent.EncodeAndSaveCanvasVideo(
                                            frames = frames,
                                            outputFile = outputFile,
                                        ),
                                    )
                                },
                            )
                        }
                    } else {
                        pendingVideoCapture = true
                        galleryWritePermissionLauncher.launch(GalleryWritePermissionManager.PERMISSION)
                    }
                }

                is CanvasMainEffect.ShowGalleryVideoSaveResult -> if (effect.isSuccess) {
                    val message = gallerySaveVideoSuccessFormat.format(effect.date.month.number, effect.date.day)
                    toastPolicy.show(YGToastType.InviteCode(message))
                } else {
                    toastPolicy.showError(gallerySaveVideoFailureMessage)
                }
```

(f) 파일 하단(같은 파일의 top-level, `private const val SPOTLIGHT_TOAST_TAG` 근처)에 캡처 루프 함수를 추가한다:

```kotlin
/**
 * 배경·토핑 이미지를 먼저 메모리 캐시에 올려 둔다 — 프레임을 여러 장 그리는 동안 이미지가
 * 늦게 도착하면 일부 프레임만 비어 보인다(정지 이미지 캡처의 OQ-P-272와 같은 결함이 프레임
 * 수만큼 반복되는 것을 막는다).
 */
private suspend fun preloadCanvasVideoImages(
    context: Context,
    snapshot: CanvasVideoSourceSnapshot,
): Result<Unit> = runCatching {
    val urls = buildList {
        (snapshot.background as? YGCanvasBackground.Image)?.let { add(it.url) }
        addAll(snapshot.toppings.map { it.imageUrl })
    }

    urls.forEach { url ->
        val result = context.imageLoader.execute(ImageRequest.Builder(context).data(url).build())
        check(result is SuccessResult) { "이미지를 미리 불러오지 못했다: $url" }
    }
}

/**
 * [CanvasVideoSourceHolder] 스냅샷을 오프스크린으로 재생하며 프레임을 순서대로 캡처해 캐시에
 * PNG로 쓴다. 배경만 있는 시작 프레임 → 토핑마다 [CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING] 단계로
 * 페이드인+슬라이드인 → 마지막에 [CANVAS_VIDEO_FINAL_HOLD_FRAMES] 만큼 정지 프레임을 더한다.
 */
private suspend fun runCanvasVideoCapture(
    context: Context,
    videoGraphicsLayer: GraphicsLayer,
    onSnapshotActive: (CanvasVideoSourceSnapshot?) -> Unit,
    onRevealProgressChange: (ParfaitImageId, Float) -> Unit,
    onFailure: () -> Unit,
    onFramesReady: (frames: List<File>, outputFile: File) -> Unit,
) {
    val snapshot = CanvasVideoSourceHolder.peek()
    if (snapshot == null) {
        onFailure()
        return
    }

    if (preloadCanvasVideoImages(context, snapshot).isFailure) {
        onFailure()
        return
    }

    val frameDir = File(context.cacheDir, "canvas_video_frames").apply { mkdirs() }
    val frames = mutableListOf<File>()

    suspend fun captureFrame() {
        // state 반영 → 레이아웃 → 드로우까지 실제로 한 번 돌 시간을 준다
        withFrameNanos {}
        withFrameNanos {}
        val bitmap = videoGraphicsLayer.toImageBitmap().asAndroidBitmap()
        val frameFile = File(frameDir, "frame_${frames.size}.png")
        FileOutputStream(frameFile).use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        frames += frameFile
    }

    try {
        onSnapshotActive(snapshot)

        captureFrame() // 배경만 있는 시작 프레임 — 모든 토핑이 revealProgress 0

        for (topping in snapshot.toppings) {
            for (progress in canvasVideoRevealSteps(CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING)) {
                onRevealProgressChange(topping.parfaitImageId, progress)
                captureFrame()
            }
        }

        repeat(CANVAS_VIDEO_FINAL_HOLD_FRAMES) { captureFrame() }

        val outputFile = File(context.cacheDir, "canvas_video_${System.currentTimeMillis()}.mp4")
        onFramesReady(frames, outputFile)
    } catch (throwable: Throwable) {
        frames.forEach { it.delete() }
        onFailure()
    } finally {
        onSnapshotActive(null)
    }
}
```

(g) `CanvasVideoCaptureHost`를 오프스크린에 마운트한다 — `YGScaffoldV2 { ... }` 블록(323-349번째 줄) 바로 다음, `canvasState.tutorialStep?.let { ... }`(351번째 줄) 앞에 추가:

```kotlin
        activeVideoSnapshot?.let { snapshot ->
            CanvasVideoCaptureHost(
                background = snapshot.background,
                toppings = snapshot.toppings,
                revealProgress = videoRevealProgress,
                graphicsLayer = videoGraphicsLayer,
                modifier = Modifier.offset(x = 10_000.dp),
            )
        }
```

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt \
        feature/groups/canvas/impl/src/main/res/values/strings.xml
git commit -m "feat: run offscreen video frame capture from CanvasMainRoute"
```

---

### Task 9: 미리보기 화면 "동영상으로 저장" 버튼 + 결과 왕복

**Files:**
- Modify: `feature/groups/canvas/api/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/api/NavKeyCanvasImageSave.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasImageSaveScreen.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasImageSaveRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: Task 7의 `CanvasMainIntent.SaveCapturedCanvasVideo`.
- Produces: 없음(이 Task가 마지막으로 사용자 입력을 `CanvasMainIntent.SaveCapturedCanvasVideo`까지 잇는다).

- [ ] **Step 1: `CanvasImageSaveResult`에 판별 필드 추가**

`feature/groups/canvas/api/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/api/NavKeyCanvasImageSave.kt`:

```kotlin
/**
 * 미리보기에서 저장을 확정했다는 결과.
 *
 * 저장 자체를 여기서 하지 않는 이유: 결과 토스트가 뜨는 자리는 캔버스 메인이다. 미리보기가
 * 저장까지 하고 나면 알림만 남기고 사라지는 화면이 되어, 실패했을 때 알릴 곳이 없다.
 *
 * @param imagePath 저장할 이미지의 경로. 넘겨받은 [NavKeyCanvasImageSave.imagePath] 를 그대로 돌려준다
 * @param isVideoRequested true면 이미지가 아니라 동영상으로 저장해 달라는 뜻이다 — 이때는
 *  [imagePath] 대신 `CanvasVideoSourceHolder` 스냅샷을 쓴다
 */
data class CanvasImageSaveResult(
    val imagePath: String,
    val isVideoRequested: Boolean = false,
)
```

- [ ] **Step 2: 미리보기 화면에 버튼 추가**

`CanvasImageSaveScreen.kt`의 시그니처와 하단 버튼 영역을 바꾼다:

```kotlin
@Composable
internal fun CanvasImageSaveScreen(
    bitmap: ImageBitmap?,
    fallbackImagePath: String,
    date: LocalDate,
    onClickClose: () -> Unit,
    onClickSave: () -> Unit,
    onClickSaveVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

`YGButton(... onClick = onClickSave)`를 감싸던 `Box`(148-165번째 줄)를 다음으로 바꾼다:

```kotlin
        Column(
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.padding.padding6,
                    end = YGTheme.layout.padding.padding7,
                    bottom = YGTheme.layout.padding.padding7,
                ),
        ) {
            YGButton(
                text = stringResource(R.string.canvas_image_save_confirm),
                buttonType = YGButtonType.Medium.Primary,
                isEnabled = true,
                onClick = onClickSave,
            )

            YGButton(
                text = stringResource(R.string.canvas_image_save_confirm_video),
                buttonType = YGButtonType.Medium.Secondary,
                isEnabled = true,
                onClick = onClickSaveVideo,
            )
        }
```

(`YGButtonType.Medium.Secondary`는 `core/designsystem/.../ygbutton/YGButtonType.kt`에 이미 정의돼 있다 — 회색 배경+테두리의 보조 버튼 스타일이다.)

미리보기 컴포저블(178-191번째 줄)에도 `onClickSaveVideo = {}`를 추가한다.

- [ ] **Step 3: Route가 버튼 클릭을 왕복 결과로 보낸다**

`CanvasImageSaveRoute.kt`:

```kotlin
    YGScaffoldV2(modifier = modifier) { innerPadding ->
        CanvasImageSaveScreen(
            bitmap = capturedBitmap,
            fallbackImagePath = navKey.imagePath,
            date = LocalDate.parse(navKey.date),
            onClickClose = { navigator.onBack() },
            onClickSave = {
                resultEventBus.sendResult(
                    CANVAS_IMAGE_SAVE_RESULT_KEY,
                    CanvasImageSaveResult(imagePath = navKey.imagePath),
                )
                navigator.onBack()
            },
            onClickSaveVideo = {
                resultEventBus.sendResult(
                    CANVAS_IMAGE_SAVE_RESULT_KEY,
                    CanvasImageSaveResult(imagePath = navKey.imagePath, isVideoRequested = true),
                )
                navigator.onBack()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
```

- [ ] **Step 4: `CanvasMainRoute`의 기존 `ResultEffect`를 분기시킨다**

`CanvasMainRoute.kt`의 `ResultEffect<CanvasImageSaveResult>`(146-152번째 줄)를 다음으로 바꾼다:

```kotlin
    ResultEffect<CanvasImageSaveResult>(resultKey = CANVAS_IMAGE_SAVE_RESULT_KEY) { result ->
        if (result.isVideoRequested) {
            viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvasVideo)
        } else {
            scope.launch {
                withContext(Dispatchers.IO) { readCanvasCaptureCache(result.imagePath) }
                    .onSuccess(saveWithPermission)
                    .onFailure { toastPolicy.showError(gallerySaveFailureMessage) }
            }
        }
    }
```

- [ ] **Step 5: 문자열 추가**

`strings.xml`의 `canvas_image_save_confirm` 바로 아래에 추가:

```xml
    <string name="canvas_image_save_confirm_video">동영상으로 저장</string>
```

- [ ] **Step 6: 컴파일 확인**

Run: `./gradlew :feature:groups:canvas:api:compileDebugKotlin :feature:groups:canvas:impl:compileDebugKotlin`
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add feature/groups/canvas/api/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/api/NavKeyCanvasImageSave.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasImageSaveScreen.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasImageSaveRoute.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt \
        feature/groups/canvas/impl/src/main/res/values/strings.xml
git commit -m "feat: add save-as-video button to canvas save preview screen"
```

---

### Task 10: 전체 빌드·테스트 확인 + 문서 갱신

**Files:**
- Modify: `docs/status.md`

**Interfaces:**
- Consumes: 없음(마무리 검증 Task).
- Produces: 없음.

- [ ] **Step 1: 전체 유닛 테스트**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (기존 테스트 전부 + 이 계획이 추가한 테스트 전부)

- [ ] **Step 2: 전체 컴파일/린트**

Run: `./gradlew assembleDebug`
Expected: PASS

- [ ] **Step 3: `docs/status.md` "캔버스 이미지 저장" 절 갱신**

`docs/status.md`에서 "## 캔버스 이미지 저장" 절을 찾아, 이미지 저장 흐름 설명 뒤에 동영상 저장 경로를 한 문단 덧붙인다(프로젝트 CLAUDE.md 규칙대로 "여러 파일을 가로지르는 현재 상태 종합"만 적고 PR 서술은 넣지 않는다):

```markdown
같은 화면에 "동영상으로 저장"도 있다 — `positionZ` 순서대로 토핑이 페이드인+슬라이드인하며
쌓이는 mp4를 만든다. 프레임은 `CanvasToppingLayer`를 오프스크린(`CanvasVideoCaptureHost`)으로
재생하며 `GraphicsLayer`로 캡처하고(정지 이미지와 같은 메커니즘), `media3 Transformer`로
인코딩해 `MediaStore.Video`에 저장한다. 앵커 심볼: `CanvasVideoSourceHolder`,
`CanvasVideoCaptureHost`, `CanvasVideoEncoder`, `CanvasMainEffect.RequestCanvasVideoCapture`.
순서 기준은 `positionZ` 단일 기준이라 위 "토핑 생성·배치" 절의 동시 배치 흔들림(OQ-P-322)이
동영상 등장 순서에도 그대로 전이된다.
```

- [ ] **Step 4: 링크 검사**

Run: `python3 docs/script/check_links.py docs`
Expected: `깨진 링크 0건`

- [ ] **Step 5: 커밋**

```bash
git add docs/status.md
git commit -m "docs: 캔버스 동영상 저장 현재 상태 반영"
```
