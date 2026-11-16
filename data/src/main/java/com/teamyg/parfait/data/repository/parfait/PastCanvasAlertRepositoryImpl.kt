package com.teamyg.parfait.data.repository.parfait

import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.parfait.local.PastCanvasAlertLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.repository.parfait.PastCanvasAlertRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** 확인 기록은 [UserInfoLocalDataSource.myAccount] 의 계정에 묶는다. 계정을 모르면 읽기는 `null`, 쓰기는 건너뛴다. */
class PastCanvasAlertRepositoryImpl
@Inject
constructor(
    private val pastCanvasAlertLocalDataSource: PastCanvasAlertLocalDataSource,
    private val userInfoLocalDataSource: UserInfoLocalDataSource,
) : PastCanvasAlertRepository {
    override suspend fun lastSeenClosedDate(groupId: GroupId): LocalDate? = currentMemberId()
        ?.let { memberId -> pastCanvasAlertLocalDataSource.lastSeenClosedDate(memberId, groupId) }

    override suspend fun markSeen(
        groupId: GroupId,
        lastClosedDate: LocalDate,
    ) {
        currentMemberId()?.let { memberId ->
            pastCanvasAlertLocalDataSource.markSeenClosedDate(memberId, groupId, lastClosedDate)
        }
    }

    private suspend fun currentMemberId(): MemberId? = userInfoLocalDataSource.myAccount.first()?.memberId
}
