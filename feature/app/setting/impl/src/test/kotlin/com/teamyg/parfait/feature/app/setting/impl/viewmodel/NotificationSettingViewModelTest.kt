package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.notification.NotificationSettingVO
import com.teamyg.parfait.domain.usecase.notification.GetNotificationSettingUseCase
import com.teamyg.parfait.domain.usecase.notification.UpdateActivityNotificationUseCase
import com.teamyg.parfait.domain.usecase.notification.UpdateCanvasNotificationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getNotificationSetting: GetNotificationSettingUseCase = mockk()
    private val updateCanvasNotification: UpdateCanvasNotificationUseCase = mockk()
    private val updateActivityNotification: UpdateActivityNotificationUseCase = mockk()

    private fun viewModel(
        setting: Result<NotificationSettingVO> = Result.success(
            NotificationSettingVO(
                isCanvasNotificationEnabled = false,
                isActivityNotificationEnabled = false,
            ),
        ),
        updateCanvasResult: Result<Unit> = Result.success(Unit),
        updateActivityResult: Result<Unit> = Result.success(Unit),
    ): NotificationSettingViewModel {
        coEvery { getNotificationSetting() } returns setting
        coEvery { updateCanvasNotification(any()) } returns updateCanvasResult
        coEvery { updateActivityNotification(any()) } returns updateActivityResult
        return NotificationSettingViewModel(
            getNotificationSetting = getNotificationSetting,
            updateCanvasNotification = updateCanvasNotification,
            updateActivityNotification = updateActivityNotification,
        )
    }

    @Test
    fun init_loadsSettingIntoState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캔버스 알림만 켜져 있는 설정
        val viewModel = viewModel(
            setting = Result.success(
                NotificationSettingVO(
                    isCanvasNotificationEnabled = true,
                    isActivityNotificationEnabled = false,
                ),
            ),
        )

        // When 조회가 끝난다
        advanceUntilIdle()

        // Then 받아온 값이 상태에 반영된다
        assertTrue(viewModel.state.value.isCanvasNotificationChecked)
        assertFalse(viewModel.state.value.isActivityNotificationChecked)
    }

    @Test
    fun init_loadFails_keepsDefaultState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 실패하는 설정
        val viewModel = viewModel(setting = Result.failure(IllegalStateException()))

        // When 조회가 끝난다
        advanceUntilIdle()

        // Then 기본값(둘 다 꺼짐)이 유지된다
        assertFalse(viewModel.state.value.isCanvasNotificationChecked)
        assertFalse(viewModel.state.value.isActivityNotificationChecked)
    }

    @Test
    fun changeCanvasNotification_updatesStateAndCallsUseCase() = runTest(mainDispatcherRule.dispatcher) {
        // Given 둘 다 꺼져 있는 상태
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 캔버스 알림을 켠다
        viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = true))
        advanceUntilIdle()

        // Then 캔버스 알림만 켜지고 변경 UseCase 가 호출된다
        assertTrue(viewModel.state.value.isCanvasNotificationChecked)
        assertFalse(viewModel.state.value.isActivityNotificationChecked)
        coVerify(exactly = 1) { updateCanvasNotification(true) }
        coVerify(exactly = 0) { updateActivityNotification(any()) }
    }

    @Test
    fun changeActivityNotification_updatesStateAndCallsUseCase() = runTest(mainDispatcherRule.dispatcher) {
        // Given 둘 다 꺼져 있는 상태
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 활동 알림을 켠다
        viewModel.processIntent(NotificationSettingIntent.ChangeActivityNotification(checked = true))
        advanceUntilIdle()

        // Then 활동 알림만 켜지고 변경 UseCase 가 호출된다
        assertFalse(viewModel.state.value.isCanvasNotificationChecked)
        assertTrue(viewModel.state.value.isActivityNotificationChecked)
        coVerify(exactly = 1) { updateActivityNotification(true) }
        coVerify(exactly = 0) { updateCanvasNotification(any()) }
    }

    @Test
    fun changeCanvasNotification_updateFails_revertsToPreviousValue() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장이 실패하는 상태
        val viewModel = viewModel(updateCanvasResult = Result.failure(IllegalStateException()))
        advanceUntilIdle()

        viewModel.effect.test {
            // When 캔버스 알림을 켠다
            viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = true))
            advanceUntilIdle()

            // Then 스위치가 이전 값(꺼짐)으로 돌아가고 저장 실패 안내 effect 가 나간다
            assertFalse(viewModel.state.value.isCanvasNotificationChecked)
            assertEquals(NotificationSettingSideEffect.ShowSaveError, awaitItem())
        }
    }

    @Test
    fun changeActivityNotification_updateFails_revertsToPreviousValue() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장이 실패하는 상태
        val viewModel = viewModel(updateActivityResult = Result.failure(IllegalStateException()))
        advanceUntilIdle()

        viewModel.effect.test {
            // When 활동 알림을 켠다
            viewModel.processIntent(NotificationSettingIntent.ChangeActivityNotification(checked = true))
            advanceUntilIdle()

            // Then 스위치가 이전 값(꺼짐)으로 돌아가고 저장 실패 안내 effect 가 나간다
            assertFalse(viewModel.state.value.isActivityNotificationChecked)
            assertEquals(NotificationSettingSideEffect.ShowSaveError, awaitItem())
        }
    }

    @Test
    fun clickBack_postsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 알림 설정 화면
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.effect.test {
            // When 설정으로 이동을 누른다
            viewModel.processIntent(NotificationSettingIntent.ClickOpenDeviceSettings)

            // Then 기기 알림 설정을 여는 effect 가 나간다
            assertEquals(NotificationSettingSideEffect.OpenDeviceNotificationSettings, awaitItem())
        }
    }

    @Test
    fun toggles_areEnabledOnlyWhenDeviceNotificationOn() = runTest(mainDispatcherRule.dispatcher) {
        // Given 기기 알림 상태를 아직 읽기 전인 화면
        val viewModel = viewModel()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isToggleEnabled)
        assertFalse(viewModel.state.value.isDeviceNotificationOff)

        // When 기기 알림이 켜져 있다고 읽힌다
        viewModel.processIntent(NotificationSettingIntent.UpdateDeviceNotificationEnabled(enabled = true))

        // Then 스위치를 조작할 수 있다
        assertTrue(viewModel.state.value.isToggleEnabled)

        // When 기기 알림이 꺼졌다고 읽힌다
        viewModel.processIntent(NotificationSettingIntent.UpdateDeviceNotificationEnabled(enabled = false))

        // Then 안내 카드를 보이고 스위치를 막는다
        assertTrue(viewModel.state.value.isDeviceNotificationOff)
        assertFalse(viewModel.state.value.isToggleEnabled)
    }
}
