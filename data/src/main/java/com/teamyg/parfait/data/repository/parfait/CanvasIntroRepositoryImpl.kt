package com.teamyg.parfait.data.repository.parfait

import com.teamyg.parfait.data.source.parfait.local.CanvasIntroLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.repository.parfait.CanvasIntroRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class CanvasIntroRepositoryImpl
@Inject
constructor(
    private val canvasIntroLocalDataSource: CanvasIntroLocalDataSource,
) : CanvasIntroRepository {
    override suspend fun playedDate(
        memberId: MemberId,
        groupId: GroupId,
    ): LocalDate? = canvasIntroLocalDataSource.playedDate(memberId, groupId)

    override suspend fun markPlayed(
        memberId: MemberId,
        groupId: GroupId,
        date: LocalDate,
    ) = canvasIntroLocalDataSource.markPlayed(memberId, groupId, date)
}
