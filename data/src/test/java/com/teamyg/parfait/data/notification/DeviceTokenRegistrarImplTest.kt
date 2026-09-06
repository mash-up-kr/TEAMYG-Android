package com.teamyg.parfait.data.notification

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.usecase.notification.RegisterCurrentDeviceTokenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeviceTokenRegistrarImplTest {
    private val registerCurrentDeviceToken: RegisterCurrentDeviceTokenUseCase = mockk()

    @Test
    fun register_success_callsOnce() = runTest {
        // Given 등록이 한 번에 성공한다
        coEvery { registerCurrentDeviceToken() } returns Result.success(Unit)
        val registrar = DeviceTokenRegistrarImpl(registerCurrentDeviceToken, TestScope(testScheduler))

        // When 등록을 건다
        registrar.register()
        advanceUntilIdle()

        // Then 재시도 없이 끝난다
        coVerify(exactly = 1) { registerCurrentDeviceToken() }
    }

    @Test
    fun register_transientFailure_retriesUntilSuccess() = runTest {
        // Given 두 번 실패한 뒤 성공한다
        coEvery { registerCurrentDeviceToken() } returnsMany listOf(
            Result.failure(AppError.Network(cause = null)),
            Result.failure(AppError.Network(cause = null)),
            Result.success(Unit),
        )
        val registrar = DeviceTokenRegistrarImpl(registerCurrentDeviceToken, TestScope(testScheduler))

        // When 등록을 건다
        registrar.register()
        advanceUntilIdle()

        // Then 성공할 때까지만 다시 시도한다 — 성공 뒤에는 더 부르지 않는다
        coVerify(exactly = 3) { registerCurrentDeviceToken() }
    }

    @Test
    fun register_keepsFailing_stopsAtAttemptLimit() = runTest {
        // Given 계속 실패한다
        coEvery { registerCurrentDeviceToken() } returns Result.failure(AppError.Network(cause = null))
        val registrar = DeviceTokenRegistrarImpl(registerCurrentDeviceToken, TestScope(testScheduler))

        // When 등록을 건다
        registrar.register()
        advanceUntilIdle()

        // Then 무한히 돌지 않는다 — 나머지는 다음 세션 트리거가 메운다
        coVerify(exactly = DeviceTokenRegistrarImpl.MAX_ATTEMPTS) { registerCurrentDeviceToken() }
    }

    @Test
    fun register_whileInFlight_doesNotStartSecondRequest() = runTest {
        // Given 첫 등록이 아직 응답을 못 받았다
        val inFlight = CompletableDeferred<Result<Unit>>()
        coEvery { registerCurrentDeviceToken() } coAnswers { inFlight.await() }
        val registrar = DeviceTokenRegistrarImpl(registerCurrentDeviceToken, TestScope(testScheduler))
        registrar.register()
        advanceUntilIdle()

        // When 끝나기 전에 또 건다
        registrar.register()
        advanceUntilIdle()

        // Then 같은 토큰으로 동시 요청을 보내지 않는다 — 서버가 유니크 제약 위반으로 500 을 준다
        coVerify(exactly = 1) { registerCurrentDeviceToken() }

        inFlight.complete(Result.success(Unit))
        advanceUntilIdle()
    }
}
