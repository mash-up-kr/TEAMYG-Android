package com.teamyg.parfait.data.source.notification.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.NotificationSettingService
import com.teamyg.parfait.data.service.model.request.notification.UpdateNotificationSettingsRequest
import com.teamyg.parfait.data.source.notification.mapper.toNotificationSettingVO
import com.teamyg.parfait.domain.model.notification.NotificationSettingVO
import javax.inject.Inject

class NotificationSettingRemoteDataSourceImpl @Inject constructor(
    private val notificationSettingService: NotificationSettingService,
    private val apiCaller: ApiCaller,
) : NotificationSettingRemoteDataSource {
    override suspend fun getNotificationSetting(): Result<NotificationSettingVO> = apiCaller
        .safeApiCall(
            block = { notificationSettingService.getNotificationsSettings() },
            transform = { it.toNotificationSettingVO() },
        )

    override suspend fun updateCanvasNotification(enabled: Boolean): Result<Unit> = apiCaller
        .safeApiCall(
            block = {
                notificationSettingService.patchNotificationsSettings(
                    UpdateNotificationSettingsRequest(dailyReminderEnabled = enabled),
                )
            },
            transform = {},
        )

    override suspend fun updateActivityNotification(enabled: Boolean): Result<Unit> = apiCaller
        .safeApiCall(
            block = {
                notificationSettingService.patchNotificationsSettings(
                    UpdateNotificationSettingsRequest(activityDigestEnabled = enabled),
                )
            },
            transform = {},
        )
}
