package com.teamyg.parfait.data.service.model.request.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 보낸 항목만 바뀐다. null(미전송)은 서버가 바꾸지 않는다.
 *
 * `RemoteJson`(`JsonModule.kt`)이 `explicitNulls`를 끄지 않아, 채우지 않은 필드는 생략되지 않고
 * `null` 리터럴로 그대로 나간다(실기기로 확인됨). 서버가 명시적 `null`을 미전송과 동일하게
 * 처리하는 것이 이 계약의 전제다.
 */
@Serializable
data class UpdateNotificationSettingsRequest(
    @SerialName("dailyReminderEnabled")
    val dailyReminderEnabled: Boolean? = null,
    @SerialName("activityDigestEnabled")
    val activityDigestEnabled: Boolean? = null,
)
