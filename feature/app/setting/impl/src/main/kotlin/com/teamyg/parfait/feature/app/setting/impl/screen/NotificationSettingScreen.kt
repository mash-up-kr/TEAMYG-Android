package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.component.ListItemToggle
import com.teamyg.parfait.feature.app.setting.impl.component.NotificationInfoCard
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.NotificationSettingState

private val ToastTopOffset = 13.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationSettingScreen(
    state: NotificationSettingState,
    toastPolicy: YGToastPolicy,
    onClickBack: () -> Unit,
    onClickOpenDeviceSettings: () -> Unit,
    onRefresh: () -> Unit,
    onChangeCanvasNotification: (Boolean) -> Unit,
    onChangeActivityNotification: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    YGScreen(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarDetail(
                title = stringResource(R.string.notification_setting_title),
                onIconClick = onClickBack,
                modifier = Modifier.fillMaxWidth(),
            )

            // 토스트는 상단바 바로 아래 영역의 윗변 기준으로 띄운다. 영역 밖으로는 그리지 않아
            // 슬라이드 인이 상단바를 덮지 않는다
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
            ) {
                // TODO: PullToRefresh 디자인이 아직 미정이라 Material3 기본 인디케이터를 쓴다. 확정되면 공통 컴포넌트로 교체
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // 내용이 화면보다 짧아도 당길 수 있도록 스크롤 가능한 컨테이너로 둔다
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = YGTheme.layout.padding.padding7),
                    ) {
                        if (state.isDeviceNotificationOff) {
                            NotificationInfoCard(
                                onClickOpenDeviceSettings = onClickOpenDeviceSettings,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap8))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
                            ListItemToggle(
                                title = stringResource(R.string.notification_setting_canvas_title),
                                subText = stringResource(R.string.notification_setting_canvas_sub),
                                checked = state.isCanvasNotificationChecked,
                                enabled = state.isToggleEnabled,
                                onCheckedChange = onChangeCanvasNotification,
                            )
                            ListItemToggle(
                                title = stringResource(R.string.notification_setting_activity_title),
                                subText = stringResource(R.string.notification_setting_activity_sub),
                                checked = state.isActivityNotificationChecked,
                                enabled = state.isToggleEnabled,
                                onCheckedChange = onChangeActivityNotification,
                            )
                        }
                    }
                }

                YGToastHost(
                    policy = toastPolicy,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = ToastTopOffset)
                        .fillMaxWidth(),
                )
            }
        }

        OnBack { onClickBack() }
    }
}

@YGPreview
@Composable
private fun NotificationSettingScreenPreview() = PreviewBox {
    Column {
        NotificationSettingScreen(
            state = NotificationSettingState(isSettingLoaded = true, isDeviceNotificationEnabled = false),
            toastPolicy = rememberYGToastPolicy(),
            onClickBack = {},
            onClickOpenDeviceSettings = {},
            onRefresh = {},
            onChangeCanvasNotification = {},
            onChangeActivityNotification = {},
        )
        NotificationSettingScreen(
            state = NotificationSettingState(
                isCanvasNotificationChecked = true,
                isSettingLoaded = true,
                isDeviceNotificationEnabled = true,
            ),
            toastPolicy = rememberYGToastPolicy(),
            onClickBack = {},
            onClickOpenDeviceSettings = {},
            onRefresh = {},
            onChangeCanvasNotification = {},
            onChangeActivityNotification = {},
        )
    }
}
