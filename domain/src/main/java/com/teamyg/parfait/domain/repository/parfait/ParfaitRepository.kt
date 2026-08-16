package com.teamyg.parfait.domain.repository.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId

interface ParfaitRepository {
    /**
     * ⚠️ 조회인데 서버가 캔버스를 만든다 — 오늘 날짜 파르페가 없으면 생성해 저장한다
     * (`api/parfait.md`). 화면이 반복 호출하면 빈 캔버스가 양산되므로 호출 지점을 아껴야 한다.
     *
     * 오늘 날짜가 이미 마감돼 있으면 그것을 그대로 돌려준다 — status 가 ACTIVE 가 아닐 수 있고,
     * 서버는 마감된 캔버스의 편집도 막지 않으므로 잠그는 것은 화면 책임이다.
     */
    suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO>
}
