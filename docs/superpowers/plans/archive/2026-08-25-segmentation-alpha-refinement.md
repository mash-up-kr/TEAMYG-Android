---
id: segmentation-alpha-refinement
title: 세그멘테이션 알파 경계 정련 구현 계획 (1 PR)
status: done
type: work-order
created: 2026-08-25
updated: 2026-08-27
platforms: android
owner: android
related_adr: ADR-0012
related_spec: segmentation-alpha-refinement, segmentation-mask-postprocessing
related_code: AlphaRefine.kt#refineAlpha, AlphaRefine.kt#boxMean, AlphaRefine.kt#guidedCoefficients, AlphaRefine.kt#applyCoefficients, AlphaPostProcessor.kt#postProcessAlpha, AlphaPostProcessor.kt#refineWithin, AlphaPostProcessor.kt#erodeEdge, AlphaComponents.kt#ceilDiv, SegmentationMask.kt#maskSubjectAlpha, ImageSegmentationRepositoryImpl#postProcess, ImageSegmentationRepositoryImpl#toForegroundCandidate
archived_reason: Task 1~7 이 PR #363(`4da18230`, 2026-08-27)으로 develop 에 머지됐다. 사진 세트 판정(OQ-P-296~300)은 처음부터 이 계획 밖이다
tags: [plan, parfait]
---

# 세그멘테이션 알파 경계 정련 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 원본 휘도를 안내자로 삼아 알파 경계를 실제 물체 경계로 끌어당기고, 그 과정에서 하드
매트의 계단을 소프트 매트로 바꾼다.

**Architecture:** 가이드 필터를 쓰되 **계수는 축소판에서 구하고 적용만 원본 해상도에서 한다**
(Fast Guided Filter). 정련은 기존 후처리 커널의 `keep 적용`과 `침식` 사이에 들어가고, 앞뒤 단계의
동작은 바꾸지 않는다. 계산은 전부 배열 위 순수 함수라 기기 없이 JVM으로 덮는다.

**Tech Stack:** Kotlin, kotlin.test + JUnit4(JVM 유닛)

**Spec:** [`parfait/specs/archive/2026-08-25-segmentation-alpha-refinement.md`](../../specs/archive/2026-08-25-segmentation-alpha-refinement.md)

> ✅ **Task 1~7 이 develop 에 들어갔다** — PR #363 `4da18230`(2026-08-27). 이 브랜치가 스택의 맨
> 위였고, 아래 둘을 품은 채 통째로 머지됐다. 설계와 어긋난 자리는 없고, 머지 시점의 코드가 이
> 계획과 다른 유일한 자리는 **`refineAlpha` 계열이 `checkCancelled` 를 갖지 않는다**는 것이다 —
> 뒤 라운드인 [커널 취소 확인 전환](2026-08-27-alpha-kernel-suspend-cancellation.md)이 rebase 로
> 이 브랜치 위에 얹혀 함께 왔기 때문이다. 사진 세트 판정(OQ-P-296~300)은 이 계획이 처음부터
> 범위 밖에 뒀고 여전히 사람 손을 기다린다.

> **이 계획은 검수 2회를 받고 다시 쓴 판본이다.** 초판에서 뒤집힌 것: ① **주 경로 안내자가 원본이
> 아니라 ML Kit이 배경을 도려낸 판이었다** — 그러면 `I ≡ p`가 되어 정련이 틀린 경계를 스스로
> 강화한다(기능 무효화). ② **Task 6이 자기 게이트를 통과 못 했다** — `SegmentationMask.kt`가
> `postProcessAlpha`를 위치 인자로 불러 새 파라미터 삽입에 컴파일이 깨진다. ③ **테스트 넷의
> 기대값이 수학적으로 틀렸다** — 계수를 다시 창 평균하는 단계를 기대값에 안 넣었고, 소프트 매트
> 픽스처가 `p ≡ I`라 필터가 **일부러 보존하는** 상태를 단언했다. ④ **⚠️ 메모가 정반대 진단을
> 유도했다** — 그대로 두면 구현자가 가이드 필터의 핵심 단계를 지우고도 전부 초록이 된다.
> ⑤ **원본 해상도 실수 배열 둘이 약 96MB를 더 얹었다** — 휘도를 즉석 계산으로 바꿨다.

## Global Constraints

- **작업 저장소는 `TJYG-Android`다**(이 계획 문서가 있는 repo와 다르다). 로컬 절대경로는 private
  submodule의 `wiki/personal-private/project-paths.md`에 있다.
- **이 계획은 PR 하나다.** 베이스는 앞 라운드의 3단계 브랜치(`feature/segmentation-postprocess-wiring`)
  위다. 그 브랜치가 만든 커널과 배선을 전제로 한다.
- **커밋만 하고 push·PR은 하지 않는다.**
- **매 태스크는 커밋으로 끝나고, 커밋 직전에 `./gradlew test ktlintCheck :app:assembleDebug`가
  통과해야 한다. 예외는 없다.** ⚠️ **시그니처가 바뀌는 변경은 호출부 수정까지 한 태스크에 넣는다.**
- ⚠️ ktlint: 파라미터 2개 이상 시그니처는 멀티라인, **`max_line_length = 120`**, 미사용·중복 import
  금지, 래핑된 인자 목록은 인자 하나당 한 줄. 막히면 `./gradlew ktlintFormat`으로 먼저 편다.
- **주석 규약**: 코드가 이미 말하는 것은 쓰지 않는다. `@return`·`@param`은 타입·이름이 말하지 못할
  때만. **다른 컴포넌트의 현재 상태를 단정하지 않는다.** 아키텍처 결정은 문서에 쓰고 코드에는
  포인터 한 줄만 둔다. 주석 분량은 그 코드의 **어려움**에 비례한다.
- **기존 파일을 "전문"으로 덮어쓰지 않는다.** 기존 주석을 지울 때는 그 정보가 어디에 살아남는지
  확인한다. 코드에서 지웠는데 문서에도 없으면 정리가 아니라 유실이다.
- 테스트는 `kotlin.test`를 쓴다. `org.junit.*`을 import하지 않는다. 이름은 `함수명_조건_기대`이고
  본문에 `// Given` `// When` `// Then` 주석을 단다.
- **이 저장소에 Robolectric이 없고 `testOptions.unitTests.isReturnDefaultValues`도 안 켜져 있다.**
  `Bitmap`이 걸린 코드는 JVM 유닛으로 못 덮는다. 그런 태스크는 **왜 테스트가 없는지를 본문에 적는다.**
- **알파를 `ByteArray`에서 읽을 때는 반드시 `and 0xFF`를 쓴다.**
- ⚠️ **적분 영상을 쓰지 않는다.** 12MP에서 누적값이 `Float` 정밀도를 넘어 창 차분이 무너진다.
  박스 평균은 **행·열 슬라이딩 합 2패스**로 구한다.
- ⚠️ **창은 실제 포함된 픽셀 수로 정규화한다.** 고정 개수 `(2r+1)²`로 나누면 가장자리 알파가
  체계적으로 낮아진다.
- ⚠️ **원본 해상도 실수 배열을 실체화하지 않는다.** 휘도는 축소하며 즉석 계산하고, 적용 단계에서도
  픽셀마다 그 자리에서 구한다. 실체화하면 12MP에서 96MB가 더 붙는다(OQ-P-300).
- **취소 콜백은 각 패스의 행(또는 열) 루프마다 부른다.** 12MP에서 한 패스가 1200만 회 순회다.
- **`refineDownscale`은 판정 버퍼 배율 `downscaleFactor`와 별개 값이다.**
- `minSdk`는 26이다.

---

## 파일 구성

| 파일 | 책임 | 태스크 |
|---|---|---|
| `data/.../repository/image/AlphaRefine.kt` (신설) | 박스 평균·축소 휘도/알파·계수·되올림 적용·조립 | 1~5 |
| `data/src/test/.../repository/image/AlphaRefineTest.kt` (신설) | 위 함수들의 유닛 | 1~5 |
| `data/.../repository/image/AlphaPostProcessor.kt` | 옵션 넷, 안내자 공급자, 측정 분기, 정련 시간 | 6 |
| `data/.../repository/image/SegmentationMask.kt` | `postProcessAlpha` 호출부 named 인자화, 안내자 전달 | 6·7 |
| `data/src/test/.../repository/image/AlphaPostProcessorTest.kt` | 정련 켜고 끄기 통합 | 6 |
| `data/.../repository/image/ImageSegmentationRepositoryImpl.kt` | 두 경로 안내자, 정련 시간 로그 | 7 |

**패키지는 전부 `com.teamyg.parfait.data.repository.image`다.** `data` 모듈 소스 레이아웃은
`src/main/java`·`src/test/java`다.

## 손대는 기존 코드의 실제 형태

⚠️ **아래는 계획이 지어낸 것이 아니라 베이스 브랜치에서 그대로 옮긴 것이고, 검수가 문자 단위로
대조해 확인했다.**

`AlphaComponents.kt` — 그대로 재사용한다:

```kotlin
internal fun ceilDiv(
    value: Int,
    divisor: Int,
): Int = (value + divisor - 1) / divisor
```

> **as-built(#360 리뷰 반영)** — 위 인용은 당시 베이스 브랜치의 본문이다. 이후 `require(divisor > 0)`
> 이 붙었고 계산식은 그대로다. 이 계획이 쓰는 `ceilDiv(width, downscale)` 는 `refineAlpha` 가
> 이미 `require(downscale >= 1)` 로 막고 있어 호출 결과가 달라지지 않는다.

`AlphaPostProcessor.kt`의 현재 조립부:

```kotlin
    val applied = applyKeepMask(alpha, width, height, keep, maskWidth, factor, checkCancelled)
    val eroded = options.erodeEdge && erodeEdge(alpha, width, height, checkCancelled)
    val measured = measureAlpha(alpha, width, height, checkCancelled) ?: return null

    return AlphaPostProcessResult(
        bounds = measured.bounds,
        alphaSum = measured.alphaSum,
        partialAlphaPixels = measured.partialAlphaPixels,
        changed = applied || eroded,
    )
```

`AlphaPostProcessOptions`의 현재 형태:

```kotlin
internal data class AlphaPostProcessOptions(
    val downscaleFactor: Int = 4,
    val binaryThreshold: Int = 127,
    val areaOpeningMinPixels: Int = AREA_OPENING_MIN_PIXELS,
    val erodeEdge: Boolean = true,
    val minPixelsForDownscale: Int = MIN_PIXELS_FOR_DOWNSCALE,
)
```

⚠️ `SegmentationMask.kt`의 호출부는 **위치 인자**다. Task 6이 이 줄을 같이 고친다:

```kotlin
    val result = postProcessAlpha(alpha, width, height, options, checkCancelled) ?: return null
```

`AlphaPostProcessorTest.kt`에는 헬퍼 `alphaBytes`·`asInts`와 import
`SegmentationBounds`·`Test`·`assertContentEquals`·`assertEquals`·`assertFailsWith`·`assertNull`·
`assertTrue`가 이미 있다. **새 import는 필요 없다.**

`AlphaPostProcessor.kt`에 파일 private 상수 `OPAQUE = 255`(Int)가 이미 있다. `AlphaRefine.kt`에도
같은 이름을 두지만 **top-level `private`은 파일 스코프라 충돌하지 않는다**(이 저장소에 선례가 있다).

---

## Task 1: 박스 평균

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaRefine.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaRefineTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `internal fun boxMean(src: FloatArray, width: Int, height: Int, radius: Int, checkCancelled: () -> Unit = {}): FloatArray`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`AlphaRefineTest.kt`. ⚠️ **import는 `assertTrue`만이다.** `assertEquals`는 Task 2가 처음 쓰므로
여기서 import하면 미사용으로 게이트가 깨진다.

```kotlin
package com.teamyg.parfait.data.repository.image

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** 값이 0~1 범위라 이 정도면 충분하다 */
private const val TOLERANCE = 1e-4f

private fun assertClose(
    expected: Float,
    actual: Float,
    message: String = "",
) {
    assertTrue(abs(expected - actual) <= TOLERANCE, "$message expected=$expected actual=$actual")
}

class AlphaRefineTest {
    @Test
    fun boxMean_everyValueIsOne_staysOneEvenAtTheCorners() {
        // Given — 고정 개수로 나누면 모서리가 4/9 로 내려앉는다
        val src = FloatArray(9) { 1f }

        // When
        val mean = boxMean(src, width = 3, height = 3, radius = 1)

        // Then
        for (index in mean.indices) assertClose(1f, mean[index], "index=$index")
    }

    @Test
    fun boxMean_radiusLargerThanTheArray_averagesEverything() {
        // Given
        val src = floatArrayOf(0f, 1f, 0f, 1f)

        // When
        val mean = boxMean(src, width = 2, height = 2, radius = 5)

        // Then
        for (index in mean.indices) assertClose(0.5f, mean[index], "index=$index")
    }

    @Test
    fun boxMean_singleSpike_spreadsOverTheWindowOnly() {
        // Given — 5×5 한가운데(2,2)만 1 이다
        val src = FloatArray(25)
        src[12] = 1f

        // When
        val mean = boxMean(src, width = 5, height = 5, radius = 1)

        // Then — 중앙과 대각 이웃(1,1)은 창이 온전히 안에 들어 1/9, 두 칸 밖은 0
        assertClose(1f / 9f, mean[12], "center")
        assertClose(1f / 9f, mean[6], "diagonal neighbour")
        assertClose(0f, mean[0], "two cells away")
    }

    @Test
    fun boxMean_windowClippedAtTheEdge_usesTheActualCount() {
        // Given — 한 행짜리. 왼쪽 끝의 창은 두 칸만 포함한다
        val src = floatArrayOf(1f, 0f, 0f, 0f)

        // When
        val mean = boxMean(src, width = 4, height = 1, radius = 1)

        // Then
        assertClose(0.5f, mean[0], "left edge counts two cells")
        assertClose(1f / 3f, mean[1], "interior counts three cells")
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaRefineTest*"`
Expected: 컴파일 실패 — `Unresolved reference: boxMean`

- [ ] **Step 3: 구현한다**

`AlphaRefine.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

private const val OPAQUE = 255f

/**
 * 반경 [radius] 창의 평균을 낸다.
 *
 * ⚠️ 적분 영상을 쓰지 않는다. 12MP 판에서 누적값이 `Float` 정밀도를 넘어 창 차분이 무너진다.
 * 슬라이딩 합 2패스는 누적 구간이 한 행·한 열이라 그 문제가 없고 계산량도 같다.
 *
 * 가장자리에서는 창이 잘리므로 **실제 포함된 픽셀 수로 나눈다.**
 */
internal fun boxMean(
    src: FloatArray,
    width: Int,
    height: Int,
    radius: Int,
    checkCancelled: () -> Unit = {},
): FloatArray {
    val horizontal = FloatArray(src.size)
    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        var sum = 0f
        for (x in 0..minOf(radius, width - 1)) sum += src[rowOffset + x]
        for (x in 0 until width) {
            horizontal[rowOffset + x] = sum
            val exiting = x - radius
            val entering = x + radius + 1
            if (exiting >= 0) sum -= src[rowOffset + exiting]
            if (entering < width) sum += src[rowOffset + entering]
        }
    }

    val mean = FloatArray(src.size)
    for (x in 0 until width) {
        checkCancelled()
        var sum = 0f
        for (y in 0..minOf(radius, height - 1)) sum += horizontal[y * width + x]
        val columns = minOf(width - 1, x + radius) - maxOf(0, x - radius) + 1
        for (y in 0 until height) {
            val rows = minOf(height - 1, y + radius) - maxOf(0, y - radius) + 1
            mean[y * width + x] = sum / (columns * rows)
            val exiting = y - radius
            val entering = y + radius + 1
            if (exiting >= 0) sum -= horizontal[exiting * width + x]
            if (entering < height) sum += horizontal[entering * width + x]
        }
    }

    return mean
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 창 평균을 슬라이딩 합 2패스로 구한다"
```

---

## Task 2: 축소 휘도와 축소 알파

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaRefine.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaRefineTest.kt`

**Interfaces:**
- Consumes: `ceilDiv` (`AlphaComponents.kt`, 기존)
- Produces: `internal fun downscaleLuminance(pixels: IntArray, width: Int, height: Int, factor: Int, checkCancelled: () -> Unit = {}): FloatArray`,
  `internal fun downscaleAlpha(alpha: ByteArray, width: Int, height: Int, factor: Int, checkCancelled: () -> Unit = {}): FloatArray`

⚠️ **원본 해상도 휘도 배열을 만들지 않는다.** 축소하면서 픽셀마다 즉석 계산한다. 실체화하면
12MP에서 48MB가 붙는다(OQ-P-300).

- [ ] **Step 1: 실패하는 테스트를 쓴다**

import에 `kotlin.test.assertEquals`를 **이제** 더한다. 클래스 안에 다섯을 더한다:

```kotlin
    @Test
    fun downscaleLuminance_greenAndBlueWeights_areNotSwapped() {
        // Given — 순수색 셋. 계수를 맞바꾸면 회색·빨강 테스트로는 안 잡힌다
        val pixels = intArrayOf(0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFF0000.toInt())

        // When
        val sub = downscaleLuminance(pixels, width = 3, height = 1, factor = 1)

        // Then
        assertClose(0.587f, sub[0], "green")
        assertClose(0.114f, sub[1], "blue")
        assertClose(0.299f, sub[2], "red")
    }

    @Test
    fun downscaleLuminance_ignoresTheAlphaChannel() {
        // Given — 같은 빨강인데 알파만 다르다. 안내자는 색만 봐야 한다
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0x00FF0000)

        // When
        val sub = downscaleLuminance(pixels, width = 2, height = 1, factor = 1)

        // Then
        assertClose(0.299f, sub[0], "opaque red")
        assertClose(0.299f, sub[1], "transparent red")
    }

    @Test
    fun downscaleAlpha_valueAbove127_isNotMisreadAsNegative() {
        // Given — 부호 처리를 빠뜨리면 음수가 된다
        val alpha = byteArrayOf(0, 128.toByte(), 255.toByte())

        // When
        val sub = downscaleAlpha(alpha, width = 3, height = 1, factor = 1)

        // Then
        assertClose(0f, sub[0], "transparent")
        assertClose(128f / 255f, sub[1], "half")
        assertClose(1f, sub[2], "opaque")
    }

    @Test
    fun downscaleAlpha_widthIsNotAMultipleOfFactor_averagesTheShortBlock() {
        // Given — 3×1 을 배율 2 로 줄이면 두 번째 블록에 한 칸만 든다
        val alpha = byteArrayOf(255.toByte(), 0, 255.toByte())

        // When
        val sub = downscaleAlpha(alpha, width = 3, height = 1, factor = 2)

        // Then
        assertEquals(2, sub.size)
        assertClose(0.5f, sub[0], "full block")
        assertClose(1f, sub[1], "short block averages one cell, not two")
    }

    @Test
    fun downscaleAlpha_heightIsNotAMultipleOfFactor_keepsTheTrailingRow() {
        // Given — 3×3 을 배율 2 로 줄이면 2×2 다. 마지막 행·열은 한 칸짜리 블록이다.
        // 아래 행만 불투명하게 두면 세로 인덱싱이 틀렸을 때 값이 어긋난다
        val alpha = ByteArray(9) { index -> if (index >= 6) 255.toByte() else 0 }

        // When
        val sub = downscaleAlpha(alpha, width = 3, height = 3, factor = 2)

        // Then
        assertEquals(4, sub.size)
        assertClose(0f, sub[0], "top-left block has no opaque row")
        assertClose(0f, sub[1], "top-right block has no opaque row")
        assertClose(1f, sub[2], "bottom-left block is the trailing row")
        assertClose(1f, sub[3], "bottom-right block is the trailing row")
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaRefineTest*"`
Expected: 컴파일 실패 — `Unresolved reference: downscaleLuminance`

- [ ] **Step 3: 구현한다**

`AlphaRefine.kt`에 더한다:

```kotlin
/**
 * 안내자를 휘도로 바꾸며 [factor] 배율로 줄인다. 컬러 3채널을 쓰지 않는 이유는
 * `specs/2026-08-25-segmentation-alpha-refinement.md` 「범위 - 제외」 참고.
 */
internal fun downscaleLuminance(
    pixels: IntArray,
    width: Int,
    height: Int,
    factor: Int,
    checkCancelled: () -> Unit = {},
): FloatArray = downscale(width, height, factor, checkCancelled) { index -> luminanceOf(pixels[index]) }

internal fun downscaleAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    factor: Int,
    checkCancelled: () -> Unit = {},
): FloatArray = downscale(width, height, factor, checkCancelled) { index ->
    (alpha[index].toInt() and 0xFF) / OPAQUE
}

/**
 * 가장자리 블록은 **존재하는 칸만** 평균한다 — 없는 칸을 0으로 치면 오른쪽·아래 가장자리의
 * 안내자가 어두워져 경계가 그쪽으로 끌린다.
 */
private inline fun downscale(
    width: Int,
    height: Int,
    factor: Int,
    checkCancelled: () -> Unit,
    value: (Int) -> Float,
): FloatArray {
    val subWidth = ceilDiv(width, factor)
    val sums = FloatArray(subWidth * ceilDiv(height, factor))
    val counts = IntArray(sums.size)

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        val subRowOffset = (y / factor) * subWidth
        for (x in 0 until width) {
            val index = subRowOffset + x / factor
            sums[index] += value(rowOffset + x)
            counts[index]++
        }
    }

    for (index in sums.indices) sums[index] /= counts[index]

    return sums
}

private fun luminanceOf(pixel: Int): Float {
    val red = (pixel ushr 16) and 0xFF
    val green = (pixel ushr 8) and 0xFF
    val blue = pixel and 0xFF
    return (0.299f * red + 0.587f * green + 0.114f * blue) / OPAQUE
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 계수용 축소판을 휘도·알파에서 즉석으로 만든다"
```

---

## Task 3: 가이드 필터 계수

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaRefine.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaRefineTest.kt`

**Interfaces:**
- Consumes: `boxMean` (Task 1)
- Produces: `internal class GuidedCoefficients(val a: FloatArray, val b: FloatArray)`,
  `internal fun guidedCoefficients(guidance: FloatArray, input: FloatArray, width: Int, height: Int, radius: Int, epsilon: Float, checkCancelled: () -> Unit = {}): GuidedCoefficients`

⚠️ **계수는 마지막에 한 번 더 창 평균한다.** 그것이 가이드 필터의 정본이다(`q = ā·I + b̄`).
아래 두 테스트의 기대값은 **그 두 번째 평균까지 반영한 값**이다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun guidedCoefficients_constantGuidance_degeneratesToADoubleMean() {
        // Given — 안내자에 경계가 없으면 알파를 옮길 근거가 없다. a 는 0 이고 b 만 남는다
        val guidance = FloatArray(16) { 0.5f }
        val input = FloatArray(16) { index -> if (index % 4 < 2) 1f else 0f }

        // When
        val coefficients = guidedCoefficients(
            guidance = guidance,
            input = input,
            width = 4,
            height = 4,
            radius = 1,
            epsilon = 1e-4f,
        )

        // Then — b 는 창 평균을 **두 번** 거친 값이다. 한 번만 기대하면 가장자리에서 0.167 어긋난다
        val once = boxMean(input, width = 4, height = 4, radius = 1)
        val twice = boxMean(once, width = 4, height = 4, radius = 1)
        for (index in coefficients.a.indices) {
            assertClose(0f, coefficients.a[index], "a index=$index")
            assertClose(twice[index], coefficients.b[index], "b index=$index")
        }
    }

    @Test
    fun guidedCoefficients_inputEqualsGuidance_reproducesTheGuidance() {
        // Given — p 가 I 와 같으면 q = I 여야 하므로 a = 1, b = 0 이다.
        // ⚠️ 안내자는 **모든 창에 분산이 있어야** 한다. 계단형이면 가장자리 창의 분산이 0 이라
        // 그 자리 a 가 0 으로 떨어지고 두 번째 평균이 그것을 안쪽까지 번지게 한다
        val guidance = FloatArray(16) { index -> (index % 4) * 0.3f }

        // When
        val coefficients = guidedCoefficients(
            guidance = guidance,
            input = guidance.copyOf(),
            width = 4,
            height = 4,
            radius = 1,
            epsilon = 1e-8f,
        )

        // Then
        for (index in coefficients.a.indices) {
            assertClose(1f, coefficients.a[index], "a index=$index")
            assertClose(0f, coefficients.b[index], "b index=$index")
        }
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaRefineTest*"`
Expected: 컴파일 실패 — `Unresolved reference: guidedCoefficients`

- [ ] **Step 3: 구현한다**

```kotlin
internal class GuidedCoefficients(
    val a: FloatArray,
    val b: FloatArray,
)

/**
 * 창마다 `q = a·I + b` 의 계수를 구하고 그 계수를 다시 창 평균한다.
 *
 * `a` 는 안내자와 입력의 공분산을 안내자의 분산으로 나눈 값이라 **안내자에 경계가 있는 자리에서만
 * 커진다.** [epsilon] 이 크면 `a` 가 눌려 평균 필터로 퇴화한다.
 *
 * 근거는 `specs/2026-08-25-segmentation-alpha-refinement.md` 「설계 - 정련 알고리즘」에 있다.
 */
internal fun guidedCoefficients(
    guidance: FloatArray,
    input: FloatArray,
    width: Int,
    height: Int,
    radius: Int,
    epsilon: Float,
    checkCancelled: () -> Unit = {},
): GuidedCoefficients {
    val meanGuidance = boxMean(guidance, width, height, radius, checkCancelled)
    val meanInput = boxMean(input, width, height, radius, checkCancelled)
    val meanSquare = boxMean(
        FloatArray(guidance.size) { index -> guidance[index] * guidance[index] },
        width,
        height,
        radius,
        checkCancelled,
    )
    val meanProduct = boxMean(
        FloatArray(guidance.size) { index -> guidance[index] * input[index] },
        width,
        height,
        radius,
        checkCancelled,
    )

    val a = FloatArray(guidance.size)
    val b = FloatArray(guidance.size)
    for (y in 0 until height) {
        checkCancelled()
        for (x in 0 until width) {
            val index = y * width + x
            // 부동소수 오차로 음수가 나올 수 있다. 음수 분산은 a 의 부호를 뒤집는다
            val variance = maxOf(0f, meanSquare[index] - meanGuidance[index] * meanGuidance[index])
            val covariance = meanProduct[index] - meanGuidance[index] * meanInput[index]
            a[index] = covariance / (variance + epsilon)
            b[index] = meanInput[index] - a[index] * meanGuidance[index]
        }
    }

    return GuidedCoefficients(
        a = boxMean(a, width, height, radius, checkCancelled),
        b = boxMean(b, width, height, radius, checkCancelled),
    )
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

⚠️ **마지막 두 `boxMean`을 지우면 `constantGuidance`가 깨진다.** 그 테스트가 기대하는 것이
"두 번 평균한 값"이기 때문이다. (`inputEqualsGuidance`는 이 변이를 잡지 못한다 — 평균을 한 번
빼도 `a = 1`, `b = 0`이 그대로 나온다. 최종 리뷰가 실측으로 정정한 사항이다.) 그 두 줄이 가이드
필터의 핵심이므로 **테스트가 빨개지면 기대값이 아니라 구현을 의심해라.**

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 가이드 필터 계수를 구한다"
```

---

## Task 4: 계수 되올림과 적용

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaRefine.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaRefineTest.kt`

**Interfaces:**
- Consumes: `GuidedCoefficients` (Task 3)
- Produces: `internal fun applyCoefficients(alpha: ByteArray, guidance: IntArray, coefficients: GuidedCoefficients, width: Int, height: Int, subWidth: Int, subHeight: Int, factor: Int, checkCancelled: () -> Unit = {}): Boolean`

⚠️ **안내자를 `IntArray` ARGB로 받아 픽셀마다 휘도를 즉석 계산한다.** 원본 해상도 `FloatArray`를
만들면 12MP에서 48MB가 붙는다(OQ-P-300).

- [ ] **Step 1: 실패하는 테스트를 쓴다**

파일 최상위에 헬퍼를 더한다:

```kotlin
/** 회색 픽셀. 휘도가 `value / 255` 다 */
private fun gray(value: Int): Int = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
```

클래스 안에 넷을 더한다:

```kotlin
    @Test
    fun applyCoefficients_identityCoefficients_writeTheGuidanceAsAlpha() {
        // Given — a = 1, b = 0 이면 알파가 안내자 휘도 그대로여야 한다
        val alpha = ByteArray(4)
        val guidance = intArrayOf(gray(0), gray(128), gray(255), gray(64))
        val coefficients = GuidedCoefficients(a = FloatArray(4) { 1f }, b = FloatArray(4))

        // When
        val changed = applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 2,
            height = 2,
            subWidth = 2,
            subHeight = 2,
            factor = 1,
        )

        // Then
        assertEquals(true, changed)
        assertEquals(0, alpha[0].toInt() and 0xFF)
        assertEquals(128, alpha[1].toInt() and 0xFF)
        assertEquals(255, alpha[2].toInt() and 0xFF)
        assertEquals(64, alpha[3].toInt() and 0xFF)
    }

    @Test
    fun applyCoefficients_resultMatchesTheCurrentAlpha_reportsNoChange() {
        // Given — 호출부가 이 값으로 원본 판 재사용을 판정한다. 늘 참이면 그 경로가 죽는다
        val alpha = ByteArray(2) { 255.toByte() }
        val guidance = intArrayOf(gray(255), gray(255))
        val coefficients = GuidedCoefficients(a = FloatArray(2) { 1f }, b = FloatArray(2))

        // When
        val changed = applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 2,
            height = 1,
            subWidth = 2,
            subHeight = 1,
            factor = 1,
        )

        // Then
        assertEquals(false, changed)
    }

    @Test
    fun applyCoefficients_outOfRangeResult_isClampedInsteadOfWrapping() {
        // Given — 자르지 않으면 바이트가 감겨 반대 값이 된다
        val alpha = ByteArray(2)
        val guidance = intArrayOf(gray(255), gray(255))
        val coefficients = GuidedCoefficients(a = floatArrayOf(4f, -4f), b = FloatArray(2))

        // When
        applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 2,
            height = 1,
            subWidth = 2,
            subHeight = 1,
            factor = 1,
        )

        // Then
        assertEquals(255, alpha[0].toInt() and 0xFF)
        assertEquals(0, alpha[1].toInt() and 0xFF)
    }

    @Test
    fun applyCoefficients_verticallyUpscaledCoefficients_interpolateBetweenRows() {
        // Given — 세로로만 변하는 계수. 세로 보간을 빠뜨리면 계단 둘만 나온다
        val alpha = ByteArray(8)
        val guidance = IntArray(8) { gray(255) }
        val coefficients = GuidedCoefficients(a = FloatArray(2), b = floatArrayOf(0f, 1f))

        // When
        applyCoefficients(
            alpha = alpha,
            guidance = guidance,
            coefficients = coefficients,
            width = 1,
            height = 8,
            subWidth = 1,
            subHeight = 2,
            factor = 4,
        )

        // Then
        val values = IntArray(8) { alpha[it].toInt() and 0xFF }
        for (index in 1 until 8) {
            assertTrue(values[index] >= values[index - 1], "index=$index values=${values.toList()}")
        }
        assertTrue(values.toSet().size > 2, "nearest 되올림이면 값이 둘뿐이다 values=${values.toList()}")
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaRefineTest*"`
Expected: 컴파일 실패 — `Unresolved reference: applyCoefficients`

- [ ] **Step 3: 구현한다**

파일 상단 import에 `kotlin.math.roundToInt`를 더한다.

```kotlin
/**
 * 축소판 계수를 이중선형으로 되올려 원본 알파에 적용한다.
 *
 * **경계 선명도는 이 단계에서 나온다** — 계수는 저주파라 축소해도 되지만 곱해지는 안내자는 원본
 * 해상도다. nearest 로 되올리면 계수 자체의 블록 경계가 알파에 찍힌다.
 *
 * @param guidance ARGB. 휘도를 픽셀마다 즉석 계산한다 — 원본 해상도 실수 배열을 만들지 않는다
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal fun applyCoefficients(
    alpha: ByteArray,
    guidance: IntArray,
    coefficients: GuidedCoefficients,
    width: Int,
    height: Int,
    subWidth: Int,
    subHeight: Int,
    factor: Int,
    checkCancelled: () -> Unit = {},
): Boolean {
    var changed = false

    for (y in 0 until height) {
        checkCancelled()
        val sourceY = ((y + 0.5f) / factor - 0.5f).coerceIn(0f, (subHeight - 1).toFloat())
        val topRow = sourceY.toInt()
        val bottomRow = minOf(topRow + 1, subHeight - 1)
        val weightY = sourceY - topRow
        val rowOffset = y * width

        for (x in 0 until width) {
            val sourceX = ((x + 0.5f) / factor - 0.5f).coerceIn(0f, (subWidth - 1).toFloat())
            val leftColumn = sourceX.toInt()
            val rightColumn = minOf(leftColumn + 1, subWidth - 1)
            val weightX = sourceX - leftColumn

            val slope = bilinear(
                values = coefficients.a,
                stride = subWidth,
                leftColumn = leftColumn,
                rightColumn = rightColumn,
                topRow = topRow,
                bottomRow = bottomRow,
                weightX = weightX,
                weightY = weightY,
            )
            val offset = bilinear(
                values = coefficients.b,
                stride = subWidth,
                leftColumn = leftColumn,
                rightColumn = rightColumn,
                topRow = topRow,
                bottomRow = bottomRow,
                weightX = weightX,
                weightY = weightY,
            )

            val index = rowOffset + x
            val refined = slope * luminanceOf(guidance[index]) + offset
            val value = (refined * OPAQUE).roundToInt().coerceIn(0, 255)
            if (value != (alpha[index].toInt() and 0xFF)) {
                alpha[index] = value.toByte()
                changed = true
            }
        }
    }

    return changed
}

private fun bilinear(
    values: FloatArray,
    stride: Int,
    leftColumn: Int,
    rightColumn: Int,
    topRow: Int,
    bottomRow: Int,
    weightX: Float,
    weightY: Float,
): Float {
    val topOffset = topRow * stride
    val bottomOffset = bottomRow * stride
    val top = values[topOffset + leftColumn] * (1f - weightX) +
        values[topOffset + rightColumn] * weightX
    val bottom = values[bottomOffset + leftColumn] * (1f - weightX) +
        values[bottomOffset + rightColumn] * weightX
    return top * (1f - weightY) + bottom * weightY
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 축소판 계수를 되올려 원본 알파에 적용한다"
```

---

## Task 5: 정련 조립

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaRefine.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaRefineTest.kt`

**Interfaces:**
- Consumes: Task 1~4의 함수 전부
- Produces: `internal fun refineAlpha(alpha: ByteArray, guidance: IntArray, width: Int, height: Int, downscale: Int, radius: Int, epsilon: Float, checkCancelled: () -> Unit = {}): Boolean`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

파일 최상위에 헬퍼 둘을 더한다:

```kotlin
/** 왼쪽 [darkColumns] 칸이 검고 나머지가 흰 안내자 */
private fun splitGuidance(
    width: Int,
    height: Int,
    darkColumns: Int,
) = IntArray(width * height) { index -> if (index % width < darkColumns) gray(0) else gray(255) }

/** [opaqueFrom] 칸부터 오른쪽 끝까지 불투명한 알파 */
private fun maskFrom(
    width: Int,
    height: Int,
    opaqueFrom: Int,
) = ByteArray(width * height) { index -> if (index % width >= opaqueFrom) 255.toByte() else 0 }
```

클래스 안에 넷을 더한다:

```kotlin
    @Test
    fun refineAlpha_maskOverhangsIntoTheDarkSide_pullsTheEdgeBackToTheColourEdge() {
        // Given — 색 경계는 16 인데 마스크가 13 까지 넘어와 배경 3칸을 물고 있다
        val guided = maskFrom(width = 32, height = 8, opaqueFrom = 13)
        val flat = maskFrom(width = 32, height = 8, opaqueFrom = 13)

        // When — 같은 마스크를 색 경계가 있는 안내자와 균일한 안내자로 각각 정련한다
        refineAlpha(
            alpha = guided,
            guidance = splitGuidance(width = 32, height = 8, darkColumns = 16),
            width = 32,
            height = 8,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )
        refineAlpha(
            alpha = flat,
            guidance = IntArray(32 * 8) { gray(255) },
            width = 32,
            height = 8,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )

        // Then — 배경을 물고 있던 자리가 색 안내자 쪽에서 더 투명해진다.
        // 안내자를 무시하는 구현이면 두 값이 같아 이 단언이 깨진다
        val overhang = 4 * 32 + 14
        assertTrue(
            (guided[overhang].toInt() and 0xFF) < (flat[overhang].toInt() and 0xFF),
            "guided=${guided[overhang].toInt() and 0xFF} flat=${flat[overhang].toInt() and 0xFF}",
        )
    }

    @Test
    fun refineAlpha_insideTheSubject_staysOpaque() {
        // Given — 정련이 내부까지 반투명하게 만들면 안 된다.
        // 탐침 자리를 경계에서 창 하나 안쪽(20)에 둔다 — 끝(28)에 두면 계수가 평탄해져
        // 마지막 창 평균을 지우는 변이를 못 잡는다
        val alpha = maskFrom(width = 32, height = 8, opaqueFrom = 13)

        // When
        refineAlpha(
            alpha = alpha,
            guidance = splitGuidance(width = 32, height = 8, darkColumns = 16),
            width = 32,
            height = 8,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )

        // Then
        val inside = 4 * 32 + 20
        assertTrue((alpha[inside].toInt() and 0xFF) > 250, "value=${alpha[inside].toInt() and 0xFF}")
    }

    @Test
    fun refineAlpha_misalignedHardEdge_becomesASoftTransition() {
        // Given — 이 라운드의 목적이다. 정련 전에는 0 과 255 뿐이다.
        // ⚠️ 마스크 경계를 색 경계와 어긋나게 둔다. 같은 자리(p ≡ I)면 가이드 필터는 경계를
        // **일부러 보존하므로** 부분 알파가 생기지 않는다
        val alpha = maskFrom(width = 32, height = 8, opaqueFrom = 13)

        // When
        refineAlpha(
            alpha = alpha,
            guidance = splitGuidance(width = 32, height = 8, darkColumns = 16),
            width = 32,
            height = 8,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )

        // Then
        val partial = alpha.count { (it.toInt() and 0xFF) in 1..254 }
        assertTrue(partial > 0, "partial=$partial")
    }

    @Test
    fun refineAlpha_downscaledCoefficients_keepTheEdgeAtTheSamePlace() {
        // Given — 계수를 축소판에서 구해도 경계 위치가 밀리면 안 된다.
        // 두 설정의 유효 창은 같지 않다(원본 기준 9 대 12). 그래도 경계는 2칸 안에 들어야 한다
        val fullScale = maskFrom(width = 64, height = 16, opaqueFrom = 28)
        val downscaled = maskFrom(width = 64, height = 16, opaqueFrom = 28)
        val guidance = splitGuidance(width = 64, height = 16, darkColumns = 32)

        // When
        refineAlpha(
            alpha = fullScale,
            guidance = guidance,
            width = 64,
            height = 16,
            downscale = 1,
            radius = 4,
            epsilon = 1e-4f,
        )
        refineAlpha(
            alpha = downscaled,
            guidance = guidance,
            width = 64,
            height = 16,
            downscale = 4,
            radius = 1,
            epsilon = 1e-4f,
        )

        // Then — 가운데 행에서 알파가 128 을 넘는 첫 칸이 두 칸 이상 어긋나지 않는다
        val row = 8 * 64
        val fullCrossing = (0 until 64).first { (fullScale[row + it].toInt() and 0xFF) > 128 }
        val downCrossing = (0 until 64).first { (downscaled[row + it].toInt() and 0xFF) > 128 }
        assertTrue(abs(fullCrossing - downCrossing) <= 2, "full=$fullCrossing down=$downCrossing")
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaRefineTest*"`
Expected: 컴파일 실패 — `Unresolved reference: refineAlpha`

- [ ] **Step 3: 구현한다**

```kotlin
/**
 * 원본 휘도를 안내자로 알파 경계를 정련한다. 알파를 **그 자리에서** 고친다.
 *
 * 계수는 [downscale] 배율 축소판에서 구하고 적용만 원본 해상도에서 한다. 근거와 전체 설계는
 * `specs/2026-08-25-segmentation-alpha-refinement.md` 참고.
 *
 * @param guidance [alpha] 와 같은 크기·같은 좌표계의 ARGB. **ML Kit 이 배경을 도려낸 판이 아니라
 *   원본 사진에서 읽은 것이어야 한다** — 도려낸 판을 주면 안내자 경계가 알파 경계와 겹쳐
 *   정련이 지금 경계를 그대로 재현한다
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal fun refineAlpha(
    alpha: ByteArray,
    guidance: IntArray,
    width: Int,
    height: Int,
    downscale: Int,
    radius: Int,
    epsilon: Float,
    checkCancelled: () -> Unit = {},
): Boolean {
    require(alpha.size == width * height) {
        "alpha length ${alpha.size} does not match ${width}x$height"
    }
    require(guidance.size == alpha.size) {
        "guidance length ${guidance.size} does not match alpha length ${alpha.size}"
    }
    require(downscale >= 1) { "downscale must be at least 1 but was $downscale" }
    require(radius >= 1) { "radius must be at least 1 but was $radius" }
    require(epsilon > 0f) { "epsilon must be positive but was $epsilon" }
    if (width <= 0 || height <= 0) return false

    val subWidth = ceilDiv(width, downscale)
    val subHeight = ceilDiv(height, downscale)

    val coefficients = guidedCoefficients(
        guidance = downscaleLuminance(guidance, width, height, downscale, checkCancelled),
        input = downscaleAlpha(alpha, width, height, downscale, checkCancelled),
        width = subWidth,
        height = subHeight,
        radius = radius,
        epsilon = epsilon,
        checkCancelled = checkCancelled,
    )

    return applyCoefficients(
        alpha = alpha,
        guidance = guidance,
        coefficients = coefficients,
        width = width,
        height = height,
        subWidth = subWidth,
        subHeight = subHeight,
        factor = downscale,
        checkCancelled = checkCancelled,
    )
}
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

⚠️ `pullsTheEdgeBackToTheColourEdge`가 깨지면 안내자가 계수에 반영되지 않은 것이다. **값을 조정하지
말고 보고해라** — 그 테스트가 이 기능의 존재 이유다.

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 안내자로 알파 경계를 정련한다"
```

---

## Task 6: 커널 배선

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessor.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessorTest.kt`

**Interfaces:**
- Consumes: `refineAlpha` (Task 5)
- Produces: `internal fun interface GuidanceProvider`,
  `AlphaPostProcessOptions.refineEdges/refineDownscale/refineRadius/refineEpsilon`,
  `AlphaPostProcessResult.refineElapsedNanos`,
  `postProcessAlpha(alpha, width, height, options, guidance, checkCancelled)`

⚠️ **`SegmentationMask.kt`의 호출부 수정이 이 태스크에 들어 있다.** 그 파일이 `postProcessAlpha`를
위치 인자로 부르므로, 새 파라미터를 끼우면 같은 커밋 안에서 고치지 않는 한 컴파일이 깨진다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`AlphaPostProcessorTest.kt` 최상위에 헬퍼 둘을 더한다. ⚠️ **마스크를 직사각형으로 두면 안 된다** —
tight bounds가 알파 경계와 정확히 겹쳐 잘라 낸 패치 안의 알파가 상수가 되고, 그러면 공분산이 0이라
정련이 아무것도 하지 않는다:

```kotlin
/** 왼쪽 위가 파인 마스크. bbox 안에 투명 픽셀이 남아야 정련이 일할 거리가 있다 */
private fun notchedMask() = ByteArray(64) { index ->
    val x = index % 8
    val y = index / 8
    if (x >= 3 && !(y < 2 && x < 6)) 255.toByte() else 0
}

/** 원본 좌표 x >= 4 가 흰 안내자 */
private val splitGuidance = GuidanceProvider { bounds ->
    IntArray(bounds.width * bounds.height) { index ->
        if ((index % bounds.width) + bounds.left >= 4) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    }
}
```

클래스 안에 셋을 더한다:

```kotlin
    @Test
    fun postProcessAlpha_refineEnabledWithGuidance_producesPartialAlpha() {
        // Given — 하드 매트에는 부분 알파가 없다
        val alpha = notchedMask()
        val options = AlphaPostProcessOptions(
            downscaleFactor = 1,
            areaOpeningMinPixels = 4,
            erodeEdge = false,
            refineEdges = true,
            refineDownscale = 1,
            refineRadius = 2,
        )

        // When
        val result = postProcessAlpha(
            alpha,
            width = 8,
            height = 8,
            options = options,
            guidance = splitGuidance,
        )

        // Then
        assertEquals(true, result?.changed)
        assertTrue((result?.partialAlphaPixels ?: 0) > 0, "partial=${result?.partialAlphaPixels}")
    }

    @Test
    fun postProcessAlpha_refineDisabled_leavesTheAlphaUntouchedByRefinement() {
        // Given — 같은 입력·같은 안내자로 켜고 끈다. 플래그가 무시되면 두 결과가 같아진다
        val enabled = notchedMask()
        val disabled = notchedMask()
        val base = AlphaPostProcessOptions(
            downscaleFactor = 1,
            areaOpeningMinPixels = 4,
            erodeEdge = false,
            refineDownscale = 1,
            refineRadius = 2,
        )

        // When
        postProcessAlpha(
            enabled,
            width = 8,
            height = 8,
            options = base.copy(refineEdges = true),
            guidance = splitGuidance,
        )
        postProcessAlpha(
            disabled,
            width = 8,
            height = 8,
            options = base.copy(refineEdges = false),
            guidance = splitGuidance,
        )

        // Then
        assertTrue(
            !enabled.asInts().contentEquals(disabled.asInts()),
            "refineEdges 가 무시됐다 enabled=${enabled.asInts().toList()}",
        )
    }

    @Test
    fun postProcessAlpha_refineEnabledWithoutGuidance_skipsRefinement() {
        // Given — 안내자를 못 대는 호출부가 커널을 그대로 쓸 수 있어야 한다
        val withoutGuidance = notchedMask()
        val disabled = notchedMask()
        val base = AlphaPostProcessOptions(
            downscaleFactor = 1,
            areaOpeningMinPixels = 4,
            erodeEdge = false,
            refineDownscale = 1,
            refineRadius = 2,
        )

        // When
        postProcessAlpha(withoutGuidance, width = 8, height = 8, options = base.copy(refineEdges = true))
        postProcessAlpha(disabled, width = 8, height = 8, options = base.copy(refineEdges = false))

        // Then
        assertContentEquals(disabled.asInts(), withoutGuidance.asInts())
    }
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*AlphaPostProcessorTest*"`
Expected: 컴파일 실패 — `Unresolved reference: refineEdges`

- [ ] **Step 3: 옵션·공급자·결과 필드를 더한다**

`AlphaPostProcessor.kt`:

```kotlin
/** 정련 계수를 구할 배율. 판정 버퍼 배율(`downscaleFactor`)과 **별개 값**이다 */
internal const val REFINE_DOWNSCALE = 4

/** 축소판 기준 창 반경. 값의 근거는 `synthesis/open-questions.md` OQ-P-298 */
internal const val REFINE_RADIUS = 2

/** 정칙화. 작을수록 안내자를 바싹 따라간다. 근거는 OQ-P-298 */
internal const val REFINE_EPSILON = 1e-4f
```

`AlphaPostProcessOptions`에 넷을 더한다(기존 다섯 필드는 그대로):

```kotlin
    val refineEdges: Boolean = true,
    val refineDownscale: Int = REFINE_DOWNSCALE,
    val refineRadius: Int = REFINE_RADIUS,
    val refineEpsilon: Float = REFINE_EPSILON,
```

`AlphaPostProcessResult`에 하나를 더한다:

```kotlin
    /** 정련에 든 시간. 안 돌았으면 0 이다. 원본 해상도 적용이 감당 가능한지 판정할 근거다 */
    val refineElapsedNanos: Long,
```

공급자를 더한다:

```kotlin
/**
 * 정련이 쓸 안내자를 공급한다. 커널이 `Bitmap` 을 모른다는 원칙을 지키면서 두 경로가 서로 다른
 * 방식으로 픽셀을 대게 하는 통로다.
 */
internal fun interface GuidanceProvider {
    /** [bounds] 크기의 ARGB. 행 우선이고 stride 는 `bounds.width` 다 */
    fun pixelsIn(bounds: SegmentationBounds): IntArray
}
```

- [ ] **Step 4: 조립부를 고친다**

`postProcessAlpha` 시그니처에 `guidance`를 더하고, 「손대는 기존 코드의 실제 형태」에 옮겨 둔
조립부를 아래로 치환한다. ⚠️ **1차 측정은 정련이 실제로 도는 경우에만 한다** — 안 그러면 정련을
꺼도 원본 해상도 순회가 하나 늘어난다:

```kotlin
internal fun postProcessAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    guidance: GuidanceProvider? = null,
    checkCancelled: () -> Unit = {},
): AlphaPostProcessResult? {
```

```kotlin
    val applied = applyKeepMask(alpha, width, height, keep, maskWidth, factor, checkCancelled)

    // 정련이 훑을 영역을 먼저 정한다. 창 통계를 내는 연산이라 빈 여백까지 훑으면 값 없이 비싸다
    val beforeRefine = if (options.refineEdges && guidance != null) {
        measureAlpha(alpha, width, height, checkCancelled) ?: return null
    } else {
        null
    }

    val startedAt = System.nanoTime()
    val refined = beforeRefine != null && guidance != null &&
        refineWithin(alpha, width, beforeRefine.bounds, guidance, options, checkCancelled)
    val refineElapsedNanos = if (beforeRefine != null) System.nanoTime() - startedAt else 0L

    val eroded = options.erodeEdge && erodeEdge(alpha, width, height, checkCancelled)

    val measured = if (beforeRefine != null && !refined && !eroded) {
        beforeRefine
    } else {
        measureAlpha(alpha, width, height, checkCancelled) ?: return null
    }

    return AlphaPostProcessResult(
        bounds = measured.bounds,
        alphaSum = measured.alphaSum,
        partialAlphaPixels = measured.partialAlphaPixels,
        changed = applied || refined || eroded,
        refineElapsedNanos = refineElapsedNanos,
    )
```

같은 파일에 private 헬퍼를 더한다:

```kotlin
/**
 * [bounds] 사각형만 잘라 정련하고 되쓴다. 잘라 내는 이유는 [refineAlpha] 가 연속된 배열을
 * 전제하기 때문이고, 그 전제를 유지하는 편이 stride 를 함수 넷에 실어 나르는 것보다 싸다.
 */
private fun refineWithin(
    alpha: ByteArray,
    rowStride: Int,
    bounds: SegmentationBounds,
    guidance: GuidanceProvider,
    options: AlphaPostProcessOptions,
    checkCancelled: () -> Unit,
): Boolean {
    val patch = ByteArray(bounds.width * bounds.height)
    for (y in 0 until bounds.height) {
        val source = (bounds.top + y) * rowStride + bounds.left
        alpha.copyInto(patch, y * bounds.width, source, source + bounds.width)
    }

    val changed = refineAlpha(
        alpha = patch,
        guidance = guidance.pixelsIn(bounds),
        width = bounds.width,
        height = bounds.height,
        downscale = options.refineDownscale,
        radius = options.refineRadius,
        epsilon = options.refineEpsilon,
        checkCancelled = checkCancelled,
    )
    if (!changed) return false

    for (y in 0 until bounds.height) {
        val target = (bounds.top + y) * rowStride + bounds.left
        patch.copyInto(alpha, target, y * bounds.width, y * bounds.width + bounds.width)
    }

    return true
}
```

`postProcessAlpha`의 KDoc `@return`에 있는 "침식 단계에서 전멸했다면" 문구를 **"정련이나 침식
단계에서 전멸했다면"으로 고친다.** 정련도 알파를 지울 수 있게 됐다.

- [ ] **Step 5: 깨진 호출부를 같은 커밋에서 고친다**

`SegmentationMask.kt`의 위치 인자 호출을 named로 바꾼다. 이 한 줄을 안 고치면 이 태스크의 게이트가
통과하지 못한다:

```kotlin
    val result = postProcessAlpha(
        alpha,
        width,
        height,
        options,
        checkCancelled = checkCancelled,
    ) ?: return null
```

- [ ] **Step 6: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 7: 커밋한다**

```bash
git add -A
git commit -m "feat: 후처리 커널에 경계 정련 단계를 끼운다"
```

---

## Task 7: 호출부 배선과 관측

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`

**Interfaces:**
- Consumes: `GuidanceProvider`, `AlphaPostProcessResult.refineElapsedNanos` (Task 6)
- Produces: 없음 (private)

**테스트가 없는 이유:** 이 코드는 `Bitmap.getPixels`를 부른다. 저장소에 Robolectric이 없고
`isReturnDefaultValues`도 꺼져 있어 JVM 유닛에서 터진다. 판단은 전부 Task 1~6의 순수 함수로 빠져
있고 여기 남는 것은 사각형 좌표 산술과 로그다.

- [ ] **Step 1: 폴백 경로가 안내자를 넘기게 한다**

`SegmentationMask.kt`의 `maskSubjectAlpha`에 파라미터를 더하고(기본값 `null`) 본문에서 그대로
넘긴다. 호출부는 프로덕션 1곳·테스트 4곳이고 **전부 named 인자라 안전하다:**

```kotlin
internal fun maskSubjectAlpha(
    mask: FloatBuffer,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    guidance: GuidanceProvider? = null,
    checkCancelled: () -> Unit = {},
): MaskedAlpha? {
```

본문의 호출에 `guidance = guidance`를 더한다.

- [ ] **Step 2: 주 경로에 원본 안내자를 댄다**

⚠️ **`pixels`(후보 판)를 안내자로 쓰면 안 된다.** 그 판은 ML Kit이 이미 배경을 도려낸 것이라
안내자 경계가 지금 알파 경계와 겹치고, 그러면 정련이 지금 경계를 그대로 재현한다. `origin`에서
`subject` 오프셋을 더해 읽는다.

`postProcess` 안의 `postProcessAlpha` 호출과 그 뒤 로그를 아래로 바꾼다. **기존 전멸 로그와 부분
알파 로그를 지우지 말고 이 형태로 합친다** — 둘 다 앞 라운드가 미결 판정용으로 심은 것이다:

```kotlin
        val result = postProcessAlpha(
            alpha,
            width,
            height,
            guidance = { bounds ->
                IntArray(bounds.width * bounds.height).also { pixels ->
                    origin.getPixels(
                        pixels,
                        0,
                        bounds.width,
                        subject.startX + bounds.left,
                        subject.startY + bounds.top,
                        bounds.width,
                        bounds.height,
                    )
                }
            },
            checkCancelled = checkCancelled,
        ) ?: run {
            // 후처리 이전 알파는 있었는데(비었으면 애초에 후보가 안 됐다) 커널이 전부 지웠다는
            // 뜻이다 — OOM 되돌림과 달리 임계 튜닝 신호로 값이 있다
            repositoryLogger.i {
                "세그멘테이션 후처리가 후보 ${width}x$height 판의 알파를 전부 지워 원본으로 되돌린다"
            }
            return null
        }

        repositoryLogger.i {
            "세그멘테이션 후보 부분 알파 ${result.partialAlphaPixels}/${width * height}, " +
                "정련 ${result.refineElapsedNanos / 1_000_000}ms"
        }
```

- [ ] **Step 3: 폴백 경로에 원본 안내자를 댄다**

`toForegroundCandidate`의 `maskSubjectAlpha` 호출에 안내자를 더한다. 알파가 원본 좌표계이므로
사각형도 원본 좌표계다:

```kotlin
        val masked = try {
            maskSubjectAlpha(
                foregroundMask,
                width,
                height,
                guidance = { bounds ->
                    IntArray(bounds.width * bounds.height).also { pixels ->
                        origin.getPixels(
                            pixels,
                            0,
                            bounds.width,
                            bounds.left,
                            bounds.top,
                            bounds.width,
                            bounds.height,
                        )
                    }
                },
                checkCancelled = checkCancelled,
            )
        } catch (e: OutOfMemoryError) {
```

같은 함수의 부분 알파 로그에 정련 시간을 더한다:

```kotlin
        repositoryLogger.i {
            "세그멘테이션 폴백 부분 알파 ${masked.result.partialAlphaPixels}/${width * height}, " +
                "정련 ${masked.result.refineElapsedNanos / 1_000_000}ms"
        }
```

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat: 두 경로에 원본 안내자를 대고 정련 시간을 남긴다"
```

---

## 자체 점검

**스펙 커버리지** — 「설계」의 여섯 절이 각각 태스크로 간다. 정련 알고리즘 → Task 3·4·5, 처리
해상도 → Task 2·4, 파이프라인 자리 → Task 6, 안내자 공급 → Task 6·7, API/인터페이스 → Task 6,
관측 → Task 6(시간 측정)·7(로그). 「테스트」 절 여섯 항목은 Task 1(박스 합)·3(계수 퇴화
둘)·5(경계 이동·반대 방향 비교·배율 일치)·6(끄면 달라진다)에 대응한다.

**타입 일관성** — `GuidedCoefficients`는 Task 3이 만들고 4·5가 쓴다. `GuidanceProvider`와
`refineElapsedNanos`는 Task 6이 만들고 7이 쓴다. `ceilDiv`는 기존 것을 재사용한다(Task 2·5).
`gray` 헬퍼는 Task 4가 만들고 5가 쓴다. `OPAQUE`는 `AlphaRefine.kt` 파일 private이고
`AlphaPostProcessor.kt`의 동명 상수와 파일이 달라 충돌하지 않는다.

**검수가 잡은 변이 생존 자리** — 세로 보간(Task 4의 세로 되올림 테스트), 계수의 마지막 창
평균(Task 3의 `constantGuidance` 기대값), `changed` 항상 참(Task 4의 무변화 테스트),
녹·청 계수 교환(Task 2의 순수색 테스트), 축소의 세로 인덱싱(Task 2의 3×3 테스트), `refineEdges`
플래그(Task 6의 켬·끔 비교)가 각각 대응한다.

**최종 리뷰가 정정한 것** — Task 5의 탐침 위치 20은 마지막 창 평균 변이를 잡지 못한다(두 경우 다
255다). 그 변이를 실제로 죽이는 것은 Task 3의 `constantGuidance` 하나다. 그리고 Task 5의 배율
왕복 테스트(`keepTheEdgeAtTheSamePlace`)는 반픽셀 중심 정렬 삭제와 가로 보간 삭제를 **둘 다
통과한다.** 가로 축 되올림은 별도 대칭 테스트로 덮었다(최종 리뷰 뒤 수정 커밋).

**남는 것** — 사진 세트 판정(OQ-P-296·297·298·299·300)은 이 계획에 넣지 않는다. 실기기에서 사람이
돌려야 한다.
