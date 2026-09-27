package com.teamyg.parfait.domain.usecase.notification

import com.teamyg.parfait.domain.repository.notification.NotificationRepository
import javax.inject.Inject

class RegisterCurrentDeviceTokenUseCase
@Inject
constructor(
    private val notificationRepository: NotificationRepository,
) {
    operator fun invoke() = notificationRepository.registerCurrentDeviceToken()
}
