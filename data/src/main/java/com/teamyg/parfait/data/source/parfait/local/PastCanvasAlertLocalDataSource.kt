package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.datetime.LocalDate

interface PastCanvasAlertLocalDataSource {
    suspend fun lastSeenClosedDate(
        memberId: MemberId,
        groupId: GroupId,
    ): LocalDate?

    suspend fun markSeenClosedDate(
        memberId: MemberId,
        groupId: GroupId,
        date: LocalDate,
    )
}
