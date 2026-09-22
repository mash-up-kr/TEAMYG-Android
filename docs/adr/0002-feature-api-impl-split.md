---
id: ADR-0002
title: feature 모듈을 :api / :impl로 분리
status: accepted
date: 2026-05-19
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0006
related_spec:
related_architecture:
platforms: android
tags: [adr, parfait]
---
# ADR-0002: feature 모듈을 :api / :impl로 분리

## 맥락
feature가 다른 feature로 이동(navigate)하려면 목적지를 알아야 한다. feature 전체를 서로 참조하게 두면 화면·ViewModel·UI까지 컴파일 의존이 걸려 결합이 커지고 증분 빌드가 무너진다.

## 결정
모든 feature를 `:api`와 `:impl` 두 모듈로 나눈다.

- **:api** — 공개 계약만. 목적지 `NavKey`(예: `NavKeyLogin`, `NavKeySegmentation`, `NavKeyCameraCustom`)뿐. `@Serializable`. `ModuleFeatureApiConventionPlugin`이 Android library + serialization + navigation3 번들(`libs.bundles.navigation`)을 적용 — NavKey가 navigation3 타입을 참조하므로 필요.
- **:impl** — 화면·ViewModel·컴포넌트·엔트리 빌더. `:api`와 core·domain에 의존. Compose + Hilt 적용(`ModuleFeatureImplConventionPlugin`).

한 feature가 다른 feature로 이동할 때는 상대의 `:impl`이 아니라 **`:api`(NavKey)만** 참조한다. 예: `feature/login/impl`은 `feature/intro/api`(`NavKeyTermAgree`)에, `feature/intro/impl`은 `feature/groups/list/api`(`NavKeyGroupList`)에 의존.

## 대안
- **feature 단일 모듈** — 구조 단순. 그러나 feature 간 이동 시 impl 전체 결합.
  **→ 기각:** 증분 빌드·결합도 악화.
- **중앙 라우트 레지스트리에 모든 NavKey 집중** — api 분리 없이 한 모듈에 목적지 모음.
  **→ 기각:** 그 모듈이 모든 feature 변경에 재컴파일되는 병목.

## 영향

**긍정**
- feature 간 결합이 `:api`(NavKey)로 최소화 → impl 변경이 다른 feature 재컴파일 유발 안 함.
- 엔트리 빌더를 DI로 주입([[0006-navigation3-custom-navigator]])해 `:impl`을 런타임 조립 → app이 impl을 직접 알 필요 없음.

**트레이드오프**
- feature마다 모듈 2개 → 개수 증가. 컨벤션 플러그인으로 세팅 비용 상쇄.

**위험·방어**
- `:api`에 UI/로직이 새어들지 않도록 api 컨벤션 플러그인이 의존을 제한.
