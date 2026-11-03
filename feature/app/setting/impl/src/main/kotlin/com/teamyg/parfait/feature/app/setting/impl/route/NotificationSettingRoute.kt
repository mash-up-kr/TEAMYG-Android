package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.android.extension.buildAppNotificationSettingsIntent
import com.teamyg.parfait.core.util.android.extension.buildAppSettingsIntent
import com.teamyg.parfait.core.util.android.permission.NotificationPermissionManager
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.screen.NotificationSettingScreen
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.NotificationSettingIntent
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.NotificationSettingSideEffect
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.NotificationSettingViewModel

@Composable
internal fun NotificationSettingRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toastPolicy = rememberYGToastPolicy()
    val saveErrorMessage = stringResource(R.string.notification_setting_save_error)
    val loadErrorMessage = stringResource(R.string.notification_setting_load_error)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NotificationSettingSideEffect.NavigateBack -> navigator.onBack()

                NotificationSettingSideEffect.OpenDeviceNotificationSettings -> {
                    // 알림 설정 화면을 못 여는 기기면 앱 정보 화면으로 대신 보낸다
                    runCatching { context.startActivity(context.buildAppNotificationSettingsIntent()) }
                        .recoverCatching { context.startActivity(context.buildAppSettingsIntent()) }
                }

                NotificationSettingSideEffect.ShowSaveError -> toastPolicy.showError(saveErrorMessage)

                NotificationSettingSideEffect.ShowLoadError -> toastPolicy.showError(loadErrorMessage)
            }
        }
    }

    // 기기 설정에서 허용하고 돌아오는 경로가 이 화면의 유일한 상태 변경 경로라 복귀 때마다 다시 읽는다
    LifecycleResumeEffect(Unit) {
        viewModel.processIntent(
            NotificationSettingIntent.UpdateDeviceNotificationEnabled(
                enabled = NotificationPermissionManager.areNotificationsEnabled(context),
            ),
        )
        onPauseOrDispose {}
    }

    YGScaffoldV2(modifier = modifier) { innerPadding ->
        NotificationSettingScreen(
            state = state,
            toastPolicy = toastPolicy,
            onClickBack = { viewModel.processIntent(NotificationSettingIntent.ClickBack) },
            onClickOpenDeviceSettings = {
                viewModel.processIntent(NotificationSettingIntent.ClickOpenDeviceSettings)
            },
            onChangeCanvasNotification = {
                viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(it))
            },
            onChangeActivityNotification = {
                viewModel.processIntent(NotificationSettingIntent.ChangeActivityNotification(it))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
