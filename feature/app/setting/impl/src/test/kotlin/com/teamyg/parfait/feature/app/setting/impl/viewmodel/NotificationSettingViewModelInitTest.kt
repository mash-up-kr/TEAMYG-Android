package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.notification.NotificationSettingVO
import com.teamyg.parfait.domain.usecase.notification.GetNotificationSettingUseCase
import com.teamyg.parfait.domain.usecase.notification.UpdateActivityNotificationUseCase
import com.teamyg.parfait.domain.usecase.notification.UpdateCanvasNotificationUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 조회가 중단 없이 끝나면 `init` 안에서 결과 처리까지 이어진다. 기본 테스트 디스패처는 실행을
 * 미뤄서 이 경로를 가리므로, 즉시 실행하는 디스패처로 따로 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingViewModelInitTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Test
    fun init_settingReturnsWithoutSuspending_isAppliedToState() {
        // Given 중단 없이 바로 돌아오는 조회
        val getNotificationSetting: GetNotificationSettingUseCase = mockk()
        coEvery { getNotificationSetting() } returns Result.success(
            NotificationSettingVO(isCanvasNotificationEnabled = true, isActivityNotificationEnabled = false),
        )

        // When 뷰모델을 만든다
        val viewModel = NotificationSettingViewModel(
            getNotificationSetting = getNotificationSetting,
            updateCanvasNotification = mockk<UpdateCanvasNotificationUseCase>(),
            updateActivityNotification = mockk<UpdateActivityNotificationUseCase>(),
            analyticsLogger = mockk<AnalyticsLogger>(relaxed = true),
        )

        // Then init 안에서 결과가 반영된다
        assertTrue(viewModel.state.value.isSettingLoaded)
        assertTrue(viewModel.state.value.isCanvasNotificationChecked)
    }
}
