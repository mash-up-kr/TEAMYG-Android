---
id: alpha-kernel-suspend-cancellation
title: 알파 커널 취소 확인 전환 구현 계획 (6 Task, 브랜치 3개)
status: done
type: work-order
created: 2026-08-27
updated: 2026-08-27
platforms: android
owner: android
related_adr:
related_spec: alpha-kernel-suspend-cancellation, segmentation-alpha-refinement, segmentation-mask-postprocessing
related_code: AlphaComponents.kt#downscaleMask, AlphaComponents.kt#dilateMask, AlphaPostProcessor.kt#postProcessAlpha, AlphaPostProcessor.kt#erodeEdge, AlphaPostProcessor.kt#refineWithin, AlphaRefine.kt#refineAlpha, SegmentationMask.kt#maskSubjectAlpha, ImageSegmentationRepositoryImpl#postProcess, CountingJob.kt
archived_reason: Task 1~6 이 PR #363(`4da18230`, 2026-08-27)으로 develop 에 머지됐다. 남은 수동 작업(force push 3회·리뷰 스레드 회신)은 머지로 소멸했다
tags: [plan, parfait, coroutines]
---

# 알파 커널 취소 확인 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 알파 후처리 커널이 취소를 확인하는 수단을 호출부가 넘기는 `checkCancelled` 콜백에서 `suspend` 함수 + `ensureActive()` 로 바꾼다. 프로덕션 동작은 바뀌지 않는다.

**Architecture:** 취소 확인 루프를 가진 함수는 `suspend` 가 되고 진입 시 `currentCoroutineContext().job` 으로 `Job` 을 한 번 꺼내 루프에서 `job.ensureActive()` 를 부른다. 확인 루프 없이 하위에 전달만 하던 함수는 `suspend` 만 붙고 파라미터가 사라진다. 테스트는 `Job` 을 위임 구현한 더블(`CountingJob`)을 `Continuation` 컨텍스트로 직접 넣어 확인 호출 수를 센다.

**Tech Stack:** Kotlin 2.4.10 · kotlinx-coroutines 1.11.0 · kotlin.test + kotlinx-coroutines-test(`test-unit` 번들에 이미 있음) · Android Gradle 라이브러리 모듈 `:data`

**Spec:** [`parfait/specs/archive/2026-08-27-alpha-kernel-suspend-cancellation.md`](../../specs/archive/2026-08-27-alpha-kernel-suspend-cancellation.md)

> ✅ **Task 1~6 이 develop 에 들어갔다** — PR #363 `4da18230`(2026-08-27). 세 브랜치가 계획이 재설계한
> 순서대로 rebase 된 뒤 **정련 브랜치 하나로 접혀** 머지됐다. 그래서 `develop` 이 본 머지는 하나이고,
> 「남은 수동 작업」에 적어 둔 force push 3회와 리뷰 스레드 회신은 그 머지로 소멸했다.
> 계획이 예고한 rebase 함정(결선은 충돌이 안 나고, 정련은 `--continue` 가 `checkCancelled` 를
> 되살린다)은 실행 중에 그대로 나타났고 재설계한 방식으로 처리됐다.

## Global Constraints

모든 태스크의 요구사항에 아래가 암묵적으로 포함된다.

- **작업 저장소는 `TJYG-Android`다.** 로컬 절대경로는 `wiki/personal-private/project-paths.md` 에 있다. 이 계획 문서가 있는 저장소가 아니다.
- **시그니처를 바꾸는 변경은 그 호출부까지 같은 태스크에 넣는다.** 태스크 경계에서 컴파일이 깨지면 안 된다. 이 제약이 빠진 자리에서 앞 라운드가 실제로 게이트를 통과하지 못했다.
- **기존 파일을 전문으로 덮어쓰지 않는다.** 지정된 치환만 한다. 전문 교체는 무관한 코드를 조용히 지운다.
- **앵커 문자열이 파일 안에서 유일한지 먼저 확인하고 치환한다.** 이 계획은 유일하지 않은 앵커에 대해 함수명을 함께 적어 두었다.
- **ktlint 가 CI 게이트다.** 커밋 전에 `./gradlew :data:ktlintCheck` 를 돌린다. `testDebugUnitTest` 는 ktlint 를 태우지 않는다. 파라미터 2개 이상 시그니처는 멀티라인을 유지한다. 이 프로젝트는 `ktlint_standard_import-ordering` 이 꺼져 있어 import 순서는 자유다.
- **코드 주석·KDoc 규약**(정본은 `parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다
  - `@return`·`@param` 은 타입·이름이 말하지 못할 때만 쓴다
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다)
- **커밋은 로컬까지만 한다.** `git push`·`gh pr create`·force push 는 사용자 승인 후에만 실행한다. 태스크 안에 push 스텝이 없는 것은 실수가 아니다.
- **테스트 실행 명령은 `./gradlew :data:testDebugUnitTest` 다.** 특정 클래스만 돌릴 때는 `--tests` 를 붙인다.
- **`Job` 확장은 `currentCoroutineContext().job` 을 쓴다.** `get(Job)?.ensureActive()` 계열(`job?.ensureActive()`, `currentCoroutineContext().ensureActive()`)은 `Job` 이 없을 때 조용히 통과하므로 쓰지 않는다.
- **`suspend` 는 잎 방향으로 전염시키지 않는다.** 취소 확인이 없는 순수 함수(`ceilDiv`·`countRuns`·`fillRuns`·`findRoot`·`union`·`bilinear`·`luminanceOf`)는 그대로 둔다.

## 브랜치 구조와 rebase 방침

스택 PR 이고 아래에서 위로 쌓인다.

```
feature/segmentation-candidate-coverage   (이 계획이 손대지 않는다)
 └ feature/segmentation-alpha-kernel       ← Task 1·2·3
    └ feature/segmentation-postprocess-wiring  (커밋 6개) ← Task 4
       └ feature/segmentation-alpha-refinement  (커밋 8개) ← Task 5
```

⚠️ **rebase 방침이 이 계획의 핵심 제약이다.** 상위 브랜치의 커밋들은 `checkCancelled` 를 계속 쓰는 코드이고, 정련 브랜치의 뒤쪽 커밋 둘은 Task 4 가 지운 파라미터를 **다시 넣는다.** 그래서 다음을 지킨다.

1. **rebase 를 먼저 끝까지 돌린다.** 충돌이 나면 상위 커밋 원본을 그대로 채택한다. 전환은 rebase 중에 하지 않는다.
2. **rebase 종료를 `git status` 로 확인한다.** "rebase in progress" 가 없어야 한다.
3. **그 위에 새 커밋 하나로 전환을 얹는다.** 컴파일·테스트 게이트는 이 커밋을 만들 때 통과시킨다.

rebase 도중에 고치면 뒤 커밋이 재생되며 고친 것이 되살아난다. 결선 브랜치는 충돌이 아예 나지 않아 `git rebase --continue` 가 `fatal: No rebase in progress?` 로 죽는다.

---

### Task 1: `CountingJob` 테스트 헬퍼

확인 호출 수를 세는 `Job` 더블과 그것을 커널에 닿게 하는 실행 헬퍼를 만든다. 이후 모든 취소 테스트가 이것을 쓴다.

**Files:**
- Create: `data/src/test/java/com/teamyg/parfait/data/repository/image/CountingJob.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/CountingJobTest.kt`

**브랜치:** `feature/segmentation-alpha-kernel`

**Interfaces:**
- Produces:
  - `internal class CountingJob(delegate: Job = Job()) : Job` — `val calls: Int`, `var cancelAfter: Int`
  - `internal fun <T> runKernelCounting(job: CountingJob, block: suspend () -> T): T`

- [ ] **Step 1: 헬퍼 자체 테스트를 먼저 쓴다**

`CountingJobTest.kt` 를 만든다. 이 테스트는 헬퍼가 **함정을 실제로 막는지** 검증한다.

```kotlin
package com.teamyg.parfait.data.repository.image

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/** 커널을 흉내 낸 단계. rows 번 확인한다 */
private suspend fun fakeStage(rows: Int) {
    val job = currentCoroutineContext().job
    for (y in 0 until rows) job.ensureActive()
}

private suspend fun fakePipeline(rows: Int) {
    fakeStage(rows)
    fakeStage(rows)
}

class CountingJobTest {
    @Test
    fun runKernelCounting_twoStagePipeline_countsEveryCheck() {
        // Given
        val job = CountingJob()

        // When
        runKernelCounting(job) { fakePipeline(rows = 8) }

        // Then — 8행 × 2단계
        assertEquals(16, job.calls)
    }

    @Test
    fun runKernelCounting_oneStageRemoved_countsFewer() {
        // Given — 뒤 단계가 지워진 회귀를 흉내 낸다
        val job = CountingJob()

        // When
        runKernelCounting(job) { fakeStage(rows = 8) }

        // Then — 이 차이가 안 잡히면 헬퍼가 회귀를 못 막는다
        assertEquals(8, job.calls)
    }

    @Test
    fun runKernelCounting_cancelAfterThirdCheck_throwsCancellation() {
        // Given
        val job = CountingJob()
        job.cancelAfter = 3

        // When · Then — cancelAfter + 1 번째 조회에서 던진다
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { fakePipeline(rows = 8) }
        }
        assertEquals(4, job.calls)
    }

    @Test
    fun countingJob_isTheJobTheKernelSees() {
        // Given — Job by delegate 는 컨텍스트 조회까지 위임하므로 더블이 사라질 수 있다
        val job = CountingJob()
        var seen: Job? = null

        // When
        runKernelCounting(job) { seen = currentCoroutineContext().job }

        // Then
        assertSame(job, seen)
    }

    @Test
    fun countingJob_plus_keepsTheDoubleOnTheLeft() {
        // Given — plus 도 위임된다. 기본 위임을 쓰면 왼쪽 피연산자가 위임 Job 으로 바뀌어
        // 더블이 조용히 사라진다
        val job = CountingJob()

        // When
        val combined = job + CoroutineName("probe")

        // Then
        assertSame(job, combined[Job])
    }

    @Test
    fun runKernelCounting_blockThatSuspends_fails() {
        // Given — 하니스는 중단 없는 커널만 검증한다. 중단하면 조용히 통과하면 안 된다.
        // yield() 로는 이걸 못 만든다(인터셉터가 없으면 중단하지 않는다). withContext 로도 안 된다
        // (컨텍스트를 합성하고, 완료 타이밍에 따라 통과해 버린다). 재개되지 않는 중단이 유일하게
        // 결정적이다
        val job = CountingJob()

        // When · Then
        assertFailsWith<IllegalStateException> {
            runKernelCounting(job) { suspendCoroutine<Unit> { } }
        }
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests '*CountingJobTest'
```

기대: 컴파일 실패. `CountingJob` 과 `runKernelCounting` 이 없다.

- [ ] **Step 3: 헬퍼를 구현한다**

`CountingJob.kt` 를 만든다.

```kotlin
package com.teamyg.parfait.data.repository.image

import kotlinx.coroutines.InternalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * `isActive` 조회 수를 세는 [Job]. 커널이 행 경계마다 취소를 확인하는지 검증하는 데 쓴다.
 *
 * ⚠️ `Job by delegate` 는 [CoroutineContext] 의 `get`·`fold`·`minusKey`·`plus` 까지 위임한다.
 * 넷을 다 오버라이드하지 않으면 컨텍스트 조회가 위임 대상을 돌려주어 [isActive] 가 영영 안 불리고,
 * 증상은 "테스트가 조용히 통과한다"다.
 *
 * ⚠️ 코루틴이 [Job] 상속에 opt-in 을 요구하고 장래 에러 승격을 예고했다. 버전을 올릴 때 이 위임이
 * 막힐 수 있다.
 */
@OptIn(InternalForInheritanceCoroutinesApi::class)
internal class CountingJob(
    private val delegate: Job = Job(),
) : Job by delegate {
    var calls: Int = 0
        private set

    /** 이 횟수를 넘긴 조회부터 취소된 것으로 답한다 */
    var cancelAfter: Int = Int.MAX_VALUE

    override val isActive: Boolean
        get() {
            calls++
            // 위임을 먼저 취소해야 한다 — getCancellationException() 은 아직 활성인 Job 에서
            // 부르면 CancellationException 이 아니라 IllegalStateException 을 던진다
            if (calls > cancelAfter && delegate.isActive) delegate.cancel()
            return delegate.isActive
        }

    @Suppress("UNCHECKED_CAST")
    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? =
        if (key === Job) this as E else null

    override fun <R> fold(
        initial: R,
        operation: (R, CoroutineContext.Element) -> R,
    ): R = operation(initial, this)

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext =
        if (key === Job) EmptyCoroutineContext else this

    // 위임된 plus 는 왼쪽 피연산자를 위임 Job 으로 바꿔 더블을 지운다. 인터페이스 기본 구현을
    // 부르면 this 가 왼쪽에 남는다
    override fun plus(context: CoroutineContext): CoroutineContext = super.plus(context)
}

/**
 * [block] 을 [job] 만 담긴 컨텍스트에서 돌린다.
 *
 * ⚠️ `withContext(job)` 을 쓰면 안 된다. 새 `ScopeCoroutine` 이 만들어져 컨텍스트의 [Job] 자리를
 * 차지하므로 커널이 [job] 을 못 본다.
 *
 * ⚠️ 컨텍스트에 `ContinuationInterceptor`(디스패처)를 넣으면 안 된다. 본문이 비동기로 제출되고
 * 이 함수가 즉시 반환해, 확인 호출 수가 0 인 채로 단언이 통과한다. 아래 완료 단언이 그것을 잡는다.
 *
 * 이 하니스는 커널에 중단 지점이 없다는 것을 전제한다.
 */
internal fun <T> runKernelCounting(
    job: CountingJob,
    block: suspend () -> T,
): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context: CoroutineContext = job

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )

    return checkNotNull(outcome) {
        "커널이 중단했다 — 이 하니스는 중단 지점이 없는 커널만 검증한다"
    }.getOrThrow()
}
```

- [ ] **Step 4: 테스트를 돌려 통과를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests '*CountingJobTest'
```

기대: 6건 전부 PASS.

- [ ] **Step 5: ktlint 를 돌리고 커밋한다**

```bash
./gradlew :data:ktlintCheck
git add data/src/test/java/com/teamyg/parfait/data/repository/image/CountingJob.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/image/CountingJobTest.kt
git commit -m "test: 취소 확인 수를 세는 Job 더블과 실행 하니스를 세운다"
```

---

### Task 2: 커널 브랜치 프로덕션 전환과 기존 테스트 복구

`AlphaComponents.kt` 와 `AlphaPostProcessor.kt` 의 여덟 함수를 한 번에 전환한다. **나눌 수 없다** — `downscaleMask` 가 `suspend` 가 되는 순간 `postProcessAlpha` 도 `suspend` 여야 컴파일된다.

순수 리팩터링이라 새 기능 테스트를 먼저 쓰지 않는다. **기존 테스트 전부가 회귀 게이트다.**

커널 브랜치에서 이 여덟 함수를 부르는 파일은 아래 넷뿐이다. 이 태스크가 컴파일 경계를 닫는다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaComponents.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessor.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaComponentsTest.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessorTest.kt`

**브랜치:** `feature/segmentation-alpha-kernel`

**Interfaces:**
- Consumes: Task 1 의 `CountingJob`·`runKernelCounting`
- Produces:
  - `internal suspend fun downscaleMask(alpha: ByteArray, width: Int, height: Int, factor: Int, threshold: Int): BooleanArray`
  - `internal suspend fun applyAreaOpening(mask: BooleanArray, width: Int, height: Int, minPixels: Int): Boolean`
  - `internal suspend fun dilateMask(mask: BooleanArray, width: Int, height: Int): BooleanArray`
  - `internal suspend fun applyKeepMask(alpha: ByteArray, width: Int, height: Int, keep: BooleanArray, maskWidth: Int, factor: Int): Boolean`
  - `internal suspend fun measureAlpha(alpha: ByteArray, width: Int, height: Int): AlphaMeasurement?`
  - `internal suspend fun erodeEdge(alpha: ByteArray, width: Int, height: Int): Boolean`
  - `internal suspend fun postProcessAlpha(alpha: ByteArray, width: Int, height: Int, options: AlphaPostProcessOptions = AlphaPostProcessOptions()): AlphaPostProcessResult?`

- [ ] **Step 1: `AlphaComponents.kt` 의 import 를 추가한다**

이 파일에는 현재 import 가 하나도 없다. `package` 선언 다음에 빈 줄과 함께 넣는다.

```kotlin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
```

- [ ] **Step 2: `AlphaComponents.kt` 의 네 함수를 전환한다**

**`downscaleMask`** — 확인 루프가 있다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun downscaleMask(` → `internal suspend fun downscaleMask(`
- `val maskWidth = ceilDiv(width, factor)` **앞**에 `val job = currentCoroutineContext().job` 을 넣는다 (이 앵커는 파일 안에서 유일하다)
- 루프 안의 `checkCancelled()` → `job.ensureActive()`

**`applyAreaOpening`** — 확인 루프가 **없다.** 하위에 전달만 하므로 `Job` 을 꺼내지 않는다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun applyAreaOpening(` → `internal suspend fun applyAreaOpening(`
- `unionAdjacentRows(height, runStart, runEnd, rowFirstRun, parent, checkCancelled)` → `unionAdjacentRows(height, runStart, runEnd, rowFirstRun, parent)`

**`unionAdjacentRows`** — 확인 루프가 있다.

- 시그니처에서 `checkCancelled: () -> Unit,` 줄을 지운다
- `private fun unionAdjacentRows(` → `private suspend fun unionAdjacentRows(`
- `for (y in 0 until height - 1) {` **앞**에 `val job = currentCoroutineContext().job` 을 넣는다 (이 앵커는 유일하다)
- 루프 안의 `checkCancelled()` → `job.ensureActive()`

**`dilateMask`** — 확인 루프가 있다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun dilateMask(` → `internal suspend fun dilateMask(`
- `val dilated = BooleanArray(mask.size)` **앞**에 `val job = currentCoroutineContext().job` 을 넣는다 (유일하다)
- 루프 안의 `checkCancelled()` → `job.ensureActive()`

- [ ] **Step 3: `AlphaPostProcessor.kt` 의 import 를 추가한다**

`import com.teamyg.parfait.domain.model.SegmentationBounds` 아래에 넣는다.

```kotlin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
```

- [ ] **Step 4: `AlphaPostProcessor.kt` 의 네 함수를 전환한다**

⚠️ **`var changed = false` 는 이 파일에 두 곳 있다**(`applyKeepMask`·`erodeEdge`). 전체 치환하면 `erodeEdge` 에 `val job` 이 중복 선언된다. 반드시 함수를 보고 해당 자리만 고친다.

**`applyKeepMask`** — 확인 루프가 있다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun applyKeepMask(` → `internal suspend fun applyKeepMask(`
- **`applyKeepMask` 본문의** `var changed = false` 앞에 `val job = currentCoroutineContext().job` 을 넣는다
- 루프 안의 `checkCancelled()` → `job.ensureActive()`

**`measureAlpha`** — 확인 루프가 있다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun measureAlpha(` → `internal suspend fun measureAlpha(`
- `var left = Int.MAX_VALUE` 앞에 `val job = currentCoroutineContext().job` 을 넣는다 (유일하다)
- 루프 안의 `checkCancelled()` → `job.ensureActive()`

**`erodeEdge`** — 확인 루프가 있다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun erodeEdge(` → `internal suspend fun erodeEdge(`
- `if (width <= 0 || height <= 0) return false` **앞**에 `val job = currentCoroutineContext().job` 을 넣는다. 조기 반환 경로에서도 `Job` 부재를 잡기 위해서다. (같은 파일의 `return null` 판본과 구분된다)
- 루프 안의 `checkCancelled()` → `job.ensureActive()`

**`postProcessAlpha`** — 확인 루프가 **없다.** 하위에 전달만 하므로 `Job` 을 꺼내지 않는다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- KDoc 의 `@param checkCancelled 행 경계마다 불린다. 이 함수는 코루틴을 모르므로 호출부가 넣어 준다` 줄을 지운다
- KDoc 의 `@param alpha 길이가 ...` 줄 **앞**에 빈 주석 줄과 함께 아래를 넣는다

```
 * 행 경계마다 취소를 확인하고, 취소되면 `CancellationException` 을 던진다. 순수 CPU 루프라
 * 중단 지점이 없어서 `suspend` 표시만으로는 이 성질이 드러나지 않는다.
```

- `internal fun postProcessAlpha(` → `internal suspend fun postProcessAlpha(`
- 본문의 하위 호출 **여섯**에서 `checkCancelled` 인자를 지운다. 결과는 이렇게 된다.

```kotlin
    val mask = downscaleMask(alpha, width, height, factor, options.binaryThreshold)

    val minComponentPixels = maxOf(1, options.areaOpeningMinPixels / (factor * factor))
    if (!applyAreaOpening(mask, maskWidth, maskHeight, minComponentPixels)) return null

    val keep = dilateMask(mask, maskWidth, maskHeight)

    val applied = applyKeepMask(alpha, width, height, keep, maskWidth, factor)
    val eroded = options.erodeEdge && erodeEdge(alpha, width, height)
    val measured = measureAlpha(alpha, width, height) ?: return null
```

- [ ] **Step 5: 컴파일을 돌려 테스트가 깨지는 것을 확인한다**

```bash
./gradlew :data:compileDebugUnitTestKotlin
```

기대: FAIL. 테스트가 `suspend` 함수를 non-suspend 문맥에서 부른다.

- [ ] **Step 6: `AlphaComponentsTest.kt` 를 전환한다**

import 에 `import kotlinx.coroutines.test.runTest` 를 추가한다.

각 `@Test` 함수를 `= runTest { ... }` 형태로 바꾼다.

```kotlin
    @Test
    fun downscaleMask_someCase_doesSomething() = runTest {
```

단언과 픽스처는 손대지 않는다. 이 파일에는 취소 관련 테스트가 없다. `ceilDiv_*` 처럼 `suspend` 를 안 부르는 테스트도 함께 감싸도 무해하다.

- [ ] **Step 7: `AlphaPostProcessorTest.kt` 를 전환한다**

import 에 `import kotlinx.coroutines.test.runTest` 를 추가하고 각 `@Test` 를 `= runTest { ... }` 로 바꾼다. **다만 아래 둘은 예외다.**

**`postProcessAlpha_cancelledMidway_propagatesTheCallersThrow` 는 통째로 삭제한다.**
콜백에 `error("cancelled")` 를 심어 임의의 `IllegalStateException` 이 전파되는지 보는 테스트인데, 전환 후에는 호출부가 임의 예외를 주입할 통로 자체가 사라진다. Task 3 의 커널별 취소 테스트가 그 자리를 대신한다.

**`postProcessAlpha_countingCancelledCallback_isCalledPastTheDownscaleStage` 는 `CountingJob` 으로 옮긴다.** `runTest` 를 쓰지 않는다 — 하니스가 컨텍스트를 직접 지정하기 때문이다. **하한 단언을 그대로 유지한다.**

```kotlin
    @Test
    fun postProcessAlpha_countingChecks_isCalledPastTheDownscaleStage() {
        // Given — 첫 단계만 확인하고 마는 회귀를 잡는다. 8×8·배율1이면 각 단계가 행 수만큼(또는
        // 그 -1) 부른다. 한 단계를 통째로 지웠을 때의 최대치가 40이라 그 위를 요구한다
        val alpha = ByteArray(64) { 255.toByte() }
        val job = CountingJob()

        // When
        runKernelCounting(job) {
            postProcessAlpha(
                alpha,
                width = 8,
                height = 8,
                options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4),
            )
        }

        // Then
        assertTrue(job.calls > 40)
    }
```

원래 주석 중 "정확한 값으로 고정해"로 시작하는 문단은 이미 하한으로 완화된 상태와 어긋나므로 위 형태로 줄인다.

- [ ] **Step 8: 전체 테스트를 돌려 통과를 확인한다**

```bash
./gradlew :data:testDebugUnitTest
```

기대: PASS. 삭제한 한 건을 제외하고 기존 단언이 전부 살아 있어야 한다. 하나라도 깨지면 전환이 동작을 바꾼 것이므로 되돌아가 원인을 찾는다.

- [ ] **Step 9: ktlint 를 돌리고 커밋한다**

```bash
./gradlew :data:ktlintCheck
git add data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaComponents.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessor.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaComponentsTest.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessorTest.kt
git commit -m "refactor: 알파 커널의 취소 확인을 콜백에서 suspend 로 옮긴다"
```

---

### Task 3: 커널별 취소 테스트

각 커널이 취소를 존중하는지 함수 단위로 검증한다.

⚠️ **한계를 알고 쓴다.** 확인 루프가 없는 함수(`applyAreaOpening`·`postProcessAlpha`)의 취소 테스트는 **서브트리가 확인을 전부 잃었을 때만** 실패한다. 한 단계만 확인을 잃으면 다음 단계의 확인이 "첫 확인"이 되어 그대로 통과한다. 개별 단계의 회귀를 잡는 것은 Task 2 의 카운팅 테스트다. 그래서 Step 3 의 뮤테이션 확인이 필수다.

**Files:**
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaComponentsTest.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessorTest.kt`

**브랜치:** `feature/segmentation-alpha-kernel`

**Interfaces:**
- Consumes: Task 1 의 `CountingJob`·`runKernelCounting`, Task 2 의 `suspend` 커널들. **Task 2 없이는 컴파일되지 않는다.**

- [ ] **Step 1: `AlphaComponentsTest.kt` 에 취소 테스트 셋을 추가한다**

import 에 `import kotlinx.coroutines.CancellationException` 을 추가한다. 헬퍼 `alphaOf(vararg rows: String)` 과 `maskOf(vararg rows: String)` 은 이 파일에 이미 있다.

```kotlin
    @Test
    fun downscaleMask_cancelledAtSecondRow_throwsAndStops() {
        // Given — 4행짜리 판에서 둘째 확인 때 취소한다
        val alpha = alphaOf(
            "####",
            "####",
            "####",
            "####",
        )
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { downscaleMask(alpha, width = 4, height = 4, factor = 1, threshold = 127) }
        }
        // 확인이 행 루프 맨 위에 있으므로 4행을 다 돌지 못한다
        assertEquals(2, job.calls)
    }

    @Test
    fun applyAreaOpening_cancelledMidway_throws() {
        // Given — union 단계가 행 쌍마다 확인한다
        val mask = maskOf(
            "####",
            "####",
            "####",
            "####",
        )
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { applyAreaOpening(mask, width = 4, height = 4, minPixels = 1) }
        }
    }

    @Test
    fun dilateMask_cancelledMidway_throws() {
        // Given
        val mask = maskOf(
            "####",
            "####",
            "####",
            "####",
        )
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { dilateMask(mask, width = 4, height = 4) }
        }
    }
```

- [ ] **Step 2: 돌려서 통과를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests '*AlphaComponentsTest'
```

기대: 셋 다 PASS. Task 2 가 이미 구현했으므로 여기서는 **회귀 그물을 치는 것이 목적**이다. 하나라도 실패하면 Task 2 의 전환이 그 함수에서 빠진 것이므로 즉시 고친다.

- [ ] **Step 3: 확인 호출을 하나씩 지워 테스트가 잡는지 본다 (필수)**

`downscaleMask`·`unionAdjacentRows`·`dilateMask` 의 `job.ensureActive()` 를 **하나씩** 주석 처리하고 아래를 돌려, 대응하는 테스트가 실패하는지 본다. 셋 다 확인했으면 주석을 되돌린다.

```bash
./gradlew :data:testDebugUnitTest --tests '*AlphaComponentsTest' --tests '*AlphaPostProcessorTest'
```

주석 처리하면 `val job` 미사용 경고가 뜨지만 컴파일은 된다. 이 단계를 건너뛰면 아무것도 검증하지 않는 테스트가 초록으로 남을 수 있다.

- [ ] **Step 4: `AlphaPostProcessorTest.kt` 에 취소 테스트 넷을 추가한다**

import 에 `import kotlinx.coroutines.CancellationException` 을 추가한다.

```kotlin
    @Test
    fun applyKeepMask_cancelledMidway_throws() {
        // Given
        val alpha = ByteArray(16) { 255.toByte() }
        val keep = BooleanArray(16) { true }
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) {
                applyKeepMask(alpha, width = 4, height = 4, keep = keep, maskWidth = 4, factor = 1)
            }
        }
    }

    @Test
    fun measureAlpha_cancelledMidway_throws() {
        // Given
        val alpha = ByteArray(16) { 255.toByte() }
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { measureAlpha(alpha, width = 4, height = 4) }
        }
    }

    @Test
    fun erodeEdge_cancelledMidway_throws() {
        // Given
        val alpha = ByteArray(16) { 255.toByte() }
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { erodeEdge(alpha, width = 4, height = 4) }
        }
    }

    @Test
    fun postProcessAlpha_cancelledAtFirstCheck_throws() {
        // Given
        val alpha = ByteArray(64) { 255.toByte() }
        val job = CountingJob()
        job.cancelAfter = 0

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) {
                postProcessAlpha(
                    alpha,
                    width = 8,
                    height = 8,
                    options = AlphaPostProcessOptions(downscaleFactor = 1, areaOpeningMinPixels = 4),
                )
            }
        }
    }
```

- [ ] **Step 5: 돌리고 ktlint 를 통과시킨 뒤 커밋한다**

```bash
./gradlew :data:testDebugUnitTest
./gradlew :data:ktlintCheck
git add data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaComponentsTest.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessorTest.kt
git commit -m "test: 커널마다 취소를 존중하는지 개별로 검증한다"
```

---

### Task 4: 결선 브랜치 rebase 와 전환

`feature/segmentation-postprocess-wiring`(커밋 6개)을 새 커널 위로 rebase 하고, `maskSubjectAlpha` 와 `ImageSegmentationRepositoryImpl` 의 private 넷을 전환한다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationMaskTest.kt`

**브랜치:** `feature/segmentation-postprocess-wiring`

**Interfaces:**
- Consumes: Task 2 의 `suspend fun postProcessAlpha`
- Produces:
  - `internal suspend fun maskSubjectAlpha(mask: FloatBuffer, width: Int, height: Int, options: AlphaPostProcessOptions = AlphaPostProcessOptions()): MaskedAlpha?`

- [ ] **Step 1: rebase 를 끝까지 돌린다**

```bash
git checkout feature/segmentation-postprocess-wiring
git rebase feature/segmentation-alpha-kernel
git status
```

이 브랜치의 여섯 커밋은 Task 2 가 손댄 네 파일을 하나도 건드리지 않으므로 **충돌 없이 끝난다.** `git status` 에 "rebase in progress" 가 없어야 한다.

⚠️ 이 시점의 트리는 컴파일되지 않는다. `checkCancelled` 를 넘기는 코드가 남아 있다. 정상이다. 아래에서 새 커밋으로 고친다.

- [ ] **Step 2: `SegmentationMask.kt` 의 `maskSubjectAlpha` 를 전환한다**

이 함수에는 자체 확인 루프가 없다. import 는 추가하지 않는다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun maskSubjectAlpha(` → `internal suspend fun maskSubjectAlpha(`
- `postProcessAlpha(alpha, width, height, options, checkCancelled)` 에서 **다섯 번째 위치 인자** `checkCancelled` 를 지운다. 이 자리는 명명 인자가 아니다

- [ ] **Step 3: `ImageSegmentationRepositoryImpl.kt` 의 private 넷을 전환한다**

넷 다 자체 확인 루프가 없고 하위에 전달만 한다. `suspend` 를 붙이고 파라미터를 지운다. 아래는 형태만 보인 것이고 **실제 선언은 ktlint 규칙대로 멀티라인을 유지한다.**

```kotlin
private suspend fun SubjectSegmentationResult.toCandidatePairs(origin: Bitmap): List<CandidatePair>
private suspend fun buildCandidatePair(subject: Subject, bitmap: Bitmap, origin: Bitmap): CandidatePair
private suspend fun postProcess(subject: Subject, bitmap: Bitmap, origin: Bitmap): SegmentationCandidate?
private suspend fun SubjectSegmentationResult.toForegroundCandidate(origin: Bitmap): List<SegmentationCandidate>
```

각 본문에서 하위 호출로 넘기던 `checkCancelled` 인자를 지운다. 이 파일 쪽은 `checkCancelled = checkCancelled` 명명 인자다.

as-built: `catch (e: CancellationException) { throw e }` 는 `buildCandidatePair` 안이 아니라 호출부
`segmentImage`·`segmentForeground` 의 `try` 에 있다. `buildCandidatePair` 자신은 `catch (e: OutOfMemoryError)`
만 갖는다. 두 자리 다 전환 후에도 같은 의미로 동작하므로 그대로 둔다.

- [ ] **Step 4: 콜백 생성 두 곳을 지운다**

`segmentImage` 안은 두 줄이다.

```kotlin
                val job = currentCoroutineContext()[Job]
                val checkCancelled: () -> Unit = { job?.ensureActive() }
```

`segmentForeground` 안은 **사이에 설명 주석이 있어 세 줄이다.** 주석까지 함께 지운다. 남기면 대상이 사라진 거짓 주석이 된다.

```kotlin
                val job = currentCoroutineContext()[Job]
                // 타입을 명시하지 않으면 `() -> Unit?` 으로 추론돼 `() -> Unit` 자리에 못 들어간다
                val checkCancelled: () -> Unit = { job?.ensureActive() }
```

두 곳 모두 뒤따르는 호출에서 `checkCancelled` 인자를 뺀다.

`withContext(Dispatchers.Default) { ... }` 블록 자체는 그대로 둔다. 그 블록이 커널이 볼 `Job` 을 만든다.

쓰이지 않게 된 import 를 지운다. `kotlinx.coroutines.Job`·`kotlinx.coroutines.currentCoroutineContext`·`kotlinx.coroutines.ensureActive` 가 이 파일의 다른 자리에서 안 쓰이면 함께 지운다. `withContext`·`Dispatchers`·`CancellationException` 은 남는다.

- [ ] **Step 5: 컴파일해서 남은 자리를 찾는다**

```bash
./gradlew :data:compileDebugKotlin
```

기대: PASS. 실패하면 `checkCancelled` 가 남은 자리다.

- [ ] **Step 6: `SegmentationMaskTest.kt` 를 전환하고 취소 테스트를 추가한다**

import 에 `import kotlinx.coroutines.test.runTest` 와 `import kotlinx.coroutines.CancellationException` 을 추가한다.

`maskSubjectAlpha` 를 부르는 테스트를 `= runTest { ... }` 로 바꾼다. 단언과 픽스처는 손대지 않는다.

취소 테스트를 추가한다. 이 파일의 기존 헬퍼 `confidenceBuffer(values: FloatArray)` 와 상수 `TEST_OPTIONS` 를 그대로 쓴다.

```kotlin
    @Test
    fun maskSubjectAlpha_cancelledAtFirstCheck_throws() {
        // Given
        val values = FloatArray(64) { 1f }
        val job = CountingJob()
        job.cancelAfter = 0

        // When · Then — maskSubjectAlpha 자체에는 확인 루프가 없다. postProcessAlpha 아래
        // downscaleMask 의 첫 행에서 걸린다
        assertFailsWith<CancellationException> {
            runKernelCounting(job) {
                maskSubjectAlpha(confidenceBuffer(values), width = 8, height = 8, options = TEST_OPTIONS)
            }
        }
    }
```

- [ ] **Step 7: 테스트와 ktlint 를 돌린다**

```bash
./gradlew :data:testDebugUnitTest
./gradlew :data:ktlintCheck
```

기대: PASS.

- [ ] **Step 8: 새 커밋으로 얹는다**

rebase 는 Step 1 에서 이미 끝났다. `git rebase --continue` 를 부르지 않는다.

```bash
git add data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationMaskTest.kt
git commit -m "refactor: 결선 경로의 취소 확인을 suspend 로 옮긴다"
```

⚠️ 이 브랜치는 이미 리모트에 있다. force push 가 필요하지만 **여기서 하지 않는다.**

---

### Task 5: 정련 브랜치 rebase 와 전환

`feature/segmentation-alpha-refinement`(커밋 8개)를 rebase 하고 나머지를 전환한다.

⚠️ **이 태스크의 함정 둘.**

1. **`refineWithin` 은 커널 브랜치에 없다.** 정련 커밋이 `AlphaPostProcessor.kt` 에 추가하므로, Task 2 가 이미 손댄 파일을 여기서 다시 건드린다.
2. **정련 커밋이 Task 4 가 지운 `checkCancelled` 를 되살린다.** `SegmentationMask.kt` 와 `ImageSegmentationRepositoryImpl.kt` 를 다시 고쳐야 한다. rebase 중간에 고치면 뒤 커밋이 재생되며 또 살아나므로, **rebase 를 끝까지 돌린 뒤** 한 번에 고친다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaRefine.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessor.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaRefineTest.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/repository/image/AlphaPostProcessorTest.kt`

**브랜치:** `feature/segmentation-alpha-refinement`

**Interfaces:**
- Consumes: Task 2·4 의 `suspend` 커널들
- Produces:
  - `internal suspend fun boxMean(src: FloatArray, width: Int, height: Int, radius: Int): FloatArray`
  - `internal suspend fun refineAlpha(alpha: ByteArray, guidance: IntArray, width: Int, height: Int, downscale: Int, radius: Int, epsilon: Float): Boolean`

- [ ] **Step 1: rebase 를 끝까지 돌린다**

```bash
git checkout feature/segmentation-alpha-refinement
git rebase feature/segmentation-postprocess-wiring
```

⚠️ 여덟 커밋 중 뒤쪽 셋이 Task 2·4 가 고친 파일을 건드리므로 **rebase 가 여러 번 멈출 수 있다.** 멈출 때마다 **상위 커밋 원본을 그대로 채택하고** 계속한다. 여기서 전환하지 않는다.

```bash
git checkout --theirs <충돌 파일>   # 필요하면. 판단이 서지 않으면 파일을 열어 정련 커밋 쪽 내용을 남긴다
git add <충돌 파일>
git rebase --continue
```

끝나면 확인한다.

```bash
git status   # "rebase in progress" 가 없어야 한다
```

이 시점의 트리는 컴파일되지 않는다. 정상이다.

- [ ] **Step 2: `AlphaRefine.kt` 의 import 를 추가한다**

`import kotlin.math.roundToInt` 위에 넣는다.

```kotlin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
```

- [ ] **Step 3: `AlphaRefine.kt` 의 확인 루프 보유 함수를 전환한다**

**`boxMean`** — 확인 루프가 **둘**(행 방향·열 방향)이다. `Job` 은 한 번만 꺼낸다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun boxMean(` → `internal suspend fun boxMean(`
- `val horizontal = FloatArray(src.size)` 앞에 `val job = currentCoroutineContext().job` 을 넣는다 (유일하다)
- 두 루프 안의 `checkCancelled()` → `job.ensureActive()`

**`downscale`** — 확인 루프가 있고 `private inline fun` 이다. `private suspend inline fun` 으로 컴파일되는 것을 확인했다. 인라인 람다 파라미터에 `noinline`·`crossinline` 이 필요하지 않다.

- 시그니처에서 `checkCancelled: () -> Unit,` 줄을 지운다
- `private inline fun downscale(` → `private suspend inline fun downscale(`
- `val subWidth = ceilDiv(width, factor)` 앞에 `val job = currentCoroutineContext().job` 을 넣는다 (`ceilDiv(width, downscale)` 판본과 구분된다)
- 루프 안의 `checkCancelled()` → `job.ensureActive()`
- 값 추출 람다 `value: (Int) -> Float` 은 그대로 둔다

**`guidedCoefficients`** — 계수 루프에 확인이 있고 `boxMean` 을 여섯 번 부른다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun guidedCoefficients(` → `internal suspend fun guidedCoefficients(`
- `val meanGuidance = boxMean(` 앞에 `val job = currentCoroutineContext().job` 을 넣는다 (유일하다)
- `boxMean` 호출 여섯에서 `checkCancelled` 인자를 지운다
- 계수 루프 안의 `checkCancelled()` → `job.ensureActive()`

**`applyCoefficients`** — 확인 루프가 있다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다
- `internal fun applyCoefficients(` → `internal suspend fun applyCoefficients(`
- `var changed = false` 앞에 `val job = currentCoroutineContext().job` 을 넣는다 (이 앵커는 `AlphaRefine.kt` 안에서 유일하다)
- 루프 안의 `checkCancelled()` → `job.ensureActive()`

- [ ] **Step 4: `AlphaRefine.kt` 의 전달 전용 함수를 전환한다**

확인 루프가 없다. `suspend` 만 붙이고 인자를 지운다.

- **`downscaleLuminance`** · **`downscaleAlpha`** — `internal fun` → `internal suspend fun`, 시그니처에서 `checkCancelled` 제거, `downscale(width, height, factor, checkCancelled) { ... }` → `downscale(width, height, factor) { ... }`
- **`refineAlpha`** — `internal fun` → `internal suspend fun`, 시그니처에서 `checkCancelled` 제거, `downscaleLuminance`·`downscaleAlpha`·`guidedCoefficients`·`applyCoefficients` 호출에서 `checkCancelled` 인자를 지운다

- [ ] **Step 5: `AlphaPostProcessor.kt#refineWithin` 을 전환한다**

- 시그니처에서 `checkCancelled: () -> Unit,` 줄을 지운다
- `private fun refineWithin(` → `private suspend fun refineWithin(`
- `refineAlpha(...)` 호출에서 `checkCancelled = checkCancelled` 인자를 지운다

같은 파일의 `postProcessAlpha` 에서 `refineWithin(alpha, width, beforeRefine.bounds, guidance, options, checkCancelled)` → `refineWithin(alpha, width, beforeRefine.bounds, guidance, options)` 로 바꾼다. 정련 브랜치에는 `measureAlpha` 호출이 둘이므로 둘 다 인자가 없는지 확인한다.

- [ ] **Step 6: 정련 커밋이 되살린 `checkCancelled` 를 다시 제거한다**

Task 4 에서 이미 고친 두 파일이 rebase 로 되돌아왔다.

**`SegmentationMask.kt`** — 정련 커밋이 `guidance: GuidanceProvider? = null,` 을 `checkCancelled` **앞에** 추가하고 호출을 명명 인자로 바꿨다.

- 시그니처에서 `checkCancelled: () -> Unit = {},` 줄을 지운다. `guidance` 파라미터는 **남긴다**
- `internal fun maskSubjectAlpha(` → `internal suspend fun maskSubjectAlpha(`
- `postProcessAlpha(...)` 호출에서 `checkCancelled = checkCancelled,` 를 지운다. `guidance = guidance` 는 남긴다

**`ImageSegmentationRepositoryImpl.kt`** — `postProcess` 의 `postProcessAlpha(..., guidance = {...}, checkCancelled = checkCancelled)` 와 `toForegroundCandidate` 의 `maskSubjectAlpha(..., guidance = {...}, checkCancelled = checkCancelled)` 에서 `checkCancelled` 인자를 지운다. Task 4 에서 `suspend` 를 붙인 private 넷은 rebase 로 유지되지만, 되돌아왔다면 다시 붙인다.

- [ ] **Step 7: 컴파일한다**

```bash
./gradlew :data:compileDebugKotlin
```

기대: PASS. 실패하면 `checkCancelled` 가 남은 자리다.

- [ ] **Step 8: `AlphaPostProcessorTest.kt` 에서 정련 커밋이 추가한 테스트 셋을 전환한다**

정련 커밋이 이 파일에 `@Test` 셋을 새로 넣었다(`postProcessAlpha_refineEnabledWithGuidance_producesPartialAlpha` 등 정련 관련). 셋을 `= runTest { ... }` 로 바꾼다. Task 2 에서 전환한 나머지는 rebase 로 유지된다.

- [ ] **Step 9: `AlphaRefineTest.kt` 를 전환하고 취소 테스트를 추가한다**

이 파일의 기존 import 는 `kotlin.math.abs`·`kotlin.test.Test`·`assertEquals`·`assertTrue` 넷뿐이다. **`assertFailsWith` 가 없으므로 함께 추가한다.**

```kotlin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.assertFailsWith
```

기존 `@Test` 를 `= runTest { ... }` 로 바꾼다. 그리고 취소 테스트를 추가한다.

```kotlin
    @Test
    fun boxMean_cancelledMidway_throws() {
        // Given
        val src = FloatArray(16) { 1f }
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) { boxMean(src, width = 4, height = 4, radius = 1) }
        }
    }

    @Test
    fun guidedCoefficients_cancelledMidway_throws() {
        // Given
        val guidance = FloatArray(16) { 0.5f }
        val input = FloatArray(16) { 0.5f }
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) {
                guidedCoefficients(guidance, input, width = 4, height = 4, radius = 1, epsilon = 1e-4f)
            }
        }
    }

    @Test
    fun applyCoefficients_cancelledMidway_throws() {
        // Given
        val alpha = ByteArray(16) { 255.toByte() }
        val guidance = IntArray(16) { 0xFF808080.toInt() }
        val coefficients = GuidedCoefficients(a = FloatArray(16), b = FloatArray(16) { 1f })
        val job = CountingJob()
        job.cancelAfter = 1

        // When · Then
        assertFailsWith<CancellationException> {
            runKernelCounting(job) {
                applyCoefficients(
                    alpha = alpha,
                    guidance = guidance,
                    coefficients = coefficients,
                    width = 4,
                    height = 4,
                    subWidth = 4,
                    subHeight = 4,
                    factor = 1,
                )
            }
        }
    }
```

- [ ] **Step 10: `refineAlpha` 카운팅 테스트를 만든다 (필수)**

⚠️ **이것이 정련 브랜치의 유일한 개별 회귀 그물이다.** 위 취소 테스트들은 `refineAlpha` 서브트리가 확인을 **전부** 잃었을 때만 실패한다. `downscale` 하나의 확인이 사라져도 전부 초록으로 남는 것을 실험으로 확인했다.

먼저 실제 확인 횟수를 잰다. 아래 테스트를 넣고 `assertTrue(job.calls > 0)` 으로 한 번 돌려 로그나 실패 메시지로 실제 값을 읽는다.

```kotlin
    @Test
    fun refineAlpha_countingChecks_visitsEveryStage() {
        // Given — 4×4·배율1·반경1. boxMean 여섯 번(각 행·열 두 루프)·downscale 둘·계수 루프·
        // applyCoefficients 가 각각 확인한다
        val alpha = ByteArray(16) { 255.toByte() }
        val guidance = IntArray(16) { 0xFF808080.toInt() }
        val job = CountingJob()

        // When
        runKernelCounting(job) {
            refineAlpha(
                alpha = alpha,
                guidance = guidance,
                width = 4,
                height = 4,
                downscale = 1,
                radius = 1,
                epsilon = 1e-4f,
            )
        }

        // Then — 실제 값을 재서 채우고, 한 단계를 통째로 지웠을 때의 최대치 위로 하한을 건다
        assertTrue(job.calls > MEASURED_LOWER_BOUND)
    }
```

값을 정하는 절차는 이렇다.

1. 위 테스트를 `assertEquals(0, job.calls)` 로 두고 돌려, 실패 메시지에서 **실제 총합**을 읽는다
2. `downscale`·`boxMean` 의 두 루프·`guidedCoefficients` 계수 루프·`applyCoefficients` 의 확인을 **하나씩** 지우고 각각의 총합을 잰다
3. 그중 **가장 큰 값**을 하한으로 쓴다. 그러면 어느 하나를 지워도 잡힌다
4. 주석에 산식을 남긴다

- [ ] **Step 11: 확인 호출을 하나씩 지워 그물을 검증한다 (필수)**

Step 10 의 하한이 실제로 작동하는지 본다. `AlphaRefine.kt` 의 확인 지점을 하나씩 주석 처리하고 아래를 돌려 **Step 10 의 테스트가 실패하는지** 확인한다. 전부 확인했으면 되돌린다.

```bash
./gradlew :data:testDebugUnitTest --tests '*AlphaRefineTest'
```

- [ ] **Step 12: 전체 테스트와 ktlint 를 돌리고 새 커밋으로 얹는다**

```bash
./gradlew :data:testDebugUnitTest
./gradlew :data:ktlintCheck
git add -A
git commit -m "refactor: 정련 커널의 취소 확인을 suspend 로 옮긴다"
```

⚠️ force push 는 여기서 하지 않는다.

---

### Task 6: 문서 갱신과 as-built 기록

**Files:**
- Modify: `parfait/specs/2026-08-27-alpha-kernel-suspend-cancellation.md`
- Modify: `parfait/plans/README.md`

**저장소:** 이 계획 문서가 있는 저장소(`team-yg-pesonal-agent`)

- [ ] **Step 1: 스펙의 `status` 를 갱신한다**

frontmatter 의 `status: draft` 를 구현 결과에 맞게 바꾼다. 구현 중 스펙과 달라진 것이 있으면 그 자리를 고치고 **왜 달라졌는지**를 남긴다.

- [ ] **Step 2: 스펙의 「자체 확인 / 전염」 표를 as-built 로 맞춘다**

아래 넷은 확인 루프가 없고 하위에 전달만 한다. 스펙 표는 이들을 「자체 확인」열에 두었으므로 「전염」으로 옮긴다.

- `applyAreaOpening`
- `postProcessAlpha`
- `downscaleLuminance`
- `downscaleAlpha`

- [ ] **Step 3: 스펙의 테스트 절에 개별 그물의 한계를 남긴다**

확인 루프가 없는 함수의 취소 테스트는 서브트리가 확인을 전부 잃었을 때만 실패한다. 개별 단계의 회귀를 잡는 것은 카운팅 테스트뿐이라는 사실을 적는다. 이 성질을 모르면 다음 사람이 카운팅 테스트를 "중복"으로 보고 지운다.

- [ ] **Step 4: `parfait/plans/README.md` 에 이 계획을 등록한다**

기존 행들의 밀도를 따라 한 줄을 추가한다. 태스크 수, 브랜치 셋, 그리고 검수가 잡은 결함을 남긴다.

- [ ] **Step 5: 커밋한다**

```bash
git add parfait/specs/2026-08-27-alpha-kernel-suspend-cancellation.md parfait/plans/README.md
git commit -m "docs: 알파 커널 취소 확인 전환의 as-built 를 남긴다"
```

---

## 남은 수동 작업

계획에 포함하지 않은 것들이다. 태스크가 다 끝난 뒤 사용자와 함께 처리한다.

- **force push 3회.** 커널·결선·정련 브랜치가 전부 rebase 되므로 필요하다. 리모트로 나가는 작업이라 승인이 먼저다.
- **리뷰 스레드 회신.** 리뷰가 달린 PR 에 전환 결과와 근거(`yield` 를 배제한 측정)를 남긴다.
