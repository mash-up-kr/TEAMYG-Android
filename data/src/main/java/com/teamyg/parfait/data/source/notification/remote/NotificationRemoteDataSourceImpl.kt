package com.teamyg.parfait.data.source.notification.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.NotificationService
import com.teamyg.parfait.data.service.model.request.notification.RegisterDeviceTokenRequest
import com.teamyg.parfait.domain.model.notification.DeviceToken
import javax.inject.Inject

class NotificationRemoteDataSourceImpl @Inject constructor(
    private val notificationService: NotificationService,
    private val apiCaller: ApiCaller,
) : NotificationRemoteDataSource {
    /**
     * 서버가 token 을 유일 키로 upsert 하므로 앱 시작·토큰 갱신처럼 반복되는 자리에서 그대로
     * 불러도 된다(`api/notification.md`).
     */
    override suspend fun registerDeviceToken(deviceToken: DeviceToken): Result<Unit> = apiCaller
        .safeApiCallNoContent {
            notificationService.postNotificationsDevices(
                request = RegisterDeviceTokenRequest(
                    token = deviceToken.value,
                    platform = PLATFORM_ANDROID,
                ),
            )
        }

    companion object {
        private const val PLATFORM_ANDROID = "ANDROID"
    }
}
