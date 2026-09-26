---
id: ADR-0017
title: 원격 네트워크 DataSource·서비스 규약 (AndroidNetworkConventionPlugin + ApiResponse/safeApiCall)
status: accepted
date: 2026-07-26
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0001, ADR-0003, ADR-0004
related_spec: data-network-setup, network-envelope-token-storage, data-api-service-layer, image-api-service-layer, member-parfait-image-api-service-layer
related_architecture: data-layer
platforms: android
tags: [adr, parfait, network, data]
---

# ADR-0017: 원격 네트워크 DataSource·서비스 규약

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락

`:data` 모듈은 `libs.bundles.network`(Retrofit·OkHttp·kotlinx-serialization 컨버터) 의존만 준비돼
있었고 Retrofit 서비스 정의는 없었다([[data-layer]] "네트워킹" 섹션 참고). 원격 API 연동을 시작하려면
컨벤션 플러그인 배치, 공통 응답 계약, 인증 헤더 주입 자리, remote DataSource 배치 관례를 먼저
확정해야 한다.

## 결정

network 관심사를 전용 컨벤션 플러그인으로 응집하고, 공통 응답 envelope·인증 자리·remote
DataSource 배치 관례를 확립한다.

- **컨벤션 플러그인**: `AndroidNetworkConventionPlugin`(plugin id `com.teamyg.parfait.plugin.android.network`,
  alias `parfait-android-network`)이 `buildConfig` 활성화 + `BuildConfig.BASE_URL` buildConfigField를
  부여한다. `NetworkConfig`의 `setConfigNetwork`가 값을 채우고, `PropertySettingManager`의
  `loadBaseUrl`이 project property → `local.properties`(`YG_BASE_URL`) → placeholder fallback
  순으로 조회한다(서명 키 로드 관례와 동형). `libs.bundles.network`·kotlinx-serialization 의존과
  serialization 플러그인 적용을 `ModuleDataConventionPlugin`에서 이 플러그인으로 이관한다.
- **DI 배치**: DI 모듈은 `di/` **평면 배치 + 역할당 파일 1개**다(하위 패키지를 만들지 않는다) —
  `RepositoryModule`·`LocalDataSourceModule`·`RemoteDataSourceModule`·`ServiceModule`·
  `NetworkModule`·`JsonModule`·`DataStoreModule`·`SingletonInjectModule`. 도메인이 늘면 해당 역할
  파일에 바인딩을 **추가**한다.
  - `NetworkModule`(object, `@InstallIn(SingletonComponent::class)`): `provideTokenProvider`
    (=`EmptyTokenProvider`, **as-built: `TokenStoreTokenProvider` — 아래 "인증" 절 참고**)·
    `provideAuthInterceptor`·`provideOkHttpClient`·`provideRetrofit`.
  - `ServiceModule`(object): Retrofit 서비스 생성(`provideTempService` 등). 설정 코드인
    `NetworkModule`과 서비스 목록을 분리해 둔다.
  - `RemoteDataSourceModule`(interface, `@Binds`): `TempRemoteDataSourceImpl` → 인터페이스.
  - 도메인별(`di/repository/<도메인>` 등) 하위 패키지 분할은 **하지 않는다**: 바인딩 1개짜리 파일이
    양산돼 "이 Repository가 어디 묶였나"를 매번 찾게 된다. 분할의 근거였던 rebase 충돌은 `@Binds`
    append라 해소가 기계적이고(양쪽 유지), 실제로 DI를 동시에 건드리는 병렬 브랜치도 관측되지
    않았다. 역할 파일 하나가 감당하기 어려울 만큼 커지면 그때 그 파일만 쪼갠다.
- **`Json` 한정자**: `Json`은 용도별로 분리해 `@Qualifier`로 구분한다 — 로컬(DataStore)용
  `@LocalJson`, 원격(Retrofit)용 `@RemoteJson`. 두 한정자는 `model/qualifier` 패키지에 두고,
  **두 인스턴스 모두 `JsonModule`(`provideLocalJson`·`provideRemoteJson`)이 제공**한다(같은 관심사를
  한 파일에 모아 설정 차이를 나란히 본다 — `DataStoreModule`은 `DataStore<Preferences>`만 제공).
  같은 `SingletonComponent`에 동일 타입 `Json`이 둘이어도 한정자로 구분돼 중복 바인딩이 아니다.
  현재 두 인스턴스의 직렬화 설정(`ignoreUnknownKeys`·`coerceInputValues`·`encodeDefaults`)은
  동일하나, 원격은 서버 응답 규약에 맞춰 독립적으로 조정할 수 있다.
- **응답 계약**: 공통 `ApiResponse<T>`(`code`/`message`/`data`, `@Serializable`, `isSuccess`
  프로퍼티)를 모든 서비스 응답 타입으로 쓰고, `SafeApiCall.kt`의 함수가 이를 `Result<T>`로 변환한다.
  **진입점은 payload 유무로 둘**이다 — `safeApiCall`(payload 필요: 성공 코드 + `data` 존재를 모두
  요구)과 `safeApiCallWithoutData`(payload 없음: `ApiResponse<Unit>`를 받아 성공 코드만 검사하고
  `data`를 보지 않음, `Result<Unit>` 반환). 삭제·설정 변경처럼 본문 없는 응답을 단일 진입점에서
  `data != null`로 판정하면 성공 호출이 실패로 분류되기 때문이다.

  > ⚠️ **as-built 갱신(`network-envelope-token-storage` 라운드 — 2026-08-04 PR #190으로 develop 머지)** — 위 원안은
  > 서버 계약 대조 전 설계였다. 실제로는 세 지점이 바뀌었다.
  > - **성공 판정**: `code == SUCCESS_CODE`(단일 상수) 대신 **`success` 필드**를 그대로 쓴다.
  >   서버가 성공 코드를 `"OK"`·`"CREATED"` 2종으로 쓰기 때문에 단일 상수 비교가 애초에 불가능했다.
  >   `isSuccess` 프로퍼티는 제거됐다 — `ApiResponse.success`가 그 역할을 대신한다.
  > - **진입점**: 2개에서 **3개**로 늘었다. `safeApiCallNoContent`(`network/ApiCaller.kt`)가 신설돼
  >   `logout`처럼 204라 응답 본문 자체가 없는 API를 처리한다 — 기존 `safeApiCallWithoutData`는
  >   `ApiResponse<Unit>` envelope를 전제하므로 본문 자체가 없는 응답은 파싱할 수 없다.
  > - **소속**: `SafeApiCall.kt`의 top-level 함수들이 **`ApiCaller` 클래스**(`network/ApiCaller.kt`)로
  >   승격됐다. 에러 envelope 역직렬화에 `@RemoteJson` `Json`이 필요한데, top-level 함수는 호출부마다
  >   `Json`을 인자로 넘겨야 하고 파일 내 `private val`로 두면 `@LocalJson`/`@RemoteJson` 분리(아래
  >   "`Json` 한정자")가 무의미해지기 때문이다. `SafeApiCall.kt`는 삭제됐다.
- **에러 타입 계층**: 실패는 sealed `ApiException`(`ApiException.kt`)으로 분류한다 — `Business`(HTTP
  성공이나 envelope 실패 코드), `EmptyBody`(envelope 성공 코드인데 `data` 없음 — payload가 필요한
  호출에서만 실패), `Http`(4xx/5xx, `HttpException` + `statusCode`), `Network`
  (`IOException`), `Unknown`(그 외). `Business`와 `EmptyBody`를 나눈 이유는 "서버가 거절"과 "계약과
  다른 빈 응답"의 대응(재시도·재인증 vs 계약 점검)이 다르기 때문이다.
  `safeApiCall`이 `try/catch`로 예외를 이 타입들에 매핑하고,
  `CancellationException`은 다시 던져 코루틴 취소 전파를 보존한다(`runCatching`의 취소 삼킴 회피).
  소비자는 `Result.exceptionOrNull()`을 `ApiException`으로 분기해 재시도·재인증 등을 판단할 수 있다.

  > ⚠️ **as-built 갱신(`network-envelope-token-storage` 라운드 — 2026-08-04 PR #190으로 develop 머지)** — `Business`에
  > **`statusCode: Int?`·`errorDetail: Map<String, String>?`이 추가**됐다. 코드 문자열이 enum 간
  > 유일하지 않아서다(`MEMBER_NOT_FOUND`가 401/404 둘 다로 쓰인다). 또한 **에러가 HTTP 4xx/5xx로
  > 오므로 `HttpException` 바디를 파싱해야 envelope에 도달한다** — 이걸 안 하면 이 절이 설계한
  > `Business` 분기가 죽은 코드가 된다. `ApiCaller`의 `toApiException(e: HttpException)`이
  > `e.response()?.errorBody()?.string()`을 `ApiResponse<Unit>`로 역직렬화 시도해 성공하면
  > `Business(statusCode = e.code(), ...)`를, 실패하면 기존 `Http(statusCode, e)`로 폴백한다.
  > `statusCode`가 nullable인 이유는 2xx인데 `success=false`인 경로(현재 서버엔 없음, `HttpException`을
  > 거치지 않고 envelope에서 직접 실패 판정되는 경우)에서는 HTTP 상태 코드를 알 수 없기 때문이다.
- **인증**: `AuthInterceptor`가 `TokenProvider`(인터페이스, stub 구현 `EmptyTokenProvider`)로부터
  토큰을 받아 `Authorization: Bearer` 헤더를 주입할 자리를 만든다. 현재 `EmptyTokenProvider`는
  항상 null을 반환 — 실제 토큰 소스 연동은 후속.

  > ⚠️ **as-built 갱신(`network-envelope-token-storage` 라운드 — 2026-08-04 PR #190으로 develop 머지)** — `EmptyTokenProvider`가
  > **`TokenStoreTokenProvider`로 교체**됐다(`EmptyTokenProvider`는 삭제). `AuthInterceptor`·
  > `TokenProvider` 인터페이스는 시그니처 변경 없음 — 구현체만 바뀌었다. 토큰을 어디에 어떻게
  > 저장하는지, 동기 인터페이스를 유지한 채 suspend 저장소를 어떻게 연결하는지는
  > [ADR-0019](0019-encrypted-token-storage.md) 소관.

  > ⚠️ **as-built 갱신(`network-envelope-token-storage` 라운드 — 2026-08-04 PR #190으로 develop 머지)** — 서버
  > 화이트리스트 경로(`kakao`·`signup`·`reissue`)에 `Authorization` 헤더를 붙이지 않는 판정 방식이
  > **경로 문자열 상수 목록**(`AuthInterceptor` 내 하드코딩, 서버 `SecurityConfig.WHITELIST_PATHS`와
  > 별도 관리)에서 **`@NoAuth` 어노테이션 + Retrofit `Invocation` 태그** 조회로 교체됐다.
  > `AuthInterceptor.intercept`가
  > `chain.request().tag(Invocation::class.java)?.method()?.isAnnotationPresent(NoAuth::class.java)`로
  > 스킵 여부를 판정한다(`network/NoAuth.kt`). 경로 문자열을 서버 화이트리스트와 이중 관리하지
  > 않아도 되고(오타는 서비스 인터페이스 컴파일 타임에 걸린다), 선언이 서비스 메서드의 URL 옆에
  > 붙는다 — **앱이 아는 것은 "이 호출에 토큰을 붙일지"이지 서버 보안 설정이 아니기 때문**이다.
  >
  > **판정 후 동작(develop 확정형)** — 초안은 스킵 대상이면 `tokenProvider.getToken()` 호출 자체를
  > 생략(early return)해 `runBlocking`·DataStore 읽기·Keystore 복호화를 아끼는 형태였으나, PR #190
  > 코드 리뷰 반영으로 **early return을 걷어내고 헤더 부착 조건만 `token != null && skipAuth.not()`으로
  > 게이팅**하는 형태가 머지됐다. 헤더 부착 결과는 동일하고, 화이트리스트 경로에서 토큰 조회 비용만
  > 추가로 든다.
  >
  > **R8 유지 규칙 — 유효한 자리로 정리됐다(2026-08-06, PR #197 머지).** 어노테이션을 런타임에
  > 읽으므로 `-keep @interface`가 필요한데, 규칙이 있어야 할 곳은 `data/proguard-rules.pro`가 아니라
  > **`data/consumer-rules.pro`**다. `:data`는 Android **라이브러리** 모듈이라 `proguardFiles`는 그
  > 모듈 자체의 R8 실행에만 적용되고, 앱(`:app`)의 R8 실행에는 `consumerProguardFiles`로 명시한
  > 규칙만 전달되기 때문이다. PR #190이 keep 규칙을 무효한 자리(`data/proguard-rules.pro`)에 넣어
  > 머지한 뒤, PR #197이 이를 `data/consumer-rules.pro`로 옮기고
  > `AndroidConfig.kt#setConfigAndroidLibrary`에 `consumerProguardFiles("consumer-rules.pro")`를
  > 등록했다. develop의 라이브러리 모듈 전부가 `consumer-rules.pro`를 이미 갖고 있어 등록이 다른
  > 모듈을 깨지 않는다. 잔여 미검증 항목(Retrofit 밖 OkHttp 직접 요청, release 빌드 실행 확인)은
  > [open-questions](../synthesis/open-questions.md).
- **로깅**: `HttpLoggingInterceptor` 레벨을 `BuildConfig.DEBUG`로 게이팅한다(debug=`BODY`,
  release=`NONE`). release 빌드에서 `Authorization` 토큰과 요청/응답 바디가 로그로 노출되는 것을
  막기 위한 결정이다. 여기에 더해 `redactHeader("Authorization")`로 **debug 빌드에서도 헤더 값을
  가린다** — release 게이팅만으로는 개발 중 logcat에 access token이 그대로 남기 때문이다.
  다만 redact 대상은 헤더뿐이라 **요청 바디에 실리는 refresh token은 여전히 평문**이다
  → [open-questions](../synthesis/open-questions.md). `OkHttpClient`는 connect/read/write 타임아웃
  3종을 설정한다.
  > 📌 **표면이 셋이 되면서 이 규칙에 예외가 생겼다**(2026-08-20 develop 머지, PR #322).
  > S3 presigned PUT 전용 `@UploadClient`
  > 클라이언트는 **로깅 인터셉터를 아예 달지 않는다.** presigned URL은 서명을 쿼리 스트링에 싣는
  > 방식이라 **URL 자체가 자격증명**이고, `HttpLoggingInterceptor`는 `HEADERS` 이상 모든 레벨에서
  > 요청 라인을 남기며 `redactHeader`로는 URL을 가릴 수 없다. 이 표면이 보내는 요청은 그 PUT
  > 하나뿐이라 로깅으로 얻는 값이 노출을 감수할 만큼 크지 않다. 같은 표면만 **`callTimeout`을
  > 추가로 둔다** — `writeTimeout`은 바이트 사이 유휴 상한이라 전송 전체가 느린 것을 못 잡는다.
  > 이로써 OQ-P-030(타임아웃·클라이언트 분리·재시도)이 닫혔다. 근거는
  > [specs/2026-08-20-c106-topping-place-api](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md).
- **remote DataSource 배치**: 원격 DataSource는 `source.<도메인>.remote` 패키지에 인터페이스+`Impl`
  쌍으로 둔다. 예시 1세트로 `TempService`+`TempRequest`/`TempResponse`+`TempRemoteDataSource`(+`Impl`)를
  두고, `RemoteDataSourceModule`(`@Binds`)로 바인딩한다.

  > ⚠️ **as-built 갱신(`data-api-service-layer` 라운드 — 2026-08-06 PR #197로 develop 머지)** —
  > `Temp*` 예시 6파일이 **삭제**되고 실제 도메인 4세트(`auth`·`policy`·`group`·`parfait`)로
  > 대체됐다. Service는 계약 문서와 1:1이고(`AuthService`·`PolicyService`·`ParfaitGroupService`·
  > `ParfaitService`, 분할 기준은 서버 패키지가 아니라 **URL 세그먼트**), Service 함수명은
  > `<method><경로 세그먼트 PascalCase>`(경로 변수는 `By<파라미터명>`) 규칙을 따른다 —
  > 전송 계층이 계약을 그대로 비추고 의미 부여는 DataSource가 맡는다. 배치·규칙 전문은
  > [data-api-service-layer 스펙](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md).
  >
  > 📌 **7세트로 늘었다(2026-08-12, PR #230)** — `ImageService`·`MemberService`·`ParfaitImageService` +
  > 각 remote DataSource·`VOMapper`가 붙어 Android가 쓰는 20 엔드포인트 전량을 덮는다. 같은 규칙이
  > 그대로 적용됐고(`postImagesByImageIdConfirm`·`patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId`)
  > **Service 이름은 계약 문서가 이미 짚어 둔 URL 세그먼트 예외를 따랐다** — `MemberService`는 경로가
  > `users`인데 서버 도메인 이름을 쓰고, `ParfaitImageService`도 경로(그룹 하위 `images`)가 아니라
  > 도메인 이름을 쓴다. 최상위 `/api/v1/images`(업로드)와 그룹 하위 `.../images`(배치)가 **다른 도메인**이라
  > 세그먼트만으로는 갈리지 않기 때문이다 → [api/conventions.md](../api/conventions.md) "URL 규약".
  > 이 라운드가 더한 규칙 하나: **요청 방향 변환도 같은 `VOMapper.kt`에 둔다**(`ToppingTransform.toPlaceRequest` —
  > domain sealed를 서버가 받는 평면 필드로 편다). 전문은
  > [member·parfait-image 스펙](../superpowers/specs/archive/2026-08-11-member-parfait-image-api-service-layer.md).
- **응답 → 도메인 매핑 위치**: 원격 DataSource의 **반환 타입은 도메인 모델**이다
  (`TempRemoteDataSource.getTemp(id): Result<TempVO>`). 서버 응답 타입(머지 코드 기준
  `service.model.response`의 `TempResponse` — 요청 타입은 `service.model.request`)은 data 안에서만 살고, `source.<도메인>.mapper`의 확장 함수
  (`TempResponse.toTempVO()`, 파일 `VOMapper.kt`, `internal`)가 경계에서 변환한다. data 전용
  중간 모델(구 `data.model.dto.TempDto`)은 두지 않는다 — Response와 필드가 같아 값 없는 3단
  변환(Response→DTO→도메인)이 되기 때문이다. 도메인 모델은 `domain.model`에 둔다.

## 대안

- **대안 A — 모듈 `build.gradle`에 buildConfig·network 의존을 인라인** — 별도 플러그인 없이 `:data`
  모듈 스크립트에 직접 작성하면 당장은 빠르다. 그러나 서명 플러그인
  (`AndroidApplicationSigningConventionPlugin`)이 세운 "환경별 설정은 전용 컨벤션 플러그인" 관례를
  이탈하고, 다른 모듈이 network를 필요로 할 때 재사용할 수 없다.
  **→ 기각:** 관례 일치·재사용성을 위해 전용 플러그인으로 분리.
- **대안 B — 단일 `Json`을 로컬·원격이 공유(한정자 없음)** — 로컬(DataStore)용 `Json` 하나를
  Retrofit 컨버터가 그대로 주입받아 쓰면 인스턴스가 하나로 단순하다. 그러나 로컬 영속화와 원격 응답
  파싱은 요구되는 직렬화 관용(예: `isLenient`·`explicitNulls`·미지 키 처리)이 갈라질 수 있는데, 공유
  인스턴스는 한쪽 설정을 바꾸면 다른 쪽까지 영향을 준다.
  **→ 기각:** `@LocalJson`/`@RemoteJson` 한정자로 두 인스턴스를 분리해, 중복 바인딩 없이 용도별로
  독립 조정 가능하게 한다.
- **대안 C — Retrofit `CallAdapter`로 `Result<T>` 직접 반환** — 서비스 인터페이스가 `ApiResponse<T>`
  대신 바로 `Result<T>`를 반환하도록 커스텀 `CallAdapter.Factory`를 두면 `safeApiCall` 래핑이
  호출부에서 사라진다. 그러나 현재는 서비스 1세트뿐이라 CallAdapter 인프라가 과설계다.
  **→ 보류:** 서비스가 늘어나 `safeApiCall` 반복 래핑이 눈에 띄게 부담될 때 재검토.
- **대안 D — data 전용 DTO를 두고 Repository에서 도메인 변환** — `Response → DTO`(data) →
  `DTO → 도메인`(Repository) 2단으로 나누면 DataSource 시그니처가 data 타입으로 닫히고, 서버 응답
  형태가 도메인과 크게 갈라질 때 중간 표현을 붙일 자리가 생긴다. 그러나 현재 예시 세트에서 DTO는
  Response와 필드가 동일한 복제본이라 실제 격리 효과 없이 클래스와 매퍼만 하나씩 늘었다.
  **→ 기각:** DTO를 제거하고 DataSource가 도메인 모델을 반환한다. `:data`는 이미 `:domain`에
  의존하므로([[0001-layered-multi-module]]) 레이어 역전은 아니다. 응답이 도메인과 크게 어긋나는
  도메인이 나오면 그 도메인에 한해 중간 모델을 되살린다.
- **대안 E — DI 모듈을 도메인별 하위 패키지로 분할**(`di/repository/<도메인>`·`di/source/<도메인>` 등)
  — 도메인마다 파일이 따로라 병렬 브랜치가 같은 파일 끝에 `@Binds`를 append하며 생기는 rebase 충돌이
  사라진다. 그러나 현재 바인딩 총량 대비 파일이 과하게 쪼개져(절반이 바인딩 1개짜리) 바인딩 위치
  추적 비용이 상시 발생하는 반면, 충돌은 발생 빈도가 낮고(관측상 DI를 건드리는 병렬 브랜치 거의 없음)
  해소도 "양쪽 유지"로 기계적이다.
  **→ 기각:** 상시 비용(추적)이 간헐 비용(충돌 해소)보다 크다. 역할별 평면 배치를 채택하고, 역할 파일이
  비대해지면 그 파일에 한해 분할한다.

## 영향

**긍정**

- 서명 플러그인과 동일한 "환경별 설정은 컨벤션 플러그인" 관례를 network에도 적용해 배치가
  일관된다.
- `BASE_URL`이 properties/`local.properties`에서 로드되어 VCS에 원문이 노출되지 않는다.
- `source.<도메인>.remote` + `ApiResponse`/`safeApiCall` 패턴이 확립되어, 다음 도메인의 remote
  DataSource 추가가 예시(`TempRemoteDataSource`)를 복제하는 수준으로 단순해진다.
- DI 모듈이 역할당 1파일이라 "이 Repository/DataSource가 어디 바인딩됐나"를 파일명으로 바로 찾는다.
- Repository가 서버 응답 타입을 몰라도 되어, 도메인 소비자까지 오는 변환 단계가 1회로 줄었다.

**트레이드오프**

- 현재 소비 모듈이 `:data` 하나뿐인 상태에서 별도 컨벤션 플러그인을 두는 것은 당장은 오버헤드다
  (모듈이 늘 때 재사용성으로 상쇄될 것으로 본다).
- `TempService`/`TempRequest`/`TempResponse`/`TempVO`/`TempRemoteDataSource` 예시 세트가 실제
  도메인 API 확정 전까지 코드베이스에 placeholder로 남는다.
  (**해소: 2026-08-06 PR #197로 6파일 전부 삭제** — 실제 도메인 4세트가 그 자리를 대신한다.)
- 같은 역할 파일을 여러 브랜치가 동시에 고치면 append 지점에서 충돌이 난다(해소는 양쪽 유지로
  기계적). 파일이 커지면 그 파일만 쪼개는 것으로 대응한다.
- DataSource 시그니처가 도메인 타입에 묶여, 서버 응답이 도메인과 갈라지는 도메인이 나오면 그때
  중간 모델을 되살려야 한다(대안 D).

**위험·방어**

- 이번 변경에는 자동화된 테스트가 없다(코드베이스 관례상 무테스트) — `:app:assembleDebug`로 Hilt
  전체 그래프(`TokenProvider`→`AuthInterceptor`→`OkHttpClient`→`Retrofit`→`TempService`→
  `TempRemoteDataSource` 체인, `@LocalJson`/`@RemoteJson` 한정자 해소 정상)를 검증했다.
- `ApiResponse.isSuccess` 판정에 쓰는 성공 코드 규약과 `TokenProvider`의 실제 토큰 소스는
  미확정이다 → [open-questions](../synthesis/open-questions.md)로 추적.
  **as-built: 둘 다 2026-08-04 PR #190으로 develop 머지됐다** — 성공 판정은 `success` 필드로
  (`isSuccess`·`SUCCESS_CODE` 제거), `TokenProvider`는 `TokenStoreTokenProvider`(암호화 저장소 연동,
  [ADR-0019](0019-encrypted-token-storage.md))로 교체됐다. 해당 [open-questions](../synthesis/open-questions.md)
  항목 3건은 해소 처리했다. 다만 **실서버 요청은 여전히 0건**이다 — 2026-08-06 PR #197로 14 엔드포인트,
  2026-08-12 PR #230으로 나머지 6(image·member·parfait-image)까지 **Service 7·DataSource 7쌍이 다
  들어왔지만** 이를 **호출하는 Repository·UseCase·화면이 없고**, 개발 서버 평문
  HTTP 차단·`YG_BASE_URL` 부재로 앱에서 요청을 보낼 수도 없다. 응답 파싱의 실동작은 실연동 라운드까지
  확인되지 않는다. **표면이 20/20으로 닫힌 지금은 "덮을 게 남아서"라는 사유도 없다.**
- 도메인 모델 이름이 `TempVO`로 `VO` 접미사를 쓰는데, 기존 `domain.model`은 무접미사
  (`SegmentationResult`·`GalleryImageGroup`·`NameValidResult` 등)다. 접미사 규약이 갈라진 상태 →
  [open-questions](../synthesis/open-questions.md) [2026-07-30]로 추적.
  **as-built: 2026-08-06 PR #197로 `VO` 접미사가 원격 도메인 모델 전반(`KakaoLoginVO`·`AuthSessionVO`·
  `PolicyVO`·group 7종)으로 확산됐다** — 규약이 정해져서가 아니라 예시를 따라간 결과다. 값 하나짜리
  래퍼는 접미사 없는 value class(`GroupId`·`AccessToken` 등)로 갈려 두 관례가 공존한다.
