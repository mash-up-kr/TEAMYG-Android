package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId
import kotlin.time.Instant

data class MyParfaitGroupVO(
    val groupId: GroupId,
    val groupName: GroupName,
    val recentImageUrl: String?,
    /** 오프셋이 붙은 절대 시점 — 기기 타임존과 무관하게 같은 순간을 가리킨다 */
    val recentImageUploadedAt: Instant?,
    /**
     * 마지막으로 토핑을 올린 사람의 칩. 그 사람이 이미 그룹을 나갔으면
     * [NametagChipType.RELEASED] 이고, 토핑이 하나도 없으면 `null` 이다.
     */
    val lastPlacedByNametagChip: NametagChipType?,
)
