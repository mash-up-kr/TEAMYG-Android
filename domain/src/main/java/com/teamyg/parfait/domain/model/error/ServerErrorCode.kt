package com.teamyg.parfait.domain.model.error

/**
 * 서버가 에러 envelope 에 실어 보내는 `code` 문자열.
 *
 * [AppError.Server.code] 와 대조할 때 쓴다. 화면마다 문자열 리터럴을 다시 적으면
 * 오타가 컴파일·lint 어디에도 안 걸리고 런타임에야 드러나므로 여기 한 곳에 모은다.
 *
 * ⚠️ **코드 문자열은 도메인 간 유일하지 않다.** 예를 들어 `MEMBER_NOT_FOUND` 는 인증
 * 도메인에서 401 이지만 그룹·이미지·회원 도메인에서는 404 다. 그래서 서버의 enum 구조
 * (`AuthErrorCode`·`ImageErrorCode` …)를 그대로 따라 도메인별로 감싼다 — 같은 문자열이
 * 서로 다른 상수로 남아야 소비 측이 둘을 헷갈리지 않는다. 분기할 때는 `statusCode` 도
 * 함께 본다.
 *
 * 여기에는 **앱이 실제로 분기에 쓰는 코드만** 둔다. 서버 enum 전체를 미리 옮겨 적지
 * 않는다 — 쓰지 않는 상수는 계약이 바뀌어도 아무도 고치지 않아 거짓말이 된다.
 */
object ServerErrorCode {
    /** 인증 도메인(`/api/v1/auth/...`) — 서버 `AuthErrorCode` 에 대응한다 */
    object Auth {
        /** 401 — 유효하지 않은 ID 토큰입니다 */
        const val INVALID_ID_TOKEN = "INVALID_ID_TOKEN"

        /** 502 — 카카오 공개키 조회에 실패했습니다 */
        const val KAKAO_JWKS_FETCH_FAILED = "KAKAO_JWKS_FETCH_FAILED"

        /** 503 — 카카오 서버에 연결할 수 없습니다 */
        const val KAKAO_SERVER_UNAVAILABLE = "KAKAO_SERVER_UNAVAILABLE"
    }
}
