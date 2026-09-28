---
id: topping-pinch-gesture
title: 토핑 두 손가락 변환 제스처
status: draft
type: work-order
created: 2026-09-28
updated: 2026-09-28
platforms: android
owner: android
related_adr:
related_spec: topping-pinch-gesture
related_code:
  - ToppingHitTestInput.kt#toppingTransformInput
  - CanvasToppingPlaceViewModel.kt#CanvasToppingPlaceIntent
  - CanvasBGEditViewModel.kt#CanvasBGEditIntent
archived_reason:
tags: [plan, parfait]
---

# 토핑 두 손가락 변환 제스처 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 배치(C-106)·편집(C-301/C-305) 화면의 회전·크기 핸들을 없애고, 두 손가락 제스처 하나로 이동·회전·확대를 동시에 처리한다.

**Architecture:** 새 modifier `toppingTransformInput`이 한 손가락 이동과 두 손가락 핀치를 한 포인터 루프에서 처리하고, 프레임마다 `(pan, zoom, rotationDelta)`를 전달한다. 두 화면의 ViewModel은 이동·크기·회전 intent 셋을 `OnToppingTransform` 하나로 합친다. 클램프 정책과 저장 경로는 바꾸지 않는다.

**Tech Stack:** Kotlin, Jetpack Compose pointer input (`awaitEachGesture`, `calculatePan/Zoom/Rotation`), JUnit4 + MockK + coroutines-test, Compose UI 계측 테스트(`createComposeRule`).

**Spec:** [`docs/superpowers/specs/2026-09-28-topping-pinch-gesture-design.md`](../specs/2026-09-28-topping-pinch-gesture-design.md)

## Global Constraints

- 코드 주석·KDoc은 [`docs/code-conventions.md`](../../code-conventions.md)를 따른다. 구현·리뷰 서브에이전트 브리프에 이 링크를 넣는다.
- 경로 약어: `C` = `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl`, `T` = 같은 모듈 `src/test/...`, `A` = 같은 모듈 `src/androidTest/kotlin/...`.
- 두 손가락 시작 위치: 캔버스 어디서든. 한 손가락 이동: down 좌표가 `ToppingHitTarget.containsPoint` 안일 때만.
- 편집 화면에서 선택된 토핑이 없으면 제스처를 무시한다. 자동 선택하지 않는다.
- 회전·확대 기준점은 토핑 중심. 손가락 중점 이동량만 pan으로 반영한다.
- 손가락 수가 바뀌어도 제스처는 끊기지 않는다. 잡힘 상태는 모든 손가락이 떨어질 때까지 유지된다.
- 두 번째 포인터가 down된 제스처는 탭이 아니다.
- 변화가 없는 프레임(pan == `Offset.Zero`, zoom == 1f, rotationDelta == 0f)은 `onTransform`을 부르지 않는다. 편집 화면의 불필요한 dirty·PATCH와 배치 화면의 `hasUserAdjustedPlacement` 오염을 막는다.
- 편집 화면의 삭제·편집 버튼은 오버레이 제스처가 진행되는 동안(첫 down부터 모든 손가락 up까지) 숨긴다. 두 번째 손가락이 닿은 뒤 숨기면 이미 늦다 — 그 포인터는 down 시점의 히트 테스트로 버튼에 간다.
- 클램프는 지금 값 그대로. 배치: `coerceIn(minScaleForTouchTarget(), maxScaleToOverflowCanvas())`. 편집: `coerceAtLeast(TOPPING_MIN_SCALE)`.
- `rotationDelta`는 도 단위, 화면 좌표에서 시계 방향이 양수(`graphicsLayer(rotationZ)`와 같은 방향).
- 편집 화면 TL 삭제·BL 편집 버튼과 `ToppingSelectionStroke`는 유지한다. `ic_rotate` `ic_scale` 드로어블은 유지한다.
- 커밋은 task마다 하나. 메시지는 영어 Conventional Commits, `Co-Authored-By` 줄 없음.
- 검증 명령: 단위 `./gradlew :feature:groups:canvas:impl:testDebugUnitTest`, 계측 `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest`(기기·에뮬레이터 필요), 스타일 `./gradlew ktlintCheck`.

## Review Focus

1. 핀치 중 손가락 수가 바뀌는 프레임(2→1, 1→2) — 토핑이 튀지 않아야 한다. Task 1 `pointerCountChange_doesNotJumpPan`이 고정.
2. 두 손가락을 대고 움직이지 않은 채 떼는 경우 — 편집 화면에서 선택 해제(`onMiss`)가 일어나면 안 된다. Task 1 `twoFingerTapWithoutMove_doesNotFireTap`이 고정.
3. 세 번째 손가락 — 계산에서 빠지고 크래시나 튐이 없어야 한다. Task 1 `thirdPointer_isIgnored`가 고정.
4. 배율 경계에서 반대 방향 핀치 — 바로 반응해야 한다. Task 2·3 `*_atClampBoundary_reversesImmediately`가 고정.
5. 두 번째 손가락이 삭제·편집 버튼 자리에 닿음 — 버튼은 첫 down부터 숨겨져 있어 포인터가 오버레이로 가야 한다. Task 3 수동 확인과 Task 1 `onGestureActiveChange` 테스트가 고정.
6. 제스처 중 선택 대상이 바뀌거나 사라짐(삭제 다이얼로그 등) — `targetAt`은 첫 down 시점에만 읽으므로 한 제스처 안에서는 콜백만 계속 나간다. ViewModel이 `selectedToppingId == null`이면 무시하므로 안전하다. Task 3 `transform_withoutSelection_isIgnored`가 고정.

---

### Task 1: `toppingTransformInput` 제스처 modifier와 탭 취소

**Files:**
- Modify: `C/component/ToppingHitTestInput.kt` (`toppingTransformInput` 추가, `toppingTapInput` 다중 포인터 취소. `toppingDragInput`은 Task 3에서 삭제)
- Modify: `feature/groups/canvas/impl/build.gradle.kts` (`alias(libs.plugins.parfait.test.android)`, `alias(libs.plugins.parfait.test.compose)` 추가)
- Modify: `.github/workflows/test.yml` (Compile instrumented tests 단계에 `:feature:groups:canvas:impl:assembleDebugAndroidTest` 추가)
- Create: `A/component/ToppingTransformInputTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Composable
  internal fun Modifier.toppingTransformInput(
      targetAt: () -> ToppingHitTarget?,
      onTransform: (pan: Offset, zoom: Float, rotationDelta: Float) -> Unit,
      onGestureActiveChange: (Boolean) -> Unit = {},
  ): Modifier
  ```
  `pan`은 px. 손가락 하나면 `zoom = 1f`, `rotationDelta = 0f`. `onGestureActiveChange(true)`는 첫 down에서 `targetAt()`이 `null`이 아닐 때, `false`는 모든 손가락이 떨어지거나 제스처가 취소될 때(`finally`) 한 번씩 부른다.

- [ ] **Step 1: 계측 테스트 작성**

`ModifierCenteredAtTest`와 같은 구성(`@MediumTest`, `AndroidJUnit4`, `createComposeRule` from `androidx.compose.ui.test.junit4.v2`). 400×400px 크기 Box(`testTag`)에 화면과 같은 순서 `toppingTapInput(...).toppingTransformInput(...)`으로 붙이고 콜백 호출을 리스트에 기록한다. 대상은 중심 (200,200), 100×100px, 회전 0, outline `null`(박스 판정), 테두리 0인 `ToppingHitTarget`. 좌표는 `performTouchInput`의 px. 슬롭은 `LocalViewConfiguration.current.touchSlop`을 `setContent` 안에서 읽어 둔다. 한 손가락 pan의 기대값은 슬롭 구간이 빠지므로 `이동 벡터 − touchSlop × 방향 단위벡터`다.

| 테스트 | 입력 | 기대 |
|---|---|---|
| `singleFinger_outsideTarget_doesNotTransform` | (20,20)에서 (120,20)으로 드래그 | `onTransform` 0회 |
| `singleFinger_insideTarget_panOnly` | (200,200)에서 (260,200)으로 여러 스텝 이동 | pan 합 ≈ (60 − touchSlop, 0) ±1, 모든 호출의 zoom == 1f, rotationDelta == 0f |
| `twoFingers_outsideTarget_zoomAndRotate` | (100,200),(300,200) down, 여러 스텝에 걸쳐 (59,59),(341,341)로 이동 | zoom 곱 ≈ 2 ±0.05, rotationDelta 합 ≈ 45 ±1 |
| `pointerCountChange_doesNotJumpPan` | 실루엣 안 한 손가락을 슬롭 넘게 드래그한 뒤 두 번째 down, 둘 다 같은 벡터로 이동, 첫 손가락 up, 남은 손가락 이동 | pan 합 == 중점 이동량 − touchSlop × 첫 드래그 방향 단위벡터 ±1. 어떤 한 호출의 pan도 스텝 이동량의 2배를 넘지 않음 |
| `twoFingerTapWithoutMove_doesNotFireTap` | 대상 밖 두 점 down 후 이동 없이 둘 다 up | `onHit`·`onMiss` 0회 |
| `thirdPointer_isIgnored` | 두 손가락 핀치 중 세 번째 down 후 세 번째만 크게 이동 | 세 번째 이동 구간의 zoom == 1f ±0.01, rotationDelta == 0f ±0.5 |
| `nullTarget_doesNothing` | `targetAt = { null }`에서 두 손가락 핀치 | `onTransform` 0회, `onGestureActiveChange` 0회 |
| `twoFingersWithoutMove_doesNotTransform` | 대상 밖 두 점 down, 이동 없이 여러 프레임 유지 후 up | `onTransform` 0회 |
| `gestureActive_spansFirstDownToLastUp` | 대상 밖 한 손가락 down → 두 번째 down → 둘 다 up | `onGestureActiveChange` 기록이 `[true, false]`, `true`는 첫 down 직후 |

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:assembleDebugAndroidTest`
Expected: 컴파일 실패 — `toppingTransformInput` 미정의. 기기가 있으면 아래도 돌린다.

Run: `./gradlew :feature:groups:canvas:impl:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.teamyg.parfait.feature.groups.canvas.impl.component.ToppingTransformInputTest`
Expected: 컴파일 실패 — `toppingTransformInput` 미정의.

- [ ] **Step 3: `toppingTransformInput` 구현**

`toppingTapInput`처럼 핸들러를 `remember`로 고정하고 콜백은 `rememberUpdatedState`로 읽는다. `awaitTouchSlopOrCancellation`은 두 번째 down에 반환하지 않으므로 슬롭 단계도 직접 루프로 짠다.

```text
down = awaitFirstDown(requireUnconsumed = false)
target = targetAt() ?: return
onGestureActiveChange(true); try {
  grabbed = false; slopAccum = Offset.Zero
  canDrag = target.containsPoint(down.position)
  loop:
    event = awaitPointerEvent()
    if event.changes.none { it.pressed }: break
    if !grabbed:
      if event.changes.count { it.pressed } >= 2: grabbed = true      // 이 이벤트는 계산하지 않는다
      else if canDrag:
        slopAccum += down 포인터의 positionChange()
        if slopAccum.getDistance() > touchSlop:
          grabbed = true
          overSlop = slopAccum - slopAccum / |slopAccum| * touchSlop
          emit(overSlop, 1f, 0f); consume
      continue
    tracked = event.changes.filter { it.pressed && it.previousPressed }.sortedBy { it.id.value }.take(2)
    1개: pan = position − previousPosition, zoom 1f, rotation 0f
    2개: pan = 중점 − 이전 중점
         zoom = 거리 / 이전 거리 (이전 거리 == 0 이면 1f)
         rotation = angle(현재 벡터) − angle(이전 벡터), angle(v) = -atan2(v.x, v.y) 도 단위, ±180 로 정규화
    emit(pan, zoom, rotation); event.changes 전부 consume
} finally { onGestureActiveChange(false) }
```

`emit`은 항등 변환(pan == `Offset.Zero` && zoom == 1f && rotation == 0f)이면 건너뛴다. `pressed && previousPressed` 필터가 새로 down된 포인터와 막 뗀 포인터를 그 이벤트의 계산에서 빼므로, 포인터 수가 바뀌어도 중점이 튀지 않는다. `angle(v) = -atan2(v.x, v.y)`는 화면 좌표(y 아래)에서 시계 방향이 양수라 `graphicsLayer(rotationZ)`와 같다.

- [ ] **Step 4: `toppingTapInput` 다중 포인터 취소**

`waitForUpOrCancellation()` 대신 `awaitPointerEvent()` 루프로 바꾼다. 종료 조건은 `waitForUpOrCancellation`의 기존 동작을 모두 유지하고 하나를 더한다.

- down의 id가 아닌 pressed 포인터가 있으면 콜백 없이 종료(추가).
- down 포인터의 change가 소비됐으면 종료.
- down 포인터가 `isOutOfBounds(size, extendedTouchPadding)`이면 종료.
- down 포인터가 up이면 기존 throttle·콜백 경로.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:assembleDebugAndroidTest` (컴파일 게이트, 기기 불필요)
Expected: BUILD SUCCESSFUL.
Run: Step 2의 `connectedDebugAndroidTest` 명령(기기 있을 때)
Expected: 9 tests PASS. 이어서 `./gradlew :feature:groups:canvas:impl:testDebugUnitTest` PASS(`ToppingHitTestTest`, `ToppingClickThrottleTest` 회귀 없음).

- [ ] **Step 6: Commit**

```bash
git add feature/groups/canvas/impl .github/workflows/test.yml
git commit -m "feat: add two-finger topping transform gesture input"
```

---

### Task 2: 배치 화면을 두 손가락 제스처로 전환

**Files:**
- Modify: `C/viewmodel/CanvasToppingPlaceViewModel.kt`
- Modify: `C/route/CanvasToppingPlaceRoute.kt`
- Modify: `C/screen/CanvasToppingPlaceScreen.kt`
- Test: `T/viewmodel/CanvasToppingPlaceViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 `toppingTransformInput`
- Produces: `CanvasToppingPlaceIntent.OnToppingTransform(pan: DpOffset, zoom: Float, rotationDelta: Float)`. `OnToppingMoveDrag` `OnToppingResize` `OnToppingRotate` 삭제. Screen 파라미터 `onToppingTransform: (pan: DpOffset, zoom: Float, rotationDelta: Float) -> Unit`가 기존 세 콜백을 대체.

- [ ] **Step 1: ViewModel 테스트 교체**

기존 `onToppingResize_*` 4개와 `onToppingRotate_accumulatesAcrossMultipleDrags`를 아래로 바꾼다. 기존 기대값(2.5 상한, 0.5 하한, 1.21 누적, 30도 누적)은 그대로 쓴다.

- `onToppingTransform_appliesPanZoomAndRotationTogether`: `OnToppingTransform(DpOffset(10.dp, 20.dp), 1.1f, 15f)` 한 번 → `offsetX` +10dp, `offsetY` +20dp, `scale` 1.1, `rotationDegrees` 15, `hasUserAdjustedPlacement` true.
- `onToppingTransform_accumulatesAcrossFrames`: zoom 1.1 두 번 → 1.21, rotation 20 + 10 → 30.
- `onToppingTransform_clampsAtMaxScale`: zoom 100 → 2.5.
- `onToppingTransform_clampsAtMinScale`: zoom 0 → 0.5.
- `onToppingTransform_atClampBoundary_reversesImmediately`: zoom 100 뒤 zoom 0.5 → 1.25.

기존 테스트 중 `OnToppingMoveDrag`·`OnToppingResize`·`OnToppingRotate`를 쓰는 나머지 호출도 `OnToppingTransform`으로 전부 치환한다(`grep -n "OnToppingMoveDrag\|OnToppingResize\|OnToppingRotate" T/viewmodel/CanvasToppingPlaceViewModelTest.kt`가 빈 결과).

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*CanvasToppingPlaceViewModelTest'`
Expected: 컴파일 실패 — `OnToppingTransform` 미정의.

- [ ] **Step 3: intent 통합**

세 intent와 핸들러를 삭제하고 `handleOnToppingTransform`에서 `updateState` 한 번으로 스펙의 네 필드를 갱신한다.

- [ ] **Step 4: Screen·Route 전환**

- 토핑 박스의 `.dragBy(Unit) { ... }` 삭제.
- 캔버스 Box(`aspectRatio(CANVAS_AREA_ASPECT_RATIO)`, `clipToBounds`) 안 맨 위에 `Modifier.matchParentSize().toppingTransformInput(...)` Box 추가. 오버레이는 토핑 Box 뒤(형제 순서상 아래)에 둔다. `targetAt`은 `center`·`sizeAfterScale`을 px로 바꾸고 `rotationDegrees`, `outline`으로 `ToppingHitTarget`을 만든다. `borderWidthPx`는 `CanvasToppingLayer`의 규칙과 같이 `borderColorArgb != null && isToppingImageLoaded`일 때만 `borderWidthDp`의 px 값, 아니면 0. `pan`은 `density`로 `DpOffset`으로 바꿔 `onToppingTransform`에 넘긴다.
- `ToppingPlaceCornerButtons`와 그 호출 Box 삭제. 스트로크 Box는 유지.
- Screen KDoc에서 "회전·리사이즈 버튼" 서술을 두 손가락 제스처로 고친다. Preview의 콜백 인자를 맞춘다.
- Route는 `onToppingTransform = { pan, zoom, rotation -> viewModel.processIntent(OnToppingTransform(pan, zoom, rotation)) }`.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest && ./gradlew ktlintCheck`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add feature/groups/canvas/impl
git commit -m "feat: replace topping place handles with pinch gesture"
```

---

### Task 3: 편집 화면을 두 손가락 제스처로 전환

**Files:**
- Modify: `C/viewmodel/CanvasBGEditViewModel.kt`
- Modify: `C/route/CanvasBGEditRoute.kt`
- Modify: `C/screen/CanvasBGEditScreen.kt`
- Modify: `C/component/ToppingHitTestInput.kt` (`toppingDragInput` 삭제)
- Test: `T/viewmodel/CanvasBGEditViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 `toppingTransformInput`
- Produces: `CanvasBGEditIntent.OnToppingTransform(panX: Float, panY: Float, zoom: Float, rotationDelta: Float)`(pan은 캔버스 대비 비율). `OnToppingResize` `OnToppingRotate` `OnToppingMoveDrag` 삭제. Screen 파라미터 `onToppingTransform: (panX: Float, panY: Float, zoom: Float, rotationDelta: Float) -> Unit`.

- [ ] **Step 1: ViewModel 테스트 교체**

`toppingMoveDrag_movesByTheRatioItReceives`와 `toppingResize_largeDrag_isNoLongerClampedToTheOldMax`를 옮기고 추가한다.

- `toppingTransform_movesByTheRatioItReceives`: 선택 후 `OnToppingTransform(0.1f, 0.05f, 1f, 0f)` → positionX 0.35, positionY 0.80(기존 기대값).
- `toppingTransform_appliesPanZoomAndRotationTogether`: 선택 후 `(0.1f, 0.05f, 2f, 30f)` 한 번 → 위치는 위와 같고, scale은 원래의 2배, rotationDegrees는 원래 + 30.
- `toppingTransform_largeZoom_isNotClampedToTheOldMax`: zoom 10 → scale > 2.5.
- `toppingTransform_atClampBoundary_reversesImmediately`: zoom 0 → 0.05, 이어서 zoom 2 → 0.1. `TOPPING_MIN_SCALE`은 `private`이라 리터럴을 쓴다.
- `transform_withoutSelection_isIgnored`: 선택 없이 transform → `toppings`와 `dirtyToppingIds` 변화 없음.
- `toppingTransform_marksOnlyTheSelectedToppingDirty`: 선택 후 transform 한 번 → `dirtyToppingIds == setOf(선택 id)`.

나머지 기존 테스트(dirty·batch PATCH·merge 등)의 `OnToppingMoveDrag(dx, dy)` 호출은 전부 `OnToppingTransform(dx, dy, 1f, 0f)`로 치환한다. `grep -n "OnToppingMoveDrag\|OnToppingResize\|OnToppingRotate" T/viewmodel/CanvasBGEditViewModelTest.kt`가 빈 결과여야 한다.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*CanvasBGEditViewModelTest'`
Expected: 컴파일 실패 — `OnToppingTransform` 미정의.

- [ ] **Step 3: intent 통합**

세 intent와 핸들러를 삭제한다. `handleOnToppingTransform`은 `selectedToppingId ?: return` 뒤 `applyToppingTransform` 한 번으로 position, scale, rotation을 갱신한다.

- [ ] **Step 4: Screen·Route 전환**

- 오버레이의 `.toppingDragInput(...)`을 `.toppingTransformInput(targetAt = { selectedEntry?.target }, onTransform = { pan, zoom, rotation -> onToppingTransform(pan.x / canvasWidthPx, pan.y / canvasHeightPx, zoom, rotation) })`로 바꾼다.
- `ToppingCornerButtons`에서 `ToppingRotateHandleButton`·`ToppingResizeHandleButton` 호출과 `onResize`·`onRotate` 파라미터를 삭제한다. KDoc을 "좌측 상단=삭제, 좌측 하단=편집"으로 고친다.
- 화면에 `var isToppingGestureActive by remember { mutableStateOf(false) }`를 두고 `toppingTransformInput(onGestureActiveChange = { isToppingGestureActive = it })`로 갱신한다. `ToppingCornerButtons`에 `showActionButtons: Boolean` 파라미터를 추가하고 `!isToppingGestureActive`를 넘긴다. `false`면 삭제·편집 버튼을 컴포지션에서 뺀다. 스트로크는 계속 그린다.
- Preview 두 곳의 콜백 인자를 맞춘다. Route 연결.
- `ToppingHitTestInput.kt`의 `toppingDragInput`과 쓰지 않게 된 import를 삭제한다.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest && ./gradlew ktlintCheck`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add feature/groups/canvas/impl
git commit -m "feat: replace topping edit handles with pinch gesture"
```

---

### Task 4: 핸들 잔재 삭제

**Files:**
- Modify: `C/component/ToppingHandleComponents.kt` (`ToppingDragHandleButton` `ToppingRotateHandleButton` `ToppingResizeHandleButton` `handleVectorPx` 삭제)
- Modify: `C/util/ToppingGeometry.kt` (`resizeScaleFactor` `rotationDeltaDegrees` 삭제)
- Modify: `T/util/ToppingGeometryTest.kt` (`rotationDeltaDegrees_*` `resizeScaleFactor_*` 삭제)
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml` (`canvas_bg_edit_topping_rotate` `canvas_bg_edit_topping_resize` 삭제. 다른 로케일 `values-*`에도 있으면 함께)
- Modify: `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/Modifier.kt` (`dragBy` 삭제)

**Interfaces:**
- Consumes: Task 2·3 이후 위 심볼의 호출자가 0이다.

- [ ] **Step 1: 호출자 0 확인**

Run: `grep -rnE "ToppingDragHandleButton|ToppingRotateHandleButton|ToppingResizeHandleButton|handleVectorPx|resizeScaleFactor|rotationDeltaDegrees|\.dragBy\(|topping_rotate|topping_resize" --include='*.kt' --include='*.xml' . | grep -v /build/`
Expected: 정의부와 `ToppingGeometryTest`만 나온다.

- [ ] **Step 2: 삭제**

위 심볼, 해당 테스트 케이스, 쓰지 않게 된 import와 상수를 지운다. `ToppingSelectionStroke`, `computeToppingButtonPoints`, `computeToppingStrokeCorners`는 남는다.

- [ ] **Step 3: 확인**

Run: Step 1 명령
Expected: 출력 없음.
Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest :core:util:android:testDebugUnitTest :feature:groups:canvas:impl:assembleDebugAndroidTest ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add feature/groups/canvas/impl core/util/android
git commit -m "refactor: remove topping rotate and resize handle leftovers"
```

---

### Task 5: 문서 반영

**Files:**
- Modify: `docs/status.md` (C-106, C-301 절의 조작 방식 서술)
- Modify: `docs/synthesis/open-questions.md` (OQ-P-202 문구를 핀치 기준으로. 상태는 미결 그대로)
- Modify: `docs/superpowers/specs/2026-09-28-topping-pinch-gesture-design.md` (`status: implemented`, `verified` 갱신)
- Modify: `docs/superpowers/specs/README.md`, `docs/superpowers/plans/README.md` (상태 갱신)
- Modify: 삭제된 심볼을 grep 앵커로 쓰는 문서 — `docs/architecture/module-structure.md`·`docs/architecture/design-system.md`(`Modifier.dragBy`, 손잡이로 쓰는 `YGCircleButton`), `docs/synthesis/open-questions.md`(`dragBy`, `resizeScaleFactor`, `OnToppingResize` 언급 항목). Step 1 전에 `grep -rnE "dragBy|resizeScaleFactor|rotationDeltaDegrees|OnToppingResize|OnToppingRotate|OnToppingMoveDrag|toppingDragInput|RotateHandleButton|ResizeHandleButton" docs`로 전수를 뽑는다. 닫힌 미결의 이력 서술은 고치지 말고, 현재 코드를 가리키는 앵커만 고친다.

- [ ] **Step 1: 갱신**

`CLAUDE.md` "무엇을 적나" 기준을 따른다. 현재 상태만 적고 "핸들을 제거했다" 같은 변경 서술은 쓰지 않는다. `docs/log.md`에 한 줄 추가.

- [ ] **Step 2: 링크 확인**

Run: `python3 docs/script/check_links.py docs`
Expected: `깨진 링크 0건`.

- [ ] **Step 3: Commit**

```bash
git add docs
git commit -m "docs: reflect topping pinch gesture in status and open questions"
```

---

## 수동 확인 (구현 후, 실기기)

- 배치 화면: 토핑 밖 두 손가락 핀치로 확대·회전·이동이 동시에 된다. 토핑 밖 한 손가락은 움직이지 않는다.
- 편집 화면: 작은 토핑을 핀치하며 두 번째 손가락을 삭제·편집 버튼 자리에 대도 핀치가 이어지고, 손을 떼도 삭제 다이얼로그나 편집 화면이 뜨지 않는다. 캔버스를 한 번 탭하면 버튼이 잠깐 사라졌다 돌아오는 정도는 허용.
- 편집 화면: 선택 없이 핀치하면 아무 일도 없다. 선택 후 다른 사람 토핑 위에서 시작한 핀치도 내 토핑을 변환한다. 핀치를 끝내도 선택이 풀리지 않는다.
- 두 화면: 핀치 중 한 손가락을 떼고 계속 끌면 토핑이 따라온다.
