package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.canvas.TodayCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.datetime.LocalDate

interface ParfaitRemoteDataSource {
    suspend fun getYears(groupId: GroupId): Result<List<Int>>

    /**
     * 오늘의 캔버스를 상태·멤버·배경·배치 토핑까지 한 번에 읽는다.
     *
     * ⚠️ 조회인데 서버가 캔버스를 만든다 — 오늘 날짜 파르페가 없으면 생성해 저장한다
     * (`api/parfait.md`). 화면이 반복 호출하면 빈 캔버스가 양산되므로 호출 지점을 아껴야 한다.
     *
     * 오늘 날짜가 이미 마감돼 있으면 그것을 그대로 돌려준다 — status 가 ACTIVE 가 아닐 수 있고,
     * 서버는 마감된 캔버스의 편집도 막지 않으므로 잠그는 것은 화면 책임이다.
     */
    suspend fun getTodayCanvas(groupId: GroupId): Result<TodayCanvasVO>

    /**
     * 과거 캔버스 목록. 범위를 생략하면 서버 기본값(오늘 - 30일 ~ 오늘)이다.
     *
     * 페이지네이션도 범위 상한도 없다 — 넓게 주면 그만큼 전량이 내려온다.
     * from 이 to 보다 늦으면 400 INVALID_DATE_RANGE 다.
     */
    suspend fun getPastCanvases(
        groupId: GroupId,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): Result<List<PastCanvasVO>>
}
