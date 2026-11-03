---
id: camera-zoom-button
title: 커스텀 카메라 줌 버튼 (Camera zoom toggle)
status: implemented
category: ui-spec
platforms: android
verified: 2026-11-03
related_code:
  - YGCameraZoomButton.kt#YGCameraZoomButton
  - CameraZoomLevel.kt#CameraZoomLevel
  - CustomCameraViewModel.kt#CustomCameraViewModel
  - CustomCameraScreen.kt#CustomCameraScreen
related_adr:
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, camera]
---

# Spec: 커스텀 카메라 줌 버튼

## 목표

커스텀 카메라(C-101) 뷰파인더 하단 중앙의 줌 버튼을 누를 때마다 배율을 순환한다.
근거: Figma `BL_06 카메라 줌인/줌아웃 디자인`(node `6724:13405`) — "버튼 디폴트 값은 1x,
Camera-Zoom 버튼 클릭 시 1x > 2x > 0.5x 순서로 반복".

## 범위

- 포함: `core:designsystem`의 `YGCameraZoomButton`, 배율 순환 상태, 렌즈 전환 시 초기화, 미사용 줌 컴포넌트 삭제(OQ-P-052 해소)
- 제외: 핀치 줌(2026-11-03 시도 후 Figma 사양과 달라 폐기), 연속 배율 표시

## 순환 규칙 (사용자 결정)

Figma 순서 `1x → 2x → 0.5x`에서 현재 렌즈 `zoomRange`(CameraX `ZoomState` min..max)에 들어가는 단계만 남긴다.

| 지원 | 순환 |
|---|---|
| 2x·0.5x 모두 | 1x → 2x → 0.5x → 1x |
| 0.5x 미지원 | 1x → 2x → 1x |
| 2x 미지원 | 1x → 0.5x → 1x |
| 둘 다 미지원 | 버튼 숨김 |

- 1x는 CameraX에서 항상 범위 안이라 별도 처리하지 않는다.
- 전면 카메라도 같은 규칙이다 — 둘 다 미지원이면 숨김.
- **렌즈 전환 시 항상 1x로 초기화** (사용자 결정). 새 렌즈 범위가 들어오면 목록을 다시 만든다.

## Approaches

| 안 | 내용 | 트레이드오프 |
|---|---|---|
| A | `core:designsystem`에 `YGCameraZoomButton` | Figma `Button-Camera-Zoom`이 DS 컴포넌트이고 `YGCameraShutter` 선례와 같다. 모듈 2개 수정 |
| B | `feature/camera/impl/component` 로컬 컴포넌트 | 변경 작음. Figma 컴포넌트와 대응이 끊김 |

**A 채택** (사용자 결정). 순환 로직은 어느 안이든 VM이 갖는다 — 렌즈 전환 초기화가 VM에 있고, 단위 테스트가 가능하다.
클릭 단위 변경이라 기존 `LaunchedEffect(zoomRatio, camera)` 반영 경로를 그대로 쓴다.

## UI (Figma `Button-Camera-Zoom` `6724:13383`)

- 터치 영역 44dp(`SizeTokens.Size44`), 안쪽 `padding2`(4dp) → 원 36dp
- 원: `radius.round`, 테두리 1.5dp `Transparency.White25`, 배경 Default `Transparency.Black50` / Pressed `Transparency.Black75`
- 글자: `typography.caption.c01M`, 색 `Gray.White`, 라벨 `1x` `2x` `0.5x`
- 눌림은 `YGCameraShutter`처럼 `collectIsPressedAsState` + `clickableYGNoRipple`
- 위치: 뷰파인더 하단 중앙, 아래 여백 `padding4`(10dp)

## 빌더에게 남긴 것

- DS 컴포넌트는 라벨 문자열을 받는다(`label: String`) — DS가 feature 모델을 알면 안 된다. 라벨 문자열은 `CameraZoomLevel`이 가진다.
- `contentDescription` 문구.
