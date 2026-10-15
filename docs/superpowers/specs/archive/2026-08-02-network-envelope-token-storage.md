---
id: network-envelope-token-storage
title: 서버 계약 정합 — envelope·에러 파싱·암호화 토큰 저장
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-04
related_code: ApiResponse.kt, ApiCaller.kt, ApiException.kt#Business, AuthInterceptor.kt, TokenProvider.kt, TokenStoreTokenProvider.kt, NoAuth.kt, CryptoManager.kt, TokenStore.kt, EncryptedTokenStore.kt, NetworkModule.kt#provideTokenProvider, LocalDataSourceModule.kt#bindTokenStore, data/proguard-rules.pro, http/
related_adr: ADR-0017, ADR-0008, ADR-0004
related_spec: data-network-setup
related_architecture: data-layer
supersedes:
superseded_by:
tags: [spec, parfait, network, data, auth, security]
---

# 서버 계약 정합 — envelope·에러 파싱·암호화 토큰 저장

## 배경

`:data`의 원격 네트워크 기초 구조는 [ADR-0017](../../../adr/0017-remote-network-datasource.md)로 확정돼
develop에 머지됐다(#174). 그러나 서버 계약과 대조한 적이 없어 실제 API를 호출하면 동작하지 않는다.

`parfait/api/` 계약 문서 체계를 세우며 서버(`mash-up-kr/TEAMYG-SERVER` `main` `6f5bffc`)와 1:1 대조한
결과가 근거다([api/conventions.md](../../../api/conventions.md) "Android 불일치", [api/spec/](../../../api/spec/README.md)).

### 확인된 결함

| # | 결함 | 결과 |
|---|---|---|
| 1 | `ApiResponse`에 `success`·`errorDetail` 필드 없음 | 서버가 보내는 두 필드를 소비 못 함 |
| 2 | `isSuccess`가 `code == "SUCCESS"` 단일 비교(`SUCCESS_CODE`가 `TODO`) — 서버는 `"OK"`/`"CREATED"` | **현 상태로 모든 호출이 실패 판정** |
| 3 | **에러 envelope에 도달할 수 없음** | 아래 별도 서술 |
| 4 | envelope 없는 204 응답을 받을 진입점 없음 | `logout` 소비 불가 |
| 5 | `TokenProvider` 구현이 항상 null | 화이트리스트 밖 전 API 401 |

### 3번이 가장 크다

서버 `GlobalExceptionHandler`는 `ResponseEntity.status(errorCode.status).body(ApiResponse.error(...))`를
반환한다 — **에러가 HTTP 4xx/5xx로 나간다.** Retrofit은 suspend 함수가 `ApiResponse<T>`를 직접 반환할 때
4xx/5xx를 `HttpException`으로 던지므로, 현재 `safeApiCall`은 `ApiException.Http(statusCode)`로만 잡고
**envelope 안의 `code`·`message`·`errorDetail`에 영영 도달하지 못한다.**

결과: `INVALID_ID_TOKEN`·`EXPIRED_TOKEN`·`MEMBER_NOT_FOUND`를 구분할 수 없어 **재로그인 분기와 재시도
판단이 불가능**하다. 반대로 `ApiException.Business`는 사실상 죽은 분기다 — 서버가 200 + `success:false`를
보내는 경로가 없기 때문이다.

## 목표

서버 계약대로 응답을 해석하고, 인증이 필요한 API를 호출할 수 있는 상태까지 `:data`를 정리한다.
실제 도메인 API 구현은 이 스펙의 범위가 아니다.

## 범위

**포함**
- `ApiResponse` 필드 보강 + 성공 판정 교체
- 에러 envelope 파싱 경로 신설, `ApiException.Business` 확장
- envelope 없는 204 진입점 신설
- 암호화 토큰 저장소 신설 + `TokenProvider` 실구현
- DI 배선, ADR 2건(신설 1·갱신 1)

**제외**
- auth 도메인의 Service·Response·RemoteDataSource·Repository 구현 — 다음 라운드
- **401 자동 재발급**(OkHttp `Authenticator`로 refresh 호출 후 재시도) — 토큰 저장이 선행돼야 설계 가능
- 화면·카카오 SDK 연동·`nonce` 생성
- `TempService` 등 예시 세트 제거 — 실제 API가 들어올 때 함께 정리

## 설계

### 1. envelope · 성공 판정

`ApiResponse<T>`를 서버 `parfait.common.response.ApiResponse`와 필드 단위로 맞춘다.

| 필드 | 타입 | 비고 |
|---|---|---|
| `success` | Boolean | **신규** |
| `code` | String | |
| `message` | String | |
| `data` | T? | |
| `errorDetail` | Map<String, String>? | **신규**, 기본값 `null` |

- 성공 판정은 **`success` 필드를 그대로 쓴다.** 별도의 `isSuccess` computed property는 두지 않는다 —
  기존 `isSuccess`(`code == SUCCESS_CODE` 단일 비교)와 `SUCCESS_CODE`·`TODO` 주석을 제거하고,
  `ApiCaller`가 `response.success`를 직접 검사한다. 성공 코드 문자열이 늘어도(`"OK"`·`"CREATED"` 외
  신규) 깨지지 않기 때문이다. `code`는 분기용으로만 쓴다.
- `errorDetail`은 **서버가 현재 항상 `null`로 보낸다**(`GlobalExceptionHandler`의 네 핸들러가 인자 없이
  `ApiResponse.error(errorCode)`를 호출). 계약에 있으므로 필드는 두되, 값이 온다고 가정한 UI를 만들지 않는다
  → [api/conventions.md](../../../api/conventions.md).

### 2. 에러 envelope 파싱 — `ApiCaller`

**`SafeApiCall.kt`의 top-level 함수들을 클래스 `ApiCaller`로 승격한다.**

에러 바디를 역직렬화하려면 `@RemoteJson` `Json` 인스턴스가 필요하다. top-level 함수로 두면 호출부마다
`Json`을 인자로 넘겨야 하고, 파일 내 `private val`로 두면 DI를 우회해 `@RemoteJson`/`@LocalJson` 분리
([ADR-0017](../../../adr/0017-remote-network-datasource.md))가 무의미해진다. `@Inject constructor(@RemoteJson json)`을
받는 클래스가 두 문제를 동시에 해소하고, remote DataSource가 생성자 주입으로 받는다.

진입점은 **셋**이다.

| 메서드 | 서버 응답 | 반환 |
|---|---|---|
| `safeApiCall` | envelope + `data` 필요 | `Result<T>` |
| `safeApiCallWithoutData` | envelope, `data` 안 봄 | `Result<Unit>` |
| `safeApiCallNoContent` | **본문 자체가 없음**(204) | `Result<Unit>` |

`safeApiCallNoContent`는 서비스 메서드가 `Unit`을 반환하는 경우다 — `logout`이 여기 해당한다
([api/spec/auth-logout.md](../../../api/spec/auth-logout.md): 컨트롤러가 Unit 반환 + `@ResponseStatus(NO_CONTENT)`라
응답 본문이 비어 있다). `safeApiCallWithoutData`로는 파싱할 JSON이 없어 처리할 수 없다.

**에러 경로**: `catch (e: HttpException)`에서 `e.response()?.errorBody()?.string()`을 `ApiResponse<Unit>`로
역직렬화 시도한다.

- 성공 → `ApiException.Business(code, serverMessage, statusCode, errorDetail)`
- 바디가 없거나 파싱 실패 → 기존 `ApiException.Http(statusCode, e)`로 폴백

**폴백을 반드시 둔다.** 서버가 항상 envelope를 준다고 가정하면 안 된다 — 인프라 계층(게이트웨이·WAF)에서
나오는 429·502는 envelope 없이 올 수 있고, 실제로 팀 명세의 429는 서버 코드에 구현이 없다
→ [open-questions](../../../synthesis/open-questions.md).

`errorBody()`는 **한 번만 읽을 수 있다**(`ResponseBody`는 일회성 스트림). `string()` 호출을 한 곳으로
모으고 결과를 지역 변수에 담는다.

### 3. `ApiException.Business` 확장

```
Business(code: String, serverMessage: String, statusCode: Int?, errorDetail: Map<String, String>?)
```

`statusCode`를 더하는 이유는 **코드 문자열이 enum 간 유일하지 않기 때문이다.** `MEMBER_NOT_FOUND`가
`AuthErrorCode`에서는 401, `ParfaitGroupApiErrorCode`에서는 404다 → [api/conventions.md](../../../api/conventions.md).
`code` 단독으로 분기하면 "인증 실패"와 "그룹의 대상 회원 없음"이 한 갈래로 뭉개진다. 소비자는
**`code`와 `statusCode`를 함께 보고 판정**한다.

`EmptyBody`·`Http`·`Network`·`Unknown`은 그대로 둔다.

### 4. 토큰 저장

세 조각으로 나눈다. 각각 한 가지 책임만 갖는다.

**`CryptoManager`** — Android Keystore에 AES/GCM 키를 만들고 문자열을 암복호화한다. 저장 매체를 모른다.
- 키 별칭 1개, `KeyGenParameterSpec`(`ENCRYPT`/`DECRYPT`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`)
- GCM은 **IV를 매번 새로 생성**해야 한다. 암호문과 IV를 함께 보관해야 복호화할 수 있으므로
  `IV + 암호문`을 이어붙여 Base64 문자열 하나로 만들어 반환한다.

**`TokenStore`**(인터페이스) + **`EncryptedTokenStore`** — `CryptoManager`로 암호화한 문자열을
Preferences DataStore에 넣고 뺀다. 전부 suspend.
- `getAccessToken(): String?` · `getRefreshToken(): String?` · `save(accessToken, refreshToken)` · `clear()`
- DataStore 인스턴스는 기존 `DataStoreModule`이 제공하는 `DataStore<Preferences>`를 재사용한다
  ([ADR-0008](../../../adr/0008-datastore-local-persistence.md)).

**`TokenProvider`** — ADR-0017이 만든 **동기 추상화를 유지**하고 구현만 교체한다
(`EmptyTokenProvider` → `TokenStoreTokenProvider`). 인터페이스를 남기는 이유는 인터셉터를 테스트할 때
fake를 끼울 자리가 필요하기 때문이다. `EmptyTokenProvider`는 삭제한다.

**`AuthInterceptor`는 시그니처를 바꾸지 않는다.** `TokenProvider.getToken()`이 동기인 채로 남고,
`TokenStoreTokenProvider` 내부에서 `runBlocking { tokenStore.getAccessToken() }`으로 suspend 경계를 넘는다.

> **`runBlocking` 사용 근거(리뷰 대비)** — OkHttp `Interceptor.intercept`는 동기 API라 suspend를 직접
> 호출할 수 없다. 이 코드는 OkHttp dispatcher 스레드에서 실행되므로 **메인 스레드를 막지 않는다**.
> 대안이던 "메모리 캐시(StateFlow) + 동기 읽기"는 앱 시작 직후 캐시가 비어 있는 창(window)에서 첫 요청이
> 토큰 없이 나가는 타이밍 문제를 새로 만든다. 코루틴 규율 이탈이라는 지적은 타당하나, 이 경계에서는
> 의도된 선택이다.

**인증 헤더 스킵**: 인증이 불필요한 엔드포인트(`kakao`·`signup`·`reissue` 등 서버 화이트리스트 경로)는
서비스 메서드에 `@NoAuth`(`network/NoAuth.kt`)를 붙여 표시한다. `AuthInterceptor`는
`chain.request().tag(Invocation::class.java)?.method()?.isAnnotationPresent(NoAuth::class.java)`로
판정한다.

> ⚠️ **as-built(PR #190 develop 머지)** — 설계는 스킵 대상이면 `tokenProvider.getToken()` **호출 자체를
> 생략**해 `runBlocking`·DataStore 읽기·Keystore 복호화를 아낀다고 적었으나, 머지된 코드는
> **`getToken()`을 항상 호출하고 헤더 부착 조건만 `token != null && skipAuth.not()`으로 게이팅**한다
> (early return 없음). 갈림은 **코드 리뷰 반영으로 확정**됐다 — PR #190의 마지막 커밋(`refactor: 코드
> 리뷰 반영`)이 early return을 걷어낸 단일 변경이다. **헤더 부착 결과는 네 경우 모두 동일**하다 — 갈리는 것은 비용뿐으로,
> 화이트리스트 경로 요청마다 토큰 조회가 한 번씩 더 일어난다. 이 라운드 시점엔 `@NoAuth`를 붙인
> 서비스 메서드가 develop에 0건이라(auth·policy Service가 `data-api-service-layer` 미머지) 실제
> 비용은 아직 발생하지 않는다.

이전에는 경로 문자열 상수(`NO_AUTH_HEADER_PATHS`)를
`AuthInterceptor`에 하드코딩해 서버 `SecurityConfig.WHITELIST_PATHS`와 이중 관리했으나, 선언을
서비스 인터페이스의 URL 옆으로 옮겨 오타가 컴파일 타임에 걸리게 했다 —
[ADR-0017](../../../adr/0017-remote-network-datasource.md) "인증" 참고.

### 5. 키 유실 처리 (핵심)

기기 복원·잠금 자격증명 변경 등으로 Keystore 키가 무효화되면 복호화가 예외를 던진다.

**`EncryptedTokenStore`는 복호화 예외를 밖으로 던지지 않는다.** 예외를 잡아 `clear()`를 호출하고
`null`을 반환한다. 앱은 "토큰 없음" 상태가 되어 자연스럽게 재로그인 경로로 간다.

예외를 전파하면 `TokenProvider.getToken()` → `AuthInterceptor.intercept`에서 터져 **모든 네트워크 요청이
죽는다.** 사용자가 앱을 지우기 전까지 복구할 수 없는 상태가 된다.

> ⚠️ **as-built** — 구현된 `read()`는 설계보다 넓게 잡는다. `runCatching`이 복호화만이 아니라
> **DataStore 읽기까지** 감싸고, 복구 경로의 `clear()`도 `runCatching`으로 감싼다. 키 무효화 외에
> 저장소 I/O 실패도 같은 경로로 떨어지며, 삭제가 실패해도 `null` 반환은 보장된다
> → [ADR-0019](../../../adr/0019-encrypted-token-storage.md).

### 6. DI 배선

[ADR-0017](../../../adr/0017-remote-network-datasource.md)의 "DI 모듈은 역할당 1파일 평면 배치" 관용을 따른다.
**하위 패키지를 새로 만들지 않는다.**

| 대상 | 어디에 | 방식 |
|---|---|---|
| `TokenProvider` | `NetworkModule.provideTokenProvider` | 반환을 `EmptyTokenProvider` → `TokenStoreTokenProvider`로 교체(기존 `@Provides` 수정) |
| `TokenStore` ← `EncryptedTokenStore` | `LocalDataSourceModule` | `@Binds` `@Singleton` 1개 추가 — 이 파일이 이미 로컬 저장소 계열(`FileCameraCache`·`RecentImage`) 바인딩을 모으고 있다 |
| `CryptoManager` | 없음 | `@Inject constructor`로 해소된다. `@Provides` 불필요 |
| `ApiCaller` | 없음 | 동일 — `@Inject constructor(@RemoteJson json)` |

`EmptyTokenProvider.kt`는 삭제한다. 새 DI 파일을 만들지 않는다.

## 스펙 범위 밖 동반 변경 (as-built, PR #190)

머지된 PR은 위 설계 외에 두 가지를 함께 들여왔다. 설계 단계에 없던 것이라 여기 사후 기록한다.

- **R8 유지 규칙** — `data/proguard-rules.pro`에 `-keep @interface …network.NoAuth`가 추가됐다.
  런타임에 어노테이션을 읽으므로 규칙 자체는 필요하지만, **develop 기준으로 이 규칙은 아무 곳에도
  전달되지 않는다.** `:data`는 Android 라이브러리 모듈이라 앱의 R8 실행에는 `consumerProguardFiles`로
  명시한 규칙만 전달되는데, develop의 컨벤션 플러그인(`AndroidConfig.kt#setConfigAndroidLibrary`)은
  `consumerProguardFiles`도 `proguardFiles`도 등록하지 않고 `data/consumer-rules.pro`는 비어 있다
  (`consumerProguardFiles` 문자열이 develop 전체에 0건). 같은 문제를 `data-api-service-layer` 라운드가
  이미 발견해 `consumer-rules.pro` 이관 + 컨벤션 플러그인 등록으로 고쳤으나 **그 브랜치가 미머지**라,
  develop에는 무효한 배치만 남았다 → [ADR-0017](../../../adr/0017-remote-network-datasource.md) "인증",
  [open-questions](../../../synthesis/open-questions.md).
- **`http/` 요청 모음** — IntelliJ HTTP Client로 서버 API를 직접 호출하는 요청 파일 묶음
  (`auth.http`·`parfait-group.http`·`parfait.http`·`health.http`·`_reset.http` + `http-client.env.json`
  + `README.md`)이 저장소 루트에 신설되고, 실제 주소·토큰이 들어가는 `http-client.private.env.json`은
  `.gitignore`에 등록됐다. 계약 근거는 [api/](../../../api/README.md) 문서와 같은 서버 코드이므로 **두
  표면이 같은 계약을 이중 관리**하게 된다 → [open-questions](../../../synthesis/open-questions.md).

## 검증

**이 라운드의 약점이다.** 코드베이스에 `test`·`androidTest` 디렉토리가 **하나도 없고**(무테스트 관례,
[ADR-0017](../../../adr/0017-remote-network-datasource.md)에도 명시), Android Keystore는 JVM 유닛 테스트에서
동작하지 않는다. 계측 테스트 인프라를 이 스펙에서 새로 세우지 않는다.

대신:

1. `:data:assembleDebug` · ktlint 통과
2. `:app:assembleDebug`로 **Hilt 그래프 전체 해소** 확인
   (`CryptoManager`→`TokenStore`→`TokenProvider`→`AuthInterceptor`→`OkHttpClient`→`Retrofit` 체인,
   `@RemoteJson` 한정자로 `ApiCaller` 해소)
3. **실기기 암복호화 왕복 검증은 수행 불가 → 로그인 연동 라운드로 이월.** 사람이 수행하면 된다고 봤으나,
   `TokenStore.save()` 호출부가 코드베이스에 **0건**이라 저장을 트리거할 방법 자체가 없다(이 라운드
   범위에서 auth 도메인 Service·RemoteDataSource·Repository 구현이 빠져 있어서다). 로그인이 실제로
   붙어 `save()`가 호출되는 다음 라운드로 미룬다.
4. 에러 파싱은 실제 서버 호출 없이 확인하기 어렵다. auth 서비스가 들어오는 다음 라운드에서
   `INVALID_ID_TOKEN` 등 실제 에러 코드가 `Business`로 잡히는지 확인한다.

**미검증으로 남는 것**: 키 유실 경로(재현이 어렵다), 실기기 암복호화 왕복(위 3번, 트리거 수단 부재),
에러 envelope 파싱의 실서버 동작. [open-questions](../../../synthesis/open-questions.md)에 등록하고
다음 라운드로 넘긴다.

> **머지 시점 재확인(2026-08-04, PR #190)** — 위 미검증 3건은 그대로 남는다. `TokenStore.save()`
> 호출부는 develop 기준 여전히 **0건**이고(auth 도메인이 미머지), 서버로 나간 요청도 0건이다.
> 즉 이 라운드 산출물은 **컴파일·Hilt 그래프 해소까지만 확인된 채 develop에 들어왔다.**

## 문서 산출물

- **ADR-0019 신설** — 토큰 암호화 저장(Android Keystore AES/GCM + Preferences DataStore).
  대안으로 검토한 Tink·EncryptedSharedPreferences와 기각 사유, 키 유실 시 `clear()` 정책을 담는다.
- **[ADR-0017](../../../adr/0017-remote-network-datasource.md) 갱신** — 성공 판정 근거를 `success` 필드로 교체,
  진입점 2개 → 3개, `safeApiCall` top-level 함수 → `ApiCaller` 클래스 승격과 그 근거, 에러 envelope 파싱,
  `TokenProvider` 실구현.
- **[architecture/data-layer.md](../../../architecture/data-layer.md)** — 토큰 저장 경로와 `ApiCaller` 사용법 반영.

## 열린 질문

- ~~`runBlocking`을 인터셉터에서 쓰는 것이 코드리뷰를 통과할지 미확정.~~ **통과했다**(PR #190 머지 —
  리뷰 반영 커밋은 `AuthInterceptor`의 early return만 걷어냈고 `TokenStoreTokenProvider`는 무수정).
- 401 자동 재발급(`Authenticator`)을 다음 라운드에 붙일 때, `reissue`가 **화이트리스트라 인증 헤더를
  받지 않는다**는 점이 설계에 영향을 준다 → [api/spec/auth-reissue.md](../../../api/spec/auth-reissue.md).
- 키 유실 시 `clear()` 후 재로그인 유도가 UX상 충분한지(예: 사용자에게 알릴지) 미결.
