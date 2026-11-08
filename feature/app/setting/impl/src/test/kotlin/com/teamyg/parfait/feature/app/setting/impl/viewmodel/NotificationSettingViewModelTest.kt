package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.notification.NotificationSettingVO
import com.teamyg.parfait.domain.usecase.notification.GetNotificationSettingUseCase
import com.teamyg.parfait.domain.usecase.notification.UpdateActivityNotificationUseCase
import com.teamyg.parfait.domain.usecase.notification.UpdateCanvasNotificationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

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
            analyticsLogger = analyticsLogger,
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
    fun init_loadSucceeds_marksSettingLoaded() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 성공하는 설정
        val viewModel = viewModel()

        // When 조회가 끝난다
        advanceUntilIdle()

        // Then 불러온 것으로 표시된다
        assertTrue(viewModel.state.value.isSettingLoaded)
    }

    @Test
    fun init_loadFails_keepsTogglesDisabledAndShowsLoadError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 실패하는 설정
        val viewModel = viewModel(setting = Result.failure(IllegalStateException()))
        viewModel.processIntent(NotificationSettingIntent.UpdateDeviceNotificationEnabled(enabled = true))

        viewModel.effect.test {
            // When 조회가 끝난다
            advanceUntilIdle()

            // Then 기본값이 실제 설정처럼 조작되지 않도록 스위치를 막고 실패를 알린다
            assertFalse(viewModel.state.value.isSettingLoaded)
            assertFalse(viewModel.state.value.isToggleEnabled)
            assertEquals(NotificationSettingSideEffect.ShowLoadError, awaitItem())
        }
    }

    @Test
    fun toggles_areEnabledOnlyWhenSettingLoadedAndDeviceNotificationOn() = runTest(mainDispatcherRule.dispatcher) {
        // Given 설정을 불러온 화면 — 기기 알림 상태는 아직 읽기 전이다
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

    @Test
    fun changeNotification_updateFails_revertsToLastConfirmedValue() = runTest(mainDispatcherRule.dispatcher) {
        // Given 한 번 저장에 성공해 켜짐이 확정된 상태
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = true))
        advanceUntilIdle()

        // When 끄는 저장이 실패한다
        coEvery { updateCanvasNotification(false) } returns Result.failure(IllegalStateException())
        viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = false))
        advanceUntilIdle()

        // Then 마지막으로 확정된 값(켜짐)으로 돌아간다
        assertTrue(viewModel.state.value.isCanvasNotificationChecked)
    }

    @Test
    fun changeNotification_olderRequestFailsWhileNewerPending_doesNotOverwriteLatestValue() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 첫 저장 요청이 늦게 실패하도록 붙잡아 둔 상태
            val viewModel = viewModel()
            advanceUntilIdle()
            val firstResult = CompletableDeferred<Result<Unit>>()
            coEvery { updateCanvasNotification(true) } coAnswers { firstResult.await() } andThen Result.success(Unit)
            coEvery { updateCanvasNotification(false) } returns Result.success(Unit)

            viewModel.effect.test {
                // When 켬 → 끔 → 켬 으로 연달아 누르고 첫 요청이 그제서야 실패한다
                viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = true))
                viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = false))
                viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = true))
                runCurrent()
                firstResult.complete(Result.failure(IllegalStateException()))
                advanceUntilIdle()

                // Then 사용자가 마지막에 누른 값(켜짐)이 유지되고 실패 안내는 나가지 않는다
                assertTrue(viewModel.state.value.isCanvasNotificationChecked)
                expectNoEvents()
            }
        }

    @Test
    fun changeCanvasNotification_success_logsChangedEventWithTypeAndEnabled() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장이 성공하는 상태
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 캔버스 알림을 켠다
        viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = true))
        advanceUntilIdle()

        // Then type=canvas, enabled=true 로 변경 이벤트가 한 번 나간다
        verify(exactly = 1) {
            analyticsLogger.logEvent(
                AnalyticsEvent(
                    type = AnalyticsEventType.APP_NOTIFICATION_SETTING_CHANGED,
                    params = mapOf("type" to "canvas", "enabled" to "true"),
                ),
            )
        }
    }

    @Test
    fun changeActivityNotification_success_logsChangedEventWithTypeAndEnabled() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 저장이 성공하는 상태
            val viewModel = viewModel()
            advanceUntilIdle()

            // When 활동 알림을 켠다
            viewModel.processIntent(NotificationSettingIntent.ChangeActivityNotification(checked = true))
            advanceUntilIdle()

            // Then type=activity, enabled=true 로 변경 이벤트가 한 번 나간다
            verify(exactly = 1) {
                analyticsLogger.logEvent(
                    AnalyticsEvent(
                        type = AnalyticsEventType.APP_NOTIFICATION_SETTING_CHANGED,
                        params = mapOf("type" to "activity", "enabled" to "true"),
                    ),
                )
            }
        }

    @Test
    fun changeNotification_updateFails_doesNotLogChangedEvent() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장이 실패하는 상태
        val viewModel = viewModel(
            updateCanvasResult = Result.failure(IllegalStateException()),
            updateActivityResult = Result.failure(IllegalStateException()),
        )
        advanceUntilIdle()

        // When 두 알림을 각각 켠다
        viewModel.processIntent(NotificationSettingIntent.ChangeCanvasNotification(checked = true))
        viewModel.processIntent(NotificationSettingIntent.ChangeActivityNotification(checked = true))
        advanceUntilIdle()

        // Then 되돌아간 변경은 이벤트로 남기지 않는다
        verify(exactly = 0) { analyticsLogger.logEvent(any()) }
    }

    @Test
    fun clickOpenDeviceSettings_logsDeviceSettingsTappedEvent() = runTest(mainDispatcherRule.dispatcher) {
        // Given 알림 설정 화면
        val viewModel = viewModel()

        // When 설정으로 이동을 누른다
        viewModel.processIntent(NotificationSettingIntent.ClickOpenDeviceSettings)

        // Then 기기 설정 이동 이벤트가 나간다
        verify(exactly = 1) {
            analyticsLogger.logEvent(AnalyticsEvent(type = AnalyticsEventType.APP_NOTIFICATION_DEVICE_SETTINGS_TAPPED))
        }
    }

    @Test
    fun refresh_reloadsSettingAndEndsRefreshing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 처음엔 모두 꺼져 있다가 서버에서 캔버스 알림이 켜진 상태
        val viewModel = viewModel()
        advanceUntilIdle()
        coEvery { getNotificationSetting() } returns Result.success(
            NotificationSettingVO(isCanvasNotificationEnabled = true, isActivityNotificationEnabled = false),
        )

        // When 당겨서 새로고침한다
        viewModel.processIntent(NotificationSettingIntent.Refresh)
        advanceUntilIdle()

        // Then 최신 값이 반영되고 새로고침 표시는 끝난다
        assertTrue(viewModel.state.value.isCanvasNotificationChecked)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun refresh_loadFails_showsErrorAndEndsRefreshing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 처음 조회는 성공했지만 다시 조회하면 실패한다
        val viewModel = viewModel()
        advanceUntilIdle()
        coEvery { getNotificationSetting() } returns Result.failure(IllegalStateException())

        viewModel.effect.test {
            // When 당겨서 새로고침한다
            viewModel.processIntent(NotificationSettingIntent.Refresh)
            advanceUntilIdle()

            // Then 에러를 알리고 새로고침 표시는 끝난다
            assertEquals(NotificationSettingSideEffect.ShowLoadError, awaitItem())
            assertFalse(viewModel.state.value.isRefreshing)
        }
    }
}
