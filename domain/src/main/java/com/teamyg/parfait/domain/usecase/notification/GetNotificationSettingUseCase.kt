package com.teamyg.parfait.domain.usecase.notification

import com.teamyg.parfait.domain.model.notification.NotificationSettingVO
import com.teamyg.parfait.domain.repository.notification.NotificationSettingRepository
import javax.inject.Inject

class GetNotificationSettingUseCase @Inject constructor(
    private val notificationSettingRepository: NotificationSettingRepository,
) {
    suspend operator fun invoke(): Result<NotificationSettingVO> =
        notificationSettingRepository.getNotificationSetting()
}
