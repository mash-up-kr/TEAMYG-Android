package com.teamyg.data.repository

import android.content.Context
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.teamyg.domain.entity.KakaoLoginResult
import com.teamyg.domain.repository.KakaoUserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class KakaoUserRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val userApiClient: UserApiClient,
    ) : KakaoUserRepository {
        override fun isKakaoTalkLoginAvailable(): Boolean = userApiClient.isKakaoTalkLoginAvailable(context)

        override suspend fun loginWithKakaoTalk(): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
            userApiClient.loginWithKakaoTalk(
                context,
                callback = { token, error ->
                    if (error != null) {
                        // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                        // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            continuation.resume(
                                value = KakaoLoginResult.Cancel(throwable = error),
                                onCancellation = null,
                            )
                        } else {
                            continuation.resume(
                                value = KakaoLoginResult.Failure(throwable = error),
                                onCancellation = null,
                            )
                        }
                    } else if (token != null) {
                        continuation.resume(
                            value = KakaoLoginResult.Success(token = token.accessToken),
                            onCancellation = null,
                        )
                    } else {
                        continuation.resume(
                            value = KakaoLoginResult.Failure(throwable = IllegalStateException("token is null")),
                            onCancellation = null,
                        )
                    }
                },
            )
        }

        override suspend fun loginWithKakaoAccount(): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
            userApiClient.loginWithKakaoAccount(
                context,
                callback = { token, error ->
                    if (error != null) {
                        continuation.resume(
                            value = KakaoLoginResult.Failure(throwable = error),
                            onCancellation = null,
                        )
                    } else if (token != null) {
                        continuation.resume(
                            value = KakaoLoginResult.Success(token = token.accessToken),
                            onCancellation = null,
                        )
                    } else {
                        continuation.resume(
                            value = KakaoLoginResult.Failure(throwable = IllegalStateException("token is null")),
                            onCancellation = null,
                        )
                    }
                },
            )
        }
    }
