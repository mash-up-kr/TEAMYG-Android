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

        /** 401 — 유효하지 않은 토큰입니다. `reissue`·`logout` 의 refreshToken 검증 실패 */
        const val INVALID_TOKEN = "INVALID_TOKEN"

        /** 401 — 만료된 토큰입니다. `reissue`·`logout` 의 refreshToken */
        const val EXPIRED_TOKEN = "EXPIRED_TOKEN"

        /** 403 — 다른 회원의 Refresh Token 입니다. `logout` 의 `LogoutService` */
        const val FORBIDDEN_REFRESH_TOKEN = "FORBIDDEN_REFRESH_TOKEN"

        /** 502 — 카카오 공개키 조회에 실패했습니다 */
        const val KAKAO_JWKS_FETCH_FAILED = "KAKAO_JWKS_FETCH_FAILED"

        /** 503 — 카카오 서버에 연결할 수 없습니다 */
        const val KAKAO_SERVER_UNAVAILABLE = "KAKAO_SERVER_UNAVAILABLE"
    }

    /** 그룹 도메인(`/api/parfait-groups...`) — 서버 `ParfaitGroupApiErrorCode` 에 대응한다 */
    object ParfaitGroup {
        /** 404 — 초대코드에 해당하는 그룹이 없다 */
        const val INVALID_INVITE_CODE = "INVALID_INVITE_CODE"

        /** 409 — 이미 참여한 그룹이다 */
        const val GROUP_ALREADY_JOINED = "GROUP_ALREADY_JOINED"

        /** 409 — 그룹 정원이 찼다 */
        const val GROUP_MEMBER_LIMIT_REACHED = "GROUP_MEMBER_LIMIT_REACHED"

        /** 400 — 그룹명이 1~10자·`^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+(?: [가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+)*$` 를 벗어났다 */
        const val INVALID_GROUP_NAME = "INVALID_GROUP_NAME"

        /** 400 — 그룹 닉네임이 1~15자·그룹명과 같은 패턴을 벗어났다 */
        const val INVALID_GROUP_NICKNAME = "INVALID_GROUP_NICKNAME"

        /** 400 — 그룹 정원이 1~12 밖이다 */
        const val INVALID_GROUP_MEMBER_LIMIT = "INVALID_GROUP_MEMBER_LIMIT"

        /** 404 — 토큰의 memberId 가 서버에 없다. 같은 문자열이 인증 도메인에서는 401 이라 [Auth] 와 나눠 둔다 */
        const val MEMBER_NOT_FOUND = "MEMBER_NOT_FOUND"
    }

    /** 회원 도메인(`/api/v1/users/...`) — 서버 `MemberErrorCode` 에 대응한다 */
    object Member {
        /** 400 — 전역 닉네임 길이·문자 패턴 위반. `GlobalNickname.of` */
        const val INVALID_NICKNAME = "INVALID_NICKNAME"

        /** 404 — 조회·갱신 대상 회원 부재. 같은 문자열이 다른 도메인에서는 401/409 로도 쓰여
         * [ParfaitGroup.MEMBER_NOT_FOUND] 와 나눠 둔다 */
        const val MEMBER_NOT_FOUND = "MEMBER_NOT_FOUND"
    }

    /** 도메인을 가리지 않는 공통 코드 — 서버 `CommonErrorCode` 에 대응한다 */
    object Common {
        /** 400 — 바디 파싱 실패·필드 누락·타입 불일치. 정상 동선에서는 나오지 않는다(= 앱 버그) */
        const val INVALID_REQUEST = "INVALID_REQUEST"
    }
}
