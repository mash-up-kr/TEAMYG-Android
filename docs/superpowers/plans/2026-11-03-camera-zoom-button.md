---
id: camera-zoom-button
title: 커스텀 카메라 줌 버튼
status: draft
type: work-order
created: 2026-11-03
updated: 2026-11-03
platforms: android
owner: android
related_adr:
related_spec: camera-zoom-button
related_code:
  - YGCameraZoomButton.kt#YGCameraZoomButton
  - CameraZoomLevel.kt#CameraZoomLevel
  - CustomCameraViewModel.kt#CustomCameraViewModel
  - CustomCameraScreen.kt#CustomCameraScreen
archived_reason:
tags: [plan, parfait]
---

# 커스텀 카메라 줌 버튼 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** C-101 뷰파인더 하단 중앙 줌 버튼이 지원 배율만 `1x → 2x → 0.5x` 순서로 순환한다.

**Architecture:** 배율 단계는 `impl/model/CameraZoomLevel` enum이 갖고, VM이 `OnZoomRangeReady`로 지원 단계 목록을 만들고 `OnClickZoom`으로 순환한다.
`CustomCameraState`의 `zoomRatio: Float`·`zoomRange`를 `zoomLevel`·`zoomLevels`로 바꾸고, 카메라 반영은 기존 `LaunchedEffect`에 `zoomLevel.ratio`를 넘긴다.
버튼 UI는 `core:designsystem`의 `YGCameraZoomButton`(라벨 문자열만 받음).

**Tech Stack:** Kotlin, Jetpack Compose, CameraX, kotlin.test + Turbine

**Spec:** `docs/superpowers/specs/2026-11-03-camera-zoom-button-design.md`

## Global Constraints

- 순환 순서는 `1x → 2x → 0.5x`, 범위 밖 단계는 뺀다. 남은 단계가 1x 하나면 버튼을 그리지 않는다.
- 렌즈 전환(`OnClickFlip`)과 새 범위 도착(`OnZoomRangeReady`) 모두 1x로 초기화.
- DS 컴포넌트는 feature 모델을 모른다 — `label: String`.
- 비UI 타입은 `impl/model`에 한 파일 하나, enum 동작은 멤버로. 컴포넌트에는 프리뷰.
- 토큰만 쓴다: `SizeTokens.Size44`, `padding2`, `padding4`, `radius.round`, `Transparency.White25/Black50/Black75`, `Gray.White`, `typography.caption.c01M`. 테두리 1.5dp만 raw.
- 주석·KDoc은 `docs/code-conventions.md`. 문서에 hex·라인번호 금지.
- 커밋 메시지 `type: 한국어 설명`, Co-Authored-By 없음.

## Review Focus

- 0.5x 미지원 기기(대부분 전면) → 1x↔2x만. (Task 1 테스트)
- 2x 미지원 → 1x↔0.5x. (Task 1 테스트)
- 1x만 지원 → 버튼 숨김. (Task 1 `isZoomButtonVisible` 테스트 + Task 3 화면 분기)
- 0.5x 상태에서 렌즈 전환 → 1x, 새 렌즈 단계로 순환. (Task 1 테스트)
- 범위 도착 전(초기 `1f..1f`) 버튼 탭 → 아무 일 없음, 버튼도 숨김. (Task 1 테스트)

---

### Task 1: 배율 단계 모델과 VM 순환

**Files:**
- Create: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/model/CameraZoomLevel.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/viewmodel/CustomCameraViewModel.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/CustomCameraRoute.kt` (`zoomRatio = state.zoomLevel.ratio` 한 줄 — 컴파일 유지)
- Test: `feature/camera/impl/src/test/java/com/teamyg/parfait/feature/camera/impl/viewmodel/CustomCameraViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class CameraZoomLevel(val ratio: Float, val label: String) {
      ONE(1f, "1x"), TWO(2f, "2x"), HALF(0.5f, "0.5x");
      fun next(available: List<CameraZoomLevel>): CameraZoomLevel
      companion object { fun availableIn(range: ClosedFloatingPointRange<Float>): List<CameraZoomLevel> }
  }
  // CustomCameraState: zoomLevel: CameraZoomLevel = ONE, zoomLevels: List<CameraZoomLevel> = listOf(ONE)
  // val isZoomButtonVisible: Boolean get() = zoomLevels.size > 1
  // CustomCameraIntent.OnClickZoom : data object
  ```
  선언 순서가 곧 순환 순서다. `next`는 `available` 안의 다음 원소, 끝이면 처음, `this`가 없으면 `ONE`.

- [ ] **Step 1: 실패하는 테스트** — 헬퍼 `fun CustomCameraViewModel.clickZoom(times: Int)`로 반복.

```kotlin
@Test fun zoom_bothSupported_cyclesOneTwoHalf()     // 범위 0.5f..8f: 클릭 1·2·3회 → TWO, HALF, ONE
@Test fun zoom_halfUnsupported_cyclesOneTwo()        // 1f..8f: 클릭 1·2회 → TWO, ONE
@Test fun zoom_twoUnsupported_cyclesOneHalf()        // 0.5f..1.5f: 클릭 1·2회 → HALF, ONE
@Test fun zoom_neitherSupported_hidesButton()        // 1f..1f: isZoomButtonVisible == false, 클릭해도 ONE
@Test fun zoom_beforeRangeReady_hidesButton()        // 범위 없이: isZoomButtonVisible == false, 클릭해도 ONE
@Test fun zoom_onFlip_resetsToOne()                  // 0.5f..8f, 클릭 2회(HALF) → OnClickFlip → ONE
@Test fun zoom_onNewRange_resetsToOneAndRebuildsLevels() // 0.5f..8f, 클릭 1회(TWO) → OnZoomRangeReady(1f..1f) → ONE, 숨김
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:camera:impl:testDebugUnitTest --tests '*CustomCameraViewModelTest'`
Expected: 컴파일 실패 — `CameraZoomLevel`·`OnClickZoom` 없음

- [ ] **Step 3: 구현** — `CameraZoomLevel.kt` 작성. State에서 `zoomRatio`·`zoomRange` 제거하고 `zoomLevel`·`zoomLevels`·`isZoomButtonVisible` 추가.
  `handleOnZoomRangeReady`: `zoomLevels = availableIn(range)`, `zoomLevel = ONE`. `handleOnClickFlip`: `zoomLevel = ONE` 추가.
  `handleOnClickZoom`: `zoomLevel = zoomLevel.next(zoomLevels)`. route는 `zoomRatio = state.zoomLevel.ratio`.

- [ ] **Step 4: 통과 확인** — Step 2 명령, Expected: 10 tests PASS

- [ ] **Step 5: 커밋**

```bash
git add feature/camera/impl
git commit -m "feat: 카메라 줌 단계 순환 상태 추가"
```

---

### Task 2: `YGCameraZoomButton`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcamerazoombutton/YGCameraZoomButton.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Composable
  fun YGCameraZoomButton(
      label: String,
      onClick: () -> Unit,
      modifier: Modifier = Modifier,
      interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  )
  ```

- [ ] **Step 1: 구현** — `YGCameraShutter` 구조를 따른다. 바깥 `Box`가 `size(Size44)` + `clickableYGNoRipple` + `semantics { role = Role.Button }` + `padding(padding2)`,
  안쪽 원은 `fillMaxSize`, `radius.round`, 배경 `isPressed`면 `Black75` 아니면 `Black50`, `border(1.5.dp, White25, shape)`, 가운데 `Text(label, style = c01M, color = Gray.White)`.
  KDoc 한 줄 `Figma Button-Camera-Zoom`. 프리뷰는 `Gray.Black` 배경 위에 세 라벨을 `Row`로.

- [ ] **Step 2: 빌드·lint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add core/designsystem
git commit -m "feat: YGCameraZoomButton 추가"
```

---

### Task 3: 화면 연결과 정리

**Files:**
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/screen/CustomCameraScreen.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/CustomCameraRoute.kt`
- Delete: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/component/CameraZoomIndicatorComponent.kt`, `.../component/controls/ZoomLevelRow.kt`
- Modify: `docs/status.md`, `docs/synthesis/open-questions.md`, `docs/architecture/design-system.md`, `docs/superpowers/specs/README.md`, `docs/superpowers/plans/README.md`

**Interfaces:**
- Consumes: Task 1 `CustomCameraState.zoomLevel`·`isZoomButtonVisible`, `CustomCameraIntent.OnClickZoom`; Task 2 `YGCameraZoomButton`
- Produces: `CustomCameraScreen(..., onClickZoom: () -> Unit, ...)` — `onClickFlash` 뒤

- [ ] **Step 1: 화면** — `CustomCameraScreen`에 `onClickZoom` 추가, `CameraContent`에 `zoomLevel: CameraZoomLevel`·`isZoomButtonVisible: Boolean`·`onClickZoom` 전달.
  뷰파인더 `Box` 안 `YGToastHost` 옆에 `if (isZoomButtonVisible)`로 `YGCameraZoomButton(label = zoomLevel.label, onClick = onClickZoom, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = padding4))`.
  프리뷰 provider에 `zoomLevels = listOf(ONE, TWO, HALF)`인 상태 하나 추가.

- [ ] **Step 2: route** — `onClickZoom = { viewModel.processIntent(CustomCameraIntent.OnClickZoom) }`.

- [ ] **Step 3: 삭제** — 두 컴포넌트 파일 삭제, `grep -rn "ZoomLevelRow\|CameraZoomIndicatorComponent" feature core` → 0건.

- [ ] **Step 4: 빌드·테스트·lint**

Run: `./gradlew :feature:camera:impl:compileDebugKotlin :feature:camera:impl:testDebugUnitTest :feature:camera:impl:ktlintCheck :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 실기기 확인** — 후면(0.5x 지원 기기 있으면)·전면 순환, 0.5x에서 전환 시 1x, 버튼 위치. 사용자에게 받는다.

- [ ] **Step 6: 문서**
  - `status.md` 「토핑 생성·배치」 절에 한 줄: C-101 줌 버튼이 지원 단계만 순환·전환 시 1x·1x만이면 숨김. 앵커 `CameraZoomLevel`, `YGCameraZoomButton`.
  - `open-questions.md` OQ-P-052 상태를 해소됨으로, 해소 근거 한 줄(스펙 링크).
  - `design-system.md` 컴포넌트 표에 `YGCameraZoomButton` | `component/ygcamerazoombutton/` | 스펙 링크.
  - 스펙 `status: implemented` 후 `archive/`로 이동, 계획 `status: done` 후 `archive/`로 이동, 두 README 표 갱신.
  - `python3 docs/script/check_links.py docs` → exit 0.

- [ ] **Step 7: 커밋**

```bash
git add -A feature/camera/impl docs
git commit -m "feat: 커스텀 카메라에 줌 버튼 연결"
```
