package com.teamyg.parfait.domain.exception

import com.teamyg.parfait.domain.model.id.TermsId

sealed class SignUpException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * 필수 약관에 동의하지 않은 채로 가입을 시도한 경우.
     * 화면에서 버튼을 막고 있어도 서버로 잘못된 요청이 나가지 않도록 도메인에서 한 번 더 막는다.
     */
    class RequiredPolicyNotAgreed(
        val termsIds: List<TermsId>,
    ) : SignUpException("동의하지 않은 필수 약관이 있다: $termsIds")
}
