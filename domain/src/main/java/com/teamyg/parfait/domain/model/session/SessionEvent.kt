package com.teamyg.parfait.domain.model.session

/**
 * 화면 하나가 결정할 수 없는 세션 수준의 사건.
 *
 * 지금은 갈래가 하나뿐이지만 `sealed` 로 두는 이유는, 소비 측이 `when` 을 exhaustive 로
 * 쓰게 해 갈래가 늘 때 컴파일 단계에서 누락을 잡기 위해서다.
 */
sealed interface SessionEvent {
    /** refresh token 이 서버에 거절당해 세션을 더 유지할 수 없다 */
    data object ForcedLogout : SessionEvent
}
