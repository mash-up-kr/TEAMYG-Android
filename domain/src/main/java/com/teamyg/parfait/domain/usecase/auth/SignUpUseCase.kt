package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.exception.SignUpException
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import javax.inject.Inject

/**
 * 약관 동의 내역과 함께 회원 가입을 완료하고, **성공하면 세션을 저장한다.**
 *
 * 저장을 화면이 아니라 여기서 하는 이유는 [LoginWithKakaoUseCase] 와 같다 — 저장 전에
 * 내비게이션이 나가면 다음 화면의 첫 API 호출이 토큰 없이 나간다.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    /**
     * 서버는 동의하지 않은 약관도 `agreed = false` 로 함께 받아야 하므로,
     * 화면이 보여준 약관 전체를 동의 여부와 묶어서 보낸다.
     *
     * @param policies 화면에 노출한 약관 전체
     * @param agreedTermsIds 그중 사용자가 동의한 약관의 id
     */
    suspend operator fun invoke(
        registrationToken: RegistrationToken,
        policies: List<PolicyVO>,
        agreedTermsIds: Set<TermsId>,
    ): Result<AuthSessionVO> {
        val requiredNotAgreed = policies.filter { it.required && it.termsId !in agreedTermsIds }
        if (requiredNotAgreed.isNotEmpty()) {
            return Result.failure(SignUpException.RequiredPolicyNotAgreed(requiredNotAgreed.map(PolicyVO::termsId)))
        }

        val signUpResult = authRepository.signUp(
            registrationToken = registrationToken,
            agreements = policies.map { policy ->
                TermsAgreement(
                    termsId = policy.termsId,
                    agreed = policy.termsId in agreedTermsIds,
                )
            },
        )
        val session = signUpResult.getOrElse { return Result.failure(it) }

        // 저장 실패를 선언된 실패 채널로 되돌린다 — 그냥 던지면 호출부가 `Result` 만
        // 봐서는 영영 못 본다. 취소는 [runSuspendCatching] 이 걸러 재던진다.
        runSuspendCatching { authRepository.saveSession(session) }
            .getOrElse { return Result.failure(AppError.Unexpected(it)) }

        return Result.success(session)
    }
}
