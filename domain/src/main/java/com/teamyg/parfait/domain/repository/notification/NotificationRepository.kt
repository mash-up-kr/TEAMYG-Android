package com.teamyg.parfait.domain.repository.notification

import com.teamyg.parfait.domain.model.notification.DeviceToken

interface NotificationRepository {

    suspend fun registerDeviceToken(deviceToken: DeviceToken): Result<Unit>
}
