package com.teamyg.parfait.data.service.model.request.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 보낸 항목만 바뀐다. null(미전송)은 서버가 바꾸지 않는다. */
@Serializable
data class UpdateNotificationSettingsRequest(
    @SerialName("dailyReminderEnabled")
    val dailyReminderEnabled: Boolean? = null,
    @SerialName("activityDigestEnabled")
    val activityDigestEnabled: Boolean? = null,
)
