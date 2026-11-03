package com.teamyg.parfait.domain.usecase.notification

import com.teamyg.parfait.domain.model.notification.NotificationSettingVO
import com.teamyg.parfait.domain.repository.notification.NotificationSettingRepository
import javax.inject.Inject

// TODO: 서버 작업이 완료되면 실제 서버 연동을 추가한다. 지금은 저장소가 mock 값을 내려준다
class GetNotificationSettingUseCase @Inject constructor(
    private val notificationSettingRepository: NotificationSettingRepository,
) {
    suspend operator fun invoke(): Result<NotificationSettingVO> =
        notificationSettingRepository.getNotificationSetting()
}
