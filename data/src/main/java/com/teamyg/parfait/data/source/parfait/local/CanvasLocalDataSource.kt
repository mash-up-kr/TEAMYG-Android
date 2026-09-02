package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.Flow

/**
 * 오늘 캔버스의 인메모리 SSoT (`adr/0029-canvas-today-ssot-polling.md`).
 *
 * 지난 날 캔버스는 여기 두지 않는다 — 마감돼 바뀌지 않으므로 공유해 얻을 것이 없고,
 * 날짜 축을 들이면 무효화 규칙이 그만큼 늘어난다.
 *
 * IO 가 없어 모든 함수가 non-suspend 다.
 */
interface CanvasLocalDataSource {
    /** 아직 한 번도 못 받았으면 `null` (`api/parfait.md` — 서버가 오늘 캔버스를 만들어 주므로 "0건"이 없다) */
    fun todayCanvas(groupId: GroupId): Flow<CanvasVO?>

    /** 구독하지 않고 현재 값만 본다 — [todayCanvas] 를 구독하면 그 자체가 부수 효과를 갖는다 */
    fun cachedTodayCanvas(groupId: GroupId): CanvasVO?

    /** [CanvasVO] 에 그룹이 실려 오지 않아 따로 받는다 */
    fun saveTodayCanvas(
        groupId: GroupId,
        canvas: CanvasVO,
    )

    fun clear()
}
