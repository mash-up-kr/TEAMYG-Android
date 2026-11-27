package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.datetime.LocalDate

interface CanvasIntroLocalDataSource {
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
