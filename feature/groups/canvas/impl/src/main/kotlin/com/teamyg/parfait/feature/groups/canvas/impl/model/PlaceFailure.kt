package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 텍스트 쌓기 실패를 사용자에게 어떻게 안내할지 가르는 갈래 */
enum class PlaceFailure {
    Retryable,

    /** 다시 눌러도 같은 실패라 안내가 재시도를 권하지 않는다 */
    Permanent,

    /** 요청이 서버에 닿았는지 모른다. POST 가 멱등이 아니라 재시도 대신 쌓였는지 확인을 권한다 */
    Uncertain,
}
