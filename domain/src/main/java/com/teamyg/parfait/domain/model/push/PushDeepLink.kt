package com.teamyg.parfait.domain.model.push

/**
 * 푸시 알림을 탭했을 때 가야 할 목적지. 화면 하나가 결정할 수 없어 앱 루트에서 다룬다.
 *
 * `sealed` 로 두는 이유는 [SessionEvent][com.teamyg.parfait.domain.model.session.SessionEvent]
 * 와 같다 — 소비 측 `when` 을 exhaustive 로 둬 갈래가 늘 때 컴파일 단계에서 누락을 잡는다.
 */
sealed interface PushDeepLink {
    /**
     * 라우팅에는 안 쓴다(각 하위 타입의 KDoc 참고) — 탭 분석 등 라우팅 밖의 용도로 들고 있는다.
     * 서버가 보낸 `type` 값이 아는 값이 아니면(향후 필드 추가 등) `null`.
     */
    val type: PushNotificationType?

    /** P-01 토핑 등록 알림. 알림이 가리키던 날짜가 아니라 항상 그 그룹의 최신 캔버스로 연다. */
    data class AddTopping(val groupId: Long) : PushDeepLink {
        override val type: PushNotificationType get() = PushNotificationType.TOPPING

        companion object {
            /** @param groupId FCM `data` 는 전부 String 이라 숫자가 아닌 값도 온다. */
            fun parse(groupId: String?): AddTopping? {
                val id = groupId?.toLongOrNull() ?: return null

                return if (id <= 0) null else AddTopping(groupId = id)
            }
        }
    }

    /**
     * P-02/P-03 리마인드 알림. 그룹 목록으로 연다.
     */
    data class GroupList(override val type: PushNotificationType?) : PushDeepLink
}
