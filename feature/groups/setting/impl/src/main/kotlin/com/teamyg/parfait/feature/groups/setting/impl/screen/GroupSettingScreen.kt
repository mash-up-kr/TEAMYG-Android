package com.teamyg.parfait.feature.groups.setting.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.card.YGInviteCard
import com.teamyg.parfait.core.designsystem.component.card.YGInviteCardStatus
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygdangerzone.YGDangerZone
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.focus.clearFocusOnTap
import com.teamyg.parfait.feature.groups.setting.impl.R
import com.teamyg.parfait.feature.groups.setting.impl.component.GroupMemberList
import com.teamyg.parfait.feature.groups.setting.impl.component.GroupNicknameField
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingUiState

@Composable
internal fun GroupSettingScreen(
    state: GroupSettingUiState,
    onClickBack: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onNicknameFocusChange: (Boolean) -> Unit,
    onConfirmNickname: () -> Unit,
    onClickCopyInviteCode: () -> Unit,
    onClickLeaveGroup: () -> Unit,
    onClickReportGroup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val confirmAndDismissKeyboard = {
        onConfirmNickname()
        focusManager.clearFocus()
    }

    YGScreen(modifier = modifier.clearFocusOnTap()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                YGTopBarDetail(
                    title = state.groupName,
                    onIconClick = onClickBack,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap8),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = YGTheme.layout.padding.padding7,
                            end = YGTheme.layout.padding.padding7,
                            bottom = YGTheme.layout.padding.padding8,
                        ),
                ) {
                    GroupNicknameField(
                        nickname = state.nicknameInput,
                        errorMessageResId = state.errorMessageResId,
                        onNicknameChange = onNicknameChange,
                        onFocusChange = onNicknameFocusChange,
                        onConfirmNickname = confirmAndDismissKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    GroupMemberList(
                        members = state.members,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    YGInviteCard(
                        label = stringResource(R.string.group_setting_invite_label),
                        inviteCode = state.inviteCode,
                        subText = inviteCardSubText(state),
                        status = inviteCardStatus(state),
                        copyButtonText = stringResource(R.string.group_setting_copy),
                        onCopyClick = onClickCopyInviteCode,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    YGDangerZone(
                        topZone = {
                            YGActionItem(
                                text = stringResource(R.string.group_setting_leave),
                                onClick = onClickLeaveGroup,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        bottomZone = {
                            YGActionItem(
                                text = stringResource(R.string.group_setting_report),
                                onClick = onClickReportGroup,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        OnBack { onClickBack() }
    }
}

@Composable
private fun inviteCardSubText(state: GroupSettingUiState): String = when {
    state.remainingCount <= 0 -> stringResource(R.string.group_setting_invite_full)
    state.isCodeCopied -> stringResource(R.string.group_setting_invite_copied)
    else -> stringResource(R.string.group_setting_invite_remaining, state.remainingCount)
}

private fun inviteCardStatus(state: GroupSettingUiState): YGInviteCardStatus =
    if (state.remainingCount > 0) YGInviteCardStatus.Active else YGInviteCardStatus.Invalid

private class GroupSettingPreviewParameterProvider :
    PreviewParameterProvider<GroupSettingUiState> {
    override val values: Sequence<GroupSettingUiState>
        get() = sequenceOf(
            GroupSettingUiState(),
            GroupSettingUiState(isCodeCopied = true),
            GroupSettingUiState(remainingCount = 0),
            GroupSettingUiState(
                groupName = "열글자를꽉채운그룹명",
                myNickname = "열다섯글자를꽉꽉채운닉네임야호",
                nicknameInput = "열다섯글자를꽉꽉채운닉네임야호",
            ),
        )
}

@YGPreview
@Composable
private fun GroupSettingScreenPreview(
    @PreviewParameter(GroupSettingPreviewParameterProvider::class)
    state: GroupSettingUiState,
) = PreviewBox {
    GroupSettingScreen(
        state = state,
        onClickBack = {},
        onNicknameChange = {},
        onNicknameFocusChange = {},
        onConfirmNickname = {},
        onClickCopyInviteCode = {},
        onClickLeaveGroup = {},
        onClickReportGroup = {},
        modifier = Modifier.fillMaxSize(),
    )
}
