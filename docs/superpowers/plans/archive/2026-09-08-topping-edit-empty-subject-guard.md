---
id: topping-edit-empty-subject-guard
title: 토핑 편집 빈 알맹이 차단
status: done
type: work-order
created: 2026-09-08
updated: 2026-09-09
platforms: android
owner: Parfait 팀
related_adr:
related_spec: topping-edit-empty-subject-guard
related_code: SubjectCoverage, SegmentationCandidateFilter, ImageSegmentationRepositoryImpl, ToppingEditMask, SubjectMeasure, ToppingEditViewModel, ToppingEditEffect, ToppingEditRoute, SubjectCoverageTest, ToppingEditMaskTest
archived_reason: Task 1~4 를 전량 수행하고 develop 에 머지했다(2026-09-09, PR #472 `753675abe`). 신규 유닛 13건(SubjectCoverageTest 8·ToppingEditMaskTest 5). Task 5(실기기 수동 검증)는 저장소에서 확인할 수 없어 미체크로 두고, borderOnly 예외는 구현이 드러낸 뒤 스펙에 반영됐다
tags: [plan, parfait, segmentation, topping, validation]
---

# 토핑 편집 빈 알맹이 차단 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 토핑 편집에서 영역을 전부 지운(또는 하한 미만만 남긴) 알맹이가 저장·업로드되지 않게 막는다.

**Architecture:** 자동 누끼 경로가 이미 쓰는 커버리지 하한을 `data`의 `internal`에서 `domain`의 `SubjectCoverage`로 올려 두 경로가 같은 함수를 보게 한다. 편집 화면은 `buildCutoutBitmap` 결과를 **한 번의 스캔**으로 재고(경계 + 알파 합), 하한 미달이면 파일을 쓰기 전에 되돌린 뒤 Toast만 띄우고 화면에 머문다. 픽셀 스캔 로직은 `android.graphics`에 의존하지 않는 순수 함수로 빼서 JVM 유닛으로 덮는다.

**Tech Stack:** Kotlin, `android.graphics.Bitmap`, Kotlin Coroutines, 자체 MVI(`BaseViewModel`), 테스트는 kotlin-test.

**Spec:** [`parfait/specs/2026-09-08-topping-edit-empty-subject-guard.md`](../../specs/archive/2026-09-08-topping-edit-empty-subject-guard.md)

**작업 저장소:** `TJYG-Android` (remote `mash-up-kr/TEAMYG-Android`). 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다.

## Global Constraints

- **작업 위치**: `TJYG-Android` 저장소의 **본 체크아웃**. **git worktree를 만들지 않는다.**
- **브랜치**: `develop`에서 새 브랜치를 판다(기본 이름 `feature/topping-edit-empty-subject-guard`, 이슈 번호가 정해지면 `feature/#<번호>-topping-edit-empty-subject-guard`). 현재 체크아웃된 `feature/#471-image-down-scale` **위에 얹지 않는다** — 다운스케일 라운드와 범위가 다르다.
- **커밋하지 않는다.** 사용자가 커밋을 요청하지 않았다. 각 Task는 코드 편집 + 검증까지만 하고 멈춘다. `git add`·`git commit`·`git push` 모두 금지.
- **하한 값을 새로 만들지 않는다.** 비율 하한 **5/10000**(만분율), 절대 하한 **2,500px**, 알파 스케일 **255**. 지금 `SegmentationCandidateFilter`에 있는 값 그대로이고, 옮기면서 바꾸지 않는다. 값의 근거는 `parfait/specs/archive/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」이다.
- **Toast 문구는 `남은 영역이 너무 작아 저장할 수 없습니다`**, 문자열 이름은 `topping_edit_subject_too_small`. 임의로 바꾸지 않는다.
- **차단해도 화면을 떠나지 않는다.** `navigator.onBack()`을 부르지 않는다.
- **새 의존성을 추가하지 않는다.** Robolectric·계측 테스트 소스셋을 만들지 않는다.
- **알파 합은 `Long`으로 다룬다.** 12MP 불투명 이미지의 합이 `Int` 범위를 넘는다.
- **코드 주석·KDoc 규약**(`parfait/CLAUDE.md` 요지):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다). 써야 하면 근거 문서를 가리킨다.
  - 주석 분량은 그 코드의 **어려움**에 비례한다. 중요하지만 단순한 코드에 긴 주석을 달지 않는다.
- **주석·KDoc은 한국어**로 쓴다. 기존 파일들의 어투를 따른다.
- **매퍼 단독 테스트를 만들지 않는다.**
- ktlint가 CI 게이트다(`.github/workflows/ktlint.yml`이 `./gradlew ktlintCheck`를 돈다). 각 Task 끝에 해당 모듈의 `ktlintCheck`를 돌린다.
- 테스트 태스크 이름은 모듈마다 다르다. `:domain`은 JVM 모듈이라 **`:domain:test`**, Android 모듈은 **`testDebugUnitTest`**다.

---

## File Structure

**Create**

| 파일 | 역할 |
|------|------|
| `domain/src/main/java/com/teamyg/parfait/domain/model/SubjectCoverage.kt` | 알맹이 커버리지 하한 정책 — 상수, `floorPixels`, `isLargeEnough` |
| `domain/src/test/java/com/teamyg/parfait/domain/model/SubjectCoverageTest.kt` | 정책 경계 JVM 유닛 |
| `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/editor/ToppingEditMaskTest.kt` | `measureSubject` 순수 함수 JVM 유닛 |

**Modify**

| 파일 | 변경 |
|------|------|
| `data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationCandidateFilter.kt` | 하한 상수·`coverageFloorPixels` 삭제, `SubjectCoverage` 호출로 교체 |
| `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt` | `coverageFloorPixels` → `SubjectCoverage.floorPixels` |
| `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/editor/ToppingEditMask.kt` | `SubjectMeasure`·`measureSubject`·`trimTo` 신설, `trimTransparentBounds` 대체 |
| `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/ToppingEditViewModel.kt` | `completeEdit`에 하한 판정·차단, `SubjectTooSmall` effect 추가 |
| `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/ToppingEditRoute.kt` | `SubjectTooSmall` 분기(Toast만, 되감기 없음) |
| `feature/segmentation/impl/src/main/res/values/strings.xml` | `topping_edit_subject_too_small` 추가 |

**Task 의존 순서**: Task 1(정책 신설) → Task 2(`data` 이관) → Task 3(측정 함수) → Task 4(차단 배선) → Task 5(실기기 검증). Task 2는 Task 1이 만든 `SubjectCoverage`가 있어야 컴파일된다. Task 4는 Task 1·3의 산출물을 함께 쓴다.

---

## Task 1: 커버리지 하한 정책을 domain에 만든다

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/SubjectCoverage.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/model/SubjectCoverageTest.kt`

**Interfaces:**
- Consumes: 없음(첫 Task).
- Produces: `com.teamyg.parfait.domain.model.SubjectCoverage` — `fun floorPixels(canvasArea: Long): Long`, `fun isLargeEnough(alphaSum: Long, canvasArea: Long): Boolean`. Task 2·4가 이 두 함수를 부른다.

- [x] **Step 1: 실패하는 테스트를 쓴다**

`domain/src/test/java/com/teamyg/parfait/domain/model/SubjectCoverageTest.kt`를 새로 만든다.

```kotlin
package com.teamyg.parfait.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/** 100×100 캔버스는 비율 하한이 5px 이라 절대 하한(2,500px)이 이긴다 */
private const val SMALL_CANVAS_AREA = 100L * 100L

/** 4000×3000 캔버스는 비율 하한이 6,000px 이라 절대 하한을 넘어선다 */
private const val BIG_CANVAS_AREA = 4_000L * 3_000L

class SubjectCoverageTest {
    @Test
    fun floorPixels_smallCanvas_usesTheAbsoluteFloor() {
        // When
        val floor = SubjectCoverage.floorPixels(SMALL_CANVAS_AREA)

        // Then
        assertEquals(2_500L, floor)
    }

    @Test
    fun floorPixels_bigCanvas_usesTheRatioFloor() {
        // When
        val floor = SubjectCoverage.floorPixels(BIG_CANVAS_AREA)

        // Then
        assertEquals(6_000L, floor)
    }

    @Test
    fun isLargeEnough_coverageIsExactlyTheFloor_isTrue() {
        // Given — 알파 합은 픽셀 수 × 255 다
        val alphaSum = 255L * 2_500L

        // Then
        assertTrue(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = SMALL_CANVAS_AREA))
    }

    @Test
    fun isLargeEnough_coverageIsOneShortOfTheFloor_isFalse() {
        // Given
        val alphaSum = 255L * 2_500L - 1L

        // Then
        assertFalse(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = SMALL_CANVAS_AREA))
    }

    @Test
    fun isLargeEnough_fullyTransparent_isFalse() {
        // Then
        assertFalse(SubjectCoverage.isLargeEnough(alphaSum = 0L, canvasArea = SMALL_CANVAS_AREA))
    }

    @Test
    fun isLargeEnough_bigCanvas_ratioFloorTakesOverTheAbsoluteFloor() {
        // Given — 절대 하한은 넘지만 비율 하한에는 못 미치는 커버리지
        val alphaSum = 255L * 3_000L

        // Then
        assertFalse(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = BIG_CANVAS_AREA))
    }

    @Test
    fun isLargeEnough_canvasAreaIsZero_isFalse() {
        // Given — 좌표계가 없으면 비율을 잴 수 없다
        val alphaSum = 255L * 10_000L

        // Then
        assertFalse(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = 0L))
    }

    @Test
    fun isLargeEnough_twelveMegapixelOpaqueSubject_survivesTheIntBoundary() {
        // Given — 4000×3000 전면 불투명. 알파 합 30.6억은 Int 범위를 넘는다
        val canvasArea = BIG_CANVAS_AREA
        val alphaSum = 255L * canvasArea

        // Then
        assertTrue(SubjectCoverage.isLargeEnough(alphaSum = alphaSum, canvasArea = canvasArea))
    }
}
```

- [x] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :domain:test --tests "com.teamyg.parfait.domain.model.SubjectCoverageTest"`

Expected: 컴파일 실패. `Unresolved reference: SubjectCoverage`.

- [x] **Step 3: 최소 구현을 쓴다**

`domain/src/main/java/com/teamyg/parfait/domain/model/SubjectCoverage.kt`를 새로 만든다.

```kotlin
package com.teamyg.parfait.domain.model

/**
 * 알맹이가 "올릴 만큼 남았는가"를 재는 하한.
 *
 * 자동 누끼 후보 필터와 수동 편집이 같은 값을 봐야 한다. 기준이 갈리면 자동으로는 버려질
 * 크기를 수동 편집으로 만들어 낼 수 있어, 하한이 있다는 사실 자체가 의미를 잃는다.
 *
 * 지표를 bounds 사각형이 아니라 알파 합으로 고른 이유와 값의 근거는
 * `parfait/specs/archive/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」에 있다.
 */
object SubjectCoverage {
    /** 캔버스 면적 대비 이 비율 **미만** 커버리지는 하한 미달이다 (만분율) */
    private const val MIN_COVERAGE_PERMYRIAD = 5L

    /** 작은 사진에서 비율만으로는 너무 헐거워지므로 두는 하한 (원본 픽셀) */
    private const val MIN_COVERAGE_PIXELS = 2_500L

    /** 알파 한 픽셀의 최대값 */
    private const val MAX_ALPHA = 255L

    private const val PERMYRIAD_BASE = 10_000L

    fun floorPixels(canvasArea: Long): Long =
        maxOf(MIN_COVERAGE_PIXELS, canvasArea * MIN_COVERAGE_PERMYRIAD / PERMYRIAD_BASE)

    /**
     * @param alphaSum 알파의 총합. [MAX_ALPHA] 로 나누면 실제로 칠해진 픽셀 수가 된다
     */
    fun isLargeEnough(
        alphaSum: Long,
        canvasArea: Long,
    ): Boolean {
        if (canvasArea <= 0L) return false

        // 나누지 않고 양변에 255를 곱해 부동소수를 거치지 않는다
        return alphaSum >= MAX_ALPHA * floorPixels(canvasArea)
    }
}
```

- [x] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :domain:test --tests "com.teamyg.parfait.domain.model.SubjectCoverageTest"`

Expected: PASS (8건).

- [x] **Step 5: ktlint를 돌린다**

Run: `./gradlew :domain:ktlintCheck`

Expected: BUILD SUCCESSFUL. 실패하면 `./gradlew :domain:ktlintFormat` 후 다시 확인한다.

---

## Task 2: data의 두 호출부를 새 정책으로 갈아끼운다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationCandidateFilter.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/utils/image/SegmentationCandidateFilterTest.kt` (수정하지 않는다 — 회귀 게이트로 그대로 돌린다)

**Interfaces:**
- Consumes: Task 1의 `SubjectCoverage.floorPixels`·`SubjectCoverage.isLargeEnough`.
- Produces: 없음(이관만 한다). `filterCandidates`의 시그니처와 동작은 그대로다.

**이 Task는 TDD의 red를 새로 만들지 않는다.** 판정 결과가 **바뀌면 안 되는** 변경이라, 기존
`SegmentationCandidateFilterTest` 13건이 그대로 초록인 것이 곧 검증이다. 새 단언을 추가하지 않는다.

- [x] **Step 1: 기존 테스트가 지금 초록인 것을 먼저 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.utils.image.SegmentationCandidateFilterTest"`

Expected: PASS. 이 값이 이관 전후를 비교하는 기준선이다.

- [x] **Step 2: 필터에서 하한 상수와 계산 함수를 걷어낸다**

`SegmentationCandidateFilter.kt`에서 아래 셋을 **삭제**한다.

```kotlin
/** 캔버스 면적 대비 이 비율 **미만** 커버리지의 후보는 버린다 (만분율) */
internal const val MIN_SUBJECT_COVERAGE_PERMYRIAD = 5L

/** 작은 사진에서 비율만으로는 너무 헐거워지므로 두는 하한 (원본 픽셀) */
internal const val MIN_SUBJECT_COVERAGE_PIXELS = 2_500L
```

```kotlin
/**
 * 후보가 넘어야 하는 "실제로 칠해진 픽셀 수".
 *
 * 값의 근거와 이 지표를 고른 이유는
 * `parfait/specs/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」에 있다.
 */
internal fun coverageFloorPixels(canvasArea: Long): Long =
    maxOf(MIN_SUBJECT_COVERAGE_PIXELS, canvasArea * MIN_SUBJECT_COVERAGE_PERMYRIAD / 10_000L)
```

`MAX_SUBJECT_COUNT`와 `DUPLICATE_IOU_PERMYRIAD`는 **그대로 둔다** — 커버리지 하한이 아니고
`ImageSegmentationRepositoryImpl`이 `MAX_SUBJECT_COUNT`를 쓴다.

그리고 `isLargeEnough()`를 새 정책에 위임한다.

```kotlin
private fun SegmentationCandidate.isLargeEnough(): Boolean =
    SubjectCoverage.isLargeEnough(
        alphaSum = coverageAlphaSum,
        canvasArea = canvasWidth.toLong() * canvasHeight,
    )
```

import에 `com.teamyg.parfait.domain.model.SubjectCoverage`를 더한다.

- [x] **Step 3: 저장소의 bbox 사전 절단도 새 정책을 부르게 한다**

`ImageSegmentationRepositoryImpl.kt`의 `toCandidatePairs`에서 한 줄을 바꾼다.

```kotlin
val floor = SubjectCoverage.floorPixels(origin.width.toLong() * origin.height)
```

import에서 `com.teamyg.parfait.data.utils.image.coverageFloorPixels`를 지우고
`com.teamyg.parfait.domain.model.SubjectCoverage`를 더한다. 그 위의 KDoc("후처리 전에 bbox 로
값싸게 자르는 이유…")은 여전히 사실이므로 **그대로 둔다**.

- [x] **Step 4: 기존 테스트가 여전히 초록인 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.utils.image.SegmentationCandidateFilterTest"`

Expected: PASS, Step 1과 같은 건수. 하나라도 빨개지면 이관 중 값이 바뀐 것이므로 되짚는다.

- [x] **Step 5: data 모듈 전체 유닛과 ktlint를 돌린다**

Run: `./gradlew :data:testDebugUnitTest :data:ktlintCheck`

Expected: BUILD SUCCESSFUL. `coverageFloorPixels`를 참조하던 다른 자리가 남아 있으면 여기서
컴파일 실패로 드러난다.

---

## Task 3: 경계와 알파 합을 한 번의 스캔으로 재는 순수 함수

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/editor/ToppingEditMask.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/editor/ToppingEditMaskTest.kt` (신규)

**Interfaces:**
- Consumes: 없음.
- Produces:
  - `internal data class SubjectMeasure(val left: Int, val top: Int, val right: Int, val bottom: Int, val alphaSum: Long)` — `val isEmpty: Boolean`
  - `internal fun measureSubject(pixels: IntArray, width: Int, height: Int): SubjectMeasure`
  - `internal fun Bitmap.measureSubject(): SubjectMeasure`
  - `internal fun Bitmap.trimTo(measure: SubjectMeasure): Bitmap`

  Task 4가 `Bitmap.measureSubject()`와 `Bitmap.trimTo(measure)`를 부른다. 기존
  `Bitmap.trimTransparentBounds()`는 **삭제된다**(호출부는 `ToppingEditViewModel` 하나뿐이고
  Task 4가 고친다).

- [x] **Step 1: 실패하는 테스트를 쓴다**

`feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/editor/ToppingEditMaskTest.kt`를 새로 만든다.

```kotlin
package com.teamyg.parfait.feature.segmentation.impl.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SIDE = 4

/** ARGB 에서 알파만 의미가 있다 — 색은 SRC_IN 단계에서 원본으로 덮이므로 0 으로 둔다 */
private fun pixel(alpha: Int): Int = alpha shl 24

class ToppingEditMaskTest {
    private fun pixels(vararg alphas: Int): IntArray = IntArray(SIDE * SIDE) { index -> pixel(alphas[index]) }

    @Test
    fun measureSubject_opaqueBlockInTheMiddle_returnsItsBounds() {
        // Given — (1,1)~(2,2) 네 픽셀만 불투명하다
        val pixels = pixels(
            0, 0, 0, 0,
            0, 255, 255, 0,
            0, 255, 255, 0,
            0, 0, 0, 0,
        )

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then
        assertFalse(measure.isEmpty)
        assertEquals(1, measure.left)
        assertEquals(1, measure.top)
        assertEquals(2, measure.right)
        assertEquals(2, measure.bottom)
    }

    @Test
    fun measureSubject_opaqueBlockInTheMiddle_sumsTheAlpha() {
        // Given
        val pixels = pixels(
            0, 0, 0, 0,
            0, 255, 255, 0,
            0, 255, 255, 0,
            0, 0, 0, 0,
        )

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then — 네 픽셀 × 255
        assertEquals(1_020L, measure.alphaSum)
    }

    @Test
    fun measureSubject_semiTransparentPixels_countTheirOwnAlpha() {
        // Given — 반투명 픽셀은 "있음"이 아니라 알파 값 그대로 합에 들어간다
        val pixels = pixels(
            0, 0, 0, 0,
            0, 10, 20, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then
        assertEquals(30L, measure.alphaSum)
        assertEquals(1, measure.left)
        assertEquals(2, measure.right)
        assertEquals(1, measure.top)
        assertEquals(1, measure.bottom)
    }

    @Test
    fun measureSubject_fullyTransparent_isEmpty() {
        // Given
        val pixels = IntArray(SIDE * SIDE)

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then
        assertTrue(measure.isEmpty)
        assertEquals(0L, measure.alphaSum)
    }

    @Test
    fun measureSubject_singleOpaqueCorner_boundsAreOnePixelWide() {
        // Given — 오른쪽 아래 한 픽셀만 남았다
        val pixels = pixels(
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 255,
        )

        // When
        val measure = measureSubject(pixels = pixels, width = SIDE, height = SIDE)

        // Then
        assertEquals(3, measure.left)
        assertEquals(3, measure.right)
        assertEquals(3, measure.top)
        assertEquals(3, measure.bottom)
        assertEquals(255L, measure.alphaSum)
    }
}
```

- [x] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMaskTest"`

Expected: 컴파일 실패. `Unresolved reference: measureSubject`.

- [x] **Step 3: 측정 함수를 쓰고 trim을 그 위에 다시 얹는다**

`ToppingEditMask.kt`에서 기존 `trimTransparentBounds`를 **아래로 통째로 대체**한다.
`buildCutoutBitmap`과 `strokePaint`는 손대지 않는다.

```kotlin
/**
 * 알파가 있는 픽셀의 최소 사각형과 알파 총합.
 *
 * [isEmpty] 면 자를 기준이 없다는 뜻이고, 그때 [left]·[top]·[right]·[bottom] 은 읽지 않는다.
 */
internal data class SubjectMeasure(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val alphaSum: Long,
) {
    val isEmpty: Boolean get() = right < left || bottom < top
}

/**
 * 경계와 알파 합을 **한 번의 스캔**으로 잰다. 둘을 나눠 재면 원본 해상도를 두 번 훑는다.
 *
 * 알파 합이 `Long` 인 이유는 12MP 불투명 이미지의 합이 `Int` 범위를 넘기 때문이다.
 */
internal fun measureSubject(
    pixels: IntArray,
    width: Int,
    height: Int,
): SubjectMeasure {
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    var alphaSum = 0L

    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val alpha = pixels[rowOffset + x] ushr 24
            if (alpha == 0) continue

            alphaSum += alpha.toLong()
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }

    return SubjectMeasure(left = left, top = top, right = right, bottom = bottom, alphaSum = alphaSum)
}

internal fun Bitmap.measureSubject(): SubjectMeasure {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    return measureSubject(pixels = pixels, width = width, height = height)
}

/**
 * 투명한 여백을 걷어내고 [measure] 가 가리키는 사각형만 남긴다.
 * 자를 기준이 없으면 원본을 그대로 돌려준다.
 */
internal fun Bitmap.trimTo(measure: SubjectMeasure): Bitmap {
    if (measure.isEmpty) return this

    return Bitmap.createBitmap(
        this,
        measure.left,
        measure.top,
        measure.right - measure.left + 1,
        measure.bottom - measure.top + 1,
    )
}
```

이 시점에서 `ToppingEditViewModel`이 사라진 `trimTransparentBounds`를 부르고 있어
**`:feature:segmentation:impl` 컴파일이 깨진다.** Task 4가 그것을 고친다. Step 4의 명령은
테스트 소스만 겨냥하지 않으므로, 이 Task에서는 아래 순서대로 확인한다.

- [x] **Step 4: 새 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMaskTest"`

Expected: 컴파일 실패 — 다만 실패 원인이 **`ToppingEditViewModel`의 `trimTransparentBounds`
미해결 참조 하나뿐**이어야 한다. 다른 오류가 함께 나오면 위 대체가 잘못된 것이다.

Task 4의 Step 3까지 마친 뒤 같은 명령을 다시 돌려 PASS(5건)를 확인한다. 이 두 Task는 한 덩이다.

- [x] **Step 5: ktlint를 돌린다**

Run: `./gradlew :feature:segmentation:impl:ktlintCheck`

Expected: BUILD SUCCESSFUL(ktlint는 컴파일과 무관하게 돈다). 실패하면 `ktlintFormat` 후 다시 확인한다.

---

## Task 4: 하한 미달을 차단하고 사용자에게 알린다

**Files:**
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/ToppingEditViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/ToppingEditRoute.kt`

**Interfaces:**
- Consumes: Task 1의 `SubjectCoverage.isLargeEnough`, Task 3의 `Bitmap.measureSubject()`·`Bitmap.trimTo(measure)`.
- Produces: `ToppingEditEffect.SubjectTooSmall` — Route가 Toast로 소비한다.

- [x] **Step 1: 문자열을 더한다**

`feature/segmentation/impl/src/main/res/values/strings.xml`의 `topping_edit_save_failed` 아래에
한 줄을 더한다.

```xml
    <string name="topping_edit_subject_too_small">남은 영역이 너무 작아 저장할 수 없습니다</string>
```

- [x] **Step 2: effect를 더한다**

`ToppingEditViewModel.kt`의 `ToppingEditEffect`에 갈래를 더한다.

```kotlin
sealed interface ToppingEditEffect : UiSideEffect {
    data object LoadFailed : ToppingEditEffect

    data object SaveFailed : ToppingEditEffect

    /**
     * 남은 영역이 하한에 못 미쳐 저장하지 않았다.
     *
     * [SaveFailed] 와 합치지 않는 이유는 사용자가 할 일이 다르기 때문이다 — 저장 실패는 재시도이고
     * 이쪽은 되돌리기다.
     */
    data object SubjectTooSmall : ToppingEditEffect

    data class EditCompleted(val result: ToppingEditResult) : ToppingEditEffect
}
```

- [x] **Step 3: 저장 전에 판정하고 되돌린다**

`ToppingEditViewModel.kt`의 `completeEdit()`에서 `cutout` 생성 다음 두 줄
(`val trimmedCutout = …`)을 아래로 바꾼다.

```kotlin
            val measure = withContext(Dispatchers.Default) { cutout.measureSubject() }

            // 파일을 쓰기 전에 판정한다 — 뒤로 미루면 쓸모없는 캐시 파일 두 장이 남는다
            if (!SubjectCoverage.isLargeEnough(
                    alphaSum = measure.alphaSum,
                    canvasArea = cutout.width.toLong() * cutout.height,
                )
            ) {
                cutout.recycle()
                updateState { copy(isSaving = false) }
                postSideEffect(ToppingEditEffect.SubjectTooSmall)
                return@launch
            }

            // cutout 은 재편집 좌표계를 지키려고 원본 크기를 유지해야 하고, 보여 주고 올릴 알맹이는
            // 투명 여백 없이 실제 토핑 크기여야 한다. 여백이 붙은 채로 올라가면 배치 좌표가 어긋난다
            val trimmedCutout = withContext(Dispatchers.Default) { cutout.trimTo(measure) }
```

import 둘을 바꾼다 — `…impl.editor.trimTransparentBounds`를 지우고
`com.teamyg.parfait.domain.model.SubjectCoverage`·`…impl.editor.measureSubject`·
`…impl.editor.trimTo`를 더한다.

`isSaving` 복구를 빠뜨리면 완료 버튼이 영구히 잠긴 화면이 된다. 되돌아 나가기 전에는 풀 방법이 없다.

- [x] **Step 4: Task 3의 테스트가 이제 통과하는 것을 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMaskTest"`

Expected: PASS (5건). Task 3에서 깨져 있던 컴파일이 여기서 다시 초록이 된다.

- [x] **Step 5: Route에 분기를 더한다**

`ToppingEditRoute.kt`의 `when (effect)`에 갈래를 더한다. `SaveFailed` 바로 아래에 둔다.

```kotlin
                is ToppingEditEffect.SubjectTooSmall -> {
                    val message = context.getString(R.string.topping_edit_subject_too_small)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
```

`LoadFailed`와 달리 **`navigator.onBack()`을 부르지 않는다.** 편집 화면에 남아야 되돌리기로
방금 지운 획을 되살릴 수 있다.

- [x] **Step 6: 모듈 전체 유닛과 ktlint, 앱 빌드를 돌린다**

Run: `./gradlew :domain:test :data:testDebugUnitTest :feature:segmentation:impl:testDebugUnitTest ktlintCheck :app:assembleDebug`

Expected: 전부 BUILD SUCCESSFUL.

---

## Task 5: 실기기 수동 검증

**Files:** 없음(코드 변경 없음).

**Interfaces:**
- Consumes: Task 4까지의 결과물.
- Produces: 검증 결과 보고.

`ToppingEditViewModel`은 `android.graphics.Bitmap`에 묶여 있고 이 저장소에는 Robolectric이 없다.
차단 분기·`isSaving` 복구·effect 전달은 여기서 눈으로 확인한다.

- [ ] **Step 1: 앱을 설치한다**

Run: `./gradlew :app:installDebug`

- [ ] **Step 2: 다섯 갈래를 확인한다**

| # | 조작 | 기대 |
|---|---|---|
| 1 | 누끼 확인 → 사진 편집 → 영역 탭에서 지우기로 전부 지우고 완료 | `남은 영역이 너무 작아 저장할 수 없습니다` Toast가 뜨고 **편집 화면에 남는다** |
| 2 | 1번 직후 되돌리기를 누른 뒤 완료 | 획이 살아나고 확인 화면으로 정상 이동한다 |
| 3 | 아주 작은 조각만 남기고 완료 | 하한 미만이면 같은 Toast, 하한 이상이면 정상 저장 |
| 4 | 손대지 않고 완료 / 정상 편집 후 완료 | 확인 화면으로 이동하고 배치·업로드까지 성립한다 |
| 5 | 테두리만 고치는 진입(`borderOnly`)에서 완료 | 영역을 건드릴 수 없으므로 차단이 걸리지 않는다 |

1번 직후 완료 버튼을 **다시 누를 수 있는지** 함께 본다. 눌리지 않으면 `isSaving`이 잠긴 것이다.

- [ ] **Step 3: 결과를 보고한다**

다섯 갈래의 관측 결과를 그대로 적는다. 어긋난 것이 있으면 어느 단계에서 갈렸는지 함께 남긴다.

---

## Self-Review 결과

**스펙 커버리지**

| 스펙 요구 | 담당 |
|---|---|
| 자동 경로 하한 재사용(5/10000·2,500px·알파 합) | Task 1 |
| 정책을 `domain`으로 올려 두 경로가 같은 것을 봄 | Task 1·2 |
| `data` 두 호출부(필터·bbox 사전 절단) 전환 | Task 2 |
| 한 번의 스캔으로 경계 + 알파 합 | Task 3 |
| 파일을 쓰기 전 차단, 비트맵 회수·`isSaving` 복구 | Task 4 Step 3 |
| `SubjectTooSmall` effect와 `SaveFailed` 분리 | Task 4 Step 2 |
| Toast만 띄우고 화면을 떠나지 않음 | Task 4 Step 5 |
| 문구 `topping_edit_subject_too_small` | Task 4 Step 1 |
| 자동 테스트 3종 | Task 1·2·3 |
| 수동 검증 5갈래 | Task 5 |

**타입 일관성** — `SubjectCoverage.isLargeEnough(alphaSum, canvasArea)`의 인자 이름과 순서가
Task 1 정의, Task 2 필터 호출, Task 4 ViewModel 호출에서 같다. `SubjectMeasure`의 필드 이름은
Task 3 정의와 Task 4 사용처에서 같다.

**알려진 특이점** — Task 3 끝과 Task 4 시작 사이에 `:feature:segmentation:impl` 컴파일이
일시적으로 깨진다. 계획이 그 지점을 명시하고 있으며, 두 Task를 연달아 실행해야 한다.
