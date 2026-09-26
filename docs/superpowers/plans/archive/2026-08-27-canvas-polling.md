---
id: canvas-polling
title: PR3 — 캔버스 주기 폴링 · 병합 규칙 · 하루 경계
status: done
type: work-order
created: 2026-08-27
updated: 2026-08-31
platforms: android
owner: Parfait 팀
related_adr: ADR-0029, ADR-0023, ADR-0026, ADR-0025
related_spec: canvas-today-ssot-polling
related_code: BaseViewModel, CanvasPoller, CanvasLocalDataSource, ParfaitRepository, ParfaitRepositoryImpl, TokenAuthenticator, ObserveParfaitDayBoundaryUseCase, RefreshTodayParfaitDetailUseCase, CanvasMainViewModel, CanvasBGEditViewModel, CanvasBGEditUiState, CanvasToppingPlaceViewModel, CanvasToppingPlaceUiState, AddToppingUseCase, UpdateToppingUseCase, DayWindow
archived_reason: PR #404 로 develop 머지(2026-08-31) — 주기 폴링이 스택 3단의 셋째 단으로 함께 들어왔다
tags: [plan, parfait, canvas, polling, coroutines]
---

# PR3 — 캔버스 주기 폴링 · 병합 규칙 · 하루 경계 Implementation Plan

> ✅ **develop 에 머지됐다(2026-08-31, PR #404 `2c7bb31b`)** — 스택 3단이 한 머지로 들어왔고
> 머지 커밋의 트리가 브랜치 팁(`6bd21fb7`)과 같아 **충돌 해소 편집이 0건**이다. 아래 머리말이
> "release 계보에만 있다"고 적던 상태는 끝났다(OQ-P-311 ③). Task 의 체크는 완료 표시이고,
> 「수동 확인」은 실기기 확인 기록이 없어 그대로 둔다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ⚠️ **이 계획은 2026-08-30 델타(PR #407)를 모른다.** `loadTodayCanvas()` 를 지우고 구독으로
> 대신하자고 적는데, 그 함수가 그날 **첫 조회 덮개**(`isInitialLoading`)를 갖게 됐다. 그대로
> 지우면 덮개도 함께 사라진다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-326 ⑥.
>
> ✅ **코드는 이미 그 자리를 풀었고, 답이 계획보다 한 겹 두껍다(2026-08-30 리베이스).** 덮개는
> 구독 안에서 파생시키되(구독이 열릴 때 캔버스가 없으면 켜고, 캔버스가 실리면 내린다) 그것만으로는
> 갱신 실패에서 덮개가 풀리지 않는다 — 캐시가 아무것도 방출하지 않기 때문이다. 그래서 폴러에
> `refreshFailures` 를 더하고 `ParfaitRepository.todayCanvasRefreshFailures` ·
> `ObserveTodayParfaitRefreshFailureUseCase` 로 화면까지 잇는다. 아래 Task 가 지우자고 적는
> `ShowTodayCanvasError` 도 그 트리거를 얻어 되살아났다 → 스펙 「실패 표현」 as-built.
>
> 📌 **`develop` 에도 들어왔다(2026-08-31, PR #404)** — PR1·PR2 를 품은 이 스택이 한 머지로 갔다
> (OQ-P-311 ③).

**Goal:** 오늘 캔버스 저장소 위에 주기 폴링을 얹어, 다른 멤버가 올린 토핑이 화면을 나갔다 오지 않아도 나타나게 한다.

**Architecture:** 폴링 로직은 `:data`의 `CanvasPoller`(`@Singleton`) 하나가 소유하고, 수명은 `BaseViewModel`에 새로 세우는 **구독 수 기반 헬퍼**에 매단다. 라우트 셋이 모두 `collectAsStateWithLifecycle()`을 쓰므로 화면이 백그라운드로 가거나 컴포지션에서 빠지면 구독이 끊기고 폴러의 참조 계수도 내려간다. 주기 갱신은 **부작용 없는 상세 조회**를 쓰고, 캔버스를 만들어야 할 때만 오늘 조회를 쓴다. 모든 갱신이 폴러를 통과하며 갱신마다 주기를 다시 센다.

**Tech Stack:** Kotlin, Coroutines/Flow, Hilt, MockK, Turbine, kotlinx-coroutines-test

**Spec:** [`parfait/specs/2026-08-27-canvas-today-ssot-polling.md`](../../specs/archive/2026-08-27-canvas-today-ssot-polling.md) 「PR3」
**대응 ADR:** [`parfait/adr/0029-canvas-today-ssot-polling.md`](../../../adr/0029-canvas-today-ssot-polling.md)

**작업 저장소:** `TJYG-Android`. **PR2 위에 쌓는다.**

## ⚠️ 착수 전 확인 게이트

Task 1은 `core:ui`에 **공용 API를 새로 내고** `architecture/state-management.md`에 **두 번째 구독 방식을 규약으로 세운다.** 이 스택에서 가장 넓게 퍼지는 변경이고, ADR-0029 스스로 그 대안("저장소가 폴링을 소유하되 화면이 명시적으로 시작·중지")을 **"기각 근거가 얇아 되돌릴 첫 후보"**로 적어 두었다.

**Task 1을 시작하기 전에 사용자에게 두 가지를 확인받는다.**

1. 헬퍼를 `core:ui`의 공용 표면으로 낼지, 캔버스 모듈 안에 가둘지.
2. ADR-0029의 기각 후보(화면이 명시적으로 start/stop)로 되돌릴지.

확인 없이 진행하지 않는다.

## Global Constraints

- **커밋은 하되 push·PR은 사용자 확인 후에만.**
- **코드 주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다).** 서버 동작은 반드시 `api/parfait.md`를 괄호로 가리킨다.
  - 아키텍처 결정은 코드가 아니라 `parfait/adr/`·`parfait/architecture/`에 쓰고 코드에는 포인터 한 줄만 둔다.
- **주석·KDoc·문서는 한국어로 쓴다.**
- **테스트 함수 이름은 카멜케이스**(`invoke_condition_expectation`). 이 저장소에 백틱 이름은 한 건도 없다.
- **기존 테스트 헬퍼 이름을 지어내지 않는다** — 팩토리는 `viewModel()`, 캔버스 헬퍼는 각 파일의 기존 `canvas(...)`.
- **매직 넘버 대신 이름 있는 상수.** 폴링 주기는 `CANVAS_POLL_INTERVAL`, 구독 정지 유예는 `SUBSCRIPTION_STOP_TIMEOUT`.
- **ViewModel 테스트만** `runTest(mainDispatcherRule.dispatcher)`. `:data`·`:domain`에는 `Dispatchers.Main`이 없어 맨 `runTest`가 맞다.
- **⚠️ `backgroundScope`에서 도는 작업은 `advanceUntilIdle()`이 돌리지 않는다.** coroutines-test 1.11.0의 `advanceUntilIdle`은 foreground 이벤트만 기준으로 삼는다. 폴러 테스트는 `runCurrent()`와 `advanceTimeBy(d)` + `runCurrent()`를 쓴다.
- **폴러는 `ParfaitRepository`를 주입받지 않는다** — 저장소가 폴러를 주입받으므로 순환이 된다. 원격·로컬 데이터소스를 직접 쓴다.
- **`:feature`는 `ParfaitRepository`를 직접 보지 않는다.** 갱신은 UseCase를 거친다.
- **⚠️ `:domain`은 순수 Kotlin JVM 모듈이다.** 전체 테스트는 `./gradlew test`로 돌린다.
- **CI가 게이트하는 것은 `ktlintCheck`와 `test`다.**

---

## File Structure

**신설**

| 파일 | 책임 |
|------|------|
| `data/src/main/java/com/teamyg/parfait/data/model/qualifier/ApplicationScope.kt` | 프로세스 수명 스코프 한정자(이 저장소 관례상 한정자는 파일 하나당 하나) |
| `data/src/main/java/com/teamyg/parfait/data/di/ApplicationScopeModule.kt` | 그 스코프 제공 |
| `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt` | 참조 계수 + 주기 갱신 루프 + 강제 갱신 |
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/RefreshTodayParfaitDetailUseCase.kt` | 상세 조회로 캐시 갱신(`:feature`가 저장소를 직접 못 본다) |
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/ObserveParfaitDayBoundaryUseCase.kt` | 파르페 하루 경계 티커 |
| `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt` | 폴러 테스트 |
| `domain/src/test/java/com/teamyg/parfait/domain/usecase/parfait/ObserveParfaitDayBoundaryUseCaseTest.kt` | 티커 테스트 |

**변경**

| 파일 | 변경 |
|------|------|
| `core/ui/.../BaseViewModel.kt` | `launchWhileSubscribed` 헬퍼 |
| `core/ui/src/test/.../BaseViewModelTest.kt` | 헬퍼 테스트(새 파일을 만들지 않고 여기 더한다) |
| `data/.../repository/parfait/ParfaitRepositoryImpl.kt` | 구독에 폴러 계수 연동, 갱신을 폴러로 위임 |
| `data/.../network/TokenAuthenticator.kt` | 정리 시 폴러 중단 |
| `feature/.../viewmodel/CanvasMainViewModel.kt` | 구독 헬퍼 전환, 경계 티커, 스포트라이트 해제, 진입 갱신 제거, 지난 날 구독 중단 |
| `feature/.../viewmodel/CanvasBGEditViewModel.kt` | 구독 헬퍼 전환, dirty·툼스톤 병합, `confirmedToppings` 제거, 진입 갱신 제거, 쓰기 뒤 강제 갱신 |
| `feature/.../viewmodel/CanvasToppingPlaceViewModel.kt` | 구독 헬퍼 전환, `positionZ` 재계산, 진입 갱신 제거, 쓰기 뒤 강제 갱신 |
| `parfait/architecture/state-management.md` | 구독 수명 헬퍼 규약 한 절 |

`LogoutUseCase`는 PR2에서 `ParfaitRepository.clearTodayCanvas()`를 부르고, Task 4가 그 구현에 `stopAll()`을 넣으므로 이 PR에서 손대지 않는다.

---

### Task 1: `BaseViewModel`에 구독 수명 헬퍼를 세운다

> **위 「착수 전 확인 게이트」를 먼저 통과할 것.**

**Files:**
- Modify: `core/ui/src/main/java/com/teamyg/parfait/core/ui/BaseViewModel.kt`
- Modify: `core/ui/src/test/java/com/teamyg/parfait/core/ui/BaseViewModelTest.kt`
- Modify: `parfait/architecture/state-management.md` (문서 저장소)

`core/ui`에는 `test` 소스셋과 `parfait.test.unit`, `BaseViewModelTest`, `MainDispatcherRule`이 **이미 있다.** 새 테스트 파일을 만들지 않고 기존 파일에 케이스를 더한다.

**Interfaces:**
- Consumes: `BaseViewModel._state`(내부)
- Produces: `protected fun <T> launchWhileSubscribed(stopTimeout: Duration = SUBSCRIPTION_STOP_TIMEOUT, source: () -> Flow<T>, collector: suspend (T) -> Unit): Job`

- [x] **Step 1: 실패하는 테스트 다섯을 쓴다**

`BaseViewModelTest`에 더한다. 다섯 번째가 **자기 고착 회귀 테스트**다 — 이것이 없으면 Task 6에서 같은 실수를 다시 한다.

```kotlin
private data class ProbeState(val value: Int = 0) : UiState
private object ProbeIntent : UiIntent
private object ProbeEffect : UiSideEffect

private class ProbeViewModel(
    private val upstream: Flow<Int>,
    private val readsOwnState: Boolean = false,
) : BaseViewModel<ProbeState, ProbeIntent, ProbeEffect>(ProbeState()) {
    var openCount = 0
        private set

    init {
        launchWhileSubscribed(
            source = {
                openCount++
                if (readsOwnState) state.map { it.value }.flatMapLatest { upstream } else upstream
            },
            collector = { value -> updateState { copy(value = value) } },
        )
    }

    override fun processIntent(intent: ProbeIntent) = Unit
}
```

```kotlin
    @Test
    fun launchWhileSubscribed_withoutSubscribers_doesNotOpenTheUpstream() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProbeViewModel(MutableStateFlow(1))
        advanceUntilIdle()

        assertEquals(0, viewModel.openCount)
    }

    @Test
    fun launchWhileSubscribed_withASubscriber_opensTheUpstream() = runTest(mainDispatcherRule.dispatcher) {
        val upstream = MutableStateFlow(1)
        val viewModel = ProbeViewModel(upstream)

        val job = backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        assertEquals(1, viewModel.openCount)
        assertEquals(1, viewModel.state.value.value)
        job.cancel()
    }

    @Test
    fun launchWhileSubscribed_afterTheTimeout_closesTheUpstream() = runTest(mainDispatcherRule.dispatcher) {
        val upstream = MutableStateFlow(1)
        val viewModel = ProbeViewModel(upstream)

        val job = backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        job.cancel()
        advanceTimeBy(10.seconds)
        advanceUntilIdle()

        upstream.value = 2
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.value)
    }

    @Test
    fun launchWhileSubscribed_resubscribedWithinTheTimeout_doesNotReopen() = runTest(mainDispatcherRule.dispatcher) {
        val upstream = MutableStateFlow(1)
        val viewModel = ProbeViewModel(upstream)

        val first = backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        first.cancel()
        advanceTimeBy(1.seconds)

        val second = backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        assertEquals(1, viewModel.openCount)
        second.cancel()
    }

    @Test
    fun launchWhileSubscribed_whenTheSourceReadsOwnState_stillCloses() = runTest(mainDispatcherRule.dispatcher) {
        val upstream = MutableStateFlow(1)
        val viewModel = ProbeViewModel(upstream, readsOwnState = true)

        val job = backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        job.cancel()
        advanceTimeBy(10.seconds)
        advanceUntilIdle()

        upstream.value = 2
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.value)
    }
```

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :core:ui:test`
Expected: 컴파일 실패 — `launchWhileSubscribed` 없음

- [x] **Step 3: 헬퍼를 구현한다**

`BaseViewModel` 안, `launch` 아래에 넣는다. 파라미터 이름은 **`source`**다 — 이 계획의 모든 호출부가 named argument로 그 이름을 쓴다.

```kotlin
    /**
     * 화면이 **실제로 보고 있는 동안에만** [source] 를 연다. 라우트가
     * `collectAsStateWithLifecycle()` 로 [state] 를 구독하므로, 화면이 백그라운드로 가거나
     * 컴포지션에서 빠지면 여기서 연 업스트림도 함께 끊긴다.
     *
     * [launch] 와 갈라 두는 이유는 수명이 다르기 때문이다 — [launch] 는 ViewModel 수명이라
     * 백스택 아래에 깔린 화면에서도 계속 돈다(`architecture/state-management.md`).
     *
     * ⚠️ **[source] 안에서 [state] 를 수집하면 안 된다.** 활성 조건이 [state] 의 구독자 수라,
     * 열린 업스트림 자신이 구독자로 세어져 계수가 0 으로 내려가지 않는다. 화면 조건으로 업스트림을
     * 가르려면 [state] 가 아닌 별도 flow 를 둔다.
     *
     * @param stopTimeout 마지막 구독자가 떠난 뒤 업스트림을 닫기까지의 유예. 화면 전환·구성
     *   변경의 짧은 공백에서 업스트림이 껐다 켜지지 않게 한다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <T> launchWhileSubscribed(
        stopTimeout: Duration = SUBSCRIPTION_STOP_TIMEOUT,
        source: () -> Flow<T>,
        collector: suspend (T) -> Unit,
    ): Job = viewModelScope.launch {
        _state.subscriptionCount
            .map { it > 0 }
            .distinctUntilChanged()
            .flatMapLatest { subscribed ->
                // 구독이 끊겨도 유예 동안은 열어 둔다. 그 사이 다시 붙으면 아래 flatMapLatest 가
                // 이 대기를 취소하므로 업스트림이 이어진다
                if (subscribed) flowOf(true) else flow { delay(stopTimeout); emit(false) }
            }.distinctUntilChanged()
            .flatMapLatest { active -> if (active) source() else emptyFlow() }
            .collect(collector)
    }

    private companion object {
        val SUBSCRIPTION_STOP_TIMEOUT: Duration = 5.seconds
    }
```

`flow { … }` 빌더와 이름이 겹치지 않으므로 정규화가 필요 없다. 기본 인자값은 `BaseViewModel` 안에서 해석되므로 companion이 `private`이어도 된다.

- [x] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :core:ui:test :core:ui:ktlintCheck`
Expected: PASS. 다섯 번째 테스트는 헬퍼만으로는 통과하지 않을 수 있다 — 그때는 **KDoc 경고를 계약으로 삼고 그 테스트를 지운 뒤 Task 6이 `state`를 안 읽도록 하는 것**으로 대신한다. 어느 쪽을 택했는지 계획 실행 기록에 남긴다.

- [x] **Step 5: 규약 문서에 한 절을 더한다**

`parfait/architecture/state-management.md`에서 "구독은 `BaseViewModel.launch`로 연다"는 **`## UI State가 담는 것 / 담지 않는 것` 안의 중첩 불릿**이지 절이 아니다. 거기에 헤딩을 끼우면 문서 구조가 깨진다. **`## 3분할 계약` 아래 `### 작업 실행은 launch(key, onError, block)` 다음에 형제 `###`로** 넣고, 기존 불릿에서는 새 절로 링크만 건다.

```markdown
### 화면이 보는 동안만 살아야 하는 구독은 `launchWhileSubscribed`

`BaseViewModel.launch`로 연 구독은 **ViewModel 수명**에 걸린다 — 백스택 아래에 깔린 화면에서도
계속 돈다. 그것이 맞는 경우가 대부분이지만, 업스트림이 주기적으로 서버를 부르는 종류라면
보이지 않는 화면 때문에 요청이 계속 나간다.

그런 구독은 `launchWhileSubscribed`로 연다. 노출한 `state`의 구독자 수가 0보다 큰 동안에만
업스트림이 살아 있고, 라우트가 `collectAsStateWithLifecycle()`을 쓰므로 화면이 백그라운드로
가거나 컴포지션에서 빠지면 함께 끊긴다. 마지막 구독자가 떠난 뒤 유예를 두어 화면 전환의 짧은
공백에서 업스트림이 껐다 켜지지 않게 한다.

⚠️ **`source` 안에서 `state`를 수집하면 안 된다** — 열린 업스트림 자신이 구독자로 세어져
계수가 0으로 내려가지 않는다. 화면 조건으로 업스트림을 가르려면 별도 flow를 둔다.

**둘 중 하나를 임의로 고르지 않는다.** 기준은 "이 구독이 서버를 계속 부르는가"다.
근거는 [ADR-0029](../../adr/0029-canvas-today-ssot-polling.md).
```

- [x] **Step 6: 커밋**

```bash
git add core/ui/
git commit -m "feat: 화면이 보는 동안만 사는 구독 헬퍼를 세운다"
```

문서 저장소는 따로 커밋한다.

---

### Task 2: `@ApplicationScope`를 제공한다

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/model/qualifier/ApplicationScope.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/di/ApplicationScopeModule.kt`

이 저장소의 한정자는 전부 `data/model/qualifier/`에 파일 하나당 하나씩 산다(`RemoteJson`·`LocalJson`·`UnauthenticatedClient`·`UploadClient`). 그 관례를 따른다.

- [x] **Step 1: 한정자를 쓴다**

```kotlin
package com.teamyg.parfait.data.model.qualifier

import javax.inject.Qualifier

/** 프로세스와 수명을 같이 하는 스코프. 화면·ViewModel 보다 오래 살아야 하는 작업에만 쓴다 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
```

- [x] **Step 2: 제공 모듈을 쓴다**

```kotlin
package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.model.qualifier.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationScopeModule {
    /** [SupervisorJob] 인 이유: 여기서 도는 작업 하나가 실패해도 나머지를 함께 끄면 안 된다 */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
```

- [x] **Step 3: 컴파일·ktlint 확인 후 커밋**

Run: `./gradlew :data:compileDebugKotlin :data:ktlintCheck`

```bash
git add data/src/main/java/com/teamyg/parfait/data/model/qualifier/ApplicationScope.kt data/src/main/java/com/teamyg/parfait/data/di/ApplicationScopeModule.kt
git commit -m "chore: 프로세스 수명 코루틴 스코프를 제공한다"
```

---

### Task 3: `CanvasPoller`를 만든다

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt`

**Interfaces:**
- Consumes: Task 2의 `@ApplicationScope`, `ParfaitRemoteDataSource`(함수 5개), `CanvasLocalDataSource`
- Produces:
  - `fun acquire(groupId: GroupId)` — **non-suspend**
  - `fun release(groupId: GroupId)` — **non-suspend**
  - `suspend fun refreshNow(groupId: GroupId, forceToday: Boolean = false): Result<Unit>`
  - `fun refreshNowAsync(groupId: GroupId, forceToday: Boolean = false)`
  - `fun stopAll()`

계수 조작을 `Mutex`가 아니라 `synchronized`로 하는 이유는 둘이다. 첫째, `acquire`/`release`가 `Flow`의 `onStart`/`onCompletion`에서 불리는데 `onCompletion`은 **취소된 코루틴에서** 돌아, 거기서 서스펜드하면 계수가 안 내려간다. 둘째, `stopAll()`은 `TokenAuthenticator`가 OkHttp 스레드의 `runBlocking` 안에서 부르므로 같은 자료구조를 다른 스레드가 만진다.

- [x] **Step 1: 실패하는 테스트를 쓴다**

⚠️ `advanceUntilIdle()`을 쓰지 않는다. 폴러가 `backgroundScope`에서 도는데 그 이벤트는 background로 분류돼 `advanceUntilIdle()`이 하나도 돌리지 않는다.

```kotlin
package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

private val GROUP = GroupId(1L)

class CanvasPollerTest {
    private fun canvas(date: LocalDate = parfaitToday()) = CanvasVO(
        parfaitId = ParfaitId(100L),
        date = date,
        status = CanvasStatus.ACTIVE,
        lastClosedDate = null,
        members = emptyList(),
        background = null,
        toppings = emptyList(),
    )

    /**
     * [gate] 를 주면 응답을 붙들어 둔다 — 중첩 가드와 정리 경합을 재현하는 데 쓴다.
     * `ParfaitRemoteDataSource` 의 나머지 셋(getYears·getPastCanvases·changeCanvasBackground)은
     * `error("폴러가 부르지 않는다")` 로 채운다.
     */
    private class FakeRemote(
        private val response: CanvasVO,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : ParfaitRemoteDataSource {
        var todayCallCount = 0
            private set
        var detailCallCount = 0
            private set

        override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> {
            todayCallCount++
            gate?.await()
            return Result.success(response)
        }

        override suspend fun getCanvasDetail(groupId: GroupId, parfaitId: ParfaitId): Result<CanvasVO> {
            detailCallCount++
            gate?.await()
            return Result.success(response)
        }
    }

    @Test
    fun acquire_callsTodayOnceImmediately() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()

        assertEquals(1, remote.todayCallCount)
        assertEquals(0, remote.detailCallCount)
    }

    @Test
    fun poll_afterTheCacheIsWarm_usesTheDetailEndpoint() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(5.seconds)
        runCurrent()

        assertEquals(1, remote.todayCallCount)
        assertEquals(1, remote.detailCallCount)
    }

    @Test
    fun poll_whenTheCachedDateIsStale_fallsBackToToday() = runTest {
        val yesterday = parfaitToday().minus(DatePeriod(days = 1))
        val remote = FakeRemote(canvas(yesterday))
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(5.seconds)
        runCurrent()

        // 캐시에 실린 날짜가 어제라 다음 주기도 오늘 조회를 고른다
        assertEquals(2, remote.todayCallCount)
        assertEquals(0, remote.detailCallCount)
    }

    @Test
    fun acquire_twice_stillCallsOncePerInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(5.seconds)
        runCurrent()

        assertEquals(2, remote.todayCallCount + remote.detailCallCount)
    }

    @Test
    fun release_lastSubscriber_stopsCalling() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        poller.release(GROUP)
        advanceTimeBy(30.seconds)
        runCurrent()

        assertEquals(1, remote.todayCallCount + remote.detailCallCount)
    }

    @Test
    fun refreshNow_sendsExactlyOneRequest() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        val before = remote.todayCallCount + remote.detailCallCount

        poller.refreshNow(GROUP)
        runCurrent()

        assertEquals(before + 1, remote.todayCallCount + remote.detailCallCount)
    }

    @Test
    fun refreshNow_restartsTheInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()

        advanceTimeBy(4.seconds)
        poller.refreshNow(GROUP)
        runCurrent()
        val afterForced = remote.todayCallCount + remote.detailCallCount

        // 원래 주기였다면 1초 뒤에 한 번 더 나갔어야 한다
        advanceTimeBy(2.seconds)
        runCurrent()

        assertEquals(afterForced, remote.todayCallCount + remote.detailCallCount)
    }

    @Test
    fun refresh_whileAnotherIsInFlight_skipsThisRound() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = FakeRemote(canvas(), gate)
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        assertEquals(1, remote.todayCallCount)

        // 첫 요청이 아직 안 끝난 채로 주기를 두 번 민다
        advanceTimeBy(11.seconds)
        runCurrent()

        assertEquals(1, remote.todayCallCount + remote.detailCallCount)

        gate.complete(Unit)
        runCurrent()
    }

    @Test
    fun stopAll_lateResponse_doesNotReviveTheCache() = runTest {
        val gate = CompletableDeferred<Unit>()
        val local = CanvasLocalDataSourceImpl()
        val poller = CanvasPoller(backgroundScope, FakeRemote(canvas(), gate), local)

        poller.acquire(GROUP)
        runCurrent()

        poller.stopAll()
        gate.complete(Unit)
        runCurrent()

        assertNull(local.cachedTodayCanvas(GROUP))
    }
}
```

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :data:test --tests "*CanvasPollerTest*"`
Expected: 컴파일 실패 — `CanvasPoller` 없음

- [x] **Step 3: 폴러를 구현한다**

```kotlin
package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.data.model.qualifier.ApplicationScope
import com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 실측 전 값이다(OQ-P-320) */
private val CANVAS_POLL_INTERVAL: Duration = 5.seconds

/**
 * 오늘 캔버스를 주기적으로 다시 받아 [CanvasLocalDataSource] 에 싣는다
 * (`adr/0029-canvas-today-ssot-polling.md`).
 *
 * 값이 아니라 **트리거**를 소유한다 — 나중에 푸시로 갈아 끼울 때 바뀌는 자리를 하나로 두기
 * 위해서다. 저장소(`ParfaitRepositoryImpl`)가 이쪽을 주입받으므로 반대로 저장소를 주입받지
 * 않는다.
 *
 * 계수 조작에 코루틴 뮤텍스가 아니라 [synchronized] 를 쓰는 이유: [release] 가 `onCompletion`
 * 에서 불리는데 그 블록은 **취소된 코루틴에서 돈다** — 거기서 서스펜드하면 계수가 안 내려가
 * 폴링이 남는다. [stopAll] 도 OkHttp 스레드에서 불린다.
 *
 * @param clock 캐시의 날짜가 오늘인지 보는 데 쓴다. 주입하지 않으면 하루 경계 전환을 테스트로
 *   고정할 수 없다.
 */
@Singleton
class CanvasPoller @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val remote: ParfaitRemoteDataSource,
    private val local: CanvasLocalDataSource,
    private val clock: Clock = Clock.System,
) {
    private val lock = Any()
    private val subscriberCounts = mutableMapOf<GroupId, Int>()
    private val pollJobs = mutableMapOf<GroupId, Job>()
    private val refreshing = mutableSetOf<GroupId>()

    /** [stopAll] 이 올린다. 그 전에 출발한 응답은 캐시에 싣지 않는다 */
    private var generation = 0

    fun acquire(groupId: GroupId) {
        val isFirst = synchronized(lock) {
            val next = (subscriberCounts[groupId] ?: 0) + 1
            subscriberCounts[groupId] = next
            next == 1
        }
        if (isFirst.not()) return

        restartPollTimer(groupId)
        scope.launch { refresh(groupId, forceToday = false) }
    }

    fun release(groupId: GroupId) {
        synchronized(lock) {
            val next = (subscriberCounts[groupId] ?: 0) - 1
            if (next <= 0) {
                subscriberCounts.remove(groupId)
                pollJobs.remove(groupId)?.cancel()
            } else {
                subscriberCounts[groupId] = next
            }
        }
    }

    /**
     * 쓰기 직후처럼 주기를 기다릴 수 없을 때 부른다. **주기도 이 시점부터 다시 센다** —
     * 그러지 않으면 강제 갱신 직후에 주기 타이머가 또 터져 요청이 붙어 나간다.
     *
     * 실패해도 주기를 다시 세운다. 실패한 갱신 때문에 다음 주기가 앞당겨질 이유가 없다.
     */
    suspend fun refreshNow(
        groupId: GroupId,
        forceToday: Boolean = false,
    ): Result<Unit> {
        val result = refresh(groupId, forceToday)
        val hasSubscriber = synchronized(lock) { subscriberCounts.containsKey(groupId) }
        if (hasSubscriber) restartPollTimer(groupId)
        return result
    }

    /**
     * 화면이 곧 사라지는 자리에서 부른다 — 호출자 스코프에서 기다리면 되감기가 늦어지거나
     * `viewModelScope` 취소로 요청이 끊긴다.
     */
    fun refreshNowAsync(
        groupId: GroupId,
        forceToday: Boolean = false,
    ) {
        scope.launch { refreshNow(groupId, forceToday) }
    }

    /** 세션이 끝날 때 부른다. 이미 출발한 응답이 캐시를 되살리지 못하게 세대를 올린다 */
    fun stopAll() {
        synchronized(lock) {
            generation++
            pollJobs.values.forEach(Job::cancel)
            pollJobs.clear()
            subscriberCounts.clear()
            refreshing.clear()
        }
    }

    private fun restartPollTimer(groupId: GroupId) {
        synchronized(lock) {
            pollJobs.remove(groupId)?.cancel()
            pollJobs[groupId] = scope.launch {
                while (isActive) {
                    delay(CANVAS_POLL_INTERVAL)
                    refresh(groupId, forceToday = false)
                }
            }
        }
    }

    /**
     * 오늘 조회는 캔버스가 없으면 서버가 만들어 저장한다(`api/parfait.md`) — 그래서 캔버스를
     * 만들 필요가 있을 때만 쓴다. 캐시가 비었거나 실린 날짜가 오늘이 아니면(하루 경계를 넘겼다)
     * 그 경우다. 나머지는 부작용 없는 상세 조회를 쓴다.
     */
    private suspend fun refresh(
        groupId: GroupId,
        forceToday: Boolean,
    ): Result<Unit> {
        val startedGeneration = synchronized(lock) {
            if (refreshing.add(groupId).not()) return Result.success(Unit)
            generation
        }

        try {
            val cached = local.cachedTodayCanvas(groupId)
            val cachedParfaitId = cached?.parfaitId
            val needsToday = forceToday || cachedParfaitId == null || cached.date != parfaitToday(clock)

            val result = if (needsToday || cachedParfaitId == null) {
                remote.getTodayCanvas(groupId)
            } else {
                remote.getCanvasDetail(groupId = groupId, parfaitId = cachedParfaitId)
            }

            return result
                .onSuccess { canvas ->
                    synchronized(lock) {
                        if (generation == startedGeneration) local.saveTodayCanvas(groupId, canvas)
                    }
                }.map { }
        } finally {
            synchronized(lock) { refreshing.remove(groupId) }
        }
    }
}
```

`needsToday || cachedParfaitId == null` 조건을 두 번 쓰는 것은 스마트캐스트 때문이다 — `cachedParfaitId`를 지역 `val`로 뽑아 두었으므로 `else` 가지에서 널이 아님이 확정된다.

- [x] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:test --tests "*CanvasPollerTest*" && ./gradlew :data:ktlintCheck`
Expected: PASS (9건)

- [x] **Step 5: 스펙의 as-built 차이를 지금 반영한다**

구현이 끝난 뒤로 미루지 않는다 — 이 갈림은 설계 결정이라 Task 5~8이 어느 쪽을 전제하는지 흐려진다. 문서 저장소에서 스펙 「하루 경계」와 「폴링을 어디에 두는가」를 고친다.

- 폴러는 경계 티커를 구독하지 않는다. **캐시에 실린 날짜가 오늘인지**로 오늘 조회 여부를 정한다. 경계를 넘기면 캐시의 날짜가 어제가 되므로 다음 주기가 저절로 오늘 조회를 고른다.
- 티커는 캔버스 메인의 날짜 갱신용으로만 남는다.
- `refreshTodayCanvasDetail`의 `parfaitId`는 호출부의 의도 표시이고 실제 대상은 캐시가 정한다.
- 폴러는 로컬 데이터소스가 아니라 **트리거 소유자**다(패키지만 `local/`을 공유한다).

- [x] **Step 6: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt
git commit -m "feat: 오늘 캔버스 폴러를 만든다"
```

문서 저장소는 따로 커밋한다.

---

### Task 4: 저장소·세션 정리·상세 갱신 UseCase를 잇는다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/parfait/ParfaitRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/network/TokenAuthenticator.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/network/TokenAuthenticatorTest.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/RefreshTodayParfaitDetailUseCase.kt`

**Interfaces:**
- Consumes: Task 3의 `CanvasPoller`
- Produces:
  - `suspend operator fun RefreshTodayParfaitDetailUseCase.invoke(groupId: GroupId, parfaitId: ParfaitId): Result<Unit>`
  - 저장소 표면은 PR2에서 정한 다섯 그대로

- [x] **Step 1: 구독에 폴러 계수를 매단다**

⚠️ **`onSubscription`이 아니라 `onStart`다.** `onSubscription`은 `SharedFlow`/`StateFlow` 확장이고, `CanvasLocalDataSource.todayCanvas`는 `map`·`distinctUntilChanged`를 거친 콜드 `Flow`라 컴파일되지 않는다. 업스트림이 `MutableStateFlow` 기반이라 현재 값이 항상 재생되므로 `onSubscription`의 보장도 필요 없다.

```kotlin
class ParfaitRepositoryImpl @Inject constructor(
    private val parfaitRemoteDataSource: ParfaitRemoteDataSource,
    private val canvasLocalDataSource: CanvasLocalDataSource,
    private val canvasPoller: CanvasPoller,
) : ParfaitRepository {
    /**
     * 구독이 붙어 있는 동안만 폴러가 돈다 — 화면이 보지 않는 캔버스를 계속 부르지 않는다.
     * 화면은 폴러의 존재를 모른다.
     */
    override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = canvasLocalDataSource
        .todayCanvas(groupId)
        .onStart { canvasPoller.acquire(groupId) }
        .onCompletion { canvasPoller.release(groupId) }

    /** 폴러를 지나므로 이 갱신도 주기를 다시 세운다 */
    override suspend fun refreshTodayCanvas(groupId: GroupId): Result<Unit> = canvasPoller
        .refreshNow(groupId, forceToday = true)
        .mapErrorToAppError()

    override suspend fun refreshTodayCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<Unit> = canvasPoller
        .refreshNow(groupId)
        .mapErrorToAppError()

    override fun clearTodayCanvas() {
        canvasPoller.stopAll()
        canvasLocalDataSource.clear()
    }
    // 나머지 함수는 그대로
}
```

`onStart`·`onCompletion` import를 더한다. `refreshTodayCanvasDetail`의 `parfaitId`는 이제 폴러가 캐시에서 정하므로, PR2가 그 KDoc에 적어 둔 대로 **호출부의 의도 표시**임을 유지한다.

- [x] **Step 2: 강제 로그아웃 경로도 폴링을 끊는다**

`LogoutUseCase`는 `:domain`이라 `clearTodayCanvas()`를 지나 Step 1의 `stopAll()`에 닿는다. **`TokenAuthenticator`는 `:data`라 로컬 데이터소스를 직접 부르므로 그 경로에 안 닿는다.**

`TokenAuthenticator` 생성자에 `CanvasPoller`를 주입하고, `canvasLocalDataSource.clear()` **앞에** `canvasPoller.stopAll()`을 넣는다 — 캐시를 지우기 전에 트리거를 끊어야 늦게 온 응답이 되살리지 못한다.

`TokenAuthenticatorTest`에 `stopAll()`이 `canvasLocalDataSource.clear()`보다 먼저 불리는 것을 `verifyOrder`로 고정한다.

- [x] **Step 3: 상세 갱신 UseCase를 만든다**

`:feature`는 `ParfaitRepository`를 직접 보지 않는다.

```kotlin
package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import javax.inject.Inject

/** 부작용 없는 상세 조회로 오늘 캔버스 캐시를 갱신한다. 쓰기 직후에 부른다 */
class RefreshTodayParfaitDetailUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<Unit> = parfaitRepository.refreshTodayCanvasDetail(groupId = groupId, parfaitId = parfaitId)
}
```

- [x] **Step 4: 컴파일·테스트·ktlint 확인**

Run: `./gradlew :data:test :domain:test :data:ktlintCheck :domain:ktlintCheck`
Expected: PASS

- [x] **Step 5: 커밋**

```bash
git add data/ domain/
git commit -m "feat: 오늘 캔버스 구독에 폴링 수명을 매단다"
```

---

### Task 5: 하루 경계 티커를 만든다

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/parfait/ObserveParfaitDayBoundaryUseCase.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/parfait/ObserveParfaitDayBoundaryUseCaseTest.kt`

**Interfaces:**
- Consumes: `parfaitToday`, `PARFAIT_TIME_ZONE`, `DayWindow.DAY_BOUNDARY_HOUR`(전부 `:domain`의 `com.teamyg.parfait.domain.model`)
- Produces: `operator fun invoke(clock: Clock = Clock.System): Flow<LocalDate>`

- [x] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun invoke_emitsTheCurrentParfaitDayFirst() = runTest {
        val clock = FixedClock(atKst(2026, 8, 27, hour = 10))

        ObserveParfaitDayBoundaryUseCase().invoke(clock).test {
            assertEquals(LocalDate(2026, 8, 27), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun invoke_crossingTheBoundary_emitsTheNewDay() = runTest {
        val clock = MutableClock(atKst(2026, 8, 28, hour = 2, minute = 59))

        ObserveParfaitDayBoundaryUseCase().invoke(clock).test {
            // 새벽 3시 전이라 아직 27일이다
            assertEquals(LocalDate(2026, 8, 27), awaitItem())

            clock.advanceBy(2.minutes)
            advanceTimeBy(2.minutes)

            assertEquals(LocalDate(2026, 8, 28), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
```

`FixedClock`·`MutableClock`은 `kotlin.time.Clock`을 구현하는 테스트 헬퍼다. 저장소에 이미 있으면 그것을 쓰고, 없으면 이 파일 안에 `private class`로 둔다. `atKst(...)`는 `LocalDateTime(...).toInstant(PARFAIT_TIME_ZONE)` 한 줄이다.

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :domain:test --tests "*ObserveParfaitDayBoundaryUseCaseTest*"`
Expected: 컴파일 실패

- [x] **Step 3: 구현을 쓴다**

이 저장소는 `kotlin.time.Clock`만 쓴다(`kotlinx.datetime.Clock` 사용처 0건). 섞지 않는다.

```kotlin
package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.DayWindow
import com.teamyg.parfait.domain.model.PARFAIT_TIME_ZONE
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * 파르페 기준의 오늘을 내고, 하루 경계(새벽 3시)를 넘길 때마다 새 날짜를 다시 낸다.
 *
 * 값 스트림에 필터를 다는 방식으로는 이 판정을 못 한다 — 캔버스 캐시가 조용하면 재방출이 없어
 * 필터가 아예 평가되지 않는다(`specs/2026-08-27-canvas-today-ssot-polling.md` 「하루 경계」).
 */
class ObserveParfaitDayBoundaryUseCase
@Inject
constructor() {
    /** @param clock 경계 판정과 대기 시간 계산에 쓴다. 테스트에서 경계 앞뒤를 고정한다 */
    operator fun invoke(clock: Clock = Clock.System): Flow<LocalDate> = flow {
        while (true) {
            emit(parfaitToday(clock))
            delay(clock.durationUntilNextBoundary())
        }
    }.distinctUntilChanged()
}

/** 반올림으로 경계 직전에 깨어나면 같은 날짜를 다시 낼 수 있어 위에서 걸러 준다 */
private fun Clock.durationUntilNextBoundary(): Duration {
    val now = now()
    val nowDateTime = now.toLocalDateTime(PARFAIT_TIME_ZONE)
    val boundaryTime = LocalTime(DayWindow.DAY_BOUNDARY_HOUR, 0)

    val nextBoundaryDate = if (nowDateTime.time < boundaryTime) {
        nowDateTime.date
    } else {
        nowDateTime.date.plus(1, DateTimeUnit.DAY)
    }

    return nextBoundaryDate.atTime(boundaryTime).toInstant(PARFAIT_TIME_ZONE) - now
}
```

- [x] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :domain:test :domain:ktlintCheck`
Expected: PASS

- [x] **Step 5: 커밋**

```bash
git add domain/
git commit -m "feat: 파르페 하루 경계 티커를 만든다"
```

---

### Task 6: 캔버스 메인을 구독 헬퍼로 옮기고 경계·스포트라이트를 잇는다

**Files:**
- Modify: `feature/.../viewmodel/CanvasMainViewModel.kt`
- Modify: `feature/.../viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `launchWhileSubscribed`, Task 5의 `ObserveParfaitDayBoundaryUseCase`
- Produces: 없음

- [x] **Step 1: 실패하는 테스트를 쓴다**

`launchWhileSubscribed`를 쓰므로 **테스트가 `viewModel.state`를 실제로 구독해야** 업스트림이 열린다. 매 테스트에서 `backgroundScope.launch { viewModel.state.collect { } }`를 먼저 붙인다.

```kotlin
    @Test
    fun observeTodayCanvas_whenTheSpotlightedToppingDisappears_resetsTheSpotlight() =
        runTest(mainDispatcherRule.dispatcher) {
            todayCanvases.value = canvasWith(otherTopping(OTHER_IMAGE_ID))
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.state.collect { } }
            advanceUntilIdle()

            viewModel.processIntent(CanvasMainIntent.OnClickTopping(otherTopping(OTHER_IMAGE_ID)))
            advanceUntilIdle()
            assertEquals(ParfaitImageId(OTHER_IMAGE_ID), viewModel.state.value.spotlightedToppingId)

            todayCanvases.value = canvasWith()
            advanceUntilIdle()

            assertNull(viewModel.state.value.spotlightedToppingId)
        }

    @Test
    fun observeTodayCanvas_whenTheSpotlightedToppingRemains_keepsTheSpotlight() =
        runTest(mainDispatcherRule.dispatcher) {
            todayCanvases.value = canvasWith(otherTopping(OTHER_IMAGE_ID))
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.state.collect { } }
            advanceUntilIdle()

            viewModel.processIntent(CanvasMainIntent.OnClickTopping(otherTopping(OTHER_IMAGE_ID)))
            advanceUntilIdle()

            // 남이 토핑을 하나 더 올려도 강조는 그대로다
            todayCanvases.value = canvasWith(otherTopping(OTHER_IMAGE_ID), otherTopping(THIRD_IMAGE_ID))
            advanceUntilIdle()

            assertEquals(ParfaitImageId(OTHER_IMAGE_ID), viewModel.state.value.spotlightedToppingId)
        }

    @Test
    fun dayBoundary_movesTodayAndTheSelectedDate() = runTest(mainDispatcherRule.dispatcher) {
        val days = MutableStateFlow(LocalDate(2026, 8, 27))
        every { observeParfaitDayBoundary(any()) } returns days

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        days.value = LocalDate(2026, 8, 28)
        advanceUntilIdle()

        assertEquals(LocalDate(2026, 8, 28), viewModel.state.value.today)
        assertEquals(LocalDate(2026, 8, 28), viewModel.state.value.selectedDate)
    }

    @Test
    fun enter_doesNotRefreshTheTodayCanvas() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        viewModel.processIntent(CanvasMainIntent.Enter)
        advanceUntilIdle()

        coVerify(exactly = 0) { refreshTodayParfait(any(), any()) }
    }
```

PR2가 더한 `enter_...` 테스트 중 갱신 호출을 단언하던 것은 이 단계에서 반대로 뒤집힌다 — 함께 고친다.

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasMainViewModelTest*"`
Expected: 컴파일 실패

- [x] **Step 3: 지난 날 게이트를 `state` 밖에 둔다**

⚠️ **`launchWhileSubscribed`의 `source` 안에서 `state`를 수집하면 안 된다.** 활성 조건이 `state`의 구독자 수라, 열린 업스트림 자신이 구독자로 세어져 계수가 0으로 내려가지 않고 폴링이 영영 안 멎는다.

```kotlin
    /**
     * 지난 날을 보는 동안 구독을 끊기 위한 게이트.
     *
     * [state] 를 읽지 않는 이유는 [launchWhileSubscribed] KDoc 에 있다 — 그 안에서 [state] 를
     * 수집하면 그 수집 자체가 구독자로 세어진다.
     */
    private val isViewingToday = MutableStateFlow(true)
```

`selectedDate`·`today`를 바꾸는 자리 전부에서 이 값을 함께 갱신한다 — 달력 날짜 선택(`handleClickDate`), "오늘로 가기"(`handleClickGoToToday`), 경계 티커 수집부. 갱신은 `isViewingToday.value = (selectedDate == today)` 한 줄이다.

- [x] **Step 4: 구독을 헬퍼로 옮기고 스포트라이트 해제를 넣는다**

```kotlin
    private fun observeTodayCanvas() {
        launchWhileSubscribed(
            source = {
                isViewingToday
                    .distinctUntilChanged()
                    .flatMapLatest { viewingToday ->
                        // 마감된 날은 바뀌지 않으므로 오늘 캔버스를 계속 부를 이유가 없다.
                        // 마지막 값은 그대로 둔다 — 비우면 오늘로 돌아올 때 빈 캔버스가 깜빡인다
                        if (viewingToday) getTodayParfaitFlowUseCase(groupId) else emptyFlow()
                    }
            },
        ) { canvas ->
            updateState {
                copy(
                    todayCanvas = canvas,
                    memberChips = canvas?.members?.toMemberChips() ?: memberChips,
                    // 강조 중이던 토핑이 사라지면 딤도 사라져 Dim 탭이라는 해제 계기가 없어진다.
                    // 상태값을 그대로 두면 handleOnClickTopping 가드에 걸려 탭이 전부 먹지 않는다
                    spotlightedToppingId = spotlightedToppingId?.takeIf { id ->
                        canvas?.toppings.orEmpty().any { it.parfaitImageId == id }
                    },
                )
            }
        }
    }
```

파일에 `@OptIn(ExperimentalCoroutinesApi::class)`를 붙인다(`flatMapLatest`).

- [x] **Step 5: 경계 티커를 구독한다**

```kotlin
    /**
     * 화면을 열어 둔 채 파르페 하루 경계를 넘기면 오늘을 다시 센다. 진입 시점만으로는 부족하다 —
     * 그 사이 폴링이 받아 온 오늘 캔버스가 어제 날짜 헤더 아래 그려진다.
     */
    private fun observeDayBoundary() {
        launchWhileSubscribed(source = { observeParfaitDayBoundaryUseCase() }) { today ->
            updateState {
                if (today == this.today) return@updateState this

                if (this.isViewingToday) {
                    copy(today = today, selectedDate = today, displayedMonth = today.toFirstDayOfMonth())
                } else {
                    copy(today = today)
                }
            }
            isViewingToday.value = state.value.selectedDate == state.value.today
        }
    }
```

`syncToday()`를 지운다. 그러면 `handleClickGoToToday`의 KDoc이 `[syncToday]`를 가리키는 깨진 링크가 되므로, 그 문장을 **폴러 재구독이 그 자리를 맡는다**는 서술로 다시 쓴다.

- [x] **Step 6: 진입 갱신을 걷어낸다**

폴러의 구독 시작 즉시 조회가 그 역할을 한다. 남겨 두면 재진입마다 부작용 있는 오늘 조회가 한 번 더 나간다.

```kotlin
    /**
     * 화면이 앞에 섰다. 오늘 캔버스 갱신은 폴러가 구독 시작에서 이미 한다
     * (`adr/0029-canvas-today-ssot-polling.md`).
     *
     * 달력 기록은 폴링 대상이 아니라 여기서 받는다 — 다른 멤버가 오늘 토핑을 올리면 오늘 칸의
     * 점이 생기는데, 연 단위 캐시는 그것을 스스로 알 방법이 없다. 바뀔 수 있는 해는 올해뿐이다.
     */
    private fun handleEnter() {
        if (state.value.isViewingToday.not()) return
        // 해가 바뀐 직후를 대비해 상태가 아니라 시계에서 읽는다
        loadParfaitHistories(parfaitToday().year)
    }
```

`loadTodayCanvas()`를 부르는 곳이 `handleClickGoToToday` 하나만 남으면 그것도 지우고, 오늘로 돌아가는 것을 `isViewingToday.value = true`로 대신한다 — 구독이 다시 열리며 폴러가 즉시 조회한다.

- [x] **Step 7: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasMainViewModelTest*"`
Expected: PASS

- [x] **Step 8: 커밋**

```bash
git add feature/groups/canvas/impl/
git commit -m "fix: 강조된 토핑이 사라지면 스포트라이트를 푼다"
```

---

### Task 7: 배경 편집의 병합 규칙을 세운다

**Files:**
- Modify: `feature/.../viewmodel/CanvasBGEditViewModel.kt`
- Modify: `feature/.../viewmodel/CanvasBGEditViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `launchWhileSubscribed`, Task 4의 `RefreshTodayParfaitDetailUseCase`
- Produces:
  - `CanvasBGEditUiState.dirtyToppingIds: Set<Long>`
  - `CanvasBGEditUiState.deletedToppingIds: Set<Long>`
  - ViewModel의 `private var confirmedToppings` 제거

- [x] **Step 1: 실패하는 테스트 여섯을 쓴다**

전부 `backgroundScope.launch { viewModel.state.collect { } }`를 먼저 붙인다. `canvasWith(..., stamp)`는 같은 토핑 목록이라도 `CanvasVO`가 달라 보이게 하는 헬퍼다(`distinctUntilChanged`가 방출을 삼키지 않게 한다) — `lastClosedDate` 같은 무해한 필드를 달리해 만든다.

```kotlin
    @Test
    fun merge_dirtyTopping_isNotOverwritten() = runTest(mainDispatcherRule.dispatcher) {
        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID, positionX = 0.1))
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        viewModel.processIntent(CanvasBGEditIntent.OnClickTopping(viewModel.state.value.toppings.first()))
        viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.2f, deltaY = 0f))
        advanceUntilIdle()
        val moved = viewModel.state.value.toppings.first().positionX

        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID, positionX = 0.9))
        advanceUntilIdle()

        assertEquals(moved, viewModel.state.value.toppings.first().positionX)
    }

    @Test
    fun merge_cleanTopping_takesTheServerValue() = runTest(mainDispatcherRule.dispatcher) {
        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID, positionX = 0.1))
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID, positionX = 0.9))
        advanceUntilIdle()

        assertEquals(0.9f, viewModel.state.value.toppings.first().positionX)
    }

    @Test
    fun merge_newToppingFromAnotherMember_appears() = runTest(mainDispatcherRule.dispatcher) {
        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID))
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.toppings.size)

        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID), otherTopping(OTHER_IMAGE_ID))
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.toppings.size)
    }

    @Test
    fun merge_deletedTopping_doesNotComeBack() = runTest(mainDispatcherRule.dispatcher) {
        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID))
        coEvery { deleteTopping(any(), any(), any()) } returns Result.success(Unit)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        viewModel.processIntent(CanvasBGEditIntent.OnClickTopping(viewModel.state.value.toppings.first()))
        viewModel.processIntent(CanvasBGEditIntent.OnClickDeleteToppingButton)
        viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.toppings.isEmpty())

        // 삭제 직전에 출발한 응답이 뒤늦게 도착한다
        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID), stamp = 2)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.toppings.isEmpty())
    }

    @Test
    fun merge_whenTheServerDropsIt_clearsTheTombstone() = runTest(mainDispatcherRule.dispatcher) {
        // 위 흐름에 이어 서버가 그 토핑을 뺀 응답을 주면 툼스톤이 빈다
        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID))
        coEvery { deleteTopping(any(), any(), any()) } returns Result.success(Unit)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        viewModel.processIntent(CanvasBGEditIntent.OnClickTopping(viewModel.state.value.toppings.first()))
        viewModel.processIntent(CanvasBGEditIntent.OnClickDeleteToppingButton)
        viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()

        todayCanvases.value = canvasWith()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.deletedToppingIds.isEmpty())
    }

    @Test
    fun confirm_patchesOnlyDirtyToppings() = runTest(mainDispatcherRule.dispatcher) {
        todayCanvases.value = canvasWith(myTopping(MY_IMAGE_ID), myTopping(SECOND_IMAGE_ID))
        coEvery { updateTopping(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            Result.success(updatedTopping())
        coEvery { changeCanvasBackground(any(), any(), any()) } returns Result.success(null)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        viewModel.processIntent(
            CanvasBGEditIntent.OnClickTopping(
                viewModel.state.value.toppings.first { it.parfaitImageId == MY_IMAGE_ID },
            ),
        )
        viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.2f, deltaY = 0f))
        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            updateTopping(any(), any(), ParfaitImageId(MY_IMAGE_ID), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            updateTopping(any(), any(), ParfaitImageId(SECOND_IMAGE_ID), any(), any(), any(), any(), any())
        }
    }
```

`UpdateToppingUseCase.invoke`는 파라미터가 여덟이다(`groupId`·`parfaitId`·`parfaitImageId`·`positionX`·`positionY`·`positionZ`·`scale`·`rotation`, 뒤 다섯은 기본값 `null`). MockK는 전 인자 매처를 요구하므로 위처럼 여덟을 다 적는다.

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasBGEditViewModelTest*"`
Expected: 컴파일 실패

- [x] **Step 3: UiState에 두 집합을 넣는다**

```kotlin
    /**
     * 아직 서버에 반영되지 않은 로컬 변경. 이동·크기조절·회전·테두리 편집이 여기 든다.
     *
     * **삭제는 넣지 않는다** — 삭제 모달의 확인이 곧 DELETE 라 이미 서버에 반영돼 있고,
     * 확인 버튼은 삭제를 다루지 않는다.
     *
     * 같은 화면의 [selectedToppingId] 가 `Long` 이라 그것에 맞춘다. `ParfaitImageId` 로 감싸는
     * 자리는 지금처럼 API 호출 직전 한 곳뿐이다.
     */
    val dirtyToppingIds: Set<Long> = emptySet(),
    /**
     * 지운 토핑의 툼스톤. 삭제 직전에 출발한 갱신 응답이 뒤늦게 도착하면 그 토핑이 아직 서버
     * 목록에 있어서, 이게 없으면 방금 지운 토핑이 되살아난다.
     */
    val deletedToppingIds: Set<Long> = emptySet(),
```

ViewModel의 `private var confirmedToppings`와 그것을 갱신·조회하던 자리를 지운다. **UiState 필드가 아니라 ViewModel의 스냅샷 필드다** — 스펙이 UiState라고 적은 것은 오기이고 Task 9에서 함께 고친다.

🔁 **as-built(2026-08-28 스택 리베이스)** — 지우기는 지웠으나 **같은 자리에 `serverToppings`가 들어왔다.** 확인 시 축별 판정이 필요해서다(아래 Step 7 각주). 갱신 시점은 같다 — 구독이 방출할 때마다 서버 목록을 그대로 담는다.

- [x] **Step 4: 병합 함수를 쓴다**

```kotlin
    /**
     * 구독 방출을 받을 때마다 돈다. 최초 방출도 예외가 아니다 — 그때는 두 집합이 비어 있어
     * 결과가 통째 대입과 같아진다. 화면은 그 방출이 폴링에서 왔는지 강제 갱신에서 왔는지
     * 구분하지 않는다.
     */
    private fun CanvasBGEditUiState.mergeToppings(incoming: List<CanvasToppingItem>): CanvasBGEditUiState {
        val incomingIds = incoming.mapTo(mutableSetOf()) { it.parfaitImageId }
        val localById = toppings.associateBy { it.parfaitImageId }

        val merged = incoming
            .filterNot { it.parfaitImageId in deletedToppingIds }
            .map { server ->
                if (server.parfaitImageId in dirtyToppingIds) localById[server.parfaitImageId] ?: server else server
            }

        return copy(
            toppings = merged,
            // 서버 목록에서 사라진 것은 두 집합에서도 뺀다 — 없는 토핑에 PATCH 를 보낼 수 없고,
            // 툼스톤도 제 역할을 다했다
            dirtyToppingIds = dirtyToppingIds intersect incomingIds,
            deletedToppingIds = deletedToppingIds intersect incomingIds,
            selectedToppingId = selectedToppingId?.takeIf { it in incomingIds && it !in deletedToppingIds },
        )
    }
```

- [x] **Step 5: 구독을 헬퍼로 옮기고 진입 갱신을 걷어낸다**

`withCanvas`에서 **`toppings` 파라미터와 `copy(toppings = …)`를 제거해** 배경 시딩만 남긴다(PR2는 토핑 대입을 갖고 있다). 토핑은 `mergeToppings`가 단독으로 맡는다.

```kotlin
    private fun observeCanvas() {
        launchWhileSubscribed(source = { getTodayParfaitFlowUseCase(groupId) }) { canvas ->
            if (canvas == null) return@launchWhileSubscribed

            if (hasSeededFromCanvas.not() && canvas.parfaitId != parfaitId) {
                viewModelLogger.e {
                    "편집을 연 캔버스와 조회 결과가 다르다 — 조회 쪽으로 옮긴다" +
                        " (열린 것: ${parfaitId.value}, 받은 것: ${canvas.parfaitId.value})"
                }
                parfaitId = canvas.parfaitId
            }

            val incoming = canvas.toppings
                .sortedBy { topping -> topping.transform.positionZ }
                .map { topping -> topping.toToppingItem() }

            updateState { withCanvas(canvas).mergeToppings(incoming) }
            hasSeededFromCanvas = true
        }
    }
```

PR2가 `init`에 둔 `refreshCanvas()` 호출을 **지운다.** 폴러의 구독 시작 즉시 조회가 그 역할을 하고, 남겨 두면 배경 편집을 열 때마다 부작용 있는 오늘 조회가 한 번 더 나간다. 그 실패에 달려 있던 `ShowError` 이펙트도 함께 사라진다 — 폴링 실패를 화면에 표현하지 않는다는 스펙 결정과 일관되므로 **의도적으로 없애는 것**이다.

- [x] **Step 6: 각 조작이 집합을 채우게 한다**

이동·크기조절·회전·테두리 편집 결과 처리에서 대상 id를 `dirtyToppingIds`에 넣는다.

```kotlin
    private fun CanvasBGEditUiState.markDirty(toppingId: Long): CanvasBGEditUiState =
        copy(dirtyToppingIds = dirtyToppingIds + toppingId)
```

테두리 재편집도 넣는다. 그 토핑의 로컬 값이 갱신에 덮이면 안 되는 것은 같다. 이 판단을 `OnToppingEditResult` 처리 자리에 한 줄로 남긴다.

⚠️ **이 계획이 쓰인 다음 날 테두리 PATCH에 소비처가 생겼다**(2026-08-27, PR #369 develop 머지 — OQ-P-276 ①③ 해소). 아래 Step 7의 `updateDirtyToppings`를 **적힌 그대로 구현하면 그 저장이 되돌아간다** → OQ-P-326 ①.

삭제 성공 처리를 고친다. 그 스코프의 지역변수 이름은 **`selectedId`**이고, `showDeleteToppingDialog = false`는 `launch` 앞에서 이미 처리되므로 다시 쓰지 않는다.

```kotlin
                .onSuccess {
                    updateState {
                        copy(
                            toppings = toppings.filterNot { it.parfaitImageId == selectedId },
                            deletedToppingIds = deletedToppingIds + selectedId,
                            dirtyToppingIds = dirtyToppingIds - selectedId,
                            selectedToppingId = null,
                        )
                    }
                    refreshTodayParfaitDetailUseCase(groupId = groupId, parfaitId = parfaitId)
                }
```

이 자리는 화면이 남아 있으므로 호출자 코루틴에서 기다려도 된다.

- [x] **Step 7: 확인의 PATCH 대상을 집합으로 바꾼다**

`async`는 `CoroutineScope` 리시버 없이 부를 수 없다(coroutines 1.11.0에서 오류 수준 deprecation). 기존 `updateToppingIfChanged`는 `launch(key = CONFIRM_KEY) { … }` 블록 안에서 불려 리시버가 있었지만, 새 함수에는 없으므로 `coroutineScope`로 감싼다.

⚠️ **아래 코드 블록은 develop보다 낡았다**(2026-08-28 문서 점검). PR #369 이후 `updateToppingIfChanged`는 위치(`updateToppingUseCase`)와 테두리(`updateToppingBorderUseCase`)를 **독립적으로 판정하고 독립적으로 보낸다.** 집합으로 옮길 때 두 갈래를 함께 옮겨야 한다 — 테두리는 집합에 든 토핑에 대해 `topping.borderLayers.toToppingBorder()`를 그대로 보내면 되고, 스냅샷 대조가 사라지므로 "테두리가 바뀌었는가"를 따로 볼 필요가 없어진다.

🔁 **위 문단의 마지막 절이 틀렸다 — as-built(2026-08-28 스택 리베이스).** "따로 볼 필요가 없어진다"고 적었지만, 축별 판정은 **없애면 안 되는 것**이었다. 집합은 어느 축을 만져서 dirty 가 됐는지 기억하지 않으므로, 판정을 빼면 위치만 옮긴 토핑에도 테두리 PATCH 가 따라 나간다. develop 의 `onClickConfirm_toppingBorderEdited_savesOnlyTheBorder` 가 그 반대 방향("테두리만 바꾸면 위치 PATCH 는 안 나간다")을 잠그고 있어 실제로 그 테스트가 깨졌다.

그래서 스냅샷은 이름을 바꿔 남았다 — `confirmedToppings` 대신 `serverToppings`(서버가 마지막으로 준 그대로)를 들고, **집합이 이미 고른 토핑 안에서만** 축을 가린다. 이 계획이 스냅샷을 버리려던 이유("목록 전체를 견주면 남의 새 토핑이 스냅샷에 없음 = 바뀜으로 잡힌다")는 집합 필터가 앞에서 막으므로 되살아나지 않는다. 실제로 들어간 형태는 다음과 같다.

```kotlin
    private suspend fun updateDirtyToppings() = coroutineScope {
        val current = state.value
        current.toppings
            .filter { it.parfaitImageId in current.dirtyToppingIds }
            .map { topping -> async { updateToppingIfChanged(topping) } }
            .awaitAll()
    }
```

`updateToppingIfChanged` 자체는 PR #369 의 것을 그대로 쓰되, 대조 대상만 `confirmedToppings` 에서 `serverToppings` 로 바뀐다.

```kotlin
    /**
     * PATCH 대상은 지금 목록에 있으면서 손댄 토핑뿐이다. 스냅샷 대조를 쓰면 갱신이 들여온
     * 남의 새 토핑이 "스냅샷에 없음 = 바뀜"으로 잡혀 남의 토핑에 PATCH 를 쏜다.
     */
    private suspend fun updateDirtyToppings() = coroutineScope {
        val current = state.value
        current.toppings
            .filter { it.parfaitImageId in current.dirtyToppingIds }
            .map { topping ->
                async {
                    updateToppingUseCase(
                        groupId = groupId,
                        parfaitId = parfaitId,
                        parfaitImageId = ParfaitImageId(topping.parfaitImageId),
                        positionX = topping.positionX.toDouble(),
                        positionY = topping.positionY.toDouble(),
                        scale = topping.scale.toDouble(),
                        rotation = topping.rotationDegrees.toDouble(),
                    ).onFailure { throwable ->
                        viewModelLogger.e(throwable) { "토핑을 저장하지 못했다 - ${topping.parfaitImageId}" }
                    }
                }
            }.awaitAll()
    }
```

**성공·실패를 가리지 않고 집합을 비운다.** 화면이 이미 되감긴 뒤라 실패분을 되살릴 자리가 없다(현 as-built의 "토핑 저장 실패는 화면에 닿지 않는다"를 승계하며 그 공백은 OQ-P-276 소관이다). 확인 처리 끝에서 `updateState { copy(dirtyToppingIds = emptySet()) }`를 한 번 부른다.

🔁 **as-built(2026-08-28) — 이 지시대로 구현하지 않았다.** 같은 브랜치에서 **확인이 하나라도 실패하면 화면을 닫지 않게** 바뀌어, 이 문단이 근거로 삼은 "되감긴 뒤라 되살릴 자리가 없다"가 거짓이 됐다. `updateDirtyToppings()`가 **실패한 토핑 id 집합을 반환**하고 그것을 그대로 대입한다(`copy(dirtyToppingIds = failedToppingIds)`) — 통째로 비우면 다시 누른 확인이 못 보낸 토핑을 건너뛴다. 스펙 「배경 편집 화면의 진행·실패 표현」 절 참고.

- [x] **Step 8: 배경 저장 성공 뒤에도 강제 갱신을 건다**

스펙은 **배경 저장·토핑 추가·토핑 삭제 셋 모두**에 강제 갱신을 요구한다. 배경 저장은 성공 직후 `ConfirmBackground` 이펙트를 쏘고 라우트가 되감으므로, 거기서 `launch`한 갱신은 `viewModelScope`가 취소되며 끊긴다.

`changeCanvasBackgroundUseCase(...).onSuccess` 안, **`postSideEffect(ConfirmBackground)` 앞에** 갱신을 건다. 폴러 스코프에서 도는 비동기 표면을 쓰면 되감기를 늦추지 않으면서 갱신이 끝까지 간다 — Task 4의 `RefreshTodayParfaitDetailUseCase`가 그것을 감싸도록 non-suspend 표면(`CanvasPoller.refreshNowAsync`)을 부르는 별도 UseCase를 하나 더 두거나, 이 자리에서만 `suspend` 호출을 이펙트보다 앞에 두고 기다린다. **후자를 택한다** — 배경 저장은 이미 네트워크 왕복을 한 뒤라 한 번 더 기다리는 체감이 크지 않고, 표면이 늘지 않는다.

- [x] **Step 9: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasBGEditViewModelTest*"`
Expected: PASS

- [x] **Step 10: 커밋**

```bash
git add feature/groups/canvas/impl/
git commit -m "feat: 배경 편집이 편집 중인 배치를 갱신에서 지킨다"
```

---

### Task 8: 토핑 배치의 `positionZ`를 확정 시점에 다시 센다

**Files:**
- Modify: `feature/.../viewmodel/CanvasToppingPlaceViewModel.kt`
- Modify: `feature/.../viewmodel/CanvasToppingPlaceViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `launchWhileSubscribed`, Task 4의 `RefreshTodayParfaitDetailUseCase`
- Produces: `CanvasToppingPlaceUiState.observedParfaitId: ParfaitId?`

- [x] **Step 1: 실패하는 테스트 셋을 쓴다**

`AddToppingUseCase.invoke`는 파라미터가 다섯이다(`groupId`·`parfaitId`·`filePath`·`transform`·`border`). MockK 매처를 다섯 다 적는다.

```kotlin
    @Test
    fun confirm_sameCanvas_recomputesTheDepthFromTheSubscription() = runTest(mainDispatcherRule.dispatcher) {
        // 초안 nextPositionZ = 3, 구독 캔버스(초안과 같은 parfaitId)에 z = 1..5 인 토핑
        drafts.value = draft(nextPositionZ = 3, parfaitId = PARFAIT_ID)
        todayCanvases.value = canvasWith(parfaitId = PARFAIT_ID, maxPositionZ = 5)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        measureAndReady(viewModel)

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        coVerify { addTopping(any(), any(), any(), match { it.positionZ == 6 }, any()) }
    }

    @Test
    fun confirm_differentCanvas_fallsBackToTheDraft() = runTest(mainDispatcherRule.dispatcher) {
        drafts.value = draft(nextPositionZ = 3, parfaitId = PARFAIT_ID)
        todayCanvases.value = canvasWith(parfaitId = OTHER_PARFAIT_ID, maxPositionZ = 5)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        measureAndReady(viewModel)

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        coVerify { addTopping(any(), any(), any(), match { it.positionZ == 3 }, any()) }
    }

    @Test
    fun confirm_withoutACanvas_fallsBackToTheDraft() = runTest(mainDispatcherRule.dispatcher) {
        drafts.value = draft(nextPositionZ = 3, parfaitId = PARFAIT_ID)
        todayCanvases.value = null
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        measureAndReady(viewModel)

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        coVerify { addTopping(any(), any(), any(), match { it.positionZ == 3 }, any()) }
    }
```

`measureAndReady(viewModel)`는 `OnCanvasMeasured`·`OnToppingBaseSizeMeasured`·`OnToppingImageReadyChanged(true)`를 순서대로 넣는 헬퍼다 — 기존 확인 테스트가 쓰는 방식을 그대로 뽑아 쓴다.

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test --tests "*CanvasToppingPlaceViewModelTest*"`
Expected: FAIL

- [x] **Step 3: 구독 캔버스의 id를 상태에 들이고 재계산을 넣는다**

`CanvasToppingPlaceUiState`의 기존 `parfaitId`는 **초안이 못 박은 값**이다. 그 KDoc("흐름 진입 때 초안에 못 박힌 캔버스다")을 그 필드에 붙어 있게 유지하고, 옆에 구독 값을 담는 필드를 새로 둔다.

```kotlin
    /** 구독 중인 오늘 캔버스의 id. 하루 경계를 넘기면 초안의 [parfaitId] 와 다를 수 있다 */
    val observedParfaitId: ParfaitId? = null,
```

`withCanvas`에서 함께 채운다.

```kotlin
    /**
     * 흐름 진입 때 초안에 못 박은 값은 카메라·누끼를 거치는 사이 남이 토핑을 올리면 낡는다.
     * 그래서 확정 시점에 다시 센다.
     *
     * **초안과 같은 캔버스일 때만이다** — 구독 값은 "오늘"로 걸러진 캔버스라 하루 경계를 넘기면
     * 초안이 가리키는 캔버스와 다를 수 있고, 그때 재계산한 값을 초안의 캔버스에 실으면 사용자가
     * 들어간 캔버스가 아닌 곳의 깊이를 쓰게 된다(`adr/0026-topping-draft-datastore-ssot.md`).
     *
     * 겹침의 해결이 아니라 완화다 — 폴링 주기 안에 두 사람이 확인을 누르면 여전히 겹친다
     * (OQ-P-322).
     */
    private fun CanvasToppingPlaceUiState.resolvedPositionZ(): Int? {
        if (observedParfaitId == null || observedParfaitId != parfaitId) return nextPositionZ
        return (existingToppings.maxOfOrNull { it.transform.positionZ } ?: 0) + 1
    }
```

`handleOnClickConfirm`에서 **초안 결손 가드는 `nextPositionZ`로 그대로 두고**, `toToppingTransform(positionZ = …)`에 넘기는 값만 `current.resolvedPositionZ()`로 바꾼다. 가드까지 바꾸면 판정 대상이 달라진다.

- [x] **Step 4: 구독을 헬퍼로 옮기고 진입 갱신을 걷어낸다**

`observeCanvasOnce`의 `launch`를 `launchWhileSubscribed`로 바꾸고, PR2가 그 안에 둔 `refreshTodayParfaitUseCase` 호출을 **지운다** — 폴러의 구독 시작 즉시 조회가 대신한다. 이 화면은 오늘 캔버스를 만들 필요가 없으므로 오늘 조회를 부를 이유도 없다.

- [x] **Step 5: 추가 성공 뒤 강제 갱신을 넣는다**

`handleOnClickConfirm`의 `onSuccess` 블록에는 이미 순서 제약이 둘 박혀 있다 — `addRecentImageUseCase`는 알림보다 먼저 남기고(되감기며 `viewModelScope`가 취소된다), `toppingDraftRepository.clear()`는 되감기를 먼저 알린다.

여기서는 **기다리면 안 된다.** 강제 갱신을 `suspend`로 기다리면 확인 버튼을 누른 뒤 네트워크 왕복만큼 화면이 멈춘 것처럼 보이고, `PlaceSucceeded` 뒤에 두면 취소돼 아예 안 돈다.

`RefreshTodayParfaitDetailUseCase` 대신 폴러의 비동기 표면을 감싸는 **non-suspend UseCase**를 하나 더 두고, `PlaceSucceeded` **앞에** 부른다. 즉시 반환하므로 되감기를 늦추지 않고, 갱신은 폴러 스코프에서 끝까지 간다.

```kotlin
// domain/usecase/parfait/RequestTodayParfaitRefreshUseCase.kt
/** 화면이 곧 사라지는 자리에서 부른다 — 결과를 기다리지 않고 저장소 층이 마저 끝낸다 */
class RequestTodayParfaitRefreshUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    operator fun invoke(groupId: GroupId) = parfaitRepository.requestTodayCanvasRefresh(groupId)
}
```

`ParfaitRepository`에 `fun requestTodayCanvasRefresh(groupId: GroupId)`를 더하고 구현이 `canvasPoller.refreshNowAsync(groupId)`로 위임한다. 이 표면 추가도 Task 9에서 스펙에 반영한다.

- [x] **Step 6: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:test`
Expected: PASS

- [x] **Step 7: 커밋**

```bash
git add feature/groups/canvas/impl/ domain/ data/
git commit -m "fix: 토핑 깊이를 확정 시점에 다시 센다"
```

---

### Task 9: 전체 검증과 문서 반영

- [x] **Step 1: 전체 컴파일**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 2: 전체 유닛 테스트**

Run: `./gradlew test`
Expected: PASS. `testDebugUnitTest`를 쓰지 않는다 — `:domain`을 건너뛴다.

- [x] **Step 3: ktlint**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

- [x] **Step 4: 남은 스펙 차이를 반영한다**

Task 3 Step 5에서 하루 경계 판정은 이미 고쳤다. 여기서는 나머지를 고친다.

- `confirmedToppings`는 `CanvasBGEditUiState`의 필드가 아니라 ViewModel의 `private var`다. 스펙 「배경 편집 화면의 병합」의 "UiState에서 제거한다 — 렌더링에 쓰이지 않던 필드라"를 그렇게 고친다.
- `ParfaitRepository` 표면이 스펙의 넷에서 **일곱**이 됐다 — `cachedTodayCanvasDate`(PR2)·`requestTodayCanvasRefresh`(Task 8)가 늘었다. 각각의 근거를 스펙에 적는다.
- 「폴링 수명을 무엇에 매다는가」의 `onSubscription`을 `onStart`로 고친다.
- 「검증」에 in-flight 스킵과 정리 후 응답 무시 두 항목을 더한다.
- 진입 갱신 제거가 세 화면 모두에 적용된다는 것을 「폴링을 어디에 두는가」에 적는다.

- [x] **Step 5: ADR 상태를 올린다**

머지된 뒤에 `accepted`로 바꾼다. 머지 전이면 그대로 둔다.

- [x] **Step 6: 커밋(문서 저장소)**

```bash
git add parfait/specs/2026-08-27-canvas-today-ssot-polling.md parfait/adr/0029-canvas-today-ssot-polling.md
git commit -m "docs: 캔버스 폴링 구현의 as-built 차이를 스펙에 반영한다"
```

---

## 수동 확인 (구현자가 직접)

기기 두 대가 필요한 항목이 있다.

- [ ] 기기 A에서 토핑을 올리면 기기 B의 캔버스에 폴링 주기 안에 나타난다
- [ ] 배경 편집 중 남이 토핑을 올려도 내 배치가 안 밀리고 남의 토핑만 나타난다
- [ ] 배경 편집 중 배경을 고른 뒤 기다려도 그 선택이 서버 배경으로 되돌아가지 않는다
- [ ] 배경 편집에서 토핑을 지운 뒤 지운 것이 다시 나타나지 않는다
- [ ] 카메라·갤러리로 나가 있는 동안 네트워크 요청이 멎는다(프로파일러·프록시로 확인)
- [ ] 앱을 백그라운드로 보내면 요청이 멎고, 돌아오면 즉시 한 번 나간다
- [ ] 달력에서 지난 날을 보는 동안 요청이 멎는다
- [ ] 남의 토핑을 강조한 상태에서 그 사람이 그 토핑을 지우면, 딤이 걷힌 뒤 다른 토핑을 탭할 수 있다
- [ ] 계정 전환 시 이전 계정의 캔버스가 남지 않는다
- [ ] 배경 편집·토핑 배치를 열 때 요청이 한 번만 나간다(진입 갱신 제거 확인)

---

## Self-Review 결과

**스펙 커버리지** — 「폴링을 어디에 두는가」(Task 3·4), 「폴링 수명을 무엇에 매다는가」(Task 1·4·6), 「하루 경계」(Task 3의 캐시 날짜 판정 + Task 5의 티커 + Task 6의 구독), 「배경 편집 화면의 병합」(Task 7), 「캔버스 메인의 스포트라이트」(Task 6), 「토핑 배치 화면의 `positionZ`」(Task 8), 「세션 정리와 진행 중인 갱신」(Task 3의 세대 카운터 + Task 4의 두 경로), 「쓰기 성공 뒤 강제 갱신」 셋 전부(Task 7 Step 6·8, Task 8 Step 5), 「검증」(각 태스크 + Task 9).

**타입 일관성** — `launchWhileSubscribed`의 파라미터 이름은 Task 1에서 `source`로 정하고 Task 6·7·8이 같은 이름을 쓴다. `CanvasPoller`의 다섯 함수는 Task 3에서 정의하고 Task 4가 그대로 부른다. `dirtyToppingIds`·`deletedToppingIds`는 Task 7에서만 쓴다. `RefreshTodayParfaitDetailUseCase`는 Task 4에서 만들고 Task 7이 쓴다. `RequestTodayParfaitRefreshUseCase`는 Task 8에서 만들고 Task 8만 쓴다.

**스펙과의 as-built 차이 셋** — ① 폴러가 하루 경계 티커 대신 캐시 날짜로 판정한다(Task 3에서 즉시 반영). ② `ParfaitRepository` 표면이 일곱이 된다. ③ `confirmedToppings`의 위치가 스펙 오기다. 셋 다 Task 9에서 문서에 반영한다.

**Task 1의 다섯 번째 테스트** — 헬퍼만으로 자기 고착을 막을 수 있는지는 구현해 봐야 안다. 못 막으면 KDoc 경고를 계약으로 삼고 Task 6이 `state`를 안 읽는 것으로 대신한다. 어느 쪽을 택했는지 실행 기록에 남긴다.
