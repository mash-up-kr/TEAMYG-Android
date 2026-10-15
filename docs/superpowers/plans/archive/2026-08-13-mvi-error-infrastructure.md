---
id: mvi-error-infrastructure
title: MVI 공통 에러·이펙트 인프라 구현 계획
status: done
type: work-order
created: 2026-08-13
updated: 2026-08-15
platforms: android
owner: Android
related_adr: ADR-0005, ADR-0020
related_spec: mvi-error-infrastructure
related_code: BaseViewModel, MviContract, AppError, AppErrorMapper, ApiException, viewModelLogger, screenLogger, MainDispatcherRule
archived_reason: PR #241 develop 머지 완료(2026-08-15, `80895eb1`)
tags: [plan, parfait, mvi, error]
---

# MVI 공통 에러·이펙트 인프라 Implementation Plan

> ✅ **완료 · develop 머지**(2026-08-15, PR #241 `80895eb1`). SDD 4 Task, fix 라운드 0.
> **계획 대비 가장 큰 이탈은 Task 4의 산출물이 통째로 철회된 것이다** — 공용 `error` 채널·
> `postError`·`CollectAppError`가 최종 리뷰 뒤 사용자 판정으로 걷혔다
> ([ADR-0020 번복 절](../../../adr/0020-mvi-error-effect-infrastructure.md#번복-공용-error-채널-철회)).
> 아래 Task 4 본문은 **당시 계획 그대로** 두고 고치지 않는다 — 무엇을 지었다가 왜 되돌렸는지가
> 기록이다. 실제 develop 코드는 [스펙](../../specs/archive/2026-08-13-mvi-error-infrastructure.md)이 정본.

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** `BaseViewModel`이 실패 경로·중복 실행·이펙트 유실을 다룰 수 있게 만들고, 도메인 에러 타입을 신설한다.

**Architecture:** `:domain`에 `AppError`(sealed, `Exception` 하위) 3갈래를 두고 `:data`가 Repository 경계에서 `ApiException`을 변환한다. `core:ui`의 `BaseViewModel`은 이펙트 전달을 `Channel(BUFFERED)`로 바꾸고 `launch`·`postError`·`error`를 더한다. 기존 API 시그니처는 그대로라 19개 ViewModel은 수정하지 않는다.

**Tech Stack:** Kotlin, kotlinx-coroutines(`Channel`·`StateFlow`), Hilt, JUnit4 + kotlin.test + Turbine + MockK, `parfait.test.unit` 컨벤션 플러그인

**Spec:** [`parfait/specs/2026-08-13-mvi-error-infrastructure.md`](../../specs/archive/2026-08-13-mvi-error-infrastructure.md) · 결정 근거 [ADR-0020](../../../adr/0020-mvi-error-effect-infrastructure.md)

**작업 대상 저장소:** `TJYG-Android`(별도 repo). 경로는 그 repo 루트 기준이다.

## Global Constraints

- **기존 19개 ViewModel을 수정하지 않는다.** `updateState`·`postSideEffect` 시그니처를 바꾸면 안 된다.
- **`CancellationException`은 절대 삼키지 않는다.** 잡았으면 재던진다. 변환도 하지 않는다.
- **커밋은 Task 단위 로컬 커밋만.** `git push`·PR 생성은 하지 않는다.
- 테스트는 Given/When/Then 한국어 주석 + `kotlin.test` 단언을 쓴다(기존 `PolicyRemoteDataSourceImplTest`·`GroupSettingViewModelTest` 스타일).
- ViewModel 테스트는 `runTest(mainDispatcherRule.dispatcher)`로 스케줄러를 하나로 묶는다. 인자 없는 `runTest`는 Main 큐를 비우지 못한다.
- 새 DI 모듈 파일을 만들지 않는다. 바인딩은 기존 역할 모듈에 추가한다.
- 로딩 상태는 인터페이스로 강제하지 않는다. 각 `UiState`가 소유하되 **필드명은 `isLoading`**으로 통일한다(이 계획은 규약만 정하고, 실제 사용은 각 화면 결선 라운드에서).
- ktlint를 통과해야 한다: `./gradlew ktlintCheck`.

## File Structure

| 파일 | 책임 |
|---|---|
| `domain/src/main/java/com/teamyg/parfait/domain/model/error/AppError.kt` | 도메인 에러 3갈래. 신설 |
| `data/src/main/java/com/teamyg/parfait/data/model/error/AppErrorMapper.kt` | `ApiException → AppError` 변환 + `Result` 확장. 신설 |
| `core/ui/src/main/java/com/teamyg/parfait/core/ui/BaseViewModel.kt` | 이펙트 `Channel` 전환 + `launch`·`postError`·`error`. 수정 |
| `core/ui/src/main/java/com/teamyg/parfait/core/ui/CollectAppError.kt` | 에러 수집 컴포저블(기본 = 로그 + TODO). 신설 |
| `core/ui/build.gradle.kts` | `parfait.test.unit` 플러그인 추가. 수정 |
| `core/ui/src/test/java/com/teamyg/parfait/core/ui/BaseViewModelTest.kt` | 베이스 동작 테스트. 신설 |
| `data/src/test/java/com/teamyg/parfait/data/model/error/AppErrorMapperTest.kt` | 매핑 테스트. 신설 |

---

### Task 1: AppError 도메인 타입과 매핑

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/error/AppError.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/model/error/AppErrorMapper.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/model/error/AppErrorMapperTest.kt`

**Interfaces:**
- Consumes: `com.teamyg.parfait.data.model.exception.ApiException`(기존 sealed 5종 — `Business(code, serverMessage, statusCode, errorDetail)`·`EmptyBody(code, serverMessage)`·`Http(statusCode, cause)`·`Network(cause)`·`Unknown(cause)`)
- Produces: `AppError.Network(cause)`·`AppError.Server(code, statusCode, serverMessage)`·`AppError.Unexpected(cause)`, `internal fun Throwable.toAppError(): AppError`, `internal fun <T> Result<T>.mapErrorToAppError(): Result<T>`

- [x] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/model/error/AppErrorMapperTest.kt`

```kotlin
package com.teamyg.parfait.data.model.error

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.domain.model.error.AppError
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class AppErrorMapperTest {
    // `ApiCallerTest` 의 기존 헬퍼와 같은 방식이다
    private fun httpException(statusCode: Int): HttpException {
        val responseBody = "".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Unit>(statusCode, responseBody))
    }

    @Test
    fun toAppError_business_mapsToServerWithCodeAndStatus() {
        // Given 서버가 에러 envelope 를 준 상황
        val exception = ApiException.Business(
            code = "INVALID_ID_TOKEN",
            serverMessage = "유효하지 않은 ID 토큰입니다",
            statusCode = 401,
            errorDetail = null,
        )

        // When 도메인 에러로 변환
        val error = exception.toAppError()

        // Then code·statusCode·메시지가 그대로 실린다
        val server = assertIs<AppError.Server>(error)
        assertEquals("INVALID_ID_TOKEN", server.code)
        assertEquals(401, server.statusCode)
        assertEquals("유효하지 않은 ID 토큰입니다", server.serverMessage)
    }

    @Test
    fun toAppError_network_mapsToNetworkKeepingCause() {
        // Given 연결 실패
        val cause = IOException("connection reset")
        val exception = ApiException.Network(cause)

        // When 도메인 에러로 변환
        val error = exception.toAppError()

        // Then Network 갈래이고 원인이 보존된다
        val network = assertIs<AppError.Network>(error)
        assertSame(cause, network.cause)
    }

    @Test
    fun toAppError_httpAndEmptyBodyAndUnknown_mapToUnexpected() {
        // Given envelope 밖 HTTP 실패·빈 본문·정체불명 예외
        val http = ApiException.Http(statusCode = 500, cause = httpException(500))
        val emptyBody = ApiException.EmptyBody(code = "OK", serverMessage = "본문 없음")
        val unknown = ApiException.Unknown(IllegalStateException("boom"))

        // When 각각 변환
        // Then 셋 다 Unexpected 로 접힌다
        assertIs<AppError.Unexpected>(http.toAppError())
        assertIs<AppError.Unexpected>(emptyBody.toAppError())
        assertIs<AppError.Unexpected>(unknown.toAppError())
    }

    @Test
    fun toAppError_cancellation_rethrows() {
        // Given 취소 예외
        val cancellation = CancellationException("cancelled")

        // When·Then 변환하지 않고 그대로 다시 던진다
        assertFailsWith<CancellationException> { cancellation.toAppError() }
    }

    @Test
    fun mapErrorToAppError_failure_replacesThrowableWithAppError() {
        // Given ApiException 을 실은 실패 Result
        val result: Result<String> = Result.failure(
            ApiException.Business(
                code = "KAKAO_SERVER_UNAVAILABLE",
                serverMessage = "카카오 서버에 연결할 수 없습니다",
                statusCode = 503,
                errorDetail = null,
            ),
        )

        // When 도메인 에러로 갈아끼운다
        val mapped = result.mapErrorToAppError()

        // Then 실패 원인이 AppError.Server 다
        val server = assertIs<AppError.Server>(mapped.exceptionOrNull())
        assertEquals("KAKAO_SERVER_UNAVAILABLE", server.code)
    }

    @Test
    fun mapErrorToAppError_success_passesValueThrough() {
        // Given 성공 Result
        val result = Result.success("ok")

        // When 변환
        val mapped = result.mapErrorToAppError()

        // Then 값이 그대로다
        assertEquals("ok", mapped.getOrNull())
    }
}
```

- [x] **Step 2: 실패 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*AppErrorMapperTest*"`
Expected: FAIL — `Unresolved reference: AppError` / `Unresolved reference: toAppError`

- [x] **Step 3: AppError 작성**

`domain/src/main/java/com/teamyg/parfait/domain/model/error/AppError.kt`

```kotlin
package com.teamyg.parfait.domain.model.error

/**
 * 화면·UseCase 가 보는 도메인 에러.
 *
 * 갈래가 셋인 이유는 화면이 실제로 다르게 굴 수 있는 경우가 셋뿐이기 때문이다 —
 * 재시도를 권할 수 있는가([Network]), 서버가 이유를 말해줬는가([Server]),
 * 우리 쪽 결함인가([Unexpected]).
 *
 * `Exception` 하위인 이유는 `Result.failure` 가 `Throwable` 을 요구해서다.
 * 데이터 레이어의 `ApiException` 을 Repository 경계에서 이 타입으로 바꾼다.
 */
sealed class AppError(
    message: String?,
    cause: Throwable?,
) : Exception(message, cause) {
    /** 연결 실패·타임아웃. 재시도가 의미 있는 유일한 갈래다 */
    data class Network(override val cause: Throwable?) : AppError(cause?.message, cause)

    /**
     * 서버가 에러 envelope 를 준 경우.
     *
     * [code] 를 enum 이 아니라 String 으로 두는 이유: 서버가 코드를 추가할 때마다 앱이
     * 깨지면 안 된다. 코드 문자열은 도메인 간 유일하지 않으므로([statusCode] 가 다른
     * 동명 코드가 존재한다) 분기할 때 둘을 함께 본다.
     */
    data class Server(
        val code: String,
        val statusCode: Int?,
        val serverMessage: String,
    ) : AppError(serverMessage, null)

    /** envelope 밖 HTTP 실패·빈 본문·파싱/매핑 실패 등 그 외 전부 */
    data class Unexpected(override val cause: Throwable?) : AppError(cause?.message, cause)
}
```

- [x] **Step 4: 매퍼 작성**

`data/src/main/java/com/teamyg/parfait/data/model/error/AppErrorMapper.kt`

```kotlin
package com.teamyg.parfait.data.model.error

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.domain.model.error.AppError
import kotlinx.coroutines.CancellationException

/**
 * 데이터 레이어 예외를 도메인 에러로 바꾼다. **Repository 경계에서만** 호출한다 —
 * 이 변환이 있어야 feature 모듈이 `:data` 를 보지 않는다.
 *
 * [CancellationException] 은 변환하지 않고 재던진다. 취소를 에러로 오분류하면
 * 화면을 벗어날 때마다 에러가 발행된다.
 */
internal fun Throwable.toAppError(): AppError = when (this) {
    is CancellationException -> throw this
    is ApiException.Business -> AppError.Server(
        code = code,
        statusCode = statusCode,
        serverMessage = serverMessage,
    )

    is ApiException.Network -> AppError.Network(cause)
    is ApiException.Http -> AppError.Unexpected(this)
    is ApiException.EmptyBody -> AppError.Unexpected(this)
    is ApiException.Unknown -> AppError.Unexpected(cause)
    else -> AppError.Unexpected(this)
}

/** 실패 원인만 [AppError] 로 갈아끼운다. 성공 값은 그대로 통과한다 */
internal fun <T> Result<T>.mapErrorToAppError(): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { Result.failure(it.toAppError()) },
)
```

- [x] **Step 5: 통과 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*AppErrorMapperTest*"`
Expected: PASS (6 tests)

- [x] **Step 6: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/error/AppError.kt \
        data/src/main/java/com/teamyg/parfait/data/model/error/AppErrorMapper.kt \
        data/src/test/java/com/teamyg/parfait/data/model/error/AppErrorMapperTest.kt
git commit -m "feat(domain): AppError 도메인 에러 타입과 ApiException 매핑 추가"
```

---

### Task 2: core:ui 테스트 소스셋 + 이펙트 Channel 전환

**Files:**
- Modify: `core/ui/build.gradle.kts`
- Modify: `core/ui/src/main/java/com/teamyg/parfait/core/ui/BaseViewModel.kt`
- Test: `core/ui/src/test/java/com/teamyg/parfait/core/ui/BaseViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `AppError`(이 Task에서는 아직 쓰지 않는다), 기존 `UiState`·`UiIntent`·`UiSideEffect`, `viewModelLogger`
- Produces: `val effect: Flow<E>`(구독자 카운트 포함), `postSideEffect(effect: E)`(시그니처 불변, 내부 `trySend`)

- [x] **Step 1: 테스트 플러그인 추가**

`core/ui/build.gradle.kts`의 `plugins` 블록에 한 줄 더한다.

```kotlin
plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.parfait.test.unit)
}
```

- [x] **Step 2: 실패 테스트 작성**

`core/ui/src/test/java/com/teamyg/parfait/core/ui/BaseViewModelTest.kt`

```kotlin
package com.teamyg.parfait.core.ui

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

private data class TestState(val count: Int = 0) : UiState

private sealed interface TestIntent : UiIntent {
    data object Increase : TestIntent
}

private sealed interface TestSideEffect : UiSideEffect {
    data class Notify(val value: Int) : TestSideEffect
}

private class TestViewModel : BaseViewModel<TestState, TestIntent, TestSideEffect>(TestState()) {
    override fun processIntent(intent: TestIntent) {
        when (intent) {
            TestIntent.Increase -> updateState { copy(count = count + 1) }
        }
    }

    fun emit(value: Int) = postSideEffect(TestSideEffect.Notify(value))
}

class BaseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun postSideEffect_noCollectorAtEmission_stillDeliveredOnLaterCollection() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 아무도 수집하지 않는 시점의 ViewModel
            val viewModel = TestViewModel()

            // When 이펙트를 먼저 발행하고 나중에 수집을 시작
            viewModel.emit(1)
            runCurrent()

            // Then 유실되지 않고 전달된다
            viewModel.effect.test {
                assertEquals(TestSideEffect.Notify(1), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun effect_afterCollectionEnds_doesNotReplayConsumedItems() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 이펙트 하나를 이미 소비한 ViewModel
            val viewModel = TestViewModel()
            viewModel.emit(1)
            runCurrent()
            viewModel.effect.test {
                assertEquals(TestSideEffect.Notify(1), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            // When 다시 구독하고 새 이펙트를 발행
            viewModel.effect.test {
                viewModel.emit(2)
                runCurrent()

                // Then 소비한 1 은 다시 오지 않고 2 만 온다
                assertEquals(TestSideEffect.Notify(2), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun updateState_reducer_appliesToLatestState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태
        val viewModel = TestViewModel()

        // When 인텐트를 두 번 처리
        viewModel.processIntent(TestIntent.Increase)
        viewModel.processIntent(TestIntent.Increase)

        // Then 누적된다(기존 API 회귀 방지)
        assertEquals(2, viewModel.state.value.count)
    }
}
```

- [x] **Step 3: 실패 확인**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "*BaseViewModelTest*"`
Expected: FAIL — `postSideEffect_noCollectorAtEmission_stillDeliveredOnLaterCollection`이 타임아웃/유실로 실패한다(현재 `MutableSharedFlow`는 구독자가 없으면 `emit`이 대기하고 값을 보관하지 않는다)

- [x] **Step 4: BaseViewModel 이펙트 전달 교체**

`core/ui/src/main/java/com/teamyg/parfait/core/ui/BaseViewModel.kt` — `_effect`·`effect`·`postSideEffect`만 바꾼다.

```kotlin
package com.teamyg.parfait.core.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(initialState: S) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    private val effectSubscribers = AtomicInteger(0)

    /**
     * 1회성 효과. **화면당 한 곳(Route)에서만 수집한다.**
     *
     * `Channel` 인 이유: 구독자가 없는 순간 발행해도 버퍼에 남았다가 전달되고, 이미 소비한
     * 이펙트는 재구독해도 다시 오지 않는다. `SharedFlow` + `replay` 는 후자를 깨서
     * 화면 재진입·Activity 재생성 때 내비게이션이 저절로 다시 실행된다.
     *
     * 대신 단일 소비자다 — 두 곳에서 수집하면 이펙트가 한쪽에만 간다. 조용히 넘어가지
     * 않도록 동시 구독자 수를 세어 로그를 남긴다.
     */
    val effect: Flow<E> = _effect.receiveAsFlow()
        .onStart {
            val count = effectSubscribers.incrementAndGet()
            if (count > 1) {
                viewModelLogger.e { "effect 를 ${count}곳에서 수집한다 — 이펙트가 한쪽에만 전달된다" }
            }
        }
        .onCompletion { effectSubscribers.decrementAndGet() }

    abstract fun processIntent(intent: I)

    protected fun updateState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun postSideEffect(effect: E) {
        if (_effect.trySend(effect).isFailure) {
            viewModelLogger.e { "이펙트 버퍼가 가득 차 드롭됐다: $effect" }
        }
    }
}
```

- [x] **Step 5: 통과 확인**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "*BaseViewModelTest*"`
Expected: PASS (3 tests)

- [x] **Step 6: 기존 화면 회귀 확인**

Run: `./gradlew :app:assembleDebug :feature:groups:setting:impl:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — `postSideEffect` 시그니처가 그대로라 19개 ViewModel과 21개 수집 지점이 무수정으로 컴파일된다. `GroupSettingViewModelTest`의 `viewModel.effect.test { }` 3건도 통과한다.

- [x] **Step 7: 커밋**

```bash
git add core/ui/build.gradle.kts \
        core/ui/src/main/java/com/teamyg/parfait/core/ui/BaseViewModel.kt \
        core/ui/src/test/java/com/teamyg/parfait/core/ui/BaseViewModelTest.kt
git commit -m "fix(core-ui): 이펙트 전달을 Channel 로 바꿔 유실·재발화 차단"
```

---

### Task 3: launch 가드 · postError · error 채널

**Files:**
- Modify: `core/ui/src/main/java/com/teamyg/parfait/core/ui/BaseViewModel.kt`
- Test: `core/ui/src/test/java/com/teamyg/parfait/core/ui/BaseViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `com.teamyg.parfait.domain.model.error.AppError`, Task 2의 `BaseViewModel`
- Produces: `val error: Flow<AppError>`, `protected fun postError(error: AppError)`, `protected fun launch(key: Any? = null, onError: ((AppError) -> Unit)? = null, block: suspend CoroutineScope.() -> Unit): Job?`

- [x] **Step 1: 실패 테스트 추가**

`BaseViewModelTest.kt`의 `TestViewModel`에 헬퍼를 더하고 테스트 6개를 추가한다.

`TestViewModel`을 아래로 교체:

```kotlin
private class TestViewModel : BaseViewModel<TestState, TestIntent, TestSideEffect>(TestState()) {
    override fun processIntent(intent: TestIntent) {
        when (intent) {
            TestIntent.Increase -> updateState { copy(count = count + 1) }
        }
    }

    fun emit(value: Int) = postSideEffect(TestSideEffect.Notify(value))

    fun run(
        key: Any? = null,
        onError: ((AppError) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? = launch(key = key, onError = onError, block = block)
}
```

추가 import:

```kotlin
import com.teamyg.parfait.domain.model.error.AppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
```

추가 테스트:

```kotlin
    @Test
    fun launch_sameKeyWhileRunning_doesNotStartSecondJob() = runTest(mainDispatcherRule.dispatcher) {
        // Given 끝나지 않는 작업이 key "load" 로 돌고 있다
        val viewModel = TestViewModel()
        var secondRan = false
        viewModel.run(key = "load") { awaitCancellation() }
        runCurrent()

        // When 같은 key 로 다시 요청
        val second = viewModel.run(key = "load") { secondRan = true }
        advanceUntilIdle()

        // Then 두 번째 job 은 생기지 않고 블록도 실행되지 않는다
        assertNull(second)
        assertEquals(false, secondRan)
    }

    @Test
    fun launch_differentKeys_bothRun() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서로 다른 key 두 개
        val viewModel = TestViewModel()
        var firstRan = false
        var secondRan = false

        // When 각각 실행
        viewModel.run(key = "a") { firstRan = true }
        viewModel.run(key = "b") { secondRan = true }
        advanceUntilIdle()

        // Then 둘 다 실행된다
        assertEquals(true, firstRan)
        assertEquals(true, secondRan)
    }

    @Test
    fun launch_afterPreviousJobCompleted_sameKeyRunsAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 같은 key 의 첫 작업이 이미 끝났다
        val viewModel = TestViewModel()
        viewModel.run(key = "load") { }
        advanceUntilIdle()

        // When 같은 key 로 다시 요청
        var secondRan = false
        val second = viewModel.run(key = "load") { secondRan = true }
        advanceUntilIdle()

        // Then 실행된다(완료된 job 이 맵에서 정리됐다)
        assertNotNull(second)
        assertEquals(true, secondRan)
    }

    @Test
    fun launch_blockThrows_emitsUnexpectedError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 에러를 수집하는 화면
        val viewModel = TestViewModel()

        viewModel.error.test {
            // When 블록이 예상 못 한 예외를 던진다
            viewModel.run { throw IllegalStateException("boom") }
            advanceUntilIdle()

            // Then Unexpected 로 감싸져 발행된다
            val error = assertIs<AppError.Unexpected>(awaitItem())
            assertEquals("boom", error.cause?.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun launch_onErrorGiven_handlerReceivesInsteadOfChannel() = runTest(mainDispatcherRule.dispatcher) {
        // Given onError 를 넘긴 호출
        val viewModel = TestViewModel()
        var handled: AppError? = null

        // When 블록이 던진다
        viewModel.run(onError = { handled = it }) { throw IllegalStateException("boom") }
        advanceUntilIdle()

        // Then 핸들러가 받는다
        assertIs<AppError.Unexpected>(handled)
    }

    @Test
    fun launch_cancellation_doesNotEmitError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 에러를 수집하는 화면
        val viewModel = TestViewModel()

        viewModel.error.test {
            // When 블록이 CancellationException 을 던진다
            viewModel.run { throw CancellationException("cancelled") }
            advanceUntilIdle()

            // Then 에러로 오분류하지 않는다
            expectNoEvents()
        }
    }
```

- [x] **Step 2: 실패 확인**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "*BaseViewModelTest*"`
Expected: FAIL — `Unresolved reference: launch` / `Unresolved reference: error`

- [x] **Step 3: BaseViewModel 확장**

`BaseViewModel.kt`에 아래를 더한다(Task 2 내용은 유지).

추가 import:

```kotlin
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.domain.model.error.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
```

클래스 본문에 추가:

```kotlin
    private val _error = Channel<AppError>(Channel.BUFFERED)

    /**
     * 화면이 따로 선언하지 않아도 되는 공통 실패 통로. `E` 와 분리해 두면 화면마다
     * `SideEffect` 에 `ShowError` 를 중복 선언하지 않아도 된다. 수집은 `CollectAppError`.
     */
    val error: Flow<AppError> = _error.receiveAsFlow()

    /** `viewModelScope` 는 `Main.immediate` 라 이 맵 접근은 항상 메인 스레드 단일이다 */
    private val runningJobs = mutableMapOf<Any, Job>()

    protected fun postError(error: AppError) {
        if (_error.trySend(error).isFailure) {
            viewModelLogger.e { "에러 버퍼가 가득 차 드롭됐다: $error" }
        }
    }

    /**
     * ViewModel 작업을 실행한다. UI 이벤트를 코루틴으로 옮기는 상태홀더 경계라
     * `viewModelScope` 를 쓰는 것이 맞다.
     *
     * @param key 같은 key 의 작업이 아직 돌고 있으면 **새로 시작하지 않고 `null` 을 반환**한다
     *   (버튼 연타로 인한 중복 호출 차단). `null` 이면 중복 검사를 하지 않는다.
     * @param onError 예상 못 한 예외 처리. 없으면 [postError] 로 흘린다.
     *
     * `Result.failure` 는 값이지 예외가 아니므로 여기서 잡히지 않는다 — 호출부가 명시적으로
     * 처리한다. 이 가드는 매퍼 버그·NPE 같은 *예상 못 한* 예외용이다.
     */
    protected fun launch(
        key: Any? = null,
        onError: ((AppError) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? {
        if (key != null && runningJobs[key]?.isActive == true) return null

        val job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val appError = e as? AppError ?: AppError.Unexpected(e)
                if (onError != null) onError(appError) else postError(appError)
            }
        }

        if (key != null) {
            runningJobs[key] = job
            // 같은 key 로 이미 다음 job 이 등록됐다면 그것을 지우면 안 된다
            job.invokeOnCompletion { if (runningJobs[key] === job) runningJobs.remove(key) }
        }
        return job
    }
```

- [x] **Step 4: 통과 확인**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "*BaseViewModelTest*"`
Expected: PASS (9 tests)

- [x] **Step 5: 커밋**

```bash
git add core/ui/src/main/java/com/teamyg/parfait/core/ui/BaseViewModel.kt \
        core/ui/src/test/java/com/teamyg/parfait/core/ui/BaseViewModelTest.kt
git commit -m "feat(core-ui): BaseViewModel 에 launch 중복 가드·공통 에러 채널 추가"
```

---

### Task 4: CollectAppError 컴포저블과 전체 검증

**Files:**
- Create: `core/ui/src/main/java/com/teamyg/parfait/core/ui/CollectAppError.kt`

**Interfaces:**
- Consumes: Task 3의 `BaseViewModel.error`
- Produces: `@Composable fun CollectAppError(viewModel: BaseViewModel<*, *, *>, onError: (AppError) -> Unit = …)`

- [x] **Step 1: 컴포저블 작성**

`core/ui/src/main/java/com/teamyg/parfait/core/ui/CollectAppError.kt`

```kotlin
package com.teamyg.parfait.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.teamyg.parfait.domain.model.error.AppError

/**
 * ViewModel 의 공통 에러 통로를 수집한다. Route 에서 한 줄로 붙인다.
 *
 * 기본 동작이 로그뿐인 것은 **의도된 공백**이다 — 에러 UX 디자인이 아직 없다.
 * 문구·토스트가 정해지면 [defaultAppErrorHandler] 한 곳만 고치면 전 화면에 적용된다.
 */
@Composable
fun CollectAppError(
    viewModel: BaseViewModel<*, *, *>,
    onError: (AppError) -> Unit = defaultAppErrorHandler,
) {
    LaunchedEffect(viewModel) {
        viewModel.error.collect(onError)
    }
}

private val defaultAppErrorHandler: (AppError) -> Unit = { error ->
    // TODO(에러 UX 미정): 디자인 확정 후 YGToast 노출로 교체한다
    screenLogger.e { "처리되지 않은 AppError: $error" }
}
```

- [x] **Step 2: 컴파일 확인**

Run: `./gradlew :core:ui:assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 3: 전체 검증**

Run: `./gradlew ktlintCheck :core:ui:testDebugUnitTest :data:testDebugUnitTest :domain:test :app:assembleDebug`
Expected: BUILD SUCCESSFUL — 신규 15 테스트 통과, 기존 테스트 회귀 0

- [x] **Step 4: 커밋**

```bash
git add core/ui/src/main/java/com/teamyg/parfait/core/ui/CollectAppError.kt
git commit -m "feat(core-ui): 공통 에러 수집 컴포저블 CollectAppError 추가"
```

---

## 완료 조건

- [x] `./gradlew ktlintCheck` 통과
- [x] `:core:ui:testDebugUnitTest` 9건, `:data:testDebugUnitTest` 신규 6건 포함 전체 통과
- [x] `:app:assembleDebug` 통과 — **기존 19개 ViewModel 파일 diff 0줄**임을 `git diff --stat`으로 확인
- [x] push·PR 없음(로컬 커밋 4개)

## 함정

- **`runTest`를 인자 없이 부르지 않는다.** `runTest(mainDispatcherRule.dispatcher)`로 스케줄러를 묶어야 `advanceUntilIdle()`이 Main 큐를 비운다.
- **`invokeOnCompletion`에서 무조건 `remove(key)` 하면 안 된다.** 이전 job이 완료됐지만 핸들러가 아직 안 돈 사이에 같은 key로 새 job이 등록될 수 있고, 그때 새 job의 엔트리를 지우면 중복 방어가 뚫린다. `=== job` 비교가 그 방어다.
- **`catch (e: Throwable)`이 `CancellationException`보다 뒤에 와야 한다.** 순서가 바뀌면 취소를 삼킨다.
- **`effect`를 두 곳에서 수집하지 않는다.** 로그가 뜨면 설계가 잘못된 것이지 로그를 끄면 되는 것이 아니다.
