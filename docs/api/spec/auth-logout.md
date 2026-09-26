---
id: auth-logout
title: 로그아웃
spec_source: 팀 노션 API 명세
spec_status: 완료
spec_issue: "#45"
server_commit: aa9cc9b
verified: 2026-09-04
related_api: auth.md
tags: [api, parfait, spec, auth]
---

# 로그아웃 (팀 명세)

> 팀이 합의한 **의도**입니다. 서버 코드의 현실은 [../auth.md](../auth.md) — 갈리는 지점은
> [코드 대조](#코드-대조)에 모았습니다.

- **Method / Path**: `POST /api/v1/auth/logout`
- **구분**: 세션 · **인증**: Bearer (Access)
- **설명**: 서버에 저장된 Refresh Token 삭제

## 개요

서버에 저장된 Refresh Token을 삭제한다. Access는 수명이 짧아 곧 만료된다.

## 요청

`Authorization: Bearer {accessToken}`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `refreshToken` | string | Y | 삭제 대상 Refresh Token |

```json
{ "refreshToken": "eyJhbGciOi..." }
```

## 응답 204

본문 없음.

## 상태 코드

| 코드 | 의미 | code |
|---|---|---|
| 204 | 로그아웃 성공 (이미 삭제된 세션이어도 204) | `NO_CONTENT` |
| 400 | `refreshToken` 형식 오류 | `INVALID_REQUEST` |
| 401 | Access Token 무효 | `INVALID_TOKEN` |
| 403 | 다른 회원의 Refresh Token | `FORBIDDEN_REFRESH_TOKEN` |

## 코드 대조

서버 `main` `6f5bffc` 기준.

### 일치

- 경로·메서드, `Authorization: Bearer` 필요(화이트리스트 밖 — 인증 도메인 4개 중 유일), 요청 1필드
- 성공 **204**, 본문 없음
- **멱등하다** — `LogoutService`가 존재 확인 없이 `TokenDeletePort.delete`만 호출하므로 이미 삭제된
  세션이어도 204다. 명세 서술과 일치한다.
- 403 `FORBIDDEN_REFRESH_TOKEN` — `LogoutService`가 `claims.memberId != memberId`일 때 던진다.
  즉 **access token의 주인과 바디 refresh token의 주인이 다르면** 403이다.

### 명세에만 있음

- **`code` = `NO_CONTENT`** — 서버에 그런 코드가 **없다.** envelope의 `code`는 `ApiResponse.ok`(`"OK"`)와
  `ApiResponse.created`(`"CREATED"`) 두 값뿐이고 `CommonErrorCode`에도 없다. 애초에 이 응답은
  **envelope 자체가 오지 않는다** — `LogoutController.logout`은 반환 타입이 없는(Unit) 함수이고
  `@ResponseStatus(HttpStatus.NO_CONTENT)`만 붙어 있어 응답 본문이 비어 있다. **명세 표기 오류다.**

### 코드에만 있음

- **로그아웃이 refresh token만 지우지 않는다**(2026-09-02). `LogoutService`가 세션 삭제 직후
  `DeviceTokenDeletePort.delete(memberId, sessionId)`로 **그 로그인 세션이 등록한 기기(FCM) 토큰 행도
  지운다** — 명세는 "서버에 저장된 Refresh Token 삭제"만 적는다.
  요청·응답·상태 코드는 그대로여서 **명세와 어긋나지는 않고, 명세가 덜 적은 것**이다
  → [../notification.md](../notification.md).
- **401은 한 코드가 아니다.** `Authorization` 헤더가 아예 없으면 `JwtAuthFilter`가 그냥 통과시키고
  `SecurityConfig.authenticationEntryPoint`가 **`UNAUTHORIZED`**를 던진다. 헤더가 있고 토큰이 무효면
  `validateAccessToken`이 **`INVALID_TOKEN`**·**`EXPIRED_TOKEN`**을, 토큰은 유효하나 회원이 없으면
  `JwtAuthFilter`가 **`MEMBER_NOT_FOUND`**를 던진다. 명세의 단일 `INVALID_TOKEN` 표기는 일부만 맞다.
- **바디 refresh token 검증 실패**가 명세에 없다. `LogoutService`가 memberId를 비교하기 **전에**
  `TokenValidatePort.validateRefreshToken`을 돌리므로, 바디의 refresh token이 만료·위조면
  403이 아니라 **401 `EXPIRED_TOKEN`/`INVALID_TOKEN`**이 먼저 난다.

## Android 구현 시 주의

1. **응답 본문이 없다.** envelope를 기대하고 파싱하면 실패한다. `safeApiCallWithoutData`조차
   `ApiResponse<Unit>`를 파싱하려 하므로 이 엔드포인트에는 맞지 않는다 — 본문 없는 204 전용 처리 경로가
   필요하다([conventions.md](../conventions.md) Android 불일치 참고).
2. **access token과 refresh token을 둘 다 보내야 한다.** 헤더에 access, 바디에 refresh. 한쪽만으로는
   호출되지 않는다.
3. **만료된 refresh token으로는 로그아웃할 수 없다.** 서버가 먼저 JWT 검증을 하므로 401이 난다.
   로컬 토큰을 이미 버린 상태라면 서버 세션이 남는다 — 로컬 정리와 서버 호출의 순서를 정해야 한다.
4. 로그아웃 실패를 사용자에게 막지 않는 편이 낫다. 멱등이고, access는 곧 만료된다.

## 미결

- 로컬 토큰 정리와 서버 로그아웃 호출의 순서·실패 시 정책은 앱 결정 사항이다(서버 계약 밖).
- 기기 토큰 정리가 **세션 단위**라, 세션 없이 등록된 행(구 access token 사용분)은 로그아웃으로 지워지지
  않는다. 명세에 이 갈래가 없다 → [open-questions](../../synthesis/open-questions.md).
