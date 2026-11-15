package com.teamyg.parfait.data.repository.notification

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.notification.remote.NotificationSettingRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.notification.NotificationSettingVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NotificationSettingRepositoryImplTest {
    private val remoteDataSource: NotificationSettingRemoteDataSource = mockk()
    private val repository = NotificationSettingRepositoryImpl(remoteDataSource)

    private val setting = NotificationSettingVO(
        isCanvasNotificationEnabled = true,
        isActivityNotificationEnabled = false,
    )

    @Test
    fun getNotificationSetting_dataSourceReturnsSuccess_returnsSameValue() = runTest {
        // Given 원격 데이터소스가 설정을 준다
        coEvery { remoteDataSource.getNotificationSetting() } returns Result.success(setting)

        // When 설정 조회
        val result = repository.getNotificationSetting()

        // Then 값을 가공 없이 그대로 전달한다
        assertEquals(setting, result.getOrThrow())
    }

    @Test
    fun updateCanvasNotification_delegatesToDataSourceWithSameValue() = runTest {
        // Given 저장 성공
        coEvery { remoteDataSource.updateCanvasNotification(false) } returns Result.success(Unit)

        // When 캔버스 알림을 끈다
        repository.updateCanvasNotification(enabled = false)

        // Then 원격 데이터소스에 같은 값으로 위임한다
        coVerify(exactly = 1) { remoteDataSource.updateCanvasNotification(false) }
    }

    @Test
    fun updateActivityNotification_delegatesToDataSourceWithSameValue() = runTest {
        // Given 저장 성공
        coEvery { remoteDataSource.updateActivityNotification(true) } returns Result.success(Unit)

        // When 활동 알림을 켠다
        repository.updateActivityNotification(enabled = true)

        // Then 원격 데이터소스에 같은 값으로 위임한다
        coVerify(exactly = 1) { remoteDataSource.updateActivityNotification(true) }
    }

    @Test
    fun getNotificationSetting_dataSourceFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결 실패
        coEvery { remoteDataSource.getNotificationSetting() } returns
            Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 설정 조회
        val result = repository.getNotificationSetting()

        // Then Network 갈래로 바뀌어 나온다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun updateCanvasNotification_dataSourceFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 서버가 에러 envelope 로 응답
        coEvery { remoteDataSource.updateCanvasNotification(false) } returns Result.failure(
            ApiException.Business(
                code = "INVALID_REQUEST",
                serverMessage = "요청이 올바르지 않습니다",
                statusCode = 400,
                errorDetail = null,
            ),
        )

        // When 캔버스 알림을 끈다
        val result = repository.updateCanvasNotification(enabled = false)

        // Then 도메인 에러로 바뀌어 나온다(호출부가 :data 를 보지 않아도 된다)
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("INVALID_REQUEST", error.code)
        assertEquals(400, error.statusCode)
    }
}
