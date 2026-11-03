package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.notification.GetNotificationSettingUseCase
import com.teamyg.parfait.domain.usecase.notification.UpdateActivityNotificationUseCase
import com.teamyg.parfait.domain.usecase.notification.UpdateCanvasNotificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property isDeviceNotificationEnabled 기기에서 앱 알림이 켜져 있는지. `null` 은 아직 읽기 전이다
 */
data class NotificationSettingState(
    val isCanvasNotificationChecked: Boolean = false,
    val isActivityNotificationChecked: Boolean = false,
    val isDeviceNotificationEnabled: Boolean? = null,
) : UiState {
    val isDeviceNotificationOff: Boolean get() = isDeviceNotificationEnabled == false

    val isToggleEnabled: Boolean get() = isDeviceNotificationEnabled == true
}

sealed interface NotificationSettingIntent : UiIntent {
    data object ClickBack : NotificationSettingIntent

    data object ClickOpenDeviceSettings : NotificationSettingIntent

    data class UpdateDeviceNotificationEnabled(val enabled: Boolean) : NotificationSettingIntent

    data class ChangeCanvasNotification(val checked: Boolean) : NotificationSettingIntent

    data class ChangeActivityNotification(val checked: Boolean) : NotificationSettingIntent
}

sealed interface NotificationSettingSideEffect : UiSideEffect {
    data object NavigateBack : NotificationSettingSideEffect

    data object OpenDeviceNotificationSettings : NotificationSettingSideEffect

    data object ShowSaveError : NotificationSettingSideEffect
}

@HiltViewModel
class NotificationSettingViewModel
@Inject
constructor(
    private val getNotificationSetting: GetNotificationSettingUseCase,
    private val updateCanvasNotification: UpdateCanvasNotificationUseCase,
    private val updateActivityNotification: UpdateActivityNotificationUseCase,
) : BaseViewModel<
    NotificationSettingState,
    NotificationSettingIntent,
    NotificationSettingSideEffect,
    >(initialState = NotificationSettingState()) {
    init {
        viewModelLogger.i { "NotificationSettingViewModel::init" }

        loadNotificationSetting()
    }

    override fun processIntent(intent: NotificationSettingIntent) {
        when (intent) {
            NotificationSettingIntent.ClickBack -> postSideEffect(NotificationSettingSideEffect.NavigateBack)

            NotificationSettingIntent.ClickOpenDeviceSettings ->
                postSideEffect(NotificationSettingSideEffect.OpenDeviceNotificationSettings)

            is NotificationSettingIntent.UpdateDeviceNotificationEnabled ->
                updateState { copy(isDeviceNotificationEnabled = intent.enabled) }

            is NotificationSettingIntent.ChangeCanvasNotification -> handleChangeCanvasNotification(intent.checked)

            is NotificationSettingIntent.ChangeActivityNotification -> handleChangeActivityNotification(intent.checked)
        }
    }

    private fun loadNotificationSetting() {
        launch(key = KEY_LOAD) {
            getNotificationSetting()
                .onSuccess { setting ->
                    updateState {
                        copy(
                            isCanvasNotificationChecked = setting.isCanvasNotificationEnabled,
                            isActivityNotificationChecked = setting.isActivityNotificationEnabled,
                        )
                    }
                }.onFailure { viewModelLogger.e(it) { "알림 설정 조회에 실패했다" } }
        }
    }

    // 스위치가 손가락을 따라 바로 움직이도록 먼저 반영하고, 저장에 실패하면 이전 값으로 되돌린다
    private fun handleChangeCanvasNotification(checked: Boolean) {
        val previous = state.value.isCanvasNotificationChecked
        updateState { copy(isCanvasNotificationChecked = checked) }

        launch {
            updateCanvasNotification(checked).onFailure {
                viewModelLogger.e(it) { "캔버스 알림 설정 변경에 실패했다" }
                updateState { copy(isCanvasNotificationChecked = previous) }
                postSideEffect(NotificationSettingSideEffect.ShowSaveError)
            }
        }
    }

    private fun handleChangeActivityNotification(checked: Boolean) {
        val previous = state.value.isActivityNotificationChecked
        updateState { copy(isActivityNotificationChecked = checked) }

        launch {
            updateActivityNotification(checked).onFailure {
                viewModelLogger.e(it) { "활동 알림 설정 변경에 실패했다" }
                updateState { copy(isActivityNotificationChecked = previous) }
                postSideEffect(NotificationSettingSideEffect.ShowSaveError)
            }
        }
    }

    private companion object {
        const val KEY_LOAD = "loadNotificationSetting"
    }
}
