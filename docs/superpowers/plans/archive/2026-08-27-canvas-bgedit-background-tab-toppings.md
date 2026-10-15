---
id: canvas-bgedit-background-tab-toppings
title: PR1 — 배경 편집 화면 배경 탭 토핑 렌더링
status: done
type: work-order
created: 2026-08-27
updated: 2026-08-31
platforms: android
owner: Parfait 팀
related_adr: ADR-0029
related_spec: canvas-today-ssot-polling
related_code: CanvasBGEditScreen, CanvasToppingItem, CanvasEditTab, CanvasBGEditUiState, ToppingGeometry, ToppingHitTarget, YGToppingCutoutImage, ToppingCornerButtons
archived_reason: PR #404 로 develop 머지(2026-08-31) — 배경 탭 토핑 렌더링이 스택 3단의 첫 단으로 함께 들어왔다
tags: [plan, parfait, canvas, compose]
---

# PR1 — 배경 편집 화면 배경 탭 토핑 렌더링 Implementation Plan

> ✅ **develop 에 머지됐다(2026-08-31, PR #404 `2c7bb31b`)** — 스택 3단이 한 머지로 들어왔고
> 머지 커밋의 트리가 브랜치 팁(`6bd21fb7`)과 같아 **충돌 해소 편집이 0건**이다. 아래 머리말이
> "release 계보에만 있다"고 적던 상태는 끝났다(OQ-P-311 ③). Task 의 체크는 완료 표시이고,
> 「수동 확인」은 실기기 확인 기록이 없어 그대로 둔다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 배경 편집 화면(C-301)의 배경 탭에서도 캔버스에 올라간 토핑을 반투명·비상호작용으로 그린다.

**Architecture:** `CanvasBGEditScreen`이 토핑 레이어 전체를 `selectedTab == CanvasEditTab.TOPPING` 조건 하나로 감싸고 있다. 그 게이트를 걷어내고 **그리기**와 **상호작용**을 분리한다. 두 탭이 공유하는 것은 저장된 배치대로 토핑 이미지를 겹쳐 그리는 부분뿐이고, 딤 오버레이·입력 레이어·모서리 버튼·접근성 클릭·알파 마스크는 전부 토핑 탭 전용으로 남긴다. ViewModel과 UiState는 건드리지 않는다.

**Tech Stack:** Kotlin, Jetpack Compose, Coil 3(`rememberAsyncImagePainter`)

**Spec:** [`parfait/specs/2026-08-27-canvas-today-ssot-polling.md`](../../specs/archive/2026-08-27-canvas-today-ssot-polling.md) 「PR1 — 배경 탭 토핑 렌더링」

**작업 저장소:** `TJYG-Android` (remote `mash-up-kr/TJYG-Android`). 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다. 브랜치 `feature/#392-canvas-topping` 위에 쌓는다 — 그 브랜치는 지금 `develop`과 같고 작업 트리는 깨끗하다.

## Global Constraints

- **커밋은 하되 push·PR은 사용자 확인 후에만.** 로컬 커밋과 브랜치 생성은 확인 없이 해도 된다.
- **코드 주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다). 써야 하면 근거 문서를 가리킨다.
  - 아키텍처 결정은 코드가 아니라 `parfait/adr/`·`parfait/architecture/`에 쓰고 코드에는 포인터 한 줄만 둔다.
- **주석·KDoc은 한국어로 쓴다.** 기존 파일의 문체를 따른다.
- **이 모듈에는 `androidTest`도 Robolectric도 없다.** `build.gradle.kts`가 `parfait.test.unit` 하나만 쓴다. **이 PR에서 테스트 하니스를 새로 세우지 않는다** — 검증은 `@YGPreview` + 수동 확인이다.
- **한 파일만 바뀐다.** `CanvasBGEditScreen.kt` 밖으로 나가는 수정이 필요해 보이면 멈추고 사용자에게 묻는다.
- **매직 넘버 대신 이름 있는 상수.** 불투명도 값은 `BACKGROUND_TAB_TOPPING_ALPHA`.
- **⚠️ `lintDebug`는 기준선에서 이미 실패한다.** 이 PR과 무관한 `route/CanvasToppingPlaceRoute.kt`의 `LocalContextGetResourceValueCall` 4건 때문이다. **그 파일을 고치지 않는다.** 판정 기준은 아래 각 Task에 적었다.
- **CI가 게이트하는 것은 `ktlintCheck`와 `test`다**(`.github/workflows/`). `lintDebug`를 도는 CI는 없다. 그래서 이 계획은 매 Task에서 `ktlintMainSourceSetCheck`를 돌린다.

---

## File Structure

| 파일 | 책임 | 변경 |
|------|------|------|
| `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt` | 배경 편집 화면 전체 — 캔버스 미리보기 박스, 토핑 레이어, 팔레트, 모달 | 그리기 정보 추출 함수 신설, 탭 게이트 분리, `alpha` 파라미터, Preview 두 벌 |

이 파일은 이미 600줄대라 더 키우지 않는다. 새로 만드는 것은 **기존 `rememberBGEditHitEntries`에서 그리기용 정보만 떼어낸 것 하나**뿐이다.

`BGEditHitEntry`·`CanvasToppingImage`·`rememberBGEditHitEntries`를 참조하는 다른 파일은 저장소에 없다.

---

### Task 1: 그리기 정보와 판정 정보를 분리한다

지금 `rememberBGEditHitEntries`는 그리기에 필요한 `painter`와 판정에 필요한 `ToppingHitTarget`을 한 번에 만들고, 그 과정에서 `rememberToppingAlphaMasks`로 비트맵을 디코딩한다. 배경 탭은 판정을 하지 않으므로 마스크가 필요 없다.

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt`

**Interfaces:**
- Consumes: `CanvasToppingItem`, `toppingImageSize(longSide, aspectRatio): DpSize`, `toppingLongSide`, `toppingCenter(...): DpOffset`, `ToppingHitTarget`, `rememberToppingAlphaMasks`
- Produces:
  - `private data class BGEditDrawEntry(val topping: CanvasToppingItem, val painter: AsyncImagePainter, val center: DpOffset, val size: DpSize, val drawnBorderWidthDp: Float)`
  - `@Composable private fun rememberBGEditDrawEntries(toppings: List<CanvasToppingItem>, canvasWidth: Dp, canvasHeight: Dp): List<BGEditDrawEntry>`
  - `private data class BGEditHitEntry(val draw: BGEditDrawEntry, val target: ToppingHitTarget)` — `topping`은 `draw`로 위임
  - `@Composable private fun rememberBGEditHitEntries(drawEntries: List<BGEditDrawEntry>): List<BGEditHitEntry>`

- [x] **Step 1: import를 더한다**

이 파일에는 `androidx.compose.ui.unit.DpOffset` import가 **없다.** `toppingCenter`가 `DpOffset`을 돌려주므로 반드시 추가한다.

```kotlin
import androidx.compose.ui.unit.DpOffset
```

- [x] **Step 2: `BGEditDrawEntry`와 그리기 정보 추출 함수를 만든다**

기존 `BGEditHitEntry` 선언 바로 위에 넣는다.

```kotlin
/** 두 탭이 공유하는 그리기 정보. 판정(마스크·[ToppingHitTarget])은 여기 없다 */
private data class BGEditDrawEntry(
    val topping: CanvasToppingItem,
    // Painter 로 좁히면 state 를 잃어 테두리 조건을 볼 수 없다
    val painter: AsyncImagePainter,
    val center: DpOffset,
    val size: DpSize,
    val drawnBorderWidthDp: Float,
)

/**
 * 배경 탭에서도 부르므로 알파 마스크를 요청하지 않는다 — 마스크 준비는 비트맵 디코딩을
 * 동반하는데 배경 탭은 판정을 하지 않는다.
 */
@Composable
private fun rememberBGEditDrawEntries(
    toppings: List<CanvasToppingItem>,
    canvasWidth: Dp,
    canvasHeight: Dp,
): List<BGEditDrawEntry> = toppings.map { topping ->
    key(topping.parfaitImageId) {
        val painter = rememberAsyncImagePainter(model = topping.drawnModel)
        val painterState by painter.state.collectAsState()
        val intrinsicSize = painter.intrinsicSize

        val aspectRatio = if (intrinsicSize.isSpecified && intrinsicSize.height > 0f) {
            intrinsicSize.width / intrinsicSize.height
        } else {
            0f
        }

        BGEditDrawEntry(
            topping = topping,
            painter = painter,
            center = toppingCenter(
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                positionX = topping.positionX,
                positionY = topping.positionY,
            ),
            size = toppingImageSize(
                longSide = toppingLongSide(canvasWidth, topping.scale),
                aspectRatio = aspectRatio,
            ),
            // 테두리를 그리지 않는 상태에서는 판정도 넓히지 않는다 — 그리지 않은 링만큼 부풀면
            // 판정이 외형과 어긋난다
            drawnBorderWidthDp = topping.borderLayers
                .firstOrNull()
                ?.takeIf { painterState is AsyncImagePainter.State.Success }
                ?.widthDp
                ?: 0f,
        )
    }
}
```

- [x] **Step 3: `BGEditHitEntry`가 그리기 정보를 품게 바꾼다**

```kotlin
private data class BGEditHitEntry(
    val draw: BGEditDrawEntry,
    val target: ToppingHitTarget,
) {
    val topping: CanvasToppingItem get() = draw.topping
}
```

- [x] **Step 4: `rememberBGEditHitEntries`가 그리기 함수의 결과를 받게 바꾼다**

⚠️ 시그니처를 **한 줄로** 쓴다. `.editorconfig`가 `ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than = 2`이고 이 선언은 한 줄로 합쳐도 120자 미만이라, 멀티라인으로 두면 ktlint가 잡는다.

```kotlin
/**
 * 그리기와 판정이 같은 painter 를 본다. 각각 만들면 비율이 서로 다른 시점의 값이 될 수 있다.
 *
 * 마스크는 판정에 실제로 쓰는 내 토핑만 요청한다. 남의 토핑은 탭 대상도 드래그 대상도 아니고
 * 그리는 데는 painter 만 있으면 되므로, 마스크가 없어도 화면이 달라지지 않는다.
 */
@Composable
private fun rememberBGEditHitEntries(drawEntries: List<BGEditDrawEntry>): List<BGEditHitEntry> {
    val masks = rememberToppingAlphaMasks(
        drawEntries.filter { it.topping.isMine }.map { it.topping.drawnModel },
    )
    val density = LocalDensity.current

    return drawEntries.map { entry ->
        BGEditHitEntry(
            draw = entry,
            target = with(density) {
                ToppingHitTarget(
                    centerXPx = entry.center.x.toPx(),
                    centerYPx = entry.center.y.toPx(),
                    imageWidthPx = entry.size.width.toPx(),
                    imageHeightPx = entry.size.height.toPx(),
                    rotationDegrees = entry.topping.rotationDegrees,
                    borderWidthPx = entry.drawnBorderWidthDp.dp.toPx(),
                    mask = masks[entry.topping.drawnModel],
                )
            },
        )
    }
}
```

- [x] **Step 5: 호출부와 `CanvasToppingImage`의 painter 접근을 고친다**

`BoxWithConstraints` 안, 기존 `val entries = rememberBGEditHitEntries(...)` 자리를 두 줄로 가른다.

```kotlin
val drawEntries = rememberBGEditDrawEntries(
    toppings = uiState.toppings,
    canvasWidth = canvasWidth,
    canvasHeight = canvasHeight,
)
val entries = rememberBGEditHitEntries(drawEntries = drawEntries)
```

`CanvasToppingImage` 안의 painter 접근 두 자리를 바꾼다 — `entry.painter.state` → `entry.draw.painter.state`, `painter = entry.painter` → `painter = entry.draw.painter`.

**`ToppingCornerButtons`는 손대지 않는다.** 그 함수는 `entry.target`과 `entry.topping`만 읽고 `painter`를 읽지 않으며, Step 3의 `topping` 위임 덕에 그대로 컴파일된다.

- [x] **Step 6: 컴파일과 ktlint를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:ktlintMainSourceSetCheck`
Expected: 둘 다 BUILD SUCCESSFUL. ktlint가 실패하면 `./gradlew :feature:groups:canvas:impl:ktlintFormat`으로 고친 뒤 diff를 확인한다.

- [x] **Step 7: 기존 테스트가 그대로 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test`
Expected: PASS (화면만 바꿨으므로 ViewModel 테스트는 영향 없음)

- [x] **Step 8: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt
git commit -m "refactor: 배경 편집의 토핑 그리기 정보를 판정 정보에서 떼어낸다"
```

---

### Task 2: `CanvasToppingImage`가 불투명도와 클릭 없음을 받게 한다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt`

**Interfaces:**
- Consumes: Task 1의 `BGEditDrawEntry`
- Produces: `@Composable private fun CanvasToppingImage(entry: BGEditDrawEntry, alpha: Float, onClick: (() -> Unit)?, modifier: Modifier = Modifier)`

- [x] **Step 1: 불투명도 상수를 둔다**

이 파일에는 최상위 상수가 하나도 없다. **import 블록 바로 아래, `@Composable internal fun CanvasBGEditScreen` 선언 위에** 새로 둔다.

```kotlin
/** 배경 탭에서 토핑은 배경 선택의 참고로만 존재한다 — 고를 수 없다는 것을 불투명도로 알린다 */
private const val BACKGROUND_TAB_TOPPING_ALPHA = 0.5f
```

- [x] **Step 2: `CanvasToppingImage`를 고친다**

중심점·크기는 `BGEditDrawEntry`에 이미 있으므로 다시 계산하지 않는다. `alpha`에 기본값을 주지 않는다 — Compose 관례상 `modifier`가 첫 선택 파라미터여야 하고, 호출부 셋이 모두 값을 명시하므로 기본값이 필요 없다(스펙 「불투명도」는 `= 1f`로 적었으나 의도적으로 다르게 간다).

```kotlin
/**
 * 캔버스 미리보기 박스 안, 저장된 배치([CanvasToppingItem.positionX]/[positionY])대로 겹쳐 그리는
 * 이미지. 캔버스 메인([CanvasToppingLayer])과 같은 규칙을 써야 편집한 그대로 돌아간다.
 *
 * 선택 시 보이는 스트로크·버튼은 이 이미지와 함께 돌지 않아야 해서 [ToppingCornerButtons]에서
 * 별도로 그린다.
 *
 * @param onClick null 이면 접근성 클릭도 붙지 않는다 — 실제로 누를 수 없는 화면에서 버튼으로
 *   읽히면 안 된다.
 */
@Composable
private fun CanvasToppingImage(
    entry: BGEditDrawEntry,
    alpha: Float,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val painterState by entry.painter.state.collectAsState()
    val border = entry.topping.borderLayers.firstOrNull()
    val description = stringResource(R.string.canvas_topping_content_description)

    Box(
        modifier = modifier
            .centeredAt(entry.center)
            .requiredSize(entry.size)
            .graphicsLayer(
                rotationZ = entry.topping.rotationDegrees,
                alpha = alpha,
            ).let { base ->
                if (onClick == null) {
                    base
                } else {
                    // 판정은 입력 레이어가 하지만, 접근성 서비스에는 토핑이 개별 버튼으로 보여야 한다
                    base.semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = description
                        onClick {
                            onClick()
                            true
                        }
                    }
                }
            },
    ) {
        YGToppingCutoutImage(
            painter = entry.painter,
            // 로딩·실패 상태에서 찍으면 플레이스홀더 실루엣이 테두리로 보인다
            borderColor = border
                ?.let { Color(it.colorArgb) }
                ?.takeIf { painterState is AsyncImagePainter.State.Success },
            borderWidth = (border?.widthDp ?: 0f).dp,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

`semantics` 블록 안쪽의 `onClick`은 `SemanticsPropertyReceiver.onClick(label, action)`이고 바깥은 값 파라미터다. 인자 없는 `onClick()` 호출은 파라미터의 `invoke`로 해석되며, 값 파라미터라 `else` 가지에서 스마트캐스트가 유지된다. **현재 코드가 이미 같은 섀도잉을 하고 있고 컴파일된다** — `onClick.invoke()`로 바꿀 필요는 없다.

`Modifier.let { … }`으로 조건부 수정자를 붙이는 방식은 같은 파일의 캔버스 박스에 선례가 있다.

- [x] **Step 3: 토핑 탭 호출부 둘을 새 시그니처에 맞춘다**

```kotlin
entries.filterNot { it.topping.isMine }.forEach { entry ->
    CanvasToppingImage(
        entry = entry.draw,
        alpha = 1f,
        onClick = onClickDeselectTopping,
    )
}
```

```kotlin
myEntries.forEach { entry ->
    CanvasToppingImage(
        entry = entry.draw,
        alpha = 1f,
        onClick = { onClickTopping(entry.topping) },
    )
}
```

- [x] **Step 4: 컴파일과 ktlint 확인**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:ktlintMainSourceSetCheck`
Expected: 둘 다 BUILD SUCCESSFUL

- [x] **Step 5: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt
git commit -m "feat: 토핑 이미지가 불투명도와 클릭 없음을 받게 한다"
```

---

### Task 3: 배경 탭에서 토핑을 그린다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt`

**Interfaces:**
- Consumes: Task 1의 두 `remember*` 함수, Task 2의 `CanvasToppingImage`
- Produces: 없음(화면 내부)

- [x] **Step 1: 캔버스 박스 안의 토핑 레이어를 다시 짠다**

지금은 `if (uiState.selectedTab == CanvasEditTab.TOPPING) { BoxWithConstraints { … } }` 한 덩어리다. `BoxWithConstraints`는 탭과 무관하게 항상 들어가고, 그 **안에서** 탭이 갈린다.

기존 `// 남의 토핑` / `// 내 토핑` 주석은 옮기지 않는다 — 바로 아래 `filterNot { it.topping.isMine }`이 그대로 말한다.

```kotlin
// 배치가 모두 이 영역 대비 비율이라, 캔버스 메인과 같은 자리에 그리려면 실제 크기를 알아야 한다
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val canvasWidth = maxWidth
    val canvasHeight = maxHeight
    val density = LocalDensity.current
    val canvasWidthPx = with(density) { canvasWidth.toPx() }
    val canvasHeightPx = with(density) { canvasHeight.toPx() }

    val drawEntries = rememberBGEditDrawEntries(
        toppings = uiState.toppings,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
    )

    if (uiState.selectedTab == CanvasEditTab.BACKGROUND) {
        // 배경을 고르는 화면이라 토핑은 참고로만 둔다 — 딤·입력 레이어·모서리 버튼·접근성
        // 클릭을 붙이지 않고, 저장된 z 순서 그대로 겹쳐 그린다
        drawEntries.forEach { entry ->
            CanvasToppingImage(
                entry = entry,
                alpha = BACKGROUND_TAB_TOPPING_ALPHA,
                onClick = null,
            )
        }
    } else {
        val entries = rememberBGEditHitEntries(drawEntries = drawEntries)
        val myEntries = entries.filter { it.topping.isMine }
        val selectedEntry = myEntries.firstOrNull {
            it.topping.parfaitImageId == uiState.selectedToppingId
        }

        entries.filterNot { it.topping.isMine }.forEach { entry ->
            CanvasToppingImage(
                entry = entry.draw,
                alpha = 1f,
                onClick = onClickDeselectTopping,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(YGAtomicColors.Transparency.Black25),
        )

        myEntries.forEach { entry ->
            CanvasToppingImage(
                entry = entry.draw,
                alpha = 1f,
                onClick = { onClickTopping(entry.topping) },
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .toppingTapInput(
                    entries = { myEntries.map { it.topping to it.target } },
                    keyOf = { it.parfaitImageId },
                    onHit = onClickTopping,
                    onMiss = onClickDeselectTopping,
                ).toppingDragInput(
                    targetAt = { selectedEntry?.target },
                    onDrag = { amount ->
                        onToppingMoveDrag(
                            amount.x / canvasWidthPx,
                            amount.y / canvasHeightPx,
                        )
                    },
                ),
        )

        selectedEntry?.let { entry ->
            ToppingCornerButtons(
                entry = entry,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                onClickDelete = onClickDeleteTopping,
                onClickEdit = onClickEditTopping,
                onResizeDrag = onToppingResizeDrag,
                onRotateDrag = onToppingRotateDrag,
            )
        }
    }
}
```

`uiState.toppings`는 `CanvasBGEditViewModel`이 `positionZ` 오름차순으로 채운다. 배경 탭이 그 순서를 그대로 쓰는 것은 캔버스 메인과 같은 규칙이다.

- [x] **Step 2: 컴파일과 ktlint 확인**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:ktlintMainSourceSetCheck`
Expected: 둘 다 BUILD SUCCESSFUL

- [x] **Step 3: Android Lint 확인 — 판정 기준이 다르다**

Run: `./gradlew :feature:groups:canvas:impl:lintDebug`
Expected: **BUILD FAILED가 정상이다.** 이 모듈의 lint는 기준선에서 이미 `CanvasToppingPlaceRoute.kt`의 `LocalContextGetResourceValueCall` 4건으로 실패한다. 판정 기준은 **`CanvasBGEditScreen.kt`에 새 지적이 없을 것**이다. `feature/groups/canvas/impl/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`를 열어 파일명으로 걸러 확인한다. **기존 4건은 손대지 않는다.**

- [x] **Step 4: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt
git commit -m "feat: 배경 탭에서도 토핑을 반투명으로 그린다"
```

---

### Task 4: 두 탭의 Preview를 나눈다

이 모듈에는 계측 테스트가 없다. Preview가 유일한 시각 검증 수단이므로 두 탭을 각각 볼 수 있어야 한다.

이 파일의 기존 Preview는 **`PreviewCanvasBGEditScreen` 하나**이고, `CanvasBGEditUiState()`(기본값 = 배경 탭, 토핑 없음)에 `modifier = Modifier.fillMaxSize()`를 넘긴다. 새로 둘을 더하면 같은 화면 Preview가 셋이 되므로, **기존 것을 배경 탭용으로 개명하고 복제한다.**

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt`

**Interfaces:**
- Consumes: `CanvasBGEditUiState`, `CanvasToppingItem`, `ToppingBorderLayer`, `YGPreview`, `PreviewBox`
- Produces: 없음

- [x] **Step 1: Preview용 토핑 표본을 만든다**

`imageUrl`에 빈 문자열을 두지 않는다. 실제 리소스를 넘겨야 painter가 `Success`에 닿고 테두리도 함께 그려져, 불투명도가 이미지와 테두리에 한 번에 적용되는지(스펙 「불투명도」)를 눈으로 판정할 수 있다. 이 모듈이나 `:core:designsystem`의 기존 drawable 하나를 고른다.

```kotlin
private const val PREVIEW_TOPPING_MODEL = "android.resource://com.teamyg.parfait.feature.groups.canvas.impl/drawable/nukkiii"

private val previewToppings = listOf(
    CanvasToppingItem(
        parfaitImageId = 1L,
        isMine = true,
        imageUrl = PREVIEW_TOPPING_MODEL,
        positionX = 0.3f,
        positionY = 0.4f,
        borderLayers = listOf(ToppingBorderLayer(colorArgb = 0xFFFFFFFF.toInt(), widthDp = 4f)),
    ),
    CanvasToppingItem(
        parfaitImageId = 2L,
        isMine = false,
        imageUrl = PREVIEW_TOPPING_MODEL,
        positionX = 0.7f,
        positionY = 0.6f,
        scale = 1.2f,
        rotationDegrees = 15f,
    ),
)
```

`ToppingBorderLayer`의 실제 생성자 파라미터 이름·타입을 `feature/segmentation/api`에서 확인해 맞춘다. drawable 이름과 패키지도 실제 리소스로 맞춘다.

- [x] **Step 2: 기존 Preview를 개명하고 복제한다**

`PreviewCanvasBGEditScreen`을 **`PreviewCanvasBGEditScreenBackgroundTab`**으로 바꾸고 `uiState`에 탭과 토핑을 넣는다. `modifier = Modifier.fillMaxSize()`는 기존대로 유지한다 — 두 Preview에서 캔버스 박스 크기가 같아야 "두 탭에서 상대 위치가 같다"를 판정할 수 있다.

```kotlin
@YGPreview
@Composable
private fun PreviewCanvasBGEditScreenBackgroundTab() {
    PreviewBox {
        CanvasBGEditScreen(
            uiState = CanvasBGEditUiState(
                selectedTab = CanvasEditTab.BACKGROUND,
                toppings = previewToppings,
            ),
            onSelectTab = {},
            onSelectColor = {},
            onClickCamera = {},
            onClickGallery = {},
            onClickCloseButton = {},
            onQuitDialogConfirm = {},
            onQuitDialogCancel = {},
            onClickConfirm = {},
            onClickTopping = {},
            onClickDeselectTopping = {},
            onClickDeleteTopping = {},
            onDeleteToppingDialogConfirm = {},
            onDeleteToppingDialogCancel = {},
            onClickEditTopping = {},
            onToppingResizeDrag = {},
            onToppingRotateDrag = {},
            onToppingMoveDrag = { _, _ -> },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

🔁 **위 두 코드 블록의 콜백 이름은 2026-08-27(PR #397) 이전 것이다.** 그 라운드가 크기조절·회전
환산을 화면으로 올리면서 `onToppingResizeDrag`·`onToppingRotateDrag` 가
**`onToppingResize: (Float) -> Unit`·`onToppingRotate: (Float) -> Unit`** 으로 바뀌었고,
`ToppingCornerButtons` 쪽 인자도 `onResize`·`onRotate` 다. 2026-08-28 스택 리베이스에서 실제 코드는
새 이름으로 맞췄다 — 이 계획을 다시 읽을 때는 이름만 바꿔 읽으면 되고, 프리뷰를 두 탭으로 나누는
이 Task 의 취지는 그대로다.

그것을 복제해 `PreviewCanvasBGEditScreenToppingTab`을 만들고 `uiState`만 바꾼다.

```kotlin
            uiState = CanvasBGEditUiState(
                selectedTab = CanvasEditTab.TOPPING,
                toppings = previewToppings,
                selectedToppingId = 1L,
            ),
```

콜백 17개는 위 목록이 실제 시그니처와 이름·개수 모두 일치한다.

- [x] **Step 3: 컴파일·ktlint·테스트 확인**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:ktlintCheck :feature:groups:canvas:impl:test`
Expected: 셋 다 BUILD SUCCESSFUL. `ktlintCheck`는 테스트 소스셋까지 본다.

- [x] **Step 4: Android Lint — Task 3 Step 3과 같은 기준**

Run: `./gradlew :feature:groups:canvas:impl:lintDebug`
Expected: 기존 4건 외에 `CanvasBGEditScreen.kt` 지적이 없을 것.

- [x] **Step 5: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt
git commit -m "test: 배경 편집 두 탭의 Preview 를 나눈다"
```

---

## 수동 확인 (구현자가 직접)

Preview에서 다음을 눈으로 확인한다.

- [ ] 배경 탭에서 토핑 두 개가 모두 보이고 반투명이다
- [ ] 배경 탭에서 토핑 이미지와 그 테두리가 **함께** 반투명하다(겹치는 자리만 진해지지 않는다)
- [ ] 배경 탭에서 딤 오버레이가 없다(배경색·배경 이미지가 그대로 보인다)
- [ ] 배경 탭에서 선택 스트로크와 모서리 버튼이 없다
- [ ] 토핑 탭으로 옮기면 기존과 같다 — 남의 토핑 위에 딤, 그 위에 내 토핑, 선택된 것에 모서리 버튼
- [ ] 두 탭에서 토핑의 상대 위치가 같다

## 검증 못 한 것

- 배경 탭에서 토핑을 눌러도 아무 반응이 없는지(실기기)
- TalkBack이 배경 탭의 토핑을 버튼으로 읽지 않는지(실기기)
- **로딩·실패 상태의 토핑이 배경 탭에서 어떻게 보이는지.** `PreviewBox`가 Coil의 프리뷰 핸들러에 항상 성공 비트맵을 주입하므로, Preview에서는 그 분기를 볼 수 없다.

---

## Self-Review 결과

**스펙 커버리지** — 「PR1」의 요구 다섯을 각각 짚는다. 게이트 분리(Task 3), 붙이지 않는 것 넷(Task 2·3), 마스크 호출 배제(Task 1), 불투명도(Task 2), 두 탭 Preview(Task 4). 「배치·크기」 절은 코드 변경을 요구하지 않는다.

**타입 일관성** — `BGEditDrawEntry`는 Task 1에서 정의하고 Task 2·3에서 그대로 쓴다. `CanvasToppingImage`의 파라미터 이름(`entry`·`alpha`·`onClick`)은 Task 2에서 정하고 Task 3의 세 호출부가 같은 이름을 쓴다. `BACKGROUND_TAB_TOPPING_ALPHA`는 Task 2에서 정의하고 Task 3에서 쓴다.

**스펙과의 의도적 이탈 1건** — `alpha`에 기본값 `1f`를 주지 않는다(스펙은 준다). Compose 관례상 `modifier`가 첫 선택 파라미터여야 하고 호출부 셋이 모두 값을 명시하므로 필요 없다.

**확인 필요한 것 둘** — `ToppingBorderLayer`의 생성자 파라미터와 Preview에 쓸 drawable 이름은 Task 4 Step 1에서 실제 정의로 맞춘다. 나머지 전제(반환 타입·import·기존 Preview 이름·콜백 목록·문자열 리소스·모듈 경로)는 전부 코드로 확인됐다.
