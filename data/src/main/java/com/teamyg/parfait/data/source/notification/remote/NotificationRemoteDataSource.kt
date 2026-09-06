package com.teamyg.parfait.data.source.notification.remote

import com.teamyg.parfait.domain.model.notification.DeviceToken

interface NotificationRemoteDataSource {
    suspend fun registerDeviceToken(deviceToken: DeviceToken): Result<Unit>
}
