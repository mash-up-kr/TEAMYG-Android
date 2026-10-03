package com.teamyg.parfait.feature.groups.list.impl.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroupType
import com.teamyg.parfait.feature.groups.list.impl.R

internal data class GroupListEmptyDummyGroup(
    @DrawableRes val imageRes: Int,
    @StringRes val nameRes: Int,
    val minutesAgo: Int,
    val chipType: YGGrouptagChipType,
    val type: YGToppingGroupType,
)

/** 순서가 곧 `ToppingLayout` 자리(왼쪽 상단, 오른쪽 중단, 왼쪽 하단)이자 등장 순서다. */
internal val GROUP_LIST_EMPTY_DUMMY_GROUPS: List<GroupListEmptyDummyGroup> = listOf(
    GroupListEmptyDummyGroup(
        imageRes = R.drawable.img_group_list_dummy_matcha,
        nameRes = R.string.group_list_empty_dummy_name_collector,
        minutesAgo = 1,
        chipType = YGGrouptagChipType.TYPE_5_6,
        type = YGToppingGroupType.TYPE_2_LEFT,
    ),
    GroupListEmptyDummyGroup(
        imageRes = R.drawable.img_group_list_dummy_cap,
        nameRes = R.string.group_list_empty_dummy_name_parfait,
        minutesAgo = 2,
        chipType = YGGrouptagChipType.TYPE_1_2,
        type = YGToppingGroupType.TYPE_1_RIGHT,
    ),
    GroupListEmptyDummyGroup(
        imageRes = R.drawable.img_group_list_dummy_camera,
        nameRes = R.string.group_list_empty_dummy_name_daily,
        minutesAgo = 3,
        chipType = YGGrouptagChipType.TYPE_3_4,
        type = YGToppingGroupType.TYPE_2_LEFT,
    ),
)
