package com.teamyg.parfait.data.repository.notification

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.notification.remote.NotificationRemoteDataSource
import com.teamyg.parfait.domain.model.notification.DeviceToken
import com.teamyg.parfait.domain.repository.notification.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
) : NotificationRepository {
    override suspend fun registerDeviceToken(deviceToken: DeviceToken): Result<Unit> = notificationRemoteDataSource
        .registerDeviceToken(deviceToken)
        .mapErrorToAppError()
}
