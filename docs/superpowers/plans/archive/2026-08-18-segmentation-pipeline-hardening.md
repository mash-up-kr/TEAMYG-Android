---
id: segmentation-pipeline-hardening
title: 카메라·갤러리 → 세그멘테이션 파이프라인 보강 + YGScaffoldV2 이관 구현 계획
status: done
type: work-order
created: 2026-08-18
updated: 2026-08-20
platforms: android
owner:
related_adr: ADR-0011, ADR-0012, ADR-0020
related_spec: segmentation-pipeline-hardening
related_code: Navigator.kt#popUpTo, SegmentationMask.kt#maskSubjectPixels, SegmentationCacheDir.kt#clearFiles, ClearSegmentationCacheUseCase.kt#ClearSegmentationCacheUseCase, ImageSegmentationRepositoryImpl.kt#segmentImage, SegmentationViewModel.kt#SegmentationViewModel, PictureConfirmRoute.kt#PictureConfirmRoute, CanvasToppingPlaceRoute.kt#CanvasToppingPlaceRoute, DecodeImageUseCase.kt#DecodeImageUseCase
archived_reason: 실행 완료 후 PR #309로 develop 머지됨(2026-08-20, `cf357937`)
tags: [plan, parfait]
---

# 카메라·갤러리 → 세그멘테이션 파이프라인 보강 + YGScaffoldV2 이관 구현 계획

> ✅ **완료·develop 머지(PR #309 `refactor/segmentation-develop` → `cf357937`, 2026-08-20)** —
> 리베이스 두 번을 거친 head `63ec2989`가 충돌 해소 편집 없이 그대로 들어갔다. 체크박스는 실행 기록을
> 이 블록과 스펙 as-built에 모으는 관례대로 미체크로 둔다. 실행 결과·뒤집힌 결정·수치의 정본은
> [스펙 as-built 재정정 절](../../specs/archive/2026-08-18-segmentation-pipeline-hardening.md#as-built-재정정-2026-08-20-두-번째-리베이스)이다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 카메라·갤러리에서 받은 사진이 세그멘테이션까지 가는 경로의 크래시 셋과 미결 셋을 닫고, `camera`·`gallery`·`segmentation` 세 모듈을 `YGScaffoldV2`로 이관한다.

**Architecture:** 아래에서 위로 올라간다. 먼저 `core:navigation`에 타입 기준 pop API를, `data`에 세그멘테이션 순수 로직과 캐시 정리를 넣고 JVM 유닛으로 잠근다. 그 위에서 `SegmentationViewModel`을 고치고, 죽은 결과 경로를 걷어낸 뒤, 모듈별로 스캐폴드를 EntryBuilder에서 Route로 내린다. 마지막이 문서 갱신이다.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation3, Hilt, ML Kit Subject Segmentation, JUnit4 + mockk + kotlinx-coroutines-test

**Spec:** `parfait/specs/archive/2026-08-18-segmentation-pipeline-hardening.md` (이 저장소 기준 경로. 코드 작업 대상은 별도 저장소다)

## Global Constraints

- **작업 저장소는 `TJYG-Android`다.** 이 계획 문서가 사는 위키 저장소가 아니다. 절대경로는 `wiki/personal-private/project-paths.md`에 있다.
- ⚠️ **이 계획은 실행이 끝났고 결과가 develop에 들어갔다(PR #309, 2026-08-20, develop `cf357937`).
  아래 브랜치 전제는 두 번의 리베이스와 그 머지로 낡았다 — 다시 실행하지 마라.** 실행 브랜치는
  **`refactor/segmentation-develop`**(develop `750cc2dd` 기준, head `63ec2989`, 커밋 15개)였고
  그 팁이 충돌 해소 편집 없이 그대로 머지됐다. 무엇이 어떻게 갈렸는지는
  [스펙의 as-built 재정정 절](../../specs/archive/2026-08-18-segmentation-pipeline-hardening.md#as-built-재정정-2026-08-20-두-번째-리베이스)이 정본이다.
  - ~~**작업 브랜치는 `refactor/segmentation-logic`이다.** 이미 `develop` + `origin/feature/topping-add-screen`(PR #290) 머지 상태로 준비돼 있다. 새 브랜치를 만들지 마라 — #290이 고친 자리를 이어서 고치는 것이 이 라운드의 전제다.~~ (#290은 2026-08-19에 develop으로 머지됐고, 이 계획이 얹혀 있던 로컬 머지 브랜치는 폐기됐다)
- **`git commit`은 확인 없이 해도 된다. `git push`·`gh pr create`·`gh pr merge`는 사용자 확인 없이 실행하지 마라.**
- 모든 태스크 끝에서 `./gradlew ktlintCheck`가 통과해야 한다. CI가 이 명령 그대로 돈다.
- 주석은 한국어로 쓰고 **"무엇"이 아니라 "왜"를 적는다.** 코드가 이미 말하는 것을 되풀이하지 마라.
- 새 사용자 노출 문자열은 각 모듈 `src/main/res/values/strings.xml`에 넣는다. 하드코딩 금지.
- 마스크 임계값은 `0.5f` **초과**다(`>`, `>=` 아님). 현행 동작이고 바꾸지 않는다.

---

### Task 1: Navigator 타입 기준 pop

**Files:**
- Modify: `core/navigation/src/main/java/com/teamyg/parfait/core/navigation/Navigator.kt`
- Test: `core/navigation/src/test/java/com/teamyg/parfait/core/navigation/NavigatorTest.kt`

**Interfaces:**
- Consumes: 없음 (첫 태스크)
- Produces: `Navigator.popUpTo(type: KClass<out NavKey>): Boolean`, `inline fun <reified T : NavKey> Navigator.popUpTo(): Boolean`. Task 6·8이 `navigator.popUpTo<NavKeyCanvasMain>()` 형태로 쓴다.

기존 `goToSingleClearTop`은 키 **동등성** 비교라 `NavKeyCanvasMain(groupId)`의 groupId를 알아야 한다. 카메라·세그멘테이션 NavKey들은 groupId를 안 들고 다니므로 그대로는 못 쓴다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`NavigatorTest.kt`의 기존 `import` 블록에 `kotlin.test.assertFalse`와 `kotlin.test.assertTrue`를 더하고, 클래스 안 맨 끝에 세 케이스를 붙인다. 파일 위쪽의 `private data object Home / Detail / Login`은 이미 있으니 새로 만들지 마라.

```kotlin
    @Test
    fun popUpTo_targetIsBelowSeveralEntries_removesEverythingAboveIt() {
        // Given 목적지 위로 두 화면이 쌓인 백스택
        val navigator = navigator()
        navigator.goTo(Detail)
        navigator.goTo(Login)

        // When 목적지 타입까지 걷어낸다
        val reached = navigator.popUpTo<Home>()

        // Then 목적지만 남고 도달했다고 알린다
        assertTrue(reached)
        assertEquals(listOf(Home), navigator.backStack)
    }

    @Test
    fun popUpTo_targetIsAlreadyOnTop_leavesTheStackAlone() {
        // Given 목적지가 이미 최상단인 백스택
        val navigator = navigator()

        // When 같은 타입까지 걷어낸다
        val reached = navigator.popUpTo<Home>()

        // Then 걷어낼 것이 없으므로 그대로다. 도달 여부는 참이다 —
        // 호출부가 보기에 "그 화면에 있게 됐다"는 결과가 같다
        assertTrue(reached)
        assertEquals(listOf(Home), navigator.backStack)
    }

    @Test
    fun popUpTo_targetIsNotInTheStack_changesNothing() {
        // Given 목적지 타입이 없는 백스택
        val navigator = navigator()
        navigator.goTo(Detail)

        // When 없는 타입까지 걷어내려 한다
        val reached = navigator.popUpTo<Login>()

        // Then 백스택을 건드리지 않고 실패를 알린다 — 못 찾았는데 비우면
        // 사용자가 어디에도 없는 화면에 남는다
        assertFalse(reached)
        assertEquals(listOf(Home, Detail), navigator.backStack)
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :core:navigation:testDebugUnitTest --tests "*NavigatorTest*"
```

Expected: 컴파일 실패 — `Unresolved reference: popUpTo`

- [ ] **Step 3: 구현한다**

`Navigator.kt`에 `import kotlin.reflect.KClass`를 더하고, `goToAndPopCurrent` 아래에 붙인다.

```kotlin
    /**
     * [T] 타입 키가 백스택에 있으면 그 위에 쌓인 것을 모두 걷어낸다.
     *
     * [goToSingleClearTop] 과 달리 **키 값이 아니라 타입**으로 찾는다. 목적지 키가 인자를 갖는
     * 경우(예: 그룹 id) 걷어내려는 쪽이 그 인자를 모를 수 있어서다 — 촬영·누끼 화면들은 어느
     * 그룹에서 시작했는지를 들고 다니지 않는다.
     *
     * @return 그 타입에 도달했으면 `true`. 백스택에 없으면 **아무것도 걷어내지 않고** `false` —
     *   못 찾았는데 비우면 사용자가 어느 화면에도 없는 상태로 남는다
     */
    inline fun <reified T : NavKey> popUpTo(): Boolean = popUpTo(T::class)

    /** 타입을 값으로 받는 [popUpTo]. reified 판이 이쪽으로 넘긴다 */
    fun popUpTo(type: KClass<out NavKey>): Boolean {
        val destinationIndex = _backStack.indexOfLast { it::class == type }

        if (destinationIndex == -1) return false
        if (destinationIndex == _backStack.lastIndex) return true

        // 하나씩 걷어내면 스냅샷에도 그만큼 변경이 쌓이므로 한 번에 잘라낸다
        _backStack.removeRange(destinationIndex + 1, _backStack.size)

        return true
    }
```

`_backStack`이 private이라 inline 판에서 직접 못 읽는다. 그래서 reified 판은 public인 `KClass` 판으로만 넘긴다.

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
./gradlew :core:navigation:testDebugUnitTest --tests "*NavigatorTest*" && ./gradlew ktlintCheck
```

Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add core/navigation/src/main/java/com/teamyg/parfait/core/navigation/Navigator.kt core/navigation/src/test/java/com/teamyg/parfait/core/navigation/NavigatorTest.kt
git commit -m "feat(navigation): pop back to a destination found by type"
```

---

### Task 2: 세그멘테이션 마스크 순수 함수 + segmentImage 재작성

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationMaskTest.kt`

**Interfaces:**
- Consumes: `SegmentationBounds`(domain, 기존)
- Produces: `internal fun maskSubjectPixels(pixels: IntArray, mask: FloatBuffer, width: Int, height: Int): SegmentationBounds?`, `internal const val SUBJECT_CONFIDENCE_THRESHOLD`

현행 `segmentImage`는 마스크가 참인 픽셀마다 `Bitmap.getPixel`을 부른다. 픽셀당 JNI 왕복이고, 결과용 `IntArray`를 따로 만들어 큰 사진에서 같은 크기 배열을 둘 들고 있게 된다. 마스크가 `null`일 때는 `error()`로 raw throw하는데, 그 자리가 `try` 밖 `withContext` 안이라 예외 매핑도 안 탄다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationMaskTest.kt`

```kotlin
package com.teamyg.parfait.data.repository.image

import java.nio.FloatBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val OPAQUE_WHITE = 0xFFFFFFFF.toInt()
private const val TRANSPARENT = 0

class SegmentationMaskTest {
    /** 3×3 이미지 전 픽셀을 불투명 흰색으로 채운다 */
    private fun pixels() = IntArray(9) { OPAQUE_WHITE }

    private fun mask(vararg confidences: Float): FloatBuffer = FloatBuffer.wrap(confidences)

    @Test
    fun maskSubjectPixels_oneCenterPixelIsSubject_boundsCoverThatPixelOnly() {
        // Given 가운데 한 칸만 객체인 3×3 마스크
        val pixels = pixels()
        val mask = mask(
            0f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 0f,
        )

        // When 객체가 아닌 자리를 지운다
        val bounds = maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 그 한 칸만 감싸는 영역이 나온다. right·bottom 은 마지막 픽셀을 포함하는 exclusive 값이다
        assertEquals(1, bounds?.left)
        assertEquals(1, bounds?.top)
        assertEquals(2, bounds?.right)
        assertEquals(2, bounds?.bottom)
    }

    @Test
    fun maskSubjectPixels_oneCenterPixelIsSubject_erasesEverythingElse() {
        // Given 가운데 한 칸만 객체인 3×3 마스크
        val pixels = pixels()
        val mask = mask(
            0f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 0f,
        )

        // When 객체가 아닌 자리를 지운다
        maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 객체 픽셀만 원본 색으로 남는다
        assertEquals(OPAQUE_WHITE, pixels[4])
        assertTrue(pixels.filterIndexed { index, _ -> index != 4 }.all { it == TRANSPARENT })
    }

    @Test
    fun maskSubjectPixels_nothingDetected_returnsNull() {
        // Given 아무 데도 객체가 없는 마스크
        val pixels = pixels()
        val mask = mask(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

        // When 지운다
        val bounds = maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 감쌀 것이 없으므로 영역도 없다
        assertNull(bounds)
        assertTrue(pixels.all { it == TRANSPARENT })
    }

    @Test
    fun maskSubjectPixels_confidenceIsExactlyTheThreshold_isNotSubject() {
        // Given 정확히 임계값인 칸 하나뿐인 마스크
        val pixels = pixels()
        val mask = mask(0f, 0f, 0f, 0f, SUBJECT_CONFIDENCE_THRESHOLD, 0f, 0f, 0f, 0f)

        // When 지운다
        val bounds = maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 임계값 자체는 객체가 아니다 — 판정이 초과이지 이상이 아니다
        assertNull(bounds)
    }

    @Test
    fun maskSubjectPixels_everyPixelIsSubject_keepsAllPixelsAndCoversTheWholeImage() {
        // Given 전부 객체인 마스크
        val pixels = pixels()
        val mask = mask(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)

        // When 지운다
        val bounds = maskSubjectPixels(pixels, mask, width = 3, height = 3)

        // Then 이미지 전체가 영역이고 지워진 픽셀이 없다
        assertEquals(0, bounds?.left)
        assertEquals(0, bounds?.top)
        assertEquals(3, bounds?.right)
        assertEquals(3, bounds?.bottom)
        assertTrue(pixels.all { it == OPAQUE_WHITE })
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*SegmentationMaskTest*"
```

Expected: 컴파일 실패 — `Unresolved reference: maskSubjectPixels`

- [ ] **Step 3: 순수 함수를 만든다**

`data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt`

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import java.nio.FloatBuffer

/** 이 값을 **넘는** 신뢰도만 객체로 본다 */
internal const val SUBJECT_CONFIDENCE_THRESHOLD = 0.5f

private const val TRANSPARENT = 0

/**
 * [pixels] 에서 객체가 아닌 자리를 투명으로 지우고, 남은 자리를 감싸는 사각 영역을 돌려준다.
 *
 * 넘겨받은 배열을 **그 자리에서** 고친다. 결과용 배열을 따로 만들면 큰 사진에서 같은 크기 배열을
 * 둘 들고 있게 되는데, 이 함수가 도는 동안엔 원본 비트맵도 아직 살아 있다.
 *
 * `Bitmap` 을 받지 않는 것은 이 판단(임계·경계 계산)을 기기 없이 검증하기 위해서다.
 *
 * @param mask 픽셀별 전경 신뢰도. 길이가 `width * height` 여야 한다 — 호출부가 검사한다
 * @return 객체 픽셀이 하나도 없으면 `null`
 */
internal fun maskSubjectPixels(
    pixels: IntArray,
    mask: FloatBuffer,
    width: Int,
    height: Int,
): SegmentationBounds? {
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = -1
    var bottom = -1

    for (index in 0 until width * height) {
        if (mask[index] > SUBJECT_CONFIDENCE_THRESHOLD) {
            val x = index % width
            val y = index / width

            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        } else {
            pixels[index] = TRANSPARENT
        }
    }

    if (left > right || top > bottom) return null

    // right·bottom 은 마지막 픽셀을 포함하도록 exclusive 로 담는다
    return SegmentationBounds(left = left, top = top, right = right + 1, bottom = bottom + 1)
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*SegmentationMaskTest*"
```

Expected: PASS (5건)

- [ ] **Step 5: `segmentImage`가 그 함수를 쓰도록 고친다**

`ImageSegmentationRepositoryImpl.kt`에서 `return withContext(Dispatchers.Default) { … }` 블록을 통째로 아래로 바꾼다. `image.width`/`image.height` 대신 비트맵 치수를 쓴다 — 픽셀 배열의 좌표계가 비트맵의 것이다.

```kotlin
        return withContext(Dispatchers.Default) {
            // 마스크가 없으면 잘라낼 근거가 없다. 여기는 위 try 밖이라 예외를 던져 봐야
            // toSegmentationException 매핑을 타지 못하므로 실패 값으로 돌려준다
            val foregroundMask = result.foregroundConfidenceMask
                ?: return@withContext Result.failure(SegmentationException.Process(null))

            val width = bitmap.width
            val height = bitmap.height

            // InputImage.fromBitmap(bitmap, 0) 이라 지금은 치수가 같지만 그 일치가 계약으로
            // 적혀 있지 않다. 어긋난 채로 읽으면 엉뚱한 자리를 객체로 오려낸다
            if (foregroundMask.capacity() != width * height) {
                return@withContext Result.failure(SegmentationException.Process(null))
            }

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val subjectBounds = maskSubjectPixels(pixels, foregroundMask, width, height)

            val subjectBitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

            val file = subjectBitmap.saveToCacheAsPng()

            // 미리보기·배치는 투명 여백 없이 실제 객체 크기만 필요하므로, 이미 알고 있는 bounding box 로 바로 잘라 둔다
            val trimmedFile = subjectBounds?.let { bounds ->
                val trimmedBitmap = Bitmap.createBitmap(
                    subjectBitmap,
                    bounds.left,
                    bounds.top,
                    bounds.width,
                    bounds.height,
                )
                val saved = trimmedBitmap.saveToCacheAsPng()
                if (trimmedBitmap !== subjectBitmap) trimmedBitmap.recycle()
                saved
            }
            subjectBitmap.recycle()

            val segmentationResult = SegmentationResult(
                subjectImagePath = file.absolutePath,
                trimmedSubjectImagePath = (trimmedFile ?: file).absolutePath,
                subjectBounds = subjectBounds,
            )

            return@withContext Result.success(segmentationResult)
        }
```

바깥 `val result`(ML Kit 결과)와 이름이 겹치던 안쪽 `val result`를 `segmentationResult`로 바꾼 것도 포함이다.

이제 안 쓰는 `import com.teamyg.parfait.domain.model.SegmentationBounds`를 지운다 — bounds 생성이 `SegmentationMask.kt`로 옮겨 갔다.

- [ ] **Step 6: 컴파일·정적검사를 확인한다**

```bash
./gradlew :data:compileDebugKotlin ktlintCheck
```

Expected: 통과. 경고에 unused import가 남아 있지 않아야 한다.

- [ ] **Step 7: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationMaskTest.kt
git commit -m "refactor(segmentation): read the source pixels once instead of per pixel

The mask loop called Bitmap.getPixel for every subject pixel, one JNI
round trip each, and allocated a second array the size of the image.
Read the pixels once and erase the non-subject ones in place.

A null confidence mask used to escape as a raw throw from outside the
try block, so it never reached the exception mapping. Return it as a
failure instead, and reject a mask whose size disagrees with the bitmap
rather than reading the wrong offsets."
```

---

### Task 3: 캐시 PNG 전용 디렉토리 + 정리 경로

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationCacheDir.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/ClearSegmentationCacheUseCase.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCacheDirTest.kt`

**Interfaces:**
- Consumes: Task 2가 만든 `SegmentationMask.kt`와 같은 패키지에 놓는다
- Produces: `ImageSegmentationRepository.clearSegmentationCache()`, `ClearSegmentationCacheUseCase.invoke()`, `internal const val SEGMENTATION_CACHE_DIR_NAME`, `internal fun File.clearFiles()`. Task 4가 유스케이스를 쓴다.

지금은 `cacheDir` 바로 밑에 타임스탬프 이름으로 떨구고 지우는 곳이 없다. 세그멘테이션 1회에 2장, 편집을 마칠 때마다 2장이 더 는다. 같은 밀리초에 두 번 저장하면 이름까지 겹친다 — PR #290이 subject·trimmed를 연달아 저장하면서 실제로 가능해졌다.

`cacheDir` 하위를 쓰는 다른 소비자(`FileCameraCacheLocalDataSourceImpl`)는 자기 서브디렉토리를 쓰고, 최근 이미지는 `filesDir`에 있다. 세그멘테이션 전용 디렉토리만 비우면 남의 것을 건드리지 않는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCacheDirTest.kt`

```kotlin
package com.teamyg.parfait.data.repository.image

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SegmentationCacheDirTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun clearFiles_directoryHasFiles_removesThemAndKeepsTheDirectory() {
        // Given 파일 셋이 든 디렉토리
        val directory = temporaryFolder.newFolder("segmentation")
        repeat(3) { index -> File(directory, "parfait_$index.png").writeText("x") }

        // When 비운다
        directory.clearFiles()

        // Then 파일만 사라지고 디렉토리는 남는다 — 다음 저장이 mkdirs 없이도 쓸 수 있어야 한다
        assertTrue(directory.exists())
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun clearFiles_directoryDoesNotExist_doesNothing() {
        // Given 아직 만들어진 적 없는 디렉토리
        val directory = File(temporaryFolder.root, "segmentation")

        // When 비운다 — 첫 진입에서 실제로 일어나는 상황이다
        directory.clearFiles()

        // Then 예외 없이 지나가고 디렉토리를 만들지도 않는다
        assertTrue(directory.exists().not())
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*SegmentationCacheDirTest*"
```

Expected: 컴파일 실패 — `Unresolved reference: clearFiles`

- [ ] **Step 3: 헬퍼를 만든다**

`data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationCacheDir.kt`

```kotlin
package com.teamyg.parfait.data.repository.image

import java.io.File

/** `cacheDir` 하위의 세그멘테이션 전용 디렉토리 이름. 다른 캐시 소비자와 섞이지 않게 가른다 */
internal const val SEGMENTATION_CACHE_DIR_NAME = "segmentation"

/**
 * 디렉토리 안의 파일을 지운다. **디렉토리 자체는 남긴다.**
 *
 * 없는 디렉토리를 비우는 것은 오류가 아니다 — 앱 설치 후 첫 진입이 그 상태다.
 */
internal fun File.clearFiles() {
    listFiles()?.forEach { it.delete() }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*SegmentationCacheDirTest*"
```

Expected: PASS (2건)

- [ ] **Step 5: 리포지토리 계약에 정리 경로를 더한다**

`domain/.../repository/image/ImageSegmentationRepository.kt`에서 `decodeImage` 선언을 KDoc과 함께 바꾸고, 파일 끝에 `clearSegmentationCache`를 더한다.

```kotlin
    /**
     * [uri] 가 가리키는 이미지를 비트맵으로 읽는다.
     *
     * **실패하면 던진다.** URI 가 만료됐거나 파일이 깨졌으면 디코더의 예외가 그대로 올라오므로
     * 호출부가 감싸야 한다.
     */
    suspend fun decodeImage(uri: String): BitmapWrapper
```

```kotlin
    /**
     * 세그멘테이션이 만든 캐시 파일을 전부 지운다.
     *
     * **새 흐름이 시작될 때 부른다.** 이전 흐름이 남긴 파일은 그 시점에 아무도 보지 않는다 —
     * 캔버스로 돌아와야만 새 흐름을 시작할 수 있고, 돌아오는 길에 그 화면들이 이미 걷힌다.
     */
    suspend fun clearSegmentationCache()
```

- [ ] **Step 6: 구현을 옮긴다**

`ImageSegmentationRepositoryImpl.kt`에서 기존 `saveToCacheAsPng`를 아래로 바꾸고 `clearSegmentationCache`를 더한다.

```kotlin
    override suspend fun clearSegmentationCache() {
        withContext(Dispatchers.IO) { segmentationCacheDir.clearFiles() }
    }

    private val segmentationCacheDir: File
        get() = File(context.cacheDir, SEGMENTATION_CACHE_DIR_NAME)

    /**
     * 밀리초 이름 대신 [File.createTempFile] 을 쓰는 이유: 한 번의 세그멘테이션이 subject 와
     * trimmed 를 연달아 저장해서 같은 밀리초에 두 번 떨어질 수 있다. 그러면 뒤엣것이 앞엣것을 덮는다.
     */
    private suspend fun Bitmap.saveToCacheAsPng(): File = withContext(Dispatchers.IO) {
        val directory = segmentationCacheDir.also { it.mkdirs() }
        val file = File.createTempFile("parfait_", ".png", directory)

        file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }

        file
    }
```

- [ ] **Step 7: 유스케이스를 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/usecase/image/ClearSegmentationCacheUseCase.kt`

```kotlin
package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

class ClearSegmentationCacheUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "ClearSegmentationCacheUseCase::init" }
    }

    suspend operator fun invoke() = repository.clearSegmentationCache()
}
```

- [ ] **Step 8: 전체 유닛 테스트와 정적검사를 돌린다**

```bash
./gradlew test ktlintCheck
```

Expected: 전부 PASS

- [ ] **Step 9: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/repository/image/ domain/src/main/java/com/teamyg/parfait/domain/ data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCacheDirTest.kt
git commit -m "feat(segmentation): give the cached cutouts a home that gets emptied

Every run dropped PNGs straight into cacheDir and nothing ever deleted
them. Move them into a segmentation-only directory and empty it when a
new flow starts, which bounds the leftovers at one flow's worth.

Two saves in the same millisecond used to collide on the file name.
createTempFile picks a unique one."
```

---

### Task 4: SegmentationViewModel — 캐시 정리 선행, 디코드 실패 흡수

**Files:**
- Modify: `feature/segmentation/impl/build.gradle.kts`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: Task 3의 `ClearSegmentationCacheUseCase`
- Produces: `SegmentationViewModel(sourceImageUri, clearSegmentationCacheUseCase, decodeImageUseCase, segmentImageUseCase)` — 생성자 파라미터가 하나 는다

`SegmentationViewModel`은 `DecodeImageUseCase`를 맨몸으로 부른다. URI 가 만료됐거나 파일이 깨지면 그 예외가 `init` 코루틴에서 그대로 터진다. 같은 유스케이스를 쓰는 `ToppingEditViewModel`은 이미 `runCatching` 으로 감싸 실패로 접는다 — 같은 관용구를 쓴다.

이 모듈에는 테스트가 하나도 없어서 유닛 테스트 플러그인부터 붙여야 한다.

- [ ] **Step 1: 유닛 테스트 플러그인을 붙인다**

`feature/segmentation/impl/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}
```

나머지 블록은 그대로 둔다.

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

```kotlin
package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationResult
import com.teamyg.parfait.domain.usecase.image.ClearSegmentationCacheUseCase
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SegmentImageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SOURCE_URI = "content://media/external/images/1"
private const val SUBJECT_PATH = "/cache/segmentation/subject.png"
private const val TRIMMED_PATH = "/cache/segmentation/trimmed.png"

class SegmentationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clearSegmentationCache: ClearSegmentationCacheUseCase = mockk(relaxed = true)
    private val decodeImage: DecodeImageUseCase = mockk()
    private val segmentImage: SegmentImageUseCase = mockk()

    private val bitmapWrapper: BitmapWrapper = mockk(relaxed = true)

    private val success = SegmentationResult(
        subjectImagePath = SUBJECT_PATH,
        trimmedSubjectImagePath = TRIMMED_PATH,
        subjectBounds = SegmentationBounds(left = 0, top = 0, right = 10, bottom = 10),
    )

    @Before
    fun stubTheHappyPath() {
        coEvery { decodeImage(SOURCE_URI) } returns bitmapWrapper
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(success)
    }

    private fun viewModel() = SegmentationViewModel(
        sourceImageUri = SOURCE_URI,
        clearSegmentationCacheUseCase = clearSegmentationCache,
        decodeImageUseCase = decodeImage,
        segmentImageUseCase = segmentImage,
    )

    @Test
    fun init_segmentationSucceeds_publishesBothImagePaths() = runTest {
        // Given 정상 응답을 주는 유스케이스들
        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 두 경로가 모두 상태에 실린다 — 배치 화면이 trimmed 를, 재편집이 원본 크기를 쓴다
        val state = viewModel.state.value
        assertEquals(SUBJECT_PATH, state.subjectImagePath)
        assertEquals(TRIMMED_PATH, state.trimmedSubjectImagePath)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
    }

    @Test
    fun init_always_clearsTheCacheBeforeDecoding() = runTest {
        // Given 정상 응답
        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 정리가 디코드보다 먼저다 — 뒤에 두면 이번 흐름이 방금 만든 파일을 지운다
        coVerifyOrder {
            clearSegmentationCache()
            decodeImage(SOURCE_URI)
        }
    }

    @Test
    fun init_decodeThrows_endsInErrorWithoutSegmenting() = runTest {
        // Given URI 가 만료돼 디코드가 던지는 상황
        coEvery { decodeImage(SOURCE_URI) } throws IllegalStateException("broken uri")

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 크래시 대신 에러 화면으로 접히고 세그멘테이션은 시도하지 않는다
        val state = viewModel.state.value
        assertTrue(state.isError)
        assertFalse(state.isLoading)
        coVerify(exactly = 0) { segmentImage(any()) }
    }

    @Test
    fun init_segmentationFails_endsInError() = runTest {
        // Given 세그멘테이션이 실패를 돌려주는 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 에러 화면이고 로딩에 갇히지 않는다
        val state = viewModel.state.value
        assertTrue(state.isError)
        assertFalse(state.isLoading)
    }

    @Test
    fun init_noSubjectDetected_endsInError() = runTest {
        // Given 성공했지만 감지된 객체가 없는 응답
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(success.copy(subjectBounds = null))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 에러다 — 하이라이트도 다음 화면으로 갈 방법도 없는 화면만 남기지 않는다
        assertTrue(viewModel.state.value.isError)
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

```bash
./gradlew :feature:segmentation:impl:testDebugUnitTest
```

Expected: 컴파일 실패 — `SegmentationViewModel` 생성자에 `clearSegmentationCacheUseCase` 가 없다

- [ ] **Step 4: 구현한다**

`SegmentationViewModel.kt`의 `import`에 `com.teamyg.parfait.domain.usecase.image.ClearSegmentationCacheUseCase`를 더하고, 생성자와 `init`을 바꾼다.

```kotlin
@HiltViewModel(assistedFactory = SegmentationViewModel.Factory::class)
class SegmentationViewModel
@AssistedInject constructor(
    @Assisted private val sourceImageUri: String,
    private val clearSegmentationCacheUseCase: ClearSegmentationCacheUseCase,
    private val decodeImageUseCase: DecodeImageUseCase,
    private val segmentImageUseCase: SegmentImageUseCase,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    init {
        viewModelScope.launch {
            // 이번 흐름이 파일을 만들기 전에 지운다 — 뒤에 두면 방금 만든 것을 지운다
            clearSegmentationCacheUseCase()

            // URI 가 만료됐거나 파일이 깨지면 디코더가 던진다. 잡지 않으면 이 코루틴이 그대로 터진다
            val bitmapWrapper = runCatching { decodeImageUseCase(sourceImageUri) }.getOrNull()

            if (bitmapWrapper == null) {
                updateState { copy(isLoading = false, isError = true) }
                return@launch
            }

            val originBitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            updateState { copy(originBitmap = originBitmap) }

            segmentImageUseCase(bitmapWrapper)
                .onSuccess { result ->
                    val subjectBounds = result.subjectBounds

                    // bounds 가 없으면 하이라이트도 다음 화면으로 갈 방법도 없는 화면만 남는다
                    if (subjectBounds == null) {
                        updateState { copy(isError = true) }
                        return@onSuccess
                    }

                    updateState {
                        copy(
                            subjectImagePath = result.subjectImagePath,
                            trimmedSubjectImagePath = result.trimmedSubjectImagePath,
                            subjectBounds = subjectBounds,
                        )
                    }
                }.onFailure { updateState { copy(isError = true) } }

            // 실패해도 로딩 화면에 갇히지 않도록 성공/실패와 무관하게 해제한다
            updateState { copy(isLoading = false) }
        }
    }
```

`Factory`와 `processIntent`는 그대로 둔다.

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
./gradlew :feature:segmentation:impl:testDebugUnitTest ktlintCheck
```

Expected: PASS (5건)

- [ ] **Step 6: 커밋한다**

```bash
git add feature/segmentation/impl/build.gradle.kts feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt feature/segmentation/impl/src/test/
git commit -m "fix(segmentation): survive an image that will not decode

A stale or broken URI made the decoder throw inside the init coroutine
and took the screen down with it. Fold that into the error state, the
way the topping editor already does, and empty the cutout cache before
this flow writes anything into it."
```

---

### Task 5: 죽은 결과 경로 제거

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/viewmodel/CustomCameraViewModel.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/CustomCameraRoute.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `CustomCameraEffect.Cancel`(data object). `CanvasMainIntent.CacheImage`·`CanvasMainEffect.NavigateToSegmentation`이 **사라진다** — Task 6이 그 사실에 의존한다.

`CanvasMainRoute`는 `ResultEffect<String>`으로 결과를 받아 `CacheImage`를 쏜다. 그런데 사진을 확정하면 `PictureConfirmRoute`가 세그멘테이션으로 직행해 그 uri 가 캔버스에 영영 오지 않고, 카메라를 취소하면 `ReturnResult`가 `null`을 결과 버스에 흘린다. 결과 키가 타입 이름이라 nullable 여부로 갈리지 않아서, 캔버스로 돌아오는 순간 그 `null`이 `CacheImage`로 들어간다. 기능은 죽고 크래시만 살아 있는 통로다.

`AddRecentImageUseCase`·`RecentImageRepository`는 **지우지 마라.** 최근 이미지 저장은 업로드가 확정되는 C-106에서, 테두리를 두르기 전 알맹이로 이뤄진다. 그 라운드가 쓸 물건이다.

- [ ] **Step 1: 테스트에서 사라질 동작을 먼저 걷어낸다**

`CanvasMainViewModelTest.kt`에서 아래를 지운다.

- `import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase`
- `private val addRecentImage: AddRecentImageUseCase = mockk()` 필드
- `viewModel()` 팩토리의 `addRecentImageUseCase = addRecentImage,` 인자
- `CanvasMainIntent.CacheImage` 또는 `CanvasMainEffect.NavigateToSegmentation`을 참조하는 테스트 함수 전부

지운 뒤 파일에 `addRecentImage`·`CacheImage`·`NavigateToSegmentation`이 하나도 남지 않아야 한다. 확인:

```bash
grep -n "addRecentImage\|CacheImage\|NavigateToSegmentation" feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt
```

Expected: 출력 없음

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest
```

Expected: 컴파일 실패 — `CanvasMainViewModel` 생성자가 아직 `addRecentImageUseCase`를 요구한다

- [ ] **Step 3: 캔버스에서 죽은 경로를 걷어낸다**

`CanvasMainViewModel.kt`에서 지운다.

- `import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase`
- 생성자 파라미터 `private val addRecentImageUseCase: AddRecentImageUseCase,`
- `CanvasMainEffect`의 `NavigateToSegmentation` 항목
- `CanvasMainIntent`의 `CacheImage` 항목
- `processIntent`의 `is CanvasMainIntent.CacheImage -> handleCacheImage(intent)` 분기
- `handleCacheImage` 함수 전체

`CanvasMainRoute.kt`에서 지운다.

- `import androidx.navigation3.runtime.result.ResultEffect`
- `import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation`
- `ResultEffect<String> { imageUri -> viewModel.processIntent(CanvasMainIntent.CacheImage(imageUri)) }` 블록
- `effect.collect` 의 `is CanvasMainEffect.NavigateToSegmentation -> …` 분기

- [ ] **Step 4: 카메라 취소 이펙트를 좁힌다**

`CustomCameraViewModel.kt`에서 `ReturnResult`를 `Cancel`로 바꾼다.

```kotlin
    /**
     * 촬영을 접고 부른 쪽으로 돌아간다.
     *
     * 결과를 실어 보내지 않는다 — 취소는 값이 없는 사건이고, 예전처럼 `null` 을 결과 버스에
     * 흘리면 그것을 결과로 아는 화면이 받아 터진다.
     */
    data object Cancel : CustomCameraEffect
```

`handleOnCaptureFailed`·`handleOnCancel` 두 곳의 `postSideEffect(CustomCameraEffect.ReturnResult(uri = null))`를 `postSideEffect(CustomCameraEffect.Cancel)`로 바꾼다.

`CustomCameraRoute.kt`의 `effect.collect` 분기를 바꾼다.

```kotlin
                is CustomCameraEffect.Cancel -> navigator.onBack()
```

`resultEventBus`가 이 파일에서 더 이상 쓰이지 않으면 `val resultEventBus = LocalResultEventBus.current` 줄과 `import androidx.navigation3.runtime.result.LocalResultEventBus`도 지운다. 지우기 전에 확인:

```bash
grep -n "resultEventBus" feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/CustomCameraRoute.kt
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
./gradlew test ktlintCheck
```

Expected: 전부 PASS. `AddRecentImageUseCase`는 호출부가 0건이 되지만 Hilt 바인딩이라 컴파일은 통과한다.

- [ ] **Step 6: 앱이 빌드되는지 확인한다**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋한다**

```bash
git add feature/groups/canvas/impl/src feature/camera/impl/src
git commit -m "fix(canvas): stop routing a cancelled capture into the canvas

The canvas listened for a String result to cache the picked image and
move on to segmentation. Nothing ever sent it one - the confirm screen
navigates to segmentation directly - except a cancelled capture, which
sent null. The result key is the type name, so nullability did not keep
them apart and the canvas took the null as an image.

Drop the listener and the intent it fed, and let a cancel be a cancel.
AddRecentImageUseCase stays: the recent list gets filled at upload time,
from the cutout before its border is baked in."
```

---

### Task 6: camera 모듈 — YGScaffoldV2 이관 + 닫기 결선

**Files:**
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/navigation/EntryBuilder.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/CustomCameraRoute.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/SystemCameraRoute.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/PictureConfirmRoute.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/screen/CustomCameraScreen.kt`
- Modify: `feature/camera/impl/build.gradle.kts` (필요 시 `projects.feature.groups.canvas.api` 추가)

**Interfaces:**
- Consumes: Task 1의 `Navigator.popUpTo<T>()`, Task 5의 `CustomCameraEffect.Cancel`
- Produces: 없음 (다른 태스크가 의존하지 않는다)

이관은 이름 교체가 아니라 **소유 위치 이동**이다. `hiltViewModel()`이 Route 안에서 불리므로 EntryBuilder는 `state`도 이펙트도 못 본다.

`CustomCameraScreen`의 `YGToastHost`는 지금 화면 상단이 아니라 **뷰파인더 Box 안**에 얹혀 있다. 스캐폴드로 옮기면 상태바 인셋 아래 상단으로 올라간다. 눈에 보이는 변화지만 Toast 공통 정책이 "위→아래 노출"이라 그쪽이 맞다.

> Task 5가 촬영 실패와 취소를 둘 다 `Cancel`로 묶었다. 이 태스크에서 촬영 실패만 다시 가른다 — 실패를 토스트로 알리려면 토스트 자리가 먼저 있어야 해서 순서가 이렇다. 되돌리기가 아니라 이어 짓기다.

- [ ] **Step 1: EntryBuilder를 얇게 만든다**

`feature/camera/impl/.../navigation/EntryBuilder.kt` 전체를 아래로 바꾼다.

```kotlin
package com.teamyg.parfait.feature.camera.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.camera.api.NavKeyCameraSystem
import com.teamyg.parfait.feature.camera.api.NavKeyPictureConfirm
import com.teamyg.parfait.feature.camera.impl.route.CustomCameraRoute
import com.teamyg.parfait.feature.camera.impl.route.PictureConfirmRoute
import com.teamyg.parfait.feature.camera.impl.route.SystemCameraRoute

fun EntryProviderScope<NavKey>.featureCameraEntryBuilder(navigator: Navigator) {
    entry<NavKeyCameraCustom> { navKey ->
        CustomCameraRoute(
            navigator = navigator,
            showGuideToast = navKey.showGuideToast,
            returnResultOnly = navKey.returnResultOnly,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<NavKeyCameraSystem> {
        SystemCameraRoute(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<NavKeyPictureConfirm> { navKey ->
        PictureConfirmRoute(
            uri = navKey.uri,
            source = navKey.source,
            returnResultOnly = navKey.returnResultOnly,
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

- [ ] **Step 2: CustomCameraRoute가 스캐폴드를 소유하게 한다**

`import`에 더한다.

```kotlin
import androidx.compose.foundation.layout.padding
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
```

`toastPolicy` 선언 아래에 실패 문구를 읽는 줄을 더한다. 문자열은 이미 있다.

```kotlin
    val captureFailedMessage = stringResource(R.string.camera_capture_failed)
```

`effect.collect`의 `Cancel` 분기를 두 갈래로 나눈다. 촬영 실패는 지금 조용히 뒤로 가는데, 실패를 알리고 끝나는 종류라 토스트가 맞다.

`CustomCameraViewModel`의 `handleOnCaptureFailed`가 `Cancel`을 쏘던 것을 별도 이펙트로 가른다.

```kotlin
    /** 촬영이 실패했다. 알리고 그 자리에 머문다 — 되돌아가면 사용자는 왜 아무 일도 없었는지 모른다 */
    data object CaptureFailed : CustomCameraEffect
```

`handleOnCaptureFailed`를 바꾼다.

```kotlin
    private fun handleOnCaptureFailed() {
        postSideEffect(CustomCameraEffect.CaptureFailed)
    }
```

`CustomCameraRoute`의 `effect.collect`에 분기를 더한다.

```kotlin
                is CustomCameraEffect.CaptureFailed -> {
                    toastPolicy.showError(captureFailedMessage)
                }
```

마지막으로 `CustomCameraScreen(...)` 호출을 스캐폴드로 감싸고 `toastPolicy` 인자를 뺀다.

```kotlin
    YGScaffoldV2(toastPolicy = toastPolicy) { innerPadding ->
        CustomCameraScreen(
            state = state,
            onClickGrantPermission = { viewModel.processIntent(CustomCameraIntent.OnRequestPermission) },
            onClickOpenAppSettings = { viewModel.processIntent(CustomCameraIntent.OnOpenAppSettings) },
            onClickZoomLevel = { viewModel.processIntent(CustomCameraIntent.OnClickZoomLevel(it)) },
            onClickShutter = { viewModel.processIntent(CustomCameraIntent.OnClickShutter) },
            onClickFlip = { viewModel.processIntent(CustomCameraIntent.OnClickFlip) },
            onClickCancel = { viewModel.processIntent(CustomCameraIntent.OnCancel) },
            onClickFlash = { viewModel.processIntent(CustomCameraIntent.OnClickFlash) },
            modifier = modifier,
            onViewfinderRectChange = { viewfinderRect = it },
            cameraFeed = <현행 호출부의 cameraFeed 인자를 글자 그대로 옮긴다>,
        )
    }
```

바뀌는 것은 **`toastPolicy` 인자를 빼고 전체를 `YGScaffoldV2`로 감싸는 것**뿐이다. `cameraFeed`를 비롯한 나머지 인자는 현행 호출부에서 글자 그대로 옮겨라 — 이 태스크는 카메라 피드 렌더링을 건드리지 않는다.

`innerPadding`은 쓰지 않으므로 람다 파라미터 이름을 `_`로 둔다. 카메라 피드가 시스템 바 아래까지 덮어야 해서 인셋을 화면에 먹이지 않고, 인셋은 `CustomCameraScreen`의 컨트롤 영역이 직접 처리한다. 그 이유를 주석으로 남겨라 — 지금 EntryBuilder에 있던 주석이 갈 곳이 여기다.

- [ ] **Step 3: CustomCameraScreen에서 토스트 배선을 걷는다**

`CustomCameraScreen.kt`에서 지운다.

- `import ...ygtoast.YGToastHost`
- `import ...ygtoast.YGToastPolicy`
- `import ...ygtoast.rememberYGToastPolicy`
- `CustomCameraScreen`·`CameraContent` 두 함수의 `toastPolicy: YGToastPolicy,` 파라미터와 넘기는 인자
- 뷰파인더 `Box` 안의 `YGToastHost(...)` 블록 전체 (`Box` 자체는 남긴다 — `onGloballyPositioned`로 뷰파인더 위치를 통지하는 것이 본래 역할이다)
- 프리뷰 세 곳의 `toastPolicy = rememberYGToastPolicy(),` 인자

`YGToastHost` 삭제로 안 쓰게 된 `import`(`requiredWidth`·`LocalConfiguration`·`windowInsetsPadding`·`WindowInsets` 등)가 있으면 함께 지운다. ktlint 가 잡아 준다.

- [ ] **Step 4: SystemCameraRoute·PictureConfirmRoute에 스캐폴드를 씌운다**

`SystemCameraRoute.kt` — `import`에 `androidx.compose.foundation.layout.padding`과 `com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2`를 더하고, `SystemCameraScreen(...)`을 감싼다.

```kotlin
    YGScaffoldV2 { innerPadding ->
        SystemCameraScreen(
            state = state,
            onClickGrantPermission = { viewModel.processIntent(SystemCameraIntent.OnRequestPermission) },
            onClickOpenAppSettings = { viewModel.processIntent(SystemCameraIntent.OnOpenAppSettings) },
            onClickRetry = { viewModel.processIntent(SystemCameraIntent.OnRetry) },
            onClickCancel = { viewModel.processIntent(SystemCameraIntent.OnCancel) },
            modifier = modifier.padding(innerPadding),
        )
    }
```

`PictureConfirmRoute.kt` — 스캐폴드를 씌우고 닫기를 결선한다. 전체를 아래로 바꾼다.

```kotlin
package com.teamyg.parfait.feature.camera.impl.route

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.PictureConfirmResult
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.camera.impl.screen.PictureConfirmScreen
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation

@Composable
internal fun PictureConfirmRoute(
    uri: String,
    source: PictureConfirmSource,
    returnResultOnly: Boolean,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current

    YGScaffoldV2 { innerPadding ->
        PictureConfirmScreen(
            uri = uri,
            source = source,
            onClickReCapture = { navigator.onBack() },
            onClickConfirm = {
                if (returnResultOnly) {
                    resultEventBus.sendResult(PictureConfirmResult(uri = uri, source = source))
                    navigator.onBack() // PictureConfirm
                    navigator.onBack() // Camera/Gallery
                } else {
                    navigator.goToAndPopCurrent(
                        destination = NavKeySegmentation(
                            sourceImageUri = uri,
                        ),
                    )
                }
            },
            // 배경 편집에서 들어온 경우 캔버스까지 튀면 편집 중이던 배경이 날아간다.
            // 그 경로의 닫기는 부른 화면으로 돌아가는 것이고, 확인 버튼과 같은 백 처리다
            onClickClose = {
                if (returnResultOnly) {
                    navigator.onBack() // PictureConfirm
                    navigator.onBack() // Camera/Gallery
                } else {
                    navigator.popUpTo<NavKeyCanvasMain>()
                }
            },
            modifier = modifier.padding(innerPadding),
        )
    }
}
```

`feature/camera/impl/build.gradle.kts`의 `dependencies`에 아래 한 줄을 더한다. 지금은 없다 — `NavKeyCanvasMain`이 그 모듈에 있다.

```kotlin
    implementation(projects.feature.groups.canvas.api)
```

- [ ] **Step 5: 빌드와 정적검사를 확인한다**

```bash
./gradlew :feature:camera:impl:compileDebugKotlin :app:assembleDebug ktlintCheck
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋한다**

```bash
git add feature/camera/impl
git commit -m "refactor(camera): move the scaffold into the routes

The entry builder owned YGScaffold, so it could never see the state or
the effects that live inside the route. Hand the scaffold to each route
and let it pass isLoading and the toast policy.

The toast host moves out of the viewfinder box and up under the status
bar, which is where the shared toast policy says toasts belong. A failed
capture now says so instead of quietly going back, and close leaves for
the canvas - except from background editing, where it returns to the
screen that asked."
```

---

### Task 7: gallery 모듈 — YGScaffoldV2 이관

**Files:**
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/navigation/EntryBuilder.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/route/CustomGalleryPickerRoute.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/route/SystemGalleryPickerRoute.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/screen/CustomGalleryPickerScreen.kt`

**Interfaces:**
- Consumes: 없음
- Produces: 없음

`CustomGalleryPickerScreen`의 로딩은 그리드 자리에 인디케이터를 그리는 **화면 고유 표현**이라 `isLoading`으로 옮기지 않는다. V2 스펙이 화면 고유 로딩은 흡수하지 않겠다고 명시했다.

- [ ] **Step 1: EntryBuilder를 얇게 만든다**

`feature/gallery/impl/.../navigation/EntryBuilder.kt` 전체를 아래로 바꾼다.

```kotlin
package com.teamyg.parfait.feature.gallery.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.gallery.api.NavKeySystemGalleryPicker
import com.teamyg.parfait.feature.gallery.impl.route.CustomGalleryPickerRoute
import com.teamyg.parfait.feature.gallery.impl.route.SystemGalleryPickerRoute

fun EntryProviderScope<NavKey>.featureSystemGalleryEntryBuilder(navigator: Navigator) {
    entry<NavKeySystemGalleryPicker> {
        SystemGalleryPickerRoute(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

fun EntryProviderScope<NavKey>.featureCustomGalleryEntryBuilder(navigator: Navigator) {
    entry<NavKeyCustomGalleryPicker> { navKey ->
        CustomGalleryPickerRoute(
            navigator = navigator,
            showGuideToast = navKey.showGuideToast,
            returnResultOnly = navKey.returnResultOnly,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

- [ ] **Step 2: 두 Route가 스캐폴드를 소유하게 한다**

`CustomGalleryPickerRoute.kt` — `import`에 `androidx.compose.foundation.layout.padding`과 `com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2`를 더하고, 파일 끝의 `CustomGalleryPickerScreen(...)` 호출을 감싼다.

```kotlin
    YGScaffoldV2(toastPolicy = toastPolicy) { innerPadding ->
        CustomGalleryPickerScreen(
            state = state,
            onClickGrantPermission = { viewModel.processIntent(CustomGalleryPickerIntent.OnRequestPermission) },
            onClickOpenSettings = { viewModel.processIntent(CustomGalleryPickerIntent.OnRequestOpenSettings) },
            onClickManageMedia = { viewModel.processIntent(CustomGalleryPickerIntent.OnRequestManageMedia) },
            onClickImage = { viewModel.processIntent(CustomGalleryPickerIntent.OnClickImage(it)) },
            onClickCancel = { viewModel.processIntent(CustomGalleryPickerIntent.OnCancel) },
            modifier = modifier.padding(innerPadding),
        )
    }
```

`SystemGalleryPickerRoute.kt` — 같은 두 `import`를 더하고 감싼다.

```kotlin
    YGScaffoldV2 { innerPadding ->
        SystemGalleryPickerScreen(
            state = state,
            modifier = modifier.padding(innerPadding),
            onClickConfirm = {
                viewModel.processIntent(SystemGalleryIntent.ConfirmPhoto(state.imageUri))
            },
        )
    }
```

- [ ] **Step 3: CustomGalleryPickerScreen에서 토스트 배선을 걷는다**

`CustomGalleryPickerScreen.kt`에서 지운다.

- `import ...ygtoast.YGToastHost`
- `import ...ygtoast.YGToastPolicy`
- `import ...ygtoast.rememberYGToastPolicy`
- 두 컴포저블의 `toastPolicy: YGToastPolicy,` 파라미터와 넘기는 인자
- `YGToastHost(policy = toastPolicy, …)` 블록
- 프리뷰의 `toastPolicy = rememberYGToastPolicy(),` 인자

안 쓰게 된 `import`가 남으면 함께 지운다.

- [ ] **Step 4: 빌드와 정적검사를 확인한다**

```bash
./gradlew :feature:gallery:impl:compileDebugKotlin :app:assembleDebug ktlintCheck
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋한다**

```bash
git add feature/gallery/impl
git commit -m "refactor(gallery): move the scaffold into the routes

Same move as the camera module: the entry builder cannot reach the
state or the effects, so the scaffold belongs in the route. The picker
keeps drawing its own indicator where the grid goes - that is a
screen-specific loading state, not the shared overlay."
```

---

### Task 8: segmentation 모듈 — YGScaffoldV2 이관 + 닫기 결선

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/navigation/EntryBuilder.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationConfirmRoute.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/ToppingEditRoute.kt`

**Interfaces:**
- Consumes: Task 1의 `Navigator.popUpTo<T>()`, Task 4가 바꾼 `SegmentationViewModel`
- Produces: 없음

`SegmentationLoadingScreen`은 문구와 닫기 버튼을 가진 전용 화면이라 `isLoading`으로 흡수하지 않는다. `SegmentationErrorScreen`도 그대로 둔다 — V2가 다루는 것은 "알리고 끝나는" 실패뿐이다.

이 모듈은 `feature/groups/canvas/api`에 이미 의존한다(`NavKeyCanvasToppingPlace` 때문). `NavKeyCanvasMain`도 같은 모듈이라 의존성 추가가 없다.

- [ ] **Step 1: EntryBuilder를 얇게 만든다**

`feature/segmentation/impl/.../navigation/EntryBuilder.kt` 전체를 아래로 바꾼다.

```kotlin
package com.teamyg.parfait.feature.segmentation.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.impl.route.SegmentationConfirmRoute
import com.teamyg.parfait.feature.segmentation.impl.route.SegmentationRoute
import com.teamyg.parfait.feature.segmentation.impl.route.ToppingEditRoute

fun EntryProviderScope<NavKey>.featureSegmentationEntryBuilder(navigator: Navigator) {
    entry<NavKeySegmentation> { key ->
        SegmentationRoute(
            navigator = navigator,
            key = key,
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<NavKeySegmentationConfirm> { key ->
        SegmentationConfirmRoute(
            navigator = navigator,
            key = key,
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<NavKeyToppingEdit> { key ->
        ToppingEditRoute(
            navigator = navigator,
            key = key,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

- [ ] **Step 2: SegmentationRoute가 스캐폴드를 소유하고 닫기를 연결한다**

`import`에 더한다.

```kotlin
import androidx.compose.foundation.layout.padding
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
```

`SegmentationScreen(...)` 호출을 감싸고 `onClickClose`를 채운다. `onClickSubject`의 두 경로 검사는 PR #290이 넣은 그대로 둔다.

```kotlin
    YGScaffoldV2 { innerPadding ->
        SegmentationScreen(
            state = state,
            modifier = modifier.padding(innerPadding),
            onClickBack = { navigator.onBack() },
            // 토핑 만들기를 접고 캔버스로 돌아간다. 사이에 쌓인 화면은 모두 걷는다
            onClickClose = { navigator.popUpTo<NavKeyCanvasMain>() },
            // 백스택에 쌓아 올려서 뒤로가기 하면 객체 인식이 끝난 이 화면으로 그대로 돌아온다
            onClickSubject = {
                val subjectImagePath = state.subjectImagePath
                val trimmedSubjectImagePath = state.trimmedSubjectImagePath
                if (subjectImagePath != null && trimmedSubjectImagePath != null) {
                    navigator.goTo(
                        NavKeySegmentationConfirm(
                            sourceImageUri = key.sourceImageUri,
                            subjectImagePath = subjectImagePath,
                            trimmedSubjectImagePath = trimmedSubjectImagePath,
                        ),
                    )
                }
            },
        )
    }
```

- [ ] **Step 3: SegmentationConfirmRoute에 스캐폴드를 씌우고 닫기를 연결한다**

`SegmentationConfirmRoute.kt`의 `import`에 더한다.

```kotlin
import androidx.compose.foundation.layout.padding
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
```

파일 끝의 `SegmentationConfirmScreen(...)` 호출을 감싸고 `onClickClose`를 채운다. `subjectImagePath`·`cutoutImagePath`·`borderLayers` 선언과 `ResultEffect` 블록은 스캐폴드 **밖**에 그대로 둔다 — 상태와 결과 수신은 레이아웃과 무관하다.

```kotlin
    YGScaffoldV2 { innerPadding ->
        SegmentationConfirmScreen(
            subjectImagePath = subjectImagePath,
            onClickBack = { navigator.onBack() },
            // 토핑 만들기를 접고 캔버스로 돌아간다. 사이에 쌓인 화면은 모두 걷는다
            onClickClose = { navigator.popUpTo<NavKeyCanvasMain>() },
            onClickEditPhoto = {
                navigator.goTo(
                    NavKeyToppingEdit(
                        sourceImageUri = key.sourceImageUri,
                        // 편집 화면은 ContentResolver 로 읽으므로 파일 경로를 file 스킴 uri 로 바꿔서 넘긴다
                        segmentationImageUri = File(cutoutImagePath).toUri().toString(),
                        borderLayers = borderLayers,
                    ),
                )
            },
            onClickNext = { navigator.goTo(NavKeyCanvasToppingPlace(imageUri = subjectImagePath)) },
            modifier = modifier.padding(innerPadding),
        )
    }
```

- [ ] **Step 4: ToppingEditRoute에 스캐폴드를 씌운다**

이 화면에는 **닫기 버튼이 없다** — 뒤로만 있다. 그래서 여기서는 닫기 결선이 없고 스캐폴드만 씌운다.

`import`에 `androidx.compose.foundation.layout.padding`과 `com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2`를 더한다. `NavKeyCanvasMain`은 이 파일에서 쓰지 않으므로 넣지 마라.

파일 끝의 `ToppingEditScreen(...)` 호출을 감싸고, 넘기는 인자는 현행 그대로 두되 `modifier`만 바꾼다.

```kotlin
    YGScaffoldV2 { innerPadding ->
        ToppingEditScreen(
            // …현행 인자를 글자 그대로 옮긴다…
            modifier = modifier.padding(innerPadding),
        )
    }
```

`ToppingEditEffect.LoadFailed`·`SaveFailed`가 쓰는 안드로이드 `Toast`는 **건드리지 마라.** 스캐폴드 이관 범위 밖이고, `YGToastPolicy`로 옮기려면 문구 소유와 정책 배선을 함께 정해야 한다.

- [ ] **Step 5: 빌드와 테스트를 확인한다**

```bash
./gradlew :feature:segmentation:impl:compileDebugKotlin test :app:assembleDebug ktlintCheck
```

Expected: BUILD SUCCESSFUL, 테스트 전부 PASS

- [ ] **Step 6: 커밋한다**

```bash
git add feature/segmentation/impl
git commit -m "refactor(segmentation): move the scaffold into the routes and give close a target

Close was an empty lambda on both routes that have one, so the
topping-making path had no exit. Pop back to the canvas by type - these
screens do not carry the group id, so matching on the key value was not
an option. The segmentation route shares one close callback across its
loading, error, and content screens, so all three get the exit.

The loading and error screens stay as they are. They carry their own
copy and a close button, which is more than the shared overlay does."
```

---

### Task 9: CanvasMove 죽은 코드 삭제

**Files:**
- Delete: `feature/groups/canvas/api/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/api/NavKeyCanvasMove.kt`
- Delete: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMoveRoute.kt`
- Delete: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasMoveScreen.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/navigation/EntryBuilder.kt`

**Interfaces:**
- Consumes: 없음
- Produces: 없음

PR #290이 `SegmentationConfirmRoute`의 다음 화면을 `NavKeyCanvasToppingPlace`로 옮기면서 `NavKeyCanvasMove` 호출이 끊겼지만 파일은 남았다.

- [ ] **Step 1: 정말 아무도 안 쓰는지 확인한다**

```bash
grep -rn --include='*.kt' "NavKeyCanvasMove\|CanvasMoveRoute\|CanvasMoveScreen" . | grep -v "/build/"
```

Expected: `NavKeyCanvasMove.kt`, `CanvasMoveRoute.kt`, `CanvasMoveScreen.kt`, 그리고 canvas `EntryBuilder.kt` 안의 import·entry 블록만 나온다. 그 밖의 파일이 하나라도 나오면 **삭제하지 말고 멈춰서 보고해라.**

- [ ] **Step 2: 파일을 지운다**

```bash
git rm feature/groups/canvas/api/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/api/NavKeyCanvasMove.kt \
       feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMoveRoute.kt \
       feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasMoveScreen.kt
```

- [ ] **Step 3: EntryBuilder에서 항목을 걷는다**

canvas `EntryBuilder.kt`에서 지운다.

- `import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMove`
- `import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasMoveRoute`
- `entry<NavKeyCanvasMove> { … }` 블록 전체

- [ ] **Step 4: 빌드와 테스트를 확인한다**

```bash
./gradlew test :app:assembleDebug ktlintCheck
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋한다**

```bash
git add -A feature/groups/canvas
git commit -m "chore(canvas): delete the move screen nothing navigates to

The topping placement round pointed the confirm screen at
NavKeyCanvasToppingPlace and left the old destination behind."
```

---

### Task 10: parfait 문서 갱신

**Files (이 위키 저장소, `TJYG-Android`가 아니다):**
- Modify: `parfait/specs/2026-08-18-segmentation-pipeline-hardening.md`
- Modify: `parfait/specs/README.md`
- Modify: `parfait/adr/0012-mlkit-subject-segmentation.md`
- Modify: `parfait/synthesis/open-questions.md`
- Modify: `parfait/architecture/navigation-flow.md`
- Modify: `parfait/architecture/design-system.md`

**Interfaces:**
- Consumes: Task 1~9의 실제 구현 결과
- Produces: 없음

문서는 **구현이 끝난 뒤** 실제로 만들어진 것을 적는다. 설계와 다르게 구현된 부분이 있으면 설계를 고치지 말고 **as-built로 차이를 적어라** — 무엇이 뒤집혔는지가 다음 라운드의 근거다.

이 저장소는 `main`에 직접 커밋하지 않는다. 브랜치 `docs/parfait-segmentation-pipeline-hardening`이 이미 있고 스펙 커밋이 올라가 있다. 거기에 이어서 커밋해라.

- [ ] **Step 1: 스펙 상태와 as-built를 적는다**

`parfait/specs/2026-08-18-segmentation-pipeline-hardening.md`의 frontmatter에서 `status: draft`를 `status: implemented`로 바꾸고 `verified`를 구현일로 갱신한다. `related_code`에 새로 생긴 심볼을 더한다: `SegmentationMask.kt#maskSubjectPixels`, `SegmentationCacheDir.kt#clearFiles`, `ClearSegmentationCacheUseCase.kt#ClearSegmentationCacheUseCase`, `Navigator.kt#popUpTo`.

본문 맨 끝에 절을 붙인다.

```markdown
## as-built (YYYY-MM-DD 구현)

브랜치 `refactor/segmentation-logic`, 커밋 N개. `./gradlew test ktlintCheck :app:assembleDebug` 통과.
테스트 총량: 유닛 <before> → <after>건.

설계에서 **뒤집힌 결정 N건.** (없으면 "0건"이라고 적고, 있으면 무엇이 왜 바뀌었는지 한 줄씩)

구현·리뷰가 더한 것:
- (예: 캐시 파일 이름이 밀리초 기준이라 같은 밀리초에 두 번 저장하면 덮어썼다. `File.createTempFile` 로 바꿨다)

실기기 확인: (했으면 기기명과 확인 항목, 안 했으면 "없음"이라고 적어라 — 머지가 검증을 대신하지 않는다)
```

- [ ] **Step 2: specs/README.md 인덱스 행의 상태를 갱신한다**

`draft`를 `implemented(미머지)` 또는 머지됐으면 `implemented`로 바꾼다. 요약 본문에서 구현과 달라진 서술이 있으면 함께 고친다.

- [ ] **Step 3: ADR-0012에 as-built를 더한다**

`parfait/adr/0012-mlkit-subject-segmentation.md`의 기존 "As-built 갱신" 절 뒤에 새 절을 붙인다. 담을 것 셋:

- 캐시 정리 정책이 정해졌다 — `cacheDir` 전용 하위 디렉토리 + 세그멘테이션 진입 시 비우기. "캐시 파일 정리 정책 필요"라던 트레이드오프와 "정리 정책은 더 급해졌다"는 경고를 여기서 닫는다
- `foregroundConfidenceMask == null` 이 `Result.failure` 를 타게 됐다 — "실패 표현은 여전히 완전하지 않다"는 문장을 지운다
- 마스크 루프가 `getPixels` 1회 + 배열 내 마스킹으로 바뀌었고, 마스크 크기 불일치 방어가 붙었다

- [ ] **Step 4: open-questions 3건의 상태를 갱신한다**

`parfait/synthesis/open-questions.md`에서:

- **OQ-P-003** — ③(캐시 정리)을 해소로 표기하고 해소 메모에 디렉토리·시점을 적는다. ①(재시도 경로)은 **잔존**이다. 항목 전체를 해소로 바꾸지 마라
- **OQ-P-004** — ②(null 마스크 raw throw)를 해소로 표기한다. ①은 이미 해소돼 있으므로 항목 상태가 "해소됨"이 된다
- **OQ-P-055** — ②(닫기 TODO)를 해소로 표기하고 `popUpTo<NavKeyCanvasMain>()` 과 배경 편집 경로 분기를 적는다

- [ ] **Step 5: architecture 문서 둘을 갱신한다**

`parfait/architecture/navigation-flow.md` — `Navigator` API 목록에 `popUpTo<T>()`를 더하고, 언제 `goToSingleClearTop` 대신 이것을 쓰는지(목적지 키의 인자를 호출부가 모를 때) 한 줄 적는다. "신규 목적지 등록 체크리스트"에 닫기 경로를 비워 두지 말라는 항목이 없으면 더한다.

`parfait/architecture/design-system.md` — "화면 컨테이너" 절의 `YGScaffoldV2` 이관 현황 수치를 갱신한다(이 라운드로 8개 엔트리가 늘었다). V1 잔존 사용처가 몇 곳인지 세어서 적어라:

```bash
grep -rn --include='*.kt' "YGScaffold\b" <TJYG-Android 경로> | grep -v "/build/" | grep -v core/designsystem
```

- [ ] **Step 6: 문서 lint 대신 링크를 눈으로 확인한다**

새로 넣은 상대 경로 링크가 실제 파일을 가리키는지 확인한다. `parfait/`는 위키 스키마 밖이라 `wiki/script/lint.py` 대상이 아니다.

- [ ] **Step 7: 커밋한다**

```bash
git add parfait/
git commit -m "docs(parfait): record the segmentation pipeline round as built

Closes OQ-P-004 (2), OQ-P-003 (3), and OQ-P-055 (2). OQ-P-003 (1),
the missing retry path, stays open."
```

---

## 마무리 확인

모든 태스크가 끝나면 아래를 순서대로 돌리고 결과를 보고해라. 실패하면 무엇이 어떻게 실패했는지 그대로 적어라 — 통과했다고 쓰지 마라.

```bash
./gradlew test
./gradlew ktlintCheck
./gradlew :app:assembleDebug
```

그다음 **실기기 확인**을 사람에게 요청한다. 유닛 테스트가 못 잡는 것들이다.

1. 카메라로 찍고 확인 → 세그멘테이션이 성공하고 객체가 하이라이트된다
2. 갤러리에서 고르고 확인 → 같다
3. 카메라를 취소한다 → 크래시 없이 캔버스로 돌아온다 (이 라운드가 고친 크래시다)
4. 세그멘테이션 로딩·에러·본 화면과 누끼 확인 화면에서 닫기 → 캔버스로 돌아온다 (토핑 편집 화면에는 닫기가 없다)
5. 캔버스 배경 편집 → 카메라 → 확인 화면에서 닫기 → 배경 편집 화면으로 돌아온다 (캔버스까지 튀지 않는다)
6. 카메라 가이드 토스트가 상태바 아래 상단에 뜬다 (뷰파인더 안이 아니다)
7. 세그멘테이션을 두 번 연달아 돌린다 → `cacheDir` 하위 세그멘테이션 디렉토리에 직전 흐름 파일이 남지 않는다
8. 고해상도 사진으로 세그멘테이션을 돌린다 → OOM 없이 끝난다 (다운샘플을 넣지 않았으므로 이 항목이 특히 중요하다)

푸시와 PR 생성은 **사용자 확인을 받고** 한다.
