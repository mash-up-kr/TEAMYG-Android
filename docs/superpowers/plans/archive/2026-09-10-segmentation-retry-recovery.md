---
id: segmentation-retry-recovery
title: 세그멘테이션 재시도 회복 구현 계획 (8 Task)
status: done
type: work-order
created: 2026-09-10
updated: 2026-09-10
platforms: android
owner: android
related_adr: ADR-0012
related_spec: segmentation-retry-recovery, segmentation-preprocessing, c103-error-use-original
related_code: ImageSegmentationRepositoryImpl#segmentImage, ImageSegmentationRepositoryImpl#segmentForeground, ImageSegmentationRepositoryImpl#recoverCandidates, SegmentationCandidateHarvest.kt#harvestSubjects, SegmentationCandidateHarvest.kt#harvestForeground, SegmentationRecoveryNormalizer.kt#normalizeForDetection, SegmentationMask.kt#maskSubjectAlpha, AlphaPostProcessor.kt#postProcessAlpha, AlphaComposite.kt#composeCroppedArgb, SegmentationCandidateFilter.kt#filterCandidates, SubjectCoverage.kt#floorPixels, SegmentationViewModel.kt#loadCandidates, SegmentationViewModel.kt#recover
archived_reason: develop 머지(PR #487 `95b7fc4d5`, 2026-09-10)
tags: [plan, parfait]
---

# 세그멘테이션 재시도 회복 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ✅ **완료·develop 머지(2026-09-10, PR #487 `95b7fc4d5`, 트리 = 브랜치 팁)**: Task 1~7과 Task 8의 자동 검증까지
> 수행했고 커밋은 `d55880fb0`~`3217e62f7` 15개다. **머지본이 계획과 갈린 자리는 넷이다.**
> ① 신규 유닛이 46건이 아니라 52건이다(`isLongSideCapped` 4건, `projectAlpha` 2건 추가).
> ② Task 1·2가 최상위 `const`로 둔 상수와 좌표·단계 타입이 구현 뒤 정리에서 `data/model/image/`(파일 하나에 선언
> 하나)와 `SegmentationRecoverySpec`·`SegmentationContrastSpec`·`SegmentationMaskSpec` object로 옮겨 갔다. 아래 코드
> 블록의 `DETECTION_MIN_SHORT_SIDE`·`LUMINANCE_LEVELS` 같은 최상위 이름은 당시 이름이다.
> ③ 최종 리뷰가 수정 3커밋(`capped` 판정, `CancellationException` 재던짐, `projectAlpha`)을 더 얹었다. 세부는 대응
> 스펙의 as-built 배너에 있다.
> ④ 계획에 없던 마지막 커밋 `3217e62f7`이 실패 화면의 「편집 없이 사용」을 「직접 편집」으로 바꿨다(유닛 +1).
> 그래서 **Task 8 Step 2의 4번 확인 항목은 대상이 사라졌다.**
>
> ⚠️ **체크박스는 실행 세션이 남기지 않아 전부 미체크(43개)다.** 진행의 정본은 `git log`다. **Task 8 Step 2(1차
> 경로 실기기 회귀)는 수행 기록이 없다**(OQ-P-400).

> ⚠️ **계획 검수 2회가 초판을 뒤집었다.** 대비 LUT가 원본에 직접 쓰던 것, 알파 제자리 소거로 되돌림
> 커버리지가 틀리던 것, 회복 되돌림 후보가 불투명 사각형이 되던 것, 2단계 폴백 오프셋 누락, 사다리 격회 반복,
> import 누락이 치명이었다. 스펙도 같은 날 함께 고쳤으므로 **스펙과 이 계획의 시그니처는 일치한다.**

**Goal:** 후보 0건으로 실패한 사진에서 「다시 시도」가 입력을 손봐 가며 다시 검출하게 만든다.

**Architecture:** 저장소에 `recoverCandidates`를 더해 전처리 두 단계를 순서대로 시도한다. 손본 판은 알파를
얻는 데만 쓰고 픽셀은 원본에서 읽는다 — 이 규칙은 `PlateSource`와 `DetectionPlate` 두 타입이 구조적으로
강제한다. 판단은 순수 함수로 빼서 JVM에서 덮는다.

**Tech Stack:** Kotlin, ML Kit Subject Segmentation, Hilt, kotlinx.coroutines, JUnit4 + kotlin.test + MockK + Turbine

**Spec:** [`parfait/specs/archive/2026-09-10-segmentation-retry-recovery.md`](../../specs/archive/2026-09-10-segmentation-retry-recovery.md)

## Global Constraints

- **작업 저장소는 `TJYG-Android`, 브랜치는 `feature/#486-segmentation-error-case`다.** 이 계획 문서만 다른 저장소에 있다.
- **커밋은 각 Task 끝에서 한다.** 이번 계획에서 사용자가 요청했다(이 저장소의 기본은 미커밋). **서브에이전트
  디스패치 프롬프트에 매번 명시한다.** 푸시와 PR은 사용자 승인 전까지 하지 않는다.
- **커밋 메시지 형식** — 접두어(`feat`/`refactor`/`test`) + "~다"로 끝나는 제목, 빈 줄, 한두 줄 본문, 빈 줄,
  `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>`.
- **깨진 Task 경계를 만들지 않는다.** `testDebugUnitTest`가 main 소스셋 컴파일을 선행으로 잡는다.
- **1차 경로 동작** — Task 4는 순수 이동이다. Task 5의 의도한 변경은 둘뿐이다: 캔버스 밖 후보를 버리는 것,
  전경 폴백이 `ModuleNotReady`만 실패로 올리는 것. 그 밖에는 바꾸지 않는다.
- **원본 비트맵에는 쓰지도 회수하지도 않는다.** 크롭도 축소도 없으면 검출 판이 곧 원본 인스턴스이고, 원본은 가변이다.
- **취소 확인 관용구** — `val job = currentCoroutineContext().job`을 함수 머리에서 호이스팅하고 `job.ensureActive()`를
  부른다. `job?.ensureActive()`와 `CoroutineContext.ensureActive()`는 **금지**다(Job이 없으면 조용히 통과한다).
- **테스트 이름은 카멜케이스.** ktlint 최대 줄 길이 120자. `:domain`은 `test`, 나머지는 `testDebugUnitTest`.
- **주석 규약**(`parfait/CLAUDE.md`) — 코드가 이미 말하는 것은 쓰지 않는다. `@return`·`@param`은 타입·이름이
  말하지 못할 때만. 다른 컴포넌트의 현재 상태를 단정하지 않는다.
- **잠정값**(스펙 4-1) — 짧은 변 하한 512, 긴 변 상한 2048, 대비 퍼센타일 1·99, 대비 켬, 힌트 하한
  `AlphaPostProcessOptions.binaryThreshold`, 힌트 여유 20%, 수축 가드 70%(정수 비교), 중앙 폴백은 **짧은 변 70%
  정사각형**, 로그용 완화 배수 1/4(**판정에 쓰지 않는다**), 사다리 상한 30초, 왕복 허용오차 각 축 1px.

---

### Task 1: 회복 계획 순수 계산

좌표 타입, 해상도 목표, 두 단계의 계획과 가드, 힌트, 투영과 교집합, 캔버스 검사를 만든다. 호출부가 없는
순수 추가라 이 Task만으로 컴파일이 닫힌다.

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationRecoveryPlan.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/utils/image/SegmentationRecoveryPlanTest.kt`

**Interfaces:**
- Consumes: `SegmentationBounds`(domain)
- Produces: `DetectionBounds`, `ScaledSize`, `RecoveryTransform`, `RecoveryStage`, `DetectionProjection`,
  `ProjectedRegion`, `resolveTargetSize`, `normalizeStage`, `focusCrop`, `cropAreaPercent`, `focusStage`,
  `hintBounds`, `projectRegion`, `SegmentationBounds.offsetBy`, `isInsideCanvas`, `DETECTION_MIN_SHORT_SIDE`,
  `ROUND_TRIP_TOLERANCE_PX`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`SegmentationRecoveryPlanTest.kt` (24건):

```kotlin
package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SegmentationRecoveryPlanTest {
    private val tenfold = RecoveryTransform(scaleX = 10f, scaleY = 10f, offsetX = 0, offsetY = 0)

    @Test
    fun resolveTargetSize_shortSideBelowTheFloor_upscalesKeepingTheRatio() {
        assertEquals(ScaledSize(width = 683, height = 512), resolveTargetSize(width = 400, height = 300))
    }

    @Test
    fun resolveTargetSize_shortSideExactlyTheFloor_doesNotChange() {
        assertEquals(ScaledSize(width = 683, height = 512), resolveTargetSize(width = 683, height = 512))
    }

    @Test
    fun resolveTargetSize_longSideAboveTheCeiling_downscales() {
        assertEquals(ScaledSize(width = 2048, height = 1536), resolveTargetSize(width = 4032, height = 3024))
    }

    @Test
    fun resolveTargetSize_floorAndCeilingConflict_theCeilingWins() {
        // Given 하한을 맞추면 긴 변이 상한을 넘는 극단 종횡비
        val target = resolveTargetSize(width = 5000, height = 400)

        // Then 상한이 이겨 짧은 변은 하한에 못 미친다
        assertEquals(ScaledSize(width = 2048, height = 164), target)
        assertTrue(target.height < DETECTION_MIN_SHORT_SIDE)
    }

    @Test
    fun normalizeStage_targetEqualsSourceAndNoContrast_isNull() {
        assertNull(normalizeStage(width = 1920, height = 1080, applyContrast = false))
    }

    @Test
    fun normalizeStage_targetEqualsSourceButContrastApplies_isNotNull() {
        assertNotNull(normalizeStage(width = 1920, height = 1080, applyContrast = true))
    }

    @Test
    fun normalizeStage_hasNoCropAndNoOffset() {
        val stage = requireNotNull(normalizeStage(width = 4032, height = 3024, applyContrast = true))

        assertNull(stage.cropRect)
        assertEquals(0, stage.transform.offsetX)
        assertEquals(0, stage.transform.offsetY)
    }

    @Test
    fun hintBounds_onePixelAboveTheThreshold_wrapsItExclusively() {
        val alpha = ByteArray(9)
        alpha[4] = 200.toByte()

        assertEquals(DetectionBounds(1, 1, 2, 2), hintBounds(alpha, width = 3, height = 3, threshold = 127))
    }

    @Test
    fun hintBounds_scatteredPixels_wrapsThemAll() {
        // Given 대각선 양 끝의 두 점 — 최소·최대를 갱신하지 않고 마지막 값을 대입하면 틀린다
        val alpha = ByteArray(9)
        alpha[0] = 200.toByte()
        alpha[8] = 200.toByte()

        assertEquals(DetectionBounds(0, 0, 3, 3), hintBounds(alpha, width = 3, height = 3, threshold = 127))
    }

    @Test
    fun hintBounds_nothingAboveTheThreshold_isNull() {
        assertNull(hintBounds(ByteArray(9), width = 3, height = 3, threshold = 127))
    }

    @Test
    fun hintBounds_valueEqualToTheThreshold_isExcluded() {
        assertNull(hintBounds(ByteArray(4) { 127.toByte() }, width = 2, height = 2, threshold = 127))
    }

    @Test
    fun focusCrop_smallHint_addsTheMarginOnEverySide() {
        val crop = focusCrop(1000, 1000, hint = DetectionBounds(40, 40, 60, 60), hintTransform = tenfold)

        // 원본 좌표 400..600, 크기 200 의 20% 인 40 이 각 변에 붙는다
        assertEquals(SegmentationBounds(left = 360, top = 360, right = 640, bottom = 640), crop)
    }

    @Test
    fun focusCrop_hintTouchesTheEdge_clampsToTheOrigin() {
        val crop = focusCrop(1000, 1000, hint = DetectionBounds(0, 0, 10, 10), hintTransform = tenfold)

        assertEquals(SegmentationBounds(left = 0, top = 0, right = 120, bottom = 120), crop)
    }

    @Test
    fun focusCrop_noHintOnALandscapeOrigin_isACenteredSquareOfTheShortSide() {
        // Given 가로가 긴 원본 — 축마다 70% 로 자르면 2800x2100 이 되어 틀린다
        val crop = focusCrop(4000, 3000, hint = null, hintTransform = null)

        assertEquals(SegmentationBounds(left = 950, top = 450, right = 3050, bottom = 2550), crop)
    }

    @Test
    fun focusStage_cropIsExactlySeventyPercent_isNull() {
        // Given 넓이가 원본의 정확히 70% — 비교를 > 로 바꾸면 통과해 버리는 경계
        assertNull(focusStage(10, 10, crop = SegmentationBounds(0, 0, 7, 10), applyContrast = true))
    }

    @Test
    fun focusStage_cropIsSmaller_carriesTheCropAsTheOffset() {
        val crop = SegmentationBounds(left = 360, top = 360, right = 640, bottom = 640)

        val stage = requireNotNull(focusStage(1000, 1000, crop, applyContrast = true))

        assertEquals(crop, stage.cropRect)
        assertEquals(ScaledSize(512, 512), stage.targetSize)
        assertEquals(360, stage.transform.offsetX)
        assertEquals(360, stage.transform.offsetY)
    }

    @Test
    fun cropAreaPercent_halfTheArea_isFifty() {
        assertEquals(50, cropAreaPercent(SegmentationBounds(0, 0, 50, 100), width = 100, height = 100))
    }

    @Test
    fun recoveryTransform_differentScalesPerAxis_mapsEachAxisWithItsOwnScale() {
        // Given 축마다 배율이 다른 변환 — 같은 배율 픽스처만 있으면 scaleY 대신 scaleX 를 써도 통과한다
        val transform = RecoveryTransform(scaleX = 2f, scaleY = 3f, offsetX = 10, offsetY = 20)

        assertEquals(SegmentationBounds(12, 23, 20, 35), transform.toOrigin(DetectionBounds(1, 1, 5, 5)))
    }

    @Test
    fun recoveryTransform_roundTrip_returnsWithinOnePixel() {
        // Given 상한에 걸려 축소되고 비율도 딱 안 떨어지는 크롭 — 배율이 1 이면 반올림을 안 탄다
        val crop = SegmentationBounds(left = 137, top = 251, right = 4137, bottom = 2918)
        val target = resolveTargetSize(crop.width, crop.height)
        val transform = RecoveryTransform(
            scaleX = crop.width.toFloat() / target.width,
            scaleY = crop.height.toFloat() / target.height,
            offsetX = crop.left,
            offsetY = crop.top,
        )

        val origin = transform.toOrigin(DetectionBounds(0, 0, target.width, target.height))

        assertTrue(abs(origin.left - crop.left) <= ROUND_TRIP_TOLERANCE_PX)
        assertTrue(abs(origin.top - crop.top) <= ROUND_TRIP_TOLERANCE_PX)
        assertTrue(abs(origin.right - crop.right) <= ROUND_TRIP_TOLERANCE_PX)
        assertTrue(abs(origin.bottom - crop.bottom) <= ROUND_TRIP_TOLERANCE_PX)
    }

    @Test
    fun projectRegion_mappedRectCrossesTheCrop_clipsToTheIntersectionAndKeepsTheMappedRect() {
        val projection = DetectionProjection(
            transform = RecoveryTransform(scaleX = 2f, scaleY = 2f, offsetX = 100, offsetY = 100),
            clip = SegmentationBounds(150, 150, 400, 400),
        )

        val projected = requireNotNull(projectRegion(DetectionBounds(0, 0, 100, 100), projection, 1000, 1000))

        // 재표본은 사상 사각형 크기로 하므로 잘리기 전 사각형도 들고 나와야 한다
        assertEquals(SegmentationBounds(100, 100, 300, 300), projected.mapped)
        assertEquals(SegmentationBounds(150, 150, 300, 300), projected.clipped)
    }

    @Test
    fun projectRegion_noOverlapWithTheCrop_isNull() {
        val projection = DetectionProjection(
            transform = RecoveryTransform(scaleX = 1f, scaleY = 1f, offsetX = 0, offsetY = 0),
            clip = SegmentationBounds(500, 500, 600, 600),
        )

        assertNull(projectRegion(DetectionBounds(0, 0, 10, 10), projection, 1000, 1000))
    }

    @Test
    fun offsetBy_movesEveryEdge() {
        assertEquals(SegmentationBounds(11, 22, 13, 24), SegmentationBounds(1, 2, 3, 4).offsetBy(dx = 10, dy = 20))
    }

    @Test
    fun isInsideCanvas_exactFit_isTrue() {
        assertTrue(isInsideCanvas(SegmentationBounds(0, 0, 100, 50), canvasWidth = 100, canvasHeight = 50))
    }

    @Test
    fun isInsideCanvas_anyEdgeOutside_isFalse() {
        assertFalse(isInsideCanvas(SegmentationBounds(-1, 0, 100, 50), canvasWidth = 100, canvasHeight = 50))
        assertFalse(isInsideCanvas(SegmentationBounds(0, -1, 100, 50), canvasWidth = 100, canvasHeight = 50))
        assertFalse(isInsideCanvas(SegmentationBounds(0, 0, 101, 50), canvasWidth = 100, canvasHeight = 50))
        assertFalse(isInsideCanvas(SegmentationBounds(0, 0, 100, 51), canvasWidth = 100, canvasHeight = 50))
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :data:compileDebugUnitTestKotlin`
Expected: FAIL — `Unresolved reference: resolveTargetSize` 등

- [ ] **Step 3: 최소 구현을 쓴다**

`SegmentationRecoveryPlan.kt`:

```kotlin
package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.math.roundToInt

/** ML Kit 가이드가 "at least 512x512" 를 적는다 */
internal const val DETECTION_MIN_SHORT_SIDE = 512

/** 문서 근거가 아니라 자원에서 나온 값이다. 판을 네 번 추론하므로 피크를 여기서 막는다 */
internal const val DETECTION_MAX_LONG_SIDE = 2048

/** 테스트가 구현의 오차에 맞춰지지 않게 여기서 고정한다 */
internal const val ROUND_TRIP_TOLERANCE_PX = 1

private const val FOCUS_MARGIN_RATIO = 0.20f

/** 정수로 비교한다. 부동소수 비율이면 정확히 70% 인 경계가 반올림 오차로 흔들린다 */
private const val FOCUS_SHRINK_CEILING_PERCENT = 70

private const val CENTER_CROP_RATIO = 0.70f

/**
 * 검출 공간의 사각형.
 *
 * `SegmentationBounds` 는 KDoc 이 원본 좌표를 단정하고 있어, 같은 타입으로 두 좌표계를 겸하면 짝이 안 맞는
 * 조합이 컴파일된다.
 */
internal data class DetectionBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal data class ScaledSize(val width: Int, val height: Int)

/**
 * 검출 공간의 좌표를 원본 공간으로 되돌린다.
 *
 * 축마다 배율이 다른 것은 목표 치수를 정수로 반올림하기 때문이다.
 */
internal data class RecoveryTransform(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Int,
    val offsetY: Int,
) {
    fun toOrigin(bounds: DetectionBounds): SegmentationBounds = SegmentationBounds(
        left = offsetX + (bounds.left * scaleX).roundToInt(),
        top = offsetY + (bounds.top * scaleY).roundToInt(),
        right = offsetX + (bounds.right * scaleX).roundToInt(),
        bottom = offsetY + (bounds.bottom * scaleY).roundToInt(),
    )
}

internal data class RecoveryStage(
    /** 널이면 원본 전체를 쓴다. 좌표는 원본 기준이다 */
    val cropRect: SegmentationBounds?,
    val targetSize: ScaledSize,
    val applyContrast: Boolean,
    val transform: RecoveryTransform,
)

/** 검출 공간이 원본에 어떻게 놓이는가. 1차 경로에는 없다 */
internal data class DetectionProjection(
    val transform: RecoveryTransform,
    val clip: SegmentationBounds,
)

/** 재표본은 [mapped] 크기로 하고, 그다음 [clipped] 로 자른다. 순서를 뒤집으면 알파가 어긋난다 */
internal data class ProjectedRegion(
    val mapped: SegmentationBounds,
    val clipped: SegmentationBounds,
)

/**
 * 검출에 쓸 치수. 하한과 상한이 충돌하면 상한이 이긴다 — 확대는 정보를 늘리지 않지만 상한 초과는
 * 메모리로 죽는다. 배율이 하나라 두 축은 언제나 같은 방향으로 움직인다.
 */
internal fun resolveTargetSize(width: Int, height: Int): ScaledSize {
    require(width > 0 && height > 0) { "size must be positive but was ${width}x$height" }

    val floorScale = maxOf(1f, DETECTION_MIN_SHORT_SIDE.toFloat() / minOf(width, height))
    val ceilingScale = DETECTION_MAX_LONG_SIDE.toFloat() / maxOf(width, height)
    val scale = minOf(floorScale, ceilingScale)

    return ScaledSize(
        width = maxOf(1, (width * scale).roundToInt()),
        height = maxOf(1, (height * scale).roundToInt()),
    )
}

/** 크롭 없는 1단계. 목표 치수가 원본과 같고 대비도 안 걸면 1차 경로의 재실행일 뿐이라 널이다 */
internal fun normalizeStage(width: Int, height: Int, applyContrast: Boolean): RecoveryStage? {
    val target = resolveTargetSize(width, height)
    if (!applyContrast && target.width == width && target.height == height) return null

    return RecoveryStage(
        cropRect = null,
        targetSize = target,
        applyContrast = applyContrast,
        transform = RecoveryTransform(
            scaleX = width.toFloat() / target.width,
            scaleY = height.toFloat() / target.height,
            offsetX = 0,
            offsetY = 0,
        ),
    )
}

/** 2단계가 자를 사각형. 힌트가 없으면 짧은 변의 70% 를 한 변으로 하는 중앙 정사각형이다 */
internal fun focusCrop(
    width: Int,
    height: Int,
    hint: DetectionBounds?,
    hintTransform: RecoveryTransform?,
): SegmentationBounds {
    if (hint == null || hintTransform == null) return centerSquare(width, height)

    val origin = hintTransform.toOrigin(hint)
    val marginX = (origin.width * FOCUS_MARGIN_RATIO).roundToInt()
    val marginY = (origin.height * FOCUS_MARGIN_RATIO).roundToInt()

    return SegmentationBounds(
        left = (origin.left - marginX).coerceIn(0, width),
        top = (origin.top - marginY).coerceIn(0, height),
        right = (origin.right + marginX).coerceIn(0, width),
        bottom = (origin.bottom + marginY).coerceIn(0, height),
    )
}

internal fun cropAreaPercent(crop: SegmentationBounds, width: Int, height: Int): Int =
    (crop.width.toLong() * crop.height * 100 / (width.toLong() * height)).toInt()

/** 크롭이 원본의 70% 이상이면 널이다 — 1단계 재탕에 추론만 더 쓰게 된다 */
internal fun focusStage(width: Int, height: Int, crop: SegmentationBounds, applyContrast: Boolean): RecoveryStage? {
    if (crop.width <= 0 || crop.height <= 0) return null

    val cropArea = crop.width.toLong() * crop.height
    if (cropArea * 100 >= width.toLong() * height * FOCUS_SHRINK_CEILING_PERCENT) return null

    val target = resolveTargetSize(crop.width, crop.height)

    return RecoveryStage(
        cropRect = crop,
        targetSize = target,
        applyContrast = applyContrast,
        transform = RecoveryTransform(
            scaleX = crop.width.toFloat() / target.width,
            scaleY = crop.height.toFloat() / target.height,
            offsetX = crop.left,
            offsetY = crop.top,
        ),
    )
}

private fun centerSquare(width: Int, height: Int): SegmentationBounds {
    val side = maxOf(1, (minOf(width, height) * CENTER_CROP_RATIO).roundToInt())
    val left = (width - side) / 2
    val top = (height - side) / 2

    return SegmentationBounds(left = left, top = top, right = left + side, bottom = top + side)
}

/**
 * 임계를 **초과**하는 픽셀을 감싸는 사각형.
 *
 * 임계는 폴백 이진화와 같은 축에서 받는다. 더 높은 축을 쓰면 1단계가 실패한 상황에서 힌트가 구조적으로
 * 거의 항상 빈다.
 */
internal fun hintBounds(alpha: ByteArray, width: Int, height: Int, threshold: Int): DetectionBounds? {
    require(alpha.size == width * height) { "alpha ${alpha.size} does not match ${width}x$height" }

    var left = width
    var top = height
    var right = -1
    var bottom = -1

    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            if ((alpha[row + x].toInt() and 0xFF) <= threshold) continue

            left = minOf(left, x)
            right = maxOf(right, x)
            top = minOf(top, y)
            bottom = maxOf(bottom, y)
        }
    }

    if (right < 0) return null

    return DetectionBounds(left = left, top = top, right = right + 1, bottom = bottom + 1)
}

/**
 * 검출 사각형을 원본에 옮기고 크롭과 원본의 교집합으로 자른다. 교집합이 비면 널이다.
 *
 * 원본 경계만으로 자르면 2단계에서 크롭 밖으로 새는 사각형이 생긴다.
 */
internal fun projectRegion(
    detection: DetectionBounds,
    projection: DetectionProjection,
    width: Int,
    height: Int,
): ProjectedRegion? {
    val mapped = projection.transform.toOrigin(detection)
    if (mapped.width <= 0 || mapped.height <= 0) return null

    val clip = projection.clip
    val left = maxOf(mapped.left, clip.left, 0)
    val top = maxOf(mapped.top, clip.top, 0)
    val right = minOf(mapped.right, clip.right, width)
    val bottom = minOf(mapped.bottom, clip.bottom, height)
    if (right <= left || bottom <= top) return null

    return ProjectedRegion(mapped = mapped, clipped = SegmentationBounds(left, top, right, bottom))
}

internal fun SegmentationBounds.offsetBy(dx: Int, dy: Int): SegmentationBounds =
    SegmentationBounds(left = left + dx, top = top + dy, right = right + dx, bottom = bottom + dy)

/**
 * 판 치수와 사각형 치수를 비교하는 검사는 사각형을 판에서 만들기 때문에 항진명제다. 실제로 깨질 수 있는
 * 것은 이쪽이고, 깨지면 예외가 아니라 `persistSubject` 의 `drawBitmap` 이 조용히 자른다.
 */
internal fun isInsideCanvas(bounds: SegmentationBounds, canvasWidth: Int, canvasHeight: Int): Boolean =
    bounds.left >= 0 && bounds.top >= 0 && bounds.right <= canvasWidth && bounds.bottom <= canvasHeight
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*SegmentationRecoveryPlanTest*"`
Expected: PASS 24건

- [ ] **Step 5: ktlint를 돌린다**

Run: `./gradlew :data:ktlintCheck`
Expected: 통과

- [ ] **Step 6: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationRecoveryPlan.kt \
        data/src/test/java/com/teamyg/parfait/data/utils/image/SegmentationRecoveryPlanTest.kt
git commit -m "$(cat <<'MSG'
feat: 세그멘테이션 회복 단계의 좌표와 해상도 계산을 만든다

검출 공간과 원본 공간을 타입으로 가르고, 두 단계의 계획과 가드를 순수 함수로 둔다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 2: 대비 정규화 LUT

휘도 히스토그램에서 퍼센타일을 잘라 선형 확장 LUT를 만든다. 순수 추가다.

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationContrast.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/utils/image/SegmentationContrastTest.kt`

**Interfaces:**
- Produces: `LUMINANCE_LEVELS`, `contrastLut(histogram: IntArray): IntArray`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`SegmentationContrastTest.kt` (6건):

```kotlin
package com.teamyg.parfait.data.utils.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SegmentationContrastTest {
    private fun band(from: Int, to: Int, count: Int = 1_000) = IntArray(LUMINANCE_LEVELS).also { histogram ->
        for (level in from..to) histogram[level] = count
    }

    @Test
    fun contrastLut_emptyHistogram_isIdentity() {
        val lut = contrastLut(IntArray(LUMINANCE_LEVELS))

        assertTrue((0 until LUMINANCE_LEVELS).all { lut[it] == it })
    }

    @Test
    fun contrastLut_everythingOnOneLevel_isIdentity() {
        // Given 단색 — 절단점이 겹쳐 분모가 0 이 된다
        val lut = contrastLut(band(from = 128, to = 128, count = 10_000))

        assertTrue((0 until LUMINANCE_LEVELS).all { lut[it] == it })
    }

    @Test
    fun contrastLut_narrowBand_stretchesItToTheFullRange() {
        val lut = contrastLut(band(from = 100, to = 150))

        assertTrue(lut[100] <= 16)
        assertTrue(lut[150] >= 239)
    }

    @Test
    fun contrastLut_outliersAtBothEnds_areClippedByThePercentiles() {
        // Given 본체는 100..150 인데 양 끝에 이상치 한 픽셀씩 — 최소·최대로 늘리면 대역이 거의 안 벌어진다
        val histogram = band(from = 100, to = 150)
        histogram[0] = 1
        histogram[LUMINANCE_LEVELS - 1] = 1

        val lut = contrastLut(histogram)

        assertTrue(lut[100] <= 16)
        assertTrue(lut[150] >= 239)
    }

    @Test
    fun contrastLut_isMonotonic() {
        val histogram = IntArray(LUMINANCE_LEVELS)
        for (level in 30..220) histogram[level] = level

        val lut = contrastLut(histogram)

        assertTrue((1 until LUMINANCE_LEVELS).all { lut[it] >= lut[it - 1] })
    }

    @Test
    fun contrastLut_clampsOutsideTheBand() {
        val lut = contrastLut(band(from = 100, to = 150))

        assertEquals(0, lut[0])
        assertEquals(255, lut[LUMINANCE_LEVELS - 1])
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :data:compileDebugUnitTestKotlin`
Expected: FAIL — `Unresolved reference: contrastLut`

- [ ] **Step 3: 최소 구현을 쓴다**

```kotlin
package com.teamyg.parfait.data.utils.image

import kotlin.math.roundToInt

internal const val LUMINANCE_LEVELS = 256

private const val LOW_PERCENTILE = 0.01f
private const val HIGH_PERCENTILE = 0.99f
private const val MAX_LEVEL = LUMINANCE_LEVELS - 1

/**
 * 퍼센타일 절단 후 선형 확장 LUT. 절단점이 겹치면 항등이다.
 *
 * 전역 히스토그램 평활화를 쓰지 않는 것은 계조를 뭉개기 때문이다. 이상치만 자르고 본체의 비율은 유지한다.
 */
internal fun contrastLut(histogram: IntArray): IntArray {
    require(histogram.size == LUMINANCE_LEVELS) { "histogram must have $LUMINANCE_LEVELS levels" }

    var total = 0L
    for (count in histogram) total += count
    if (total <= 0L) return identityLut()

    val low = levelReaching(histogram, (total * LOW_PERCENTILE).toLong())
    val high = levelReaching(histogram, (total * HIGH_PERCENTILE).toLong())
    if (high <= low) return identityLut()

    val span = (high - low).toFloat()

    return IntArray(LUMINANCE_LEVELS) { level ->
        ((level - low) / span * MAX_LEVEL).roundToInt().coerceIn(0, MAX_LEVEL)
    }
}

private fun levelReaching(histogram: IntArray, target: Long): Int {
    var accumulated = 0L
    for (level in 0 until LUMINANCE_LEVELS) {
        accumulated += histogram[level]
        if (accumulated >= target) return level
    }

    return MAX_LEVEL
}

private fun identityLut(): IntArray = IntArray(LUMINANCE_LEVELS) { it }
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*SegmentationContrastTest*"`
Expected: PASS 6건

- [ ] **Step 5: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationContrast.kt \
        data/src/test/java/com/teamyg/parfait/data/utils/image/SegmentationContrastTest.kt
git commit -m "$(cat <<'MSG'
feat: 퍼센타일 절단 대비 LUT 를 만든다

회복 1단계가 검출 판에만 거는 대비 정규화다. 결과 픽셀에는 쓰지 않는다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 3: 마스크 유틸 분해와 알파 재표본·자르기·합

`maskSubjectAlpha`는 `FloatBuffer`를 받아 램프와 후처리를 한 덩어리로 돌아서, 되올린 알파를 넣을 입구가 없다.
앞뒤로 쪼개되 **기존 함수는 위임 껍데기로 남겨** 호출부를 안 건드린다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationMask.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/utils/image/SegmentationMaskTest.kt`

**Interfaces:**
- Consumes: `postProcessAlpha`, `AlphaPostProcessOptions`, `GuidanceProvider`, `MaskedAlpha`, Task 1의 `SegmentationBounds.offsetBy`는 쓰지 않는다
- Produces: `confidenceToAlphaArray(mask, width, height): ByteArray`,
  `postProcessMaskedAlpha(alpha, width, height, options, guidance): MaskedAlpha?`,
  `resampleAlpha(alpha, width, height, targetWidth, targetHeight): ByteArray`,
  `cropAlpha(alpha, width, height, region): ByteArray`, `alphaSum(alpha): Long`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`SegmentationMaskTest.kt`에 아래 9건을 **추가**한다. 기존 테스트는 지우지 않는다.
import에 `com.teamyg.parfait.domain.model.SegmentationBounds`와 `kotlin.test.assertContentEquals`를 더한다.
`FloatBuffer`·`assertEquals`·`assertFailsWith`는 이미 있다.

```kotlin
    @Test
    fun confidenceToAlphaArray_mapsEveryPixelThroughTheRamp() {
        val mask = FloatBuffer.wrap(floatArrayOf(0.0f, 0.5f, 1.0f, 0.2f))

        val alpha = confidenceToAlphaArray(mask, width = 2, height = 2)

        assertEquals(confidenceToAlpha(0.0f), alpha[0].toInt() and 0xFF)
        assertEquals(confidenceToAlpha(0.5f), alpha[1].toInt() and 0xFF)
        assertEquals(confidenceToAlpha(1.0f), alpha[2].toInt() and 0xFF)
        assertEquals(confidenceToAlpha(0.2f), alpha[3].toInt() and 0xFF)
    }

    @Test
    fun resampleAlpha_sameSize_returnsTheSameValues() {
        val alpha = byteArrayOf(0, 64, 128.toByte(), 255.toByte())

        assertContentEquals(alpha, resampleAlpha(alpha, 2, 2, 2, 2))
    }

    @Test
    fun resampleAlpha_upscaleTwoToThree_interpolatesTheMiddle() {
        // Given 한 줄 두 칸 — 박스 평균이나 최근접으로 바꾸면 가운데가 0 이나 254 가 된다
        val alpha = byteArrayOf(0, 254.toByte())

        val resampled = resampleAlpha(alpha, width = 2, height = 1, targetWidth = 3, targetHeight = 1)

        assertEquals(127, resampled[1].toInt() and 0xFF)
    }

    @Test
    fun resampleAlpha_upscale_keepsTheCorners() {
        val alpha = byteArrayOf(0, 255.toByte(), 0, 255.toByte())

        val resampled = resampleAlpha(alpha, 2, 2, 4, 4)

        assertEquals(16, resampled.size)
        assertEquals(0, resampled[0].toInt() and 0xFF)
        assertEquals(255, resampled[3].toInt() and 0xFF)
    }

    @Test
    fun resampleAlpha_downscale_averagesTheBox() {
        val alpha = byteArrayOf(0, 100, 100, 200.toByte())

        val resampled = resampleAlpha(alpha, 2, 2, 1, 1)

        assertEquals(100, resampled.single().toInt() and 0xFF)
    }

    @Test
    fun resampleAlpha_lengthDoesNotMatch_throws() {
        assertFailsWith<IllegalArgumentException> { resampleAlpha(ByteArray(3), 2, 2, 2, 2) }
    }

    @Test
    fun cropAlpha_middleRegion_copiesOnlyThatRegion() {
        val alpha = ByteArray(9) { it.toByte() }

        val cropped = cropAlpha(alpha, width = 3, height = 3, region = SegmentationBounds(1, 1, 3, 3))

        assertContentEquals(byteArrayOf(4, 5, 7, 8), cropped)
    }

    @Test
    fun cropAlpha_regionOutsideTheSource_throws() {
        assertFailsWith<IllegalArgumentException> {
            cropAlpha(ByteArray(9), width = 3, height = 3, region = SegmentationBounds(1, 1, 4, 3))
        }
    }

    @Test
    fun alphaSum_countsBytesAsUnsigned() {
        // 부호 있는 합이면 255 가 -1 로 세어진다
        assertEquals(256L, alphaSum(byteArrayOf(255.toByte(), 1)))
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :data:compileDebugUnitTestKotlin`
Expected: FAIL — `Unresolved reference: confidenceToAlphaArray`

- [ ] **Step 3: 최소 구현을 쓴다**

`SegmentationMask.kt`의 `maskSubjectAlpha`를 아래로 바꾸고 나머지를 더한다. 파일 import에
`com.teamyg.parfait.domain.model.SegmentationBounds`를 더한다.

```kotlin
/** 신뢰도를 알파로. 검출 공간에서 돈다 */
internal fun confidenceToAlphaArray(mask: FloatBuffer, width: Int, height: Int): ByteArray {
    val alpha = ByteArray(width * height)
    for (index in alpha.indices) alpha[index] = confidenceToAlpha(mask[index]).toByte()

    return alpha
}

/**
 * 알파 후처리. 원본 공간에서 돈다 — [guidance] 가 원본을 읽기 때문이다.
 *
 * ⚠️ [alpha] 를 제자리에서 지운다. 되돌림에 쓸 알파는 부르기 전에 사본을 떠 둔다.
 */
internal suspend fun postProcessMaskedAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    guidance: GuidanceProvider? = null,
): MaskedAlpha? {
    val result = postProcessAlpha(alpha, width, height, options, guidance = guidance) ?: return null

    return MaskedAlpha(alpha = alpha, result = result)
}

/**
 * 검출 공간과 원본 공간이 같을 때만 쓴다. 다르면 두 단계를 직접 불러 사이에 [resampleAlpha] 를 낀다.
 *
 * @param mask 길이가 `width * height` 여야 한다 — 호출부가 검사한다
 */
internal suspend fun maskSubjectAlpha(
    mask: FloatBuffer,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    guidance: GuidanceProvider? = null,
): MaskedAlpha? = postProcessMaskedAlpha(confidenceToAlphaArray(mask, width, height), width, height, options, guidance)

/**
 * 알파를 다른 치수로 옮긴다. 확대는 쌍선형, 축소는 박스 평균이다.
 *
 * 확대만 받게 두면 안 된다 — 짧은 변 하한이 걸리거나 크롭이 작으면 되올림이 축소가 된다. 목표 치수가 한 배율에서
 * 나오므로 두 축의 방향은 언제나 같아서 가로만 보고 가른다.
 */
internal fun resampleAlpha(alpha: ByteArray, width: Int, height: Int, targetWidth: Int, targetHeight: Int): ByteArray {
    require(alpha.size == width * height) { "alpha ${alpha.size} does not match ${width}x$height" }
    require(targetWidth > 0 && targetHeight > 0) { "target must be positive but was ${targetWidth}x$targetHeight" }

    if (targetWidth == width && targetHeight == height) return alpha.copyOf()

    return if (targetWidth < width) {
        boxAverageAlpha(alpha, width, height, targetWidth, targetHeight)
    } else {
        bilinearAlpha(alpha, width, height, targetWidth, targetHeight)
    }
}

internal fun cropAlpha(alpha: ByteArray, width: Int, height: Int, region: SegmentationBounds): ByteArray {
    require(alpha.size == width * height) { "alpha ${alpha.size} does not match ${width}x$height" }
    require(
        region.left >= 0 && region.top >= 0 && region.right <= width && region.bottom <= height &&
            region.width > 0 && region.height > 0,
    ) { "region $region escapes ${width}x$height" }

    val out = ByteArray(region.width * region.height)
    for (y in 0 until region.height) {
        System.arraycopy(alpha, (region.top + y) * width + region.left, out, y * region.width, region.width)
    }

    return out
}

internal fun alphaSum(alpha: ByteArray): Long {
    var sum = 0L
    for (value in alpha) sum += value.toInt() and 0xFF

    return sum
}

private fun bilinearAlpha(alpha: ByteArray, width: Int, height: Int, targetWidth: Int, targetHeight: Int): ByteArray {
    val out = ByteArray(targetWidth * targetHeight)
    val scaleX = if (targetWidth > 1) (width - 1).toFloat() / (targetWidth - 1) else 0f
    val scaleY = if (targetHeight > 1) (height - 1).toFloat() / (targetHeight - 1) else 0f

    for (y in 0 until targetHeight) {
        val sourceY = y * scaleY
        val y0 = sourceY.toInt().coerceIn(0, height - 1)
        val y1 = (y0 + 1).coerceAtMost(height - 1)
        val weightY = sourceY - y0

        for (x in 0 until targetWidth) {
            val sourceX = x * scaleX
            val x0 = sourceX.toInt().coerceIn(0, width - 1)
            val x1 = (x0 + 1).coerceAtMost(width - 1)
            val weightX = sourceX - x0

            val upper = lerpAlpha(alphaAt(alpha, width, x0, y0), alphaAt(alpha, width, x1, y0), weightX)
            val lower = lerpAlpha(alphaAt(alpha, width, x0, y1), alphaAt(alpha, width, x1, y1), weightX)
            out[y * targetWidth + x] = (upper + (lower - upper) * weightY).toInt().toByte()
        }
    }

    return out
}

private fun boxAverageAlpha(alpha: ByteArray, width: Int, height: Int, targetWidth: Int, targetHeight: Int): ByteArray {
    val out = ByteArray(targetWidth * targetHeight)

    for (y in 0 until targetHeight) {
        val startY = y * height / targetHeight
        val endY = maxOf(startY + 1, (y + 1) * height / targetHeight)

        for (x in 0 until targetWidth) {
            val startX = x * width / targetWidth
            val endX = maxOf(startX + 1, (x + 1) * width / targetWidth)

            var sum = 0
            for (sourceY in startY until endY) {
                for (sourceX in startX until endX) sum += alphaAt(alpha, width, sourceX, sourceY)
            }
            out[y * targetWidth + x] = (sum / ((endY - startY) * (endX - startX))).toByte()
        }
    }

    return out
}

private fun alphaAt(alpha: ByteArray, width: Int, x: Int, y: Int): Int = alpha[y * width + x].toInt() and 0xFF

private fun lerpAlpha(from: Int, to: Int, weight: Float): Float = from + (to - from) * weight
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*SegmentationMaskTest*"`
Expected: PASS. **기존 테스트가 전부 그대로 통과해야 한다** — 위임 껍데기가 동작을 안 바꿨다는 증거다.

- [ ] **Step 5: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationMask.kt \
        data/src/test/java/com/teamyg/parfait/data/utils/image/SegmentationMaskTest.kt
git commit -m "$(cat <<'MSG'
refactor: 마스크 유틸을 램프와 후처리로 쪼개고 알파 재표본을 더한다

회복 경로가 검출 공간 알파를 원본 공간으로 옮긴 뒤 후처리할 수 있게 입구를 연다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 4: 후보 수확 코드 순수 이동

수확 코드를 저장소 구현에서 새 파일로 **옮기기만** 한다. 시그니처도 본문도 바꾸지 않는다. 동작 변경은 Task 5가
따로 한다 — 리뷰가 "이동은 승인, 변경은 기각"을 가를 수 있게 둘을 나눴다.

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationCandidateHarvest.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`

**Interfaces:**
- Produces: `MAX_POST_PROCESS_CANDIDATES`, `CandidatePair`, `SubjectSegmentationResult.toCandidatePairs(origin)`,
  `SubjectSegmentationResult.toForegroundCandidate(origin)` (전부 `internal`, Task 5가 다시 바꾼다)

- [ ] **Step 1: 옮길 선언을 확인한다**

`ImageSegmentationRepositoryImpl` 클래스 본문에서 아래 일곱을 찾는다. 전부 클래스 멤버다.

| 지금 | 옮긴 뒤 |
|---|---|
| `private val maxPostProcessCandidates = MAX_SUBJECT_COUNT + 3` | `internal const val MAX_POST_PROCESS_CANDIDATES = MAX_SUBJECT_COUNT + 3` (최상위) |
| `private class CandidatePair` | `internal class CandidatePair` |
| `private suspend fun SubjectSegmentationResult.toCandidatePairs(origin: Bitmap)` | `internal suspend fun …` |
| `private suspend fun buildCandidatePair(…)` | `private suspend fun …` |
| `private fun originalCandidate(…)` | `private fun …` |
| `private suspend fun postProcess(…)` | `private suspend fun …` |
| `private suspend fun SubjectSegmentationResult.toForegroundCandidate(origin: Bitmap)` | `internal suspend fun …` |

- [ ] **Step 2: 새 파일로 옮긴다**

KDoc까지 **그대로** 잘라 붙인다. 본문에서 바꾸는 것은 `maxPostProcessCandidates`를
`MAX_POST_PROCESS_CANDIDATES`로 부르는 자리 하나뿐이다. 파일 머리는 이렇다.

```kotlin
package com.teamyg.parfait.data.utils.image

import android.graphics.Bitmap
import com.google.mlkit.vision.segmentation.subject.Subject
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.jvm.extension.sumArgbAlpha
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SubjectCoverage
import kotlinx.coroutines.CancellationException

/** 후처리를 태울 후보 수 상한. 후처리는 `filterCandidates` 의 상한 절단 앞에 있다 */
internal const val MAX_POST_PROCESS_CANDIDATES = MAX_SUBJECT_COUNT + 3

// 여기에 Step 1 표의 나머지 여섯을 순서대로 붙인다
```

`maskSubjectAlpha`·`postProcessAlpha`·`composeCroppedArgb`·`applyAlphaInPlace`·`MAX_SUBJECT_COUNT`는 같은
패키지라 import가 필요 없다.

- [ ] **Step 3: 저장소 구현의 import를 정리한다**

옮긴 코드만 쓰던 import를 지운다: `com.google.mlkit.vision.segmentation.subject.Subject`,
`com.teamyg.parfait.core.util.jvm.extension.sumArgbAlpha`, `com.teamyg.parfait.data.utils.image.MAX_SUBJECT_COUNT`,
`com.teamyg.parfait.data.utils.image.applyAlphaInPlace`, `com.teamyg.parfait.data.utils.image.composeCroppedArgb`,
`com.teamyg.parfait.data.utils.image.maskSubjectAlpha`, `com.teamyg.parfait.domain.model.SegmentationBounds`,
`com.teamyg.parfait.domain.model.SubjectCoverage`. 대신 `toCandidatePairs`·`toForegroundCandidate`를 부르는 데
필요한 import를 더한다(같은 `data.utils.image` 패키지의 확장 함수다).

⚠️ 목록은 지금 코드를 읽고 만든 것이다. **최종 판정은 ktlint가 한다** — Step 4에서 남은 미사용 import를 지운다.

- [ ] **Step 4: 모듈 전체 유닛과 ktlint를 돌린다**

Run: `./gradlew :data:testDebugUnitTest :data:ktlintCheck`
Expected: PASS. **기존 테스트가 하나도 안 깨져야 한다.** 깨지면 이동 중에 본문이 바뀐 것이다.

- [ ] **Step 5: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationCandidateHarvest.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt
git commit -m "$(cat <<'MSG'
refactor: 후보 수확 코드를 저장소 구현에서 떼어 옮긴다

동작은 바꾸지 않는다. 회복 경로가 같은 코드를 쓰도록 다음 커밋에서 일반화한다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 5: 수확 일반화 — 판 출처 분리와 공통 subject 루프

Task 4가 옮긴 코드를 1차·회복 두 경로가 함께 쓰게 바꾼다. **의도한 1차 동작 변경은 둘뿐이다** — 캔버스 밖 후보를
버리는 것, 전경 폴백이 `ModuleNotReady`만 실패로 올리는 것. 이 Task는 쪼갤 수 없다: `CandidatePair`를 바꾸는 순간
저장소 구현이 깨지고 이 Task 끝에서 다시 컴파일된다.

⚠️ **유닛 테스트가 없다.** 수확은 `Bitmap`과 ML Kit를 만진다. 판단은 Task 1·3이 덮었고, 여기서는 기존 유닛이 전부
초록이고 앱이 빌드되는지를 본다. 구조적 강제 둘을 이 Task가 세운다 — `PlateSource.OriginRegion`에 **픽셀 인자가
없다**는 것, 출처를 **호출부가 아니라 subject 루프가 투영 유무로 고른다**는 것이다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationCandidateHarvest.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`

**Interfaces:**
- Consumes: Task 1의 `DetectionBounds`·`DetectionProjection`·`ProjectedRegion`·`projectRegion`·`offsetBy`·
  `isInsideCanvas`·`hintBounds`, Task 3의 `confidenceToAlphaArray`·`postProcessMaskedAlpha`·`resampleAlpha`·`cropAlpha`·`alphaSum`
- Produces:
  - `internal sealed interface PlateSource { class MlKitPlate(plate: Bitmap, region: SegmentationBounds); class OriginRegion(detectionPlate: Bitmap, projected: ProjectedRegion) }`
  - `internal class HarvestedCandidate(val candidate: SegmentationCandidate, val reverted: Boolean)`
  - `internal class ForegroundHarvest(val candidates: List<SegmentationCandidate>, val hint: DetectionBounds?)`
  - `internal suspend fun harvestSubjects(subjects: List<Subject>, origin: Bitmap, projection: DetectionProjection?): List<HarvestedCandidate>`
  - `internal suspend fun harvestForeground(mask: FloatBuffer, maskWidth: Int, maskHeight: Int, origin: Bitmap, projection: DetectionProjection?, hintThreshold: Int?): ForegroundHarvest`
  - `internal const val RELAXED_FLOOR_LOG_DIVISOR = 4`

- [ ] **Step 1: 수확 파일을 새 구조로 다시 쓴다 — 타입과 공통 루프**

Task 4가 옮긴 `CandidatePair`·`toCandidatePairs`·`buildCandidatePair`·`originalCandidate`·`postProcess`·
`toForegroundCandidate`를 **지우고** 아래로 바꾼다. 옛 KDoc 가운데 살아남는 근거(OOM 가드 위치, 행 단위 읽기,
ML Kit 판 치수, OQ-P-266 소유권)는 대응하는 새 함수로 옮긴다.

파일 import:

```kotlin
import android.graphics.Bitmap
import com.google.mlkit.vision.segmentation.subject.Subject
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.jvm.extension.sumArgbAlpha
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SubjectCoverage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import java.nio.FloatBuffer
```

타입과 공통 루프:

```kotlin
/** 후처리를 태울 후보 수 상한. 후처리는 `filterCandidates` 의 상한 절단 앞에 있다 */
internal const val MAX_POST_PROCESS_CANDIDATES = MAX_SUBJECT_COUNT + 3

/**
 * 로그에만 쓰는 완화 배수. **판정에는 쓰지 않는다** — 수동 편집이 같은 엄격 하한으로 저장을 막으므로, 회복 판정만
 * 완화하면 고른 후보를 손대지 않고도 저장하지 못한다. 근거는 스펙 「제외」.
 */
internal const val RELAXED_FLOOR_LOG_DIVISOR = 4

/** 판 한 장이 어디서 오는가. 어느 쪽을 쓸지는 [harvestSubjects] 가 투영 유무로 정한다 */
internal sealed interface PlateSource {
    /** 1차 경로 전용. 검출 공간이 곧 원본 공간이라 이 판의 픽셀이 원본 색이다 */
    class MlKitPlate(val plate: Bitmap, val region: SegmentationBounds) : PlateSource

    /**
     * 회복 경로 전용. **픽셀 인자가 없다** — 알파만 [detectionPlate] 에서 가져오고 픽셀은 원본에서 읽는다.
     * 검출 판은 대비를 건 판이라 그 픽셀을 쓰면 결과 색이 변한다.
     */
    class OriginRegion(val detectionPlate: Bitmap, val projected: ProjectedRegion) : PlateSource
}

internal class HarvestedCandidate(
    val candidate: SegmentationCandidate,
    /** 후처리가 실패하거나 알파를 전멸시켜 후처리 이전 판으로 되돌렸다 */
    val reverted: Boolean,
)

internal class ForegroundHarvest(
    val candidates: List<SegmentationCandidate>,
    /** 다음 단계가 어디를 크롭할지 정하는 데만 쓴다. 검출 공간 좌표다 */
    val hint: DetectionBounds?,
)

private class PlacedSubject(val plate: Bitmap, val projected: ProjectedRegion)

/**
 * subject 들을 후보로 만든다. 1차 경로와 회복 경로가 함께 쓴다.
 *
 * [projection] 이 없으면 1차다. **판의 출처를 호출부가 고르지 않는 것이 요점이다** — 회복 경로가 ML Kit 판을
 * 쓸 길이 없다.
 *
 * bbox 사전 절단은 원본 좌표 사각형 면적으로 한다. bbox 픽셀 수는 커버리지의 상계라 하한 미만이면 커버리지도
 * 하한 미만이다.
 */
internal suspend fun harvestSubjects(
    subjects: List<Subject>,
    origin: Bitmap,
    projection: DetectionProjection?,
): List<HarvestedCandidate> {
    val job = currentCoroutineContext().job
    val floor = SubjectCoverage.floorPixels(origin.width.toLong() * origin.height)

    val withPlate = subjects.mapNotNull { subject -> subject.bitmap?.let { subject to it } }
    val placed = withPlate.mapNotNull { (subject, plate) ->
        // ML Kit 문서는 getWidth()·getHeight() 가 getBitmap() 의 실제 치수와 같다고 보장하지 않으므로 판에서 뽑는다
        val detection = DetectionBounds(
            left = subject.startX,
            top = subject.startY,
            right = subject.startX + plate.width,
            bottom = subject.startY + plate.height,
        )
        val projected = if (projection == null) {
            val region = SegmentationBounds(detection.left, detection.top, detection.right, detection.bottom)
            ProjectedRegion(mapped = region, clipped = region)
        } else {
            projectRegion(detection, projection, origin.width, origin.height) ?: return@mapNotNull null
        }
        PlacedSubject(plate, projected)
    }

    val eligible = placed
        .filter { it.projected.clipped.area() >= floor }
        .sortedByDescending { it.projected.clipped.area() }

    repositoryLogger.i {
        // 1차 로그 한 줄은 그대로 둔다. 회복 경로에서만 완화했다면 통과했을 수를 덧붙인다
        val relaxed = if (projection == null) {
            ""
        } else {
            val count = placed.count { it.projected.clipped.area() >= floor / RELAXED_FLOOR_LOG_DIVISOR }
            "(1/$RELAXED_FLOOR_LOG_DIVISOR 하한이었다면 ${count}개)"
        }
        "세그멘테이션 후보 쌍 생성: subject ${subjects.size}개 중 판 있음 ${withPlate.size}개, " +
            "bbox 하한 통과 ${eligible.size}개$relaxed"
    }

    val considered = eligible.take(MAX_POST_PROCESS_CANDIDATES)
    if (eligible.size > considered.size) {
        repositoryLogger.i { "세그멘테이션 후처리 대상을 ${eligible.size}개 중 ${considered.size}개로 자른다" }
    }

    return considered.mapNotNull { subject ->
        job.ensureActive()
        val source = if (projection == null) {
            PlateSource.MlKitPlate(subject.plate, subject.projected.clipped)
        } else {
            PlateSource.OriginRegion(subject.plate, subject.projected)
        }
        harvestCandidate(source, origin)
    }
}

private fun SegmentationBounds.area(): Long = width.toLong() * height

/** 캔버스 밖으로 새는 후보는 흐름 전체가 아니라 그 후보만 버린다 */
private suspend fun harvestCandidate(source: PlateSource, origin: Bitmap): HarvestedCandidate? {
    val region = when (source) {
        is PlateSource.MlKitPlate -> source.region
        is PlateSource.OriginRegion -> source.projected.clipped
    }
    if (!isInsideCanvas(region, origin.width, origin.height)) {
        repositoryLogger.w { "세그멘테이션 후보 $region 이 캔버스 ${origin.width}x${origin.height} 밖이라 버린다" }
        return null
    }

    return when (source) {
        is PlateSource.MlKitPlate -> harvestMlKitPlate(source, origin)
        is PlateSource.OriginRegion -> harvestOriginRegion(source, origin)
    }
}

private fun originGuidance(origin: Bitmap, region: SegmentationBounds) = GuidanceProvider { bounds ->
    IntArray(bounds.width * bounds.height).also { pixels ->
        origin.getPixels(pixels, 0, bounds.width, region.left + bounds.left, region.top + bounds.top, bounds.width, bounds.height)
    }
}
```

> ⚠️ `originGuidance`의 `getPixels` 한 줄이 120자를 넘으면 인자마다 줄바꿈한다.

- [ ] **Step 2: 두 출처의 수확을 쓴다**

1차 경로(`MlKitPlate`)는 옛 `buildCandidatePair`·`postProcess`·`originalCandidate`의 동작을 그대로 옮긴다.
`subject.startX`·`startY`가 `region.left`·`top`이 됐고, 되돌림 후보는 **쓸 때만** 만든다(출력은 같다).

```kotlin
/**
 * ⚠️ `try` 가 픽셀 배열 할당까지 감싼다. 12MP 후보에서 `OutOfMemoryError` 가 가장 잘 나는 자리가 후처리 안이
 * 아니라 그 할당이다.
 */
private suspend fun harvestMlKitPlate(source: PlateSource.MlKitPlate, origin: Bitmap): HarvestedCandidate {
    val postProcessed = try {
        postProcessMlKitPlate(source.plate, source.region, origin)
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 후처리가 메모리로 실패해 원본 후보로 되돌린다" }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        repositoryLogger.w(e) { "세그멘테이션 후처리가 예외로 실패해 원본 후보로 되돌린다" }
        null
    }

    if (postProcessed != null) return HarvestedCandidate(postProcessed, reverted = false)

    return HarvestedCandidate(mlKitOriginal(source.plate, source.region, origin), reverted = true)
}

/** ML Kit 판은 후처리가 지우지 않았으므로 되돌림 커버리지를 그 판에서 센다 */
private fun mlKitOriginal(plate: Bitmap, region: SegmentationBounds, origin: Bitmap): SegmentationCandidate {
    require(plate.width == region.width && plate.height == region.height) {
        "plate ${plate.width}x${plate.height} does not match region $region"
    }

    // 행 단위로 읽는다 — 판 전체 크기 버퍼는 후처리가 메모리로 실패한 직후 같은 크기를 한 번 더 요구한다
    val row = IntArray(plate.width)
    var coverage = 0L
    for (y in 0 until plate.height) {
        plate.getPixels(row, 0, plate.width, 0, y, plate.width, 1)
        coverage += row.sumArgbAlpha()
    }

    return SegmentationCandidate(
        bounds = region,
        bitmap = plate.toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = coverage,
    )
}

private suspend fun postProcessMlKitPlate(plate: Bitmap, region: SegmentationBounds, origin: Bitmap): SegmentationCandidate? {
    val width = plate.width
    val height = plate.height
    val pixels = IntArray(width * height)
    plate.getPixels(pixels, 0, width, 0, 0, width, height)

    val alpha = ByteArray(width * height)
    for (index in pixels.indices) alpha[index] = (pixels[index] ushr 24).toByte()

    val result = postProcessAlpha(alpha, width, height, guidance = originGuidance(origin, region)) ?: run {
        repositoryLogger.i { "세그멘테이션 후처리가 후보 ${width}x$height 판의 알파를 전부 지워 원본으로 되돌린다" }
        return null
    }

    repositoryLogger.i {
        "세그멘테이션 후보 부분 알파 ${result.partialAlphaPixels}/${width * height}, " +
            "정련 ${result.refineElapsedNanos / 1_000_000}ms"
    }

    val inner = result.bounds
    // ML Kit 판은 ML Kit 소유라 알파가 안 바뀌면 그대로 써도 된다(OQ-P-266). 회복 경로는 이 최적화를 안 쓴다
    val unchangedWholePlate = !result.changed && inner.width == width && inner.height == height
    val trimmed = if (unchangedWholePlate) {
        plate
    } else {
        Bitmap.createBitmap(composeCroppedArgb(pixels, alpha, width, inner), inner.width, inner.height, Bitmap.Config.ARGB_8888)
    }

    return SegmentationCandidate(
        bounds = inner.offsetBy(region.left, region.top),
        bitmap = trimmed.toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = result.alphaSum,
    )
}
```

회복 경로(`OriginRegion`):

```kotlin
/**
 * 회복 경로의 후보. **판을 항상 새로 만든다** — 재사용할 판이 원본뿐이다.
 *
 * ⚠️ `postProcessAlpha` 는 알파를 제자리에서 지운다. 되돌림에 쓸 알파는 그 전에 사본을 떠 둔다. 원본 픽셀의
 * 알파는 JPEG 에서 전부 255 라, 사본 없이 되돌리면 불투명 사각형이 커버리지 만점으로 필터 1위에 오른다.
 *
 * 후보 하나의 실패는 그 후보만 버린다.
 */
private suspend fun harvestOriginRegion(source: PlateSource.OriginRegion, origin: Bitmap): HarvestedCandidate? {
    val region = source.projected.clipped

    return try {
        val alpha = originAlphaOf(source)
        val pristine = alpha.copyOf()
        val pixels = IntArray(region.width * region.height)
        origin.getPixels(pixels, 0, region.width, region.left, region.top, region.width, region.height)

        val postProcessed = try {
            postProcessOriginRegion(pixels, alpha, region, origin)
        } catch (e: OutOfMemoryError) {
            repositoryLogger.w(e) { "회복 후처리가 메모리로 실패해 사본 알파로 되돌린다" }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            repositoryLogger.w(e) { "회복 후처리가 예외로 실패해 사본 알파로 되돌린다" }
            null
        }

        if (postProcessed != null) {
            HarvestedCandidate(postProcessed, reverted = false)
        } else {
            HarvestedCandidate(originRegionReverted(pixels, pristine, region, origin), reverted = true)
        }
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "회복 후보 $region 이 메모리로 실패해 버린다" }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        repositoryLogger.w(e) { "회복 후보 $region 이 예외로 실패해 버린다" }
        null
    }
}

/** 사상 사각형 크기로 **먼저** 재표본하고 그다음 잘린 사각형으로 자른다. 순서를 뒤집으면 알파가 어긋난다 */
private fun originAlphaOf(source: PlateSource.OriginRegion): ByteArray {
    val plate = source.detectionPlate
    val row = IntArray(plate.width)
    val detectionAlpha = ByteArray(plate.width * plate.height)
    for (y in 0 until plate.height) {
        plate.getPixels(row, 0, plate.width, 0, y, plate.width, 1)
        for (x in 0 until plate.width) detectionAlpha[y * plate.width + x] = (row[x] ushr 24).toByte()
    }

    val mapped = source.projected.mapped
    val full = resampleAlpha(detectionAlpha, plate.width, plate.height, mapped.width, mapped.height)

    return cropAlpha(full, mapped.width, mapped.height, source.projected.clipped.offsetBy(-mapped.left, -mapped.top))
}

private suspend fun postProcessOriginRegion(
    pixels: IntArray,
    alpha: ByteArray,
    region: SegmentationBounds,
    origin: Bitmap,
): SegmentationCandidate? {
    val result = postProcessAlpha(alpha, region.width, region.height, guidance = originGuidance(origin, region))
        ?: return null
    val inner = result.bounds
    // composeCroppedArgb 는 알파 채널을 덮어쓴다. 원본 픽셀의 255 가 남지 않는다
    val cropped = composeCroppedArgb(pixels, alpha, region.width, inner)

    return SegmentationCandidate(
        bounds = inner.offsetBy(region.left, region.top),
        bitmap = Bitmap.createBitmap(cropped, inner.width, inner.height, Bitmap.Config.ARGB_8888).toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = result.alphaSum,
    )
}

private fun originRegionReverted(
    pixels: IntArray,
    pristine: ByteArray,
    region: SegmentationBounds,
    origin: Bitmap,
): SegmentationCandidate {
    val whole = SegmentationBounds(0, 0, region.width, region.height)
    val plate = composeCroppedArgb(pixels, pristine, region.width, whole)

    return SegmentationCandidate(
        bounds = region,
        bitmap = Bitmap.createBitmap(plate, region.width, region.height, Bitmap.Config.ARGB_8888).toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = alphaSum(pristine),
    )
}
```

전경 수확:

```kotlin
/**
 * 전경 신뢰도에서 후보 하나와 힌트를 만든다.
 *
 * ⚠️ 마스크 치수와 출력 치수가 다를 수 있다 — 회복 경로의 마스크는 검출 판 치수다. 하나로 묶으면 길이 검사가
 * 언제나 실패해 예외도 로그도 없이 빈 목록이 되고, 연쇄로 2단계 힌트까지 사라진다.
 *
 * @param hintThreshold 널이면 힌트를 구하지 않는다. 1차 경로에 원본 전체를 한 번 더 훑는 비용을 얹지 않는다
 */
internal suspend fun harvestForeground(
    mask: FloatBuffer,
    maskWidth: Int,
    maskHeight: Int,
    origin: Bitmap,
    projection: DetectionProjection?,
    hintThreshold: Int?,
): ForegroundHarvest {
    // absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼으므로 remaining() 으로 비교한다
    if (mask.remaining() != maskWidth * maskHeight) return ForegroundHarvest(emptyList(), hint = null)

    val detectionAlpha = confidenceToAlphaArray(mask, maskWidth, maskHeight)
    val hint = hintThreshold?.let { hintBounds(detectionAlpha, maskWidth, maskHeight, it) }

    val placed = try {
        placeForegroundAlpha(detectionAlpha, maskWidth, maskHeight, origin, projection)
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 폴백 알파 되올림이 메모리로 실패했다" }
        null
    } ?: return ForegroundHarvest(emptyList(), hint)

    val (region, alpha) = placed
    if (!isInsideCanvas(region, origin.width, origin.height)) return ForegroundHarvest(emptyList(), hint)

    val masked = try {
        postProcessMaskedAlpha(alpha, region.width, region.height, guidance = originGuidance(origin, region))
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 폴백 후처리가 메모리로 실패했다" }
        null
    } ?: return ForegroundHarvest(emptyList(), hint)

    repositoryLogger.i {
        "세그멘테이션 폴백 부분 알파 ${masked.result.partialAlphaPixels}/${region.width * region.height}, " +
            "정련 ${masked.result.refineElapsedNanos / 1_000_000}ms"
    }

    val local = masked.result.bounds
    // 로컬 사각형에 원점을 더한다. 빠뜨리면 1단계는 오프셋이 0 이라 멀쩡하고 2단계만 엉뚱한 곳을 오린다
    val bounds = local.offsetBy(region.left, region.top)

    // ⚠️ 이 판이 폴백에서 가장 큰 할당이라 OOM 가드를 여기까지 넓힌다
    val candidate = try {
        val trimmedPixels = IntArray(local.width * local.height)
        origin.getPixels(trimmedPixels, 0, local.width, bounds.left, bounds.top, local.width, local.height)
        applyAlphaInPlace(trimmedPixels, masked.alpha, region.width, local)

        SegmentationCandidate(
            bounds = bounds,
            bitmap = Bitmap.createBitmap(trimmedPixels, local.width, local.height, Bitmap.Config.ARGB_8888)
                .toAndroidBitmap(),
            canvasWidth = origin.width,
            canvasHeight = origin.height,
            coverageAlphaSum = masked.result.alphaSum,
        )
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 폴백 판 생성이 메모리로 실패했다" }
        null
    }

    return ForegroundHarvest(listOfNotNull(candidate), hint)
}

/** 1차는 마스크가 곧 원본 전체다. 회복은 사상 사각형 크기로 재표본한 뒤 잘린 사각형으로 자른다 */
private fun placeForegroundAlpha(
    detectionAlpha: ByteArray,
    maskWidth: Int,
    maskHeight: Int,
    origin: Bitmap,
    projection: DetectionProjection?,
): Pair<SegmentationBounds, ByteArray>? {
    if (projection == null) return SegmentationBounds(0, 0, maskWidth, maskHeight) to detectionAlpha

    val projected = projectRegion(DetectionBounds(0, 0, maskWidth, maskHeight), projection, origin.width, origin.height)
        ?: return null
    val mapped = projected.mapped
    val full = resampleAlpha(detectionAlpha, maskWidth, maskHeight, mapped.width, mapped.height)

    return projected.clipped to cropAlpha(full, mapped.width, mapped.height, projected.clipped.offsetBy(-mapped.left, -mapped.top))
}
```

- [ ] **Step 3: 저장소 구현을 새 수확에 결선한다**

`ImageSegmentationRepositoryImpl#segmentImage`에서 `toCandidatePairs`를 부르던 부분부터 끝까지를 아래로 바꾼다.
앞의 비트맵 캐스팅·`InputImage`·다중 subject 옵션·`runSegmenter` 호출은 그대로다.

```kotlin
        val result = runSegmenter(multipleSubjectOptions, image).getOrElse { return Result.failure(it) }

        val harvested = try {
            withContext(Dispatchers.Default) { harvestSubjects(result.subjects, bitmap, projection = null) }
        } catch (e: CancellationException) {
            // 취소는 실패가 아니다 — 값으로 접으면 상위로 전파되지 않아 취소된 흐름이 계속 돈다
            throw e
        } catch (e: Exception) {
            return Result.failure(SegmentationException.Process(e))
        }

        if (harvested.isEmpty()) {
            repositoryLogger.i { "세그멘테이션: 후처리 대상이 0건이다. 전경 마스크 폴백으로 내려간다" }
            return segmentForeground(image, bitmap)
        }

        val reverted = harvested.count { it.reverted }
        if (reverted > 0) {
            // 후처리는 개선 수단이지 후보를 없앨 권한이 아니다
            repositoryLogger.i { "세그멘테이션 후처리: ${harvested.size}개 중 ${reverted}개를 후처리 이전 후보로 되돌린다" }
        }

        val candidates = try {
            withContext(Dispatchers.Default) { filterCandidates(harvested.map { it.candidate }) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(SegmentationException.Process(e))
        }

        // 전멸이 아니라 일부만 걸러진 경우도 남긴다 — 전멸 로그만 있으면 극단값에서만 판정된다
        repositoryLogger.i { "세그멘테이션 필터 통과 ${candidates.size}/${harvested.size}" }

        if (candidates.isNotEmpty()) return Result.success(candidates)

        repositoryLogger.i { "세그멘테이션: 필터가 후보 ${harvested.size}개를 전부 걸러 냈다. 전경 마스크 폴백으로 내려간다" }
        return segmentForeground(image, bitmap)
```

`segmentForeground`를 `Result`로 바꾼다. KDoc의 `SIGSEGV` 경고는 그대로 두고, 「여기서 실패하면 값으로
접는다」 문단만 아래 주석의 내용으로 바꾼다.

```kotlin
    private suspend fun segmentForeground(
        image: InputImage,
        origin: Bitmap,
    ): Result<List<SegmentationCandidate>> {
        val options = SubjectSegmenterOptions
            .Builder()
            .enableForegroundConfidenceMask()
            .build()

        val result = runSegmenter(options, image).getOrElse { cause ->
            // 모듈이 없으면 위로 올린다. 그 밖의 실패는 종전처럼 「인식된 대상 없음」으로 접는다 — 모두 올리면
            // 1차의 처리 실패가 빈 목록에서 예외로 분류가 바뀌어 그 사진의 재시도가 회복 경로로 못 간다
            return if (cause is SegmentationException.ModuleNotReady) {
                Result.failure(cause)
            } else {
                Result.success(emptyList())
            }
        }
        val mask = result.foregroundConfidenceMask ?: return Result.success(emptyList())

        return try {
            val harvest = withContext(Dispatchers.Default) {
                harvestForeground(mask, origin.width, origin.height, origin, projection = null, hintThreshold = null)
            }
            Result.success(harvest.candidates)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }
```

import에서 `toCandidatePairs`·`toForegroundCandidate`를 지우고 `harvestSubjects`·`harvestForeground`를 더한다.

- [ ] **Step 4: 모듈 전체 유닛과 ktlint를 돌린다**

Run: `./gradlew :data:testDebugUnitTest :data:ktlintCheck`
Expected: PASS. 기존 테스트가 하나도 안 깨져야 한다.

- [ ] **Step 5: 앱이 빌드되는지 확인한다**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationCandidateHarvest.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt
git commit -m "$(cat <<'MSG'
refactor: 수확의 판 출처를 타입으로 가르고 subject 루프를 두 경로가 함께 쓰게 한다

1차 경로의 의도한 변경은 둘이다. 캔버스 밖 후보를 그 후보만 버리고, 전경 폴백이 ModuleNotReady 만 실패로 올린다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 6: 정규화 실행기와 회복 사다리

⚠️ **유닛 테스트가 없고, 회복 경로는 병합 시점에 실기기에서도 돌지 않는다**(검증 수단을 넣지 않기로 했다). 이 Task의
결함은 리뷰로만 잡힌다. 리뷰어는 아래 셋을 코드로 확인한다 — ① `DetectionPlate.ownedByUs`가 거짓인 판에
`setPixels`·`recycle`이 닿지 않는가 ② 픽셀 작업이 전부 `Dispatchers.Default` 안인가 ③ 가드에 걸린 단계도 로그가 찍히는가.

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationRecoveryNormalizer.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/RecoverCandidatesUseCase.kt`

**Interfaces:**
- Consumes: Task 1~5 전부
- Produces: `ImageSegmentationRepository.recoverCandidates(bitmapWrapper): Result<List<SegmentationCandidate>>`,
  `RecoverCandidatesUseCase`

인터페이스 구현체는 `ImageSegmentationRepositoryImpl` 하나뿐이다(테스트 가짜 없음) — 메서드를 더해도 다른 곳이 안 깨진다.

- [ ] **Step 1: 정규화 실행기를 만든다**

```kotlin
package com.teamyg.parfait.data.utils.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job

/** [ownedByUs] 가 거짓이면 원본이다. 쓰지도 회수하지도 않는다 */
internal class DetectionPlate(val bitmap: Bitmap, val ownedByUs: Boolean)

/**
 * 계획을 비트맵에 적용한다. 크롭 → 축소 → 축소판에서 히스토그램 → 축소판에 LUT 순서다. 원본에서 히스토그램을
 * 모으면 그 픽셀 배열 하나가 판 하나만큼 크다.
 *
 * ⚠️ 크롭도 축소도 필요 없으면 검출 판이 곧 원본 인스턴스다. 원본은 가변으로 디코드되므로 거기에 대비를 적용하면
 * 예외 없이 사용자 사진이 바뀐다. 그 경우 먼저 복사한다.
 *
 * LUT 적용이 픽셀 루프인 것은 `minSdk` 가 26 이라 `RenderEffect` 를 못 쓰고, 임의 LUT 가
 * `ColorMatrixColorFilter` 로 표현되지 않기 때문이다.
 */
internal suspend fun normalizeForDetection(origin: Bitmap, stage: RecoveryStage): DetectionPlate {
    val job = currentCoroutineContext().job

    val cropped = stage.cropRect
        ?.let { rect -> Bitmap.createBitmap(origin, rect.left, rect.top, rect.width, rect.height) }
        ?: origin
    val target = stage.targetSize
    val scaled = if (cropped.width == target.width && cropped.height == target.height) {
        cropped
    } else {
        Bitmap.createScaledBitmap(cropped, target.width, target.height, true)
    }
    // 두 팩토리는 조건에 따라 입력을 그대로 돌려준다
    if (cropped !== origin && cropped !== scaled) cropped.recycle()
    job.ensureActive()

    val owned = scaled !== origin
    if (!stage.applyContrast) return DetectionPlate(scaled, owned)

    val writable = if (owned && scaled.isMutable) {
        scaled
    } else {
        val copy = requireNotNull(scaled.copy(Bitmap.Config.ARGB_8888, true)) { "detection plate copy failed" }
        if (owned) scaled.recycle()
        copy
    }
    applyContrastInPlace(writable)

    return DetectionPlate(writable, ownedByUs = true)
}

private suspend fun applyContrastInPlace(bitmap: Bitmap) {
    val job = currentCoroutineContext().job
    val width = bitmap.width
    val row = IntArray(width)
    val histogram = IntArray(LUMINANCE_LEVELS)

    for (y in 0 until bitmap.height) {
        job.ensureActive()
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        for (pixel in row) histogram[contrastLuminance(pixel)]++
    }

    val lut = contrastLut(histogram)

    for (y in 0 until bitmap.height) {
        job.ensureActive()
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        for (index in row.indices) row[index] = throughLut(row[index], lut)
        bitmap.setPixels(row, 0, width, 0, y, width, 1)
    }
}

private fun contrastLuminance(pixel: Int): Int =
    (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000

private fun throughLut(pixel: Int, lut: IntArray): Int =
    Color.argb(Color.alpha(pixel), lut[Color.red(pixel)], lut[Color.green(pixel)], lut[Color.blue(pixel)])
```

- [ ] **Step 2: 저장소 계약을 넓힌다**

`ImageSegmentationRepository.kt`의 `segmentImage` 아래에 더한다.

```kotlin
    /**
     * [segmentImage] 가 후보를 하나도 못 낸 뒤에만 부른다. 입력을 손봐 가며 다시 찾는다.
     *
     * 후보의 픽셀은 언제나 [bitmapWrapper] 에서 오려낸다 — 손본 판은 검출에만 쓴다.
     */
    suspend fun recoverCandidates(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>>
```

- [ ] **Step 3: 사다리를 쓴다**

`ImageSegmentationRepositoryImpl.kt`에 import를 더한다.

```kotlin
import android.os.SystemClock
import com.teamyg.parfait.data.utils.image.AlphaPostProcessOptions
import com.teamyg.parfait.data.utils.image.DetectionBounds
import com.teamyg.parfait.data.utils.image.DetectionProjection
import com.teamyg.parfait.data.utils.image.RELAXED_FLOOR_LOG_DIVISOR
import com.teamyg.parfait.data.utils.image.RecoveryStage
import com.teamyg.parfait.data.utils.image.RecoveryTransform
import com.teamyg.parfait.data.utils.image.cropAreaPercent
import com.teamyg.parfait.data.utils.image.focusCrop
import com.teamyg.parfait.data.utils.image.focusStage
import com.teamyg.parfait.data.utils.image.normalizeForDetection
import com.teamyg.parfait.data.utils.image.normalizeStage
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SubjectCoverage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withTimeoutOrNull
```

클래스 본문에 더한다.

```kotlin
    override suspend fun recoverCandidates(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>> {
        val origin = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            ?: return Result.failure(SegmentationException.ImageNotFound(null))

        // ⚠️ runSegmenter 는 Tasks.await 블로킹 대기라 추론 도중에는 끊기지 않는다. 상한은 진행 중인 추론 하나가
        // 끝난 뒤에 걸린다
        return withTimeoutOrNull(RECOVERY_TIMEOUT_MS) { runRecoveryLadder(origin) } ?: run {
            repositoryLogger.w { "회복: 대기 상한 ${RECOVERY_TIMEOUT_MS}ms 를 넘겨 빈 결과로 접는다" }
            Result.success(emptyList())
        }
    }

    private suspend fun runRecoveryLadder(origin: Bitmap): Result<List<SegmentationCandidate>> {
        val job = currentCoroutineContext().job
        var hint: DetectionBounds? = null
        var hintTransform: RecoveryTransform? = null

        val normalize = normalizeStage(origin.width, origin.height, RECOVERY_APPLY_CONTRAST)
        if (normalize == null) {
            repositoryLogger.i { "회복 1단계: 목표 치수가 원본과 같고 대비가 꺼져 있어 무동작 가드로 건너뛴다" }
        } else {
            when (val outcome = runStage("1단계", origin, normalize)) {
                is StageOutcome.Found -> return Result.success(outcome.candidates)
                is StageOutcome.Aborted -> return Result.failure(outcome.cause)
                is StageOutcome.Empty -> {
                    hint = outcome.hint
                    hintTransform = normalize.transform
                }
            }
        }
        job.ensureActive()

        val crop = focusCrop(origin.width, origin.height, hint, hintTransform)
        val percent = cropAreaPercent(crop, origin.width, origin.height)
        val focus = focusStage(origin.width, origin.height, crop, RECOVERY_APPLY_CONTRAST)
        if (focus == null) {
            repositoryLogger.i { "회복 2단계: 크롭이 원본의 ${percent}% 라 수축 가드로 건너뛴다, 힌트 ${hint != null}" }
            return Result.success(emptyList())
        }
        repositoryLogger.i { "회복 2단계: 크롭이 원본의 ${percent}%, 힌트 ${hint != null}" }

        return when (val outcome = runStage("2단계", origin, focus)) {
            is StageOutcome.Found -> Result.success(outcome.candidates)
            is StageOutcome.Aborted -> Result.failure(outcome.cause)
            is StageOutcome.Empty -> Result.success(emptyList())
        }
    }

    /**
     * 한 단계를 돌린다. `ModuleNotReady` 만 사다리를 멈추고, 그 밖의 실패는 이 단계만 포기한다.
     *
     * 단계가 끝나면 ML Kit 결과를 놓고 힌트 좌표 넷만 들고 나온다. 결과를 다음 단계까지 붙들면 피크가 커지고,
     * 네이티브 신뢰도 버퍼가 새 세그멘터를 연 뒤에도 유효한지에 기대게 된다.
     */
    private suspend fun runStage(name: String, origin: Bitmap, stage: RecoveryStage): StageOutcome {
        val startedAt = SystemClock.elapsedRealtime()
        val plate = try {
            withContext(Dispatchers.Default) { normalizeForDetection(origin, stage) }
        } catch (e: OutOfMemoryError) {
            repositoryLogger.w(e) { "회복 $name: 검출 판을 만들다 메모리로 실패해 이 단계를 포기한다" }
            return StageOutcome.Empty(hint = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            repositoryLogger.w(e) { "회복 $name: 검출 판을 만들다 실패해 이 단계를 포기한다" }
            return StageOutcome.Empty(hint = null)
        }

        try {
            val image = InputImage.fromBitmap(plate.bitmap, 0)
            val whole = SegmentationBounds(0, 0, origin.width, origin.height)
            val source = stage.cropRect ?: whole
            val projection = DetectionProjection(stage.transform, clip = source)
            val capped = stage.targetSize.width < source.width

            val multi = runSegmenter(multipleSubjectOptions(), image).getOrElse { cause ->
                return if (cause is SegmentationException.ModuleNotReady) {
                    StageOutcome.Aborted(cause)
                } else {
                    StageOutcome.Empty(hint = null)
                }
            }
            val harvested = withContext(Dispatchers.Default) { harvestSubjects(multi.subjects, origin, projection) }
            val candidates = withContext(Dispatchers.Default) { filterCandidates(harvested.map { it.candidate }) }
            val relaxed = harvested.count { it.candidate.passesRelaxedFloor() }

            repositoryLogger.i {
                "회복 $name: 목표 ${stage.targetSize.width}x${stage.targetSize.height}, 상한 걸림 $capped, " +
                    "필터 통과 ${candidates.size}/${harvested.size}(1/$RELAXED_FLOOR_LOG_DIVISOR 하한이었다면 $relaxed)"
            }
            if (candidates.isNotEmpty()) return StageOutcome.Found(candidates)

            val foreground = runSegmenter(foregroundOptions(), image).getOrElse { cause ->
                return if (cause is SegmentationException.ModuleNotReady) {
                    StageOutcome.Aborted(cause)
                } else {
                    StageOutcome.Empty(hint = null)
                }
            }
            val mask = foreground.foregroundConfidenceMask ?: return StageOutcome.Empty(hint = null)
            val harvest = withContext(Dispatchers.Default) {
                harvestForeground(
                    mask = mask,
                    maskWidth = plate.bitmap.width,
                    maskHeight = plate.bitmap.height,
                    origin = origin,
                    projection = projection,
                    hintThreshold = AlphaPostProcessOptions().binaryThreshold,
                )
            }

            // 폴백 후보는 필터를 거치지 않는다 — 1차 경로와 같은 규칙이다
            repositoryLogger.i {
                "회복 $name: 폴백 후보 ${harvest.candidates.size}, 힌트 ${harvest.hint != null}, " +
                    "소요 ${SystemClock.elapsedRealtime() - startedAt}ms"
            }

            return if (harvest.candidates.isNotEmpty()) {
                StageOutcome.Found(harvest.candidates)
            } else {
                StageOutcome.Empty(harvest.hint)
            }
        } catch (e: OutOfMemoryError) {
            repositoryLogger.w(e) { "회복 $name: 메모리로 실패해 이 단계를 포기한다" }
            return StageOutcome.Empty(hint = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            repositoryLogger.w(e) { "회복 $name: 실패해 이 단계를 포기한다" }
            return StageOutcome.Empty(hint = null)
        } finally {
            if (plate.ownedByUs) plate.bitmap.recycle()
        }
    }

    private fun multipleSubjectOptions(): SubjectSegmenterOptions = SubjectSegmenterOptions
        .Builder()
        .enableMultipleSubjects(
            SubjectSegmenterOptions.SubjectResultOptions.Builder().enableSubjectBitmap().build(),
        ).build()

    private fun foregroundOptions(): SubjectSegmenterOptions = SubjectSegmenterOptions
        .Builder()
        .enableForegroundConfidenceMask()
        .build()

    /** 로그 전용. 알파 합은 칠해진 픽셀 수의 255 배다 */
    private fun SegmentationCandidate.passesRelaxedFloor(): Boolean {
        val floor = SubjectCoverage.floorPixels(canvasWidth.toLong() * canvasHeight) / RELAXED_FLOOR_LOG_DIVISOR
        return coverageAlphaSum >= floor * 255L
    }

    private sealed interface StageOutcome {
        class Found(val candidates: List<SegmentationCandidate>) : StageOutcome

        /** 다음 단계가 쓸 힌트. 후보가 없어도 힌트는 있을 수 있다 */
        class Empty(val hint: DetectionBounds?) : StageOutcome

        /** 남은 단계도 같은 이유로 실패한다 */
        class Aborted(val cause: Throwable) : StageOutcome
    }
```

파일 아래 최상위 상수 둘을 더한다.

```kotlin
private const val RECOVERY_TIMEOUT_MS = 30_000L

/** 조건부 항목이다. 로그가 대비 스트레치를 철회하면 여기만 끈다 — 그러면 1단계 무동작 가드가 의미를 갖는다 */
private const val RECOVERY_APPLY_CONTRAST = true
```

- [ ] **Step 4: UseCase를 만든다**

```kotlin
package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

class RecoverCandidatesUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "RecoverCandidatesUseCase::init" }
    }

    suspend operator fun invoke(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>> =
        repository.recoverCandidates(bitmapWrapper)
}
```

- [ ] **Step 5: 전체 유닛·ktlint·빌드를 확인한다**

Run: `./gradlew :domain:test :data:testDebugUnitTest :data:ktlintCheck :domain:ktlintCheck :app:assembleDebug`
Expected: 전부 통과

- [ ] **Step 6: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/utils/image/SegmentationRecoveryNormalizer.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt \
        domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt \
        domain/src/main/java/com/teamyg/parfait/domain/usecase/image/RecoverCandidatesUseCase.kt
git commit -m "$(cat <<'MSG'
feat: 재시도 회복 사다리를 저장소에 넣는다

검출 판은 원본을 복사해 만들고, 픽셀은 원본에서 읽는다. 단계마다 ModuleNotReady 만 사다리를 멈춘다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 7: ViewModel 재시도 분기

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: `RecoverCandidatesUseCase`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`SegmentationViewModelTest.kt`에 import `com.teamyg.parfait.domain.usecase.image.RecoverCandidatesUseCase`를 더하고,
필드 `private val recoverCandidates: RecoverCandidatesUseCase = mockk()`를 더한 뒤 `viewModel()` 헬퍼에
`recoverCandidatesUseCase = recoverCandidates,`를 넘긴다. 아래 7건을 추가한다. `candidate`·`bitmapWrapper`는 기존
필드이고 `saveBitmap`은 `@Before`가 이미 스텁한다. 파일의 기존 테스트처럼 `runTest {}`를 인자 없이 쓴다.

```kotlin
    @Test
    fun retry_afterEmptyCandidates_runsTheRecoveryLadder() = runTest {
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } returns Result.success(listOf(candidate))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        // 1차를 다시 돌지 않는다
        coVerify(exactly = 1) { segmentImage(any()) }
        coVerify(exactly = 1) { recoverCandidates(any()) }
        assertEquals(listOf(candidate), viewModel.state.value.candidates)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun retry_afterAnException_takesTheOriginalPathAgain() = runTest {
        coEvery { segmentImage(any()) } returns Result.failure(SegmentationException.ModuleNotReady(null))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        coVerify(exactly = 2) { segmentImage(any()) }
        coVerify(exactly = 0) { recoverCandidates(any()) }
    }

    @Test
    fun retry_afterTheRecoveryAlsoFailed_fallsBackToTheOriginalPath() = runTest {
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } returns Result.success(emptyList())
        val viewModel = viewModel()
        advanceUntilIdle()

        repeat(2) {
            viewModel.processIntent(SegmentationIntent.Retry)
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { recoverCandidates(any()) }
        coVerify(exactly = 2) { segmentImage(any()) }
    }

    @Test
    fun retry_pressedFourTimesAfterEmpty_runsTheLadderOnlyOnce() = runTest {
        // Given 같은 사진이라 1차도 회복도 계속 0건이다
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } returns Result.success(emptyList())
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 네 번 누른다 — 1차의 0건이 플래그를 덮으면 세 번째에 사다리가 다시 돈다
        repeat(4) {
            viewModel.processIntent(SegmentationIntent.Retry)
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { recoverCandidates(any()) }
        coVerify(exactly = 4) { segmentImage(any()) }
    }

    @Test
    fun retry_afterTheLadderWasAbortedByTheModule_mayRunTheLadderAgain() = runTest {
        // Given 사다리가 모듈 문제로 중간에 접혔다 — 끝까지 돈 것이 아니다
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } returns Result.failure(SegmentationException.ModuleNotReady(null))
        val viewModel = viewModel()
        advanceUntilIdle()

        repeat(3) {
            viewModel.processIntent(SegmentationIntent.Retry)
            advanceUntilIdle()
        }

        // 회복 → 1차(0건) → 회복
        coVerify(exactly = 2) { recoverCandidates(any()) }
        coVerify(exactly = 2) { segmentImage(any()) }
    }

    @Test
    fun retry_whileTheRecoveryRuns_clearsTheErrorAndShowsLoading() = runTest {
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } coAnswers {
            delay(1_000)
            Result.success(listOf(candidate))
        }
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isError)

        viewModel.processIntent(SegmentationIntent.Retry)
        runCurrent()

        // 에러 화면 위에 로딩 덮개가 겹치는 조합을 막는다
        assertFalse(viewModel.state.value.isError)
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun retry_recoveryThrowsUnexpectedly_restoresTheErrorScreen() = runTest {
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } throws IllegalStateException("boom")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        // 되돌리지 않으면 에러도 후보도 없는 화면에 갇힌다
        assertTrue(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isLoading)
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:compileDebugUnitTestKotlin`
Expected: FAIL — `No parameter with name 'recoverCandidatesUseCase' found`

- [ ] **Step 3: ViewModel을 고친다**

import `com.teamyg.parfait.domain.usecase.image.RecoverCandidatesUseCase`를 더하고, 생성자의 `segmentImageUseCase` 아래에
`private val recoverCandidatesUseCase: RecoverCandidatesUseCase,`를 더한다.

클래스 본문에 상태를 더한다.

```kotlin
    private enum class LastFailure { EXCEPTION, EMPTY }

    /** 재시도가 무엇을 돌지만 정한다. 화면이 안 쓰는 값이라 상태로 올리지 않는다 */
    private var lastFailure: LastFailure? = null

    /**
     * 이 사진으로 사다리를 끝까지 돌렸는가. 입력이 같으면 결과도 같아서 한 번만 돈다.
     *
     * ⚠️ 1차에서 다시 0건이 나와도 되돌리지 않는다. 되돌리면 사다리가 한 번 걸러 되풀이된다.
     */
    private var recoveryAttempted = false
```

`loadCandidates`의 세그멘테이션 결과 처리를 바꾼다.

```kotlin
            segmentImageUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    if (candidates.isEmpty()) {
                        lastFailure = LastFailure.EMPTY
                        updateState { copy(isError = true) }
                        return@onSuccess
                    }

                    lastFailure = null
                    updateState { copy(candidates = candidates) }
                }.onFailure { throwable ->
                    // 화면이 원인을 가르지 않으므로 원인은 여기에만 남는다
                    viewModelLogger.e(throwable) {
                        "세그멘테이션 실패 ${throwable::class.simpleName}, 원인 ${throwable.cause}"
                    }
                    lastFailure = LastFailure.EXCEPTION
                    updateState { copy(isError = true) }
                }
```

`processIntent`의 `Retry` 갈래를 바꾼다.

```kotlin
            SegmentationIntent.Retry ->
                if (lastFailure == LastFailure.EMPTY && !recoveryAttempted) recover() else loadCandidates()
```

`recover`를 더한다.

```kotlin
    /**
     * ⚠️ [LOAD_CANDIDATES_KEY] 를 진입·재시도와 공유한다. 다른 키를 쓰면 연타가 사다리를 겹쳐 돈다.
     */
    private fun recover() {
        val bitmapWrapper = originBitmapWrapper ?: return loadCandidates()

        launch(
            key = LOAD_CANDIDATES_KEY,
            onError = {
                lastFailure = LastFailure.EXCEPTION
                updateState { copy(isLoading = false, isError = true) }
            },
        ) {
            // 에러 표시를 안 걷으면 실패 화면 위에 로딩 덮개가 겹친다
            updateState { copy(isLoading = true, isError = false, candidates = emptyList()) }

            recoverCandidatesUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    // 끝까지 돌았다. 모듈 문제로 중간에 접히면 여기 오지 않아 다시 돌 기회가 남는다
                    recoveryAttempted = true
                    if (candidates.isEmpty()) {
                        lastFailure = LastFailure.EMPTY
                        updateState { copy(isError = true) }
                    } else {
                        lastFailure = null
                        updateState { copy(candidates = candidates) }
                    }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "회복 실패 ${throwable::class.simpleName}" }
                    lastFailure = LastFailure.EXCEPTION
                    updateState { copy(isError = true) }
                }

            updateState { copy(isLoading = false) }
        }
    }
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest :feature:segmentation:impl:ktlintCheck`
Expected: PASS. 기존 테스트가 전부 통과해야 한다.

- [ ] **Step 5: 커밋한다**

```bash
git add feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt \
        feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt
git commit -m "$(cat <<'MSG'
feat: 후보 0건 뒤 재시도가 회복 사다리를 한 번만 돌게 한다

직전 실패와 사다리 플래그로 가른다. 플래그는 1차 결과로 되돌리지 않아 사다리가 되풀이되지 않는다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 8: 전체 검증과 문서

코드 변경은 없다.

**Files:**
- Modify: `parfait/specs/2026-09-10-segmentation-retry-recovery.md` (문서 저장소)
- Modify: `parfait/specs/README.md`, `parfait/plans/README.md` (문서 저장소)

- [ ] **Step 1: 전체 검증을 돌린다**

```bash
./gradlew :domain:test :data:testDebugUnitTest :feature:segmentation:impl:testDebugUnitTest
./gradlew ktlintCheck
./gradlew :app:assembleDebug
```

Expected: 전부 통과. 새 유닛은 46건이다(Task 1 24, Task 2 6, Task 3 9, Task 7 7).

- [ ] **Step 2: 1차 경로 회귀를 실기기에서 확인한다**

Task 4·5가 1차 경로의 수확을 옮기고 일반화했으므로 그 회귀를 본다.

1. 평범한 사진 — 후보가 나오고, 로그에 `회복` 줄이 없다.
2. 피사체가 여럿인 사진 — 후보를 골라 확인 화면으로 간다. 공통 subject 루프의 회귀를 본다.
3. 고른 토핑을 캔버스에 올린 뒤 — **색이 원본과 같다.** 수확 이동의 회귀를 본다.
4. 실패 화면의 「편집 없이 사용」 — 기존 동작 그대로다.
5. 로그의 `세그멘테이션 후보 쌍 생성` 줄 — 1차에서는 `하한이었다면` 꼬리가 **붙지 않는다.**

- [ ] **Step 3: 회복 경로 확인을 보류로 기록한다**

검증 수단을 넣지 않기로 했으므로 회복 경로는 실기기에서 돌지 않는다. 스펙 「주의 / 열린 질문」의 「회복 경로 실기기
미검증」 항목이 실패 사진이 생겼을 때 볼 네 가지를 이미 적고 있다. PR 본문에도 이 사실을 적는다.

- [ ] **Step 4: 문서를 갱신한다**

- 스펙 `status`를 `implemented`로 올리고 머리에 as-built 배너를 단다. ⚠️ 배너에 **회복 경로는 실기기에서 돌지 않았다**를
  반드시 적는다. 계획과 갈린 자리가 있으면 적는다.
- `parfait/specs/README.md`와 `parfait/plans/README.md`의 **이미 있는 행**을 갱신한다. 새 행을 더하지 않는다.
- 머지 뒤 스펙과 계획을 `archive/`로 옮기는 것은 이 Task 밖이다.

- [ ] **Step 5: 문서 저장소에 커밋한다**

문서 저장소의 `docs/segmentation-retry-recovery-spec` 브랜치에서 커밋한다(`main` 직접 커밋 금지). 푸시와 PR은 사용자
승인 후에 한다.

---

## Self-Review

**스펙 커버리지**

| 스펙 절 | Task |
|---|---|
| §1 재시도 분기와 끈적한 플래그 | 7 |
| §2 사다리 두 단계, 무동작·수축 가드 | 1(계산), 6(실행·가드 로그) |
| §3 힌트 축, 단계 안 추출, 중앙 정사각형, 1차 비계산 | 1, 5(`hintThreshold = null`), 6 |
| §4·4-1 검출 해상도와 잠정값 | 1, 2, 5, 6 |
| §5 좌표계, 타입 강제, 재표본 후 자르기, 오프셋 | 1, 3, 5 |
| §6 알파 사본과 되돌림 새 판 | 5 |
| §7 캔버스 검사와 후보 단위 버림 | 1, 5 |
| §8 공통 subject 루프, 엄격 필터, 완화 로그 | 5, 6 |
| 동작/상태 — 상태 시퀀스, 취소, 디스패처 | 6, 7 |
| 에러 처리 — `ModuleNotReady` 양 갈래, 단계 포기, 1차 폴백 분류 유지 | 5, 6 |
| 판 소유권, 메모리 피크 | 5, 6 |
| 테스트 — 뮤테이션을 잡는 픽스처 | 1, 2, 3, 7 |
| 철회 조건 로그 다섯 필드 | 5, 6 |

**타입 일관성** — `DetectionBounds`(검출 공간)와 `SegmentationBounds`(원본 공간)를 끝까지 가르고
`RecoveryTransform.toOrigin`·`projectRegion`만 둘을 잇는다. Task 5·6이 쓰는 이름은 Task 1·3이 정의한 것과 같고,
스펙 API 절과도 같다.

**뮤테이션 대응** — 검수가 통과시킨 여섯에 각각 픽스처를 두었다: 축별 배율이 다른 변환(Task 1), 정확히 70% 경계
(Task 1), 흩어진 두 점(Task 1), 양 끝 이상치(Task 2), 2→3 확대 중간값(Task 3), 네 번 누르기(Task 7). 유닛이 닿지 않는
규칙 둘(픽셀은 원본에서, 원본에는 쓰지 않는다)은 `PlateSource.OriginRegion`에 픽셀 인자가 없다는 것과
`DetectionPlate.ownedByUs`가 구조적으로 막고, Task 6 머리의 리뷰 체크리스트가 확인한다.

**빈자리** — Task 4는 코드 블록 대신 이동표로 적었다. 옮기는 원본이 저장소에 그대로 있고 바꿀 것이 상수 이름 하나뿐이다.
