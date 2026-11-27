---
id: ADR-0038
title: 혼자 공간은 화면만 함께 쓰고 ViewModel·저장소를 따로 둔다
status: accepted
date: 2026-11-26
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0026, ADR-0029, ADR-0031
related_spec: 2026-11-26-solo-space-design
related_architecture: data-layer, navigation-flow
platforms: android
tags: [adr, parfait, canvas, solo-space]
---

# ADR-0038: 혼자 공간은 화면만 함께 쓰고 ViewModel·저장소를 따로 둔다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락
서버가 닫힌 뒤에도 쓸 수 있는 기기 로컬 캔버스(혼자 공간)가 필요하다. 그룹 캔버스와 화면은 같게 두되
서버를 부를 경로가 있어서는 안 된다. 운영 중에는 가짜 id 호출이 조용히 4xx로 끝나고, 종료 뒤에는 화면이
로딩에 갇힌다.

## 결정
**B. 화면만 함께 쓰고 ViewModel·저장소를 따로 둔다.** 화면 composable·UiState는 그룹 캔버스와 함께 쓰고, ViewModel·use case·저장소는 혼자 공간 전용으로 따로 둔다.

- `Solo*ViewModel` 다섯은 `domain/usecase/solo/*`, 토핑 초안 use case, 기기 로컬 use case만 의존한다.
  `AnalyticsLogger`와 서버를 부르는 저장소에 닿는 use case는 의존하지 않는다. `SoloViewModelDependencyTest`가
  생성자 의존을 허용 목록으로 검사한다.
- 그룹 ViewModel 안의 순수 계산은 `impl/util`의 internal 함수로 빼서 두 쪽이 부른다.
- 혼자 공간 nav key를 따로 두므로 토핑 흐름의 `popUpTo`는 표식 인터페이스(`CanvasHomeNavKey`,
  `CanvasBGEditNavKey`)와 `Navigator.popUpTo`의 `isInstance` 비교로 푼다.
- 카메라·갤러리·누끼 화면은 공간을 모르므로 공간은 `ToppingDraftTarget`이 초안에 실어 나른다.

## 대안
- **A. 특수 `GroupId` + `:data` 분기** — 기존 화면·ViewModel을 그대로 쓴다.
  그러나 분기가 빠진 경로가 가짜 id로 서버를 부르고 컴파일러가 잡지 못한다. 날짜 전제 계약
  (`todayCanvasRefreshFailures`·`getYears`·`getPastCanvases`)도 가짜로 채워야 한다.
  **→ 기각:** 정확성을 구조로 보장할 수 없다.
- **C. Hilt qualifier로 화면마다 구현 바꿔 끼우기** — ViewModel 코드는 그대로다.
  그러나 ViewModel → UseCase → 저장소 사슬마다 qualifier 판이 필요하고, A와 같은 가짜 계약 문제가 남는다.
  **→ 기각:** 보일러플레이트가 결정안(B)보다 많고 이득이 없다.

## 영향

**긍정**

- 혼자 공간 ViewModel에는 서버를 부를 경로가 구조상 없다.
- 그룹 UseCase 변경이 혼자 공간을 깨지 않고, 반대도 마찬가지다.
- 서버가 닫힌 뒤 기능은 혼자 공간에만 더하면 되고 날짜·그룹 계약에 묶이지 않는다.

**트레이드오프**

- 배치·정렬·텍스트·배경 편집 ViewModel 로직이 두 벌이다. 순수 계산만 공유한다.
- `SoloCanvasViewModel`이 `SoloCanvasVO`를 `CanvasVO`로 바꿔 `parfaitId`·`groupName` 같은 고정값을 채운다.
  변환은 `SoloCanvasVOMapper` 한 곳뿐이고, 혼자 공간 저장소·use case 시그니처에 `CanvasVO`·`ParfaitId`·`GroupId`가
  없음을 `SoloViewModelDependencyTest`가 검사한다. `CanvasMainUiState`에 그룹 전용 필드가 늘면 고정값도 는다.
- Solo ViewModel은 그룹의 UiState·Intent(`CanvasToppingPlaceUiState`·`CanvasToppingPlaceIntent` 등)를 그대로
  import한다. 화면을 함께 쓰기로 한 결과라 의도된 결합이다. 그룹 쪽 Intent·State가 바뀌면 Solo ViewModel도
  컴파일 단계에서 함께 고쳐야 한다.
- 카메라·갤러리·누끼 화면은 그룹과 함께 쓰므로 혼자 공간에서도 분석 이벤트를 보낸다.

**위험·방어**

- 혼자 공간 ViewModel에 서버 use case가 들어오면 `SoloViewModelDependencyTest`가 깨진다. 허용 목록이 낡을 수
  있으므로 새 기기 로컬 use case를 쓸 때 목록을 함께 고친다.
- 화면 조회 분석은 `NavKeyAnalyticsScreen`이 혼자 공간 키 다섯에 `null`을 돌려 제외한다.
