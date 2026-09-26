---
id: ADR-0006
title: Navigation3 + 커스텀 Navigator + feature 엔트리 빌더
status: accepted
date: 2026-05-19
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec:
related_architecture:
platforms: android
tags: [adr, parfait]
---
# ADR-0006: Navigation3 + 커스텀 Navigator + feature 엔트리 빌더

## 맥락
feature :api/:impl 분리([[0002-feature-api-impl-split]]) 상태에서 app이 각 화면을 직접 알지 않으면서도 목적지로 이동하고 결과를 주고받아야 한다. 화면 간 결과 전달·공유 요소 전환도 필요했다.

## 결정
**Navigation3**(`androidx.navigation3`, `1.2.0` alpha 계열 — 채택 시 `alpha04`, 현행 핀은 버전 카탈로그가 정본)를 채택하고 그 위에 자체 라우팅을 얹는다. alpha 버전을 택한 이유는 결과 전달용 `ResultEventBusNavEntryDecorator` 지원.

- **Navigator**(`core:navigation`) — `@ActivityRetainedScoped`. 백스택을 `SnapshotStateList<NavKey>`로 보유. `goTo()`, `onBack()`, `clearBackStack()`.
- **목적지 = NavKey** — 각 feature `:api`가 `@Serializable NavKey`(예: `NavKeyLogin`)를 노출.
- **엔트리 빌더 주입** — 각 feature `:impl`이 엔트리 빌더 함수(예: `featureLoginEntryBuilder()`)를 노출하고, Hilt 멀티바인딩 `Set<EntryProviderScope<NavKey>.(Navigator) -> Unit>`로 `MainActivity`에 주입. `MainRoute`가 `entryProvider { }` DSL로 순회 등록.
- **NavEntry 데코레이터**(`MainRoute`) — `rememberSaveableStateHolderNavEntryDecorator`(상태 보존), `rememberViewModelStoreNavEntryDecorator`(엔트리별 ViewModel 수명), `rememberResultEventBusNavEntryDecorator`(엔트리 간 결과 전달).

## 대안
- **Navigation-Compose(2.x, 안정)** — 성숙. 그러나 결과 전달·타입 안전 목적지에서 이 프로젝트 요구와 마찰.
  **→ 기각:** Navigation3의 NavKey·데코레이터 모델이 :api/:impl 격리와 잘 맞음.
- **단일 라우트 그래프에 화면 직접 등록** — 간단하나 app이 모든 impl에 의존.
  **→ 기각:** feature 격리 붕괴.

## 영향

**긍정**
- app이 `:impl`을 직접 참조하지 않고 DI로 조립 → 결합 최소.
- 백스택이 `ActivityRetainedScoped`라 설정 변경을 넘어 유지.

**트레이드오프**
- alpha 의존 → API 변경 위험. 버전 SoT는 카탈로그에 고정.

**위험·방어**
- 빈 백스택 접근 크래시를 가드(히스토리: backstack crash fix). 신규 화면 등록 절차는 [[navigation-flow]] 체크리스트로 표준화.
