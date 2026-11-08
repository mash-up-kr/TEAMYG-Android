package com.teamyg.parfait.domain.repository.notification

import com.teamyg.parfait.domain.model.notification.NotificationSettingVO

interface NotificationSettingRepository {
    suspend fun getNotificationSetting(): Result<NotificationSettingVO>

    suspend fun updateCanvasNotification(enabled: Boolean): Result<Unit>

    suspend fun updateActivityNotification(enabled: Boolean): Result<Unit>
}
