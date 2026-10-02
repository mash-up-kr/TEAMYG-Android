---
id: c105-arrange-edit-flow
title: 배치 화면 테두리 패널 — 수정 플로우 (계획 B)
status: draft
type: work-order
created: 2026-10-02
updated: 2026-10-02
platforms: android
owner: android
related_adr: ADR-0025, ADR-0026
related_spec: c105-arrange-border-merge
related_code:
  - CanvasBGEditViewModel.kt#CanvasBGEditViewModel
  - CanvasBGEditScreen.kt#CanvasBGEditScreen
  - CanvasMainViewModel.kt#handleOnClickMyTopping
  - NavKeyToppingEdit.kt#NavKeyToppingEdit
  - ToppingEditViewModel.kt#ToppingEditViewModel
archived_reason:
tags: [plan, parfait]
---

# 배치 화면 테두리 패널 — 수정 플로우 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 이미 쌓은 본인 토핑을 고치는 흐름을 토핑 전용 화면으로 떼어 내 배치 화면과 같은 테두리 패널을 쓰게 하고, 남은 테두리 편집 코드를 전부 지운다.

**Architecture:** `CanvasBGEditViewModel`의 토핑 부분을 새 `CanvasToppingArrangeViewModel`로 옮기고 `CanvasBGEdit`는 배경만 다룬다. 토핑 한 장의 테두리는 겹 목록이 아니라 단일 값(`ToppingBorderStyle?`)이다. 화면은 계획 A가 만든 `ToppingArrangeLayout`·`ToppingBorderPanel`·`dismissPanelOnTouch`·`panelFocusCenter`를 그대로 쓴다. 마지막에 `NavKeyToppingEdit`의 테두리 인자와 편집 화면의 테두리 탭 코드를 삭제한다.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation3, Hilt(assisted), JUnit4 + MockK + Turbine + coroutines-test, Compose UI 계측 테스트.

**Spec:** [`docs/superpowers/specs/2026-10-02-c105-arrange-border-merge-design.md`](../specs/2026-10-02-c105-arrange-border-merge-design.md)

**선행:** [계획 A](2026-10-02-c105-arrange-add-flow.md)가 머지돼 있어야 한다.

## Global Constraints

- 코드 주석·KDoc은 [`docs/code-conventions.md`](../../code-conventions.md)를 따른다. 구현·리뷰 서브에이전트 브리프에 이 링크를 넣는다.
- 경로 약어는 계획 A와 같다(`C`, `CT`, `CA`, `S`, `ST`, `DS`). 더해서 `CAPI` = `feature/groups/canvas/api/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/api`, `SAPI` = `feature/segmentation/api/src/main/java/com/teamyg/parfait/feature/segmentation/api`.
- 검증 명령: 단위 `./gradlew :feature:groups:canvas:impl:testDebugUnitTest`, 계측 `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest`, 스타일 `./gradlew ktlintCheck`.
- 모든 Task는 끝났을 때 전체 빌드가 통과해야 한다.
- 좌표는 캔버스 대비 비율을 유지한다. 저장 경로(`UpdateToppingsUseCase`, `UpdateToppingBorderUseCase`, `DeleteToppingUseCase`)와 서버 계약은 바꾸지 않는다.
- 삭제 동작은 바꾸지 않는다 — 모달 확인 → 서버 삭제 → 오늘 캔버스 갱신 → 캔버스 메인으로 돌아간다.
- 닫기와 시스템 뒤로가기의 그만두기 팝업은 변경 여부와 무관하게 뜬다. 패널이 열려 있을 때의 시스템 뒤로가기만 패널을 닫는다.
- 지난 캔버스에서는 본인 토핑을 탭해도 반응이 없다(`isViewingToday` 가드 유지).
- 문구
  - 화면 제목: "배치" / 하단 버튼: "캔버스에 쌓기"
  - 남의 토핑 탭 토스트: "다른 사람의 사진은 편집할 수 없어요"
  - 그만두기 팝업: 지금 `canvas_bg_edit_quit_dialog_*`의 값 그대로("편집을 그만둘까요?")
- 분석 화면 id: `NavKeyCanvasToppingArrange` → `"C-305"`, `NavKeyCanvasBGEdit` → `"C-301"`, `NavKeyToppingEdit` → `"C-104"`.

## Review Focus

1. 패널이 열린 동안 폴링이 포커스된 토핑을 목록에서 뺀다 — 패널이 닫히고 포커스가 풀려야 하며, 없는 토핑에 PATCH가 나가면 안 된다. (Task 3 테스트)
2. 패널에서 색을 바꿨다가 원래 색으로 되돌리고 확정한다 — 테두리 PATCH가 나가지 않아야 한다. (Task 3 테스트)
3. 서버의 테두리 색이 팔레트 9색에 없는 토핑을 포커스하고 굵기만 바꾼다 — 색은 그대로 두고 굵기만 바뀌어 저장돼야 한다. (Task 3 테스트)
4. 변형 PATCH는 성공하고 테두리 PATCH만 실패한다 — 화면에 남고 실패한 토핑만 다시 보낼 대상으로 남아야 한다. (Task 3 테스트)
5. 포커스된 토핑이 가운데 자리에 있는 다른 본인 토핑보다 깊이가 낮다 — 패널이 열린 동안에는 포커스된 토핑이 위에 보여야 한다. (Task 4 계측 테스트)

---

### Task 1: 토핑 한 장의 테두리를 단일 값으로 바꾼다

**Files:**
- Create: `C/util/EditableTopping.kt`
- Modify: `C/viewmodel/CanvasBGEditViewModel.kt`
- Modify: `C/screen/CanvasBGEditScreen.kt`
- Modify: `C/route/CanvasBGEditRoute.kt`
- Test: `CT/viewmodel/CanvasBGEditViewModelTest.kt`

**Interfaces:**
- Consumes: 계획 A `ToppingBorderStyle`, `toToppingBorder`, `toToppingBorderStyleOrNull`
- Produces:
  ```kotlin
  /** 편집 화면이 다루는 토핑 하나. 위치·크기는 Canvas-Area 대비 비율이다 */
  internal data class EditableTopping(
      val parfaitImageId: Long,
      val isMine: Boolean,
      val imageUrl: String,
      val positionX: Float,
      val positionY: Float,
      val scale: Float = 1f,
      val rotationDegrees: Float = 0f,
      val border: ToppingBorderStyle? = null,
      val editedImagePath: String? = null,
      val cutoutImagePath: String? = null,
  )

  internal fun CanvasToppingVO.toEditableTopping(): EditableTopping
  ```
  `CanvasToppingItem`은 사라진다. `editedImagePath`·`cutoutImagePath`는 Task 5에서 뺀다 — 테두리 편집 왕복이 그때까지 쓴다.

- [ ] **Step 1: 테스트의 타입과 테두리 단언을 새 형태로 바꾼다**

`CanvasBGEditViewModelTest`의 `CanvasToppingItem` → `EditableTopping`, `borderLayers = listOf(layer)` → `border = ToppingBorderStyle(...)`, `borderLayers.isEmpty()` → `border == null`. 테스트 이름과 검증 대상은 그대로다.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*CanvasBGEditViewModelTest'`
Expected: FAIL — 미해결 참조

- [ ] **Step 3: 타입을 옮기고 호출부를 고친다**

- `CanvasToppingItem`의 KDoc은 `EditableTopping`으로 옮긴다.
- `toBorderLayers`·`List<ToppingBorderLayer>.toToppingBorder`는 지우고 계획 A의 변환을 쓴다.
- `hasBorderChange`는 `border != original.border`.
- 편집 화면 왕복의 경계에서만 겹 목록과 바꾼다: 보낼 때 `listOfNotNull(border?.let { ToppingBorderLayer(it.colorArgb, it.widthDp) })`, 받을 때 `result.borderLayers.lastOrNull()?.let { ToppingBorderStyle(it.colorArgb, it.widthDp) }`.
- `CanvasBGEditScreen`의 `borderLayers.firstOrNull()` 두 곳은 `border`를 읽는다.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add feature/groups/canvas/impl
git commit -m "refactor: hold a single border style per editable topping"
```

---

### Task 2: 토핑 그리기 부품을 화면 밖으로 뺀다

**Files:**
- Create: `C/component/EditableToppingLayer.kt`
- Modify: `C/screen/CanvasBGEditScreen.kt`

**Interfaces:**
- Consumes: Task 1 `EditableTopping`
- Produces (`CanvasBGEditScreen.kt`의 `private` 선언을 이름만 바꿔 `internal`로 옮긴다):
  ```kotlin
  internal val EditableTopping.drawnModel: String

  internal data class EditableToppingDrawEntry(
      val topping: EditableTopping,
      val painter: AsyncImagePainter,
      val center: DpOffset,
      val size: DpSize,
      val drawnBorderWidthDp: Float,
  )

  internal data class EditableToppingHitEntry(
      val draw: EditableToppingDrawEntry,
      val target: ToppingHitTarget,
  )

  @Composable
  internal fun rememberEditableToppingDrawEntries(
      toppings: List<EditableTopping>,
      canvasWidth: Dp,
      canvasHeight: Dp,
  ): List<EditableToppingDrawEntry>

  @Composable
  internal fun rememberEditableToppingHitEntries(
      drawEntries: List<EditableToppingDrawEntry>,
      outlines: Map<String, ToppingOutline>,
  ): List<EditableToppingHitEntry>

  /** @param centerOverride 패널이 열린 동안 그릴 자리. null 이면 저장된 배치대로 그린다 */
  @Composable
  internal fun EditableToppingImage(
      entry: EditableToppingDrawEntry,
      outline: ToppingOutline?,
      alpha: Float,
      onClick: (() -> Unit)?,
      modifier: Modifier = Modifier,
      centerOverride: DpOffset? = null,
  )

  /** 점선 선택 박스와 좌상단 삭제 버튼. [onClickEdit] 이 null 이면 연필 버튼을 그리지 않는다 */
  @Composable
  internal fun ToppingFocusDecoration(
      entry: EditableToppingHitEntry,
      center: DpOffset,
      onClickDelete: () -> Unit,
      onClickEdit: (() -> Unit)?,
      showActionButtons: Boolean,
      modifier: Modifier = Modifier,
  )
  ```

- [ ] **Step 1: 선언을 옮긴다**

`BGEditDrawEntry` → `EditableToppingDrawEntry`, `BGEditHitEntry` → `EditableToppingHitEntry`, `rememberBGEdit*` → `rememberEditableTopping*`, `CanvasToppingImage` → `EditableToppingImage`, `ToppingCornerButtons` → `ToppingFocusDecoration`. 본문과 KDoc은 그대로 옮긴다.

- `ToppingFocusDecoration`은 중심을 스스로 계산하지 않고 `center`로 받는다. `CanvasBGEditScreen`은 지금처럼 `toppingCenter(...)`를 넘긴다.
- `EditableToppingImage`는 `centerOverride ?: entry.center`에 놓는다.

- [ ] **Step 2: `CanvasBGEditScreen`이 옮긴 선언을 쓰게 한다**

`onClickEdit`은 지금처럼 넘긴다(연필 버튼은 Task 5에서 사라진다).

- [ ] **Step 3: 회귀 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest :feature:groups:canvas:impl:connectedDebugAndroidTest`
Expected: PASS — 동작 변화가 없는 이동이다

- [ ] **Step 4: Commit**

```bash
git add feature/groups/canvas/impl
git commit -m "refactor: extract editable topping drawing into a shared component"
```

---

### Task 3: `CanvasToppingArrangeViewModel`

**Files:**
- Create: `C/viewmodel/CanvasToppingArrangeViewModel.kt`
- Create: `C/viewmodel/CanvasToppingArrangeError.kt`
- Test: `CT/viewmodel/CanvasToppingArrangeViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 `EditableTopping`, `toEditableTopping`; 계획 A `ToppingBorderStyle`, `DEFAULT_TOPPING_BORDER_WIDTH_DP`, `TOPPING_BORDER_WIDTH_RANGE_DP`, `toToppingBorder`
- Produces:
  ```kotlin
  data class CanvasToppingArrangeUiState(
      val backgroundColor: Color = YGAtomicColors.Gray.White,
      val backgroundImageUrl: String? = null,
      val toppings: List<EditableTopping> = emptyList(),
      val focusedToppingId: Long? = null,
      val pendingBorderWidthDp: Float = DEFAULT_TOPPING_BORDER_WIDTH_DP,
      val isBorderPanelOpen: Boolean = false,
      val showQuitDialog: Boolean = false,
      val showDeleteToppingDialog: Boolean = false,
      val dirtyToppingIds: Set<Long> = emptySet(),
      val deletedToppingIds: Set<Long> = emptySet(),
      val isLoading: Boolean = false,
  ) : UiState {
      val focusedTopping: EditableTopping?
      val panelBorderColorArgb: Int? get() = focusedTopping?.border?.colorArgb
      val panelBorderWidthDp: Float get() = focusedTopping?.border?.widthDp ?: pendingBorderWidthDp
      val canOpenBorderPanel: Boolean get() = focusedToppingId != null
  }

  sealed interface CanvasToppingArrangeIntent : UiIntent {
      data class OnClickTopping(val topping: EditableTopping)
      data object OnClickEmptyCanvas
      data object OnToggleBorderPanel
      data object OnDismissBorderPanel
      data class OnSelectBorderColor(val colorArgb: Int?)
      data class OnChangeBorderWidth(val widthDp: Float)
      data class OnToppingTransform(val panX: Float, val panY: Float, val zoom: Float, val rotationDelta: Float)
      data object OnClickDeleteToppingButton
      data object OnDeleteToppingDialogConfirm
      data object OnDeleteToppingDialogCancel
      data object OnClickClose
      data object OnSystemBack
      data object OnQuitDialogConfirm
      data object OnQuitDialogCancel
      data object OnClickConfirm
  }

  sealed interface CanvasToppingArrangeEffect : UiSideEffect {
      data object NavigateBack
      data object ShowOthersToppingNotEditable
      data class ShowError(val error: CanvasToppingArrangeError)
  }

  enum class CanvasToppingArrangeError { NETWORK, TOPPING_SAVE_UNKNOWN, TOPPING_DELETE_UNKNOWN }

  @HiltViewModel(assistedFactory = CanvasToppingArrangeViewModel.Factory::class)
  class CanvasToppingArrangeViewModel @AssistedInject constructor(
      @Assisted("groupId") groupIdValue: Long,
      @Assisted("parfaitId") parfaitIdValue: Long,
      @Assisted("initialToppingId") initialToppingId: Long,
      getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
      refreshTodayParfaitDetailUseCase: RefreshTodayParfaitDetailUseCase,
      deleteToppingUseCase: DeleteToppingUseCase,
      updateToppingsUseCase: UpdateToppingsUseCase,
      updateToppingBorderUseCase: UpdateToppingBorderUseCase,
  )
  ```

- [ ] **Step 1: 실패하는 테스트를 쓴다**

테스트 틀(오늘 캔버스 흐름, 구독 유지 헬퍼, 유스케이스 mock)은 `CanvasBGEditViewModelTest`의 것을 따른다. 기본 캔버스는 본인 토핑 둘(id 1, 2)과 남의 토핑 하나(id 3)이고 `initialToppingId = 1`이다.

```kotlin
// 진입
@Test fun firstEmission_focusesInitialTopping()                 // focusedToppingId == 1L

// 탭 네 갈래
@Test fun clickFocusedTopping_opensPanel()                      // isBorderPanelOpen == true
@Test fun clickOtherOwnTopping_movesFocus()                     // focusedToppingId == 2L, 패널 닫힘
@Test fun clickOthersTopping_showsToastAndKeepsFocus() {
    // effect == ShowOthersToppingNotEditable, focusedToppingId == 1L
}
@Test fun clickEmptyCanvas_clearsFocus()                        // focusedToppingId == null

// 패널
@Test fun togglePanel_withoutFocus_staysClosed()
@Test fun panelValues_followFocusedTopping() {
    // 토핑 1: border = (black, 8f), 토핑 2: border = null, pending 기본값
    // 포커스 1 → panelBorderColorArgb == black, panelBorderWidthDp == 8f
    // 포커스 2 → panelBorderColorArgb == null, panelBorderWidthDp == 10f
}
@Test fun changeWidth_withoutBorder_keepsPendingAcrossFocusChange() {
    // 포커스 2 → OnChangeBorderWidth(16f) → 포커스 1 → 포커스 2
    // panelBorderWidthDp == 16f, 토핑 2 는 dirty 가 아니다
}
@Test fun selectColor_setsBorderOnFocusedToppingAndMarksDirty() {
    // toppings[1].border == ToppingBorderStyle(color, 10f), 1 in dirtyToppingIds
}
@Test fun selectNone_removesBorderAndMarksDirty()
@Test fun changeWidth_onOffPaletteColor_keepsColor() {
    // 서버 색 0xFF123456 → OnChangeBorderWidth(12f)
    // border == ToppingBorderStyle(0xFF123456.toInt(), 12f)
}
@Test fun changeWidth_isClampedToRange()

// 패널이 열린 동안
@Test fun transform_whilePanelOpen_isIgnored()                  // 위치 불변, dirty 아님
@Test fun focusedToppingRemovedByPolling_closesPanelAndClearsFocus() {
    // 패널을 연 뒤 토핑 1 이 빠진 캔버스를 방출
    // isBorderPanelOpen == false, focusedToppingId == null, 1 !in dirtyToppingIds
}

// 변형
@Test fun transform_appliesToFocusedToppingAndMarksDirty()
@Test fun transform_withoutFocus_isIgnored()
@Test fun transform_clampsAtMinScale()                          // TOPPING_MIN_SCALE

// 닫기
@Test fun clickClose_withoutChanges_stillShowsQuitDialog()      // showQuitDialog == true
@Test fun systemBack_withPanelOpen_onlyClosesPanel()
@Test fun systemBack_withPanelClosed_showsQuitDialog()
@Test fun quitDialogConfirm_emitsNavigateBack()

// 확정
@Test fun confirm_sendsOnlyChangedAxes() {
    // 토핑 1 은 옮기기만, 토핑 2 는 색만 바꾼다
    // updateToppingsUseCase 는 토핑 1 만, updateToppingBorderUseCase 는 토핑 2 만 받는다
}
@Test fun confirm_colorChangedAndReverted_sendsNoBorderPatch()
@Test fun confirm_whilePanelOpen_sendsStoredPosition()
@Test fun confirm_success_refreshesThenNavigatesBack() {
    // coVerifyOrder { updateToppingsUseCase(...); refreshTodayParfaitDetailUseCase(...) }
    // 그 뒤 effect == NavigateBack
}
@Test fun confirm_nothingDirty_refreshesAndNavigatesBack()
@Test fun confirm_borderPatchFails_staysAndKeepsOnlyFailedDirty() {
    // 토핑 1 변형 성공, 토핑 2 테두리 실패
    // effect == ShowError(TOPPING_SAVE_UNKNOWN), dirtyToppingIds == setOf(2L), NavigateBack 없음
}

// 삭제
@Test fun deleteConfirm_success_refreshesThenNavigatesBack()
@Test fun deleteConfirm_failure_showsErrorAndStays()
```

`CanvasBGEditViewModelTest`에 있는 토핑 병합·툼스톤·dirty 케이스(폴링이 dirty 토핑을 덮지 않음, 지운 토핑이 되살아나지 않음, 서버에서 사라진 토핑이 두 집합에서 빠짐)는 이 파일로 **복제**한다. 원본에서 지우는 것은 Task 5다.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*CanvasToppingArrangeViewModelTest'`
Expected: FAIL — 미해결 참조

- [ ] **Step 3: ViewModel을 구현한다**

`CanvasBGEditViewModel`에서 아래를 옮겨 온다. 본문과 주석은 그대로 쓰고 이름만 새 상태에 맞춘다.

| 옮기는 것 | 달라지는 점 |
|---|---|
| `observeCanvas`, `hasSeededFromCanvas`, `parfaitId` 이전 규칙 | 배경은 방출마다 갱신한다(이 화면은 배경을 고르지 않는다). 최초 방출에만 `focusedToppingId = initialToppingId` |
| `mergeToppings` | 포커스가 풀리면 `isBorderPanelOpen = false`도 함께 |
| `serverToppings`, `hasTransformChange`, `hasBorderChange`, `updateDirtyToppings`, `saveTransforms`, `saveBorder`, `toTransformUpdate` | 그대로 |
| `handleOnDeleteToppingDialogConfirm`, `failToDeleteTopping` | 오류 타입만 새 enum |
| `handleOnToppingTransform`, `applyToppingTransform`, `markDirty` | `isBorderPanelOpen`이면 무시 |

새로 쓰는 것:

- `OnClickTopping`: `isMine == false`면 `ShowOthersToppingNotEditable`. 포커스된 토핑이면 패널을 연다. 다른 본인 토핑이면 포커스를 옮긴다.
- `OnSelectBorderColor`·`OnChangeBorderWidth`: 포커스가 없으면 무시. 규칙은 스펙 「패널 > 값을 바꾸면」.
- `OnClickConfirm`: `updateDirtyToppings()` → 실패가 없으면 `refreshTodayParfaitDetailUseCase`를 **기다린 뒤** `NavigateBack`. 실패가 있으면 `dirtyToppingIds`를 실패분으로 줄이고 `ShowError(TOPPING_SAVE_UNKNOWN)`.
- `Throwable.toCanvasToppingArrangeError(unknown)`는 `toCanvasBGEditError`와 같은 규칙으로 `NETWORK`만 가른다.

- [ ] **Step 4: 통과 확인**

Run: Step 2와 같은 명령
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add feature/groups/canvas/impl
git commit -m "feat: add CanvasToppingArrangeViewModel"
```

---

### Task 4: 수정 화면·Route·네비게이션

**Files:**
- Create: `CAPI/NavKeyCanvasToppingArrange.kt`
- Create: `C/screen/CanvasToppingArrangeScreen.kt`
- Create: `C/route/CanvasToppingArrangeRoute.kt`
- Modify: `C/navigation/EntryBuilder.kt`
- Modify: `C/viewmodel/CanvasMainViewModel.kt`, `C/route/CanvasMainRoute.kt`
- Modify: `app/src/main/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreen.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`
- Test: `CA/screen/CanvasToppingArrangeScreenTest.kt`
- Test: `CT/viewmodel/CanvasMainViewModelTest.kt`
- Test: `app/src/test/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreenTest.kt`

**Interfaces:**
- Consumes: Task 2 그리기 부품, Task 3 ViewModel; 계획 A `ToppingArrangeLayout`, `ToppingBorderPanel`, `dismissPanelOnTouch`, `panelFocusCenter`, `YGFloatingBarTitle`
- Produces:
  ```kotlin
  @Serializable
  data class NavKeyCanvasToppingArrange(
      val groupId: Long,
      val parfaitId: Long,
      val initialToppingId: Long,
  ) : NavKey

  // CanvasMainEffect
  data class NavigateToToppingArrange(
      val groupId: GroupId,
      val parfaitId: ParfaitId,
      val toppingId: ParfaitImageId,
  ) : CanvasMainEffect
  ```
  `CanvasMainEffect.NavigateToCanvasBGEdit`에서 `toppingId`를 뺀다.

- [ ] **Step 1: 문자열을 더한다**

```xml
<string name="canvas_topping_arrange_others_not_editable">다른 사람의 사진은 편집할 수 없어요</string>
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

```kotlin
// CanvasMainViewModelTest — 기존 본인 토핑 탭 테스트의 기대 effect 를 바꾼다
@Test
fun clickMyTopping_onToday_emitsNavigateToToppingArrange() {
    // effect == NavigateToToppingArrange(groupId, parfaitId, toppingId)
}

// NavKeyAnalyticsScreenTest
@Test
fun toAnalyticsScreenOrNull_toppingArrange_isC305() {
    assertEquals(
        "C-305",
        NavKeyCanvasToppingArrange(groupId = 1L, parfaitId = 2L, initialToppingId = 3L)
            .toAnalyticsScreenOrNull()?.screenId,
    )
}
// 기존 toAnalyticsScreenOrNull_canvasBGEdit_splitsByInitialToppingId 는
// NavKeyCanvasBGEdit(1L, 2L) == "C-301" 하나만 남긴다

// CanvasToppingArrangeScreenTest — 본인 토핑 둘(하나는 포커스), 남의 토핑 하나로 화면을 직접 띄운다
@Test
fun header_showsTitleAndCloseOnly() {
    onNodeWithText("배치").assertIsDisplayed()
    onNodeWithContentDescription("닫기").assertIsDisplayed()
    onNodeWithContentDescription("뒤로가기").assertDoesNotExist()
}

@Test
fun focusedTopping_showsDeleteButton_andNoEditButton() {
    onNodeWithContentDescription("삭제").assertIsDisplayed()
    onNodeWithContentDescription("편집").assertDoesNotExist()
}

@Test
fun tapOnOthersTopping_reportsThatTopping()          // onClickTopping(남의 토핑)

@Test
fun tapOnEmptyCanvas_reportsEmpty()                  // onClickEmptyCanvas 1회

@Test
fun panelOpen_touchAnywhereOnCanvas_onlyDismisses() {
    // isBorderPanelOpen = true. 다른 본인 토핑 위, 남의 토핑 위, 삭제 버튼 위를 차례로 누른다
    // dismiss 3회, onClickTopping 0회, onClickDelete 0회
}

@Test
fun panelOpen_focusedToppingIsDrawnLast() {
    // 포커스 토핑의 깊이가 다른 본인 토핑보다 낮게 주어져도
    // 본인 토핑 노드들 중 포커스 토핑이 마지막 순서로 놓인다
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest :app:testDebugUnitTest --tests '*NavKeyAnalyticsScreenTest'`
Expected: FAIL

- [ ] **Step 4: 화면을 만든다**

`ToppingArrangeLayout`을 루트로 쓴다. `header`는 `YGFloatingBarTitle`. `canvas` 슬롯은 `CanvasBGEditScreen`의 토핑 탭 갈래를 옮겨 온 것이다.

- 그리는 순서: 배경 → 남의 토핑 → 딤(`Transparency.Black25`) → 본인 토핑 → 입력 레이어 → `ToppingFocusDecoration`.
- 패널이 열려 있으면 본인 토핑 중 포커스된 것을 맨 뒤로 보내 그린다. 저장되는 깊이는 건드리지 않는다.
- 포커스된 토핑과 `ToppingFocusDecoration`의 중심은 `lerp(저장된 중심, panelFocusCenter(canvasSize), focusFraction)`이다. `focusFraction`은 계획 A의 배치 화면과 같은 `animateFloatAsState`다.
- 입력 레이어(패널 닫힘):
  ```kotlin
  .toppingTapInput(
      entries = { (othersEntries + myEntries).map { it.topping to it.target } },  // 아래에서 위 순서
      keyOf = { it.parfaitImageId },
      onHit = onClickTopping,
      onMiss = onClickEmptyCanvas,
  ).toppingTransformInput(targetAt = { focusedEntry?.target }, ...)
  ```
- 입력 레이어(패널 열림): `.dismissPanelOnTouch(onDismissBorderPanel)`만. 이때는 이 레이어를 `ToppingFocusDecoration`보다 **위에** 둔다 — 삭제 버튼은 보이지만 그 위를 눌러도 입력 레이어가 먼저 받아 패널만 닫힌다.
- `ToppingFocusDecoration`의 `onClickEdit`은 null.
- 본인 토핑의 접근성 클릭(`EditableToppingImage.onClick`)은 `onClickTopping`, 남의 토핑은 null.
- 패널: `isEnabled = uiState.canOpenBorderPanel`, `selectedColorArgb = uiState.panelBorderColorArgb`, `widthDp = uiState.panelBorderWidthDp`.
- 그만두기 팝업과 삭제 모달은 `CanvasBGEditScreen`의 것을 같은 문자열로 띄운다.

- [ ] **Step 5: Route·entry·진입을 잇는다**

- `CanvasToppingArrangeRoute`: `YGScaffoldV2(isLoading, toastPolicy)`, `BackHandler → OnSystemBack`, effect 처리(`NavigateBack → navigator.onBack()`, `ShowOthersToppingNotEditable → toastPolicy.showError(문구)`, `ShowError → 문구`). 오류 문구는 `canvas_bg_edit_save_error_network`, `canvas_bg_edit_topping_save_error_unknown`, `canvas_bg_edit_topping_delete_error_unknown`을 그대로 쓴다.
- `EntryBuilder`에 `entry<NavKeyCanvasToppingArrange>`를 더한다. `CanvasBGEdit`과 같은 이유로 바깥 Scaffold를 씌우지 않는다.
- `CanvasMainViewModel.handleOnClickMyTopping`이 `NavigateToToppingArrange`를 낸다. `isViewingToday` 가드는 그대로다. KDoc의 "편집 화면이 토핑 탭에서…"는 지금 상태에 맞게 고친다.
- `CanvasMainRoute`가 `NavKeyCanvasToppingArrange`로 보낸다.
- `NavKeyAnalyticsScreen`에 `is NavKeyCanvasToppingArrange -> AnalyticsScreen("C-305", "NavKeyCanvasToppingArrange")`. `NavKeyCanvasBGEdit`는 언제나 `"C-301"`.

`NavKeyCanvasBGEdit.initialToppingId`와 `CanvasBGEdit`의 토핑 탭은 이 Task에서 지우지 않는다. 아무도 값을 넘기지 않는 인자로 남고, Task 5가 걷는다.

- [ ] **Step 6: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest :feature:groups:canvas:impl:connectedDebugAndroidTest :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add app feature/groups/canvas
git commit -m "feat: open own toppings in the arrange screen"
```

---

### Task 5: `CanvasBGEdit`를 배경 전용으로 줄인다

**Files:**
- Modify: `CAPI/NavKeyCanvasBGEdit.kt`
- Modify: `C/viewmodel/CanvasBGEditViewModel.kt`, `C/viewmodel/CanvasBGEditError.kt`
- Modify: `C/screen/CanvasBGEditScreen.kt`, `C/route/CanvasBGEditRoute.kt`, `C/navigation/EntryBuilder.kt`
- Modify: `C/util/EditableTopping.kt`, `C/component/EditableToppingLayer.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`
- Test: `CT/viewmodel/CanvasBGEditViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Serializable
  data class NavKeyCanvasBGEdit(val groupId: Long, val parfaitId: Long) : NavKey

  data class CanvasBGEditUiState(
      val selectedColor: Color,
      val selectedImageUri: String?,
      val selectedImageSource: PictureConfirmSource?,
      val showQuitDialog: Boolean,
      val toppings: List<EditableTopping>,
      val isLoading: Boolean,
  )

  enum class CanvasBGEditError { NETWORK, UNSUPPORTED_IMAGE, BACKGROUND_SAVE_UNKNOWN }
  ```
  `EditableTopping`에서 `editedImagePath`·`cutoutImagePath`가 사라지고 `drawnModel`은 `imageUrl`이다. `ToppingFocusDecoration`에서 `onClickEdit` 인자가 사라진다.

- [ ] **Step 1: 테스트를 줄인다**

`CanvasBGEditViewModelTest`에서 토핑 선택·변형·삭제·dirty·테두리 편집 결과·탭 전환 케이스를 지운다(Task 3에서 새 테스트 파일로 복제해 뒀다). 남는 것은 배경 색·이미지 선택, 배경 저장의 성공·실패, 그만두기 팝업, 최초 방출 시딩, 그리고 아래 하나다.

```kotlin
@Test
fun toppings_followEveryEmission() {
    // 두 번째 방출이 토핑을 하나 더 들고 오면 state.toppings 도 그만큼 는다
}

@Test
fun confirm_savesBackgroundOnly() {
    // 배경 저장 성공 → effect == ConfirmBackground. 테스트 클래스에서 토핑 유스케이스 mock 을 지운다
}
```

- [ ] **Step 2: ViewModel·화면·Route를 줄인다**

지우는 것:

- `CanvasEditTab`, `selectedTab`, `OnSelectTab`
- `selectedToppingId`, `showDeleteToppingDialog`, `dirtyToppingIds`, `deletedToppingIds`, `serverToppings`
- 토핑 intent 전부(`OnClickTopping` ~ `OnToppingEditResult`), `NavigateToToppingEdit`
- `deleteToppingUseCase`, `updateToppingsUseCase`, `updateToppingBorderUseCase` 주입과 `updateDirtyToppings` 이하
- `initialToppingId` 인자(ViewModel, Route, entry, NavKey)
- Route의 `ResultEffect<ToppingEditResult>`와 `editingToppingId`
- 화면의 토핑 탭 갈래, 삭제 모달, `YGFloatingBarEditTab`
- `CanvasBGEditError`의 `TOPPING_SAVE_UNKNOWN`·`TOPPING_DELETE_UNKNOWN` (문자열은 수정 화면이 쓰므로 남긴다)
- 문자열 `canvas_bg_edit_tab_background`, `canvas_bg_edit_tab_topping`, `canvas_bg_edit_topping_edit`

남기는 것:

- `mergeToppings` 자리는 `toppings = incoming` 대입으로 바뀐다.
- `handleOnClickConfirm`은 `saveBackground()` 결과만 본다.
- 하단 바는 `YGFloatingBarEdit`. 제목 문자열 `canvas_bg_edit_title`을 새로 둔다(값 "배경").
- 캔버스 여백은 지금 배경 탭의 값(`padding4`)으로 고정한다.

- [ ] **Step 3: `EditableTopping`과 그리기 부품을 정리한다**

`editedImagePath`·`cutoutImagePath`를 빼고 `drawnModel`을 `imageUrl`로 줄인다. `ToppingFocusDecoration`에서 `onClickEdit`과 연필 버튼을 지운다. `CanvasToppingArrangeScreen`의 호출부를 맞춘다.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest :feature:groups:canvas:impl:connectedDebugAndroidTest :feature:camera:impl:compileDebugKotlin`
Expected: PASS. `PictureConfirmRoute`의 `popUpTo<NavKeyCanvasBGEdit>()`는 그대로 컴파일된다.

- [ ] **Step 5: Commit**

```bash
git add feature/groups/canvas
git commit -m "refactor: reduce CanvasBGEdit to background editing"
```

---

### Task 6: 편집 화면의 테두리 코드를 삭제한다

**Files:**
- Modify: `SAPI/NavKeyToppingEdit.kt`
- Delete: `SAPI/ToppingBorderLayer.kt`
- Modify: `S/viewmodel/ToppingEditViewModel.kt`, `S/screen/ToppingEditScreen.kt`, `S/route/ToppingEditRoute.kt`, `S/editor/ToppingEditStroke.kt`
- Modify: `S/viewmodel/SegmentationConfirmViewModel.kt`, `S/route/SegmentationConfirmRoute.kt`, `S/viewmodel/ToppingEditDraft.kt`
- Delete: `S/screen/ToppingBorderEditScreen.kt`, `S/component/ToppingBorderPreviewLayout.kt`, `S/component/BorderColorChipRow.kt`, `S/editor/ToppingBorderColors.kt`, `S/editor/ToppingBorderOutline.kt`
- Delete: `ST/component/ToppingBorderPreviewLayoutTest.kt`
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreen.kt`
- Test: `ST/viewmodel/ToppingEditViewModelTest.kt`, `ST/viewmodel/SegmentationConfirmViewModelTest.kt`, `app/src/test/.../NavKeyAnalyticsScreenTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Serializable
  data class NavKeyToppingEdit(
      val sourceImageUri: String,
      val segmentationImageUri: String,
      val completion: ToppingEditCompletion = ToppingEditCompletion.ReturnResult,
  ) : NavKey

  data class ToppingEditResult(
      val subjectImagePath: String,
      val cutoutImagePath: String,
      val sourceLongSide: Int,
  )
  ```
  `sourceLongSide`는 null이 될 수 없다 — null이던 경우는 `borderOnly` 진입뿐이었다.

- [ ] **Step 1: 테스트를 고친다**

- `ToppingEditViewModelTest`: 테두리 선택·굵기·되돌리기·`borderOnly` 케이스를 지운다. 남는 완료 케이스에서 `result.sourceLongSide`가 원본 긴 변과 같은지 단언한다. 영역이 하한에 못 미치면 `SubjectTooSmall`이 나는 케이스는 조건 없이(`isBorderOnly` 분기 없이) 유지된다.
- `SegmentationConfirmViewModelTest`: `isBorderOnlyEdit` 단언을 `canEditPhoto`로 바꾼 것만 남긴다.
- `NavKeyAnalyticsScreenTest`: `toAnalyticsScreenOrNull_toppingEdit_mergesBorderOnlyIntoOneId`를 `NavKeyToppingEdit(...) == "C-104"` 하나로 줄인다.

- [ ] **Step 2: 삭제하고 고친다**

- `ToppingEditState`: `tab`, `isBorderOnly`, `pendingBorderWidthDp`, `borderHistory`, `borderLayers`, `selectedBorderColor`, `borderWidthDp`, `min/maxBorderWidthDp`와 테두리 상수를 지운다.
- `ToppingEditIntent`: `ChangeTab`, `SelectBorderColor`, `ChangeBorderWidth`, `UndoBorder`, `RedoBorder`를 지운다. `UndoArea`·`RedoArea`는 이름을 그대로 둔다.
- `ToppingEditViewModel`: `borderLayers`·`borderOnly` assisted 인자와 `completeEdit`의 `isBorderOnly` 분기 둘을 지운다.
- `ToppingEditScreen`: 테두리 탭 갈래, `SegmentationBorderControls`, `isBorderOnly` 하단 바 갈래를 지운다.
- `ToppingEditTab` enum과 `topping_edit_tab_*`·`topping_edit_border_*` 문자열을 지운다.
- `SegmentationConfirmRoute`: `borderOnly` 인자와 `sourceImageUri ?: editImageUri` 폴백을 지운다. `canEditPhoto`일 때만 버튼이 보이므로 `sourceImageUri`는 null이 아니다 — null이면 이동하지 않는다.
- `SegmentationConfirmUiState`: `isBorderOnlyEdit`를 지운다(`canEditPhoto`가 대신한다). `editImagePath`는 쓰는 곳이 남는지 보고, 없으면 지운다.
- `ToppingEditDraft.kt`: `sourceLongSide`가 non-null이 된 것에 맞춘다.
- `NavKeyAnalyticsScreen`: `NavKeyToppingEdit`는 언제나 `"C-104"`. 갈래를 설명하던 주석을 지운다.

- [ ] **Step 3: 남은 참조가 없는지 확인**

Run: `grep -rn "ToppingBorderLayer\|borderOnly\|isBorderOnly\|ToppingBorderEditScreen\|TOPPING_EDIT_RESULT_KEY" --include='*.kt' app feature core domain data`
Expected: `TOPPING_EDIT_RESULT_KEY`는 `SAPI`의 선언과 `SegmentationConfirmRoute`·`ToppingEditRoute`에만 남는다. 나머지 넷은 0건.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A app feature
git commit -m "refactor: remove border editing from the topping edit screen"
```

---

### Task 7: 검증과 문서

**Files:**
- Modify: `docs/status.md`, `docs/synthesis/open-questions.md`, `docs/log.md`
- Modify: `docs/adr/0034-topping-border-set-at-placement.md`
- Move: 스펙과 두 계획을 `archive/`로

- [ ] **Step 1: 전체 검증**

Run: `./gradlew ktlintCheck testDebugUnitTest :feature:groups:canvas:impl:connectedDebugAndroidTest assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 에뮬레이터에서 피그마와 대조한다**

본인 토핑 둘 이상, 남의 토핑 하나 이상이 있는 캔버스에서 본다.

| 확인 | 기대 |
|---|---|
| 캔버스 메인에서 본인 토핑 탭 | 수정 화면. "배치" + 닫기만, 탭한 토핑에 점선 박스와 삭제 버튼, 남의 토핑은 딤 (피그마 `5465:20693`) |
| 포커스된 토핑 다시 탭 / 접힌 바 탭 | 패널이 열리고 토핑이 가운데 위로 온다. 다른 본인 토핑 위에 그려진다 (피그마 `5465:20560`) |
| 다른 본인 토핑 탭 | 포커스가 옮겨 간다 |
| 남의 토핑 탭 | "다른 사람의 사진은 편집할 수 없어요" 토스트 (피그마 `5479:13704`) |
| 빈 캔버스 탭 | 포커스가 풀리고 접힌 바를 눌러도 안 열린다 |
| 패널 열린 채 남의 토핑·다른 본인 토핑·삭제 버튼 탭 | 패널만 닫힌다. 토스트·포커스 이동·삭제 모달 없음 |
| 색·굵기 바꾸고 "캔버스에 쌓기" | 캔버스 메인에 바로 반영돼 보인다 |
| 삭제 버튼 → 확인 | 캔버스 메인으로 돌아가고 토핑이 없다 |
| 아무것도 안 바꾸고 닫기 | 그만두기 팝업이 뜬다 |
| 패널 열린 채 시스템 뒤로가기 | 패널만 닫힌다 |
| 지난 캔버스에서 본인 토핑 탭 | 반응 없음 |
| 캔버스 메뉴 → 배경 편집 | 탭 없이 배경만. 토핑은 반투명 |
| 추가 플로우 한 바퀴(촬영 → 배치 → 쌓기) | 계획 A의 동작 그대로 |

- [ ] **Step 3: `docs/status.md`를 덮어쓴다**

"토핑 생성·배치" 영역과 C-301 영역을 지금 코드에 맞춘다. 수정 화면의 앵커(`CanvasToppingArrangeViewModel`, `EditableTopping`, `ToppingBorderPanel`)를 넣고, 배경 편집 영역에서 토핑 관련 문장과 ⚠️ 줄을 걷는다.

- [ ] **Step 4: 미결 항목을 정리한다**

| 항목 | 처리 |
|---|---|
| OQ-P-324, OQ-P-338, OQ-P-201, OQ-P-276 ②, OQ-P-337 ③, OQ-P-379 | 닫는다(항목을 지운다) |
| OQ-P-391 | `borderOnly` 절만 걷는다 |
| OQ-P-202 ①② | 수정 화면의 탭 규칙으로 고쳐 쓰거나, 남는 물음이 없으면 닫는다. ③은 남긴다 |
| OQ-P-175 ①③, OQ-P-326 | 화면 분리에 맞춰 고쳐 쓴다 |
| OQ-P-081 ②, OQ-P-203 ③ | 바뀐 사용처로 고쳐 쓴다 |
| OQ-P-270, OQ-P-245 | 그대로 둔다 |

새로 여는 것: 수정 화면의 하단 버튼 문구가 "캔버스에 쌓기"인 것, 닫기 팝업이 정책 메모와 달리 변경이 없어도 뜨는 것, 분석 화면 id가 정책의 C-105-Arrange와 다른 것.

- [ ] **Step 5: ADR·로그·보관**

- ADR-0034에 수정 플로우도 같은 패널을 쓴다는 것과 `NavKeyToppingEdit`에서 테두리가 빠진 것을 반영한다.
- `docs/log.md`에 한 줄.
- 스펙의 `status`를 `implemented`로, 두 계획의 `status`를 `done`으로 바꾸고 `archive/`로 옮긴다. 두 README의 줄을 옮긴다.

- [ ] **Step 6: 링크 확인과 커밋**

Run: `python3 docs/script/check_links.py docs`
Expected: 깨진 링크 0건

```bash
git add docs
git commit -m "docs: record the topping arrange screen and close resolved questions"
```
