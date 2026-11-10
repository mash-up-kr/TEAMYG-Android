package com.teamyg.parfait.data.source.notification.remote

import com.teamyg.parfait.domain.model.notification.NotificationSettingVO

interface NotificationSettingRemoteDataSource {
    suspend fun getNotificationSetting(): Result<NotificationSettingVO>

    suspend fun updateCanvasNotification(enabled: Boolean): Result<Unit>

    suspend fun updateActivityNotification(enabled: Boolean): Result<Unit>
}
