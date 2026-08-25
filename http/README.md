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

    "terms_id_1": "",
    "terms_id_2": "",

    "group_id": "",
    "invite_code": "",

    "image_id": "",
    "image_upload_url": "",

    "parfait_image_id": "",
    "parfait_id": ""
  }
}
```

> `terms_id_*`·`group_id`·`invite_code`·`image_*`·`parfait_image_id`·`parfait_id`는 **손으로 채우는 값이 아니다** —
> 각 `.http` 파일의 응답 핸들러가 `client.global.set`으로 런타임에 채운다. 여기 구조로 적어 두는 것은
> "이 체계에 어떤 변수가 있는가"를 한곳에서 보기 위해서고, `_reset.http`가 도메인별로 비우는 목록과
> 짝을 이룬다.

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
| `policy.http` | 현재 유효 약관 목록(회원가입이 쓰는 `termsId` 출처) |
| `parfait-group.http` | 그룹 8종(목록·생성·참여 미리보기·참여·상세·닉네임 변경·신고·탈퇴) |
| `parfait.http` | 그룹 캘린더 연도 리스트 · **오늘의 캔버스** · **과거 캔버스 목록** · **상세 조회** · **배경 변경**(단색·이미지) · ⚠️ 테스트 전용 강제 회전 |
| `health.http` | 헬스체크(인증 유무 대조용) |
| `images.http` | 이미지 업로드 URL 발급 · 업로드 확인(**2번 요청만 서버가 아니라 S3로 나간다**) |
| `users.http` | 내 계정 조회 · 전역 닉네임 변경 · **탈퇴**(선행: `auth.http`만) |
| `parfait-image.http` | 토핑 배치 확정 · 위치/크기/각도 수정 · **테두리 수정** · **삭제**(**선행이 넷** — `auth.http` → `parfait-group.http` → `images.http` → `parfait.http`) |

**권장 순서**: `auth.http` 1 → `policy.http` 1 → `auth.http` 2 → `parfait-group.http` 2(생성) → 나머지 → `auth.http` 4(로그아웃)

⚠️ `users.http`의 마지막 요청은 회원 탈퇴, `parfait-image.http`의 10번은 토핑 삭제다 — 파일을 위에서부터 통째로 순서대로 돌리면 계정·데이터가 지워진다.

⚠️ **`parfait.http`의 마지막 두 요청은 파괴 범위가 더 넓다.** 테스트 전용 강제 회전은 **인증 없이 서버의 모든 그룹**의 캔버스를 즉시 마감한다(내 그룹만이 아니다). 그 뒤로는 그 그룹에 쓰기를 시험할 수 없다 — 오늘 날짜 캔버스가 `CLOSED`로 남고 "오늘의 캔버스 조회"가 계속 그것을 돌려주기 때문이다. **409 `PARFAIT_ALREADY_CLOSED`를 재현할 때만** 쓰고, 이어서 시험하려면 그룹을 새로 만든다.

`policy.http`를 먼저 돌려야 `auth.http` 2번의 `termsId`가 채워진다. 기존 회원으로 로그인했다면 회원가입을 건너뛰므로 `policy.http`도 건너뛰어도 된다.

`parfait-image.http`는 준비가 가장 길다 — `images.http`의 발급 → S3 PUT → confirm 까지 끝내 이미지를 `COMPLETED`로 만들어야 배치가 통과한다(`PENDING`이면 `409 IMAGE_NOT_CONFIRMED`). `parfaitId`는 `parfait.http`의 "오늘의 캔버스 조회"가 응답 핸들러로 `parfait_id`에 채워 준다.

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

**예외**: `POST /api/v1/auth/logout`과 `DELETE /api/v1/users/me`(회원 탈퇴)는 **204**라서 응답 본문이 아예 없다. envelope도 오지 않는다.

**함정**: 같은 삭제라도 `DELETE .../images/{parfaitImageId}`(토핑 삭제)는 204가 아니라 **200 + `data: null`**이라 envelope가 그대로 온다 — 두 DELETE의 성공 표현이 다르다.

`errorDetail`은 계약에는 있으나 **서버가 현재 항상 `null`로 보낸다**(필드별 검증 메시지가 채워지지 않는다).

---

## 4. 스웨거(OpenAPI)와 실제가 다른 곳

스웨거 문서는 springdoc이 생성한 것이라 몇 군데가 실제와 어긋난다. **여기 적힌 쪽이 맞다**(서버 코드로 확인).

### 판별자 키는 `isNewUser`다 — 스웨거의 `newUser`가 틀렸다 ⚠️ 가장 중요

서버 `KakaoLoginResponse`는 Kotlin `val isNewUser: Boolean`이고, 서버가 `jackson-module-kotlin`을 쓰므로 **JSON 키에 `is` 접두사가 그대로 남는다.** 컨트롤러 테스트가 실제 응답 본문에 `$.data.isNewUser`를 단언한다.

스웨거만 `newUser`로 적는데, springdoc이 Kotlin 모듈이 없는 자기 ObjectMapper로 모델을 유도하기 때문이다 — **런타임 직렬화 결과와 다르다.** 앱 `KakaoLoginResponse`는 `@SerialName("isNewUser")`로 정정됐고 와이어 계약 테스트가 그 키를 잠근다.

### 회원가입은 200이 아니라 201이다

스웨거에는 `200 OK`로 적혀 있지만 `SignupController`는 `ResponseEntity.status(HttpStatus.CREATED)`로 내보낸다. springdoc이 `ResponseEntity`의 런타임 status를 읽지 못해 기본값 200을 문서화한 것이다.

### 약관 목록은 스웨거 `Auth` 태그 아래 있는데 경로는 `auth` 하위가 아니다

`GET /api/v1/policies`는 컨트롤러가 `http/auth` 패키지에 있어 스웨거 태그가 `Auth`로 잡힌다. 경로는 `/api/v1/auth/...`가 아니라 최상위 `/api/v1/policies`다. 태그로 경로를 유추하지 않는다.

### 스웨거에 없는 에러 코드가 많다

스웨거는 성공 응답만 열거한다. 실제 에러 코드는 `AuthErrorCode`(14종)·`ParfaitGroupApiErrorCode`(10종)·`ImageErrorCode`(4종)·`MemberErrorCode`(2종)·`ParfaitImageErrorCode`(5종)·`ParfaitErrorCode`(5종)·`CommonErrorCode`(3종)에 있고, 각 `.http` 파일 주석에 엔드포인트별로 적어뒀다.

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

### `termsId`는 하드코딩하지 않는다

회원가입이 요구하는 `termsId`는 서버가 정한 "현재 유효한 약관"의 id다. **`GET /api/v1/policies`(`policy.http` 1번)가 그 출처다** — 실행하면 `terms_id_1`·`terms_id_2` 전역 변수가 채워지고 `auth.http` 2번이 그대로 쓴다. 손으로 넣은 값이 최신 버전이 아니면 `400 TERMS_NOT_FOUND`.

서버는 **타입당 최신 1건씩**(`TERMS_OF_SERVICE`·`PRIVACY_POLICY`) 주므로 배열 길이는 **0~2**다. 0건이어도 `200`에 빈 배열이고 에러가 아니다 — 그대로 회원가입을 호출하면 `400 REQUIRED_TERMS_NOT_AGREED`가 난다. 1건뿐이면 `auth.http` 2번 바디의 항목 하나를 지워야 `400 DUPLICATE_TERMS_ID`를 피한다.

### 약관 응답의 `url`이 링크가 아닐 수 있다

`policies[].url`은 URL 전용 컬럼이 아니라 `tos.content`(`LONGTEXT`) 컬럼을 그대로 매핑한 값이다. 운영 데이터에 약관 **전문**이 들어 있으면 링크 대신 전문이 내려온다. 앱에서 WebView·브라우저로 열기 전에 실제 값을 확인해야 한다.

---

## 6. ⚠️ base URL — 평문에서 HTTPS로 옮겨 가는 중이다

서버가 도메인에 HTTPS를 적용했다(서버 PR #113, 서버 저장소 `docs/operations/https-setup.md`). 앞단 리버스 프록시가 TLS를 종단하고 애플리케이션 컨테이너의 평문 포트로 넘긴다. 그 런북은 **검증이 끝나면 평문 포트를 닫는 단계**를 두고 있다.

**앱이 해야 하는 일은 순서가 정해져 있다.**

1. `local.properties`의 `YG_BASE_URL`을 **새 HTTPS 주소로 바꾼다.** 이 키가 비어 있으면 앱은 placeholder로 빌드된다(`PropertySettingManager`의 fallback). 평문 포트가 닫히는 순간 옛 주소로 빌드된 앱은 전부 연결에 실패하므로, 이 교체가 그 시점보다 앞서야 한다.
2. 그다음 `app/src/main/AndroidManifest.xml`의 `android:usesCleartextTraffic="true"`를 **지운다.** 평문 서버에 붙으려고 넣은 임시 조치이고(PR #241), main 매니페스트라 릴리즈 빌드까지 따라간다. 서버가 HTTPS를 갖춘 뒤로는 평문 다운그레이드만 허용하는 자리로 남는다.

**순서를 뒤집으면 앱이 즉시 끊긴다** — 1번 전까지는 그 플래그가 유일한 통로다.

지우기 전에 서버가 내려 주는 URL도 함께 본다. 이미지 presigned URL과 약관 `policies[].url`이 평문이면 그쪽도 같이 막힌다(위 "약관 응답의 `url`이 링크가 아닐 수 있다" 참고).

이 `.http` 파일들은 IntelliJ가 직접 보내는 것이라 위 이야기와 무관하다 — 앱 코드에서 호출할 때만 걸린다. `http-client.env.json`의 `base_url`은 각자 채운다.

---

## 7. 자주 나오는 에러

| 코드 | code | 원인 |
|---|---|---|
| 401 | `INVALID_ID_TOKEN` | `idToken` 만료·서명 불일치, 또는 **`nonce` 불일치** |
| 401 | `EXPIRED_TOKEN` | access/refresh 만료. 재발급 또는 재로그인 |
| 401 | `INVALID_TOKEN` | 토큰 위조, 또는 refresh가 서버 저장값과 불일치(재사용 의심) |
| 401 | `UNAUTHORIZED` | `Authorization` 헤더 자체가 없음 |
| 401 | `MEMBER_NOT_FOUND` | 토큰의 회원이 존재하지 않음 (그룹·이미지 API의 404 `MEMBER_NOT_FOUND`와 **다른 코드다**) |
| 403 | `FORBIDDEN_REFRESH_TOKEN` | access token의 주인과 바디 refresh token의 주인이 다름 |
| 403 | `GROUP_NOT_JOINED` | 참여하지 않은 그룹 |
| 404 | `GROUP_NOT_FOUND` | 없는 그룹 |
| 404 | `INVALID_INVITE_CODE` | 초대코드 무효 |
| 400 | `INVALID_CONTENT_TYPE` | 이미지 MIME이 `image/png`·`image/jpeg`가 아님 |
| 404 | `IMAGE_NOT_FOUND` | 없는 `imageId`로 업로드 확인 |
| 409 | `IMAGE_ALREADY_CONFIRMED` | 이미 확정된 이미지를 다시 확인 (재시도 안전장치가 아니다) |
| 409 | `ALREADY_REGISTERED` | 이미 가입된 회원인데 signup 호출 |
| 409 | `GROUP_ALREADY_JOINED` · `GROUP_MEMBER_LIMIT_REACHED` | 참여 관련 충돌 |
| 409 | `PARFAIT_ALREADY_CLOSED` | 마감된(`CLOSED`·`EMPTY`) 캔버스에 쓰기 — 토핑 배치·수정·테두리·삭제와 배경 변경 다섯 경로 전부 |
| 400 | `TERMS_NOT_FOUND` · `DUPLICATE_TERMS_ID` · `REQUIRED_TERMS_NOT_AGREED` | 약관 관련 |
| 400 | `INVALID_GROUP_NAME` · `INVALID_GROUP_NICKNAME` · `INVALID_GROUP_MEMBER_LIMIT` · `INVALID_GROUP_REPORT_REASON` | 그룹 입력값 |

> `MEMBER_NOT_FOUND`는 **`AuthErrorCode`에서 401, `ParfaitGroupApiErrorCode`와 `ImageErrorCode`에서 404**로 중복 정의돼 있다. 코드 문자열만으로 분기하면 세 상황이 뭉개진다 — HTTP status와 함께 봐야 한다.

---

## 8. 파일 구조

```
http/
├── README.md                     # 이 문서
├── _reset.http                   # 전역 변수 초기화·확인 (API 테스트 아님)
├── auth.http                     # 인증 4종
├── policy.http                   # 약관 목록 조회
├── parfait-group.http            # 그룹 8종
├── parfait.http                  # 파르페 조회
├── health.http                   # 헬스체크
├── images.http                   # 이미지 업로드 2종 (+ S3 PUT)
├── users.http                    # 내 계정 조회 · 전역 닉네임 변경 · 탈퇴
├── parfait-image.http            # 토핑 배치 확정 · 위치/크기/각도 수정 · 테두리 수정 · 삭제
├── http-client.env.json          # 환경 변수 구조(값 비움, 커밋됨)
└── http-client.private.env.json  # 실제 값 (gitignore — 커밋되지 않음)
```

`private` 파일이 있으면 IntelliJ가 그 값을 우선한다. **토큰·서버 주소 같은 실제 값은 반드시 `private` 쪽에** 넣는다.

---

## 참고

- 서버 API 계약 문서: 문서 저장소 `parfait/api/`
- [IntelliJ HTTP Client 문서](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
