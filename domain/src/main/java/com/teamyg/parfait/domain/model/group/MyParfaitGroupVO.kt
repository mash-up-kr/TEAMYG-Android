package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId
import kotlin.time.Instant

data class MyParfaitGroupVO(
    val groupId: GroupId,
    val groupName: GroupName,
    val recentImageUrl: String?,
    /**
     * 마지막으로 토핑이 올라온 시각. **토핑이 하나도 없으면 그룹이 만들어진 시각**이 오므로
     * "활동이 있었다"는 뜻이 아니다 — 그것을 가르려면 [recentImageUrl] 이 `null` 인지 함께 본다.
     */
    val recentImageUploadedAt: Instant?,
    /**
     * 마지막으로 토핑을 올린 사람의 칩. **토핑이 하나도 없으면 그룹을 만든 사람의 칩**이 온다 —
     * 그때 이 값은 "마지막으로 바꾼 사람"이 아니라 "만든 사람"을 가리킨다.
     * 가리키는 사람이 그룹을 나갔으면 [NametagChipType.DEFAULT] 다. 앱이 모르는 타입 문자열이
     * 왔을 때도 매퍼가 같은 값으로 접는다.
     */
    val lastPlacedByNametagChip: NametagChipType,
)
