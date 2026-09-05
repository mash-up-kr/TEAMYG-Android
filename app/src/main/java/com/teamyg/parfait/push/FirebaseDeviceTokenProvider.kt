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
     * 코루틴이 취소돼도 [Task] 는 멈추지 않는다 — GMS `Task` 에는 리스너를 뗄 API 가 없다.
     * 취소된 continuation 에 `resume` 을 불러도 무시되므로 `Task` 가 끝날 때까지 참조가
     * 잠깐 더 남을 뿐이다.
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
