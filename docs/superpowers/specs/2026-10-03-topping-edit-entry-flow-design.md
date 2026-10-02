---
id: topping-edit-entry-flow
title: 누끼 편집(C-104) 진입 흐름 단축과 화면 개편 (Topping edit entry flow & UI)
status: draft
category: behavior-spec
platforms: android
verified: 2026-10-03
related_code:
  - feature/segmentation/api/.../NavKeyToppingEdit.kt#NavKeyToppingEdit
  - feature/segmentation/api/.../NavKeySegmentationConfirm.kt#NavKeySegmentationConfirm
  - feature/segmentation/impl/.../screen/ToppingEditScreen.kt#ToppingEditScreen
  - feature/segmentation/impl/.../route/ToppingEditRoute.kt#ToppingEditRoute
  - feature/segmentation/impl/.../viewmodel/ToppingEditViewModel.kt#ToppingEditViewModel
  - feature/segmentation/impl/.../viewmodel/SegmentationViewModel.kt#selectCandidate
  - feature/segmentation/impl/.../route/SegmentationRoute.kt#SegmentationRoute
  - feature/segmentation/impl/.../route/SegmentationConfirmRoute.kt#SegmentationConfirmRoute
  - feature/gallery/impl/.../viewmodel/CustomGalleryPickerViewModel.kt#handleOnClickCutoutImage
  - domain/.../usecase/topping/EnsureDraftSubjectRecordedUseCase.kt#EnsureDraftSubjectRecordedUseCase
  - domain/.../model/member/TutorialKind.kt#TutorialKind
  - app/.../analytics/NavKeyAnalyticsScreen.kt#toAnalyticsScreenOrNull
related_adr: ADR-0006, ADR-0026
related_spec: c101-loading, c106-topping-place-api
related_architecture: navigation-flow, design-system
supersedes:
superseded_by:
tags: [spec, parfait]
---

# Spec: 누끼 편집(C-104) 진입 흐름 단축과 화면 개편

## 목표

토핑 추가 흐름에서 누끼 결과를 고른 뒤 **언제나 C-104(누끼 편집)로 곧장 들어가고**, C-104의 「다음」이
**C-106(토핑 배치)** 으로 간다. 그 사이에 있던 확인 화면(`SegmentationConfirm`, 분석 화면명 `C-103-select`)은
없어진다. C-104 화면은 Figma `C-104`·`C-104-Toast` 시안으로 바뀐다.

- Figma `C-104` — `node-id=5461-6875`
- Figma `C-104-Toast` — `node-id=5477-8932`

## 범위

- 포함
  - 진입 세 갈래의 이동 경로 변경(아래 「흐름」)
  - C-104 화면 레이아웃 교체(헤더·편집 영역·「다음」 버튼)
  - 감지 실패로 들어온 C-104의 안내 토스트
  - C-104 안의 오류 메시지를 화면 내 토스트 호스트로 옮기기(불러오기 실패는 제외)
  - 확인 화면과 그 튜토리얼, 편집 결과 왕복(`ReturnResult`) 삭제
  - 분석 화면 매핑·`docs/status.md` 갱신
- 제외
  - 캔버스 그리기·마스크 합성·확대 제스처·붓 굵기 범위 — 지금 동작 그대로
  - C-106 화면 자체 — 진입점만 바뀐다
  - 위키(`wiki/`) 갱신 — 기획 문서 쪽 작업이다

## 흐름

| 진입 | 지금 | 바꾼 뒤 |
|---|---|---|
| 후보 선택(C-103) | 저장 + 초안 기록 → 확인 → 「사진 편집」 → C-104 → 결과를 돌려받아 확인 → C-106 | 저장 → C-104 → C-106 |
| 감지 실패(후보 0건·예외·디코드 실패) | C-104 → 확인 → C-106 | C-104(토스트) → C-106 |
| 갤러리 최근 줄의 누끼 재사용 | 확인(편집 버튼 숨김) → C-106 | 초안 맞춤 → C-106 |

### 후보 선택 → C-104

- `SegmentationViewModel.selectCandidate`는 `PersistSubjectUseCase`로 후보를 파일로 떨군 뒤 **초안을 기록하지
  않고** C-104로 간다. 초안 기록은 C-104 「다음」 한 곳으로 모은다.
- 이동은 `goTo`다. C-104에서 뒤로 가면 후보 선택 UI로 돌아와 다른 후보를 고를 수 있다.
- C-104의 시작 마스크는 원본 크기를 유지한 저장본(`PersistSubjectUseCase` 결과의 `subjectImagePath`)이다. 편집
  화면이 `ContentResolver`로 읽으므로 `file` 스킴 uri로 바꿔 싣는다 — 지금 `SegmentationConfirmRoute`의
  「사진 편집」이 하던 변환과 같다.
- 저장 실패는 지금처럼 `SegmentationEffect.ShowError`다.

### 감지 실패 → C-104

- 지금처럼 `goToAndPopCurrent`로 분석 화면을 치환하고, 원본 uri를 원본·마스크 자리 둘 다에 싣는다.
- 키에 `isDetectionFailed = true`를 실어 C-104가 안내 토스트를 띄운다(「토스트」 절).

### C-104 「다음」 → C-106

- 언제나 편집 결과를 초안에 기록한 뒤(`RecordToppingDraftUseCase`) `goTo(NavKeyCanvasToppingPlace)`로 간다.
  C-104는 백스택에 남는다. C-106 헤더의 뒤로는 편집 상태가 그대로인 C-104로 돌아온다.
- 저장 중 연타와 이동 직전 탭으로 C-106이 두 번 쌓이지 않도록, 지금 `GoToConfirm` 처리가 하던 "이 화면이 맨
  위일 때만 이동" 가드를 그대로 옮긴다.
- 기록 실패는 저장 실패와 같은 토스트다.

### C-104 닫기·뒤로

- 헤더 X → `YGModalQuitEdit` → 확정하면 `popUpTo<NavKeyCanvasMain>()`. 취소하면 팝업만 닫는다.
- 헤더 뒤로와 시스템 뒤로는 팝업 없이 `navigator.onBack()`.
  - 후보 선택에서 왔으면 선택 UI로, 감지 실패로 왔으면 사진 확인(`PictureConfirm`)으로 간다.

### 갤러리 최근 누끼 → C-106

- `CustomGalleryPickerViewModel.handleOnClickCutoutImage`가 `EnsureDraftSubjectRecordedUseCase(filePath)`를
  부른다. `true`면 C-106으로 가는 이펙트를 보낸다. `false`나 예외면 화면에 오류 토스트를 띄우고 머문다.
  - 지금 이 맞춤은 확인 화면 VM이 진입 때 한다. 확인 화면이 없어지므로 고르는 시점으로 옮긴다.
  - 문구는 확인 화면의 `segmentation_confirm_draft_unavailable`를 갤러리 모듈로 옮겨 쓴다.
- 연타로 두 번 맞추고 두 번 이동하지 않게 `launch` 키를 하나로 묶는다.
- `feature:gallery:impl`이 `feature:groups:canvas:api`를 의존한다(`NavKeyCanvasToppingPlace`).

## API / 인터페이스

```kotlin
@Serializable
data class NavKeyToppingEdit(
    val sourceImageUri: String,
    val segmentationImageUri: String,
    /** 자동 감지가 대상을 못 찾아 원본 그대로 들어왔다. 첫 진입에 안내 토스트를 띄운다 */
    val isDetectionFailed: Boolean = false,
) : NavKey
```

- 삭제: `ToppingEditCompletion`, `TOPPING_EDIT_RESULT_KEY`, `NavKeySegmentationConfirm`
- `ToppingEditResult`는 `:api`에서 `:impl` 내부로 옮긴다. 화면 밖으로 결과를 돌려주지 않으므로 공개할
  이유가 없다. `recordEditResult` 확장은 그대로 쓴다.
- `ToppingEditEffect`
  - 삭제: `EditCompleted`, `GoToConfirm`
  - 추가: `GoToPlace` — 초안 기록을 마쳤다
  - 추가: `ShowDetectionFailed` — 감지 실패 안내
- `ToppingEditIntent`에 X 팝업 상태용 의도를 추가한다(`ClickClose`·`ConfirmQuit`·`DismissQuit`). 팝업 표시
  여부는 `ToppingEditState.showQuitDialog`. `SegmentationViewModel`과 같은 모양이다.
- `CustomGalleryPickerEffect.NavigateToSegmentationConfirm` → `NavigateToToppingPlace`(인자 없음)로 바꾸고,
  실패 이펙트(`ShowDraftUnavailable`)를 추가한다.

## 화면

Figma 기준. 색·간격은 디자인시스템 토큰으로 옮긴다.

| 영역 | 구성 |
|---|---|
| 헤더 | `YGFloatingBarBackTitleClose`, 제목 「대상 영역 선택」 |
| 사진 | 남은 높이 전부(`weight(1f)`), 좌우 `padding7`. `ToppingEditCanvas` 그대로 |
| 편집 영역 | 위 `gap5`, 좌우 `padding7`, 세로 간격 `gap3`. ① 「브러시 크기」 라벨 줄 — 같은 줄 오른쪽 끝에 되돌리기·다시하기(`YGEditActionButton` 둘, 간격 `gap1`) ② `YGSlider` ③ 「영역 지우기」·「영역 채우기」(`YGEditButton`, 좌우 반씩) |
| 하단 | `YGButton` `YGButtonType.Large` 「다음」, 좌우 `padding7` |

- 되돌리기·다시하기는 Figma에서 편집 영역 위쪽 경계에 겹쳐 있다(절대 배치). 라벨 줄 오른쪽에
  `Row`로 붙이고, 겹침은 버튼 높이만큼 라벨 줄을 늘리지 않는 정도로 맞춘다. 정확한 겹침은 구현 PR에서
  미리보기로 대조한다.
- 지우기·채우기 버튼 사이 간격은 Figma에서 11px이고 맞는 토큰이 없다. 두 버튼이 반씩 나눠 갖는 것만 지키고,
  간격은 가장 가까운 토큰을 쓴다.
- 「다음」은 이미지를 불러오는 동안(`isLoading`) 비활성이다. 저장 중 딤(`ToppingEditSavingOverlay`)은 유지한다.
- 이제 쓰지 않는 문자열(`topping_edit_area_only_title`)은 지운다. 「다음」 문구는 이 모듈에 새로 둔다
  (`segmentation_confirm_next`는 확인 화면과 함께 지운다).

## 토스트

- 감지 실패 안내 문구: 「대상 감지에 실패했어요, 영역을 직접 선택해 주세요」 — `YGToastType.Edit`.
- 자리: 헤더 바로 아래, 화면 폭 전체로 사진 영역 위에 겹친다. 스캐폴드의 토스트 자리는 상태바 바로 아래라
  헤더를 덮는다. 그래서 화면이 헤더 아래에 `YGToastHost`를 둔다 — 배치 수정 화면(`ToppingArrangeLayout`의
  `toast` 슬롯)과 같은 방식이다. 로딩 덮개보다 아래 층이다.
- 한 진입에 한 번: `ToppingEditViewModel`이 `init`에서 `isDetectionFailed`일 때 `ShowDetectionFailed`를 보낸다.
  보냈다는 표시는 `SavedStateHandle`에 남긴다. 프로세스가 되살아나도 다시 띄우지 않는다. C-106에서 돌아올
  때는 같은 VM이라 `init`이 다시 돌지 않는다.
- 같은 호스트로 옮기는 것: `SaveFailed`·`SubjectTooSmall` → `YGToastType.Fail`.
- 옮기지 않는 것: `LoadFailed`. 띄우자마자 화면을 닫아서 화면 내 호스트로는 보이지 않는다. 지금처럼
  `android.widget.Toast`로 둔다.

## 삭제

- `NavKeySegmentationConfirm`, `SegmentationConfirmRoute`, `SegmentationConfirmScreen`,
  `SegmentationConfirmViewModel`, `SegmentationConfirmViewModelTest`, 엔트리 등록
- 튜토리얼: `img_segmentation_tutorial`, `segmentation_confirm_*` 문자열 전부
- `TutorialKind.SEGMENTATION`
  - 이름이 로컬 저장 키다. 저장값을 읽는 `UserConfigEntityMapper`는 모르는 이름을 버리므로 지워도 안전하다.
  - 이미 저장된 `"SEGMENTATION"`은 다음 저장 때 사라진다.
- 분석 화면 매핑: `NavKeySegmentationConfirm` → `C-103-select` 줄과 그 테스트
- `SegmentationEffect.GoToConfirm` — 후보 선택도 `GoToEdit`으로 간다. `GoToEdit`은 경로와 감지 실패 여부를
  싣도록 바꾼다

## 테스트

- `ToppingEditViewModelTest`
  - 「다음」이 언제나 초안을 기록하고 `GoToPlace`를 보낸다. 기록 실패는 `SaveFailed`다.
  - `isDetectionFailed = true`면 첫 생성에 `ShowDetectionFailed` 한 번. 같은 `SavedStateHandle`로 다시 만들면
    보내지 않는다. `false`면 보내지 않는다.
  - X → 팝업 표시, 취소 → 닫힘, 확정 → 닫기 이펙트.
- `SegmentationViewModelTest`
  - 후보 선택이 저장만 하고 초안을 기록하지 않으며, 저장본 경로를 실은 `GoToEdit(isDetectionFailed = false)`를 보낸다.
  - 0건·실패는 `GoToEdit(isDetectionFailed = true)`다.
- `CustomGalleryPickerViewModelTest`
  - 누끼 재사용이 맞춤 성공이면 `NavigateToToppingPlace`, 실패·예외면 `ShowDraftUnavailable`.
- `NavKeyAnalyticsScreenTest`: 확인 화면 줄을 걷는다. `NavKeyToppingEdit` 생성자 호출을 새 인자에 맞춘다.

## 주의 / 열린 질문

- 확인 화면이 하던 일 중 옮기지 않는 것
  - 완성된 알맹이를 크게 보여 주던 미리보기. 이제 C-104 캔버스가 그 역할이다.
  - 초안 흐름 구독(`GetToppingDraftFlowUseCase`)과 `DraftMissing` 알림. 초안이 비어 있는 경우는 C-106이 받는다.
- `docs/synthesis/open-questions.md`의 `ResultEffect` 항목은 `TOPPING_EDIT_RESULT_KEY` 왕복을 "되살아난
  실사용"으로 적었다. 이 왕복이 없어지면 근거가 바뀌므로 마지막 PR에서 그 항목을 갱신한다.
- `docs/status.md`의 「누끼 추출」·「토핑 생성·배치」 절에 확인 화면 서술이 있다. 마지막 PR에서 덮어쓴다.
