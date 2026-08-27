package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.data.model.qualifier.ApplicationScope
import com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    /**
     * 그룹별 "진행 중" 표시. 값은 그 갱신을 시작한 [generation] 이다 — [stopAll] 로 세대가 바뀐
     * 뒤 곧바로 재시작된 새 세대의 항목을, 이전 세대의 지연 응답이 자기 것인 줄 알고 지우는
     * 사고를 막는다(세대가 다르면 자기 것이 아니므로 지우지 않는다).
     */
    private val refreshing = mutableMapOf<GroupId, Int>()

    /** [stopAll] 이 올린다. 그 전에 출발한 응답은 캐시에 싣지 않는다 */
    private var generation = 0

    private val _refreshFailures = MutableSharedFlow<GroupId>(extraBufferCapacity = FAILURE_BUFFER)

    /**
     * 갱신이 실패했다는 신호. 값이 아니라 실패 사실만 흘린다 — 값을 얻는 길은 캐시 하나라는
     * 규칙(`adr/0029-canvas-today-ssot-polling.md`)은 그대로 둔다.
     *
     * 첫 조회를 기다리는 화면이 로딩을 풀 계기로 쓴다. 캐시는 실패했을 때 아무것도 방출하지
     * 않아, 이 신호가 없으면 화면이 로딩에 갇힌다.
     */
    val refreshFailures: SharedFlow<GroupId> = _refreshFailures.asSharedFlow()

    /** 테스트 전용 — 계수 경합 테스트가 구독자와 폴 잡의 최종 상태를 나눠 관찰하는 데 쓴다 */
    internal fun hasSubscriberForTest(groupId: GroupId): Boolean =
        synchronized(lock) { subscriberCounts.containsKey(groupId) }

    /** 테스트 전용 — [hasSubscriberForTest] 참고 */
    internal fun isPollingForTest(groupId: GroupId): Boolean = synchronized(lock) { pollJobs.containsKey(groupId) }

    fun acquire(groupId: GroupId) {
        val isFirst = synchronized(lock) {
            val next = (subscriberCounts[groupId] ?: 0) + 1
            subscriberCounts[groupId] = next
            next == 1
        }
        if (isFirst.not()) return

        restartPollTimer(groupId)
        scope.launch { refresh(groupId) }
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
     *
     * 구독 확인과 재시작을 한 [synchronized] 안에서 한다 — 갈라 두면 그 사이에 마지막
     * [release] 가 끼어들어 구독자가 없는데도 폴 잡이 살아난다.
     */
    suspend fun refreshNow(groupId: GroupId): Result<Unit> {
        val result = refresh(groupId)
        synchronized(lock) {
            if (subscriberCounts.containsKey(groupId)) restartPollTimerLocked(groupId)
        }
        return result
    }

    /**
     * 화면이 곧 사라지는 자리에서 부른다 — 호출자 스코프에서 기다리면 되감기가 늦어지거나
     * `viewModelScope` 취소로 요청이 끊긴다.
     */
    fun refreshNowAsync(groupId: GroupId) {
        scope.launch { refreshNow(groupId) }
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
        synchronized(lock) { restartPollTimerLocked(groupId) }
    }

    /** [lock] 을 이미 쥔 자리에서만 부른다 — [synchronized] 는 재진입 가능해 중첩 호출도 안전하다 */
    private fun restartPollTimerLocked(groupId: GroupId) {
        pollJobs.remove(groupId)?.cancel()
        pollJobs[groupId] = scope.launch {
            while (isActive) {
                delay(CANVAS_POLL_INTERVAL)
                refresh(groupId)
            }
        }
    }

    /**
     * 오늘 조회는 캔버스가 없으면 서버가 만들어 저장한다(`api/parfait.md`) — 그래서 캔버스를
     * 만들 필요가 있을 때만 쓴다. 캐시가 비었거나 실린 날짜가 오늘이 아니면(하루 경계를 넘겼다)
     * 그 경우다. 나머지는 부작용 없는 상세 조회를 쓴다.
     */
    private suspend fun refresh(groupId: GroupId): Result<Unit> {
        val startedGeneration = synchronized(lock) {
            if (refreshing.containsKey(groupId)) return Result.success(Unit)
            refreshing[groupId] = generation
            generation
        }

        try {
            val cached = local.cachedTodayCanvas(groupId)
            val cachedParfaitId = cached?.parfaitId
            val needsToday = cachedParfaitId == null || cached.date != parfaitToday(clock)

            val result = if (needsToday) {
                remote.getTodayCanvas(groupId)
            } else {
                remote.getCanvasDetail(groupId = groupId, parfaitId = cachedParfaitId)
            }

            return result
                .onSuccess { canvas ->
                    synchronized(lock) {
                        if (generation == startedGeneration) local.saveTodayCanvas(groupId, canvas)
                    }
                }.onFailure {
                    // 세대가 바뀌었으면 이미 버려진 갱신의 실패라 화면에 알리지 않는다
                    if (synchronized(lock) { generation == startedGeneration }) _refreshFailures.tryEmit(groupId)
                }.map { }
        } finally {
            synchronized(lock) {
                if (refreshing[groupId] == startedGeneration) refreshing.remove(groupId)
            }
        }
    }

    private companion object {
        /** 화면이 없는 사이 실패가 몰려도 방출이 막히지 않을 만큼만 든다 */
        const val FAILURE_BUFFER = 16
    }
}
