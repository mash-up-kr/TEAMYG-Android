package com.teamyg.parfait.feature.groups.setting.impl.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGUserChip
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGUserNameStyle
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.feature.groups.setting.impl.R
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupMemberUiModel

@Composable
internal fun GroupMemberList(
    members: List<GroupMemberUiModel>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
    ) {
        YGLabel(text = stringResource(R.string.group_setting_member_label, members.size))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = YGTheme.layout.gap.gap3),
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        ) {
            members.forEach { member ->
                YGUserChip(
                    colorChipType = member.colorChipType,
                    userFirstName = member.nickname.take(1),
                    chip = YGNametagChipStyle.Style40,
                    userName = if (member.isMe) {
                        stringResource(R.string.group_setting_member_name_me, member.nickname)
                    } else {
                        member.nickname
                    },
                    userStyle = if (member.isMe) {
                        YGUserNameStyle.StyleBold
                    } else {
                        YGUserNameStyle.StyleMedium
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
