package com.teamyg.parfait.domain.model.push

/**
 * 푸시 알림을 탭했을 때 가야 할 목적지. 화면 하나가 결정할 수 없어 앱 루트에서 다룬다.
 *
 * `sealed` 로 두는 이유는 [SessionEvent][com.teamyg.parfait.domain.model.session.SessionEvent]
 * 와 같다 — 소비 측 `when` 을 exhaustive 로 둬 갈래가 늘 때 컴파일 단계에서 누락을 잡는다.
 */
sealed interface PushDeepLink {
    /**
     * FCM `data.type` 을 서버가 보낸 그대로 들고 있다. 라우팅에는 안 쓰고(각 하위 타입의 KDoc 참고)
     * 탭 분석에만 쓴다. enum 으로 좁히지 않는 이유: 서버가 값을 추가·변경해도 분석이 그 값을
     * 그대로 따라가야 해서다. 키가 없으면 `null`.
     */
    val type: String?

    /** P-01 토핑 등록 알림. 알림이 가리키던 날짜가 아니라 항상 그 그룹의 최신 캔버스로 연다. */
    data class AddTopping(
        val groupId: Long,
        override val type: String?,
    ) : PushDeepLink {
        companion object {
            /** @param groupId FCM `data` 는 전부 String 이라 숫자가 아닌 값도 온다. */
            fun parse(
                groupId: String?,
                type: String?,
            ): AddTopping? {
                val id = groupId?.toLongOrNull() ?: return null

                return if (id <= 0) null else AddTopping(groupId = id, type = type)
            }
        }
    }

    /**
     * P-02/P-03 리마인드 알림. 그룹 목록으로 연다.
     */
    data class GroupList(override val type: String?) : PushDeepLink
}
