package com.teamyg.parfait.domain.model.deeplink

import com.teamyg.parfait.domain.model.group.InviteCode

/**
 * 외부 링크(App Links/Universal Links, 또는 스토어 설치 후 최초 실행 시 Install Referrer)로
 * 들어왔을 때 가야 할 목적지. [com.teamyg.parfait.domain.model.push.PushDeepLink] 와 갈래를
 * 합치지 않은 이유는 발행 경로가 다르기 때문이다 — 이쪽은 URL 쿼리 파라미터에서 오고, 그쪽은
 * FCM data payload 에서 온다.
 *
 * ⚠️ 실제 URL 파라미터 스펙은 서버가 아직 확정하지 않았다. 지금 값(`inviteCode`)은 임시로
 * 설계한 것이라, 서버 스펙이 나오면 이 타입을 다시 봐야 한다.
 */
sealed interface AppLinkDeepLink {
    /** 코드 유효성은 [InviteCode] 규칙을 그대로 따른다. */
    data class OpenGroupInvite(val inviteCode: String) : AppLinkDeepLink {
        companion object {
            fun parse(inviteCode: String?): OpenGroupInvite? =
                InviteCode.parseOrNull(inviteCode)?.let { OpenGroupInvite(inviteCode = it.value) }
        }
    }
}
