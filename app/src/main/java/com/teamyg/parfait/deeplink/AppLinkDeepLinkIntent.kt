package com.teamyg.parfait.deeplink

import android.content.Intent
import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink

/** 서버가 App Links 로 서빙하는 경로 — 매니페스트 필터와 같아야 한다. 루트(`/`)는 랜딩 페이지라 앱으로 열면 안 된다. */
private const val APP_LINK_PATH = "/link"

/**
 * App Links 로 열린 `Intent` 에서 목적지를 읽는다. 링크 양식은
 * `{host}/link?inviteCode={code}` 로 확정이다.
 */
fun Intent.toAppLinkDeepLinkOrNull(): AppLinkDeepLink? {
    if (action != Intent.ACTION_VIEW) return null

    val uri = data ?: return null

    val path = uri.path.orEmpty()
    if (path != APP_LINK_PATH && !path.startsWith("$APP_LINK_PATH/")) return null

    val inviteCode = uri.getQueryParameter(AppLinkDeepLink.OpenGroupInvite.INVITE_CODE_QUERY_KEY)
    return AppLinkDeepLink.OpenGroupInvite.parse(inviteCode = inviteCode)
}
