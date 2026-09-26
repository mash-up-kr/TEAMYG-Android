---
id: conventions
title: 서버 API 전역 계약
server_module: common/response, common/error, http/global
server_commit: aa9cc9b
verified: 2026-09-04
tags: [api, parfait, server-contract, conventions]
---

# 서버 API 전역 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다.
> 도메인별 계약은 [README.md](README.md)의 인덱스 참고.

## 응답 envelope

모든 응답은 `parfait.common.response.ApiResponse<T>`로 감싼다.

| 필드 | 타입 | 비고 |
|---|---|---|
| `success` | Boolean | 성공 여부 |
| `code` | String | 성공 시 `"OK"`/`"CREATED"`, 실패 시 에러 코드 |
| `message` | String | 사람이 읽는 메시지 |
| `data` | T? | 성공 payload, 실패 시 `null` |
| `errorDetail` | Map<String, String>? | **현재 항상 `null`** — 아래 참고 |

생성 지점은 세 개다 — `ApiResponse.ok(data)`(`code`=`"OK"`) · `ApiResponse.created(data)`(`code`=`"CREATED"`) ·
`ApiResponse.error(errorCode, errorDetail)`.

**성공 코드가 2종**이라는 점이 중요하다. 클라이언트가 성공을 단일 상수 비교로 판정하면 `CREATED` 응답을
실패로 분류한다.

### envelope를 쓰지 않는 응답이 셋 있다

`POST /api/v1/auth/logout` · `DELETE /api/v1/users/me`(2026-08-15 신설) ·
`POST /api/v1/notifications/devices`(2026-09-02 신설)는 **204 No Content에 본문이 없다**
(`@ResponseStatus(HttpStatus.NO_CONTENT)` + 반환 타입 `Unit`). 나머지 전 엔드포인트는 성공이든 실패든
`ApiResponse`를 준다. **envelope를 무조건 파싱하는 클라이언트는 이 셋에서 깨진다**
([auth.md](auth.md)·[member.md](member.md)·[notification.md](notification.md)).

**셋 다 쓰기이고 돌려줄 것이 없는 요청이라는 공통점이 있다** — 예외가 임의로 늘어난 것이 아니라
서버가 "본문 없는 성공"을 쓰는 자리가 하나 더 생긴 것이다. 다만 **같은 성질의 삭제가 반대로 가는
사례가 그대로 남아 있다**(아래).

같은 delta의 토핑 삭제(`DELETE .../images/{parfaitImageId}`)는 반대로 **200 + `data: null`**이다
([parfait-image.md](parfait-image.md)) — **두 DELETE가 성공 표현을 달리한다.**

### `errorDetail`은 계약에만 있고 채워지지 않는다

`GlobalExceptionHandler`의 **다섯** 핸들러(`BusinessException`·`ParfaitGroupException`·bad-request 4종·
`HttpRequestMethodNotSupportedException`·`Exception`)가 모두 `errorDetail` 인자 없이
`ApiResponse.error(errorCode)`를 호출한다. 검증 실패
(`MethodArgumentNotValidException`)도 필드별 상세 없이 `CommonErrorCode.INVALID_REQUEST` 하나로 뭉개진다.

## 에러 코드 체계

`parfait.common.error.BaseErrorCode` 인터페이스(`status: Int`·`code: String`·`message: String`)를
도메인별 enum이 구현한다.

| enum | 위치 | 종수 |
|---|---|---|
| `CommonErrorCode` | `common/error` | 3 (2026-08-19 `METHOD_NOT_ALLOWED` 신설로 2 → 3) |
| `AuthErrorCode` | `core/auth/exception` | 14 |
| `ParfaitGroupApiErrorCode` | `http/parfaitgroup` | 10 (2026-08-15 `GROUP_NICKNAME_ALREADY_USED` 삭제로 11 → 10) |
| `ImageErrorCode` | `core/image/exception` | 4 |
| `MemberErrorCode` | `core/member/exception` | 2 |
| `ParfaitImageErrorCode` | `core/parfaitimage/exception` | 5 |
| `ParfaitErrorCode` | `core/parfait/exception` | 5 (2026-08-15 신설, 2026-08-16 상세 조회·배경 변경으로 2 → 5) |

### `CommonErrorCode`

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 요청 형식이 올바르지 않습니다 |
| 405 | `METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드입니다 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류가 발생했습니다 |

🔁 **2026-08-19 — 405가 생겼다.** 그전에는 `HttpRequestMethodNotSupportedException` 전용 핸들러가 없어
**catch-all로 떨어져 500**이 나갔다(POST 전용 API에 GET을 보낸 사례가 500으로 잘못 보고된 것이 계기다).
지금은 전용 핸들러가 405를 낸다 — **경로별 도메인 코드가 아니라 전역 코드**이므로 모든 엔드포인트에서
날 수 있고, 도메인 문서의 에러 표에는 401·`INVALID_REQUEST`와 같은 이유로 반복하지 않는다.

`ParfaitGroupApiErrorCode`는 core 계층 `ParfaitGroupError`와 **이름이 1:1**이다(`from(error) = valueOf(error.name)`).

### 코드 문자열은 enum 간 유일하지 않다

`code` 문자열은 **각 enum 내부에서만** 유일하다 — enum을 넘어서는 전역 유일성 보장이 없다. 실례:
`MEMBER_NOT_FOUND`는 `AuthErrorCode`에서 **401**(존재하지 않는 회원, [auth.md](auth.md) "도메인 에러 코드
전수" 참고)이고 `ParfaitGroupApiErrorCode`·`ImageErrorCode`·`MemberErrorCode`에서는 **404**(같은 의미지만
다른 status, [parfait-group.md](parfait-group.md)·[image.md](image.md)·[member.md](member.md) "도메인 에러
코드 전수" 참고)다. **소비 측은 envelope `code` 문자열 단독이 아니라 HTTP status와 함께 판정해야 한다** —
`code`만으로 분기하면 서로 다른 네 상황(토큰의 회원 부재 vs 그룹 관련 회원 부재 vs 업로드 URL 발급 시
회원 부재 vs 계정 조회·닉네임 변경 시 회원 부재)을 한 브랜치로 뭉갠다. **같은 문자열을 가진 enum이 넷**이
됐다(2026-08-11 member 도메인 신설).

📌 **반대로 status·message까지 같아 와이어에서 구분되지 않는 쌍도 있다.** `PARFAIT_NOT_FOUND`가
`ParfaitErrorCode`와 `ParfaitImageErrorCode` 양쪽에 있고 둘 다 404·같은 메시지다 — 어느 enum이 던졌는지
소비 측은 알 수 없고 알 필요도 없다. 2026-08-20 delta로 토핑 네 엔드포인트가 전부 이 코드를 내게 되면서
사례가 넓어졌다([parfait-image.md](parfait-image.md)).

⚠️ **한 엔드포인트가 같은 `code`를 서로 다른 status로 낼 수도 있다.** `GET /api/v1/users/me`가 그렇다 —
`JwtAuthFilter`가 던지면 **401**, `MemberService`가 던지면 **404**인데 문자열은 둘 다 `MEMBER_NOT_FOUND`다
([member.md](member.md)).

## 인증

JWT Bearer. `JwtAuthFilter`가 검증하고 인증 주체의 이름(`Authentication.name`)이 **memberId(Long 문자열)**다.
컨트롤러는 `Authentication.memberId(): Long = name.toLong()` 확장으로 꺼낸다.

**2026-09-02 — access token이 `sessionId` 클레임을 싣는다.** `JwtTokenAdapter.createAccessToken`이
refresh token과 **같은 세션 식별자**를 담고, `validateAccessToken`의 반환이 `Long`에서
`AccessTokenClaims(memberId, sessionId?)`로 바뀌었다. `JwtAuthFilter`는 그 세션을 인증 객체의
**credentials 슬롯**에 실어 컨트롤러가 꺼내 쓰게 한다(`principal`은 memberId 문자열 그대로).
와이어 계약(요청 헤더 형식·응답 토큰 필드)은 **한 글자도 바뀌지 않았다** — 토큰 문자열 안에 클레임이
하나 는 것이라 앱이 고칠 것은 없다.
⚠️ **`sessionId`는 널을 허용한다.** 이 변경 전에 발급된 access token에는 클레임이 없어 널로 채워지고
**인증은 정상으로 통과한다.** 그 세션 값을 쓰는 첫 소비처가 기기 토큰 등록이며, 널이면 로그아웃이
그 행을 못 지우는 구간이 생긴다([notification.md](notification.md)).

`SecurityConfig`는 세션을 쓰지 않고(STATELESS), 아래 화이트리스트 외 **전 요청 인증 필수**다.

- `/actuator/health`
- `/swagger-ui.html` · `/swagger-ui/**`
- `/favicon.ico`
- `/v3/api-docs/**`
- `/api/v1/auth/kakao`
- `/api/v1/auth/apple`
- `/api/v1/auth/signup`
- `/api/v1/auth/reissue`
- `/api/v1/policies`
- `/api/v1/test/parfait-canvas/rotate` ⚠️ **테스트 전용**

인증 실패는 `AuthErrorCode.UNAUTHORIZED`(401)로 나간다.

⚠️ **화이트리스트에 테스트 전용 경로가 들어왔다(2026-08-15).** `/api/v1/test/parfait-canvas/rotate`는
**인증 없이 전체 그룹의 캔버스를 즉시 마감·재생성**한다([parfait.md](parfait.md)). 서버 코드가 컨트롤러와
이 등록 양쪽에 "프로덕션 오픈 전 함께 제거" TODO를 달아 두었다 → [open-questions](../synthesis/open-questions.md).

`[Feat/#45] 토큰 재발급(refresh) / 로그아웃 API 구현 (#63)`(`6f5bffc`)이 기존 `/api/v1/auth/**` 와일드카드를
위 auth 경로 개별 등록으로 좁혔다. **`/api/v1/auth/logout`은 화이트리스트에 없어 인증 대상**이다 — 인증 도메인
5개 엔드포인트 중 access token이 필요한 유일한 엔드포인트다(상세는 [auth.md](auth.md)).
`/api/v1/policies`는 `[Feat/#64] 약관 목록 조회 API 구현 (#65)`(`69654bc`)이,
`/api/v1/auth/apple`은 `[Feat/#50] 애플 로그인 API 구현 (#76)`(`96affd0`)이 같은 개별 등록 방식으로
추가했다(상세는 [policy.md](policy.md)·[auth.md](auth.md)).

**2026-08-10 image 도메인 2건이 들어왔지만 화이트리스트는 그대로다** — `/api/v1/images`와
`/api/v1/images/{imageId}/confirm`은 **인증 대상**이다(상세는 [image.md](image.md)). 단 confirm은
토큰 유효성만 보고 **이미지 소유자를 대조하지 않는다** → [open-questions](../synthesis/open-questions.md).

**2026-08-15 delta의 신규 5건**(파르페 오늘·과거 목록 · 토핑 테두리 수정·삭제 · 회원 탈퇴)**도 전부
화이트리스트 밖이라 인증 대상**이다. 위 테스트 전용 회전 1건만 예외다.

**2026-08-16 delta의 신규 2건**(파르페 상세 조회 · 배경 변경)**도 화이트리스트 밖이라 인증 대상**이다
([parfait.md](parfait.md)). 화이트리스트 자체는 이 delta에서 바뀌지 않았다.

**2026-09-02 delta의 신규 1건**(기기 FCM 토큰 등록)**도 화이트리스트 밖이라 인증 대상**이다
([notification.md](notification.md)). 화이트리스트는 이번에도 바뀌지 않았고, 그 엔드포인트는 회원과
세션을 **요청이 아니라 토큰에서** 정한다 — 남의 기기를 지목할 경로가 없다.

**2026-08-11 member 2건·parfait-image 2건도 화이트리스트 밖이라 전부 인증 대상**이다
([member.md](member.md)·[parfait-image.md](parfait-image.md)). 네 엔드포인트 모두 대상 회원을 요청이 아니라
**토큰에서** 정한다 — 남의 계정을 지목할 경로가 없다. 다만 토핑 배치(POST)는 배치자 대조가 없어
**같은 그룹의 다른 멤버가 남의 배치를 덮어쓸 수 있다** → [parfait-image.md](parfait-image.md).

**관측 사실**: `HealthController`가 매핑한 `GET /health`(`http/global/health`, #63이 `http/api/health`에서
옮겼다)는 화이트리스트의 `/actuator/health`와 경로가 달라 **인증 대상**이다.

## URL 규약

현재 3형태가 공존한다.

| 형태 | 예 |
|---|---|
| `/api/v1/<도메인>` | `/api/v1/auth/kakao` · `/api/v1/auth/apple` · `/api/v1/auth/signup` · `/api/v1/auth/reissue` · `/api/v1/auth/logout` · `/api/v1/policies` · `/api/v1/images` |
| `/api/v1/<도메인>/{id}/<동작>` | `/api/v1/images/{imageId}/confirm` |
| `/api/v1/<도메인>/me/<하위>` | `/api/v1/users/me` · `/api/v1/users/me/nickname` |
| `/api/v1/groups/{groupId}/<하위>` | `/api/v1/groups/{groupId}/parfaits` · `.../parfaits/year` · `.../parfaits/today` · `.../parfaits/{parfaitId}` · `.../parfaits/{parfaitId}/background` · `.../parfaits/{parfaitId}/images/{parfaitImageId}/border` |
| `/api/v1/test/<도메인>/<동작>` | `/api/v1/test/parfait-canvas/rotate` (테스트 전용) |
| `/api/<도메인>` (버전 없음) | `/api/parfait-groups` |

버전 프리픽스 유무가 갈리고, **그룹을 가리키는 경로가 `groups`와 `parfait-groups` 둘**이다.
서버에 URL 규약 문서가 없어 관측 사실로만 적는다 → [open-questions](../synthesis/open-questions.md).

**URL 세그먼트와 서버 도메인 이름이 갈리는 사례가 늘었다** — `users`↔`member`,
그룹 하위 `images`↔`parfaitimage`. 도메인 파일명 규약은 경로 기준이지만([README.md](README.md))
이 둘은 경로만으로 이름이 겹치거나(`images`) 뜻이 흐려져(`users`) **서버 도메인 이름을 따랐다** —
`users` 경로는 [member.md](member.md)가, 그룹 하위 `images` 경로는 [parfait-image.md](parfait-image.md)가
다룬다.

⚠️ **`images`라는 세그먼트가 두 도메인에 있다** — 최상위 `/api/v1/images`는 업로드([image.md](image.md)),
그룹 하위 `.../parfaits/{parfaitId}/images`는 배치([parfait-image.md](parfait-image.md))다.

## 직렬화 규약

### Boolean 필드의 `is` 접두사는 JSON 키에 **남는다**

> 🔁 **2026-08-11 정정.** 2026-08-02~08-10 판본은 이 절을 "`is` 접두사가 사라진다"로 적었다. **틀렸다.**
> 근거를 OpenAPI 스키마 하나에만 뒀던 것이 원인이다.

서버 `http` 모듈이 `tools.jackson.module:jackson-module-kotlin`을 의존한다. 이 모듈이 붙으면 Jackson이
getter 이름이 아니라 **Kotlin 주 생성자 파라미터명**으로 프로퍼티를 잡으므로, `val isXxx: Boolean`의
**JSON 키는 `isXxx` 그대로**다.

해당 필드는 둘이다 — `KakaoLoginResponse`·`AppleLoginResponse`의 `isNewUser`
(→ [auth.md](auth.md) "판별자 키"). `KakaoLoginControllerTest`·`AppleLoginControllerTest`가 실제 직렬화된
응답 본문에 대해 `jsonPath("$.data.isNewUser")`를 단언한다.

**OpenAPI 스키마는 이 필드를 `newUser`로 적는다.** springdoc이 swagger-core의 자체 ObjectMapper로 모델을
유도하는데 거기엔 Kotlin 모듈이 없어 bean 규칙(`is` 제거)이 적용되기 때문이다. **런타임 직렬화 결과와
다르다** — 아래 [OpenAPI](#openapi) 절의 "코드가 정본" 규칙이 그대로 적용된다.

**소비 측 규칙**: 서버 DTO 프로퍼티명을 **그대로** JSON 키로 본다. 스키마가 다르게 적어도 스키마를 믿지
않는다. Android는 어느 쪽이든 `@SerialName`으로 키를 명시하되 **값은 서버 DTO 프로퍼티명**이어야 한다 —
어긋나면 기본값 없는 필드에서 `MissingFieldException`이 나고 호출이 통째로 실패한다.

### 응답 `null` 필드는 생략되지 않는다

`spring.jackson.default-property-inclusion: always`(`application.yaml`)라, 분기 응답에서 채워지지 않은
쪽도 **키가 `null` 값으로 실려 온다.** 키 존재 여부로 분기를 판정하면 안 된다는 뜻이다.

### 전송 — 서버가 HTTPS 도메인으로 옮겨 가고 평문 경로는 닫힌다

**2026-08-25 delta(#112·#113)가 이 절의 전제를 뒤집었다.** 그전까지 서버는 IP + 포트의 **평문
`http`**로 서비스됐고(주소는 private submodule `project-paths.md`·앱 `local.properties` 참고),
이 절은 그것을 사실로 적고 있었다.

- **TLS 종단이 앞단 리버스 프록시(Caddy)로 갔다.** 도메인에 Let's Encrypt 인증서가 붙고 갱신은
  자동이며, 프록시가 애플리케이션 컨테이너의 평문 포트로 넘긴다. 애플리케이션 자체는 여전히
  평문으로 듣는다 — **HTTPS는 프록시가 만들어 주는 것**이라 서버 코드에는 흔적이 없다.
- **`server.forward-headers-strategy: framework`가 붙었다**(`application.yaml`). 이게 없으면
  애플리케이션이 스킴을 `http`로 인식해 OpenAPI 문서가 광고하는 `servers[0].url` 같은 **절대 URL이
  `http://`로 나간다.** 즉 이 설정은 계약 문서가 읽는 스키마의 정확성에도 걸린다.
- ⚠️ **그 대가로 `X-Forwarded-*`를 무조건 신뢰한다.** 프록시를 우회하는 평문 포트가 열려 있는
  동안에는 스킴·클라이언트 IP를 위조할 수 있어, 서버 런북이 검증 뒤 **평문 포트를 닫는 단계**를
  절차에 두었다.
- ⚠️ **그 차단이 앱을 끊는다.** 지금까지 앱이 붙던 주소는 평문 포트고, 닫히는 순간 **기존
  `YG_BASE_URL`로 빌드된 앱은 전부 연결에 실패한다.** 앱이 새 HTTPS 주소로 옮겨야 하는데 그
  시점을 서버 커밋만 봐서는 알 수 없다(1회성 인프라 절차라 코드에 남지 않는다) →
  [open-questions](../synthesis/open-questions.md) OQ-P-302.

**Android 쪽 서술을 함께 정정한다.** 이 절은 앱에 `usesCleartextTraffic`도 `networkSecurityConfig`도
**없다**고 적어 왔으나, `app/src/main/AndroidManifest.xml`에 `android:usesCleartextTraffic="true"`가
**2026-08-15(PR #241)부터 있었다** — 로그인 실기기 검증을 막고 있어 넣은 임시 조치이고 main 매니페스트라
릴리즈 빌드까지 따라갔다(OQ-P-076).

✅ **앱이 같은 날 따라왔다(2026-08-25, PR #358).** `usesCleartextTraffic`이 빠지고
`app/src/main/res/xml/network_security_config.xml`이 그 자리를 대신한다 — `base-config`가
`cleartextTrafficPermitted="false"`에 시스템 인증서만 신뢰하고, `debug-overrides`가 평문을 허용하면서
사용자 설치 인증서까지 신뢰한다. 즉 **릴리즈 빌드는 이제 HTTPS를 강제하고, 디버그 빌드만 평문
개발 주소로 계속 붙는다.** 이 문서가 권고했던 순서(base URL 교체가 먼저)를 코드는 뒤집었지만
`debug-overrides`가 그 위험을 흡수했다. ⚠️ 좁히기는 권고보다 넓다 — "개발 서버 도메인만"이 아니라
**디버그 빌드의 모든 호스트**에 평문이 열려 있다. 앱 `YG_BASE_URL` 교체는 `local.properties`라 커밋
delta로 확인할 수 없어 OQ-P-302가 계속 쥔다.

📌 **저장소가 가르치는 주소가 HTTPS로 옮겨졌다**(2026-08-26, PR #376). 앱 요청 모음 `http/README.md`가
`base_url` 예시를 평문 IP·포트에서 **개발 서버 도메인**으로 바꾸고, "앱에서는 아직 이 서버를 호출할 수
없다"던 절을 "`YG_BASE_URL`은 HTTPS 주소를 넣는다"로 교체했다. 같은 PR이 `network_security_config.xml`에
**호스트를 가리지 않는다는 경고를 코드 주석으로** 박았다. 즉 위 ⚠️ 두 개가 문서에만 있던 상태를 벗어나
저장소 안에서도 읽히게 됐다. **그래도 실제로 어떤 주소로 빌드되는지는 여전히 안 보인다** — 값은
`local.properties`에 있고 CI(`restore-app-secrets`)는 이 키를 복원하지 않으므로 키가 없으면 빌드는
`https://TODO.example.com/` 플레이스홀더로 조립된다.

## HTTP 밖 계약 — 서버가 앱으로 미는 푸시

**2026-09-04, 이 디렉토리의 전제가 하나 깨졌다.** 지금까지 여기 적힌 계약은 전부 **앱이 요청하고 서버가
응답하는 왕복**이었다 — envelope도 에러 코드도 인증도 그 왕복 위에 서 있다. `[Feat/#127]`이 붙인 FCM
발송은 **앱이 요청한 적 없는 메시지를 서버가 밀어 넣는** 두 번째 축이고, 위 규약 어느 것도 적용되지
않는다.

| 축 | 형태 | 적용되는 전역 규약 |
|---|---|---|
| HTTP 왕복 | 요청 → 응답 | envelope · 에러 코드 · 인증 · URL · 직렬화 |
| **푸시(신설)** | 서버 → 앱 단방향 | **없다** — 스키마는 FCM 메시지 구조이고 실패는 응답이 아니라 서버 로그로 간다 |

📌 **2026-09-04에 이 축이 1종에서 3종이 됐다**(`[Feat/#128]`) — 토핑 등록에 더해 **데일리 리마인드
`REMIND_AM`·`REMIND_PM`**이 시각 트리거로 나간다. **종류마다 `data` 키 구성이 다르다**(토핑 넷,
리마인드 둘)는 것이 이 축의 새 성질이다.

계약 내용은 도메인 문서에 둔다 → [notification.md](notification.md) "서버가 보내는 푸시".
여기서는 **전역으로 성립하는 세 가지**만 적는다.

1. **실패가 클라이언트에 보이지 않는다.** 발송 실패는 HTTP 상태 코드도 에러 코드도 아니고, 재시도를
   소진하면 조용히 버려진다. **에러 코드 체계로 대응할 수 없는 실패 부류가 처음 생겼다.**
2. **최소 한 번(at-least-once) 보장이다.** 왕복 API가 요청당 응답 하나를 주는 것과 달리 **같은 알림이
   두 번 도착할 수 있다.** ⚠️ **이것도 알림 종류마다 다르다**(2026-09-04) — Outbox를 거치는 토핑 등록만
   재시도·중복이 있고, 리마인드는 배치가 한 번 쏘고 끝이라 **재시도도 중복도 없는 대신 실패가 그대로
   유실된다.** "푸시는 이렇다"고 한 문장으로 묶을 수 없는 축이 됐다.
3. **직렬화 규약이 다르다.** FCM `data`는 값이 전부 문자열이라, 숫자 식별자도 **문자열로 온다** —
   위 [직렬화 규약](#직렬화-규약)의 JSON 타입 서술을 이 축에 그대로 적용하면 안 된다.

⚠️ **이 축은 앱이 기기 토큰을 등록해야만 도달한다.** 등록이 0건인 동안에는 발송이 전부 취소되므로
**계약이 살아 있어도 관측되지 않는다** — 어긋난 채로 오래 갈 수 있는 자리다.
> 📌 **앱이 등록을 시작했다(2026-09-05, PR #450)** — 세션 축 넷에서 부른다
> ([notification.md](notification.md) "기기 토큰 등록 결선"). 그러므로 위 두 행(`date` 미사용·중복
> 알림)은 이제 **관측될 수 있는 상태**가 됐다. 다만 실서버·실기기 확인이 0회라 아직 아무도 보지 않았다.

## 스키마 소유권 — 코드가 정본이어도 운영 응답은 다를 수 있다

**2026-08-24, 세 번째 근거 축이 드러났다.** 이 체계는 지금까지 근거를 둘로만 봤다 — 서버 코드(정본)와
OpenAPI 스키마(보조). 그런데 **운영 DB 스키마**가 코드와 어긋나 있으면 코드대로 읽은 계약이 실제 응답과
다르다.

Flyway 마이그레이션이 운영 히스토리에는 V4까지만 기록돼 있었고, 그 뒤로는 `ddl-auto: update`가 스키마를
대신 관리했다. `update`는 컬럼·인덱스를 **추가만 하고 삭제하지 않으므로** DROP을 포함한 V5·V6·V8·V10과
제약·기본값을 바꾸는 V7·V11·V15, 그리고 데이터 이관을 포함한 V14가 운영에 반영되지 않았다. 이 문서들이
"서버가 이렇게 한다"고 적은 것 중 최소 둘이 운영에서는 거짓이었다 —
[그룹 내 닉네임 중복 허용](parfait-group.md#get-apiparfait-groupsjoin-preview)과
[토핑 배치](parfait-image.md#post-apiv1groupsgroupidparfaitsparfaitidimages) 성공이다.

`#110`이 그 소유권을 Flyway로 되돌렸다.

- **`ddl-auto: validate`** — 어긋나면 **기동 시점에 실패**한다. 조용한 드리프트가 끝났다는 뜻이고,
  앞으로 "코드가 정본"이 운영에서도 성립할 근거다.
- **`V16__reconcile_ddl_auto_schema_drift.sql`** — 그 차이만 메운다. 고아 컬럼 제거(`member.email`·
  `member.apple_refresh_token`), 그룹 닉네임 유니크 인덱스 제거, `parfait_image` FK 복원,
  `image_meta.image_type`·`status`와 `parfait.status`의 기본값 복원, `nametag_chip` 백필과 `NOT NULL`.
  신규 DB에서도 도는 조건부 SQL이라 두 경로가 같은 스키마로 수렴한다(`FlywayMigrationTest`가 잠근다).
- 배치 메타테이블 `initialize-schema`가 `always` → `never`로 되돌아가고 소유권이 V12로 갔다.
  `always`는 Flyway가 꺼져 있어 V12가 안 돌던 기간의 임시 방편이었다.
- `JpaConfigurationTest`가 `ddl-auto`·`flyway.enabled`·`batch.initialize-schema` 세 값을 고정한다.

⚠️ **이 전환은 배포만으로 끝나지 않는다.** 운영 히스토리가 V1~V4뿐이라 Flyway가 V5부터 재실행하려 하고,
`ddl-auto`가 이미 만들어 둔 컬럼을 다시 ADD 하다 죽는다. **사람이 1회성으로 baseline SQL을 실행**해야
한다(서버 `docs/operations/flyway-cutover.md`). 그 절차가 언제 돌았는지 앱 쪽에서 알 방법이 없어
**"이 문서의 서술이 운영에서 언제부터 참인가"는 확정되지 않는다** →
[open-questions](../synthesis/open-questions.md).

## OpenAPI

서버는 springdoc을 켜 두었다(`OpenApiConfig`, title `Parfait API`, version `v1`) — `/v3/api-docs`·`/swagger-ui`.
이 문서 체계는 **서버 코드 직독**을 근거로 삼고 OpenAPI JSON을 파싱하지 않는다(서버 실행이 필요하고
에러코드 열거·검증 로직이 스키마에 안 잡힌다).

**2026-08-02, OpenAPI 실물을 받아 코드와 대조했다.** 그 결과 두 축이 서로를 보완한다는 것이 확인됐다.

- **코드만 아는 것**: 에러 코드 열거(스키마는 성공 응답만 문서화한다), 검증 규칙, 그리고 **실제 HTTP
  상태 코드**. `POST /api/v1/auth/signup`이 대표적이다 — 스키마는 **200**으로 적었으나 컨트롤러가
  `ResponseEntity.status(HttpStatus.CREATED)`를 쓰므로 실제는 **201**이다. springdoc이 `ResponseEntity`의
  런타임 status를 읽지 못한 것이다. 같은 이유로 `@ResponseStatus`를 쓴 엔드포인트는 정확히 나온다.
- **스키마가 틀리는 것**: 직렬화 키와 `required` 목록. 둘 다 springdoc이 **앱의 ObjectMapper가 아닌
  자기 ObjectMapper**로 유도해 생기는 차이다 → 위 [직렬화 규약](#직렬화-규약)·아래 `required` 절.

> 🔁 **2026-08-11 정정.** 이전 판본은 여기에 "스키마만 아는 것: 직렬화 결과"를 두고 `isNewUser` → `newUser`
> 변환을 그 예로 들었다. 실제로는 **스키마가 틀린 사례**였다 — 직렬화 결과도 코드(컨트롤러 테스트의
> 응답 본문 단언)가 답한다.

**규칙**: 두 근거가 갈리면 **코드가 정본**이다. 스키마는 코드 대조를 대체하지 못한다 — 지금까지 확인된
스키마-코드 차이는 3건(성공 status, `required` 목록, 직렬화 키)이고 전부 코드가 옳았다.

### 스키마 `required`는 Bean Validation 애노테이션만 반영한다

**2026-08-10 OpenAPI 실물을 다시 받아 image 도메인 포함 전 요청 DTO를 대조했다.** 스키마 `required`
배열에 들어가는 필드는 `@NotBlank` 같은 Bean Validation 애노테이션이 붙은 것뿐이고, **Kotlin 비널
타입이지만 애노테이션이 없는 필드는 빠진다.**

| 요청 DTO | 스키마 `required` | 실제 비널 필드 |
|---|---|---|
| `KakaoLoginRequest` | `idToken`·`nonce` | 같음 |
| `SignupRequest` | `registrationToken` | + `agreements` |
| `IssueImageUploadUrlRequest` | `fileName`·`contentType` | + `imageType` |
| `CreateParfaitGroupRequest` | (없음) | `groupName`·`groupNickname`·`memberLimit` |

**2026-08-11 delta로 이 간극이 넓어졌다.** 스키마 실물을 다시 받지는 않았으나(서버 실행 필요) 규칙상
아래가 예측된다 — 애노테이션이 하나도 없어 `required`가 통째로 비고 실제로는 전부 필수다.

| 요청 DTO | 스키마 `required`(예측) | 실제 비널 필드 |
|---|---|---|
| `AppleLoginRequest` | `identityToken`·`nonce` | 같음(둘 다 `@NotBlank`) — 2026-08-15에 `authorizationCode`가 빠졌다 |
| `ChangeGlobalNicknameRequest` | `nickname` | 같음 |
| `PlaceParfaitImageRequest` | (없음) | `imageId`·`positionX`·`positionY`·`positionZ`·`scale`·`rotation`·`borderType` |
| `UpdateParfaitImageRequest` | (없음) | (없음 — 전 필드가 널 허용, 빈 바디도 유효) |
| `UpdateParfaitImageBorderRequest` | (없음) | `borderType` (2026-08-15 신설) |
| `ChangeParfaitBackgroundRequest` | (없음) | `type` — `value`·`imageId`는 널 허용이지만 **`type`에 따라 하나가 필수**다(2026-08-16 신설) |

⚠️ **조건부 필수는 스키마도 타입도 표현하지 못한다.** `ChangeParfaitBackgroundRequest`가 첫 사례다 —
`type = COLOR`면 `value`가, `type = IMAGE`면 `imageId`가 필수인데 둘 다 Kotlin 널 허용이라 위 표의
"실제 비널 필드"로도 안 잡힌다. 판정은 서비스·도메인이 하고 실패는 `INVALID_BACKGROUND`(400)다.
이런 계약은 **도메인 문서의 "필수" 열 비고로만** 남는다 → [parfait.md](parfait.md).

✅ **앱은 이것을 도메인 타입으로 표현했다**(2026-08-16, PR #266) — wire DTO는 평면·널 허용 그대로 두고
(서버의 거울), `:domain`의 sealed `CanvasBackgroundEdit`(`Color(hex)` / `Image(imageId)`)이 잘못된 조합을
**컴파일에서** 막는다. 펴는 일은 매퍼(`toRequest()`)가 한다. 조건부 필수 계약을 만나는 다른 엔드포인트가
생기면 같은 형태를 따른다 → [data-layer](../architecture/data-layer.md).

빠진 필드도 **누락하면 400이다** — jackson-module-kotlin이 비널 파라미터 부재에서 실패하고
`GlobalExceptionHandler`의 bad-request 핸들러가 `INVALID_REQUEST`로 바꾼다.

**소비 측 규칙**: **스키마에서 클라이언트 모델을 생성하지 않는다.** 생성하면 비널 필드가 nullable로
떨어져 "보내도 되고 안 보내도 되는" 것처럼 보인다. 각 도메인 문서의 "필수" 열이 이 구분을 이미
표기하고 있다 — `필수(@NotBlank)`(스키마에도 있음) vs `필수(non-null 타입)`(스키마에는 없음).

## Android 불일치

TJYG-Android `:data`의 원격 네트워크 구조([ADR-0017](../adr/0017-remote-network-datasource.md))와 위 계약의 간극.

⚠️ **2026-09-08 기준 4건.** 하나는 2026-08-31 서버 delta(`02e11be`)가 그룹 목록 `recentImageUrl`의 뜻을
좁히면서 벌어진 것이고, **둘은 2026-09-05에 앱이 푸시 수신부를 붙이며 새로 생겼다**(PR #446·#447).
뒤의 둘은 HTTP 왕복이 아니라 **서버→앱 단방향 푸시의 간극**이라 엔드포인트 셈에 안 잡힌다.
**넷째는 2026-09-08에 앱이 스스로 만든 간극이다**(PR #464) — 서버가 검증하지 않는 범위를 앱이 읽는
쪽에서 가두기로 했고, 그래서 같은 행을 서버와 앱이 다른 값으로 본다.

✅ **다섯째였던 캔버스 응답 `groupName`은 하루 만에 닫혔다**(2026-09-08, PR #469 develop 머지) —
2026-09-07 서버 delta(`9c13852`)가 더한 필드를 앱 DTO(`GetTodayParfaitResponse`)와 `CanvasVO`가 받는다.
**서버 delta가 벌린 간극 중 가장 짧게 열려 있던 것**이고, 그래서 이 표의 셈이 5 → 4로 내려갔다.
다만 같은 delta가 만든 404 `GROUP_NOT_FOUND`는 앱이 여전히 구별하지 않는다(OQ-P-383 ④) — 값이 아니라
**실패 경로**라 이 표의 대상이 아니다.

| 항목 | 계약 | 앱 | 결과 |
|---|---|---|---|
| `MyParfaitGroupResponse.recentImageUrl`의 뜻 | **오늘 캔버스**(`ParfaitDay.current()` — 03시 경계)에 토핑이 있으면 그 이미지, 없으면 `null`. 어제 이전 토핑은 안 잡힌다 | `MyParfaitGroupVO.recentImageUploadedAt` KDoc과 `feature/groups/list/impl/util/ToppingImage.kt`의 `toToppingImage`가 `null`을 **"토핑이 하나도 없는 그룹"**으로 읽는다 | 어제까지 토핑이 있었고 오늘 캔버스만 빈 그룹이 G-001에서 **템플릿 그래픽**으로 그려지는데, 같은 줄의 경과 시간은 **어제 토핑 시각**을 가리킨다 — 두 표시가 서로를 반박한다 → [parfait-group.md](parfait-group.md) · OQ-P-336 |
| 푸시 `data`의 `date` | 알림이 가리키는 **캔버스 날짜**를 함께 보낸다(`NotificationMessageFactory`) — 계약 표가 `route`·`groupId`와 함께 이 값으로 캔버스에 도달할 것을 요구한다 | `PushDeepLinkIntent.kt`가 `type`·`route`·`groupId` 셋만 extras에서 읽는다. `PushDeepLink.AddTopping`은 **항상 그 그룹의 최신 캔버스**로 연다 | 지난 캔버스에 올라온 토핑 알림을 탭해도 오늘 캔버스가 열린다. 발송 지연·재시도(최대 6시간)와 03시 마감이 겹치면 **알림이 가리킨 캔버스가 아닌 곳**에 도착한다 → [notification.md](notification.md) · OQ-P-359 |
| 푸시 중복 수신 | at-least-once라 **같은 알림이 두 번 올 수 있고** 앱이 그것을 견뎌야 한다(`dedup_key`는 생산 쪽 멱등일 뿐이다) | 알림 id가 `message.messageId?.hashCode()`라 재시도로 온 같은 알림이 **다른 id**를 받는다 | 이동은 접히지만(`Channel(CONFLATED)`) **알림은 두 개로 쌓인다** → [notification.md](notification.md) · OQ-P-359 |
| 토핑 `borderWidth` 범위 | 서버가 범위를 검증하지 않는다 — `borderType=SOLID` 면 두께가 있기만 하면 받는다 | `ToppingBorder.solidClamped` 가 읽는 쪽에서 `WIDTH_RANGE_DP`(2.0..30.0)로 가둔다. 가두는 자리는 공용 `data/source/common/mapper/ToppingBorderMapper.kt` 의 `toToppingBorder` 하나이고, 캔버스·토핑 수정·**그룹 목록**(2026-09-11 PR #496) 세 `VOMapper` 가 그것을 부른다 | 상한을 내리기 전에 50 으로 저장된 행이 앱에서는 30 으로 그려진다. **서버가 준 값과 앱이 그리는 값이 다르다** — 서버나 정책이 범위를 정하면 걷을 임시 코드다(OQ-P-381). 🔁 **드러나는 자리가 둘이 됐다**(2026-09-16 PR #497) — 그룹 목록 썸네일도 테두리를 그리기 시작해, 클램프가 목록 카드에서도 보인다 |

**아래 문단은 첫 행(`recentImageUrl`, 2026-09-01)에 대한 것이다.** 뒤의 두 행은 잡은 수단이 다르다 —
계약 문서와 앱 코드를 나란히 놓는 `develop` 기준 문서 점검이 찾았고, 그 자리에서 자동으로 잡을 수단은
서버·앱 어느 쪽에도 없다(OQ-P-352 ③과 같은 성격이다).

⚠️ **이번 것도 계약 문서 감사가 잡았다.** 앱 쪽 타입도 `@SerialName` 키도 하나 안 바뀌었고 서버가 같은
필드에 담는 **뜻**만 바뀐 부류라, 역직렬화도 매퍼도 초록으로 지나간다. OQ-P-234 ③이 붙이려는 와이어
계약 테스트로도 이 부류는 못 잡는다 — 문자열은 그대로이기 때문이다. **뜻의 변화를 잡는 수단은 지금
계약 문서 대조뿐이다.**

**아래는 그 앞 회차의 기록이다.**

✅ **2026-08-20 기준 0건.** 오래 걸려 있던 두 건이 PR #308·#310 머지로 같은 날 닫혔다.

| 항목 | 무엇이었나 | 어떻게 닫혔나 |
|---|---|---|
| `MyParfaitGroupResponse.recentImageUploadedAt` 파싱 | 계약은 오프셋 없는 `yyyy-MM-ddTHH:mm:ss`(컨트롤러 테스트가 검증)인데 `data/source/group/mapper/VOMapper.kt`가 `kotlin.time.Instant::parse`(오프셋 필수)로 읽었다. 2026-08-19 서버 `COALESCE` 비널화로 **그룹이 하나라도 있으면** G-001 목록 조회가 통째로 실패하는 상태까지 갔다 | PR #310이 `LocalDateTime::parse` → `toInstant(PARFAIT_TIME_ZONE)`로 고쳤다. **근거는 계약 사실**이다 — 서버 DB 커넥션 세 환경이 `serverTimezone=Asia/Seoul`이라 그 벽시계는 KST다. 이 버그를 초록으로 지켜 온 `MyParfaitGroupVOMapperTest`(오프셋 붙은 입력을 스스로 지어 넣었다)를 지우고 커버리지를 `ParfaitGroupRemoteDataSourceImplTest`로 옮겼다 |
| "오늘"의 경계 | 서버는 `ParfaitDay.current()` — **03:00** 기준(위키 [[캔버스-마감-스케줄]]의 마감 시각), 앱 `parfaitToday()`는 **KST 자정** 기준이라 00:00~03:00 KST에 부작용 있는 오늘 조회가 두 번 돌고 화면이 D 아래 D−1 캔버스를 그렸다 | PR #308이 앱 경계를 03시로 옮겼다. 방향이 분명했던 쪽(**서버가 정책에 맞고 앱이 자정에 머물러 있었다**)이라 앱만 고쳤다 → [parfait.md](parfait.md) "하루 경계" |

⚠️ **재발 방지 수단은 둘 다 안 생겼다.** 첫 항목을 잡은 것은 계약 문서 감사이고, 앱 테스트는 자기
DTO를 자기가 만들어 넣어 `@SerialName` 문자열도 날짜 포맷도 서버 원본과 대조하지 않는다. 와이어 계약
테스트 선례(`KakaoLoginResponseSerializationTest`)는 있으나 이 부류에 아직 안 붙었다 → OQ-P-234 ③.
둘째 항목은 앱이 서버 상수를 **복제**해 맞춘 것이라(계약이 경계 시각을 내려주지 않는다) 서버가 배치
시각을 바꾸면 다시 갈린다 — 그 조건은 `DayWindow.DAY_BOUNDARY_HOUR` KDoc에 적혀 있다.

> **필드가 늘어난 것은 불일치가 아니다.** 2026-08-18~19 delta가 그룹 상세·목록·생성·캔버스·토핑 배치
> 응답에 필드를 더했고, `JsonModule`이 `ignoreUnknownKeys = true`라 앱이 안 읽어도 역직렬화가 깨지지
> 않으므로 이 표의 대상이 아니다 — **계약이 앱보다 넓은 것**이고, 각 도메인 문서의 Android 매핑 절이
> 기회로 적는다.
>
> ✅ **그 기회 대부분이 2026-08-20에 소비됐다**(PR #308·#310 develop 머지) — 그룹 상세의
> `groupName`·`memberLimit`, 상세 `members[].nameTagChip`, 목록 `lastPlacedByNameTagChip`, 캔버스
> `groupMembers[].nameTagChip`이 전부 VO를 얻고 화면까지 닿았다. **남은 것은 둘**이고 성격이 다르다 —
> 토핑 배치·캔버스의 `placedBy.nameTagChip`은 읽는 화면이 0건이라 **DTO에서 멈춰 세운 것**이고(도메인
> 모양을 소비자 없이 굳히지 않는다), 그룹 **생성** 응답의 `recentImageUrl`·`recentImageUploadedAt`·
> `lastPlacedByNameTagChip`은 DTO에 거울로 두었지만 `CreatedGroupVO`가 그 셋을 갖지 않는다
> (생성 직후 화면이 쓰지 않고, 같은 이름 필드가 목록 응답과 **다른 컬럼에서 나온다**).
>
> ✅ **2026-08-26 — 늘어난 필드가 같은 날 소비된 첫 사례가 나왔다.** 서버가 캔버스 응답
> `images[].placedBy`에 `ownerType`을 더하고(PR #115), 앱이 같은 날 그것을 읽어
> `CanvasToppingVO.isMine`으로 접었다(PR #376). 위 "남은 것은 둘"에 이 필드가 합류하지 않은 이유가
> 그것이고, **기회와 소비 사이의 간격이 0일인 것도 처음**이다 → [parfait.md](parfait.md).
>
> ⚠️ **키 이름이 바뀐 것은 다른 문제였다.** 2026-08-19에 응답 JSON 키가 `nametagChip` → `nameTagChip`,
> `lastPlacedByNametagChip` → `lastPlacedByNameTagChip`으로 바뀌었다(서버 코어 프로퍼티명은 그대로,
> HTTP DTO 경계에서만). 그 필드를 옛 키로 읽던 코드는 예외 없이 **조용히 `null`**이 됐다 — 기본값이
> 있어 `MissingFieldException`도 안 난다(OQ-P-227이 경고한 "큰 소리로 깨지는" 쪽의 반대 극단이다).
> ✅ **PR #310이 키를 맞춘 상태로 머지됐고**, 남은 것은 **재발 방지 수단**이다 — 이 부류를 잡은 것은
> 두 번 다 계약 문서 감사였다 → OQ-P-234 ③.

✅ 오래 걸려 있던 로그인 판별자 키 불일치(응답 키가 `isNewUser`인데 Android가
`@SerialName("newUser")`를 붙였던 건)는 **PR #241로 정정됐고 와이어 계약 테스트가 잠갔다**
([auth.md](auth.md) 각주). 계약 해석의 근거는 위 [직렬화 규약](#직렬화-규약)이다.

> ⚠️ **다만 `http/auth.http`는 아직 `newUser`를 가르친다** — 정정이 앱 DTO와 `http/README.md`에는
> 닿았고 요청 모음 파일 하나에 안 닿았다. 계약 표에 남길 불일치는 아니지만 그 파일로 실서버 응답을
> 확인하려는 사람이 조용히 잘못된 분기를 탄다 → [open-questions](../synthesis/open-questions.md).

**아래는 그보다 앞선 회차의 해소 기록이다.**

**2026-08-04 기준 남은 항목 없음.** 오래 걸려 있던 3건(Android `ApiResponse`에 `success`·`errorDetail`
부재 / `isSuccess`가 `code == "SUCCESS"` 단일 비교 / `TokenProvider`가 항상 null)은
`network-envelope-token-storage` 라운드가 **PR #190으로 develop에 머지되며 전부 해소**됐다 —
envelope 5필드 정합, 성공 판정은 `success` 필드, `TokenProvider`는 `TokenStoreTokenProvider`
([ADR-0019](../adr/0019-encrypted-token-storage.md)). 대응 [open-questions](../synthesis/open-questions.md)
항목도 해소 처리했다.

> **다만 "일치"가 "검증됨"은 아니다.** 14 엔드포인트 Service·DataSource는 2026-08-06 PR #197로,
> 나머지 6개는 2026-08-12 PR #230으로 develop에 들어왔고, **2026-08-15에 다섯 라운드**(PR #241·#242·
> #243·#244·#248)가 카카오 로그인·약관 조회·회원가입·그룹 목록/생성/참여/닉네임 변경 **8 엔드포인트를
> 화면까지** 이었다. 같은 날 **PR #250**이 남은 5 엔드포인트의 표면까지 채웠다. 그럼에도 **실서버 요청
> 검증은 여전히 0건**이다(실기기 미수행) — 위에서 닫은 시각 파싱 불일치도 처음부터 끝까지 **코드·계약
> 대조로만** 드러나고 사라졌다 → [open-questions](../synthesis/open-questions.md).

**2026-08-18·2026-08-19 delta 둘 다 엔드포인트를 늘리지 않았다**(28 + 테스트 전용 1 유지) — 바뀐 것은
응답 필드·JSON 키·"오늘"의 정의·전역 405다. 아래 표면 셈은 그대로 유효하다.

**2026-08-16 기준 서버 엔드포인트는 28개(+테스트 전용 1)고 Android 표면은 27개다.** 분모에서 빠지는 것은
애플 로그인 1건(`해당 없음`)과 테스트 전용 회전 1건이라 **27/27, 공백 0**이다. 서버 delta가 벌린 공백 2
(파르페 상세 조회·배경 변경)를 **같은 날 PR #266이 닫았다**
([spec](../superpowers/specs/archive/2026-08-16-canvas-detail-background-api-service-layer.md)) — 직전 라운드의 공백 5는
PR #250이 닫았다. **벌어졌다 닫히는 왕복이 다섯 번째**이고, 이번에는 `:data` 표면만 닫히고 `http/` 요청
모음은 25/27로 남아 **두 표면이 처음으로 갈렸다**. 소비처는 두 도메인 전부 0건이다
→ [open-questions](../synthesis/open-questions.md).

📌 **2026-08-31 delta로 공백이 다시 하나 벌어졌다 — 29 + 테스트 전용 1이 됐고 표면 셈은 27/28이다.**
신설된 **토핑 일괄 수정 PATCH**([parfait-image.md](parfait-image.md))에 `:data` 표면이 없고 `http/`
요청 모음도 25/28로 함께 벌어졌다 — 두 표면이 갈리지 않고 같이 뒤처진 것은 2026-08-16 이후 처음이다.
같은 delta가 과거 캔버스 목록 응답에 붙인 `status`는 앱 DTO가 받지 않으나 **`⚠️불일치`가 아니다**
(`ignoreUnknownKeys = true`, 읽는 화면 0건) → [parfait.md](parfait.md) ·
[open-questions](../synthesis/open-questions.md) OQ-P-333 · OQ-P-334.

✅ **2026-08-31 두 번째 라운드 — 두 공백 다 앱이 메웠다**(브랜치 `feature/#427-sync-backend-api-260831`,
**2026-09-01 develop 머지 — PR #428 `e870fb87`**).
일괄 PATCH가 `:data` 표면(Service·DataSource)과 소비처(Repository·UseCase·확인 버튼)까지 얻고,
대신 **위치/크기/각도 단건 수정 PATCH**가 표면을 잃었다(소비처가 그 화면 하나뿐이었는데 일괄로
옮겨 탔다) — **표면 셈은 27/28로 그대로**이고 공백의 정체만 일괄에서 단건으로 옮겨 앉았다. `http/`
요청 모음은 여전히 **25/28**이다(`parfait-image.http`에 일괄 요청을 안 넣기로 한 이 라운드의
결정 — 실서버 요청 계획이 없다). 과거 캔버스 목록의 `status`도 앱 DTO·`PastCanvasVO`까지 올라왔다
(OQ-P-333 부분 해소 — 달력 점 기준은 개수 그대로 두기로 했다, [parfait.md](parfait.md)). 남는
것은 OQ-P-334의 잔존 항목(실패 항목 미식별·검사 순서 차이·`items` 상한 없음)과 단건 PATCH 표면
소멸을 다루는 신규 미결이다 → [open-questions](../synthesis/open-questions.md).

새 간극이 발견되면 이 절에 표를 다시 세운다.
