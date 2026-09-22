---
id: c103-segmentation-topping-edit
title: C-103~C-105 누끼 추출·수동 편집·테두리 편집 플로우 (Segmentation / ToppingEdit)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-15
related_code:
  - NavKeySegmentation
  - NavKeySegmentationConfirm
  - NavKeyToppingEdit
  - ToppingBorderLayer
  - ToppingEditResult
  - TOPPING_EDIT_RESULT_KEY
  - SegmentationRoute.kt#SegmentationRoute
  - SegmentationConfirmRoute.kt#SegmentationConfirmRoute
  - ToppingEditRoute.kt#ToppingEditRoute
  - SegmentationScreen.kt#SegmentationScreen
  - SegmentationConfirmScreen.kt#SegmentationConfirmScreen
  - ToppingEditScreen.kt#ToppingEditScreen
  - ToppingBorderEditScreen.kt#ToppingBorderEditScreen
  - SegmentationViewModel.kt#SegmentationViewModel
  - ToppingEditViewModel.kt#ToppingEditViewModel
  - ToppingEditMask.kt#buildCutoutBitmap
  - ToppingEditMask.kt#withBorders
  - ToppingBorderOutline.kt#ToppingOutlineDistanceField
  - ToppingBorderOutline.kt#toOutlineDistanceField
  - ToppingBorderOutline.kt#toBorderBands
  - ToppingBorderColors.kt#TOPPING_BORDER_COLORS
  - ToppingEditStroke.kt#ToppingEditStroke
  - UndoRedoStack.kt#UndoRedoStack
  - BitmapUtils.kt#BitmapViewMapping
  - BorderColorChipRow.kt#BorderColorChipRow
  - BrushWidthSlider.kt#BrushWidthSlider
  - GuideBanner.kt#GuideBanner
  - SegmentationSubjectHighlight.kt#SegmentationSubjectHighlight
  - EntryBuilder.kt#featureSegmentationEntryBuilder
  - ImageSegmentationRepositoryImpl.kt#saveEditedImage
  - SaveEditedImageUseCase
  - SegmentationBounds
  - SegmentationResult
  - Navigator.kt#goToAndPopCurrent
  - Offset.kt#toPath
  - Offset.kt#toAndroidPath
  - ArgbExtension.kt#mixArgb
  - FloatArrayExtension.kt#fillWithSquaredDistance
  - feature/segmentation/impl/res/values/strings.xml
related_adr: ADR-0002, ADR-0005, ADR-0006, ADR-0009, ADR-0011, ADR-0012
related_spec: c101-camera-picture-confirm, c102-custom-gallery-picker, c001-canvas-main, c301-topping-edit-tab, designsystem-bar-listdate-components, designsystem-button-missing-components
related_architecture: navigation-flow, module-structure, data-layer, design-system
supersedes:
superseded_by:
tags: [spec, parfait, segmentation, topping, c103, c104, c105]
---

# Spec: C-103~C-105 누끼 추출·수동 편집·테두리 편집 플로우

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #221(`feature/segmentation-edit-stroke`,
> 이슈 #202)이 develop에 머지됐다(2026-08-14). `feature/segmentation` 모듈과 `NavKeySegmentation`은
> ADR-0012 라운드부터 있었으나 화면은 비어 있었고, 이번 PR이 로딩·에러·대상 하이라이트·확인·
> 수동 편집·테두리 편집을 한 번에 채워 **토핑 생성 경로가 카메라부터 캔버스 앞까지 이어졌다.**
> 아래는 머지 코드를 역기록한 것이며, 설계 대조가 아니라 **규약(parfait)·정책(위키) 대조**로
> 드리프트를 표기한다.

- **화면 ID**: C-103-loading · C-103-select · C-103 · C-104 · C-105 (아래 "화면 ID 대응" 참고)
- **대상 모듈**: `feature/segmentation/{api,impl}` + `domain`(모델·Repository·UseCase) +
  `data`(`ImageSegmentationRepositoryImpl`) + `core:navigation` + `core:util:{android,jvm}`

## 목표

카메라·갤러리에서 넘어온 사진에서 피사체를 잘라내고(C-103), 자동 결과를 손으로 다듬고(C-104),
테두리를 둘러(C-105) 캔버스 배치(C-106) 직전까지 이어지는 토핑 생성 경로를 완성한다.

## 화면 ID 대응

코드의 목적지 3개가 위키 [[누끼-따기]]의 서브 화면 5개를 다음처럼 묶는다.

| 위키 화면 | 코드 | 비고 |
|---|---|---|
| C-103-loading | `NavKeySegmentation` → `SegmentationLoadingScreen` | `state.isLoading` 분기 |
| C-103-select | `NavKeySegmentation` → `SegmentationScreen` 본문 | 단일 bounding box 하이라이트 탭. ~~**다중 검출 분기 없음**~~ 🔁 |
| C-103 | `NavKeySegmentationConfirm` → `SegmentationConfirmScreen` | 잘라낸 결과 확인 + "사진 편집"/"다음" |
| C-104 | `NavKeyToppingEdit` → `ToppingEditScreen` **영역 탭** | 브러시로 마스크 가감 |
| C-105 | 같은 목적지의 **테두리 탭** | 색·굵기 선택. 별도 화면이 아니라 탭 |

> 🔁 **첫 행이 화면에서 오버레이로 바뀌었다(2026-08-22, PR #311 develop 머지)** — C-103-loading을
> 받던 `SegmentationLoadingScreen`이 삭제되고 그 자리를 `SegmentationRoute`의
> `YGScaffoldV2(isLoading = state.isLoading)` 공통 오버레이가 받는다. **분기 조건은 그대로**
> (`state.isLoading`)이고 바뀐 것은 그것이 화면을 갈아 끼우는 대신 위에 덮인다는 점이다.
> 함께 지워진 `SegmentationErrorScreen`은 이 표에 대응 화면이 없었다 — 위키가 정의한 실패 처리
> (재시도·원본 사용)를 담은 적이 없기 때문이고(OQ-P-153), 지금은 공통 에러 토스트가 대신한다 →
> [ygscaffold-v2 스펙 "제외 철회"](2026-08-16-ygscaffold-v2-common-loading-error.md#제외-철회-2026-08-22-화면-고유-로딩과-에러-화면-흡수).

> 🔁 **C-103-select 행의 "다중 검출 분기 없음"이 거짓이 됐다(2026-08-23)** — 같은
> `NavKeySegmentation` 목적지가 이제 후보 수만큼 점선 박스를 그리고 탭으로 하나를 고르게 한다.
> **목적지를 쪼개지는 않았다** — 두 상태의 UI가 같은 형태라 화면 ID의 구분이 코드의 구분으로
> 이어지지 않는다. 후보가 1개면 이 표를 쓸 당시와 픽셀 단위로 같은 화면이다 →
> [c103-multi-subject-selection 스펙](2026-08-23-c103-multi-subject-selection.md).

## 범위

- **포함**
  - 진입 결선 — C-101-confirm "다음"이 `navigator.goToAndPopCurrent(NavKeySegmentation(sourceImageUri))`.
    확인 화면은 백스택에서 걷히므로 뒤로가면 카메라/갤러리로 돌아간다.
  - `Navigator.goToAndPopCurrent(destination)` 신설(`core:navigation`).
  - 세그멘테이션 결과 모델 재편 — `SegmentationResult`가 `BitmapWrapper`를 버리고
    `subjectImagePath` + `subjectBounds: SegmentationBounds?` 2필드가 된다.
    > 📌 **필드가 셋이 됐다(2026-08-19, PR #290)** — `trimmedSubjectImagePath`가 붙었다.
    > 수동 편집은 원본과 픽셀로 겹쳐 그려야 해 원본 크기를 유지하고, C-106 배치·미리보기는 여백 없는
    > 실제 크기가 필요해 갈렸다. `NavKeySegmentationConfirm`도 두 경로를 다 싣고 확인 화면은
    > 트리밍본으로 초기화한다 → [c106-topping-place 스펙](2026-08-19-c106-topping-place.md).
  - ML Kit 모듈 준비 확인 — `ModuleInstall.areModulesAvailable`/`installModules`로 optional module을
    실제 사용 직전에 확인·설치하고, 실패를 `SegmentationException.ModuleNotReady`/`Process`로 가른다.
  - 편집 결과 저장 — `ImageSegmentationRepository.saveEditedImage(BitmapWrapper)` +
    `SaveEditedImageUseCase`(캐시에 PNG로 떨구고 절대 경로 반환).
  - C-104 편집 — 지우기/채우기 2모드, dp 기준 브러시, 2핑거 확대(1~3배)·Pan clamping,
    지워진 자리 원본 50% 잔상, 탭별 `UndoRedoStack`.
  - C-105 테두리 — 색 9종 칩(맨 앞 투명 = 두르지 않음)·굵기 슬라이더·거리장(distance field) 기반 렌더.
  - 편집 결과 반환 — `ToppingEditResult`(최종 이미지·테두리 전 알맹이·겹 목록)를
    `LocalResultEventBus`로 `TOPPING_EDIT_RESULT_KEY`에 실어 확인 화면이 `ResultEffect`로 받는다.
  - 픽셀 유틸 3종을 `core:util`로 승격 — `List<Offset>.toPath()`/`toAndroidPath()`(android),
    `Int.fadeArgb`/`mixArgb`·`FloatArray.fillWithSquaredDistance`(jvm) + 유닛 테스트 2파일.
  - `feature/segmentation/impl` `strings.xml` 신설(안내·로딩·에러·버튼·탭·라벨 14종).
- **제외**(이번 라운드에서 안 함)
  - ~~다중 피사체 선택(C-103-select 본래 의미) — ML Kit `foregroundConfidenceMask` 단일 전경만 쓴다.~~
    🔁 **닫혔다(2026-08-23)** — `enableMultipleSubjects` + `enableSubjectBitmap`으로 바꿔 후보를
    목록으로 받는다. 전경 마스크는 후보가 0건일 때의 폴백으로만 남는다 →
    [c103-multi-subject-selection 스펙](2026-08-23-c103-multi-subject-selection.md).
  - 플로우 종료 경로 — 세 화면의 `onClickClose`가 전부 빈 람다 + TODO다.
  - 세그멘테이션 실패 후 재시도·원본 사용 — 에러 화면에 닫기뿐이다.
  - 확인 화면 디자인 — 코드 주석이 "디자인 확정 후 문구와 레이아웃 조정 필요"라고 남긴 임시 배치다.
  - 캐시 파일 정리 — 편집을 마칠 때마다 PNG를 최대 2장 더 떨구고 지우지 않는다.

## 동작 / 구조

### C-103 추출 (`NavKeySegmentation`)

- `SegmentationViewModel`이 `init`에서 `DecodeImageUseCase` → `SegmentImageUseCase`를 연달아 돈다.
  `originBitmap`은 성공 여부와 무관하게 먼저 상태에 실리고, `isLoading`은 성공/실패 어느 쪽이든
  마지막에 풀린다(로딩 화면에 갇히지 않게).
- `subjectBounds == null`(감지 픽셀 0)이면 하이라이트도 다음 경로도 없으므로 `isError`로 간다.
  > 🔁 **`isError` 상태가 없어졌다(2026-08-22, PR #311)** — 이 갈래를 포함한 실패 셋이
  > `SegmentationEffect.ShowError` 1회성 이펙트 + 공통 에러 토스트가 됐고 화면은 그대로 남는다
  > (아래 [화면 ID 대응](#화면-id-대응) 정정). 위 `isLoading` 서술은 그대로 유효하다 — 걷히는
  > 대상이 로딩 화면에서 오버레이로 바뀌었을 뿐이다.
- 본문은 원본 이미지(`ContentScale.Fit`) 위에 `SegmentationSubjectHighlight`를 같은 크기로 겹친다 —
  bounds 바깥만 `Transparency.Black25` 딤(`ClipOp.Difference`), 경계는 흰 dashed `Stroke`.
  탭 판정과 그리기가 같은 `subjectRect()` 계산을 공유한다.
- 상단 `GuideBanner`(`Gray850` 배경 + `ic_info_round`)가 "대상을 하나 선택" 안내를 띄운다.
- 대상을 탭하면 `goTo(NavKeySegmentationConfirm(sourceImageUri, subjectImagePath))` — `goTo`라
  뒤로가면 인식이 끝난 이 화면으로 그대로 돌아온다.

### C-103 확인 (`NavKeySegmentationConfirm`)

- **NavKey는 처음 열 때의 인자라 편집 결과를 담지 못한다.** 그래서 Route가 화면 상태 3개를
  `rememberSaveable`로 들고 있다 — `subjectImagePath`(표시용 최종본)·`cutoutImagePath`(테두리 전
  알맹이)·`borderLayers`.
- `ToppingBorderLayer`가 `rememberSaveable` 대상이 못 되므로 `listSaver`(`BorderLayersSaver`)로
  `colorArgb`·`widthDp` 두 값씩 펼쳐 저장한다.
- "사진 편집" → `goTo(NavKeyToppingEdit(sourceImageUri, …, borderLayers))`. 편집 화면이 `ContentResolver`로
  읽으므로 알맹이 파일 경로를 `File(...).toUri()`로 `file` 스킴 uri로 바꿔 넘긴다.
  **마스크로 넘기는 것은 최종본이 아니라 알맹이다** — 테두리가 구워진 이미지를 마스크로 주면
  그 색이 `SRC_IN` 단계에서 원본 픽셀로 덮여 테두리가 사라진다.
- "다음" → ~~`goTo(NavKeyCanvasMove(imageUri = subjectImagePath))`~~.
  > 📌 **목적지가 바뀌었다(2026-08-19, PR #290)** — `goTo(NavKeyCanvasToppingPlace(imageUri = …))`이고,
  > 넘기는 값도 파일 경로가 아니라 `File(...).toUri()`로 감싼 `file` 스킴 uri다(배치 화면이 Coil로
  > 읽는다). `NavKeyCanvasMove`는 호출자를 잃고 도달 불가로 남았다 →
  > [c106-topping-place 스펙](2026-08-19-c106-topping-place.md).

### C-104/C-105 편집 (`NavKeyToppingEdit`)

- `ToppingEditViewModel`은 Assisted 3인자(`sourceImageUri`·`segmentationImageUri`·`borderLayers`)를 받아
  두 비트맵을 디코드한다. 하나라도 실패하면 `LoadFailed` → Toast + `onBack()`.
  > 🔁 **as-built(PR #264, 2026-08-16)** — Assisted가 **4인자**가 됐다(`borderOnly` 추가).
  > `true`면 초기 탭이 `BORDER`이고 `ToppingEditState.isBorderOnly`가 켜져, 화면이
  > `YGFloatingBarEditTab` 대신 **`YGFloatingBarEdit`(제목 "테두리 편집")**을 그려 탭 전환 자체를
  > 없앤다 — 캔버스에 이미 놓인 토핑은 잘라내기 영역을 다시 못 건드린다는 규칙이 UI 부재로
  > 강제된다. 호출자는 C-301 토핑 탭이다 →
  > [c301-topping-edit-tab 스펙](2026-08-16-c301-topping-edit-tab.md).
- **되돌리기 스택이 탭마다 따로다**(`areaHistory`·`borderHistory`). 영역에서 지운 획이 테두리 탭의
  되돌리기로 살아나면 안 되기 때문이다.
- 마스크 합성(`buildCutoutBitmap`)은 알파 채널 3단계다 — ① segmentation 결과를 그려 시작 마스크,
  ② `ADD`는 불투명 `ERASE`는 `PorterDuff.CLEAR`로 획을 얹어 마스크 가감, ③ 원본을 `SRC_IN`으로 덮어
  마스크가 남은 자리에만 원본 픽셀을 채운다. ③ 덕분에 지운 곳을 다시 칠하면 원본이 복원된다.
- 테두리는 **거리장**으로 그린다(`toOutlineDistanceField`) — 실루엣 사본을 원 둘레에 찍어 두르는 방식은
  굵어질수록 자국 사이가 벌어지므로, 픽셀마다 실루엣까지의 거리를 한 번 재 두고 "거리 ≤ 굵기"인 자리를
  칠한다. 굵기를 밀 때는 거리를 다시 재지 않고 칠하기만 다시 한다.
  거리 계산은 `core:util:jvm`의 `FloatArray.fillWithSquaredDistance`(2-pass, 칸 수 비례)를 쓴다.
  > 🔁 **as-built(PR #425, 2026-09-01)** — 미리보기가 알맹이를 뷰에 꽉 채우던 것을 그만두고,
  > 사방에 여백을 둔 판 가운데에 앉힌 뒤 그 판 위에 테두리를 그린다. 그전에는 굵은 테두리가
  > 알맹이 가장자리에서 깎였다. 여백 상수와 굵기 상한이 서로를 모르는 것은 → OQ-P-337.
- **테두리는 겹치지 않는다.** `borderHistory`는 두른 겹이 아니라 *무엇을 골랐는지*를 쌓고,
  실제로 둘러지는 것은 `latest` 하나뿐이다(`borderLayers`가 최대 1건). 투명 칩은 "두르지 않음"이라
  이력만 남고 겹은 비운다.
- 굵기 슬라이더는 `push`가 아니라 `replaceLast`로 마지막 선택을 밀어 고친다 — 미는 동안 칸을 쌓으면
  되돌리기 한 번에 한 픽셀씩 물러나 쓸모가 없어진다.
- **굵기 단위는 dp다.** 화면이 편집 영역에 맞춰 사진을 줄인 배율을 되짚어(`originPxPerDp`)
  `ChangeOriginPxPerDp` 인텐트로 올려 보내고, 저장할 때 그 값으로 원본 비트맵 좌표계 굵기를 얻는다.
  탭을 열지 않아도 환산할 수 있도록 편집 영역 크기가 잡히는 즉시 보낸다.
- 획은 **원본 비트맵 좌표계**로 담는다(`ToppingEditStroke.points`·`width`). 화면 크기·회전이 바뀌어도
  편집 결과가 따라 변하지 않는다.
- 그리는 도중의 획은 ViewModel이 아니라 화면의 `mutableStateListOf`가 들고 있다가 드래그가 끝날 때
  한 번만 `AddStroke`로 확정한다. 두 번째 손가락이 닿으면 그리던 획을 버리고 확대/이동으로 넘어간다.
- 저장(`ClickDone`): `isSaving` 가드 → `buildCutoutBitmap` → `withBorders` → 파일 2회 저장
  (테두리가 없으면 알맹이가 곧 결과라 한 번만) → 두 비트맵 `recycle` → `EditCompleted`.
  저장 중에는 `ToppingEditSavingOverlay`가 `PointerEventPass.Initial`에서 눌림을 삼켜 뒤쪽 조작을 막는다.

### 결과 반환

```kotlin
// api — Compose 비의존(ARGB 정수로 색을 들고 NavKey 로 실린다)
@Serializable data class ToppingBorderLayer(val colorArgb: Int, val widthDp: Float)

@Serializable data class NavKeyToppingEdit(
    val sourceImageUri: String,
    val segmentationImageUri: String,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
    val borderOnly: Boolean = false,        // as-built #264 — true면 테두리 편집만 연다
) : NavKey

data class ToppingEditResult(               // @Serializable 아님 — 버스로만 오간다
    val editedImagePath: String,
    val cutoutImagePath: String,
    val borderLayers: List<ToppingBorderLayer>,
)
const val TOPPING_EDIT_RESULT_KEY = "topping_edit_result"
```

편집 화면이 `resultEventBus.sendResult(TOPPING_EDIT_RESULT_KEY, result)` 후 `onBack()`,
확인 화면이 `ResultEffect<ToppingEditResult>(resultKey = …)`로 받는다.
**`ResultEventBus` 왕복이 실사용 소비처를 되찾은 첫 사례다**(직전까지 남은 수신부는 死경로뿐이었다 →
[open-questions](../../../synthesis/open-questions.md) [2026-08-10]).

## 표시·제어 규칙

- 로딩·에러 화면은 같은 골격이다 — `YGFloatingBarClose` + 세로 중앙 `Column`,
  인디케이터 자리에 `CircularProgressIndicator`(`Cherry100`) 또는 `ic_warning_round`(`Cherry600`),
  아래 제목 `title.t03SB`/`Gray900` + 설명 `body.b02R`/`Gray500`.
- 추출·확인 화면은 `YGFloatingBarBackClose`, 편집 화면은 `YGFloatingBarEditTab`.
  **`YGFloatingBar`의 첫 실화면 소비처**이며 탭 문자열("영역"/"테두리")은 화면(`ToppingEditTab.label`)
  소유다 — 컴포넌트 기본값이 아니다.
- 편집 화면 조작부: 되돌리기/다시실행 `YGEditActionButton` 2개(탭에 따라 다른 스택을 본다),
  모드 전환 `YGEditButton` 2개(`ic_minus_round`/`ic_add_round`), 굵기 `BrushWidthSlider`.
  **`YGEditActionButton`·`YGEditButton`의 첫 실화면 소비처다.**
- `BrushWidthSlider`는 Material3 `Slider`의 트랙만 갈아 끼운다(기본 트랙이 stop indicator·gap을 함께
  그려 디자인과 어긋난다). 값 읽기를 `drawBehind` 안에서 해 드래그 중 recomposition 없이 다시 그리기만 한다.
- 붓 굵기를 미는 동안에만 캔버스 한가운데에 실제 크기 원을 미리 보여준다(`Cherry500`, 채움 50% + 1dp 테두리).
- 색상칩은 `LazyRow`이고 좌우 여백을 `modifier`가 아니라 `contentPadding`으로 준다 — 바깥에서 패딩을
  걸면 스크롤 영역까지 좁아져 칩이 화면 끝에 닿기 전에 잘린다.
- 선택 표시는 칩 색 위에 `Transparency.Black25`를 `compositeOver`로 섞고 흰 `ic_check`를 얹는다.
  투명 칩만 예외로 테두리·사선 색 자체를 바꾼다(섞을 바탕이 없어서).
- 엔트리 3개 모두 `YGScaffold { innerPadding -> …padding(innerPadding) }` 기본형이다
  (`contentWindowInsets` 조정 없음). 화면 최외곽 `YGScreen`은 쓰지 않는다.

## 드리프트 / 잔존

1. **누끼 캔버스 Safe Margin이 없다** — 위키 [[누끼-따기]] C-103-Selected 규격은 바운딩 박스 +20%로
   잘라낸 캔버스를 C-103~C-105에 넘기라고 하는데, 코드는 **원본 전체 크기**를 끝까지 들고 간다.
   `subjectBounds`는 계산되지만 하이라이트 표시에만 쓰이고 크롭에 쓰이지 않는다
   → [open-questions](../../../synthesis/open-questions.md) [2026-08-15].
2. **브러시·테두리 굵기 단위가 정책과 다르다** — 위키 [[누끼-편집]]은 2~50 **px**, 코드는 2~50 **dp**다.
   코드 주석이 "사진 해상도·기기 밀도가 달라도 체감 굵기가 같도록" dp를 골랐다고 근거를 남겼다.
3. ~~**C-103-select가 사실상 없다** — 다중 검출 분기 없이 단일 bounding box 하나를 탭하는 화면이다.~~
   🔁 **해소(2026-08-23)** — 후보가 여럿이면 점선 박스가 그 수만큼 뜨고 탭으로 고른다. 다만
   1번(Safe Margin 부재)은 **그대로 열려 있다** — 저장이 `persistSubject`로 갈렸어도 만드는 판은
   여전히 원본 전체 크기와 여백 0% 둘이다 →
   [c103-multi-subject-selection 스펙](2026-08-23-c103-multi-subject-selection.md) 정책 대조 표.
4. **테두리 색 9종의 정책 근거가 없다** — 4종은 `YGAtomicColors`(투명·흰·검·`Cherry200`)지만 5종은
   `Color(0xFF……)` 리터럴이고, 위키에 C-105 색 팔레트 정책 문서가 없다.
5. **死코드 2건** — `BitmapUtils.kt`의 `mapViewToBitmap`·`mapBitmapToViewFloat`가 사용처 0건이다.
6. ~~**플로우를 나갈 수 없다**~~ → ✅ **해소(2026-08-20, PR #309)** — 세 화면의 `onClickClose`와
   C-101-confirm의 닫기가 `Navigator.popUpTo<NavKeyCanvasMain>()`으로 결선됐다(배경 편집에서 들어온
   경로만 부른 화면으로 되감는다). 세그멘테이션 쪽은 로딩·에러·본문이 콜백 하나를 공유해 한 자리를
   채우자 셋이 함께 열렸다 → OQ-P-152.
7. **에러 화면에 재시도가 없다** — 위키 [[누끼-따기]]는 "실패 시 재시도 또는 원본 사용 옵션"을 적었는데
   닫기(빈 람다)뿐이다. `ModuleNotReady`는 코드가 "잠시 후 재시도하면 해결"이라 적어 둔 일시적 실패인데도
   재시도 경로가 없다.
8. **확인 화면이 [[이미지-렌더링-정책]] Case A/B를 안 탄다** — 비율 분기 없이 `ContentScale.Fit`이다.
   같은 정책을 C-101-confirm은 이미 지킨다(코드 주석이 임시 배치임을 자인).
9. **`UiState`가 raw `Bitmap`을 들고 있다** — `SegmentationState.originBitmap`,
   `ToppingEditState.originBitmap`/`segmentationBitmap`. `BitmapWrapper` 추상을 화면 경계에서 벗겨
   쓰는 형태다(ADR-0011은 domain 경계만 규정한다).
10. **치수 리터럴** — `Spacer(23.dp)`·`spacedBy(12.dp)`·`height(4.dp)`·`height(11.dp)`,
    칩 `36.dp`/체크 `24.dp`, 슬라이더 트랙 `2.dp`/thumb `16.dp`. A-002 라운드와 같은 유형이다.
11. **유닛 테스트가 편집 로직을 안 덮는다** — 이번 라운드에는 `feature/segmentation/impl`에
    `parfait.test.unit`조차 붙지 않았다. 추가된 테스트 2파일은 `core:util:jvm`의 픽셀 유틸
    (`ArgbExtension`·`FloatArrayExtension`)뿐이었다.
    > 📌 **절반만 닫혔다(2026-08-20, PR #309)** — 플러그인이 붙고 모듈이 첫 `src/test`를 얻었지만
    > 들어온 것은 `SegmentationViewModelTest`이고, **마스크 합성·거리장·`UndoRedoStack`은 여전히
    > 검증이 없다** → OQ-P-155.
12. ~~**`error()` 잔존**~~ → ✅ **해소(2026-08-20, PR #309)** — `foregroundConfidenceMask == null`이
    `Result.failure(SegmentationException.Process)`를 탄다. 같은 라운드가 마스크 크기 불일치와 저장
    구간의 `IOException`까지 같은 블록 안에서 접었다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-004 ②.

## 정책 대조 (위키)

| 위키 정책 | 코드 | 판정 |
|---|---|---|
| [[누끼-따기]] 입력 = C-101 촬영 / C-102 갤러리 | 두 진입점이 C-101-confirm에서 합류해 여기로 온다 | 일치 |
| [[누끼-따기]] 피사체 마스킹 → 투명 PNG | ML Kit 마스크(임계 0.5) → `cacheDir` PNG | 일치 |
| [[누끼-따기]] C-103-Selected 캔버스 = 바운딩 박스 **+20% Safe Margin**·투명 확장 | 원본 전체 크기 유지, 크롭 없음 | ⚠️ **미이행** |
| [[누끼-따기]] C-103-loading / C-103-select 분리 | ~~loading 있음, select는 단일 대상 하이라이트로 축약~~ 🔁 loading은 공통 오버레이(PR #311), select는 다중 후보 탭으로 채워졌다(2026-08-23). 목적지는 여전히 `NavKeySegmentation` 하나다 | 이행(화면 ID는 통합) |
| [[누끼-따기]] 실패 시 재시도 또는 원본 사용 | 에러 화면에 닫기뿐(#309로 캔버스 되감기 결선). 재시도·원본 사용은 없다 | 미이행 |
| [[누끼-편집]] 초기 렌더 Aspect Fit + 세로/가로 중앙 | `BitmapViewMapping.fitCenter` | 일치 |
| [[누끼-편집]] 2핑거 확대 허용 / **Scale 1.0 미만 축소 차단** | `MIN_ZOOM = 1f`, `MAX_ZOOM = 3f` | 일치(상한은 코드가 먼저 확정) |
| [[누끼-편집]] 확대 후 Pan **Clamping** | `clampPan` — 뷰포트보다 작은 축은 중앙 고정 | 일치 |
| [[누끼-편집]] 지워진 배경을 **불투명도 50%**로 노출 | `ERASED_AREA_ALPHA = 0.5f` | 일치 |
| (대응 조항 없음) | 🔁 **남는 영역에 `Cherry500` 틴트 한 겹**(`MASK_TINT_ALPHA`, `BlendMode.SrcAtop`)이 얹혔다(2026-09-03, PR #442). 영역 탭에서만 그린다 | **정책 문서 없음** → [open-questions](../../../synthesis/open-questions.md) OQ-P-347 |
| [[누끼-편집]] 영역 채우기가 지난 자리만 **100% 복구** | `SRC_IN`으로 원본 픽셀 복원 | 일치 |
| [[누끼-편집]] 브러시 2~50 / 테두리 2~50 | 값은 같으나 단위가 **dp**(정책은 px) | 값 일치·단위 갈림 |
| [[누끼-편집]] 상/하단 UI ↔ 이미지 최소 여백 10px | 편집 영역 좌우 `padding7` + 위아래 리터럴 간격 | 대응 조항 없음 |
| [[누끼-편집]] 확대 시 작업 공간 `Device Width − 40px` | 좌우 `padding7` 안에서만 그린다(`clipToBounds`) | 방향 일치(수치 미기재) |
| [[토핑]] 편집 완료 후 캔버스 배치(C-106) | "다음" → ~~`NavKeyCanvasMove(imageUri)`~~ → **`NavKeyCanvasToppingPlace(imageUri)`**(#290) | 일치 |
| C-105 테두리 색 팔레트 | 코드가 9종을 먼저 확정 | **정책 문서 없음** |

## 파일 구성

- `api/` — `NavKeySegmentation`(기존)·`NavKeySegmentationConfirm`·`NavKeyToppingEdit`+`ToppingEditResult`+
  `TOPPING_EDIT_RESULT_KEY`·`ToppingBorderLayer`.
- `impl/route/` — `SegmentationRoute`·`SegmentationConfirmRoute`(결과 수신·상태 보존)·`ToppingEditRoute`(효과 소비).
- `impl/screen/` — `SegmentationScreen`(로딩/에러/본문 분기)·`SegmentationLoadingScreen`·
  `SegmentationErrorScreen`·`SegmentationConfirmScreen`·`ToppingEditScreen`·`ToppingBorderEditScreen`·
  `BitmapUtils.kt`(`BitmapViewMapping`·`fitScale`·`clampPan`·좌표 변환).
  > 🔁 **파일 둘이 삭제됐다(2026-08-22, PR #311)** — `SegmentationLoadingScreen`·`SegmentationErrorScreen`이
  > 없어지고 `SegmentationScreen`은 분기 없이 **본문 하나**만 그린다.
- `impl/editor/` — `ToppingEditStroke.kt`(`ToppingEditTab`·`ToppingEditMode`·`ToppingEditStroke`)·
  `UndoRedoStack.kt`·`ToppingEditMask.kt`(`buildCutoutBitmap`·`withBorders`)·
  `ToppingBorderOutline.kt`(거리장·밴드)·`ToppingBorderColors.kt`.
- `impl/component/` — `GuideBanner`·`SegmentationSubjectHighlight`·`BorderColorChipRow`·`BrushWidthSlider`.
- `impl/viewmodel/` — `SegmentationViewModel`·`ToppingEditViewModel`.
- `impl/navigation/EntryBuilder.kt#featureSegmentationEntryBuilder` — entry 3개를 한 함수에서 등록.
- `domain/` — `model/SegmentationBounds`·`model/SegmentationResult`(재편)·
  `exception/SegmentationException`(+`ModuleNotReady`·`Process`)·`repository/image/ImageSegmentationRepository`
  (+`saveEditedImage`)·`usecase/image/SaveEditedImageUseCase`.
- `data/` — `ImageSegmentationRepositoryImpl`(모듈 설치 확인·bounds 계산·`saveEditedImage`).
- `core:navigation` — `Navigator.goToAndPopCurrent`.
- `core:util:android` — `extension/Offset.kt`. `core:util:jvm` — `extension/ArgbExtension.kt`·
  `extension/FloatArrayExtension.kt`(+ 유닛 테스트 2파일).

## 주의 / 열린 질문

- **저장 경로가 캐시에만 있다** — `parfait_<timestamp>.png`가 편집을 마칠 때마다 최대 2장 늘고 정리 경로가
  없다. ADR-0012 라운드부터 열려 있던 캐시 정리 미결이 이번에 파일 수만큼 커졌다
  → [open-questions](../../../synthesis/open-questions.md) [2026-07-12].
  > ✅ **정리 경로가 생겼다(2026-08-20, PR #309)** — 저장 위치가 `cacheDir`의 세그멘테이션 전용 하위
  > 디렉토리로 내려가고, 다음 세그멘테이션이 진입할 때 그 디렉토리를 통째로 비운다. 누적 상한이
  > 직전 흐름 1회분이 됐다 → OQ-P-003 ③. **저장 자체가 캐시에만 있다는 것은 그대로다.**
- **`ToppingEditResult`가 직렬화 대상이 아니다** — 프로세스 사망 후 복원 시 결과가 사라진다.
  확인 화면 쪽 `rememberSaveable` 3개는 살아남으므로 편집 완료분 자체는 보존되지만, 편집 중 사망은
  복구되지 않는다.
- **`saveEditedImage`가 `Result`를 두 번 감싼다** — `runCatching`으로 잡는 대상이 파일 쓰기뿐이라
  다운캐스트 실패는 별도 `Result.failure`다. 호출부는 `getOrNull()`만 보고 원인을 구분하지 않는다.
- **테두리 겹침이 모델과 실제가 갈린다** — `ToppingBorderLayer`의 KDoc·`toBorderBands`는 "겹겹이 쌓임"을
  전제로 누적 outset을 계산하는데, `ToppingEditState.borderLayers`는 언제나 최대 1건이라 그 경로가
  실제로는 한 겹만 탄다.
