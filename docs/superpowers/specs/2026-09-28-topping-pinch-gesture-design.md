---
id: topping-pinch-gesture
title: 토핑 두 손가락 변환 제스처 (Topping pinch/rotate/pan gesture)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-28
related_code:
  - feature/groups/canvas/impl/.../component/ToppingHitTestInput.kt#toppingTransformInput
  - feature/groups/canvas/impl/.../component/ToppingHitTestInput.kt#toppingTapInput
  - feature/groups/canvas/impl/.../util/ToppingHitTarget.kt#ToppingHitTarget
  - feature/groups/canvas/impl/.../screen/CanvasBGEditScreen.kt#ToppingCornerButtons
  - feature/groups/canvas/impl/.../viewmodel/CanvasToppingPlaceViewModel.kt#OnToppingTransform
  - feature/groups/canvas/impl/.../viewmodel/CanvasBGEditViewModel.kt#OnToppingTransform
related_adr:
related_spec: c106-topping-place, c301-topping-edit-tab
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait]
---

# Spec: 토핑 두 손가락 변환 제스처

## 목표

토핑 배치 화면(C-106)과 편집 화면(C-301 토핑 탭, C-305)의 회전 핸들과 크기 핸들 버튼을 없앤다.
대신 인스타그램 스토리 스티커처럼 두 손가락 제스처 하나로 이동·회전·확대를 동시에 처리한다.

## 범위

- 포함
  - 두 손가락 핀치로 이동·회전·확대를 동시에 처리. 한 손가락으로 시작한 드래그는 옮기지 않는다.
  - 두 화면의 회전·크기 핸들 제거.
  - 두 화면의 이동·크기·회전 intent를 `OnToppingTransform` 하나로 통합.
  - 제스처 modifier의 Compose UI 테스트.
- 제외
  - 스냅, 정렬 가이드라인, 햅틱.
  - 회전·위치 범위 제한.
  - 접근성 대체 입력(OQ-P-202). 핸들 드래그도 대체 입력이 아니었다.
  - 저장 경로(`toToppingTransform`, 편집 화면의 배치 PATCH) 변경.
  - `wiki/` 반영.

## 확정한 제스처 규칙

| 규칙 | 결정 |
|---|---|
| 두 손가락 시작 위치 | 캔버스 어디서든. 토핑 위일 필요 없음 |
| 두 손가락 최소 폭 | 64dp. 이보다 붙어 있으면 이동·확대·회전 모두 멈춘다 |
| 대상 | 배치 화면은 유일한 토핑, 편집 화면은 선택된 토핑 |
| 편집 화면에서 선택 없음 | 제스처 무시. 자동 선택하지 않음 |
| 한 손가락으로 시작한 드래그 | 옮기지 않음. 소비하지 않으므로 떼면 `toppingTapInput`이 탭·미스로 판정 |
| 회전·확대 기준점 | 토핑 중심. 손가락 중점의 이동량만 pan으로 반영 |
| 손가락 수 변화 | 한 손가락을 댄 채 두 번째 손가락이 닿으면 핀치가 시작된다. 핀치 중 한 손가락을 떼면 남은 손가락으로 이동을 계속 |
| 핀치와 탭 | 두 번째 포인터가 down된 제스처는 탭으로 판정하지 않음 |
| 변화 없는 프레임 | `onTransform`을 부르지 않음. 두 손가락을 대기만 해서는 dirty·`hasUserAdjustedPlacement`가 바뀌지 않음 |
| 편집 화면 삭제·편집 버튼 | 오버레이 제스처가 진행되는 동안(첫 down부터 모든 손가락 up까지) 숨김 |

"잡힘" 상태는 한 제스처 안에서 한 번 들어가면 모든 손가락이 떨어질 때까지 유지된다. 그래서 핀치 중에
토핑 밖 손가락만 남아도 이동이 계속된다.

## API / 인터페이스

```kotlin
// component/ToppingHitTestInput.kt — toppingDragInput 을 대체
@Composable
internal fun Modifier.toppingTransformInput(
    enabled: () -> Boolean,
    onTransform: (pan: Offset, zoom: Float, rotationDelta: Float) -> Unit,
    onGestureActiveChange: (Boolean) -> Unit = {},
): Modifier
```

- `enabled`: 제스처 첫 down 시점에 읽는다. `false`면 이번 제스처를 버린다.
- `onTransform`: 잡힘 상태의 프레임마다 한 번 호출된다.
  - `pan`: 누른 손가락들의 중점 이동량(px).
  - `zoom`: 손가락 사이 거리 비율. 손가락이 하나면 `1f`.
  - `rotationDelta`: 도 단위, 화면 좌표 기준 시계 방향이 양수. 손가락이 하나면 `0f`.
- `onGestureActiveChange`: 첫 down에서 `enabled()`가 `true`면 `true`, 모든 손가락이 떨어지거나 취소되면 `false`.
- 핸들러는 `toppingTapInput`처럼 `remember`로 고정하고 콜백은 `rememberUpdatedState`로 읽는다.

```kotlin
// CanvasToppingPlaceIntent
data class OnToppingTransform(val pan: DpOffset, val zoom: Float, val rotationDelta: Float)

// CanvasBGEditIntent — pan 은 캔버스 대비 비율
data class OnToppingTransform(val panX: Float, val panY: Float, val zoom: Float, val rotationDelta: Float)
```

두 화면의 `OnToppingMoveDrag`, `OnToppingResize`, `OnToppingRotate`는 삭제한다. Route와 Screen의
`onToppingMoveDrag`, `onToppingResize`, `onToppingRotate` 콜백은 `onToppingTransform` 하나로 바꾼다.

## 동작 / 상태

### 제스처 루프 (`awaitEachGesture`)

1. `awaitFirstDown(requireUnconsumed = false)`로 첫 down을 받는다. `enabled()`가 `false`면 종료한다.
   아니면 `onGestureActiveChange(true)`.
2. 한 손가락만 눌린 동안은 잡힘 상태가 아니다. 이동을 보내지도 소비하지도 않는다.
3. 두 번째 포인터가 down되면 잡힘 상태로 들어간다.
4. 잡힘 상태에서는 매 이벤트마다 `pressed && previousPressed`인 포인터 중 id 순 앞의 두 개로 중점 이동량,
   거리 비율, 각도 변화를 직접 계산한다. 세 번째 이후 포인터는 뺀다. 결과가 항등 변환이 아니면
   `onTransform`을 호출하고 change를 소비한다.
5. 새로 down된 포인터와 막 뗀 포인터는 그 이벤트의 계산에서 빠지므로 포인터 수가 바뀌어도 중점이 튀지
   않는다. 이 성질은 UI 테스트로 고정한다.
   - 두 손가락 거리가 `TOPPING_PINCH_MIN_SPAN`(64dp, 약 1cm) 미만이면 이동·확대·회전 모두 하지 않는다.
     좁은 폭에서는 손가락이 조금만 굴러도 거리비가 크게 흔들리고, 터치 패널이 두 손가락을 하나로 합쳤다
     나누며 좌표를 튀게 만든다.
   - 거리 비율과 각도는 기준 거리·각도 대비로 재고, 기준은 최소 폭 아래에서 갱신하지 않는다. 오므렸다
     벌려도 배율이 누적 오차 없이 맞는다.
   - 한 이벤트에 추적 중인 포인터가 `TOPPING_POINTER_MAX_JUMP`(48dp)보다 멀리 움직이면 그 이벤트는 버린다.
     두 손가락이 붙으면 터치 패널이 추적을 놓쳐 가짜 up과 좌표 점프를 만든다.
   - 두 손가락이 최소 폭보다 붙은 채 한쪽이 떨어지면 남은 손가락으로 옮기지 않는다. 패널이 두 접점을
     합친 것일 수 있고, 그때 남은 좌표의 점프는 점프 한계보다 작다.
   - 최소 폭 아래에 있다가 다시 벌어진 첫 프레임은 회전을 버린다. 붙어 있는 동안 손가락이 굴러간 각도가
     한꺼번에 들어오지 않게 한다.
6. 눌린 포인터가 하나도 없으면 종료하고 `onGestureActiveChange(false)`.

잡힘 상태가 아닌 한 손가락 제스처는 소비하지 않는다. 같은 노드의 `toppingTapInput`이
탭·미스를 판정한다.

### `toppingTapInput` 변경

`waitForUpOrCancellation` 대신 직접 루프로 기다린다. 기존 종료 조건(소비됨, 영역 밖으로 나감)은 그대로 두고, 두 번째 포인터가 down되면 이번 제스처는 탭이 아니므로 콜백을 부르지 않는다.
핀치가 change를 소비하는 경우는 소비 조건으로 이미 끝난다. 추가 조건은 두 손가락을 대고 움직이지 않은 채
떼는 경우를 막는다.

### 배치 화면 ViewModel

`OnToppingTransform` 핸들러는 `updateState` 한 번으로 아래를 갱신한다.

- `offsetX/offsetY += pan`
- `scale = (scale * zoom).coerceAtLeast(minScale(canvasSize, baseSize))` — 서버 scale `TOPPING_MIN_SCALE`(0.05)을
  이 화면 배율로 환산한 하한만 있고 상한은 없다. 편집 화면과 같은 규칙이다. 캔버스·토핑을 둘 다 실측하기
  전에는 변환을 무시한다
- `rotationDegrees += rotationDelta`
- `hasUserAdjustedPlacement = true`

화면은 중심을 `offset + baseSize / 2`로 계산해서 scale과 무관하다. 그래서 확대 기준점이 이미 토핑 중심이고,
좌표 모델은 바꾸지 않는다.

### 편집 화면 ViewModel

`selectedToppingId`가 없으면 무시한다. 있으면 `applyToppingTransform` 한 번으로 아래를 갱신하고
`markDirty`도 한 번만 한다.

- `positionX/positionY += panX/panY`
- `scale = (scale * zoom).coerceAtLeast(TOPPING_MIN_SCALE)`
- `rotationDegrees += rotationDelta`

### 클램프 경계

배율이 경계에 닿은 뒤에도 zoom은 매 프레임 현재 값에 곱해지고 결과가 다시 클램프된다. 누적된 초과분이
없으므로 경계에서 반대 방향으로 핀치하면 바로 반응한다.

## 표시·제어 규칙

- 배치 화면
  - 토핑 박스의 `dragBy`를 없앤다.
  - 캔버스 전체 크기 오버레이에 `toppingTransformInput`을 붙인다.
  - `enabled`는 이미지가 로드됐을 때만 `true`. 로드 전 크기는 폴백이라, 이때 제스처를 받으면 초기 배치가 꺼진다.
  - `ToppingPlaceCornerButtons`는 통째로 삭제한다. `ToppingSelectionStroke`는 남겨서 조작 대상 토핑을 표시한다.
- 편집 화면
  - 오버레이의 `toppingDragInput`을 `toppingTransformInput`으로 바꾼다. `enabled`는 `selectedEntry != null`.
  - `ToppingCornerButtons`에서 TR 회전 핸들과 BR 크기 핸들을 뺀다.
  - TL 삭제, BL 편집 버튼과 스트로크는 그대로 둔다. KDoc을 맞춘다.
  - 삭제·편집 버튼은 오버레이보다 위에 있는 형제 노드라, 두 번째 손가락이 그 위에 닿으면 포인터를 버튼이
    가져간다. 그러면 핀치가 끊기고, 손을 떼는 순간 삭제 다이얼로그나 편집 화면이 열린다. 그래서 화면이
    `onGestureActiveChange`로 제스처 진행 여부를 들고 있다가, 진행 중에는 두 버튼을 컴포지션에서 뺀다.
    두 번째 손가락이 닿은 뒤 숨기면 늦다. 포인터는 down 시점의 히트 테스트로 대상이 정해지기 때문에
    첫 down부터 숨긴다. 캔버스를 탭하면 버튼이 잠깐 사라졌다 돌아오는 것은 허용한다.

## 파일 구성

| 파일 | 변경 |
|---|---|
| `component/ToppingHitTestInput.kt` | `toppingTransformInput` 추가, `toppingDragInput` 삭제, `toppingTapInput`에 다중 포인터 취소 추가 |
| `component/ToppingHandleComponents.kt` | `ToppingRotateHandleButton` `ToppingResizeHandleButton` `ToppingDragHandleButton` `handleVectorPx` 삭제. `ToppingSelectionStroke` 유지 |
| `util/ToppingGeometry.kt` | `resizeScaleFactor` `rotationDeltaDegrees` 삭제. `computeToppingButtonPoints`는 편집 화면이 쓰므로 유지 |
| `screen/CanvasToppingPlaceScreen.kt` | 오버레이 추가, `dragBy`와 `ToppingPlaceCornerButtons` 삭제 |
| `screen/CanvasBGEditScreen.kt` | 제스처 교체, TR·BR 핸들 제거, 제스처 중 삭제·편집 버튼 숨김 |
| `viewmodel/CanvasToppingPlaceViewModel.kt` `viewmodel/CanvasBGEditViewModel.kt` | intent 통합 |
| `route/CanvasToppingPlaceRoute.kt` `route/CanvasBGEditRoute.kt` | 콜백 교체 |
| `res/values/strings.xml` | `canvas_bg_edit_topping_rotate` `canvas_bg_edit_topping_resize` 삭제 |
| `core/util/android/.../Modifier.kt` | `dragBy` 삭제. 이 작업 뒤 호출하는 곳이 없다 |
| `feature/groups/canvas/impl/build.gradle.kts` | `parfait.test.android` `parfait.test.compose` 플러그인 추가 |

`ic_rotate` `ic_scale` 드로어블은 디자인시스템 프리뷰가 쓰므로 남긴다.

## 테스트

- JVM 단위 테스트
  - `CanvasToppingPlaceViewModelTest`, `CanvasBGEditViewModelTest`: 기존 Resize·Rotate·MoveDrag 케이스를 `OnToppingTransform` 기준으로 옮긴다.
  - 새 케이스: 한 번의 transform에서 pan, zoom, rotation이 동시에 반영된다.
  - 새 케이스: 클램프 경계에서 반대 방향 zoom이 바로 반응한다.
  - 새 케이스(편집 화면): 선택 없음이면 무시한다. transform 한 번에 dirty 마크가 한 번이다.
  - `ToppingGeometryTest`: 삭제한 함수의 케이스를 지운다.
- Compose UI 테스트 (`androidTest`, `createComposeRule`, `performTouchInput` 멀티터치)
  - 토핑 위에서 시작한 한 손가락 드래그도 `onTransform`을 부르지 않는다.
  - 캔버스 어디서든 두 손가락 핀치·회전이 zoom과 rotation을 전달한다.
  - 손가락 수가 바뀌는 순간 pan이 튀지 않는다.
  - 두 손가락을 대고 뗀 제스처는 탭 콜백을 부르지 않는다.
  - `enabled`가 `false`면 아무것도 전달하지 않는다.
  - 두 손가락을 대고 움직이지 않으면 `onTransform`을 부르지 않는다.
  - `onGestureActiveChange`는 첫 down에 `true`, 마지막 up에 `false`를 한 번씩 전달한다.

## 주의 / 열린 질문

- 두 손가락 제스처는 캔버스 어디서든 시작되므로 편집 화면에서 다른 사람 토핑 위에서 시작한 핀치도 선택된
  내 토핑을 변환한다. 의도한 동작이다.
- 오버레이는 캔버스 영역 안에만 있다. 캔버스 밖 여백에서 시작한 손가락은 제스처에 들어가지 않는다.
- 편집 화면에서 두 번째 손가락이 첫 손가락과 거의 같은 프레임에 삭제·편집 버튼 위에 닿으면, 버튼을
  숨기는 재구성보다 먼저 히트 테스트가 끝나 버튼이 그 포인터를 가져간다. 버튼이 작아 드물다.
- 메인 스레드가 밀려 여러 이벤트가 하나로 묶여 오면 정상 이동도 점프 한계를 넘어 버려질 수 있다.
- `docs/status.md` C-106·C-301 절의 조작 방식 서술을 구현 후 갱신한다.
- `docs/synthesis/open-questions.md` OQ-P-202는 핸들 대신 핀치 기준으로 문구만 고친다. 상태는 미결 그대로.
