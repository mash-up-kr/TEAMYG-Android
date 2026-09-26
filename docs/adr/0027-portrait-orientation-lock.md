---
id: ADR-0027
title: 앱 전체를 세로로 고정하고 대화면 예외까지 opt-out으로 되돌린다
status: accepted
date: 2026-08-22
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0006
related_spec: c101-camera-picture-confirm, a002-kakao-login-api
related_architecture: navigation-flow
platforms: android
tags: [adr, parfait, manifest, window, orientation]
---

# ADR-0027: 앱 전체를 세로로 고정하고 대화면 예외까지 opt-out으로 되돌린다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락

지금까지 화면 방향은 아무도 정하지 않아 기기 설정을 그대로 따랐다. 그런데 이 앱의 화면 규격은
세로 폭을 전제로 잡혀 있다 — G-001 목록의 지그재그 좌표는 실측 y값이고, C-101 뷰파인더는
좌우·상하 여백이 고정이며, C-001 캔버스는 Area만 고정 종횡비다. 가로에서 무엇을 어떻게 보일지는
위키 정책에도, 구현 문서에도 없다.

방향이 바뀌면 액티비티가 재생성된다는 것도 별개의 비용이다. A-002 라운드의 최종 리뷰는
**카카오 로그인 창이 떠 있는 동안 회전하면 로그인이 유실된다**는 것을 잡았고, 그 항목은 실기기
확인 목록에 남은 채다(OQ-P-146 ⑨).

## 결정

**앱을 세로로 고정한다.** `MainActivity`(`app`·`app-preview` 양쪽)와 카카오 리다이렉트
`AuthCodeHandlerActivity`에 `android:screenOrientation="portrait"`를 붙인다.

- **로그인 왕복의 액티비티까지 붙인다** — 단일 액티비티 앱이지만 카카오 SDK가 자기 액티비티를
  쌓으므로, 그것만 빠지면 하필 **로그인 구간에서만** 회전이 살아 있게 된다. 위 유실 경로가 바로
  그 구간이다.
- **대화면 예외를 opt-out으로 되돌린다** — `targetSdk 36`(Android 16)은 smallest width 600dp
  이상 기기에서 `screenOrientation`을 무시한다. `<application>`에
  `PROPERTY_COMPAT_ALLOW_RESTRICTED_ORIENTATION_AND_ASPECT_RATIO_OPT_OUT`을 두어 그 예외를
  끈다. 켜 두면 폰과 태블릿·폴더블의 거동이 갈리는데, 갈린 쪽의 레이아웃은 아무도 만든 적이 없다.
- **시한이 있다** — 이 opt-out은 `targetSdk 37`부터 속성 자체가 사라진다. 그전에 대화면 대응
  방침을 정해야 하고, 지금 그 방침은 없다 → [open-questions](../synthesis/open-questions.md).

## 대안

- **대안 A: 가로를 지원한다(반응형)** — 태블릿·폴더블 사용자가 자연스럽게 쓰고 Android 16이
  가는 방향과도 맞다. 그러나 **가로 규격이 정책에 없다.** 캔버스 종횡비·토핑 정규화 좌표는 회전을
  견디지만 목록 지그재그 좌표·뷰파인더 여백·달력 그리드는 세로 폭 실측이라, 지원한다는 것은 곧
  정책을 새로 만든다는 뜻이다.
  **→ 기각:** 근거가 될 정책이 없고 이 라운드의 목적(출시 준비)에서 비용이 가장 크다.
- **대안 B: 화면별로 방향을 정한다** — 카메라만 세로, 나머지는 자유 같은 식.
  **→ 기각:** 단일 액티비티라 매니페스트에 나눌 자리가 없고, 런타임으로 바꾸면 화면마다 되돌릴
  책임이 생긴다. 무엇보다 화면별로 다르게 둘 근거가 아직 없다.
- **대안 C: 세로 고정만 하고 opt-out은 붙이지 않는다** — 매니페스트가 단순해진다.
  **→ 기각:** 폰만 고정되고 대화면은 눕는다. 검증한 적 없는 레이아웃이 특정 기기군에서만 나오는
  것이 가장 나쁜 조합이다.

## 영향

**긍정**

- 방향 변경으로 인한 액티비티 재생성 경로가 사라진다 — 로그인 왕복 중 유실(OQ-P-146 ⑨)의
  재현 경로가 닫힌다. 다만 프로세스 사망·기타 구성 변경은 그대로이므로 `rememberSaveable`·
  DataStore 초안([ADR-0026](0026-topping-draft-datastore-ssot.md))의 근거는 유지된다.
- 세로 폭을 전제로 실측한 화면 규격들이 깨질 경로가 없다.

**트레이드오프**

- **시한부 결정이다.** `targetSdk 37`에서 opt-out이 무력화되며, 그때 대화면 방침이 없으면
  거동이 조용히 갈린다.
- **카메라 촬영이 기기 방향을 따라가지 않는다.** `CustomCameraRoute`는 `targetRotation`을 직접
  주지 않고 `ImageProxy#imageInfo.rotationDegrees`로 보정하는데, 고정된 뒤로 그 값이 실제로 든
  방향을 반영하지 않는다 → [open-questions](../synthesis/open-questions.md).
- `app-preview` 컴포넌트 갤러리도 함께 고정된다. 넓은 화면에서 컴포넌트를 늘어놓고 보는 용도와는
  어긋나지만, 본 앱과 다르게 두면 갤러리에서 본 것이 본 앱의 모습이 아니게 된다.
- 기기를 가로로 두고 쓰는 사용자(거치대·접근성 사유)에게는 선택지가 사라진다.

**위험·방어**

- 매니페스트 속성이라 **유닛 테스트로 덮을 수 없다.** 확인 수단은 실기기뿐이고 아직 0회다.
- 대화면 opt-out은 폰에서 드러나지 않는다 — 태블릿·폴더블 1회 확인이 필요하다.
