package com.teamyg.parfait.domain.model.pushdeeplink

/**
 * 푸시 알림을 탭했을 때 가야 할 목적지. 화면 하나가 결정할 수 없어 앱 루트에서 다룬다.
 *
 * `sealed` 로 두는 이유는 [SessionEvent][com.teamyg.parfait.domain.model.session.SessionEvent]
 * 와 같다 — 소비 측 `when` 을 exhaustive 로 둬 갈래가 늘 때 컴파일 단계에서 누락을 잡는다.
 */
sealed interface PushDeepLink {
    /** P-01 토핑 등록 알림. 알림이 가리키던 날짜가 아니라 항상 그 그룹의 최신 캔버스로 연다. */
    data class AddTopping(val groupId: Long) : PushDeepLink

    /** P-02/P-03 리마인드 알림. 그룹 목록으로 연다. */
    data object Reminder : PushDeepLink
}
