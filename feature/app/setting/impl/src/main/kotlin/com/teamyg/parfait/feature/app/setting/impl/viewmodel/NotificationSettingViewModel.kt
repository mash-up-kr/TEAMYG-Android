package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property isCanvasNotificationChecked 캔버스 오픈·마감 알림 수신 여부
 * @property isActivityNotificationChecked 그룹원 활동 알림을 모아 받을지 여부
 */
data class NotificationSettingState(
    val isCanvasNotificationChecked: Boolean = true,
    val isActivityNotificationChecked: Boolean = true,
) : UiState

sealed interface NotificationSettingIntent : UiIntent {
    data object ClickBack : NotificationSettingIntent

    data object ClickOpenDeviceSettings : NotificationSettingIntent

    data class ChangeCanvasNotification(val checked: Boolean) : NotificationSettingIntent

    data class ChangeActivityNotification(val checked: Boolean) : NotificationSettingIntent
}

sealed interface NotificationSettingSideEffect : UiSideEffect {
    data object NavigateBack : NotificationSettingSideEffect

    data object OpenDeviceNotificationSettings : NotificationSettingSideEffect
}

@HiltViewModel
class NotificationSettingViewModel
@Inject
constructor() : BaseViewModel<
    NotificationSettingState,
    NotificationSettingIntent,
    NotificationSettingSideEffect,
    >(initialState = NotificationSettingState()) {
    init {
        viewModelLogger.i { "NotificationSettingViewModel::init" }
    }

    override fun processIntent(intent: NotificationSettingIntent) {
        when (intent) {
            NotificationSettingIntent.ClickBack -> postSideEffect(NotificationSettingSideEffect.NavigateBack)

            NotificationSettingIntent.ClickOpenDeviceSettings ->
                postSideEffect(NotificationSettingSideEffect.OpenDeviceNotificationSettings)

            is NotificationSettingIntent.ChangeCanvasNotification ->
                updateState { copy(isCanvasNotificationChecked = intent.checked) }

            is NotificationSettingIntent.ChangeActivityNotification ->
                updateState { copy(isActivityNotificationChecked = intent.checked) }
        }
    }
}
