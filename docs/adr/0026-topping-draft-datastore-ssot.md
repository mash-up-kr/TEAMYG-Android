---
id: ADR-0026
title: 토핑 만들기 흐름 상태를 DataStore 초안 한 벌로 모은다
status: accepted
date: 2026-08-20
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0022, ADR-0023, ADR-0025
related_spec: c106-topping-place-api, c106-topping-place, segmentation-pipeline-hardening
related_architecture: data-layer, state-management, navigation-flow
platforms: android
tags: [adr, parfait, topping, state, datastore, navigation]
---

# ADR-0026: 토핑 만들기 흐름 상태를 DataStore 초안 한 벌로 모은다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락

토핑 만들기는 화면 다섯을 지난다 — 캔버스 → 카메라·갤러리 → 세그멘테이션 → 확인 → 배치.
그 사이 편집을 거쳐 확정된 흐름 결과가 `SegmentationConfirmRoute`의 `rememberSaveable` 셋에만
살아서, **그 화면을 벗어나면 다음 화면이 볼 길이 NavKey 인자뿐**이다.

배치 API를 결선하려면 여기에 넷이 더 붙는다 — `groupId`·`parfaitId`·`nextPositionZ`와
테두리 값([ADR-0025](0025-topping-border-as-server-field.md)). 전부 NavKey에 실으면
`NavKeyCanvasToppingPlace`가 인자 여섯을 갖고, **`camera`·`segmentation` 모듈이 캔버스 개념을
떠안는다.** 배경 편집처럼 캔버스와 무관한 경로까지 같은 인자를 나르게 된다 —
`segmentation-pipeline-hardening`이 `popUpTo` 도입 때 "NavKey 다섯에 groupId를 싣는 대안"을
같은 이유로 기각한 자리다.

## 결정

**토핑 만들기 흐름 상태를 `:data`의 DataStore에 초안 한 벌로 두고, 흐름의 화면들이 그것을 읽고
쓴다. 초안은 흐름당 하나만 존재한다.**

- 담는 것: 캔버스 식별값(`groupId`·`parfaitId`·`nextPositionZ`), 알맹이·cutout 경로,
  테두리 색·굵기.
- **여는 시점은 흐름 진입 하나뿐이다.** `CanvasMain`이 카메라·갤러리로 떠날 때 캔버스 식별값으로
  **새로 덮어쓰고** 이미지·테두리를 비운다. 낡은 초안이 다음 흐름에 따라붙는 문제가 이 규칙 하나로
  닫히므로 별도 만료·정리 경로를 두지 않는다.
- 채우는 곳은 세그멘테이션 완료와 편집 완료, 읽는 곳은 배치 화면, 비우는 곳은 배치 성공이다.
- 걷는 것은 `SegmentationConfirmRoute`의 `rememberSaveable` 셋**뿐**이다.
- ⚠️ **`TOPPING_EDIT_RESULT_KEY`는 걷지 않는다.** 그 결과 키의 소비자가 둘이다 —
  `SegmentationConfirmRoute`와 **`CanvasBGEditRoute`**(C-301에서 이미 놓인 토핑을 `borderOnly`로
  다시 손보는 경로). 편집 화면이 결과 키 대신 초안에 쓰도록 바꾸면 배경 편집 쪽은 편집을 마쳐도
  아무것도 반영되지 않고 **컴파일은 통과한다.** 결과 키는 전달 수단으로 남기고, 그것을 받은
  `SegmentationConfirmRoute`가 초안에 옮겨 적는다.
- **`NavKeySegmentationConfirm`의 경로 셋도 그대로 둔다.** 그것은 화면을 여는 인자이고 초안은
  흐름의 결과물이다. 두 값이 겹치는 구간에서는 **초안이 정본**이다 — 편집을 거치면 NavKey의 값이
  낡는다.
- `NavKeyCanvasToppingPlace`는 인자가 없어진다.

**영속을 고른 이유**는 프로세스 사망 복원이다. NavKey와 `rememberSaveable`은 직렬화돼 복원되므로,
인메모리 보관으로 옮기면 지금 있는 보장을 잃는다 — 백그라운드에서 죽었다 돌아온 사용자가 촬영·
누끼·편집을 처음부터 다시 하게 된다.

**진입 캔버스가 못 박히는 것**도 이 배치가 지킨다. 흐름 진입 시 기록한 `parfaitId`를 배치가 그대로
쓰므로, 도중에 하루 경계를 넘어도 다른 캔버스로 조용히 옮겨 가지 않는다. 그 경우는 서버가
409 `PARFAIT_ALREADY_CLOSED`로 거절하고 화면이 사용자에게 알린다.

## 대안

- **전부 NavKey에 싣는다** — 직렬화 복원이 공짜이고 의존이 명시적이다. 그러나
  `NavKeyCanvasToppingPlace`가 인자 여섯이 되고 `camera`·`segmentation`이 캔버스 개념을 떠안는다.
  **→ 기각:** 흐름이 길어질수록 중간 모듈이 나르는 남의 인자가 계속 는다.
- **인메모리 초안(프로세스 수명)** — DataStore IO가 없고 정리가 저절로 된다. 그러나 프로세스
  사망 복원에서 편집 결과가 사라져 지금 있는 보장을 되레 잃는다.
  **→ 기각:** 흐름이 길어(촬영·누끼·편집) 잃었을 때 사용자가 치르는 비용이 크다.
- **초안 없이 배치 시점에 재조회** — `parfaitId`를 확정 때 `getTodayCanvas`로 다시 얻는다.
  마감 경계를 넘어도 409를 안 맞는다. 그러나 사용자가 들어간 캔버스가 아닌 곳에 토핑이 올라간다.
  **→ 기각:** 조용히 다른 캔버스에 쓰는 것보다 거절하고 알리는 편이 옳다.

## 영향

**긍정**

- 흐름의 결과물에 **정본이 생긴다.** 지금은 확정된 편집 결과가 한 화면의 `rememberSaveable`에만
  살아 다음 화면이 볼 길이 없다.
- `camera`·`segmentation` 모듈이 캔버스 개념과 무관해진다. NavKey는 한 줄도 안 바뀐다.
- 프로세스 사망 복원 범위가 오히려 넓어진다 — 지금은 `SegmentationConfirmRoute`를 벗어나면
  편집 결과가 NavKey에 없어 되살아나지 않는다.

**트레이드오프**

- 흐름 상태가 **암묵적**이 된다. 배치 화면의 시그니처만 봐서는 무엇에 의존하는지 안 보인다.
- DataStore IO가 흐름 경로에 들어온다(읽기·쓰기 각 몇 회).
- 초안이 비어 있는 채로 배치 화면에 도달하는 경로가 이론상 생긴다 — 정상 흐름에서는 불가능하지만
  방어는 필요하다.

**위험·방어**

- `CanvasMainUiState.todayCanvas`가 없으면 `parfaitId`도 `nextPositionZ`도 없다. 그 상태에서는
  **토핑 추가 버튼을 비활성한다** — 열어 두면 촬영·누끼·편집을 다 마친 뒤에야 올릴 데가 없음을
  알게 된다. 서버는 오늘 조회 때 캔버스를 만들어 주므로 "서버에 없다"는 경우는 없고, 없는 것은
  **앱이 아직 못 받은 경우**다(로딩 구간과 조회 실패). 그 실패가 화면에 아무 표현이 없던 것도
  이 라운드가 함께 연다 — 안 그러면 버튼이 왜 안 눌리는지가 보이지 않는다.
- 초안이 비어 있으면 배치 화면은 확인을 비활성한다.
- **초안이 가리키는 파일이 이미 없을 수 있다.** DataStore는 영속되지만 그것이 가리키는 것은
  `cacheDir` 하위 파일이고, 세그멘테이션 진입이 그 디렉토리를 통째로 비운다
  (`SegmentationCacheDir#clearFiles`). OS도 저장 공간 압박 시 회수한다. 초안을 읽을 때
  **"경로는 있는데 파일이 없다"를 그 경로가 처음부터 없었던 것과 같이 취급한다** — 비우는 것은
  **이미지 경로 둘뿐**이고 캔버스 식별값과 테두리는 남긴다. 초안 전체를 버리면 아래 "진입 캔버스가
  못 박히는 것"이 함께 깨진다.
- 덮어쓰기·비우기를 단위 테스트로 고정한다. 이 결정의 안전성이 전부 그 두 규칙에 걸려 있다.
- 초안에 담기는 것은 캐시 파일 경로와 id·색·수치뿐이다. 개인 식별 정보가 아니라
  `EncryptedPreferences`(ADR-0019) 대상이 아니다.
