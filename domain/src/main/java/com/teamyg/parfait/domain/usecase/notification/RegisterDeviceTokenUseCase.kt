package com.teamyg.parfait.domain.usecase.notification

import com.teamyg.parfait.domain.model.notification.DeviceToken
import com.teamyg.parfait.domain.repository.notification.NotificationRepository
import javax.inject.Inject

/** 토큰 값이 이미 손에 있는 자리용. 값 없이 부르려면 [RegisterCurrentDeviceTokenUseCase]. */
class RegisterDeviceTokenUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(deviceToken: DeviceToken): Result<Unit> =
        notificationRepository.registerDeviceToken(deviceToken)
}
