---
id: ADR-0015
title: feature/common 공유 feature 레이어 도입
status: accepted
date: 2026-07-21
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0001, ADR-0002, ADR-0006
related_spec: feature-common-terms-module
related_architecture: module-structure
platforms: android
tags: [adr, parfait, module, common]
---
# ADR-0015: feature/common 공유 feature 레이어 도입

## 맥락
약관/개인정보 화면(S-004)이 처음엔 `:feature:app:setting` 안에 구현됐다. 그런데 **A-003 서비스
이용약관** 화면에서도 동일 화면을 세부 정보 뷰로 재사용해야 한다. 한 화면이 특정 feature 도메인
(app/setting) 소유로 남으면, 그 화면을 쓰려는 다른 feature가 소유 feature에 얽매인다.

기존 규칙([[0002-feature-api-impl-split]])상 feature 간 이동은 상대 `:api`(NavKey)만 참조하므로
"소비 feature → terms:api" 참조 자체는 문제없지만, **terms 화면을 어느 feature 밑에 둘지**가 애매하다
(setting 소유로 두면 의미상 왜곡, A-003 소속 feature에 두면 반대로 setting이 그쪽에 의존).

## 결정
특정 feature 도메인에 속하지 않고 **여러 feature가 공유**하는 feature 모듈을 위한 그룹
**`feature/common/*`** 를 신설한다. 첫 멤버는 **`:feature:common:terms:{api,impl}`**.

- 구조·컨벤션은 기존 feature와 동일: `:api`(NavKey 계약) + `:impl`(화면·VM·엔트리 빌더),
  각각 `ModuleFeatureApi`/`ModuleFeatureImpl` 컨벤션 플러그인.
- 소비 feature는 상대 `:impl`이 아니라 **`common:terms:api`(NavKey)만** 참조해 `goTo` — [[0002-feature-api-impl-split]] 그대로.
- 화면 렌더는 `common:terms:impl`의 엔트리 빌더가 `@IntoSet`으로 공급 → `MainRoute`가 자동 조립([[0006-navigation3-custom-navigator]]). app만 impl을 알면 됨.

## 대안
- **setting 소유 유지** — 현상. A-003 등 타 feature가 app/setting에 의미상 종속. **→ 기각**: 도메인 경계 왜곡.
- **core로 승격** — terms 화면을 core에 둠. **→ 기각**: core는 화면·NavKey·Hilt entry를 두는 레이어가 아님(디자인시스템·유틸·MVI 베이스 전용). feature 성격의 코드가 core로 새면 레이어 규칙 붕괴.
- **A-003 소속 feature로 이동** — 반대 방향 종속. **→ 기각**: 대칭적으로 setting이 그 feature에 의존하게 됨.

## 영향

**긍정**
- 공유 화면이 중립 위치(`feature/common`)에 놓여 여러 feature가 대등하게 `:api`로만 참조.
- 의존 방향 단방향 유지(`feature/*/impl → common/*/api`), impl 간 직접 의존 없음.

**트레이드오프**
- 모듈 그룹 하나 추가 → 개수 증가. 컨벤션 플러그인으로 세팅 비용 상쇄([[0003-convention-plugins-version-catalog]]).
- 정적 문자열 등 일부 리소스가 소비 feature와 common에 중복될 수 있음(모듈 독립성과의 교환).

**위험·방어**
- `common`이 잡동사니 모듈이 되지 않도록: **2개 이상 feature가 실제로 공유**할 때만 `common`에 둔다(단일 소비면 소유 feature에 유지). terms는 S-001 + A-003 두 소비처가 확정됐다.
