package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.exception.SignUpException
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.usecase.member.RefreshMyAccountUseCase
import javax.inject.Inject

/**
 * 약관 동의 내역과 함께 회원 가입을 완료하고, **성공하면 세션을 저장하고 계정 정보를 갱신한다.**
 *
 * 저장을 화면이 아니라 여기서 하는 이유는 [LoginWithKakaoUseCase] 와 같다 — 저장 전에
 * 내비게이션이 나가면 다음 화면의 첫 API 호출이 토큰 없이 나간다.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val refreshMyAccountUseCase: RefreshMyAccountUseCase,
) {
    /**
     * 서버는 동의하지 않은 약관도 `agreed = false` 로 함께 받아야 하므로
     * 화면에 노출한 [policies] 전체를 [agreedTermsIds] 와 묶어서 보낸다.
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

        // 세션 저장 직후 계정 정보를 한 번 당겨온다 — 실패해도 가입은 이미 성공했고
        // 되돌릴 곳이 없다. 값은 다음 앱 진입(스플래시)에서 채워진다.
        refreshMyAccountUseCase().onFailure {
            useCaseLogger.w(it) { "SignUpUseCase - refreshMyAccount failed" }
        }

        return Result.success(session)
    }
}
