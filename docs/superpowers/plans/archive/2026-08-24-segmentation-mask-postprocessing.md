---
id: segmentation-mask-postprocessing
title: 세그멘테이션 마스크 후처리 구현 계획 (3 PR)
status: done
type: work-order
created: 2026-08-24
updated: 2026-08-27
platforms: android
owner: android
related_adr: ADR-0012, ADR-0011
related_spec: segmentation-mask-postprocessing, segmentation-preprocessing
related_code: SegmentationCandidateFilter.kt#filterCandidates, SegmentationCandidate, SegmentationMask.kt#maskSubjectAlpha, AlphaPostProcessor.kt#postProcessAlpha, AlphaComponents.kt#applyAreaOpening, AlphaComposite.kt#applyAlphaInPlace, ArgbExtension.kt#sumArgbAlpha, ImageSegmentationRepositoryImpl#toCandidatePairs, ImageSegmentationRepositoryImpl#buildCandidatePair, ImageSegmentationRepositoryImpl#toForegroundCandidate, ImageSegmentationRepositoryImpl#segmentImage, SegmentationHighlightGeometry.kt#pickCandidateIndex, Logger.kt#repositoryLogger
archived_reason: Task 1~13 이 PR #363(`4da18230`, 2026-08-27)으로 develop 에 머지됐다. Task 14 의 사진 세트 판정은 실기기에서 사람이 하는 일이라 OQ-P-287~295 가 이어받는다
tags: [plan, parfait]
---

# 세그멘테이션 마스크 후처리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ML Kit이 돌려준 마스크에서 잡티 성분을 지우고 경계를 한 겹 다듬어, 후보의 `bounds`가
실제 객체에 붙고 후보 판정이 사각형이 아니라 실제 알파를 보게 한다.

**Architecture:** 판정과 적용을 다른 해상도에서 돌린다. 이진화·연결 요소·area opening은 축소판
비트 마스크에서 하고, 그 결과인 keep-mask만 되올려 **원본 알파에 곱한다.** 경계 모양은 원본 알파가
그대로 만든다. 판정 로직은 전부 `ByteArray`·`BooleanArray` 위 순수 함수라 기기 없이 JVM으로 덮는다.

**Tech Stack:** Kotlin, ML Kit Subject Segmentation, Hilt, kotlin.test + JUnit4(JVM 유닛)

**Spec:** [`parfait/specs/archive/2026-08-24-segmentation-mask-postprocessing.md`](../../specs/archive/2026-08-24-segmentation-mask-postprocessing.md)

> ✅ **Task 1~13 이 develop 에 들어갔다** — PR #363 `4da18230`(2026-08-27). 세 PR 로 나누기로 한
> 스택이 뒤 라운드 둘(알파 정련·커널 취소 확인 전환)까지 함께 접혀 **머지 하나**로 왔다. 계획이
> 지정한 커밋 메시지는 그대로 남아 있고, 실행 중 리뷰가 되돌린 자리(`AlphaCoverage.kt` → `:core`
> 의 `ArgbExtension.kt`, `ceilDiv` 의 `require` 위치)는 아래 각 Task 의 as-built 주석에 이미 적혀
> 있다. **Task 14 는 Step 4~5(문서 이동·커밋)만 이 회차가 수행했다** — Step 1~3 의 사진 세트 판정은
> 실기기에서 사람이 해야 하고, 그 판정을 기다리는 항목은 [open-questions](../../../synthesis/open-questions.md)
> 의 OQ-P-287~295 로 남아 있다. 근거 등급 표의 조건부 항목을 아직 아무도 철회하지 않았다.

## Global Constraints

- **작업 저장소는 `TJYG-Android`다**(이 계획 문서가 있는 repo와 다르다). 로컬 절대경로는 private
  submodule의 `wiki/personal-private/project-paths.md`에 있다.
- **세 PR 모두 베이스는 `develop`이다.** PR #349 위에 스택하지 않는다 — 파일 교집합이 0이다.
- **1단계와 2단계는 서로 독립이라 병렬로 열 수 있다. 3단계는 둘이 머지된 뒤에 연다.**
  ⚠️ **순서를 바꾸면 회귀다.** 현행 `MIN_SUBJECT_AREA_RATIO`는 bounds 사각형으로 재므로, 커널을
  먼저 배선해 bounds를 tight로 줄이면 **구석 잡티 덕에 bounds가 부풀어 있어 지금 통과하던 후보가
  탈락한다.** 그 회귀를 막는 커버리지 판정이 1단계다.
- **커밋만 하고 push·PR은 하지 않는다.** 사용자가 명시적으로 요청할 때까지 리모트로 내보내지 않는다.
- **매 태스크는 커밋으로 끝나고, 커밋 직전에 `./gradlew test ktlintCheck :app:assembleDebug`가
  통과해야 한다. 예외는 없다.** 태스크 경계를 그렇게 잡아 두었다 — 시그니처가 바뀌는 변경은 호출부
  수정까지 한 태스크에 들어 있다.
- ⚠️ **ktlint가 파라미터 2개 이상인 함수 시그니처를 멀티라인으로 강제한다**(`.editorconfig`의
  `ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than = 2`).
  짧아 보여도 한 줄로 쓰면 `ktlintCheck`가 깨진다. 막히면 `./gradlew ktlintFormat`으로 먼저 편다.
- **주석 규약**: 코드가 이미 말하는 것은 쓰지 않는다. `@return`·`@param`은 타입·이름이 말하지 못할
  때만 쓴다. **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다 — 근거는 문서 포인터로 적는다).
  아키텍처 결정은 코드가 아니라 문서에 쓰고 코드에는 포인터 한 줄만 둔다. 주석 분량은 그 코드의
  **어려움**에 비례해야지 **중요함**에 비례하면 안 된다.
- 테스트는 `kotlin.test`를 쓴다. `org.junit.*`을 import하지 않는다. 이름은 `함수명_조건_기대`이고
  본문에 `// Given` `// When` `// Then` 주석을 단다.
- **변환 자체를 위한 단독 테스트를 만들지 않는다.** 판단이 든 순수 함수만 덮는다.
- **이 저장소에 Robolectric이 없고 `testOptions.unitTests.isReturnDefaultValues`도 안 켜져 있다.**
  `Bitmap`이 걸린 코드는 JVM 유닛으로 못 덮는다. 그런 태스크는 **왜 테스트가 없는지를 본문에 적는다.**
- **기존 파일을 "전문"으로 덮어쓰지 않는다.** 기존 주석을 지울 때는 그 정보가 어디에 살아남는지
  확인한다.
- **알파를 `ByteArray`에서 읽을 때는 반드시 `and 0xFF`를 쓴다.** Kotlin `Byte`는 부호가 있어
  128~255가 음수로 읽히고, 그대로 비교하면 **불투명한 절반이 통째로 배경으로 판정된다.**
- **알파 총합은 `Long`으로 누적한다.** 12MP 전면 불투명 후보의 합은 30억을 넘어 `Int`를 벗어난다.
- `minSdk`는 26이다.

---

## 파일 구성

| 파일 | 책임 | 단계 |
|---|---|---|
| `data/.../repository/image/AlphaCoverage.kt` (신설) | 픽셀 배열의 알파 총합 — **#359 리뷰로 `core:util:jvm`의 `extension/ArgbExtension.kt`로 옮겼다**(Task 1 as-built 참고) | 1 |
| `data/src/test/.../repository/image/AlphaCoverageTest.kt` (신설) | 위 함수의 유닛 — **같은 이유로 `ArgbExtensionTest.kt`로 옮겼다** | 1 |
| `domain/.../model/SegmentationCandidate.kt` | `coverageAlphaSum: Long` 추가 | 1 |
| `data/.../repository/image/SegmentationCandidateFilter.kt` | 커버리지 판정·IoU 병합·전순서 정렬 | 1 |
| `data/src/test/.../repository/image/SegmentationCandidateFilterTest.kt` | 위 판정의 유닛 (재작성) | 1 |
| `data/.../repository/image/ImageSegmentationRepositoryImpl.kt` | 두 생성 지점에 커버리지 채우기 | 1 |
| `feature/segmentation/impl/src/test/.../viewmodel/SegmentationViewModelTest.kt` | 후보 생성 두 자리 보정 | 1 |
| `data/.../repository/image/AlphaComponents.kt` (신설) | 축소·런·union-find·area opening·팽창 | 2 |
| `data/src/test/.../repository/image/AlphaComponentsTest.kt` (신설) | 위 함수들의 유닛 | 2 |
| `data/.../repository/image/AlphaPostProcessor.kt` (신설) | 적용·침식·측정·조립 | 2 |
| `data/src/test/.../repository/image/AlphaPostProcessorTest.kt` (신설) | 위 함수들의 유닛 | 2 |
| `data/.../repository/image/SegmentationMask.kt` | 램프 사상 + 커널 호출로 재작성 | 3 |
| `data/src/test/.../repository/image/SegmentationMaskTest.kt` | 재작성 | 3 |
| `data/.../repository/image/ImageSegmentationRepositoryImpl.kt` | 두 경로 배선·전멸 분기·OOM·관측 | 3 |
| `feature/segmentation/impl/src/test/.../component/SegmentationHighlightGeometryTest.kt` | 탭 승자 역전 회귀 | 3 |

**패키지는 전부 `com.teamyg.parfait.data.repository.image`다.** `data` 모듈의 소스 레이아웃은
`src/main/java`·`src/test/java`다.

## 손대는 기존 코드의 실제 형태

⚠️ **아래는 계획이 지어낸 것이 아니라 `develop`에서 그대로 옮긴 것이다.** 태스크의 diff 지시는
이 형태를 전제로 한다.

`SegmentationCandidateFilterTest.kt` — `candidate`는 **클래스 멤버 함수**이고, 파라미터가
`left`·`top`·**`width`·`height`**이며, 캔버스가 상수로 고정되어 호출부에서 못 바꾼다:

```kotlin
private const val CANVAS_SIDE = 100

/** 필터는 비트맵을 보지 않는다 — 자리만 채운다 */
private object FakeBitmap : BitmapWrapper

class SegmentationCandidateFilterTest {
    /** 원본은 100×100(면적 10000)이라, 한 변이 10이면 면적 100 = 정확히 1% 다 */
    private fun candidate(
        left: Int = 0,
        top: Int = 0,
        width: Int = 50,
        height: Int = 50,
    ) = SegmentationCandidate(
        bounds = SegmentationBounds(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        ),
        bitmap = FakeBitmap,
        canvasWidth = CANVAS_SIDE,
        canvasHeight = CANVAS_SIDE,
    )
```

기존 테스트는 일곱이다 — `areaIsExactlyTheThreshold`, `areaIsBelowTheThreshold`,
`everyCandidateIsBelowTheThreshold`, `moreThanTheLimit_keepsTheBiggestOnes`,
`sameArea_ordersByTopThenLeft`, `duplicateBounds_keepsOnlyOne`, `emptyInput_returnsEmpty`.

`SegmentationHighlightGeometry.kt` — `pickCandidateIndex`는 파라미터가 일곱이고 `Int?`를 돌려준다:

```kotlin
internal fun pickCandidateIndex(
    boundsList: List<SegmentationBounds>,
    imageWidth: Int,
    imageHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    tapX: Float,
    tapY: Float,
): Int?
```

`SegmentationHighlightGeometryTest.kt` — 그것을 감싼 헬퍼와 좌표계 상수가 이미 있다. **이미지
100×100을 캔버스 200×400에 Fit으로 그리므로 세로 오프셋이 100이다** — 탭 y가 100 미만이면 레터박스
여백이라 어떤 후보에도 안 걸리고 `null`이 나온다:

```kotlin
private const val IMAGE_SIDE = 100
private const val CANVAS_WIDTH = 200f
private const val CANVAS_HEIGHT = 400f

    private fun pick(
        boundsList: List<SegmentationBounds>,
        tapX: Float,
        tapY: Float,
    ) = pickCandidateIndex(
        boundsList = boundsList,
        imageWidth = IMAGE_SIDE,
        imageHeight = IMAGE_SIDE,
        canvasWidth = CANVAS_WIDTH,
        canvasHeight = CANVAS_HEIGHT,
        tapX = tapX,
        tapY = tapY,
    )
```

`Logger` — `repositoryLogger`는 `data` 모듈의 `internal val`이고 첫 위치 인자가 `throwable`이다:
`fun i(throwable: Throwable? = null, tag: String? = null, message: () -> String)`.
import 경로는 `com.teamyg.parfait.data.utils.repositoryLogger`다.

---

# 1단계 — 필터 판정 (PR 1)

`git checkout develop && git pull && git checkout -b feature/segmentation-candidate-coverage`

## Task 1: 알파 총합 순수 함수

> **as-built(#359 리뷰 반영)** — 아래 작업 지시대로 `data`에 `AlphaCoverage.kt#sumAlpha`를 만들었으나,
> 리뷰 지적을 받아 `core:util:jvm`의 `extension/ArgbExtension.kt`로 옮기고 `IntArray.sumArgbAlpha`
> 확장으로 바꿨다. 알파를 꺼내는 부분은 `Int.argbAlpha`로 갈랐다. 아래 본문은 당시 작업 지시
> 그대로 두고 고치지 않는다.

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaCoverage.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaCoverageTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `internal fun sumAlpha(pixels: IntArray): Long`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`AlphaCoverageTest.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import kotlin.test.Test
import kotlin.test.assertEquals

private const val OPAQUE_WHITE = 0xFFFFFFFF.toInt()
private const val TRANSPARENT = 0

class AlphaCoverageTest {
    @Test
    fun sumAlpha_everyPixelIsOpaque_sumsTo255PerPixel() {
        // Given
        val pixels = IntArray(4) { OPAQUE_WHITE }

        // When
        val sum = sumAlpha(pixels)

        // Then
        assertEquals(4L * 255, sum)
    }

    @Test
    fun sumAlpha_partialAlpha_countsTheActualValue() {
        // Given — 알파 128·64 와 투명 둘
        val pixels = intArrayOf(0x80FFFFFF.toInt(), 0x40FFFFFF, TRANSPARENT, TRANSPARENT)

        // When
        val sum = sumAlpha(pixels)

        // Then
        assertEquals(192L, sum)
    }

    @Test
    fun sumAlpha_wouldOverflowInt_staysCorrectInLong() {
        // Given — Int.MAX_VALUE 를 넘는 합. 12MP 전면 불투명 후보가 이 구간이다
        val pixels = IntArray(10_000_000) { OPAQUE_WHITE }

        // When
        val sum = sumAlpha(pixels)

        // Then
        assertEquals(10_000_000L * 255, sum)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaCoverageTest*"`
Expected: 컴파일 실패 — `Unresolved reference: sumAlpha`

- [ ] **Step 3: 최소 구현을 쓴다**

`AlphaCoverage.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

/**
 * 후보의 "실제 크기"를 재는 지표. 불투명 픽셀 개수가 아니라 알파 총합인 것은, 개수로 세면 소프트
 * 매트 피사체(머리카락·유리)가 과소 계수되고 전 구간이 반투명인 피사체는 0으로 삭제되기 때문이다.
 *
 * `Long` 인 이유: 12MP 전면 불투명 후보의 합이 `Int` 를 넘어 음수로 래핑된다.
 */
internal fun sumAlpha(pixels: IntArray): Long {
    var sum = 0L
    for (pixel in pixels) sum += (pixel ushr 24).toLong()
    return sum
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 후보 커버리지를 재는 알파 총합 함수를 더한다"
```

---

## Task 2: `SegmentationCandidate`에 커버리지 필드를 심는다

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationCandidate.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilterTest.kt`
- Modify: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: `sumAlpha(pixels)` (Task 1)
- Produces: `SegmentationCandidate.coverageAlphaSum: Long`

**기본값을 주지 않는다.** 빠뜨린 생성 지점을 컴파일러가 잡게 한다.

- [ ] **Step 1: 필드를 추가한다**

`SegmentationCandidate.kt`의 `canvasHeight` 아래에 프로퍼티를 하나 더한다:

```kotlin
    val canvasWidth: Int,
    val canvasHeight: Int,
    /**
     * [bitmap] 알파의 총합. 255로 나누면 "실제로 칠해진 픽셀 수"가 된다.
     *
     * 후보 판정이 [bounds] 사각형이 아니라 이 값을 보는 이유는
     * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」에 있다.
     */
    val coverageAlphaSum: Long,
)
```

- [ ] **Step 2: 컴파일 실패로 생성 지점을 전부 찾는다**

Run: `./gradlew :data:compileDebugUnitTestKotlin :feature:segmentation:impl:compileDebugUnitTestKotlin`
Expected: FAIL — `No value passed for parameter 'coverageAlphaSum'`가 **다섯 자리**에서 난다
(`ImageSegmentationRepositoryImpl` 두 곳, `SegmentationCandidateFilterTest` 헬퍼 하나,
`SegmentationViewModelTest` 두 자리).

⚠️ `:data:compileDebugKotlin`만 돌리면 테스트 소스를 안 건드려 헬퍼 실패가 안 드러난다.

- [ ] **Step 3: 주 경로 생성 지점을 채운다**

`ImageSegmentationRepositoryImpl.kt`의 `toCandidates` 안에서 픽셀을 읽어 총합을 낸다.
**기존 주석 세 줄을 그대로 둔다** — 그 함정 정보는 이번 변경으로 사라지지 않는다:

```kotlin
        subjects.mapNotNull { subject ->
            val subjectBitmap = subject.bitmap ?: return@mapNotNull null

            val pixels = IntArray(subjectBitmap.width * subjectBitmap.height)
            subjectBitmap.getPixels(
                pixels,
                0,
                subjectBitmap.width,
                0,
                0,
                subjectBitmap.width,
                subjectBitmap.height,
            )

            SegmentationCandidate(
                // right·bottom 은 exclusive 라 폭·높이를 그대로 더한다.
                // ML Kit 문서는 getWidth()·getHeight() 가 getBitmap() 의 실제 치수와 같다고
                // 보장하지 않으므로, subject 가 아니라 subjectBitmap 에서 치수를 뽑는다
                bounds = SegmentationBounds(
                    left = subject.startX,
                    top = subject.startY,
                    right = subject.startX + subjectBitmap.width,
                    bottom = subject.startY + subjectBitmap.height,
                ),
                bitmap = subjectBitmap.toAndroidBitmap(),
                canvasWidth = origin.width,
                canvasHeight = origin.height,
                coverageAlphaSum = sumAlpha(pixels),
            )
        }
```

- [ ] **Step 4: 폴백 경로 생성 지점을 채운다**

같은 파일 `toForegroundCandidate`의 `SegmentationCandidate(...)` 호출에 한 줄을 더한다. 그 함수에는
이미 `pixels`가 있고 **`maskSubjectPixels`가 그 자리에서 투명으로 지운 뒤**라 그대로 쓰면 된다:

```kotlin
                canvasWidth = width,
                canvasHeight = height,
                coverageAlphaSum = sumAlpha(pixels),
```

- [ ] **Step 5: 테스트 헬퍼를 확장한다**

`SegmentationCandidateFilterTest`의 멤버 함수 `candidate`에 **파라미터 셋을 더한다.**
기존 `width`·`height` 파라미터와 `CANVAS_SIDE` 기본값을 **유지해야** 살아남는 기존 테스트가 안
깨진다. KDoc은 Task 3에서 판정이 바뀌므로 함께 지운다:

```kotlin
    private fun candidate(
        left: Int = 0,
        top: Int = 0,
        width: Int = 50,
        height: Int = 50,
        canvasWidth: Int = CANVAS_SIDE,
        canvasHeight: Int = CANVAS_SIDE,
        coverageAlphaSum: Long = 255L * width * height,
    ) = SegmentationCandidate(
        bounds = SegmentationBounds(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        ),
        bitmap = FakeBitmap,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        coverageAlphaSum = coverageAlphaSum,
    )
```

기본 커버리지를 "bbox를 꽉 채운 불투명 판"으로 두는 이유: 커버리지는 bbox 면적을 넘을 수 없으므로
그 값이 상한이고, 기존 테스트의 전제(사각형 면적)를 그대로 옮겨 놓는다.

- [ ] **Step 6: ViewModel 테스트의 두 자리를 채운다**

`SegmentationViewModelTest.kt`의 `SegmentationCandidate(...)` 두 곳에
`coverageAlphaSum = 255L * 10_000`을 더한다. 그 테스트는 `bounds`를 단언에 안 쓴다.

- [ ] **Step 7: 전체 빌드와 테스트를 돌린다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS (판정 로직은 아직 안 바꿨으므로 기존 테스트가 전부 그대로 통과한다)

- [ ] **Step 8: 커밋한다**

```bash
git add -A
git commit -m "feat: 후보 모델에 커버리지 총합을 싣는다"
```

---

## Task 3: 면적 판정과 정렬을 커버리지로 바꾼다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilter.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilterTest.kt`

**Interfaces:**
- Consumes: `SegmentationCandidate.coverageAlphaSum` (Task 2)
- Produces: `MIN_SUBJECT_COVERAGE_PERMYRIAD`, `MIN_SUBJECT_COVERAGE_PIXELS`,
  `coverageFloorPixels(canvasArea: Long): Long`

⚠️ **새 하한이 기존 픽스처를 통과 못 한다.** 캔버스 100×100에서 하한은
`max(2500, 10000 × 5 / 10000 = 5) = 2500`픽셀, 즉 **캔버스의 25%**다. 그래서 살아남는 기존 테스트
둘(`moreThanTheLimit`·`duplicateBounds`)도 함께 다시 써야 한다.

- [ ] **Step 1: 기존 테스트 넷을 지운다**

`areaIsExactlyTheThreshold`, `areaIsBelowTheThreshold`, `everyCandidateIsBelowTheThreshold`,
`sameArea_ordersByTopThenLeft`를 삭제한다. `emptyInput_returnsEmpty`는 그대로 둔다.

- [ ] **Step 2: 살아남는 기존 테스트 둘을 다시 쓴다**

```kotlin
    @Test
    fun filterCandidates_duplicateBounds_keepsOnlyOne() {
        // Given — 기본 후보는 50×50 이라 커버리지가 정확히 하한(2,500px)이다
        val first = candidate(left = 10, top = 10)
        val second = candidate(left = 10, top = 10)

        // When
        val filtered = filterCandidates(listOf(first, second))

        // Then
        assertEquals(1, filtered.size)
    }

    @Test
    fun filterCandidates_moreThanTheLimit_keepsTheBiggestOnes() {
        // Given — 겹치지 않게 떼어 놓은 여섯 후보. 커버리지 내림차순으로 다섯만 남아야 한다
        val sides = listOf(50, 60, 70, 80, 90, 100)
        val candidates = sides.mapIndexed { index, side ->
            candidate(
                left = index * 200,
                top = 0,
                width = side,
                height = side,
                canvasWidth = 2_000,
                canvasHeight = 1_000,
            )
        }

        // When
        val filtered = filterCandidates(candidates)

        // Then
        assertEquals(listOf(100, 90, 80, 70, 60), filtered.map { it.bounds.width })
    }
```

- [ ] **Step 3: 새 테스트 여섯을 더한다**

```kotlin
    @Test
    fun filterCandidates_coverageIsExactlyTheFloor_keepsIt() {
        // Given — 캔버스 1,000,000px 이면 하한은 max(2500, 500) = 2500px
        val exactly = candidate(
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 2_500,
        )

        // When
        val filtered = filterCandidates(listOf(exactly))

        // Then
        assertEquals(listOf(exactly), filtered)
    }

    @Test
    fun filterCandidates_coverageIsBelowTheFloor_dropsIt() {
        // Given
        val below = candidate(
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 2_499,
        )

        // When
        val filtered = filterCandidates(listOf(below))

        // Then
        assertEquals(emptyList(), filtered)
    }

    @Test
    fun filterCandidates_bigCanvas_ratioFloorTakesOverTheAbsoluteFloor() {
        // Given — 12,000,000px 캔버스에서 비율 하한은 6,000px 이라 절대 하한 2,500 을 넘는다.
        // 둘의 bounds 를 떼어 놓아야 중복 판정에 걸리지 않는다
        val below = candidate(
            left = 0,
            width = 100,
            height = 100,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 5_999,
        )
        val above = candidate(
            left = 500,
            width = 100,
            height = 100,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 6_000,
        )

        // When
        val filtered = filterCandidates(listOf(below, above))

        // Then
        assertEquals(listOf(above), filtered)
    }

    @Test
    fun filterCandidates_thinButLargeSubject_survivesWhileTinyFragmentDoesNot() {
        // Given — 같은 큰 사각형을 차지하지만 하나는 알맹이가 있고 하나는 파편이다
        val pen = candidate(
            width = 1_000,
            height = 1_000,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 10_000,
        )
        val fragment = candidate(
            width = 900,
            height = 900,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 300,
        )

        // When
        val filtered = filterCandidates(listOf(fragment, pen))

        // Then
        assertEquals(listOf(pen), filtered)
    }

    @Test
    fun filterCandidates_twelveMegapixelOpaqueSubject_survivesTheIntBoundary() {
        // Given — 12MP 전면 불투명 후보의 알파 총합은 30억이라 Int 를 넘는다.
        // Int 로 누적하면 음수로 래핑되어 가장 큰 피사체가 조용히 삭제된다
        val fullFrame = candidate(
            width = 4_000,
            height = 3_000,
            canvasWidth = 4_000,
            canvasHeight = 3_000,
            coverageAlphaSum = 255L * 12_000_000,
        )

        // When
        val filtered = filterCandidates(listOf(fullFrame))

        // Then
        assertEquals(listOf(fullFrame), filtered)
    }

    @Test
    fun filterCandidates_sameCoverage_ordersByTopThenLeftThenBottomThenRight() {
        // Given — 커버리지가 같고 좌상단도 같은데 크기가 다른 쌍을 섞는다
        val shorter = candidate(
            left = 1,
            top = 1,
            width = 60,
            height = 60,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 3_000,
        )
        val taller = candidate(
            left = 1,
            top = 1,
            width = 60,
            height = 70,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 3_000,
        )
        val lower = candidate(
            left = 1,
            top = 9,
            width = 60,
            height = 70,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 3_000,
        )

        // When
        val filtered = filterCandidates(listOf(lower, taller, shorter))

        // Then
        assertEquals(listOf(shorter, taller, lower), filtered)
    }
```

⚠️ 세 후보의 IoU가 각각 0.857·0.667·0.795라 Task 4의 병합 임계 0.9 아래다. 값을 바꾸면 그 여유를
다시 계산해야 한다.

- [ ] **Step 4: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*SegmentationCandidateFilterTest*"`
Expected: FAIL — 아직 bounds 면적으로 재므로 커버리지 기반 단언이 어긋난다

- [ ] **Step 5: 판정과 정렬을 구현한다**

`SegmentationCandidateFilter.kt`에서 `MIN_SUBJECT_AREA_RATIO`와 그 KDoc, `area` 확장 프로퍼티와
그 KDoc을 **삭제하고** 아래로 갈아 넣는다. 사각형으로 재는 이유를 적은 주석이 남으면 거짓 주석이
된다:

```kotlin
/** 캔버스 면적 대비 이 비율 **미만** 커버리지의 후보는 버린다 (만분율) */
internal const val MIN_SUBJECT_COVERAGE_PERMYRIAD = 5L

/** 작은 사진에서 비율만으로는 너무 헐거워지므로 두는 하한 (원본 픽셀) */
internal const val MIN_SUBJECT_COVERAGE_PIXELS = 2_500L

internal const val MAX_SUBJECT_COUNT = 5

/**
 * 후보가 넘어야 하는 "실제로 칠해진 픽셀 수".
 *
 * 값의 근거와 이 지표를 고른 이유는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」에 있다.
 */
internal fun coverageFloorPixels(canvasArea: Long): Long =
    maxOf(MIN_SUBJECT_COVERAGE_PIXELS, canvasArea * MIN_SUBJECT_COVERAGE_PERMYRIAD / 10_000L)

/**
 * 정렬이 결정적이어야 하는 이유가 둘이다 — 테스트가 ML Kit 반환 순서에 흔들리지 않아야 하고,
 * 탭 판정이 목록 순서를 근거로 삼지 않더라도 상한 절단 결과가 매번 같아야 한다.
 *
 * `top`·`left` 만으로는 전순서가 아니다(좌상단이 같고 크기가 다른 조합이 성립한다). 뒤 두 키가
 * 없으면 동률에서 순서가 ML Kit 반환 순서를 따라간다.
 */
private val candidateOrder = compareByDescending<SegmentationCandidate> { it.coverageAlphaSum }
    .thenBy { it.bounds.top }
    .thenBy { it.bounds.left }
    .thenBy { it.bounds.bottom }
    .thenBy { it.bounds.right }

private fun SegmentationCandidate.isLargeEnough(): Boolean {
    val canvasArea = canvasWidth.toLong() * canvasHeight
    if (canvasArea <= 0L) return false

    // coverage = coverageAlphaSum / 255 이므로 양변에 255를 곱해 부동소수를 거치지 않는다
    return coverageAlphaSum >= 255L * coverageFloorPixels(canvasArea)
}
```

`filterCandidates` 본문을 아래로 바꾼다(중복 병합은 Task 4에서 넣으므로 이번에는
`distinctBy { it.bounds }`를 그대로 둔다):

```kotlin
internal fun filterCandidates(candidates: List<SegmentationCandidate>): List<SegmentationCandidate> = candidates
    .distinctBy { it.bounds }
    .filter { it.isLargeEnough() }
    .sortedWith(candidateOrder)
    .take(MAX_SUBJECT_COUNT)
```

- [ ] **Step 6: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 7: 커밋한다**

```bash
git add -A
git commit -m "feat: 후보 면적 판정을 사각형에서 커버리지로 옮긴다"
```

---

## Task 4: 중복 판정을 IoU 병합으로 바꾼다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilter.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilterTest.kt`

**Interfaces:**
- Consumes: `candidateOrder`, `coverageFloorPixels` (Task 3)
- Produces: `DUPLICATE_IOU_PERMYRIAD`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun filterCandidates_iouAboveTheThreshold_keepsTheOneWithMoreCoverage() {
        // Given — 100×100 과 96×96(안쪽으로 2px). IoU = 9216 / 10000 = 0.9216
        val bigger = candidate(
            width = 100,
            height = 100,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 9_000,
        )
        val nearlySame = candidate(
            left = 2,
            top = 2,
            width = 96,
            height = 96,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 8_000,
        )

        // When
        val filtered = filterCandidates(listOf(nearlySame, bigger))

        // Then
        assertEquals(listOf(bigger), filtered)
    }

    @Test
    fun filterCandidates_iouBelowTheThreshold_keepsBoth() {
        // Given — 100×100 과 94×94(안쪽으로 3px). IoU = 8836 / 10000 = 0.8836
        val bigger = candidate(
            width = 100,
            height = 100,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 9_000,
        )
        val overlapping = candidate(
            left = 3,
            top = 3,
            width = 94,
            height = 94,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 8_000,
        )

        // When
        val filtered = filterCandidates(listOf(overlapping, bigger))

        // Then
        assertEquals(listOf(bigger, overlapping), filtered)
    }

    @Test
    fun filterCandidates_smallSubjectInsideABigOne_keepsBoth() {
        // Given — 사람 안의 든 물건. 포함 관계는 IoU 가 낮아 병합되지 않고, 그게 의도다
        val person = candidate(
            width = 300,
            height = 900,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 100_000,
        )
        val heldItem = candidate(
            left = 100,
            top = 400,
            width = 80,
            height = 80,
            canvasWidth = 1_000,
            canvasHeight = 1_000,
            coverageAlphaSum = 255L * 5_000,
        )

        // When
        val filtered = filterCandidates(listOf(heldItem, person))

        // Then
        assertEquals(listOf(person, heldItem), filtered)
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*SegmentationCandidateFilterTest*"`
Expected: FAIL — `iouAboveTheThreshold` 가 후보 둘을 다 남긴다

- [ ] **Step 3: 병합을 구현한다**

```kotlin
/** 이 값 **이상** 겹치는 후보 쌍은 같은 것으로 본다 (만분율) */
internal const val DUPLICATE_IOU_PERMYRIAD = 9_000L

internal fun filterCandidates(candidates: List<SegmentationCandidate>): List<SegmentationCandidate> = candidates
    .filter { it.isLargeEnough() }
    .sortedWith(candidateOrder)
    .dropNearDuplicates()
    .take(MAX_SUBJECT_COUNT)

/**
 * 앞에서부터 훑으며 이미 채택한 것과 크게 겹치는 후보를 버린다. 정렬이 전순서라 결과가 매번 같다.
 *
 * ⚠️ **포함 관계는 병합하지 않는다.** 교집합을 작은 쪽 면적으로 나누는 지표로 바꾸면 사람이 든
 * 물건이 지워진다. 그 판단의 근거는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「필터 판정」에 있다.
 */
private fun List<SegmentationCandidate>.dropNearDuplicates(): List<SegmentationCandidate> {
    val kept = mutableListOf<SegmentationCandidate>()
    for (candidate in this) {
        if (kept.none { it.bounds.overlapsAsDuplicate(candidate.bounds) }) kept += candidate
    }
    return kept
}

private fun SegmentationBounds.overlapsAsDuplicate(other: SegmentationBounds): Boolean {
    val overlapWidth = minOf(right, other.right) - maxOf(left, other.left)
    val overlapHeight = minOf(bottom, other.bottom) - maxOf(top, other.top)
    if (overlapWidth <= 0 || overlapHeight <= 0) return false

    val intersection = overlapWidth.toLong() * overlapHeight
    val union = width.toLong() * height + other.width.toLong() * other.height - intersection
    if (union <= 0L) return false

    return intersection * 10_000L >= DUPLICATE_IOU_PERMYRIAD * union
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS. `duplicateBounds_keepsOnlyOne`은 IoU 1이라 병합되고 `emptyInput_returnsEmpty`는
그대로다.

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 거의 같은 후보를 IoU 로 합친다"
```

---

# 2단계 — 순수 커널 (PR 2)

`git checkout develop && git checkout -b feature/segmentation-alpha-kernel`

이 단계는 **호출부를 만들지 않는다.** 3단계가 같은 스프린트에 붙는 것을 전제로 감수한다.
⚠️ **"아직 아무도 안 부른다"는 주석을 달지 않는다** — 3단계 머지 즉시 거짓이 되고, 그것은
`parfait/CLAUDE.md`가 금지하는 "다른 컴포넌트의 현재 상태" 서술이다. 이 단계의 모든 `internal`
함수는 같은 PR의 테스트가 호출하므로 CI에서 미사용으로 걸리지 않는다.

## Task 5: 이진화와 축소

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaComponents.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaComponentsTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `ceilDiv(value, divisor)`, `downscaleMask(alpha, width, height, factor, threshold, checkCancelled)`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`AlphaComponentsTest.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** `#` 는 불투명, `.` 은 투명. 한 줄이 한 행이다 */
private fun alphaOf(vararg rows: String): ByteArray {
    val flat = rows.joinToString(separator = "")
    return ByteArray(flat.length) { if (flat[it] == '#') 255.toByte() else 0 }
}

class AlphaComponentsTest {
    @Test
    fun downscaleMask_factorFour_orsEachBlock() {
        // Given — 8×8 에서 왼쪽 위 블록에 한 점만 있다
        val alpha = alphaOf(
            "#.......",
            "........",
            "........",
            "........",
            "........",
            "........",
            "........",
            "........",
        )

        // When
        val mask = downscaleMask(alpha, width = 8, height = 8, factor = 4, threshold = 127)

        // Then — 2×2 축소판에서 왼쪽 위만 참이다
        assertContentEquals(booleanArrayOf(true, false, false, false), mask)
    }

    @Test
    fun downscaleMask_alphaAbove127_readsAsForegroundDespiteSignedByte() {
        // Given — 128 은 Byte 로 담으면 음수다. and 0xFF 가 없으면 배경으로 오판한다
        val alpha = ByteArray(1) { 128.toByte() }

        // When
        val mask = downscaleMask(alpha, width = 1, height = 1, factor = 1, threshold = 127)

        // Then
        assertContentEquals(booleanArrayOf(true), mask)
    }

    @Test
    fun downscaleMask_alphaExactlyAtThreshold_readsAsBackground() {
        // Given
        val alpha = ByteArray(1) { 127.toByte() }

        // When
        val mask = downscaleMask(alpha, width = 1, height = 1, factor = 1, threshold = 127)

        // Then
        assertContentEquals(booleanArrayOf(false), mask)
    }

    @Test
    fun downscaleMask_sizeIsNotAMultipleOfFactor_keepsTheTrailingEdge() {
        // Given — 5×1 에서 마지막 픽셀만 불투명하다. 내림하면 그 픽셀이 판정에서 빠진다
        val alpha = alphaOf("....#")

        // When
        val mask = downscaleMask(alpha, width = 5, height = 1, factor = 4, threshold = 127)

        // Then — 축소판 폭은 2 이고 두 번째 칸이 참이다
        assertEquals(2, mask.size)
        assertContentEquals(booleanArrayOf(false, true), mask)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaComponentsTest*"`
Expected: 컴파일 실패 — `Unresolved reference: downscaleMask`

- [ ] **Step 3: 구현한다**

`AlphaComponents.kt`:

> **as-built(#360 리뷰 반영)** — `ceilDiv` 는 아래 지시대로 만들었으나, 리뷰 지적을 받아
> `require(divisor > 0)` 과 KDoc 을 더했다. 0 을 돌려주고 넘어가는 방식은 쓰지 않았다 —
> `factor` 로 나누는 나머지 세 자리가 그대로라 같은 예외가 더 안쪽으로 밀릴 뿐이다.
> 같은 라운드에서 `AlphaPostProcessOptions` 에도 `require(downscaleFactor >= 1)` 이 붙었다.

```kotlin
package com.teamyg.parfait.data.repository.image

internal fun ceilDiv(
    value: Int,
    divisor: Int,
): Int = (value + divisor - 1) / divisor

/**
 * 알파를 [threshold] 로 이진화하고 [factor] × [factor] 블록마다 OR 해서 축소 마스크를 만든다.
 *
 * 이진화를 먼저 하고 OR 하는 것과 최댓값을 먼저 구하고 이진화하는 것은 결과가 같지만
 * (`max(aᵢ) > t ⇔ ∃i: aᵢ > t`), 이 순서면 축소 버퍼가 알파 바이트가 아니라 참·거짓이면 된다.
 * 평균으로 줄이면 그 동치가 깨지고 1~2픽셀 폭 구조가 축소에서 끊긴다.
 *
 * 치수가 [factor] 의 배수가 아닐 때 올림하는 이유: 내림하면 오른쪽·아래 가장자리가 판정에서
 * 빠져 그 자리 알파가 0이 되고, 프레임에 걸친 피사체의 테두리가 사라진다.
 */
internal fun downscaleMask(
    alpha: ByteArray,
    width: Int,
    height: Int,
    factor: Int,
    threshold: Int,
    checkCancelled: () -> Unit = {},
): BooleanArray {
    val maskWidth = ceilDiv(width, factor)
    val mask = BooleanArray(maskWidth * ceilDiv(height, factor))

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        val maskRowOffset = (y / factor) * maskWidth
        for (x in 0 until width) {
            if ((alpha[rowOffset + x].toInt() and 0xFF) > threshold) {
                mask[maskRowOffset + x / factor] = true
            }
        }
    }

    return mask
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 알파를 이진화해 축소 판정 마스크를 만든다"
```

---

## Task 6: 8-근방 연결 요소와 area opening

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaComponents.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaComponentsTest.kt`

**Interfaces:**
- Consumes: `downscaleMask` (Task 5)
- Produces: `applyAreaOpening(mask, width, height, minPixels, checkCancelled): Boolean`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

파일 최상위에 헬퍼 둘을 더한다:

```kotlin
/** `#` 는 참, `.` 은 거짓 */
private fun maskOf(vararg rows: String): BooleanArray {
    val flat = rows.joinToString(separator = "")
    return BooleanArray(flat.length) { flat[it] == '#' }
}

private fun BooleanArray.render(width: Int): String =
    toList().chunked(width).joinToString(separator = "\n") { row ->
        row.joinToString(separator = "") { if (it) "#" else "." }
    }
```

클래스 안에 테스트 여섯을 더한다:

```kotlin
    @Test
    fun applyAreaOpening_tinyComponentBesideABigOne_removesOnlyTheTinyOne() {
        // Given — 왼쪽 4×4 덩어리(16px)와 오른쪽 아래 한 점
        val mask = maskOf(
            "####..",
            "####..",
            "####..",
            "####..",
            "......",
            ".....#",
        )

        // When
        val survived = applyAreaOpening(mask, width = 6, height = 6, minPixels = 4)

        // Then
        assertEquals(true, survived)
        assertEquals(
            """
            ####..
            ####..
            ####..
            ####..
            ......
            ......
            """.trimIndent(),
            mask.render(width = 6),
        )
    }

    @Test
    fun applyAreaOpening_componentExactlyAtThreshold_keepsIt() {
        // Given — 정확히 4픽셀짜리 성분 하나
        val mask = maskOf(
            "##..",
            "##..",
            "....",
            "....",
        )

        // When
        val survived = applyAreaOpening(mask, width = 4, height = 4, minPixels = 4)

        // Then
        assertEquals(true, survived)
        assertEquals(4, mask.count { it })
    }

    @Test
    fun applyAreaOpening_componentOnePixelBelowThreshold_removesIt() {
        // Given — 3픽셀짜리 성분 하나
        val mask = maskOf(
            "##..",
            "#...",
            "....",
            "....",
        )

        // When
        val survived = applyAreaOpening(mask, width = 4, height = 4, minPixels = 4)

        // Then
        assertEquals(false, survived)
        assertEquals(0, mask.count { it })
    }

    @Test
    fun applyAreaOpening_blobsTouchingOnlyDiagonally_countAsOneComponent() {
        // Given — 두 2×2 가 대각선으로만 닿는다. 합치면 8픽셀이라 살고, 따로면 각 4픽셀이라 죽는다
        val mask = maskOf(
            "##....",
            "##....",
            "..##..",
            "..##..",
            "......",
            "......",
        )

        // When
        val survived = applyAreaOpening(mask, width = 6, height = 6, minPixels = 5)

        // Then
        assertEquals(true, survived)
        assertEquals(8, mask.count { it })
    }

    @Test
    fun applyAreaOpening_oneRunBridgesTwoRunsAbove_mergesAllThree() {
        // Given — 윗행 두 런을 아랫행 한 런이 잇는다. 첫 매치에서 멈추는 구현이면 갈린다
        val mask = maskOf(
            "#.#.",
            "###.",
            "....",
            "....",
        )

        // When — 전부 한 성분이면 5픽셀이라 산다. 갈리면 어느 조각도 5를 못 넘는다
        val survived = applyAreaOpening(mask, width = 4, height = 4, minPixels = 5)

        // Then
        assertEquals(true, survived)
        assertEquals(5, mask.count { it })
    }

    @Test
    fun applyAreaOpening_everythingIsBackground_reportsNothingSurvived() {
        // Given
        val mask = maskOf("....", "....")

        // When
        val survived = applyAreaOpening(mask, width = 4, height = 2, minPixels = 1)

        // Then
        assertEquals(false, survived)
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaComponentsTest*"`
Expected: 컴파일 실패 — `Unresolved reference: applyAreaOpening`

- [ ] **Step 3: 구현한다**

`AlphaComponents.kt`에 더한다:

```kotlin
/**
 * 픽셀 수가 [minPixels] 미만인 8-연결 성분을 [mask] 에서 그 자리에 지운다.
 *
 * 성분을 행 단위 run-length 로 잡고 union-find 로 잇는다. 픽셀마다 라벨 배열을 두면 마스크와 같은
 * 크기의 `IntArray` 가 하나 더 필요한데, 런 개수는 통상 그보다 몇 자릿수 적다.
 *
 * 8-근방인 이유: 4-근방이면 대각선 1픽셀 가닥(머리카락·줄)이 본체와 끊겨 크기 1인 성분 여럿이
 * 되고 이 함수가 전부 지운다. 대각선 잡티 하나를 더 지우려다 대각선 구조 전체를 잃는 교환이다.
 *
 * @return 살아남은 성분이 하나라도 있으면 true
 */
internal fun applyAreaOpening(
    mask: BooleanArray,
    width: Int,
    height: Int,
    minPixels: Int,
    checkCancelled: () -> Unit = {},
): Boolean {
    val runCount = countRuns(mask, width, height)
    if (runCount == 0) return false

    val runRow = IntArray(runCount)
    val runStart = IntArray(runCount)
    val runEnd = IntArray(runCount)
    // rowFirstRun[y] 는 행 y 의 첫 런 인덱스다. 마지막 칸이 전체 런 개수라 y+1 을 안전하게 읽는다
    val rowFirstRun = IntArray(height + 1)
    fillRuns(mask, width, height, runRow, runStart, runEnd, rowFirstRun)

    val parent = IntArray(runCount) { it }
    unionAdjacentRows(height, runStart, runEnd, rowFirstRun, parent, checkCancelled)

    val componentPixels = IntArray(runCount)
    for (run in 0 until runCount) {
        componentPixels[findRoot(parent, run)] += runEnd[run] - runStart[run]
    }

    var survived = false
    for (run in 0 until runCount) {
        if (componentPixels[findRoot(parent, run)] >= minPixels) {
            survived = true
            continue
        }
        val rowOffset = runRow[run] * width
        for (x in runStart[run] until runEnd[run]) mask[rowOffset + x] = false
    }

    return survived
}

private fun countRuns(
    mask: BooleanArray,
    width: Int,
    height: Int,
): Int {
    var count = 0
    for (y in 0 until height) {
        val rowOffset = y * width
        var x = 0
        while (x < width) {
            if (!mask[rowOffset + x]) {
                x++
                continue
            }
            count++
            while (x < width && mask[rowOffset + x]) x++
        }
    }
    return count
}

private fun fillRuns(
    mask: BooleanArray,
    width: Int,
    height: Int,
    runRow: IntArray,
    runStart: IntArray,
    runEnd: IntArray,
    rowFirstRun: IntArray,
) {
    var run = 0
    for (y in 0 until height) {
        rowFirstRun[y] = run
        val rowOffset = y * width
        var x = 0
        while (x < width) {
            if (!mask[rowOffset + x]) {
                x++
                continue
            }
            val start = x
            while (x < width && mask[rowOffset + x]) x++
            runRow[run] = y
            runStart[run] = start
            runEnd[run] = x
            run++
        }
    }
    rowFirstRun[height] = run
}

/**
 * 인접한 두 행의 런을 투 포인터로 훑어 잇는다.
 *
 * ⚠️ **한 런은 윗행의 겹치는 런 전부와 이어야 한다.** 첫 매치에서 멈추면 윗행 두 런을 아랫행 한
 * 런이 잇는 배치에서 성분이 갈린다. 그래서 조건이 맞아도 포인터를 멈추지 않고, 끝이 작은 쪽만
 * 전진시킨다.
 *
 * `xEnd` 가 exclusive 라 8-근방 겹침은 `aStart <= bEnd && bStart <= aEnd` 다.
 */
private fun unionAdjacentRows(
    height: Int,
    runStart: IntArray,
    runEnd: IntArray,
    rowFirstRun: IntArray,
    parent: IntArray,
    checkCancelled: () -> Unit,
) {
    for (y in 0 until height - 1) {
        checkCancelled()
        var upper = rowFirstRun[y]
        var lower = rowFirstRun[y + 1]
        val upperEnd = rowFirstRun[y + 1]
        val lowerEnd = rowFirstRun[y + 2]

        while (upper < upperEnd && lower < lowerEnd) {
            val touching = runStart[upper] <= runEnd[lower] && runStart[lower] <= runEnd[upper]
            if (touching) union(parent, upper, lower)

            if (runEnd[upper] < runEnd[lower]) upper++ else lower++
        }
    }
}

private fun findRoot(
    parent: IntArray,
    node: Int,
): Int {
    var root = node
    while (parent[root] != root) root = parent[root]

    var cursor = node
    while (parent[cursor] != root) {
        val next = parent[cursor]
        parent[cursor] = root
        cursor = next
    }

    return root
}

private fun union(
    parent: IntArray,
    left: Int,
    right: Int,
) {
    val leftRoot = findRoot(parent, left)
    val rightRoot = findRoot(parent, right)
    if (leftRoot != rightRoot) parent[rightRoot] = leftRoot
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 잡티 성분을 area opening 으로 지운다"
```

---

## Task 7: keep-mask 팽창

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaComponents.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaComponentsTest.kt`

**Interfaces:**
- Consumes: `applyAreaOpening` (Task 6)
- Produces: `dilateMask(mask, width, height, checkCancelled): BooleanArray`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun dilateMask_singlePixel_growsToThreeByThree() {
        // Given
        val mask = maskOf(
            ".....",
            ".....",
            "..#..",
            ".....",
            ".....",
        )

        // When
        val dilated = dilateMask(mask, width = 5, height = 5)

        // Then
        assertEquals(
            """
            .....
            .###.
            .###.
            .###.
            .....
            """.trimIndent(),
            dilated.render(width = 5),
        )
    }

    @Test
    fun dilateMask_pixelAtTheCorner_doesNotWrapAround() {
        // Given
        val mask = maskOf(
            "#..",
            "...",
            "...",
        )

        // When
        val dilated = dilateMask(mask, width = 3, height = 3)

        // Then
        assertEquals(
            """
            ##.
            ##.
            ...
            """.trimIndent(),
            dilated.render(width = 3),
        )
    }

    @Test
    fun dilateMask_componentRemovedByAreaOpening_staysRemoved() {
        // Given — 지워진 소성분이 살아남은 성분과 체비쇼프 거리 2 다. 8-연결이라 그보다 가까울 수 없다
        val mask = maskOf(
            "###..#",
            "###...",
            "###...",
            "......",
        )
        applyAreaOpening(mask, width = 6, height = 4, minPixels = 4)

        // When
        val dilated = dilateMask(mask, width = 6, height = 4)

        // Then — 오른쪽 위 한 점이 부활하지 않는다
        assertEquals(false, dilated[5])
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaComponentsTest*"`
Expected: 컴파일 실패 — `Unresolved reference: dilateMask`

- [ ] **Step 3: 구현한다**

```kotlin
/**
 * 8-근방으로 1픽셀 팽창한 새 마스크를 돌려준다.
 *
 * 되올리기 전에 축소판에서 팽창시키는 이유는 훑을 픽셀이 배율 제곱만큼 적어서다. 팽창 자체가
 * 필요한 이유는, 없으면 성분 경계 바로 바깥의 원본 알파가 블록 경계에서 계단처럼 잘려서다.
 *
 * 반경 1인 것이 계약이다 — 8-연결 성분끼리는 최소 거리가 2라, 반경 1 팽창은 area opening 이
 * 지운 성분을 되살릴 수 없다. 반경을 키우면 그 불변식이 깨진다.
 */
internal fun dilateMask(
    mask: BooleanArray,
    width: Int,
    height: Int,
    checkCancelled: () -> Unit = {},
): BooleanArray {
    val dilated = BooleanArray(mask.size)

    for (y in 0 until height) {
        checkCancelled()
        for (x in 0 until width) {
            if (!mask[y * width + x]) continue

            for (neighborY in maxOf(0, y - 1)..minOf(height - 1, y + 1)) {
                for (neighborX in maxOf(0, x - 1)..minOf(width - 1, x + 1)) {
                    dilated[neighborY * width + neighborX] = true
                }
            }
        }
    }

    return dilated
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: keep 마스크를 한 픽셀 팽창시킨다"
```

---

## Task 8: keep-mask 적용과 알파 측정

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessor.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessorTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `applyKeepMask(alpha, width, height, keep, maskWidth, factor, checkCancelled): Boolean`,
  `AlphaMeasurement(bounds, alphaSum, partialAlphaPixels)`,
  `measureAlpha(alpha, width, height, checkCancelled): AlphaMeasurement?`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`AlphaPostProcessorTest.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** 각 값이 알파 하나. 행 구분은 호출부가 width 로 준다 */
private fun alphaBytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

private fun ByteArray.asInts() = IntArray(size) { this[it].toInt() and 0xFF }

class AlphaPostProcessorTest {
    @Test
    fun applyKeepMask_maskIsFalse_clearsThatBlock() {
        // Given — 2×2 원본, 배율 2 라 축소판은 1픽셀이다
        val alpha = alphaBytes(255, 255, 255, 255)
        val keep = booleanArrayOf(false)

        // When
        val changed = applyKeepMask(alpha, width = 2, height = 2, keep = keep, maskWidth = 1, factor = 2)

        // Then
        assertContentEquals(intArrayOf(0, 0, 0, 0), alpha.asInts())
        assertEquals(true, changed)
    }

    @Test
    fun applyKeepMask_maskIsTrue_leavesTheOriginalAlphaUntouched() {
        // Given — 이진화 결과로 알파를 덮어쓰지 않는 것이 이 설계의 요점이다
        val alpha = alphaBytes(10, 200, 255, 0)
        val keep = booleanArrayOf(true)

        // When
        val changed = applyKeepMask(alpha, width = 2, height = 2, keep = keep, maskWidth = 1, factor = 2)

        // Then
        assertContentEquals(intArrayOf(10, 200, 255, 0), alpha.asInts())
        assertEquals(false, changed)
    }

    @Test
    fun measureAlpha_someOpaquePixels_returnsTightBoundsAndCoverage() {
        // Given — 4×3 에서 가운데 두 픽셀만 남았다
        val alpha = alphaBytes(
            0, 0, 0, 0,
            0, 100, 255, 0,
            0, 0, 0, 0,
        )

        // When
        val measured = measureAlpha(alpha, width = 4, height = 3)

        // Then — right·bottom 은 마지막 픽셀을 포함하도록 exclusive 다
        assertEquals(SegmentationBounds(left = 1, top = 1, right = 3, bottom = 2), measured?.bounds)
        assertEquals(355L, measured?.alphaSum)
        assertEquals(1, measured?.partialAlphaPixels)
    }

    @Test
    fun measureAlpha_everythingIsTransparent_returnsNull() {
        // Given
        val alpha = alphaBytes(0, 0, 0, 0)

        // When
        val measured = measureAlpha(alpha, width = 2, height = 2)

        // Then
        assertNull(measured)
    }

    @Test
    fun measureAlpha_alphaAbove127_isNotMisreadAsNegative() {
        // Given — 부호 처리를 빠뜨리면 합이 음수가 되고 bounds 도 안 잡힌다
        val alpha = alphaBytes(200, 255)

        // When
        val measured = measureAlpha(alpha, width = 2, height = 1)

        // Then
        assertEquals(455L, measured?.alphaSum)
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 2, bottom = 1), measured?.bounds)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaPostProcessorTest*"`
Expected: 컴파일 실패 — `Unresolved reference: applyKeepMask`

- [ ] **Step 3: 구현한다**

`AlphaPostProcessor.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds

private const val OPAQUE = 255

/**
 * [keep] 이 거짓인 자리의 알파를 0으로 만든다. 참인 자리는 **원본 알파를 그대로 둔다.**
 *
 * 판정 마스크로 알파를 덮어쓰면 ML Kit 이 이미 만들어 둔 부드러운 경계를 깎아먹는다. 임계는
 * 성분 판정에만 쓰고 값은 통과시킨다.
 *
 * 되올리기는 마스크를 실체화하지 않고 축소판 인덱스를 나눗셈으로 직접 읽는다.
 *
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal fun applyKeepMask(
    alpha: ByteArray,
    width: Int,
    height: Int,
    keep: BooleanArray,
    maskWidth: Int,
    factor: Int,
    checkCancelled: () -> Unit = {},
): Boolean {
    var changed = false

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        val maskRowOffset = (y / factor) * maskWidth
        for (x in 0 until width) {
            val index = rowOffset + x
            if ((alpha[index].toInt() and 0xFF) == 0) continue
            if (keep[maskRowOffset + x / factor]) continue

            alpha[index] = 0
            changed = true
        }
    }

    return changed
}

internal data class AlphaMeasurement(
    val bounds: SegmentationBounds,
    val alphaSum: Long,
    /** 알파가 1~254 인 픽셀 수. 관측이 램프 띠 폭과 침식 유효성을 판정하는 데 쓴다 */
    val partialAlphaPixels: Int,
)

/**
 * 남은 알파를 감싸는 사각 영역과 커버리지를 잰다.
 *
 * 불투명 판정을 "알파 0 초과"로 두는 근거는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「tight bounds 판정 기준 통일」에 있다.
 *
 * @return 남은 알파가 없으면 `null`
 */
internal fun measureAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    checkCancelled: () -> Unit = {},
): AlphaMeasurement? {
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = -1
    var bottom = -1
    var sum = 0L
    var partial = 0

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        for (x in 0 until width) {
            val value = alpha[rowOffset + x].toInt() and 0xFF
            if (value == 0) continue

            sum += value
            if (value < OPAQUE) partial++
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }

    if (right < 0) return null

    return AlphaMeasurement(
        // right·bottom 은 마지막 픽셀을 포함하도록 exclusive 로 담는다
        bounds = SegmentationBounds(left = left, top = top, right = right + 1, bottom = bottom + 1),
        alphaSum = sum,
        partialAlphaPixels = partial,
    )
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: keep 마스크를 원본 알파에 적용하고 남은 영역을 잰다"
```

---

## Task 9: 경계 한 겹 침식 (능선 보호)

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessor.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessorTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `erodeEdge(alpha, width, height, checkCancelled): Boolean`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun erodeEdge_ramp_shiftsItInwardByOnePixelWithoutMakingAStep() {
        // Given — 한 행짜리 램프. 제자리에서 돌리면 왼쪽부터 연쇄로 전멸한다
        val alpha = alphaBytes(0, 64, 128, 191, 255, 255)

        // When
        val changed = erodeEdge(alpha, width = 6, height = 1)

        // Then
        assertEquals(true, changed)
        assertContentEquals(intArrayOf(0, 0, 64, 128, 191, 255), alpha.asInts())
    }

    @Test
    fun erodeEdge_hardMatte_stillLosesOneLayer() {
        // Given — 알파가 0 아니면 255 뿐이다. "1~254 만 대상"이면 아무 일도 안 일어난다
        val alpha = alphaBytes(0, 255, 255, 255, 255, 0)

        // When
        erodeEdge(alpha, width = 6, height = 1)

        // Then
        assertContentEquals(intArrayOf(0, 0, 255, 255, 0, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_oneVerticalOpaqueLine_isProtectedByTheRidgeRule() {
        // Given — 폭 1 불투명 선. 좌우가 둘 다 0 이라 능선으로 보호한다
        val alpha = alphaBytes(
            0, 255, 0,
            0, 255, 0,
            0, 255, 0,
        )

        // When
        val changed = erodeEdge(alpha, width = 3, height = 3)

        // Then
        assertEquals(false, changed)
        assertContentEquals(intArrayOf(0, 255, 0, 0, 255, 0, 0, 255, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_oneVerticalPartialAlphaLine_isProtectedToo() {
        // Given — 값이 낮아도 능선이면 안 깎는다
        val alpha = alphaBytes(
            0, 100, 0,
            0, 100, 0,
            0, 100, 0,
        )

        // When
        erodeEdge(alpha, width = 3, height = 3)

        // Then
        assertContentEquals(intArrayOf(0, 100, 0, 0, 100, 0, 0, 100, 0), alpha.asInts())
    }

    @Test
    fun erodeEdge_twoPixelWideBar_disappears() {
        // Given — 양쪽에서 한 겹씩 깎이므로 사라진다. 1픽셀 침식에 내재한 한계다
        val alpha = alphaBytes(
            0, 255, 255, 0,
            0, 255, 255, 0,
            0, 255, 255, 0,
        )

        // When
        erodeEdge(alpha, width = 4, height = 3)

        // Then
        assertEquals(0, alpha.asInts().sum())
    }

    @Test
    fun erodeEdge_subjectTouchingTheFrame_keepsTheEdgeRow() {
        // Given — 판 전체가 불투명하다. 이미지 밖을 투명으로 치면 테두리가 깎인다
        val alpha = alphaBytes(
            255, 255,
            255, 255,
        )

        // When
        val changed = erodeEdge(alpha, width = 2, height = 2)

        // Then
        assertEquals(false, changed)
        assertContentEquals(intArrayOf(255, 255, 255, 255), alpha.asInts())
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaPostProcessorTest*"`
Expected: 컴파일 실패 — `Unresolved reference: erodeEdge`

- [ ] **Step 3: 구현한다**

```kotlin
private const val ABSENT = -1

/**
 * 경계를 한 겹 안으로 깎는다. `a' = min(a, 4-근방 알파의 최소)` 다.
 *
 * ⚠️ **판정은 침식 전 값으로 한다.** 제자리에서 래스터 순서로 돌리면 방금 낮아진 왼쪽 값을 다음
 * 픽셀이 읽어, 부분 알파 띠가 스캔 방향으로만 연쇄 침식된다. 반대 방향은 한 겹만 깎이므로 피사체가
 * 한쪽으로 밀린 것처럼 보인다. 그래서 직전 행의 침식 전 알파 한 줄과 좌측 픽셀의 침식 전 값을
 * 들고 돈다. 오른쪽·아래는 아직 안 고쳤으므로 현재 배열을 그대로 읽는다.
 *
 * **능선 보호**: 마주 보는 4-근방 쌍(좌·우 또는 상·하)이 둘 다 0이면 건너뛴다. 이 조건이 폭 1픽셀
 * 구조 보존·하드 매트 한 겹 제거·램프 단차 없음을 동시에 만족시킨다. 값 범위로 대상을 가르면
 * (예: 1~254) 하드 매트에서 아무 일도 안 일어나고 램프에는 단차가 생긴다.
 *
 * 이미지 밖 이웃은 최소 계산에서도 능선 판정에서도 빠진다. 밖을 투명으로 치면 프레임에 걸친
 * 피사체의 테두리가 깎인다.
 *
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal fun erodeEdge(
    alpha: ByteArray,
    width: Int,
    height: Int,
    checkCancelled: () -> Unit = {},
): Boolean {
    if (width <= 0 || height <= 0) return false

    var previousRow = ByteArray(width)
    var currentRow = ByteArray(width)
    var changed = false

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        alpha.copyInto(currentRow, 0, rowOffset, rowOffset + width)

        var leftBefore = ABSENT
        for (x in 0 until width) {
            val here = currentRow[x].toInt() and 0xFF
            val left = leftBefore
            val right = if (x < width - 1) currentRow[x + 1].toInt() and 0xFF else ABSENT
            val up = if (y > 0) previousRow[x].toInt() and 0xFF else ABSENT
            val down = if (y < height - 1) alpha[rowOffset + width + x].toInt() and 0xFF else ABSENT
            leftBefore = here

            if (here == 0) continue
            if (left == 0 && right == 0) continue
            if (up == 0 && down == 0) continue

            var lowest = here
            if (left in 0 until lowest) lowest = left
            if (right in 0 until lowest) lowest = right
            if (up in 0 until lowest) lowest = up
            if (down in 0 until lowest) lowest = down

            if (lowest != here) {
                alpha[rowOffset + x] = lowest.toByte()
                changed = true
            }
        }

        val spare = previousRow
        previousRow = currentRow
        currentRow = spare
    }

    return changed
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

⚠️ `erodeEdge_ramp_...` 가 `[0,0,0,0,0,255]` 로 나오면 스냅샷을 안 쓰고 제자리 값을 읽은 것이다.
`erodeEdge_hardMatte_...` 가 안 바뀌면 능선 조건이 아니라 값 범위로 대상을 가른 것이다.

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 경계를 한 겹 침식하되 능선은 지킨다"
```

---

## Task 10: 커널 조립

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessor.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessorTest.kt`

**Interfaces:**
- Consumes: `ceilDiv`·`downscaleMask`·`applyAreaOpening`·`dilateMask` (Task 5~7),
  `applyKeepMask`·`measureAlpha`·`erodeEdge` (Task 8~9)
- Produces: `AREA_OPENING_MIN_PIXELS`, `MIN_PIXELS_FOR_DOWNSCALE`, `AlphaPostProcessOptions`,
  `AlphaPostProcessResult`, `postProcessAlpha(alpha, width, height, options, checkCancelled)`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun postProcessAlpha_speckAwayFromTheBlob_isRemovedAndBoundsTightenToTheBlob() {
        // Given — 배율 1 층위. 8×8 에 4×4 덩어리와 떨어진 한 점
        val alpha = ByteArray(64)
        for (y in 0 until 4) for (x in 0 until 4) alpha[y * 8 + x] = 255.toByte()
        alpha[7 * 8 + 7] = 255.toByte()

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4, erodeEdge = false),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 4, bottom = 4), result?.bounds)
        assertEquals(0, alpha[7 * 8 + 7].toInt() and 0xFF)
        assertEquals(true, result?.changed)
    }

    @Test
    fun postProcessAlpha_everyPixelSurvives_reportsNoChangeAndCoversTheWholePlate() {
        // Given — 판 전체가 불투명하다. 침식을 켜도 프레임 테두리는 안 깎인다
        val alpha = ByteArray(64) { 255.toByte() }

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 8, bottom = 8), result?.bounds)
        assertEquals(false, result?.changed)
        assertEquals(64L * 255, result?.alphaSum)
    }

    @Test
    fun postProcessAlpha_everythingIsSpeck_returnsNull() {
        // Given
        val alpha = ByteArray(64)
        alpha[0] = 255.toByte()

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4),
        )

        // Then
        assertNull(result)
    }

    @Test
    fun postProcessAlpha_everythingIsTransparent_returnsNull() {
        // Given
        val alpha = ByteArray(64)

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 1),
        )

        // Then
        assertNull(result)
    }

    @Test
    fun postProcessAlpha_alphaLengthDoesNotMatch_failsFast() {
        // Given — 호출부가 어긋난 배열을 넘기면 엉뚱한 자리를 읽는다. 조용히 틀리지 않게 막는다
        val alpha = ByteArray(10)

        // When · Then
        assertFailsWith<IllegalArgumentException> { postProcessAlpha(alpha, width = 4, height = 4) }
    }

    @Test
    fun postProcessAlpha_cancelledMidway_propagatesTheCallersThrow() {
        // Given — 순수 커널에는 중단점이 없다. 콜백이 유일한 탈출구다
        val alpha = ByteArray(64) { 255.toByte() }
        var calls = 0

        // When · Then
        assertFailsWith<IllegalStateException> {
            postProcessAlpha(
                alpha,
                width = 8,
                height = 8,
                options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4),
            ) {
                calls++
                if (calls > 2) error("cancelled")
            }
        }
    }

    @Test
    fun postProcessAlpha_downscaleFour_keepsTheBlobAndDropsTheDistantSpeck() {
        // Given — 32×32 에 16×16 덩어리와 멀리 떨어진 4×4 점
        val alpha = ByteArray(32 * 32)
        for (y in 0 until 16) for (x in 0 until 16) alpha[y * 32 + x] = 255.toByte()
        for (y in 28 until 32) for (x in 28 until 32) alpha[y * 32 + x] = 255.toByte()

        // When — 원본 환산 임계 64px 이면 축소판 임계는 4px 이다. 4×4 점은 축소판 1px 이라 죽는다
        val result = postProcessAlpha(
            alpha,
            width = 32,
            height = 32,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 64,
                erodeEdge = false,
                minPixelsForDownscale = 0,
            ),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 16, bottom = 16), result?.bounds)
        assertEquals(0, alpha[31 * 32 + 31].toInt() and 0xFF)
    }

    @Test
    fun postProcessAlpha_sameSizeSpeckOnAndOffTheBlockGrid_isCountedDifferently() {
        // Given — 원본 4×4 잡티 둘. 하나는 블록에 정렬되고 하나는 한 칸 어긋났다.
        // OR 풀링이라 어긋난 쪽이 축소판에서 더 넓게 잡힌다 — 위상 슬롭을 여기 고정한다
        val aligned = ByteArray(32 * 32)
        for (y in 0 until 4) for (x in 0 until 4) aligned[y * 32 + x] = 255.toByte()

        val shifted = ByteArray(32 * 32)
        for (y in 1 until 5) for (x in 1 until 5) shifted[y * 32 + x] = 255.toByte()

        val options = AlphaPostProcessOptions(
            downscaleFactor = 4,
            areaOpeningMinPixels = 32,
            erodeEdge = false,
            minPixelsForDownscale = 0,
        )

        // When — 축소판 임계는 32 / 16 = 2 다. 정렬된 잡티는 1블록이라 죽고 어긋난 쪽은 4블록이라 산다
        val alignedResult = postProcessAlpha(aligned, width = 32, height = 32, options = options)
        val shiftedResult = postProcessAlpha(shifted, width = 32, height = 32, options = options)

        // Then
        assertNull(alignedResult)
        assertEquals(SegmentationBounds(left = 1, top = 1, right = 5, bottom = 5), shiftedResult?.bounds)
    }

    @Test
    fun postProcessAlpha_speckCloseToTheBlob_isNotRemovable() {
        // Given — 잡티가 실루엣에서 배율의 두 배 이내다. OR 풀링이 본체와 같은 성분으로 묶는다.
        // 제거할 수 없는 것이 한계이지 결함이 아니라는 사실을 여기 고정한다
        val alpha = ByteArray(32 * 32)
        for (y in 0 until 16) for (x in 0 until 16) alpha[y * 32 + x] = 255.toByte()
        alpha[17] = 255.toByte()

        // When
        val result = postProcessAlpha(
            alpha,
            width = 32,
            height = 32,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 64,
                erodeEdge = false,
                minPixelsForDownscale = 0,
            ),
        )

        // Then — bounds 가 그 한 점까지 늘어난다
        assertEquals(18, result?.bounds?.right)
        assertEquals(255, alpha[17].toInt() and 0xFF)
    }

    @Test
    fun postProcessAlpha_sizeIsNotAMultipleOfFactor_keepsTheTrailingEdgeAlpha() {
        // Given — 33×33 전면 불투명. 축소를 내림하면 오른쪽·아래 한 줄이 판정에서 빠져 0이 된다
        val alpha = ByteArray(33 * 33) { 255.toByte() }

        // When
        val result = postProcessAlpha(
            alpha,
            width = 33,
            height = 33,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 64,
                erodeEdge = false,
                minPixelsForDownscale = 0,
            ),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 33, bottom = 33), result?.bounds)
        assertEquals(255, alpha[33 * 33 - 1].toInt() and 0xFF)
    }

    @Test
    fun postProcessAlpha_belowTheDownscaleFloor_runsAtFactorOne() {
        // Given — 하한 미만이면 배율 1 로 돈다. 축소했다면 이 3픽셀 성분이 한 블록에 뭉쳐 살아남는다
        val alpha = ByteArray(64)
        alpha[0] = 255.toByte()
        alpha[1] = 255.toByte()
        alpha[8] = 255.toByte()

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 4,
                minPixelsForDownscale = 1_000,
            ),
        )

        // Then
        assertNull(result)
    }

    @Test
    fun postProcessAlpha_atTheDownscaleFloor_runsAtTheConfiguredFactor() {
        // Given — 같은 입력인데 하한을 낮춰 축소가 발동하게 만든다
        val alpha = ByteArray(64)
        alpha[0] = 255.toByte()
        alpha[1] = 255.toByte()
        alpha[8] = 255.toByte()

        // When — 배율 4 면 세 픽셀이 한 블록에 뭉쳐 축소판 임계 1 을 넘는다
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = AlphaPostProcessOptions(
                downscaleFactor = 4,
                areaOpeningMinPixels = 4,
                erodeEdge = false,
                minPixelsForDownscale = 64,
            ),
        )

        // Then
        assertEquals(SegmentationBounds(left = 0, top = 0, right = 2, bottom = 2), result?.bounds)
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaPostProcessorTest*"`
Expected: 컴파일 실패 — `Unresolved reference: postProcessAlpha`

- [ ] **Step 3: 구현한다**

```kotlin
/** 원본 픽셀 환산. 이 값 **미만** 크기의 성분은 잡티로 보고 버린다 */
internal const val AREA_OPENING_MIN_PIXELS = 256

/** 판정 버퍼를 줄이지 않는 크기 하한. 이 값 **미만** 픽셀 수의 판은 배율 1로 돈다 */
internal const val MIN_PIXELS_FOR_DOWNSCALE = 2_000_000

internal data class AlphaPostProcessOptions(
    val downscaleFactor: Int = 4,
    val binaryThreshold: Int = 127,
    val areaOpeningMinPixels: Int = AREA_OPENING_MIN_PIXELS,
    val erodeEdge: Boolean = true,
    val minPixelsForDownscale: Int = MIN_PIXELS_FOR_DOWNSCALE,
)

internal data class AlphaPostProcessResult(
    val bounds: SegmentationBounds,
    val alphaSum: Long,
    val partialAlphaPixels: Int,
    /**
     * 거짓이면 알파가 하나도 안 바뀌었다는 뜻이다. 원본 판을 그대로 쓰려면 [bounds] 가 판 전체와
     * 같은지도 함께 봐야 한다 — 알파를 안 바꿔도 원판에 투명 여백이 있으면 판 치수와 [bounds]
     * 치수가 어긋나 `SegmentationCandidate` 의 계약이 깨진다.
     */
    val changed: Boolean,
)

/**
 * [alpha] 를 그 자리에서 다듬고 남은 영역을 돌려준다.
 *
 * 판정(이진화·성분·팽창)은 축소판에서, 적용과 측정은 원본 해상도에서 한다. 축소판이 정하는 것은
 * "이 영역이 살아남는 성분인가"뿐이고 경계 모양은 원본 알파가 그대로 만든다. 근거는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「처리 해상도」.
 *
 * @param alpha 길이가 `width * height` 여야 한다
 * @param checkCancelled 행 경계마다 불린다. 이 함수는 코루틴을 모르므로 호출부가 넣어 준다
 * @return 남은 알파가 없으면 `null`
 */
internal fun postProcessAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    checkCancelled: () -> Unit = {},
): AlphaPostProcessResult? {
    require(alpha.size == width * height) {
        "alpha length ${alpha.size} does not match ${width}x$height"
    }
    if (width <= 0 || height <= 0) return null

    val factor = if (width.toLong() * height < options.minPixelsForDownscale) 1 else options.downscaleFactor
    val maskWidth = ceilDiv(width, factor)
    val maskHeight = ceilDiv(height, factor)

    val mask = downscaleMask(alpha, width, height, factor, options.binaryThreshold, checkCancelled)

    val minComponentPixels = maxOf(1, options.areaOpeningMinPixels / (factor * factor))
    if (!applyAreaOpening(mask, maskWidth, maskHeight, minComponentPixels, checkCancelled)) return null

    val keep = dilateMask(mask, maskWidth, maskHeight, checkCancelled)

    val applied = applyKeepMask(alpha, width, height, keep, maskWidth, factor, checkCancelled)
    val eroded = options.erodeEdge && erodeEdge(alpha, width, height, checkCancelled)
    val measured = measureAlpha(alpha, width, height, checkCancelled) ?: return null

    return AlphaPostProcessResult(
        bounds = measured.bounds,
        alphaSum = measured.alphaSum,
        partialAlphaPixels = measured.partialAlphaPixels,
        changed = applied || eroded,
    )
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 알파 후처리 커널을 조립한다"
```

---

# 3단계 — 배선 (PR 3)

**1단계와 2단계가 `develop`에 머지된 뒤에 시작한다.**
`git checkout develop && git pull && git checkout -b feature/segmentation-postprocess-wiring`

## Task 11: 램프 사상과 폴백 경로 배선

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationMaskTest.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`

**Interfaces:**
- Consumes: `postProcessAlpha`·`AlphaPostProcessOptions`·`AlphaPostProcessResult` (Task 10)
- Produces: `confidenceToAlpha(confidence: Float): Int`, `MaskedAlpha(alpha, result)`,
  `maskSubjectAlpha(mask, width, height, options, checkCancelled): MaskedAlpha?`

⚠️ `maskSubjectPixels`와 `SUBJECT_CONFIDENCE_THRESHOLD`는 **삭제한다.** 함수가 픽셀 배열을 더 이상
받지 않으므로 이름이 거짓이 되고, 상수의 의미는 아래 등가식 주석으로 이관된다. 프로덕션 호출부는
같은 태스크에서 고치는 `toForegroundCandidate` 하나뿐이다.

**커널 배선과 테스트 재작성을 한 태스크에 두는 이유:** 시그니처가 바뀌면 호출부가 같이 안 바뀌고는
빌드가 안 된다. 매 태스크가 커밋으로 끝나야 하므로 둘을 가르지 않는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`SegmentationMaskTest.kt`를 아래로 재작성한다:

```kotlin
package com.teamyg.parfait.data.repository.image

import java.nio.FloatBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private val TEST_OPTIONS = AlphaPostProcessOptions(
    downscaleFactor = 1,
    areaOpeningMinPixels = 4,
    erodeEdge = false,
)

private fun confidenceBuffer(values: FloatArray): FloatBuffer = FloatBuffer.wrap(values)

class SegmentationMaskTest {
    @Test
    fun confidenceToAlpha_atOrBelowTheRampFloor_isFullyTransparent() {
        // Given · When · Then
        assertEquals(0, confidenceToAlpha(0f))
        assertEquals(0, confidenceToAlpha(0.35f))
    }

    @Test
    fun confidenceToAlpha_atOrAboveTheRampCeiling_isFullyOpaque() {
        // Given · When · Then
        assertEquals(255, confidenceToAlpha(0.65f))
        assertEquals(255, confidenceToAlpha(1f))
    }

    @Test
    fun confidenceToAlpha_exactlyAtTheOldThreshold_staysBackground() {
        // Given — 종전 상수는 "이 값을 넘는" 신뢰도만 객체로 봤다. 램프도 그 경계를 지켜야 한다.
        // 알파 > 127 은 신뢰도 0.5 가 아니라 0.35 + 128 × 0.3 / 255 ≈ 0.5006 이다

        // When · Then
        assertEquals(127, confidenceToAlpha(0.5f))
        assertEquals(128, confidenceToAlpha(0.5006f))
    }

    @Test
    fun maskSubjectAlpha_confidentBlobWithASpeck_dropsTheSpeck() {
        // Given — 8×8. 왼쪽 위 4×4 는 확실하고 오른쪽 아래 한 점만 튄다
        val values = FloatArray(64)
        for (y in 0 until 4) for (x in 0 until 4) values[y * 8 + x] = 1f
        values[63] = 1f

        // When
        val masked = maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)

        // Then
        assertEquals(0, masked?.alpha?.get(63)?.toInt()?.and(0xFF))
        assertEquals(4, masked?.result?.bounds?.right)
    }

    @Test
    fun maskSubjectAlpha_everyPixelIsConfident_coversTheWholePlate() {
        // Given — 프레임에 걸친 피사체가 테두리를 잃지 않는지 본다
        val values = FloatArray(64) { 1f }

        // When
        val masked = maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)

        // Then
        assertEquals(8, masked?.result?.bounds?.right)
        assertEquals(8, masked?.result?.bounds?.bottom)
    }

    @Test
    fun maskSubjectAlpha_nothingIsConfident_returnsNull() {
        // Given
        val values = FloatArray(64) { 0.1f }

        // When
        val masked = maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)

        // Then
        assertNull(masked)
    }

    @Test
    fun maskSubjectAlpha_bufferLimitBelowCapacity_stopsAtLimitInsteadOfReadingPastIt() {
        // Given — absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼는다
        val buffer = FloatBuffer.allocate(64).apply { limit(10) }

        // When · Then
        assertFailsWith<IndexOutOfBoundsException> {
            maskSubjectAlpha(buffer, width = 8, height = 8, options = TEST_OPTIONS)
        }
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*SegmentationMaskTest*"`
Expected: 컴파일 실패 — `Unresolved reference: confidenceToAlpha`

- [ ] **Step 3: 램프와 알파 산출을 구현한다**

`SegmentationMask.kt`를 아래로 갈아 넣는다:

```kotlin
package com.teamyg.parfait.data.repository.image

import java.nio.FloatBuffer

/** 이 신뢰도 이하는 완전히 투명하다 */
private const val RAMP_FLOOR = 0.35f

/** 이 신뢰도 이상은 완전히 불투명하다 */
private const val RAMP_CEILING = 0.65f

private const val FULLY_OPAQUE = 255

/**
 * 전경 신뢰도를 알파로 사상한다. 이진 컷 대신 램프를 쓰는 것은 경계 한두 픽셀을 부드럽게 하기
 * 위해서다.
 *
 * ⚠️ 변환은 **버림**이다. 종전 상수가 "이 값을 **넘는**" 신뢰도만 객체로 봤고, 버림이면
 * `알파 > 127 ⇔ 신뢰도 ≥ 0.35 + 128 × 0.3 / 255`(대략 0.5006)라 그 경계가 거의 그대로 옮겨진다.
 * 반올림으로 바꾸면 0.5가 전경이 되어 판정이 뒤집힌다.
 */
internal fun confidenceToAlpha(confidence: Float): Int {
    if (confidence <= RAMP_FLOOR) return 0
    if (confidence >= RAMP_CEILING) return FULLY_OPAQUE

    return (FULLY_OPAQUE * (confidence - RAMP_FLOOR) / (RAMP_CEILING - RAMP_FLOOR)).toInt()
}

internal class MaskedAlpha(
    val alpha: ByteArray,
    val result: AlphaPostProcessResult,
)

/**
 * 전경 신뢰도 마스크에서 후처리까지 끝낸 알파를 만든다.
 *
 * `Bitmap` 을 받지도 돌려주지도 않는 것은 이 판단을 기기 없이 검증하기 위해서다. 호출부는 돌려받은
 * [AlphaPostProcessResult.bounds] 영역만 원본에서 읽어 판을 만들면 된다 — 원본 크기 픽셀 배열을
 * 거칠 이유가 없다.
 *
 * @param mask 픽셀별 전경 신뢰도. 길이가 `width * height` 여야 한다 — 호출부가 검사한다
 * @return 남은 알파가 없으면 `null`
 */
internal fun maskSubjectAlpha(
    mask: FloatBuffer,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    checkCancelled: () -> Unit = {},
): MaskedAlpha? {
    val alpha = ByteArray(width * height)
    for (index in alpha.indices) {
        alpha[index] = confidenceToAlpha(mask[index]).toByte()
    }

    val result = postProcessAlpha(alpha, width, height, options, checkCancelled) ?: return null

    return MaskedAlpha(alpha = alpha, result = result)
}
```

⚠️ `OPAQUE` 라는 이름은 `AlphaPostProcessor.kt` 의 파일 private 상수와 겹치지 않게 `FULLY_OPAQUE`
로 둔다. 둘 다 `private` 이라 충돌은 안 나지만 읽는 사람이 헷갈린다.

- [ ] **Step 4: 폴백 경로를 배선한다**

`ImageSegmentationRepositoryImpl.kt`의 `toForegroundCandidate`에서 기존 본문(픽셀 배열 생성 →
`maskSubjectPixels` → `masked` 비트맵 → 자르기 → `recycle` 분기)을 아래로 치환한다:

```kotlin
    /**
     * 마스크가 없거나 치수가 어긋나면 빈 목록이다 — 없는 후보를 지어내지 않는다.
     */
    private fun SubjectSegmentationResult.toForegroundCandidate(
        origin: Bitmap,
        checkCancelled: () -> Unit,
    ): List<SegmentationCandidate> {
        val foregroundMask = foregroundConfidenceMask ?: return emptyList()

        val width = origin.width
        val height = origin.height

        // InputImage.fromBitmap(bitmap, 0) 이라 지금은 치수가 같지만 그 일치가 계약으로
        // 적혀 있지 않다. 어긋난 채로 읽으면 엉뚱한 자리를 객체로 오려낸다.
        // absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼으므로(넘으면
        // IndexOutOfBoundsException), 남은 유효 구간을 뜻하는 remaining() 으로 비교한다
        if (foregroundMask.remaining() != width * height) return emptyList()

        val masked = try {
            maskSubjectAlpha(foregroundMask, width, height, checkCancelled = checkCancelled)
        } catch (e: OutOfMemoryError) {
            // 후처리는 개선 수단이라 실패했다고 흐름 전체를 실패로 접을 이유가 없다.
            // 기존 catch (e: Exception) 은 Error 를 안 잡으므로 여기서 따로 받는다
            repositoryLogger.w(e) { "세그멘테이션 폴백 후처리가 메모리로 실패했다" }
            null
        } ?: return emptyList()

        repositoryLogger.i {
            "세그멘테이션 폴백 부분 알파 ${masked.result.partialAlphaPixels}/${width * height}"
        }

        val bounds = masked.result.bounds

        // 살아남은 영역만 읽는다. 원본 크기 픽셀 배열과 원본 크기 중간 판을 만들었다가 자르면
        // 12MP 사진에서 그 둘만 100MB 가까이 든다
        val trimmedPixels = IntArray(bounds.width * bounds.height)
        origin.getPixels(
            trimmedPixels,
            0,
            bounds.width,
            bounds.left,
            bounds.top,
            bounds.width,
            bounds.height,
        )
        applyAlphaInPlace(trimmedPixels, masked.alpha, width, bounds)

        val trimmed = Bitmap.createBitmap(
            trimmedPixels,
            bounds.width,
            bounds.height,
            Bitmap.Config.ARGB_8888,
        )
        require(trimmed.width == bounds.width && trimmed.height == bounds.height) {
            "trimmed ${trimmed.width}x${trimmed.height} does not match bounds ${bounds.width}x${bounds.height}"
        }

        return listOf(
            SegmentationCandidate(
                bounds = bounds,
                bitmap = trimmed.toAndroidBitmap(),
                canvasWidth = width,
                canvasHeight = height,
                coverageAlphaSum = masked.result.alphaSum,
            ),
        )
    }

    /**
     * 원본에서 잘라 온 [pixels] 에 후처리한 알파를 얹는다. [alpha] 는 원본 전체 좌표계라
     * [bounds] 로 오프셋을 잡아 읽는다.
     */
    private fun applyAlphaInPlace(
        pixels: IntArray,
        alpha: ByteArray,
        alphaRowStride: Int,
        bounds: SegmentationBounds,
    ) {
        for (y in 0 until bounds.height) {
            val alphaRow = (bounds.top + y) * alphaRowStride + bounds.left
            val pixelRow = y * bounds.width
            for (x in 0 until bounds.width) {
                val value = alpha[alphaRow + x].toInt() and 0xFF
                pixels[pixelRow + x] =
                    if (value == 0) 0 else (value shl 24) or (pixels[pixelRow + x] and 0x00FFFFFF)
            }
        }
    }
```

- [ ] **Step 5: `segmentForeground`가 취소 콜백을 넘기게 고친다**

```kotlin
        return try {
            withContext(Dispatchers.Default) {
                val job = currentCoroutineContext()[Job]
                // 타입을 명시하지 않으면 `() -> Unit?` 으로 추론돼 `() -> Unit` 자리에 못 들어간다
                val checkCancelled: () -> Unit = { job?.ensureActive() }
                result.toForegroundCandidate(origin, checkCancelled)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
```

import을 더한다: `kotlinx.coroutines.Job`, `kotlinx.coroutines.currentCoroutineContext`,
`kotlinx.coroutines.ensureActive`, `com.teamyg.parfait.data.utils.repositoryLogger`.
⚠️ `com.teamyg.parfait.domain.model.SegmentationBounds`는 **이미 import되어 있다.** 다시 더하면
ktlint의 중복 import 규칙에 걸린다.

- [ ] **Step 6: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 7: 커밋한다**

```bash
git add -A
git commit -m "feat: 폴백 경로를 후처리 커널에 태우고 중간 판을 없앤다"
```

---

## Task 12: 주 경로 배선과 전멸 원인별 분기

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`

**Interfaces:**
- Consumes: `postProcessAlpha` (Task 10), `sumAlpha` (Task 1), `coverageFloorPixels`·
  `MAX_SUBJECT_COUNT`·`filterCandidates` (Task 3~4)
- Produces: 없음 (private)

**테스트가 없는 이유:** 이 코드는 `Bitmap.getPixels`·`Bitmap.createBitmap`을 부른다. 저장소에
Robolectric이 없고 `isReturnDefaultValues`도 꺼져 있어 JVM 유닛에서 터진다. 판단은 전부 Task 10의
순수 함수로 빠져 있고 여기 남은 것은 좌표 산술과 비트맵 생성과 분기다. 세 갈래는 관측 로그가
필드에서 판정한다.

- [ ] **Step 1: `toCandidates`를 후보 쌍 생성으로 갈아 넣는다**

기존 `toCandidates` 함수를 **통째로 지우고** 아래를 넣는다:

```kotlin
    /** 후처리를 태울 후보 수 상한. 후처리는 `filterCandidates` 의 상한 절단 앞에 있다 */
    private val maxPostProcessCandidates = MAX_SUBJECT_COUNT + 3

    /**
     * 후처리 전후 후보를 짝지어 들고 다닌다. 후처리가 실패하거나 알파를 전멸시킨 후보를
     * **개별로** 되돌리기 위해서다 — 목록 전체가 비었을 때만 되돌리면 넷 중 하나만 전멸한 경우
     * 그 후보가 조용히 사라진다.
     */
    private class CandidatePair(
        val original: SegmentationCandidate,
        val postProcessed: SegmentationCandidate?,
    )

    /**
     * `getBitmap()` 은 널을 돌려줄 수 있다 — `enableSubjectBitmap()` 을 켰다는 이유로 비널을
     * 단정하지 않는다. 판이 없는 후보는 고를 수 없으므로 버린다.
     *
     * 후처리 전에 bbox 로 값싸게 자르는 이유: bbox 픽셀 수는 커버리지의 상계라, 하한 미만이면
     * 커버리지도 하한 미만이다. 최종 판정을 바꾸지 않으면서 큰 판을 훑는 일을 건너뛴다.
     */
    private fun SubjectSegmentationResult.toCandidatePairs(
        origin: Bitmap,
        checkCancelled: () -> Unit,
    ): List<CandidatePair> {
        val floor = coverageFloorPixels(origin.width.toLong() * origin.height)

        val eligible = subjects
            .mapNotNull { subject -> subject.bitmap?.let { subject to it } }
            .filter { (_, bitmap) -> bitmap.width.toLong() * bitmap.height >= floor }
            .sortedByDescending { (_, bitmap) -> bitmap.width.toLong() * bitmap.height }

        val considered = eligible.take(maxPostProcessCandidates)
        if (eligible.size > considered.size) {
            repositoryLogger.i {
                "세그멘테이션 후처리 대상을 ${eligible.size}개 중 ${considered.size}개로 자른다"
            }
        }

        return considered.map { (subject, bitmap) ->
            buildCandidatePair(subject, bitmap, origin, checkCancelled)
        }
    }

    /**
     * ⚠️ `try` 가 픽셀 배열 할당까지 감싼다. 12MP 후보에서 `OutOfMemoryError` 가 가장 잘 나는
     * 자리가 후처리 안이 아니라 그 할당이다.
     */
    private fun buildCandidatePair(
        subject: Subject,
        bitmap: Bitmap,
        origin: Bitmap,
        checkCancelled: () -> Unit,
    ): CandidatePair {
        val postProcessed = try {
            postProcess(subject, bitmap, origin, checkCancelled)
        } catch (e: OutOfMemoryError) {
            // 후처리는 개선 수단이라 실패했다고 흐름 전체를 실패로 접을 이유가 없다.
            // 기존 catch (e: Exception) 은 Error 를 안 잡으므로 여기서 따로 받는다
            repositoryLogger.w(e) { "세그멘테이션 후처리가 메모리로 실패해 원본 후보로 되돌린다" }
            null
        }

        return CandidatePair(
            // 후처리가 성공하면 이 후보는 안 쓰이므로 커버리지 계산을 건너뛴다
            original = originalCandidate(subject, bitmap, origin, countCoverage = postProcessed == null),
            postProcessed = postProcessed,
        )
    }
```

- [ ] **Step 2: 후처리와 원본 후보 생성을 넣는다**

```kotlin
    /** 되돌리는 후보는 후처리 이전 알파로 커버리지를 채운다. 커널 결과가 없으므로 직접 센다 */
    private fun originalCandidate(
        subject: Subject,
        bitmap: Bitmap,
        origin: Bitmap,
        countCoverage: Boolean,
    ): SegmentationCandidate {
        val coverage = if (countCoverage) {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            sumAlpha(pixels)
        } else {
            0L
        }

        val bounds = SegmentationBounds(
            // right·bottom 은 exclusive 라 폭·높이를 그대로 더한다.
            // ML Kit 문서는 getWidth()·getHeight() 가 getBitmap() 의 실제 치수와 같다고
            // 보장하지 않으므로, subject 가 아니라 bitmap 에서 치수를 뽑는다
            left = subject.startX,
            top = subject.startY,
            right = subject.startX + bitmap.width,
            bottom = subject.startY + bitmap.height,
        )
        require(bitmap.width == bounds.width && bitmap.height == bounds.height) {
            "bitmap ${bitmap.width}x${bitmap.height} does not match bounds ${bounds.width}x${bounds.height}"
        }

        return SegmentationCandidate(
            bounds = bounds,
            bitmap = bitmap.toAndroidBitmap(),
            canvasWidth = origin.width,
            canvasHeight = origin.height,
            coverageAlphaSum = coverage,
        )
    }

    /**
     * ⚠️ **자르기는 알파를 바꾸지 않는다.** 후처리 결과를 픽셀에 반영하려면 새 판을 만들어야 한다.
     * ML Kit 판에 되쓰는 것은 안 된다 — 그 판의 수명은 `SubjectSegmentationResult` 가 쥐고 있고
     * 네이티브에서 온 비트맵이 immutable 이면 예외다. 소유권 논의는
     * `synthesis/open-questions.md` 의 OQ-P-266 에 있다.
     */
    private fun postProcess(
        subject: Subject,
        bitmap: Bitmap,
        origin: Bitmap,
        checkCancelled: () -> Unit,
    ): SegmentationCandidate? {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val alpha = ByteArray(width * height)
        for (index in pixels.indices) alpha[index] = (pixels[index] ushr 24).toByte()

        val result = postProcessAlpha(alpha, width, height, checkCancelled = checkCancelled) ?: return null

        repositoryLogger.i {
            "세그멘테이션 후보 부분 알파 ${result.partialAlphaPixels}/${width * height}"
        }

        val inner = result.bounds
        val unchangedWholePlate = !result.changed && inner.width == width && inner.height == height
        val trimmed = if (unchangedWholePlate) bitmap else cropWithAlpha(pixels, alpha, width, inner)

        require(trimmed.width == inner.width && trimmed.height == inner.height) {
            "trimmed ${trimmed.width}x${trimmed.height} does not match bounds ${inner.width}x${inner.height}"
        }

        return SegmentationCandidate(
            bounds = SegmentationBounds(
                left = subject.startX + inner.left,
                top = subject.startY + inner.top,
                right = subject.startX + inner.right,
                bottom = subject.startY + inner.bottom,
            ),
            bitmap = trimmed.toAndroidBitmap(),
            canvasWidth = origin.width,
            canvasHeight = origin.height,
            coverageAlphaSum = result.alphaSum,
        )
    }

    /** 출력 판을 bounds 크기로 바로 만든다. 원본 크기로 만들고 나중에 자르면 큰 배열이 헛돈다 */
    private fun cropWithAlpha(
        pixels: IntArray,
        alpha: ByteArray,
        rowStride: Int,
        bounds: SegmentationBounds,
    ): Bitmap {
        val cropped = IntArray(bounds.width * bounds.height)
        for (y in 0 until bounds.height) {
            val sourceRow = (bounds.top + y) * rowStride + bounds.left
            val targetRow = y * bounds.width
            for (x in 0 until bounds.width) {
                val value = alpha[sourceRow + x].toInt() and 0xFF
                cropped[targetRow + x] =
                    if (value == 0) 0 else (value shl 24) or (pixels[sourceRow + x] and 0x00FFFFFF)
            }
        }
        return Bitmap.createBitmap(cropped, bounds.width, bounds.height, Bitmap.Config.ARGB_8888)
    }
```

import을 더한다: `com.google.mlkit.vision.segmentation.subject.Subject`.

- [ ] **Step 3: `segmentImage`의 후보 흐름을 세 갈래로 가른다**

`segmentImage`에서 기존 `candidates` 블록과 두 `return`을 아래로 치환한다:

```kotlin
        val pairs = try {
            withContext(Dispatchers.Default) {
                val job = currentCoroutineContext()[Job]
                val checkCancelled: () -> Unit = { job?.ensureActive() }
                result.toCandidatePairs(bitmap, checkCancelled)
            }
        } catch (e: CancellationException) {
            // 취소는 실패가 아니다 — 값으로 접으면 상위로 전파되지 않아 취소된 흐름이 계속 돈다
            throw e
        } catch (e: Exception) {
            // 필터·변환이 던질 수 있는 예상 밖 실패를 화면에 토스트로 전달할 수 있게 감싼다
            return Result.failure(SegmentationException.Process(e))
        }

        if (pairs.isEmpty()) {
            repositoryLogger.i { "세그멘테이션: ML Kit 이 후보를 0건 줬다. 전경 마스크 폴백으로 내려간다" }
            return Result.success(segmentForeground(image, bitmap))
        }

        val reverted = pairs.count { it.postProcessed == null }
        if (reverted > 0) {
            // 후처리는 개선 수단이지 후보를 없앨 권한이 아니다
            repositoryLogger.i {
                "세그멘테이션 후처리: ${pairs.size}개 중 ${reverted}개를 후처리 이전 후보로 되돌린다"
            }
        }

        val candidates = try {
            withContext(Dispatchers.Default) {
                filterCandidates(pairs.map { it.postProcessed ?: it.original })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(SegmentationException.Process(e))
        }

        if (candidates.isNotEmpty()) return Result.success(candidates)

        repositoryLogger.i {
            "세그멘테이션: 필터가 후보 ${pairs.size}개를 전부 걸러 냈다. 전경 마스크 폴백으로 내려간다"
        }
        return Result.success(segmentForeground(image, bitmap))
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 주 경로를 후처리에 태우고 후보 전멸을 원인별로 가른다"
```

---

## Task 13: 탭 승자 역전 회귀 테스트

**Files:**
- Modify: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/component/SegmentationHighlightGeometryTest.kt`

**Interfaces:**
- Consumes: 기존 `pick(boundsList, tapX, tapY)` 헬퍼
- Produces: 없음

`pickCandidateIndex`는 겹친 후보 중 bbox 면적이 최소인 것을 고른다. 후처리 축소량은 후보마다
다르므로 대소가 뒤집히면 **같은 자리를 탭했을 때 이전과 다른 후보가 잡힌다.** 규칙 자체는 유지하되
그 민감성을 테스트로 고정한다.

⚠️ **탭 y는 100 이상이어야 한다.** 그 파일은 100×100 이미지를 200×400 캔버스에 Fit으로 그려서
세로 오프셋이 100이다. 그 아래는 레터박스 여백이라 `pick`이 `null`을 돌려준다.

- [ ] **Step 1: 테스트를 더한다**

```kotlin
    @Test
    fun pickCandidateIndex_shrinkingOneBounds_flipsTheWinner() {
        // Given — 후처리 전에는 A 가 작고, A 만 덜 줄어들면 후처리 후에는 B 가 작아진다
        val beforeA = SegmentationBounds(left = 0, top = 0, right = 40, bottom = 40)
        val beforeB = SegmentationBounds(left = 0, top = 0, right = 50, bottom = 50)
        val afterA = SegmentationBounds(left = 0, top = 0, right = 38, bottom = 38)
        val afterB = SegmentationBounds(left = 0, top = 0, right = 30, bottom = 30)

        // When — 캔버스 좌표다. 이미지가 세로로 100 밀려 있으므로 y 는 그보다 커야 한다
        val before = pick(listOf(beforeA, beforeB), tapX = 10f, tapY = 110f)
        val after = pick(listOf(afterA, afterB), tapX = 10f, tapY = 110f)

        // Then — 승자 규칙은 그대로 "면적 최소"다. 바뀌는 것은 어느 쪽이 작으냐다
        assertEquals(0, before)
        assertEquals(1, after)
    }

    @Test
    fun pickCandidateIndex_lowAlphaResidueWidensBounds_alsoFlipsTheWinner() {
        // Given — 팽창 띠에 남은 아주 낮은 알파가 bounds 를 넓히는 반대 방향도 성립한다
        val tightA = SegmentationBounds(left = 0, top = 0, right = 30, bottom = 30)
        val tightB = SegmentationBounds(left = 0, top = 0, right = 40, bottom = 40)
        val widenedA = SegmentationBounds(left = 0, top = 0, right = 45, bottom = 45)

        // When
        val tight = pick(listOf(tightA, tightB), tapX = 10f, tapY = 110f)
        val widened = pick(listOf(widenedA, tightB), tapX = 10f, tapY = 110f)

        // Then
        assertEquals(0, tight)
        assertEquals(1, widened)
    }
```

- [ ] **Step 2: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

⚠️ `null` 이 나오면 탭 좌표가 이미지 영역 밖이다. 기존 테스트가 쓰는 좌표를 다시 확인한다.

- [ ] **Step 3: 커밋한다**

```bash
git add -A
git commit -m "test: bounds 변화가 탭 승자를 뒤집는 경우를 고정한다"
```

---

## Task 14: 사진 세트 판정과 문서 갱신

**Files:**
- Modify: `parfait/specs/2026-08-24-segmentation-mask-postprocessing.md`
- Modify: `parfait/plans/2026-08-24-segmentation-mask-postprocessing.md`
- Modify: `parfait/plans/README.md`, `parfait/specs/README.md`
- Modify: `parfait/synthesis/open-questions.md`

**Interfaces:**
- Consumes: Task 1~13의 결과
- Produces: 없음

⚠️ **이 태스크는 `parfait` 문서 저장소에서 한다**(코드 저장소가 아니다). 산문 메모로 두면 SDD가
실행하지 않고, 그러면 근거 등급 표의 조건부 항목을 판정할 주체가 사라진다.

- [ ] **Step 1: 실기기에서 사진 세트를 돌린다**

프레임 변에 걸친 피사체, 머리카락이 있는 인물, 밝은 배경의 밝은 물체, 비스듬히 놓인 가늘고 긴
물체, 잡티가 많은 텍스처 배경, 짧은 변 512 미만. 앞 라운드 세트를 이어 쓴다. 각 사진에서
**후보 화면을 캡처하고 logcat을 저장한다.**

- [ ] **Step 2: 관측 로그로 조건부 항목을 판정한다**

| 로그 | 닫는 미결 | 판정 기준 |
|---|---|---|
| 부분 알파 비율 | OQ-P-287·288 | 비율이 0에 가까우면 램프와 침식이 값어치가 없다 |
| 되돌린 개수 | OQ-P-292 | 되돌리기가 잦으면 커버리지 임계나 area opening 임계가 과하다 |
| 사전 절단 개수 | OQ-P-292 | 잘린 후보가 최종 다섯에 들었을 사례가 보이면 여유 3을 늘린다 |
| 0건 원인 3분기 | OQ-P-292 | 필터 전멸이 잦으면 임계를 다시 본다 |
| 짧은 변 512 미만 사진 | OQ-P-294 | 축소 하한이 실제로 발동하는지 확인한다 |
| 교차하는 얇은 피사체 캡처 | OQ-P-289 | 사각형 IoU 오판이 보이면 마스크 IoU로 승격한다 |

- [ ] **Step 3: 스펙을 as-built로 고친다**

근거 등급 표의 조건부 항목 중 **철회 조건을 만족한 것은 뺀다.** 빼는 것이 정답인 경우가 있다는
사실을 스펙이 명시하고 있다. 판정 결과를 각 OQ의 「상태」와 「해소 메모」에 적는다.

- [ ] **Step 4: 문서 상태를 옮긴다**

계획의 `status`를 `done`으로, `archived_reason`을 채우고 `plans/archive/`로 옮긴다. 스펙도
`implemented`로 바꾸고 `specs/archive/`로 옮긴다. 두 `README.md`의 인덱스를 아카이브 표로 옮긴다.
아카이브로 옮기면 상대 링크 깊이가 한 단계 늘어나므로 `../` → `../../`로 보정한다.

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "docs: 마스크 후처리 구현 결과를 문서에 반영한다"
```

---

## 자체 점검

**스펙 커버리지**

| 스펙 절 | Task |
|---|---|
| 처리 해상도 — 이진화 후 OR 풀링·올림·축소 하한 | 5, 10 |
| 런 추출 + union-find 8-근방 | 6 |
| area opening (원본 픽셀 환산 임계) | 6, 10 |
| keep-mask 8-근방 1픽셀 팽창 | 7 |
| keep-mask 적용 (참인 자리 원본 알파 유지) | 8 |
| 경계 한 겹 침식 — 능선 보호·직전 행 스냅샷 | 9 |
| tight bounds + 커버리지 산출, `trimTransparentBounds` 정합 | 8 |
| 취소 확인 콜백 (행 경계) | 5~10 |
| API / 인터페이스 | 10 |
| 필터 — 커버리지 판정, `Long` | 1, 2, 3 |
| 필터 — `MAX_SUBJECT_COUNT` 유지 | 3 |
| 필터 — IoU 0.9 병합, 포함 관계 보존 | 4 |
| 필터 — 전순서 정렬 | 3 |
| 램프 사상 + 등가식 | 11 |
| 폴백 중간 판 제거 | 11 |
| 주 경로 bbox 사전 절단 + 상한 로그 | 12 |
| 주 경로 새 판 생성 + 모든 생성 지점의 `require` | 11, 12 |
| 후보 전멸 3분기 + **후보 단위** 되돌리기 | 12 |
| `OutOfMemoryError` (두 경로) | 11, 12 |
| `CancellationException` 재던지기 | 11, 12 |
| 관측 세 줄 | 11, 12 |
| 탭 승자 역전 회귀 | 13 |
| 사진 세트 판정 + as-built | 14 |
| 기존 테스트 처리 표 전 항목 | 2, 3, 4, 11 |

**타입 추적** — `sumAlpha`는 Task 1이 정의하고 2·12가 쓴다. `coverageFloorPixels`·
`MAX_SUBJECT_COUNT`는 3이 정의하고 12가 쓴다. `filterCandidates`는 4가 완성하고 12가 쓴다.
`ceilDiv`는 5가 정의하고 10이 쓴다. `downscaleMask`·`applyAreaOpening`·`dilateMask`는 5~7이
정의하고 10이 쓴다. `applyKeepMask`·`measureAlpha`·`erodeEdge`는 8~9가 정의하고 10이 쓴다.
`postProcessAlpha`·`AlphaPostProcessOptions`·`AlphaPostProcessResult`는 10이 정의하고 11·12가
쓴다. `maskSubjectAlpha`·`MaskedAlpha`는 11이 정의하고 11 안에서 쓴다. `CandidatePair`는 12
안에서만 산다. **뒤 Task의 산출물을 앞에서 쓰는 곳은 없다.**

**PR 경계** — PR 1은 Task 1~4, PR 2는 Task 5~10, PR 3은 Task 11~13이다. Task 14는 문서 저장소
작업이라 PR 밖이다. **모든 태스크가 커밋으로 끝나고, 커밋 직전에 전체 검사가 통과한다.**
