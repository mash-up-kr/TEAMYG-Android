package com.teamyg.parfait.domain.model.notification

/**
 * @property isCanvasNotificationEnabled 캔버스 오픈·마감 알림 수신 여부
 * @property isActivityNotificationEnabled 그룹원 활동 알림을 모아 받을지 여부
 */
data class NotificationSettingVO(
    val isCanvasNotificationEnabled: Boolean,
    val isActivityNotificationEnabled: Boolean,
)
