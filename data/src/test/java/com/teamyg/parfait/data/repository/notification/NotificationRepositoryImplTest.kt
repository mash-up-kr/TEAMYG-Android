package com.teamyg.parfait.data.repository.notification

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.notification.remote.NotificationRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.notification.DeviceToken
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import java.io.IOException

class NotificationRepositoryImplTest {
    private val notificationRemoteDataSource: NotificationRemoteDataSource = mockk()
    private val repository = NotificationRepositoryImpl(notificationRemoteDataSource)

    @Test
    fun registerDeviceToken_remoteSucceeds_returnsSuccess() = runTest {
        // Given 원격 등록이 성공한다
        coEvery { notificationRemoteDataSource.registerDeviceToken(DeviceToken("fcm-token")) } returns
            Result.success(Unit)

        // When 기기 토큰을 등록한다
        val result = repository.registerDeviceToken(DeviceToken("fcm-token"))

        // Then 그대로 성공을 전달한다
        assertTrue(result.isSuccess)
    }

    @Test
    fun registerDeviceToken_remoteFails_mapsToAppError() = runTest {
        // Given 원격이 :data 전용 예외로 실패한다
        coEvery { notificationRemoteDataSource.registerDeviceToken(DeviceToken("fcm-token")) } returns
            Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 기기 토큰을 등록한다
        val result = repository.registerDeviceToken(DeviceToken("fcm-token"))

        // Then feature 모듈이 :data 의 ApiException 을 보지 않도록 AppError 로 바뀐다
        assertEquals(true, result.isFailure)
        assertIs<AppError.Network>(result.exceptionOrNull())
    }
}
