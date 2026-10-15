package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.Flow

/**
 * 오늘 캔버스의 인메모리 SSoT (`docs/adr/0029-canvas-today-ssot-polling.md`).
 *
 * 지난 날 캔버스는 여기 두지 않는다 — 마감돼 바뀌지 않으므로 공유해 얻을 것이 없고,
 * 날짜 축을 들이면 무효화 규칙이 그만큼 늘어난다.
 */
interface CanvasLocalDataSource {
    /** 아직 한 번도 못 받았으면 `null` (`docs/api/parfait.md` — 서버가 오늘 캔버스를 만들어 주므로 "0건"이 없다) */
    fun todayCanvas(groupId: GroupId): Flow<CanvasVO?>

    /** 구독하지 않고 현재 값만 본다 — [todayCanvas] 를 구독하면 그 자체가 부수 효과를 갖는다 */
    fun cachedTodayCanvas(groupId: GroupId): CanvasVO?

    /**
     * 캐시 값이 실린 응답의 ETag. 캐시 값과 한 쌍이라 [saveTodayCanvas]·[clear] 가 함께 다룬다 —
     * 따로 두면 캐시는 비었는데 ETag 만 남아 304 로 빈 화면에 갇힌다.
     */
    fun cachedTodayEtag(groupId: GroupId): String?

    /**
     * [CanvasVO] 에 그룹이 실려 오지 않아 따로 받는다.
     *
     * @param etag `null` 이면 이전 ETag 도 남기지 않는다. 새 값에 옛 ETag 가 붙으면 안 된다.
     */
    fun saveTodayCanvas(
        groupId: GroupId,
        canvas: CanvasVO,
        etag: String? = null,
    )

    fun clear()
}
