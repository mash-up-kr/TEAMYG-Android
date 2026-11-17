package com.teamyg.parfait.domain.repository.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.datetime.LocalDate

/**
 * "지난 캔버스 보기" 알럿을 계정·그룹별로 어느 마감일까지 봤는지 기억한다.
 *
 * [com.teamyg.parfait.domain.model.canvas.CanvasVO.lastClosedDate] 는 03시 회전으로 캔버스가
 * 마감될 때만 새 값이 된다 — 그 값을 기준으로 삼아, 같은 마감일은 화면을 몇 번을 다시 열어도
 * 한 번만 묻는다.
 */
interface PastCanvasAlertRepository {
    /**
     * 한 번도 확인한 적 없으면 `null`. 같음 비교까지 대신하지 않는 이유는 "처음 확인"과
     * "마감일이 바뀜"을 호출부가 다르게 다뤄야 해서다(처음이면 기준선만 세우고 알리지 않는다).
     */
    suspend fun lastSeenClosedDate(
        memberId: MemberId,
        groupId: GroupId,
    ): LocalDate?

    suspend fun markSeen(
        memberId: MemberId,
        groupId: GroupId,
        lastClosedDate: LocalDate,
    )
}
