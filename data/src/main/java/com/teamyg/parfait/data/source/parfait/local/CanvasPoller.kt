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
