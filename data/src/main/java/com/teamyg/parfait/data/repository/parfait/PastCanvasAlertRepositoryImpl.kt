package com.teamyg.parfait.data.repository.parfait

import com.teamyg.parfait.data.source.parfait.local.PastCanvasAlertLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.parfait.PastCanvasAlertRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class PastCanvasAlertRepositoryImpl
@Inject
constructor(
    private val pastCanvasAlertLocalDataSource: PastCanvasAlertLocalDataSource,
) : PastCanvasAlertRepository {
    override suspend fun lastSeenClosedDate(groupId: GroupId): LocalDate? =
        pastCanvasAlertLocalDataSource.lastSeenClosedDate(groupId)

    override suspend fun markSeen(
        groupId: GroupId,
        lastClosedDate: LocalDate,
    ) = pastCanvasAlertLocalDataSource.markSeenClosedDate(groupId, lastClosedDate)
}
