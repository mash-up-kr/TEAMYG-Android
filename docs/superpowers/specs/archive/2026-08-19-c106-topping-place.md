---
id: c106-topping-place
title: C-106 토핑 배치 화면 — 정중앙·40%·48dp 초기 배치 + 드래그·리사이즈·회전 (Topping Place)
status: implemented
category: ui-spec
platforms: android
verified: 2026-09-01
related_code:
  - NavKeyCanvasToppingPlace.kt#NavKeyCanvasToppingPlace
  - CanvasToppingPlaceRoute.kt#CanvasToppingPlaceRoute
  - CanvasToppingPlaceScreen.kt#CanvasToppingPlaceScreen
  - CanvasToppingPlaceScreen.kt#ToppingPlaceCornerButtons
  - CanvasToppingPlaceViewModel.kt#CanvasToppingPlaceUiState
  - CanvasToppingPlaceViewModel.kt#applyInitialPlacementIfNeeded
  - CanvasToppingPlaceViewModel.kt#minScaleForTouchTarget
  - CanvasToppingPlaceViewModel.kt#maxScaleToOverflowCanvas
  - CanvasToppingPlaceViewModel.kt#CanvasToppingPlaceEffect.ToppingPlaced
  - CanvasToppingPlaceViewModelTest
  - ToppingHandleComponents.kt#rememberToppingBaseSize
  - ToppingHandleComponents.kt#ToppingSelectionStroke
  - ToppingHandleComponents.kt#ToppingDragHandleButton
  - ToppingGeometry.kt#resizeScaleFactor
  - ToppingGeometry.kt#rotationDeltaDegrees
  - ToppingGeometryTest
  - CanvasToppingLayer.kt#TOPPING_BASE_LONG_SIDE_RATIO
  - SegmentationResult.kt#trimmedSubjectImagePath
  - ImageSegmentationRepositoryImpl.kt#segmentImage
  - ToppingEditMask.kt#trimTransparentBounds
  - ToppingEditViewModel.kt#completeEdit
  - NavKeySegmentationConfirm.kt#trimmedSubjectImagePath
  - SegmentationConfirmRoute.kt#SegmentationConfirmRoute
related_adr: ADR-0005, ADR-0006, ADR-0011, ADR-0012
related_spec: c103-segmentation-topping-edit, c301-topping-edit-tab, c001-canvas-today-detail, ygscaffold-v2-common-loading-error, segmentation-pipeline-hardening
related_architecture: navigation-flow, design-system, data-layer, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, canvas, topping, c-106, ui]
---

# Spec: C-106 토핑 배치 화면

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> **사후 기록(post-hoc)** — 선작성 스펙 없이 develop 머지(PR #290, 브랜치 `feature/topping-add-screen`,
> 2026-08-19). as-built 역기록이고 **코드가 SoT**다.

## 목표

토핑 생성 경로의 마지막 화면을 채운다. 다듬기(C-103~C-105)를 마친 누끼 하나를 캔버스 위에 놓고
위치·크기·각도를 정하는 화면이며, 위키 [[C-106-토핑-배치-정책-v0.1]]이 확정한 초기 배치 규칙
네 가지가 **처음으로 코드에 들어온 자리**다(OQ-P-200이 "코드 어디에도 없다"고 적던 것).

## 범위

- 포함: 목적지 `NavKeyCanvasToppingPlace(imageUri)` 신설 + Route/Screen/ViewModel 한 벌,
  C-106 초기 배치 계산, 드래그 이동·핸들 리사이즈·핸들 회전, 리사이즈 상·하한,
  `CanvasBGEditScreen`이 쥐고 있던 토핑 표시 컴포넌트 3종 추출, 세그멘테이션 결과에
  **여백을 걷어낸 두 번째 이미지 경로** 추가.
- 제외: **배치 확정의 서버 저장**(확인 버튼은 이펙트만 쏜다) · 실제 캔버스 배경·기존 토핑 표시
  (배경은 흰색 고정) · 삭제·테두리 재편집(그건 C-301 편집 탭 몫) · 실기기 확인.

## 화면이 하는 일

`CanvasBGEditScreen`의 토핑 탭과 UI가 닮았지만 더 단순하다 — 다룰 대상이 **하나뿐**이라 탭해서
고를 필요가 없고 처음부터 드래그가 붙으며, 하단 바 가운데는 탭 전환이 아니라 고정 문구
(`YGFloatingBarEdit`)다. 모서리 버튼도 넷이 아니라 **둘**이다 — ~~우측 상단 크기조절 · 우측 하단 회전~~
→ **as-built(#412 develop 머지, 2026-09-01)**: 둘이 자리를 맞바꿔 **우측 상단 회전 · 우측 하단
크기조절**이다. C-301 편집 탭도 같은 라운드에 같은 방향으로 바뀌어 두 화면의 핸들 배치는 여전히
같다. 환산은 핸들 좌표와 중심만 쓰므로(`ToppingGeometry`) 계산식은 그대로다.

## 초기 배치 (C-106 정책 대조)

| 정책 항목 | 코드 | 일치 |
|---|---|---|
| 초기 크기 = 긴 변이 캔버스 **가로 너비의 40%** | `TOPPING_BASE_LONG_SIDE_RATIO`를 `internal`로 열어 `applyInitialPlacementIfNeeded`가 캔버스 실측 너비에 곱한다 | ✅ |
| 초기 위치 = 캔버스 **정중앙** | `offsetX`·`offsetY`를 `(canvas - base) / 2`로 놓아 중심이 캔버스 중심에 온다 | ✅ |
| **최소 터치 방어** — 짧은 변이 48 미만이면 짧은 변 기준으로 전체 상향 | `MIN_TOPPING_SHORT_SIDE`(48dp)로 역산한 배율과 40% 배율 중 **큰 쪽**을 쓴다 | ✅ (단위는 dp) |
| **이탈 허용 + Clipping Mask** | 좌표를 clamp하지 않고, 토핑·모서리 버튼은 `clipToBounds()` 안에서 잘린다 | ✅ |

- 정책의 `48px`을 코드는 **dp**로 읽는다. OQ-P-200 ②(단위 확정)가 그렇게 굳었고, 정책 문서 쪽은
  아직 px 그대로다.
- 계산은 **캔버스 실측**과 **토핑 원본 크기** 둘 다 있어야 성립하고 그 둘이 서로 다른 시점에
  비동기로 도착하므로, 어느 쪽이 먼저 오든 매번 다시 시도한다(`OnCanvasMeasured`·
  `OnToppingBaseSizeMeasured` 두 인텐트). **사용자가 한 번이라도 손대면**
  (`hasUserAdjustedPlacement`) 자동 배치를 멈춘다 — 안 그러면 늦게 도착한 실측이 사용자의 조작을
  덮는다.
- 40% 상수는 **읽기(서버 배치를 그리는 `CanvasToppingLayer`)와 쓰기(신규 배치)가 같은 값을 공유**한다.
  OQ-P-200 ③("새 배치에만 적용할지, 받은 토핑에도 적용할지")은 이로써 **양쪽 다**로 답이 났다.

## 조작

- **이동**: 토핑 자신에 `Modifier.dragBy`. 드래그한 만큼 그대로 옮긴다(경계 clamp 없음).
- **리사이즈**: 핸들이 **우측 상단 모서리**에 있으므로(#412로 우측 하단으로 옮겨 갔다),
  그 모서리의 바깥 방향으로 끌면 커지고
  안쪽이면 작아진다. `resizeOutwardDirection(rotation)`이 회전을 반영한 단위 벡터를 주고 드래그
  변위를 거기에 투영해 증감을 낸다 — 거꾸로 선 토핑도 "바깥으로 끌면 커진다"가 성립한다.
- **회전**: 가로 드래그 거리에 비례해 각도를 누적한다. 상·하한 없음.

> 🔁 **위 리사이즈·회전 서술은 2026-08-27(PR #397) 이전의 기록이다.** 환산이 ViewModel에서 화면으로
> 옮겨 가면서 인텐트가 `OnToppingResize(scaleFactor)`·`OnToppingRotate(deltaDegrees)`가 됐다.
> 리사이즈는 바깥 방향 단위벡터에 투영하는 대신 **핸들이 중심에서 멀어진 비율**을 배율에 곱하고,
> 회전은 가로 성분 대신 드래그를 **핸들 벡터의 접선에 투영**한다
> (`ToppingGeometry#resizeScaleFactor`·`#rotationDeltaDegrees`). `resizeOutwardDirection`과 감도 상수
> 둘은 그 라운드에 사라졌다. 상·하한 규칙 자체는 이 화면에서 바뀌지 않았다 — 48dp 역산 하한도
> 오버플로우 상한도 그대로이고, 달라진 것은 클램프에 들어가기 전 값을 만드는 방법이다.
- **상·하한(정책에 없는 것, 코드가 정했다)**:
  - 하한은 고정 배율이 아니라 **48dp 최소 터치 영역에서 역산**한다. 고정값(0.5)으로 두면 초기 배율이
    그보다 작은 큰 원본 사진은 리사이즈를 한 번 건드리는 순간 처음 크기로 되돌아갈 수 없다.
  - 상한은 **캔버스 긴 변의 1.5배**를 원본 크기로 역산한다. 고정 배율만 두면 원본이 큰 사진은
    "아무리 키워도 캔버스를 못 벗어나는" 것처럼 보인다.
  - 캔버스·토핑 실측 전에는 임시 고정값(`TOPPING_MIN_SCALE_FALLBACK`·`TOPPING_MAX_SCALE_FALLBACK`)을 쓴다.

## 그리기 — 같은 배율을 세 번 표현하지 않는다

이미지·스트로크·핸들이 **같은 자리·같은 크기**로 보여야 하는데 표현 수단이 서로 다르다
(`graphicsLayer(scale)` vs `requiredSize(sizeAfterScale)`). 화면이 `center`와 `sizeAfterScale`을
**한 번만 계산해 셋에 그대로 넘긴다** — 서로 다른 방식이 같은 값을 내는지는 Compose 내부 구현에
기대는 셈이라 어긋나기 쉽다. 같은 이유로 `Image`는 자기 크기를 `intrinsicSize`로 다시 정하지 못하게
바깥 `Box`가 크기를 고정하고 자신은 채우기만 한다.

> ⚠️ **as-built 갱신(2026-09-08, PR #465 develop 머지)** — **같은 값을 넘기는 것만으로는 부족했다.**
> 이미지와 스트로크는 좌상단을 직접 계산해 `offset` 으로 넘기고 모서리 버튼만 `centeredAt` 으로
> 놓았는데, 토핑을 캔버스보다 크게 키우면 **셋이 같은 `center`·`sizeAfterScale` 을 보는데도 버튼만
> 바깥으로 갔다.** `requiredSize` 가 부모 제약을 무시하는 것은 자기 자식을 잴 때뿐이고, 그 노드가
> 부모에게 보고하는 겉크기는 잘린다. Compose 는 그 잘린 겉크기 안에 실제 내용을 가운데 정렬하므로
> (`Placeable.apparentToRealOffset`) 좌상단을 직접 계산한 쪽만 **넘친 양의 절반**만큼 밀렸다.
> 셋 다 `Modifier.centeredAt` 으로 놓는 방식을 통일했다. 계약은 `core:util:android` 계측 테스트
> `ModifierCenteredAtTest` 가 두 방식을 나란히 재서 고정한다(밀리는 쪽도 함께 남겼다).
> 스트로크는 C-301 배경 편집 화면도 공유하므로 거기서도 함께 고쳐졌다.

세 겹은 클리핑 규칙이 갈린다:

| 겹 | 클리핑 |
|---|---|
| 토핑 이미지 | 캔버스 경계에서 자른다 |
| 선택 스트로크 | **자르지 않는다** — 캔버스를 넘어가도 진짜 크기가 보여야 한다 |
| 모서리 버튼 | 좌표는 캔버스 밖 진짜 모서리를 쓰되(clamp 없음) 픽셀은 잘린다 |

배경 전체에 `Black25` 딤을 깔아 배치 중인 토핑만 도드라지게 한다.

> 🔁 **as-built 갱신(2026-08-25, PR #357)** — 딤이 덮는 것이 배경만이 아니게 됐다. 겹은
> **배경(색 또는 이미지) → 기존 토핑 → `Black25` 딤 → 배치 중인 토핑** 순이라, 이미 올라간 남의
> 토핑도 딤 아래로 들어가고 지금 옮기는 것만 그 위에 뜬다.

## 컴포넌트 추출

`CanvasBGEditScreen`의 private 컴포저블 3종이 `component/ToppingHandleComponents.kt`로 올라가
두 화면이 공유한다 — `rememberToppingBaseSize`(painter `intrinsicSize` → dp, 로딩 중 폴백 60dp) ·
`ToppingSelectionStroke`(회전 따라가는 흰 점선) · `ToppingDragHandleButton`. 마지막 것은 인자가
`toppingId: Long` → **`key: Any?`**로 일반화됐다(대상이 하나뿐인 화면은 `Unit`을 넘긴다).
`ToppingSelectionStroke`가 `size` → `requiredSize`로 바뀌면서 **C-301 편집 탭의 토핑도 같이 바뀐다**
(`CanvasToppingImage`가 `requiredSize`로 이관됐다) — 부모 제약을 넘겨 그리게 된 것이고, 이 라운드가
기존 화면에 남긴 유일한 동작 변경이다.

## 세그멘테이션 결과가 둘이 됐다

배치 화면은 **투명 여백이 없는 실제 객체 크기**가 필요하다(여백이 붙어 있으면 40%·48dp 계산이
여백까지 포함해 어긋난다). 그런데 수동 편집(C-104/C-105)은 원본과 픽셀 단위로 겹쳐 그려야 해서
**원본 크기를 유지해야** 한다. 그래서 두 벌을 따로 들고 다닌다.

- `SegmentationResult`에 `trimmedSubjectImagePath` 추가. `:data`가 이미 알고 있는 bounding box로
  바로 잘라 두 번째 PNG를 캐시에 떨군다(bounding box가 `null`이면 원본 경로를 그대로 쓴다).
- `NavKeySegmentationConfirm`도 두 경로를 다 싣고, 확인 화면은 **트리밍본**으로 초기화한다.
- 편집을 거친 경우는 `ToppingEditMask.trimTransparentBounds()`가 알파 있는 픽셀의 최소 사각형을
  구해 자른다(전 픽셀 스캔, 자를 기준이 없으면 원본 반환). 재편집 좌표계를 지켜야 하는 `cutout`은
  건드리지 않고 **결과물만** 자른다.
- 대가: `ToppingEditViewModel`의 "테두리가 없으면 같은 파일을 두 번 떨구지 않는다" 최적화가
  사라져 편집 경로도 **항상 파일 2장**이다. 세그멘테이션 경로도 흐름당 캐시 PNG가 1장 → 2장이고,
  `segmentImage` 안에 동시에 사는 전체 해상도 버퍼도 하나 늘었다(OQ-P-228·OQ-P-003 ③).

## 드리프트

1. ⚠️ **배치 확정이 아무 데도 저장되지 않는다.** 확인 버튼은 `ToppingPlaced(imageUri, offset, scale,
   rotation)` 이펙트만 쏘고 Route는 `// TODO` 뒤에 캔버스로 이동한다. 서버 토핑 수정 표면은 테두리만
   받아 좌표·배율·회전 계약 자체가 없다(OQ-P-199 ②·OQ-P-209).
2. ~~⚠️ **`NavKeyCanvasMain(groupId = 0L)` 하드코딩 + `goTo`.**~~ → ✅ **해소(2026-08-20, PR #309)** —
   배치 완료 이펙트가 `navigator.popUpTo<NavKeyCanvasMain>()`이다. 이미 백스택에 있는 엔트리로
   되감으므로 그룹 id를 알 필요가 없어져 하드코딩도 사라졌고, 흐름 화면이 캔버스 밑에 쌓이지도
   않는다. 세그멘테이션 라운드가 자기 캐시 정리의 안전 근거를 세우려고 함께 고쳤다 → OQ-P-238 ①.
3. ⚠️ **`NavKeyCanvasMove` 계열이 도달 불가로 남았다** — 유일한 호출자였던
   `SegmentationConfirmRoute.onClickNext`가 새 목적지로 갈아탔는데 목적지·Route·Screen·엔트리는
   그대로다 → OQ-P-239.
4. ~~**배치 화면이 보여 주는 캔버스가 실제 캔버스가 아니다**~~ — ✅ **대부분 해소(2026-08-25, PR #357)**.
   `CanvasToppingPlaceViewModel`이 초안의 `groupId`로 `GetTodayParfaitUseCase`를 **다시 불러**
   배경(색·이미지 URL)과 기존 토핑을 상태에 싣고, 화면이 `CanvasToppingLayer`로 그것을 그린다.
   `groupId` 하나당 한 번만 조회하고, 실패는 로그만 남긴 채 기본 배경·빈 목록으로 배치를 계속하게
   둔다. 배경 이미지는 C-001과 같은 `ContentScale.Crop`이고 기존 토핑은 `positionZ` 오름차순이라
   두 화면의 그림이 갈리지 않는다 → OQ-P-240 해소. **남은 것 셋**: 캔버스 상자가 여전히 `YGCanvas`가
   아닌 화면 자작 `Box`라 **좌상단 컷 도형이 없고**(OQ-P-174), 겹침 z 규칙에 정책 근거가 없으며,
   이 라운드가 연 조회·매핑 경로에 **테스트가 0건**이다(OQ-P-303).
5. **회전과 리사이즈 한계는 정책에 근거가 없다** — 위키 C-106은 초기 배치·이탈만 규정하고 회전을
   아예 다루지 않는다. 드래그 감도 상수 둘, 오버플로 상한, 회전 무제한이 전부 코드 판단이다
   → OQ-P-241.
6. **규약 이탈이 그대로 복사됐다** — 상단 `60.dp`·하단 `14.dp` 패딩이 토큰 밖 리터럴이고 주석이
   "공통에 없음"이라고 자인한다(OQ-P-203 ③과 같은 자리, 같은 값).
7. **드래그 핸들 접근성 대체 수단 없음**이 두 번째 화면으로 번졌다 — `YGCircleButton(onClick = {})`에
   `dragBy`를 얹는 방식이라 스크린리더에는 눌리는 버튼인데 눌러도 아무 일이 없다(OQ-P-202 ③).
8. **로딩 폴백 60dp** — 이미지 크기를 알기 전에 사용자가 드래그하면 `hasUserAdjustedPlacement`가
   서고, 실측이 도착해도 초기 배치를 다시 계산하지 않는다.

## 테스트

유닛 474 → **484건**. `ToppingGeometryTest` 4건(회전 0·90·180·270에서 바깥 방향 벡터),
`CanvasToppingPlaceViewModelTest` 6건(리사이즈 공식·네 경계 회전에서의 증감·상한·하한·회전 누적).
⚠️ 실기기 확인 없음.
