package com.teamyg.parfait.feature.groups.setting.impl.route

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.setting.impl.screen.GroupSettingScreen
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingIntent
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingSideEffect
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingViewModel

private const val CLIP_LABEL_INVITE_CODE = "invite_code"

@Composable
internal fun GroupSettingRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupSettingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupSettingSideEffect.NavigateBack -> navigator.onBack()

                is GroupSettingSideEffect.CopyInviteCode -> clipboard.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(CLIP_LABEL_INVITE_CODE, effect.inviteCode),
                    ),
                )
            }
        }
    }

    GroupSettingScreen(
        state = state,
        onClickBack = { viewModel.processIntent(GroupSettingIntent.ClickBack) },
        onNicknameChange = { viewModel.processIntent(GroupSettingIntent.InputNickname(it)) },
        onNicknameFocusChange = {
            viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(it))
        },
        onConfirmNickname = { viewModel.processIntent(GroupSettingIntent.ConfirmNickname) },
        onClickCopyInviteCode = {
            viewModel.processIntent(GroupSettingIntent.ClickCopyInviteCode)
        },
        onClickLeaveGroup = { viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup) },
        onClickReportGroup = { viewModel.processIntent(GroupSettingIntent.ClickReportGroup) },
        modifier = modifier,
    )
}
