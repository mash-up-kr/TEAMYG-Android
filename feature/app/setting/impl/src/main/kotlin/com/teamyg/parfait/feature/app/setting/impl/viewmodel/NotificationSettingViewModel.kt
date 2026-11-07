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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * @property isSettingLoaded 서버 설정을 한 번이라도 받아왔는지. 받기 전의 두 스위치 값(꺼짐)은 실제 설정이
 *   아니라서 이때는 스위치를 조작할 수 없다
 * @property isDeviceNotificationEnabled 기기에서 앱 알림이 켜져 있는지. `null` 은 아직 읽기 전이다
 */
data class NotificationSettingState(
    val isCanvasNotificationChecked: Boolean = false,
    val isActivityNotificationChecked: Boolean = false,
    val isSettingLoaded: Boolean = false,
    val isDeviceNotificationEnabled: Boolean? = null,
) : UiState {
    val isDeviceNotificationOff: Boolean get() = isDeviceNotificationEnabled == false

    val isToggleEnabled: Boolean get() = isSettingLoaded && isDeviceNotificationEnabled == true
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

    data object ShowLoadError : NotificationSettingSideEffect
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

            is NotificationSettingIntent.ChangeCanvasNotification -> changeNotification(
                sync = canvasSync,
                checked = intent.checked,
                reflect = { checked -> copy(isCanvasNotificationChecked = checked) },
                save = updateCanvasNotification::invoke,
                failureMessage = "캔버스 알림 설정 변경에 실패했다",
            )

            is NotificationSettingIntent.ChangeActivityNotification -> changeNotification(
                sync = activitySync,
                checked = intent.checked,
                reflect = { checked -> copy(isActivityNotificationChecked = checked) },
                save = updateActivityNotification::invoke,
                failureMessage = "활동 알림 설정 변경에 실패했다",
            )
        }
    }

    private fun loadNotificationSetting() {
        launch(key = KEY_LOAD) {
            getNotificationSetting()
                .onSuccess { setting ->
                    canvasSync.confirmed = setting.isCanvasNotificationEnabled
                    activitySync.confirmed = setting.isActivityNotificationEnabled
                    updateState {
                        copy(
                            isCanvasNotificationChecked = setting.isCanvasNotificationEnabled,
                            isActivityNotificationChecked = setting.isActivityNotificationEnabled,
                            isSettingLoaded = true,
                        )
                    }
                }.onFailure {
                    viewModelLogger.e(it) { "알림 설정 조회에 실패했다" }
                    postSideEffect(NotificationSettingSideEffect.ShowLoadError)
                }
        }
    }

    /**
     * 스위치가 손가락을 따라 바로 움직이도록 먼저 반영하고, 저장은 스위치별로 한 줄로 세운다.
     * 실패하면 마지막으로 저장에 성공한 값으로 되돌린다.
     *
     * 뒤에 대기 중인 요청이 있을 때는 되돌리지 않는다 — 그 요청의 결과가 최종 값을 정하는데,
     * 지금 되돌리면 사용자가 방금 누른 값을 덮어쓴다.
     */
    private fun changeNotification(
        sync: ToggleSync,
        checked: Boolean,
        reflect: NotificationSettingState.(Boolean) -> NotificationSettingState,
        save: suspend (Boolean) -> Result<Unit>,
        failureMessage: String,
    ) {
        updateState { reflect(checked) }
        sync.pending++

        launch {
            try {
                sync.mutex.withLock {
                    save(checked)
                        .onSuccess { sync.confirmed = checked }
                        .onFailure {
                            viewModelLogger.e(it) { failureMessage }
                            if (sync.pending == 1) {
                                updateState { reflect(sync.confirmed) }
                                postSideEffect(NotificationSettingSideEffect.ShowSaveError)
                            }
                        }
                }
            } finally {
                sync.pending--
            }
        }
    }

    /** 스위치 하나의 저장 요청 상태. 메인 스레드에서만 건드린다 */
    private class ToggleSync {
        val mutex = Mutex()
        var confirmed = false
        var pending = 0
    }

    private val canvasSync = ToggleSync()
    private val activitySync = ToggleSync()

    private companion object {
        const val KEY_LOAD = "loadNotificationSetting"
    }
}
