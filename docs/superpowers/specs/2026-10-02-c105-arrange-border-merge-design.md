---
id: c105-arrange-border-merge
title: 배치 화면에 테두리 설정 합치기 (C-105-Arrange border panel)
status: draft
category: behavior-spec
platforms: android
verified: 2026-10-02
related_code:
  - feature/groups/canvas/impl/.../screen/CanvasToppingPlaceScreen.kt#CanvasToppingPlaceScreen
  - feature/groups/canvas/impl/.../viewmodel/CanvasToppingPlaceViewModel.kt#CanvasToppingPlaceViewModel
  - feature/groups/canvas/impl/.../screen/CanvasBGEditScreen.kt#CanvasBGEditScreen
  - feature/groups/canvas/impl/.../viewmodel/CanvasBGEditViewModel.kt#CanvasBGEditViewModel
  - feature/groups/canvas/impl/.../component/ToppingTransformInput.kt#toppingTransformInput
  - feature/groups/canvas/impl/.../component/ToppingHitTestInput.kt#toppingTapInput
  - feature/segmentation/impl/.../screen/ToppingEditScreen.kt#ToppingEditScreen
  - feature/segmentation/impl/.../screen/ToppingBorderEditScreen.kt#ToppingBorderEditScreen
  - feature/segmentation/impl/.../component/BorderColorChipRow.kt#BorderColorChipRow
  - feature/segmentation/impl/.../component/BrushWidthSlider.kt#BrushWidthSlider
  - feature/segmentation/api/.../NavKeyToppingEdit.kt#NavKeyToppingEdit
  - domain/.../model/topping/ToppingDraft.kt#ToppingDraft
related_adr: ADR-0025, ADR-0026
related_spec: c106-topping-place, c301-topping-edit-tab, topping-pinch-gesture, topping-border-distance-field
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait]
---

# Spec: 배치 화면에 테두리 설정 합치기

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

테두리 편집을 별도 페이지에서 걷어 내고 배치 화면의 하단 패널로 합친다. 사진을 새로 쌓는
흐름(추가)과 이미 쌓은 본인 사진을 고치는 흐름(수정)이 같은 패널을 쓰고, 편집 화면은 영역
수정만 남긴다. 화면 뎁스가 하나 줄고, 테두리를 캔버스 배경 위에서 바로 보며 고를 수 있다.

## 정책 출처

- 피그마 `[디자인] 파르페 v0.1`
  - 추가: `C-105-Arrange`(node `5453:10418`), `C-105`(node `5461:9261`)
  - 수정: `C-105-Arrange`(node `5465:20693`), `C-105`(node `5465:20560`),
    `C-105-Arrange-Toast`(node `5479:13704`)
- 수정 플로우 정책 메모(피그마 주석 1~6절): 구조 변경·진입·헤더·패널 닫힘 상태·패널 열림
  상태·완료.

이 정책은 아직 `wiki/`에 반영되지 않았다. 위키 반영은 별도 `ingest` 작업이고 이 스펙의
범위가 아니다.

## 범위

- 포함
  - 공용 테두리 패널과 패널 열림 동작
  - 추가 플로우: 배치 화면 헤더·하단 버튼·패널·그만두기 팝업
  - 수정 플로우: 토핑 전용 화면 신설, 사진 상태 3종, 탭 동작, 변경 시에만 뜨는 그만두기 팝업
  - `CanvasBGEdit`의 배경 전용 축소
  - 편집 화면(`ToppingEdit`)의 테두리 탭과 `borderOnly` 진입 삭제
  - 초안(`ToppingDraft`)의 테두리 필드 삭제
- 제외
  - 테두리 되돌리기·다시 실행(패널에 없다)
  - 테두리를 여러 겹 두르기
  - 패널이 열릴 때 토핑 크기를 줄여 맞추기
  - 배경 편집 화면의 디자인 변경(탭이 사라지는 것 외에는 그대로 둔다)
  - 위키 반영

구현 계획은 둘로 나눈다. **계획 A**는 공용 부품과 추가 플로우, **계획 B**는 수정 플로우와
남은 테두리 편집 코드의 삭제다. A가 먼저 머지된다.

## API / 인터페이스

### 공용 부품 (`feature/groups/canvas/impl`)

```kotlin
/** 토핑에 두른 테두리. null 이면 두르지 않은 것이다 */
internal data class ToppingBorderStyle(
    val colorArgb: Int,
    val widthDp: Float,
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

@Composable
internal fun ToppingArrangeLayout(
    header: @Composable () -> Unit,
    onClickConfirm: () -> Unit,
    panel: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    canvas: @Composable BoxScope.() -> Unit,
)

/** 패널이 열려 있는 동안 포커스 토핑을 그릴 중심. 상태의 위치값은 건드리지 않는다 */
internal fun panelFocusCenter(canvasSize: DpSize): DpOffset
```

- `ToppingBorderPanel.selectedColorArgb`가 null이면 "없음" 칩이 켜진다.
- `isEnabled = false`면 접힌 바를 눌러도 `onClickToggle`이 불리지 않는다. 수정 플로우에서
  포커스가 없을 때 쓴다.
- `ToppingArrangeLayout`의 `panel` 슬롯은 캔버스 영역 하단에 겹쳐 놓인다.

### 옮기는 것

| 대상 | 지금 위치 | 옮길 위치 |
|---|---|---|
| `BorderColorChipRow`, `TOPPING_BORDER_COLORS` | `feature/segmentation/impl` | `feature/groups/canvas/impl` |
| `BrushWidthSlider` | `feature/segmentation/impl` | `core/designsystem` (`YGSlider`) |

슬라이더만 `core/designsystem`으로 가는 이유는 영역 탭의 붓 굵기가 계속 쓰기 때문이다.
색상칩과 팔레트는 테두리 탭이 사라지면 쓰는 곳이 canvas 모듈뿐이다.

### 헤더

- 수정 플로우: 이미 있는 `YGFloatingBarTitle`(제목 + 닫기).
- 추가 플로우: `YGFloatingBar.kt`에 뒤로 + 제목 + 닫기 변형을 더한다.

### 네비게이션

```kotlin
/** 오늘 캔버스에 쌓은 본인 토핑을 다시 배치하고 테두리를 고치는 화면 */
@Serializable
data class NavKeyCanvasToppingArrange(
    val groupId: Long,
    val parfaitId: Long,
    val initialToppingId: Long? = null,
) : NavKey
```

- `NavKeyCanvasBGEdit`에서 `initialToppingId`를 뺀다.
- `NavKeyToppingEdit`에서 `borderLayers`·`borderOnly`를 뺀다. `ToppingEditResult`에서
  `borderLayers`를 뺀다. `ToppingBorderLayer`는 삭제한다.

## 동작 / 상태

### 패널 (두 플로우 공통)

상태는 각 ViewModel `UiState`의 `isBorderPanelOpen` 하나다.

| 상태 | 표시 |
|---|---|
| 닫힘 | "테두리 설정" + 위 화살표. 배경 `YGAtomicColors.Transparency.White75` |
| 열림 | "테두리 굵기" 슬라이더 + 색상칩 줄 + 아래 화살표. 배경 `YGAtomicColors.Gray.White` |

두 상태 모두 좌·우·하 테두리는 `YGAtomicColors.Gray.Gray500`, 문구는
`YGTheme.typography.caption.c01M`, 색은 `YGAtomicColors.Gray.Gray700`이다.

**여는 입력**

1. 포커스된 토핑을 탭한다.
2. 접힌 바를 탭한다. 정책은 "▲ 아이콘 탭"이지만 바 전체를 터치 영역으로 잡는다.

**열려 있는 동안**

- 포커스 토핑을 그릴 때만 `panelFocusCenter`로 옮긴다. `UiState`의 위치값은 그대로다.
  그래서 저장은 따로 처리하지 않아도 원래 위치 기준이 된다.
- 옮기는 자리는 캔버스 가로 중앙, 세로는 캔버스 중앙에서 위로 올린 자리다. 올리는 양은
  피그마 두 프레임이 같은 값을 쓴다.
- 크기와 각도는 그대로 둔다. 이동은 애니메이션으로 한다. 닫히면 같은 애니메이션으로
  돌아온다.
- 점선 선택 박스와 삭제 버튼도 함께 움직인다.
- 캔버스 입력 레이어를 "무엇이든 닫기" 하나로 바꾼다. 탭·드래그·핀치 모두 패널만 닫고
  히트 판정을 하지 않는다. 이동·포커스 전환·토스트가 일어날 길이 없다.
- 아래 화살표를 탭해도 닫힌다.
- "캔버스에 쌓기"는 열린 채로도 닫힌 때와 같이 저장한다.

**값**

- 색을 고르면 그 색과 지금 굵기로 테두리를 두른다. "없음"을 고르면 벗긴다.
- 색이 "없음"인 동안 굵기를 바꾸면 다음에 고를 색에 쓸 굵기만 바뀐다.
- 굵기 범위는 `ToppingBorder.WIDTH_RANGE_DP`, 기본값은 지금 테두리 탭과 같다.

### 추가 플로우 (`CanvasToppingPlace`)

- 헤더는 뒤로 · "배치" · 닫기다. 하단 `YGFloatingBarEdit`는 없애고 "캔버스에 쌓기" 버튼이
  확정을 맡는다.
- 뒤로는 확인 화면으로 돌아간다. 고른 테두리는 화면 상태에만 있어 함께 사라진다.
- 닫기는 그만두기 팝업을 띄운다. 변경 여부를 보지 않고 언제나 띄운다. 확인하면
  `popUpTo<NavKeyCanvasMain>()`로 되감는다. 확인 화면의 닫기와 같은 동작이다.
- 토핑 위 탭이 패널을 연다. 지금은 변형 제스처만 받으므로 `toppingTapInput`을 더한다.
- 이미 쌓인 토핑은 지금처럼 전부 딤 아래에 깔린다.
- 테두리 값은 초안이 아니라 패널이 채운다. 초기값은 "없음"이다. 확정 때
  `toToppingBorder`는 그대로 쓴다.

### 수정 플로우 (`CanvasToppingArrange`)

`CanvasBGEditViewModel`의 토핑 부분을 새 ViewModel로 옮긴다. `CanvasToppingItem`, 오늘 캔버스
구독, `dirtyToppingIds`, `deletedToppingIds` 툼스톤, 삭제 모달, 변형 처리,
`updateDirtyToppings`가 그대로 따라간다. 좌표는 캔버스 대비 비율을 유지한다.

**사진 상태 3종**

| 상태 | 대상 | 표시 |
|---|---|---|
| 포커스 | 진입 때 탭한 본인 사진, 또는 화면에서 탭해 옮긴 본인 사진 | 점선 박스 + 좌상단 삭제 버튼 |
| 활성 | 나머지 본인 사진 | 딤 위에 그대로 |
| 딤 | 남의 사진 | 딤 아래 |

그리는 순서는 남의 사진 → 딤 → 본인 사진 → 포커스 장식이다.

**탭 동작 (패널 닫힘)**

| 탭 대상 | 결과 |
|---|---|
| 포커스된 사진 | 패널이 열린다 |
| 다른 본인 사진 | 포커스가 그 사진으로 옮겨 간다 |
| 남의 사진 | "다른 사람의 사진은 편집할 수 없어요" 토스트. 포커스는 그대로다 |
| 빈 캔버스 | 포커스가 풀린다 |

판정은 그리는 순서의 역순이다. 본인 사진을 먼저 보고, 남의 사진, 빈 곳 순이다.

- 포커스가 없으면 접힌 바를 눌러도 열리지 않는다. 바는 그대로 보인다.
- 드래그와 두 손가락 제스처는 포커스된 사진에만 걸린다.
- 포커스된 사진을 지우면 포커스가 없는 상태가 된다.
- 연필 버튼은 없앤다. `ToppingCornerButtons`에는 삭제 버튼만 남는다.

**패널**: 포커스된 사진의 테두리를 직접 갱신하고 `dirtyToppingIds`에 넣는다. 화면을
옮기지 않는다.

**닫기**: `dirtyToppingIds`가 비어 있지 않을 때만 그만두기 팝업을 띄운다. 비어 있으면 바로
돌아간다. 삭제는 모달 확인이 곧 서버 반영이라 변경으로 치지 않는다. 팝업 문구는 지금
배경·토핑 편집의 것("편집을 그만둘까요?")을 쓴다.

**확정**: `updateDirtyToppings()`가 전부 성공하면 돌아간다. 실패한 사진이 있으면 토스트를
띄우고 화면에 남으며, 실패한 것만 `dirtyToppingIds`에 남긴다.

### 배경 편집 (`CanvasBGEdit`)

- `CanvasEditTab`과 토핑 관련 상태·intent·effect를 뺀다.
- 토핑은 지금 배경 탭처럼 반투명으로 보여 주기만 한다.
- 하단 바는 탭 없는 `YGFloatingBarEdit`다.

### 편집 화면 (`ToppingEdit`)

- 영역 수정만 남긴다. `ToppingEditTab`, `borderHistory`, 테두리 intent,
  `SegmentationBorderControls`, `ToppingBorderEditScreen`, `ToppingBorderPreviewLayout`을
  삭제한다. 하단 바는 탭 없는 `YGFloatingBarEdit`다.
- 삭제는 계획 B의 마지막 단계다. 계획 A는 새 토핑 흐름에서 탭을 숨기기만 한다 —
  `borderOnly` 진입을 C-301이 B가 끝날 때까지 쓰기 때문이다.

### 확인 화면 (`SegmentationConfirm`)

- 갤러리 최근 줄의 알맹이로 들어온 경우(`isBorderOnlyEdit`)에는 "사진 편집" 버튼을
  숨긴다. 원본이 없어 영역을 고칠 수 없고 테두리는 다음 화면에서 고른다.
- 그 밖의 진입에서는 "사진 편집"이 영역 수정을 연다.
- 미리보기에 테두리를 그리지 않는다.

## 표시·제어 규칙

- 그만두기 팝업 문구는 플로우마다 다르다. 추가는 "사진 편집을 그만둘까요?", 수정은
  "편집을 그만둘까요?"다. 추가 쪽 문자열 네 개는 canvas impl `strings.xml`에 새로 둔다.
  segmentation 것과 같은 문구이고, 모듈 사이에 리소스를 나누지 않는다.
- 하단 버튼 문구는 두 플로우 모두 "캔버스에 쌓기"다.
- 분석 화면 id는 지금 값을 유지한다. 추가는 `"C-106"`, 수정은 `"C-305"`, 배경 편집은
  `"C-301"`이다. `NavKeyToppingEdit`의 `"C-105/C-306"` 갈래는 `borderOnly`와 함께 사라진다.

## 데이터

- `ToppingDraft`, `ToppingDraftEntity`, `ToppingDraftRepository.record`,
  `RecordToppingDraftUseCase`에서 `borderColorArgb`·`borderWidthDp`를 뺀다. 계획 A에서 한다.
- DataStore에 남아 있던 옛 초안의 테두리 값은 읽지 않고 버린다. 초안은 흐름이 끝나면
  비워지는 값이라 옮겨 줄 것이 없다.
- 서버 계약은 바뀌지 않는다. 추가는 `AddToppingUseCase`의 `border`, 수정은
  `UpdateToppingBorderUseCase`를 그대로 쓴다.

## 파일 구성

**계획 A**

| 파일 | 역할 |
|---|---|
| `canvas/impl/.../component/ToppingBorderPanel.kt` | 신규. 패널 |
| `canvas/impl/.../component/ToppingArrangeLayout.kt` | 신규. 화면 뼈대 |
| `canvas/impl/.../util/ToppingPanelFocus.kt` | 신규. `panelFocusCenter` |
| `canvas/impl/.../component/BorderColorChipRow.kt` | segmentation에서 이동 |
| `canvas/impl/.../util/ToppingBorderColors.kt` | segmentation에서 이동 |
| `core/designsystem/.../component/ygslider/YGSlider.kt` | segmentation에서 이동·개명 |
| `core/designsystem/.../ygfloatingbar/YGFloatingBar.kt` | 뒤로 + 제목 + 닫기 변형 추가 |
| `CanvasToppingPlaceScreen.kt`·`ViewModel.kt`·`Route.kt` | 헤더·버튼·패널·팝업 |
| `ToppingEditScreen.kt`·`ToppingEditViewModel.kt` | 새 토핑 흐름에서 탭 숨김 |
| `SegmentationConfirm*.kt` | "사진 편집" 조건, 테두리 미리보기 제거 |
| `ToppingDraft` 계열 (domain·data) | 테두리 필드 삭제 |

**계획 B**

| 파일 | 역할 |
|---|---|
| `canvas/api/.../NavKeyCanvasToppingArrange.kt` | 신규 |
| `canvas/impl/.../CanvasToppingArrangeRoute.kt`·`Screen.kt`·`ViewModel.kt` | 신규 |
| `CanvasBGEdit*.kt` | 배경 전용으로 축소 |
| `CanvasMainRoute.kt`·`CanvasMainViewModel.kt` | 본인 사진 탭 진입 변경 |
| `NavKeyToppingEdit.kt`, `ToppingBorderLayer.kt` | 테두리 인자·타입 삭제 |
| `ToppingBorderEditScreen.kt`, `ToppingBorderPreviewLayout.kt` 등 | 삭제 |
| `NavKeyAnalyticsScreen.kt` | 새 NavKey 매핑, `borderOnly` 갈래 삭제 |

## 테스트

- `CanvasToppingPlaceViewModelTest`: 패널 열고 닫기, 색·굵기 변경, 열린 채 확정했을 때 원래
  위치로 저장, 테두리 없이 확정.
- `CanvasToppingArrangeViewModelTest`(신규): 탭 네 갈래, 포커스 없을 때 패널이 안 열림,
  패널로 바꾼 테두리가 `dirtyToppingIds`에 들어가 PATCH됨, 변경 없을 때 닫기가 팝업 없이
  돌아감, 열린 채 확정했을 때 위치 불변. `CanvasBGEditViewModelTest`의 토핑 케이스를 옮긴다.
- `ToppingEditViewModelTest`·`SegmentationConfirmViewModelTest`: 테두리 케이스를 걷어 낸다.
- `panelFocusCenter`의 단위 테스트.
- 각 계획 끝에 에뮬레이터에서 피그마 다섯 장과 대조한다.

## 문서

구현할 때 갱신한다. 이 스펙 브랜치에서는 손대지 않는다.

- `docs/status.md`의 "토핑 생성·배치" 영역과 C-301 영역.
- `docs/synthesis/open-questions.md`: OQ-P-324(편집 세션은 여러 겹, 저장은 한 겹)와
  OQ-P-338(`borderOnly` 재편집 좌표계)은 전제가 사라져 닫는다. OQ-P-245(굵기가 절대 dp)는
  남는다.
- 새 ADR: 테두리는 편집이 아니라 배치 단계의 속성이다. ADR-0026의 초안에서 테두리 필드가
  빠지는 근거다.

## 주의 / 열린 질문

- 패널이 열릴 때 세로로 올리는 양은 피그마 한 화면 크기 기준 고정값이다. 화면 높이가 다른
  기기에서 토핑이 패널에 가려지는지 실기기로 확인해야 한다.
- 캔버스 폭 이상으로 키운 토핑은 가운데로 옮겨도 패널에 가려진다. 정책에 줄이는 규칙이
  없어 크기를 유지한다.
- 수정 플로우의 하단 버튼이 "캔버스에 쌓기"다. 이미 쌓은 사진을 고치는 화면이라 문구가
  어색하다. 피그마대로 만든다.
- 정책은 수정 플로우를 C-105-Arrange라 부르지만 분석 화면 id는 `"C-305"`를 유지한다.
  화면 id 체계를 정책에 맞출지는 정하지 않았다.
- 추가 플로우에서 뒤로 갔다 다시 오면 고른 테두리가 사라진다.
