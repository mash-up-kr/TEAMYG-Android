package com.teamyg.parfait.domain.usecase.notification

import com.teamyg.parfait.domain.model.notification.DeviceToken
import com.teamyg.parfait.domain.repository.notification.NotificationRepository
import javax.inject.Inject

/** 이미 알고 있는 토큰 하나를 서버에 등록한다 — FCM `onNewToken` 콜백처럼 토큰이 주어지는 자리용. */
class RegisterDeviceTokenUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(deviceToken: DeviceToken): Result<Unit> =
        notificationRepository.registerDeviceToken(deviceToken)
}
