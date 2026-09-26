# 세션 인프라 (401 자동 재발급 · 강제 로그아웃) Implementation Plan

> ✅ **완료·develop 머지(PR #260 `9cfbd117`, 2026-08-15)** — 9 Task 전량이 develop에 있다.
> 체크박스는 실행 기록을 이 블록에 모으는 관례대로 미체크로 둔다.
>
> **계획이 코드와 갈린 곳 3건**(전부 스펙 쪽이 최신이고 이 계획서가 낡은 판본이다):
> ① Task 3의 `authService: Provider<AuthService>` **지연 주입이 채택되지 않았다** — 재발급 전용
> 클라이언트(`@UnauthenticatedClient`)를 따로 만들며 `Retrofit`↔`OkHttpClient`↔`Authenticator`
> Dagger 순환 자체가 사라져 `Provider`가 필요 없어졌다. Task 6의 게이트(`:app:kspDebugKotlin`이
> 순환을 끊었는지 판정)도 그래서 의미가 바뀌었고, `NetworkModule`·`ServiceModule`에
> provider 세 개(`OkHttpClient`·`Retrofit`·`AuthService`)가 더 붙었다.
> ② Task 8 Step 6·Task 9 Step 1의 `clearBackStack()` + `goTo(NavKeyLogin)`가 **`replaceAll(NavKeyLogin)`**
> 한 줄로 바뀌었다 — 같은 라운드가 `Navigator.replaceAll`을 신설하고 `clearBackStack()`을 **제거**해
> 기존 호출부 3곳(`SplashRoute`·`TermAgreeRoute`·`LoginRoute`)도 함께 옮겼다. 계획에 없던
> `NavigatorTest` 3케이스가 그 결정을 잠근다.
> ③ Task 8이 계획에 없던 UI 상태를 하나 더 들였다 — `AppSettingState.isLoggingOut` + `try`/`finally`,
> `YGActionItem(enabled = …)` 파라미터 신설(클릭만 막고 **색은 안 바꾼다** — 비활성 색이 DS에 없다).
>
> 신규 테스트는 계획의 12건 대비 **파일 5개 26케이스**로 늘었다(위 `NavigatorTest` 포함).
> 수동 확인 4항목은 **미수행**이다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-146.
> 산출물 계약의 정본은 [스펙](../../specs/archive/2026-08-15-session-token-refresh-infra.md)이다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** access token이 만료되면 화면 모르게 재발급해 원요청을 잇고, 재발급까지 서버에 거절당하면 앱 전체를 한 번에 로그인 화면으로 보낸다.

**Architecture:** OkHttp `Authenticator`가 401을 가로챈다. 동시성 방어는 세 겹(`Mutex` 직렬화 · `Authorization` 값 대조 선점 확인 · `priorResponse` 루프 가드). 재발급 실패는 두 부류로 갈라 서버 거절(401·403)만 세션을 버리고 네트워크 실패는 토큰을 유지한다. 세션 종료는 `:domain`에 둔 `SessionEvent.ForcedLogout`으로 알리고 앱 루트(`MainRoute`) 한 곳만 수집한다.

**Tech Stack:** Kotlin, OkHttp 3 `Authenticator`, Retrofit, Hilt(`Provider` 지연 주입), kotlinx.coroutines(`Mutex`·`Channel`), MockWebServer(`mockwebserver3`), MockK, kotlin.test

**Spec:** [`parfait/specs/archive/2026-08-15-session-token-refresh-infra.md`](../../specs/archive/2026-08-15-session-token-refresh-infra.md) · 대응 ADR [`parfait/adr/0021-token-refresh-forced-logout.md`](../../../adr/0021-token-refresh-forced-logout.md)

## Global Constraints

- **작업 저장소는 `TJYG-Android`**(이 문서가 있는 repo가 아니다). 브랜치는 `develop`에서 딴다.
- **Task별 로컬 커밋은 승인됐다**(2026-08-15). 각 Task의 마지막 커밋 스텝을 실행한다. SDD가 `BASE..HEAD` diff로 리뷰 패키지를 만들기 때문에 커밋이 없으면 리뷰 게이트가 돌지 않는다. **`push`와 PR 생성은 별도 승인 사항**이며 이 계획에 포함되지 않는다.
- 모든 신규 Kotlin 파일은 기존 패키지 관례를 따른다 — `data`는 `com.teamyg.parfait.data.*`, `domain`은 `com.teamyg.parfait.domain.*`.
- **`:domain`은 Android·OkHttp를 참조하지 않는다**(ADR-0001·0011). 세션 이벤트 인터페이스만 `:domain`, 구현은 `:data`.
- 실패는 Repository 경계에서 `AppError`로 바꾼다(ADR-0020). `TokenAuthenticator`는 Repository가 아니므로 `ApiException`을 직접 본다.
- 테스트는 Given-When-Then 주석을 단다(저장소 관례). 테스트 함수명은 `대상_조건_기대` 형식.
- 검증 명령은 `./gradlew :data:testDebugUnitTest`, `:domain:testDebugUnitTest`, `:feature:app:setting:impl:testDebugUnitTest`. 전체는 `./gradlew testDebugUnitTest`.
- ktlint: `./gradlew ktlintCheck`. 커밋 전 통과해야 한다.

---

## 파일 구성

| 파일 | 책임 | 신규/수정 |
|---|---|---|
| `domain/.../model/session/SessionEvent.kt` | 세션 사건 어휘. 지금은 `ForcedLogout` 하나 | 신규 |
| `domain/.../repository/session/SessionEventSource.kt` | 구독 인터페이스. feature가 보는 유일한 면 | 신규 |
| `data/.../session/SessionEventBus.kt` | `SessionEventSource` 구현. 발행+구독 겸함 | 신규 |
| `data/.../di/SessionModule.kt` | `SessionEventBus` → `SessionEventSource` 바인딩 | 신규 |
| `domain/.../model/error/ServerErrorCode.kt` | `Auth`에 토큰 거절 코드 3종 추가 | 수정 |
| `data/.../network/TokenAuthenticator.kt` | 401 가로채 재발급·재시도. 이 계획의 핵심 | 신규 |
| `data/.../di/NetworkModule.kt` | `OkHttpClient`에 `authenticator` 결합 | 수정 |
| `domain/.../repository/auth/AuthRepository.kt` | `logout()` 추가 | 수정 |
| `data/.../repository/auth/AuthRepositoryImpl.kt` | `logout()` 구현 | 수정 |
| `domain/.../usecase/auth/LogoutUseCase.kt` | 신규 |
| `feature/app/setting/impl/.../AppSettingViewModel.kt` | 로그아웃 stub 제거 + `NavigateToLogin` | 수정 |
| `feature/app/setting/impl/.../screen/AppSettingRoute.kt` | `NavigateToLogin` 처리 | 수정 |
| `feature/app/setting/impl/build.gradle.kts` | `feature.login.api` 의존 추가 | 수정 |
| `app/.../MainRoute.kt` | `ForcedLogout` 단일 수집 | 수정 |

`AuthRemoteDataSource.logout(refreshToken)`은 **이미 있다** — 새로 만들지 않는다.

---

### Task 1: 세션 이벤트 통로

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/session/SessionEvent.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/session/SessionEventSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/session/SessionEventBus.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/di/SessionModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/session/SessionEventBusTest.kt`

**Interfaces:**
- Consumes: 없음(첫 Task)
- Produces: `SessionEvent.ForcedLogout`(object), `SessionEventSource.events: Flow<SessionEvent>`, `SessionEventBus.postForcedLogout()`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/session/SessionEventBusTest.kt`:

```kotlin
package com.teamyg.parfait.data.session

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.session.SessionEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionEventBusTest {
    @Test
    fun postForcedLogout_beforeSubscribe_stillDelivers() = runTest {
        // Given 아직 아무도 구독하지 않은 버스
        val bus = SessionEventBus()

        // When 이벤트를 발행한 뒤에 구독한다
        bus.postForcedLogout()

        // Then 버퍼에 남아 있다가 전달된다 — 앱 루트가 붙기 전에 401이 나도 잃지 않는다
        bus.events.test {
            assertEquals(SessionEvent.ForcedLogout, awaitItem())
        }
    }

    @Test
    fun postForcedLogout_calledTwice_deliversOnce() = runTest {
        // Given 401 이 연달아 터져 이벤트가 두 번 발행된 버스
        val bus = SessionEventBus()
        bus.postForcedLogout()
        bus.postForcedLogout()

        // When 구독한다
        bus.events.test {
            // Then 한 번만 온다 — 이동이 두 번 일어나면 안 된다
            assertEquals(SessionEvent.ForcedLogout, awaitItem())
            expectNoEvents()
        }
    }
}
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*SessionEventBusTest*'`
Expected: 컴파일 실패 — `Unresolved reference: SessionEventBus`

- [ ] **Step 3: 최소 구현을 쓴다**

`domain/src/main/java/com/teamyg/parfait/domain/model/session/SessionEvent.kt`:

```kotlin
package com.teamyg.parfait.domain.model.session

/**
 * 화면 하나가 결정할 수 없는 세션 수준의 사건.
 *
 * 지금은 갈래가 하나뿐이지만 `sealed` 로 두는 이유는, 소비 측이 `when` 을 exhaustive 로
 * 쓰게 해 갈래가 늘 때 컴파일 단계에서 누락을 잡기 위해서다.
 */
sealed interface SessionEvent {
    /** refresh token 이 서버에 거절당해 세션을 더 유지할 수 없다 */
    data object ForcedLogout : SessionEvent
}
```

`domain/src/main/java/com/teamyg/parfait/domain/repository/session/SessionEventSource.kt`:

```kotlin
package com.teamyg.parfait.domain.repository.session

import com.teamyg.parfait.domain.model.session.SessionEvent
import kotlinx.coroutines.flow.Flow

/**
 * 세션 사건 구독구. **앱 루트 한 곳에서만 수집한다** — 화면마다 구독하면 한 이벤트로
 * 여러 번 이동한다. 구현이 `Channel` 기반이라 실제로도 단일 소비자다.
 */
interface SessionEventSource {
    val events: Flow<SessionEvent>
}
```

`data/src/main/java/com/teamyg/parfait/data/session/SessionEventBus.kt`:

```kotlin
package com.teamyg.parfait.data.session

import com.teamyg.parfait.domain.model.session.SessionEvent
import com.teamyg.parfait.domain.repository.session.SessionEventSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `Channel(CONFLATED)` 인 이유는 두 가지다.
 *
 * 구독자가 없는 순간 발행해도 버퍼에 남았다가 전달돼야 한다 — 앱 루트가 수집을 시작하기
 * 전에 401 이 나도 잃으면 안 된다. `SharedFlow` + `replay` 는 이미 소비한 이벤트가
 * 재구독으로 다시 와서 이동이 저절로 반복된다(ADR-0020 이 이펙트에서 같은 이유로 기각).
 *
 * `CONFLATED` 는 401 이 여러 건 터져 이벤트가 연달아 발행돼도 이동을 한 번으로 접는다.
 */
@Singleton
class SessionEventBus @Inject constructor() : SessionEventSource {
    private val channel = Channel<SessionEvent>(Channel.CONFLATED)

    override val events: Flow<SessionEvent> = channel.receiveAsFlow()

    fun postForcedLogout() {
        channel.trySend(SessionEvent.ForcedLogout)
    }
}
```

`data/src/main/java/com/teamyg/parfait/data/di/SessionModule.kt`:

```kotlin
package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.domain.repository.session.SessionEventSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun provideSessionEventSource(sessionEventBus: SessionEventBus): SessionEventSource = sessionEventBus
}
```

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*SessionEventBusTest*'`
Expected: PASS 2건

- [ ] **Step 5: 커밋** (사용자가 커밋을 지시한 경우에만)

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/session/SessionEvent.kt \
        domain/src/main/java/com/teamyg/parfait/domain/repository/session/SessionEventSource.kt \
        data/src/main/java/com/teamyg/parfait/data/session/SessionEventBus.kt \
        data/src/main/java/com/teamyg/parfait/data/di/SessionModule.kt \
        data/src/test/java/com/teamyg/parfait/data/session/SessionEventBusTest.kt
git commit -m "feat(session): 세션 사건 통로 신설 — ForcedLogout"
```

---

### Task 2: 토큰 거절 코드 상수 추가

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/error/ServerErrorCode.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `ServerErrorCode.Auth.INVALID_TOKEN`, `ServerErrorCode.Auth.EXPIRED_TOKEN`, `ServerErrorCode.Auth.FORBIDDEN_REFRESH_TOKEN` — 전부 `String` 상수

이 Task는 상수 선언뿐이라 자체 테스트가 없다. Task 4가 이 상수를 분기에 쓰면서 검증한다.

- [ ] **Step 1: `Auth` 객체에 상수 3개를 추가한다**

`ServerErrorCode.kt`의 `object Auth { ... }` 안, 기존 `INVALID_ID_TOKEN` 아래에 붙인다:

```kotlin
        /** 401 — 유효하지 않은 토큰입니다. `reissue`·`logout` 의 refreshToken 검증 실패 */
        const val INVALID_TOKEN = "INVALID_TOKEN"

        /** 401 — 만료된 토큰입니다. `reissue`·`logout` 의 refreshToken */
        const val EXPIRED_TOKEN = "EXPIRED_TOKEN"

        /** 403 — 다른 회원의 Refresh Token 입니다. `logout` 의 `LogoutService` */
        const val FORBIDDEN_REFRESH_TOKEN = "FORBIDDEN_REFRESH_TOKEN"
```

- [ ] **Step 2: 컴파일을 확인한다**

Run: `./gradlew :domain:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋** (사용자가 커밋을 지시한 경우에만)

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/error/ServerErrorCode.kt
git commit -m "feat(domain): 토큰 거절 서버 에러 코드 3종 추가"
```

---

### Task 3: TokenAuthenticator — 재발급 성공 경로

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt`

**Interfaces:**
- Consumes: `TokenStore`(기존: `getAccessToken()`·`getRefreshToken()`·`save(accessToken, refreshToken)`·`clear()`, 전부 `suspend`), `ApiCaller.safeApiCall(block, transform)`, `AuthService.postAuthReissue(ReissueRequest)`, `SessionEventBus.postForcedLogout()`(Task 1)
- Produces: `TokenAuthenticator(tokenStore, authService: Provider<AuthService>, apiCaller, sessionEventBus) : Authenticator` — `authenticate(route: Route?, response: Response): Request?`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt`:

```kotlin
package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.data.source.token.local.TokenStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 저장 매체를 끌어들이지 않으려고 [TokenStore] 를 메모리 페이크로 둔다.
 * 실제 [com.teamyg.parfait.data.source.token.local.EncryptedTokenStore] 는 Keystore 를
 * 요구해 JVM 단위 테스트에서 돌지 않는다.
 */
private class FakeTokenStore(
    var accessToken: String? = null,
    var refreshToken: String? = null,
) : TokenStore {
    var clearCount: Int = 0

    override suspend fun getAccessToken(): String? = accessToken

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun save(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override suspend fun clear() {
        clearCount++
        accessToken = null
        refreshToken = null
    }
}

class TokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private lateinit var tokenStore: FakeTokenStore
    private lateinit var sessionEventBus: SessionEventBus
    private lateinit var authenticator: TokenAuthenticator

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()

        tokenStore = FakeTokenStore(accessToken = OLD_ACCESS_TOKEN, refreshToken = REFRESH_TOKEN)
        sessionEventBus = SessionEventBus()

        val authService = Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthService::class.java)

        authenticator = TokenAuthenticator(
            tokenStore = tokenStore,
            authService = Provider { authService },
            apiCaller = ApiCaller(json),
            sessionEventBus = sessionEventBus,
        )
    }

    @AfterTest
    fun tearDown() {
        server.close()
    }

    /** 인증이 필요한 요청이 [token] 을 달고 나갔다가 401 을 맞은 상황을 만든다 */
    private fun unauthorizedResponse(token: String?): Response {
        val request = Request
            .Builder()
            .url(server.url("/api/parfait-groups"))
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .build()

        return Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }

    private fun enqueueReissueSuccess() {
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body(
                    """
                    {"success":true,"code":"OK","message":"성공",
                     "data":{"accessToken":"$NEW_ACCESS_TOKEN","refreshToken":"$NEW_REFRESH_TOKEN","expiresIn":3600}}
                    """.trimIndent(),
                ).build(),
        )
    }

    @Test
    fun authenticate_reissueSucceeds_retriesWithNewToken() {
        // Given 만료된 access token 으로 나갔다가 401 을 맞았고, 재발급은 성공한다
        enqueueReissueSuccess()

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 새 토큰을 단 요청이 나오고, 저장소도 갱신된다
        assertNotNull(retried)
        assertEquals("Bearer $NEW_ACCESS_TOKEN", retried.header("Authorization"))
        assertEquals(NEW_ACCESS_TOKEN, runBlocking { tokenStore.getAccessToken() })
        assertEquals(NEW_REFRESH_TOKEN, runBlocking { tokenStore.getRefreshToken() })
        assertEquals(0, tokenStore.clearCount)
    }

    private companion object {
        const val OLD_ACCESS_TOKEN = "old-access"
        const val NEW_ACCESS_TOKEN = "new-access"
        const val REFRESH_TOKEN = "refresh"
        const val NEW_REFRESH_TOKEN = "new-refresh"
    }
}
```

> `ApiCaller`의 생성자는 `@RemoteJson private val json: Json` 하나다. 한정자는 Hilt가 볼 뿐이라
> 테스트에서 `ApiCaller(json)`으로 직접 만들면 된다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*TokenAuthenticatorTest*'`
Expected: 컴파일 실패 — `Unresolved reference: TokenAuthenticator`

- [ ] **Step 3: 최소 구현을 쓴다**

`data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt`:

```kotlin
package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.data.source.auth.mapper.toAuthSessionVO
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 401 을 가로채 access token 을 재발급하고 원요청을 다시 만든다.
 *
 * [authService] 가 `Provider` 인 이유: `Retrofit` 이 `OkHttpClient` 를, `OkHttpClient` 가 이
 * 인증기를 요구해 직접 주입하면 Dagger 순환이다. 지연 주입으로 끊는다.
 *
 * `runBlocking` 을 쓰는 이유: [Authenticator] 계약이 동기다. 저장소 읽기·재발급이 모두
 * `suspend` 라 다른 길이 없다 — `TokenStoreTokenProvider` 도 같은 이유로 같은 방식이다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val authService: Provider<AuthService>,
    private val apiCaller: ApiCaller,
    private val sessionEventBus: SessionEventBus,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? = runBlocking {
        mutex.withLock {
            val refreshToken = tokenStore.getRefreshToken() ?: return@withLock null

            val session = reissue(refreshToken) ?: return@withLock null
            tokenStore.save(
                accessToken = session.accessToken.value,
                refreshToken = session.refreshToken.value,
            )
            response.request.withToken(session.accessToken.value)
        }
    }

    private suspend fun reissue(refreshToken: String): AuthSessionVO? = apiCaller
        .safeApiCall(
            block = { authService.get().postAuthReissue(ReissueRequest(refreshToken = refreshToken)) },
            transform = { it.toAuthSessionVO() },
        ).getOrElse { throwable ->
            sourceLogger.e(throwable) { "토큰 재발급 실패" }
            null
        }

    private fun Request.withToken(accessToken: String): Request = newBuilder()
        .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
        .build()

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
```

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*TokenAuthenticatorTest*'`
Expected: PASS 1건

- [ ] **Step 5: 커밋** (사용자가 커밋을 지시한 경우에만)

```bash
git add data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt \
        data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt
git commit -m "feat(network): 401 재발급 인증기 — 성공 경로"
```

---

### Task 4: 재발급 실패 갈래 4종

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt` (Task 3 파일에 추가)

**Interfaces:**
- Consumes: Task 2의 `ServerErrorCode.Auth.INVALID_TOKEN`·`EXPIRED_TOKEN`·`FORBIDDEN_REFRESH_TOKEN`, Task 1의 `SessionEventBus.postForcedLogout()`, 기존 `ApiException.Business(code, serverMessage, statusCode, errorDetail)`·`ApiException.Http(statusCode, cause)`·`ApiException.Network(cause)`
- Produces: 없음(Task 3의 클래스를 채운다)

갈래 판정은 이렇게 나눈다.

| 실패 | 토큰 | 이벤트 |
|---|---|---|
| `Business`·`Http`이고 `statusCode`가 401·403 | `clear()` | `ForcedLogout` |
| `Business`이고 `code`가 토큰 거절 3종 (`statusCode` 없어도) | `clear()` | `ForcedLogout` |
| `Network` | 유지 | 없음 |
| 그 외 | 유지 | 없음 |
| refresh token 부재 | 유지(지울 것 없음) | 없음 |

- [ ] **Step 1: 실패하는 테스트 4건을 쓴다**

`TokenAuthenticatorTest`의 `private companion object` **앞에** 추가한다:

```kotlin
    @Test
    fun authenticate_reissueRejected_clearsTokensAndPostsForcedLogout() = runTest {
        // Given 서버가 refresh token 을 401 INVALID_TOKEN 으로 거절한다
        server.enqueue(
            MockResponse
                .Builder()
                .code(401)
                .body("""{"success":false,"code":"INVALID_TOKEN","message":"유효하지 않은 토큰입니다","data":null}""")
                .build(),
        )

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 재시도하지 않고 세션을 버린다
        assertNull(retried)
        assertEquals(1, tokenStore.clearCount)
        sessionEventBus.events.test {
            assertEquals(SessionEvent.ForcedLogout, awaitItem())
        }
    }

    @Test
    fun authenticate_reissueNetworkFails_keepsTokensAndPostsNothing() = runTest {
        // Given 연결이 끊겨 재발급 요청 자체가 실패한다
        server.close()

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 토큰을 지우지 않는다 — 연결 실패는 자격증명이 죽은 것과 다른 사건이다
        assertNull(retried)
        assertEquals(0, tokenStore.clearCount)
        assertEquals(REFRESH_TOKEN, tokenStore.refreshToken)
        sessionEventBus.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun authenticate_reissueServerError_keepsTokens() = runTest {
        // Given 재발급이 500 으로 실패한다
        server.enqueue(MockResponse.Builder().code(500).body("{}").build())

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 서버 장애로 세션을 버리지 않는다
        assertNull(retried)
        assertEquals(0, tokenStore.clearCount)
        sessionEventBus.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun authenticate_noRefreshToken_postsNothing() = runTest {
        // Given 로그인한 적이 없어 refresh token 이 없다
        tokenStore.accessToken = null
        tokenStore.refreshToken = null

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(token = null))

        // Then 조용히 포기한다 — 여기서 강제 로그아웃을 쏘면 로그인 화면이 자기 자신으로 튕긴다
        assertNull(retried)
        assertEquals(0, tokenStore.clearCount)
        assertEquals(0, server.requestCount)
        sessionEventBus.events.test {
            expectNoEvents()
        }
    }
```

import에 아래를 추가한다:

```kotlin
import app.cash.turbine.test
import com.teamyg.parfait.domain.model.session.SessionEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.assertNull
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*TokenAuthenticatorTest*'`
Expected: `authenticate_reissueRejected_clearsTokensAndPostsForcedLogout`가 FAIL — `clearCount`가 0이고 이벤트가 오지 않는다. 나머지 3건은 이미 통과할 수 있다(현재 구현이 실패 시 `null`을 반환하므로) — 통과해도 그대로 둔다. 이 스텝의 목적은 **거절 갈래가 아직 없다**는 것을 확인하는 것이다.

- [ ] **Step 3: 실패 분류를 구현한다**

`TokenAuthenticator`의 `reissue`를 아래로 교체하고 `classify`를 추가한다:

```kotlin
    private suspend fun reissue(refreshToken: String): AuthSessionVO? = apiCaller
        .safeApiCall(
            block = { authService.get().postAuthReissue(ReissueRequest(refreshToken = refreshToken)) },
            transform = { it.toAuthSessionVO() },
        ).getOrElse { throwable ->
            if (throwable.isSessionDead()) {
                sourceLogger.e(throwable) { "재발급 거절 — 세션 종료" }
                tokenStore.clear()
                sessionEventBus.postForcedLogout()
            } else {
                // 연결 실패·서버 장애로 2주짜리 refresh token 을 버리지 않는다.
                // 원요청은 401 그대로 화면에 도달하고 화면이 실패를 표시한다.
                sourceLogger.e(throwable) { "재발급 실패 — 세션 유지" }
            }
            null
        }

    /**
     * 서버가 refresh token 자체를 거절했는가.
     *
     * `statusCode` 와 `code` 를 함께 보는 이유: envelope 실패는 `statusCode` 가 비어 올 수
     * 있고(`ApiCaller.runCatchingApi`), HTTP 실패는 code 없이 status 만 온다.
     */
    private fun Throwable.isSessionDead(): Boolean = when (this) {
        is ApiException.Business -> statusCode in SESSION_DEAD_STATUS_CODES || code in SESSION_DEAD_CODES
        is ApiException.Http -> statusCode in SESSION_DEAD_STATUS_CODES
        else -> false
    }
```

`private companion object`에 추가한다:

```kotlin
        val SESSION_DEAD_STATUS_CODES = setOf(401, 403)

        val SESSION_DEAD_CODES = setOf(
            ServerErrorCode.Auth.INVALID_TOKEN,
            ServerErrorCode.Auth.EXPIRED_TOKEN,
            ServerErrorCode.Auth.FORBIDDEN_REFRESH_TOKEN,
        )
```

import 추가:

```kotlin
import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.domain.model.error.ServerErrorCode
```

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*TokenAuthenticatorTest*'`
Expected: PASS 5건

- [ ] **Step 5: 커밋** (사용자가 커밋을 지시한 경우에만)

```bash
git add data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt \
        data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt
git commit -m "feat(network): 재발급 실패 갈래 분리 — 서버 거절만 세션 폐기"
```

---

### Task 5: 동시성 방어 — 선점 확인과 루프 가드

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt`

**Interfaces:**
- Consumes: Task 3·4의 `TokenAuthenticator`
- Produces: 없음

`Mutex`는 Task 3에서 이미 걸었다. 이 Task가 더하는 것은 **선점 확인**과 **루프 가드** 둘이다.

- [ ] **Step 1: 실패하는 테스트 2건을 쓴다**

`TokenAuthenticatorTest`에 추가한다:

```kotlin
    @Test
    fun authenticate_tokenAlreadyRefreshed_retriesWithoutReissue() = runTest {
        // Given 401 두 건이 같은 낡은 토큰을 들고 있었고, 첫 건이 재발급을 끝냈다
        enqueueReissueSuccess()
        authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // When 뒤따라온 두 번째 401 이 처리된다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 재발급을 다시 쏘지 않고 이미 갱신된 토큰으로 재시도만 한다
        assertNotNull(retried)
        assertEquals("Bearer $NEW_ACCESS_TOKEN", retried.header("Authorization"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun authenticate_retriedTwice_givesUp() = runTest {
        // Given 새 토큰으로 재시도했는데도 서버가 또 401 을 준 상황
        enqueueReissueSuccess()
        val exhausted = unauthorizedResponse(OLD_ACCESS_TOKEN)
            .newBuilder()
            .priorResponse(unauthorizedResponse(OLD_ACCESS_TOKEN))
            .build()

        // When 인증기가 그 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = exhausted)

        // Then 재발급조차 시도하지 않고 포기한다 — 무한 재시도를 끊는다
        assertNull(retried)
        assertEquals(0, server.requestCount)
    }
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*TokenAuthenticatorTest*'`
Expected:
- `authenticate_tokenAlreadyRefreshed_retriesWithoutReissue` FAIL — 두 번째 호출이 재발급을 또 쏴서 `server.requestCount`가 2가 되거나, 큐가 비어 실패한다
- `authenticate_retriedTwice_givesUp` FAIL — 가드가 없어 재발급을 쏜다

- [ ] **Step 3: 선점 확인과 루프 가드를 구현한다**

`authenticate`를 아래로 교체한다:

```kotlin
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        // 새 토큰으로 재시도했는데 또 401 이면 재발급으로 풀릴 문제가 아니다
        if (response.retryCount() >= MAX_RETRY) {
            sourceLogger.e { "재발급 후에도 401 — 재시도를 끊는다" }
            return null
        }

        val failedToken = response.request
            .header(AUTHORIZATION_HEADER)
            ?.removePrefix(BEARER_PREFIX)

        return runBlocking {
            mutex.withLock {
                // 기다리는 동안 다른 요청이 이미 갱신했다면 재발급 없이 새 토큰만 달아준다.
                // 이 확인이 없으면 Mutex 는 직렬화만 할 뿐, 대기하던 요청들이 차례로 각자
                // 재발급을 쏜다.
                val currentToken = tokenStore.getAccessToken()
                if (currentToken != null && currentToken != failedToken) {
                    return@withLock response.request.withToken(currentToken)
                }

                val refreshToken = tokenStore.getRefreshToken() ?: return@withLock null

                val session = reissue(refreshToken) ?: return@withLock null
                tokenStore.save(
                    accessToken = session.accessToken.value,
                    refreshToken = session.refreshToken.value,
                )
                response.request.withToken(session.accessToken.value)
            }
        }
    }

    /** 이 응답이 몇 번째 시도인가. `priorResponse` 체인 길이 + 자기 자신 */
    private fun Response.retryCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
```

`private companion object`에 추가한다:

```kotlin
        const val MAX_RETRY = 2
```

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*TokenAuthenticatorTest*'`
Expected: PASS 7건

- [ ] **Step 5: 커밋** (사용자가 커밋을 지시한 경우에만)

```bash
git add data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt \
        data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt
git commit -m "feat(network): 재발급 선점 확인·재시도 루프 가드"
```

---

### Task 6: OkHttpClient에 인증기 결합

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt`

**Interfaces:**
- Consumes: Task 3~5의 `TokenAuthenticator`
- Produces: 없음

- [ ] **Step 1: `provideOkHttpClient`를 고친다**

```kotlin
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient
        .Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
            },
        ).connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
```

import 추가:

```kotlin
import com.teamyg.parfait.data.network.TokenAuthenticator
```

- [ ] **Step 2: Hilt 그래프가 성립하는지 확인한다**

Run: `./gradlew :app:kspDebugKotlin`
Expected: BUILD SUCCESSFUL. **실패하면 `Provider<AuthService>` 지연 주입이 순환을 못 끊은 것이다** — 에러 메시지의 순환 경로를 그대로 보고하고 멈춘다.

- [ ] **Step 3: 기존 테스트가 깨지지 않았는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest`
Expected: 전부 PASS

- [ ] **Step 4: 커밋** (사용자가 커밋을 지시한 경우에만)

```bash
git add data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt
git commit -m "feat(network): OkHttpClient에 재발급 인증기 결합"
```

---

### Task 7: AuthRepository.logout

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/auth/AuthRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImpl.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/LogoutUseCase.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImplTest.kt` (기존 파일에 추가)

**Interfaces:**
- Consumes: 기존 `AuthRemoteDataSource.logout(refreshToken: RefreshToken): Result<Unit>`, `TokenStore.getRefreshToken()`·`clear()`
- Produces: `AuthRepository.logout(): Result<Unit>`, `LogoutUseCase.invoke(): Result<Unit>`

- [ ] **Step 1: 실패하는 테스트 3건을 쓴다**

기존 `AuthRepositoryImplTest`에 추가한다. 기존 파일의 mock 이름·생성 방식을 그대로 따르고, 아래는 그 관례에 맞춰 쓴다:

```kotlin
    @Test
    fun logout_serverSucceeds_clearsLocalTokens() = runTest {
        // Given 로그인 상태이고 서버 로그아웃이 성공한다
        coEvery { tokenStore.getRefreshToken() } returns "refresh"
        coEvery { authRemoteDataSource.logout(RefreshToken("refresh")) } returns Result.success(Unit)
        coEvery { tokenStore.clear() } returns Unit

        // When 로그아웃한다
        val result = repository.logout()

        // Then 성공이고 로컬 토큰이 지워진다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { tokenStore.clear() }
    }

    @Test
    fun logout_serverFails_stillClearsLocalTokens() = runTest {
        // Given 서버 로그아웃이 실패한다
        coEvery { tokenStore.getRefreshToken() } returns "refresh"
        coEvery { authRemoteDataSource.logout(RefreshToken("refresh")) } returns
            Result.failure(ApiException.Network(IOException("연결 실패")))
        coEvery { tokenStore.clear() } returns Unit

        // When 로그아웃한다
        val result = repository.logout()

        // Then 사용자가 눌렀으니 이 기기에서는 나간다 — 서버 실패는 전파하지 않는다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { tokenStore.clear() }
    }

    @Test
    fun logout_noRefreshToken_clearsWithoutCallingServer() = runTest {
        // Given 이미 토큰이 없는 상태
        coEvery { tokenStore.getRefreshToken() } returns null
        coEvery { tokenStore.clear() } returns Unit

        // When 로그아웃한다
        val result = repository.logout()

        // Then 서버를 부르지 않고 로컬만 정리한다
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { authRemoteDataSource.logout(any()) }
        coVerify(exactly = 1) { tokenStore.clear() }
    }
```

import 추가:

```kotlin
import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.domain.model.auth.RefreshToken
import java.io.IOException
import kotlin.test.assertTrue
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*AuthRepositoryImplTest*'`
Expected: 컴파일 실패 — `Unresolved reference: logout`

- [ ] **Step 3: 구현한다**

`AuthRepository`에 추가:

```kotlin
    /**
     * 서버 세션을 끊고 로컬 토큰을 지운다.
     *
     * **서버 호출이 실패해도 로컬은 정리하고 성공을 반환한다** — 사용자가 로그아웃을 눌렀으면
     * 이 기기에서는 나가는 것이 기대 동작이고, 서버 세션 정리 실패는 로그로 남긴다.
     */
    suspend fun logout(): Result<Unit>
```

`AuthRepositoryImpl`에 추가:

```kotlin
    override suspend fun logout(): Result<Unit> {
        val refreshToken = tokenStore.getRefreshToken()

        if (refreshToken != null) {
            authRemoteDataSource
                .logout(RefreshToken(refreshToken))
                .onFailure { throwable -> repositoryLogger.e(throwable) { "서버 로그아웃 실패 — 로컬은 정리한다" } }
        }

        tokenStore.clear()
        return Result.success(Unit)
    }
```

import 추가:

```kotlin
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.auth.RefreshToken
```

`LogoutUseCase.kt` 신규:

```kotlin
package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.repository.auth.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}
```

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*AuthRepositoryImplTest*'`
Expected: PASS

- [ ] **Step 5: 커밋** (사용자가 커밋을 지시한 경우에만)

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/repository/auth/AuthRepository.kt \
        domain/src/main/java/com/teamyg/parfait/domain/usecase/auth/LogoutUseCase.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImpl.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/auth/AuthRepositoryImplTest.kt
git commit -m "feat(auth): 로그아웃 — 서버 실패해도 로컬 정리"
```

---

### Task 8: 설정 화면 로그아웃 결선

**Files:**
- Modify: `feature/app/setting/impl/build.gradle.kts`
- Modify: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/viewmodel/AppSettingViewModel.kt`
- Modify: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/route/AppSettingRoute.kt`
- Test: `feature/app/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/app/setting/impl/viewmodel/AppSettingViewModelTest.kt`

**Interfaces:**
- Consumes: Task 7의 `LogoutUseCase.invoke(): Result<Unit>`, 기존 `BaseViewModel.launch(key)`, `feature.login.api`의 `NavKeyLogin`
- Produces: `AppSettingSideEffect.NavigateToLogin`

- [ ] **Step 1: 의존을 추가한다**

`feature/app/setting/impl/build.gradle.kts`의 `dependencies`에:

```kotlin
    implementation(projects.feature.login.api)
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`AppSettingViewModelTest.kt`(신규):

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.usecase.auth.LogoutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val logout: LogoutUseCase = mockk()

    private fun viewModel() = AppSettingViewModel(logout = logout)

    @Test
    fun clickLogout_succeeds_navigatesToLogin() = runTest(mainDispatcherRule.dispatcher) {
        // Given 로그아웃이 성공하는 화면
        coEvery { logout() } returns Result.success(Unit)
        val viewModel = viewModel()

        viewModel.effect.test {
            // When 로그아웃을 누른다
            viewModel.processIntent(AppSettingIntent.ClickLogout)
            advanceUntilIdle()

            // Then 로그인 화면으로 간다
            assertEquals(AppSettingSideEffect.NavigateToLogin, awaitItem())
            coVerify(exactly = 1) { logout() }
        }
    }

    @Test
    fun clickLogout_whileLoggingOut_doesNotRequestAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 로그아웃 요청이 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        coEvery { logout() } coAnswers {
            gate.await()
            Result.success(Unit)
        }
        val viewModel = viewModel()
        viewModel.processIntent(AppSettingIntent.ClickLogout)
        runCurrent()

        // When 한 번 더 누른다
        viewModel.processIntent(AppSettingIntent.ClickLogout)
        runCurrent()

        // Then 중복 요청이 나가지 않는다
        coVerify(exactly = 1) { logout() }

        gate.complete(Unit)
        advanceUntilIdle()
    }
}
```

- [ ] **Step 3: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :feature:app:setting:impl:testDebugUnitTest`
Expected: 컴파일 실패 — `AppSettingViewModel` 생성자가 인자를 받지 않고 `NavigateToLogin`이 없다

- [ ] **Step 4: ViewModel을 고친다**

`AppSettingSideEffect`에 추가:

```kotlin
    data object NavigateToLogin : AppSettingSideEffect
```

생성자와 `handleClickLogout`를 교체:

```kotlin
@HiltViewModel
class AppSettingViewModel
@Inject
constructor(
    private val logout: LogoutUseCase,
) : BaseViewModel<AppSettingState, AppSettingIntent, AppSettingSideEffect>(
    initialState = AppSettingState(),
) {
```

```kotlin
    private fun handleClickLogout() {
        launch(key = KEY_LOGOUT) {
            // logout() 은 서버 실패도 성공으로 접어 돌려준다 — 이 기기에서 나가는 것이
            // 사용자가 누른 것의 의미이고, 화면이 갈래를 나눌 이유가 없다
            logout()
            postSideEffect(AppSettingSideEffect.NavigateToLogin)
        }
    }
```

클래스 말미에 추가:

```kotlin
    private companion object {
        /** [launch] 중복 실행 가드 키 — 로그아웃 job 하나를 가리킨다 */
        const val KEY_LOGOUT = "logout"
    }
```

import 추가: `com.teamyg.parfait.domain.usecase.auth.LogoutUseCase`

- [ ] **Step 5: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :feature:app:setting:impl:testDebugUnitTest`
Expected: PASS 2건

- [ ] **Step 6: Route에 이동을 붙인다**

Route의 `viewModel.effect.collect` `when`에 추가한다(기존 갈래들과 같은 형식):

```kotlin
                AppSettingSideEffect.NavigateToLogin -> {
                    navigator.clearBackStack()
                    navigator.goTo(NavKeyLogin)
                }
```

import 추가: `com.teamyg.parfait.feature.login.api.NavKeyLogin`

- [ ] **Step 7: 빌드를 확인한다**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 커밋** (사용자가 커밋을 지시한 경우에만)

```bash
git add feature/app/setting/impl/
git commit -m "feat(setting): 로그아웃 결선 — stub 제거"
```

---

### Task 9: 앱 루트 강제 로그아웃 수집

**Files:**
- Modify: `app/src/main/java/com/teamyg/parfait/MainRoute.kt`

**Interfaces:**
- Consumes: Task 1의 `SessionEventSource.events`, `SessionEvent.ForcedLogout`, 기존 `Navigator.clearBackStack()`·`goTo()`, `feature.login.api`의 `NavKeyLogin`
- Produces: 없음(마지막 Task)

`app` 모듈은 이미 `projects.domain`·`projects.feature.login.api`를 의존하므로 gradle 수정이 없다.

- [ ] **Step 1: `MainRoute`에 수집을 붙인다**

`MainRoute`의 시그니처에 `sessionEventSource`를 추가하고 본문 앞에 `LaunchedEffect`를 둔다:

```kotlin
@Composable
fun MainRoute(
    navigator: Navigator,
    entryBuilders: Set<EntryProviderScope<NavKey>.(Navigator) -> Unit>,
    sessionEventSource: SessionEventSource,
    modifier: Modifier = Modifier,
) {
    // 세션 사건은 화면 하나가 결정할 수 없다. 여기 한 곳에서만 수집한다 —
    // 화면마다 구독하면 한 이벤트로 이동이 여러 번 일어난다.
    LaunchedEffect(Unit) {
        sessionEventSource.events.collect { event ->
            when (event) {
                SessionEvent.ForcedLogout -> {
                    navigator.clearBackStack()
                    navigator.goTo(NavKeyLogin)
                }
            }
        }
    }

    SharedTransitionLayout(modifier = modifier) {
```

import 추가:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import com.teamyg.parfait.domain.model.session.SessionEvent
import com.teamyg.parfait.domain.repository.session.SessionEventSource
import com.teamyg.parfait.feature.login.api.NavKeyLogin
```

- [ ] **Step 2: `MainActivity`에서 주입해 내려준다**

`MainActivity`에 필드를 추가한다:

```kotlin
    @Inject
    lateinit var sessionEventSource: SessionEventSource
```

`setContent`의 `MainRoute` 호출에 인자를 추가한다:

```kotlin
                MainRoute(
                    navigator = navigator,
                    entryBuilders = entryBuilders,
                    sessionEventSource = sessionEventSource,
                    modifier = Modifier.fillMaxSize(),
                )
```

import 추가: `com.teamyg.parfait.domain.repository.session.SessionEventSource`

- [ ] **Step 3: 빌드를 확인한다**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 전체 테스트를 돌린다**

Run: `./gradlew testDebugUnitTest`
Expected: 전부 PASS

- [ ] **Step 5: ktlint를 돌린다**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋** (사용자가 커밋을 지시한 경우에만)

```bash
git add app/src/main/java/com/teamyg/parfait/MainRoute.kt \
        app/src/main/java/com/teamyg/parfait/MainActivity.kt
git commit -m "feat(app): 강제 로그아웃 이벤트 단일 수집"
```

---

## 수동 확인 (구현 후)

자동 테스트로 덮지 않은 것들이다. 실기기·에뮬레이터에서 확인한다.

1. **재발급 투명성** — 로그인 후 서버에서 access token을 만료시키고(또는 만료 짧은 환경) 그룹 목록을 새로고침한다. 화면에 에러가 뜨지 않고 목록이 그려져야 한다.
2. **강제 로그아웃** — refresh token까지 무효화한 뒤 아무 화면에서 API를 호출한다. 로그인 화면으로 이동하고, 뒤로가기로 이전 화면에 돌아갈 수 없어야 한다.
3. **오프라인** — 비행기 모드에서 앱을 켜고 인증 API를 호출한다. **로그아웃되지 않아야 한다** — 이 갈래가 무너지면 지하철 진입이 곧 로그아웃이다.
4. **로그아웃 버튼** — 설정에서 로그아웃 → 로그인 화면. 다시 로그인하면 정상 동작.
