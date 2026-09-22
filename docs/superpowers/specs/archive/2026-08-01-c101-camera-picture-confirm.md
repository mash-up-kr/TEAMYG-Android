---
id: c101-camera-picture-confirm
title: C-101 커스텀 카메라 · C-101-confirm 사진 확인 화면 (Custom Camera / Picture Confirm)
status: implemented
category: ui-spec
platforms: android
verified: 2026-09-21
related_code: PictureConfirmResult, CameraFeedLayer, CameraPermissionRequestComponent, CustomCameraRoute, CameraPreviewViewComponent, CameraPreviewHandle, CameraControlComponent, CameraCrop, CustomCameraViewModel, CustomCameraScreen, PictureConfirmScreen, NavKeyPictureConfirm, PictureConfirmSource, GalleryPermissionRequestComponent, DateTextFormat
related_adr: ADR-0018, ADR-0006
related_spec: designsystem-button-missing-components, g001-group-list, c102-custom-gallery-picker, c103-segmentation-topping-edit, c301-canvas-background-edit
related_architecture: navigation-flow, design-system, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, camera, c101]
---

# Spec: C-101 커스텀 카메라 · C-101-confirm 사진 확인 화면

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #182(`feature/#170-camera-screen`)가 develop에
> 머지됐다(2026-08-01). 아래는 머지 코드를 역기록한 것이고, 설계 대조가 아니라 **정책(위키)·규약
> (parfait) 대조**로 드리프트를 표기한다.

## 목표
[[카메라-뷰파인더]] 정책의 C-101(촬영)·C-101-confirm(확인) 화면 플로우를 세운다.
단일 카메라 피드 위에서 **뷰파인더 안쪽만 선명·바깥은 블러**를 구현하고, 촬영 결과를 화면에서 보던
뷰파인더 영역 그대로 잘라 저장한 뒤 확인 화면으로 넘긴다.

## 범위
- **포함**:
  - `CameraFeedLayer` — 카메라 피드 1개를 `GraphicsLayer`로 기록해 두 번 그리는 블러 합성 + 뷰파인더
    테두리·코너 마커 + 뷰파인더 안쪽 탭 포커스(`FocusMeteringAction`).
  - `CameraPreviewViewComponent`(CameraX 바인딩) + `CameraPreviewHandle`(`PreviewView`·`Camera` 상태 반환).
  - `CameraCrop` — 화면 뷰파인더 좌표 → 촬영 이미지 픽셀 좌표 역산·회전 보정·JPEG 저장.
  - 컨트롤 영역 치환 — 임시 `ShutterButton`·`FlipCameraButton` 삭제, `YGCameraShutter`·`YGCircleButton`
    (플래시·전환·닫기) 사용.
  - 플래시 on/off(`FlashMode` enum + `not()` 연산자 오버로딩) → `ImageCapture.flashMode` 반영.
  - 촬영 성공 시 `NavKeyPictureConfirm(uri)`로 이동 + `PictureConfirmScreen`(이미지 확인·다시 찍기·다음).
  - 권한 거부 화면 재디자인(카메라·갤러리 공통 형태: 경고 아이콘 + 제목·설명 + "설정으로 이동").
  - 촬영 가이드 토스트(`YGToastPolicy`/`YGToastHost` 첫 실사용처).
  - 카메라·갤러리 `strings.xml` 신설, 갤러리 빈 상태 그래픽(`image_gallery_empty`)·플래시 아이콘 추가.
  - 공용 날짜 포맷 `core:util:jvm` `model/DateTextFormat`(요일·월일 축약) → 상단 `YGDate` 표시.
- **제외**(이번 라운드에서 안 함):
  - ~~확인 화면 이후 경로~~ — **"다음"은 #221(2026-08-14)에서 결선됐다**: `navigator.goToAndPopCurrent(NavKeySegmentation(sourceImageUri = uri))`.
    `goTo`가 아니라 `goToAndPopCurrent`라 확인 화면은 백스택에서 걷히고, 세그멘테이션에서 뒤로 가면 촬영/갤러리로 돌아간다.
    `feature/camera/impl` → `feature/segmentation/api` 의존이 함께 추가됐다(규약대로 `:api`만).
    ~~**닫기(C-001 캔버스)는 여전히 `onClickClose = {}` TODO다.**~~ → ✅ **#309(2026-08-20)로 결선됐다**: `navigator.popUpTo<NavKeyCanvasMain>()`(배경 편집 경로는 `popUpTo<NavKeyCanvasBGEdit>()`).
  - 줌 UI — 상태·인텐트는 살아 있으나 화면에 노출되는 컨트롤이 없다(아래 "잔존").
  - 시스템 카메라 화면(`SystemCameraScreen`)의 디자인 정합 — 맨 Material3 위젯 그대로.

## 동작 / 구조

### 뷰파인더 블러 합성 (`CameraFeedLayer`)
1. `contentLayer.record { drawContent() }` — 카메라 피드를 한 번 기록.
2. `blurLayer.renderEffect = BlurEffect(...)` 후 `blurLayer.record { drawLayer(contentLayer) }` → 전체를
   흐리게 그린다. 블러 미지원(API 31 미만)이면 `contentLayer`를 그대로 그린다.
3. `Transparency.Black25` 스크림을 전면에 얹는다.
4. 뷰파인더 `Rect`로 `clipRect` 후 `contentLayer`를 다시 그려 **그 영역만 원본으로 복원**한다.
5. 뷰파인더 테두리(`Gray500` `Stroke`) + 안쪽으로 밀어낸 코너 마커 8선분을 그린다.

- 뷰파인더 위치는 `CustomCameraScreen`이 `onGloballyPositioned`로 통지하고(`onViewfinderRectChange`),
  피드 자신의 위치는 `onFeedRectChange`로 올려 크롭 계산에 쓴다. 즉 **레이아웃은 화면이, 렌더는 피드가**
  맡는다(화면 쪽 뷰파인더 자리는 빈 `Box`).
- `PreviewView.implementationMode = COMPATIBLE` — `GraphicsLayer` 기록이 되려면 `SurfaceView`가 아닌
  `TextureView`여야 한다(코드 주석).

> ⚠️ **[ADR-0018](../../../adr/0018-backdrop-blur-haze.md)과의 관계** — ADR-0018은 Top Bar **배경 블러**에서
> 자체 `GraphicsLayer` 방식을 기각하고 Haze를 채택하면서 "C-101도 같은 구조이니 그 라운드에서 재검토"를
> 남겼다. 실제로는 재검토 없이 자체 구현으로 머지됐다. 두 경우는 대상이 다르다 — 여기서 흐리는 것은
> **자기 자식(카메라 피드)**이고 ADR-0018이 막힌 것은 **자기 밖 배경**을 레이어로 옮겨 담는 경로다.
> 따라서 정합성 문제는 "동작 여부"가 아니라 **관용구가 둘로 갈린 것**이다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-01].
> 블러 실동작은 ADR-0018이 경고한 대로 **극단값 대조로 검증해야 한다**(이 라운드에서 그 기록은 없다).

### 촬영 → 저장 → 확인
- 셔터 → `CustomCameraIntent.OnClickShutter` → `CreateCameraCacheFileUseCase`로 파일 생성 →
  `CustomCameraEffect.CaptureImage(file)`.
- Route가 `ImageCapture.takePicture`로 받은 `ImageProxy`를 비트맵으로 바꾸고 `Dispatchers.IO`에서
  `saveViewfinderCapture`를 호출한다.
- `computeCropRect` — `PreviewView`가 `FILL_CENTER`로 그리는 배율·중앙 정렬을 역으로 적용해 화면
  좌표를 이미지 좌표로 옮긴다. 전면 카메라는 프리뷰만 좌우 반전되므로 이미지 좌표에서 되돌린다.
  계산 불가(영역 0 이하 등)면 `null`을 돌려 **크롭 없이 전체 프레임을 저장**한다.
- 저장 성공 → `OnCaptureSaved` → `CreateCameraCacheUriUseCase` → `NavigateToConfirm(uri)`.
  실패·취소는 `ReturnResult(null)` + `LocalResultEventBus`로 호출 화면에 반환.

### 확인 화면 as-built 갱신 (2026-08-04, PR #191)
갤러리 라운드가 이 화면을 **두 진입점 공용**으로 바꿨다 → [c102 스펙](2026-08-04-c102-custom-gallery-picker.md).
- `NavKeyPictureConfirm(uri, source)` — 신설 `PictureConfirmSource`(CAMERA/GALLERY)가 인자로 추가됐고,
  엔트리 빌더가 `navKey.source`를 Route에 그대로 넘긴다. 화면은 이 값으로 좌측 버튼 문구만 가른다
  ("다시 찍기" / "다시 선택"). 뒤로 동작은 양쪽 다 `navigator.onBack()`이다.
- **이미지 노출이 위키 [[이미지-렌더링-정책]]에 맞춰졌다.** 이전 구현은 이미지 영역에 늘 테두리를
  둘렀으나, 지금은 `BoxWithConstraints`에서 원본 비율과 영역 비율을 비교해 분기한다 — 가로가 먼저
  차면 **컨테이너·테두리 없이 단독 노출**(Case A, 가로 100%·중앙 정렬), 세로가 먼저 차면 **흰 배경 +
  `Gray500` 테두리 컨테이너** 안에 `ContentScale.Fit`으로 담는다(Case B). 컨테이너는 잔여 영역을
  가득 채우지만 세로 한계 이미지라 실질 높이는 이미지 높이와 같다.
  원본 크기를 모르는 첫 프레임(비동기 로드 전)은 Case A로 떨어진다.
- 갤러리 쪽 잔존 지적 2건이 이 PR로 정리됐다 — 빈 상태 그래픽이 `isEmpty` 분기 **안으로** 들어갔고
  빈 상태 문구가 `strings.xml`로 갔다(그래픽 자체는 벡터 → 밀도별 PNG로 교체). **로딩 인디케이터가
  흰 배경 위 흰색인 것은 그대로다.**

### 확인 화면 as-built 갱신 (2026-08-15, PR #231)
배경 편집 라운드가 카메라·확인 화면을 **두 플로우 공용**으로 바꿨다 →
[c301 스펙](2026-08-15-c301-canvas-background-edit.md).
- `NavKeyCameraCustom`이 `data object` → `data class(showGuideToast, returnResultOnly)`로 승격되고
  `NavKeyPictureConfirm`에 `returnResultOnly`가 붙었다. 둘 다 기본값이 있어 기존 호출부는 생성자
  호출로만 바뀌었다(동작 불변).
- `showGuideToast = false`면 진입 가이드 토스트를 띄우지 않는다(`hasShownGuideToast` 게이트 앞에 조건 추가).
  > 📌 **권한 게이트가 하나 더 붙었다(2026-08-25, PR #350)** — 아래 「권한 화면 as-built 갱신」 참고.
- 확인 버튼이 갈린다 — `returnResultOnly`면 신설 `PictureConfirmResult(uri, source)`를
  `LocalResultEventBus`로 보내고 `onBack()`을 **두 번**(확인 화면 → 카메라) 부르고, 아니면 종전대로
  `goToAndPopCurrent(NavKeySegmentation)`으로 전진한다.
- 실패·취소 경로는 그대로 `sendResult(uri: String?)`다. 즉 이 화면이 돌려주는 값의 타입이 둘로
  갈렸고, `PictureConfirmResult`만 구독하는 호출자는 실패를 받지 못한다
  → [open-questions](../../../synthesis/open-questions.md) [2026-08-15].
- 좌측 버튼 문구 분기(`PictureConfirmSource`)·닫기 빈 람다 TODO는 변하지 않았다.

### 권한 화면 as-built 갱신 (2026-08-25, PR #350)
이슈 #345는 갤러리 권한 화면의 닫기 버튼이 설계보다 상태바 높이만큼 내려앉은 것이었고, 같은 라운드가
카메라 권한 화면의 같은 부류 결함을 함께 고쳤다. **두 화면의 결함은 기전이 서로 다르다** — 갤러리는
Scaffold가 이미 주는 인셋을 컴포넌트가 한 번 더 물어 이중 적용이었고, 카메라는 Scaffold가 인셋을
주지 않으므로 컴포넌트가 무는 것 자체는 맞되 **무는 자리**가 틀렸다.
- **인셋을 무는 자리가 닫기 `Row` → 바깥 `Box`로 옮겨졌다.** 닫기 줄에 물리면 하단 인셋이 버튼
  **아래**의 빈칸으로 들어가, 세로 가운데 정렬인 안내 블록이 내비게이션 바 높이의 절반만큼 위로 떴다.
  카메라 entry가 `innerPadding`을 화면에 먹이지 않는 예외(아래 「규약 대조」)의 하위 귀결이라,
  이 컴포넌트가 인셋을 직접 무는 구조 자체는 유지된다.
- **최외곽이 `Column` → `Box`가 되고 닫기 줄이 겹쳐 놓인다.** 줄을 세로로 쌓으면 안내 블록이 줄
  아래 남은 공간의 가운데로 앉아 화면 기준으로는 줄 높이의 절반만큼 내려가 보인다. 안내 블록에는
  하단 `padding3` 보정이 함께 붙었다. 갤러리 쪽도 같은 형태로 맞춰졌다 →
  [c102 스펙](2026-08-04-c102-custom-gallery-picker.md).
- **가이드 토스트가 권한을 기다린다.** `CustomCameraRoute`의 게이트가 `LaunchedEffect(Unit)` →
  `LaunchedEffect(state.hasPermission)`이 되고 조건에 `state.hasPermission`이 들어갔다. 촬영 요령을
  말하는 토스트가 **권한 거부 화면 위에** 뜨던 것을 막고, 설정에서 권한을 켜고 돌아온 시점에 처음
  뜬다(`hasShownGuideToast`가 `rememberSaveable`이라 1회는 그대로다). 갤러리 Route는 진작
  `state.access.hasPermission`을 게이트에 걸고 있었으므로 **카메라가 갤러리 쪽에 맞춰진 것**이고,
  두 모듈에 각각 정의된 같은 문구의 토스트가 이제 같은 조건으로 뜬다.
- **PR 요약이 이 토스트 변경을 다루지 않는다.** 인셋 수정만 적혀 있어 동작 변경 1건이 리뷰 서술
  밖에 있었다. 또 인셋 수정은 **실기기 확인 없이** 머지됐다(PR 본문이 스스로 밝힌다) →
  [open-questions](../../../synthesis/open-questions.md) [2026-08-25].
- `permanentlyDenied`·`onClickGrantPermission`은 **이번에도 본문에서 쓰이지 않는다** — 컴포넌트를
  통째로 다시 짜면서도 권한 요청 경로 부재는 그대로다(아래 「주의 / 열린 질문」).

### 상태(MVI)
`CustomCameraState`: `isInit` · `hasPermission` · `permanentlyDenied` · `lensFacing` · `zoomRatio` ·
`zoomRange` · `flashMode`. 권한은 `LifecycleResumeEffect`로 재개 시마다 재확인하고, 요청 결과의
`shouldShowRationale`이 false면 `permanentlyDenied`로 승격한다.
> 📌 **재확인 결과가 요청을 발행한다(2026-09-11, PR #489 develop 머지)**: 첫 권한 없음 확인에서 `RequestPermission`을
> 한 번 발행한다. 요청 여부는 상태가 아니라 VM 필드 `hasRequestedPermission`이 든다 → 아래 「권한 요청 as-built 갱신」.

## 정책 대조 (위키 [[카메라-뷰파인더]])

| 정책 항목 | 코드 | 판정 |
|---|---|---|
| 좌우 여백 20 고정 | `padding.padding7` | 일치 |
| 블러 프레임 안 선명 / 바깥 흐림 | `CameraFeedLayer` 2회 그리기 | 일치 |
| 블러 스펙 `Transparency/Black-25`·값 4 | `Black25` 스크림 + `BlurEffect` 반경 상수 | 일치 |
| 뷰파인더 비율 고정 없음 | `weight(1f)`로 잔여 공간 차지 | 일치 |
| 코너 마커로 선명 경계 안내 | 코너 8선분 + 안쪽 프레임 패딩 | 일치(정책에 없는 **전체 테두리 1선**이 추가로 그려진다) |
| 상단바 하단 ↔ 뷰파인더 **8** | 상단 Row 아래 `Spacer(10.dp)` 리터럴(주석 "10.dp가 없어서 넣었습니다") | **어긋남** — 값도 정책과 다르고 토큰(`padding4`=10, `gap3`=8)이 이미 있는데 리터럴을 썼다 |
| 뷰파인더 하단 ↔ 버튼 영역 **10** | `Spacer(gap3)` | **어긋남** — 상·하 간격이 정책과 뒤바뀐 모양새다 |

→ 두 간격 건은 [open-questions](../../../synthesis/open-questions.md) [2026-08-01]에 등록.

## 규약 대조 (parfait)
- **화면 컨테이너**: 카메라 entry는 `YGScaffold { CustomCameraRoute(...) }`로 **`innerPadding`을 화면에
  적용하지 않는다**(피드가 시스템 바 아래까지 덮어야 하므로, 인셋은 컨트롤 `Column`이
  `windowInsetsPadding`으로 직접 처리). 의도적 예외이고 코드 주석에 근거가 있다.
  > 📌 **스캐폴드가 Route로 내려갔다(2026-08-20, PR #309)** — `YGScaffoldV2`를 Route가 소유하고
  > **인셋을 무시하는 예외는 그대로 유지**한다. 같은 라운드가 `CustomCameraScreen`이 뷰파인더 Box
  > 안에 얹고 있던 `YGToastHost`를 걷어 스캐폴드로 옮겼다 — **토스트가 상태바 인셋 아래 상단으로
  > 올라가 눈에 보이는 위치가 바뀌었고**, 위키 Toast 공통 정책("위→아래 노출")에는 이쪽이 맞는다.
  > 조용히 뒤로 가던 촬영 실패에도 `showError` 토스트가 붙었다.
  > 🔁 **토스트 자리는 되돌아왔다(2026-08-26, PR #371)** — 아래
  > [as-built 재정정](#as-built-재정정-2026-08-26-pr-371-develop-머지) 참고. 위 문장의
  > "위키 Toast 공통 정책에는 이쪽이 맞는다"는 **이 화면에는 안 맞았다.** 스캐폴드 호스트가
  > 상태바 아래에 떠서 날짜·닫기 행을 덮었고, 위키 [[toast]]는 노출 방향만 정하지 어느 프레임의
  > 위인지는 정하지 않는다. 인셋 예외 서술 자체는 그대로 유효하다.
  > 📌 **권한 갈래도 이 예외를 따른다(2026-08-25, PR #350)** — 권한 거부 화면은 컨트롤 `Column`이
  > 없는 갈래라 인셋을 무는 주체가 `CameraPermissionRequestComponent` 자신이고, 무는 자리를 바깥
  > `Box`로 올려 각 변을 한 번씩만 물게 했다. 즉 **이 화면에서 "화면이 직접 무는" 형태는 갈래마다
  > 주체가 다르다**(피드 갈래는 컨트롤 `Column`, 권한 갈래는 컴포넌트 최외곽).
  화면 최외곽 `YGScreen`은 카메라·확인 화면 모두 쓰지 않는다(G-001과 같은 이탈 → [navigation-flow](../../../architecture/navigation-flow.md)).
- **문자열**: 카메라·갤러리 모두 feature `strings.xml` 신설로 [module-structure](../../../architecture/module-structure.md) 규약을 따랐다.
  단 갤러리 **빈 상태 문구만 코틀린 리터럴**로 남았다(#191로 해소).
- **디자인시스템 재사용**: 셔터·원형 버튼·토스트·`YGDate`·`YGButton`을 그대로 쓴다. feature 로컬 임시
  버튼 구현은 이 PR로 사라졌다([2026-07-30 셔터 2구현 항목](../../../synthesis/open-questions.md) 해소).

## 파일 구성
| 파일 | 역할 |
|---|---|
| `feature/camera/api/.../NavKeyPictureConfirm.kt` | 확인 화면 목적지(`uri` 인자 — #191에서 `source` 추가) |
| `feature/camera/api/.../PictureConfirmSource.kt` | 확인 화면 진입 출처 enum(#191 신설) |
| `feature/camera/impl/.../component/CameraFeedLayer.kt` | 피드 2회 그리기 블러 + 뷰파인더 데코 + 탭 포커스 |
| `feature/camera/impl/.../component/CameraPreviewComponent.kt`·`CameraPreviewHandle.kt` | CameraX 바인딩·핸들 |
| `feature/camera/impl/.../component/CameraControlComponent.kt` | 플래시·셔터·전환 컨트롤 행 |
| `feature/camera/impl/.../component/CameraPermissionRequestComponent.kt` | 권한 거부 화면 |
| `feature/camera/impl/.../util/CameraCrop.kt` | `computeCropRect`·`rotate`·`crop`·`saveViewfinderCapture` |
| `feature/camera/impl/.../screen/CustomCameraScreen.kt` | 레이아웃(날짜·닫기·뷰파인더 자리·컨트롤·토스트 호스트) |
| `feature/camera/impl/.../screen/PictureConfirmScreen.kt` | 촬영 결과 확인(Coil `rememberAsyncImagePainter` + 다시 찍기/다음) |
| `feature/camera/impl/.../route/CustomCameraRoute.kt`·`PictureConfirmRoute.kt` | 효과 소비·촬영 콜백·네비게이션 |
| `feature/camera/impl/.../viewmodel/CustomCameraViewModel.kt` | MVI 상태·인텐트·효과 + `FlashMode` |
| `feature/camera/impl/src/main/res/values/strings.xml`·`feature/gallery/.../strings.xml` | 화면 문구 |
| `core/util/jvm/.../model/DateTextFormat.kt` | 요일·월일 축약 포맷(`kotlinx-datetime`) |
| `core/designsystem/res/drawable/ic_lightning_fill.xml`·`image_gallery_empty` | 플래시 on 아이콘·갤러리 빈 그래픽(#191에서 벡터 → 밀도별 PNG 교체) |

## 동반된 디자인시스템 변경 (범위 밖 회귀)
이 PR은 카메라 화면 작업이면서 `core:designsystem`을 함께 건드렸고, 둘 다 **직전 sync 라운드 결과를
되돌리는 방향**이다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-01].
- `YGButtonType`에서 **`radius` 속성 삭제**, `YGButton`의 `background`·`border` `shape` 인자와 `clip` 제거.
  현재 전 변형이 `radius.none`이라 렌더 결과는 같지만, [버튼 sync 스펙](2026-07-30-designsystem-button-component-sync.md)·
  [radius-none-sync 스펙](2026-07-19-designsystem-radius-none-sync.md)이 세운 "각짐도 테마 토큰 경유" 원칙이 코드에서 사라졌다.
- `YGDate`에 `background`가 **한 번 더** 추가돼 `border` **뒤**에 온다 — modifier 체인 그리기 순서상
  테두리를 덮는다([ygtext-date-label 스펙](2026-07-18-ygtext-date-label.md)의 as-built와 어긋남).

## 주의 / 열린 질문
- **다음 경로**: "다음"은 #221에서 C-103(`NavKeySegmentation`)으로 결선됐고 → [c103 스펙](2026-08-15-c103-segmentation-topping-edit.md).
  ~~**닫기는 여전히 빈 람다 + TODO**(C-001 캔버스)이고, 세그멘테이션 3화면의 닫기도 같은 상태라 토핑 생성 경로에는 아직 출구가 없다.~~
  → ✅ **#309(2026-08-20)로 넷 다 결선됐다** — `Navigator.popUpTo<T>()`로 캔버스까지 되감고, 배경 편집에서
  들어온 경로(`returnResultOnly = true`)만 부른 화면으로 돌아간다(확인 버튼도 같은 처리다) → OQ-P-152. **#191로 갤러리 경로가 같은 화면에 합류해 영향 범위가
  두 진입점으로 늘었다**(갤러리는 결과 반환도 끊겨 이 화면이 유일한 출구다).
- **줌 死코드**: `CameraZoomIndicatorComponent`·`controls/ZoomLevelRow`가 참조 0건이고,
  `CameraControlComponent`는 `zoomRatio`·`zoomRange`·`onClickZoomLevel`을 받기만 하고 쓰지 않는다.
  VM의 `OnZoomRangeReady`·`OnClickZoomLevel`도 발신처가 없다.
- **갤러리 빈 상태**: ~~빈 그래픽 `Image`가 `when` 분기 **밖**에 있어 사진이 있어도 항상 그려지고~~
  ~~문구는 리터럴~~(둘 다 #191로 해소, 2026-08-04). 로딩 인디케이터가 흰 배경에 흰색인 것은 잔존.
- **권한 요청 경로 부재**: 권한 컴포넌트가 `permanentlyDenied`를 받지만 본문에서 분기하지 않고(거부
  화면 1종), `onClickGrantPermission`을 부르는 UI도 없다. `CustomCameraIntent.OnRequestPermission`과
  `permissionLauncher`는 살아 있으나 **발신처가 없어** 시스템 권한 다이얼로그가 뜨지 않는다 —
  미허용 상태에서는 "설정으로 이동"만 보인다. 갤러리 쪽도 같은 형태다.
  **#350(2026-08-25)이 두 컴포넌트의 레이아웃을 다시 짜면서도 이 둘은 건드리지 않았다** — 파라미터
  둘은 여전히 받기만 하고 쓰이지 않는다.
  > 📌 **진입 시 자동 요청이 들어갔다(2026-09-11, PR #489 develop 머지)**: VM이 첫 권한 없음 확인에서
  > `RequestPermission`을 발행하므로 시스템 다이얼로그가 뜬다. 두 파라미터 미사용만 남는다(OQ-P-053 ③)
  > → 아래 「권한 요청 as-built 갱신」.
- 위 항목은 전부 [open-questions](../../../synthesis/open-questions.md) [2026-08-01]에서 추적.

## as-built 재정정 (2026-08-26, PR #371 develop 머지)

> **토스트가 다시 뷰파인더 프레임 안으로 들어갔다.** 촬영 흐름·권한 갈래·인셋 예외는 한 줄도
> 안 바뀌었고, 바뀐 것은 **안내 토스트가 어느 상자를 기준으로 뜨는가**다.
> 브랜치 `feature/toast-position-fix`, 머지 `bf06c830`, 커밋 둘(브랜치 안 develop 병합 1회 포함).

- **증상**: 진입 안내 토스트가 상태바 인셋 바로 아래에 떠서 이 화면이 그 자리에 그리는
  **날짜·닫기 버튼 행을 덮었다.** #309가 호스트를 Screen에서 스캐폴드로 옮긴 결과였다.
- **처방**: `CustomCameraScreen`이 `toastPolicy: YGToastPolicy`를 **필수 파라미터로 받고**,
  뷰파인더 자리 `Box` 안에 `YGToastHost`를 `TopCenter`로 심는다. Route는 스캐폴드에 정책을
  넘기지 않고 Screen에 넘긴다. 그 `Box`는 `fillMaxWidth().weight(1f)`라 자식이 늘어도 크기가
  안 변하므로 `onViewfinderRectChange`가 보고하는 좌표도 그대로다.
- ⚠️ **브랜치 안에서 한 번 갈렸다.** 첫 커밋은 `YGScaffoldV2`에 `toastTopPadding`을 더하고 Route가
  `padding6 + gap5 + 닫기 버튼 지름 + 뷰파인더 앞 간격`을 **직접 더해** 넘겼다 — 화면 레이아웃이
  이미 쓰는 상수를 Route에 복제하는 형태다. 두 번째 커밋이 그것을 되돌렸고, 코드 주석이 근거를
  남긴다: **위치 계산 없이** 프레임 윗변에 뜬다. `YGScaffoldV2`는 결국 안 바뀌었다.
- ⚠️ **호스트가 둘이다** — Route가 스캐폴드에 정책을 안 넘기므로 스캐폴드는 자기 기본 정책으로
  호스트를 하나 더 만들고, **그쪽에는 아무도 발행할 수 없다** → OQ-P-312 ②.
- ⚠️ **호스트가 권한 허용 갈래 안에만 있다** — `CameraContent`가 그리는 자리라 권한 거부 화면에는
  호스트가 없다. 지금은 안내 토스트도 촬영 실패 토스트도 **권한이 있어야** 발행되므로 증상이
  없지만, **그 결합은 어디에도 적혀 있지 않다** → OQ-P-312 ④.

### 유닛 테스트

**0건이다.** 이 라운드가 바꾼 것은 배치라 유닛으로 덮을 수 없고, 계측도 안 붙었다
(저장소 전체 유닛 789·계측 14건 그대로). 첫 커밋의 dp 계산 방식을 버린 이유 자체가
**검증할 수 없는 중복을 만들지 않으려는 것**이었다. 확인 수단은 실기기 눈으로 보는 것뿐이다.

## 권한 요청 as-built 갱신 (2026-09-11, PR #489 develop 머지)

> 브랜치 `bugfix/permission-not-required`, develop `544ce434a` 위 커밋 둘(`1d25a4bb9`·`98286f73e`).
> 2026-09-11 PR #489로 develop에 머지됐다(머지 `be537ea93`, 머지 트리 = 브랜치 팁 `98286f73e`).
> 이 절은 머지 전에 썼고, 머지 코드와 다시 대조해 어긋난 자리가 없었다.
> [open-questions](../../../synthesis/open-questions.md) OQ-P-053 ①②의 결정을 구현했다.

**진입했을 때 권한이 없으면 시스템 권한 다이얼로그부터 띄운다.** 이전에는 권한을 한 번도 묻지 않은 설치
직후에도 「설정으로 이동」 화면이 곧바로 떴다.

- `CustomCameraViewModel#handleOnPermissionResult`가 권한 없음을 받으면 `requestPermissionOnce()`를 부른다.
  이 함수는 `private var hasRequestedPermission`이 거짓일 때만 `RequestPermission` 효과를 한 번 발행한다.
  요청 경로를 위해 Route를 바꾸지는 않았고(`1d25a4bb9`), 이미 있던 효과 수집이 `permissionLauncher`를 띄운다.
- 요청을 한 번으로 막는 이유는 다이얼로그가 닫히면 `LifecycleResumeEffect`의 재확인이 다시 들어오기 때문이다.
  막지 않으면 거부할 때마다 다이얼로그가 다시 뜬다. 요청 결과 인텐트 `OnPermissionRequestResult`는 요청을 보내지 않는다.
- 플래그는 상태가 아니라 ViewModel 필드다. 화면에 새로 들어오면 ViewModel도 새로 생기므로, 한 번만 거부한
  사용자에게는 다음 진입에서 다시 묻는다. `SavedStateHandle`에 두지 않았으므로 프로세스가 죽었다 복원될 때도 다시 묻는다.
- 물을 수 없는 상태인지 미리 가르지 않는다. 요청 전의 `shouldShowRationale`은 한 번도 묻지 않은 상태와 영구 거부
  상태에서 모두 거짓이라 둘을 구분할 수 없다. 그래서 요청을 띄우고 시스템 응답에 맡긴다. 물을 수 없는 상태라면
  시스템이 다이얼로그 없이 거부로 답하고, 설정 이동 화면이 그대로 남는다.
- **다이얼로그가 떠 있는 동안 뒤에는 설정 이동 화면이 보인다.** 작업자 결정이다. 뒤를 비우려면 재개 확인 결과를
  요청이 끝날 때까지 보류하는 로직이 더 필요해서 택하지 않았다. 그 대가로 다이얼로그가 허용 여부를 묻는 동안
  딤 뒤에는 "설정에서 카메라 권한을 허용해 주세요"가 보인다.
- 최초 거부와 영구 거부를 다른 화면으로 나누지 않았다(OQ-P-053 ②). `permanentlyDenied`·`onClickGrantPermission`은
  여전히 받기만 하고 쓰이지 않는다(OQ-P-053 ③ 잔존).
  > ✅ **그 둘이 시그니처에서 빠졌다(2026-09-20, PR #514)** — `CameraPermissionRequestComponent`의
  > `permanentlyDenied`·`onClickGrantPermission` 파라미터와 `CustomCameraIntent.OnRequestPermission`
  > 인텐트·핸들러가 삭제됐다(OQ-P-053 ③ 해소). 영구 거부 갈래 프리뷰도 함께 사라져 이 컴포넌트
  > 프리뷰는 하나다. **`CustomCameraState.permanentlyDenied` 필드는 남는다** — VM이 계속 계산해
  > 들고 있고 화면만 읽지 않는다. 같은 라운드가 줌 쪽 파라미터(`zoomRatio`·`zoomRange`·
  > `onClickZoomLevel`)와 `OnClickZoomLevel` 인텐트도 함께 걷었다(OQ-P-052).

### CameraX 바인딩이 권한을 기다린다 (`98286f73e`)

위 변경을 실기기에서 확인하다 드러난 결함이다. **다이얼로그에서 허용해도 프리뷰가 뜨지 않았고, 앱을 백그라운드에
내렸다 올려야 보였다.**

- **원인**: `CameraPreviewViewComponent`가 권한과 무관하게 첫 컴포지션에서 `bindToLifecycle`을 호출했고, 다시
  바인딩하는 계기는 `lensFacing` 변경뿐이었다. 권한 없이 바인딩하면 CameraX가 카메라 열기를 두 번 시도하다 멈춘다.
  logcat에 `W/CXCP` `SecurityException: ... cannot open camera "0" without camera permission`이 약 0.5초 간격으로
  두 번 남았다.
- 권한 다이얼로그는 Activity를 pause만 시키고 stop시키지 않는다. 바인딩된 lifecycle이 STARTED 아래로 내려가지
  않으므로 CameraX가 카메라를 다시 열 계기가 없다. 백그라운드를 다녀오면 stop과 start를 거치므로 다시 열린다.
- 이전에는 권한을 얻는 길이 설정 앱을 다녀오는 것뿐이었고, 그 길은 늘 stop과 start를 거쳤다. 그래서 이 결함이
  드러나지 않았다.
- **처방**: `CameraPreviewViewComponent`가 `hasPermission`을 받아 `DisposableEffect(lensFacing, hasPermission)`의
  키로 쓴다. 권한이 없으면 바인딩하지 않고 빈 `onDispose`로 빠진다. `CustomCameraRoute`가 `state.hasPermission`을 넘긴다.

### 검증

- `:feature:camera:impl`에 `parfait.test.unit` 플러그인이 붙어 이 모듈에 처음으로 유닛 테스트가 생겼다.
  `CustomCameraViewModelTest` 3건이다. 권한 없음이면 요청을 1회 발행하는지, 요청 뒤 권한 없음이 거듭 들어와도
  재요청하지 않는지, 권한 있음이면 요청하지 않는지를 본다. 앞의 두 건은 구현 전에 실패(요청 효과 미발행)하는 것을
  확인했다. 모듈 `ktlintCheck`도 통과했다.
- 바인딩 수정은 실제 CameraX와 lifecycle이 있어야 재현되므로 자동 테스트가 없다. 작업자가 실기기에서 다이얼로그
  허용 직후 프리뷰가 뜨는 것을 확인했다. 영구 거부 상태에서 다이얼로그 없이 설정 화면이 남는 경로는 실기기로
  확인하지 않았다.
