---
id: camera-impl-refactor
title: feature:camera:impl 정리
status: done
type: work-order
created: 2026-11-05
updated: 2026-11-05
archived_reason: 구현 완료 (refactor/camera-impl, 2026-11-05) — 실기기 확인 미수행
platforms: android
owner: android
related_adr:
related_spec:
related_code:
  - CustomCameraViewModel.kt#CustomCameraViewModel
  - PictureConfirmViewModel.kt#PictureConfirmViewModel
  - CameraCrop.kt#computeCaptureCropRect
  - ViewfinderCapture.kt#captureViewfinder
  - CameraPreviewBinding.kt#CameraPreviewBindingEffect
tags: [plan, parfait]
---

# feature:camera:impl 정리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** `feature:camera:impl`의 컨벤션 위반·죽은 코드·스레드 문제를 걷고, 촬영 크롭을 CameraX `ViewPort` 기준으로 바꾸고, 사진 확인 화면을 MVI로 맞춘다.

**Architecture:** 비UI 타입은 `impl/model/`(`FlashMode`, `LensFacing`). 촬영은 Route의 effect 수집기에서 `util/ViewfinderCapture.kt`의 `suspend fun ImageCapture.captureViewfinder(...)`로 빠지고, 크롭 영역은 순수 함수 `computeCaptureCropRect`가 `ImageProxy.cropRect`(ViewPort가 정한 보이는 영역, 버퍼 좌표) 안에서 뷰파인더 비율을 찾아 버퍼 좌표로 돌려준다. `PictureConfirmViewModel`은 `BaseViewModel` + assisted inject로 바뀌고 내비게이션은 effect로 나간다.

**Tech Stack:** Kotlin 2.4.10, Compose runtime 1.12.0(`SideEffect(key)` 있음), CameraX 1.6.1, Hilt assisted inject, kotlin.test + mockk + turbine.

**Spec:** 없음. 결정은 PR #670 리뷰 뒤 점검(2026-11-05)에서 사용자가 정했다 — 아래 Decisions.

## Decisions

- A·B·C·D 묶음 전부 진행. State/Intent/Effect는 VM 파일에 둔다(형제 모듈 패턴).
- 재저장 JPEG 품질 95는 **의도**다. 바꾸지 않는다.
- `LensFacing` enum 도입, `ViewPort`로 크롭 기준을 바꾼다.
- `ImageProxy.toBitmap()`은 JPEG에서 **cropRect를 적용하지 않는다**(CameraX 1.6.1 `ImageUtil.createBitmapFromJpegImage`가 전체를 디코드). 그래서 cropRect는 버퍼(회전 전) 좌표로 직접 쓴다.
- `ViewPort`는 종횡비·scale type만 표현한다. 뷰파인더는 피드 안의 중앙이 아닌 하위 영역이라 **ViewPort가 크롭을 다 대신하지 못한다** — ViewPort는 "피드에 보이는 영역"까지를, 뷰파인더 비율은 우리 함수가 맡는다.

## Global Constraints

- 코드 주석·KDoc: [`docs/code-conventions.md`](../../../code-conventions.md). 코드가 말하는 것은 쓰지 않는다.
- `.claude/rules/no-local-functions.md` — 함수 안 `fun` 금지.
- 비UI 타입은 `impl/model/`, 파일당 타입 하나, enum 동작은 멤버.
- 커밋 메시지: `type: 한국어 설명`, Co-Authored-By 넣지 않는다. 푸시하지 않는다.
- 각 Task 끝: `./gradlew :feature:camera:impl:testDebugUnitTest :feature:camera:impl:ktlintCheck` 통과.
- 테스트는 kotlin.test `assertEquals` + mockk + turbine(형제 모듈 패턴). Truth·`@RunWith` 도입하지 않는다.
- 기존 파일을 전문으로 덮어쓰지 않는다 — 바꿀 부분만 고친다.
- JPEG 품질: 촬영 `setJpegQuality(100)`, 재저장 `95` 유지.

## Review Focus

1. 전면 카메라 촬영 — 저장된 사진이 화면 뷰파인더에 보이던 영역과 같아야 한다(프리뷰만 좌우 반전). → Task 5 테스트 `crop_frontFacing_mirrorsHorizontally`.
2. 첫 바인딩 때 `previewView.viewPort`가 `null`(레이아웃 전) — ViewPort 없이 바인딩되고, 크롭은 피드 종횡비 중앙 영역을 합성해 같은 결과를 낸다. → Task 5 `fallbackCropRect` 테스트.
3. 센서 회전 90/270(대부분의 후면 카메라 세로 촬영) — 버퍼 좌표 변환이 맞아야 한다. → Task 5 회전 4종 테스트.
4. 촬영 도중 화면 이탈 — 취소는 실패 토스트를 띄우지 않는다. → Task 6 `runSuspendCatching`.
5. 셔터 연타 — 확인 화면 이동은 한 번. → Task 4 `shutter_whileCapturing_ignored`.

---

### Task 1: 권한 상태·인텐트 이름 정리

**Files:**
- Modify: `viewmodel/CustomCameraViewModel.kt`, `route/CustomCameraRoute.kt`, `screen/CustomCameraScreen.kt`, `component/CameraPermissionRequestComponent.kt`, `component/CameraPreviewBinding.kt`
- Test: `src/test/.../viewmodel/CustomCameraViewModelTest.kt`

(경로는 `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/` 기준. 이하 같다.)

**Interfaces:**
- Produces: `CustomCameraIntent.OnPermissionChecked(granted: Boolean)`, `CustomCameraIntent.OnPermissionRequestResult(granted: Boolean)`, `CustomCameraIntent.OnClickOpenAppSettings`, `CustomCameraState.isInitialized`, `CustomCameraState.shouldUseUltraWideCamera`, `CameraPreviewBindingEffect(..., shouldUseUltraWideCamera: Boolean, ...)`

- [ ] **Step 1:** 테스트를 새 이름으로 고치고, 추가한다:
  - `permission_whenGrantedAfterRequest_hasPermission` — `OnPermissionRequestResult(granted = true)` 뒤 `state.value.hasPermission == true`.
  - `flash_toggle_switchesOnAndOff` — `OnClickFlash` 두 번에 `ON` → `OFF`.
  - `cancel_postsCancel` — `OnCancel` → `CustomCameraEffect.Cancel`.
- [ ] **Step 2:** 컴파일 실패 확인: `./gradlew :feature:camera:impl:testDebugUnitTest` → 미해결 참조.
- [ ] **Step 3:** 구현.
  - `permanentlyDenied` 필드, `OnPermissionRequestResult.shouldShowRationale`, Route의 `shouldShowRationale(...)` 계산과 import를 지운다.
  - 이름 변경: `OnPermissionResult` → `OnPermissionChecked`, `OnOpenAppSettings` → `OnClickOpenAppSettings`(핸들러도 `handleOnClickOpenAppSettings`), `isInit` → `isInitialized`(`CameraPermissionRequestComponent` 파라미터 포함), `useUltraWideCamera` → `shouldUseUltraWideCamera`.
  - `CameraPreviewBinding.kt` 지역 변수: `bindsUltraWide` → `shouldBindUltraWide`, `ultraWideFailed` → `isUltraWideFailed`.
  - `CustomCameraEffect.Cancel` KDoc에서 "예전처럼"을 뺀다: "`null` 을 결과 버스에 흘리면 그것을 결과로 아는 화면이 받아 터진다."
- [ ] **Step 4:** 테스트·ktlint 통과.
- [ ] **Step 5:** 커밋 `refactor: 카메라 권한 죽은 상태를 걷고 인텐트 이름을 맞춤`

### Task 2: `FlashMode`·`LensFacing`을 model로

**Files:**
- Create: `model/FlashMode.kt`, `model/LensFacing.kt`
- Modify: `viewmodel/CustomCameraViewModel.kt`(enum 삭제, `lensFacing` 타입), `component/CameraControlComponent.kt`, `screen/CustomCameraScreen.kt`, `route/CustomCameraRoute.kt`, `component/CameraPreviewBinding.kt`, `model/CapturedFrame.kt`
- Test: `CustomCameraViewModelTest.kt`, `src/test/.../model/LensFacingTest.kt`

**Interfaces:**
- Produces:
  - `enum class FlashMode { OFF, ON; operator fun not(): FlashMode; val imageCaptureFlashMode: Int }` — `ImageCapture.FLASH_MODE_OFF/ON`. Route의 `when` 매핑을 대체.
  - `enum class LensFacing { BACK, FRONT; fun flipped(): LensFacing; val cameraSelectorLens: Int }` — `CameraSelector.LENS_FACING_BACK/FRONT`.
  - `CustomCameraState.lensFacing: LensFacing = LensFacing.BACK`
  - `CameraPreviewBindingEffect(lensFacing: LensFacing, ...)`, `CapturedFrame(bitmap: Bitmap, lensFacing: LensFacing)`

- [ ] **Step 1:** `LensFacingTest`: `flipped_back_isFront`, `flipped_front_isBack`, `cameraSelectorLens_matchesCameraX`(BACK → `CameraSelector.LENS_FACING_BACK`, FRONT → `LENS_FACING_FRONT`). VM 테스트에 `flip_togglesLensFacing` — `OnClickFlip` 뒤 `lensFacing == LensFacing.FRONT`.
- [ ] **Step 2:** 실패 확인.
- [ ] **Step 3:** 구현. VM의 `handleOnClickFlip`은 `lensFacing.flipped()`. 바인딩은 `requireLensFacing(lensFacing.cameraSelectorLens)`, 초광각 탐색 조건은 `lensFacing == LensFacing.BACK`. 기존 VM 테스트의 `CameraSelector.LENS_FACING_*` 참조를 바꾼다.
- [ ] **Step 4:** 테스트·ktlint 통과.
- [ ] **Step 5:** 커밋 `refactor: 플래시·렌즈 방향을 model enum으로 옮김`

### Task 3: 사진 확인 화면 MVI

**Files:**
- Modify: `viewmodel/PictureConfirmViewModel.kt`, `route/PictureConfirmRoute.kt`, `screen/PictureConfirmScreen.kt`
- Test: Create `src/test/.../viewmodel/PictureConfirmViewModelTest.kt`

**Interfaces:**
- Produces (모두 `PictureConfirmViewModel.kt` 안):
  - `data class PictureConfirmState(val showQuitDialog: Boolean = false) : UiState`
  - `sealed interface PictureConfirmIntent : UiIntent { OnClickRetake; OnClickConfirm; OnClickClose; OnDismissQuit; OnConfirmQuit }`
  - `sealed interface PictureConfirmEffect : UiSideEffect { NavigateBack; ReturnResult(uri: String, source: PictureConfirmSource); NavigateToSegmentation(uri: String); QuitToCanvas }` — 닫기 팝업은 경로와 무관하게 캔버스로 간다(문구만 `returnResultOnly`로 갈린다)
  - `@HiltViewModel(assistedFactory = PictureConfirmViewModel.Factory::class) class PictureConfirmViewModel @AssistedInject constructor(@Assisted uri: String, @Assisted source: PictureConfirmSource, @Assisted returnResultOnly: Boolean, ...)` + `Factory.create(uri, source, returnResultOnly)`. 패턴은 `SegmentationViewModel`.
  - `PictureConfirmScreen(..., onClickRetake: () -> Unit, ...)`

- [ ] **Step 1:** 테스트(이펙트는 turbine, 의존성은 mockk `relaxed = true`):
  - `init_notReturnResultOnly_preparesSegmentation` — 생성만으로 `prepareSegmentationModuleUseCase()` 1회 호출.
  - `init_returnResultOnly_doesNotPrepare` — 0회.
  - `retake_camera_logsAndNavigatesBack` — `CANVAS_CAMERA_RETAKE_TAPPED` 1회 + `NavigateBack`.
  - `retake_gallery_navigatesBackWithoutLog` — 로그 0회 + `NavigateBack`.
  - `confirm_returnResultOnly_returnsResult` — `ReturnResult(uri, source)`.
  - `confirm_otherwise_navigatesToSegmentation` — `NavigateToSegmentation(uri)`.
  - `close_showsQuitDialog_thenConfirmQuits` — `OnClickClose` → `showQuitDialog == true`, `OnConfirmQuit` → `false` + `QuitToCanvas`.
- [ ] **Step 2:** 실패 확인.
- [ ] **Step 3:** 구현.
  - 모듈 준비는 `init`에서 `returnResultOnly`가 거짓일 때만 `launch { runSuspendCatching { ... }.onFailure { 로그 } }`. 화면 재진입마다 다시 부르던 `LaunchedEffect`를 지운다.
  - 클래스 KDoc 3줄은 한 줄 포인터로: "모듈 준비가 여기인 이유: `parfait/specs/2026-09-02-segmentation-module-install.md`".
  - Route: `viewModel: PictureConfirmViewModel = hiltViewModel<PictureConfirmViewModel, PictureConfirmViewModel.Factory>(creationCallback = { it.create(uri, source, returnResultOnly) })`를 파라미터 맨 뒤 기본값으로. `rememberSaveable` 다이얼로그 상태와 지역 `fun quitToCanvas()`를 지우고 `state.showQuitDialog`를 읽는다. `LaunchedEffect(viewModel) { viewModel.effect.collect { ... } }`에서 `navigator.onBack()`, `resultEventBus.sendResult(...)` + `popUpTo<NavKeyCanvasBGEdit>()`, `goToSingleClearTop(NavKeySegmentation(...))`, `popUpTo<NavKeyCanvasMain>()`. 기존 Route 주석 둘(되감기 이유, 백스택 유지 이유)은 각 분기로 옮긴다.
  - Screen: `onClickReCapture` → `onClickRetake`. 같은 `Image`가 두 분기에 중복된 것(`:80-103`)은 하나로 합친다.
- [ ] **Step 4:** 테스트·ktlint 통과.
- [ ] **Step 5:** 커밋 `refactor: 사진 확인 화면을 MVI로 맞추고 모듈 준비를 init으로 옮김`

### Task 4: 셔터 연타 가드

**Files:**
- Modify: `viewmodel/CustomCameraViewModel.kt`
- Test: `CustomCameraViewModelTest.kt`

**Interfaces:**
- Produces: `CustomCameraState.isCapturing: Boolean = false`

- [ ] **Step 1:** 테스트(`createCameraCacheFileUseCase`는 `File("a.jpg")`를 돌려주는 mockk):
  - `shutter_postsCaptureImage_andMarksCapturing`
  - `shutter_whileCapturing_ignored` — `OnClickShutter` 두 번 → `CaptureImage` 하나, 이후 `expectNoEvents()`.
  - `captureSaved_clearsCapturing_andNavigates` — `NavigateToConfirm(uri)` + `isCapturing == false`.
  - `captureFailed_clearsCapturing_andPostsFailed`
- [ ] **Step 2:** 실패 확인.
- [ ] **Step 3:** 구현. `handleOnClickShutter`는 `isCapturing`이면 반환, 아니면 참으로 두고 이펙트. `OnCaptureSaved`·`OnCaptureFailed`가 내린다. 셔터 UI는 바꾸지 않는다(VM에서 무시).
- [ ] **Step 4:** 테스트·ktlint 통과.
- [ ] **Step 5:** 커밋 `fix: 촬영 중 셔터 연타를 무시함`

### Task 5: ViewPort 기준 크롭 함수

**Files:**
- Modify: `util/CameraCrop.kt`(아래 함수 추가)
- Test: Create `src/test/.../util/CameraCropTest.kt`

**Interfaces:**
- Produces:
  - `internal fun computeCaptureCropRect(viewfinderRect: Rect, feedRect: Rect, visibleRect: IntRect, rotationDegrees: Int, isFrontFacing: Boolean): IntRect?` — 반환은 **버퍼 좌표**. `visibleRect`는 버퍼 좌표의 "피드에 보이는 영역"(보통 `ImageProxy.cropRect`).
  - `internal fun fallbackCropRect(bufferSize: IntSize, rotationDegrees: Int, feedSize: Size): IntRect` — ViewPort 없이 바인딩됐을 때 FILL_CENTER와 같은 중앙 영역을 버퍼 좌표로.
  - `internal fun saveCroppedCapture(captured: Bitmap, cropRect: IntRect?, rotationDegrees: Int, file: File)` — `Bitmap.createBitmap(captured, x, y, w, h, Matrix().apply { postRotate(deg) }, true)` **한 번**으로 크롭+회전, `JPEG_QUALITY = 95`로 저장. `cropRect`가 `null`이면 전체를 회전만.

- [ ] **Step 1:** `CameraCropTest`. 공통: `feedRect = Rect(0f, 0f, 1000f, 2000f)`, 뷰파인더 `Rect(100f, 400f, 900f, 1200f)` → 표시 공간 비율 x 0.1~0.9, y 0.2~0.6.
  - `crop_rotation0_mapsFractionIntoVisibleRect` — `visibleRect = IntRect(0, 0, 1000, 2000)` → `IntRect(100, 400, 900, 1200)`.
  - `crop_rotation90_mapsToBuffer` — 버퍼 2000×1000(가로), `visibleRect = IntRect(0, 0, 2000, 1000)` → `IntRect(400, 100, 1200, 900)`. (표시 (u,v)에서 버퍼로: x = v, y = 1 − u)
  - `crop_rotation180` — 버퍼 1000×2000 → `IntRect(100, 800, 900, 1600)`. (x = 1 − u, y = 1 − v)
  - `crop_rotation270` — 버퍼 2000×1000 → `IntRect(800, 100, 1600, 900)`. (x = 1 − v, y = u)
  - `crop_visibleRectOffset_addsOrigin` — 회전 0, `visibleRect = IntRect(50, 100, 1050, 2100)` → `IntRect(150, 500, 950, 1300)`.
  - `crop_frontFacing_mirrorsHorizontally` — 회전 0, 뷰파인더 `Rect(0f, 400f, 500f, 1200f)` → `IntRect(500, 400, 1000, 1200)`.
  - `crop_viewfinderOutsideFeed_clamps` — 뷰파인더 `Rect(-100f, -100f, 1100f, 2100f)` → `visibleRect` 전체.
  - `crop_emptyFeed_returnsNull`, `crop_emptyIntersection_returnsNull`.
  - `fallback_rotation90_centersFeedAspect` — 버퍼 4000×3000, 회전 90, 피드 1000×2000 → 표시 3000×4000에서 피드 비율 1:2 중앙 = 폭 2000·높이 4000 → 버퍼 `IntRect(0, 500, 4000, 2500)`.
  - `fallback_rotation0_sameAspect_isFull` — 버퍼 1000×2000, 피드 500×1000 → `IntRect(0, 0, 1000, 2000)`.
- [ ] **Step 2:** 실패 확인.
- [ ] **Step 3:** 구현. 계산 순서: 뷰파인더의 피드 내 비율(0..1로 clamp) → 전면이면 u를 `1 − u`로 뒤집고 좌우 교환 → 위 회전 공식으로 버퍼 비율 → `visibleRect` 원점·크기로 픽셀화, min/max 정규화 → 넓이 0이면 `null`. `fallbackCropRect`는 90/270이면 버퍼 폭·높이를 바꿔 표시 크기로 보고, 피드 비율 중앙 영역을 잡은 뒤 같은 회전 공식의 역으로 버퍼 좌표로. KDoc은 "`visibleRect`·반환값은 버퍼(회전 전) 좌표다" 한 줄만 — 회전 공식은 테스트가 지킨다. 상수 `JPEG_QUALITY`는 KDoc이 붙지 않는 자리(파일 맨 아래)에 둔다.
- [ ] **Step 4:** 테스트·ktlint 통과. 이 Task는 새 함수를 **추가만** 한다 — 기존 `saveViewfinderCapture`·`computeCropRect`는 Route가 아직 쓰므로 Task 6에서 지운다.
- [ ] **Step 5:** 커밋 `refactor: 촬영 크롭을 버퍼 좌표 한 번 계산으로 바꿈`

### Task 6: ViewPort 바인딩과 촬영 함수

**Files:**
- Create: `util/ViewfinderCapture.kt`
- Modify: `component/CameraPreviewBinding.kt`, `model/CameraPreviewState.kt`, `route/CustomCameraRoute.kt`, `util/CameraCrop.kt`(`saveViewfinderCapture`·`computeCropRect`·`rotate`·`crop` 삭제)

**Interfaces:**
- Consumes: Task 5의 `computeCaptureCropRect`, `fallbackCropRect`, `saveCroppedCapture`. Task 2의 `LensFacing`.
- Produces:
  - `CameraPreviewState.isViewPortApplied: Boolean` (일반 필드, 바인딩이 씀)
  - `internal suspend fun ImageCapture.captureViewfinder(file: File, viewfinderRect: Rect?, feedRect: Rect?, isFrontFacing: Boolean, isViewPortApplied: Boolean)` — 실패는 예외로 던진다.

- [ ] **Step 1:** 바인딩. `previewView.viewPort`가 있으면 `UseCaseGroup.Builder().setViewPort(it).addUseCase(preview).addUseCase(imageCapture).build()`로 `bindToLifecycle(lifecycleOwner, selector, group)`, 없으면 지금처럼 개별 use case로 바인딩하고 `isViewPortApplied = false`. `bindUseCases`가 둘을 가른다.
- [ ] **Step 2:** `captureViewfinder` 구현. `suspendCancellableCoroutine`으로 `takePicture(Dispatchers.IO.asExecutor(), callback)`. 콜백 안(IO 스레드)에서 `rotationDegrees`·`cropRect`를 읽고 `toBitmap()` 후 `close()`, 그다음 크롭 영역 계산(`isViewPortApplied`면 `image.cropRect`를 `visibleRect`로, 아니면 `fallbackCropRect`), `saveCroppedCapture`. 성공·실패를 `resume`으로 넘긴다. 메인 스레드에서 디코드하지 않는 것이 이 함수의 목적이다.
- [ ] **Step 3:** Route의 `CaptureImage` 분기를 `captureScope.launch { val saved = runSuspendCatching { capture.captureViewfinder(...) }.isSuccess; viewModel.processIntent(...) }`로. `runSuspendCatching`은 `com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching`. `isFrontFacing = state.lensFacing == LensFacing.FRONT`.
- [ ] **Step 4:** 테스트·ktlint 통과. **실기기 확인(사람)**: 후면·전면·0.5x 초광각 각각 촬영해 확인 화면 사진이 뷰파인더와 같은 영역인지. 결과를 PR 본문에 적는다.
- [ ] **Step 5:** 커밋 `refactor: 촬영을 ViewPort 기준으로 IO에서 처리함`

### Task 7: Compose 정리

**Files:**
- Modify: `screen/CustomCameraScreen.kt`, `component/CameraControlComponent.kt`, `component/CameraFeedLayer.kt`, `route/CustomCameraRoute.kt`, `component/CameraPreviewBinding.kt`

- [ ] **Step 1:** `CameraContent` 파라미터 순서: `toastPolicy, flashMode, zoomLevel, isZoomButtonVisible, onClick…, onViewfinderRectChange, modifier, cameraFeed`. `CustomCameraScreen`의 `when (state.hasPermission)`은 `if/else`.
- [ ] **Step 2:** `CameraControlComponent`의 자식 하나짜리 `Column(SpaceBetween)`을 걷고 `Row`에 `modifier`를 붙인다.
- [ ] **Step 3:** `onGloballyPositioned { boundsInRoot() }` 두 곳(`CustomCameraScreen` 뷰파인더, `CameraFeedLayer` 피드)을 `Modifier.onLayoutRectChanged(debounceMillis = 0) { onXxx(it.boundsInRoot.toRect()) }`로. debounce를 0으로 두는 이유: 뷰파인더 영역을 그리기에도 쓰므로 늦으면 첫 프레임이 어긋난다.
- [ ] **Step 4:** `CameraFeedLayer`의 `drawWithContent`를 `drawWithCache { ... onDrawWithContent { ... } }`로. `BlurEffect`·`Stroke`는 캐시 블록에서 한 번, 모서리 선은 `Pair` 목록 없이 `drawLine` 8번. 기본값 외에 아무도 안 넘기는 `isBlurSupported` 파라미터는 내부 `val`로.
- [ ] **Step 5:** Route: 안내 토스트(`:94`)와 플래시(`:190`)의 `LaunchedEffect(key)`를 `SideEffect(key)`로. `LifecycleResumeEffect { ...; onPauseOrDispose { } }`를 `LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { ... }`로.
- [ ] **Step 6:** `CameraPreviewBindingEffect`의 `onImageCaptureReady`·`onZoomRangeReady`를 `rememberUpdatedState`로 감싸 리스너가 그것을 부르게 한다.
- [ ] **Step 7:** 테스트·ktlint 통과. 에뮬레이터에서 화면이 뜨고 셔터·플립·플래시·줌이 도는지 확인.
- [ ] **Step 8:** 커밋 `refactor: 카메라 화면 Compose 관례를 맞춤`

### Task 8: 주석·import·문서

**Files:**
- Modify: `route/CustomCameraRoute.kt`, `screen/CustomCameraScreen.kt`, `component/CameraFeedLayer.kt`, 그 밖에 import가 어긋난 모듈 파일, `docs/status.md`, `docs/superpowers/plans/README.md`, 이 계획(`status: done`, `archive/`로 이동)

- [ ] **Step 1:** Route `:216`의 "Box 안에 YGToastHost…" 주석을 `CustomCameraScreen`의 해당 `Box` 위로 옮긴다. `CustomCameraScreen:129`의 "10.dp가 없어서 넣었습니다"는 그대로 두되 `// 디자인 토큰에 10dp gap 이 없다`로. `CameraFeedLayer:60`(코드 재진술) 줄을 뺀다.
- [ ] **Step 2:** 모듈 전체 import를 알파벳 순으로(ktlint `import-ordering`은 꺼져 있다 — 손으로).
- [ ] **Step 3:** `docs/status.md` 커스텀 카메라 항목에 촬영 크롭 gotcha 한 줄: ViewPort가 피드에 보이는 영역을, `computeCaptureCropRect`가 그 안의 뷰파인더를 맡고, `toBitmap()`은 cropRect를 적용하지 않는다. 앵커 갱신.
- [ ] **Step 4:** 계획을 `archive/`로 옮기고 README 표를 고친다. `python3 docs/script/check_links.py docs` 0건.
- [ ] **Step 5:** 커밋 `docs: 카메라 모듈 정리 반영`
