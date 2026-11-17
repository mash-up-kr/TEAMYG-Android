package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.datetime.LocalDate

/** 그룹별로 "지난 캔버스" 알럿을 마지막으로 확인시킨 마감일을 저장한다. */
interface PastCanvasAlertLocalDataSource {
    /** 아직 한 번도 안 봤으면(또는 이 기기에서 처음이면) `null` */
    suspend fun lastSeenClosedDate(groupId: GroupId): LocalDate?

    suspend fun markSeenClosedDate(
        groupId: GroupId,
        date: LocalDate,
    )
}
