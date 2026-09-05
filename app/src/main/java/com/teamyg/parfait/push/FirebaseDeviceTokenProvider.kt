package com.teamyg.parfait.push

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.teamyg.parfait.domain.model.notification.DeviceToken
import com.teamyg.parfait.domain.notification.DeviceTokenProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * `getToken()` 은 SDK 25.1.0 에서 deprecated 됐고 대체는 FID 기반 `register()` 다. 옮기려면
 * 서버 발송 필드까지 함께 바꿔야 해서 지금은 등록 토큰 축에 남는다 —
 * 근거와 전환 조건은 specs/2026-09-05-push-notification-permission-and-device-token 결정 6.
 */
class FirebaseDeviceTokenProvider @Inject constructor() : DeviceTokenProvider {
    @Suppress("DEPRECATION")
    override suspend fun currentToken(): DeviceToken = DeviceToken(
        FirebaseMessaging.getInstance().token.await(),
    )

    /**
     * 코루틴이 취소돼도 [Task] 는 멈추지 않는다 — GMS `Task` 에는 리스너를 뗄 API 가 없다.
     * 취소된 continuation 에 `resume` 을 불러도 무시되므로 `Task` 가 끝날 때까지 참조가
     * 잠깐 더 남을 뿐이다.
     */
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            val result = task.result

            when {
                !task.isSuccessful -> continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Task 가 실패했지만 exception 이 없다"),
                )

                result == null -> continuation.resumeWithException(
                    IllegalStateException("Task 가 성공했지만 값이 없다"),
                )

                else -> continuation.resume(result)
            }
        }
    }
}
