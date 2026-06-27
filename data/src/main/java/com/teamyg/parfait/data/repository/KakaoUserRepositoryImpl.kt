package com.teamyg.parfait.data.repository

import android.app.Activity
import android.content.Context
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.teamyg.parfait.data.utils.CurrentActivityHolder
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.domain.repository.KakaoUserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class KakaoUserRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val activityHolder: CurrentActivityHolder,
    private val userApiClient: UserApiClient,
) : KakaoUserRepository {
    override fun isKakaoTalkLoginAvailable(): Boolean = userApiClient.isKakaoTalkLoginAvailable(context)

    override suspend fun loginWithKakaoTalk(): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        val activity: Activity? = activityHolder.currentActivity

        if (activity == null) {
            continuation.resume(
                value = KakaoLoginResult.Failure(throwable = IllegalStateException("no active Activity")),
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

    override suspend fun loginWithKakaoAccount(): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
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
