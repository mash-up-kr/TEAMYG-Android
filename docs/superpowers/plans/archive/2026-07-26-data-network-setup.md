---
id: data-network-setup
title: 데이터 모듈 원격 네트워크 기초 구조 Implementation Plan
status: done
type: work-order
created: 2026-07-26
updated: 2026-07-30
platforms: android
owner: Parfait 팀
related_adr: ADR-0017
related_spec: data-network-setup
related_code: ModuleDataConventionPlugin, PropertySettingManager, NetworkModule, JsonModule, ServiceModule, RemoteDataSourceModule
archived_reason: 구현·리뷰 완료 — PR #174(`feature/network-set-up`) develop 머지(2026-08-01)
tags: [plan, parfait]
---

# 데이터 모듈 원격 네트워크 기초 구조 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** `:data` 모듈에 Retrofit·OkHttp·kotlinx-serialization 기반 원격 네트워크 기초 구조(컨벤션 플러그인 + Hilt NetworkModule + 공통 응답/에러 계약 + 인증 자리 + 예시 remote 1세트)를 세운다.

**Architecture:** 서명 컨벤션 플러그인 패턴을 따라 network 관심사를 `AndroidNetworkConventionPlugin`으로 추출(`buildConfig` 활성 + `BASE_URL` properties 로드 + network bundle·serialization). `NetworkModule`(Hilt)이 OkHttp·Retrofit·예시 Service를 provide하고, 기존 `DataStoreModule`의 `Json` 싱글톤을 재사용한다. 응답은 공통 `ApiResponse<T>` envelope + `safeApiCall`로 `Result<T>` 매핑. 인증은 `AuthInterceptor` + 빈 `TokenProvider` stub 자리만.

**Tech Stack:** Kotlin, Gradle 컨벤션 플러그인(kotlin-dsl), Retrofit 3.0.0, OkHttp 5.4.0, kotlinx-serialization 1.11.0, Hilt(KSP), ktlint 14.2.0.

> ⚠️ **as-built 정정(2026-07-30)** — 실행 후 두 가지가 아래 Task 서술과 다르다. 정본은
> [ADR-0017](../../../adr/0017-remote-network-datasource.md)·[data-layer](../../../architecture/data-layer.md)이고,
> Task 4·5의 코드 블록은 실행 당시 안이다.
> - **DI 파일 배치**: `di/` 평면 유지, 역할당 1파일 — `ServiceModule.kt`(`provideTempService`가 `NetworkModule`에서
>   이관)·`RemoteDataSourceModule.kt`·`JsonModule.kt`(`Json` 2종, `DataStoreModule`에서 분리) 신설.
>   중간에 도메인별 하위 패키지(`di/repository/<도메인>` 등)로 분할했다가 코드리뷰에서 기각돼 되돌림
>   ([ADR-0017](../../../adr/0017-remote-network-datasource.md) 대안 E).
> - **반환 타입**: `TempRemoteDataSource.getTemp`가 `Result<TempDto>` → `Result<TempVO>`(`:domain`).
>   data 전용 `TempDto` 폐지, `source/temp/mapper/VOMapper.kt`의 `toTempVO()`가 변환.
> - **`SafeApiCall.kt`**(코드리뷰 반영): 단일 `safeApiCall` → payload 유무별 진입점 2개
>   (`safeApiCall`·`safeApiCallWithoutData`) + `ApiException.EmptyBody` 신설.
> - **패키지 배치**(머지 코드 기준): 서버 타입은 `service/model/request/`·`service/model/response/`로 분리,
>   `ApiException`·`Json` 한정자는 `data` 모듈 신설 `model/exception`·`model/qualifier`,
>   `EmptyTokenProvider`는 `TokenProvider.kt`에서 분리된 별도 파일. 타임아웃은 connect/read/write 3종.

## Global Constraints

- 대상 repo: `TJYG-Android`(`mash-up-kr/TJYG-Android`), 로컬 경로는 private submodule `project-paths.md` 참조. **이 plan repo가 아니라 그쪽에서 코드 작업.**
- 브랜치: 현재 `feature/network-set-up`(이미 체크아웃됨) 위에서 작업. `main`/`develop` 직접 커밋 금지.
- 네트워크 의존은 `libs.bundles.network`(okhttp·okhttp-logging-interceptor·retrofit·retrofit-kotlin-serialization-converter)만 사용. 신규 라이브러리 추가 금지.
- 패키지 루트: `com.teamyg.parfait.data`. 서비스=`.service`, 응답·요청 타입=`.service.model`, remote DataSource=`.source.<도메인>.remote`, 매퍼=`.source.<도메인>.mapper`, 인프라=`.network`, DI=`.di`(평면, 역할당 1파일 — as-built).
- `Json` 중복 `@Provides` 금지 — `@LocalJson`/`@RemoteJson` 한정자로 구분하고 `JsonModule` 한 곳에서만 제공(as-built).
- 검증: 각 Task 끝에 `./gradlew :data:assembleDebug` + `./gradlew :data:ktlintFormat`(테스트 코드 없음 — 코드베이스 무테스트 관례). Hilt 전체 그래프는 최종 Task에서 `./gradlew :app:assembleDebug`.
- parfait 문서 규칙: 라인번호·변동수치·hex 금지, 근거는 파일명+심볼명.
- commit/push/PR은 **사용자 확인 후에만**(CLAUDE.md). 각 Task의 commit step은 사용자 승인 전제.

**경로 표기:** 아래 `TJYG/`는 TJYG-Android repo 루트, `parfait/`는 이 plan repo 루트.

---

### Task 1: network 컨벤션 플러그인 (build-logic)

**Files:**
- Modify: `TJYG/build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/utils/PropertySettingManager.kt`
- Create: `TJYG/build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/NetworkConfig.kt`
- Create: `TJYG/build-logic/convention/src/main/kotlin/AndroidNetworkConventionPlugin.kt`
- Modify: `TJYG/build-logic/convention/build.gradle.kts`
- Modify: `TJYG/gradle/libs.versions.toml`
- Modify: `TJYG/build-logic/convention/src/main/kotlin/ModuleDataConventionPlugin.kt`

**Interfaces:**
- Consumes: 기존 `PropertySettingManager`(object), `libs.bundles.network`, `libs.plugins.kotlin.serialization`, `libs.plugins.parfait.android.library`.
- Produces: 플러그인 id `com.teamyg.parfait.plugin.android.network`(alias `libs.plugins.parfait.android.network`). 적용 모듈에 `BuildConfig.BASE_URL: String` 생성 + network·serialization 의존 부여.

- [ ] **Step 1: `PropertySettingManager`에 `loadBaseUrl` 추가**

`loadReleaseKey`와 동형(findProperty → local.properties → fallback). 파일 상단 상수 영역에 키 추가, object 안에 함수 추가:

```kotlin
// 상수 영역(기존 KEY 상수들 옆)
private const val BASE_URL_KEY = "YG_BASE_URL"
private const val BASE_URL_FALLBACK = "https://TODO.example.com/"

// object 함수
fun loadBaseUrl(
    project: Project,
    rootProject: Project,
): String {
    project.findProperty(BASE_URL_KEY)?.toString()?.let { return it }

    val file = rootProject.file("local.properties")
    if (file.exists()) {
        with(Properties().apply { load(file.inputStream()) }) {
            getProperty(BASE_URL_KEY)?.let { return it }
        }
    }
    return BASE_URL_FALLBACK
}
```

- [ ] **Step 2: `NetworkConfig.kt` 작성 (`setConfigNetwork`)**

buildConfig 활성 + `BASE_URL` buildConfigField. `LibraryExtension` 대상(`:data`는 라이브러리). 기존 `AndroidConfig.kt`의 `internal fun Project.setConfigXxx` 패턴을 따른다.

```kotlin
package com.teamyg.parfait.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.utils.PropertySettingManager
import org.gradle.api.Project

internal fun Project.setConfigNetwork(extension: LibraryExtension) {
    extension.apply {
        buildFeatures {
            buildConfig = true
        }
        val baseUrl = PropertySettingManager.loadBaseUrl(
            project = project,
            rootProject = rootProject,
        )
        defaultConfig {
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        }
    }
}
```

- [ ] **Step 3: `AndroidNetworkConventionPlugin.kt` 작성**

`BaseConventionPlugin` 상속(기존 플러그인들과 동일). serialization 플러그인 적용 + `setConfigNetwork` + network·serialization 의존. `LibraryExtension` 없으면 error(서명 플러그인 패턴).

```kotlin
import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.setConfigNetwork
import com.teamyg.parfait.buildlogic.utils.extensions.implementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

class AndroidNetworkConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.kotlin.serialization.get().pluginId)
    }

    val libraryExtension: LibraryExtension = extensions.findByType(LibraryExtension::class)
        ?: error("must be applied com.android.library")

    setConfigNetwork(libraryExtension)

    dependencies {
        implementation(libs.bundles.network)
        implementation(libs.kotlinx.serialization)
    }
})
```

- [ ] **Step 4: convention `build.gradle.kts`에 플러그인 등록**

`gradlePlugin { plugins { ... } }` 블록의 `pluginRegister(...)` 목록에 한 줄 추가(기존 `android.application.signing` 아래 등):

```kotlin
pluginRegister(
    pluginName = "android.network",
    className = "AndroidNetwork",
)
```

- [ ] **Step 5: `libs.versions.toml`에 plugin alias 추가**

`[plugins]` 영역 parfait alias 목록에 추가(기존 `parfait-android-library` 아래):

```toml
parfait-android-network = { id = "com.teamyg.parfait.plugin.android.network" }
```

- [ ] **Step 6: `ModuleDataConventionPlugin`에서 network 관심사 이관**

기존 3줄(`apply(libs.plugins.kotlin.serialization...)`, `implementation(libs.kotlinx.serialization)`, `implementation(libs.bundles.network)`)을 제거하고 새 플러그인 적용으로 교체. `plugins` 블록에 추가:

```kotlin
apply(libs.plugins.parfait.android.network.get().pluginId)
```
`dependencies` 블록에서 `implementation(libs.kotlinx.serialization)`·`implementation(libs.bundles.network)` 삭제(플러그인이 부여). serialization plugin apply 라인도 삭제(플러그인이 apply). 나머지(hilt.core·domain·datastore·mlkit 등)는 유지.

- [ ] **Step 7: 빌드로 검증**

Run: `cd TJYG && ./gradlew :data:assembleDebug`
Expected: BUILD SUCCESSFUL. `data/build/generated/source/buildConfig/.../BuildConfig.java`(또는 debug 경로)에 `public static final String BASE_URL` 생성.
확인: `find data/build -name BuildConfig.java | head` 후 해당 파일에 `BASE_URL` 존재.

- [ ] **Step 8: ktlint 정리**

Run: `cd TJYG && ./gradlew :data:ktlintFormat`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit (사용자 승인 후)**

```bash
cd TJYG && git add build-logic gradle/libs.versions.toml data
git commit -m "build: add android.network convention plugin, migrate :data network deps"
```

---

### Task 2: 공통 응답 계약 — ApiResponse + safeApiCall

**Files:**
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/service/model/ApiResponse.kt`
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/network/safeApiCall.kt`

**Interfaces:**
- Consumes: retrofit `HttpException`(`retrofit2.HttpException`), kotlinx-serialization.
- Produces:
  - `data class ApiResponse<T>(val code: String, val message: String, val data: T?)` (`@Serializable`).
  - `suspend fun <T> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T>` — 성공 시 `Result.success(data)`, 실패 시 `Result.failure`.

- [ ] **Step 1: `ApiResponse.kt` 작성**

```kotlin
package com.teamyg.parfait.data.service.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: String,
    val message: String,
    val data: T? = null,
) {
    // 실제 백엔드 성공 코드 규약 확정 시 조정 (open-questions)
    val isSuccess: Boolean get() = code == SUCCESS_CODE

    companion object {
        // TODO: 실제 백엔드 성공 코드로 교체
        private const val SUCCESS_CODE = "SUCCESS"
    }
}
```

- [ ] **Step 2: `safeApiCall.kt` 작성**

envelope 성공 검사 + non-null data → `Result`. 예외(HttpException·IOException 등)는 `runCatching`이 포착.

```kotlin
package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.service.model.ApiResponse

suspend fun <T> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T> = runCatching {
    val response = block()
    val data = response.data
    if (response.isSuccess && data != null) {
        data
    } else {
        error("API error: code=${response.code}, message=${response.message}")
    }
}
```

- [ ] **Step 3: 빌드 검증**

Run: `cd TJYG && ./gradlew :data:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: ktlint 정리**

Run: `cd TJYG && ./gradlew :data:ktlintFormat`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit (사용자 승인 후)**

```bash
cd TJYG && git add data/src/main/java/com/teamyg/parfait/data/service data/src/main/java/com/teamyg/parfait/data/network
git commit -m "feat(data): add ApiResponse envelope and safeApiCall"
```

---

### Task 3: 인증 자리 — TokenProvider + AuthInterceptor

**Files:**
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/network/TokenProvider.kt`
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/network/AuthInterceptor.kt`

**Interfaces:**
- Consumes: OkHttp `Interceptor`.
- Produces:
  - `interface TokenProvider { fun getToken(): String? }`
  - `class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor` — 토큰 non-null이면 `Authorization: Bearer <token>` 헤더 주입, 아니면 원 요청 그대로.

- [ ] **Step 1: `TokenProvider.kt` 작성 (인터페이스 + 빈 stub)**

```kotlin
package com.teamyg.parfait.data.network

/**
 * 요청 인증 토큰 소스. 로그인/토큰 저장 연동 지점.
 */
interface TokenProvider {
    fun getToken(): String?
}

/**
 * TODO: 로그인/토큰 저장(예: DataStore) 연동 시 실제 구현으로 교체.
 * 현재는 인증 자리만 확보하기 위한 빈 stub.
 */
class EmptyTokenProvider : TokenProvider {
    override fun getToken(): String? = null
}
```

- [ ] **Step 2: `AuthInterceptor.kt` 작성**

```kotlin
package com.teamyg.parfait.data.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()
        val request = chain.request().newBuilder().apply {
            if (token != null) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}
```

- [ ] **Step 3: 빌드 검증**

Run: `cd TJYG && ./gradlew :data:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: ktlint 정리**

Run: `cd TJYG && ./gradlew :data:ktlintFormat`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit (사용자 승인 후)**

```bash
cd TJYG && git add data/src/main/java/com/teamyg/parfait/data/network
git commit -m "feat(data): add AuthInterceptor and TokenProvider stub"
```

---

### Task 4: NetworkModule (Hilt) — OkHttp · Retrofit

**Files:**
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt`

**Interfaces:**
- Consumes: Task 2 없음(여기선 미사용), Task 3 `AuthInterceptor`·`TokenProvider`·`EmptyTokenProvider`, 기존 `DataStoreModule.provideDataStoreJsonParser(): Json`(주입), `BuildConfig.BASE_URL`(Task 1).
- Produces(Hilt SingletonComponent):
  - `TokenProvider`(= `EmptyTokenProvider`), `AuthInterceptor`, `OkHttpClient`, `Retrofit` provide.
  - `Retrofit`은 다음 Task의 Service provide가 소비.

- [ ] **Step 1: `NetworkModule.kt` 작성**

`Json`은 새로 provide하지 않고 주입 파라미터로 받는다(중복 바인딩 방지). `okhttp3.MediaType.Companion.toMediaType`, `retrofit2.converter.kotlinx.serialization.asConverterFactory` 사용.

```kotlin
package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.BuildConfig
import com.teamyg.parfait.data.network.AuthInterceptor
import com.teamyg.parfait.data.network.EmptyTokenProvider
import com.teamyg.parfait.data.network.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider = EmptyTokenProvider()

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenProvider: TokenProvider): AuthInterceptor =
        AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private const val TIMEOUT_SECONDS = 30L
}
```

주의: `asConverterFactory` import 경로가 retrofit 3.x에서 다르면(`com.jakewharton.retrofit...` vs `retrofit2.converter.kotlinx.serialization...`) IDE 자동완성으로 실제 아티팩트(`retrofit-kotlin-serialization-converter`) 제공 심볼에 맞춘다.

- [ ] **Step 2: 빌드 검증**

Run: `cd TJYG && ./gradlew :data:assembleDebug`
Expected: BUILD SUCCESSFUL. `BuildConfig` import 해결, converter import 해결.
실패 시(converter import mismatch): 해당 아티팩트가 export하는 실제 패키지로 import 교정 후 재빌드.

- [ ] **Step 3: ktlint 정리**

Run: `cd TJYG && ./gradlew :data:ktlintFormat`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit (사용자 승인 후)**

```bash
cd TJYG && git add data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt
git commit -m "feat(data): add NetworkModule providing OkHttp and Retrofit"
```

---

### Task 5: 예시 remote 1세트 — TempService · DTO · RemoteDataSource

**Files:**
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/service/model/TempResponse.kt`
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/service/model/TempRequest.kt`
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/service/TempService.kt`
- Modify: `TJYG/data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt`
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/source/temp/remote/TempRemoteDataSource.kt`
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/source/temp/remote/TempRemoteDataSourceImpl.kt`
- Create: `TJYG/data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`

**Interfaces:**
- Consumes: Task 2 `ApiResponse`·`safeApiCall`, Task 4 `Retrofit` provide.
- Produces:
  - `TempService`(retrofit interface), `TempRemoteDataSource`(interface) + `TempRemoteDataSourceImpl`(@Inject).
  - `NetworkModule.provideTempService(retrofit): TempService`.
  - `RemoteDataSourceModule`(@Binds).

- [ ] **Step 1: DTO 작성 (`TempResponse.kt`, `TempRequest.kt`)**

```kotlin
// TempResponse.kt
package com.teamyg.parfait.data.service.model

import kotlinx.serialization.Serializable

// TODO: 실제 API 확정 시 교체·삭제 (구조 예시용)
@Serializable
data class TempResponse(
    val id: String,
    val name: String,
)
```

```kotlin
// TempRequest.kt
package com.teamyg.parfait.data.service.model

import kotlinx.serialization.Serializable

// TODO: 실제 API 확정 시 교체·삭제 (구조 예시용)
@Serializable
data class TempRequest(
    val name: String,
)
```

- [ ] **Step 2: `TempService.kt` 작성**

```kotlin
package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.ApiResponse
import com.teamyg.parfait.data.service.model.TempResponse
import retrofit2.http.GET
import retrofit2.http.Path

// TODO: 실제 API 확정 시 교체·삭제 (구조 예시용)
interface TempService {
    @GET("temp/{id}")
    suspend fun getTemp(
        @Path("id") id: String,
    ): ApiResponse<TempResponse>
}
```

- [ ] **Step 3: `NetworkModule`에 `provideTempService` 추가**

`NetworkModule` object 안, `provideRetrofit` 아래에 추가:

```kotlin
@Provides
@Singleton
fun provideTempService(retrofit: Retrofit): com.teamyg.parfait.data.service.TempService =
    retrofit.create(com.teamyg.parfait.data.service.TempService::class.java)
```
(import 정리 시 상단 `import com.teamyg.parfait.data.service.TempService` 후 반환형·인자 단순화.)

- [ ] **Step 4: `TempRemoteDataSource` 인터페이스 + Impl 작성**

```kotlin
// TempRemoteDataSource.kt
package com.teamyg.parfait.data.source.temp.remote

import com.teamyg.parfait.data.service.model.TempResponse

interface TempRemoteDataSource {
    suspend fun getTemp(id: String): Result<TempResponse>
}
```

```kotlin
// TempRemoteDataSourceImpl.kt
package com.teamyg.parfait.data.source.temp.remote

import com.teamyg.parfait.data.network.safeApiCall
import com.teamyg.parfait.data.service.TempService
import com.teamyg.parfait.data.service.model.TempResponse
import javax.inject.Inject

class TempRemoteDataSourceImpl @Inject constructor(
    private val tempService: TempService,
) : TempRemoteDataSource {
    override suspend fun getTemp(id: String): Result<TempResponse> =
        safeApiCall { tempService.getTemp(id) }
}
```

- [ ] **Step 5: `RemoteDataSourceModule.kt` 작성 (@Binds)**

기존 `LocalDataSourceModule` 패턴과 동일(interface 모듈, `@Binds @Singleton`).

```kotlin
package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.source.temp.remote.TempRemoteDataSource
import com.teamyg.parfait.data.source.temp.remote.TempRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RemoteDataSourceModule {
    @Binds
    @Singleton
    fun bindTempRemoteDataSource(
        tempRemoteDataSourceImpl: TempRemoteDataSourceImpl,
    ): TempRemoteDataSource
}
```

- [ ] **Step 6: 빌드 검증(컴파일)**

Run: `cd TJYG && ./gradlew :data:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Hilt 전체 그래프 검증**

Run: `cd TJYG && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Hilt 컴포넌트 생성 시 `NetworkModule`·`RemoteDataSourceModule` 바인딩(TokenProvider→AuthInterceptor→OkHttpClient→Retrofit→TempService→TempRemoteDataSource) 검증 통과, `Json` 중복 바인딩 에러 없음.

- [ ] **Step 8: ktlint 정리**

Run: `cd TJYG && ./gradlew :data:ktlintFormat`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit (사용자 승인 후)**

```bash
cd TJYG && git add data/src/main/java/com/teamyg/parfait/data/service data/src/main/java/com/teamyg/parfait/data/source data/src/main/java/com/teamyg/parfait/data/di
git commit -m "feat(data): add Temp remote datasource scaffolding (service, dto, remote, di)"
```

---

### Task 6: 문서 — ADR-0017 + data-layer.md 갱신 (parfait, 이 repo)

**Files:**
- Create: `parfait/adr/0017-remote-network-datasource.md`
- Modify: `parfait/adr/README.md`
- Modify: `parfait/architecture/data-layer.md`
- Modify: `parfait/index.md`
- Modify: `parfait/specs/2026-07-26-data-network-setup.md`(status), `parfait/plans/2026-07-26-data-network-setup.md`(status)

**Interfaces:**
- Consumes: `parfait/adr/template.md` 형식, 코드 Task 1~5 결과(심볼명).
- Produces: ADR-0017 문서 + 인덱스 등록 + architecture 갱신.

- [ ] **Step 1: `0017-remote-network-datasource.md` 작성**

`adr/template.md` 복사 후 채움. `status: accepted`, `date: 2026-07-26`, `deciders: Parfait 팀`, `related_spec: data-network-setup`, `related_architecture: data-layer`. 본문:
- **맥락**: `libs.bundles.network` 의존만 있고 서비스 미정의 상태(구 data-layer "Assumption"). 원격 연동 시작을 위해 규약 필요.
- **결정**: network 컨벤션 플러그인(`AndroidNetworkConventionPlugin`)으로 `buildConfig`·`BASE_URL`(properties 로드)·network 의존 응집 / 공통 `ApiResponse<T>` envelope + `safeApiCall`→`Result<T>` / `AuthInterceptor`+`TokenProvider`(stub) 인증 자리 / `Json`은 `DataStoreModule` 재사용 / remote DataSource는 `source.<도메인>.remote`.
- **대안**: (A) 모듈 build.gradle에 인라인 buildConfig — 관례(서명 플러그인) 이탈·재사용 불가 → 기각. (B) network 전용 Json 신규 provide — 중복 바인딩·불필요 분기 → 기각. (C) CallAdapter로 Result 직접 반환 — 현 단계 과설계 → 후속 보류.
- **영향**: 긍정(관례 일치·baseUrl VCS 미노출·재사용 여지) / 트레이드오프(단일 소비자 대비 플러그인 오버헤드·예시 temp 잔존) / 위험·방어(무테스트 → build+Hilt 그래프(`:app:assembleDebug`) 검증, 성공코드 규약 미확정은 open-questions).

- [ ] **Step 2: `adr/README.md` 인덱스에 ADR-0017 한 줄 등록**

기존 인덱스 테이블 형식에 맞춰 추가(파일명+심볼명, 수치 금지).

- [ ] **Step 3: `data-layer.md` "네트워킹(Assumption)" 섹션 갱신**

"Assumption/후속" 문구를 확정 내용으로 교체: 컨벤션 플러그인·`NetworkModule`·`ApiResponse`/`safeApiCall`·`AuthInterceptor`/`TokenProvider`·remote DataSource 패턴 기술. `related_adr`에 ADR-0017 추가, `verified: 2026-07-26`. "신규 데이터 추가 체크리스트"의 "원격" 항목에 remote DataSource 경로(`source.<도메인>.remote`) 언급 보강.

- [ ] **Step 4: `parfait/index.md` 상태 줄 갱신**

"원격 네트워킹은 의존만 준비, 서비스 연동은 후속" → "원격 네트워크 기초 구조(컨벤션 플러그인·NetworkModule·ApiResponse/safeApiCall) 확정, 실제 API 연동은 후속(ADR-0017)". "신규 데이터(Repo/DataSource) 추가" 라우팅 행에 remote 언급 유지.

- [ ] **Step 5: spec/plan status 갱신**

`specs/2026-07-26-data-network-setup.md` frontmatter `status: draft` → `implemented`(코드 머지 후) 또는 `in-progress`(진행 중). `plans/2026-07-26-data-network-setup.md` `status` 동일 동기화. (완료 시 각 README 아카이브 규칙 적용은 머지 후 별도.)

- [ ] **Step 6: Commit (사용자 승인 후)**

parfait 문서는 **plan repo(team-yg)** 소속. ADR·spec·plan·architecture는 코드와 같은 논리 PR이나 repo가 다르므로 별도 커밋:
```bash
cd parfait-repo-root && git add parfait/adr parfait/architecture/data-layer.md parfait/index.md parfait/specs parfait/plans
git commit -m "docs(parfait): add ADR-0017 remote network, update data-layer for network setup"
```

---

## Self-Review

- **Spec coverage**: 컨벤션 플러그인(Task 1) · ApiResponse/safeApiCall(Task 2) · AuthInterceptor/TokenProvider(Task 3) · NetworkModule(Task 4, Json 재사용 포함) · Temp 예시 1세트+RemoteDataSourceModule(Task 5) · ADR-0017+data-layer(Task 6). 스펙 "파일 구성" 표 전 항목 매핑됨.
- **Placeholder scan**: 코드 step은 실제 코드 포함. `TODO`는 스펙이 명시한 의도적 후속 지점(성공코드·토큰소스·temp 도메인)만 — 계획 공백 아님.
- **Type consistency**: `ApiResponse<T>(code,message,data)`·`safeApiCall`·`TempService.getTemp`·`TempRemoteDataSource.getTemp`·`provideTempService` 시그니처 Task 간 일치. `EmptyTokenProvider`·`AuthInterceptor(tokenProvider)` 생성자 일관.
- **Known risk**: retrofit 3.x kotlinx-serialization converter의 `asConverterFactory` import 경로 — 실제 아티팩트 export 심볼로 교정(Task 4 Step 2 명시).
