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
 * **처분이 이미 정해진 코드는 예외로 미리 둔다** — 그때는 어떻게 다룰지를 그 상수의 KDoc 에
 * 함께 적어, 소비처가 붙을 때 결정을 다시 하지 않게 한다.
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

        /** 403 — 그 그룹의 멤버가 아니다. 토핑 배치 POST 가 마감 검사보다 **먼저** 이 검사를 한다 */
        const val GROUP_NOT_JOINED = "GROUP_NOT_JOINED"
    }

    /** 회원 도메인(`/api/v1/users/...`) — 서버 `MemberErrorCode` 에 대응한다 */
    object Member {
        /** 400 — 전역 닉네임 길이·문자 패턴 위반. 앱은 `CheckNameValidUseCase` 가 요청 전에
         * 같은 규칙으로 미리 거른다 — 이 코드는 그 방어를 뚫고 서버까지 간 나머지 경우다 */
        const val INVALID_NICKNAME = "INVALID_NICKNAME"

        /** 404 — 조회·갱신 대상 회원 부재. 같은 문자열이 다른 도메인에서는 401/409 로도 쓰여
         * [ParfaitGroup.MEMBER_NOT_FOUND] 와 나눠 둔다 */
        const val MEMBER_NOT_FOUND = "MEMBER_NOT_FOUND"
    }

    /**
     * 캔버스 도메인(`/api/v1/groups/{groupId}/parfaits...`) — 서버 `ParfaitErrorCode` 에 대응한다.
     *
     * ⚠️ 토핑 API(`.../parfaits/{parfaitId}/images...`)도 마감 거부만은 자기 enum 이 아니라
     * 이 코드로 낸다 — 서버가 `ParfaitErrorCode` 를 그대로 던지기 때문이다(`api/parfait-image.md`).
     */
    object Parfait {
        /**
         * 409 — 마감된(CLOSED·EMPTY) 캔버스에 쓰기를 보냈다. 토핑 배치·수정·테두리·삭제와
         * 배경 변경 다섯 경로가 이 코드를 낸다.
         *
         * ⚠️ 다섯 경로 전부 **권한 검사가 마감 검사보다 앞이다** — 마감된 캔버스라도 남의
         * 토핑이거나 그룹 멤버가 아니면 409 가 아니라 403 이 먼저 온다(수정·테두리·삭제는
         * PARFAIT_IMAGE_NOT_OWNED, 배치·배경 변경은 GROUP_NOT_JOINED). 마감을 유일한 실패로
         * 두고 분기하면 그 경우를 놓친다. 검사 순서 전문은 `api/parfait-image.md`.
         *
         * **사용자 잘못이 아니라 시간이 지난 것이다.** 03시 회전 배치가 캔버스를 닫는 사이
         * 화면이 열려 있었을 때 나온다 — 앱은 오늘 캔버스만 편집 대상으로 두지만
         * (`CanvasMainUiState.todayCanvas`) 편집을 시작한 뒤 경계를 넘으면 그 방어를 지나간다.
         *
         * 처분이 다른 실패와 다르다 — **되돌리지 않고 알린다.** 토핑 추가 흐름이 이 코드를
         * 받으면 화면 이동은 그대로 진행하고 실패만 알린다(막 만든 토핑을 들고 갈 곳이 없어지면
         * 사용자가 작업을 통째로 잃는다). 재시도해도 같은 결과이므로 재시도를 권하지 않고,
         * 새 캔버스를 받으려면 오늘 조회를 다시 해야 한다.
         */
        const val PARFAIT_ALREADY_CLOSED = "PARFAIT_ALREADY_CLOSED"
    }

    /** 서버 `ParfaitImageErrorCode` 에 대응한다 */
    object ParfaitImage {
        /**
         * 404 — `parfaitId` 가 그 그룹의 파르페가 아니다.
         *
         * ⚠️ "존재하지 않음"과 "남의 그룹 것"을 구분하지 않는다 — 서버가
         * `findByIdAndGroupId` 하나로 판정한다. 같은 문자열을 `ParfaitErrorCode` 도 갖지만
         * (캔버스 상세 조회·배경 변경) 둘 다 404 라 와이어에서 구분되지 않는다 — 그래서 이
         * 문자열을 보는 판정은 `code` 단독으로 한다. 검사 순서는 `api/parfait-image.md`.
         */
        const val PARFAIT_NOT_FOUND = "PARFAIT_NOT_FOUND"

        /**
         * 400 — `SOLID`인데 색·두께 중 하나가 없다. 토핑 배치 POST 가 던질 수 있는 코드다
         * (`ToppingBorder` 가 그 조합을 표현 불가능하게 만들어 앱에서는 도달하지 않지만,
         * 서버 계약에는 있다 — `api/parfait-image.md` 배치(POST) 실패 표).
         */
        const val INVALID_BORDER = "INVALID_BORDER"
    }

    /** 도메인을 가리지 않는 공통 코드 — 서버 `CommonErrorCode` 에 대응한다 */
    object Common {
        /** 400 — 바디 파싱 실패·필드 누락·타입 불일치. 정상 동선에서는 나오지 않는다(= 앱 버그) */
        const val INVALID_REQUEST = "INVALID_REQUEST"
    }
}
