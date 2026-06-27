package com.teamyg.parfait.feature.login.impl.util

import android.content.Context
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.teamyg.parfait.domain.model.KakaoLoginResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class KakaoLoginHelper
@AssistedInject
constructor(
    @Assisted private val context: Context, // do not use applicationContext
    private val userApiClient: UserApiClient,
) {
    fun isKakaoTalkLoginAvailable(): Boolean = userApiClient.isKakaoTalkLoginAvailable(context)

    suspend fun loginWithKakaoTalk(): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        userApiClient.loginWithKakaoTalk(
            context = context,
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

    suspend fun loginWithKakaoAccount(): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        userApiClient.loginWithKakaoAccount(
            context = context,
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
    fun create(context: Context): KakaoLoginHelper
}
