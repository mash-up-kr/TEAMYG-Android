package com.teamyg.parfait.domain.usecase.notification

import com.teamyg.parfait.domain.repository.notification.NotificationSettingRepository
import javax.inject.Inject

class UpdateActivityNotificationUseCase @Inject constructor(
    private val notificationSettingRepository: NotificationSettingRepository,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> =
        notificationSettingRepository.updateActivityNotification(enabled)
}
