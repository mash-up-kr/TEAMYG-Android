---
id: ADR-0009
title: UseCase는 주입 가능한 클래스 + operator invoke
status: accepted
date: 2026-06-21
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec:
related_architecture:
platforms: android
tags: [adr, parfait]
---
# ADR-0009: UseCase는 주입 가능한 클래스 + operator invoke

## 맥락
도메인 로직을 ViewModel에 직접 쓰면 재사용·테스트가 어렵고 레이어 경계가 흐려진다. `domain` 레이어(순수 Kotlin, [[0001-layered-multi-module]])에 비즈니스 동작을 담는 표준 단위가 필요했다.

## 결정
각 도메인 동작을 **인터페이스가 아닌 주입 가능한 단일 클래스**로 만들고 `operator fun invoke()`로 호출한다.

```
class GetRecentCacheImagesUseCase @Inject constructor(
    private val recentImageRepository: RecentImageRepository,
)
```

- 생성자에 `@Inject`, 의존은 Repository 인터페이스.
- 반응형 결과는 `Flow`로 반환(예: `Flow<List<String>>`).
- 예: `GetRecentCacheImagesUseCase`, `GetMyAccountFlowUseCase`, `GetMyGroupsUseCase`(2026-08-15 결선 라운드에서 `CheckInviteCodeValidUseCase` 등 mock UseCase 3종은 삭제됐다. 2026-08-16 PR #263이 `delay(1000)` mock이던 `SplashInitialUseCase`도 지웠다 — 최소 노출 시간 요구가 없어 자리채움을 남길 이유가 없었고, 진입은 `BootstrapSessionUseCase`가 대신한다).
- **`Flow`를 돌려주는 UseCase는 `Get…`이다**(`Observe…`가 아니다). 호출 자체는 구독하지 않고 `Flow`만 넘기며 구독 시점은 수집하는 쪽이 정하기 때문이다. 같은 대상에 일회성 갱신이 함께 있으면 이름으로 가른다 — `GetMyAccountFlowUseCase`(구독) vs `RefreshMyAccountUseCase`(1회 서버 갱신).

UseCase는 `domain`에, Repository 인터페이스도 `domain`, 구현은 `data`([[data-layer]]).

## 대안
- **UseCase 인터페이스 + 구현 분리** — 목킹 유연. 그러나 도메인은 이미 순수 Kotlin이라 JVM 테스트에 인터페이스 불필요.
  **→ 기각:** 보일러플레이트만 증가.
- **ViewModel에서 Repository 직접 호출** — 레이어 하나 절약.
  **→ 기각:** 도메인 로직 재사용·단위 테스트 지점 상실.

## 영향

**긍정**
- 도메인 동작이 명시적·단일 책임. 순수 Kotlin이라 프레임워크 없이 단위 테스트.
- `invoke`로 호출부가 함수처럼 간결.

**트레이드오프**
- 단순 위임(패스스루) UseCase가 생기기도 함 → 가치 없으면 만들지 않는다(판단 필요).

**위험·방어**
- ViewModel이 Repository를 우회 호출하지 않도록 리뷰에서 확인.
