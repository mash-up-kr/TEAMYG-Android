---
id: auth-reissue
title: 토큰 재발급
spec_source: 팀 노션 API 명세
spec_status: 완료
spec_issue: "#45"
server_commit: aa9cc9b
verified: 2026-09-04
related_api: auth.md
tags: [api, parfait, spec, auth]
---

# 토큰 재발급 (팀 명세)

> 팀이 합의한 **의도**입니다. 서버 코드의 현실은 [../auth.md](../auth.md) — 갈리는 지점은
> [코드 대조](#코드-대조)에 모았습니다.

- **Method / Path**: `POST /api/v1/auth/reissue`
- **구분**: 토큰 · **인증**: Refresh Token
- **설명**: Access 만료 시 Refresh로 새 토큰 발급 (Refresh 회전)

## 개요

Access Token이 만료되면 Refresh Token으로 새 토큰을 받는다. **재발급 시 Refresh도 새 걸로 교체되고
기존 건 폐기된다(회전).** (`ErrorCode.EXPIRED_TOKEN` 시)

## 요청

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `refreshToken` | string | ✅ | 발급받은 Refresh Token |

```json
{ "refreshToken": "eyJhbGciOi..." }
```

## 응답 200

새 Access + 새 Refresh (공통 토큰 응답).

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "expiresIn": 3600
}
```

## 상태 코드

명세의 상태 코드 표에는 **`code` 열이 없다**(다른 명세 페이지에는 있음).

| 코드 | 의미 |
|---|---|
| 200 | 재발급 성공 |
| 400 | `refreshToken` 누락/형식 오류 |
| 401 | Refresh 만료/위조/저장값 불일치 → 재로그인 필요 |
| 403 | 정지·탈퇴 회원 |

## 코드 대조

서버 `main` `6f5bffc` 기준.

### 일치

- 경로·메서드, 요청 1필드(`refreshToken` `@NotBlank`), 응답 3필드, 성공 200 `OK`
- **회전이 실제로 일어난다** — `ReissueService`가 새 access/refresh를 발급하고 `TokenSavePort.save`로
  Redis 값을 덮어써 기존 refresh를 폐기한다.
- 401 "만료/위조/저장값 불일치" — `EXPIRED_TOKEN`(JWT 만료) · `INVALID_TOKEN`(JWT 무효 또는 Redis
  저장값과 불일치·부재) 두 코드로 실현된다.

### 명세에만 있음

- **403 정지·탈퇴 회원** — 서버에 **없다.** `AuthErrorCode`에 정지·탈퇴에 해당하는 코드가 없고,
  `ReissueService`에는 회원 상태(정지/탈퇴) 검사 자체가 없다. 회원이 존재하지 않으면
  `MemberQueryPort.existsById` 실패로 **401 `MEMBER_NOT_FOUND`**를 던진다 — 명세가 말한 403이 아니다.
  → [open-questions](../../synthesis/open-questions.md)

### 코드에만 있음

- **401 `MEMBER_NOT_FOUND`** — 토큰의 `memberId`에 해당하는 회원이 없을 때. 명세의 403 "정지·탈퇴 회원"이
  의도한 상황과 겹칠 수 있으나 **HTTP 코드와 code 문자열이 모두 다르다.**
- 회전 시 **`sessionId`는 유지된다**(`claims.sessionId` 재사용). 세션 단위 로그아웃과 맞물리는 동작인데
  명세에 서술이 없다. **2026-09-02부터 그 세션이 새 access token의 클레임으로도 실린다**
  (`createAccessToken(memberId, sessionId)`) — 응답 3필드는 그대로이고 토큰 문자열 안의 내용만
  늘었다([../auth.md](../auth.md) "access token이 세션을 싣는다").
- envelope 5필드 → [conventions.md](../conventions.md)

### 표기 차이 (실질 불일치 아님)

- 명세의 **"인증: Refresh Token"**은 HTTP 인증 헤더를 뜻하지 않는다. `/api/v1/auth/reissue`는
  `SecurityConfig` 화이트리스트라 **헤더 없이 호출할 수 있다.** 단 `JwtAuthFilter`는
  `OncePerRequestFilter`를 `addFilterBefore`로 등록한 것이고 `shouldNotFilter` 오버라이드가 없어
  **화이트리스트 경로에서도 실행된다** — `permitAll`은 인가만 통과시킬 뿐 필터를 건너뛰지 않는다.
  `Authorization` 헤더를 붙이면 필터가 `validateAccessToken`으로 검증을 시도하므로, **만료된
  access token을 붙이면 401 `EXPIRED_TOKEN`이 나 재발급 자체가 막힌다.** 검증 자체는 요청
  **바디**의 `refreshToken`을 `ReissueService`가 직접 수행한다.

## Android 구현 시 주의

1. **`Authorization` 헤더를 붙이지 않는다.** refresh token은 **바디**로 보낸다. 화이트리스트라
   헤더 없이 호출할 수 있다 — 그게 이 API의 존재 이유다. 헤더를 붙이면 `JwtAuthFilter`가 여전히
   검증하므로, 만료된 access token을 붙이면 401이 나 재발급이 불가능해진다 — 헤더를 반드시 빼야
   한다(단순 생략 권장이 아니라 필수).
2. **회전이므로 응답의 새 refresh token을 반드시 저장해야 한다.** 기존 것은 서버에서 폐기돼 재사용하면
   `INVALID_TOKEN` 401이 난다. 저장 실패 시 사용자는 재로그인 외에 복구 수단이 없다.
3. 401을 받으면 **재로그인**으로 보낸다. `EXPIRED_TOKEN`·`INVALID_TOKEN`·`MEMBER_NOT_FOUND` 셋 다
   401이라 세부 분기 없이 동일 처리해도 된다.
4. **403은 오지 않는다**(현재 서버 기준). 명세만 보고 403 분기를 만들면 죽은 코드가 된다.

## 미결

- 명세의 403 정지·탈퇴 회원이 미구현인지, 회원 상태 개념 자체가 없는 것인지
  → [open-questions](../../synthesis/open-questions.md)
