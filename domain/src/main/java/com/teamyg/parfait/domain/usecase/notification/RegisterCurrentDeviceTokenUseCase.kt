package com.teamyg.parfait.domain.usecase.notification

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.notification.DeviceTokenProvider
import javax.inject.Inject

/**
 * **토큰 값 없이** "지금 이 기기의 토큰을 등록해 달라"고만 부르는 자리용이다. 토큰은 알림
 * 권한과 무관하게 SDK 가 설치 시점에 발급하므로, `onNewToken` 이 다시 불리기를 기다리면
 * 등록이 영영 안 될 수 있다 — 그래서 여기서 직접 지금 값을 읽어 등록한다.
 *
 * [DeviceTokenProvider.currentToken] 은 예외를 던질 수 있다. 실패를 `Result` 로만 알리는
 * 이 계층의 관례를 지키려고 여기서 직접 잡는다.
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

        return registerDeviceTokenUseCase(deviceToken)
    }
}
