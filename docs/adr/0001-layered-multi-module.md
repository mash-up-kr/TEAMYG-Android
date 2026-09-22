---
id: ADR-0001
title: 레이어드 다중 모듈 구조 (core / data / domain / feature)
status: accepted
date: 2026-04-19
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0003, ADR-0002
related_spec:
related_architecture:
platforms: android
tags: [adr, parfait]
---
# ADR-0001: 레이어드 다중 모듈 구조 (core / data / domain / feature)

## 맥락
단일 모듈로 시작하면 화면·비즈니스 로직·플랫폼 코드가 뒤섞여 빌드가 느려지고 경계가 무너진다. 프로젝트 초기부터(멀티모듈 base, `feature/multi-module-base`) 명시적 레이어와 모듈 경계를 세워야 했다.

## 결정
책임별로 모듈을 나누고 의존을 **단방향**으로 강제한다: `app` → `feature/*` → `core/*` → `data` / `domain`.

- **app / app-preview** — 진입점(`BaseApplication`, `MainActivity`). 모든 feature와 core를 조립.
- **feature/\*** — 화면·ViewModel·UI 로직. 서로 직접 의존하지 않고 필요한 경우 상대 feature의 `:api`만 참조.
- **core/\*** — `ui`(MVI 베이스·테마 스코프), `designsystem`(토큰·테마), `navigation`(Navigator·엔트리 레지스트리), `util:android`, `util:jvm`.
- **domain** — 순수 Kotlin(`kotlin-jvm`). UseCase·Repository 인터페이스·도메인 모델. Android 의존 없음.
- **data** — Repository 구현·DataSource·DI 모듈. `domain`과 core util에 의존.

모듈 등록 SoT = `settings.gradle.kts`. 레이어 규칙은 build-logic 컨벤션 플러그인이 강제한다([[0003-convention-plugins-version-catalog]]).

## 대안
- **단일 모듈** — 초기 세팅 간단. 그러나 빌드 캐시·병렬화 손실, 레이어 경계 붕괴.
  **→ 기각:** 팀·화면 수 증가 시 유지 불가.
- **레이어만 나누고 feature는 한 모듈** — feature 간 결합·증분 빌드 악화.
  **→ 기각:** feature 격리([[0002-feature-api-impl-split]])의 이점을 못 얻음.

## 영향

**긍정**
- 증분 빌드·병렬 빌드 이득. `domain`이 순수 Kotlin이라 JVM 단위 테스트 가능.
- 의존 방향이 컴파일 타임에 고정 — 레이어 역전 사고 방지.

**트레이드오프**
- 모듈·플러그인 보일러플레이트 증가 → 컨벤션 플러그인으로 상쇄.

**위험·방어**
- 모듈 그래프 시각화 Gradle task로 의존 드리프트 점검(히스토리: dependency graph task).
