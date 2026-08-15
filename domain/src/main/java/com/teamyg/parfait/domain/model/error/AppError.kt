package com.teamyg.parfait.domain.model.error

/**
 * 화면·UseCase 가 보는 도메인 에러.
 *
 * 갈래가 셋인 이유는 화면이 실제로 다르게 굴 수 있는 경우가 셋뿐이기 때문이다 —
 * 재시도를 권할 수 있는가([Network]), 서버가 말해준 이유를 보여줄 수 있는가([Server]),
 * 원인을 알 수 없어 일반 문구밖에 못 주는가([Unexpected]).
 *
 * `Exception` 하위인 이유는 `Result.failure` 가 `Throwable` 을 요구해서다.
 * 데이터 레이어의 `ApiException` 을 Repository 경계에서 이 타입으로 바꾼다.
 */
sealed class AppError(
    message: String?,
    cause: Throwable?,
) : Exception(message, cause) {
    /** 연결 실패·타임아웃. 재시도가 의미 있는 유일한 갈래다 */
    data class Network(override val cause: Throwable?) : AppError(cause?.message, cause)

    /**
     * 서버가 에러 envelope 를 준 경우.
     *
     * [code] 를 enum 이 아니라 String 으로 두는 이유: 서버가 코드를 추가할 때마다 앱이
     * 깨지면 안 된다. 코드 문자열은 도메인 간 유일하지 않으므로([statusCode] 가 다른
     * 동명 코드가 존재한다) 분기할 때 둘을 함께 본다.
     */
    data class Server(
        val code: String,
        val statusCode: Int?,
        val serverMessage: String,
    ) : AppError(serverMessage, null)

    /**
     * envelope 밖 HTTP 실패·빈 본문·파싱/매핑 실패 등 그 외 전부.
     *
     * **원인이 어느 쪽인지 모르는 갈래다** — 서버가 규약 밖 응답을 준 것일 수도, 앱이 계약을
     * 잘못 읽은 것일 수도 있다. 그래서 화면에 줄 수 있는 것도 일반 문구뿐이고,
     * 원인 추적은 [cause] 로그가 맡는다.
     */
    data class Unexpected(override val cause: Throwable?) : AppError(cause?.message, cause)
}
