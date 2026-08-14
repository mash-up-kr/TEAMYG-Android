package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import javax.inject.Inject

/**
 * 카카오 ID 토큰으로 서버 로그인을 하고, **기존 회원이면 세션을 저장한다.**
 *
 * 저장을 화면이 아니라 여기서 하는 이유: 로그인 진입점이 늘어날 때마다 잊을 수 있고,
 * 저장 전에 내비게이션이 나가면 다음 화면의 첫 API 호출이 토큰 없이 나간다.
 */
class LoginWithKakaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        idToken: String,
        nonce: String,
    ): Result<KakaoLoginVO> = authRepository
        .loginWithKakao(idToken = idToken, nonce = nonce)
        .onSuccess { result ->
            if (result is KakaoLoginVO.ExistingMember) {
                authRepository.saveSession(result.session)
            }
        }
}
