package com.teamyg.parfait.push

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.teamyg.parfait.domain.model.notification.DeviceToken
import com.teamyg.parfait.domain.repository.notification.DeviceTokenProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseDeviceTokenProvider @Inject constructor() : DeviceTokenProvider {
    override suspend fun currentToken(): DeviceToken? = FirebaseMessaging
        .getInstance()
        .token
        .await()
        ?.let(::DeviceToken)

    /**
     * `com.google.android.gms:play-services-tasks` 의 [Task] 를 suspend 로 바꾼다.
     * 프로젝트에 `kotlinx-coroutines-play-services` 가 없어 이 호출 하나만을 위해 의존성을
     * 늘리는 대신 직접 감쌌다.
     *
     * 코루틴이 취소돼도 [Task] 는 못 멈춘다 — GMS `Task` 에는 리스너를 뗄 API가 없다.
     * `addOnCompleteListener` 콜백이 잡고 있는 이 continuation 은 `Task` 가 스스로 끝날 때까지
     * 살아있지만, 취소된 continuation 에 `resume` 을 불러도 조용히 무시되므로(코루틴 계약)
     * 크래시·오동작으로는 안 이어진다 — 참조가 잠깐 더 남는 것뿐이다.
     */
    private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Task 가 실패했지만 exception 이 없다"),
                )
            }
        }
    }
}
