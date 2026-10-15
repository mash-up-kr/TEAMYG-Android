# 캔버스 폴링 적응형 주기 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ✅ **완료·develop 머지(2026-09-10, PR #482 `efa771503`).** 주기 단계 셋과 리셋 계기 여덟,
> 푸시 배선이 설계대로 들어왔다. `acquire`의 락을 하나로 합친 것, 폴러가 `CanvasPollInterval`을
> 기본 인자로 받는 것, 토핑 판정을 서비스 밖으로 꺼낸 것 셋이 계획과 다르다 — 기록은
> [스펙 「as-built」](../../specs/archive/2026-09-10-canvas-adaptive-polling.md).
> ⚠️ 같은 PR이 폴링과 무관한 화면 수정 다섯을 함께 실었다(계획 밖 작업).
> ⚠️ **체크박스는 실행 세션이 남기지 않아 전부 미체크다**(41개). 진행의 정본은 `git log`다.

**Goal:** 오늘 캔버스 폴링의 고정 5초 주기를 변화 여부에 반응하는 10~20초 적응형 주기로 바꾸고, 토핑 푸시를 받으면 즉시 되돌린다.

**Architecture:** 주기 계산만 아는 순수 클래스 `CanvasPollInterval`을 `:data`에 새로 두고, `CanvasPoller`가 대기 직전마다 그 클래스에 묻는다. 변화 판정은 `refresh`가 이미 읽어 둔 캐시 값과 새 응답의 `CanvasVO` 구조적 동등성이다. 푸시는 기존 `RequestTodayParfaitRefreshUseCase`를 그대로 부른다 — 도메인·저장소 표면을 새로 만들지 않는다.

**Tech Stack:** Kotlin, kotlinx.coroutines, Hilt, kotlinx-coroutines-test(`runTest` 가상 시간), kotlin.test, Firebase Messaging.

**Spec:** [`parfait/specs/2026-09-10-canvas-adaptive-polling.md`](../../specs/archive/2026-09-10-canvas-adaptive-polling.md)

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`다.** 이 계획 문서가 있는 저장소가 아니다. 문서 태스크(Task 6)만 이 저장소에서 한다.
- **커밋만 하고 push·PR은 하지 않는다.** 사용자가 따로 지시할 때까지 리모트로 내보내지 않는다.
- **주기 단계는 10초 / 15초 / 20초 세 개, 상한 20초.** 이 값을 임의로 바꾸지 않는다.
- **갱신 실패는 주기를 건드리지 않는다.** 실패를 「변화 없음」으로 세면 안 된다.
- **코드 주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다. 낡는다.
  - 주석 분량은 코드의 **어려움**에 비례해야지 **중요함**에 비례하면 안 된다.
- **`CanvasPollInterval`은 스스로 락을 들지 않는다.** `CanvasPoller`의 `synchronized(lock)` 안에서만 불린다.
- **한국어 주석·커밋 메시지.** 커밋 메시지는 `type: 한국어 현재형 서술` 형식이고 본문 끝에 `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>`를 붙인다.

---

## File Structure

| 파일 | 책임 | 상태 |
|---|---|---|
| `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollInterval.kt` | 그룹별 주기 단계 계산. 시간·코루틴·네트워크를 모른다 | 신설 |
| `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollIntervalTest.kt` | 위 클래스의 단위 테스트 | 신설 |
| `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt` | 폴링 트리거 소유. 주기를 위 클래스에 묻고 결과를 되먹인다 | 수정 |
| `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt` | 폴러 테스트. 기존 5초 가정을 10초로 고치고 램프 케이스를 더한다 | 수정 |
| `feature/groups/canvas/impl/.../viewmodel/CanvasMainViewModel.kt` | 「폴링은 5초마다」 단정 주석 3곳 정리 | 수정 |
| `feature/groups/canvas/impl/src/test/.../viewmodel/CanvasMainViewModelTest.kt` | 같은 단정 주석 2곳 정리 | 수정 |
| `app/src/main/java/com/teamyg/parfait/push/PushDeepLinkIntent.kt` | `Intent`·`Map` extras를 `PushDeepLink`로 | 수정 |
| `app/src/main/java/com/teamyg/parfait/push/ParfaitFirebaseMessagingService.kt` | 토핑 푸시를 받으면 갱신 요청 | 수정 |
| `app/src/test/java/com/teamyg/parfait/push/PushDeepLinkIntentTest.kt` | `Map` 파싱 테스트 추가 | 수정 |
| `parfait/adr/0029-canvas-today-ssot-polling.md` | 적응형 주기 항목 신설, 리셋 계기 열거 | 수정(문서 저장소) |
| `parfait/synthesis/open-questions.md` | OQ-P-320 상태 갱신 | 수정(문서 저장소) |

---

### Task 1: `CanvasPollInterval` 신설

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollInterval.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollIntervalTest.kt`

**Interfaces:**
- Consumes: `com.teamyg.parfait.domain.model.id.GroupId`
- Produces: `class CanvasPollInterval @Inject constructor()` with
  `fun current(groupId: GroupId): Duration`,
  `fun onChanged(groupId: GroupId)`,
  `fun onUnchanged(groupId: GroupId)`,
  `fun onReset(groupId: GroupId)`,
  `fun forget(groupId: GroupId)`,
  `fun forgetAll()`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollIntervalTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.id.GroupId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private val GROUP = GroupId(1L)
private val OTHER_GROUP = GroupId(2L)

class CanvasPollIntervalTest {
    @Test
    fun current_beforeAnyFeedback_isTheShortestStage() {
        val interval = CanvasPollInterval()

        assertEquals(10.seconds, interval.current(GROUP))
    }

    @Test
    fun onUnchanged_climbsOneStageAtATime() {
        val interval = CanvasPollInterval()

        interval.onUnchanged(GROUP)
        assertEquals(15.seconds, interval.current(GROUP))

        interval.onUnchanged(GROUP)
        assertEquals(20.seconds, interval.current(GROUP))
    }

    @Test
    fun onUnchanged_atTheTop_staysThere() {
        val interval = CanvasPollInterval()

        repeat(10) { interval.onUnchanged(GROUP) }

        // 상한을 넘겨 세면 화면을 오래 열어 둘수록 남의 토핑이 늦게 보인다
        assertEquals(20.seconds, interval.current(GROUP))
    }

    @Test
    fun onChanged_fromTheTop_returnsToTheShortestStage() {
        val interval = CanvasPollInterval()
        repeat(10) { interval.onUnchanged(GROUP) }

        interval.onChanged(GROUP)

        assertEquals(10.seconds, interval.current(GROUP))
    }

    @Test
    fun onReset_fromTheTop_returnsToTheShortestStage() {
        val interval = CanvasPollInterval()
        repeat(10) { interval.onUnchanged(GROUP) }

        interval.onReset(GROUP)

        assertEquals(10.seconds, interval.current(GROUP))
    }

    @Test
    fun stages_ofOneGroup_doNotMoveAnother() {
        val interval = CanvasPollInterval()

        interval.onUnchanged(GROUP)
        interval.onUnchanged(GROUP)

        assertEquals(20.seconds, interval.current(GROUP))
        assertEquals(10.seconds, interval.current(OTHER_GROUP))
    }

    @Test
    fun forgetAll_clearsGroupsThatNobodyReleased() {
        val interval = CanvasPollInterval()
        repeat(10) { interval.onUnchanged(GROUP) }
        repeat(10) { interval.onUnchanged(OTHER_GROUP) }

        interval.forgetAll()

        assertEquals(10.seconds, interval.current(GROUP))
        assertEquals(10.seconds, interval.current(OTHER_GROUP))
    }

    @Test
    fun forget_makesTheNextVisitStartOver() {
        val interval = CanvasPollInterval()
        repeat(10) { interval.onUnchanged(GROUP) }

        interval.forget(GROUP)

        assertEquals(10.seconds, interval.current(GROUP))
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*CanvasPollIntervalTest*"
```

Expected: 컴파일 실패 — `Unresolved reference: CanvasPollInterval`.

- [ ] **Step 3: 최소 구현을 쓴다**

`data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollInterval.kt`:

```kotlin
package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.id.GroupId
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 폴링 주기를 그룹별로 재는 자. 변화 없는 조회가 이어지면 성기게, 변화가 오면 다시 촘촘하게
 * 만든다(`specs/2026-09-10-canvas-adaptive-polling.md`).
 *
 * 스스로 락을 들지 않는다 — [CanvasPoller] 의 `synchronized(lock)` 안에서만 불린다. 락을 둘로
 * 나누면 주기를 읽는 것과 타이머를 다시 세우는 것 사이가 갈라진다.
 */
class CanvasPollInterval @Inject constructor() {
    private val stages = mutableMapOf<GroupId, Int>()

    fun current(groupId: GroupId): Duration = STAGES[stages[groupId] ?: 0]

    /** 조회 결과가 캐시와 달랐다 */
    fun onChanged(groupId: GroupId) {
        stages[groupId] = 0
    }

    /** 바깥 사건(진입·쓰기·푸시)이 되돌리라고 했다 — [onChanged] 와 하는 일은 같고 뜻이 다르다 */
    fun onReset(groupId: GroupId) {
        stages[groupId] = 0
    }

    fun onUnchanged(groupId: GroupId) {
        stages[groupId] = ((stages[groupId] ?: 0) + 1).coerceAtMost(STAGES.lastIndex)
    }

    fun forget(groupId: GroupId) {
        stages.remove(groupId)
    }

    fun forgetAll() {
        stages.clear()
    }

    private companion object {
        val STAGES = listOf(10.seconds, 15.seconds, 20.seconds)
    }
}
```

- [ ] **Step 4: 통과를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*CanvasPollIntervalTest*"
```

Expected: PASS (8건).

- [ ] **Step 5: ktlint를 돌린다**

```bash
./gradlew :data:ktlintCheck
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollInterval.kt \
        data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollIntervalTest.kt
git commit -F - <<'MSG'
feat: 폴링 주기를 재는 CanvasPollInterval 을 만든다

변화 없는 조회가 이어지면 10 → 15 → 20 초로 올라가고 상한에서 멈춘다.
시간도 코루틴도 네트워크도 몰라 램프 전 구간이 값만으로 고정된다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
```

---

### Task 2: 폴러가 주기를 물어보게 한다

기존 테스트가 고정 5초를 가정하고 있어 이 태스크에서 함께 고친다. 값만 바꾸면 뜻을 잃는 테스트가 둘 있으므로 그 둘은 시간 값을 다시 계산한다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt`

**Interfaces:**
- Consumes: Task 1의 `CanvasPollInterval#current`
- Produces: `CanvasPoller`의 5번째 생성자 파라미터 `interval: CanvasPollInterval = CanvasPollInterval()`. Task 3·4가 같은 필드를 쓴다.

- [ ] **Step 1: 기존 테스트의 5초 가정을 10초로 고친다**

`CanvasPollerTest.kt`에서 `advanceTimeBy(5.seconds)` 세 자리를 `advanceTimeBy(10.seconds)`로 바꾼다. 해당 테스트는 `poll_afterTheCacheIsWarm_usesTheDetailEndpoint`, `poll_whenTheCachedDateIsStale_fallsBackToToday`, `acquire_twice_stillCallsOncePerInterval` 셋이다.

`refreshNow_restartsTheInterval`은 값만 바꾸면 안 된다. 지금은 4초에 강제 갱신하고 2초를 더 밀어 "원래 주기(5초)였다면 나갔을 것"을 잡는데, 주기가 10초가 되면 6초는 아직 첫 주기 안이라 아무것도 증명하지 못한다. 이렇게 고친다.

```kotlin
    @Test
    fun refreshNow_restartsTheInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()

        advanceTimeBy(9.seconds)
        poller.refreshNow(GROUP)
        runCurrent()
        val afterForced = remote.todayCallCount + remote.detailCallCount

        // 원래 주기였다면 1초 뒤에 한 번 더 나갔어야 한다
        advanceTimeBy(2.seconds)
        runCurrent()

        assertEquals(afterForced, remote.todayCallCount + remote.detailCallCount)
    }
```

`refresh_whileAnotherIsInFlight_skipsThisRound`도 마찬가지다. 지금 11초로 "주기를 두 번 민다"고 적혀 있는데 10초 주기에서는 한 번이다. 응답이 붙들려 있는 동안에는 `refresh`가 곧장 반환해 되먹임이 없으므로 주기가 10초에 머문다. 25초로 바꾼다.

```kotlin
        // 첫 요청이 아직 안 끝난 채로 주기를 두 번 민다
        advanceTimeBy(25.seconds)
        runCurrent()
```

- [ ] **Step 2: 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*CanvasPollerTest*"
```

Expected: FAIL. 폴러가 아직 5초로 돌아 `poll_afterTheCacheIsWarm_usesTheDetailEndpoint`가 `expected:<1> but was:<2>` 계열로 깨진다.

- [ ] **Step 3: 폴러가 주기를 물어보게 고친다**

`CanvasPoller.kt`의 파일 상단 상수를 지운다.

```kotlin
/** 실측 전 값이다(OQ-P-320) */
private val CANVAS_POLL_INTERVAL: Duration = 5.seconds
```

이 상수가 사라지면서 `kotlin.time.Duration`과 `kotlin.time.Duration.Companion.seconds` import가 쓰이지 않게 되면 함께 지운다.

생성자에 파라미터를 더한다. `clock` 과 같은 자리다 — Hilt는 Kotlin 기본값을 쓰지 않으므로 `@Inject constructor()` 가 붙은 Task 1의 클래스가 그대로 바인딩이 되고, 기본값은 테스트가 인자를 생략할 수 있게 한다.

```kotlin
    private val clock: Clock = Clock.System,
    private val interval: CanvasPollInterval = CanvasPollInterval(),
) {
```

타이머가 매 회차마다 다시 묻게 한다.

```kotlin
    private fun restartPollTimerLocked(groupId: GroupId) {
        pollJobs.remove(groupId)?.cancel()
        pollJobs[groupId] = scope.launch {
            while (isActive) {
                // 대기 직전마다 다시 묻는다 — 도는 중에 단계가 바뀌어도 다음 회차부터 반영된다
                delay(synchronized(lock) { interval.current(groupId) })
                refresh(groupId)
            }
        }
    }
```

- [ ] **Step 4: 통과를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*CanvasPollerTest*"
```

Expected: PASS.

- [ ] **Step 5: 「폴링은 5초마다」를 단정한 주석 다섯을 고친다**

주기가 더는 5초가 아니므로 이 문장들이 전부 거짓이 된다. `parfait/CLAUDE.md`가 「다른 컴포넌트의 현재 상태를 단정하지 않는다 — 낡는다」로 금지한 바로 그 유형이라, 수치를 되살리지 말고 **수치를 빼서** 다시 낡지 않게 만든다.

- `feature/groups/canvas/impl/src/main/kotlin/.../viewmodel/CanvasMainViewModel.kt:221`
  `— 폴링은 5초마다 돌아 매번 알리면 방해가 된다` → `— 폴링이 주기마다 돌아 매번 알리면 방해가 된다`
- 같은 파일 `:395`
  `폴링이 5초마다` → `폴링이 주기마다`
- 같은 파일 `:447`
  `폴링은 5초마다 도므로` → `폴링은 주기마다 도므로`
- `feature/groups/canvas/impl/src/test/kotlin/.../viewmodel/CanvasMainViewModelTest.kt:539`
  `5초마다 도는 폴링이면 매번 일어난다` → `주기마다 도는 폴링이면 매번 일어난다`
- 같은 파일 `:1081`
  `5초마다 도는 폴링의 실패로` → `주기마다 도는 폴링의 실패로`

줄 번호는 이 계획을 쓴 시점의 것이다. 옮겨졌을 수 있으니 `grep -rn "5초" --include="*.kt" feature/groups/canvas/impl/src` 로 다시 찾아 다섯 자리가 모두 처리됐는지 확인한다.

- [ ] **Step 6: ktlint와 컴파일, 영향 모듈 테스트를 확인한다**

```bash
./gradlew :data:ktlintCheck :data:compileDebugKotlin \
          :feature:groups:canvas:impl:ktlintCheck :feature:groups:canvas:impl:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt \
        data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt \
        feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt
git commit -F - <<'MSG'
refactor: 폴링 주기를 상수에서 CanvasPollInterval 로 옮긴다

주기 값은 아직 안 바뀐 동작이고, 대기 직전마다 다시 묻는 자리만 만든다.
기존 테스트가 고정 5초를 가정하고 있어 함께 고친다. 강제 갱신과 중첩 가드
테스트 둘은 값만 바꾸면 뜻을 잃어 시간 값을 다시 계산했다.

캔버스 화면의 「폴링은 5초마다」 주석 다섯도 함께 걷는다. 수치를 되살리지 않고
빼서 다시 낡지 않게 한다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
```

---

### Task 3: 조회 결과를 주기에 되먹인다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt`

**Interfaces:**
- Consumes: `CanvasPollInterval#onChanged`·`#onUnchanged`
- Produces: 없음. 폴러 내부 동작이다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`FakeRemote`가 지금은 응답 하나를 고정으로 돌려준다. 도중에 응답을 바꿀 수 있어야 하므로 `response`를 `var`로 연다. 기존 호출부는 생성자 인자를 그대로 쓰므로 영향이 없다.

```kotlin
    private class FakeRemote(
        response: CanvasVO,
        private val gate: CompletableDeferred<Unit>? = null,
        private val failure: Throwable? = null,
    ) : ParfaitRemoteDataSource {
        /** 폴링 도중에 서버가 다른 값을 주는 상황을 만드는 데 쓴다 */
        var response: CanvasVO = response
```

`CanvasPollerTest`에 테스트 셋을 더한다.

```kotlin
    @Test
    fun poll_whenNothingChanges_stretchesTheInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        val afterFirst = remote.todayCallCount + remote.detailCallCount

        // 첫 주기 10초, 그다음 15초 — 24초로는 두 번째 주기가 아직 차지 않는다
        advanceTimeBy(24.seconds)
        runCurrent()

        assertEquals(afterFirst + 1, remote.todayCallCount + remote.detailCallCount)
    }

    @Test
    fun poll_whenTheCanvasChanges_returnsToTheShortestInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(10.seconds)
        runCurrent()

        // 같은 값이 두 번 왔으니 다음 주기는 15초다. 그 회차에서 값이 달라진다
        remote.response = canvas().copy(lastClosedDate = LocalDate(2026, 1, 1))
        advanceTimeBy(15.seconds)
        runCurrent()
        val afterChange = remote.todayCallCount + remote.detailCallCount

        // 변화를 봤으니 10초로 돌아온다 — 15초짜리 주기였다면 아직 안 나갔을 시점이다
        advanceTimeBy(11.seconds)
        runCurrent()

        assertEquals(afterChange + 1, remote.todayCallCount + remote.detailCallCount)
    }

    @Test
    fun poll_whenItFails_doesNotStretchTheInterval() = runTest {
        val remote = FakeRemote(canvas(), failure = IllegalStateException("실패"))
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        val afterFirst = remote.todayCallCount + remote.detailCallCount

        // 실패를 변화 없음으로 세면 두 번째 주기가 15초가 되어 24초에 두 번이 안 나간다
        advanceTimeBy(24.seconds)
        runCurrent()

        assertEquals(afterFirst + 2, remote.todayCallCount + remote.detailCallCount)
    }
```

- [ ] **Step 2: 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*CanvasPollerTest*"
```

Expected: FAIL — 단 **세 건 중 `poll_whenNothingChanges_stretchesTheInterval` 하나만** 깨진다. 되먹임이 없어 주기가 계속 10초라 24초 안에 세 번 나가고 `expected:<2> but was:<3>`이 된다.

나머지 둘은 지금도 통과한다. 주기가 고정 10초일 때의 호출 횟수가 우연히 기대값과 같기 때문이다. 그래도 지우지 말 것 — Step 3 구현 뒤에는 잘못된 구현을 정확히 잡는다. `poll_whenTheCanvasChanges_returnsToTheShortestInterval`은 변화를 보고도 되돌리지 않는 구현에서, `poll_whenItFails_doesNotStretchTheInterval`은 실패를 「변화 없음」으로 세는 구현에서 깨진다. **"왜 안 깨지지"에서 멈추지 말 것.**

- [ ] **Step 3: 되먹임을 구현한다**

`CanvasPoller#refresh`의 `onSuccess` 블록을 고친다. `cached`는 이 함수가 조회 방식을 고르려고 이미 읽어 둔 값이다.

```kotlin
            return result
                .onSuccess { canvas ->
                    synchronized(lock) {
                        if (generation == startedGeneration) {
                            // 구독자가 없는 갱신(화면 밖 푸시)이 단계를 올려 두면 다음 진입의 첫
                            // 주기가 10초가 아니게 된다. 첫 조회(cached == null)는 변화로 친다
                            if (subscriberCounts.containsKey(groupId)) {
                                if (cached != canvas) interval.onChanged(groupId) else interval.onUnchanged(groupId)
                            }
                            local.saveTodayCanvas(groupId, canvas)
                        }
                    }
                }.onFailure {
                    // 세대가 바뀌었으면 이미 버려진 갱신의 실패라 화면에 알리지 않는다
                    if (synchronized(lock) { generation == startedGeneration }) _refreshFailures.tryEmit(groupId)
                }.map { }
```

실패 갈래에서는 `interval`을 부르지 않는다. 실패를 「변화 없음」으로 세면 서버가 흔들리는 동안 주기가 늘어져 회복이 가장 필요할 때 가장 늦어진다.

- [ ] **Step 4: 통과를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*CanvasPollerTest*"
```

Expected: PASS.

- [ ] **Step 5: ktlint를 돌린다**

```bash
./gradlew :data:ktlintCheck
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt \
        data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt
git commit -F - <<'MSG'
feat: 조회 결과의 변화 여부로 폴링 주기를 움직인다

캐시와 다른 응답이 오면 10초로 돌아오고, 같으면 한 칸 성겨진다. 판정 재료는
refresh 가 조회 방식을 고르려고 이미 읽어 둔 값이라 새로 부르는 것이 없다.

갱신 실패는 주기를 건드리지 않는다. 변화 없음으로 세면 서버가 흔들리는 동안
주기가 늘어져 회복이 가장 필요할 때 가장 늦어진다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
```

---

### Task 4: 바깥 사건이 주기를 되돌린다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt`

**Interfaces:**
- Consumes: `CanvasPollInterval#onReset`·`#forget`
- Produces: 없음. `refreshNowAsync`를 지나는 호출부(Task 5의 푸시 포함)가 이 동작을 물려받는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
    @Test
    fun refreshNow_returnsToTheShortestInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(10.seconds)
        runCurrent()

        // 강제 갱신도 조회라, 이 회차까지 세면 되돌리기가 없을 때의 다음 주기는 20초다
        poller.refreshNow(GROUP)
        runCurrent()
        val afterForced = remote.todayCallCount + remote.detailCallCount

        // 되돌렸으니 10초에 나간다 — 되돌리지 않았다면 아직이다
        advanceTimeBy(11.seconds)
        runCurrent()

        assertEquals(afterForced + 1, remote.todayCallCount + remote.detailCallCount)
    }

    @Test
    fun release_thenAcquireAgain_startsFromTheShortestInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(10.seconds)
        runCurrent()
        poller.release(GROUP)

        poller.acquire(GROUP)
        runCurrent()
        val afterReacquire = remote.todayCallCount + remote.detailCallCount

        // 단계가 남아 있었다면 15초짜리라 아직 안 나갔을 시점이다
        advanceTimeBy(11.seconds)
        runCurrent()

        assertEquals(afterReacquire + 1, remote.todayCallCount + remote.detailCallCount)
    }
```

- [ ] **Step 2: 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*CanvasPollerTest*"
```

Expected: FAIL. 되돌리는 자리가 없어 두 테스트 모두 `expected:<N+1> but was:<N>`으로 깨진다.

- [ ] **Step 3: 되돌리는 자리를 배선한다**

`acquire`에서 첫 구독일 때 되돌린다. 화면 진입이 가장 촘촘한 단계에서 시작한다는 것을 이 한 줄이 보장한다 — `release`의 `forget`에만 기대면, 구독 없이 나간 갱신이 남긴 단계가 그대로 남는다.

```kotlin
    fun acquire(groupId: GroupId) {
        val isFirst = synchronized(lock) {
            val next = (subscriberCounts[groupId] ?: 0) + 1
            subscriberCounts[groupId] = next
            next == 1
        }
        if (isFirst.not()) return

        synchronized(lock) { interval.onReset(groupId) }
        restartPollTimer(groupId)
        scope.launch { refresh(groupId) }
    }
```

`refreshNow`에서도 타이머를 다시 세우기 직전에 되돌린다.

```kotlin
    suspend fun refreshNow(groupId: GroupId): Result<Unit> {
        val result = refresh(groupId)
        synchronized(lock) {
            if (subscriberCounts.containsKey(groupId)) {
                interval.onReset(groupId)
                restartPollTimerLocked(groupId)
            }
        }
        return result
    }
```

`release`에서 마지막 구독이 빠질 때 단계를 지운다.

```kotlin
    fun release(groupId: GroupId) {
        synchronized(lock) {
            val next = (subscriberCounts[groupId] ?: 0) - 1
            if (next <= 0) {
                subscriberCounts.remove(groupId)
                pollJobs.remove(groupId)?.cancel()
                interval.forget(groupId)
            } else {
                subscriberCounts[groupId] = next
            }
        }
    }
```

`stopAll`에서는 전부 지운다.

```kotlin
    fun stopAll() {
        synchronized(lock) {
            generation++
            pollJobs.values.forEach(Job::cancel)
            pollJobs.clear()
            // 구독자 없이 단계만 남은 그룹도 있으므로 키 순회로는 부족하다
            interval.forgetAll()
            subscriberCounts.clear()
            refreshing.clear()
        }
    }
```

- [ ] **Step 4: 통과를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*CanvasPollerTest*"
```

Expected: PASS.

- [ ] **Step 5: `:data` 전체 단위 테스트와 ktlint를 돌린다**

```bash
./gradlew :data:testDebugUnitTest :data:ktlintCheck
```

Expected: BUILD SUCCESSFUL. 이 모듈의 다른 테스트가 폴러 주기를 가정하고 있지 않은지 여기서 확인한다.

- [ ] **Step 6: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/parfait/local/CanvasPoller.kt \
        data/src/test/java/com/teamyg/parfait/data/source/parfait/local/CanvasPollerTest.kt
git commit -F - <<'MSG'
feat: 강제 갱신과 구독 해제가 폴링 주기를 되돌린다

쓰기 직후의 강제 갱신은 활동이 있었다는 뜻이라 주기를 처음으로 되돌린다.
마지막 구독이 빠지거나 세션이 끝나면 단계를 지워, 다시 들어올 때 가장 촘촘한
주기에서 시작한다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
```

---

### Task 5: 토핑 푸시가 갱신을 부른다

**Files:**
- Modify: `app/src/main/java/com/teamyg/parfait/push/PushDeepLinkIntent.kt`
- Modify: `app/src/main/java/com/teamyg/parfait/push/ParfaitFirebaseMessagingService.kt`
- Test: `app/src/test/java/com/teamyg/parfait/push/PushDeepLinkIntentTest.kt`

**Interfaces:**
- Consumes: `PushDeepLinkParser#parse`, `PushDeepLink.AddTopping`, `RequestTodayParfaitRefreshUseCase#invoke(GroupId)`, Task 4까지의 `refreshNowAsync` 동작
- Produces: `fun Map<String, String>.toPushDeepLinkOrNull(): PushDeepLink?`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`app/src/test/java/com/teamyg/parfait/push/PushDeepLinkIntentTest.kt` 안의 클래스에 더한다.

```kotlin
    @Test
    fun mapToPushDeepLinkOrNull_toppingPayload_readsTheGroupId() {
        val data = mapOf("route" to "canvas", "groupId" to "34", "type" to "TOPPING")

        assertEquals(PushDeepLink.AddTopping(groupId = 34L), data.toPushDeepLinkOrNull())
    }

    @Test
    fun mapToPushDeepLinkOrNull_remindPayload_isNotAddTopping() {
        val data = mapOf("route" to "group", "type" to "REMIND_AM")

        // 리마인드에는 groupId 가 없다 — 이 갈림이 없으면 하루 두 번 엉뚱한 그룹이 되살아난다
        assertIs<PushDeepLink.GroupList>(data.toPushDeepLinkOrNull())
    }

    @Test
    fun mapToPushDeepLinkOrNull_unknownRoute_isNull() {
        assertNull(mapOf("route" to "nowhere").toPushDeepLinkOrNull())
    }
```

⚠️ `route` 값은 **소문자**다. `PushNotificationRouteType`의 키가 `canvas`·`group`이고 `fromKeyOrNull`이 정확히 일치할 때만 통과한다. 대문자로 쓰면 파서가 `null`을 내 두 테스트가 깨진다.

`import kotlin.test.assertIs`와 `import kotlin.test.assertNull`이 없으면 더한다.

- [ ] **Step 2: 실패를 확인한다**

```bash
./gradlew :app:testDebugUnitTest --tests "*PushDeepLinkIntentTest*"
```

Expected: 컴파일 실패 — `Unresolved reference: toPushDeepLinkOrNull` (Map 확장이 없다).

- [ ] **Step 3: `Map` 확장을 더한다**

`PushDeepLinkIntent.kt`에 함수를 하나 더한다. 키 상수는 파일에 이미 있는 것을 그대로 쓴다.

```kotlin
/**
 * 포그라운드에서 받은 FCM `data` 를 읽는다. `Intent` 판과 달리 태스크 되살리기 검사가 없다 —
 * 방금 도착한 메시지라 과거의 것일 수 없다.
 */
fun Map<String, String>.toPushDeepLinkOrNull(): PushDeepLink? = PushDeepLinkParser.parse(
    route = this[EXTRA_ROUTE],
    groupId = this[EXTRA_GROUP_ID],
    type = this[EXTRA_TYPE],
)
```

- [ ] **Step 4: 통과를 확인한다**

```bash
./gradlew :app:testDebugUnitTest --tests "*PushDeepLinkIntentTest*"
```

Expected: PASS.

- [ ] **Step 5: 서비스에 갈래를 더한다**

`ParfaitFirebaseMessagingService.kt`에 의존과 갈래를 더한다.

```kotlin
    @Inject
    lateinit var deviceTokenRegistrar: DeviceTokenRegistrar

    @Inject
    lateinit var requestTodayParfaitRefresh: RequestTodayParfaitRefreshUseCase

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notification = message.notification ?: return
        showNotification(
            title = notification.title.orEmpty(),
            body = notification.body.orEmpty(),
            data = message.data,
            notificationId = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(),
        )

        // 남이 토핑을 올렸다는 신호다 — 폴링 주기를 기다리지 않고 바로 받아 온다
        val deepLink = message.data.toPushDeepLinkOrNull()
        if (deepLink is PushDeepLink.AddTopping) {
            requestTodayParfaitRefresh(GroupId(deepLink.groupId))
        }
    }
```

import를 더한다.

```kotlin
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.push.PushDeepLink
import com.teamyg.parfait.domain.usecase.parfait.RequestTodayParfaitRefreshUseCase
```

클래스 KDoc의 "`message.notification` 이 없으면 곧장 return 하고 `data` 는 안 본다"는 이제 절반만 맞는다. 알림이 있을 때는 `data`를 본다는 사실이 드러나도록 그 문단을 이렇게 고친다.

```
 * FCM 페이로드 스펙 v1은 세 알림(P-01/P-02/P-03) 모두 `notification` 블록을 항상 함께
 * 보낸다 — `data` 만 있는 payload는 지금 스펙에 없다. 그래서 [onMessageReceived] 는
 * `message.notification` 이 없으면 곧장 return 한다. 알림을 띄운 뒤에는 같은 `data` 를
 * 읽어 토핑 알림이면 오늘 캔버스 갱신을 요청한다
 * (`specs/2026-09-10-canvas-adaptive-polling.md`). 나중에 `data`-only payload 가 스펙에
 * 생기면 그 분기를 여기 추가해야 한다 — 지금은 처리하지 않는다.
```

- [ ] **Step 6: 컴파일과 ktlint를 확인한다**

```bash
./gradlew :app:testDebugUnitTest :app:ktlintCheck :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: 커밋한다**

```bash
git add app/src/main/java/com/teamyg/parfait/push/PushDeepLinkIntent.kt \
        app/src/main/java/com/teamyg/parfait/push/ParfaitFirebaseMessagingService.kt \
        app/src/test/java/com/teamyg/parfait/push/PushDeepLinkIntentTest.kt
git commit -F - <<'MSG'
feat: 토핑 푸시를 받으면 오늘 캔버스를 바로 받아 온다

주기가 성겨진 상태에서 남이 토핑을 올리면 최대 20초를 기다려야 했다. 포그라운드
에서 받은 data 를 기존 파서로 읽어 토핑 알림이면 갱신을 요청한다.

도메인·저장소 표면을 새로 만들지 않는다. RequestTodayParfaitRefreshUseCase 가
가는 refreshNowAsync 가 즉시 1회 조회와 주기 되돌리기를 이미 함께 한다.
리마인드 알림은 route 가 GROUP 이라 파서 단계에서 갈라진다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
```

---

### Task 6: 문서를 갱신한다

이 태스크만 문서 저장소(`team-yg-pesonal-agent`)에서 한다. 브랜치는 `docs/spec-canvas-adaptive-polling`이고 스펙 커밋이 이미 올라가 있다.

**Files:**
- Modify: `parfait/adr/0029-canvas-today-ssot-polling.md`
- Modify: `parfait/synthesis/open-questions.md`

**Interfaces:**
- Consumes: Task 1~5로 확정된 실제 동작
- Produces: 없음

- [ ] **Step 1: 「결정」 절에 적응형 주기 항목을 더한다**

ADR-0029는 주기 **값**을 결정한 적이 없다. 「주기 폴링」이라고만 쓰고 상수는 코드에 두었다. 그래서 지울 문장이 아니라 더할 항목이다. 「결정」 절의 이 항목 **바로 뒤**에 새 항목을 넣는다.

찾을 자리(그대로 있다):

```
- **모든 갱신은 폴러를 통과하고, 갱신이 나갈 때마다 주기를 다시 센다.** 쓰기(배경 저장·토핑
  추가·토핑 삭제) 성공 뒤의 강제 갱신도 같다 — 그러지 않으면 강제 갱신 직후에 주기 타이머가 또
  터져 요청이 붙어 나간다. 진행 중인 갱신이 있으면 그 주기는 건너뛴다.
```

그 아래에 더할 것:

```
- **주기는 고정이 아니라 변화 여부에 반응한다(2026-09-10 개정).** 10초에서 시작해 캐시와 같은
  응답이 올 때마다 한 칸씩 올라가고 15초를 거쳐 20초에서 멈춘다. 되돌리는 계기는 아래
  「주기를 되돌리는 계기」에 열거한다. 주기 계산은 시간도 코루틴도 모르는 `CanvasPollInterval`
  이 들고, 폴러는 대기 직전마다 그것에 묻는다 — `CanvasPoller` 는 이미 참조 계수·세대·진행 중
  표시를 한 락 위에서 굴리고 있어 램프를 그 안에 섞으면 네 번째 규칙이 된다.
  **상한 20초의 근거는 상한이 곧 최악의 체감 지연이라는 것이다.** 푸시가 그 지연을 메우도록
  설계했지만 푸시는 보장 경로가 아니다(권한 거부·전달 지연). 단계 값과 상한은 실측이 아니라
  응답 크기와 체감 지연으로 정했다(OQ-P-320).
  설계 근거는 [canvas-adaptive-polling 스펙](../../specs/archive/2026-09-10-canvas-adaptive-polling.md).
```

frontmatter의 `related_spec` 에 `canvas-adaptive-polling` 을 더한다(기존 값과 나란히 둔다).

- [ ] **Step 2: 「주기를 되돌리는 계기」 표를 더한다**

Step 1에서 넣은 항목 바로 아래, 「결정」 절 끝에 표를 붙인다. 이 결정문이 「갱신이 나가는 시점 자체는 여전히 열거한다」를 규약으로 세웠으므로 되돌리는 계기도 같은 형식으로 열거한다.

```
### 주기를 되돌리는 계기

| 계기 | 자리 | 동작 |
|---|---|---|
| 화면 진입(첫 구독) | `CanvasPoller#acquire` | 즉시 갱신 + 가장 촘촘한 단계에서 시작 |
| 조회 결과가 캐시와 다름 | `CanvasPoller#refresh` | 가장 촘촘한 단계로 |
| 조회 결과가 캐시와 같음 | `CanvasPoller#refresh` | 한 칸 올린다(상한에서 멈춤) |

위 두 줄은 **그 그룹에 구독자가 있을 때만** 적용한다. 화면 밖에서 푸시로 나간 갱신이 단계를
올려 두면 다음 진입의 첫 주기가 가장 촘촘한 단계가 아니게 된다.

| 쓰기 성공 후 강제 갱신 | `CanvasPoller#refreshNow` | 즉시 갱신 + 가장 촘촘한 단계로 |
| 토핑 푸시 수신 | `RequestTodayParfaitRefreshUseCase` | 위와 같은 경로를 탄다 |
| 마지막 구독 해제 | `CanvasPoller#release` | 단계를 지운다 — 다시 들어오면 처음부터 |
| 갱신 실패 | `CanvasPoller#refresh` | **주기를 건드리지 않는다** |
| 세션 종료 | `CanvasPoller#stopAll` | 단계를 전부 지운다 |

실패를 램프에서 뺀 이유: 실패를 「변화 없음」으로 세면 서버가 흔들리는 동안 주기가 상한까지
늘어져, 회복이 가장 필요한 순간에 가장 늦게 회복한다.
```

- [ ] **Step 3: 위험·방어의 낡은 수치를 고치고 후속 조건을 적는다**

「위험·방어」 절의 이 문장은 주기가 고정 5초임을 전제한다.

```
- 실패를 조건 없이 알리면 5초 주기마다 토스트가 쌓인다 — 덮개가 걸려 있을 때만 받는 가드를
```

수치를 빼고 고친다.

```
- 실패를 조건 없이 알리면 주기마다 토스트가 쌓인다 — 덮개가 걸려 있을 때만 받는 가드를
```

그리고 「위험·방어」 절 끝에 후속 조건을 한 줄 더한다.

```
- 클라이언트가 값을 비교해 변화를 재는 것은 서버에 조건부 요청 수단이 없어서다 — `ETag` 와
  `If-None-Match` 가 생기면 304 가 곧 「변화 없음」이므로 이 판정을 걷고 본문 전송도 함께
  없앤다. 그때 `CanvasPollInterval` 은 남고 `refresh` 의 비교만 응답 코드 판정으로 바뀐다.
```

- [ ] **Step 4: OQ-P-320을 갱신한다**

`parfait/synthesis/open-questions.md`의 OQ-P-320은 「주기를 상수 하나로 두기로 했을 뿐, 어떤 지표를 보고 언제 바꿀지는 정하지 않았다」였다. 상수가 사라졌으므로 항목을 다시 쓴다. 값을 정하는 방식이 정해졌다는 점(변화 여부에 반응)과, 단계·상한이 여전히 실측이 아니라는 점을 남긴다. 항목 ①②③ 중 실사용 지표에 관한 것은 그대로 미해결이다.

- [ ] **Step 5: 커밋한다**

```bash
git add parfait/adr/0029-canvas-today-ssot-polling.md parfait/synthesis/open-questions.md
git commit -F - <<'MSG'
docs: ADR-0029 의 고정 주기 서술을 적응형으로 고친다

결정문이 갱신 시점을 열거하기로 해 둔 터라, 주기를 되돌리는 계기도 같은 표에
넣는다. 상수가 사라져 OQ-P-320 도 다시 쓴다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
```

---

## 마무리 확인

- [ ] `TJYG-Android`에서 아래가 전부 통과한다.

```bash
./gradlew :data:testDebugUnitTest :app:testDebugUnitTest \
          :feature:groups:canvas:impl:testDebugUnitTest \
          :data:ktlintCheck :app:ktlintCheck :feature:groups:canvas:impl:ktlintCheck
```

- [ ] `grep -rn "5초" --include="*.kt" feature/groups/canvas/impl/src` 가 아무것도 내지 않는다.
- [ ] `git log --oneline`에 Task 1~5의 커밋 다섯이 있고 push는 하지 않았다.
- [ ] 실기기에서 캔버스를 열어 두고 다른 계정으로 토핑을 올렸을 때, 알림이 뜨는 즉시 캔버스에 반영되는지 확인한다. 이 확인은 자동화 테스트로 덮지 못한다.
