package com.teamyg.parfait.data.repository.notification

import com.teamyg.parfait.domain.model.notification.NotificationSettingVO
import com.teamyg.parfait.domain.repository.notification.NotificationSettingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 연동 전 임시 구현이다. 서버 대신 메모리에 설정을 들고 있고, 앱이 종료되면 초기 mock 값으로 돌아간다.
 * 서버 API 가 정해지면 이 클래스를 원격 데이터소스 호출로 교체한다.
 */
@Singleton
class NotificationSettingRepositoryImpl @Inject constructor() : NotificationSettingRepository {
    private val setting = MutableStateFlow(
        NotificationSettingVO(
            isCanvasNotificationEnabled = true,
            isActivityNotificationEnabled = false,
        ),
    )

    override suspend fun getNotificationSetting(): Result<NotificationSettingVO> = Result.success(setting.value)

    override suspend fun updateCanvasNotification(enabled: Boolean): Result<Unit> {
        setting.update { it.copy(isCanvasNotificationEnabled = enabled) }
        return Result.success(Unit)
    }

    override suspend fun updateActivityNotification(enabled: Boolean): Result<Unit> {
        setting.update { it.copy(isActivityNotificationEnabled = enabled) }
        return Result.success(Unit)
    }
}
