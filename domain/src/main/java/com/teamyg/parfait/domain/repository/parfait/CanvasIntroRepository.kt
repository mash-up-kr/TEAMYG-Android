package com.teamyg.parfait.domain.repository.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.datetime.LocalDate

/**
 * 빈 캔버스 안내 연출을 계정·그룹별로 어느 캔버스 날짜까지 재생했는지 기억한다.
 *
 * 그룹마다 마지막 날짜 하나만 덮어쓴다 — 오늘 캔버스 날짜와 같으면 이미 재생한 것이고, 03시 이후
 * 새 캔버스가 생기면 날짜가 달라져 다시 재생한다.
 */
interface CanvasIntroRepository {
    suspend fun playedDate(
        memberId: MemberId,
        groupId: GroupId,
    ): LocalDate?

    suspend fun markPlayed(
        memberId: MemberId,
        groupId: GroupId,
        date: LocalDate,
    )
}
