package com.teamyg.parfait.data.notification

import com.teamyg.parfait.data.model.qualifier.ApplicationScope
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.notification.DeviceTokenRegistrar
import com.teamyg.parfait.domain.usecase.notification.RegisterCurrentDeviceTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 호출자의 스코프가 아니라 [ApplicationScope] 에서 돈다 — 부르는 자리(로그인·가입·앱 진입)가
 * 곧바로 화면을 갈아 끼우므로 그 스코프에 매달면 등록이 도중에 취소된다.
 *
 * [Mutex] 로 겹침을 막는 이유는 서버 쪽 경합 때문이다 — 같은 신규 토큰으로 두 요청이 동시에
 * 들어가면 두 번째가 유니크 제약 위반으로 500 을 받는다(`api/notification.md` 등록 절).
 *
 * 재시도는 [MAX_ATTEMPTS] 회에서 멈춘다. 오래 끄는 대신 다음 세션 트리거에 맡긴다 — 서버가
 * 반복 호출을 전제로 upsert 를 설계했고, 실패가 이어지는 상황은 대개 이 스코프보다 오래 간다.
 */
@Singleton
class DeviceTokenRegistrarImpl @Inject constructor(
    private val registerCurrentDeviceToken: RegisterCurrentDeviceTokenUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : DeviceTokenRegistrar {
    private val mutex = Mutex()

    override fun register() {
        // 진행 중이면 그냥 돌아간다 — 대기시켜 봐야 같은 토큰을 한 번 더 올릴 뿐이다
        if (mutex.isLocked) return

        applicationScope.launch {
            if (!mutex.tryLock()) return@launch

            try {
                registerWithRetry()
            } finally {
                mutex.unlock()
            }
        }
    }

    private suspend fun registerWithRetry() {
        repeat(MAX_ATTEMPTS) { attempt ->
            val failure = registerCurrentDeviceToken().exceptionOrNull() ?: return

            val isLast = attempt == MAX_ATTEMPTS - 1
            repositoryLogger.w(failure) { "기기 토큰 등록 실패 ${attempt + 1}/$MAX_ATTEMPTS" }
            if (isLast) return

            delay(RETRY_DELAY * (attempt + 1))
        }
    }

    internal companion object {
        const val MAX_ATTEMPTS = 3
        private val RETRY_DELAY: Duration = 3.seconds
    }
}
