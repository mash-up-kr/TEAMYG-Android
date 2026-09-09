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
