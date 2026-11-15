package com.teamyg.parfait.data.source.notification.mapper

import com.teamyg.parfait.data.service.model.response.notification.NotificationSettingsResponse
import com.teamyg.parfait.domain.model.notification.NotificationSettingVO

internal fun NotificationSettingsResponse.toNotificationSettingVO(): NotificationSettingVO = NotificationSettingVO(
    isCanvasNotificationEnabled = dailyReminderEnabled,
    isActivityNotificationEnabled = activityDigestEnabled,
)
