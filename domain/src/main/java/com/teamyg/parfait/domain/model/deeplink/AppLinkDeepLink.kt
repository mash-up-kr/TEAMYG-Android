package com.teamyg.parfait.domain.model.deeplink

import com.teamyg.parfait.domain.model.group.InviteCode

/**
 * 외부 링크(App Links/Universal Links, 또는 스토어 설치 후 최초 실행 시 Install Referrer)로
 * 들어왔을 때 가야 할 목적지. [com.teamyg.parfait.domain.model.push.PushDeepLink] 와 갈래를
 * 합치지 않은 이유는 발행 경로가 다르기 때문이다 — 이쪽은 URL 쿼리 파라미터에서 오고, 그쪽은
 * FCM data payload 에서 온다.
 */
sealed interface AppLinkDeepLink {
    data class OpenGroupInvite(val inviteCode: String) : AppLinkDeepLink {
        companion object {
            const val INVITE_CODE_QUERY_KEY = "inviteCode"

            fun parse(inviteCode: String?): OpenGroupInvite? =
                InviteCode.parseOrNull(inviteCode)?.let { OpenGroupInvite(inviteCode = it.value) }
        }
    }
}
