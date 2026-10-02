---
id: c105-arrange-add-flow
title: 배치 화면 테두리 패널 — 추가 플로우 (계획 A)
status: draft
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
archived_reason:
tags: [plan, parfait]
---

# 배치 화면 테두리 패널 — 추가 플로우 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 새 토핑을 쌓는 배치 화면에서 테두리를 하단 패널로 고르게 하고, 편집 화면의 테두리 탭을 이 흐름에서 걷어 낸다.

**Architecture:** 패널·화면 뼈대·패널 열림 위치 계산을 `feature/groups/canvas/impl`의 공용 부품으로 만든다. `CanvasToppingPlaceViewModel`은 테두리를 초안에서 읽지 않고 패널 intent로 받는다. 패널이 열린 동안 토핑은 그릴 때만 가운데로 옮기고 상태의 위치값은 건드리지 않는다. 초안의 테두리 필드는 domain·data까지 삭제한다. 편집 화면의 테두리 탭 코드는 지우지 않고 숨기기만 한다 — C-301이 계획 B가 끝날 때까지 쓴다.

**Tech Stack:** Kotlin, Jetpack Compose(`pointerInput`, `animateFloatAsState`, `BackHandler`), Hilt, JUnit4 + MockK + Turbine + coroutines-test, Compose UI 계측 테스트(`createComposeRule`).

**Spec:** [`docs/superpowers/specs/2026-10-02-c105-arrange-border-merge-design.md`](../specs/2026-10-02-c105-arrange-border-merge-design.md)

## Global Constraints

- 코드 주석·KDoc은 [`docs/code-conventions.md`](../../code-conventions.md)를 따른다. 구현·리뷰 서브에이전트 브리프에 이 링크를 넣는다.
- 경로 약어
  - `C` = `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl`
  - `CT` = 같은 모듈 `src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl`
  - `CA` = 같은 모듈 `src/androidTest/kotlin/com/teamyg/parfait/feature/groups/canvas/impl`
  - `S` = `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl`
  - `ST` = 같은 모듈 `src/test/java/com/teamyg/parfait/feature/segmentation/impl`
  - `DS` = `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem`
- 검증 명령: 단위 `./gradlew :feature:groups:canvas:impl:testDebugUnitTest`, 계측 `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest`, 스타일 `./gradlew ktlintCheck`.
- 이 계획만 머지된 상태에서 전체 빌드가 통과하고 C-301의 테두리 재편집 왕복(`NavKeyToppingEdit(borderOnly = true)` → `TOPPING_EDIT_RESULT_KEY`)이 그대로 돌아야 한다.
- `feature/segmentation/impl`의 `BorderColorChipRow.kt`, `ToppingBorderColors.kt`, `ToppingBorderEditScreen.kt`, `ToppingBorderPreviewLayout.kt`, `ToppingBorderOutline.kt`, `NavKeyToppingEdit`의 `borderLayers`·`borderOnly`, `ToppingBorderLayer`는 **지우지 않는다**. 계획 B의 몫이다.
- 테두리 굵기 범위는 `ToppingBorder.WIDTH_RANGE_DP`. 기본 굵기는 `10f`.
- 테두리 팔레트는 9색, 맨 앞이 "없음"이다. 순서와 값은 `S/editor/ToppingBorderColors.kt`와 같다.
- 패널이 열린 동안 포커스 토핑의 세로 위치는 캔버스 중앙에서 `18.5.dp` 위다. 크기와 각도는 바꾸지 않는다.
- 문구
  - 화면 제목: "배치"
  - 접힌 패널: "테두리 설정" / 펼친 패널: "테두리 굵기"
  - 하단 버튼: "캔버스에 쌓기"
  - 그만두기 팝업: 제목 "사진 편집을 그만둘까요?", 본문 "지금까지 진행한 내용은 저장되지 않아요.\n정말 그만두시겠어요?", 확인 "그만두기", 취소 "계속 편집"
  - 접근성 설명: "테두리 설정 열기", "테두리 설정 닫기"
- 분석 화면 id는 바꾸지 않는다(`NavKeyCanvasToppingPlace` → `"C-106"`).
- 이 계획은 `docs/status.md`·`open-questions.md`·ADR을 마지막 Task에서만 고친다.

## Review Focus

1. 패널이 열린 채 "캔버스에 쌓기"를 누른다 — 서버에 가는 위치가 가운데 임시 위치가 아니라 원래 위치여야 한다. (Task 4 테스트)
2. 테두리 색을 고르지 않은 채 굵기만 바꾸고 확정한다 — `ToppingBorder.None`이 가야 하고 굵기만 실린 `Solid`가 가면 서버가 400을 낸다. (Task 4 테스트)
3. 패널을 닫는 터치가 그대로 드래그로 이어진다 — 닫기만 하고 토핑이 움직이지 않아야 한다. (Task 3 계측 테스트)
4. 슬라이더를 끄는 손가락이 패널 밖으로 벗어난다 — 패널이 닫히지 않아야 한다. (Task 3 계측 테스트)
5. 옛 버전이 DataStore에 남긴 테두리 필드가 든 초안을 새 버전이 읽는다 — 디코드가 깨지지 않고 나머지 필드가 살아야 한다. (Task 6 테스트)

---

### Task 1: 슬라이더를 디자인시스템으로 옮긴다

**Files:**
- Create: `DS/component/ygslider/YGSlider.kt`
- Delete: `S/component/BrushWidthSlider.kt`
- Modify: `S/screen/ToppingEditScreen.kt` (호출부 두 곳)

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

패키지는 `com.teamyg.parfait.core.designsystem.component.ygslider`, 함수 이름은 `YGSlider`, 가시성은 `public`. 트랙·thumb·프리뷰는 그대로 둔다. KDoc 첫 줄의 "브러시 굵기를 조절하는"은 용도를 가리지 않는 문장으로 고친다.

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
  internal data class ToppingBorderStyle(val colorArgb: Int, val widthDp: Float)
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

`toToppingBorder_style_isSolidWithRgbHex`의 기대 문자열은 `Int.toRgbHexString()`이 실제로 내는 형식에 맞춘다(`core/util/android`). 형식이 다르면 테스트를 그 형식으로 고친다.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*ToppingPanelFocusTest' --tests '*ToppingBorderStyleTest'`
Expected: FAIL — 미해결 참조

- [ ] **Step 3: 세 파일을 구현한다**

- `TOPPING_BORDER_WIDTH_RANGE_DP`는 `ToppingBorder.WIDTH_RANGE_DP`의 양 끝을 `Float`로 바꾼 것이다. 숫자를 다시 적지 않는다.
- `toToppingBorder`·`toToppingBorderStyleOrNull`은 `CanvasToppingPlaceViewModel.toToppingBorder`와 `CanvasBGEditViewModel.toBorderLayers`가 쓰는 변환(`toRgbHexString`, `toColorOrNull`)을 그대로 쓴다.
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

### Task 3: 패널·화면 뼈대·헤더·닫기 입력

**Files:**
- Create: `C/component/BorderColorChipRow.kt`
- Create: `C/component/ToppingBorderPanel.kt`
- Create: `C/component/ToppingPanelDismissInput.kt`
- Create: `C/component/ToppingArrangeLayout.kt`
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

  /** 닿는 순간 [onDismiss] 를 부르고 그 제스처를 끝까지 삼킨다 */
  internal fun Modifier.dismissPanelOnTouch(onDismiss: () -> Unit): Modifier

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
- 테스트 태그: `"topping_border_panel"`(패널 전체), `"topping_border_panel_toggle"`(접힌 바 또는 아래 화살표), `"topping_border_slider"`.

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
    onNodeWithContentDescription("테두리 설정 닫기").performClick()
    assertEquals(1, toggleCount)
}

@Test
fun open_selectingNoneChip_emitsNull() {
    // isOpen = true, selectedColorArgb = 0xFF000000.toInt()
    // 첫 칩("없음")을 누르면 onSelectColor(null)
    assertEquals(listOf<Int?>(null), selected)
}

// ToppingPanelDismissInputTest — 아래에 "움직임을 세는" pointerInput 레이어, 위에 dismissPanelOnTouch
@Test
fun down_dismissesImmediately() {
    onRoot().performTouchInput { down(center) }
    assertEquals(1, dismissCount)
}

@Test
fun dragAfterDown_isSwallowed_andDismissesOnce() {
    onRoot().performTouchInput { down(center); moveBy(Offset(120f, 0f)); up() }
    assertEquals(1, dismissCount)
    assertEquals(0, lowerLayerEventCount)
}

@Test
fun sliderDragLeavingPanel_doesNotDismiss() {
    // Box { Box(dismissPanelOnTouch) ; ToppingBorderPanel(isOpen = true, align Bottom) }
    onNodeWithTag("topping_border_slider").performTouchInput {
        down(centerLeft); moveBy(Offset(60f, -400f)); up()
    }
    assertEquals(0, dismissCount)
}

@Test
fun tapOnPanelBlankArea_doesNotDismiss() {
    onNodeWithTag("topping_border_panel").performTouchInput { click(topLeft + Offset(4f, 4f)) }
    assertEquals(0, dismissCount)
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingBorderPanelTest`
Expected: FAIL — 미해결 참조

- [ ] **Step 4: `BorderColorChipRow`를 구현한다**

`S/component/BorderColorChipRow.kt`를 복제하고 시그니처를 위 형태로 바꾼다. 색 비교는 `Color.toArgb()`로 한다. 목록 첫 항목(`Color.Transparent`)은 `selectedColorArgb == null`일 때 켜지고, 누르면 `onSelectColor(null)`이다.

- [ ] **Step 5: `ToppingBorderPanel`을 구현한다**

| 상태 | 배경 | 안쪽 여백 | 내용 |
|---|---|---|---|
| 닫힘 | `YGAtomicColors.Transparency.White75` | `padding6` 사방 | "테두리 설정" + 오른쪽 `ic_caret_top` |
| 열림 | `YGAtomicColors.Gray.White` | 가로 `padding6`, 세로 `padding7` | "테두리 굵기", 4dp 아래 `YGSlider`, `gap3` 아래 `BorderColorChipRow`, 오른쪽 위 `ic_caret_bottom` |

- 두 상태 모두 좌·우·하에 0.5dp `YGAtomicColors.Gray.Gray500` 선. 위에는 선이 없다.
- 문구는 `YGTheme.typography.caption.c01M`, `YGAtomicColors.Gray.Gray700`.
- 화살표 버튼의 터치 영역은 44dp, 아이콘은 16dp.
- 닫힘 상태에서는 바 전체가 `onClickToggle`을 부른다. `isEnabled == false`면 클릭을 달지 않는다.
- 열림 상태에서는 아래 화살표만 `onClickToggle`을 부른다.
- 패널 루트는 자기 영역의 포인터를 모두 소비한다 — 아래 레이어의 `dismissPanelOnTouch`로 이벤트가 떨어지면 안 된다.
- 색상칩 줄의 오른쪽 끝은 패널 가장자리까지 스크롤된다(`contentPadding`으로 여백을 주고 `modifier`에 padding을 걸지 않는다).
- 토글 노드에 `Role.Button`과 상태에 맞는 contentDescription을 단다.

- [ ] **Step 6: `dismissPanelOnTouch`를 구현한다**

`awaitEachGesture` 안에서 첫 down을 소비하고 `onDismiss()`를 부른 뒤, 모든 포인터가 떨어질 때까지 들어오는 변화를 전부 소비한다. 핸들러는 `toppingTapInput`처럼 `rememberUpdatedState`로 최신 람다를 본다.

- [ ] **Step 7: `ToppingArrangeLayout`과 `YGFloatingBarBackTitleClose`를 구현한다**

`ToppingArrangeLayout`의 배치:

```
Column(fillMaxSize)
├─ header()
├─ Box(weight 1f, 가로 padding7, 세로 가운데 정렬)
│   └─ Box(fillMaxWidth, aspectRatio(CANVAS_AREA_ASPECT_RATIO))   ← 자르지 않는다
│       ├─ canvas()
│       └─ panel()            ← 호출부가 Alignment.BottomCenter 로 놓는다
└─ YGButton(Large, "캔버스에 쌓기", fillMaxWidth, 가로 padding7, 아래 padding1)
```

- 바깥 캔버스 박스를 자르지 않는 이유는 점선 선택 박스가 캔버스 밖으로 나가도 보여야 하기 때문이다. 캔버스 내용을 자르는 것은 `canvas` 슬롯 안에서 호출부가 한다.
- `YGFloatingBarBackTitleClose`는 `YGFloatingBarBackClose`와 같은 좌우 버튼에 `YGFloatingBarEdit`와 같은 제목 스타일을 가운데 둔다.

- [ ] **Step 8: 계측 테스트 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest`
Expected: 새 테스트 8개 포함 전부 PASS

- [ ] **Step 9: Commit**

```bash
git add core/designsystem feature/groups/canvas/impl
git commit -m "feat: add topping border panel, arrange layout and dismiss input"
```

---

### Task 4: `CanvasToppingPlaceViewModel` — 패널 상태와 그만두기

**Files:**
- Modify: `C/viewmodel/CanvasToppingPlaceViewModel.kt`
- Test: `CT/viewmodel/CanvasToppingPlaceViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2 `ToppingBorderStyle`, `DEFAULT_TOPPING_BORDER_WIDTH_DP`, `TOPPING_BORDER_WIDTH_RANGE_DP`, `toToppingBorder`
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
  ```

- [ ] **Step 1: 실패하는 테스트를 쓴다**

기존 `draft_fillsTheToppingImageAndBorder`는 테두리 단언을 빼고 `draft_fillsTheToppingImage`로 고친다. `draft()` 헬퍼의 `borderColorArgb`·`borderWidthDp` 인자는 Task 6에서 뺀다 — 여기서는 넘기지 않기만 한다.

```kotlin
@Test
fun onClickTopping_opensPanel()                       // isBorderPanelOpen == true

@Test
fun onToggleBorderPanel_flipsOpenState()              // false → true → false

@Test
fun onDismissBorderPanel_closesPanel()

@Test
fun onSelectBorderColor_usesPendingWidth() {
    // OnChangeBorderWidth(14f) → OnSelectBorderColor(0xFF000000.toInt())
    assertEquals(ToppingBorderStyle(0xFF000000.toInt(), 14f), state.border)
}

@Test
fun onSelectBorderColor_null_removesBorderAndKeepsWidthAsPending() {
    // OnSelectBorderColor(black) → OnChangeBorderWidth(20f) → OnSelectBorderColor(null)
    assertNull(state.border)
    assertEquals(20f, state.panelBorderWidthDp)
}

@Test
fun onChangeBorderWidth_withBorder_changesBorderWidth() {
    // OnSelectBorderColor(black) → OnChangeBorderWidth(6f)
    assertEquals(6f, state.border?.widthDp)
}

@Test
fun onChangeBorderWidth_isClampedToRange() {
    // OnChangeBorderWidth(100f) → 30f, OnChangeBorderWidth(0f) → 2f
}

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
    // OnChangeBorderWidth(20f) → OnClickConfirm
    // addToppingUseCase 가 받은 border == ToppingBorder.None
}

@Test
fun onClickConfirm_withColor_sendsSolidBorder() {
    // OnSelectBorderColor(0xFFFCC2CC.toInt()) → OnClickConfirm
    // border is ToppingBorder.Solid, width == 10.0
}

@Test
fun onClickBack_emitsNavigateBack()

@Test
fun onClickClose_showsQuitDialog()                    // showQuitDialog == true, effect 없음

@Test
fun onSystemBack_withPanelOpen_onlyClosesPanel() {
    // isBorderPanelOpen == false, showQuitDialog == false, effect 없음
}

@Test
fun onSystemBack_withPanelClosed_showsQuitDialog()

@Test
fun onQuitDialogConfirm_emitsQuitToCanvas()           // showQuitDialog == false

@Test
fun onQuitDialogCancel_hidesDialog()

@Test
fun confirmFailure_keepsPanelOpen() {
    // OnClickTopping → addToppingUseCase 실패 → isBorderPanelOpen == true, isLoading == false
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*CanvasToppingPlaceViewModelTest'`
Expected: FAIL — 미해결 참조

- [ ] **Step 3: ViewModel을 고친다**

- `observeDraft`는 테두리를 읽지 않는다.
- `handleOnToppingTransform`은 `isBorderPanelOpen`이면 상태를 그대로 돌려준다.
- `handleOnClickConfirm`은 `current.border.toToppingBorder()`를 쓴다. 파일 안의 `private fun toToppingBorder(colorArgb, widthDp)`는 지운다.
- `OnClickTopping`은 그림이 준비되지 않았으면(`isToppingImageReady == false`) 무시한다.
- 기존 `OnClickClose → NavigateBack` 연결은 `OnClickBack → NavigateBack`으로 옮긴다.

- [ ] **Step 4: 통과 확인**

Run: Step 2와 같은 명령
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add feature/groups/canvas/impl
git commit -m "feat: drive topping border from the place screen panel"
```

---

### Task 5: `CanvasToppingPlaceScreen`·`Route` — 새 화면 구성

**Files:**
- Modify: `C/screen/CanvasToppingPlaceScreen.kt`
- Modify: `C/route/CanvasToppingPlaceRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`
- Test: `CA/screen/CanvasToppingPlaceScreenTest.kt`

**Interfaces:**
- Consumes: Task 3 `ToppingArrangeLayout`, `ToppingBorderPanel`, `dismissPanelOnTouch`, `YGFloatingBarBackTitleClose`; Task 2 `panelFocusCenter`, `TOPPING_BORDER_WIDTH_RANGE_DP`; Task 4 상태·intent·effect
- Produces:
  ```kotlin
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

  /** 추가 플로우의 그만두기 팝업. 문구가 편집·배경 편집의 것과 다르다 */
  @Composable
  internal fun ToppingPlaceQuitDialog(onConfirmQuit: () -> Unit, onDismiss: () -> Unit)
  ```

- [ ] **Step 1: 그만두기 팝업 문자열을 더한다**

```xml
<string name="canvas_topping_place_quit_dialog_title">사진 편집을 그만둘까요?</string>
<string name="canvas_topping_place_quit_dialog_body">지금까지 진행한 내용은 저장되지 않아요.\n정말 그만두시겠어요?</string>
<string name="canvas_topping_place_quit_dialog_confirm">그만두기</string>
<string name="canvas_topping_place_quit_dialog_cancel">계속 편집</string>
```

- [ ] **Step 2: 실패하는 계측 테스트를 쓴다**

화면을 `CanvasToppingPlaceUiState`(초안 로드·그림 준비가 끝난 값, 토핑 그림은 `androidTest`의 리소스 drawable)로 직접 띄운다.

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
fun panelClosed_tapOnTopping_requestsOpen()          // onClickTopping 1회

@Test
fun panelClosed_tapOnEmptyCanvas_doesNothing()       // 어떤 콜백도 안 불림

@Test
fun panelOpen_touchOnCanvas_onlyDismisses() {
    // isBorderPanelOpen = true 로 띄우고 캔버스 위에서 드래그
    assertEquals(1, dismissCount)
    assertEquals(0, transformCount)
    assertEquals(0, clickToppingCount)
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasToppingPlaceScreenTest`
Expected: FAIL

- [ ] **Step 4: 화면을 고친다**

- 루트를 `ToppingArrangeLayout`으로 바꾼다. `header`는 `YGFloatingBarBackTitleClose`. 하단 `YGFloatingBarEdit`와 `60.dp`/`14.dp` 여백은 없앤다.
- `canvas` 슬롯에 지금의 두 박스(자르는 캔버스 박스, 자르지 않는 선택 박스 레이어)를 그대로 넣는다.
- 그릴 때 쓰는 중심을 하나 더 만든다.
  ```kotlin
  val focusFraction by animateFloatAsState(if (uiState.isBorderPanelOpen) 1f else 0f)
  val drawnCenter = lerp(center, panelFocusCenter(canvasSizeDp), focusFraction)
  ```
  토핑 그림과 `ToppingSelectionStroke`는 `drawnCenter`를 쓴다. `ToppingHitTarget`은 `center`를 그대로 쓴다.
- 입력 레이어는 패널 상태로 갈린다.
  - 닫힘: `.toppingTapInput(entries = 토핑 하나, onHit = onClickTopping, onMiss = {}).toppingTransformInput(...)`. `CanvasBGEditScreen`의 같은 조합과 순서를 맞춘다.
  - 열림: `.dismissPanelOnTouch(onDismissBorderPanel)`만.
- `panel` 슬롯에 `ToppingBorderPanel`을 `Alignment.BottomCenter`, `fillMaxWidth`로 놓는다. `selectedColorArgb = uiState.border?.colorArgb`, `widthDp = uiState.panelBorderWidthDp`, `widthRange = TOPPING_BORDER_WIDTH_RANGE_DP`.
- 테두리를 그리는 자리와 `borderWidthPx`는 `uiState.border`에서 읽는다.
- 화면 상단 KDoc의 "삭제·테두리 재편집 없음"은 지금 상태에 맞게 고친다.

- [ ] **Step 5: Route를 고친다**

- 새 콜백을 intent에 잇는다.
- `BackHandler { viewModel.processIntent(OnSystemBack) }`를 단다.
- `uiState.showQuitDialog`면 `ToppingPlaceQuitDialog`를 띄운다.
- `NavigateBack → navigator.onBack()`, `QuitToCanvas → navigator.popUpTo<NavKeyCanvasMain>()`.

- [ ] **Step 6: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest && ./gradlew :feature:groups:canvas:impl:testDebugUnitTest`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add feature/groups/canvas/impl
git commit -m "feat: rebuild topping place screen with border panel and quit dialog"
```

---

### Task 6: 초안에서 테두리 필드를 뺀다

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
- Test: `data/src/test/.../ToppingDraftLocalDataSourceImplTest.kt`, `ToppingDraftRepositoryImplTest.kt`, `domain/src/test/.../EnsureDraftSubjectRecordedUseCaseTest.kt`, `ST/viewmodel/SegmentationViewModelTest.kt`, `ST/viewmodel/SegmentationConfirmViewModelTest.kt`, `CT/viewmodel/CanvasToppingPlaceViewModelTest.kt`

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
  `ToppingDraft`·`ToppingDraftEntity`에는 `borderColorArgb`·`borderWidthDp`가 없다. `SegmentationConfirmUiState`에는 `borderColorArgb`·`borderWidthDp`·`borderLayers`가 없다. `SegmentationConfirmScreen`은 테두리 인자를 받지 않는다.

- [ ] **Step 1: 옛 초안을 읽는 테스트를 쓴다**

```kotlin
// ToppingDraftLocalDataSourceImplTest
@Test
fun read_legacyDraftWithBorderFields_keepsTheRest() {
    // DataStore 에 옛 형식 JSON 을 직접 써 넣는다:
    // {"groupId":1,"parfaitId":2,"nextPositionZ":3,
    //  "subjectImagePath":"/s.png","borderColorArgb":-16777216,"borderWidthDp":10.0}
    // 읽은 엔티티: groupId == 1, parfaitId == 2, nextPositionZ == 3, subjectImagePath == "/s.png"
}
```

JSON의 키 이름은 `ToppingDraftEntity`의 실제 직렬 이름에 맞춘다.

- [ ] **Step 2: 통과 확인(현재 코드)**

Run: `./gradlew :data:testDebugUnitTest --tests '*ToppingDraftLocalDataSourceImplTest'`
Expected: PASS — 필드를 빼기 전이라 통과한다. 이 테스트는 다음 단계 뒤에도 통과해야 하는 기준이다.

- [ ] **Step 3: 필드와 인자를 뺀다**

- domain·data의 일곱 파일에서 `borderColorArgb`·`borderWidthDp`를 뺀다. `ToppingDraft` KDoc의 "이미지와 테두리가 빈 초안"은 지금 상태에 맞게 고친다.
- `ToppingEditDraft.kt`의 `recordEditResult`는 `result.borderLayers`를 읽지 않는다.
- `SegmentationViewModel`·`EnsureDraftSubjectRecordedUseCase`의 `= null` 인자 두 줄을 뺀다.
- `SegmentationConfirmViewModel`은 초안에서 테두리를 읽지 않고 상태에서 세 필드를 뺀다. `SegmentationConfirmScreen`의 `YGToppingCutoutImage`는 `borderColor = null`, `borderWidth = 0.dp`로 그린다.
- `SegmentationConfirmRoute`가 `NavKeyToppingEdit`에 넘기던 `borderLayers = uiState.borderLayers`는 뺀다(기본값 `emptyList()`).

- [ ] **Step 4: 테스트를 고친다**

테두리 인자·단언을 걷는다. `CanvasToppingPlaceViewModelTest.draft()` 헬퍼의 두 인자도 뺀다.

- [ ] **Step 5: 전체 단위 테스트 확인**

Run: `./gradlew :domain:test :data:testDebugUnitTest :feature:segmentation:impl:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest`
Expected: PASS. Step 1의 테스트도 PASS.

- [ ] **Step 6: Commit**

```bash
git add domain data feature
git commit -m "refactor: drop border fields from the topping draft"
```

---

### Task 7: 편집 화면의 테두리 탭을 숨기고 확인 화면을 맞춘다

**Files:**
- Modify: `S/screen/ToppingEditScreen.kt`
- Modify: `S/viewmodel/ToppingEditViewModel.kt`
- Modify: `S/viewmodel/SegmentationConfirmViewModel.kt`, `S/screen/SegmentationConfirmScreen.kt`, `S/route/SegmentationConfirmRoute.kt`
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`
- Test: `ST/viewmodel/ToppingEditViewModelTest.kt`, `ST/viewmodel/SegmentationConfirmViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  // SegmentationConfirmUiState
  /** 원본이 있어 영역을 고칠 수 있을 때만 "사진 편집"을 보인다 */
  val canEditPhoto: Boolean get() = sourceImageUri != null
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
fun borderOnly_stillOpensOnBorderTab() {
    // borderOnly = true → tab == BORDER (C-301 왕복이 계획 B 까지 쓴다)
}

// SegmentationConfirmViewModelTest
@Test
fun canEditPhoto_isFalse_withoutSourceImage()

@Test
fun tutorial_isHidden_whenPhotoCannotBeEdited() {
    // sourceImageUri = null, getTutorialVisibleFlowUseCase 가 true 를 흘려도
    assertFalse(state.isTutorialVisible)
    // completeTutorialUseCase 는 불리지 않는다
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

- `SegmentationConfirmScreen`: `canEditPhoto == false`면 "사진 편집" 버튼을 그리지 않고 "다음" 버튼이 줄 전체를 차지한다.
- `SegmentationConfirmViewModel`: `isTutorialVisible`은 `canEditPhoto && 흐름 값`이다.
- 튜토리얼 문자열을 고친다.
  ```xml
  <string name="segmentation_confirm_tutorial_title">영역 수정</string>
  <string name="segmentation_confirm_tutorial_description">하단 사진 편집 버튼을 통해\n완벽히 편집되지 않은 배경 영역을 수정할 수 있어요</string>
  ```
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

### Task 8: 검증과 문서

**Files:**
- Modify: `docs/status.md`
- Modify: `docs/synthesis/open-questions.md`
- Create: `docs/adr/0034-topping-border-set-at-placement.md`
- Modify: `docs/adr/README.md`, `docs/log.md`

- [ ] **Step 1: 전체 검증**

Run: `./gradlew ktlintCheck testDebugUnitTest :feature:groups:canvas:impl:connectedDebugAndroidTest assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 에뮬레이터에서 피그마와 대조한다**

| 확인 | 기대 |
|---|---|
| 촬영 → 누끼 → 확인 → 배치 | 헤더 뒤로·"배치"·닫기, 접힌 패널, "캔버스에 쌓기" (피그마 `5453:10418`) |
| 접힌 바 탭 / 토핑 탭 | 패널이 열리고 토핑이 가운데 위로 움직인다 (피그마 `5461:9261`) |
| 색·굵기 변경 | 토핑 테두리가 바로 바뀐다 |
| 열린 채 캔버스 드래그 | 패널만 닫히고 토핑은 원래 자리로 돌아온다 |
| 열린 채 "캔버스에 쌓기" | 캔버스 메인에서 토핑이 원래 자리에 테두리와 함께 보인다 |
| 헤더 뒤로 | 팝업 없이 확인 화면 |
| 헤더 닫기 / 시스템 뒤로가기 | 그만두기 팝업 → 확인 시 캔버스 메인 |
| 패널이 열린 채 시스템 뒤로가기 | 패널만 닫힌다 |
| 확인 화면 "사진 편집" | 탭 없이 영역 수정만 열린다 |
| 갤러리 최근 줄의 알맹이로 진입 | 확인 화면에 "사진 편집" 버튼이 없다 |
| 캔버스 메인 → 본인 토핑 탭 → 토핑 탭의 연필 | 테두리 편집이 지금처럼 열리고 저장된다 (C-301 회귀 확인) |

화면 높이가 다른 기기 두 대(작은 것, 큰 것)에서 패널이 열렸을 때 토핑이 패널에 가려지는지 본다. 가려지면 고치지 않고 Step 4에서 미결 항목으로 적는다.

- [ ] **Step 3: `docs/status.md`를 덮어쓴다**

"토핑 생성·배치" 영역의 상태 문단·앵커·⚠️ 줄을 지금 코드에 맞춘다. 테두리가 배치 화면 패널에서 정해진다는 것, 초안에 테두리가 없다는 것, 편집 화면의 테두리 탭은 C-301 진입에만 남아 있다는 것을 적는다. 변경 서술("~에서 ~로 바뀌었다")은 쓰지 않는다.

- [ ] **Step 4: 미결 항목과 ADR**

- `open-questions.md`: OQ-P-081 ②, OQ-P-203 ③을 바뀐 사용처로 고쳐 쓴다. Step 2에서 가림을 봤다면 새 항목을 연다. OQ-P-324·338은 닫지 않는다 — 계획 B의 몫이다.
- ADR-0034: 테두리는 편집이 아니라 배치 단계의 속성이다. ADR-0026의 프로세스 사망 복원 보장에서 테두리가 빠진다는 것과 그 이유(고르는 데 몇 초 걸리는 값이고 헤더의 뒤로로 돌아갈 때도 버린다)를 적는다.
- `docs/log.md`에 한 줄.

- [ ] **Step 5: 링크 확인과 커밋**

Run: `python3 docs/script/check_links.py docs`
Expected: 깨진 링크 0건

```bash
git add docs
git commit -m "docs: record the placement-stage border panel"
```
