package com.teamyg.parfait.data.installer.image

import com.teamyg.parfait.data.utils.repositoryLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 모듈 준비의 단일 소유자.
 *
 * ⚠️ **코루틴 스코프를 들지 않는다.** 스코프를 소유하면 취소·에러·재시작을 이 클래스가 증명해야
 * 하는데 그럴 수 없다. 대기는 호출자의 스코프에서 일어나고, 진행 중인 설치만 여기서 공유한다.
 */
@Singleton
class SegmentationModuleInstaller
@Inject
constructor(
    private val gateway: ModuleInstallGateway,
) {
    private val mutex = Mutex()
    private var inFlight: CompletableDeferred<ModuleInstallSignal>? = null

    suspend fun ensureInstalled(): ModuleInstallOutcome {
        if (gateway.isAvailable()) return ModuleInstallOutcome.Ready

        val pending = mutex.withLock {
            // 끝난 대기를 재사용하면 한 번 실패한 뒤 재시도가 영영 그 실패만 돌려준다
            inFlight?.takeIf { !it.isCompleted } ?: startInstall().also { inFlight = it }
        }

        val signal = withTimeoutOrNull(INSTALL_TIMEOUT_MS) { pending.await() }
            ?: return timedOut(pending)

        return signal.toOutcome()
    }

    private fun startInstall(): CompletableDeferred<ModuleInstallSignal> {
        val deferred = CompletableDeferred<ModuleInstallSignal>()

        gateway.install { signal ->
            repositoryLogger.i { "$LOG_PREFIX 설치 신호 $signal" }
            deferred.complete(signal)
        }

        return deferred
    }

    private suspend fun timedOut(pending: CompletableDeferred<ModuleInstallSignal>): ModuleInstallOutcome {
        repositoryLogger.w { "$LOG_PREFIX 설치 대기가 ${INSTALL_TIMEOUT_MS}ms 를 넘겨 포기한다" }
        mutex.withLock { if (inFlight === pending) inFlight = null }

        return ModuleInstallOutcome.TimedOut
    }

    /**
     * 완료 신호에도 가용 여부를 다시 묻는다 — 성공으로 접으면 곧바로 process 가 죽는데 로그에는
     * 설치 성공만 남아 원인이 가려진다(스펙 「종료 판정」 절).
     */
    private suspend fun ModuleInstallSignal.toOutcome(): ModuleInstallOutcome = when (this) {
        is ModuleInstallSignal.Failed -> ModuleInstallOutcome.Failed(installState, errorCode)

        ModuleInstallSignal.AlreadyInstalled,
        ModuleInstallSignal.Completed,
        -> if (gateway.isAvailable()) {
            ModuleInstallOutcome.Ready
        } else {
            ModuleInstallOutcome.Failed(installState = STATE_COMPLETED_BUT_UNAVAILABLE, errorCode = 0)
        }
    }

    companion object {
        internal const val INSTALL_TIMEOUT_MS = 20_000L

        /** `ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED` 와 값이 같다 */
        internal const val STATE_COMPLETED_BUT_UNAVAILABLE = 4

        private const val LOG_PREFIX = "[MLKIT-MODULE]"
    }
}
