package com.teamyg.parfait.data.repository.notification

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.notification.remote.NotificationRemoteDataSource
import com.teamyg.parfait.domain.model.notification.DeviceToken
import com.teamyg.parfait.domain.provider.DeviceTokenProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test

class NotificationRepositoryImplTest {
    private val remoteDataSource: NotificationRemoteDataSource = mockk()
    private val deviceTokenProvider: DeviceTokenProvider = mockk {
        coEvery { currentToken() } returns TOKEN
    }

    private fun TestScope.repository() =
        NotificationRepositoryImpl(remoteDataSource, deviceTokenProvider, TestScope(testScheduler))

    @Test
    fun registerCurrentDeviceToken_success_callsOnce() = runTest {
        // Given 등록이 한 번에 성공한다
        coEvery { remoteDataSource.registerDeviceToken(TOKEN) } returns Result.success(Unit)

        // When 등록을 건다
        repository().registerCurrentDeviceToken()
        advanceUntilIdle()

        // Then 지금 토큰으로 재시도 없이 끝난다
        coVerify(exactly = 1) { remoteDataSource.registerDeviceToken(TOKEN) }
    }

    @Test
    fun registerCurrentDeviceToken_transientFailure_retriesUntilSuccess() = runTest {
        // Given 두 번 실패한 뒤 성공한다
        coEvery { remoteDataSource.registerDeviceToken(TOKEN) } returnsMany listOf(
            Result.failure(ApiException.Network(IOException("reset"))),
            Result.failure(ApiException.Network(IOException("reset"))),
            Result.success(Unit),
        )

        // When 등록을 건다
        repository().registerCurrentDeviceToken()
        advanceUntilIdle()

        // Then 성공할 때까지만 다시 시도한다
        coVerify(exactly = 3) { remoteDataSource.registerDeviceToken(TOKEN) }
    }

    @Test
    fun registerCurrentDeviceToken_keepsFailing_stopsAtAttemptLimit() = runTest {
        // Given 계속 실패한다
        coEvery { remoteDataSource.registerDeviceToken(TOKEN) } returns
            Result.failure(ApiException.Network(IOException("reset")))

        // When 등록을 건다
        repository().registerCurrentDeviceToken()
        advanceUntilIdle()

        // Then 무한히 돌지 않는다 — 나머지는 다음 세션 트리거가 메운다
        coVerify(exactly = NotificationRepositoryImpl.MAX_ATTEMPTS) { remoteDataSource.registerDeviceToken(TOKEN) }
    }

    @Test
    fun registerCurrentDeviceToken_tokenFetchFails_retriesWithoutCallingServer() = runTest {
        // Given SDK 가 아직 토큰을 못 줬다가 두 번째에 준다
        coEvery { deviceTokenProvider.currentToken() } throws IllegalStateException("not ready") andThen TOKEN
        coEvery { remoteDataSource.registerDeviceToken(TOKEN) } returns Result.success(Unit)

        // When 등록을 건다
        repository().registerCurrentDeviceToken()
        advanceUntilIdle()

        // Then 조회 실패는 크래시가 아니라 재시도 대상이고, 토큰 없이 서버를 부르지 않는다
        coVerify(exactly = 2) { deviceTokenProvider.currentToken() }
        coVerify(exactly = 1) { remoteDataSource.registerDeviceToken(TOKEN) }
    }

    @Test
    fun registerCurrentDeviceToken_whileInFlight_doesNotStartSecondRequest() = runTest {
        // Given 첫 등록이 아직 응답을 못 받았다
        val inFlight = CompletableDeferred<Result<Unit>>()
        coEvery { remoteDataSource.registerDeviceToken(TOKEN) } coAnswers { inFlight.await() }
        val repository = repository()
        repository.registerCurrentDeviceToken()
        advanceUntilIdle()

        // When 끝나기 전에 또 건다
        repository.registerCurrentDeviceToken()
        advanceUntilIdle()

        // Then 같은 토큰으로 동시 요청을 보내지 않는다 — 서버가 유니크 제약 위반으로 500 을 준다
        coVerify(exactly = 1) { remoteDataSource.registerDeviceToken(TOKEN) }

        inFlight.complete(Result.success(Unit))
        advanceUntilIdle()
    }

    private companion object {
        val TOKEN = DeviceToken("fcm-token")
    }
}
