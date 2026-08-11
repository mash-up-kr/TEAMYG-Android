package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ReportId

data class ReportedGroupVO(
    val groupId: GroupId,
    val reportId: ReportId,
)
