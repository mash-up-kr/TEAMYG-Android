---
id: c301-topping-edit-tab
title: C-301 토핑 편집 탭 (선택·이동·크기·회전·삭제 + 테두리 재편집 왕복)
status: implemented
category: ui-spec
platforms: android
verified: 2026-09-01
related_code:
  - CanvasBGEditScreen.kt#CanvasBGEditScreen
  - CanvasBGEditScreen.kt#CanvasToppingImage
  - CanvasBGEditScreen.kt#ToppingCornerButtons
  - CanvasBGEditScreen.kt#ToppingSelectionStroke
  - CanvasBGEditScreen.kt#ToppingDragHandleButton
  - CanvasBGEditScreen.kt#rememberToppingSize
  - ToppingGeometry.kt#TOPPING_BASE_LONG_SIDE_RATIO
  - ToppingGeometry.kt#toppingLongSide
  - ToppingGeometry.kt#toppingCenter
  - CanvasBGEditViewModel.kt#CanvasToppingItem
  - CanvasBGEditViewModel.kt#CanvasBGEditViewModel
  - CanvasBGEditViewModel.kt#handleOnDeleteToppingDialogConfirm
  - CanvasBGEditViewModel.kt#handleOnClickConfirm
  - CanvasBGEditViewModel.kt#updateToppingIfChanged
  - DeleteToppingUseCase.kt#DeleteToppingUseCase
  - UpdateToppingUseCase.kt#UpdateToppingUseCase
  - ToppingRepository.kt#delete
  - ToppingRepository.kt#update
  - ToppingRepositoryImpl.kt#delete
  - ToppingRepositoryImpl.kt#update
  - CanvasBGEditRoute.kt#CanvasBGEditRoute
  - ToppingGeometry.kt#computeToppingStrokeCorners
  - ToppingGeometry.kt#computeToppingButtonPoints
  - ToppingGeometry.kt#toppingStrokeSize
  - ToppingGeometry.kt#resizeScaleFactor
  - ToppingGeometry.kt#rotationDeltaDegrees
  - Modifier.kt#centeredAt
  - Modifier.kt#dragBy
  - NavKeyToppingEdit
  - ToppingEditResult
  - TOPPING_EDIT_RESULT_KEY
  - ToppingEditViewModel.kt#ToppingEditViewModel
  - ToppingEditState.kt#isBorderOnly
  - YGCircleButton
  - YGFloatingBarEdit
  - YGModalPopup
  - feature/groups/canvas/impl/res/values/strings.xml
  - feature/segmentation/impl/res/values/strings.xml
related_adr: ADR-0002, ADR-0005, ADR-0006, ADR-0007
related_spec: c301-canvas-background-edit, c103-segmentation-topping-edit, c001-canvas-main, designsystem-canvas-components, designsystem-button-missing-components
related_architecture: navigation-flow, design-system, state-management, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, canvas, topping, c301, c305, c306]
---

# Spec: C-301 토핑 편집 탭

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #264(`feature/canvas-topping-screen`)가
> develop에 머지됐다(2026-08-16). 아래는 머지 코드를 역기록한 것이며, 설계 대조가 아니라
> **규약(parfait)·정책(위키) 대조**로 드리프트를 표기한다.

> **자리** — [c301 배경 편집 스펙](2026-08-15-c301-canvas-background-edit.md)이 "토핑 탭이
> 아무 일도 안 한다"(드리프트 3)고 적어 둔 그 절반이 이번에 채워졌다. 같은 화면·같은 세 파일이지만
> 다루는 축이 다르고(배경 선택 vs 토핑 조작) 규모가 커서 스펙을 나눠 적는다.

## 목표
C-301 편집 모드의 **토핑 탭**에서, 캔버스에 이미 놓인 **내 토핑**을 골라 옮기고(드래그)
크기·회전을 바꾸고 삭제하고, 테두리만 다시 손볼 수 있게 한다.

## 범위
- **포함**
  - 토핑 모델 `CanvasToppingItem` 신설(`parfaitImageId`·`isMine`·`imageResId`·`offsetX/Y`·
    `sourceImageUri`·`segmentationImageUri`·`scale`·`rotationDegrees`·`borderLayers`·`editedImagePath`).
  - 토핑 탭 본문 — 남의 토핑 → 딤 → 내 토핑 → 선택 오버레이의 **4층 스택**.
  - 선택/해제(내 토핑 탭 토글, 딤·남의 토핑 탭 = 해제), 드래그 이동, 모서리 4버튼
    (삭제·크기조절·편집·회전), 회전 따라 도는 점선 스트로크.
  - 삭제 확인 모달(`YGModalPopup`) + `strings.xml` 8줄(버튼 접근성 4 + 모달 문구 4).
  - **테두리 전용 재편집 왕복** — `NavKeyToppingEdit(borderOnly = true)`로 C-104/C-105 편집 화면을
    열되 영역 탭 없이 테두리만 열고, `ToppingEditResult`를 `ResultEffect`로 되받아 그 토핑에 반영.
  - `core:util:android` `extension/`에 `Modifier.centeredAt(DpOffset)`·`Modifier.dragBy(key, onDrag)` 신설
    (화면에서 뽑아 올린 것). `feature/groups/canvas/impl`이 `core:util:android`에 의존하기 시작.
  - 기하 계산 `util/ToppingGeometry.kt` 신설(회전 사각형 꼭짓점·스트로크 여백·버튼 밀어내기·
    크기조절 바깥 방향).
  - `core:designsystem` 아이콘 2종 신설(`ic_edit`·`ic_scale`).
  - 인텐트·이펙트 무인자 선언이 `class`에서 `data object`로 정리됐다(직전 스펙이 적어 둔 형태 교정).
- **제외**(이번 라운드에서 안 함)
  - **토핑 목록 조회** — `loadMockToppings()`가 템플릿 이미지 4개·좌표 하드코딩으로 만든 mock이고
    코드에 `TODO`가 달려 있다. 서버에 이미 있는 조회·수정·삭제 표면을 쓰지 않는다.
  - **편집 결과 저장** — 이동·크기·회전·삭제가 전부 `UiState` 안에서만 일어나고 확인 버튼은
    여전히 배경만 싣는다(`ConfirmBackground`). 화면을 나가면 사라진다.
  - **토핑 추가**(C-106 신규 배치) — 이 탭에는 추가 경로가 없다. 목록에 있는 것만 고친다.
  - z-index 조작, 다중 선택, 스냅·정렬 보조, 유닛 테스트(테스트 파일 변경 0건).

## 동작 / 구조

### 탭에 따라 화면이 갈린다
- `selectedTab == TOPPING`이면 팔레트 행이 통째로 사라지고, 미리보기 `Box`의 상하 패딩이
  `padding4`에서 **60dp/14dp 리터럴**로 바뀐다(코드 주석이 "공통에 없음"이라고 자인).
- 토핑은 미리보기 `Box`(`aspectRatio` + `clipToBounds`) **안쪽**에 그려지므로, 캔버스 밖으로 밀어낸
  픽셀은 잘린다.

### 4층 스택과 선택 규칙
```
[3] 선택 오버레이 — 점선 스트로크 + 모서리 4버튼   (selectedToppingId 있을 때만)
[2] 내 토핑들 (isMine)                            — 탭=선택 토글, 선택된 것만 드래그
[1] 딤 (Transparency.Black25, 전면)               — 탭=선택 해제
[0] 남의 토핑들 (!isMine)                          — 딤 아래라 어둡게 보인다, 탭=선택 해제
```
- 딤이 **한 장으로 캔버스 전체**를 덮고 그 위에 내 토핑을 다시 그린다. 그래서 "내 것만 밝다"가
  레이어 순서로 표현되고, 선택 여부와 무관하게 토핑 탭에 들어서면 곧바로 갈린다.
- `OnClickTopping`은 **`isMine`이 아니면 즉시 반환**한다. 남의 토핑 탭은 [0]층의 클릭이 아니라
  그 위 딤이 먼저 받으므로 결과적으로 선택 해제다.
- 같은 토핑을 다시 탭하면 `selectedToppingId`가 `null`이 된다(토글).

🔁 **탭을 받는 자리가 바뀌었다 (2026-08-27, PR #390·#389).**
[토핑 알파 판정](2026-08-26-topping-alpha-hit-test.md)이 토핑과 딤의 클릭을 전부 걷어내고,
[2]와 [3] 사이에 **전면을 덮는 판정 오버레이**를 하나 끼웠다. 이 표의 "탭=…" 결과는 그대로이되
만들어 내는 주체가 다르다 — 내 토핑 실루엣에 맞으면 선택 토글, 아무것도 안 맞으면 선택 해제이고,
남의 토핑은 결과가 어차피 선택 해제라 판정 대상에서 빠졌다. 드래그도 같은 오버레이가 받는다
(`Modifier.dragBy` 호출은 이 화면에서 걷혔고, 코너 드래그 핸들과 배치 화면은 그대로 쓴다).

### 조작 3종
| 조작 | 인텐트 | 환산 |
|---|---|---|
| 이동 | `OnToppingMoveDrag(DpOffset)` | 화면이 px→dp로 바꿔 올리고 `offsetX/Y`에 그대로 더한다 |
| 크기 | `OnToppingResizeDrag(Offset)` | 드래그 벡터를 **회전된 바깥 방향 단위벡터에 투영**해 배율 증분, 0.5~2.5로 클램프(**상한은 PR #335에서, 하한은 PR #398에서 바뀌었다** — 지금은 하한 0.05만 남았다) |
| 회전 | `OnToppingRotateDrag(Offset)` | **가로 성분만** 각도로 환산, 상·하한 없음 |

> 🔁 **위 두 줄은 2026-08-27(PR #397) 이전의 기록이다.** 인텐트가 픽셀이 아니라 환산된 값을 싣도록
> 바뀌었다 — `OnToppingResize(scaleFactor: Float)`·`OnToppingRotate(deltaDegrees: Float)`이고,
> 환산은 핸들 위치를 아는 **화면**이 하고 ViewModel은 곱하거나 더하기만 한다. 크기는 핸들이 중심에서
> 멀어진 비율을, 회전은 드래그를 핸들 벡터의 **접선에 투영**한 값을 쓴다
> (`ToppingGeometry#resizeScaleFactor`·`#rotationDeltaDegrees`). 아래 "바깥 방향 단위벡터" 설명과
> `resizeOutwardDirection`도 그 라운드에 함께 사라졌다 — 감도 상수(`TOPPING_DRAG_DEGREES_PER_PX`)와
> 바깥을 45도로 보던 가정이 둘 다 필요 없어졌기 때문이다. 고친 계기는 **결함**이다: 회전 핸들이
> 우측 하단이라 가로 성분만 보면 시계방향으로 끌 때 반시계로 돌았고, 배율에 고정량을 더하던 탓에
> 원본이 큰 사진에서 같은 손동작이 훨씬 크게 먹었다. 배율 하한은 손대지 않았다(OQ-P-325).

> 🔁 **핸들 두 개가 자리를 맞바꿨다(2026-09-01, PR #412)** — 네 모서리는 이제 좌상단 삭제 ·
> **우상단 회전** · 좌하단 편집 · **우하단 크기조절**이다(C-106 배치 화면도 같이 바뀌어 둘의 배치는
> 여전히 같다). #397 이후 환산이 핸들 좌표와 중심만 보므로 계산식·상하한은 손대지 않았고, 바뀐 것은
> 어느 모서리에 무엇을 그리느냐뿐이다. 커밋이 근거를 적지 않아 **디자인 근거는 확인되지 않았다**.

- 크기조절 핸들이 우측 상단에 고정이라(#412로 우측 하단), 토핑이 돌아 있으면 "바깥"도 같이 돌아야 한다.
  `resizeOutwardDirection(rotationDegrees)`가 우상단 대각선 단위벡터를 같은 각만큼 돌려 주고,
  드래그 벡터와의 내적이 곧 증감이다. 그래서 **거꾸로 선 토핑도 핸들을 바깥으로 끌면 커진다.**
- 세 조작 모두 `applyToppingTransform`을 지나며, 주석이 밝히듯 **경계 되돌림(clamp)을 하지 않는다** —
  캔버스 밖은 클리핑으로 안 보일 뿐 좌표는 그대로 나간다.

### 선택 표시의 기하
- 토핑 자신은 `graphicsLayer(scaleX/scaleY/rotationZ)`로 돌고 커지지만, **스트로크와 버튼은
  같이 회전시키지 않는다** — 버튼이 뒤집히면 아이콘이 거꾸로 보이기 때문이다.
- 그래서 좌표를 따로 계산한다. `ToppingGeometry`가 토핑 크기(배율 적용 후)에 여백
  (가로 `SizeTokens.Size8`·세로 `Size10`)을 두른 사각형의 회전 꼭짓점을 내고,
  버튼은 그 꼭짓점에서 **대각선 방향으로 (버튼 반지름 + 간격)만큼 더 밀어낸** 지점에 놓인다.
  밀어내는 양을 축별로 나눌 때 대각선 길이 비율을 쓰므로 종횡비가 달라도 간격이 고르다.
- 스트로크만은 `graphicsLayer(rotationZ)`로 돌린다(사각형이라 뒤집혀도 같다).
- 버튼 배치는 `Modifier.centeredAt(point)` — `layout`으로 자기 크기의 절반을 빼서 **중심을 점에 맞춘다**.
  🔁 **2026-09-08(PR #465)부터 스트로크와 토핑 이미지도 같은 방식으로 놓는다** — 좌상단을 직접 계산해
  `offset` 으로 넘기면 토핑이 부모보다 커지는 순간 넘친 양의 절반만큼 밀린다
  ([c106 스펙](2026-08-19-c106-topping-place.md) 「그리기」 절).
- 토핑의 기본 크기는 `painter.intrinsicSize`를 dp로 읽은 값이고, 아직 모를 때는 60dp 정사각을 임시로
  쓴다(`rememberToppingBaseSize`).

### 모서리 4버튼
좌상 삭제(`ic_close`) · 우상 크기조절(`ic_scale`, 핸들) · 좌하 편집(`ic_edit`) · 우하 회전(`ic_rotate`, 핸들).
- 넷 다 `YGCircleButton(YGCircleButtonType.Small)`이고, 핸들 둘은 **`onClick = {}` 빈 람다에
  `Modifier.dragBy`를 덧대는 방식**이다(누르는 버튼이 아니라 잡는 손잡이).
- 삭제는 곧바로 지우지 않고 `showDeleteToppingDialog`로 확인 모달을 띄운다. 확인 시 목록에서 제거 +
  선택 해제. 배치는 **파괴적 액션=좌 Secondary(`삭제하기`) / 취소=우 Primary(`그만두기`)**로,
  같은 화면의 그만두기 모달과 같은 진영이다.

### 테두리 재편집 왕복
```
NavKeyCanvasBGEdit ─(편집 버튼)─▶ NavKeyToppingEdit(source, segmentation, borderLayers, borderOnly = true)
        ▲                                                   │ sendResult(TOPPING_EDIT_RESULT_KEY) + onBack
        └───────────────────────────────────────────────────┘
```
- `borderOnly = true`면 `ToppingEditViewModel`이 초기 탭을 `BORDER`로 세우고 `isBorderOnly`를 켠다.
  > 🔁 **as-built(PR #425, 2026-09-01)** — 이 플래그의 뜻이 "이미 캔버스에 놓인 토핑"에서
  > **"되살릴 원본이 없는 진입"**으로 넓어졌다. 누끼 확인 화면의 최근 알맹이 재사용 진입이 같은 값으로
  > 들어와 호출자가 넷이 됐다(→ OQ-P-338).
  화면은 `YGFloatingBarEditTab`(영역|테두리) 대신 **`YGFloatingBarEdit`(제목 "테두리 편집")**을 그려
  탭 전환 자체를 없앤다. 이미 캔버스에 놓인 토핑의 잘라내기 영역은 다시 건드릴 수 없다는 규칙이
  UI 부재로 강제된다.
- 편집을 시작한 토핑 id는 **Route의 `rememberSaveable`**(`editingToppingId`)이 들고 있다가
  결과가 오면 인텐트에 실어 준다. ViewModel은 어느 토핑을 편집하러 갔는지 모른다.
- 결과 반영은 세 필드다 — `segmentationImageUri`를 **방금 구운 알맹이(`cutoutImagePath`)로 교체**하고
  (다시 열 때 지운/되살린 영역이 유지된다), `borderLayers`·`editedImagePath`를 갈아 끼운다.
  `editedImagePath`가 있으면 그리기도 그쪽 파일을 쓴다.
- 같은 목적지를 **세 번째 호출자**가 쓰기 시작했다(C-103 확인 화면 → C-301). 인자 하나로 두 모드를
  겸하는 방식은 배경 편집이 만든 `returnResultOnly` 관용구와 같은 부류다.

## 정책 대조 (위키)

| 정책 항목 | 코드 | 판정 |
|---|---|---|
| 본인 토핑 탭 → C-305 토핑 편집([[C-202-토핑-편집자-확인-규칙-v0.1]]) | 본인 토핑 탭 = 선택(스트로크+4버튼), 편집 버튼이 따로 있다 | **방식 다름** — 탭이 곧 편집 진입이 아니다 |
| 타인 토핑 탭 → Spotlight + 작성자 Toast | 딤이 먼저 받아 **선택 해제**, 작성자 표시 없음 | **미구현**(C-202는 캔버스 상세 대상이라 화면이 다르다 — 편집 모드 규칙은 정책 부재) |
| C-106 초기 크기 = 더 긴 쪽이 캔버스 가로 40% | 이미지 `intrinsicSize` 그대로(배율 1) | **불일치** — 다만 이 탭에는 신규 배치 경로가 없다 |
| C-106 초기 위치 = 정중앙 | mock 하드코딩 좌표 | **대조 불가**(신규 배치 없음) |
| C-106 최소 터치 방어(짧은 쪽 48px) | 없음 | **미구현** |
| C-106 이탈 허용 + 캔버스 클리핑 | clamp 없음 + `clipToBounds()` | **일치** |
| C-306 테두리 편집(위키 표 잔존) | `borderOnly` 모드로 성립 | 일치(화면은 C-104/C-105와 공유) |
| 토핑 삭제 확인 문구 | 위키에 대응 소스 없음 | **대조 대상 부재** — 코드가 확정 |

## 규약 대조 (parfait)
- **MVI**: 3분할 유지, 이펙트 수집은 Route 한 곳. 무인자 인텐트·이펙트가 `data object`로 정리돼
  [state-management](../../../architecture/state-management.md) 관용구에 맞았다. 다만 편집 대상 id를
  Route가 들고 있어 **화면 상태의 일부가 ViewModel 밖**에 산다.
- **State가 UI 타입을 든다**: `CanvasToppingItem`이 `@DrawableRes Int`·`Dp`·Compose `Offset` 계열을
  들고 화면 목록이 곧 도메인 목록이다(배경 팔레트가 `Color`를 든 것과 같은 부류).
- **클릭 규약 이탈**: 토핑·딤이 `clickable(indication = null)`을 직접 쓴다(`clickableYG` 미사용).
  ✅ **소멸했다(2026-08-27, PR #390)** — 두 클릭이 판정 오버레이의 `pointerInput` 하나로 합쳐져
  이 화면에 클릭 모디파이어가 남지 않았다(팔레트 원형 둘은 `clickableYGNoRipple`을 쓴다).
- **그리기 프리미티브**: 점선 스트로크를 `core:designsystem`의 `dashedBorder()`가 아니라 화면이
  `drawBehind` + `dashPathEffect`로 직접 그린다. 같은 층위가 또 한 곳 늘었다
  → [design-system](../../../architecture/design-system.md).
- **치수 리터럴**: 60·14dp(탭별 패딩)·2dp(스트로크 굵기)·7.5/9dp(점선 간격)·14dp(버튼 시각 반지름)·
  7dp(모서리 간격)가 토큰 밖이다. 스트로크 여백만 `SizeTokens`를 쓴다.
- **모듈 경계**: 제스처·배치 확장 2종을 화면에 두지 않고 `core:util:android`로 올린 것은 규약대로다
  (`clearFocusOnTap` 선례). 반면 기하 계산은 feature `util/`에 남았다 — 캔버스 전용이라 타당하다.

## 드리프트 / 잔존

1. ~~**고칠 대상이 mock이다**~~ — ✅ **해소(PR #329, 2026-08-22)**. `loadMockToppings()`가 사라지고
   `GetTodayParfaitUseCase` 응답을 그린다. 자세한 것은 아래
   [as-built 재정정](#as-built-재정정-2026-08-22-pr-329-develop-머지)
   → [open-questions](../../../synthesis/open-questions.md) OQ-P-199 ①.
2. **편집 결과가 어디에도 남지 않는다** — 이동·크기·회전·삭제·테두리 재편집이 전부 `UiState`에서
   끝나고, 확인 버튼은 배경만 싣는다. 배경이 겪던 것과 같은 왕복 미완이 토핑에서 반복된다
   → [open-questions](../../../synthesis/open-questions.md) [2026-08-16].
   > 🔁 **부분 해소(PR #335, 2026-08-23)** — **삭제만** 서버에 남는다. 나머지 넷은 그대로다.
   > 아래 [as-built 재정정](#as-built-재정정-2026-08-23-pr-335-develop-머지) 참고.
   > ✅ **거의 닫혔다(PR #336, 2026-08-23)** — 이동·크기·회전이 확인 버튼에서 PATCH로 나간다.
   > **다섯 중 넷이 남고 테두리 재편집만 혼자 사라진다**
   > → 아래 [as-built 재정정](#as-built-재정정-2026-08-23-pr-336-develop-머지) ·
   > [open-questions](../../../synthesis/open-questions.md) OQ-P-276.
3. **C-106 배치 규격이 코드에 없다** — 40%·정중앙·48px 방어가 전부 빠졌다. 신규 배치 경로가 없어
   당장 어긋나지는 않지만, 추가 경로가 붙을 때 이 규격이 어디에 살지 정해져 있지 않다
   → [open-questions](../../../synthesis/open-questions.md) [2026-08-16].
4. **회전에 상·하한이 없다** — ~~크기는 0.5~2.5로 클램프하는데~~ 각도는 무한히 누적된다.
   `rotationDegrees`가 커져도 렌더는 같지만 저장 계약이 생기면 정규화 주체가 필요하다.
   > ⚠️ **크기 쪽 전제가 뒤집혔다(PR #335, 2026-08-23)** — `TOPPING_MAX_SCALE`이 삭제돼 상한이
   > 사라졌고 하한만 남았다. 이제 **상·하한이 없는 축이 둘**이다
   > → [open-questions](../../../synthesis/open-questions.md) OQ-P-271.
   > ⚠️ **"저장 계약이 생기면"이 같은 날 현실이 됐다(PR #336, 2026-08-23)** — 두 축의 값이 그대로
   > PATCH 본문에 실린다. 정규화 주체는 여전히 없고 서버도 범위를 검증하지 않는다
   > ([api/parfait-image.md](../../../api/parfait-image.md) 미결).
   > ⚠️ **남아 있던 하한마저 열 배 낮아졌다(PR #398, 2026-08-27)** — `TOPPING_MIN_SCALE`이
   > 0.5에서 **0.05**가 됐다. 같은 저장소의 배치 화면(C-106)은 하한을 상수가 아니라 **짧은 변
   > 48dp**에서 역산하는데(`CanvasToppingPlaceViewModel`의 `MIN_TOPPING_SHORT_SIDE`), 이 탭은
   > 그 방어가 없어 두 화면의 하한이 크게 갈렸다. 줄인 결과는 그대로 PATCH로 나간다
   > → [open-questions](../../../synthesis/open-questions.md) OQ-P-325.
5. **선택 상태가 목록 변화를 견디지 못한다** — `selectedToppingId`가 목록에서 사라진 id를 가리켜도
   화면은 조용히 아무것도 그리지 않는다(삭제 경로에서는 함께 비우지만, 목록이 서버에서 갱신되기
   시작하면 그 보장이 사라진다).
6. **접근성 표시가 아이콘 라벨뿐이다** — 핸들 2종은 `onClick = {}`이라 스크린리더가 "버튼"으로 읽고
   눌러도 아무 일이 없다. 드래그 조작의 대체 수단이 없다.

## 파일 구성

```
build-logic/convention/
  buildlogic/AndroidConfig.kt          release 빌드 타입에 release signingConfig 결선(이번 delta 동승)
core/designsystem/
  res/drawable/ic_edit.xml             신설
  res/drawable/ic_scale.xml            신설
core/util/android/
  extension/Modifier.kt                centeredAt·dragBy 추가
feature/groups/canvas/impl/
  build.gradle.kts                     core:util:android 의존 추가
  route/CanvasBGEditRoute.kt           ToppingEditResult 수신 + editingToppingId + 토핑 콜백 결선
  screen/CanvasBGEditScreen.kt         토핑 4층 스택·모서리 버튼·스트로크·삭제 모달
  viewmodel/CanvasBGEditViewModel.kt   CanvasToppingItem + mock 로드 + 조작 인텐트 8종
  util/ToppingGeometry.kt              신설 — 회전 사각형 기하
  res/values/strings.xml               8줄 추가
feature/segmentation/
  api/NavKeyToppingEdit.kt             borderOnly 인자 추가
  impl/route/ToppingEditRoute.kt       borderOnly 전달
  impl/screen/ToppingEditScreen.kt     borderOnly면 YGFloatingBarEdit로 교체
  impl/viewmodel/ToppingEditViewModel.kt  Assisted 4인자 + isBorderOnly 초기 상태
  impl/res/values/strings.xml          1줄 추가
```

## as-built 재정정 (2026-08-22, PR #329 develop 머지)

> **드리프트 1이 닫혔다** — 고치는 대상이 mock에서 서버 캔버스로 바뀌었다. 배경 저장이 붙은 같은
> 라운드이고, 그쪽 결정은
> [c301 배경 스펙](2026-08-15-c301-canvas-background-edit.md#as-built-재정정-2026-08-22-pr-329-develop-머지)에 있다.

### 그리는 값이 바뀌었다

`CanvasToppingItem`이 화면 좌표를 버리고 **캔버스 메인과 같은 단위**를 든다.

| 전 | 후 | 왜 |
|---|---|---|
| `imageResId`(템플릿 drawable) | `imageUrl`(서버 주소) | 조회 응답이 준다 |
| `offsetX`·`offsetY`(Dp) | `positionX`·`positionY`(0~1 중심점) | 저장된 배치가 그 단위이고 ViewModel은 화면 크기를 모른다 |
| `graphicsLayer(scaleX/scaleY)` | 긴 변 = 캔버스 너비 × 40% × `scale` | 캔버스 메인과 같은 규칙 |
| `rememberToppingBaseSize`(intrinsic을 dp로 직독) | `rememberToppingSize`(긴 변에서 비율로 편다) | 스트로크·모서리 버튼이 투명 여백이 아니라 그림에 붙어야 한다 |

같은 캔버스가 두 화면에서 다르게 보이면 안 되므로, 배치 규칙 셋(`TOPPING_BASE_LONG_SIDE_RATIO`·
`toppingLongSide`·`toppingCenter`)이 `component/CanvasToppingLayer.kt`에서 `util/ToppingGeometry.kt`로
올라가 **캔버스 메인·편집 탭·배치 화면 셋이 같은 값을 본다.** 드래그도 픽셀이 아니라 비율로
넘어온다 — 화면이 `BoxWithConstraints`로 실측을 알고 있어 거기서 환산한다.

### 잔존과 새 위험

- **드리프트 2(편집 결과가 어디에도 안 남는다)는 그대로다.** 이동·크기·회전은 여전히 `UiState`
  안에서 끝나고, 삭제는 `TODO(#271 대기)`로 화면에서만 사라지며, 토핑 편집 진입은
  `TODO(#274 대기)`다(서버 토핑은 https 주소라 편집 화면이 `ContentResolver`로 열지 못한다).
  확인 버튼은 배경만 저장한다 → OQ-P-199 ②③.
  ✅ **`TODO(#274)`가 닫혔다**(2026-08-27, PR #369 — 아래
  [as-built 재정정](#as-built-재정정-2026-08-27-라운드)).
- ⚠️ **소유 판정이 축이 다른 두 id를 비교한다** — `isMine`이 상수 `false`를 벗어나
  `MyAccountVO.memberId`(계정 id)와 `placedBy.groupMemberId`(그룹 멤버십 행 id)를 견준다. 코드
  KDoc이 그 사실을 `TODO(서버 응답 확장 대기)`로 적어 두었다. **이 화면에서는 그 판정이 곧
  게이트**라, 참으로 새면 남의 토핑을 만지고 거짓으로 접히면 내 토핑도 못 만진다
  → [open-questions](../../../synthesis/open-questions.md) OQ-P-250.
  ✅ **닫혔다**(2026-08-26, PR #376 — 아래 [as-built 재정정](#as-built-재정정-2026-08-26-pr-376-develop-머지)).
  그 `TODO`가 예고한 "응답에 isMine이 실리면 그것으로 갈아탄다"가 그대로 일어났다.
- ~~**테두리는 여전히 안 그린다**~~ → ✅ **그린다(2026-08-27, PR #388)**. `CanvasToppingImage`가
  `YGToppingCutoutImage`로 갈아타고 `borderLayers` 첫 겹의 색·두께를 넘긴다. 판정 모양을 외형과
  맞추려는 [토핑 알파 판정](2026-08-26-topping-alpha-hit-test.md)이 렌더링까지 범위에 넣은 결과다
  → [open-questions](../../../synthesis/open-questions.md) OQ-P-254 해소. **저장 쪽(`borderLayers`가
  PATCH에 안 실린다, OQ-P-276)은 그대로다.** ✅ **그것도 닫혔다(2026-08-27, PR #369)** — 다만
  **그리는 겹과 보내는 겹이 다르다**(첫 겹 vs 마지막 겹) → OQ-P-324.
- 드리프트 4(회전 한계 없음)·5(선택 상태가 목록 변화를 못 견딤)는 그대로다. 5는 목록이 실제로
  서버에서 오기 시작해 **전제가 현실이 됐다** — 다시 조회하는 경로가 진입 1회뿐이라 아직 증상이
  없을 뿐이다. **6(접근성)은 부분 해소다** — 2026-08-27 라운드가 토핑마다 자손 병합
  시맨틱스(역할·콘텐츠 설명·클릭 액션)를 붙여 스크린리더가 토핑을 잡을 수 있게 됐다. 읽어 줄
  문구는 임시 문자열이다(OQ-P-314).

### 유닛 테스트

`CanvasBGEditViewModelTest`에 이 탭 몫으로 다섯이 붙었다 — 비율 배치·`positionZ` 정렬·소유 판정,
드래그가 받은 비율만큼 옮기는지, 남의 토핑 탭이 선택으로 이어지지 않는지, 테두리 한 겹 펴기와
못 읽는 색에서 겹을 만들지 않는지다.

## as-built 재정정 (2026-08-23, PR #335 develop 머지)

> **드리프트 2가 삭제 한 갈래만 열렸다** — 편집 결과 다섯 중 삭제가 처음으로 서버에 남는다.
> 브랜치 `feature/topping-delete`, 머지 `f31b8c30`, 6파일 삽입 163줄·삭제 9줄. 머지 커밋 트리가
> 브랜치 팁 `122d950b`과 같아 충돌 해소 편집은 0건이다.

### 삭제는 확인 모달이 곧 서버 요청이다

`ToppingRepository.delete`와 `DeleteToppingUseCase`가 신설돼 **DataSource의 넷 중 둘째**가 열렸다
(첫째는 배치, [api/parfait-image.md](../../../api/parfait-image.md)). 흐름은 이렇다.

1. 모달의 "삭제하기"를 누르면 **모달을 먼저 닫고**(`showDeleteToppingDialog = false`)
   `launch(key = DELETE_TOPPING_KEY)`로 `DELETE`를 태운다.
2. 성공해야 목록에서 빠지고 선택이 풀린다. 서버에 반영되지 않은 것을 화면에서 지우지 않는다.
3. 선택된 토핑이 없으면 인텐트가 와도 요청하지 않는다.

`selectedToppingId`가 `Long`이라 호출 직전에 `ParfaitImageId`로 감싼다.

### 이 라운드가 만든 비대칭

- **삭제만 즉시 영구다.** 이동·크기·회전·테두리 재편집은 여전히 `UiState` 안에서 끝나고 확인 버튼은
  배경만 저장한다. 그래서 **"그만두기"로 나가도 지운 토핑은 돌아오지 않는다** — 같은 화면의 두
  파괴적 조작이 되돌림 가능성에서 갈렸는데 화면은 그 차이를 말하지 않는다.
- **실패가 조용하다.** 실패 갈래가 `viewModelLogger.e` 한 줄이다. 이 화면에는 이미
  `CanvasBGEditEffect.ShowError`와 Route의 토스트 정책이 있고 배경 저장 실패는 그 길로 나가는데,
  삭제만 쓰지 않는다. 계약상 403 `PARFAIT_IMAGE_NOT_OWNED`·409 `PARFAIT_ALREADY_CLOSED`·404가
  전부 무반응으로 접힌다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-270.
- **크기 상한이 사라졌다.** 같은 커밋이 `TOPPING_MAX_SCALE = 2.5f`를 지우고
  `coerceIn(MIN, MAX)`를 `coerceAtLeast(MIN)`으로 바꿨다. 근거가 커밋에도 코드에도 없고, 서버 검증도
  없어 막는 자리가 이제 아무 데도 없다 → 같은 문서 OQ-P-271.

### 유닛 테스트

다섯이 붙었다. `ToppingRepositoryImplTest` 둘 — 성공 값을 가공 없이 넘기는지, `ApiException.Business`
403을 `AppError.Server`로 바꾸면서 **코드 문자열과 상태 코드를 함께 살리는지**.
`CanvasBGEditViewModelTest` 셋 — 성공 시 목록에서 빠지고 선택이 풀리는지, 실패 시 목록이 그대로인지,
선택이 없으면 호출하지 않는지. 저장소 전체 유닛은 737 → **745건**(같은 라운드의 갤러리 저장 셋 포함),
테스트 클래스는 **87개**로 그대로다.

## as-built 재정정 (2026-08-23, PR #336 develop 머지)

> **드리프트 2가 거의 닫혔다** — 편집 결과 다섯 중 넷이 서버에 남는다. 브랜치
> `feature/topping-edit-c305`, 머지 `d634efd3`, 6파일 삽입 318줄·삭제 32줄. 머지 커밋 트리가
> 브랜치의 develop 병합 커밋 `dd29dce5`와 같아 충돌 해소 편집은 0건이다.

### 확인 버튼이 두 축을 순서대로 태운다

`handleOnClickConfirm`이 하던 일이 갈렸다 — 이제 **토핑 저장을 먼저 완전히 끝내고** 기존 배경
저장 흐름(`saveBackground`)을 그대로 태운다. 코루틴 키도 `SAVE_BACKGROUND_KEY`에서 `CONFIRM_KEY`로
바뀌어 확인 버튼 전체가 한 단위가 됐다.

순서에 근거가 있다. 둘을 진짜 병렬로 얽으면 **어느 한쪽만 실패한 경우를 갈라 다뤄야 하고**, 그
설계가 이 라운드 범위 밖이다. 반면 토핑들끼리는 서로 독립적인 요청이라 `async` + `awaitAll`로
동시에 나간다 — 순차로 기다리면 확인 버튼이 바뀐 토핑 수만큼 느려진다.

### 바뀐 것만 보낸다

`confirmedToppings`가 새로 생겼다. **서버에서 막 받아온 그대로의 스냅샷**이고 화면 렌더링에는
쓰지 않는다 — 확인 시점에 `state.toppings`와 대조해 실제로 달라진 토핑만 PATCH 한다.
비교 대상은 넷(`positionX`·`positionY`·`scale`·`rotationDegrees`)이다.

| 보내는 값 | 안 보내는 값 |
|---|---|
| `positionX`·`positionY`·`scale`·`rotation` | `positionZ`(널로 두어 서버가 겹침 순서를 유지한다) |
| | `borderLayers`·`editedImagePath`(비교 대상도 아니다) |

계약이 부분 병합(`null`이면 기존 값 유지)이라 넘기지 않은 축이 그대로 남는다
([api/parfait-image.md](../../../api/parfait-image.md) 위치 PATCH 절).

`ToppingRepository.update`·`UpdateToppingUseCase`가 신설돼 **DataSource의 넷 중 셋째**가 열렸다.
Repository는 여기서도 **에러 변환만** 하고 좌표를 손대지 않는다.

### 이 라운드가 만든 비대칭

- **토핑 저장 실패는 화면에 닿지 않는데, 확인은 그대로 성공한다.** 실패 갈래가
  `viewModelLogger.e` 한 줄이고 그다음 `saveBackground()`가 이어져, 배경이 성공하면
  `ConfirmBackground`가 나가 **화면이 넘어간다.** 사용자는 옮긴 자리가 저장됐다고 믿지만 캔버스
  메인은 재조회로 옛 좌표를 그린다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-275.
  같은 확인 버튼 안에서 **배경 실패는 토스트 + 화면 잔류, 토핑 실패는 무반응 + 화면 이동**으로
  처분이 갈렸다.
- **테두리 재편집만 혼자 남았다.** `borderLayers`·`editedImagePath`는 비교에도 요청에도 없고,
  테두리 PATCH는 여전히 소비처 0건이다. 다른 넷이 저장되기 시작했으므로 사용자 기준으로는
  **"확인했는데 이것만 사라지는" 갈래**가 됐다 → 같은 문서 OQ-P-276.
- **되돌림 가능성이 다시 갈렸다.** 삭제는 모달 확인 시점, 이동·크기·회전은 확인 버튼 시점에
  영구가 된다. "그만두기"로 나가면 이동·크기·회전은 여전히 사라지므로 **시점이 둘**이다
  (OQ-P-270 ②의 전제가 절반 바뀌었다).
- **스냅샷을 성공 후 갱신하지 않는다.** 배경 저장이 실패해 화면에 남은 채 다시 확인을 누르면
  이미 저장된 토핑도 같은 값으로 다시 PATCH 된다. 서버가 부분 병합이라 결과는 같지만 요청은
  중복된다.

### 유닛 테스트

여섯이 붙었다. `ToppingRepositoryImplTest` 둘 — 성공 값을 가공 없이 넘기는지, 403
`PARFAIT_IMAGE_NOT_OWNED`를 `AppError.Server`로 바꾸면서 코드와 상태 코드를 함께 살리는지.
`CanvasBGEditViewModelTest` 넷 — 옮긴 토핑만 요청이 나가고 안 건드린 토핑은 안 나가는지, 아무것도
안 바꾸면 호출이 0건인지, 토핑 저장이 실패해도 배경 확인이 진행되는지(**위 비대칭을 그대로
잠갔다**), 그리고 **크기가 예전 상한 2.5를 넘어 커지는지**.

마지막 하나가 특기할 자리다 — PR #335가 근거 없이 지운 상한(OQ-P-271)이 이번에 **회귀 테스트로
굳었다.** 상한을 되살리려면 이제 그 테스트를 함께 지워야 한다.

저장소 전체 유닛은 745 → **751건**, 테스트 클래스는 **87개**로 그대로다.

## as-built 재정정 (2026-08-26, PR #376 develop 머지)

> **게이트의 근거가 앱의 추측에서 서버의 판정으로 바뀌었다.** 화면 동작·레이어 구성은 한 줄도
> 안 바뀌었고, `isMine`을 **누가 정하는가**만 바뀌었다.
> 브랜치 `feature/#373-sync-backend-api-260825`, 머지 `c55e10bc`.

- **`CanvasBGEditViewModel`이 계정 SSoT 의존을 통째로 버렸다.** 생성자에서
  `GetMyAccountFlowUseCase`가 빠지고, `loadCanvas()` 첫 줄의 `getMyAccountFlowUseCase().first()`
  대기도 사라졌다. `toToppingItem(myMemberId)`는 인자 없는 `toToppingItem()`이 되고 본문은
  `isMine = isMine` — 도메인 모델이 이미 답을 들고 온다.
- **그래서 축이 다른 비교가 사라졌다.** 계정 id와 그룹 멤버십 행 id를 견주던 자리가 없어졌고,
  판정은 서버가 `placedBy.ownerType`으로 끝낸다([api/parfait.md](../../../api/parfait.md)).
  이 화면에서 판정은 표현이 아니라 **게이트**였으므로, 이 변경이 없앤 것은 색이 아니라
  **남의 토핑을 만질 수 있었던 가능성**이다.
- 부수 효과 하나 — 편집 화면 진입이 계정 SSoT가 값을 낼 때까지 기다리지 않는다. 그만큼 조회
  시작이 앞당겨지고, 계정 값이 아직 없을 때 소유 판정이 전부 `false`로 접히던 경로도 함께 없어졌다.

### 유닛 테스트

**신규 0건이다.** `CanvasBGEditViewModelTest`는 `getMyAccountFlow` 스텁과 `MyAccountVO` 조립이
빠지고, 상수 `MY_GROUP_MEMBER_ID`가 **`PLACER_GROUP_MEMBER_ID`**로 개명되며 픽스처가 `isMine`을
직접 받는다(22건 그대로).

⚠️ **그래서 이 탭 몫 다섯 중 "소유 판정"은 더는 판정을 안 덮는다** — 테스트가 넣은 불리언이
그대로 나오는지 볼 뿐이다. 판정의 진짜 자리는 `:data` 매퍼로 옮겨졌고 그쪽을
`ParfaitRemoteDataSourceImplTest` 두 케이스가 덮는다(`ME`는 참, `null`·모르는 값은 거짓).

## as-built 재정정 (2026-08-27 라운드)

> **이 탭이 남겨 두었던 `TODO` 둘이 같은 날 닫혔다** — 토핑 편집 진입(#274)과 테두리 저장이다.
> 머지 `c46bacab`(#369)·`84a89728`(#398)·`a22583a3`(#400), 세 머지 모두 트리가 브랜치 팁과 같다.

- **`TODO(#274 대기)`가 사라졌다.** 서버 토핑의 `imageUrl`이 `https://`라 `ContentResolver`로 열지
  못하던 것을, `ImageSegmentationRepositoryImpl.decodeImage`가 **스킴을 갈라** 원격이면
  `RemoteImageDownloadDataSource`로 바이트를 받아 디코드하는 방식으로 풀었다. 편집 화면은 URL을
  그대로 받아도 된다. 전용 `@DownloadClient`가 따로 있는 이유는 커넥션 풀·`Dispatcher` 격리이고,
  업로드 쪽 `@UploadClient`처럼 기능 전제는 아니다
  → [architecture/data-layer](../../../architecture/data-layer.md).
- **확인 버튼이 테두리도 보낸다.** `updateToppingIfChanged`가 하나의 `changed` 판정을 **둘로 갈라**
  위치·배율·각도(`update`)와 테두리(`updateBorder`)를 독립적으로 비교하고 독립적으로 보낸다 —
  서버 API 자체가 두 엔드포인트로 갈라져 있어서다. 겹 목록을 한 겹으로 접는 규칙은
  `toToppingBorder`의 `lastOrNull()`(가장 바깥 겹)이고, 비면 `ToppingBorder.None`이다.
  ⚠️ **같은 화면이 그릴 때는 `firstOrNull()`을 쓴다** — 겹이 둘 이상이면 보이는 테두리와 저장되는
  테두리가 갈린다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-324.
  ⚠️ **테두리를 구워 넣은 이미지(`editedImagePath`)는 여전히 아무 데로도 안 나간다** — OQ-P-276 ②는
  그대로다.
- **본인 토핑 탭의 목적지가 이 화면이 됐다.** `NavKeyCanvasBGEdit`에 `initialToppingId`가 붙고,
  `withCanvas`가 첫 조회 결과에 `selectedTab = TOPPING`·`selectedToppingId = initialToppingId`를
  얹는다. C-001에서 본인 토핑을 탭하면 이 탭이 그 토핑을 선택한 채로 열린다
  → [c202 스펙](2026-08-20-c202-canvas-spotlight.md) as-built 재정정.
  ⚠️ 시딩이 `withCanvas` 안에 있어 **조회가 한 번뿐인 지금은 문제가 없지만**, 선작성된 폴링 계획이
  이 자리를 구독으로 바꾸면 매 방출마다 탭과 선택이 초기값으로 되돌아간다 → OQ-P-326 ②.
- **배율 하한이 0.5에서 0.05가 됐다**(#398) — 위 드리프트 4 참고.

### 유닛 테스트

`CanvasBGEditViewModelTest`에 둘이 붙었다 —
`init_withInitialToppingId_opensToppingTabWithThatToppingSelected`(진입 시 탭·선택 시딩)와
`onClickConfirm_toppingBorderEdited_savesOnlyTheBorder`(테두리만 고치면 테두리 PATCH 하나만 나간다).
`:data` 쪽은 `ToppingRepositoryImplTest`에 `updateBorder`의 성공·실패 두 케이스가 붙었다.
⚠️ **`toToppingBorder`의 겹 접기 자체를 겹 둘 이상으로 덮은 테스트는 없다** — 붙은 케이스가
한 겹짜리 목록만 쓴다(OQ-P-324의 감지선이 없다).
