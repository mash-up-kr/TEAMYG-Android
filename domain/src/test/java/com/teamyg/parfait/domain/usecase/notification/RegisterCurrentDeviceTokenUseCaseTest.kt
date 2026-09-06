package com.teamyg.parfait.domain.usecase.notification

import com.teamyg.parfait.domain.model.notification.DeviceToken
import com.teamyg.parfait.domain.notification.DeviceTokenProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegisterCurrentDeviceTokenUseCaseTest {
    private val deviceTokenProvider: DeviceTokenProvider = mockk()
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase = mockk()
    private val registerCurrentDeviceToken = RegisterCurrentDeviceTokenUseCase(
        deviceTokenProvider = deviceTokenProvider,
        registerDeviceTokenUseCase = registerDeviceTokenUseCase,
    )

    @Test
    fun invoke_tokenAvailable_registersIt() = runTest {
        // Given SDK 가 이미 토큰을 발급해 뒀다
        coEvery { deviceTokenProvider.currentToken() } returns DeviceToken("fcm-token")
        coEvery { registerDeviceTokenUseCase(DeviceToken("fcm-token")) } returns Result.success(Unit)

        // When 지금 토큰을 등록한다
        val result = registerCurrentDeviceToken()

        // Then 그 토큰으로 등록을 부른다
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { registerDeviceTokenUseCase(DeviceToken("fcm-token")) }
    }

    @Test
    fun invoke_tokenProviderThrows_returnsFailureInstead() = runTest {
        // Given Firebase SDK 쪽 Task 가 예외로 실패한다
        val failure = IllegalStateException("token fetch failed")
        coEvery { deviceTokenProvider.currentToken() } throws failure

        // When 지금 토큰을 등록한다
        val result = registerCurrentDeviceToken()

        // Then 이 계층의 관례(항상 Result 로만 실패를 알림)를 지키려고 여기서 잡아 옮긴다
        assertIs<IllegalStateException>(result.exceptionOrNull())
        coVerify(exactly = 0) { registerDeviceTokenUseCase(any()) }
    }

    @Test
    fun invoke_registrationFails_propagatesTheFailure() = runTest {
        // Given 토큰은 있는데 서버 등록이 실패한다
        val failure = RuntimeException("server rejected")
        coEvery { deviceTokenProvider.currentToken() } returns DeviceToken("fcm-token")
        coEvery { registerDeviceTokenUseCase(DeviceToken("fcm-token")) } returns Result.failure(failure)

        // When 지금 토큰을 등록한다
        val result = registerCurrentDeviceToken()

        // Then 그 실패를 그대로 돌려준다
        assertSame(failure, result.exceptionOrNull())
    }
}
