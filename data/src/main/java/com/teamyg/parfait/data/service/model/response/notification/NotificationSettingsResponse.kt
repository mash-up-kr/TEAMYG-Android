package com.teamyg.parfait.data.service.model.response.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param dailyReminderEnabled 오늘의 캔버스 오픈·마감 알림 (서버 S-105 `dailyReminderEnabled`)
 * @param activityDigestEnabled 그룹원 활동 알림 모아 받기 (서버 S-105 `activityDigestEnabled`)
 */
@Serializable
data class NotificationSettingsResponse(
    @SerialName("dailyReminderEnabled")
    val dailyReminderEnabled: Boolean,
    @SerialName("activityDigestEnabled")
    val activityDigestEnabled: Boolean,
)
