---
id: upload-image-downscale
title: 업로드 이미지 다운스케일·배경 JPEG 고정
status: done
type: work-order
created: 2026-09-08
updated: 2026-09-09
platforms: android
owner: Parfait 팀
related_adr: ADR-0017
related_spec: upload-image-downscale
related_code: ImageUploadRepositoryImpl, UploadImagePreprocessor, UploadImagePreprocessorImpl, UploadImagePlan, PreparedUploadImage, UploadImageSize, UploadImageFormat, UtilsModule, File#readExifDegrees, ImageUploadRepositoryImplTest, UploadImagePlanTest
archived_reason: Task 1~3 을 전량 수행하고 develop 에 머지했다(2026-09-09, PR #473 `acbc4b457`). 신규 유닛 12건(UploadImagePlanTest 8 · ImageUploadRepositoryImplTest 순증 4). Task 4(실기기 수동 검증)는 저장소에서 확인할 수 없어 미체크로 둔다. 각도 판독이 `File#readExifDegrees` 로 서고 전처리 모델이 `data/model/image` 로 내려간 것은 계획 이후의 재배치다
tags: [plan, parfait, image, upload]
---

# 업로드 이미지 다운스케일·배경 JPEG 고정 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서버로 나가는 이미지를 업로드 직전에 긴 변 상한까지 줄이고, 배경은 JPEG로 고정한다.

**Architecture:** 축소·재인코딩을 `ImageUploadRepositoryImpl.upload` **한 자리**에서 한다. 판정(목표 치수·`inSampleSize`·출력 포맷)은 Android에 의존하지 않는 순수 함수 `planUploadImage`로 빼서 JVM 유닛으로 덮고, 실제 디코드·인코딩만 `UploadImagePreprocessorImpl`이 맡는다. 저장소는 인터페이스만 알기 때문에 배선 검증도 JVM에서 끝난다.

> 📌 **구현 뒤 재배치했다(2026-09-09, 리뷰 지적).** 아래 태스크 본문의 파일 경로·심볼은 작성 시점
> 기준이므로 그대로 둔다. 현행은 이렇다 — 순수 판정이 `UploadImageScale.kt`의 top-level
> `planUploadImage`에서 **`UploadImagePlan.of`**(`model/image/UploadImagePlan.kt`의 companion)로
> 옮겨졌고 상한 상수·헬퍼는 그 companion의 `private`이다. `UPLOAD_JPEG_QUALITY`는
> `UploadImagePlan.JPEG_QUALITY`다. `UploadImageSize`·`PreparedUploadImage`는 각자 파일로 갈라져
> `model/image/`에 있고, `UploadImagePreprocessor`·`UploadImagePreprocessorImpl`은 데이터 접근이
> 아니라 비트맵 도구라 **`utils/image/`**로 옮겼다. 그 결과 Hilt `@Binds`도 `LocalDataSourceModule`을
> 떠나 신설 `UtilsModule`로 갔다. 테스트는 `UploadImagePlanTest.kt`이고 메서드 접두사는 `of_`다.

**Tech Stack:** Kotlin, Hilt, `android.graphics.BitmapFactory`/`Bitmap`, Kotlin Coroutines, 테스트는 kotlin-test + MockK + kotlinx-coroutines-test.

**Spec:** [`parfait/specs/2026-09-08-upload-image-downscale.md`](../../specs/archive/2026-09-08-upload-image-downscale.md)

**작업 저장소:** `TJYG-Android` (remote `mash-up-kr/TEAMYG-Android`). 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다. 브랜치 `feature/#471-image-down-scale` 위에서 작업한다.

## Global Constraints

- **작업 위치**: `TJYG-Android` 저장소의 **본 체크아웃**, 현재 브랜치 `feature/#471-image-down-scale`. **git worktree를 만들지 않는다.**
- **커밋하지 않는다.** 사용자가 커밋을 요청하지 않았다. 각 Task는 코드 편집 + 검증까지만 하고 멈춘다. `git add`·`git commit`·`git push` 모두 금지.
- **긴 변 상한은 imageType마다 다르다**: `NUKKI` = **1500**, `BACKGROUND` = **2048**. **JPEG quality는 90.** 이 셋은 `TEAMYG-iOS`의 `ToppingImageEncoder.maximumLongEdge`·`BackgroundImageLoader.maximumLongEdge`·`jpegCompressionQuality`와 맞춘 값이다. 임의로 바꾸지 않는다.
- **확대는 어떤 경우에도 하지 않는다.** 긴 변이 상한 이하면 치수를 건드리지 않는다.
- **발급 요청과 S3 PUT은 반드시 같은 contentType을 쓴다.** 둘이 갈라지면 S3가 서명 불일치로 거절하고 그 실패는 서버 로그에 남지 않는다.
- **새 의존성을 추가하지 않는다.** Robolectric·계측 테스트 소스셋을 만들지 않는다(사용자가 명시적으로 배제했다).
- **코드 주석·KDoc 규약**(`parfait/CLAUDE.md` 요지):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다). 써야 하면 근거 문서를 가리킨다.
  - 주석 분량은 그 코드의 **어려움**에 비례한다. 중요하지만 단순한 코드에 긴 주석을 달지 않는다.
- **주석·KDoc은 한국어**로 쓴다. 기존 파일들의 어투를 따른다.
- **매퍼 단독 테스트를 만들지 않는다.**
- ktlint가 CI 게이트다(`.github/workflows/ktlint.yml`이 `./gradlew ktlintCheck`를 돈다). 각 Task 끝에 `./gradlew :data:ktlintCheck`를 돌린다.

---

## File Structure

**Create**

| 파일 | 역할 |
|------|------|
| `data/src/main/java/com/teamyg/parfait/data/model/image/UploadImageScale.kt` | 순수 판정 — 상한 상수, 목표 치수, `inSampleSize`, 출력 포맷, 통과/재인코딩 결정 |
| `data/src/main/java/com/teamyg/parfait/data/source/image/local/UploadImagePreprocessor.kt` | 전처리 인터페이스 + `PreparedUploadImage` |
| `data/src/main/java/com/teamyg/parfait/data/source/image/local/UploadImagePreprocessorImpl.kt` | `BitmapFactory` 디코드 → 축소 → 인코딩 |
| `data/src/test/java/com/teamyg/parfait/data/model/image/UploadImageScaleTest.kt` | 순수 판정 JVM 유닛 |

**Modify**

| 파일 | 변경 |
|------|------|
| `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImpl.kt` | 전처리기 주입, 전처리 결과로 발급·PUT, 임시 파일 정리 |
| `data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt` | `@Binds` 한 줄 추가 |
| `data/src/test/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImplTest.kt` | 생성자 인자 추가로 **기존 12개 테스트가 컴파일 실패한다** — 대역을 세우고 케이스를 더한다 |

---

## Task 1: 순수 판정 로직

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/model/image/UploadImageScale.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/model/image/UploadImageScaleTest.kt`

**Interfaces:**
- Consumes: `UploadImageFormat`(기존, `data/model/image`), `ImageType`(기존, `domain/model/image`)
- Produces:
  - `UploadImageSize(width: Int, height: Int)`
  - `UploadImagePlan` — `Passthrough` / `Reencode(targetSize: UploadImageSize, sampleSize: Int, format: UploadImageFormat)`
  - `fun planUploadImage(sourceSize: UploadImageSize, imageType: ImageType, sourceFormat: UploadImageFormat): UploadImagePlan`
  - `const val UPLOAD_JPEG_QUALITY = 90`

- [x] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/model/image/UploadImageScaleTest.kt`:

```kotlin
package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.image.ImageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UploadImageScaleTest {
    @Test
    fun planUploadImage_nukkiPngUnderLimit_passesThrough() {
        // Given 누끼 PNG 가 상한 이하다
        val sourceSize = UploadImageSize(width = 1000, height = 1500)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.PNG)

        // Then 원본을 그대로 올린다 - 확대도 재인코딩도 하지 않는다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun planUploadImage_nukkiPngOverLimit_scalesKeepingRatio() {
        // Given 긴 변이 누끼 상한 1500 을 넘는다
        val sourceSize = UploadImageSize(width = 2600, height = 3832)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.PNG)

        // Then 긴 변이 상한이 되고 짧은 변은 비율을 지킨다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1500, reencode.targetSize.height)
        assertEquals(1018, reencode.targetSize.width)
        assertEquals(UploadImageFormat.PNG, reencode.format)
    }

    @Test
    fun planUploadImage_limitDiffersByImageType() {
        // Given 두 상한 사이에 놓인 크기다 - 누끼 1500 초과, 배경 2048 이하
        val sourceSize = UploadImageSize(width = 1600, height = 1200)

        // When 같은 크기를 두 용도로 계획한다
        val nukkiPlan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.JPEG)
        val backgroundPlan = planUploadImage(sourceSize, ImageType.BACKGROUND, UploadImageFormat.JPEG)

        // Then 용도마다 상한이 달라 결과가 갈린다
        assertIs<UploadImagePlan.Reencode>(nukkiPlan)
        assertEquals(UploadImagePlan.Passthrough, backgroundPlan)
    }

    @Test
    fun planUploadImage_backgroundPngUnderLimit_reencodesToJpeg() {
        // Given 상한 이하인 PNG 스크린샷을 배경으로 고른다
        val sourceSize = UploadImageSize(width = 1080, height = 1920)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.BACKGROUND, UploadImageFormat.PNG)

        // Then 치수는 그대로지만 포맷 때문에 다시 굽는다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(sourceSize, reencode.targetSize)
        assertEquals(UploadImageFormat.JPEG, reencode.format)
    }

    @Test
    fun planUploadImage_backgroundJpegAtExactLimit_passesThrough() {
        // Given 긴 변이 배경 상한과 정확히 같다
        val sourceSize = UploadImageSize(width = 2048, height = 1000)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.BACKGROUND, UploadImageFormat.JPEG)

        // Then 경계값은 축소 대상이 아니다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun planUploadImage_backgroundJpegOneOverLimit_scales() {
        // Given 긴 변이 상한보다 1px 크다
        val sourceSize = UploadImageSize(width = 2049, height = 1000)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.BACKGROUND, UploadImageFormat.JPEG)

        // Then 축소한다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(2048, reencode.targetSize.width)
    }

    @Test
    fun planUploadImage_extremeAspectRatio_keepsShortSideAtLeastOne() {
        // Given 짧은 변이 비율대로 줄이면 0 이 되는 극단 종횡비다
        val sourceSize = UploadImageSize(width = 6000, height = 2)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.PNG)

        // Then 0 픽셀 비트맵은 만들 수 없으므로 1 로 바닥을 친다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1500, reencode.targetSize.width)
        assertEquals(1, reencode.targetSize.height)
    }

    @Test
    fun planUploadImage_sampleSizeNeverUndershootsTarget() {
        // Given 큰 사진이다
        val sourceSize = UploadImageSize(width = 2600, height = 3832)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.PNG)

        // Then 사전 축소판이 목표보다 작아지면 안 된다 - 그러면 확대해서 맞추게 된다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(2, reencode.sampleSize)
        assertEquals(true, sourceSize.width / reencode.sampleSize >= reencode.targetSize.width)
        assertEquals(true, sourceSize.height / reencode.sampleSize >= reencode.targetSize.height)
    }
}
```

- [x] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.model.image.UploadImageScaleTest"`
Expected: 컴파일 실패 — `Unresolved reference: planUploadImage`

- [x] **Step 3: 최소 구현을 쓴다**

`data/src/main/java/com/teamyg/parfait/data/model/image/UploadImageScale.kt`:

```kotlin
package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.image.ImageType
import kotlin.math.roundToInt

/**
 * 업로드 이미지의 긴 변 상한. iOS 의 `ToppingImageEncoder.maximumLongEdge` ·
 * `BackgroundImageLoader.maximumLongEdge` 와 맞춘 값이다 — 같은 서버에 같은 기능으로 올리므로
 * 플랫폼마다 다르면 같은 캔버스가 기기별로 다른 화질이 된다(`specs/2026-09-08-upload-image-downscale.md`).
 */
private const val NUKKI_LONG_SIDE_LIMIT = 1500
private const val BACKGROUND_LONG_SIDE_LIMIT = 2048

/** PNG 는 무손실이라 이 값을 보지 않는다 */
const val UPLOAD_JPEG_QUALITY = 90

data class UploadImageSize(
    val width: Int,
    val height: Int,
)

sealed interface UploadImagePlan {
    /** 원본 파일을 그대로 올린다 */
    data object Passthrough : UploadImagePlan

    /**
     * @param sampleSize 디코드 단계에서 미리 줄일 배수. 원본을 통째로 힙에 올리지 않으려는 것이다
     */
    data class Reencode(
        val targetSize: UploadImageSize,
        val sampleSize: Int,
        val format: UploadImageFormat,
    ) : UploadImagePlan
}

/**
 * 치수와 포맷 둘 다 그대로여도 되는지 판정한다. 어느 한쪽이라도 바뀌어야 다시 굽는다 —
 * 이미 JPEG 이고 상한 이하인 배경을 다시 구우면 손실만 더해진다.
 */
fun planUploadImage(
    sourceSize: UploadImageSize,
    imageType: ImageType,
    sourceFormat: UploadImageFormat,
): UploadImagePlan {
    val targetSize = scaledSize(sourceSize, longSideLimitOf(imageType))
    val targetFormat = uploadFormatOf(imageType, sourceFormat)

    if (targetSize == sourceSize && targetFormat == sourceFormat) return UploadImagePlan.Passthrough

    return UploadImagePlan.Reencode(
        targetSize = targetSize,
        sampleSize = sampleSizeOf(sourceSize, targetSize),
        format = targetFormat,
    )
}

private fun longSideLimitOf(imageType: ImageType): Int = when (imageType) {
    ImageType.NUKKI -> NUKKI_LONG_SIDE_LIMIT
    ImageType.BACKGROUND -> BACKGROUND_LONG_SIDE_LIMIT
}

/** 배경은 캔버스를 덮는 불투명 이미지라 알파를 버려도 잃는 것이 없다. 누끼는 입력을 따라간다 */
private fun uploadFormatOf(
    imageType: ImageType,
    sourceFormat: UploadImageFormat,
): UploadImageFormat = when (imageType) {
    ImageType.NUKKI -> sourceFormat
    ImageType.BACKGROUND -> UploadImageFormat.JPEG
}

/** 상한 이하면 손대지 않는다 — 확대는 정보를 늘리지 않으면서 바이트만 키운다 */
private fun scaledSize(
    sourceSize: UploadImageSize,
    longSideLimit: Int,
): UploadImageSize {
    val longSide = maxOf(sourceSize.width, sourceSize.height)
    if (longSide <= longSideLimit) return sourceSize

    val ratio = longSideLimit.toDouble() / longSide
    return UploadImageSize(
        width = (sourceSize.width * ratio).roundToInt().coerceAtLeast(1),
        height = (sourceSize.height * ratio).roundToInt().coerceAtLeast(1),
    )
}

/**
 * 목표보다 작아지지 않는 선까지만 2 의 거듭제곱으로 줄인다. 넘겨서 줄이면 뒤에서 확대하게 되고,
 * `BitmapFactory` 는 2 의 거듭제곱이 아닌 값을 그 아래 거듭제곱으로 내림한다.
 */
private fun sampleSizeOf(
    sourceSize: UploadImageSize,
    targetSize: UploadImageSize,
): Int {
    var sampleSize = 1
    while (sourceSize.width / (sampleSize * 2) >= targetSize.width &&
        sourceSize.height / (sampleSize * 2) >= targetSize.height
    ) {
        sampleSize *= 2
    }
    return sampleSize
}
```

- [x] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.model.image.UploadImageScaleTest"`
Expected: PASS (8건)

- [x] **Step 5: ktlint를 돌린다**

Run: `./gradlew :data:ktlintCheck`
Expected: PASS

---

## Task 2: 전처리 인터페이스와 저장소 배선

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/source/image/local/UploadImagePreprocessor.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImplTest.kt`

**Interfaces:**
- Consumes: Task 1의 `UploadImageFormat`(기존)
- Produces:
  - `interface UploadImagePreprocessor { suspend fun prepare(file: File, imageType: ImageType): Result<PreparedUploadImage> }`
  - `data class PreparedUploadImage(val file: File, val format: UploadImageFormat, val isTemporary: Boolean)`
  - `ImageUploadRepositoryImpl(imageRemoteDataSource, presignedUploadDataSource, uploadImagePreprocessor)`

⚠️ **생성자에 인자가 하나 늘어 기존 테스트 12개가 전부 컴파일 실패한다.** Step 1이 그 대역을 세운다.

- [x] **Step 1: 인터페이스를 만든다**

`data/src/main/java/com/teamyg/parfait/data/source/image/local/UploadImagePreprocessor.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.local

import com.teamyg.parfait.data.model.image.UploadImageFormat
import com.teamyg.parfait.domain.model.image.ImageType
import java.io.File

/**
 * 업로드 직전에 이미지를 서버로 보낼 형태로 맞춘다.
 *
 * @param isTemporary 이 전처리가 새로 만든 파일이라 부른 쪽이 지워야 한다는 뜻이다.
 *   거짓이면 넘겨받은 파일 그대로라 수명은 여전히 부른 쪽 밖에 있다.
 */
data class PreparedUploadImage(
    val file: File,
    val format: UploadImageFormat,
    val isTemporary: Boolean,
)

interface UploadImagePreprocessor {
    suspend fun prepare(
        file: File,
        imageType: ImageType,
    ): Result<PreparedUploadImage>
}
```

- [x] **Step 2: 실패하는 테스트를 쓴다**

`ImageUploadRepositoryImplTest.kt`를 고친다. 대역과 생성자를 이렇게 바꾼다(기존 `imageRemoteDataSource`·`presignedUploadDataSource` 선언은 그대로 두고 아래를 더한다):

```kotlin
private val uploadImagePreprocessor: UploadImagePreprocessor = mockk()
private val repository = ImageUploadRepositoryImpl(
    imageRemoteDataSource = imageRemoteDataSource,
    presignedUploadDataSource = presignedUploadDataSource,
    uploadImagePreprocessor = uploadImagePreprocessor,
)
```

기존 `givenAllStepsSucceed()`에 전처리 기본 동작을 더한다 — **원본을 그대로 통과시키는 대역**이라 기존 12개 테스트의 단언이 그대로 성립한다:

```kotlin
private fun givenAllStepsSucceed() {
    coEvery { uploadImagePreprocessor.prepare(any(), any()) } answers {
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
    // ... 이하 기존 그대로
}
```

기존 `upload_unsupportedExtension_failsWithoutCallingServer` 테스트는 판정 자리가 전처리기로 옮겨졌으므로 **전처리 실패로 다시 쓴다.** 그리고 새 케이스 셋을 더한다:

```kotlin
@Test
fun upload_preprocessorFails_failsWithoutCallingServer() = runTest {
    // Given 전처리가 실패한다
    coEvery { uploadImagePreprocessor.prepare(any(), any()) } returns Result.failure(
        UnsupportedImageException("서버가 받지 않는 확장자다 - heic"),
    )

    // When 업로드한다
    val result = repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

    // Then 원본으로 폴백하지 않고, 화면이 사진을 바꾸라고 말할 수 있는 갈래로 올린다
    assertIs<AppError.UnsupportedImage>(result.exceptionOrNull())
    coVerify(exactly = 0) { imageRemoteDataSource.issueUploadUrl(any(), any(), any()) }
}

@Test
fun upload_preprocessorReencoded_usesPreparedFileAndFormat() = runTest {
    // Given 전처리가 JPEG 축소본을 새로 만들었다
    givenAllStepsSucceed()
    val prepared = File.createTempFile("prepared", ".jpg").also { it.writeBytes(ByteArray(FILE_SIZE)) }
    coEvery { uploadImagePreprocessor.prepare(any(), any()) } returns Result.success(
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
    repository.upload(filePath = file.absolutePath, imageType = ImageType.BACKGROUND)

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
    coEvery { uploadImagePreprocessor.prepare(any(), any()) } returns Result.success(
        PreparedUploadImage(file = prepared, format = UploadImageFormat.JPEG, isTemporary = true),
    )

    // When 업로드한다
    repository.upload(filePath = file.absolutePath, imageType = ImageType.BACKGROUND)

    // Then 축소본은 남지 않는다 - 캐시가 쌓이기만 하는 자리를 늘리지 않는다
    assertEquals(false, prepared.exists())
}

@Test
fun upload_temporaryPreparedFile_isDeletedEvenWhenPutFails() = runTest {
    // Given 전처리는 임시 파일을 만들었으나 전송이 실패한다
    givenAllStepsSucceed()
    val prepared = File.createTempFile("prepared", ".jpg").also { it.writeBytes(ByteArray(FILE_SIZE)) }
    coEvery { uploadImagePreprocessor.prepare(any(), any()) } returns Result.success(
        PreparedUploadImage(file = prepared, format = UploadImageFormat.JPEG, isTemporary = true),
    )
    coEvery { presignedUploadDataSource.put(any(), any(), any()) } returns Result.failure(IOException("boom"))

    // When 업로드한다
    repository.upload(filePath = file.absolutePath, imageType = ImageType.BACKGROUND)

    // Then 실패해도 지운다
    assertEquals(false, prepared.exists())
}

@Test
fun upload_passthroughFile_isNotDeleted() = runTest {
    // Given 전처리가 원본을 그대로 통과시켰다
    givenAllStepsSucceed()

    // When 업로드한다
    repository.upload(filePath = file.absolutePath, imageType = ImageType.NUKKI)

    // Then 남의 파일을 지우지 않는다 - 그 수명은 부른 쪽이 쥐고 있다
    assertEquals(true, file.exists())
}
```

import에 다음을 더한다:

```kotlin
import com.teamyg.parfait.data.model.exception.UnsupportedImageException
import com.teamyg.parfait.data.model.image.UploadImageFormat
import com.teamyg.parfait.data.source.image.local.PreparedUploadImage
import com.teamyg.parfait.data.source.image.local.UploadImagePreprocessor
```

`AppError`·`ImageType`·`File`·`IOException`은 이 파일이 이미 import 하고 있다.

- [x] **Step 3: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.repository.image.ImageUploadRepositoryImplTest"`
Expected: 컴파일 실패 — `ImageUploadRepositoryImpl` 생성자에 `uploadImagePreprocessor` 파라미터가 없다

- [x] **Step 4: 저장소를 고친다**

`ImageUploadRepositoryImpl.kt`를 통째로 아래로 바꾼다:

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.model.error.toAppError
import com.teamyg.parfait.data.source.image.local.UploadImagePreprocessor
import com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSource
import com.teamyg.parfait.data.source.image.remote.PresignedUploadDataSource
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import java.io.File
import javax.inject.Inject

class ImageUploadRepositoryImpl @Inject constructor(
    private val imageRemoteDataSource: ImageRemoteDataSource,
    private val presignedUploadDataSource: PresignedUploadDataSource,
    private val uploadImagePreprocessor: UploadImagePreprocessor,
) : ImageUploadRepository {
    override suspend fun upload(
        filePath: String,
        imageType: ImageType,
    ): Result<ImageId> {
        val file = File(filePath)
        // 발급을 먼저 부르면 올릴 것도 없는데 PENDING 행과 S3 키만 남고, 재시도해도 영원히
        // 같은 자리에서 실패한다
        if (file.isFile.not()) {
            return Result.failure(IllegalStateException("업로드할 파일이 없다 - $filePath").toAppError())
        }

        // 축소본이 원본보다 메모리를 덜 쓰므로 실패한 자리에서 원본으로 되돌리는 것은 더 큰
        // 메모리를 요구하는 선택이다. 폴백하지 않는다
        val prepared = uploadImagePreprocessor
            .prepare(file = file, imageType = imageType)
            .getOrElse { return Result.failure(it.toAppError()) }

        return try {
            // 발급 요청과 PUT 헤더가 같은 값을 써야 한다 — 둘 다 S3 서명 대상이고 어긋난 실패는
            // 서버 로그에 남지 않는다. 그래서 전처리가 정한 하나를 양쪽에 넘긴다
            val contentType = prepared.format.contentType

            val issued = imageRemoteDataSource
                .issueUploadUrl(fileName = prepared.file.name, contentType = contentType, imageType = imageType)
                .getOrElse { return Result.failure(it.toAppError()) }

            presignedUploadDataSource
                .put(uploadUrl = issued.uploadUrl, contentType = contentType, file = prepared.file)
                .getOrElse { return Result.failure(it.toAppError()) }

            imageRemoteDataSource
                .confirmUpload(issued.imageId)
                .map { confirmed -> confirmed.imageId }
                .mapErrorToAppError()
        } finally {
            if (prepared.isTemporary) prepared.file.delete()
        }
    }
}
```

- [x] **Step 5: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.repository.image.ImageUploadRepositoryImplTest"`
Expected: PASS (기존 11건 + 신규 5건)

- [x] **Step 6: ktlint를 돌린다**

Run: `./gradlew :data:ktlintCheck`
Expected: PASS

---

## Task 3: Android 전처리 구현과 DI 바인딩

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/source/image/local/UploadImagePreprocessorImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt`

**Interfaces:**
- Consumes: Task 1의 `planUploadImage`·`UploadImagePlan`·`UploadImageSize`·`UPLOAD_JPEG_QUALITY`, Task 2의 `UploadImagePreprocessor`·`PreparedUploadImage`
- Produces: 없음(마지막 소비자다)

⚠️ **이 Task에는 자동 테스트가 없다.** `Bitmap`·`BitmapFactory`는 JVM 유닛에서 돌지 않고, 계측 소스셋과 Robolectric은 도입하지 않기로 확정했다. 검증은 Task 4의 수동 확인이다.

- [x] **Step 1: 구현을 쓴다**

`data/src/main/java/com/teamyg/parfait/data/source/image/local/UploadImagePreprocessorImpl.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import com.teamyg.parfait.data.model.exception.UnsupportedImageException
import com.teamyg.parfait.data.model.image.UPLOAD_JPEG_QUALITY
import com.teamyg.parfait.data.model.image.UploadImageFormat
import com.teamyg.parfait.data.model.image.UploadImagePlan
import com.teamyg.parfait.data.model.image.UploadImageSize
import com.teamyg.parfait.data.model.image.planUploadImage
import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.image.ImageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadImagePreprocessorImpl
@Inject
constructor() : UploadImagePreprocessor {
    override suspend fun prepare(
        file: File,
        imageType: ImageType,
    ): Result<PreparedUploadImage> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFormat = UploadImageFormat.ofExtension(file.extension)
                ?: throw UnsupportedImageException("서버가 받지 않는 확장자다 - ${file.extension}")
            val sourceSize = decodeSize(file)

            when (val plan = planUploadImage(sourceSize, imageType, sourceFormat)) {
                UploadImagePlan.Passthrough -> {
                    PreparedUploadImage(file = file, format = sourceFormat, isTemporary = false)
                }

                is UploadImagePlan.Reencode -> {
                    val reencoded = writeReencoded(file, plan)
                    sourceLogger.i {
                        "업로드 이미지를 줄였다 - ${sourceSize.width}x${sourceSize.height} ${file.length()}B " +
                            "→ ${plan.targetSize.width}x${plan.targetSize.height} ${reencoded.length()}B " +
                            "(${plan.format.contentType})"
                    }
                    PreparedUploadImage(file = reencoded, format = plan.format, isTemporary = true)
                }
            }
        }
    }

    private fun decodeSize(file: File): UploadImageSize {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw UnsupportedImageException("이미지 크기를 읽지 못했다 - ${file.name}")
        }
        return UploadImageSize(width = options.outWidth, height = options.outHeight)
    }

    /**
     * 다 쓴 판을 그때그때 놓아주는 이유: 원본 해상도 비트맵 둘이 동시에 살아 있으면 줄이려다
     * OOM 이 난다.
     */
    private fun writeReencoded(
        source: File,
        plan: UploadImagePlan.Reencode,
    ): File {
        val options = BitmapFactory.Options().apply { inSampleSize = plan.sampleSize }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, options)
            ?: throw UnsupportedImageException("이미지를 디코드하지 못했다 - ${source.name}")

        val scaled = if (decoded.width == plan.targetSize.width && decoded.height == plan.targetSize.height) {
            decoded
        } else {
            Bitmap
                .createScaledBitmap(decoded, plan.targetSize.width, plan.targetSize.height, true)
                .also { if (it !== decoded) decoded.recycle() }
        }

        // JPEG 에는 알파가 없다. 합성하지 않으면 투명한 자리가 검게 앉는다
        val encodable = if (plan.format == UploadImageFormat.JPEG && scaled.hasAlpha()) {
            flattenOnWhite(scaled).also { if (it !== scaled) scaled.recycle() }
        } else {
            scaled
        }

        val target = File(source.parentFile, "${UUID.randomUUID()}.${plan.format.extension}")
        try {
            target.outputStream().use { output ->
                // compress 는 던지지 않고 false 를 준다 — 안 보면 잘린 파일이 그대로 올라간다
                check(encodable.compress(plan.format.compressFormat, UPLOAD_JPEG_QUALITY, output)) {
                    "축소본을 굽지 못했다 - ${target.name}"
                }
            }
        } catch (throwable: Throwable) {
            target.delete()
            throw throwable
        } finally {
            encodable.recycle()
        }

        return target
    }

    private fun flattenOnWhite(source: Bitmap): Bitmap {
        val flattened = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return flattened
    }
}

/**
 * 이 매핑을 [UploadImageFormat] 안에 두지 않는 이유: 그 열거형은 JVM 유닛이 그대로 읽는데,
 * `Bitmap.CompressFormat` 은 단위 테스트용 android.jar 에서 실제 값을 보장하지 않는다.
 */
private val UploadImageFormat.compressFormat: Bitmap.CompressFormat
    get() = when (this) {
        UploadImageFormat.PNG -> Bitmap.CompressFormat.PNG
        UploadImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
    }
```

- [x] **Step 2: DI 바인딩을 더한다**

`data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt`의 `bindImageFileLocalDataSource` 바로 아래에 더한다:

```kotlin
    @Binds
    @Singleton
    fun bindUploadImagePreprocessor(
        uploadImagePreprocessorImpl: UploadImagePreprocessorImpl,
    ): UploadImagePreprocessor
```

import 두 줄을 더한다:

```kotlin
import com.teamyg.parfait.data.source.image.local.UploadImagePreprocessor
import com.teamyg.parfait.data.source.image.local.UploadImagePreprocessorImpl
```

- [x] **Step 3: 빌드하고 Hilt 그래프가 서는지 확인한다**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL — 바인딩이 빠지면 여기서 `[Dagger/MissingBinding]`이 난다

- [x] **Step 4: 유닛 테스트 전체를 돌린다**

Run: `./gradlew :data:testDebugUnitTest`
Expected: PASS

- [x] **Step 5: ktlint를 돌린다**

Run: `./gradlew :data:ktlintCheck`
Expected: PASS

---

## Task 4: 실기기 수동 검증

**Files:** 없음(코드 변경 없음)

**Interfaces:**
- Consumes: Task 3까지의 전체 동작
- Produces: 없음

이 Task는 코드를 고치지 않는다. 앞의 자동 테스트가 닿지 못하는 디코드·인코딩을 눈으로 확인하고, 결과를 사용자에게 보고한다. **실패를 발견하면 고치지 말고 보고한다** — 원인에 따라 어느 Task로 돌아갈지가 달라진다.

- [ ] **Step 1: 앱을 설치한다**

Run: `./gradlew :app:installDebug`
Expected: 설치 성공

- [ ] **Step 2: 로그 필터를 건다**

Run: `adb logcat -s ParfaitSource:I | grep "업로드 이미지를 줄였다"`

- [ ] **Step 3: 7갈래를 확인한다**

각 항목마다 로그의 전후 치수·바이트를 적어 둔다.

1. **큰 사진 누끼 업로드** — 카메라로 찍어 토핑을 만들고 배치한다. 로그의 긴 변이 1500이어야 한다.
2. **상한 이하 누끼** — 작은 이미지로 같은 흐름을 탄다. **로그가 안 찍혀야 한다**(통과 경로라 축소하지 않는다).
3. **JPEG 배경** — 갤러리에서 사진을 골라 배경으로 넣는다. 긴 변 2048 이하면 로그가 없고, 넘으면 2048로 찍힌다.
4. **PNG 스크린샷 배경** — 스크린샷을 배경으로 고른다. 크기와 무관하게 **로그가 찍히고 contentType이 `image/jpeg`여야 한다.**
5. **투명 PNG 배경** — 투명 영역이 있는 PNG를 배경으로 고른다. 투명한 자리가 **검정이 아니라 흰색**이어야 한다.
6. **업로드본 재편집** — 올린 토핑을 다시 편집해 재업로드한다. **2회차는 로그가 안 찍혀야 한다**(이미 상한 이하라 무동작).
7. **토핑 최대 확대** — 캔버스에서 토핑을 상한까지 키운다. 화질이 눈에 띄게 뭉개지지 않아야 한다.

- [ ] **Step 4: 결과를 보고한다**

7갈래의 통과 여부와 각 항목의 전후 바이트를 표로 정리해 사용자에게 보고한다. 축소 전후 바이트 비율이 이 변경의 유일한 효과 지표다.

---

## Self-Review 결과

- **스펙 커버리지**: 배치(Task 2) · 결정 표(Task 1) · 메모리(Task 3) · 임시 파일(Task 2·3) · 실패 처리(Task 2) · 로깅(Task 3) · 검증 3종(Task 1·2·4) 모두 태스크가 있다.
- **타입 일관성**: `PreparedUploadImage`의 세 필드(`file`·`format`·`isTemporary`)가 Task 2 정의와 Task 3 사용에서 같다. `planUploadImage`의 인자 순서(`sourceSize`·`imageType`·`sourceFormat`)가 Task 1 정의와 Task 3 호출에서 같다.
- **스펙과의 차이 1건**: 스펙의 인터페이스 초안은 `PreparedUploadImage(file, format)` 2필드였다. 임시 파일 정리를 저장소가 하려면 그 사실을 알아야 해서 `isTemporary`를 더했다. 스펙의 "전처리가 새 파일을 만들었으면 지운다"를 구현하는 데 필요한 정보다.
