package com.teamyg.parfait.data.repository.notification

import com.teamyg.parfait.data.model.mapper.exception.mapErrorToAppError
import com.teamyg.parfait.data.source.notification.remote.NotificationSettingRemoteDataSource
import com.teamyg.parfait.domain.model.notification.NotificationSettingVO
import com.teamyg.parfait.domain.repository.notification.NotificationSettingRepository
import javax.inject.Inject

class NotificationSettingRepositoryImpl @Inject constructor(
    private val remoteDataSource: NotificationSettingRemoteDataSource,
) : NotificationSettingRepository {
    override suspend fun getNotificationSetting(): Result<NotificationSettingVO> = remoteDataSource
        .getNotificationSetting()
        .mapErrorToAppError()

    override suspend fun updateCanvasNotification(enabled: Boolean): Result<Unit> = remoteDataSource
        .updateCanvasNotification(enabled)
        .mapErrorToAppError()

    override suspend fun updateActivityNotification(enabled: Boolean): Result<Unit> = remoteDataSource
        .updateActivityNotification(enabled)
        .mapErrorToAppError()
}
