package com.teamyg.parfait.feature.groups.list.impl.util

import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.domain.model.group.NametagChipType

/**
 * 마지막으로 그룹을 바꾼 사람의 칩을 Grouptag-Chip 색으로 옮긴다.
 *
 * Grouptag-Chip 6종은 Nametag 12종을 둘씩 묶은 타입이라 짝이 정해져 있다. 짝을 `ordinal`
 * 산술로 내지 않는 이유는 [NametagChipType.DEFAULT] 가 그 범위 밖이어서다 — 분기로 갈라 둔다.
 *
 * 가리킬 사람이 없으면([NametagChipType.DEFAULT]) 중립 색이다. 목록 순서로 돌리면
 * 그룹이 하나 빠질 때마다 남은 카드의 색이 밀린다.
 *
 * 저장소에 같은 축의 변환이 셋이고 왜 공용화하지 않았는지는 `architecture/module-structure.md`.
 */
internal fun NametagChipType.toGrouptagChipType(): YGGrouptagChipType = when (this) {
    NametagChipType.TYPE1, NametagChipType.TYPE2 -> YGGrouptagChipType.TYPE_1_2
    NametagChipType.TYPE3, NametagChipType.TYPE4 -> YGGrouptagChipType.TYPE_3_4
    NametagChipType.TYPE5, NametagChipType.TYPE6 -> YGGrouptagChipType.TYPE_5_6
    NametagChipType.TYPE7, NametagChipType.TYPE8 -> YGGrouptagChipType.TYPE_7_8
    NametagChipType.TYPE9, NametagChipType.TYPE10 -> YGGrouptagChipType.TYPE_9_10
    NametagChipType.TYPE11, NametagChipType.TYPE12 -> YGGrouptagChipType.TYPE_11_12
    NametagChipType.DEFAULT -> YGGrouptagChipType.DEFAULT
}
