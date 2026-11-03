package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun changeCanvasNotification_updatesOnlyCanvasState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 둘 다 켜져 있는 초기 상태
        val viewModel = NotificationSettingViewModel()

        // When 캔버스 알림을 끈다
        viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = false))

        // Then 캔버스 알림만 꺼진다
        assertFalse(viewModel.state.value.isCanvasNotificationChecked)
        assertTrue(viewModel.state.value.isActivityNotificationChecked)
    }

    @Test
    fun changeActivityNotification_updatesOnlyActivityState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 둘 다 켜져 있는 초기 상태
        val viewModel = NotificationSettingViewModel()

        // When 활동 알림을 끈다
        viewModel.processIntent(NotificationSettingIntent.ChangeActivityNotification(checked = false))

        // Then 활동 알림만 꺼진다
        assertTrue(viewModel.state.value.isCanvasNotificationChecked)
        assertFalse(viewModel.state.value.isActivityNotificationChecked)
    }

    @Test
    fun clickBack_postsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 알림 설정 화면
        val viewModel = NotificationSettingViewModel()

        viewModel.effect.test {
            // When 뒤로가기를 누른다
            viewModel.processIntent(NotificationSettingIntent.ClickBack)

            // Then 뒤로 이동 effect 가 나간다
            assertEquals(NotificationSettingSideEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun clickOpenDeviceSettings_postsOpenDeviceNotificationSettings() = runTest(mainDispatcherRule.dispatcher) {
        // Given 기기 알림이 꺼져 있어 카드가 보이는 화면
        val viewModel = NotificationSettingViewModel()

        viewModel.effect.test {
            // When 설정으로 이동을 누른다
            viewModel.processIntent(NotificationSettingIntent.ClickOpenDeviceSettings)

            // Then 기기 알림 설정을 여는 effect 가 나간다
            assertEquals(NotificationSettingSideEffect.OpenDeviceNotificationSettings, awaitItem())
        }
    }
}
