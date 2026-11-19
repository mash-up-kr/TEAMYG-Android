package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.notification.UpdateNotificationSettingsRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.notification.NotificationSettingsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface NotificationSettingService {
    @GET("api/v1/notifications/settings")
    suspend fun getNotificationsSettings(): ApiResponse<NotificationSettingsResponse>

    /** 보낸 항목만 변경된다. 변경 후 최종 설정 전체를 돌려받는다. */
    @PATCH("api/v1/notifications/settings")
    suspend fun patchNotificationsSettings(
        @Body request: UpdateNotificationSettingsRequest,
    ): ApiResponse<NotificationSettingsResponse>
}
