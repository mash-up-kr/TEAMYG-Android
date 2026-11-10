package com.teamyg.parfait.data.source.notification.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.NotificationSettingService
import com.teamyg.parfait.data.service.model.request.notification.UpdateNotificationSettingsRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.notification.NotificationSettingsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NotificationSettingRemoteDataSourceImplTest {
    private val notificationSettingService: NotificationSettingService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = NotificationSettingRemoteDataSourceImpl(
        notificationSettingService = notificationSettingService,
        apiCaller = apiCaller,
    )

    private fun successResponse(
        dailyReminderEnabled: Boolean,
        activityDigestEnabled: Boolean,
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = NotificationSettingsResponse(
            dailyReminderEnabled = dailyReminderEnabled,
            activityDigestEnabled = activityDigestEnabled,
        ),
    )

    @Test
    fun getNotificationSetting_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서버가 두 값을 내려준다
        coEvery { notificationSettingService.getNotificationsSettings() } returns
            successResponse(dailyReminderEnabled = true, activityDigestEnabled = false)

        // When 설정 조회
        val setting = dataSource.getNotificationSetting().getOrThrow()

        // Then 서버 필드명이 도메인 필드로 매핑된다
        assertTrue(setting.isCanvasNotificationEnabled)
        assertEquals(false, setting.isActivityNotificationEnabled)
    }

    @Test
    fun updateCanvasNotification_sendsOnlyCanvasFieldInRequest() = runTest {
        // Given 저장 성공
        coEvery { notificationSettingService.patchNotificationsSettings(any()) } returns
            successResponse(dailyReminderEnabled = false, activityDigestEnabled = false)

        // When 캔버스 알림을 끈다
        dataSource.updateCanvasNotification(enabled = false)

        // Then 활동 알림 필드는 건드리지 않도록 null 로 보낸다
        coVerify(exactly = 1) {
            notificationSettingService.patchNotificationsSettings(
                UpdateNotificationSettingsRequest(dailyReminderEnabled = false, activityDigestEnabled = null),
            )
        }
    }

    @Test
    fun updateActivityNotification_sendsOnlyActivityFieldInRequest() = runTest {
        // Given 저장 성공
        coEvery { notificationSettingService.patchNotificationsSettings(any()) } returns
            successResponse(dailyReminderEnabled = true, activityDigestEnabled = true)

        // When 활동 알림을 켠다
        dataSource.updateActivityNotification(enabled = true)

        // Then 캔버스 알림 필드는 건드리지 않도록 null 로 보낸다
        coVerify(exactly = 1) {
            notificationSettingService.patchNotificationsSettings(
                UpdateNotificationSettingsRequest(dailyReminderEnabled = null, activityDigestEnabled = true),
            )
        }
    }

    @Test
    fun getNotificationSetting_businessFailure_returnsBusinessException() = runTest {
        // Given 서버가 success=false 로 응답
        coEvery { notificationSettingService.getNotificationsSettings() } returns ApiResponse(
            success = false,
            code = "INVALID_REQUEST",
            message = "요청이 올바르지 않습니다",
            data = null,
        )

        // When 설정 조회
        val result = dataSource.getNotificationSetting()

        // Then Business 예외로 실패한다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_REQUEST", error.code)
    }

    @Test
    fun getNotificationSetting_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery { notificationSettingService.getNotificationsSettings() } throws IOException("connection reset")

        // When 설정 조회
        val result = dataSource.getNotificationSetting()

        // Then Network 예외로 감싸진다
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }
}
