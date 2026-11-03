---
id: camera-zoom-button
title: 커스텀 카메라 줌 버튼 (Camera zoom toggle)
status: in-progress
category: ui-spec
platforms: android
verified: 2026-11-03
related_code:
  - YGCameraZoomButton.kt#YGCameraZoomButton
  - CameraZoomLevel.kt#CameraZoomLevel
  - HalfZoomSource.kt#HalfZoomSource
  - UltraWideCamera.kt#findUltraWideCamera
  - CameraPreviewComponent.kt#CameraPreviewViewComponent
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

Figma 순서 `1x → 2x → 0.5x`에서 현재 렌즈가 낼 수 있는 단계만 남긴다.
2x는 `zoomRange`(CameraX `ZoomState` min..max)에 들어갈 때, 0.5x는 아래 「0.5x를 내는 방법」 중 하나가 있을 때 낼 수 있다.

| 지원 | 순환 |
|---|---|
| 2x·0.5x 모두 | 1x → 2x → 0.5x → 1x |
| 0.5x 미지원 | 1x → 2x → 1x |
| 2x 미지원 | 1x → 0.5x → 1x |
| 둘 다 미지원 | 버튼 숨김 |

- 1x는 CameraX에서 항상 범위 안이라 별도 처리하지 않는다.
- 전면 카메라도 같은 규칙이다 — 둘 다 미지원이면 숨김.
- **렌즈 전환 시 항상 1x로 초기화** (사용자 결정). 전환 직후 목록도 1x 하나로 비워 새 렌즈 범위가 올 때까지 버튼을 숨긴다 —
  비우지 않으면 그 사이 이전 렌즈 단계로 순환한다.

## 0.5x를 내는 방법 (사용자 결정: 초광각 전환 포함)

후면에서만 따진다. 우선순위 순서:

1. **줌 배율** — 메인 카메라 `zoomRange`에 0.5가 들면 `setZoomRatio(0.5f)`. 메인·초광각을 묶은 논리 카메라를 앱에 노출하는 기기(Pixel 등).
2. **초광각 카메라 전환** — 1이 안 되고, 다른 후면 카메라 중 초광각이 있으면 0.5x에서 그 카메라로 다시 바인딩하고 배율은 1f.
   초광각을 별도 카메라 ID로만 노출하는 기기(Galaxy A35: 메인 ID 0, 초광각 ID 2, 둘 다 범위 1.0–8.0, 논리 카메라는 비공개).
   이런 기기는 이 방법으로만 0.5x가 나온다. 촬영도 초광각으로 한다.
3. 둘 다 없으면 0.5x를 뺀다.

초광각 판정 (내 판단): 화각 지표 `센서 폭 / 최소 초점거리`(`SENSOR_INFO_PHYSICAL_SIZE`, `LENS_INFO_AVAILABLE_FOCAL_LENGTHS`)가
메인의 `1 / 0.7`배 이상인 후면 카메라 중 가장 넓은 것. 초점거리만 보면 매크로·심도 센서를 잘못 고른다.
라벨은 실제 화각과 무관하게 Figma대로 `0.5x` (내 판단). 전환 때 프리뷰가 잠깐 끊기는 것은 감수한다 (내 판단).

- 범위 보고(`OnZoomRangeReady`)는 메인 카메라에 바인딩됐을 때만 보낸다. 초광각에 붙었을 때 보내면 1x 초기화가 돌아 0.5x가 바로 풀린다.

## 재적용 (리뷰 Important, 실기기 재현)

CameraX는 카메라가 비활성화되면(ON_STOP) 줌을 1.0으로 되돌린다. 복귀 때는 재바인딩도 범위 보고도 없어,
상태는 2x인데 프리뷰·촬영은 1x가 된다. 카메라 상태가 `OPEN`이 될 때마다 현재 배율을 다시 건다.

## 전환 연출 (사용자 결정: 기본 카메라 동작을 따른다)

Galaxy A35 기본 카메라 녹화를 프레임 단위로 본 결과를 따른다.

| 전환 | 연출 |
|---|---|
| 같은 카메라 안의 배율 변경 (1x↔2x, 줌 배율로 내는 0.5x) | 블러 없이 약 200ms 동안 배율을 부드럽게 바꾼다. 카메라를 다시 열지 않아 라이브 프리뷰가 이어진다 |
| 초광각 재바인딩 (0.5x↔1x·2x) | 마지막 프레임을 강한 블러로 덮고 크기 변화 없이 정지. 새 카메라가 프레임을 보내기 시작하면(`PreviewView.previewStreamState == STREAMING`) 짧게 페이드아웃 |
| 전면/후면 전환 | 블러 덮개가 Y축으로 원근 있게 약 300ms 뒤집힌다. 그 뒤는 위와 같다 |

- 검정 화면은 재바인딩(카메라를 닫고 다시 여는 동안) 때만 생긴다. 같은 카메라 줌은 덮개가 필요 없다.
- 크기 애니메이션은 넣지 않는다 — 기본 카메라에 없고, 줄이면 가장자리로 검정 바닥이 드러난다.
- 덮개는 `CameraFeedLayer` 안에 그려 뷰파인더 선명 영역·바깥 블러 합성을 그대로 받는다.
- 첫 진입은 캡처할 프레임이 없어 덮개가 없다.
- 첫 바인딩·재바인딩 직후의 배율은 애니메이션 없이 바로 건다 (내 판단 — 그 순간 화면은 덮개 아래다).
- 수치(블러 반경, 페이드·뒤집기 시간)는 실기기에서 맞춘다 (내 판단).

## 재바인딩 견고성 (리뷰 Minor 반영)

- 「다시 찍기」 등으로 다시 바인딩되어 범위가 다시 오면, 현재 단계가 새 목록에 있으면 유지하고 없을 때만 1x (사용자 결정: 유지로 통일).
  렌즈 전환은 따로 1x로 초기화한다.
- 폐기된 바인딩 리스너는 아무것도 하지 않는다(폐기 표시 확인).
- 초광각 바인딩이 실패하면 메인으로 바인딩하고 `hasUltraWideCamera = false`로 범위를 보고한다 — 0.5x가 빠지고 1x로 돌아간다.
- 카메라 정보 조회(`CameraSelector.filter`, 특성 읽기) 예외는 "초광각 없음"으로 본다.

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
- `contentDescription` 문구 — 미정. 라벨 텍스트 + Button 역할로 읽힌다.
