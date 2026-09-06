package com.teamyg.parfait.domain.model.notification

/** FCM SDK 가 기기마다 발급하는 등록 토큰. */
@JvmInline
value class DeviceToken(val value: String)
