package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import kotlinx.coroutines.CancellationException
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
    ): Result<KakaoLoginVO> {
        val loginResult = authRepository.loginWithKakao(idToken = idToken, nonce = nonce)
        val member = loginResult.getOrElse { return Result.failure(it) }

        if (member is KakaoLoginVO.ExistingMember) {
            try {
                authRepository.saveSession(member.session)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // `Result.onSuccess` 는 inline 이라 여기서 던지면 선언된 실패 채널을
                // 새어나가 호출부가 `Result` 만 봐서는 영영 못 본다 — 여기서 닫는다.
                return Result.failure(AppError.Unexpected(e))
            }
        }

        return Result.success(member)
    }
}
