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
     * 마지막으로 토핑을 올린 사람의 칩. `null` 은 값이 없거나 앱이 모르는 값이라는 뜻이다 —
     * 그 사람이 이미 그룹을 나갔으면 [NametagChipType.DEFAULT], 토핑이 하나도 없으면
     * `null` 이 오고, 앱이 아직 모르는 타입 문자열도 매퍼에서 `null` 로 접힌다.
     */
    val lastPlacedByNametagChip: NametagChipType?,
)
