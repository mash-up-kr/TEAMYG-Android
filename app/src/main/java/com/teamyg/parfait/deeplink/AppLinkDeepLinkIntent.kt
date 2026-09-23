package com.teamyg.parfait.deeplink

import android.content.Intent
import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink

/**
 * App Links 로 열린 `Intent` 에서 목적지를 읽는다. 쿼리 파라미터 이름은 서버 스펙 확정 전
 * 임시 설계라 [AppLinkDeepLink] 의 KDoc 과 함께 다시 봐야 한다.
 */
fun Intent.toAppLinkDeepLinkOrNull(): AppLinkDeepLink? {
    if (action != Intent.ACTION_VIEW) return null

    val uri = data ?: return null

    return AppLinkDeepLink.OpenGroupInvite.parse(inviteCode = uri.getQueryParameter("inviteCode"))
}
