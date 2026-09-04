package com.teamyg.parfait.feature.groups.enter.impl.component

import com.teamyg.parfait.domain.usecase.notification.RegisterCurrentDeviceTokenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class NotificationPermissionViewModelTest {
    private val registerCurrentDeviceTokenUseCase: RegisterCurrentDeviceTokenUseCase = mockk()

    /**
     * `applicationScope` 로 `this`(테스트의 [kotlinx.coroutines.test.TestScope])를 그대로
     * 넘긴다 — `viewModelScope` 였다면 `Dispatchers.Main` 이 필요해 [MainDispatcherRule] 없이는
     * 못 돌았을 자리다. 생성자로 스코프를 받는 설계 덕에 이 프로덕션 코드가 그대로 테스트된다.
     */
    @Test
    fun onNotificationPermissionGranted_registersTheCurrentToken() = runTest {
        // Given 토큰 등록이 성공한다
        coEvery { registerCurrentDeviceTokenUseCase() } returns Result.success(Unit)
        val viewModel = NotificationPermissionViewModel(
            registerCurrentDeviceTokenUseCase = registerCurrentDeviceTokenUseCase,
            applicationScope = this,
        )

        // When 알림 권한을 허용한다
        viewModel.onNotificationPermissionGranted()
        advanceUntilIdle()

        // Then 지금 토큰을 등록한다
        coVerify(exactly = 1) { registerCurrentDeviceTokenUseCase() }
    }

    @Test
    fun onNotificationPermissionGranted_registrationFails_doesNotThrow() = runTest {
        // Given 서버 등록이 실패한다
        coEvery { registerCurrentDeviceTokenUseCase() } returns Result.failure(RuntimeException("서버 오류"))
        val viewModel = NotificationPermissionViewModel(
            registerCurrentDeviceTokenUseCase = registerCurrentDeviceTokenUseCase,
            applicationScope = this,
        )

        // When 알림 권한을 허용한다
        viewModel.onNotificationPermissionGranted()
        advanceUntilIdle()

        // Then 예외를 밖으로 던지지 않고 로그만 남긴다 — 여기까지 오면(예외 없이) 통과
        coVerify(exactly = 1) { registerCurrentDeviceTokenUseCase() }
    }
}
