---
id: auth-kakao-login
title: 카카오 로그인 / 회원가입
spec_source: 팀 노션 API 명세
spec_status: 완료
spec_issue: "#48"
server_commit: aa9cc9b
verified: 2026-09-04
related_api: auth.md
tags: [api, parfait, spec, auth]
---

# 카카오 로그인 / 회원가입 (팀 명세)

> 팀이 합의한 **의도**입니다. 서버 코드의 현실은 [../auth.md](../auth.md) — 둘이 갈리는 지점은
> 아래 [코드 대조](#코드-대조)에 모았습니다.
>
> 명세 페이지의 코멘트 스레드는 옮기지 않습니다([README](README.md) 규약).

- **Method / Path**: `POST /api/v1/auth/kakao`
- **구분**: 로그인/가입 · **인증**: 불필요
- **설명**: 앱이 보낸 카카오 ID 토큰을 검증 후 회원 조회/가입, 우리 JWT 발급

## 개요

앱이 카카오 SDK로 받은 **ID 토큰(JWT)**을 백엔드로 전달하면, 백엔드가 **카카오 공개키(JWKS)**로 검증한 뒤
회원 식별 기준인 **`sub`(카카오 회원번호)**로 회원을 조회/가입하고 우리 서비스 JWT를 발급한다.

## 요청

`Content-Type: application/json`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `idToken` | string | ✅ | 카카오가 발급한 ID 토큰(JWT) |
| `nonce` | string | ✅ | 원본 nonce 값 (재생 공격 방어 검증용) |

**`nonce`는 프론트가 생성한다.** 로그인 직전에 앱이 직접 만들어 ① 카카오 SDK 로그인 요청에 전달하고
② **같은 값**을 본 API에 함께 보낸다. 서버는 ID 토큰의 `nonce` 클레임과 대조해 재생 공격을 검증한다.

```json
{
  "idToken": "eyJ0eXAiOiJKV1Qi...",
  "nonce": "a1b2c3d4"
}
```

## 응답 200

`isNewUser`로 분기한다(애플 로그인과 동일한 구조).

- **기존 회원**(`false`): access/refresh 발급
- **신규 유저**(`true`): `registrationToken`(10분)만 → 약관 동의 후 회원가입 완료 API 호출

| 필드 | 타입 | 설명 |
|---|---|---|
| `isNewUser` | boolean | 신규 가입 여부 |
| `accessToken` | string | 우리 서비스 Access JWT (**1시간**), 기존 회원만 |
| `refreshToken` | string | 우리 서비스 Refresh JWT (**2주**), 기존 회원만 |
| `expiresIn` | number | Access 만료(초), 기존 회원만 |
| `registrationToken` | string | 신규 유저 임시 가입 토큰 (**10분**), 신규 유저만 |

```json
// (a) 기존 회원
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "expiresIn": 3600,
  "isNewUser": false
}

// (b) 신규 유저
{
  "isNewUser": true,
  "registrationToken": "eyJhbGciOi..."
}
```

## 상태 코드

| 코드 | 의미 | code |
|---|---|---|
| 200 | 로그인/가입 성공 (신규 유저 포함) | `OK` |
| 400 | 필수 필드 누락/형식 오류 | `INVALID_REQUEST` |
| 401 | ID 토큰 검증 실패 (서명·만료·nonce 불일치 등) | `INVALID_ID_TOKEN` |
| 429 | 요청 한도 초과 | (명세에 code 미지정) |
| 502 | 카카오 JWKS 조회 실패 | `KAKAO_JWKS_FETCH_FAILED` |
| 503 | 카카오 서버 연결 불가·타임아웃 → 재시도 가능 | `KAKAO_SERVER_UNAVAILABLE` |

## 코드 대조

서버 `main` `e4ff23f` 기준(2026-08-15 재실행 — 카카오 로그인 계약 불변).

### 일치

- 경로·메서드·인증 불필요, 성공 200 `OK`, 요청 2필드(`idToken`·`nonce` 둘 다 `@NotBlank`)
- 응답 5필드와 `isNewUser` 분기 구조 — `KakaoLoginResult.ExistingMember` / `NewUser`
- **판별자 JSON 키 `isNewUser`** — 2026-08-11 정정으로 "실질 불일치"에서 여기로 옮겨 왔다(아래)
- 400 `INVALID_REQUEST` · 401 `INVALID_ID_TOKEN` · 502 `KAKAO_JWKS_FETCH_FAILED` ·
  503 `KAKAO_SERVER_UNAVAILABLE`
- **토큰 수명 3종이 설정값과 일치**한다 — `jwt.access-token-expiration-seconds` 3600(1시간),
  `jwt.refresh-token-expiration-seconds` 1209600(2주), `jwt.registration-token-expiration-seconds` 600(10분)

### 명세에만 있음

- **`nonce` 생성 책임이 앱에 있다는 사실.** 서버 코드는 `nonce`를 받아 대조할 뿐이라
  "누가 만드는가"를 코드에서 읽을 수 없다. **Android 구현에 직접 영향**([아래](#android-구현-시-주의)).
- **429 요청 한도 초과** — 서버에 대응 코드가 없다. `AuthErrorCode` 14종에 없고, rate limit 구현 흔적도
  코드에서 발견되지 않는다. **명세에만 존재하는 미구현 항목** → [open-questions](../../synthesis/open-questions.md)
- 토큰 수명을 **사람이 읽는 단위**로 표기(1시간·2주·10분). 코드에는 초 단위 숫자만 있다.

### 코드에만 있음

- `AuthErrorCode`의 나머지 코드들(`UNAUTHORIZED`·`INVALID_TOKEN`·`EXPIRED_TOKEN`·`MEMBER_NOT_FOUND`·
  `ALREADY_REGISTERED`·`DUPLICATE_TERMS_ID`·`TERMS_NOT_FOUND`·`REQUIRED_TERMS_NOT_AGREED`·
  `FORBIDDEN_REFRESH_TOKEN`·`APPLE_SERVER_ERROR`·`APPLE_SERVER_UNAVAILABLE`)은 이 엔드포인트 명세에
  열거되지 않았다. 대부분 다른 엔드포인트 소관이다.
- **애플 로그인**(`POST /api/v1/auth/apple`)이 2026-08-11 delta로 들어왔다. 이 명세는 카카오만 다루고
  애플 전용 명세 문서는 아직 없다 — 계약은 [../auth.md](../auth.md)에 있다.
- envelope 5필드(`success`·`code`·`message`·`data`·`errorDetail`) — 명세의 JSON 예시는 `data` 안쪽만
  보여준다. **실제 응답은 envelope로 한 겹 감싸여 온다** → [conventions.md](../conventions.md)

### 🔁 철회된 불일치 — 판별자 JSON 키는 명세가 맞았다

**2026-08-02~08-10 판본은 이 자리에 "명세는 `isNewUser`인데 실제 키는 `newUser`"라는 실질 불일치를
적었다. 그 판단이 틀렸다** — 근거를 OpenAPI 스키마 하나에만 뒀기 때문이다.

서버 `http` 모듈이 `jackson-module-kotlin`을 쓰므로 `is` 접두사가 JSON 키에 남고,
`KakaoLoginControllerTest`가 실제 응답 본문에 `jsonPath("$.data.isNewUser")`를 단언한다.
**명세 예시 JSON을 그대로 응답 타입으로 옮기는 것이 옳다.** 상세는
[../conventions.md](../conventions.md) "직렬화 규약"·[../auth.md](../auth.md) "판별자 키".

⚠️ 다만 **Android가 이미 틀린 쪽을 따라 구현했다** — `KakaoLoginResponse.isNewUser`에
`@SerialName("newUser")`가 붙어 있어 정정이 필요하다 → [open-questions](../../synthesis/open-questions.md).

### 표기 차이 (실질 불일치 아님)

- 명세 본문이 회원가입 완료 API를 `POST /auth/signup`으로 축약한다. 실제 경로는
  `POST /api/v1/auth/signup`이다.
- 명세가 서술한 흐름·필드 의미는 코드와 일치한다. 위 판별자 키 외에 이름이 갈리는 필드는 없다.

## Android 구현 시 주의

1. **`nonce`를 앱이 만들어야 한다.** 서버가 주지 않는다. 로그인 직전 생성 → 카카오 SDK 로그인 요청에
   전달 → **같은 값**을 이 API 요청 바디에 넣는다. 두 값이 다르면 서버가 401 `INVALID_ID_TOKEN`으로
   거절한다(재생 공격 방어). 값을 재사용하지 않는다.
2. **응답 필드가 분기한다.** `isNewUser`를 먼저 읽고 어느 묶음이 채워졌는지 판정한다. 신규 유저 응답에는
   `accessToken`이 없으므로 무조건 꺼내면 null이다.
3. **신규 유저는 로그인이 끝난 게 아니다.** `registrationToken`(10분)을 들고 약관 동의를 거쳐
   `POST /api/v1/auth/signup`까지 마쳐야 access/refresh를 받는다. 10분 만료를 UI 흐름에서 고려한다.
4. 실제 응답은 envelope에 감싸여 오므로 명세의 JSON 예시를 그대로 응답 타입으로 만들면 안 된다.

## 미결

- 429 요청 한도 초과가 서버 미구현 상태인 것이 의도인지 → [open-questions](../../synthesis/open-questions.md)
