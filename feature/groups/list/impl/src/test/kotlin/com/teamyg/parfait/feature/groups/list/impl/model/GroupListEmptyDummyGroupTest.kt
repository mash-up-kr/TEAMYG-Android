package com.teamyg.parfait.feature.groups.list.impl.model

import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroupType
import com.teamyg.parfait.feature.groups.list.impl.R
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupListEmptyDummyGroupTest {
    @Test
    fun dummyGroups_followTheFigmaOrderAndVariants() {
        val dummies = GROUP_LIST_EMPTY_DUMMY_GROUPS

        assertEquals(3, dummies.size)
        assertEquals(
            listOf(
                R.string.group_list_empty_dummy_name_collector,
                R.string.group_list_empty_dummy_name_parfait,
                R.string.group_list_empty_dummy_name_daily,
            ),
            dummies.map { it.nameRes },
        )
        assertEquals(listOf(1, 2, 3), dummies.map { it.minutesAgo })
        assertEquals(
            listOf(YGGrouptagChipType.TYPE_5_6, YGGrouptagChipType.TYPE_1_2, YGGrouptagChipType.TYPE_3_4),
            dummies.map { it.chipType },
        )
        assertEquals(
            listOf(YGToppingGroupType.TYPE_2_LEFT, YGToppingGroupType.TYPE_1_RIGHT, YGToppingGroupType.TYPE_2_LEFT),
            dummies.map { it.type },
        )
    }
}
