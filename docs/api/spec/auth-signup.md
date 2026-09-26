---
id: auth-signup
title: 회원가입 완료 (약관동의)
spec_source: 팀 노션 API 명세
spec_status: 완료
spec_issue: "#49"
server_commit: aa9cc9b
verified: 2026-09-04
related_api: auth.md
tags: [api, parfait, spec, auth]
---

# 회원가입 완료 (약관동의) (팀 명세)

> 팀이 합의한 **의도**입니다. 서버 코드의 현실은 [../auth.md](../auth.md) — 갈리는 지점은
> [코드 대조](#코드-대조)에 모았습니다.

- **Method / Path**: `POST /api/v1/auth/signup`
- **구분**: 로그인/가입 · **인증**: 불필요
- **설명**: 신규 유저가 약관동의 후 최종 회원가입 처리, 이때 정식 토큰 발급

## 개요

로그인 응답이 `isNewUser: true`였을 때, 앱은 약관동의 화면을 보여주고 **동의 내역 + 로그인 단계에서 받은
`registrationToken`**을 함께 보낸다. **이 호출이 성공해야 비로소 정식 회원이 되고 access/refresh가 발급된다.**

로그인 단계(`/auth/kakao`, `/auth/apple`)에서 신규면 access/refresh 대신 `registrationToken`(임시,
검증된 소셜 신원을 담음)만 내려주고, 이 API에서 그것을 검증해 회원을 생성한다.

## 요청

`Content-Type: application/json`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `registrationToken` | string | ✅ | 신규 로그인 응답에서 받은 임시 토큰 |
| `agreements` | array | ✅ | 동의 내역 목록. 약관 하나당 항목 하나 |
| `agreements[].termsId` | number | ✅ | 약관 식별자 (몇 번째 약관인지) |
| `agreements[].agreed` | boolean | ✅ | 해당 약관의 동의 여부 |

```json
{
  "registrationToken": "eyJhbGciOi...",
  "agreements": [
    { "termsId": 1, "agreed": true },
    { "termsId": 2, "agreed": true }
  ]
}
```

**`termsId`는 동의한 약관(버전) 참조다.** 각 약관별 yes/no 목록이 아니라 "어떤 약관에 동의했는가"를
기록한다. 서버는 해당 약관이 필수인지 확인해 `agreed`가 true가 아니면 400.

## 응답 201

정식 가입 완료 → 공통 토큰 응답(access/refresh) 발급.

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "expiresIn": 3600
}
```

## 상태 코드

| 코드 | 의미 | code |
|---|---|---|
| 201 | 회원가입 완료 | `CREATED` |
| 400 | `registrationToken` 누락 / `agreements` 구조 오류 (bean validation) | `INVALID_REQUEST` |
| 400 | `agreements`에 동일 `termsId` 중복 | `DUPLICATE_TERMS_ID` |
| 400 | 현재 유효(타입별 최신 버전)하지 않은 `termsId` | `TERMS_NOT_FOUND` |
| 400 | 현재 필수 약관 중 `agreed:true`로 없는 항목 존재 | `REQUIRED_TERMS_NOT_AGREED` |
| 401 | `registrationToken` 만료 | `EXPIRED_TOKEN` |
| 401 | `registrationToken` 위조 / purpose 불일치 | `INVALID_TOKEN` |
| 409 | 이미 가입된 회원 | `ALREADY_REGISTERED` |

## 코드 대조

서버 `main` `69654bc` 기준.

### 일치

- 경로·메서드·인증 불필요(화이트리스트 `/api/v1/auth/signup`), 성공 **201** `CREATED`
- 요청 4필드(`registrationToken` `@NotBlank`, `agreements` `@Valid`, 원소 `termsId`·`agreed`)
- 응답 3필드 — 로그인 응답과 달리 셋 다 널 아님
- 에러 코드 7종 전부와 **검증 순서까지 일치**한다. `SignupService.validateAgreements`가
  중복(`DUPLICATE_TERMS_ID`) → 현재 유효 여부(`TERMS_NOT_FOUND`) → 필수 미동의(`REQUIRED_TERMS_NOT_AGREED`)
  순으로 검사하고, 그 뒤에 중복 가입(`ALREADY_REGISTERED`)을 본다.
- "현재 유효(타입별 최신 버전)" 서술 — `TosQueryPort.findCurrentTerms`가 근거. `TosRepository.findCurrentTerms`가
  `type`별 `published_at` 내림차순 1건씩 뽑는다
- `agreed`가 false인 항목을 보내도 요청 자체는 유효하다. 서버는 `agreements.filter { it.agreed }`로
  동의한 것만 저장한다 — 명세의 "어떤 약관에 동의했는가를 기록한다"와 정확히 같은 동작이다.

### 코드에만 있음

- **가입 시 서버가 닉네임을 자동 생성한다.** `SignupService`가 `RandomNicknameGenerator.generate()`로
  만들어 `MemberRegistrar.register`에 넘긴다. **앱은 닉네임을 보내지 않고, 받지도 않는다** — 이 응답에
  닉네임 필드가 없다. 명세에 언급이 없다.
- **전체가 하나의 트랜잭션**이다(`@Transactional`). 가입 실패 시 회원 데이터가 남지 않는다.
- 🔁 **애플 분기가 사라졌다(2026-08-15 정정).** 2026-08-11 판본은 이 자리에
  "`handleProviderSpecificRegistration`이 애플 refresh token을 저장하고, 클레임이 비면 401 `INVALID_TOKEN`"을
  적었다. `refactor: 애플 로그인 authorizationCode 교환 로직 제거 (#89)`가 그 분기를 **통째로 제거**했고
  마이그레이션 `V10__drop_apple_refresh_token_from_member.sql`이 컬럼도 지웠다. **가입 경로는 이제 provider와
  무관하게 하나**이고, 명세에 없던 그 실패 경로도 함께 없어졌다 — 즉 **이 항목은 "코드에만 있음"에서 빠지고
  코드가 명세 쪽으로 돌아왔다**([../auth.md](../auth.md)). `/auth/apple` 엔드포인트 자체는 그대로 있다.
- envelope 5필드 — 명세의 JSON 예시는 `data` 안쪽만 보여준다 → [conventions.md](../conventions.md)
- **`termsId`의 출처가 되는 약관 목록 조회 API가 생겼다.** `GET /api/v1/policies`
  (`69654bc`, [../policy.md](../policy.md))가 `termsId`·`type`·`title`·`url`·`required`를 내려준다.
  이 signup 명세는 그 API를 언급하지 않는다 — 명세 작성 시점에 없었다. 같은
  `TosQueryPort.findCurrentTerms`를 쓰므로 목록이 준 `termsId`는 같은 시점의 signup에서 유효하다.

### 명세에만 있음

- 없음.

### 표기 차이

- 🔁 **2026-08-11 철회.** 이전 판본은 "명세는 `isNewUser`인데 실제 JSON 키는 `newUser`"라고 적었다 —
  **명세가 맞았다.** 실제 응답 키는 `isNewUser`다
  → [auth-kakao-login.md](auth-kakao-login.md) "철회된 불일치", [../conventions.md](../conventions.md)
  "직렬화 규약".
- 스웨거(OpenAPI)는 이 엔드포인트를 **200**으로 문서화하나 실제는 **201**이다. `SignupController`가
  `ResponseEntity.status(HttpStatus.CREATED)`로 내보내는데 springdoc이 `ResponseEntity`의 런타임
  status를 읽지 못해 기본값을 적은 것이다 — 위 "일치" 절의 201이 맞다.

## Android 구현 시 주의

1. **로그인만으로 끝나지 않는다.** `isNewUser=true`면 약관 동의 화면을 거쳐 이 API까지 성공해야
   access/refresh를 받는다. `registrationToken`은 10분 만료라 UI 흐름에서 고려한다.
2. **닉네임을 보내지 마라.** 서버가 자동 생성한다. 초기 닉네임을 화면에 보여줘야 한다면 이 응답이 아니라
   별도 회원 조회 API가 필요하다 — **2026-08-11 그 API가 생겼다**: `GET /api/v1/users/me`
   ([../member.md](../member.md)). 바꾸는 것도 같은 문서의 `PATCH /api/v1/users/me/nickname`이다.
3. `agreed=false` 항목을 목록에 포함해도 된다. 다만 **필수 약관이 `agreed:true`로 없으면**
   `REQUIRED_TERMS_NOT_AGREED` 400이다.
4. `termsId` 목록은 서버가 정하는 "현재 유효한 약관"이다. 하드코딩하면 약관이 개정될 때
   `TERMS_NOT_FOUND` 400을 받는다 — **약관 동의 화면 진입 시 `GET /api/v1/policies`를 먼저 호출해
   `termsId`를 받아 쓴다**([../policy.md](../policy.md)). 그 응답은 빈 배열일 수 있다.

## 미결

- 없음 — 약관 목록 조회 API 부재 건은 `69654bc`(`GET /api/v1/policies`)로 해소됐다
  ([../policy.md](../policy.md)). 남은 확인 항목(`url` 필드가 링크인지 전문인지)은 그 문서의 `## 미결`에 있다.
