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
