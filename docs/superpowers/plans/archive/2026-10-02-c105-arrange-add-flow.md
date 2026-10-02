---
id: c105-arrange-add-flow
title: 배치 화면 테두리 패널 — 추가 플로우 (계획 A)
status: done
type: work-order
created: 2026-10-02
updated: 2026-10-02
platforms: android
owner: android
related_adr: ADR-0025, ADR-0026
related_spec: c105-arrange-border-merge
related_code:
  - CanvasToppingPlaceViewModel.kt#CanvasToppingPlaceViewModel
  - CanvasToppingPlaceScreen.kt#CanvasToppingPlaceScreen
  - ToppingEditScreen.kt#ToppingEditScreen
  - SegmentationConfirmViewModel.kt#SegmentationConfirmViewModel
  - ToppingDraft.kt#ToppingDraft
archived_reason: 구현 완료(2026-10-02, 7 Task 수행 — 피그마·실기기 대조만 돌리지 않았다, OQ-P-412). develop 머지 전.
tags: [plan, parfait]
---

# 배치 화면 테두리 패널 — 추가 플로우 Implementation Plan

> **Archived (2026-10-02)** — 구현 완료(2026-10-02, 7 Task 수행 — 피그마·실기기 대조만 돌리지 않았다, OQ-P-412). develop 머지 전. 본문은 작성 시점의 계획이고 식별자·문구는 그때 이름이다.
> 현재 상태는 [status.md](../../../status.md)를 본다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 새 토핑을 쌓는 배치 화면에서 테두리를 하단 패널로 고르게 하고, 편집 화면의 테두리 탭을 이 흐름에서 걷어 낸다.

**Architecture:** 패널·화면 뼈대·패널 열림 위치 계산을 `feature/groups/canvas/impl`의 공용 부품으로 만든다. `CanvasToppingPlaceViewModel`은 테두리를 초안에서 읽지 않고 패널 intent로 받는다. 패널이 열린 동안 토핑은 그릴 때만 가운데로 옮기고 상태의 위치값은 건드리지 않는다. 캔버스 입력 레이어는 패널 상태에 따라 갈아 끼우지 않는다 — 닫기·탭·변형 세 입력을 늘 붙여 두고, 각자가 제스처의 첫 down에서 패널 상태를 읽어 그 제스처 전체를 받을지 버릴지 정한다. 초안의 테두리 필드는 domain·data까지 삭제한다. 편집 화면의 테두리 탭 코드는 지우지 않고 숨기기만 한다 — C-301이 계획 B가 끝날 때까지 쓴다.

**Tech Stack:** Kotlin, Jetpack Compose(`pointerInput`, `animateFloatAsState`, `BackHandler`), Hilt, JUnit4 + MockK + Turbine + coroutines-test, Compose UI 계측 테스트(`androidx.compose.ui.test.junit4.v2.createComposeRule`).

**Spec:** [`docs/superpowers/specs/2026-10-02-c105-arrange-border-merge-design.md`](../../specs/archive/2026-10-02-c105-arrange-border-merge-design.md)

## Global Constraints

- 코드 주석·KDoc은 [`docs/code-conventions.md`](../../../code-conventions.md)를 따른다. 구현·리뷰 서브에이전트 브리프에 이 링크를 넣는다.
- 경로 약어
  - `C` = `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl`
  - `CT` = 같은 모듈 `src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl`
  - `CA` = 같은 모듈 `src/androidTest/kotlin/com/teamyg/parfait/feature/groups/canvas/impl`
  - `S` = `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl`
  - `ST` = 같은 모듈 `src/test/java/com/teamyg/parfait/feature/segmentation/impl`
  - `SAPI` = `feature/segmentation/api/src/main/java/com/teamyg/parfait/feature/segmentation/api`
  - `DS` = `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem`
- 검증 명령: 단위 `./gradlew :feature:groups:canvas:impl:testDebugUnitTest`, 계측 `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest`, 스타일 `./gradlew ktlintCheck`. `domain`은 JVM 모듈이라 `./gradlew :domain:test`다.
- 모든 Task는 끝났을 때 전체 빌드가 통과해야 한다.
- 이 계획만 머지된 상태에서 C-301의 테두리 재편집 왕복(`NavKeyToppingEdit(borderOnly = true)` → `TOPPING_EDIT_RESULT_KEY`)이 그대로 돌아야 한다.
- `feature/segmentation/impl`의 `BorderColorChipRow.kt`, `ToppingBorderColors.kt`, `ToppingBorderEditScreen.kt`, `ToppingBorderPreviewLayout.kt`, `ToppingBorderOutline.kt`, `NavKeyToppingEdit`의 `borderLayers`·`borderOnly`, `ToppingBorderLayer`는 **지우지 않는다**. 계획 B의 몫이다.
- `UiState`가 public이므로 그 안에 실리는 `ToppingBorderStyle`도 public이다. 이 저장소에는 internal `UiState` 선례가 없다.
- 테두리 굵기 범위는 `ToppingBorder.WIDTH_RANGE_DP`. 기본 굵기는 `10f`.
- 테두리 팔레트는 9색, 맨 앞이 "없음"이다. 순서와 값은 `S/editor/ToppingBorderColors.kt`와 같다.
- 패널이 열린 동안 포커스 토핑의 세로 위치는 캔버스 중앙에서 `18.5.dp` 위다. 크기와 각도는 바꾸지 않는다.
- 로딩 덮개가 떠 있는 동안(`isLoading`) 시스템 뒤로가기는 무시한다.
- 문구
  - 화면 제목: "배치"
  - 접힌 패널: "테두리 설정" / 펼친 패널: "테두리 굵기"
  - 하단 버튼: "캔버스에 쌓기"
  - 그만두기 팝업: 제목 "사진 편집을 그만둘까요?", 본문 "지금까지 진행한 내용은 저장되지 않아요.\n정말 그만두시겠어요?", 확인 "그만두기", 취소 "계속 편집"
  - 접근성 설명: "테두리 설정 열기", "테두리 설정 닫기"
- 분석 화면 id는 바꾸지 않는다(`NavKeyCanvasToppingPlace` → `"C-106"`).
- `docs/status.md`·`open-questions.md`·ADR은 마지막 Task에서만 고친다.

## Review Focus

1. 패널이 열린 채 "캔버스에 쌓기"를 누른다 — 서버에 가는 위치가 가운데 임시 위치가 아니라 원래 위치여야 한다. (Task 4 단위 테스트)
2. 테두리 색을 고르지 않은 채 굵기만 바꾸고 확정한다 — `ToppingBorder.None`이 가야 한다. 굵기만 실린 `Solid`가 가면 서버가 400을 낸다. (Task 4 단위 테스트)
3. 패널을 닫은 그 터치가 드래그로 이어지거나 두 번째 손가락이 닿는다 — 패널만 닫히고 토핑이 움직이지 않아야 한다. (Task 3 계측 테스트 — 닫힘으로 상태가 뒤집힌 뒤의 입력까지 넣는다)
4. 패널의 빈 곳을 누른다 — 패널이 닫히지 않아야 한다. (Task 3 계측 테스트)
5. 옛 버전이 DataStore에 남긴 테두리 필드가 든 초안을 새 버전이 읽는다 — 디코드가 깨지지 않고 나머지 필드가 살아야 한다. (Task 5 테스트)

---

### Task 1: 슬라이더를 디자인시스템으로 옮긴다

**Files:**
- Create: `DS/component/ygslider/YGSlider.kt`
- Delete: `S/component/BrushWidthSlider.kt`
- Modify: `S/screen/ToppingEditScreen.kt` (import와 호출부 두 곳)

**Interfaces:**
- Produces:
  ```kotlin
  @Composable
  fun YGSlider(
      value: Float,
      onValueChange: (Float) -> Unit,
      valueRange: ClosedFloatingPointRange<Float>,
      modifier: Modifier = Modifier,
      isEnabled: Boolean = true,
      onValueChangeFinished: () -> Unit = {},
  )
  ```

- [ ] **Step 1: `BrushWidthSlider.kt`의 내용을 `YGSlider.kt`로 옮긴다**

패키지는 `com.teamyg.parfait.core.designsystem.component.ygslider`, 함수 이름은 `YGSlider`, 가시성은 public. 트랙·thumb·프리뷰는 그대로 둔다. KDoc 첫 줄의 "브러시 굵기를 조절하는"은 용도를 가리지 않는 문장으로 고친다.

- [ ] **Step 2: `ToppingEditScreen.kt`의 `BrushWidthSlider` 호출 두 곳을 `YGSlider`로 바꾸고 원본 파일을 지운다**

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew :core:designsystem:compileDebugKotlin :feature:segmentation:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add -A core/designsystem feature/segmentation
git commit -m "refactor: move brush width slider to designsystem as YGSlider"
```

---

### Task 2: 테두리 값·팔레트·패널 열림 위치

**Files:**
- Create: `C/util/ToppingBorderStyle.kt`
- Create: `C/util/ToppingBorderColors.kt`
- Create: `C/util/ToppingPanelFocus.kt`
- Test: `CT/util/ToppingPanelFocusTest.kt`
- Test: `CT/util/ToppingBorderStyleTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  // ToppingBorderStyle.kt
  data class ToppingBorderStyle(val colorArgb: Int, val widthDp: Float)
  internal const val DEFAULT_TOPPING_BORDER_WIDTH_DP = 10f
  internal val TOPPING_BORDER_WIDTH_RANGE_DP: ClosedFloatingPointRange<Float>
  internal fun ToppingBorderStyle?.toToppingBorder(): ToppingBorder
  internal fun ToppingBorder.toToppingBorderStyleOrNull(): ToppingBorderStyle?

  // ToppingBorderColors.kt
  internal val TOPPING_BORDER_COLORS: List<Color>

  // ToppingPanelFocus.kt
  internal val PANEL_FOCUS_LIFT: Dp   // 18.5.dp
  internal fun panelFocusCenter(canvasSize: DpSize): DpOffset
  ```

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
// ToppingPanelFocusTest
@Test
fun panelFocusCenter_isHorizontalCenterLiftedAboveVerticalCenter() {
    val center = panelFocusCenter(DpSize(335.dp, 596.dp))
    assertEquals(167.5.dp, center.x)
    assertEquals(279.5.dp, center.y)
}

// ToppingBorderStyleTest
@Test
fun toToppingBorder_null_isNone() =
    assertEquals(ToppingBorder.None, (null as ToppingBorderStyle?).toToppingBorder())

@Test
fun toToppingBorder_style_isSolidWithRgbHex() =
    assertEquals(
        ToppingBorder.Solid(color = "#FCC2CC", width = 12.0),
        ToppingBorderStyle(colorArgb = 0xFFFCC2CC.toInt(), widthDp = 12f).toToppingBorder(),
    )

@Test
fun toToppingBorderStyleOrNull_unreadableColor_isNull() =
    assertNull(ToppingBorder.Solid(color = "not-a-color", width = 4.0).toToppingBorderStyleOrNull())

@Test
fun widthRange_matchesDomainRange() {
    assertEquals(2f, TOPPING_BORDER_WIDTH_RANGE_DP.start)
    assertEquals(30f, TOPPING_BORDER_WIDTH_RANGE_DP.endInclusive)
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*ToppingPanelFocusTest' --tests '*ToppingBorderStyleTest'`
Expected: FAIL — 미해결 참조

- [ ] **Step 3: 세 파일을 구현한다**

- `TOPPING_BORDER_WIDTH_RANGE_DP`는 `ToppingBorder.WIDTH_RANGE_DP`의 양 끝을 `Float`로 바꾼 것이다. 숫자를 다시 적지 않는다.
- 변환은 `CanvasToppingPlaceViewModel.toToppingBorder`와 `CanvasBGEditViewModel.toBorderLayers`가 쓰는 것(`toRgbHexString`, `toColorOrNull`)을 그대로 쓴다.
- `TOPPING_BORDER_COLORS`는 `S/editor/ToppingBorderColors.kt`의 목록을 복제한다. `DEFAULT_TOPPING_BORDER_COLOR`는 복제하지 않는다 — canvas 쪽은 "없음"을 null로 표현한다.

- [ ] **Step 4: 통과 확인**

Run: Step 2와 같은 명령
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add feature/groups/canvas/impl
git commit -m "feat: add topping border style, palette and panel focus center"
```

---

### Task 3: 패널·화면 뼈대·헤더·입력 게이트

**Files:**
- Create: `C/component/BorderColorChipRow.kt`
- Create: `C/component/ToppingBorderPanel.kt`
- Create: `C/component/ToppingPanelDismissInput.kt`
- Create: `C/component/ToppingArrangeLayout.kt`
- Modify: `C/component/ToppingHitTestInput.kt`, `C/component/ToppingTransformInput.kt`
- Modify: `DS/component/ygfloatingbar/YGFloatingBar.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`
- Test: `CA/component/ToppingBorderPanelTest.kt`
- Test: `CA/component/ToppingPanelDismissInputTest.kt`

**Interfaces:**
- Consumes: Task 1 `YGSlider`, Task 2 `TOPPING_BORDER_COLORS`
- Produces:
  ```kotlin
  @Composable
  internal fun BorderColorChipRow(
      selectedColorArgb: Int?,
      onSelectColor: (Int?) -> Unit,
      modifier: Modifier = Modifier,
      contentPadding: PaddingValues = PaddingValues(),
  )

  @Composable
  internal fun ToppingBorderPanel(
      isOpen: Boolean,
      selectedColorArgb: Int?,
      widthDp: Float,
      widthRange: ClosedFloatingPointRange<Float>,
      onClickToggle: () -> Unit,
      onSelectColor: (Int?) -> Unit,
      onChangeWidth: (Float) -> Unit,
      modifier: Modifier = Modifier,
      isEnabled: Boolean = true,
  )

  /**
   * 제스처의 첫 down 때 [isPanelOpen] 이 참이면 [onDismiss] 를 부른다.
   * 닫혀 있으면 아무것도 하지 않는다.
   */
  @Composable
  internal fun Modifier.dismissPanelOnTouch(
      isPanelOpen: () -> Boolean,
      onDismiss: () -> Unit,
  ): Modifier

  // 기존 두 입력에 인자를 하나씩 더한다. 기본값이라 기존 호출부는 그대로 선다
  @Composable
  internal fun <T> Modifier.toppingTapInput(
      entries: () -> List<Pair<T, ToppingHitTarget>>,
      keyOf: (T) -> Any,
      onHit: (T) -> Unit,
      onMiss: () -> Unit,
      enabled: () -> Boolean = { true },
  ): Modifier

  @Composable
  internal fun Modifier.toppingTransformInput(
      targetAt: () -> ToppingHitTarget?,
      onTransform: (pan: Offset, zoom: Float, rotationDelta: Float) -> Unit,
      onGestureActiveChange: (Boolean) -> Unit = {},
      enabled: () -> Boolean = { true },
  ): Modifier

  @Composable
  internal fun ToppingArrangeLayout(
      header: @Composable () -> Unit,
      onClickConfirm: () -> Unit,
      panel: @Composable BoxScope.() -> Unit,
      modifier: Modifier = Modifier,
      canvas: @Composable BoxScope.() -> Unit,
  )

  // YGFloatingBar.kt
  @Composable
  fun YGFloatingBarBackTitleClose(
      title: String,
      onBackClick: () -> Unit,
      onCloseClick: () -> Unit,
      modifier: Modifier = Modifier,
  )
  ```
- 테스트 태그
  - `"topping_border_panel"` — 패널 루트
  - `"topping_border_panel_toggle"` — 닫힘 상태의 바, 열림 상태의 아래 화살표. 패널 루트와 **다른 노드**다(닫힘 상태에서도 루트 안쪽의 자식 노드가 클릭을 든다)
  - `"topping_border_slider"`
  - `"topping_border_chip_none"`, `"topping_border_chip_<ARGB 8자리 대문자 16진>"` — 예: `topping_border_chip_FF0E0E0E`

- [ ] **Step 1: 문자열을 더한다**

```xml
<string name="canvas_topping_border_panel_collapsed">테두리 설정</string>
<string name="canvas_topping_border_panel_width">테두리 굵기</string>
<string name="canvas_topping_border_panel_open">테두리 설정 열기</string>
<string name="canvas_topping_border_panel_close">테두리 설정 닫기</string>
<string name="canvas_topping_arrange_confirm">캔버스에 쌓기</string>
```

`canvas_topping_place_title`의 값을 `토핑배치`에서 `배치`로 바꾼다.

- [ ] **Step 2: 실패하는 계측 테스트를 쓴다**

```kotlin
// ToppingBorderPanelTest — 각 테스트는 YGTheme 안에서 ToppingBorderPanel 을 띄운다
@Test
fun collapsed_showsLabelAndTogglesOnBarTap() {
    // isOpen = false
    onNodeWithText("테두리 설정").assertIsDisplayed()
    onNodeWithTag("topping_border_slider").assertDoesNotExist()
    onNodeWithTag("topping_border_panel_toggle").performClick()
    assertEquals(1, toggleCount)
}

@Test
fun collapsed_disabled_doesNotToggle() {
    // isOpen = false, isEnabled = false
    onNodeWithTag("topping_border_panel_toggle").performClick()
    assertEquals(0, toggleCount)
}

@Test
fun open_showsSliderAndNineChips_andTogglesOnArrow() {
    // isOpen = true
    onNodeWithText("테두리 굵기").assertIsDisplayed()
    onNodeWithTag("topping_border_slider").assertIsDisplayed()
    // 태그가 "topping_border_chip_" 로 시작하는 노드를 고르는 SemanticsMatcher 를 테스트 파일에 둔다.
    // LazyRow 라 칩 아홉 개가 한 번에 놓이도록 패널 폭을 넉넉히(예: 480.dp) 준다
    onAllNodes(hasTestTagPrefix("topping_border_chip_")).assertCountEquals(9)
    onNodeWithContentDescription("테두리 설정 닫기").performClick()
    assertEquals(1, toggleCount)
}

@Test
fun open_selectingNoneChip_emitsNull() {
    // isOpen = true, selectedColorArgb = 0xFF0E0E0E.toInt()
    onNodeWithTag("topping_border_chip_none").performClick()
    assertEquals(listOf<Int?>(null), selected)
}

@Test
fun open_tapOnBlankArea_isConsumedByPanel() {
    // Box { Box(fillMaxSize, pointerInput 으로 down 을 세는 아래 레이어) ; ToppingBorderPanel(isOpen = true, Bottom) }
    onNodeWithTag("topping_border_panel").performTouchInput { click(topLeft + Offset(4f, 4f)) }
    assertEquals(0, lowerLayerDownCount)
}

// ToppingPanelDismissInputTest
// 테스트 화면: var isOpen by mutableStateOf(true)
//   Box(Modifier
//       .dismissPanelOnTouch(isPanelOpen = { isOpen }, onDismiss = { dismissCount++; isOpen = false })
//       .toppingTapInput(entries = { listOf(Unit to target) }, keyOf = { it }, onHit = { hitCount++ },
//                        onMiss = { missCount++ }, enabled = { !isOpen })
//       .toppingTransformInput(targetAt = { target }, onTransform = { _, _, _ -> transformCount++ },
//                              enabled = { !isOpen }))
// target 은 화면 전체를 덮는 ToppingHitTarget 이다.
@Test
fun panelOpen_tap_dismissesAndIsNotATap() {
    onRoot().performTouchInput { click(center) }
    assertEquals(1, dismissCount); assertEquals(0, hitCount); assertEquals(0, missCount)
}

@Test
fun panelOpen_dragAfterDismiss_doesNotTransform() {
    onRoot().performTouchInput { down(center) }
    waitForIdle()                       // isOpen == false 로 리컴포지션이 끝난다
    onRoot().performTouchInput { moveBy(Offset(120f, 0f)); up() }
    assertEquals(1, dismissCount); assertEquals(0, transformCount)
}

@Test
fun panelOpen_secondFingerAfterDismiss_doesNotTransform() {
    onRoot().performTouchInput { down(0, center) }
    waitForIdle()
    onRoot().performTouchInput {
        down(1, center + Offset(200f, 0f))
        moveBy(0, Offset(-60f, 0f)); moveBy(1, Offset(60f, 0f))
        up(0); up(1)
    }
    assertEquals(1, dismissCount); assertEquals(0, transformCount)
}

@Test
fun panelClosed_gestureWorksAsBefore() {
    // isOpen = false 로 시작
    onRoot().performTouchInput { down(center); moveBy(Offset(120f, 0f)); up() }
    assertEquals(0, dismissCount); assertTrue(transformCount > 0)
}

@Test
fun afterDismissGestureEnds_nextGestureIsNormal() {
    onRoot().performTouchInput { click(center) }     // 닫힌다
    waitForIdle()
    onRoot().performTouchInput { click(center) }     // 이번엔 탭이다
    assertEquals(1, dismissCount); assertEquals(1, hitCount)
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest`
Expected: 새 테스트가 FAIL — 미해결 참조. 기존 `ToppingTransformInputTest`는 그대로다.

- [ ] **Step 4: `BorderColorChipRow`를 구현한다**

`S/component/BorderColorChipRow.kt`를 복제하고 시그니처를 위 형태로 바꾼다. 색 비교는 `Color.toArgb()`로 한다. 목록 첫 항목(`Color.Transparent`)은 `selectedColorArgb == null`일 때 켜지고, 누르면 `onSelectColor(null)`이다. 칩마다 위 테스트 태그를 단다.

- [ ] **Step 5: `ToppingBorderPanel`을 구현한다**

| 상태 | 배경 | 안쪽 여백 | 내용 |
|---|---|---|---|
| 닫힘 | `YGAtomicColors.Transparency.White75` | `padding6` 사방 | "테두리 설정" + 오른쪽 `ic_caret_top` |
| 열림 | `YGAtomicColors.Gray.White` | 가로 `padding6`, 세로 `padding7` | "테두리 굵기", 4dp 아래 `YGSlider`, `gap3` 아래 `BorderColorChipRow`, 오른쪽 위 `ic_caret_bottom` |

- 두 상태 모두 좌·우·하에 0.5dp `YGAtomicColors.Gray.Gray500` 선. 위에는 선이 없다.
- 문구는 `YGTheme.typography.caption.c01M`, `YGAtomicColors.Gray.Gray700`.
- 화살표 버튼의 터치 영역은 44dp, 아이콘은 16dp.
- 닫힘 상태에서는 루트 안의 바 노드 전체가 `onClickToggle`을 부른다. `isEnabled == false`면 그 노드에 클릭을 달지 않는다.
- 열림 상태에서는 아래 화살표만 `onClickToggle`을 부른다.
- 패널 루트는 자기 영역의 포인터를 모두 소비한다(빈 `pointerInput`으로 충분하다) — 아래에 깔린 캔버스 입력 레이어로 이벤트가 떨어지면 안 된다.
- 색상칩 줄의 오른쪽 끝은 패널 가장자리까지 스크롤된다(`contentPadding`으로 여백을 주고 `modifier`에 padding을 걸지 않는다).
- 토글 노드에 `Role.Button`과 상태에 맞는 contentDescription을 단다.

- [ ] **Step 6: 입력 게이트를 구현한다**

세 입력은 패널 상태가 바뀌어도 **modifier 체인에서 빠지지 않는다.** 갈아 끼우면 진행 중이던 핸들러가 리셋되고, 리컴포지션 뒤에 닿는 두 번째 손가락이 새로 붙은 변형 입력으로 샌다.

- `dismissPanelOnTouch`: `awaitEachGesture` 안에서 첫 down(`requireUnconsumed = false`)을 받고, 그때 `isPanelOpen()`이 참이면 `onDismiss()`를 부른다. 소비하지 않는다.
- `toppingTapInput`·`toppingTransformInput`: 첫 down을 받은 직후 `enabled()`가 거짓이면 `return@awaitEachGesture`. `awaitEachGesture`는 모든 포인터가 떨어진 뒤에야 다음 제스처를 받으므로, 버린 제스처는 뒤늦게 닿은 손가락까지 통째로 버려진다.
- 세 핸들러 모두 `rememberUpdatedState`로 붙잡은 람다를 읽는다. `onDismiss`가 상태를 바꿔도 리컴포지션 전이라, 같은 down을 보는 세 핸들러는 같은 값을 본다.

- [ ] **Step 7: `ToppingArrangeLayout`과 `YGFloatingBarBackTitleClose`를 구현한다**

`ToppingArrangeLayout`의 배치:

```
Column(fillMaxSize)
├─ header()
├─ Box(weight 1f, 가로 padding7, 세로 가운데 정렬)
│   └─ Box(fillMaxWidth, aspectRatio(CANVAS_AREA_ASPECT_RATIO))   ← 자르지 않는다
│       ├─ canvas()
│       └─ panel()            ← 호출부가 Alignment.BottomCenter 로 놓는다
└─ YGButton(YGButtonType.Large, "캔버스에 쌓기", fillMaxWidth, 가로 padding7, 아래 padding1)
```

- 바깥 캔버스 박스를 자르지 않는 이유는 점선 선택 박스가 캔버스 밖으로 나가도 보여야 하기 때문이다. 캔버스 내용을 자르는 것은 `canvas` 슬롯 안에서 호출부가 한다.
- `YGFloatingBarBackTitleClose`는 `YGFloatingBarBackClose`와 같은 좌우 버튼에 `YGFloatingBarEdit`와 같은 제목 스타일을 가운데 둔다.

- [ ] **Step 8: 계측 테스트 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest`
Expected: 새 테스트 10개 포함 전부 PASS

- [ ] **Step 9: Commit**

```bash
git add core/designsystem feature/groups/canvas/impl
git commit -m "feat: add topping border panel, arrange layout and panel input gate"
```

---

### Task 4: 배치 화면 — ViewModel·Screen·Route

ViewModel만 먼저 바꾸면 Screen이 읽던 필드와 Route의 `when (effect)`가 깨져 컴파일되지 않는다. 셋을 한 Task에서 바꾼다.

**Files:**
- Modify: `C/viewmodel/CanvasToppingPlaceViewModel.kt`
- Modify: `C/screen/CanvasToppingPlaceScreen.kt`
- Modify: `C/route/CanvasToppingPlaceRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`
- Create: `CA/TestToppingImage.kt`
- Test: `CT/viewmodel/CanvasToppingPlaceViewModelTest.kt`
- Test: `CA/screen/CanvasToppingPlaceScreenTest.kt`

**Interfaces:**
- Consumes: Task 2 전부; Task 3 `ToppingArrangeLayout`, `ToppingBorderPanel`, `dismissPanelOnTouch`, `toppingTapInput`·`toppingTransformInput`의 `enabled`, `YGFloatingBarBackTitleClose`
- Produces:
  ```kotlin
  data class CanvasToppingPlaceUiState(
      // borderColorArgb, borderWidthDp 는 삭제
      val border: ToppingBorderStyle? = null,
      val pendingBorderWidthDp: Float = DEFAULT_TOPPING_BORDER_WIDTH_DP,
      val isBorderPanelOpen: Boolean = false,
      val showQuitDialog: Boolean = false,
      // 나머지는 그대로
  ) {
      /** 슬라이더가 가리키는 값 */
      val panelBorderWidthDp: Float get() = border?.widthDp ?: pendingBorderWidthDp
  }

  sealed interface CanvasToppingPlaceIntent {
      data object OnClickBack            // 헤더 뒤로
      data object OnClickClose           // 헤더 닫기
      data object OnSystemBack
      data object OnQuitDialogConfirm
      data object OnQuitDialogCancel
      data object OnClickTopping
      data object OnToggleBorderPanel
      data object OnDismissBorderPanel
      data class OnSelectBorderColor(val colorArgb: Int?)
      data class OnChangeBorderWidth(val widthDp: Float)
      // OnClickConfirm, OnToppingTransform, OnCanvasMeasured,
      // OnToppingBaseSizeMeasured, OnToppingImageReadyChanged 는 그대로
  }

  sealed interface CanvasToppingPlaceEffect {
      data object NavigateBack           // 확인 화면으로
      data object QuitToCanvas           // 캔버스 메인으로 되감기
      // 나머지는 그대로
  }

  @Composable
  internal fun CanvasToppingPlaceScreen(
      uiState: CanvasToppingPlaceUiState,
      onClickBack: () -> Unit,
      onClickClose: () -> Unit,
      onClickConfirm: () -> Unit,
      onClickTopping: () -> Unit,
      onToggleBorderPanel: () -> Unit,
      onDismissBorderPanel: () -> Unit,
      onSelectBorderColor: (Int?) -> Unit,
      onChangeBorderWidth: (Float) -> Unit,
      onToppingTransform: (pan: DpOffset, zoom: Float, rotationDelta: Float) -> Unit,
      onCanvasMeasured: (DpSize) -> Unit,
      onToppingBaseSizeMeasured: (DpSize) -> Unit,
      onToppingImageReadyChanged: (Boolean) -> Unit,
      modifier: Modifier = Modifier,
  )

  @Composable
  internal fun ToppingPlaceQuitDialog(onConfirmQuit: () -> Unit, onDismiss: () -> Unit)

  // CA/TestToppingImage.kt — 계획 B 의 계측 테스트도 쓴다
  /** 불투명 단색 PNG 를 캐시 디렉터리에 쓰고 절대경로를 돌려준다 */
  internal fun writeTestToppingPng(context: Context, name: String, widthPx: Int = 200, heightPx: Int = 200): String
  ```

- [ ] **Step 1: 그만두기 팝업 문자열을 더한다**

```xml
<string name="canvas_topping_place_quit_dialog_title">사진 편집을 그만둘까요?</string>
<string name="canvas_topping_place_quit_dialog_body">지금까지 진행한 내용은 저장되지 않아요.\n정말 그만두시겠어요?</string>
<string name="canvas_topping_place_quit_dialog_confirm">그만두기</string>
<string name="canvas_topping_place_quit_dialog_cancel">계속 편집</string>
```

- [ ] **Step 2: ViewModel 단위 테스트를 쓴다**

기존 테스트 셋을 먼저 정리한다.

- `draft_fillsTheToppingImageAndBorder` → 테두리 단언을 빼고 `draft_fillsTheToppingImage`로.
- `onClickConfirm_sendsDraftIdentityAndBorderAsServerFormat` → 테두리를 초안이 아니라 `OnSelectBorderColor(0xFFFF6B00.toInt())` + `OnChangeBorderWidth(4f)`로 준다. 기대 `Solid("#FF6B00", 4.0)`은 그대로다.
- `onClickConfirm_nonOpaqueBorderColor_failsInsteadOfCrashing` → 지운다. 팔레트가 불투명 색뿐이라 패널로는 반투명 색을 줄 수 없다.
- `draft()` 헬퍼의 `borderColorArgb`·`borderWidthDp` 인자는 Task 5에서 뺀다. 여기서는 넘기지 않기만 한다.

새 테스트:

```kotlin
@Test fun onClickTopping_opensPanel()
@Test fun onClickTopping_beforeImageReady_isIgnored()
@Test fun onClickTopping_whilePanelOpen_closesPanel()
@Test fun onToggleBorderPanel_flipsOpenState()              // false → true → false
@Test fun onToggleBorderPanel_beforeImageReady_isIgnored()
@Test fun onDismissBorderPanel_closesPanel()

@Test
fun onSelectBorderColor_usesPendingWidth() {
    // OnChangeBorderWidth(14f) → OnSelectBorderColor(0xFF0E0E0E.toInt())
    assertEquals(ToppingBorderStyle(0xFF0E0E0E.toInt(), 14f), state.border)
}

@Test
fun onChangeBorderWidth_withBorder_changesBorderAndPending() {
    // OnSelectBorderColor(black) → OnChangeBorderWidth(6f)
    assertEquals(6f, state.border?.widthDp)
    assertEquals(6f, state.pendingBorderWidthDp)
}

@Test
fun onSelectBorderColor_null_removesBorderAndKeepsLastWidth() {
    // OnSelectBorderColor(black) → OnChangeBorderWidth(20f) → OnSelectBorderColor(null)
    assertNull(state.border)
    assertEquals(20f, state.panelBorderWidthDp)
}

@Test
fun onChangeBorderWidth_isClampedToRange()                  // 100f → 30f, 0f → 2f

@Test
fun onToppingTransform_whilePanelOpen_isIgnored() {
    // 초기 배치 완료 → OnClickTopping → OnToppingTransform(pan = 30dp)
    // offsetX/offsetY/scale/rotationDegrees 불변, hasUserAdjustedPlacement == false
}

@Test
fun onClickConfirm_whilePanelOpen_sendsStoredPlacement() {
    // 초기 배치 → OnToppingTransform(pan (20dp, 10dp)) → OnClickTopping → OnClickConfirm
    // addToppingUseCase 가 받은 transform == 패널을 열기 전 상태로 계산한 toToppingTransform(...)
}

@Test
fun onClickConfirm_withoutColor_sendsNoBorder() {
    // OnChangeBorderWidth(20f) → OnClickConfirm → border == ToppingBorder.None
}

@Test fun confirmFailure_keepsPanelOpen()                   // isBorderPanelOpen == true, isLoading == false

@Test fun onClickBack_emitsNavigateBack()
@Test fun onClickClose_showsQuitDialog()                    // showQuitDialog == true, effect 없음
@Test fun onSystemBack_withPanelOpen_onlyClosesPanel()      // showQuitDialog == false, effect 없음
@Test fun onSystemBack_withPanelClosed_showsQuitDialog()
@Test
fun onSystemBack_whileLoading_isIgnored() {
    // addToppingUseCase 가 끝나지 않게 걸어 두고 OnClickTopping → OnClickConfirm → OnSystemBack
    // isBorderPanelOpen == true, showQuitDialog == false
}
@Test fun onQuitDialogConfirm_emitsQuitToCanvas()           // showQuitDialog == false
@Test fun onQuitDialogCancel_hidesDialog()
```

- [ ] **Step 3: 화면 계측 테스트를 쓴다**

`@Before`에서 `writeTestToppingPng(context, "place.png")`로 그림을 만든다. 화면은 아래 상태로 직접 띄운다 — 토핑이 캔버스 한가운데 놓인다.

```kotlin
CanvasToppingPlaceUiState(
    toppingImagePath = path, isDraftLoaded = true, isToppingImageReady = true,
    canvasSize = DpSize(300.dp, 534.dp), toppingBaseSize = DpSize(100.dp, 100.dp),
    offsetX = 100.dp, offsetY = 217.dp, scale = 1f, hasUserAdjustedPlacement = true,
)
```

화면 폭은 `Modifier.requiredWidth(340.dp)`로 고정한다(좌우 `padding7` 20dp씩 빼면 캔버스 폭 300dp). 탭하기 전에 `waitUntil { imageReady }`로 `onToppingImageReadyChanged(true)`를 기다린다 — 그림이 뜨기 전에는 판정 대상이 없다.

```kotlin
@Test
fun header_showsBackTitleAndClose_andBottomButton() {
    onNodeWithText("배치").assertIsDisplayed()
    onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    onNodeWithContentDescription("닫기").assertIsDisplayed()
    onNodeWithText("캔버스에 쌓기").performClick()
    assertEquals(1, confirmCount)
}

@Test
fun panelClosed_tapOnTopping_requestsOpen()          // 캔버스 중앙을 탭 → clickToppingCount == 1

@Test
fun panelClosed_tapOnEmptyCanvas_doesNothing() {
    // 캔버스 좌상단 (10dp, 10dp) 을 탭
    // clickToppingCount == 0, transformCount == 0, dismissCount == 0, toggleCount == 0
}

@Test
fun panelOpen_dragOnCanvas_onlyDismisses() {
    // isBorderPanelOpen = true. 캔버스 중앙에서 드래그
    // dismissCount == 1, transformCount == 0, clickToppingCount == 0
}
```

- [ ] **Step 4: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*CanvasToppingPlaceViewModelTest'`
Expected: FAIL — 미해결 참조

- [ ] **Step 5: ViewModel을 고친다**

- `observeDraft`는 테두리를 읽지 않는다.
- 패널이 열려 있는 동안 `OnToppingTransform`은 무시하고, `OnClickTopping`은 패널을 닫는다.
- `OnClickTopping`·`OnToggleBorderPanel`은 `isToppingImageReady == false`면 무시한다.
- `OnChangeBorderWidth`: 값을 `TOPPING_BORDER_WIDTH_RANGE_DP`로 가둔 뒤 `pendingBorderWidthDp`에 넣고, `border`가 있으면 그 굵기도 바꾼다.
- `OnSelectBorderColor(argb)`: null이면 `border = null`, 아니면 `ToppingBorderStyle(argb, panelBorderWidthDp)`.
- `OnSystemBack`: `isLoading`이면 무시. 패널이 열려 있으면 닫는다. 아니면 `showQuitDialog = true`.
- `handleOnClickConfirm`은 `current.border.toToppingBorder()`를 쓴다. 파일 안의 `private fun toToppingBorder(colorArgb, widthDp)`는 지운다.
- 기존 `OnClickClose → NavigateBack` 연결은 `OnClickBack → NavigateBack`으로 옮긴다.

- [ ] **Step 6: 화면을 고친다**

- 루트를 `ToppingArrangeLayout`으로 바꾼다. `header`는 `YGFloatingBarBackTitleClose`. 하단 `YGFloatingBarEdit`와 `60.dp`/`14.dp` 여백은 없앤다.
- `canvas` 슬롯에 지금의 두 박스(자르는 캔버스 박스, 자르지 않는 선택 박스 레이어)를 그대로 넣는다.
- 그릴 때 쓰는 중심을 하나 더 만든다. `uiState.canvasSize`가 null이면 `center`를 그대로 쓴다.
  ```kotlin
  val focusFraction by animateFloatAsState(if (uiState.isBorderPanelOpen) 1f else 0f)
  val drawnCenter = uiState.canvasSize
      ?.let { canvasSize -> lerp(center, panelFocusCenter(canvasSize), focusFraction) }
      ?: center
  ```
  `lerp`는 `androidx.compose.ui.unit.lerp(DpOffset, DpOffset, Float)`다. 토핑 그림과 `ToppingSelectionStroke`는 `drawnCenter`를 쓴다. `ToppingHitTarget`은 `center`를 그대로 쓴다.
- 입력 레이어는 하나이고 체인이 고정이다.
  ```kotlin
  Modifier
      .matchParentSize()
      .dismissPanelOnTouch(isPanelOpen = { uiState.isBorderPanelOpen }, onDismiss = onDismissBorderPanel)
      .toppingTapInput(
          entries = { listOfNotNull(hitTarget()?.let { target -> Unit to target }) },
          keyOf = { it },
          onHit = { onClickTopping() },
          onMiss = {},
          enabled = { !uiState.isBorderPanelOpen },
      )
      .toppingTransformInput(
          targetAt = ::hitTarget,
          onTransform = { ... },
          enabled = { !uiState.isBorderPanelOpen },
      )
  ```
  `hitTarget()`은 지금 `targetAt` 람다의 본문이다(그림이 뜨기 전에는 null).
- `panel` 슬롯에 `ToppingBorderPanel`을 `Alignment.BottomCenter`, `fillMaxWidth`로 놓는다. `selectedColorArgb = uiState.border?.colorArgb`, `widthDp = uiState.panelBorderWidthDp`, `widthRange = TOPPING_BORDER_WIDTH_RANGE_DP`.
- 테두리를 그리는 자리와 `borderWidthPx`는 `uiState.border`에서 읽는다.
- 화면 상단 KDoc의 "삭제·테두리 재편집 없음"은 지금 상태에 맞게 고친다.

- [ ] **Step 7: Route를 고친다**

- 새 콜백을 intent에 잇는다.
- `BackHandler { viewModel.processIntent(OnSystemBack) }`를 단다.
- `uiState.showQuitDialog`면 `ToppingPlaceQuitDialog`를 띄운다.
- `NavigateBack → navigator.onBack()`, `QuitToCanvas → navigator.popUpTo<NavKeyCanvasMain>()`.

- [ ] **Step 8: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest :feature:groups:canvas:impl:connectedDebugAndroidTest`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add feature/groups/canvas/impl
git commit -m "feat: rebuild topping place screen with border panel and quit dialog"
```

---

### Task 5: 초안에서 테두리 필드를 뺀다

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingDraft.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/RecordToppingDraftUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/EnsureDraftSubjectRecordedUseCase.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/model/entity/ToppingDraftEntity.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/model/mapper/entity/ToppingDraftEntityMapper.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt`
- Modify: `S/viewmodel/ToppingEditDraft.kt`, `S/viewmodel/SegmentationViewModel.kt`
- Modify: `S/viewmodel/SegmentationConfirmViewModel.kt`, `S/route/SegmentationConfirmRoute.kt`, `S/screen/SegmentationConfirmScreen.kt`
- Test: `data/src/test/.../ToppingDraftLocalDataSourceImplTest.kt`, `ToppingDraftRepositoryImplTest.kt`
- Test: `domain/src/test/.../EnsureDraftSubjectRecordedUseCaseTest.kt`
- Test: `ST/viewmodel/SegmentationViewModelTest.kt`, `ST/viewmodel/SegmentationConfirmViewModelTest.kt`, `ST/viewmodel/ToppingEditViewModelTest.kt`
- Test: `CT/viewmodel/CanvasToppingPlaceViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  // RecordToppingDraftUseCase, ToppingDraftRepository.record
  suspend operator fun invoke(
      subjectImagePath: String,
      cutoutImagePath: String?,
      sourceLongSide: SourceLongSide?,
  ): Boolean
  ```
  `ToppingDraft`·`ToppingDraftEntity`에는 `borderColorArgb`·`borderWidthDp`가 없다. `SegmentationConfirmState`에는 `borderColorArgb`·`borderWidthDp`·`borderLayers`가 없다. `SegmentationConfirmScreen`은 테두리 인자를 받지 않는다.

- [ ] **Step 1: 옛 초안을 읽는 테스트를 쓴다**

```kotlin
// ToppingDraftLocalDataSourceImplTest
@Test
fun draft_legacyJsonWithBorderFields_keepsTheRest() {
    // DataStore 에 옛 형식 JSON 을 직접 써 넣는다:
    // {"groupId":1,"parfaitId":2,"nextPositionZ":3,
    //  "subjectImagePath":"/s.png","borderColorArgb":-16777216,"borderWidthDp":10.0}
    // 흐름이 내는 ToppingDraft: groupId == GroupId(1), parfaitId == ParfaitId(2),
    //                          nextPositionZ == 3, subjectImagePath == "/s.png"
}
```

JSON의 키 이름은 `ToppingDraftEntity`의 실제 직렬 이름에 맞춘다. 이 테스트 클래스는 자체 `Json { ignoreUnknownKeys = true }`를 쓴다 — 프로덕션 `@LocalJson`(`data/di/JsonModule.kt`)과 같은 설정인지 한 번 대조한다.

- [ ] **Step 2: 통과 확인(현재 코드)**

Run: `./gradlew :data:testDebugUnitTest --tests '*ToppingDraftLocalDataSourceImplTest'`
Expected: PASS — 필드를 빼기 전이라 통과한다. 다음 단계 뒤에도 통과해야 하는 기준이다.

- [ ] **Step 3: 필드와 인자를 뺀다**

- domain·data의 일곱 파일에서 `borderColorArgb`·`borderWidthDp`를 뺀다.
- 낡는 KDoc을 함께 고친다: `ToppingDraft`의 "이미지와 테두리가 빈 초안", `ToppingDraftRepository.record`의 "알맹이·테두리를 초안에 적는다 … 테두리는 넘어온 값으로 매번 덮어쓴다", `SegmentationConfirmViewModel`의 "편집 결과와 테두리를 덮어쓴다".
- `ToppingEditDraft.kt`의 `recordEditResult`는 `result.borderLayers`를 읽지 않는다.
- `SegmentationViewModel`·`EnsureDraftSubjectRecordedUseCase`의 `= null` 인자 두 줄을 뺀다.
- `SegmentationConfirmViewModel`은 초안에서 테두리를 읽지 않고 상태에서 세 필드를 뺀다. `SegmentationConfirmScreen`의 `YGToppingCutoutImage`는 `borderColor = null`, `borderWidth = 0.dp`로 그린다.
- `SegmentationConfirmRoute`가 `NavKeyToppingEdit`에 넘기던 `borderLayers = uiState.borderLayers`는 뺀다(기본값 `emptyList()`).

- [ ] **Step 4: 테스트를 고친다**

- 테두리 인자와 단언을 걷는다. `ToppingEditViewModelTest`는 `recordToppingDraft` mock의 `any()` 다섯 개를 셋으로, 이름 붙인 `borderColorArgb = null, borderWidthDp = null`을 뺀다.
- 테두리가 주제인 테스트는 지운다: `SegmentationConfirmViewModelTest`의 `onEditResult_recordsBorderValues`, `draft_carriesTheBorder_backIntoTheEditor`.
- `CanvasToppingPlaceViewModelTest.draft()` 헬퍼의 두 인자를 뺀다.

- [ ] **Step 5: 전체 단위 테스트 확인**

Run: `./gradlew :domain:test :data:testDebugUnitTest :feature:segmentation:impl:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest`
Expected: PASS. Step 1의 테스트도 PASS.

- [ ] **Step 6: Commit**

```bash
git add domain data feature
git commit -m "refactor: drop border fields from the topping draft"
```

---

### Task 6: 편집 화면의 테두리 탭을 숨기고 확인 화면을 맞춘다

**Files:**
- Modify: `S/screen/ToppingEditScreen.kt`
- Modify: `S/viewmodel/ToppingEditViewModel.kt`
- Modify: `S/viewmodel/SegmentationConfirmViewModel.kt`, `S/screen/SegmentationConfirmScreen.kt`, `S/route/SegmentationConfirmRoute.kt`
- Modify: `SAPI/NavKeySegmentationConfirm.kt` (KDoc)
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`
- Test: `ST/viewmodel/ToppingEditViewModelTest.kt`, `ST/viewmodel/SegmentationConfirmViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  // SegmentationConfirmState
  /** 원본이 있어 영역을 고칠 수 있을 때만 "사진 편집"을 보인다 */
  val canEditPhoto: Boolean get() = sourceImageUri != null

  // SegmentationConfirmScreen 에 인자 하나가 는다
  showEditPhotoButton: Boolean,
  ```

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
// ToppingEditViewModelTest
@Test
fun changeTab_toBorder_whenNotBorderOnly_isIgnored() {
    // borderOnly = false 로 만든 ViewModel 에 ChangeTab(BORDER)
    assertEquals(ToppingEditTab.AREA, state.tab)
}

@Test
fun borderOnly_stillOpensOnBorderTab()               // borderOnly = true → tab == BORDER

// SegmentationConfirmViewModelTest
@Test
fun canEditPhoto_isFalse_withoutSourceImage()

@Test
fun tutorial_isHidden_whenPhotoCannotBeEdited() {
    // sourceImageUri = null, getTutorialVisibleFlowUseCase 가 true 를 흘려도
    assertFalse(state.isTutorialVisible)
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests '*ToppingEditViewModelTest' --tests '*SegmentationConfirmViewModelTest'`
Expected: FAIL

- [ ] **Step 3: 편집 화면을 고친다**

- `ToppingEditViewModel`: `ChangeTab(BORDER)`는 `isBorderOnly`가 아니면 무시한다.
- `ToppingEditScreen`: `isBorderOnly == false`일 때 하단 바를 `YGFloatingBarEditTab`에서 `YGFloatingBarEdit`로 바꾼다. 제목 문자열 `topping_edit_area_only_title`을 새로 둔다(값 "영역"). `isBorderOnly == true` 갈래는 손대지 않는다.
- 가르는 기준은 `isBorderOnly`뿐이다. `completion`은 보지 않는다.

- [ ] **Step 4: 확인 화면을 고친다**

- `SegmentationConfirmScreen`: `showEditPhotoButton == false`면 "사진 편집" 버튼을 그리지 않고 "다음" 버튼이 줄 전체를 차지한다. Route가 `uiState.canEditPhoto`를 넘긴다.
- `SegmentationConfirmViewModel`: `isTutorialVisible`은 `canEditPhoto && 흐름 값`이다.
- 튜토리얼 문자열을 고친다.
  ```xml
  <string name="segmentation_confirm_tutorial_title">영역 수정</string>
  <string name="segmentation_confirm_tutorial_description">하단 사진 편집 버튼을 통해\n완벽히 편집되지 않은 배경 영역을 수정할 수 있어요</string>
  ```
- `NavKeySegmentationConfirm`의 KDoc "'사진 편집'이 테두리 편집만 연다"를 지금 상태에 맞게 고친다.
- `SegmentationConfirmRoute`의 `borderOnly = uiState.isBorderOnlyEdit` 인자와 `sourceImageUri ?: editImageUri` 폴백은 **그대로 둔다**. 버튼이 숨겨져 닿지 않는 길이 되고, 걷는 것은 계획 B다.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add feature/segmentation
git commit -m "feat: hide the border tab in the new topping flow"
```

---

### Task 7: 검증과 문서

**Files:**
- Modify: `docs/status.md`
- Modify: `docs/synthesis/open-questions.md`
- Create: `docs/adr/0034-topping-border-set-at-placement.md`
- Modify: `docs/adr/README.md`, `docs/log.md`

- [ ] **Step 1: 전체 검증**

Run: `./gradlew ktlintCheck testDebugUnitTest :domain:test :feature:groups:canvas:impl:connectedDebugAndroidTest assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 에뮬레이터에서 피그마와 대조한다**

| 확인 | 기대 |
|---|---|
| 촬영 → 누끼 → 확인 → 배치 | 헤더 뒤로·"배치"·닫기, 접힌 패널, "캔버스에 쌓기" (피그마 `5453:10418`) |
| 접힌 바 탭 / 토핑 탭 | 패널이 열리고 토핑이 가운데 위로 움직인다 (피그마 `5461:9261`) |
| 색·굵기 변경 | 토핑 테두리가 바로 바뀐다 |
| 열린 채 캔버스 드래그 / 핀치 | 패널만 닫히고 토핑은 원래 자리로 돌아온다. 움직이거나 커지지 않는다 |
| 열린 채 "캔버스에 쌓기" | 캔버스 메인에서 토핑이 원래 자리에 테두리와 함께 보인다 |
| 헤더 뒤로 | 팝업 없이 확인 화면 |
| 헤더 닫기 / 시스템 뒤로가기 | 그만두기 팝업 → 확인 시 캔버스 메인 |
| 패널이 열린 채 시스템 뒤로가기 | 패널만 닫힌다 |
| 업로드 중 시스템 뒤로가기 | 아무 일 없다 |
| 확인 화면 "사진 편집" | 탭 없이 영역 수정만 열린다 |
| 갤러리 최근 줄의 알맹이로 진입 | 확인 화면에 "사진 편집" 버튼이 없다 |
| 캔버스 메인 → 본인 토핑 탭 → 토핑 탭의 연필 | 테두리 편집이 지금처럼 열리고 저장된다 (C-301 회귀 확인) |

화면 높이가 다른 기기 두 대(작은 것, 큰 것)에서 패널이 열렸을 때 토핑이 패널에 가려지는지 본다. 가려지면 고치지 않고 Step 4에서 미결 항목으로 적는다.

- [ ] **Step 3: `docs/status.md`를 덮어쓴다**

"토핑 생성·배치" 영역의 상태 문단·앵커·⚠️ 줄을 지금 코드에 맞춘다. 테두리가 배치 화면 패널에서 정해진다는 것, 초안에 테두리가 없다는 것, 편집 화면의 테두리 탭은 C-301 진입에만 남아 있다는 것을 적는다. 변경 서술("~에서 ~로 바뀌었다")은 쓰지 않는다.

- [ ] **Step 4: 미결 항목과 ADR**

- `open-questions.md`: OQ-P-081 ②, OQ-P-203 ③을 바뀐 사용처로 고쳐 쓴다. Step 2에서 가림을 봤다면 새 항목을 연다. OQ-P-324·338은 닫지 않는다 — 계획 B의 몫이다.
- ADR-0034: 테두리는 편집이 아니라 배치 단계의 속성이다. ADR-0026의 프로세스 사망 복원 보장에서 테두리가 빠진다는 것과 그 이유(고르는 데 몇 초 걸리는 값이고 헤더의 뒤로로 돌아갈 때도 버린다)를 적는다.
- ADR-0026 본문에서 초안의 테두리 필드를 현재형으로 말하는 문장이 있으면 ADR-0034를 가리키게 고친다.
- `docs/log.md`에 한 줄.

- [ ] **Step 5: 링크 확인과 커밋**

Run: `python3 docs/script/check_links.py docs`
Expected: 깨진 링크 0건

```bash
git add docs
git commit -m "docs: record the placement-stage border panel"
```
