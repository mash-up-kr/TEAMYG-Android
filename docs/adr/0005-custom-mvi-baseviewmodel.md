---
id: ADR-0005
title: 자체 MVI 패턴 (BaseViewModel<State, Intent, SideEffect>)
status: accepted
date: 2026-05-09
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec:
related_architecture:
platforms: android
tags: [adr, parfait]
---
# ADR-0005: 자체 MVI 패턴 (BaseViewModel<State, Intent, SideEffect>)

## 맥락
화면 상태 관리 방식이 화면마다 제각각이면 리뷰·재사용·테스트가 어렵다. 초기(`feature/mvi base code`)에 단방향 데이터 흐름(UDF) 규약을 하나로 못박아야 했다.

## 결정
`core:ui`에 제네릭 베이스를 두고 모든 화면이 이를 상속한다.

```
abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(initialState: S) : ViewModel()
```

계약은 `MviContract`의 세 인터페이스:
- **UiState** — 불변 화면 상태(예: `LoginState`). `StateFlow<S>`로 노출.
- **UiIntent** — 사용자 행위/이벤트(예: `LoginIntent.LoginWithKakao`). 화면이 `processIntent(intent)`로 전달.
- **UiSideEffect** — 1회성 효과: 내비게이션·토스트 등(예: `LoginSideEffect.NavigateToNext`). `SharedFlow<E>`로 노출.

외부 MVI 프레임워크 없이 순수 coroutines(`StateFlow`/`SharedFlow`)로 구현. `@HiltViewModel`과 결합.

## 대안
- **외부 MVI 라이브러리(MVIKotlin/Orbit 등)** — 기능 풍부. 그러나 의존·개념 추가.
  **→ 기각:** 요구가 단순(state/intent/effect)해 자체 구현으로 충분.
- **ViewModel + 노출 StateFlow만(규약 없음)** — 자유롭지만 화면마다 편차.
  **→ 기각:** 일관성·리뷰 용이성 상실.

## 영향

**긍정**
- 모든 화면이 동일한 state/intent/effect 3분할 → 예측 가능, 리뷰·테스트 쉬움.
- 1회성 효과를 state와 분리(SharedFlow) → 재구성 시 중복 실행 방지.

**트레이드오프**
- 간단한 화면에도 3종 타입 선언 필요(보일러플레이트).

**위험·방어**
- side effect를 state에 넣는 안티패턴 방지 규칙을 [[state-management]] 체크리스트에 명시.
