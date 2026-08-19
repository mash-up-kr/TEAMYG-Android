package com.teamyg.parfait.feature.groups.list.impl.util

import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.domain.model.group.NametagChipType
import kotlin.test.Test
import kotlin.test.assertEquals

class GrouptagChipTypeTest {
    @Test
    fun toGrouptagChipType_pairsTwelveNametagTypesIntoSix() {
        // Given Grouptag-Chip 은 Nametag 타입을 둘씩 묶은 6종이다
        val pairs = listOf(
            NametagChipType.TYPE1 to YGGrouptagChipType.TYPE_1_2,
            NametagChipType.TYPE2 to YGGrouptagChipType.TYPE_1_2,
            NametagChipType.TYPE3 to YGGrouptagChipType.TYPE_3_4,
            NametagChipType.TYPE4 to YGGrouptagChipType.TYPE_3_4,
            NametagChipType.TYPE5 to YGGrouptagChipType.TYPE_5_6,
            NametagChipType.TYPE6 to YGGrouptagChipType.TYPE_5_6,
            NametagChipType.TYPE7 to YGGrouptagChipType.TYPE_7_8,
            NametagChipType.TYPE8 to YGGrouptagChipType.TYPE_7_8,
            NametagChipType.TYPE9 to YGGrouptagChipType.TYPE_9_10,
            NametagChipType.TYPE10 to YGGrouptagChipType.TYPE_9_10,
            NametagChipType.TYPE11 to YGGrouptagChipType.TYPE_11_12,
            NametagChipType.TYPE12 to YGGrouptagChipType.TYPE_11_12,
        )

        // When/Then 짝이 그대로 맞는다
        pairs.forEach { (nametag, grouptag) ->
            assertEquals(grouptag, nametag.toGrouptagChipType())
        }
    }

    @Test
    fun toGrouptagChipType_defaultFallsBackToDefault() {
        // Given 마지막 토퍼가 그룹을 나갔다
        // When/Then 나간 사람 색을 계속 쓰지 않고 중립으로 간다
        assertEquals(YGGrouptagChipType.DEFAULT, NametagChipType.DEFAULT.toGrouptagChipType())
    }
}
