package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSource
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
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
        private val failure: Throwable? = null,
    ) : ParfaitRemoteDataSource {
        var todayCallCount = 0
            private set
        var detailCallCount = 0
            private set

        override suspend fun getYears(groupId: GroupId): Result<List<Int>> = error("폴러가 부르지 않는다")

        override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> {
            todayCallCount++
            gate?.await()
            return failure?.let { Result.failure(it) } ?: Result.success(response)
        }

        override suspend fun getPastCanvases(
            groupId: GroupId,
            from: LocalDate?,
            to: LocalDate?,
        ): Result<List<PastCanvasVO>> = error("폴러가 부르지 않는다")

        override suspend fun getCanvasDetail(
            groupId: GroupId,
            parfaitId: ParfaitId,
        ): Result<CanvasVO> {
            detailCallCount++
            gate?.await()
            return failure?.let { Result.failure(it) } ?: Result.success(response)
        }

        override suspend fun changeCanvasBackground(
            groupId: GroupId,
            parfaitId: ParfaitId,
            background: CanvasBackgroundEdit,
        ): Result<CanvasBackground?> = error("폴러가 부르지 않는다")
    }

    @Test
    fun refresh_whenItFails_signalsTheFailure() = runTest {
        val remote = FakeRemote(canvas(), failure = IllegalStateException("실패"))
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())
        val failures = mutableListOf<GroupId>()
        backgroundScope.launch { poller.refreshFailures.collect { failures += it } }
        runCurrent()

        poller.acquire(GROUP)
        runCurrent()

        // 실패하면 캐시에 아무것도 실리지 않아 구독만 보는 화면은 그 사실을 알 길이 없다
        assertEquals(listOf(GROUP), failures)
    }

    @Test
    fun refresh_afterStopAll_saysNothing() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = FakeRemote(canvas(), gate = gate, failure = IllegalStateException("실패"))
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())
        val failures = mutableListOf<GroupId>()
        backgroundScope.launch { poller.refreshFailures.collect { failures += it } }
        runCurrent()

        poller.acquire(GROUP)
        runCurrent()
        poller.stopAll()
        gate.complete(Unit)
        runCurrent()

        // 세션이 끝난 뒤 도착한 실패다 — 이미 버려진 갱신의 것이라 화면에 알리지 않는다
        assertEquals(emptyList(), failures)
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

    /**
     * 호출 순서대로 다른 [CompletableDeferred] 에 붙들리는 오늘 조회 페이크. 세대가 다른 두
     * 요청을 각자 원하는 시점에 풀어야 하는 경합 테스트 전용이라 [FakeRemote] 를 재사용하지
     * 않는다.
     */
    private class SequencedGateRemote(
        private val response: CanvasVO,
        private val gates: List<CompletableDeferred<Unit>>,
    ) : ParfaitRemoteDataSource {
        var todayCallCount = 0
            private set

        override suspend fun getYears(groupId: GroupId): Result<List<Int>> = error("폴러가 부르지 않는다")

        override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> {
            val gate = gates[todayCallCount]
            todayCallCount++
            gate.await()
            return Result.success(response)
        }

        override suspend fun getPastCanvases(
            groupId: GroupId,
            from: LocalDate?,
            to: LocalDate?,
        ): Result<List<PastCanvasVO>> = error("폴러가 부르지 않는다")

        override suspend fun getCanvasDetail(
            groupId: GroupId,
            parfaitId: ParfaitId,
        ): Result<CanvasVO> = error("폴러가 부르지 않는다")

        override suspend fun changeCanvasBackground(
            groupId: GroupId,
            parfaitId: ParfaitId,
            background: CanvasBackgroundEdit,
        ): Result<CanvasBackground?> = error("폴러가 부르지 않는다")
    }

    @Test
    fun stopAll_thenReacquire_lateResponseFromPreviousGenerationDoesNotClearNewGenerationInProgressFlag() = runTest {
        val gateGen0 = CompletableDeferred<Unit>()
        val gateGen1 = CompletableDeferred<Unit>()
        val remote = SequencedGateRemote(canvas(), listOf(gateGen0, gateGen1))
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        assertEquals(1, remote.todayCallCount)

        // 이전 세대의 요청이 아직 걸려 있는 채로 stopAll 뒤 곧바로 재구독한다
        poller.stopAll()
        poller.acquire(GROUP)
        runCurrent()
        assertEquals(2, remote.todayCallCount)

        // 이전 세대(gen0)의 지연 응답이 뒤늦게 도착한다
        gateGen0.complete(Unit)
        runCurrent()

        // gen1이 아직 진행 중인데 세 번째 트리거가 들어와도 "진행 중" 가드에 걸려 건너뛰어야 한다
        poller.refreshNow(GROUP)
        runCurrent()
        assertEquals(2, remote.todayCallCount)

        gateGen1.complete(Unit)
        runCurrent()
    }
}
