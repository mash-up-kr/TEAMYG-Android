package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.datetime.LocalDate

/** 계정·그룹별로 "지난 캔버스" 알럿을 마지막으로 확인시킨 마감일을 저장한다. */
interface PastCanvasAlertLocalDataSource {
    /** 아직 한 번도 안 봤으면(또는 이 기기에서 이 계정이 처음이면) `null` */
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
