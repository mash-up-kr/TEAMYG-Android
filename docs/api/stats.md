---
id: stats
title: 운영용 가입자 통계
server_module: http/api/stats, core/stats
server_commit: 1a36480
verified: 2026-11-08
android_status: none
related_spec:
related_adr:
tags: [api, parfait, server-contract, stats]
---

# 운영용 가입자 통계 API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

**앱이 부르는 API가 아니다.** 운영진 도구가 가입자 수를 숫자로만 받아 가는 경로다. 같은 집계를 서버의
디스코드 봇(`external/discord`, 정기 리포트·슬래시 명령어)도 쓰지만 그쪽은 HTTP 계약이 아니라 여기 적지 않는다.
앱 모듈에 대응 심볼이 생기면 안 되는 표면이라 Android 열은 전부 `해당 없음`이다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| GET | `/admin/stats` | `X-Stats-Token` 정적 토큰(JWT 아님) | 쿼리 `from`·`to` | `SignupStatsResponse` | 해당 없음 |

## 엔드포인트 상세

### GET /admin/stats

- **인증**: `SecurityConfig` 화이트리스트(permitAll)라 JWT 필터를 거치지 않는다. 대신 컨트롤러 첫 줄의
  `StatsTokenVerifier#verify`가 `X-Stats-Token` 헤더를 설정 `stats.api-token`과 상수 시간 비교한다.
  설정이 비어 있으면 모든 요청이 401이다. `Authorization: Bearer`를 쓰지 않는 것은 `JwtAuthFilter`와
  부딪히지 않으려는 것이다.
- **노출**: API 호스트 앞단 Caddy가 이 경로를 막지 않는다 — 외부에서 닿고, 토큰이 유일한 방어선이다.
- **성공**: HTTP 200 · envelope `code` = `"OK"`
- **요청 필드**(쿼리)

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `from` | `LocalDate`(ISO `yyyy-MM-dd`) | ✅ | 그날 00:00부터 포함 |
| `to` | `LocalDate`(ISO `yyyy-MM-dd`) | ✅ | **그날 00:00 전까지** — `to` 날짜 자체는 들어가지 않는다 |

  `from < to`이고 간격이 366일 이하여야 한다. 하루치를 보려면 `to`를 다음 날로 준다.

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `newSignups` | Long | ❌ | 기간 안에 `created_at`이 든 회원 수 |
| `withdrawals` | Long | ❌ | 기간 안에 `deleted_at`이 든 회원 수 |
| `newSignupWithdrawals` | Long | ❌ | 기간 안에 가입하고 같은 기간 안에 탈퇴한 회원 수 |
| `netGrowth` | Long | ❌ | `newSignups - withdrawals` |
| `totalUsers` | Long | ❌ | `to` 시점에 가입돼 있고 탈퇴 전인 회원 수 |

  다섯 값 모두 설정 `report.stats.excluded-member-ids`(테스트·운영진 member id)를 뺀 수다. 탈퇴자까지 세려고
  `@SQLRestriction`을 우회하는 네이티브 쿼리(`SignupStatsAdapter`)를 쓴다. 시각은 서버 DB 기준이다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 401 | `INVALID_STATS_TOKEN` | 헤더 없음·불일치·서버 토큰 미설정 |
| 400 | `INVALID_STATS_PERIOD` | `from >= to` 또는 366일 초과 |
| 400 | `INVALID_REQUEST` | `from`·`to` 누락 또는 날짜 형식 아님(`GlobalExceptionHandler` bad-request 묶음) |

## Android 매핑

없음. 앱이 이 경로를 부를 이유가 없다.

## 미결

없음.
