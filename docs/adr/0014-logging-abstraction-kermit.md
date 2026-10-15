---
id: ADR-0014
title: 로깅 추상화 — Kermit 위임 Logger 인터페이스
status: accepted
date: 2026-07-18
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0013
related_spec:
related_architecture: module-structure
platforms: android
tags: [adr, parfait]
---
# ADR-0014: 로깅 추상화 — Kermit 위임 Logger 인터페이스

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.
> **문서화 시점 주의**: 이 추상화는 문서 기준선(`bd844a5`, 2026-07-16) 시점에 이미 코드에 존재했으나 ADR이 없었다. 2026-07-18 기준선 점검에서 미문서화 결정을 소급 기록(backfill)한 것.

## 맥락
`android.util.Log` 직접 호출은 태그 관리·테스트·로그 라이터 교체가 어렵고 모듈이 안드로이드 로깅 API에 결합된다. 로깅 진입점을 통일하고 백엔드(Kermit 등)를 감춰 교체 가능하게 만들 필요가 있었다.

## 결정
`core:util:jvm`의 `analytics` 패키지에 **자체 `Logger` 인터페이스 + `Loggers` 팩토리**를 두고, 구현은 **Kermit**(`co.touchlab:kermit`)에 위임한다.

- **`Logger` 인터페이스**: `v/d/i/w/e/a` 6레벨. 각 메서드 시그니처 `(throwable: Throwable? = null, tag: String? = null, message: () -> String)` — **메시지는 람다(lazy)**라 로그가 실제 출력될 때만 문자열 생성.
- **`KermitLoggerImpl`**(internal): `co.touchlab.kermit.Logger`에 위임. `tag`가 null이면 delegate의 tag 사용. `withTag`로 태그 파생 가능.
- **`Loggers.create(tag)`**: `KermitLoggerImpl(KermitLogger.withTag(tag))` 반환하는 팩토리(진입점).
- **`LoggerInitializer.setupDebug()`**: `Logger.setLogWriters(platformLogWriter())` — 디버그 로그 라이터 초기화.
- **모듈별 named logger 상수**: 각 모듈이 자기 스코프 로거를 `by lazy`로 노출 — `core:ui`(`viewModelLogger`·`screenLogger`), `data`·`domain`, 그리고 `core:util:android`(`coreUtilAndroidLogger`, 2026-08-25 PR #349 — EXIF 재개방 실패를 삼키는 자리가 생기면서 이 모듈도 로그를 남길 자리를 얻었다)도 동일 패턴. 모듈은 `Loggers.create("태그")`만 부르고 Kermit에 직접 의존하지 않는다.
  > 🔁 **`app`의 두 로거는 사라졌다(2026-08-22, PR #325)** — `fcmLogger`·`tokenLogger`가 유일하게
  > FCM 서비스와 토큰 조회만을 위한 것이라 그 기능이 걷히면서 `app/Logger.kt`째 지워졌다
  > ([ADR-0013](0013-firebase-fcm-crashlytics.md) 철회 정정). **패턴이 폐기된 것이 아니라 그 모듈에
  > 로그를 남길 자리가 없어진 것**이고, `app`이 다시 로그를 찍게 되면 같은 형태로 되살린다.
- 버전은 `gradle/libs.versions.toml`의 `kermit`, 별칭 `kermit`.

## 대안
- **`android.util.Log` 직접 사용** — 의존 0. 그러나 태그 관리·lazy 메시지·라이터 교체·테스트 대역 부재, 안드로이드 API 결합(순수 JVM 모듈에서 사용 불가).
  **→ 기각:** `core:util:jvm`은 안드로이드 비의존이어야 함(멀티모듈 경계, [[module-structure]]).
- **Timber** — 안드로이드 진영 표준. 그러나 안드로이드 의존이라 순수 JVM 모듈에 부적합, KMP 확장 여지 적음.
  **→ 기각:** Kermit이 멀티플랫폼·순수 Kotlin이라 `core:util:jvm`에 적합.
- **Kermit 직접 노출(래퍼 없음)** — 코드 최소. 그러나 전 모듈이 Kermit API에 결합 → 교체 시 광범위 수정.
  **→ 기각:** 얇은 인터페이스로 감싸 백엔드 교체 지점을 한 곳으로.

## 영향

**긍정**
- 모듈은 자체 `Logger`만 알고 Kermit 미노출 → 백엔드 교체가 `core:util:jvm` 한 모듈에 국한.
- lazy 메시지 람다로 비활성 레벨 로그의 문자열 비용 제거.
- 안드로이드 비의존이라 순수 JVM/테스트에서도 동일 API.

**트레이드오프**
- 얇지만 추가 간접층(인터페이스+Impl+팩토리) 유지 비용.
- `Logger`·`Loggers`가 `analytics` 패키지에 있으나 현재는 순수 로깅 — 실제 애널리틱스(이벤트 전송) 연동은 미구현(이름과 기능 범위 불일치 소지).

**위험·방어**
- `KermitLoggerImpl`는 `internal` — 모듈 외부는 `Logger` 인터페이스+`Loggers` 팩토리로만 접근.
- 프로덕션 로그 라이터 정책(릴리즈 시 로그 억제·Crashlytics 연동 등)은 `LoggerInitializer` 확장 여지로 남김 → [open-questions](../synthesis/open-questions.md).
