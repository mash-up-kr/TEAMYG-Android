package com.teamyg.parfait.feature.groups.setting.impl.model

import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType

data class GroupMemberUiModel(
    val id: Long,
    val nickname: String,
    val colorChipType: YGColorChipType,
    val isMe: Boolean = false,
)
