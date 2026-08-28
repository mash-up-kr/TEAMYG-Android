package com.teamyg.parfait.feature.login.impl.util

import android.app.Activity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.domain.util.NonceGenerator
import kotlinx.coroutines.CancellableContinuation
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
    private val nonceGenerator: NonceGenerator,
) {
    /**
     * 로그인 1회분 nonce 를 만들어 SDK 요청에 넘기고, 성공 결과에 같은 값을 실어 돌려준다.
     * 카카오톡 로그인이 실패해 계정 로그인으로 넘어가도 **nonce 는 그대로 재사용**한다 —
     * 최종 성공한 로그인이 그 nonce 로 발급받은 ID 토큰을 주므로 서버 대조가 맞는다.
     *
     * @param forceAccountLogin 참이면 카카오톡 설치 여부를 묻지 않고 계정(웹) 로그인으로 간다.
     *   디버그 모드가 이 값을 세운다(`specs/2026-08-28-login-debug-mode.md`).
     */
    suspend fun login(
        activity: Activity,
        forceAccountLogin: Boolean = false,
    ): KakaoLoginResult {
        val nonce = nonceGenerator.generate()

        return if (!forceAccountLogin && isKakaoTalkLoginAvailable(activity)) {
            when (val result = loginWithKakaoTalk(activity, nonce)) {
                is KakaoLoginResult.Success -> result
                is KakaoLoginResult.Cancel -> result
                is KakaoLoginResult.Failure -> loginWithKakaoAccount(activity, nonce)
            }
        } else {
            loginWithKakaoAccount(activity, nonce)
        }
    }

    private fun isKakaoTalkLoginAvailable(activity: Activity): Boolean =
        userApiClient.isKakaoTalkLoginAvailable(activity)

    private suspend fun loginWithKakaoTalk(
        activity: Activity,
        nonce: String,
    ): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        userApiClient.loginWithKakaoTalk(
            context = activity,
            nonce = nonce,
            callback = { token, error -> continuation.resumeWithLoginResult(nonce, token, error) },
        )
    }

    private suspend fun loginWithKakaoAccount(
        activity: Activity,
        nonce: String,
    ): KakaoLoginResult = suspendCancellableCoroutine { continuation ->
        userApiClient.loginWithKakaoAccount(
            context = activity,
            nonce = nonce,
            callback = { token, error -> continuation.resumeWithLoginResult(nonce, token, error) },
        )
    }

    private fun CancellableContinuation<KakaoLoginResult>.resumeWithLoginResult(
        nonce: String,
        token: OAuthToken?,
        error: Throwable?,
    ) {
        val result = when {
            token != null -> token.toLoginResult(nonce)
            error == null -> KakaoLoginResult.Failure(IllegalStateException("token 과 error 가 모두 null 이다"))
            error is ClientError && error.reason == ClientErrorCause.Cancelled -> KakaoLoginResult.Cancel(error)
            else -> KakaoLoginResult.Failure(error)
        }
        resume(value = result, onCancellation = null)
    }

    /**
     * `idToken` 은 nullable 이다 — 카카오 개발자 콘솔에서 **OpenID Connect 가 꺼져 있으면
     * null** 이다. 서버는 이 값을 요구하므로 없으면 로그인 자체가 성립하지 않는다.
     */
    private fun OAuthToken.toLoginResult(nonce: String): KakaoLoginResult {
        val idToken = idToken
            ?: return KakaoLoginResult.Failure(
                IllegalStateException("idToken 이 null 이다 — 카카오 콘솔 OpenID Connect 활성화를 확인한다"),
            )
        return KakaoLoginResult.Success(idToken = idToken, nonce = nonce)
    }
}
