---
id: canvas-feedback-fixes
title: 로딩 표시와 진입 흐름의 피드백 수정 다섯 (Canvas feedback fixes)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-10
related_code:
  - YGScaffoldV2
  - YG_LOADING_MINIMUM_VISIBLE_MILLIS
  - GroupListViewModel#loadGroups
  - CanvasMainRoute
  - CanvasLoadState
  - GroupCreateRoute
  - GroupCreateSideEffect.NavigateToNext
  - NotificationPermissionGate
  - GroupListAddGroupScreen
related_adr:
related_spec: ygscaffold-v2-common-loading-error, canvas-adaptive-polling, push-notification-permission-and-device-token
related_architecture:
  - design-system.md
  - state-management.md
supersedes:
superseded_by:
tags: [spec, parfait, loading, ux]
---

# Spec: 로딩 표시와 진입 흐름의 피드백 수정 다섯

> **사후 스펙(as-built)이다.** 선작성 설계 없이 사용자 피드백 대응으로 들어왔고
> (2026-09-10, PR #482 `efa771503`), 이 문서는 머지된 코드를 읽어 정본으로 세운 기록이다.
> 같은 PR의 [canvas-adaptive-polling](2026-09-10-canvas-adaptive-polling.md)은 별도 스펙이 정본이다.

## 목표

로딩·진입 흐름에서 사용자가 **무슨 일이 일어났는지 알 수 없던 자리** 다섯을 고친다. 넷은
"표시가 있었는데 안 보이는" 문제이고 하나는 "물을 이유가 없는데 묻는" 문제다.

## 1. 로딩 덮개의 최소 노출 — `YGScaffoldV2`

**덮개가 깜빡이기만 하고 사라지면 무엇을 기다렸는지 알 수 없다.** 통신이 순식간에 끝나면
사용자에게는 화면이 한 번 번쩍인 것으로만 남는다.

`YGScaffoldV2`가 `isLoading`을 그대로 그리지 않고 **켜진 순간부터 `YG_LOADING_MINIMUM_VISIBLE_MILLIS`
(500ms)가 지나기 전에는 꺼도 유지한다.** 유지 대상은 덮개와 접근성 트리 제거 둘 다다 — 하나만
붙들면 터치는 막히는데 TalkBack은 통과하는 비대칭이 생긴다.

값을 화면이 아니라 **디자인시스템이 소유한다.** 처음에는 캔버스 화면 안에 있었고
(`d4945a77f`), 같은 문제가 스캐폴드를 쓰는 모든 화면의 것이라 `YGScaffoldV2`로 올라갔다
(`2a3e5531f`). 그래서 이 성질은 **`YGScaffoldV2`를 쓰는 모든 화면의 계약**이다.

구현은 `rememberMinimumVisible` 하나다. `TimeSource.Monotonic`으로 켠 시각을 재고, 꺼질 때
남은 시간만큼 `delay`한 뒤 놓는다. 벽시계가 아니라 단조 시계라 시각 변경에 흔들리지 않는다.

## 2. 당겨서 새로고침 인디케이터의 최소 노출 — G-001

같은 문제이고 **자리가 다르다.** 그룹 목록의 pull-to-refresh 인디케이터는 스캐폴드 덮개가 아니라
`GroupListUiState.isRefreshing`이 그린다.

`GroupListViewModel#loadGroups`가 새로고침일 때만 `coroutineScope` 안에 `delay(500ms)` 잡을
**조회와 나란히** 세운다. 뒤에 붙이면 이미 느린 통신에 500ms가 더 얹힌다 — 나란히 세우면 빠른
통신에서만 기다린다.

⚠️ **두 최소 노출이 서로 다른 상수다.** `YG_LOADING_MINIMUM_VISIBLE_MILLIS`(디자인시스템)와
`GroupListViewModel.REFRESH_MINIMUM_VISIBLE_MILLIS`(화면)가 같은 값 500을 각자 들고 있다 →
[open-questions](../../../synthesis/open-questions.md) OQ-P-394.

## 3. 첫 페인트 뒤에는 덮개를 다시 띄우지 않는다 — C-001

**폴링이 남이 올린 토핑을 실어 올 때마다 보고 있던 캔버스가 덮개 뒤로 사라졌다.** 이미지 로드
상태(`CanvasLoadState`)를 접어 전체 덮개로 옮기던 것이 원인이다.

`CanvasMainRoute`가 캔버스별로 **한 번 그려졌는지**를 기억한다(`paintedCanvasIds`). 그 캔버스가
이미 그려진 뒤에는 `loadState`가 `Loaded`를 벗어나도 덮개를 띄우지 않고, 실패한 토핑만 그려지지
않는다.

판정이 두 자리에서 무너지기 쉬워 각각 방어가 들어갔다.

- **`loadState`는 아직 아무 이미지도 안 붙은 첫 컴포지션에서도 `Loaded`다.** 그래서 "로딩을 한 번
  본 뒤"(`sawLoading`)로 좁히지 않으면 캐시된 캔버스로 들어올 때 덮개가 아예 안 뜬다.
- **이미 그린 캔버스로 돌아오는 것은 리셋이 아니다.** 달력에서 지난 날을 보다 오늘로 돌아오는
  경우가 그렇다. 리셋 조건을 "표시 캔버스가 바뀌었을 때"로 두면, **토핑도 배경도 없는 캔버스는
  `loadState`가 `Loaded`를 벗어나지 않아** 한 번 버린 관측을 다시 얻지 못하고 판정이 굳는다
  (`3a4a14004`·`fa4e8bd3c`가 그 두 갈래를 각각 고쳤다). 그래서 **아직 그리지 못한 다른 캔버스로
  옮겨 왔을 때만** 관측을 처음부터 다시 모은다.

`CanvasLoadState`의 KDoc이 "이 접기 결과를 전체 덮개로 옮기는 것은 첫 페인트 전까지"라고 그
경계를 받아 적는다.

## 4. 정원 1로 만든 그룹에서는 알림 권한을 묻지 않는다 — A-005

**토핑 알림은 서버가 작성자를 빼고 보내므로 혼자인 그룹에서는 영영 오지 않는다.** 데일리
리마인드는 인원수를 보지 않아 그 사용자도 대상이지만, **한 번뿐인 런타임 권한 요청**을 실익이
적은 자리에서 소진하지 않는다.

가르는 곳은 `GroupCreateRoute`다. `GroupCreateSideEffect.NavigateToNext`가 `memberLimit`를
함께 나르고, `memberLimit > 1`일 때만 `NotificationPermissionGate`를 세운다. 아닌 경우는
곧장 새 캔버스로 간다.

`NavigateToNextSaver`도 그 필드를 함께 저장·복원한다 — 빠뜨리면 프로세스 사망 복원 뒤에 정원
1 그룹에도 모달이 뜬다. `NotificationPermissionGate`의 KDoc이 이 예외와 가르는 자리를 적는다.

**A-004(그룹 참여)는 그대로다.** 참여하는 그룹은 정의상 혼자가 아니다.

## 5. 그룹 추가 메뉴의 딤과 칩 — G-001

메뉴 바깥을 덮는 딤이 `clickableYG`라 **누를 때마다 리플이 화면 전체에 퍼졌다.**
`clickableYGNoRipple`로 바꾼다 — 딤은 버튼이 아니라 닫기 영역이다.

`YGChipButton`의 `onClick`이 빈 람다여서 **칩을 눌러도 아무 일도 일어나지 않았다.** 닫기를
연결한다.

## 테스트

- `YGScaffoldV2Test`(계측 2건) — 최소 노출 안에 껐을 때 덮개가 유지되는지, 그 뒤에 걷히는지.
  ⚠️ CI는 계측을 컴파일만 한다.
- `GroupListViewModelTest`에 새로고침 최소 노출 케이스가 붙었다.
- `GroupCreateViewModelTest`·`GroupCreateNavigateToNextSaverTest`가 `memberLimit` 전달과
  저장·복원을 단언한다.
- ⚠️ **3번(첫 페인트)에는 자기 테스트가 없다.** 판정이 `CanvasMainRoute`의 컴포지션 상태 넷에
  얹혀 있어 ViewModel 테스트가 닿지 않는다 → OQ-P-395.
