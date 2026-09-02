package com.teamyg.parfait.feature.groups.setting.impl.route

import android.content.ClipData
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.ui.R as CoreUiR
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.groups.setting.impl.screen.GroupSettingScreen
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingError
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingIntent
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingSideEffect
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingViewModel
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.toStringResource

private const val CLIP_LABEL_INVITE_MESSAGE = "invite_message"

@Composable
internal fun GroupSettingRoute(
    groupId: Long,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupSettingViewModel = hiltViewModel(
        creationCallback = { factory: GroupSettingViewModel.Factory ->
            factory.create(groupIdValue = groupId)
        },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val inviteMessageTemplate = stringResource(CoreUiR.string.group_invite_message)
    val toastPolicy = rememberYGToastPolicy()

    // 이펙트 수집은 컴포지션이 아니라 코루틴이라 그 안에서 `stringResource` 를 부를 수 없다.
    // 문구를 여기서 미리 뽑아 두고 이펙트는 고르기만 한다
    val errorMessages = GroupSettingError.entries.associateWith { it.toStringResource() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupSettingSideEffect.NavigateBack -> navigator.onBack()

                is GroupSettingSideEffect.ShowError ->
                    toastPolicy.showError(errorMessages.getValue(effect.error))

                is GroupSettingSideEffect.CopyInviteCode -> clipboard.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(
                            CLIP_LABEL_INVITE_MESSAGE,
                            String.format(inviteMessageTemplate, effect.inviteCode),
                        ),
                    ),
                )

                // 뒤로 가기(pop)가 아니라 백스택을 갈아 끼우는 이유: 여기 오기까지 쌓인 화면은
                // 전부 방금 떠난 그룹의 것이라 돌아가면 403 뿐이다. 목록도 새로 열려 다시 읽는다.
                GroupSettingSideEffect.NavigateToGroupList -> navigator.replaceAll(NavKeyGroupList)
            }
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        isLoading = state.isLoading,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
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
            onConfirmLeaveGroup = { viewModel.processIntent(GroupSettingIntent.ConfirmLeaveGroup) },
            onConfirmReportGroup = {
                viewModel.processIntent(GroupSettingIntent.ConfirmReportGroup)
            },
            onDismissDialog = { viewModel.processIntent(GroupSettingIntent.DismissDialog) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // 스캐폴드가 준 systemBars 인셋을 소비해, 하위 imePadding() 이
                // 내비게이션 바 높이를 두 번 세지 않게 한다
                .consumeWindowInsets(innerPadding),
        )
    }
}
