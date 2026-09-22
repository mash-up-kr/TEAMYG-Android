---
id: data-network-setup
title: 데이터 모듈 원격 네트워크 기초 구조 (Retrofit · OkHttp Network Setup)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-01
related_code: ModuleDataConventionPlugin, PropertySettingManager, NetworkModule, JsonModule, ServiceModule, RemoteDataSourceModule, ApiException
related_adr: ADR-0017
related_spec:
related_architecture: data-layer
supersedes:
superseded_by:
tags: [spec, parfait]
---

# Spec: 데이터 모듈 원격 네트워크 기초 구조

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ⚠️ **as-built 정정(2026-07-30 작성, PR #174로 develop 머지 2026-08-01)** — 구현 중
> 두 가지가 설계와 갈렸다. 아래 본문은 정정 반영본이고, 결정 근거는
> [ADR-0017](../../../adr/0017-remote-network-datasource.md)에 있다.
> - **DI 배치**: `di/` 평면 유지 + 역할당 1파일 — `ServiceModule`(설계에선 `NetworkModule`이 서비스까지
>   provide)·`JsonModule` 신설. `Json` 2종은 `DataStoreModule`이 아니라 `JsonModule`이 제공. 구현 중
>   도메인별 하위 패키지(`di/repository/<도메인>` 등)로 분할했다가 코드리뷰에서 기각돼 되돌렸다
>   ([ADR-0017](../../../adr/0017-remote-network-datasource.md) 대안 E).
> - **매핑 위치**: 예외적으로 범위를 넘어섰다 — "DTO→도메인 매핑은 제외"였으나, remote DataSource가
>   도메인 모델(`TempVO`)을 반환하고 `source.temp.mapper`가 변환하도록 확정. data 전용 DTO는 폐지.
> - **`safeApiCall` 진입점 2개**(코드리뷰 반영): 단일 진입점이 `isSuccess && data != null`을 성공
>   조건으로 삼아 payload 없는 성공 응답(`ApiResponse<Unit>`)을 실패로 분류하던 문제 → payload 유무로
>   함수를 나누고 `ApiException.EmptyBody`를 신설해 "서버 실패"와 "성공인데 본문 없음"을 구분.
>
> ⚠️ **머지 시점 재대조(2026-08-01, PR #174)** — 위 정정 외에 **패키지 배치**가 아래 "파일 구성" 표와
> 갈렸다(표는 머지 코드 기준으로 갱신함). 구조·API·심볼은 전부 설계대로다.
> - 서버 타입이 `service/model/` 평면이 아니라 **`service/model/request/`·`service/model/response/`**로
>   나뉜다(`ApiResponse`·`TempResponse`는 response, `TempRequest`는 request).
> - `data` 모듈에 **`model/` 패키지 신설** — `model/exception/ApiException.kt`,
>   `model/qualifier/{LocalJson,RemoteJson}.kt`. 즉 `ApiException`은 `network/`가 아니라 `model/exception`.
> - `TokenProvider`와 stub `EmptyTokenProvider`가 **파일 2개로 분리**(계획은 한 파일).
> - `safeApiCall`·`safeApiCallWithoutData`가 private `runCatchingApi`를 공유해 예외 분기를 한 곳에 둔다.
> - `OkHttpClient` 타임아웃은 connect/read/write **3종을 각각** 설정(단일 상수 아님, `callTimeout` 없음
>   → [open-questions](../../../synthesis/open-questions.md) [2026-07-30]).

## 목표
`:data` 모듈에 Retrofit·OkHttp·kotlinx-serialization 기반 **원격 네트워크 기초 구조**를 세운다.
`data-layer.md`의 "네트워킹(Assumption)"(의존만 준비, 서비스 미정의)을 실제 구조로 확정한다.
실제 API 연동은 후속 — 이번엔 **인프라 + 재사용 패턴의 예시 1세트**까지만.

## 범위
- **포함**:
  - network용 **컨벤션 플러그인** 신설(`AndroidNetworkConventionPlugin`): `buildConfig` 활성 + `BASE_URL` buildConfigField(properties 로드) + `libs.bundles.network`·serialization 의존 이관.
  - Hilt DI: `NetworkModule`이 `OkHttpClient`·`Retrofit`, `ServiceModule`이 예시 `Service` provide. `Json`(`@LocalJson`·`@RemoteJson`)은 `JsonModule`이 제공.
  - 공통 응답 envelope `ApiResponse<T>` + `safeApiCall`/`safeApiCallWithoutData` 헬퍼(→ `Result<T>`).
  - `AuthInterceptor` + `TokenProvider`(빈 stub, 헤더 주입 자리만).
  - 예시 1세트: `TempService` + `TempRequest`/`TempResponse` + `domain.model.TempVO` + `source/temp/mapper/VOMapper` + `source/temp/remote/TempRemoteDataSource(+Impl)` + `RemoteDataSourceModule`.
  - 응답→도메인 매핑 지점 확정(remote DataSource가 도메인 모델 반환, data 전용 DTO 없음).
  - ADR-0017 신설 + `data-layer.md` 네트워킹 섹션 갱신(같은 PR).
- **제외**:
  - 실제 백엔드 엔드포인트 연동, domain Repository/UseCase 소비.
  - 실제 인증 토큰 소스(로그인/토큰 저장) 연동 — `TokenProvider`는 빈 반환 + TODO.
  - debug/release baseUrl 분기(단일 `defaultConfig` buildConfigField로 시작, 후속 확장).
  - ~~구체 에러 타입 계층~~ — 완료: sealed `ApiException`(`Business`/`EmptyBody`/`Http`/`Network`/`Unknown`) 도입([ADR-0017](../../../adr/0017-remote-network-datasource.md)).

## 컨벤션 플러그인 (build-logic)
서명 플러그인(`AndroidApplicationSigningConventionPlugin` + `PropertySettingManager`) 패턴을 따른다.

```kotlin
// AndroidNetworkConventionPlugin.kt
class AndroidNetworkConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.kotlin.serialization.get().pluginId)
    }
    // LibraryExtension(또는 CommonExtension) 대상: buildFeatures.buildConfig = true
    //   defaultConfig.buildConfigField("String", "BASE_URL", "\"${loadBaseUrl()}\"")
    setConfigNetwork(...)   // NetworkConfig.kt
    dependencies {
        implementation(libs.bundles.network)
        implementation(libs.kotlinx.serialization)
    }
})
```
- `NetworkConfig.kt`(`buildlogic/`): `Project.setConfigNetwork(LibraryExtension)` — buildConfig 활성 + `buildConfigField(BASE_URL)`.
- `PropertySettingManager.loadBaseUrl(project, rootProject)`: `findProperty` → `local.properties` 순으로 `YG_BASE_URL` 조회, 누락 시 placeholder(`https://TODO.example.com/`) fallback. 서명 키 로드와 동형.
- 등록: convention `build.gradle.kts` `pluginRegister("android.network", "AndroidNetwork")` + `libs.versions.toml` `parfait-android-network` alias.
- `ModuleDataConventionPlugin`: 기존 `bundles.network`·`kotlinx.serialization`·`kotlin.serialization` 3줄 제거 → `apply(libs.plugins.parfait.android.network.get().pluginId)`로 이관(중복 제거, 관심사 이동).

## API / 인터페이스
```kotlin
// service/model/response/ApiResponse.kt
@Serializable
data class ApiResponse<T>(
    val code: String,        // 실제 규약 확정 시 조정
    val message: String,
    val data: T?,
)

// network/SafeApiCall.kt
suspend fun <T : Any> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T>
//  envelope.code 성공 검사 → data non-null → Result.success(data)
//  성공 코드 + data 없음 → ApiException.EmptyBody / 실패 코드 → ApiException.Business
//  예외(HttpException·IOException·기타) → Http·Network·Unknown, CancellationException은 재던짐
suspend fun safeApiCallWithoutData(block: suspend () -> ApiResponse<Unit>): Result<Unit>
//  payload 없는 API(삭제·설정 변경 등) — 성공 코드만 검사, data 미검사

// network/TokenProvider.kt
interface TokenProvider { fun getToken(): String? }          // stub: 항상 null 반환 + TODO

// network/AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor                                                // token 있으면 Authorization 헤더 주입

// service/TempService.kt
interface TempService {
    @GET("temp/{id}")
    suspend fun getTemp(@Path("id") id: String): ApiResponse<TempResponse>
}

// domain/model/TempVO.kt  (:domain)
data class TempVO(val id: String, val name: String)

// source/temp/mapper/VOMapper.kt
internal fun TempResponse.toTempVO(): TempVO

// source/temp/remote/TempRemoteDataSource.kt
interface TempRemoteDataSource {
    suspend fun getTemp(id: String): Result<TempVO>      // 도메인 모델 반환
}
```

## 동작 / 데이터 흐름
`RemoteDataSourceImpl` → `ExampleService`(suspend, `ApiResponse<T>` 반환) → `safeApiCall { }`로 감쌈:
- 정상 2xx + envelope 성공 코드 + `data != null` → `Result.success(data)`.
- envelope 실패 코드 → `ApiException.Business` / 성공 코드인데 `data == null` → `ApiException.EmptyBody`.
- `HttpException`(4xx/5xx)·`IOException`(네트워크)·그 외 예외 → `Http`·`Network`·`Unknown`.

본문 없는 API는 `ApiResponse<Unit>` + `safeApiCallWithoutData { }` — `data`를 판정에 쓰지 않는다.
단일 진입점이 `data != null`을 요구하면 payload 없는 성공 응답이 실패로 분류되기 때문.

`TempRemoteDataSourceImpl`은 `safeApiCall` 결과를 `map { it.toTempVO() }`로 도메인 모델에 실어 반환한다
— `TempResponse`는 data 밖으로 나가지 않는다.

`NetworkModule`(`di/`, `@InstallIn(SingletonComponent)`, `object`):
- `provideTokenProvider`·`provideAuthInterceptor`.
- `provideOkHttpClient(authInterceptor)` — `HttpLoggingInterceptor`(`BuildConfig.DEBUG` 게이팅) + `AuthInterceptor` + 타임아웃.
- `provideRetrofit(okHttp, @RemoteJson json)` — `BuildConfig.BASE_URL` + `json.asConverterFactory(...)`.

`JsonModule`(`object`): `provideLocalJson`(`@LocalJson`)·`provideRemoteJson`(`@RemoteJson`).
`ServiceModule`(`object`): `provideTempService(retrofit)` — `retrofit.create()`.
`RemoteDataSourceModule`(`interface`, `@Binds`): `TempRemoteDataSourceImpl` → `TempRemoteDataSource`.

DI 모듈은 `di/` 평면 배치·역할당 1파일(도메인별 하위 패키지 분할은 기각 — [ADR-0017](../../../adr/0017-remote-network-datasource.md) 대안 E).

## 파일 구성
| 파일 | 역할 |
|------|------|
| `build-logic/.../AndroidNetworkConventionPlugin.kt` | network 컨벤션 플러그인 |
| `build-logic/.../buildlogic/NetworkConfig.kt` | `setConfigNetwork`(buildConfig + buildConfigField) |
| `build-logic/.../buildlogic/utils/PropertySettingManager.kt` | `loadBaseUrl()` 추가 |
| `build-logic/convention/build.gradle.kts` | `pluginRegister("android.network", ...)` |
| `gradle/libs.versions.toml` | `parfait-android-network` plugin alias |
| `data/.../di/NetworkModule.kt` | TokenProvider·AuthInterceptor·OkHttp·Retrofit provide |
| `data/.../di/JsonModule.kt` | `@LocalJson`·`@RemoteJson` `Json` provide |
| `data/.../di/ServiceModule.kt` | Retrofit 서비스 provide(`TempService` 등) |
| `data/.../di/RemoteDataSourceModule.kt` | remote DataSource `@Binds` |
| `data/.../network/AuthInterceptor.kt` | 토큰 헤더 주입 자리 |
| `data/.../network/TokenProvider.kt`·`EmptyTokenProvider.kt` | 토큰 소스 인터페이스 / 빈 stub(파일 분리) |
| `data/.../network/SafeApiCall.kt` | `ApiResponse` → `Result` 매핑(진입점 2개 + private `runCatchingApi`) |
| `data/.../model/exception/ApiException.kt` | sealed 실패 분류(`Business`/`EmptyBody`/`Http`/`Network`/`Unknown`) |
| `data/.../model/qualifier/LocalJson.kt`·`RemoteJson.kt` | `Json` 용도 한정자 |
| `data/.../service/TempService.kt` | 예시 Retrofit 서비스 |
| `data/.../service/model/response/ApiResponse.kt` | 공통 응답 envelope |
| `data/.../service/model/response/TempResponse.kt`·`model/request/TempRequest.kt` | 예시 서버 응답·요청 타입(data 내부 전용) |
| `domain/.../model/TempVO.kt` | 예시 도메인 모델(DataSource 반환 타입) |
| `data/.../source/temp/mapper/VOMapper.kt` | `TempResponse.toTempVO()` 변환 |
| `data/.../source/temp/remote/TempRemoteDataSource.kt`·`...Impl.kt` | 예시 remote DataSource |

## 주의 / 열린 질문
- **예시 도메인 이름**: `temp`로 스캐폴딩(실제 첫 도메인 확정 전 임시 placeholder). 실제 API 시 교체·삭제.
- `TokenProvider` stub 바인딩 위치 — `NetworkModule.provideTokenProvider`로 확정.
- **`VO` 접미사 규약 미결**: `TempVO`는 접미사를 쓰지만 기존 `domain.model`은 무접미사 → [open-questions](../../../synthesis/open-questions.md) [2026-07-30].
- envelope `code` 성공 판정 규칙은 실제 백엔드 규약 확정 후 조정 → open-questions 연동 가능.
- debug/release baseUrl 분기·구체 에러 타입은 후속 ADR/스펙.
