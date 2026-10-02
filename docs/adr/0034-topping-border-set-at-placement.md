---
id: ADR-0034
title: 토핑 테두리는 배치 단계에서 정하고 초안에 싣지 않는다
status: proposed
date: 2026-10-02
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0025, ADR-0026
related_spec: c105-arrange-border-merge, c106-topping-place
related_architecture: state-management, navigation-flow
platforms: android
tags: [adr, parfait, topping, border, state]
---

# ADR-0034: 토핑 테두리는 배치 단계에서 정하고 초안에 싣지 않는다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락

테두리는 캔버스 배경과 이미 놓인 토핑 위에서 봐야 고를 수 있는 값이다. 편집 화면은 사진만
보여 주므로 거기서 고른 테두리가 캔버스에서 어떻게 보이는지는 배치 화면에 가서야 알게 된다.
배치 화면이 테두리를 고르는 자리이면 테두리 값이 화면 사이를 건너갈 이유가 없다 —
[ADR-0026](0026-topping-draft-datastore-ssot.md)의 초안은 고르는 화면과 쓰는 화면이 다른 값을
나르는 수단이다.

초안은 프로세스 사망 복원을 위해 DataStore에 영속한다. 테두리를 초안에서 빼면 그 보장의 범위가
줄어드므로, 줄이는 것이 결정임을 여기에 남긴다.

## 결정

**테두리는 편집이 아니라 배치 단계의 속성이다. 새 토핑의 테두리는 배치 화면의 상태에만 두고
초안에 싣지 않는다.**

- 값은 `CanvasToppingPlaceUiState.border`(`ToppingBorderStyle?`) 하나다. 배치 화면의
  `ToppingBorderPanel`이 채우고, 확정 때 `toToppingBorder`가 `ToppingBorder`로 바꿔
  `AddToppingUseCase`에 싣는다. 서버 필드로 보낸다는
  [ADR-0025](0025-topping-border-as-server-field.md)는 그대로다.
- `ToppingDraft`와 `ToppingDraftEntity`에는 테두리 필드가 없다. 그래서 **ADR-0026이 영속으로 지키는
  프로세스 사망 복원에서 테두리는 빠진다** — 배치 화면에서 프로세스가 죽었다 돌아오면 알맹이와
  캔버스 식별값은 초안에서 되살아나고 테두리는 "없음"으로 돌아간다.
- 빼도 되는 이유는 둘이다.
  - 고르는 데 몇 초 걸리는 값이다. ADR-0026이 영속을 고른 근거는 촬영·누끼·영역 편집을 처음부터
    다시 하게 되는 비용인데, 테두리는 그 비용에 들지 않는다.
  - 같은 값을 헤더의 뒤로로 확인 화면에 돌아갈 때도 버린다. 정상 경로에서 버리는 값을 프로세스
    사망에서만 살리면 두 경로의 결과가 갈린다.
- "테두리 없음"은 `border == null`이다. 색 없이 굵기만 실린 상태를 타입으로 만들 수 없게 해,
  굵기만 든 `Solid`가 서버로 가는 경로를 없앤다. 색을 고르기 전 슬라이더 값은
  `pendingBorderWidthDp`가 따로 든다.
- 한 겹만 든다. 겹 목록을 나르는 타입은 앱에 없다.
- 이미 놓인 본인 토핑의 테두리도 같은 자리에서 정한다. 배치 수정 화면(`CanvasToppingArrangeViewModel`)이
  같은 `ToppingBorderPanel`을 쓰고, 값은 토핑마다 `EditableTopping.border`(`ToppingBorderStyle?`) 하나이며
  확정 때 `UpdateToppingBorderUseCase`로 간다.
- 편집 화면은 영역만 고친다. `NavKeyToppingEdit`와 `ToppingEditResult`에는 테두리 필드가 없다.

## 대안

- **패널이 초안에 쓴다** — 프로세스 사망에서도 테두리가 살아남는다. 그러나 슬라이더를 끄는 동안
  DataStore 쓰기가 이어지고, 헤더의 뒤로로 돌아갔을 때 초안에 남은 테두리를 확인 화면이 어떻게
  다룰지를 다시 정해야 한다.
  **→ 기각:** 잃었을 때 비용이 몇 초인 값에 영속 경로와 정리 규칙을 붙이는 것이다.
- **편집 화면의 테두리 탭을 유지한다** — 초안과 복원 보장이 그대로다. 그러나 테두리를 캔버스
  배경 위에서 보며 고를 수 없고, 고르려면 화면 하나를 더 거친다.
  **→ 기각:** 이 결정이 풀려는 문제가 그대로 남는다.
- **`SavedStateHandle`로 테두리만 살린다** — DataStore 없이 프로세스 사망을 덮는다. 그러나
  배치 화면의 위치·배율·각도도 `CanvasToppingPlaceUiState`에만 있어 프로세스 사망에서 초기 배치로
  돌아간다. 테두리만 살리면 배치는 초기화되고 테두리만 남는다.
  **→ 기각:** 배치 화면 상태의 복원은 테두리와 따로 떼어 정할 일이 아니다.

## 영향

**긍정**

- 초안이 캔버스 식별값과 이미지 경로만 든다. `domain`·`data`의 초안 기록 경로
  (`RecordToppingDraftUseCase`, `EnsureDraftSubjectRecordedUseCase`, `ToppingDraftRepository.record`)가
  테두리를 모른다.
- 추가·수정 두 흐름 모두 테두리가 단일 값이라 그리는 값과 저장하는 값이 갈릴 수 없다.
- 확인 화면은 테두리를 그리지 않는다. 한 흐름에서 같은 테두리를 두 화면이 다르게 그릴 자리가
  없다.

**트레이드오프**

- 배치 화면에서 프로세스가 죽거나 헤더의 뒤로로 나갔다 돌아오면 고른 테두리를 다시 골라야 한다.
- 헤더의 뒤로는 묻지 않고 테두리를 버린다. 닫기와 시스템 뒤로가기는 그만두기 팝업을 띄우므로
  두 입력의 결과가 다르다.

**위험·방어**

- 옛 버전이 DataStore에 남긴 초안에는 테두리 키가 있다. 역직렬화가 모르는 키를 무시해 나머지
  필드는 그대로 읽힌다 — `ToppingDraftLocalDataSourceImplTest`가 고정한다.
- 색 없이 확정하면 `ToppingBorder.None`이 가고, 패널이 열린 채 확정해도 저장 위치는 화면에 보이는
  자리가 아니라 원래 자리다 — `CanvasToppingPlaceViewModelTest`가 고정한다.
- 배치 수정 화면의 테두리는 초안을 거치지 않는다. 그만두기로 나가거나 프로세스가 죽으면 저장 전
  변경은 위치·배율·각도와 함께 사라진다.
