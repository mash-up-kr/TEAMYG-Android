package com.teamyg.parfait.feature.login.impl.util

import android.app.Activity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.domain.util.NonceGenerator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class KakaoLoginHelperTest {
    private val userApiClient: UserApiClient = mockk(relaxed = true)
    private val activity: Activity = mockk(relaxed = true)

    private val helper = KakaoLoginHelper(
        userApiClient = userApiClient,
        // `NonceGenerator` 는 `fun interface` 라 람다로 고정값을 준다
        nonceGenerator = NonceGenerator { "nonce-1" },
    )

    /**
     * SDK 호출은 콜백이 돌아와야 재개된다. 스텁이 아무것도 안 하면 `login` 이 영영 매달리므로
     * 실패 콜백을 즉시 되돌려 준다 — 이 테스트가 보는 것은 결과가 아니라 **어느 경로로 갔는가**다.
     */
    private fun stubAccountLoginWithFailure() {
        every {
            userApiClient.loginWithKakaoAccount(any(), any(), any(), any(), any(), any(), any())
        } answers {
            val callback = lastArg<(OAuthToken?, Throwable?) -> Unit>()
            callback(null, IllegalStateException("stub"))
        }
    }

    /**
     * `loginWithKakaoTalk` 도 콜백을 동기로 되돌려 줘야 `login` 이 매달리지 않는다.
     * 실패를 돌려주면 프로덕션 코드가 계정 로그인으로 폴백하므로, 폴백까지 끝까지 보려면
     * `stubAccountLoginWithFailure()` 도 함께 세워야 한다.
     */
    private fun stubKakaoTalkLoginWithFailure() {
        every {
            userApiClient.loginWithKakaoTalk(any(), any(), any(), any(), any(), any())
        } answers {
            val callback = lastArg<(OAuthToken?, Throwable?) -> Unit>()
            callback(null, IllegalStateException("stub"))
        }
    }

    @Test
    fun login_default_usesKakaoTalkWhenAvailable() = runTest {
        // Given 카카오톡을 쓸 수 있는 기기
        every { userApiClient.isKakaoTalkLoginAvailable(any()) } returns true
        stubKakaoTalkLoginWithFailure()
        stubAccountLoginWithFailure()

        // When 강제 없이 기본값으로 로그인한다
        helper.login(activity = activity)

        // Then 카카오톡 로그인 경로를 탄다 — 이 분기 조건이 이번 브랜치의 변경점이다
        verify(exactly = 1) {
            userApiClient.loginWithKakaoTalk(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun login_forceAccountLogin_skipsKakaoTalkEntirely() = runTest {
        // Given 카카오톡으로 로그인할 수 있는 기기
        every { userApiClient.isKakaoTalkLoginAvailable(any()) } returns true
        stubAccountLoginWithFailure()

        // When 웹 로그인을 강제한다
        val result = helper.login(activity = activity, forceAccountLogin = true)

        // Then 설치 여부를 묻지도 않고 계정 로그인으로 간다
        verify(exactly = 0) { userApiClient.isKakaoTalkLoginAvailable(any()) }
        verify(exactly = 1) {
            userApiClient.loginWithKakaoAccount(any(), any(), any(), any(), any(), any(), any())
        }
        assertIs<KakaoLoginResult.Failure>(result)
    }

    @Test
    fun login_default_checksKakaoTalkAvailability() = runTest {
        // Given 카카오톡이 없는 기기
        every { userApiClient.isKakaoTalkLoginAvailable(any()) } returns false
        stubAccountLoginWithFailure()

        // When 기본값으로 로그인한다
        helper.login(activity = activity)

        // Then 기존 경로 그대로 설치 여부를 먼저 묻는다
        verify(exactly = 1) { userApiClient.isKakaoTalkLoginAvailable(any()) }
    }
}
