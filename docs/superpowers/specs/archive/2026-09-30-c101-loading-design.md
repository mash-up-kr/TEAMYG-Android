---
id: c101-loading
title: 사진 분석 로딩 페이지 (C-101-Loading)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-30
related_code:
  - feature/segmentation/impl/.../route/SegmentationRoute.kt#SegmentationRoute
  - feature/segmentation/impl/.../viewmodel/SegmentationViewModel.kt#SegmentationViewModel
  - feature/segmentation/impl/.../viewmodel/ToppingEditViewModel.kt#ToppingEditViewModel
  - feature/segmentation/impl/.../route/SegmentationConfirmRoute.kt#SegmentationConfirmRoute
  - feature/segmentation/api/.../NavKeyToppingEdit.kt#NavKeyToppingEdit
  - feature/camera/impl/.../route/PictureConfirmRoute.kt#PictureConfirmRoute
  - data/.../utils/image/SegmentationCandidateHarvest.kt#harvestSubjects
related_adr:
related_spec:
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, segmentation, topping]
---

# Spec: 사진 분석 로딩 페이지 (C-101-Loading)

> 이슈 #562 (상위 #517 토핑 추가 플로우 개선). Figma `C-101-Loading` 노드 `5461-6750`,
> 그만두기 팝업 노드 `5630-10693`.

## 목표

사진 확인 화면(`PictureConfirm`)에서 "다음"을 누른 뒤 딤 오버레이 위에서 돌던 세그멘테이션
분석을 전용 로딩 페이지로 옮긴다. 분석 결과는 두 갈래뿐이다 — 대상이 1개 이상이면 기존 대상
선택(C-103), 0개면 영역 지우기/채우기 편집 화면. **실패 화면과 재시도는 없어진다.**

## 범위

- 포함
  - C-101-Loading 페이지(`SegmentationRoute` 안의 분석 중 상태)
  - 분석 결과 분기: 1개 이상 → 대상 선택, 0개·예외·디코드 실패 → 편집 화면
  - 편집 화면의 새 완료 방식: 초안을 직접 기록하고 확인 화면으로 간다
  - "사진 편집을 그만둘까요?" 팝업 — C-101-Loading, C-103 대상 선택, `SegmentationConfirm`,
    `PictureConfirm`(토핑 경로)의 X. 로딩 중에는 시스템 뒤로도 같은 팝업
  - `PictureConfirm`을 백스택에 남기도록 이동 방식 변경
  - 삭제: `SegmentationErrorScreen`(C-103-Error), 재시도, 회복 사다리 전체 — 회복만 쓰던 projection
    경로(`DetectionProjection`·`RecoveryTransform`·`PlateSource.OriginRegion` 등)와 대비 보정
    (`SegmentationContrast*`)까지. 1차 경로는 늘 `projection = null`이라 남기면 죽은 코드다
- 제외
  - 배경 편집 경로(`PictureConfirm`의 `returnResultOnly = true`) — X 동작은 지금 그대로. 별도 이슈로 다룬다
  - 편집 화면(`ToppingEditScreen`)의 뒤로·닫기 — 팝업 없이 지금처럼 `onBack`
  - 분석 이벤트 화면명 분리 — 로딩 중에도 `NavKeySegmentation`이라 `C-103`으로 찍힌다
  - 위키 정책 문서 갱신 — 정책 원본이 아직 위키에 들어오지 않았다

## 흐름과 백스택

`PictureConfirm`의 "다음"은 `goToAndPopCurrent`가 아니라 **`goToSingleClearTop(NavKeySegmentation(uri))`** 다
(push. 연타로 같은 키가 두 번 쌓이지 않도록 `goTo` 대신 쓴다).
사진 확인 화면이 로딩 아래에 남는다. 로딩 페이지는 어떤 경로로도 되돌아오지 않는 경유지다.

| 분석 결과 | 이동 | 결과 백스택 (아래 → 위) |
|---|---|---|
| 1개 이상 | 같은 `NavKeySegmentation`이 선택 UI로 바뀐다 | 카메라/갤러리 · `PictureConfirm` · `Segmentation` |
| 0개·예외·디코드 실패 | `goToAndPopCurrent(NavKeyToppingEdit(..., completion = RecordAndConfirm))` | 카메라/갤러리 · `PictureConfirm` · `ToppingEdit` |

편집 완료(`RecordAndConfirm`)는 `goTo(NavKeySegmentationConfirm)` 라 편집 화면이 확인 화면 아래에 남는다.
단 `navigator.backStack.lastOrNull() == key`(편집 화면이 아직 맨 위)일 때만 이동한다 — 화면이 걷히기 전에
들어온 완료 탭이 확인 화면을 한 번 더 쌓지 않게 한다.

| 화면 | `<` · 시스템 뒤로 | X |
|---|---|---|
| C-101-Loading | 팝업 (X와 같음) | 팝업 |
| C-103 대상 선택 | `PictureConfirm` | 팝업 |
| 편집 (0개 경로) | `PictureConfirm` | 뒤로와 같음 (지금 그대로) |
| `SegmentationConfirm` | 대상 선택 또는 편집 | 팝업 |
| `PictureConfirm` (토핑 경로) | 카메라/갤러리 | 팝업 |

팝업의 "그만두기"는 `navigator.popUpTo<NavKeyCanvasMain>()`, "계속 편집"은 팝업만 닫는다.

## 동작 / 상태

### `SegmentationViewModel`

```kotlin
data class SegmentationState(
    /** 참이면 화면 전체가 C-101-Loading 이다 */
    val isAnalyzing: Boolean = true,
    /** 후보를 고른 뒤 저장하는 동안의 오버레이 (기존 YGScaffoldV2 isLoading) */
    val isSaving: Boolean = false,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
    val showQuitDialog: Boolean = false,
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent
    data object ClickClose : SegmentationIntent        // X, 로딩 중 시스템 뒤로
    data object ConfirmQuit : SegmentationIntent       // 그만두기
    data object DismissQuit : SegmentationIntent       // 계속 편집, 바깥 탭
}

sealed interface SegmentationEffect : UiSideEffect {
    data object ShowError : SegmentationEffect          // 후보 저장 실패 토스트 (기존)
    data object QuitToCanvas : SegmentationEffect
    data object GoToEdit : SegmentationEffect           // Route 가 key.sourceImageUri 로 편집을 연다
    data class GoToConfirm(val subjectImagePath: String, val trimmedSubjectImagePath: String) : SegmentationEffect
}
```

분석 순서(기존 `loadCandidates`에서 회복·에러 분기만 걷는다):

1. 세그멘테이션 캐시 비우기 (실패 무시)
2. 원본 디코드. 실패 → 결과 = **편집으로**
3. 최근 원본 기록 (실패 무시)
4. `SegmentImageUseCase`. 1개 이상 → 결과 = **선택**. 0개·`Result.failure` → 결과 = **편집으로**
5. 분석 `launch`의 `onError`(유스케이스가 `Result`로 감싸지 않고 던진 예외) → 결과 = **편집으로**.
   없으면 로딩에 갇힌다

**편집으로 가는 결과는 언제나 `GoToEdit(sourceImageUri)`다.** 원본을 따로 저장하지 않는다 —
편집 화면이 같은 `DecodeImageUseCase`로 원본 URI를 직접 읽으므로 결과가 같고, 저장 실패 분기와
캐시 파일이 없어진다. 편집으로 갈 때는 `isAnalyzing`을 참으로 둔 채 effect만 낸다 — 먼저 끄면
교체 직전 한 프레임 동안 후보 0개인 선택 UI가 보인다.

디코드 실패로 편집에 가면 편집 화면도 같은 URI를 못 읽어 `LoadFailed` 토스트 후 `onBack`으로
`PictureConfirm`에 돌아온다. 이것이 의도한 동작이다.

**팝업이 떠 있는 동안 나온 결과는 보류한다.** 결과를 `pendingOutcome`(비상태 필드)에 두고
`isAnalyzing`을 참으로 유지한다. `DismissQuit`에서 보류된 결과를 적용하고, `ConfirmQuit`은
결과를 버리고 `QuitToCanvas`만 낸다. 고르는 중에 화면이 바뀌면 사용자의 선택이 무엇에 대한
것인지 모호해진다.

`ClickCandidate` → `persistSubjectUseCase` → 초안 기록 → `GoToConfirm` 순서와 실패 토스트는
그대로다. 오버레이 플래그만 `isLoading`에서 `isSaving`으로 이름이 바뀐다.

### `NavKeyToppingEdit` 완료 방식

```kotlin
@Serializable
enum class ToppingEditCompletion {
    /** 결과를 TOPPING_EDIT_RESULT_KEY 로 돌려주고 닫는다. 확인 화면 "사진 편집", 배경 편집 */
    ReturnResult,
    /** 초안을 직접 기록하고 확인 화면으로 간다. 편집 화면은 백스택에 남는다. 0개 경로 */
    RecordAndConfirm,
}

data class NavKeyToppingEdit(
    ...,
    val completion: ToppingEditCompletion = ToppingEditCompletion.ReturnResult,
) : NavKey
```

`ToppingEditViewModel`이 완료 방식을 `@Assisted` 인자로 받아 `completeEdit` 한 코루틴 안에서 가른다.
파일 저장 뒤 `ReturnResult`면 `EditCompleted(result)`를 내고 Route가 `sendResult` + `onBack`을 한다.
`RecordAndConfirm`이면 VM이 주입받은 `RecordToppingDraftUseCase`로 `recordEditResult(result)` → 성공이면
`ToppingEditEffect.GoToConfirm`(경로 이름 뒤집힘 주의: `subjectImagePath = result.cutoutImagePath`,
`trimmedSubjectImagePath = result.subjectImagePath`), 실패·예외면 `SaveFailed` 토스트 후 머문다.
`isSaving`은 저장부터 기록까지 올라가 있어 그 사이의 "완료" 탭은 무시되고, 어느 출구에서든 이펙트 직전에 내린다
(편집 화면이 백스택에 남아 다시 완료할 수 있어야 한다).
⚠️ 알려진 한계: `isSaving`을 내린 뒤 화면이 걷히기 전의 "완료" 탭은 한 번 더 저장·기록한다. 두 번째 결과는
저장 경로가 달라 확인 키가 달라지므로 `goToSingleClearTop`으로는 못 막는다 — 위의 맨 위 검사가 두 번째 이동을
버린다. 두 번째 기록은 초안을 새 경로로 덮어쓰고, 확인 화면은 초안 흐름을 따라 둘째 결과로 바뀌어 초안과
어긋나지 않는다(첫 결과 파일만 캐시에 고아로 남는다).
JVM 테스트는 비트맵을 만드는 편집 헬퍼(`ToppingEditMask.kt`)를 `mockkStatic`으로 갈아 끼워 저장 이후의 순서를 덮는다.

### 팝업 상태

- `SegmentationRoute`: VM의 `showQuitDialog` — 결과 보류 규칙이 팝업 상태에 달려 있어서다
- `SegmentationConfirmRoute`, `PictureConfirmRoute`: Route의 `rememberSaveable` 불린. 보류할 비동기
  작업이 없다

## 표시·제어 규칙

- C-101-Loading: 흰 배경, 우상단 X(`YGCircleButton`), 가운데 `YGLoadingLottie(art = YGLoadingArt.Dark)`,
  제목 "사진을 편집하고 있어요", 부제 "잠시만 기다려 주세요...". 딤 오버레이는 쓰지 않는다.
  `Dark`인 이유는 에셋 색이다 — 반투명 검정 원 위 회색 점 셋이라 Figma의 회색 원 "···"과 같고,
  `Light`는 흰 점이라 흰 배경에서 안 보인다
- `BackHandler`는 `isAnalyzing`인 동안에만 켠다. 선택 UI에서의 시스템 뒤로는 `PictureConfirm`으로 간다
- 팝업: `YGModalPopup` + `ic_warning_round`. 제목 "사진 편집을 그만둘까요?", 본문
  "지금까지 진행한 내용은 저장되지 않아요.\n정말 그만두시겠어요?", 보조 "그만두기", 주 "계속 편집".
  바깥 탭은 "계속 편집"과 같다
- 문자열은 segmentation, camera 모듈에 각각 둔다. 공용 컴포넌트를 만들지 않는다 — 호출이 몇 줄이고
  공용화하면 모듈 의존만 는다

## 파일 구성

- 추가
  - `feature/segmentation/impl/.../screen/SegmentationLoadingScreen.kt` — C-101-Loading
  - `feature/segmentation/api/.../NavKeyToppingEdit.kt`에 `ToppingEditCompletion`
- 수정
  - `SegmentationViewModel`, `SegmentationRoute`, `SegmentationScreen` — 상태 분리, 분기, 팝업
  - `ToppingEditViewModel`, `ToppingEditRoute` — 완료 방식
  - `SegmentationConfirmRoute`, `PictureConfirmRoute` — 팝업, "다음" 이동 방식
  - segmentation·camera `strings.xml`
- 삭제
  - `SegmentationErrorScreen.kt`와 전용 문자열·리소스
  - `RecoverCandidatesUseCase`, 레포지토리의 `recoverCandidates`와 회복 전용 private 함수·상수
  - 회복 전용 data 타입·유틸: `RecoveryStage`, `RecoveryTransform`, `SegmentationRecoverySpec`,
    `SegmentationRecoveryNormalizer`, `DetectionPlate`, `ScaledSize`, `SegmentationContrast*`,
    `DetectionProjection`, `PlateSource.OriginRegion` 경로, harvest의 `projection`·`hintThreshold`
  - 1차 경로가 쓰는 `offsetBy`·`isInsideCanvas`는 `SegmentationRecoveryPlan.kt`에서
    `SegmentationGeometry.kt`로 옮기고 나머지는 지운다

## 테스트

- `SegmentationViewModelTest`
  - 1개 이상 → `isAnalyzing = false`, 후보 채워짐
  - 0개 / `Result.failure` / 던진 예외 / 디코드 실패 → `GoToEdit`, `isAnalyzing`은 참 유지
  - 팝업 중 결과 → 보류, `DismissQuit`에서 적용, `ConfirmQuit`에서 `QuitToCanvas`만
  - 재시도·회복·에러 상태 테스트 삭제
- `ToppingEditViewModelTest`
  - `ClickDone`(`RecordAndConfirm`) → 기록 성공 `GoToConfirm`(경로 이름 뒤집힘 확인), 실패·예외 `SaveFailed`,
    기록 중 재탭 무시. `ClickDone`(`ReturnResult`) → `EditCompleted`, 초안 기록 없음
- 회복 전용 테스트 삭제, `offsetBy`·`isInsideCanvas` 테스트는 `SegmentationGeometryTest`로 이동
- 실기기: 백스택 표 전 행, 팝업이 뜨는 화면 전부, 로딩 중 시스템 뒤로

## 주의 / 열린 질문

- 분석 화면명: 로딩 중에도 `C-103`으로 찍힌다(`NavKeyAnalyticsScreenTest`). 로딩을 따로 세야 하면
  NavKey 분리가 필요하다
- 위키의 C-103-Error·재시도 정책과 어긋난다. `docs/synthesis/open-questions.md` 항목으로 추적한다
- 갤러리 content URI를 편집 화면이 다시 읽을 수 있는지 실기기로 확인한다(원본을 저장하지 않으므로)
- `PictureConfirm`에 돌아올 때 `prepareSegmentationModule()`이 다시 돈다. 준비된 모듈이면 바로 끝난다
