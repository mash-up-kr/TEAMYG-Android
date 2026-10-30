package com.teamyg.parfait.data.poller

import com.teamyg.parfait.data.source.parfait.local.CanvasLocalDataSourceImpl
import com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSource
import com.teamyg.parfait.data.model.entity.TodayCanvasFetchEntity
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

private val GROUP = GroupId(1L)

class CanvasPollerTest {
    private fun canvas(date: LocalDate = parfaitToday()) = CanvasVO(
        parfaitId = ParfaitId(100L),
        groupName = GroupName("아메리카노"),
        date = date,
        status = CanvasStatus.ACTIVE,
        lastClosedDate = null,
        members = emptyList(),
        background = null,
        toppings = emptyList(),
        texts = emptyList(),
    )

    /**
     * [gate] 를 주면 응답을 붙들어 둔다 — 중첩 가드와 정리 경합을 재현하는 데 쓴다.
     * 폴러가 부르지 않는 메서드는 `error("폴러가 부르지 않는다")` 로 채운다.
     */
    private class FakeRemote(
        initial: CanvasVO,
        private val gate: CompletableDeferred<Unit>? = null,
        private val failure: Throwable? = null,
    ) : ParfaitRemoteDataSource {
        private var response = initial
        private var etag: String? = "W/\"v1\""

        /** 보낸 ETag 가 현재 값과 같으면 304 라, 본문만 바꾸면 가짜로 통과한다. 둘을 함께 바꾼다 */
        fun serve(
            canvas: CanvasVO,
            etag: String?,
        ) {
            response = canvas
            this.etag = etag
        }

        var callCount = 0
            private set
        val receivedEtags = mutableListOf<String?>()

        override suspend fun getYears(groupId: GroupId): Result<List<Int>> = error("폴러가 부르지 않는다")

        override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> = error("폴러가 부르지 않는다")

        override suspend fun getTodayCanvasIfChanged(
            groupId: GroupId,
            etag: String?,
        ): Result<TodayCanvasFetchEntity> {
            callCount++
            receivedEtags += etag
            gate?.await()
            failure?.let { return Result.failure(it) }
            return Result.success(
                when (etag != null && etag == this.etag) {
                    true -> TodayCanvasFetchEntity.NotModified
                    false -> TodayCanvasFetchEntity.Modified(response, this.etag)
                },
            )
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
    fun acquire_callsOnceImmediatelyWithoutAnEtag() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()

        assertEquals(listOf<String?>(null), remote.receivedEtags)
    }

    @Test
    fun poll_whenNothingChanges_stretchesTheInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        val afterFirst = remote.callCount

        // 첫 주기 10초, 그다음 15초 — 24초로는 두 번째 주기가 아직 차지 않는다
        advanceTimeBy(24.seconds)
        runCurrent()

        // 두 번째 요청은 저장된 ETag 를 실어 304 를 받은 회차다
        assertEquals(listOf(null, "W/\"v1\""), remote.receivedEtags)
        assertEquals(afterFirst + 1, remote.callCount)
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
        remote.serve(canvas().copy(lastClosedDate = LocalDate(2026, 1, 1)), etag = "W/\"v2\"")
        advanceTimeBy(15.seconds)
        runCurrent()
        val afterChange = remote.callCount

        // 변화를 봤으니 10초로 돌아온다 — 15초짜리 주기였다면 아직 안 나갔을 시점이다
        advanceTimeBy(11.seconds)
        runCurrent()

        assertEquals(afterChange + 1, remote.callCount)
    }

    @Test
    fun poll_whenItFails_doesNotStretchTheInterval() = runTest {
        val remote = FakeRemote(canvas(), failure = IllegalStateException("실패"))
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        val afterFirst = remote.callCount

        // 실패를 변화 없음으로 세면 두 번째 주기가 15초가 되어 24초에 두 번이 안 나간다
        advanceTimeBy(24.seconds)
        runCurrent()

        assertEquals(afterFirst + 2, remote.callCount)
    }

    @Test
    fun poll_afterTheCacheIsWarm_sendsTheStoredEtagAndKeepsTheCacheOn304() = runTest {
        val remote = FakeRemote(canvas())
        val local = CanvasLocalDataSourceImpl()
        val poller = CanvasPoller(backgroundScope, remote, local)

        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(10.seconds)
        runCurrent()

        assertEquals(listOf(null, "W/\"v1\""), remote.receivedEtags)
        assertEquals(canvas(), local.cachedTodayCanvas(GROUP))
        assertEquals("W/\"v1\"", local.cachedTodayEtag(GROUP))
    }

    @Test
    fun poll_whenTheServerSendsNoEtag_keepsAskingWithoutOne() = runTest {
        val remote = FakeRemote(canvas())
        remote.serve(canvas(), etag = null)
        val local = CanvasLocalDataSourceImpl()
        val poller = CanvasPoller(backgroundScope, remote, local)

        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(10.seconds)
        runCurrent()

        assertEquals(listOf<String?>(null, null), remote.receivedEtags)
        assertEquals(canvas(), local.cachedTodayCanvas(GROUP))
    }

    @Test
    fun poll_whenTheServerChanges_replacesTheCanvasAndTheEtagTogether() = runTest {
        val remote = FakeRemote(canvas())
        val local = CanvasLocalDataSourceImpl()
        val poller = CanvasPoller(backgroundScope, remote, local)
        poller.acquire(GROUP)
        runCurrent()

        val changed = canvas().copy(lastClosedDate = LocalDate(2026, 1, 1))
        remote.serve(changed, etag = "W/\"v2\"")
        advanceTimeBy(10.seconds)
        runCurrent()

        assertEquals(changed, local.cachedTodayCanvas(GROUP))
        assertEquals("W/\"v2\"", local.cachedTodayEtag(GROUP))
    }

    @Test
    fun poll_whenTheCachedDateIsStale_asksWithoutAnEtag() = runTest {
        val yesterday = parfaitToday().minus(DatePeriod(days = 1))
        val remote = FakeRemote(canvas(yesterday))
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(10.seconds)
        runCurrent()

        // 어제 캔버스를 304 로 굳히지 않는다 — 하루 경계를 넘긴 뒤에는 반드시 본문으로 바꾼다
        assertEquals(listOf<String?>(null, null), remote.receivedEtags)
    }

    @Test
    fun acquire_twice_stillCallsOncePerInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        poller.acquire(GROUP)
        runCurrent()
        advanceTimeBy(10.seconds)
        runCurrent()

        assertEquals(2, remote.callCount)
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

        assertEquals(1, remote.callCount)
    }

    @Test
    fun refreshNow_sendsExactlyOneRequest() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        val before = remote.callCount

        poller.refreshNow(GROUP)
        runCurrent()

        assertEquals(before + 1, remote.callCount)
    }

    @Test
    fun refreshNow_restartsTheInterval() = runTest {
        val remote = FakeRemote(canvas())
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()

        advanceTimeBy(9.seconds)
        poller.refreshNow(GROUP)
        runCurrent()
        val afterForced = remote.callCount

        // 원래 주기였다면 1초 뒤에 한 번 더 나갔어야 한다
        advanceTimeBy(2.seconds)
        runCurrent()

        assertEquals(afterForced, remote.callCount)
    }

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
        val afterForced = remote.callCount

        // 되돌렸으니 10초에 나간다 — 되돌리지 않았다면 아직이다
        advanceTimeBy(11.seconds)
        runCurrent()

        assertEquals(afterForced + 1, remote.callCount)
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
        val afterReacquire = remote.callCount

        // 단계가 남아 있었다면 15초짜리라 아직 안 나갔을 시점이다
        advanceTimeBy(11.seconds)
        runCurrent()

        assertEquals(afterReacquire + 1, remote.callCount)
    }

    @Test
    fun refresh_whileAnotherIsInFlight_skipsThisRound() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = FakeRemote(canvas(), gate)
        val poller = CanvasPoller(backgroundScope, remote, CanvasLocalDataSourceImpl())

        poller.acquire(GROUP)
        runCurrent()
        assertEquals(1, remote.callCount)

        // 첫 요청이 아직 안 끝난 채로 주기를 두 번 민다
        advanceTimeBy(25.seconds)
        runCurrent()

        assertEquals(1, remote.callCount)

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
        var callCount = 0
            private set

        override suspend fun getYears(groupId: GroupId): Result<List<Int>> = error("폴러가 부르지 않는다")

        override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> = error("폴러가 부르지 않는다")

        override suspend fun getTodayCanvasIfChanged(
            groupId: GroupId,
            etag: String?,
        ): Result<TodayCanvasFetchEntity> {
            val gate = gates[callCount]
            callCount++
            gate.await()
            return Result.success(TodayCanvasFetchEntity.Modified(response, null))
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
        assertEquals(1, remote.callCount)

        // 이전 세대의 요청이 아직 걸려 있는 채로 stopAll 뒤 곧바로 재구독한다
        poller.stopAll()
        poller.acquire(GROUP)
        runCurrent()
        assertEquals(2, remote.callCount)

        // 이전 세대(gen0)의 지연 응답이 뒤늦게 도착한다
        gateGen0.complete(Unit)
        runCurrent()

        // gen1이 아직 진행 중인데 세 번째 트리거가 들어와도 "진행 중" 가드에 걸려 건너뛰어야 한다
        poller.refreshNow(GROUP)
        runCurrent()
        assertEquals(2, remote.callCount)

        gateGen1.complete(Unit)
        runCurrent()
    }

    /**
     * [CanvasPoller.refreshNow] 의 구독 확인과 재시작이 갈라져 있으면, 그 사이에 마지막
     * [CanvasPoller.release] 가 끼어들어 구독자가 없는데도 폴 잡이 살아남을 수 있다. 이 창은
     * 몇 CPU 명령어만큼만 벌어져 있어 그냥 두 스레드를 동시에 띄우는 정도로는 좀처럼 맞지
     * 않는다 — 가상 시간으로도 재현 못 한다, [synchronized] 두 번 사이에는 코루틴이 서스펜드할
     * 지점이 없기 때문이다.
     *
     * 그래서 내부 [CanvasPoller] 락을 리플렉션으로 미리 쥔 채로 [refreshNow]·[release] 를
     * 각자 스레드에서 띄운다 — 두 스레드 모두 락을 기다리며 멈춰(BLOCKED) 선 뒤에야 놓아줘야
     * 정확히 같은 줄에서 출발한다. "진행 중" 가드([refreshing])도 리플렉션으로 미리 채워
     * [refreshNow] 의 내부 갱신이 원격 호출 없이 곧장 결정 지점까지 가게 한다 — 그래야
     * [release] 가 노려야 하는 마지막 틈이 몇 번 안 되는 시도 중 하나로 좁혀져 창을 두드릴
     * 확률이 늘어난다.
     */
    @Test
    fun refreshNowRacingWithRelease_neverLeavesAPollJobWithoutASubscriber() {
        val remote = FakeRemote(canvas())
        val local = CanvasLocalDataSourceImpl()
        val scope = CoroutineScope(Dispatchers.Default)
        val poller = CanvasPoller(scope, remote, local)
        val lockField = CanvasPoller::class.java.getDeclaredField("lock").apply { isAccessible = true }
        val lock = requireNotNull(lockField.get(poller))
        val refreshingField = CanvasPoller::class.java.getDeclaredField("refreshing").apply { isAccessible = true }

        @Suppress("UNCHECKED_CAST")
        val refreshing = refreshingField.get(poller) as MutableMap<GroupId, Int>

        try {
            repeat(2_000) {
                poller.acquire(GROUP)
                // 진행 중인 갱신이 있는 척해 refresh() 의 이른 반환을 유도한다 — 원격 호출이
                // 끼면 그 시간만큼 창을 두드릴 기회가 흩어진다
                refreshing[GROUP] = -1

                lateinit var refreshThread: Thread
                lateinit var releaseThread: Thread
                synchronized(lock) {
                    // 이 블록이 락을 쥔 채로 두 스레드를 띄워야, 둘 다 반드시 락을 기다리며
                    // 멈춘다 — 그러지 않으면 한쪽이 락 경합 없이 먼저 끝나 버릴 수 있다.
                    refreshThread = thread { runBlocking { poller.refreshNow(GROUP) } }
                    releaseThread = thread { poller.release(GROUP) }

                    val deadline = System.nanoTime() + 2.seconds.inWholeNanoseconds
                    while (refreshThread.state != Thread.State.BLOCKED || releaseThread.state != Thread.State.BLOCKED) {
                        check(System.nanoTime() < deadline) { "두 스레드가 락을 기다리며 멈추지 않았다" }
                        Thread.onSpinWait()
                    }
                }
                // 여기서부터는 둘 중 누가 락을 먼저 쥘지 JVM 스케줄러가 정한다

                refreshThread.join()
                releaseThread.join()

                // acquire 한 번에 release 한 번이라 구독자는 항상 0으로 돌아온다. 문제는
                // 폴 잡이다 — 구독자 없이 폴 잡만 남으면 그 그룹은 프로세스가 끝날 때까지 돈다.
                assertFalse(poller.hasSubscriberForTest(GROUP))
                assertFalse(poller.isPollingForTest(GROUP))

                // 다음 반복이 깨끗한 상태에서 출발하도록 정리한다
                poller.stopAll()
            }
        } finally {
            scope.cancel()
        }
    }
}
