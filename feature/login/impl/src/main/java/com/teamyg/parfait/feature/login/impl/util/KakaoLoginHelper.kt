package com.teamyg.parfait.feature.login.impl.util

import android.app.Activity
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.teamyg.parfait.domain.model.KakaoLoginResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference

@OptIn(ExperimentalCoroutinesApi::class)
class KakaoLoginHelper
@AssistedInject
constructor(
    // Kakao SDK의 loginWithKakaoTalk/loginWithKakaoAccount 는 내부적으로 startActivity()를 호출하므로
    // Activity context가 필요합니다. ApplicationContext를 넘기면 FLAG_ACTIVITY_NEW_TASK 누락으로 크래시.
    @Assisted private val activity: WeakReference<Activity>,
    private val userApiClient: UserApiClient,
) {
    suspend fun login(): KakaoLoginResult = if (isKakaoTalkLoginAvailable()) {
        when (val result = loginWithKakaoTalk()) {
            is KakaoLoginResult.Success -> result
            is KakaoLoginResult.Cancel -> result
            is KakaoLoginResult.Failure -> loginWithKakaoAccount()
        }
    } else {
        loginWithKakaoAccount()
    }

    private fun isKakaoTalkLoginAvailable(): Boolean {
        val activity = activity.get() ?: return false

        return userApiClient.isKakaoTalkLoginAvailable(activity)
    }

    private suspend fun loginWithKakaoTalk(): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        val activity = activity.get()

        if (activity == null) {
            continuation.resume(
                value = KakaoLoginResult.Failure(throwable = IllegalStateException("activity is null")),
                onCancellation = null,
            )
            return@suspendCancellableCoroutine
        }

        userApiClient.loginWithKakaoTalk(
            context = activity,
            callback = { token, error ->
                when {
                    token != null -> {
                        continuation.resume(
                            value = KakaoLoginResult.Success(token = token.accessToken),
                            onCancellation = null,
                        )
                    }

                    error == null -> {
                        continuation.resume(
                            value = KakaoLoginResult.Failure(throwable = IllegalStateException("token is null")),
                            onCancellation = null,
                        )
                    }

                    error is ClientError && error.reason == ClientErrorCause.Cancelled -> {
                        continuation.resume(
                            value = KakaoLoginResult.Cancel(throwable = error),
                            onCancellation = null,
                        )
                    }

                    else -> {
                        continuation.resume(
                            value = KakaoLoginResult.Failure(throwable = error),
                            onCancellation = null,
                        )
                    }
                }
            },
        )
    }

    private suspend fun loginWithKakaoAccount(): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        val activity = activity.get()

        if (activity == null) {
            continuation.resume(
                value = KakaoLoginResult.Failure(throwable = IllegalStateException("activity is null")),
                onCancellation = null,
            )
            return@suspendCancellableCoroutine
        }

        userApiClient.loginWithKakaoAccount(
            context = activity,
            callback = { token, error ->
                when {
                    token != null -> {
                        continuation.resume(
                            value = KakaoLoginResult.Success(token = token.accessToken),
                            onCancellation = null,
                        )
                    }

                    error == null -> {
                        continuation.resume(
                            value = KakaoLoginResult.Failure(throwable = IllegalStateException("token is null")),
                            onCancellation = null,
                        )
                    }

                    error is ClientError && error.reason == ClientErrorCause.Cancelled -> {
                        continuation.resume(
                            value = KakaoLoginResult.Cancel(throwable = error),
                            onCancellation = null,
                        )
                    }

                    else -> {
                        continuation.resume(
                            value = KakaoLoginResult.Failure(throwable = error),
                            onCancellation = null,
                        )
                    }
                }
            },
        )
    }
}

@AssistedFactory
interface KakaoLoginHelperFactory {
    fun create(activity: WeakReference<Activity>): KakaoLoginHelper
}
