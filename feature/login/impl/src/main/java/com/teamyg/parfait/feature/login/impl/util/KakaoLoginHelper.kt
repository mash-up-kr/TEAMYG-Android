package com.teamyg.parfait.feature.login.impl.util

import android.app.Activity
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.teamyg.parfait.domain.model.KakaoLoginResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class KakaoLoginHelper
@Inject
constructor(
    private val userApiClient: UserApiClient,
) {
    suspend fun login(activity: Activity): KakaoLoginResult = if (isKakaoTalkLoginAvailable(activity)) {
        when (val result = loginWithKakaoTalk(activity)) {
            is KakaoLoginResult.Success -> result
            is KakaoLoginResult.Cancel -> result
            is KakaoLoginResult.Failure -> loginWithKakaoAccount(activity)
        }
    } else {
        loginWithKakaoAccount(activity)
    }

    private fun isKakaoTalkLoginAvailable(activity: Activity): Boolean =
        userApiClient.isKakaoTalkLoginAvailable(activity)

    private suspend fun loginWithKakaoTalk(activity: Activity): KakaoLoginResult =
        suspendCancellableCoroutine { continuation ->
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

    private suspend fun loginWithKakaoAccount(activity: Activity): KakaoLoginResult =
        suspendCancellableCoroutine { continuation ->
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
