---
id: camera-zoom-button
title: 커스텀 카메라 줌 버튼
status: done
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
archived_reason: 구현 완료 (feature/camera-zoom-button, 2026-11-03)
tags: [plan, parfait]
---

# 커스텀 카메라 줌 버튼 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** C-101 뷰파인더 하단 중앙 줌 버튼이 지원 배율만 `1x → 2x → 0.5x` 순서로 순환한다.

**Architecture:** 배율 단계는 `impl/model/CameraZoomLevel` enum이 갖고, VM이 `OnZoomRangeReady`로 지원 단계 목록을 만들고 `OnClickZoom`으로 순환한다.
`CustomCameraState`의 `zoomRatio: Float`·`zoomRange`를 `zoomLevel`·`zoomLevels`로 바꾸고, 카메라 반영은 기존 `LaunchedEffect`에 `zoomLevel.ratio`를 넘긴다.
버튼 UI는 `core:designsystem`의 `YGCameraZoomButton`(라벨 문자열만 받음).

**Tech Stack:** Kotlin, Jetpack Compose, CameraX, kotlin.test + Turbine

**Spec:** `docs/superpowers/specs/archive/2026-11-03-camera-zoom-button-design.md`

## Global Constraints

- 순환 순서는 `1x → 2x → 0.5x`, 범위 밖 단계는 뺀다. 남은 단계가 1x 하나면 버튼을 그리지 않는다.
- 렌즈 전환(`OnClickFlip`)과 새 범위 도착(`OnZoomRangeReady`) 모두 1x로 초기화.
- DS 컴포넌트는 feature 모델을 모른다 — `label: String`.
- 비UI 타입은 `impl/model`에 한 파일 하나, enum 동작은 멤버로. 컴포넌트에는 프리뷰.
- 토큰만 쓴다: `SizeTokens.Size44`, `padding2`, `padding4`, `radius.round`, `Transparency.White25/Black50/Black75`, `Gray.White`, `typography.caption.c01M`. 테두리 1.5dp만 raw.
- 주석·KDoc은 `docs/code-conventions.md`. 문서에 hex·라인번호 금지.
- 커밋 메시지 `type: 한국어 설명`, Co-Authored-By 없음.

## Review Focus

(Task 4–6 추가분)
- A35처럼 초광각이 별도 ID인 후면 → 1x → 2x → 0.5x(초광각 바인딩) → 1x(메인 재바인딩). (Task 5 테스트 + Task 6 실기기)
- 초광각에 붙은 상태에서 범위 보고가 오면 0.5x가 풀린다 → 메인일 때만 보고. (Task 6 코드 + 실기기)
- 2x에서 홈→복귀 → 프리뷰도 2x. (Task 6 실기기)
- 매크로·심도 센서를 초광각으로 오인. (Task 4 테스트)
- 전환 직후 이전 렌즈 단계로 순환. (Task 5 테스트)

(Task 1–3)

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

---

### Task 4: 초광각 판정

**Files:**
- Create: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/model/LensOptics.kt`
- Create: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/util/UltraWideCamera.kt`
- Test: `feature/camera/impl/src/test/java/com/teamyg/parfait/feature/camera/impl/util/UltraWideCameraTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class LensOptics(val cameraId: String, val sensorWidthMm: Float, val minFocalLengthMm: Float) {
      val fieldOfViewIndex: Float // sensorWidthMm / minFocalLengthMm
  }
  internal fun findUltraWideCamera(main: LensOptics, backCameras: List<LensOptics>): LensOptics?
  ```
  `main`과 같은 `cameraId`는 제외, `fieldOfViewIndex >= main.fieldOfViewIndex / 0.7f`인 것 중 최대. 없으면 null.

- [ ] **Step 1: 실패하는 테스트**

```kotlin
@Test fun ultraWide_a35Like_isFound()        // main("0", 6.4f, 4.69f), [main, ("2", 4.0f, 1.64f)] → "2"
@Test fun ultraWide_macroOnly_isNull()       // [main, ("3", 2.0f, 1.8f)] (지표 1.11 < 1.36·1.43) → null
@Test fun ultraWide_several_picksWidest()    // ("2", 4.0f, 1.64f), ("4", 4.0f, 2.2f) → "2"
@Test fun ultraWide_onlyMain_isNull()        // [main] → null
```

- [ ] **Step 2: 실패 확인** — `./gradlew :feature:camera:impl:testDebugUnitTest --tests '*UltraWideCameraTest'`, Expected: 컴파일 실패
- [ ] **Step 3: 구현** — 판정 임계 `0.7f`는 상수로, 이유(매크로·심도 배제) 한 줄 주석.
- [ ] **Step 4: 통과 확인** — Expected: 4 tests PASS
- [ ] **Step 5: 커밋** — `git commit -m "feat: 후면 초광각 카메라 판정 추가"`

---

### Task 5: 0.5x 경로를 VM 상태에 반영

**Files:**
- Create: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/model/HalfZoomSource.kt`
- Modify: `.../model/CameraZoomLevel.kt`, `.../viewmodel/CustomCameraViewModel.kt`
- Test: `.../viewmodel/CustomCameraViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class HalfZoomSource { NONE, ZOOM_RATIO, ULTRA_WIDE;
      companion object { fun of(range: ClosedFloatingPointRange<Float>, hasUltraWideCamera: Boolean): HalfZoomSource } }
  // CameraZoomLevel.available(range, halfZoomSource): List<CameraZoomLevel>  (availableIn 대체)
  // CustomCameraIntent.OnZoomRangeReady(range, hasUltraWideCamera: Boolean = false)
  // CustomCameraState.halfZoomSource: HalfZoomSource = NONE
  // val useUltraWideCamera: Boolean  // zoomLevel == HALF && halfZoomSource == ULTRA_WIDE
  // val cameraZoomRatio: Float       // useUltraWideCamera면 1f, 아니면 zoomLevel.ratio
  ```
  `of`: 0.5가 범위 안이면 `ZOOM_RATIO`(우선), 아니면 초광각 있으면 `ULTRA_WIDE`, 아니면 `NONE`.

- [ ] **Step 1: 실패하는 테스트** (기존 7건은 그대로 통과해야 한다)

```kotlin
@Test fun zoom_ultraWideOnly_cyclesToHalfOnUltraWide()
// OnZoomRangeReady(1f..8f, hasUltraWideCamera = true): 클릭 2회 → HALF, useUltraWideCamera true, cameraZoomRatio 1f; 1회 더 → ONE, false
@Test fun zoom_rangeCoversHalf_prefersZoomRatio()
// OnZoomRangeReady(0.5f..8f, hasUltraWideCamera = true): 클릭 2회 → HALF, useUltraWideCamera false, cameraZoomRatio 0.5f
@Test fun zoom_onFlip_hidesButtonUntilNewRange()
// 0.5f..8f, 클릭 1회 → OnClickFlip → isZoomButtonVisible false, 클릭해도 ONE, halfZoomSource NONE
```

- [ ] **Step 2: 실패 확인** — `--tests '*CustomCameraViewModelTest'`, Expected: 컴파일 실패
- [ ] **Step 3: 구현** — `handleOnZoomRangeReady`: `halfZoomSource = of(...)`, `zoomLevels = available(range, source)`, `zoomLevel = ONE`.
  `handleOnClickFlip`: `zoomLevels = listOf(ONE)`, `halfZoomSource = NONE` 추가. route의 `zoomRatio = state.cameraZoomRatio` (컴파일 유지).
- [ ] **Step 4: 통과 확인** — Expected: 13 tests PASS
- [ ] **Step 5: 커밋** — `git commit -m "feat: 0.5x를 초광각 전환으로도 낼 수 있게 상태 확장"`

---

### Task 6: 바인딩·재적용·실기기·문서

**Files:**
- Modify: `.../component/CameraPreviewComponent.kt`, `.../route/CustomCameraRoute.kt`
- Modify: 문서 (Task 3 Step 6과 같은 목록) + 스펙·계획 재아카이브

**Interfaces:**
- Consumes: Task 4 `findUltraWideCamera`, `LensOptics`; Task 5 `useUltraWideCamera`, `cameraZoomRatio`, `OnZoomRangeReady(range, hasUltraWideCamera)`
- Produces:
  ```kotlin
  internal fun CameraPreviewViewComponent(
      lensFacing: Int, useUltraWideCamera: Boolean, zoomRatio: Float, hasPermission: Boolean,
      onImageCaptureReady: (ImageCapture) -> Unit,
      onZoomRangeReady: (range: ClosedFloatingPointRange<Float>, hasUltraWideCamera: Boolean) -> Unit,
  ): CameraPreviewHandle
  ```

- [ ] **Step 1: 바인딩** — `DisposableEffect(lensFacing, useUltraWideCamera, hasPermission)`.
  리스너 안에서 `cameraProvider.availableCameraInfos`를 `Camera2CameraInfo`로 읽어 후면 `LensOptics` 목록을 만들고,
  메인은 `CameraSelector.DEFAULT_BACK_CAMERA.filter(infos).first()`. 후면이면 `findUltraWideCamera`.
  `useUltraWideCamera && 초광각 있음`이면 그 `cameraId`만 남기는 `addCameraFilter` 셀렉터, 아니면 기존 `requireLensFacing`.
  범위 보고는 메인(초광각이 아닌 바인딩)일 때만 `onZoomRangeReady(range, ultraWide != null)` — 전면은 `false`.
  광학값 조회 실패(특성 null)는 그 카메라를 후보에서 뺀다.
- [ ] **Step 2: 재적용** — 기존 `LaunchedEffect(zoomRatio, camera)` 유지 + `DisposableEffect(camera)`로 `cameraInfo.cameraState`를 관찰해
  `CameraState.Type.OPEN`마다 `rememberUpdatedState`한 현재 배율로 `setZoomRatio`. 이유 한 줄 주석(ON_STOP에서 CameraX가 줌을 1.0으로 되돌린다).
- [ ] **Step 3: route** — `useUltraWideCamera = state.useUltraWideCamera`, `zoomRatio = state.cameraZoomRatio`,
  `onZoomRangeReady = { range, hasUltraWide -> viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(range, hasUltraWide)) }`.
- [ ] **Step 4: 빌드·테스트·lint** — Task 3 Step 4 명령, Expected: BUILD SUCCESSFUL
- [ ] **Step 5: 실기기(SM-A356N)** — 후면 1x → 2x → 0.5x(화각이 넓어짐) → 1x, 0.5x에서 촬영 후 확인 화면 진입, 2x에서 홈→복귀 시 프리뷰 2x 유지,
  0.5x에서 전면 전환 시 1x·버튼 1x↔2x, 크래시 로그 없음.
- [ ] **Step 6: 문서** — `status.md` 줌 줄에 0.5x 두 경로와 재적용 반영, 스펙 `implemented`·계획 `done` 후 다시 `archive/`, README 행 갱신, `check_links` exit 0.
- [ ] **Step 7: 커밋** — 코드 `feat: 초광각 바인딩과 줌 재적용 연결`, 문서 `docs: 카메라 줌 버튼 초광각 경로 반영`

---

### Task 7: 범위 재보고 시 단계 유지와 테스트 보강

**Files:**
- Modify: `.../viewmodel/CustomCameraViewModel.kt`, `.../model/CameraZoomLevel.kt`(필요 시)
- Test: `.../viewmodel/CustomCameraViewModelTest.kt`, `.../util/UltraWideCameraTest.kt`

**Interfaces:**
- Produces: `CustomCameraIntent.OnZoomRangeReady(range, hasUltraWideCamera: Boolean)` — 기본 인자 제거. 호출부·테스트 전부 명시.

- [ ] **Step 1: 실패하는 테스트**

```kotlin
@Test fun zoom_rangeReportedAgain_keepsLevelIfStillAvailable() // 0.5f..8f, 클릭 1회(TWO) → 같은 범위 재보고 → TWO 유지
@Test fun zoom_rangeReportedAgain_fallsBackToOneIfUnavailable() // 1f..8f,true, 클릭 2회(HALF) → (1f..8f,false) → ONE
@Test fun zoom_ultraWideWithoutTwo_cyclesOneHalf()              // (1f..1f,true): 클릭 1·2회 → HALF, ONE
@Test fun zoom_flipFromUltraWide_unbindsUltraWide()             // (1f..8f,true), 클릭 2회 → OnClickFlip → useUltraWideCamera false
// UltraWideCameraTest
@Test fun ultraWide_exactlyAtThreshold_isFound()                // 지표가 정확히 main/0.7 → 선택
```
  `zoom_onNewRange_resetsToOneAndRebuildsLevels`는 (1f..1f) 재보고라 ONE이 그대로 맞다 — 유지.

- [ ] **Step 2: 실패 확인** — `./gradlew :feature:camera:impl:testDebugUnitTest`, Expected: 앞 2건 FAIL(나머지는 PASS여도 된다 — 공백 보강용)
- [ ] **Step 3: 구현** — `handleOnZoomRangeReady`: `zoomLevel = zoomLevel.takeIf { it in levels } ?: ONE`.
- [ ] **Step 4: 통과 확인** — Expected: 전부 PASS
- [ ] **Step 5: 커밋** — `fix: 재바인딩 뒤에도 줌 단계를 유지`

---

### Task 8: 바인딩 견고성과 부드러운 줌

**Files:**
- Modify: `.../component/CameraPreviewComponent.kt`, `.../util/UltraWideCamera.kt`

- [ ] **Step 1: 견고성** — 리스너 앞에서 `disposed` 플래그 확인(`onDispose`가 세운다). 초광각 `bindToLifecycle` 실패 시 메인 셀렉터로 재시도하고
  `onZoomRangeReady(range, false)`. `findBackUltraWideCamera`·`toLensOptics`를 `runCatching`으로 감싸 실패는 null.
- [ ] **Step 2: 부드러운 줌** — `LaunchedEffect(zoomRatio, camera)`: 같은 `camera`에서 배율만 바뀌면 `Animatable`로 현재 배율→목표를 약 200ms 동안
  프레임마다 `setZoomRatio`. `camera`가 바뀐 직후(첫 바인딩·재바인딩)는 바로 건다. 상태 갱신 없이 로컬에서만 돈다.
- [ ] **Step 3: 빌드·lint** — Task 3 Step 4 명령, Expected: BUILD SUCCESSFUL
- [ ] **Step 4: 커밋** — `feat: 같은 카메라 줌을 부드럽게 바꾸고 재바인딩 실패 대비`

---

### Task 9: 전환 덮개·실기기·문서

**Files:**
- Create: `.../model/CameraTransition.kt` (덮개 한 장: `frame: ImageBitmap`, `isFlip: Boolean`)
- Modify: `.../component/CameraPreviewHandle.kt`, `.../component/CameraPreviewComponent.kt`, `.../component/CameraFeedLayer.kt`, route

**Interfaces:**
- Produces: `CameraPreviewHandle.transition: State<CameraTransition?>`; `CameraFeedLayer(..., transition: CameraTransition?)`

- [ ] **Step 1: 캡처** — 바인딩 `DisposableEffect`의 `onDispose`에서 `previewView.bitmap`을 잡아 `CameraTransition`으로 둔다. `isFlip`은 `lensFacing`이 바뀐 재바인딩.
  권한 해제·화면 이탈로 인한 dispose는 덮개를 만들지 않는다.
- [ ] **Step 2: 해제** — 새 바인딩 뒤 `previewView.previewStreamState`가 `STREAMING`이 되면 덮개를 지운다.
- [ ] **Step 3: 그리기** — `CameraFeedLayer`의 `AndroidView` 위에 덮개 `Image`(`ContentScale.Crop`, 블러 — API 31+ `Modifier.blur`, 미만은 블러 없이 스크림),
  `isFlip`이면 `graphicsLayer { rotationY; cameraDistance }`로 0→180° 약 300ms. 해제는 `AnimatedVisibility` 페이드아웃 약 150ms.
- [ ] **Step 4: 빌드·테스트·lint** — Task 3 Step 4 명령
- [ ] **Step 5: 실기기(SM-A356N)** — 1x↔2x 부드러운 줌·검정 없음, 2x→0.5x·0.5x→1x 블러 덮개·검정 없음, 전면/후면 뒤집기, 「다시 찍기」 후 2x 유지,
  화면 녹화(`screenrecord`)로 프레임 확인, 크래시 없음.
- [ ] **Step 6: 문서·재아카이브** — `status.md` 줌 줄에 전환 연출 반영, 스펙 `implemented`·계획 `done`, README, `check_links` exit 0.
- [ ] **Step 7: 커밋** — 코드 `feat: 카메라 전환에 블러 덮개와 뒤집기 연출 추가`, 문서 `docs: 카메라 전환 연출 반영`

