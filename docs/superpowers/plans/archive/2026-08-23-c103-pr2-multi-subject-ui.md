---
id: c103-pr2-multi-subject-ui
title: C-103 다중 후보 PR2 — 후보 여럿을 그리고 고르게 한다
status: done
archived_reason: PR #342(`34bf1939`)로 develop 머지 — PR1 스택을 안은 채 한 PR로 들어왔다
type: work-order
created: 2026-08-23
updated: 2026-08-24
platforms: android
owner: Parfait 팀
related_adr: ADR-0026
related_spec: c103-multi-subject-selection
related_code:
  - SegmentationHighlightGeometry.kt#pickCandidateIndex
  - SegmentationHighlightGeometry.kt#scaledRectOrNull
  - SegmentationSubjectHighlight.kt#SegmentationSubjectHighlight
  - SegmentationViewModel.kt#SegmentationState
  - SegmentationViewModel.kt#SegmentationIntent
  - SegmentationViewModel.kt#SegmentationEffect
  - SegmentationScreen.kt#SegmentationScreen
  - SegmentationRoute.kt#SegmentationRoute
  - ToppingDraftRepository
tags: [plan, parfait, segmentation, topping, c-103]
---

# C-103 다중 후보 PR2 — 후보 선택 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development`(권장) 또는 `superpowers:executing-plans`로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 후보를 화면에 전부 그리고, 사용자가 고른 하나만 저장해 다음 화면으로 보낸다.

**Architecture:** PR1이 만든 후보 목록을 화면까지 흘린다. 세 가지가 바뀐다. 첫째, 상태에서 경로 3필드를 걷고 `candidates` 목록을 둔다 — 저장이 탭 시점으로 옮겨 가면 화면이 경로를 들고 있을 이유가 없다. 둘째, 하이라이트 컴포넌트가 목록을 받아 후보마다 점선을 그리고 딤을 한 번에 뺀다. 셋째, **선택 시점의 순서가 계약이 된다** — 저장 → 초안 기록 완료 → 로딩 해제 → 화면 이동. 확인 화면은 정상 진입에서 스스로 초안을 적지 않고 구독만 하므로, 이 순서를 어기면 그 화면이 "다음"을 잠근 채 뜬다. 그리기와 탭 판정의 좌표 계산은 Compose 타입에 기대지 않는 순수 함수로 빼서 JVM에서 덮는다 — 이 화면에는 UI 테스트가 한 건도 없다.

**Tech Stack:** Kotlin · Jetpack Compose · Hilt · Navigation3 · kotlinx-coroutines-test · MockK · Turbine · kotlin.test

**Spec:** [`parfait/specs/archive/2026-08-23-c103-multi-subject-selection.md`](../../specs/archive/2026-08-23-c103-multi-subject-selection.md) — 「PR 분할」 표 **2번 행**

**베이스는 PR1 브랜치의 팁이다.** develop이 아니다. PR1이 `segmentImage`의 반환 타입과 `persistSubject`를 만들어 두지 않으면 이 계획의 어느 Task도 컴파일되지 않는다.

## Global Constraints

- **커밋하지 않는다.** 각 Task 마지막의 commit 단계는 **사용자가 명시적으로 커밋을 요청했을 때만** 실행한다. 기본은 미커밋이고, 변경 내용을 요약해 보고하는 것으로 Task를 닫는다. `git push`와 PR 생성은 어떤 경우에도 사용자 승인 없이 하지 않는다.
- **코드가 이미 말하는 것은 주석에 쓰지 않는다.** 뻔하지 않은 의도와 함정만 쓴다.
- **`@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.** 고정 틀(의도/반환값/파라미터)을 항상 두지 않는다.
- **다른 컴포넌트의 현재 상태를 단정하는 주석을 쓰지 않는다** — 낡는다. 써야 하면 근거 문서를 가리킨다.
- 아키텍처 결정 설명은 코드가 아니라 `parfait/adr/`·`parfait/architecture/` 몫이다. 코드에는 포인터 한 줄만 둔다.
- 테스트 이름은 `대상_상황_기대` 형식이고 본문에 `// Given` · `// When` · `// Then` 주석을 단다.
- **Task 1과 Task 4 끝에 `./gradlew test ktlintCheck` 가 통과해야 한다.** Task 2와 Task 3은 **의도적으로 모듈 컴파일이 깨진 상태로 끝난다**(사유는 Task 2에 적었다). 그 구간에서는 검증을 돌리지 않는다.
- ktlint 설정 둘을 미리 알아 둔다(`.editorconfig`). **파라미터가 2개 이상이면 길이와 무관하게 시그니처를 여러 줄로 쪼갠다**(`function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than = 2`). 그리고 **미사용 import는 오류다**(`no-unused-imports`).

---

## File Structure

| 파일 | 책임 | 상태 |
|---|---|---|
| `.../impl/component/SegmentationHighlightGeometry.kt` | 원본 좌표 → 화면 좌표 변환, 탭 대상 고르기 (순수) | 신설 |
| `.../impl/component/SegmentationSubjectHighlight.kt` | 딤·점선 그리기, 탭 받기 (Compose) | 수정 |
| `.../impl/viewmodel/SegmentationViewModel.kt` | 상태·의도·효과, 선택 시점 순서 | 수정 |
| `.../impl/screen/SegmentationScreen.kt` | 원본 위에 하이라이트를 겹친다 | 수정 |
| `.../impl/route/SegmentationRoute.kt` | 효과 수신·내비게이션 | 수정 |
| `.../impl/test/component/SegmentationHighlightGeometryTest.kt` | 좌표·탭 판정 검증 | 신설 |
| `.../impl/test/viewmodel/SegmentationViewModelTest.kt` | 화면 상태·순서 검증 | 수정 |

경로 접두사는 `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/`(테스트는 `src/test/java/...`)다.

---

### Task 1: 좌표 변환과 탭 대상 고르기를 순수 함수로 뺀다

지금 `subjectRect`는 `private`이고 Compose의 `Size`·`Rect`를 받는다. 이 라운드에서 탭 판정이 "맞는 후보 중 면적 최소"로 복잡해지는데, 그리기와 판정이 같은 계산을 공유하므로 여기가 조용히 깨지면 사용자는 엉뚱한 대상을 고르게 된다. Compose 타입에 기대지 않는 자리로 옮겨 JVM에서 덮는다.

**Files:**
- Create: `.../impl/component/SegmentationHighlightGeometry.kt`
- Test: `.../impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/component/SegmentationHighlightGeometryTest.kt`

**Interfaces:**
- Consumes: `SegmentationBounds`(기존)
- Produces:
  - `internal data class ScaledRect(left: Float, top: Float, right: Float, bottom: Float)` + `width`·`height`·`contains(x, y)`
  - `internal fun scaledRectOrNull(bounds: SegmentationBounds, imageWidth: Int, imageHeight: Int, canvasWidth: Float, canvasHeight: Float): ScaledRect?`
  - `internal fun pickCandidateIndex(boundsList: List<SegmentationBounds>, imageWidth: Int, imageHeight: Int, canvasWidth: Float, canvasHeight: Float, tapX: Float, tapY: Float): Int?`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`.../impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/component/SegmentationHighlightGeometryTest.kt`:

```kotlin
package com.teamyg.parfait.feature.segmentation.impl.component

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 원본 100×100 이미지를 200×400 캔버스에 Fit 으로 그리면 배율 2, 위아래 여백이 각 100 이다 */
private const val IMAGE_SIDE = 100
private const val CANVAS_WIDTH = 200f
private const val CANVAS_HEIGHT = 400f

class SegmentationHighlightGeometryTest {
    private fun bounds(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) = SegmentationBounds(left = left, top = top, right = right, bottom = bottom)

    private fun rectOf(bounds: SegmentationBounds) = scaledRectOrNull(
        bounds = bounds,
        imageWidth = IMAGE_SIDE,
        imageHeight = IMAGE_SIDE,
        canvasWidth = CANVAS_WIDTH,
        canvasHeight = CANVAS_HEIGHT,
    )

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

    @Test
    fun scaledRectOrNull_imageIsLetterboxed_offsetsByTheEmptyMargin() {
        // Given 원본 왼쪽 위 모서리에 붙은 10×10 영역
        val corner = bounds(left = 0, top = 0, right = 10, bottom = 10)

        // When 화면 좌표로 옮긴다
        val rect = rectOf(corner)

        // Then 배율 2 가 곱해지고, 세로로 남는 여백(100) 만큼 아래로 밀린다
        assertEquals(0f, rect?.left)
        assertEquals(100f, rect?.top)
        assertEquals(20f, rect?.right)
        assertEquals(120f, rect?.bottom)
    }

    @Test
    fun scaledRectOrNull_imageSizeIsNotUsable_returnsNull() {
        // Given 아직 이미지 치수를 모르는 상태
        // When 화면 좌표로 옮긴다
        val rect = scaledRectOrNull(
            bounds = bounds(left = 0, top = 0, right = 10, bottom = 10),
            imageWidth = 0,
            imageHeight = IMAGE_SIDE,
            canvasWidth = CANVAS_WIDTH,
            canvasHeight = CANVAS_HEIGHT,
        )

        // Then null 이다 — 0 으로 나눈 좌표를 그리면 Path 가 예외를 던진다
        assertNull(rect)
    }

    @Test
    fun scaledRect_contains_edgeIsInsideAndBeyondIsOutside() {
        // Given 화면 좌표로 (0,100)~(20,120) 인 사각형
        val rect = rectOf(bounds(left = 0, top = 0, right = 10, bottom = 10))!!

        // When·Then 경계는 안이고 그 바깥은 밖이다
        assertTrue(rect.contains(20f, 120f))
        assertFalse(rect.contains(20.1f, 120f))
    }

    @Test
    fun pickCandidateIndex_tapIsOutsideEveryCandidate_returnsNull() {
        // Given 왼쪽 위에 후보 하나
        val list = listOf(bounds(left = 0, top = 0, right = 10, bottom = 10))

        // When 아무 후보에도 안 걸리는 자리를 탭한다
        val picked = pick(list, tapX = 150f, tapY = 300f)

        // Then 고르지 않는다
        assertNull(picked)
    }

    @Test
    fun pickCandidateIndex_smallCandidateSitsInsideABigOne_picksTheSmallOne() {
        // Given 큰 후보 안에 작은 후보가 들어 있다(목록은 면적 내림차순)
        val big = bounds(left = 0, top = 0, right = 80, bottom = 80)
        val small = bounds(left = 20, top = 20, right = 40, bottom = 40)

        // When 둘 다 걸리는 자리를 탭한다(원본 좌표 30,30 → 화면 60,160)
        val picked = pick(listOf(big, small), tapX = 60f, tapY = 160f)

        // Then 안쪽 것을 고른다 — 바깥을 고르면 안쪽 대상을 영영 못 고른다
        assertEquals(1, picked)
    }

    @Test
    fun pickCandidateIndex_orderIsNotByArea_stillPicksTheSmallOne() {
        // Given 작은 것이 앞에 오도록 뒤집어 넘긴다
        val small = bounds(left = 20, top = 20, right = 40, bottom = 40)
        val big = bounds(left = 0, top = 0, right = 80, bottom = 80)

        // When 둘 다 걸리는 자리를 탭한다
        val picked = pick(listOf(small, big), tapX = 60f, tapY = 160f)

        // Then 목록 순서와 무관하게 면적이 작은 쪽이다 — 정렬 기준이 바뀌어도 안 깨져야 한다
        assertEquals(0, picked)
    }

    @Test
    fun pickCandidateIndex_tapHitsOnlyTheBigCandidate_picksIt() {
        // Given 큰 후보 안에 작은 후보가 들어 있다
        val big = bounds(left = 0, top = 0, right = 80, bottom = 80)
        val small = bounds(left = 20, top = 20, right = 40, bottom = 40)

        // When 작은 후보 바깥이면서 큰 후보 안인 자리를 탭한다(원본 60,60 → 화면 120,220)
        val picked = pick(listOf(big, small), tapX = 120f, tapY = 220f)

        // Then 큰 쪽도 고를 수 있다
        assertEquals(0, picked)
    }

    @Test
    fun pickCandidateIndex_noCandidates_returnsNull() {
        // Given 후보가 없다
        // When 아무 데나 탭한다
        val picked = pick(emptyList(), tapX = 100f, tapY = 200f)

        // Then 고르지 않는다
        assertNull(picked)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationHighlightGeometryTest*"`
Expected: 컴파일 실패 — `Unresolved reference: scaledRectOrNull`

- [ ] **Step 3: 순수 함수를 구현한다**

`.../impl/component/SegmentationHighlightGeometry.kt`:

```kotlin
package com.teamyg.parfait.feature.segmentation.impl.component

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.math.min

/**
 * 화면에 그려진 자리. Compose 타입을 쓰지 않는 것은 그리기와 탭 판정이 공유하는 이 계산을
 * 기기 없이 검증하기 위해서다.
 */
internal data class ScaledRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left

    val height: Float get() = bottom - top

    fun contains(
        x: Float,
        y: Float,
    ): Boolean = x in left..right && y in top..bottom
}

/**
 * 원본 픽셀 좌표인 [bounds] 를 `ContentScale.Fit` 으로 그려진 화면 좌표로 옮긴다.
 *
 * @return 이미지 치수가 아직 유효하지 않으면 `null`
 */
internal fun scaledRectOrNull(
    bounds: SegmentationBounds,
    imageWidth: Int,
    imageHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
): ScaledRect? {
    if (imageWidth <= 0 || imageHeight <= 0) return null

    val scale = min(canvasWidth / imageWidth, canvasHeight / imageHeight)
    val offsetX = (canvasWidth - imageWidth * scale) / 2f
    val offsetY = (canvasHeight - imageHeight * scale) / 2f

    return ScaledRect(
        left = offsetX + bounds.left * scale,
        top = offsetY + bounds.top * scale,
        right = offsetX + bounds.right * scale,
        bottom = offsetY + bounds.bottom * scale,
    )
}

/**
 * 탭한 자리에 걸리는 후보 중 **면적이 가장 작은 것**을 고른다.
 *
 * 큰 후보 안에 작은 후보가 들어 있을 때 바깥을 고르면 안쪽 대상을 영영 못 고른다. 목록이 면적
 * 내림차순이라 "뒤에서부터 첫 히트"로도 같은 결과가 나오지만, 그렇게 쓰면 이 함수의 올바름이
 * 호출부의 정렬 기준에 매달린다.
 *
 * @return 걸리는 후보가 없으면 `null`
 */
internal fun pickCandidateIndex(
    boundsList: List<SegmentationBounds>,
    imageWidth: Int,
    imageHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    tapX: Float,
    tapY: Float,
): Int? = boundsList
    .withIndex()
    .filter { (_, bounds) ->
        scaledRectOrNull(bounds, imageWidth, imageHeight, canvasWidth, canvasHeight)
            ?.contains(tapX, tapY) == true
    }.minByOrNull { (_, bounds) -> bounds.width.toLong() * bounds.height }
    ?.index
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationHighlightGeometryTest*"`
Expected: PASS (8건)

- [ ] **Step 5: 커밋 (사용자가 요청했을 때만)**

```bash
git add feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/component/SegmentationHighlightGeometry.kt \
        feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/component/SegmentationHighlightGeometryTest.kt
git commit -m "feat: 하이라이트 좌표 계산과 탭 대상 선택을 순수 함수로 뺀다"
```

---

### Task 2: 하이라이트 컴포넌트가 후보 목록을 받게 한다

**Files:**
- Modify: `.../impl/component/SegmentationSubjectHighlight.kt`

**Interfaces:**
- Consumes: Task 1의 `scaledRectOrNull`·`pickCandidateIndex`·`ScaledRect`
- Produces: `SegmentationSubjectHighlight(boundsList: List<SegmentationBounds>, imageWidth: Int, imageHeight: Int, onClickCandidate: (index: Int) -> Unit, modifier: Modifier, borderWidth: Dp, dashLength: Dp, dashGap: Dp)`

- [ ] **Step 1: 컴포넌트를 다시 쓴다**

`SegmentationSubjectHighlight.kt`의 `SegmentationSubjectHighlight`와 `private fun subjectRect`를 아래로 교체한다. `SegmentationHighlightDefaults`는 그대로 둔다.

```kotlin
/**
 * 감지된 후보들을 dashed Rectangle 로 표시하고, **모든 후보 바깥**을 어둡게 덮는 오버레이.
 *
 * [boundsList] 는 원본 이미지의 픽셀 좌표라서, 이미지가 [androidx.compose.ui.layout.ContentScale.Fit]
 * 으로 그려진 위치에 맞춰 변환한 뒤 그린다. 따라서 이미지와 **같은 크기의 영역**에 겹쳐 놓아야 한다.
 *
 * @param imageWidth 원본 이미지 가로 픽셀 수
 * @param imageHeight 원본 이미지 세로 픽셀 수
 */
@Composable
internal fun SegmentationSubjectHighlight(
    boundsList: List<SegmentationBounds>,
    imageWidth: Int,
    imageHeight: Int,
    onClickCandidate: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    borderWidth: Dp = SegmentationHighlightDefaults.BorderWidth,
    dashLength: Dp = SegmentationHighlightDefaults.DashLength,
    dashGap: Dp = SegmentationHighlightDefaults.DashGap,
) {
    Canvas(
        modifier = modifier.pointerInput(boundsList, imageWidth, imageHeight) {
            detectTapGestures { tapOffset ->
                pickCandidateIndex(
                    boundsList = boundsList,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    canvasWidth = size.width.toFloat(),
                    canvasHeight = size.height.toFloat(),
                    tapX = tapOffset.x,
                    tapY = tapOffset.y,
                )?.let(onClickCandidate)
            }
        },
    ) {
        val rects = boundsList.mapNotNull { bounds ->
            scaledRectOrNull(
                bounds = bounds,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                canvasWidth = size.width,
                canvasHeight = size.height,
            )
        }

        if (rects.isEmpty()) return@Canvas

        // 후보를 모두 담은 하나의 Path 를 빼면, 후보 수만큼 clipRect 를 중첩한 것과 결과가 같으면서
        // 재귀 없이 평평하다
        val holes = Path().apply {
            rects.forEach { rect ->
                addRect(Rect(left = rect.left, top = rect.top, right = rect.right, bottom = rect.bottom))
            }
        }

        clipPath(path = holes, clipOp = ClipOp.Difference) {
            drawRect(color = YGAtomicColors.Transparency.Black25)
        }

        rects.forEach { rect ->
            drawRect(
                color = YGAtomicColors.Gray.White,
                topLeft = Offset(x = rect.left, y = rect.top),
                size = Size(width = rect.width, height = rect.height),
                style = Stroke(
                    width = borderWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(dashLength.toPx(), dashGap.toPx()),
                    ),
                ),
            )
        }
    }
}
```

import를 바꾼다. **세 줄을 지운다** — `androidx.compose.ui.graphics.drawscope.clipRect` · `androidx.compose.ui.unit.toSize` · `kotlin.math.min`. 마지막 것은 삭제 대상인 `subjectRect` 에서만 쓰였고 새 코드에는 `min` 호출이 없다. **두 줄을 더한다** — `androidx.compose.ui.graphics.Path` · `androidx.compose.ui.graphics.drawscope.clipPath`.

⚠️ `androidx.compose.ui.geometry.Rect` 는 **남긴다.** `addRect(Rect(...))` 가 쓴다. 미사용 import 가 하나라도 남으면 Task 4의 `ktlintCheck` 가 실패한다.

> `Path` 를 `Canvas` 블록 안에서 만드는 것은 그리기마다 할당을 뜻한다. 이 화면은 정적인 사진 위의 오버레이라 프레임이 계속 돌지 않으므로 지금은 두고, 애니메이션이 붙으면 그때 `remember` 로 올린다.

- [ ] **Step 2: Preview를 고친다**

같은 파일 아래쪽 `SegmentationSubjectHighlightPreview`를 바꾼다. 후보 둘을 넣어 다중 표시가 Preview에서 바로 보이게 한다.

```kotlin
@YGPreview
@Composable
private fun SegmentationSubjectHighlightPreview() = PreviewBox {
    SegmentationSubjectHighlight(
        boundsList = listOf(
            SegmentationBounds(left = 80, top = 120, right = 320, bottom = 480),
            SegmentationBounds(left = 40, top = 40, right = 140, bottom = 110),
        ),
        imageWidth = 400,
        imageHeight = 600,
        onClickCandidate = {},
        modifier = Modifier.fillMaxSize(),
    )
}
```

- [ ] **Step 3: 컴파일을 확인한다**

Run: `./gradlew :feature:segmentation:impl:compileDebugKotlin`
Expected: 실패 — `SegmentationScreen.kt`가 아직 옛 파라미터(`bounds`·`onClickSubject`)로 부른다. 이 실패는 Task 3이 닫는다.

> 이 Task를 단독으로 초록으로 만들려면 화면과 상태를 함께 고쳐야 하는데, 그러면 리뷰가 그리기와 상태 재편을 한 덩어리로 보게 된다. **컴파일이 깨진 채로 Task 3으로 넘어간다** — 그래서 이 Task에는 테스트 실행 지시가 없다.

- [ ] **Step 4: 커밋 (사용자가 요청했을 때만)**

```bash
git add feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/component/SegmentationSubjectHighlight.kt
git commit -m "feat: 하이라이트가 후보 목록을 받아 여럿을 그린다"
```

---

### Task 3: 선택 시점의 순서를 못 박고 화면까지 결선한다

이 라운드에서 가장 조용히 깨지는 자리다. 확인 화면(`SegmentationConfirmViewModel`)은 정상 진입에서 스스로 초안을 적지 않고 `collectDraft`로 구독만 하므로, **이 화면이 초안의 유일한 writer**다. 저장과 기록과 이동이 모두 탭 시점으로 몰리면서 순서가 결과를 가른다.

**화면과 Route까지 이 Task에 넣는다.** 상태에서 필드를 걷으면 그것을 읽는 화면이 함께 깨지고, `testDebugUnitTest`는 main 소스셋 컴파일에 의존한다. 화면을 다음 Task로 미루면 **이 Task의 테스트를 아예 돌릴 수 없다.**

**Files:**
- Modify: `.../impl/viewmodel/SegmentationViewModel.kt`
- Modify: `.../impl/screen/SegmentationScreen.kt`
- Modify: `.../impl/route/SegmentationRoute.kt`
- Test: `.../impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: PR1의 `SegmentationCandidate`·`PersistSubjectUseCase`, `ToppingDraftRepository.record`(흐름이 안 열려 있으면 `false`)
- Produces:
  - `data class SegmentationState(isLoading: Boolean, originBitmap: Bitmap?, candidates: List<SegmentationCandidate>)`
  - `SegmentationIntent.ClickCandidate(index: Int)`
  - `SegmentationEffect.GoToConfirm(subjectImagePath: String, trimmedSubjectImagePath: String)`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`SegmentationViewModelTest.kt`에서 상태 단언을 후보 기준으로 바꾸고 선택 흐름 테스트를 더한다.

먼저 `init` 계열 3건의 단언을 바꾼다. `assertEquals(SUBJECT_PATH, ...state.subjectImagePath)` 를 쓰던 자리(`init_segmentationSucceeds_publishesSubjectImagePath`·`init_cacheClearThrows_stillSegments`·`init_recentImageRecordThrows_stillSegments`)는 모두 아래처럼 바꾼다:

```kotlin
        assertEquals(listOf(candidate), viewModel.state.value.candidates)
```

`init_segmentationSucceeds_publishesSubjectImagePath`는 이름도 `init_segmentationSucceeds_publishesCandidates`로 바꾼다.

**PR1이 남긴 `init` 기준 초안 테스트 2건을 지운다** — `init_segmentationSucceeds_recordsTheDraft`와 `init_segmentationFails_recordsNothing`. 진입이 더는 초안을 적지 않으므로 의미가 뒤집혔다. 아래 선택 시점 테스트가 그 자리를 대신한다. `init_persistFails_tellsTheUser`도 지운다 — 진입이 저장하지 않는다.

그리고 아래를 더한다:

```kotlin
    @Test
    fun init_segmentationSucceeds_persistsNothingYet() = runTest {
        // Given 후보가 잡히는 정상 응답
        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 아직 아무것도 떨구지 않는다 — 고르지도 않은 후보를 디스크에 쓰지 않는다
        coVerify(exactly = 0) { persistSubject(any()) }
        coVerify(exactly = 0) { toppingDraftRepository.record(any(), any(), any(), any()) }
    }

    @Test
    fun clickCandidate_succeeds_recordsTheDraftBeforeNavigating() = runTest {
        // Given 화면이 열려 후보가 실려 있다
        coEvery {
            toppingDraftRepository.record(any(), any(), any(), any())
        } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 초안을 다 적은 뒤에 이동한다 — 순서가 뒤집히면 확인 화면이 초안 없음으로 잠긴 채 뜬다
        coVerifyOrder {
            persistSubject(candidate)
            toppingDraftRepository.record(
                subjectImagePath = TRIMMED_SUBJECT_PATH,
                cutoutImagePath = SUBJECT_PATH,
                borderColorArgb = null,
                borderWidthDp = null,
            )
        }
        viewModel.effect.test {
            assertEquals(
                SegmentationEffect.GoToConfirm(
                    subjectImagePath = SUBJECT_PATH,
                    trimmedSubjectImagePath = TRIMMED_SUBJECT_PATH,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun clickCandidate_succeeds_releasesTheLoadingOverlay() = runTest {
        // Given 화면이 열려 후보가 실려 있다
        coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 로딩이 걷힌다 — 이동이 goTo 라 이 화면이 백스택에 남고, 켠 채 나가면 돌아왔을 때 갇힌다
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun clickCandidate_persistFails_keepsTheCandidatesAndTellsTheUser() = runTest {
        // Given 저장이 실패하는 상황
        coEvery { persistSubject(candidate) } returns Result.failure(IllegalStateException("no space"))
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 알리되 목록은 남긴다 — 사용자가 다른 후보를 고를 수 있어야 한다
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
        assertEquals(listOf(candidate), viewModel.state.value.candidates)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun clickCandidate_draftIsNotOpen_doesNotNavigate() = runTest {
        // Given 흐름이 열려 있지 않아 record 가 false 를 돌려주는 상황
        coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns false
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 이동하지 않고 알린다 — 초안 없이 보내면 확인 화면이 어차피 막힌다
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
    }

    @Test
    fun clickCandidate_tappedTwice_persistsOnlyOnce() = runTest {
        // Given 화면이 열려 후보가 실려 있다
        coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 저장이 끝나기 전에 두 번 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 한 번만 떨군다 — 로딩 오버레이가 터치를 막아 주는지에 기대지 않는다
        coVerify(exactly = 1) { persistSubject(any()) }
    }

    @Test
    fun clickCandidate_indexIsOutOfRange_doesNothing() = runTest {
        // Given 후보가 하나뿐인 상태
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 없는 자리를 가리키는 의도가 들어온다(상태 교체와 탭이 경합하면 생긴다)
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 3))
        advanceUntilIdle()

        // Then 아무 일도 일어나지 않는다
        coVerify(exactly = 0) { persistSubject(any()) }
        viewModel.effect.test { expectNoEvents() }
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationViewModelTest*"`
Expected: 컴파일 실패. **다만 터지는 자리가 테스트가 아니라 main 소스셋이다** — Task 2가 `SegmentationSubjectHighlight`의 파라미터를 바꿔 놓아 `SegmentationScreen.kt`가 먼저 깨진다. 테스트 소스셋 컴파일에는 닿지도 못한다. 이 Step은 "아직 초록이 아니다"를 확인하는 것이고, 테스트가 실제로 도는 것은 Step 8이다.

- [ ] **Step 3: 상태·의도·효과를 다시 쓴다**

`SegmentationViewModel.kt` 상단의 세 선언을 바꾼다:

```kotlin
data class SegmentationState(
    val isLoading: Boolean = true,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect {
    /** 재시도 동선이 없는 실패라 상태로 남기지 않는다 — 토스트로 한 번 알리고 끝이다. */
    data object ShowError : SegmentationEffect

    data class GoToConfirm(
        val subjectImagePath: String,
        val trimmedSubjectImagePath: String,
    ) : SegmentationEffect
}
```

import에서 `SegmentationBounds`를 걷고 `SegmentationCandidate`를 더한다.

- [ ] **Step 4: `init`에서 저장을 걷는다**

PR1이 넣어 둔 `persistSubjectUseCase(candidate)` 블록을 아래로 교체한다:

```kotlin
            segmentImageUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    if (candidates.isEmpty()) {
                        postSideEffect(SegmentationEffect.ShowError)
                        return@onSuccess
                    }

                    updateState { copy(candidates = candidates) }
                }.onFailure { postSideEffect(SegmentationEffect.ShowError) }
```

- [ ] **Step 5: 선택 처리를 구현한다**

같은 파일의 `processIntent`를 채우고 아래에 선택 처리를 더한다:

```kotlin
    override fun processIntent(intent: SegmentationIntent) {
        when (intent) {
            is SegmentationIntent.ClickCandidate -> selectCandidate(intent.index)
        }
    }

    /**
     * 저장 → 초안 기록 → 이동 순서를 지킨다. 확인 화면은 정상 진입에서 초안을 구독만 하므로,
     * 기록을 마치기 전에 보내면 그 화면이 "다음"을 잠근 채 뜬다.
     */
    private fun selectCandidate(index: Int) {
        val candidate = state.value.candidates.getOrNull(index) ?: return

        launch(key = SELECT_CANDIDATE_KEY) {
            updateState { copy(isLoading = true) }

            persistSubjectUseCase(candidate)
                .onSuccess { result ->
                    val recorded = runSuspendCatching {
                        toppingDraftRepository.record(
                            subjectImagePath = result.trimmedSubjectImagePath,
                            cutoutImagePath = result.subjectImagePath,
                            borderColorArgb = null,
                            borderWidthDp = null,
                        )
                    }.getOrDefault(false)

                    // 이동이 goTo 라 이 화면이 백스택에 남는다. 켠 채 나가면 돌아왔을 때 갇힌다
                    updateState { copy(isLoading = false) }

                    if (recorded) {
                        postSideEffect(
                            SegmentationEffect.GoToConfirm(
                                subjectImagePath = result.subjectImagePath,
                                trimmedSubjectImagePath = result.trimmedSubjectImagePath,
                            ),
                        )
                    } else {
                        postSideEffect(SegmentationEffect.ShowError)
                    }
                }.onFailure {
                    updateState { copy(isLoading = false) }
                    postSideEffect(SegmentationEffect.ShowError)
                }
        }
    }
```

파일 맨 아래(또는 상단 상수 자리)에 키를 둔다:

```kotlin
private const val SELECT_CANDIDATE_KEY = "select-candidate"
```

`launch(key = ...)`가 중복 탭을 막는다 — 같은 키의 작업이 돌고 있으면 새로 시작하지 않는다(`BaseViewModel.launch` KDoc).

- [ ] **Step 6: 화면이 후보 목록을 넘기게 한다**

`SegmentationScreen.kt`에서 `SegmentationScreen`의 `onClickSubject: () -> Unit` 파라미터를 `onClickCandidate: (index: Int) -> Unit`으로 바꾸고, `SegmentationResultImage` 호출과 정의를 바꾼다:

```kotlin
            SegmentationResultImage(
                originBitmap = state.originBitmap,
                boundsList = state.candidates.map { it.bounds },
                onClickCandidate = onClickCandidate,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
```

```kotlin
/**
 * 촬영한 원본 이미지 위에 세그멘테이션 결과(딤 + dashed Rectangle)를 겹쳐 보여준다.
 */
@Composable
private fun SegmentationResultImage(
    originBitmap: Bitmap?,
    boundsList: List<SegmentationBounds>,
    onClickCandidate: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        originBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            if (boundsList.isNotEmpty()) {
                SegmentationSubjectHighlight(
                    boundsList = boundsList,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    onClickCandidate = onClickCandidate,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}
```

파일 아래 `PreviewSegmentationScreen`의 `onClickSubject = {}` 를 `onClickCandidate = {}` 로 바꾼다. `SegmentationBounds` import는 새 시그니처가 계속 쓰므로 남긴다.

- [ ] **Step 7: Route가 효과로 이동하게 한다**

`SegmentationRoute.kt`의 `LaunchedEffect` 블록과 `SegmentationScreen` 호출을 바꾼다:

```kotlin
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SegmentationEffect.ShowError -> toastPolicy.showError(errorMessage)
                // 백스택에 쌓아 올려서 뒤로가기 하면 객체 인식이 끝난 이 화면으로 그대로 돌아온다
                is SegmentationEffect.GoToConfirm -> navigator.goTo(
                    NavKeySegmentationConfirm(
                        sourceImageUri = key.sourceImageUri,
                        subjectImagePath = effect.subjectImagePath,
                        trimmedSubjectImagePath = effect.trimmedSubjectImagePath,
                    ),
                )
            }
        }
    }
```

```kotlin
            onClickCandidate = { index ->
                viewModel.processIntent(SegmentationIntent.ClickCandidate(index))
            },
```

import에 `com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationIntent`를 더한다.

⚠️ **옛 자리의 주석을 함께 옮긴다.** `// 백스택에 쌓아 올려서 뒤로가기 하면…` 주석은 지금 `onClickSubject` 위에 붙어 있다. 위 스니펫이 그것을 `LaunchedEffect` 안으로 옮겼으므로, `SegmentationScreen` 호출부에 남은 옛 주석을 지운다.

- [ ] **Step 8: 테스트가 통과하는지 확인한다**

여기서 비로소 모듈이 온전히 컴파일된다. `testDebugUnitTest`는 main 소스셋 컴파일에 의존하므로, 화면과 Route를 고치기 전에는 이 명령이 테스트 컴파일에 닿지도 못한다.

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationViewModelTest*"`
Expected: PASS

- [ ] **Step 9: 커밋 (사용자가 요청했을 때만)**

```bash
git add feature/segmentation/impl
git commit -m "feat: 후보를 고르는 시점에 저장하고 초안을 적는다"
```

---

### Task 4: 전체 검증과 문서 갱신

코드 변경이 없는 Task다. 앞선 셋이 만든 결과를 저장소 전체 기준으로 확인하고, 이 라운드로 거짓이 된 문서를 고친다.

**Files:**
- 없음(TJYG-Android). 문서는 `team-yg-pesonal-agent` 저장소에서 고친다.

- [ ] **Step 1: 전체 검증**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: 전부 통과

- [ ] **Step 2: 선행 스펙에 갱신 표기를 단다**

이 저장소가 아니라 **`team-yg-pesonal-agent`** 쪽 작업이다. `parfait/specs/archive/2026-08-15-c103-segmentation-topping-edit.md`의 네 자리에 이 라운드로 거짓이 된 서술을 표기한다.

- 범위 제외의 "다중 피사체 선택(C-103-select 본래 의미) — ML Kit `foregroundConfidenceMask` 단일 전경만 쓴다"
- 화면 ID 대응 표 C-103-select 행 비고의 "단일 bounding box 하이라이트 탭. **다중 검출 분기 없음**"
- 드리프트 3의 "C-103-select가 사실상 없다"
- 정책 대조 표의 "C-103-loading / C-103-select 분리 → 부분 이행"

표기 형식은 같은 파일의 기존 `📌`·`🔁` 각주를 따른다. 새 스펙([c103-multi-subject-selection](../../specs/archive/2026-08-23-c103-multi-subject-selection.md))을 가리킨다.

- [ ] **Step 3: 새 스펙의 드리프트 한 줄을 고친다**

`parfait/specs/2026-08-23-c103-multi-subject-selection.md` 「다중 하이라이트」 절이 *"`Path`는 후보 목록과 캔버스 크기를 키로 `remember`해 프레임마다 새로 만들지 않는다"*고 적었는데, 구현은 `Canvas` 블록 안에서 매번 만든다. 이 화면은 정적인 사진 위의 오버레이라 프레임이 계속 돌지 않아 그렇게 정했다(Task 2의 인용문에 사유가 있다). 스펙 문장을 구현에 맞춘다.

- [ ] **Step 4: 커밋 (사용자가 요청했을 때만)**

문서 저장소(`team-yg-pesonal-agent`)에서 커밋한다. 이 저장소는 `main`에 직접 커밋하지 않으므로 브랜치를 먼저 판다.

---

## 검증

자동 테스트가 덮지 못하는 자리를 실기기로 확인한다. 이 화면에는 UI 테스트가 없다.

1. 피사체 여럿인 사진에서 점선 박스가 **후보 수만큼** 뜨고, 그 바깥만 어두운가.
2. 겹친 후보에서 **안쪽과 바깥쪽을 모두** 고를 수 있는가(안쪽 박스 안 / 안쪽 밖이면서 바깥 안).
3. 후보를 탭하면 확인 화면이 뜨고 **"다음"이 잠기지 않는가** — 초안 순서가 맞다는 뜻이다.
4. 확인 화면에서 뒤로 오면 **로딩 오버레이 없이** 후보 목록이 그대로 보이는가.
5. 다른 후보를 다시 골라도 정상 진행되는가.
6. 저장 중 뒤로 가기를 눌러도 크래시하지 않는가.
7. 배경이 복잡한 사진에서 후보 개수가 감당할 만한가 — `MIN_SUBJECT_AREA_RATIO`·`MAX_SUBJECT_COUNT` 조정 근거(OQ-P-267).
8. 후보를 들고 확인 화면까지 다녀와도 하이라이트가 정상인가 — `segmenter.close()` 이후 비트맵 수명(OQ-P-269).

## 범위 밖

- 후보 비트맵의 명시적 해제(OQ-P-266).
- Safe Margin +20% 캔버스(OQ-P-150).
- 후보가 1개일 때 확인 화면 직행 — 스펙 「화면 ID 대응」 절의 근거로 하지 않는다.
- 후보에 순번·라벨 표시.
- 세그멘테이션 실패 후 재시도(OQ-P-003 ①).
