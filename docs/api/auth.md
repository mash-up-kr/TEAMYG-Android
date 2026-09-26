---
id: auth
title: 인증(카카오·애플 로그인·회원가입·토큰 재발급·로그아웃)
server_module: http/auth
server_commit: aa9cc9b
verified: 2026-09-04
android_status: done
related_spec: a002-kakao-login-api, session-token-refresh-infra
related_adr: ADR-0017
tags: [api, parfait, server-contract, auth]
---

# 인증(카카오·애플 로그인·회원가입·토큰 재발급·로그아웃) API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/kakao` | 불필요(화이트리스트) | `KakaoLoginRequest` | `KakaoLoginResponse` | **결선됨**[^newuser] |
| POST | `/api/v1/auth/apple` | 불필요(화이트리스트) | `AppleLoginRequest` | `AppleLoginResponse` | 해당 없음[^apple] |
| POST | `/api/v1/auth/signup` | 불필요(화이트리스트) | `SignupRequest` | `SignupResponse` | **결선됨**(PR #242, 온보딩 약관) |
| POST | `/api/v1/auth/reissue` | 불필요(화이트리스트) | `ReissueRequest` | `ReissueResponse` | **결선됨**[^reissue] |
| POST | `/api/v1/auth/logout` | **필요**(화이트리스트 밖) | `LogoutRequest` | 없음(204, envelope 없음) | **결선됨**[^logout] |

[^newuser]: **2026-08-14 해소** — `@SerialName("isNewUser")`로 정정하고 실제 JSON 본문을 디코딩하는
와이어 계약 테스트(`KakaoLoginResponseSerializationTest`)를 붙였다. 되돌리면 그 테스트가
`MissingFieldException`으로 깨진다. 같은 라운드가 Repository·UseCase·화면까지 결선했다
(PR #241 `80895eb1`, 2026-08-15 develop 머지)
→ [a002-kakao-login-api 스펙](../superpowers/specs/archive/2026-08-13-a002-kakao-login-api.md).
**단 실서버 응답으로는 아직 확인하지 못했다** — 아래 [판별자 키](#판별자-키는-isnewuser다).

[^reissue]: **2026-08-15 결선(PR #260)** — 호출부는 화면이 아니라 `TokenAuthenticator`다. 401을 가로채
이 엔드포인트로 재발급한 뒤 원요청을 다시 만든다. 거절(401 또는 본문 `code`가 `INVALID_TOKEN`·
`EXPIRED_TOKEN`·`FORBIDDEN_REFRESH_TOKEN`)만 세션을 끝내고 네트워크 실패·5xx는 토큰을 유지한다
→ 아래 [Android 매핑](#android-매핑).

[^logout]: **2026-08-15 결선(PR #260)** — `AuthRepository.logout()` → `LogoutUseCase` → S-001 앱 설정.
화이트리스트 밖이라 **만료 상태의 로그아웃이 `TokenAuthenticator`를 한 번 탄다**는 점이 이 엔드포인트
고유의 문제를 만든다(refresh token 회전 → 재전송) → 아래 [Android 매핑](#android-매핑).

[^apple]: **Android는 애플 로그인을 쓰지 않기로 결정했다**(2026-08-11). 서버에는 있으나 앱 대응
심볼을 만들지 않으며 `http/auth.http`에도 요청을 넣지 않는다 — `미구현`(아직 없음)이 아니라
닫힌 결정이다. 근거는 [member·parfait-image 서비스 레이어 스펙](../superpowers/specs/archive/2026-08-11-member-parfait-image-api-service-layer.md)
"범위". iOS가 붙을 때 이 계약은 그대로 유효하다.

⚠️ **`logout`만 화이트리스트 밖이라는 비대칭.** `[Feat/#45] 토큰 재발급(refresh) / 로그아웃 API 구현 (#63)`
(`6f5bffc`)이 기존 `/api/v1/auth/**` 와일드카드를 `/api/v1/auth/kakao`·`/api/v1/auth/signup`·
`/api/v1/auth/reissue` 개별 3경로로 좁히면서(`SecurityConfig.WHITELIST_PATHS`) `logout`을 제외했다.
`[Feat/#50] 애플 로그인 API 구현 (#76)`(`96affd0`)이 같은 방식으로 `/api/v1/auth/apple`을 더했다 —
인증 도메인 5개 엔드포인트 중 access token이 필요한 유일한 엔드포인트가 여전히 `logout`이다.

## 판별자 키는 `isNewUser`다

카카오·애플 로그인 응답이 공유하는 사실이라 앞으로 뺀다. **이 문서의 2026-08-02~08-10 판본은 이 키를
`newUser`로 적었고, 그것이 틀렸다.**

- **서버 DTO**는 `KakaoLoginResponse`·`AppleLoginResponse` 둘 다 Kotlin `val isNewUser: Boolean`이다.
- **직렬화 결과는 `isNewUser`**다. 서버 `http` 모듈이 `tools.jackson.module:jackson-module-kotlin`을
  의존하고, 이 모듈이 붙으면 Jackson이 getter 이름이 아니라 **주 생성자 파라미터명**으로 프로퍼티를
  잡아 `is` 접두사가 살아남는다. `KakaoLoginControllerTest`·`AppleLoginControllerTest`가 실제 직렬화된
  본문에 대해 `jsonPath("$.data.isNewUser")`를 단언한다 — 키가 `newUser`였다면 두 테스트가 깨진다.
- **팀 명세도 `isNewUser`로 적었다**([spec/auth-kakao-login.md](spec/auth-kakao-login.md)).
- **OpenAPI 스키마만 `newUser`**로 적는다(2026-08-02 실물 확인). springdoc은 swagger-core의 자체
  ObjectMapper로 모델을 유도하는데 거기엔 Kotlin 모듈이 없어, getter `isNewUser()`에서 `is`를 떼는
  bean 규칙이 적용된다. **런타임 직렬화와 다른 결과**다.

**규칙 적용**: [conventions.md](conventions.md)의 "두 근거가 갈리면 코드가 정본"이 그대로 적용된다.
직렬화 키는 코드가 답하지 못한다고 봤던 것이 이전 판단의 출발점이었는데, **컨트롤러 테스트가 답한다** —
MockMvc 본문 단언은 실제 직렬화 결과다.

**Android 영향**: ✅ **2026-08-14 정정됨.** `data/service/model/response/auth/KakaoLoginResponse.kt`가
`@SerialName("newUser")`를 붙이고 있어 응답 키를 못 찾고 `MissingFieldException` → `ApiException.Unknown`으로
**로그인이 통째로 실패**하는 상태였다. `@SerialName("isNewUser")`로 고쳤다.

이 결함은 컴파일·ktlint·Hilt 그래프 어디에도 걸리지 않는 종류였고, **DataSource 테스트로도 못
잡는다** — 서비스를 MockK로 대체해 JSON을 지나지 않기 때문이다. 실제 본문을 디코딩하는
`KakaoLoginResponseSerializationTest`가 유일한 그물이라 그 형태로 붙였다.

## access token이 세션을 싣는다(2026-09-02)

`JwtTokenAdapter.createAccessToken`이 **refresh token과 같은 `sessionId`를 클레임으로 담기 시작했고**,
`TokenValidatePort.validateAccessToken`의 반환이 `Long`에서 `AccessTokenClaims(memberId, sessionId?)`로
바뀌었다. 발급 지점 넷(카카오·애플 로그인, 회원가입 완료, 토큰 재발급)이 전부 세션을 넘겨 준다.

**와이어 계약은 안 바뀐다.** 네 엔드포인트의 요청·응답 필드도, `Authorization: Bearer` 형식도,
토큰 수명도 그대로다 — 앱이 읽는 자리에서 보이는 변화가 **없다**. 토큰 문자열 안에 클레임이 하나
늘었을 뿐이고, 이 클레임을 쓰는 주체는 서버다(`JwtAuthFilter`가 인증 객체의 credentials 슬롯에 실어
컨트롤러에 넘긴다 — [conventions.md](conventions.md) "인증").

⚠️ **이 변경 전에 발급된 access token에는 클레임이 없다.** 그때는 `sessionId`가 널로 채워지고
**인증은 그대로 통과한다**(과도기 설계). 널이어서 갈리는 것은 인증이 아니라 **기기 토큰 정리 범위**다
→ [notification.md](notification.md).

## 엔드포인트 상세

### POST /api/v1/auth/kakao

- **인증**: 불필요(화이트리스트 — `/api/v1/auth/kakao`·`/api/v1/auth/apple`·`/api/v1/auth/signup`·
  `/api/v1/auth/reissue` 개별 등록, `logout`은 제외 → 위 비대칭 참고)
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `idToken` | String | 필수(`@NotBlank`) | 카카오 ID 토큰 |
| `nonce` | String | 필수(`@NotBlank`) | **앱이 생성한다** — 로그인 직전 만들어 카카오 SDK 요청과 이 API에 같은 값을 보낸다. 서버가 ID 토큰 `nonce` 클레임과 대조해 재생 공격을 검증한다(근거: 팀 명세, [spec/auth-kakao-login.md](spec/auth-kakao-login.md)) |

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| **`isNewUser`** | Boolean | 아니오 | 아래 필드 묶음 중 어느 쪽이 채워졌는지를 결정하는 판별자. 키에 `is` 접두사가 **그대로 남는다** → [판별자 키](#판별자-키는-isnewuser다) |
| `accessToken` | String? | 예 | `isNewUser=false`일 때만 값 있음 |
| `refreshToken` | String? | 예 | `isNewUser=false`일 때만 값 있음 |
| `expiresIn` | Long? | 예 | 단위 초(`jwt.access-token-expiration-seconds`, 기본값 3600). `isNewUser=false`일 때만 값 있음 |
| `registrationToken` | String? | 예 | `isNewUser=true`일 때만 값 있음 |

  **응답이 분기한다.** `KakaoLoginResult.ExistingMember`(기존 회원) → `isNewUser=false` +
  `accessToken`·`refreshToken`·`expiresIn`, `registrationToken`은 `null`. `KakaoLoginResult.NewUser`(신규) →
  `isNewUser=true` + `registrationToken`, 나머지 셋은 `null`. 서버 설정이
  `spring.jackson.default-property-inclusion: always`라 **채워지지 않는 쪽도 키는 `null`로 실려 온다.**

  ✅ **2026-08-19 — 탈퇴한 계정으로 다시 가입하는 경로가 고쳐졌다.** 계약은 안 바뀌었고 바뀐 것은
  성공 여부다 — 탈퇴 시 `provider_user_id`를 tombstone 값으로 덮는 UPDATE가 같은 트랜잭션의 delete
  때문에 DB에 안 닿아, **같은 계정으로 재가입하면 유니크 제약 위반 500**이 났다([member.md](member.md)
  탈퇴 절). 이 신규 가입 분기(`isNewUser=true` → `signup`)가 그 수정의 수혜자다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 401 | `INVALID_ID_TOKEN` | 유효하지 않은 ID 토큰입니다 |
| 502 | `KAKAO_JWKS_FETCH_FAILED` | 카카오 공개키 조회에 실패했습니다 |
| 503 | `KAKAO_SERVER_UNAVAILABLE` | 카카오 서버에 연결할 수 없습니다 |

  근거: `KakaoLoginControllerTest`가 세 코드를 이 엔드포인트에서 직접 검증한다. 던지는 지점은
  `KakaoIdTokenVerifyAdapter`(idToken 검증 어댑터, `external` 모듈).

- **명세 델타** — 팀 명세([spec/auth-kakao-login.md](spec/auth-kakao-login.md))가 이 엔드포인트에
  **429 요청 한도 초과**를 열거하나 서버에 대응 코드가 없다(`AuthErrorCode` 14종에 없고 rate limit 구현
  흔적도 없음). 명세가 코드에서 읽을 수 없는 것을 하나 더 담고 있다 — 위 `nonce` 생성 책임이다.
  판별자 키는 **명세 쪽이 맞았다**(위 참고).

### POST /api/v1/auth/apple

`[Feat/#50] 애플 로그인 API 구현 (#76)`(`96affd0`)으로 신설됐다. **카카오와 요청·응답 구조가 같다.**

> 🔁 **2026-08-15 계약 축소.** `refactor: 애플 로그인 authorizationCode 교환 로직 제거 (#89)`가
> 요청 필드 `authorizationCode`와 애플 토큰 서버 교환·client secret 생성·애플 refresh token 저장을
> **통째로 걷어냈다**. 서버 커밋 메시지가 사유를 밝힌다 — 교환이 `invalid_client`로 계속 실패해 로그인
> 자체가 막혀 있었고, 그 refresh token은 탈퇴 시 애플 revoke 용도였을 뿐 ID 토큰 검증(실제 인증)과는
> 무관했다. 마이그레이션 `V10__drop_apple_refresh_token_from_member.sql`이 컬럼도 지웠다.
> **이 문서의 2026-08-11~08-14 판본이 적은 3필드 요청·refresh token 저장 서술은 더 이상 사실이 아니다.**

- **인증**: 불필요(화이트리스트 — `SecurityConfig.WHITELIST_PATHS`에 `/api/v1/auth/apple` 개별 등록)
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `identityToken` | String | 필수(`@NotBlank`) | 애플 ID 토큰(JWT) |
| `nonce` | String | 필수(`@NotBlank`) | **앱이 생성한 원문 그대로** 보낸다 — 아래 참고 |

  ⚠️ **`nonce`는 해시하지 않고 원문을 보낸다.** `AppleIdTokenVerifyAdapter`가 받은 값을 **서버에서
  SHA-256 hex로 변환한 뒤** ID 토큰의 `nonce` 클레임과 비교한다. 애플 SDK에는 해시를 넘기고 이 API에는
  원문을 넘겨야 한다는 뜻이다 — 앱이 이미 해시한 값을 보내면 이중 해시가 돼 `INVALID_ID_TOKEN`이 난다.
  카카오는 서버가 해시하지 않으므로 **두 로그인의 `nonce` 취급이 다르다.**

  **요청 필드가 카카오와 같은 2개다**(카카오는 `idToken`·`nonce`, 애플은 `identityToken`·`nonce` —
  이름만 다르다). 서버가 애플 토큰 서버를 호출하는 경로는 이제 **JWKS 조회 하나뿐**이다.

- **응답 필드** — 카카오와 필드 집합·분기 규칙이 같다.

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| **`isNewUser`** | Boolean | 아니오 | 판별자. 카카오와 같은 키 규칙 → [판별자 키](#판별자-키는-isnewuser다) |
| `accessToken` · `refreshToken` | String? | 예 | `isNewUser=false`일 때만 값 있음 |
| `expiresIn` | Long? | 예 | 단위 초. `isNewUser=false`일 때만 값 있음 |
| `registrationToken` | String? | 예 | `isNewUser=true`일 때만 값 있음 |

  **분기 처리가 카카오와 완전히 같아졌다.** `AppleLoginService`가 ID 토큰에서 얻은 `sub`로 회원을 찾아
  있으면 세션(access·refresh)을 만들고, 없으면 `createRegistrationToken(provider, providerUserId)`만
  내려준다. **provider 전용 저장 데이터가 더는 없다** — registration token 클레임도
  `RegistrationTokenClaims(provider, providerUserId)` 둘뿐이다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 세 필드 중 하나가 공백(`@NotBlank` → `CommonErrorCode`) |
| 401 | `INVALID_ID_TOKEN` | ID 토큰 서명·`iss`·`aud`·`nonce`·`exp`·`sub` 검증 실패 |
| 502 | `APPLE_SERVER_ERROR` | 애플 JWKS 응답 오류·파싱 실패 |
| 503 | `APPLE_SERVER_UNAVAILABLE` | 애플 JWKS 조회 연결 실패(`IOException`) |

  🔁 **2026-08-15 원인 축소.** 이전 판본이 적은 "`authorizationCode` 교환 거부"·"응답에 `refresh_token`
  없음"·"토큰 교환 `ResourceAccessException`" 경로는 교환 로직과 함께 사라졌다. 세 코드의 원인이 전부
  **ID 토큰 검증 한 갈래**(`AppleIdTokenVerifyAdapter`)로 좁혀졌다.
  근거: `AppleLoginControllerTest`가 성공 2케이스 + 400·401·502·503을 직접 검증한다.

- **명세 델타** — 팀 명세에 애플 로그인 전용 문서가 아직 없다(`spec/`에 카카오·회원가입·재발급·로그아웃
  4건뿐). 회원가입 명세가 흐름에 `/auth/apple`을 적어 둔 것이 유일한 언급이다
  → [spec/auth-signup.md](spec/auth-signup.md).

### POST /api/v1/auth/signup

- **인증**: 불필요(화이트리스트 — `/api/v1/auth/kakao`·`/api/v1/auth/apple`·`/api/v1/auth/signup`·
  `/api/v1/auth/reissue` 개별 등록, `logout`은 제외 → 위 비대칭 참고)
- **성공**: HTTP **201** · envelope `code` = `"CREATED"`(`ResponseEntity.status(CREATED)` + `ApiResponse.created`)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `registrationToken` | String | 필수(`@NotBlank`) | **카카오 또는 애플** 로그인이 신규 판정 시 내려준 토큰. provider는 이 토큰의 클레임에서 나온다 — 요청 필드로 보내지 않는다 |
| `agreements` | List<`TermsAgreementRequest`> | 필수(`@Valid`) | 원소: `termsId` Long · `agreed` Boolean. **`termsId`는 `GET /api/v1/policies`가 내려주는 값**([policy.md](policy.md)) — 하드코딩하면 약관 개정 시 `TERMS_NOT_FOUND` 400 |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `accessToken` | String | 아니오 | 로그인 응답과 달리 셋 다 널 아님 |
| `refreshToken` | String | 아니오 | |
| `expiresIn` | Long | 아니오 | 단위 초(`jwt.access-token-expiration-seconds`, 기본값 3600) |

  흐름 메모: 로그인이 신규로 판정해 내려준 `registrationToken`을 이 API에 넘겨 가입을 끝낸다.
  회원가입 실패 시 회원 데이터가 남지 않도록 `SignupService.signup` 전체가 하나의 트랜잭션이다(#63에서
  `MemberRegistrar.register` 단독 트랜잭션 범위를 확장).

  🔁 **2026-08-15 — provider별 후처리가 사라졌다.** 이전 판본이 적은
  `handleProviderSpecificRegistration`(애플 refresh token 저장, 클레임 부재 시 401)은 #89가 통째로
  제거했다. **가입 경로는 이제 provider와 무관하게 하나**다 — 약관 검증 → 닉네임 자동 생성 → 회원 등록 →
  세션 발급.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 401 | `INVALID_TOKEN` | 유효하지 않은 토큰입니다(`registrationToken` 검증 실패) |
| 401 | `EXPIRED_TOKEN` | 만료된 토큰입니다(`registrationToken` 만료) |
| 409 | `ALREADY_REGISTERED` | 이미 가입된 회원입니다 |
| 400 | `DUPLICATE_TERMS_ID` | 중복된 약관 ID입니다 |
| 400 | `TERMS_NOT_FOUND` | 존재하지 않는 약관입니다 |
| 400 | `REQUIRED_TERMS_NOT_AGREED` | 필수 약관에 모두 동의해야 합니다 |

  근거: `SignupControllerTest`가 여섯 코드를 이 엔드포인트에서 직접 검증한다. `INVALID_TOKEN`·`EXPIRED_TOKEN`은
  `SignupService`가 `TokenValidatePort.validateRegistrationToken`을 통해 던지고, 나머지 넷은 `SignupService`
  본문(약관 검증·중복 가입 검사)에서 직접 던진다.

- **명세 델타** — 팀 명세([spec/auth-signup.md](spec/auth-signup.md))와 코드가 에러 7종·검증 순서까지
  일치한다. **명세에 없는 코드 동작 2건**: ① 가입 시 서버가 `RandomNicknameGenerator.generate()`로
  **닉네임을 자동 생성**한다(앱은 보내지도 받지도 않는다 — 응답에 닉네임 필드가 없다. 이 값을 나중에
  바꾸는 경로가 [member.md](member.md)다), ② 애플 분기가 애플 refresh token 저장으로 채워지면서
  `INVALID_TOKEN`을 던지는 경로가 하나 늘었다(위 흐름 메모) — 명세에는 없다.
  **2026-08-15 정정**: ②(애플 분기가 `INVALID_TOKEN` 경로를 하나 늘렸다)는 #89로 되돌아갔다 —
  provider 분기 자체가 없어졌으므로 명세와 코드의 이 항목 차이도 사라졌다.

### POST /api/v1/auth/reissue

- **인증**: 불필요(화이트리스트 — `/api/v1/auth/kakao`·`/api/v1/auth/apple`·`/api/v1/auth/signup`·
  `/api/v1/auth/reissue` 개별 등록, `logout`은 제외 → 위 비대칭 참고)
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `refreshToken` | String | 필수(`@NotBlank`) | |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `accessToken` | String | 아니오 | 새로 발급된 access token |
| `refreshToken` | String | 아니오 | 새로 발급된 refresh token(회전) |
| `expiresIn` | Long | 아니오 | 단위 초(`jwt.access-token-expiration-seconds`, 기본값 3600) — access token 만료까지의 초 |

  흐름 메모: 요청 refresh token을 JWT로 검증한 뒤 Redis에 저장된 값과 대조해 위조·재사용을 걸러내고,
  같은 세션(`sessionId`)으로 access/refresh 토큰을 새로 발급한다. 기존 refresh token은 Redis 값을
  덮어쓰는 방식으로 폐기된다(`ReissueService`).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 401 | `INVALID_TOKEN` | 유효하지 않은 토큰입니다 |
| 401 | `EXPIRED_TOKEN` | 만료된 토큰입니다 |
| 401 | `MEMBER_NOT_FOUND` | 존재하지 않는 회원입니다 |

  `INVALID_TOKEN`은 두 경로에서 나온다 — (1) refresh token이 JWT로서 무효(타입 불일치·서명 불일치·
  파싱 실패, `JwtTokenAdapter.validateRefreshToken`), (2) JWT는 유효하지만 Redis에 저장된 값과
  불일치하거나 저장값 자체가 없음(위조·재사용 의심, `ReissueService` 본문). `EXPIRED_TOKEN`은 refresh
  token JWT 만료(`JwtTokenAdapter`). `MEMBER_NOT_FOUND`는 토큰의 `memberId`에 해당하는 회원이
  존재하지 않을 때(`ReissueService`가 `MemberQueryPort.existsById`로 직접 검사).
  근거: `ReissueControllerTest`(`INVALID_TOKEN`·`EXPIRED_TOKEN`)·`ReissueServiceTest`(4케이스 전부,
  `MEMBER_NOT_FOUND` 포함).

- **명세 델타** — 팀 명세([spec/auth-reissue.md](spec/auth-reissue.md))가 **403 정지·탈퇴 회원**을
  열거하나 서버에 없다. `AuthErrorCode`에 정지·탈퇴 코드가 없고 `ReissueService`에 회원 상태 검사도 없다 —
  회원 부재는 **401 `MEMBER_NOT_FOUND`**로 나간다(HTTP 코드·code 문자열 둘 다 다르다)
  → [open-questions](../synthesis/open-questions.md). 또한 명세의 "인증: Refresh Token" 표기는 HTTP 인증
  헤더를 뜻하지 않는다 — 이 경로는 화이트리스트라 **헤더 없이 호출할 수 있다.** 단 `JwtAuthFilter`는
  `shouldNotFilter` 오버라이드가 없어 화이트리스트 경로에서도 실행되므로, `Authorization` 헤더를
  붙이면 필터가 검증을 시도한다 — **만료 토큰을 붙이면 401 `EXPIRED_TOKEN`이 난다.** 검증 자체는
  요청 **바디**의 `refreshToken`으로만 `ReissueService`가 수행한다.

### POST /api/v1/auth/logout

- **인증**: **필요** — 화이트리스트 밖(위 비대칭 참고). Bearer access token 필수.
- **성공**: HTTP **204** · **envelope 없음**. `LogoutController.logout`은 반환 타입이 없는(Unit)
  함수이고 `@ResponseStatus(HttpStatus.NO_CONTENT)`로만 상태 코드를 지정한다 — `ApiResponse.ok`/`created`
  같은 envelope 생성 호출이 코드에 없으므로 **응답 본문 자체가 비어 있다**(코드로 확정, 추측 아님).
  Android가 이 엔드포인트를 소비할 때 `safeApiCallWithoutData`조차 파싱할 JSON 본문이 없다는 뜻이다.
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `refreshToken` | String | 필수(`@NotBlank`) | |

- **응답 필드**: 없음(204, 본문 없음 — 위 참고)

  흐름 메모: 인증된 회원(access token에서 뽑은 `memberId`)과 요청 바디 `refreshToken`이 가리키는
  회원(`claims.memberId`)이 같은지 검증한 뒤 세션(Redis)을 삭제한다(`LogoutService`). 불일치 시 다른
  회원의 토큰을 지우지 못하도록 403으로 거부한다.

  🔁 **2026-09-02 — 로그아웃이 지우는 것이 하나 늘었다.** `LogoutService`가 refresh token 삭제 바로 뒤에
  `DeviceTokenDeletePort.delete(claims.memberId, claims.sessionId)`를 불러 **그 로그인 세션이 등록한 기기
  (FCM) 토큰 행도 함께 지운다**([notification.md](notification.md)). 요청·응답·상태 코드·에러 코드는
  **하나도 바뀌지 않았다** — 바뀐 것은 이 호출이 서버에 남기는 흔적의 범위다.
  **삭제 범위를 세션 단위로 좁힌 것이 이 delta가 access token에 `sessionId` 클레임을 더한 이유**다
  (같은 회원의 다른 기기 토큰까지 지우지 않으려면 어느 세션이 등록했는지를 알아야 한다).
  ⚠️ 이 정리는 refresh token 정리와 **같은 호출 안에서 예외 방어 없이** 이어진다 — 탈퇴 쪽이 두 정리를
  각각 `runCatching`으로 감싼 것과 다르다([member.md](member.md)).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 401 | `UNAUTHORIZED` | 인증이 필요합니다 |
| 401 | `INVALID_TOKEN` | 유효하지 않은 토큰입니다 |
| 401 | `EXPIRED_TOKEN` | 만료된 토큰입니다 |
| 401 | `MEMBER_NOT_FOUND` | 존재하지 않는 회원입니다 |
| 403 | `FORBIDDEN_REFRESH_TOKEN` | 다른 회원의 Refresh Token입니다 |

  `UNAUTHORIZED`는 `Authorization` 헤더가 없거나 `Bearer`가 아닐 때 — `logout`이 화이트리스트 밖이라
  `SecurityConfig`가 전 요청 인증을 강제하고, 미인증이면 `authenticationEntryPoint`가
  `BusinessException(UNAUTHORIZED)`로 응답한다(컨트롤러 도달 전). `MEMBER_NOT_FOUND`는 access token은
  유효하지만 그 `memberId` 회원이 존재하지 않을 때 — `JwtAuthFilter.authenticate`가 컨트롤러 도달 전에
  던진다(모든 보호 엔드포인트에 공통이며 `logout`도 예외 없음). `INVALID_TOKEN`·`EXPIRED_TOKEN`은 요청
  바디 `refreshToken`(access token이 아니라)을 `LogoutService`가 `TokenValidatePort.validateRefreshToken`으로
  검증하다 나오는 예외를 그대로 전파한 것 — reissue와 같은 근거(`JwtTokenAdapter.validateRefreshToken`)지만
  대상 토큰이 다르다는 점에 주의. `FORBIDDEN_REFRESH_TOKEN`은 검증된 `refreshToken`의 `memberId`가
  인증된 `memberId`와 다를 때 `LogoutService` 본문에서 직접 던진다.
  근거: `LogoutControllerTest`(204·`FORBIDDEN_REFRESH_TOKEN`, 미인증 401은 슬라이스 테스트 한계로
  별도 `SecurityConfigIntegrationTest`가 검증한다고 테스트 코드 주석에 명시)·`LogoutServiceTest`(3케이스
  전부)·`JwtAuthFilter` 직독.

- **명세 델타** — 팀 명세([spec/auth-logout.md](spec/auth-logout.md))가 204 응답의 `code`를
  **`NO_CONTENT`**로 적었으나 **서버에 그런 코드가 없다**(envelope `code`는 `"OK"`·`"CREATED"` 두 값뿐이고
  애초에 이 응답은 envelope 자체가 오지 않는다) — 명세 표기 오류다. 또 명세는 401을 `INVALID_TOKEN` 하나로
  적었으나 실제로는 위 표의 4종으로 갈리고, **바디 `refreshToken` 검증 실패**(만료·위조 → 401)는 명세에
  아예 없다. 멱등성("이미 삭제된 세션이어도 204")은 코드와 일치한다.

## 도메인 에러 코드 전수 — `AuthErrorCode`(14종)

5개 엔드포인트 전부(core·http·external 서버 코드 직독 + 컨트롤러/서비스 테스트)로 14종 전부의 귀속처를
확인했다 — "귀속 미대조"로 남길 항목이 없다. `UNAUTHORIZED`·`MEMBER_NOT_FOUND`·`INVALID_TOKEN`·
`EXPIRED_TOKEN`·`INVALID_ID_TOKEN`은 2개 이상 엔드포인트(또는 전역 필터)가 같은 코드를 서로 다른 상황에서
던진다 — 각 엔드포인트 상세 절의 설명이 근거다.

| code | HTTP | 의미 | 귀속 |
|---|---|---|---|
| `UNAUTHORIZED` | 401 | 인증이 필요합니다 | `logout` 미인증(전역 `SecurityConfig`) |
| `INVALID_TOKEN` | 401 | 유효하지 않은 토큰입니다 | `signup`(registrationToken 검증 실패·애플 클레임 부재) · `reissue`·`logout`(refreshToken, 근거는 각기 다름) |
| `EXPIRED_TOKEN` | 401 | 만료된 토큰입니다 | `signup`(registrationToken) · `reissue`·`logout`(refreshToken) |
| `MEMBER_NOT_FOUND` | 401 | 존재하지 않는 회원입니다 | `reissue`(`ReissueService`) · `logout`(전역 `JwtAuthFilter`, access token 검증 단계) |
| `INVALID_ID_TOKEN` | 401 | 유효하지 않은 ID 토큰입니다 | `kakao` · `apple`(ID 토큰 검증) |
| `KAKAO_JWKS_FETCH_FAILED` | 502 | 카카오 공개키 조회에 실패했습니다 | `kakao` |
| `KAKAO_SERVER_UNAVAILABLE` | 503 | 카카오 서버에 연결할 수 없습니다 | `kakao` |
| `APPLE_SERVER_ERROR` | 502 | 애플 서버 응답 오류입니다 | `apple`(JWKS 비정상·파싱 실패) |
| `APPLE_SERVER_UNAVAILABLE` | 503 | 애플 서버에 연결할 수 없습니다 | `apple`(JWKS 연결 실패) |
| `ALREADY_REGISTERED` | 409 | 이미 가입된 회원입니다 | `signup` |
| `DUPLICATE_TERMS_ID` | 400 | 중복된 약관 ID입니다 | `signup` |
| `TERMS_NOT_FOUND` | 400 | 존재하지 않는 약관입니다 | `signup` |
| `REQUIRED_TERMS_NOT_AGREED` | 400 | 필수 약관에 모두 동의해야 합니다 | `signup` |
| `FORBIDDEN_REFRESH_TOKEN` | 403 | 다른 회원의 Refresh Token입니다 | `logout`(`LogoutService`) |

**카카오·애플의 대응 코드가 대칭이 아니다.** 두 로그인이 하는 일이 같아진 지금도 카카오만
`KAKAO_JWKS_FETCH_FAILED`(502)라는 전용 이름을 갖고, 애플은 같은 실패를 `APPLE_SERVER_ERROR`(502)로 낸다 —
이름만 다른 같은 상황이다.

⚠️ **`MEMBER_NOT_FOUND`는 코드 문자열이 유일하지 않다.** `ParfaitGroupApiErrorCode`·`ImageErrorCode`·
`MemberErrorCode`에도 같은 문자열이 존재하지만 값은 전부 **404**로 다르다
([parfait-group.md](parfait-group.md)·[image.md](image.md)·[member.md](member.md) "도메인 에러 코드 전수",
[conventions.md](conventions.md) "코드 문자열은 enum 간 유일하지 않다" 참고) — 소비 측은 이 문서의 **401**과
혼동하지 않도록 HTTP status를 함께 봐야 한다.

## Android 매핑

`:data`·`:domain`에 API 표면이 구현됐다([spec](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md)) —
**2026-08-06 PR #197로 develop 머지 완료**다. 이 표면이 딛고 선 공용 인프라(`ApiCaller` 4진입점·
`ApiResponse` envelope·`@NoAuth`·`TokenStoreTokenProvider`)는 PR #190으로 먼저 들어왔고, 아래
Service·DataSource·DTO·VO가 이번에 그 위에 올라갔다.
**2026-08-15 — `kakao` 하나가 develop에 결선됐다**(PR #241 `80895eb1`).
`AuthRepository`/`AuthRepositoryImpl`(`loginWithKakao`·`saveSession`)와 `LoginWithKakaoUseCase`가
생겼고 A-002 화면까지 이어졌다 → [a002-kakao-login-api 스펙](../superpowers/specs/archive/2026-08-13-a002-kakao-login-api.md).
같은 PR이 `ServerErrorCode.Auth`(`INVALID_ID_TOKEN`·`KAKAO_JWKS_FETCH_FAILED`·
`KAKAO_SERVER_UNAVAILABLE`)를 `:domain`에 두어 아래 에러 코드 표의 세 값이 앱 코드에 실체로 있다.
**2026-08-15 — `signup`도 화면까지 결선됐다**(PR #242). `AuthRepository.signUp(registrationToken, agreements)` +
`SignUpUseCase`가 붙어 온보딩 약관 동의 화면이 호출하고, 성공 응답(`AuthSessionVO`)을 **UseCase가 그 자리에서
저장**한다(`saveSession`) → [intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md).
`agreements[]`는 화면에 노출한 약관 **전체**(미동의는 `agreed = false`)이고 그 목록 출처는 [policy.md](policy.md)다.
필수 약관 미동의는 요청 전에 도메인이 막는다(`SignUpException.RequiredPolicyNotAgreed`) — 서버
`REQUIRED_TERMS_NOT_AGREED` 400은 화면 가드가 뚫렸을 때만 도달한다.
**2026-08-15 — `reissue`·`logout`도 결선돼 애플을 뺀 4 엔드포인트 전부가 호출부를 얻었다**(PR #260
`9cfbd117`, [스펙](../superpowers/specs/archive/2026-08-15-session-token-refresh-infra.md)).
- **`reissue`의 호출부는 화면이 아니다** — `TokenAuthenticator`(OkHttp `Authenticator`)가 401을 가로채
  부른다. 이 엔드포인트만 **자격증명을 안 붙이는 전용 표면**(`@UnauthenticatedClient` `OkHttpClient`·
  `Retrofit`·`AuthService`)을 타는데, 인증기가 자기 자신을 재진입하지 않게 하려는 것과 재발급이
  자격증명을 헤더가 아니라 **본문**으로 보내기 때문이다. `ServerErrorCode.Auth`에 토큰 거절 코드
  3종(`INVALID_TOKEN`·`EXPIRED_TOKEN`·`FORBIDDEN_REFRESH_TOKEN`)이 추가돼 세션 종료 판정에 쓰인다.
- **`logout`은 회전 재전송을 한 번 한다** — 만료 상태에서 부르면 인증기가 먼저 재발급을 하고 서버가
  refresh token을 **회전시켜 방금 본문에 실어 보낸 값을 죽인다**. 인증기는 `Authorization` 헤더만 갈아
  끼우고 같은 본문을 재전송하므로 그대로 두면 로컬만 지워지고 서버 세션은 살아남는다. 그래서
  `AuthRepositoryImpl.logout()`이 실패 시 저장소의 refresh token을 다시 읽어 **바뀌었으면 새 값으로
  정확히 1회** 재전송한다. 서버가 끝내 실패해도 로컬은 정리하고 성공을 반환한다.
- 204·envelope 없음이라는 이 문서의 서술대로 `safeApiCallNoContent`를 쓴다(변경 없음).

**실제 서버 호출로는 아직 한 번도 검증되지 않았다** — 실기기 검증이 남아 있다. 다만 **막고 있던
것이 무엇인지가 바뀌었다**(2026-08-25, PR #358): 평문 HTTP를 뚫으려고 넣었던 `usesCleartextTraffic`이
`network_security_config.xml`로 대체돼 **디버그 빌드는 평문이 열려 있고 릴리즈 빌드만 HTTPS를 강제**한다.
서버도 HTTPS 도메인을 얻었으므로(→ [conventions](conventions.md) "전송") 남은 전제는 앱
`YG_BASE_URL`뿐이다 → [open-questions](../synthesis/open-questions.md) OQ-P-076·OQ-P-302.

**애플 로그인은 Android가 쓰지 않기로 했다**(2026-08-11 결정, 위 각주). 대응 심볼 0건이 공백이 아니라
결론이므로 이 표에서 "앞으로 채울 자리"로 세지 않는다.

| 엔드포인트 | Service 함수 | DataSource 함수 |
|---|---|---|
| POST `/api/v1/auth/kakao` | `AuthService#postAuthKakao` | `AuthRemoteDataSource#loginWithKakao` |
| POST `/api/v1/auth/apple` | — (해당 없음) | — (해당 없음) |
| POST `/api/v1/auth/signup` | `AuthService#postAuthSignup` | `AuthRemoteDataSource#signup` |
| POST `/api/v1/auth/reissue` | `AuthService#postAuthReissue` | `AuthRemoteDataSource#reissue` |
| POST `/api/v1/auth/logout` | `AuthService#postAuthLogout` | `AuthRemoteDataSource#logout` |

- **요청 DTO**: `KakaoLoginRequest`·`SignupRequest`(+`TermsAgreementRequest`)·`ReissueRequest`·
  `LogoutRequest` — `data/service/model/request/auth/` 패키지, 선언당 파일 하나(파일명은 선언명과
  동일: `KakaoLoginRequest.kt`·`SignupRequest.kt`·`TermsAgreementRequest.kt`·`ReissueRequest.kt`·
  `LogoutRequest.kt`).
- **응답 DTO**: `KakaoLoginResponse`·`SignupResponse`·`ReissueResponse` — 같은 규약으로
  `data/service/model/response/auth/KakaoLoginResponse.kt`·`SignupResponse.kt`·`ReissueResponse.kt`.
  `logout`은 204라 응답 DTO가 없다(`AuthService#postAuthLogout` 반환 타입이 `Unit`,
  `ApiCaller#safeApiCallNoContent`로 감싼다).
  ⚠️ **`KakaoLoginResponse`가 판별자 프로퍼티 `isNewUser`에 `@SerialName("newUser")`를 붙였다** — 실제
  응답 키는 `isNewUser`라 **키를 못 찾고 `MissingFieldException`으로 로그인이 실패한다**. 이전 판본의
  잘못된 계약 기술을 그대로 따른 결과다 → 위 [판별자 키](#판별자-키는-isnewuser다),
  [open-questions](../synthesis/open-questions.md).
- **VO**: `KakaoLoginVO`(sealed — `ExistingMember`/`NewUser`)·`AuthSessionVO`(signup·reissue가 공유)·
  `TermsAgreement` — `domain/model/auth/KakaoLoginVO.kt`·`AuthSessionVO.kt`·`TermsAgreement.kt`. 토큰은
  `AccessToken`·`RefreshToken`·`RegistrationToken` value class 각각 동명 파일(`domain/model/auth/
  AccessToken.kt`·`RefreshToken.kt`·`RegistrationToken.kt`)로 감싸 서로 대체할 수 없게 한다.
- **Mapper**: `data/source/auth/mapper/VOMapper.kt`(`toKakaoLoginVO`·`toAuthSessionVO`(signup·reissue
  각 1개)·`TermsAgreement#toRequest`). `toKakaoLoginVO`는 `newUser`로 분기하고 반대편 필드가 없으면
  `requireNotNull`로 던진다 — 이 예외는 `ApiCaller#safeApiCall(block, transform)` 가드에 잡혀
  `ApiException.Unknown`이 된다(호출부 크래시 아님).
- **이름 충돌 주의**: `domain/model/auth/KakaoLoginVO`(서버 응답)와 `domain/model/KakaoLoginResult`
  (카카오 **SDK** 로그인 결과)는 다른 것이다. 스펙이 예고한 상호 참조 KDoc은 두 파일 어디에도 없다
  → [open-questions](../synthesis/open-questions.md).

## 미결

- ✅ **해소(2026-08-14)** — Android `KakaoLoginResponse`의 `@SerialName`을 `isNewUser`로 정정하고
  와이어 계약 테스트로 잠갔다(위 [Android 매핑](#android-매핑) 참고)
- 위 판별자 키를 **실서버 응답으로는 여전히 확인하지 못했다.** 근거는 서버 코드·컨트롤러 테스트·팀 명세
  3축이고 OpenAPI 스키마만 반대다. A-002 로그인이 결선됐으므로 **실기기 검증에서 로그로 실물 응답을
  찍어 확정한다** — `MissingFieldException`이 나면 서버가 `newUser`를 쓰는 것이고 이 문서가 틀린 것이다
  → [open-questions](../synthesis/open-questions.md)
- 애플 로그인은 서버만 있고 앱 대응 심볼이 0건이다 → [open-questions](../synthesis/open-questions.md)
- `authorizationCode` 제거로 **애플 연동 해제(revoke) 수단이 서버에 없어졌다** — 탈퇴 API가 같은 delta에
  들어왔는데 애플 쪽은 끊지 못한다([member.md](member.md)) → [open-questions](../synthesis/open-questions.md)

그 외 5 엔드포인트의 요청·응답·에러 계약은 core·http·external 서버 코드와 컨트롤러/서비스 테스트로
확인했다.
