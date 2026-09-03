package com.teamyg.parfait.domain.usecase.notification

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.notification.DeviceTokenProvider
import javax.inject.Inject

/**
 * 알림 권한을 허용한 직후처럼, **토큰 값 없이** "지금 이 기기의 토큰을 등록해 달라"고만
 * 부르는 자리용이다. 토큰 자체는 앱 설치 시점에 이미 발급돼 있는 경우가 대부분이라(알림
 * 권한과 무관하게 SDK 가 만든다) `onNewToken` 이 다시 불리길 기다리면 등록이 영영 안
 * 될 수 있다 — 그래서 여기서 직접 [DeviceTokenProvider] 로 지금 값을 읽어 등록한다.
 *
 * 토큰을 아직 못 읽었으면([DeviceTokenProvider.currentToken] 이 `null`) 실패로 보지
 * 않는다 — SDK 가 나중에 발급하면 `onNewToken` 이 대신 등록한다.
 *
 * [DeviceTokenProvider.currentToken] 은(Firebase SDK Task 래핑이라) 예외를 던질 수 있다 —
 * [NotificationRepository][com.teamyg.parfait.domain.repository.notification.NotificationRepository]
 * 처럼 항상 `Result` 로만 실패를 알리는 이 계층의 관례를 지키려고 여기서 직접 잡는다.
 */
class RegisterCurrentDeviceTokenUseCase @Inject constructor(
    private val deviceTokenProvider: DeviceTokenProvider,
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase,
) {
    suspend operator fun invoke(): Result<Unit> {
        val deviceToken = runSuspendCatching { deviceTokenProvider.currentToken() }
            .getOrElse { throwable ->
                useCaseLogger.e(throwable) { "RegisterCurrentDeviceTokenUseCase - 토큰 조회 실패" }
                return Result.failure(throwable)
            }

        if (deviceToken == null) {
            useCaseLogger.i { "RegisterCurrentDeviceTokenUseCase - 아직 발급된 토큰이 없어 건너뛴다" }
            return Result.success(Unit)
        }

        return registerDeviceTokenUseCase(deviceToken)
    }
}
