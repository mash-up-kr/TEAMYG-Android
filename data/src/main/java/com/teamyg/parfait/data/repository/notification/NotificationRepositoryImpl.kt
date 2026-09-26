package com.teamyg.parfait.data.repository.notification

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.data.model.qualifier.ApplicationScope
import com.teamyg.parfait.data.source.notification.remote.NotificationRemoteDataSource
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.provider.DeviceTokenProvider
import com.teamyg.parfait.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 등록은 [ApplicationScope] 에서 돈다. 부르는 자리가 곧바로 화면을 갈아 끼워, 호출자 스코프면 도중에 취소된다.
 *
 * [Mutex] 로 겹침을 막는다. 같은 신규 토큰으로 두 요청이 동시에 들어가면 두 번째가 유니크 제약 위반으로
 * 500 을 받는다(`docs/api/notification.md` 등록 절).
 *
 * 재시도는 [MAX_ATTEMPTS] 회에서 멈추고 나머지는 다음 세션 트리거에 맡긴다. 서버 등록이 upsert 라 반복 호출해도 된다.
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val deviceTokenProvider: DeviceTokenProvider,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : NotificationRepository {
    private val mutex = Mutex()

    override fun registerCurrentDeviceToken() {
        // 진행 중이면 돌아간다. 대기시켜 봐야 같은 토큰을 한 번 더 올릴 뿐이다
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
            val failure = registerOnce().exceptionOrNull() ?: return
            val isLast = attempt == MAX_ATTEMPTS - 1

            repositoryLogger.w(failure) { "기기 토큰 등록 실패 ${attempt + 1}/$MAX_ATTEMPTS" }
            if (isLast) return

            delay(RETRY_DELAY * (attempt + 1))
        }
    }

    /** 토큰 조회 실패도 `Result` 로 옮겨 서버 실패와 같은 재시도 경로에 싣는다 */
    private suspend fun registerOnce(): Result<Unit> {
        val deviceToken = runSuspendCatching { deviceTokenProvider.currentToken() }
            .getOrElse { throwable -> return Result.failure(throwable) }

        return notificationRemoteDataSource.registerDeviceToken(deviceToken)
    }

    internal companion object {
        const val MAX_ATTEMPTS = 3
        private val RETRY_DELAY: Duration = 3.seconds
    }
}
