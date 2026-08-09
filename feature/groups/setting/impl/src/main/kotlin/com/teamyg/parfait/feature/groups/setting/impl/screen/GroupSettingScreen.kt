package com.teamyg.parfait.feature.groups.setting.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygdangerzone.YGDangerZone
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.focus.clearFocusOnTap
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
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
    val handleBack = {
        if (state.isEditing) focusManager.clearFocus() else onClickBack()
    }
    val confirmAndDismissKeyboard = {
        if (state.nicknameError == null) {
            onConfirmNickname()
            focusManager.clearFocus()
        }
    }

    YGScreen(modifier = modifier.clearFocusOnTap()) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarDetail(
                title = state.groupName.value,
                onIconClick = handleBack,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = YGTheme.layout.padding.padding7,
                        end = YGTheme.layout.padding.padding7,
                        bottom = YGTheme.layout.padding.padding8,
                    ),
                verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap8),
            ) {
                GroupNicknameField(
                    nickname = state.nicknameInput,
                    nicknameError = state.nicknameError,
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
                    inviteCode = state.inviteCode.value,
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

            if (state.isEditing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .background(YGAtomicColors.Gray.White)
                        .padding(YGTheme.layout.padding.padding7),
                ) {
                    YGButton(
                        text = stringResource(R.string.group_setting_confirm),
                        buttonType = YGButtonType.Large,
                        isEnabled = state.isConfirmEnabled,
                        onClick = confirmAndDismissKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        OnBack { handleBack() }
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
                groupName = GroupName("열글자를꽉채운그룹명"),
                myNickname = GroupNickname("열다섯글자를꽉꽉채운닉네임야호"),
                nicknameInput = "열다섯글자를꽉꽉채운닉네임야호",
            ),
            GroupSettingUiState(
                isEditing = true,
                nicknameInput = "바꾼닉네임",
            ),
            GroupSettingUiState(
                isEditing = true,
                nicknameInput = " 잘못된닉네임",
                nicknameError = NameValidResult.Error.SpaceAtEdge,
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
