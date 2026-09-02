package com.teamyg.parfait.feature.groups.setting.impl.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGUserChip
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGUserNameStyle
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.setting.impl.R
import com.teamyg.parfait.feature.groups.setting.impl.model.GroupMemberUiModel

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
                key(member.id) {
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
}

private class GroupMemberListPreviewParameterProvider :
    PreviewParameterProvider<List<GroupMemberUiModel>> {
    override val values: Sequence<List<GroupMemberUiModel>>
        get() = sequenceOf(
            listOf(
                GroupMemberUiModel(
                    id = 1L,
                    nickname = "나야나",
                    colorChipType = YGColorChipType.NametagChip1,
                    isMe = true,
                ),
            ),
            listOf(
                GroupMemberUiModel(
                    id = 1L,
                    nickname = "나야나",
                    colorChipType = YGColorChipType.NametagChip1,
                    isMe = true,
                ),
                GroupMemberUiModel(
                    id = 2L,
                    nickname = "철수",
                    colorChipType = YGColorChipType.NametagChip3,
                ),
                GroupMemberUiModel(
                    id = 3L,
                    nickname = "영희",
                    colorChipType = YGColorChipType.NametagChip5,
                ),
                GroupMemberUiModel(
                    id = 4L,
                    nickname = "민수",
                    colorChipType = YGColorChipType.NametagChip8,
                ),
            ),
            listOf(
                GroupMemberUiModel(
                    id = 1L,
                    nickname = "열다섯글자를꽉꽉채운닉네임야호",
                    colorChipType = YGColorChipType.NametagChip2,
                    isMe = true,
                ),
                GroupMemberUiModel(
                    id = 2L,
                    nickname = "열다섯글자를꽉꽉채운닉네임이야",
                    colorChipType = YGColorChipType.NametagChip6,
                ),
            ),
        )
}

@YGPreview
@Composable
private fun GroupMemberListPreview(
    @PreviewParameter(GroupMemberListPreviewParameterProvider::class)
    members: List<GroupMemberUiModel>,
) = PreviewBox {
    GroupMemberList(
        members = members,
        modifier = Modifier
            .fillMaxWidth()
            .padding(YGTheme.layout.padding.padding7),
    )
}
