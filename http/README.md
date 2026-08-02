# HTTP Client — 서버 API 테스트

Android Studio 내장 HTTP Client로 서버 API를 직접 호출한다. 스웨거에서 토큰을 복붙할 필요가 없다.

**토큰이 응답에서 자동 추출돼 다음 요청에 쓰인다.** 로그인 → 재발급 → 로그아웃을 순서대로 눌러보면 된다.
그룹 생성 응답의 `groupId`·`inviteCode`도 마찬가지로 이어진다.

---

## 1. 준비

`http-client.private.env.json`에 개발 서버 주소가 이미 들어 있다(이 파일은 gitignore 대상이라 커밋되지 않는다). 나머지는 필요한 것만 채우면 된다.

```json
{
  "dev": {
    "base_url": "http://<개발-서버>:8080",

    "id_token": "",
    "nonce": "",

    "access_token": "",
    "refresh_token": "",
    "registration_token": "",

    "group_id": "",
    "invite_code": ""
  }
}
```

### 두 가지 사용법

**A. 로그인부터 흘려보내기** — `idToken`·`nonce`만 채우고 `auth.http` 1번을 실행하면 이후 토큰이 자동으로 이어진다.

- **`idToken`** — 카카오 SDK가 발급한 ID 토큰이다. 이 파일만으로는 만들 수 없다. 앱에서 카카오 로그인을 한 번 수행하고 로그에서 뽑거나, 카카오 개발자 도구로 발급받는다.
- **`nonce`** — **앱이 직접 생성하는 값**이다. 카카오 SDK 로그인 요청에 넘긴 값과 **똑같은 값**을 API에도 보내야 한다. 서버가 ID 토큰의 `nonce` 클레임과 대조하므로 다르면 `401 INVALID_ID_TOKEN`.

**B. 토큰을 직접 붙여넣기** — 이미 발급받은 토큰이 있으면 `access_token`·`refresh_token`에 넣고 카카오 로그인을 건너뛴다. 그룹 API만 테스트할 때 편하다.

### ⚠️ 변수 우선순위 — B를 쓸 때 반드시 알아야 한다

```
런타임 global  >  http-client.private.env.json  >  http-client.env.json
```

요청을 한 번이라도 실행하면 응답 핸들러가 토큰을 **런타임 global**에 저장하고, 그 값이 env 파일보다 **우선한다**. 즉 **private env의 토큰을 고쳐도 반영되지 않는다.**

손으로 넣은 값을 쓰려면 먼저 global을 비운다 — **`_reset.http`의 `0. 전역 변수 전부 비우기`를 실행**하면 된다. 지금 어떤 값이 쓰이고 있는지 확인하는 요청(`0-2`)도 같은 파일에 있다.

---

## 2. 실행

1. `.http` 파일 열기 → 요청 왼쪽 ▶️ 클릭 → 환경 **`dev`** 선택
2. 응답과 함께 테스트 결과·로그가 표시된다

| 파일 | 내용 |
|---|---|
| `_reset.http` | 전역 변수 초기화·확인(토큰을 손으로 넣을 때 먼저 실행) |
| `auth.http` | 카카오 로그인 · 회원가입 완료 · 토큰 재발급 · 로그아웃 |
| `parfait-group.http` | 그룹 8종(목록·생성·참여 미리보기·참여·상세·닉네임 변경·신고·탈퇴) |
| `parfait.http` | 그룹 캘린더 연도 리스트 |
| `health.http` | 헬스체크(인증 유무 대조용) |

**권장 순서**: `auth.http` 1~2 → `parfait-group.http` 2(생성) → 나머지 → `auth.http` 4(로그아웃)

---

## 3. 응답 형태

**모든 응답은 envelope로 감싸여 온다.**

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": { "accessToken": "...", "refreshToken": "...", "expiresIn": 3600 },
  "errorDetail": null
}
```

토큰은 `data` 안에 있다 — `response.body.data.accessToken`이지 `response.body.accessToken`이 아니다.

**예외**: `POST /api/v1/auth/logout`은 **204**라서 응답 본문이 아예 없다. envelope도 오지 않는다.

`errorDetail`은 계약에는 있으나 **서버가 현재 항상 `null`로 보낸다**(필드별 검증 메시지가 채워지지 않는다).

---

## 4. 스웨거(OpenAPI)와 실제가 다른 곳

스웨거 문서는 springdoc이 생성한 것이라 몇 군데가 실제와 어긋난다. **여기 적힌 쪽이 맞다**(서버 코드로 확인).

### `isNewUser`가 아니라 `newUser`다 ⚠️ 가장 중요

카카오 로그인 응답의 신규 유저 판별자는 **`newUser`**다.

서버 Kotlin은 `val isNewUser: Boolean`인데, Jackson이 getter 이름에서 `is` 접두사를 떼고 직렬화해 **실제 JSON 키는 `newUser`**로 나간다(OpenAPI 스키마가 그렇게 적혀 있고, 그게 실제 응답이다).

`isNewUser`로 읽으면 항상 `undefined`/`null` → **신규 유저가 기존 회원으로 잘못 분기**되고, 없는 `accessToken`을 꺼내게 된다. Android 응답 타입도 `@SerialName("newUser")`가 필요하다.

### 회원가입은 200이 아니라 201이다

스웨거에는 `200 OK`로 적혀 있지만 `SignupController`는 `ResponseEntity.status(HttpStatus.CREATED)`로 내보낸다. springdoc이 `ResponseEntity`의 런타임 status를 읽지 못해 기본값 200을 문서화한 것이다.

### 스웨거에 없는 에러 코드가 많다

스웨거는 성공 응답만 열거한다. 실제 에러 코드는 `AuthErrorCode`(12종)·`ParfaitGroupApiErrorCode`(11종)·`CommonErrorCode`(2종)에 있고, 각 `.http` 파일 주석에 엔드포인트별로 적어뒀다.

---

## 5. 함정 (실제로 걸렸던 것)

### `reissue`에 `Authorization`을 붙이면 재발급이 막힌다

`/api/v1/auth/reissue`는 화이트리스트지만, 서버 `JwtAuthFilter`는 `shouldNotFilter`를 오버라이드하지 않은 `OncePerRequestFilter`라 **화이트리스트 경로에서도 실행된다.** `permitAll`은 인가만 통과시킬 뿐 필터를 건너뛰지 않는다.

즉 만료된 access token을 헤더에 붙이면 필터가 검증에 실패해 `401 EXPIRED_TOKEN`이 나고, **재발급 자체가 불가능해진다.** refresh token은 반드시 **바디**로만 보낸다.

> 앱이 `@NoAuth` 어노테이션으로 화이트리스트 경로의 헤더를 빼는 이유가 이것이다.
> `auth.http` 3번의 주석 처리된 `Authorization` 줄을 풀면 직접 재현할 수 있다.

### 재발급은 회전이다

응답의 **새 refresh token을 반드시 저장해야 한다.** 기존 것은 서버에서 폐기돼 재사용하면 `401 INVALID_TOKEN`. `auth.http`의 응답 핸들러가 자동으로 갱신한다.

### `logout`만 인증이 필요하다

인증 도메인 4개 중 `logout`만 화이트리스트 밖이다. 헤더에 access token, 바디에 refresh token을 **둘 다** 보낸다.

### 그룹 탈퇴는 200이다

`DELETE .../members/me`는 204가 아니라 **200**이고 envelope로 `groupId`를 돌려준다.

### 파르페 연도 조회는 그룹 존재를 확인하지 않는다

없는 `groupId`를 넣어도 `404 GROUP_NOT_FOUND`가 아니라 **`403 GROUP_NOT_JOINED`**가 온다. 같은 도메인의 다른 엔드포인트와 동작이 다르다.

### `GET /health`는 인증이 필요하다

화이트리스트에는 `/actuator/health`만 있다. `/health`(`HealthController`)는 경로가 달라 인증 대상으로 남는다.

### `termsId`를 알 방법이 없다

회원가입이 요구하는 `termsId`는 서버가 정한 "현재 유효한 약관"의 id인데, **약관 목록 조회 API가 서버 계약에 없다.** 하드코딩한 값이 최신 버전이 아니면 `400 TERMS_NOT_FOUND`. 서버팀에 물어봐야 한다.

---

## 6. ⚠️ 앱에서는 아직 이 서버를 호출할 수 없다

개발 서버는 **평문 HTTP**(`https`가 아니다)인데, 이 앱은 `targetSdk = 36`이고 `AndroidManifest.xml`에 `usesCleartextTraffic`도 `networkSecurityConfig`도 **없다.**

Android 9(API 28)부터 평문 HTTP는 기본 차단이므로, 실제 연동을 시작하면 **모든 요청이 `CLEARTEXT communication not permitted`로 실패한다.**

이 `.http` 파일들은 IntelliJ가 직접 보내는 것이라 영향받지 않는다 — 앱 코드에서 호출할 때만 문제가 된다. 해결은 둘 중 하나다:
- 서버에 HTTPS 적용(권장)
- debug 빌드에 한해 `network_security_config.xml`로 해당 호스트만 cleartext 허용

또 `local.properties`에 `YG_BASE_URL` 키가 **없어서** 앱은 지금 placeholder로 빌드되고 있다. 실제 연동 전에 채워야 한다.

---

## 7. 자주 나오는 에러

| 코드 | code | 원인 |
|---|---|---|
| 401 | `INVALID_ID_TOKEN` | `idToken` 만료·서명 불일치, 또는 **`nonce` 불일치** |
| 401 | `EXPIRED_TOKEN` | access/refresh 만료. 재발급 또는 재로그인 |
| 401 | `INVALID_TOKEN` | 토큰 위조, 또는 refresh가 서버 저장값과 불일치(재사용 의심) |
| 401 | `UNAUTHORIZED` | `Authorization` 헤더 자체가 없음 |
| 401 | `MEMBER_NOT_FOUND` | 토큰의 회원이 존재하지 않음 (그룹 API의 404 `MEMBER_NOT_FOUND`와 **다른 코드다**) |
| 403 | `FORBIDDEN_REFRESH_TOKEN` | access token의 주인과 바디 refresh token의 주인이 다름 |
| 403 | `GROUP_NOT_JOINED` | 참여하지 않은 그룹 |
| 404 | `GROUP_NOT_FOUND` | 없는 그룹 |
| 404 | `INVALID_INVITE_CODE` | 초대코드 무효 |
| 409 | `ALREADY_REGISTERED` | 이미 가입된 회원인데 signup 호출 |
| 409 | `GROUP_ALREADY_JOINED` · `GROUP_MEMBER_LIMIT_REACHED` · `GROUP_NICKNAME_ALREADY_USED` | 참여 관련 충돌 |
| 400 | `TERMS_NOT_FOUND` · `DUPLICATE_TERMS_ID` · `REQUIRED_TERMS_NOT_AGREED` | 약관 관련 |
| 400 | `INVALID_GROUP_NAME` · `INVALID_GROUP_NICKNAME` · `INVALID_GROUP_MEMBER_LIMIT` · `INVALID_GROUP_REPORT_REASON` | 그룹 입력값 |

> `MEMBER_NOT_FOUND`는 **`AuthErrorCode`에서 401, `ParfaitGroupApiErrorCode`에서 404**로 중복 정의돼 있다. 코드 문자열만으로 분기하면 두 상황이 뭉개진다 — HTTP status와 함께 봐야 한다.

---

## 8. 파일 구조

```
http/
├── README.md                     # 이 문서
├── _reset.http                   # 전역 변수 초기화·확인 (API 테스트 아님)
├── auth.http                     # 인증 4종
├── parfait-group.http            # 그룹 8종
├── parfait.http                  # 파르페 조회
├── health.http                   # 헬스체크
├── http-client.env.json          # 환경 변수 구조(값 비움, 커밋됨)
└── http-client.private.env.json  # 실제 값 (gitignore — 커밋되지 않음)
```

`private` 파일이 있으면 IntelliJ가 그 값을 우선한다. **토큰·서버 주소 같은 실제 값은 반드시 `private` 쪽에** 넣는다.

---

## 참고

- 서버 API 계약 문서: 문서 저장소 `parfait/api/`
- [IntelliJ HTTP Client 문서](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
